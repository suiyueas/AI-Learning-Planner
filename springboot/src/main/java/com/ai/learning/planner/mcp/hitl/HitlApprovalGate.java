package com.ai.learning.planner.mcp.hitl;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 人工审批门禁（Human-In-The-Loop）
 * 涉及 write/delete/exec 高危操作的工具，必须在调用前触发 wait_for_approval 钩子，
 * 挂起推理链直至外部信号（approve/deny）唤醒。
 */
@Slf4j
public class HitlApprovalGate {

    /** 高危工具名前缀 */
    private static final Set<String> HIGH_RISK_PREFIXES = Set.of("write", "delete", "exec", "remove", "drop", "truncate");

    /** 显式注册的高危工具（精确匹配） */
    private final Set<String> highRiskTools = ConcurrentHashMap.newKeySet();

    /** 挂起的审批请求（requestId -> future） */
    private final Map<String, CompletableFuture<Boolean>> pendingApprovals = new ConcurrentHashMap<>();

    /** 默认审批等待超时 */
    private static final Duration DEFAULT_APPROVAL_TIMEOUT = Duration.ofMinutes(2);

    /** 当前生效的审批等待超时（可配置，便于测试/运维调整） */
    private volatile Duration defaultTimeout = DEFAULT_APPROVAL_TIMEOUT;

    /**
     * 显式注册高危工具
     */
    public void registerHighRiskTool(String toolName) {
        highRiskTools.add(toolName);
    }

    /**
     * 判断工具是否需要人工审批
     */
    public boolean requiresApproval(String toolName) {
        if (toolName == null || toolName.isBlank()) return false;
        if (highRiskTools.contains(toolName)) return true;
        String lower = toolName.toLowerCase();
        return HIGH_RISK_PREFIXES.stream().anyMatch(lower::startsWith);
    }

    /**
     * 发起审批请求并挂起等待（默认超时 2 分钟）
     *
     * @param toolName 工具名
     * @param args     参数（用于审批展示）
     * @return 审批通过返回 true；拒绝/超时返回 false
     */
    public boolean waitForApproval(String toolName, Map<String, Object> args) {
        return waitForApproval(toolName, args, defaultTimeout);
    }

    /**
     * 配置审批等待超时（默认 2 分钟）
     */
    public void setDefaultTimeout(Duration timeout) {
        this.defaultTimeout = timeout;
    }

    /**
     * 发起审批请求并挂起等待
     */
    public boolean waitForApproval(String toolName, Map<String, Object> args, Duration timeout) {
        String requestId = "apr-" + System.nanoTime();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingApprovals.put(requestId, future);
        log.warn("[HITL] 高危工具需要人工审批: {}，请求ID: {}，参数: {}", toolName, requestId, args);

        try {
            boolean approved = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            log.info("[HITL] 审批结果: {} -> {}", requestId, approved ? "通过" : "拒绝");
            return approved;
        } catch (Exception e) {
            log.warn("[HITL] 审批等待超时/中断: {}，默认拒绝执行", requestId);
            future.complete(false);
            return false;
        } finally {
            pendingApprovals.remove(requestId);
        }
    }

    /**
     * 外部信号：批准指定请求
     */
    public boolean approve(String requestId) {
        CompletableFuture<Boolean> future = pendingApprovals.get(requestId);
        if (future == null) return false;
        return future.complete(true);
    }

    /**
     * 外部信号：拒绝指定请求
     */
    public boolean deny(String requestId) {
        CompletableFuture<Boolean> future = pendingApprovals.get(requestId);
        if (future == null) return false;
        return future.complete(false);
    }

    /**
     * 获取当前挂起的审批请求数
     */
    public int pendingCount() {
        return pendingApprovals.size();
    }

    /**
     * 获取当前挂起的审批请求 ID 列表
     */
    public Set<String> pendingRequests() {
        return Set.copyOf(pendingApprovals.keySet());
    }

    /**
     * 拒绝所有挂起的审批（Agent 终止时调用，防止线程泄漏）
     */
    public void denyAll() {
        pendingApprovals.forEach((id, future) -> future.complete(false));
        pendingApprovals.clear();
    }
}
