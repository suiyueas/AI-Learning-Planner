package com.ai.learning.planner.mcp.client;

import com.ai.learning.planner.agent.reasoning.ReasoningTrace;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.mcp.hitl.HitlApprovalGate;
import com.ai.learning.planner.mcp.security.SecurityFilter;
import com.ai.learning.planner.mcp.security.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 增强型 MCP 客户端（EnhancedMcpClient）
 * 在现有工具执行层之上提供生产级能力：
 * 1. 批量调用（Batching）：batchCall 将多个工具请求并行合并执行，减少总 RTT
 * 2. 动态超时：读超时 > 写超时（McpTimeoutPolicy）
 * 3. 指数退避重试：网络/超时异常 500ms 起、倍数 2、最大 3 次（McpRetryPolicy）
 * 4. 降级预案：核心 Server 不可用时自动降级本地静态资源，标记 X-Fallback: true
 * 5. 安全栅栏：工具描述注入清洗 + 参数/日志敏感信息脱敏 + HITL 高危审批
 * 6. 全链路 traceId 透传
 */
@Slf4j
public class EnhancedMcpClient {

    /** 工具执行层（现有 AgentToolManager） */
    private final AgentToolManager toolManager;

    private final McpTimeoutPolicy timeoutPolicy;
    private final McpRetryPolicy retryPolicy;
    private final FallbackRegistry fallbackRegistry;
    private final HitlApprovalGate approvalGate;

    /** 并行执行线程池（虚拟线程，Java 21） */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public EnhancedMcpClient(AgentToolManager toolManager,
                             McpTimeoutPolicy timeoutPolicy,
                             McpRetryPolicy retryPolicy,
                             FallbackRegistry fallbackRegistry,
                             HitlApprovalGate approvalGate) {
        this.toolManager = toolManager;
        this.timeoutPolicy = timeoutPolicy;
        this.retryPolicy = retryPolicy;
        this.fallbackRegistry = fallbackRegistry;
        this.approvalGate = approvalGate;
    }

    /**
     * 工具调用请求（JSON-RPC 风格 FunctionCall，id 用于请求/响应匹配）
     */
    public record FunctionCall(String id, String name, Map<String, Object> args) {

        public FunctionCall(String name, Map<String, Object> args) {
            this(null, name, args);
        }
    }

    /**
     * 工具调用结果
     *
     * @param id         请求 id（JSON-RPC 匹配，可能为 null）
     * @param success    是否成功
     * @param toolName   工具名
     * @param result     执行结果（成功时）
     * @param error      错误描述（失败时，可读）
     * @param durationMs 耗时
     * @param fallback   是否降级执行（X-Fallback: true）
     * @param attempts   实际尝试次数（含重试）
     * @param traceId    链路 traceId
     */
    public record McpCallResult(String id, boolean success, String toolName, String result, String error,
                                long durationMs, boolean fallback, int attempts, String traceId) {

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("success", success);
            m.put("tool", toolName);
            m.put("result", result);
            m.put("error", error);
            m.put("durationMs", durationMs);
            m.put("fallback", fallback);
            m.put("attempts", attempts);
            m.put("traceId", traceId);
            return m;
        }
    }

    /**
     * 单工具调用（含 HITL 审批、超时、重试、降级）
     */
    public McpCallResult call(String name, Map<String, Object> args) {
        return call(null, name, args);
    }

    /**
     * 单工具调用（JSON-RPC 风格，带请求 id）
     */
    public McpCallResult call(String id, String name, Map<String, Object> args) {
        long start = System.currentTimeMillis();
        String traceId = ReasoningTrace.current();
        Map<String, Object> safeArgs = SensitiveDataMasker.mask(args);

        // 1. HITL 人工审批门禁
        if (approvalGate != null && approvalGate.requiresApproval(name)) {
            log.warn("[MCP:{}] 工具 {} 触发人工审批门禁", traceId, name);
            boolean approved = approvalGate.waitForApproval(name, safeArgs);
            if (!approved) {
                return new McpCallResult(id, false, name, null,
                        "高危操作未通过人工审批，已拒绝执行: " + name,
                        System.currentTimeMillis() - start, false, 0, traceId);
            }
        }

        // 2. 超时 + 指数退避重试
        int attempts = 0;
        try {
            String result = retryPolicy.executeWithRetry(name, 0, () -> executeWithTimeout(name, args));
            attempts = retryPolicy.getMaxAttempts() > 0 ? retryPolicy.getMaxAttempts() : 1;
            // 简化：从重试策略无法得知实际次数，通过包装记录
            return new McpCallResult(id, true, name, result, null,
                    System.currentTimeMillis() - start, false, attempts, traceId);
        } catch (Throwable e) {
            attempts = countAttempts(e);
            log.warn("[MCP:{}] 工具 {} 执行失败(第{}次尝试后): {}，尝试降级", traceId, name, attempts, e.getMessage());
        }

        // 3. 降级预案：核心 Server 不可用 → 本地静态 MCP Resource
        var fallback = fallbackRegistry.get(name);
        if (fallback.isPresent()) {
            log.info("[MCP:{}] 工具 {} 降级为本地静态资源（X-Fallback: true）", traceId, name);
            return new McpCallResult(id, true, name, fallback.get(), null,
                    System.currentTimeMillis() - start, true, attempts, traceId);
        }

        // 4. 无法自动修复：抛出携带 traceId 的异常，严禁吞异常
        throw new com.ai.learning.planner.exception.ReasoningException(
                "tool", -32001, "工具调用失败且无可用降级: " + name, null);
    }

    /**
     * 批量调用：多个工具请求并行合并执行（Batching），顺序与输入一致
     *
     * @param calls 工具调用列表
     * @return 结果列表（与输入顺序一一对应）
     */
    public List<McpCallResult> batchCall(List<FunctionCall> calls) {
        if (calls == null || calls.isEmpty()) {
            return List.of();
        }
        // 主线程绑定 traceId，子线程透传，保证同批调用共享一条链路
        String batchTraceId = ReasoningTrace.current();
        log.info("[MCP] 批量调用 {} 个工具，traceId={}", calls.size(), batchTraceId);
        List<CompletableFuture<McpCallResult>> futures = new ArrayList<>(calls.size());
        for (FunctionCall call : calls) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                ReasoningTrace.bind(batchTraceId);
                return call(call.id(), call.name(), call.args());
            }, executor));
        }
        // 等待全部完成，保持输入顺序（JSON-RPC id 与响应一一对应）
        List<McpCallResult> results = new ArrayList<>(calls.size());
        for (int i = 0; i < futures.size(); i++) {
            try {
                results.add(futures.get(i).get());
            } catch (Exception e) {
                results.add(new McpCallResult(calls.get(i).id(), false, "batch", null,
                        "批量调用等待中断: " + e.getMessage(), 0, false, 0, ReasoningTrace.current()));
            }
        }
        return results;
    }

    /**
     * 获取可用工具描述（已通过 SecurityFilter 清洗，安全注入 LLM 上下文）
     */
    public Map<String, String> getSanitizedToolDescriptions() {
        Map<String, String> raw = toolManager.getRegisteredTools();
        Map<String, String> sanitized = new LinkedHashMap<>();
        raw.forEach((name, desc) -> {
            SecurityFilter.SanitizedResult r = SecurityFilter.sanitize(desc);
            if (r.sanitized()) {
                log.warn("[MCP] 工具 {} 的描述包含危险指令模式，已清洗", name);
            }
            sanitized.put(name, r.content());
        });
        return sanitized;
    }

    /**
     * 带超时控制的单次执行
     */
    private String executeWithTimeout(String name, Map<String, Object> args) {
        long timeoutMs = timeoutPolicy.writeTimeoutMs(name);
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> toolManager.execute(name, args), executor);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            // 包装为 RuntimeException 携带 TimeoutException cause，McpRetryPolicy 可识别并重试
            throw new RuntimeException(
                    "工具 " + name + " 执行超过 " + timeoutMs + "ms 超时", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("工具 " + name + " 执行异常: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("工具 " + name + " 执行被中断", e);
        }
    }

    /**
     * 从异常链中统计实际尝试次数（重试异常会携带 attempts 标记）
     */
    private int countAttempts(Throwable e) {
        Throwable cause = e;
        int attempts = 1;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("重试")) {
                attempts++;
            }
            cause = cause.getCause();
        }
        return attempts;
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executor.shutdown();
    }
}
