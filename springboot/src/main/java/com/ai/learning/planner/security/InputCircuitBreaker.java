package com.ai.learning.planner.security;

import com.ai.learning.planner.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 输入熔断器 - 第三层防御（输入熔断机制）
 *
 * 功能说明：
 * - 防止高频攻击（用户在短时间内发送大量请求）
 * - 防止高危恶意输入攻击（在短时间内发送多次高风险输入）
 * - 在检测到异常行为时触发熔断，暂时阻断用户请求
 *
 * 熔断机制：
 * 1. 请求频率限制：每分钟最多 maxRequestsPerMinute 次请求
 * 2. 高风险输入限制：每分钟最多 maxHighRiskPerMinute 次高风险输入
 * 3. 熔断器触发：当用户在 1 分钟内被拦截 openThreshold 次，打开熔断器
 * 4. 熔断恢复：resetMinutes 分钟后自动恢复
 *
 * 所有阈值均可通过 application.yml 中 security.circuit-breaker.* 配置调整。
 *
 * 处置策略：
 * - RATE_LIMIT: 请求频率超限，返回"请求过于频繁"
 * - BLOCK: 高风险输入超限或直接拦截，返回"检测到异常行为"
 * - CIRCUIT_OPEN: 熔断器打开，返回"系统繁忙"
 *
 * 使用方式：
 * <pre>
 *     CircuitBreakerResult result = circuitBreaker.check(userId, userInput);
 *     if (result.blocked()) {
 *         return result.message();
 *     }
 * </pre>
 *
 * @author AI Security Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InputCircuitBreaker {

    /**
     * 安全配置（阈值外部化，对应 security.circuit-breaker.*）
     */
    private final SecurityProperties securityProperties;

    /**
     * 输入安全过滤器依赖
     */
    private final InputSanitizer inputSanitizer;

    /**
     * 安全审计服务（拦截/熔断时记录审计日志）
     */
    private final AuditService auditService;

    /**
     * 用户请求数据存储
     * key: userId, value: UserRequestData
     */
    private final Map<String, UserRequestData> userRequestCounts = new ConcurrentHashMap<>();

    /**
     * 熔断器状态存储
     * key: userId, value: CircuitState
     */
    private final Map<String, CircuitState> circuitBreakers = new ConcurrentHashMap<>();

    // ==================== 核心检测方法 ====================

    /**
     * 检查用户输入是否需要被拦截
     *
     * @param userId 用户ID
     * @param input 用户输入
     * @return CircuitBreakerResult 包含是否拦截、处置动作、返回消息
     */
    public CircuitBreakerResult check(String userId, String input) {
        // 匿名用户使用固定标识
        if (userId == null) {
            userId = "anonymous";
        }

        // 获取或创建用户请求数据
        UserRequestData requestData = userRequestCounts.computeIfAbsent(
                userId,
                k -> new UserRequestData()
        );

        // 1. 检查请求频率
        requestData.incrementTotalRequests();
        if (requestData.getTotalRequests() > securityProperties.getCircuitBreaker().getMaxRequestsPerMinute()) {
            log.warn("[InputCircuitBreaker] 用户请求频率超限, userId={}, count={}",
                    userId, requestData.getTotalRequests());
            auditService.logBlockedAttempt(userId, "RATE_LIMIT", "请求过于频繁", input);
            return new CircuitBreakerResult(
                    true,
                    CircuitBreakerAction.RATE_LIMIT,
                    "请求过于频繁，请稍后再试"
            );
        }

        // 2. 检查高风险输入频率
        InputSanitizer.RiskLevel riskLevel = inputSanitizer.classifyRisk(input);
        if (riskLevel == InputSanitizer.RiskLevel.HIGH) {
            requestData.incrementHighRiskCount();
            if (requestData.getHighRiskCount() > securityProperties.getCircuitBreaker().getMaxHighRiskPerMinute()) {
                log.warn("[InputCircuitBreaker] 用户高风险输入超限, userId={}, count={}",
                        userId, requestData.getHighRiskCount());
                auditService.logBlockedAttempt(userId, "HIGH_RISK_LIMIT", "高风险输入超限", input);
                return new CircuitBreakerResult(
                        true,
                        CircuitBreakerAction.BLOCK,
                        "检测到异常行为，请稍后重试"
                );
            }
        }

        // 3. 检查熔断器状态
        CircuitState state = circuitBreakers.get(userId);
        if (state != null && state.isOpen()) {
            if (state.shouldReset()) {
                circuitBreakers.remove(userId);
                log.info("[InputCircuitBreaker] 熔断器已恢复, userId={}", userId);
            } else {
                log.warn("[InputCircuitBreaker] 熔断器打开中, userId={}, resetAt={}",
                        userId, state.resetAt());
                auditService.logBlockedAttempt(userId, "CIRCUIT_OPEN", "熔断器打开", input);
                return new CircuitBreakerResult(
                        true,
                        CircuitBreakerAction.CIRCUIT_OPEN,
                        "系统繁忙，请稍后重试"
                );
            }
        }

        // 4. 检查是否被输入过滤器直接拦截
        if (inputSanitizer.isBlocked(input)) {
            incrementCircuitBreaker(userId);
            auditService.logBlockedAttempt(userId, "INPUT_BLOCKED", "严重恶意指令", input);
            return new CircuitBreakerResult(
                    true,
                    CircuitBreakerAction.BLOCK,
                    "输入包含不允许的内容"
            );
        }

        // 允许通过
        return new CircuitBreakerResult(false, null, null);
    }

    /**
     * 增加熔断器失败计数
     */
    private void incrementCircuitBreaker(String userId) {
        CircuitState state = circuitBreakers.computeIfAbsent(
                userId,
                k -> new CircuitState(0, null)
        );

        int count = state.failureCount() + 1;
        if (count >= securityProperties.getCircuitBreaker().getOpenThreshold()) {
            // 达到阈值，打开熔断器
            circuitBreakers.put(userId, new CircuitState(count, Instant.now().plusSeconds(securityProperties.getCircuitBreaker().getResetMinutes() * 60L)));
            log.warn("[InputCircuitBreaker] 熔断器已打开, userId={}, failureCount={}", userId, count);
        } else {
            circuitBreakers.put(userId, new CircuitState(count, state.resetAt()));
        }
    }

    /**
     * 重置用户的所有状态
     * 用于用户主动申请解封或管理员手动解封
     *
     * @param userId 用户ID
     */
    public void resetUserState(String userId) {
        userRequestCounts.remove(userId);
        circuitBreakers.remove(userId);
        log.info("[InputCircuitBreaker] 用户状态已重置, userId={}", userId);
    }

    /**
     * 清理过期的用户状态数据
     * 建议通过定时任务定期调用
     */
    public void cleanup() {
        Instant threshold = Instant.now().minusSeconds(securityProperties.getCircuitBreaker().getCleanupAfterSeconds());
        userRequestCounts.entrySet().removeIf(entry ->
                entry.getValue().lastRequest().isBefore(threshold));

        circuitBreakers.entrySet().removeIf(entry ->
                entry.getValue().resetAt() != null && entry.getValue().resetAt().isBefore(Instant.now()));
    }

    // ==================== 结果与状态类 ====================

    /**
     * 熔断器检查结果
     *
     * @param blocked 是否被拦截
     * @param action 处置动作（可为null）
     * @param message 返回给用户的消息（可为null）
     */
    public record CircuitBreakerResult(
            boolean blocked,
            CircuitBreakerAction action,
            String message
    ) {
    }

    /**
     * 处置动作枚举
     */
    public enum CircuitBreakerAction {
        RATE_LIMIT,    // 频率限制
        BLOCK,         // 直接拦截
        CIRCUIT_OPEN   // 熔断器打开
    }

    /**
     * 熔断器状态
     *
     * @param failureCount 连续失败次数
     * @param resetAt 恢复时间（为null表示未打开）
     */
    public record CircuitState(
            int failureCount,
            Instant resetAt
    ) {
        /**
         * 熔断器是否打开
         */
        public boolean isOpen() {
            return resetAt != null && resetAt.isAfter(Instant.now());
        }

        /**
         * 是否应该重置
         */
        public boolean shouldReset() {
            return resetAt != null && resetAt.isBefore(Instant.now());
        }
    }

    /**
     * 用户请求数据
     * 记录用户的请求频率和高风险输入次数
     */
    public static class UserRequestData {
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger highRiskCount = new AtomicInteger(0);
        private volatile Instant lastRequest = Instant.now();

        public void incrementTotalRequests() {
            totalRequests.incrementAndGet();
            lastRequest = Instant.now();
        }

        public void incrementHighRiskCount() {
            highRiskCount.incrementAndGet();
        }

        public int getTotalRequests() {
            return totalRequests.get();
        }

        public int getHighRiskCount() {
            return highRiskCount.get();
        }

        public Instant lastRequest() {
            return lastRequest;
        }
    }
}