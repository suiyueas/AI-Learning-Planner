package com.ai.learning.planner.controller;

import com.ai.learning.planner.mcp.ai.AbstractAiTool;
import com.ai.learning.planner.mcp.ai.ToolDefinitionRegistry;
import com.ai.learning.planner.repository.ToolExecutionRecordRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.ToolExecutionService;
import com.ai.learning.planner.service.ToolStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具控制器
 * 提供工具列表查询、聚合搜索、工具执行记录等功能
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Slf4j
public class ToolsController {

    private final ToolExecutionService toolExecutionService;
    private final ToolExecutionRecordRepository toolExecutionRecordRepository;
    private final ToolStatsService toolStatsService;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 获取工具列表（精简为6个核心工具，过滤隐藏的调试工具）
     */
    @GetMapping
    public Map<String, Object> getTools() {
        return Map.of("success", true, "data", getVisibleToolList());
    }

    /**
     * 聚合检索接口：同时搜索内部知识库、外部联网资源、知识图谱
     * 返回标注来源的整合结果
     */
    @PostMapping("/aggregated-search")
    public ResponseEntity<Map<String, Object>> aggregatedSearch(
            @RequestBody Map<String, Object> params) {
        try {
            String query = (String) params.getOrDefault("query", "");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "搜索关键词不能为空"));
            }
            Boolean searchInternal = (Boolean) params.getOrDefault("searchInternal", true);
            Boolean searchWeb = (Boolean) params.getOrDefault("searchWeb", true);
            Boolean searchGraph = (Boolean) params.getOrDefault("searchGraph", false);
            Integer limit = (Integer) params.getOrDefault("limit", 5);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("query", query);
            result.put("timestamp", java.time.Instant.now().toString());

            List<Map<String, Object>> sources = new java.util.ArrayList<>();

            // 1. 内部知识库检索
            if (Boolean.TRUE.equals(searchInternal)) {
                try {
                    Map<String, Object> internalResult = toolExecutionService.executeTool(
                            "search_resources",
                            Map.of("keyword", query, "limit", limit),
                            "aggregated-search"
                    );
                    if (internalResult.get("success") == Boolean.TRUE) {
                        result.put("internalResources", internalResult.get("data"));
                        sources.add(Map.of("source", "internal", "name", "内部知识库", "status", "success"));
                    } else {
                        sources.add(Map.of("source", "internal", "name", "内部知识库", "status", "failed"));
                    }
                } catch (Exception e) {
                    log.warn("内部知识库检索失败: {}", e.getMessage());
                    result.put("internalResources", Map.of("error", e.getMessage()));
                    sources.add(Map.of("source", "internal", "name", "内部知识库", "status", "error", "message", e.getMessage()));
                }
            }

            // 2. 联网搜索
            if (Boolean.TRUE.equals(searchWeb)) {
                try {
                    Map<String, Object> webResult = toolExecutionService.executeTool(
                            "web_search",
                            Map.of("query", query, "numResults", limit),
                            "aggregated-search"
                    );
                    if (webResult.get("success") == Boolean.TRUE) {
                        result.put("webResults", webResult.get("data"));
                        sources.add(Map.of("source", "web", "name", "联网搜索", "status", "success"));
                    } else {
                        sources.add(Map.of("source", "web", "name", "联网搜索", "status", "failed"));
                    }
                } catch (Exception e) {
                    log.warn("联网搜索失败: {}", e.getMessage());
                    result.put("webResults", Map.of("error", e.getMessage()));
                    sources.add(Map.of("source", "web", "name", "联网搜索", "status", "error", "message", e.getMessage()));
                }
            }

            // 3. 知识图谱查询
            if (Boolean.TRUE.equals(searchGraph)) {
                try {
                    Map<String, Object> graphResult = toolExecutionService.executeTool(
                            "query_knowledge_graph",
                            Map.of("nodeId", query, "depth", 2),
                            "aggregated-search"
                    );
                    if (graphResult.get("success") == Boolean.TRUE) {
                        result.put("knowledgeGraph", graphResult.get("data"));
                        sources.add(Map.of("source", "graph", "name", "知识图谱", "status", "success"));
                    } else {
                        sources.add(Map.of("source", "graph", "name", "知识图谱", "status", "failed"));
                    }
                } catch (Exception e) {
                    log.warn("知识图谱查询失败: {}", e.getMessage());
                    result.put("knowledgeGraph", Map.of("error", e.getMessage()));
                    sources.add(Map.of("source", "graph", "name", "知识图谱", "status", "error", "message", e.getMessage()));
                }
            }

            result.put("sources", sources);
            result.put("success", true);

            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("聚合检索异常: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "聚合检索失败: " + e.getMessage()));
        }
    }

    /**
     * 诊断端点 - 测试控制器是否正常运行
     */
    @GetMapping("/diagnose")
    public Map<String, Object> diagnose() {
        try {
            String serviceStatus = toolExecutionService != null ? "✓ 已注入" : "✗ 未注入";
            return Map.of(
                    "success", true,
                    "message", "ToolsController 正常工作",
                    "toolExecutionService", serviceStatus
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "诊断失败: " + e.getMessage());
        }
    }

    /**
     * 获取工具详情
     */
    @GetMapping("/{toolId}")
    public Map<String, Object> getToolDetail(@PathVariable String toolId) {
        try {
            var tool = findToolById(toolId);
            if (tool.isPresent()) {
                return Map.of("success", true, "data", tool.get());
            }
            return Map.of("success", false, "message", "工具不存在: " + toolId);
        } catch (Exception e) {
            log.error("获取工具详情失败: toolId={}, error={}", toolId, e.getMessage(), e);
            return Map.of("success", false, "message", "获取工具详情失败: " + e.getMessage());
        }
    }

    /**
     * 执行工具 - 调用真实执行逻辑
     */
    @PostMapping("/{toolId}/execute")
    public ResponseEntity<Map<String, Object>> executeTool(@PathVariable String toolId,
                                                           @RequestBody(required = false) Map<String, Object> params,
                                                           Authentication authentication) {
        try {
            var tool = findToolById(toolId);
            if (tool.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "工具不存在: " + toolId));
            }

            String userId = authentication != null ? String.valueOf(authentication.getPrincipal()) : "anonymous";
            log.info("执行工具: toolId={}, userId={}, params={}", toolId, userId, params);

            if (params == null) {
                params = new HashMap<>();
            }

            Map<String, Object> result = toolExecutionService.executeTool(toolId, params, userId);
            boolean isSuccess = Boolean.TRUE.equals(result.get("success"));
            if (isSuccess) {
                return ResponseEntity.ok(Map.of("success", true, "data", result));
            } else {
                // 工具执行返回了业务错误（非异常），返回200但标记success=false
                String message = (String) result.getOrDefault("message", "工具执行失败");
                return ResponseEntity.ok(Map.of("success", false, "message", message, "data", result));
            }
        } catch (IllegalArgumentException e) {
            log.warn("工具参数错误: toolId={}, error={}", toolId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage(), "type", e.getClass().getSimpleName()));
        } catch (HttpMessageNotReadableException e) {
            log.warn("请求体解析失败: toolId={}, error={}", toolId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "请求参数格式错误", "type", e.getClass().getSimpleName()));
        } catch (Exception e) {
            log.error("工具执行异常: toolId={}, error={}", toolId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "服务器内部错误: " + e.getMessage(),
                            "type", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * 获取工具执行历史（分页）
     */
    @GetMapping("/executions/history")
    public Map<String, Object> getExecutionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String userId = authentication != null ? String.valueOf(authentication.getPrincipal()) : "anonymous";
        var history = toolExecutionService.getExecutionHistory(page, size, userId);
        return Map.of("success", true, "data", history);
    }

    /**
     * 删除单条工具执行记录（物理删除，仅限当前用户自己的记录）
     */
    @DeleteMapping("/executions/{id}")
    public Map<String, Object> deleteExecutionRecord(@PathVariable Long id) {
        boolean deleted = toolExecutionService.deleteRecord(id);
        if (!deleted) {
            return Map.of("success", false, "message", "执行记录不存在或无权删除: " + id);
        }
        return Map.of("success", true, "message", "执行记录已删除");
    }

    /**
     * 清空当前用户的工具执行记录（物理删除）
     */
    @DeleteMapping("/executions")
    public Map<String, Object> clearExecutionRecords() {
        // 清空所有用户的执行记录为管理员专属操作，防止越权破坏
        if (!securityContextHolder.isAdmin()) {
            throw new AccessDeniedException("仅管理员可清空执行记录");
        }
        int count = toolExecutionService.clearRecords();
        return Map.of("success", true, "message", "已清空 " + count + " 条执行记录");
    }

    /**
     * 获取工具统计（新：基于 tool_call_stats 表的每工具真实统计）
     * 返回格式: { toolId: { totalCalls, sessionCalls, lastCalled } }
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getToolStats() {
        Map<String, Map<String, Object>> stats = toolStatsService.getAllStats();
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    /**
     * 获取单个工具详细统计
     */
    @GetMapping("/{toolId}/stats")
    public ResponseEntity<Map<String, Object>> getSingleToolStats(@PathVariable String toolId) {
        Map<String, Object> stats = toolStatsService.getToolStats(toolId);
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    /**
     * 获取当前用户的工具调用统计（按用户隔离）
     * 返回格式: { toolId: { totalCalls, sessionCalls, lastCalled } }
     */
    @GetMapping("/my-stats")
    public ResponseEntity<Map<String, Object>> getMyToolStats() {
        Map<String, Map<String, Object>> stats = toolStatsService.getUserStats();
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    /**
     * 获取当前用户单个工具的调用次数（按用户隔离）
     */
    @GetMapping("/{toolId}/my-stats")
    public ResponseEntity<Map<String, Object>> getMyToolUsageCount(@PathVariable String toolId) {
        long count = toolStatsService.getUserToolUsageCount(toolId);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
                "toolId", toolId,
                "totalCalls", count
        )));
    }

    /**
     * 记录一次工具调用（每次工具执行成功后调用）
     * 仅接受注册表中的真实工具 ID，防止伪造 toolId 刷全局统计
     */
    @PostMapping("/{toolId}/record")
    public ResponseEntity<Map<String, Object>> recordToolCall(@PathVariable String toolId) {
        if (ToolDefinitionRegistry.byId(toolId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无效的工具ID: " + toolId
            ));
        }
        int totalCalls = toolStatsService.recordCall(toolId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "totalCalls", totalCalls
        ));
    }

    /**
     * 获取工具状态（给对话界面使用）
     */
    @GetMapping("/status")
    public Map<String, Object> getToolStatus() {
        // 可用工具数按前端可见列表统计（排除隐藏/已合并工具），与 GET /tools 展示保持一致
        long available = getVisibleToolList().stream()
                .filter(t -> "available".equals(t.get("status")))
                .count();
        long totalCalls = toolExecutionRecordRepository.count();
        // 降级触发次数（AI 工具层降级方案触发统计，用于降级触发率监控）
        long fallbackCount = AbstractAiTool.getFallbackTotalCount();
        return Map.of("success", true, "data", Map.of(
                "availableCount", available,
                "totalCalls", totalCalls,
                "fallbackCount", fallbackCount
        ));
    }

    /**
     * 获取工具分类（与统一注册表保持一致）
     */
    @GetMapping("/categories")
    public Map<String, Object> getToolCategories() {
        return Map.of("success", true, "data", ToolDefinitionRegistry.categories());
    }

    /**
     * 搜索工具
     */
    @GetMapping("/search")
    public Map<String, Object> searchTools(@RequestParam String keyword) {
        String lower = keyword.toLowerCase();
        var results = getToolList().stream()
                .filter(t -> {
                    String name = (String) t.getOrDefault("name", "");
                    String desc = (String) t.getOrDefault("description", "");
                    return name.toLowerCase().contains(lower) || desc.toLowerCase().contains(lower);
                })
                .toList();
        return Map.of("success", true, "data", results);
    }

    /**
     * 工具列表（由统一注册表 ToolDefinitionRegistry 生成）
     * 数据库查询失败时使用默认值0，确保控制器不因数据库异常而崩溃
     */
    private List<Map<String, Object>> getToolList() {
        return ToolDefinitionRegistry.all().stream()
                .map(def -> ToolDefinitionRegistry.toFrontendMap(def, safeCountByToolId(def.id())))
                .collect(Collectors.toList());
    }

    /**
     * 前端可见工具列表（过滤隐藏的调试工具，仅返回 isHidden=false 的工具）
     */
    private List<Map<String, Object>> getVisibleToolList() {
        return ToolDefinitionRegistry.visibleTools().stream()
                .map(def -> ToolDefinitionRegistry.toFrontendMap(def, safeCountByToolId(def.id())))
                .collect(Collectors.toList());
    }

    private Optional<Map<String, Object>> findToolById(String toolId) {
        return getToolList().stream()
                .filter(t -> toolId.equals(t.get("id")))
                .findFirst();
    }

    /**
     * 安全查询工具调用次数，数据库不可用时返回 0 而非抛出异常
     */
    private long safeCountByToolId(String toolId) {
        try {
            return toolExecutionRecordRepository.countByToolId(toolId);
        } catch (Exception e) {
            log.warn("查询工具调用次数失败(toolId={}): {}", toolId, e.getMessage());
            return 0L;
        }
    }
}