package com.ai.learning.planner.mcp.server;

import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import jakarta.annotation.PreDestroy;

/**
 * MCP Server — JSON-RPC 2.0 端点
 * 实现 MCP 协议核心方法：tools/list、tools/call、resources/list、ping
 * 支持两种传输：HTTP POST（同步）和 SSE（流式推送）
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpServerController {

    private final AgentToolManager agentToolManager;
    private final ObjectMapper objectMapper;
    private final SecurityContextHolder securityContextHolder;

    private final ExecutorService sseExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, SseEmitter> sessions = new ConcurrentHashMap<>();
    /** 并发 SSE 连接数控制，防止无限连接导致 OOM */
    private final Semaphore sseSemaphore = new Semaphore(50, true);
    private static final long SSE_TIMEOUT_MS = 600_000L; // 10 分钟

    @PreDestroy
    public void shutdown() {
        log.info("[MCP:SSE] 应用关闭，清理 {} 个活跃 SSE 会话", sessions.size());
        sessions.forEach((id, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });
        sessions.clear();
        sseExecutor.shutdownNow();
    }

    /**
     * JSON-RPC 2.0 入口（HTTP POST 同步传输）
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ObjectNode handleJsonRpc(@RequestBody ObjectNode request) {
        String id = request.has("id") ? request.get("id").asText(null) : null;
        String method = request.has("method") ? request.get("method").asText() : "";
        JsonNode params = request.get("params");

        log.info("[MCP] 方法: {}, id: {}", method, id);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) response.put("id", id);

        try {
            Object result = dispatch(method, params);
            response.set("result", objectMapper.valueToTree(result));
        } catch (McpException e) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("code", e.getCode());
            error.put("message", e.getMessage());
            response.set("error", error);
        } catch (Exception e) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("code", -32603);
            error.put("message", "Internal error: " + e.getMessage());
            response.set("error", error);
        }

        return response;
    }

    /**
     * SSE 流式传输端点 — 客户端连接后通过事件流接收推送
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(@RequestParam(defaultValue = "client") String clientId) {
        // 并发连接数限制
        if (!sseSemaphore.tryAcquire()) {
            log.warn("[MCP:SSE] 连接数已满，拒绝客户端: {}", clientId);
            SseEmitter rejectEmitter = new SseEmitter(1000L);
            try {
                rejectEmitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", "服务器连接数已满，请稍后重试")));
            } catch (IOException ignored) {}
            rejectEmitter.complete();
            return rejectEmitter;
        }

        // 关闭同一 clientId 的旧连接
        SseEmitter oldEmitter = sessions.put(clientId, new SseEmitter(SSE_TIMEOUT_MS));
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception ignored) {}
            log.info("[MCP:SSE] 关闭旧连接: {}", clientId);
        }

        SseEmitter emitter = sessions.get(clientId);

        emitter.onCompletion(() -> {
            sessions.remove(clientId);
            sseSemaphore.release();
            log.info("[MCP:SSE] 客户端断开: {}", clientId);
        });
        emitter.onTimeout(() -> {
            sessions.remove(clientId);
            sseSemaphore.release();
        });
        emitter.onError(e -> {
            sessions.remove(clientId);
            sseSemaphore.release();
        });

        log.info("[MCP:SSE] 客户端连接: {}, 当前活跃: {}/50", clientId, 50 - sseSemaphore.availablePermits());
        return emitter;
    }

    /**
     * SSE 通道上的 JSON-RPC 调用（通过 query param 传方法名）
     */
    @GetMapping(value = "/sse/call", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void handleSseCall(
            @RequestParam String clientId,
            @RequestParam String method,
            @RequestParam(required = false) String argsJson) {
        SseEmitter emitter = sessions.get(clientId);
        if (emitter == null) {
            return;
        }

        sseExecutor.submit(() -> {
            try {
                JsonNode params = argsJson != null ? objectMapper.readTree(argsJson) : objectMapper.createObjectNode();
                Object result = dispatch(method, params);
                ObjectNode event = objectMapper.createObjectNode();
                event.put("jsonrpc", "2.0");
                event.put("method", method);
                event.set("result", objectMapper.valueToTree(result));
                emitter.send(SseEmitter.event().name("message").data(event.toString()));
            } catch (Exception e) {
                ObjectNode errorEvent = objectMapper.createObjectNode();
                errorEvent.put("jsonrpc", "2.0");
                errorEvent.put("method", method);
                ObjectNode err = objectMapper.createObjectNode();
                err.put("code", -32603);
                err.put("message", e.getMessage());
                errorEvent.set("error", err);
                try {
                    emitter.send(SseEmitter.event().name("message").data(errorEvent.toString()));
                } catch (IOException ignored) {
                }
            }
        });
    }

    /**
     * MCP 协议方法分发
     */
    private Object dispatch(String method, JsonNode params) throws McpException {
        return switch (method) {
            case "ping" -> Map.of("pong", true);

            case "initialize" -> Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of(
                            "tools", Map.of("listChanged", false),
                            "resources", Map.of("listChanged", false)
                    ),
                    "serverInfo", Map.of(
                            "name", "learning-planner-mcp",
                            "version", "1.0.0"
                    )
            );

            case "tools/list" -> listTools();

            case "tools/call" -> callTool(params);

            case "resources/list" -> Map.of("resources", new Object[0]);

            default -> throw new McpException(-32601, "Method not found: " + method);
        };
    }

    /**
     * 返回所有已注册工具的 MCP 格式描述
     */
    private ObjectNode listTools() {
        Map<String, String> registered = agentToolManager.getRegisteredTools();
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode toolsArray = objectMapper.createArrayNode();

        for (Map.Entry<String, String> entry : registered.entrySet()) {
            ObjectNode tool = objectMapper.createObjectNode();
            tool.put("name", entry.getKey());
            tool.put("description", entry.getValue());

            // 参数 schema（简化版，从工具描述推断）
            ObjectNode inputSchema = objectMapper.createObjectNode();
            inputSchema.put("type", "object");
            ObjectNode properties = objectMapper.createObjectNode();

            if (entry.getKey().contains("search") || entry.getKey().contains("query")) {
                properties.set("query", objectMapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "搜索关键词"));
            }
            if (entry.getKey().contains("node")) {
                properties.set("nodeId", objectMapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "节点ID"));
            }

            inputSchema.set("properties", properties);
            tool.set("inputSchema", inputSchema);
            toolsArray.add(tool);
        }

        result.set("tools", toolsArray);
        return result;
    }

    /**
     * 调用指定工具并返回 MCP 格式结果
     */
    @SuppressWarnings("deprecation")
    private ObjectNode callTool(JsonNode params) throws McpException {
        if (params == null || !params.has("name")) {
            throw new McpException(-32602, "Missing required parameter: name");
        }

        String toolName = params.get("name").asText();
        JsonNode argsNode = params.get("arguments");

        Map<String, Object> args = new HashMap<>();
        if (argsNode != null && argsNode.isObject()) {
            argsNode.fields().forEachRemaining(entry ->
                    args.put(entry.getKey(), entry.getValue().asText()));
        }

        log.info("[MCP] 调用工具: {}, 参数: {}", toolName, args);

        String userId = securityContextHolder.getCurrentUserId();
        String result = agentToolManager.execute(toolName, args, userId);

        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode textItem = objectMapper.createObjectNode();
        textItem.put("type", "text");
        textItem.put("text", result);
        content.add(textItem);
        response.set("content", content);

        return response;
    }

    /**
     * MCP 异常（JSON-RPC error）
     */
    public static class McpException extends Exception {
        private final int code;

        public McpException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}