package com.ai.learning.planner.controller;

import com.ai.learning.planner.agent.dto.AgentInfo;
import com.ai.learning.planner.agent.dto.ReasoningLevel;
import com.ai.learning.planner.agent.dto.TaskRequest;
import com.ai.learning.planner.agent.dto.TaskResult;
import com.ai.learning.planner.agent.orchestrator.Orchestrator;
import com.ai.learning.planner.dto.AgentExecutionRequest;
import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.entity.AgentExecution;
import com.ai.learning.planner.interceptor.PointsInterceptor;
import com.ai.learning.planner.security.AuditService;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import jakarta.annotation.PreDestroy;

/**
 * 智能体控制器
 * 提供Agent管理、任务执行、流式输出等RESTful API
 */
@RestController
@RequestMapping("/agent")
@Slf4j
public class AgentController {

    private final Orchestrator orchestrator;
    private final AgentService agentService;
    private final AuditService auditService;
    private final SecurityContextHolder securityContextHolder;
    private final PointsInterceptor pointsInterceptor;
    private final Executor taskExecutor;

    /** 跟踪活跃的 SSE 连接，用于优雅关闭 */
    private final Set<SseEmitter> activeEmitters = ConcurrentHashMap.newKeySet();
    /** 并发 SSE 连接数限制 */
    private final Semaphore sseSemaphore = new Semaphore(30, true);
    private static final long SSE_TIMEOUT_MS = 300_000L; // 5 分钟

    @PreDestroy
    public void shutdown() {
        log.info("[SSE] 应用关闭，清理 {} 个活跃 Agent SSE 连接", activeEmitters.size());
        activeEmitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });
        activeEmitters.clear();
    }

    public AgentController(Orchestrator orchestrator,
                          AgentService agentService,
                          AuditService auditService,
                          SecurityContextHolder securityContextHolder,
                          PointsInterceptor pointsInterceptor,
                          Executor taskExecutor) {
        this.orchestrator = orchestrator;
        this.agentService = agentService;
        this.auditService = auditService;
        this.securityContextHolder = securityContextHolder;
        this.pointsInterceptor = pointsInterceptor;
        this.taskExecutor = taskExecutor;
        log.info("AgentController 初始化完成，使用 Spring 管理的任务执行器");
    }

    /**
     * 获取所有Agent列表
     */
    @GetMapping("/list")
    public ApiResponse<List<AgentInfo>> getAgents() {
        log.info("获取Agent列表");
        List<AgentInfo> agents = orchestrator.getAllAgents();
        return ApiResponse.success(agents);
    }

    /**
     * 获取指定Agent状态
     */
    @GetMapping("/status/{id}")
    public ApiResponse<AgentInfo> getAgentStatus(@PathVariable String id) {
        log.info("获取Agent状态: {}", id);
        AgentInfo info = orchestrator.getAgentStatus(id);
        if (info == null) {
            return ApiResponse.error("Agent不存在: " + id);
        }
        return ApiResponse.success(info);
    }

    /**
     * 同步执行任务
     */
    @PostMapping("/execute")
    public ApiResponse<TaskResult> executeTask(@Valid @RequestBody TaskRequest request) {
        log.info("执行任务: agentId={}, message={}, reasoningLevel={}",
                request.getAgentId(), request.getMessage(), request.getReasoningLevel());
        String userId = securityContextHolder.getCurrentUserId();
        long start = System.currentTimeMillis();
        try {
            // 积分检查：普通用户每次Agent调用消耗积分
            if (userId != null) {
                Long userIdLong = Long.parseLong(userId);
                pointsInterceptor.checkAndConsumeByFeature(userIdLong, "AGENT");
            }

            ReasoningLevel level = ReasoningLevel.fromValue(request.getReasoningLevel());
            TaskResult result = orchestrator.executeTask(
                    request.getAgentId(), request.getMessage(), level);
            auditService.logAgentExecution(userId, request.getAgentId(), request.getMessage(),
                    true, System.currentTimeMillis() - start, null);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("任务执行失败", e);
            auditService.logAgentExecution(userId, request.getAgentId(), request.getMessage(),
                    false, System.currentTimeMillis() - start, e.getMessage());
            return ApiResponse.error("任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 流式执行任务（SSE）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTask(
            @RequestParam String agentId,
            @RequestParam String message,
            @RequestParam(defaultValue = "standard") String reasoningLevel) {
        log.info("流式执行任务: agentId={}, message={}, reasoningLevel={}", agentId, message, reasoningLevel);

        // 并发连接数限制
        if (!sseSemaphore.tryAcquire()) {
            log.warn("[SSE] 连接数已满，拒绝请求: agentId={}", agentId);
            SseEmitter rejectEmitter = new SseEmitter(1000L);
            try {
                rejectEmitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", "服务器连接数已满，请稍后重试")));
            } catch (Exception ignored) {}
            rejectEmitter.complete();
            return rejectEmitter;
        }

        ReasoningLevel level = ReasoningLevel.fromValue(reasoningLevel);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        activeEmitters.add(emitter);

        Runnable releaseResources = () -> {
            activeEmitters.remove(emitter);
            sseSemaphore.release();
        };

        emitter.onTimeout(() -> {
            log.warn("[SSE] 流式任务超时: agentId={}", agentId);
            try {
                emitter.send(SseEmitter.event().name("timeout")
                        .data(Map.of("message", "任务执行超时")));
            } catch (Exception e) {
                log.warn("[SSE] 超时发送失败: {}", e.getMessage());
            }
            emitter.complete();
            releaseResources.run();
        });

        emitter.onError(e -> {
            log.error("[SSE] 流式任务异常: agentId={}", agentId, e);
            releaseResources.run();
        });

        emitter.onCompletion(() -> {
            log.info("[SSE] 流式任务完成: agentId={}", agentId);
            releaseResources.run();
        });

        taskExecutor.execute(() -> {
            try {
                orchestrator.executeTaskStream(agentId, message, emitter, level);
            } catch (Exception e) {
                log.error("[SSE] 流式执行异常: agentId={}", agentId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", "执行异常: " + e.getMessage())));
                } catch (Exception ex) {
                    log.warn("[SSE] 异常发送失败: {}", ex.getMessage());
                }
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 流式执行任务（POST + SSE）
     * 前端通过 fetch + ReadableStream 接收 SSE 流，支持 JSON 请求体
     */
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(@RequestBody TaskRequest request) {
        log.info("POST流式执行任务: agentId={}, message={}, reasoningLevel={}",
                request.getAgentId(), request.getMessage(), request.getReasoningLevel());

        // 并发连接数限制
        if (!sseSemaphore.tryAcquire()) {
            log.warn("[SSE] POST连接数已满，拒绝请求: agentId={}", request.getAgentId());
            SseEmitter rejectEmitter = new SseEmitter(1000L);
            try {
                rejectEmitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", "服务器连接数已满，请稍后重试")));
            } catch (Exception ignored) {}
            rejectEmitter.complete();
            return rejectEmitter;
        }

        ReasoningLevel level = ReasoningLevel.fromValue(request.getReasoningLevel());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        activeEmitters.add(emitter);

        Runnable releaseResources = () -> {
            activeEmitters.remove(emitter);
            sseSemaphore.release();
        };

        emitter.onTimeout(() -> {
            log.warn("[SSE] POST流式任务超时: agentId={}", request.getAgentId());
            emitter.complete();
            releaseResources.run();
        });

        emitter.onError(e -> {
            log.error("[SSE] POST流式任务异常: agentId={}", request.getAgentId(), e);
            releaseResources.run();
        });

        emitter.onCompletion(releaseResources::run);

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("start")
                        .data(Map.of(
                                "agentId", request.getAgentId(),
                                "message", request.getMessage(),
                                "reasoningLevel", level.getValue(),
                                "state", "RUNNING"
                        )));
                orchestrator.executeTaskStream(request.getAgentId(), request.getMessage(), emitter, level);
            } catch (Exception e) {
                log.error("[SSE] POST流式执行异常: agentId={}", request.getAgentId(), e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", "执行异常: " + e.getMessage())));
                } catch (Exception ex) {
                    log.warn("[SSE] 异常发送失败: {}", ex.getMessage());
                }
            } finally {
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }, taskExecutor);

        return emitter;
    }

    /**
     * 多Agent编排执行（SSE流式）
     * 将复杂任务拆解为多个子任务，并行分配给多个Agent，聚合结果
     */
    @PostMapping(value = "/orchestrate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter orchestrateTask(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        log.info("多Agent编排: message={}", message);

        if (!sseSemaphore.tryAcquire()) {
            log.warn("[SSE] 连接数已满，拒绝编排请求");
            SseEmitter rejectEmitter = new SseEmitter(1000L);
            try { rejectEmitter.send(SseEmitter.event().name("error").data(Map.of("message", "服务器连接数已满")));
            } catch (Exception ignored) {}
            rejectEmitter.complete();
            return rejectEmitter;
        }

        SseEmitter emitter = new SseEmitter(600_000L); // 10分钟超时
        activeEmitters.add(emitter);

        Runnable releaseResources = () -> { activeEmitters.remove(emitter); sseSemaphore.release(); };
        emitter.onTimeout(() -> { try { emitter.send(SseEmitter.event().name("timeout").data(Map.of("message", "编排超时"))); } catch (Exception ignored) {} emitter.complete(); releaseResources.run(); });
        emitter.onError(e -> releaseResources.run());
        emitter.onCompletion(releaseResources::run);

        taskExecutor.execute(() -> {
            try {
                orchestrator.executeMultiAgentStream(message, emitter);
            } catch (Exception e) {
                log.error("编排异常", e);
                try { emitter.send(SseEmitter.event().name("error").data(Map.of("message", "编排异常: " + e.getMessage()))); } catch (Exception ex) { log.warn("异常发送失败", ex); }
            } finally { emitter.complete(); }
        });

        return emitter;
    }

    /**
     * 停止Agent执行
     */
    @PostMapping("/stop/{id}")
    public ApiResponse<Void> stopAgent(@PathVariable String id) {
        log.info("停止Agent: {}", id);
        orchestrator.stopAgent(id);
        return ApiResponse.success(null);
    }

    // ==================== 数据持久化接口 ====================

    /**
     * 执行规划Agent并保存结果到数据库
     */
    @PostMapping("/plan/execute")
    public ApiResponse<AgentExecution> executePlan(@Valid @RequestBody TaskRequest request) {
        log.info("执行规划Agent: {}", request.getMessage());
        try {
            TaskResult taskResult = orchestrator.executeTask(request.getAgentId(), request.getMessage());

            // 保存执行结果到数据库
            AgentExecution result = agentService.saveResult(
                    request.getAgentId(),
                    "",
                    request.getMessage(),
                    "plan",
                    taskResult.getOutput(),
                    "",
                    null,
                    "completed"
            );

            // 保存执行日志
            if (taskResult.getSteps() != null) {
                for (int i = 0; i < taskResult.getSteps().size(); i++) {
                    Map<String, Object> step = taskResult.getSteps().get(i);
                    String role = (String) step.getOrDefault("role", "system");
                    String stepType = switch (role) {
                        case "assistant" -> "think";
                        default -> role;
                    };
                    String content = (String) step.getOrDefault("content", "");
                    if (content != null && !content.isEmpty()) {
                        agentService.saveLog(
                                request.getAgentId(),
                                request.getMessage(),
                                stepType,
                                i + 1,
                                content,
                                "success"
                        );
                    }
                }
            }

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("规划执行失败", e);
            return ApiResponse.error("规划执行失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存Agent执行日志和结果
     * 前端完成任务后调用此接口将日志和结果持久化到数据库
     */
    @PostMapping("/execution/save")
    public ApiResponse<Map<String, Object>> saveExecution(@Valid @RequestBody AgentExecutionRequest request) {
        if (request.getAgentId() == null || request.getAgentId().isBlank()) {
            return ApiResponse.error("agentId不能为空");
        }

        try {
            Map<String, Object> response = new java.util.HashMap<>();
            String savedResultId = null;
            ObjectMapper mapper = new ObjectMapper();

            // 保存执行结果
            if (request.getResult() != null) {
                String resultType = (String) request.getResult().getOrDefault("type", "default");
                String resultContent = mapper.writeValueAsString(request.getResult());
                String resultSummary = (String) request.getResult().getOrDefault("summary", "");
                AgentExecution saved = agentService.saveResult(
                        request.getAgentId(),
                        request.getAgentName() != null ? request.getAgentName() : "",
                        request.getTaskDescription() != null ? request.getTaskDescription() : "",
                        resultType,
                        resultContent,
                        resultSummary,
                        request.getDuration(),
                        request.getStatus() != null ? request.getStatus() : "completed"
                );
                savedResultId = saved.getId();
            }

            // 保存执行日志
            String outputJson = null;
            if (request.getLogs() != null && !request.getLogs().isEmpty()) {
                String finalSessionId = (request.getSessionId() == null || request.getSessionId().isEmpty())
                        ? "session_" + System.currentTimeMillis()
                        : request.getSessionId();
                String finalExecutionId = (request.getExecutionId() == null || request.getExecutionId().isEmpty())
                        ? "exec_" + System.currentTimeMillis() + "_" + request.getAgentId()
                        : request.getExecutionId();
                if (request.getResult() != null) {
                    outputJson = mapper.writeValueAsString(request.getResult());
                }
                agentService.saveExecutionLogs(
                        finalSessionId,
                        finalExecutionId,
                        request.getAgentId(),
                        request.getAgentName() != null ? request.getAgentName() : "",
                        request.getTaskDescription() != null ? request.getTaskDescription() : "",
                        request.getLogs(),
                        outputJson
                );
            }

            response.put("resultId", savedResultId);
            log.info("Agent执行数据持久化完成: agentId={}, agentName={}, resultId={}",
                    request.getAgentId(), request.getAgentName(), savedResultId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Agent执行数据持久化失败", e);
            return ApiResponse.error("数据持久化失败: " + e.getMessage());
        }
    }

    /**
     * 获取Agent执行日志
     */
    @GetMapping("/logs/{agentId}")
    public ApiResponse<List<AgentExecution>> getAgentLogs(@PathVariable String agentId) {
        log.info("获取Agent执行日志: {}", agentId);
        List<AgentExecution> logs = agentService.getLogsByAgentId(agentId);
        return ApiResponse.success(logs);
    }

    /**
     * 获取Agent执行结果列表
     */
    @GetMapping("/results/{agentId}")
    public ApiResponse<List<AgentExecution>> getAgentResults(@PathVariable String agentId) {
        log.info("获取Agent执行结果: {}", agentId);
        List<AgentExecution> results = agentService.getResultsByAgentId(agentId);
        return ApiResponse.success(results);
    }

    /**
     * 获取所有执行日志
     */
    @GetMapping("/logs")
    public ApiResponse<List<AgentExecution>> getAllLogs() {
        return ApiResponse.success(agentService.getAllLogs());
    }

    /**
     * 获取所有执行结果
     */
    @GetMapping("/results")
    public ApiResponse<List<AgentExecution>> getAllResults() {
        return ApiResponse.success(agentService.getAllResults());
    }

    /**
     * 获取Agent最新执行结果
     */
    @GetMapping("/results/latest/{agentId}")
    public ApiResponse<AgentExecution> getLatestResult(@PathVariable String agentId) {
        log.info("获取Agent最新结果: {}", agentId);
        return agentService.getLatestResultByAgentId(agentId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.notFound("暂无结果: " + agentId));
    }

    /**
     * 按 sessionId 查询执行日志
     */
    @GetMapping("/logs/session/{sessionId}")
    public ApiResponse<List<AgentExecution>> getLogsBySession(@PathVariable String sessionId) {
        return ApiResponse.success(agentService.getLogsBySessionId(sessionId));
    }

    /**
     * 按 executionId 查询执行日志
     */
    @GetMapping("/logs/execution/{executionId}")
    public ApiResponse<List<AgentExecution>> getLogsByExecution(@PathVariable String executionId) {
        return ApiResponse.success(agentService.getLogsByExecutionId(executionId));
    }

    /**
     * 按ID获取单个执行结果详情
     * 路径用单数 /result/{id}，避免与 /results/{agentId}（按Agent查结果列表）路由歧义
     */
    @GetMapping("/result/{id}")
    public ApiResponse<AgentExecution> getResultById(@PathVariable String id) {
        return agentService.getResultById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.notFound("结果不存在: " + id));
    }

    /**
     * 清空所有执行日志
     */
    @DeleteMapping("/logs")
    public ApiResponse<Void> clearAllLogs() {
        agentService.deleteAllLogs();
        log.info("所有执行日志已清空");
        return ApiResponse.success(null);
    }

    /**
     * 清空所有执行结果
     * mode=soft（默认）：软删除，可进回收站恢复；mode=hard：物理删除不可恢复
     */
    @DeleteMapping("/results")
    public ApiResponse<Void> clearAllResults(@RequestParam(defaultValue = "soft") String mode) {
        log.info("清空所有执行结果, mode={}", mode);
        if ("hard".equals(mode)) {
            agentService.hardDeleteAllResults();
        } else {
            agentService.deleteAllResults();
        }
        return ApiResponse.success(null);
    }

    /**
     * 批量删除执行结果
     * mode=soft（默认）：软删除；mode=hard：物理删除
     */
    @DeleteMapping("/results/batch")
    public ApiResponse<Map<String, Object>> deleteResultsBatch(
            @RequestParam List<String> ids,
            @RequestParam(defaultValue = "soft") String mode) {
        log.info("批量删除执行结果: ids={}, mode={}", ids, mode);
        int count;
        if ("hard".equals(mode)) {
            count = agentService.hardDeleteResults(ids);
        } else {
            count = agentService.deleteResultsByIds(ids);
        }
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("deletedCount", count);
        return ApiResponse.success(resp);
    }

    /**
     * 按ID删除单条执行结果
     * mode=soft（默认）：软删除（is_deleted=1，可恢复）；mode=hard：物理删除
     */
    @DeleteMapping("/results/{id}")
    public ApiResponse<Void> deleteResultById(@PathVariable String id, @RequestParam(defaultValue = "soft") String mode) {
        log.info("删除执行结果: id={}, mode={}", id, mode);
        boolean deleted;
        if ("hard".equals(mode)) {
            deleted = agentService.hardDeleteResult(id);
        } else {
            deleted = agentService.deleteResultById(id);
        }
        if (!deleted) {
            return ApiResponse.notFound("执行结果不存在或已删除: " + id);
        }
        return ApiResponse.success(null);
    }

    /**
     * 当前用户活动执行结果计数（与列表同数据源）
     */
    @GetMapping("/results/count")
    public ApiResponse<Map<String, Object>> countResults() {
        long count = agentService.countActiveResults();
        return ApiResponse.success(Map.of("count", count));
    }

    /**
     * 回收站列表（软删除的执行结果）
     */
    @GetMapping("/results/trash")
    public ApiResponse<List<AgentExecution>> getTrashResults() {
        return ApiResponse.success(agentService.getTrashResults());
    }

    /**
     * 恢复软删除的执行结果
     */
    @PostMapping("/results/{id}/restore")
    public ApiResponse<Void> restoreResult(@PathVariable String id) {
        boolean restored = agentService.restoreResult(id);
        if (!restored) {
            return ApiResponse.notFound("回收站中不存在该记录: " + id);
        }
        return ApiResponse.success(null);
    }
}