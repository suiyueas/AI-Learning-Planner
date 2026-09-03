package com.ai.learning.planner.scheduler;

import com.ai.learning.planner.repository.AgentExecutionRepository;
import com.ai.learning.planner.security.InputCircuitBreaker;
import com.ai.learning.planner.security.SessionRiskTracker;
import com.ai.learning.planner.security.ToolCallConfirmationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 安全定时清理调度器
 *
 * 功能说明：
 * - 定期清理过期的安全状态数据
 * - 定期归档历史 Agent 执行记录
 * - 防止内存泄漏和数据库膨胀
 *
 * @author AI Security Team
 * @version 1.1
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SecurityCleanupScheduler {

    private final SessionRiskTracker sessionRiskTracker;
    private final InputCircuitBreaker inputCircuitBreaker;
    private final ToolCallConfirmationService toolCallConfirmationService;
    private final AgentExecutionRepository agentExecutionRepository;

    /** Agent 执行记录保留天数（超过此天数的软删除记录将被物理删除） */
    private static final int ARCHIVE_RETENTION_DAYS = 30;
    /** 每批归档处理的记录数 */
    private static final int ARCHIVE_BATCH_SIZE = 500;

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

    /**
     * 归档历史 Agent 执行记录
     * 每天凌晨 4 点执行：物理删除超过 30 天的软删除记录
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void archiveAgentExecutions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(ARCHIVE_RETENTION_DAYS);
        log.info("[SecurityCleanupScheduler] 开始归档 Agent 执行记录，截止日期: {}", cutoffDate);
        try {
            int totalDeleted = 0;
            int deleted;
            do {
                deleted = agentExecutionRepository.hardDeleteArchivedBefore(cutoffDate);
                totalDeleted += deleted;
            } while (deleted >= ARCHIVE_BATCH_SIZE);

            log.info("[SecurityCleanupScheduler] Agent 执行记录归档完成，共删除 {} 条记录", totalDeleted);
        } catch (Exception e) {
            log.error("[SecurityCleanupScheduler] Agent 执行记录归档失败: {}", e.getMessage(), e);
        }
    }
}