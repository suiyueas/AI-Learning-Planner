package com.ai.learning.planner.scheduler;

import com.ai.learning.planner.security.InputCircuitBreaker;
import com.ai.learning.planner.security.SessionRiskTracker;
import com.ai.learning.planner.security.ToolCallConfirmationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 安全定时清理调度器
 *
 * 功能说明：
 * - 定期清理过期的安全状态数据
 * - 防止内存泄漏
 * - 确保系统长期运行的稳定性
 *
 * 调度任务：
 * 1. 每天凌晨3点：清理会话风险数据（1小时无活动的数据）
 * 2. 每小时：清理熔断器状态
 * 3. 每10分钟：清理工具调用确认状态
 *
 * 注意事项：
 * - 使用内存存储时，定时清理尤为重要
 * - 生产环境建议使用Redis等分布式缓存
 * - 清理失败不应影响主业务，仅记录错误日志
 *
 * @author AI Security Team
 * @version 1.0
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SecurityCleanupScheduler {

    /**
     * 会话风险追踪器
     */
    private final SessionRiskTracker sessionRiskTracker;

    /**
     * 输入熔断器
     */
    private final InputCircuitBreaker inputCircuitBreaker;

    /**
     * 工具调用确认服务
     */
    private final ToolCallConfirmationService toolCallConfirmationService;

    /**
     * 清理过期的会话风险数据
     * 每天凌晨3点执行
     * 删除1小时无活动的会话数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredSessions() {
        log.info("[SecurityCleanupScheduler] 开始清理过期的会话风险数据");
        try {
            sessionRiskTracker.cleanupExpiredSessions();
            log.info("[SecurityCleanupScheduler] 会话风险数据清理完成");
        } catch (Exception e) {
            log.error("[SecurityCleanupScheduler] 清理会话风险数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期的熔断器状态
     * 每小时执行
     * 删除已过期的熔断器数据，防止内存泄漏
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredCircuitBreakers() {
        log.debug("[SecurityCleanupScheduler] 开始清理过期的熔断器状态");
        try {
            inputCircuitBreaker.cleanup();
            log.debug("[SecurityCleanupScheduler] 熔断器状态清理完成");
        } catch (Exception e) {
            log.error("[SecurityCleanupScheduler] 清理熔断器状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期的工具调用确认状态
     * 每10分钟执行
     * 删除已过期（5分钟）的确认令牌
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void cleanupExpiredConfirmations() {
        log.debug("[SecurityCleanupScheduler] 开始清理过期的工具调用确认状态");
        try {
            toolCallConfirmationService.removeExpiredConfirmations();
            log.debug("[SecurityCleanupScheduler] 工具调用确认状态清理完成");
        } catch (Exception e) {
            log.error("[SecurityCleanupScheduler] 清理工具调用确认状态失败: {}", e.getMessage(), e);
        }
    }
}