package com.ai.learning.planner.security;

import com.ai.learning.planner.config.SecurityProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话风险追踪器 - 多轮对话防护
 *
 * 功能说明：
 * - 追踪每个会话的风险累积
 * - 检测渐进式越狱攻击模式
 * - 在风险达到阈值时触发安全干预
 *
 * 风险累积机制：
 * - 每个会话独立追踪，互不影响
 * - 风险分数根据每次输入的风险等级累加
 * - HIGH输入: +highRiskScore（默认0.4）, MEDIUM输入: +mediumRiskScore（默认0.2）, LOW输入: +0.0分
 * - 每 resetAfterTurns（默认10）轮对话自动重置风险分数，防止误伤正常长对话
 *
 * 干预触发条件：
 * - 风险分数达到阈值（默认0.7）时标记为 escalated
 * - 触发后应进行人工复核或安全代答
 * - 最大对话轮次限制（默认50轮），超过建议重置会话
 *
 * 适用场景：
 * - 防止多轮渐进式攻击（先闲聊建立信任，再逐步引导至敏感话题）
 * - 识别异常长的对话会话
 * - 为人工复核提供依据
 *
 * 所有阈值均可通过 application.yml 中 security.session.* 配置调整。
 *
 * @author AI Security Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionRiskTracker {

    /**
     * 安全配置（阈值外部化，对应 security.session.*）
     */
    private final SecurityProperties securityProperties;

    /**
     * 输入安全过滤器依赖
     */
    private final InputSanitizer inputSanitizer;

    /**
     * 会话风险数据存储
     * key: sessionId, value: SessionRiskData
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, SessionRiskData> sessionRisks = new ConcurrentHashMap<>();

    // ==================== 核心追踪方法 ====================

    /**
     * 追踪单轮对话的风险
     * 在每次用户输入时调用
     *
     * @param sessionId 会话ID
     * @param userInput 用户输入内容
     *
     * 处理流程：
     * 1. 获取或创建会话风险数据
     * 2. 调用输入过滤器评估风险等级
     * 3. 根据风险等级累加风险分数
     * 4. 检查是否需要重置风险分数
     * 5. 检查是否达到干预阈值
     */
    public void track(String sessionId, String userInput) {
        if (sessionId == null || userInput == null || userInput.isBlank()) {
            return;
        }

        // 获取或创建会话风险数据
        SessionRiskData riskData = sessionRisks.computeIfAbsent(
                sessionId,
                k -> new SessionRiskData(sessionId, 0, 0.0, Instant.now(), 0, false)
        );

        // 增加轮次计数
        riskData.incrementTurnCount();

        // 评估输入风险等级并转换为分数
        InputSanitizer.RiskLevel riskLevel = inputSanitizer.classifyRisk(userInput);
        double riskScore = switch (riskLevel) {
            case HIGH -> securityProperties.getSession().getHighRiskScore();   // 高风险输入
            case MEDIUM -> securityProperties.getSession().getMediumRiskScore(); // 中风险输入
            case LOW -> 0.0;     // 低风险输入
        };

        // 累加风险分数
        riskData.addRiskScore(riskScore);

        // 每N轮重置风险分数（防止误伤正常长对话）
        if (riskData.getTurnCount() % securityProperties.getSession().getResetAfterTurns() == 0) {
            riskData.resetRiskScore();
            log.debug("[SessionRiskTracker] 会话风险分数已重置, sessionId={}", sessionId);
        }

        // 检查是否达到干预阈值
        if (riskData.getRiskScore() >= securityProperties.getSession().getRiskThreshold()) {
            riskData.setEscalated(true);
            log.warn("[SessionRiskTracker] 会话风险等级已升级, sessionId={}, riskScore={}",
                    sessionId, riskData.getRiskScore());
        }

        // 检查对话轮次是否过多
        if (riskData.getTurnCount() > securityProperties.getSession().getMaxTurns()) {
            log.warn("[SessionRiskTracker] 会话轮次过多，建议重置, sessionId={}, turns={}",
                    sessionId, riskData.getTurnCount());
        }
    }

    /**
     * 检查会话是否已被标记为需要干预
     *
     * @param sessionId 会话ID
     * @return true 如果风险等级已升级需要干预
     */
    public boolean isEscalated(String sessionId) {
        SessionRiskData riskData = sessionRisks.get(sessionId);
        return riskData != null && riskData.isEscalated();
    }

    /**
     * 获取会话当前风险分数
     *
     * @param sessionId 会话ID
     * @return 风险分数（0.0-1.0）
     */
    public double getRiskScore(String sessionId) {
        SessionRiskData riskData = sessionRisks.get(sessionId);
        return riskData != null ? riskData.getRiskScore() : 0.0;
    }

    /**
     * 获取会话当前轮次计数
     *
     * @param sessionId 会话ID
     * @return 轮次计数
     */
    public int getTurnCount(String sessionId) {
        SessionRiskData riskData = sessionRisks.get(sessionId);
        return riskData != null ? riskData.getTurnCount() : 0;
    }

    /**
     * 重置会话风险追踪
     * 用于会话结束或用户主动重置时调用
     *
     * @param sessionId 会话ID
     */
    public void resetSession(String sessionId) {
        sessionRisks.remove(sessionId);
        log.info("[SessionRiskTracker] 会话风险追踪已重置, sessionId={}", sessionId);
    }

    /**
     * 获取会话完整风险数据
     *
     * @param sessionId 会话ID
     * @return SessionRiskData 或 null（如果不存在）
     */
    public SessionRiskData getSessionRiskData(String sessionId) {
        return sessionRisks.get(sessionId);
    }

    /**
     * 清理过期会话数据
     * 删除1小时无活动的会话数据，防止内存泄漏
     * 建议通过定时任务定期调用
     */
    public void cleanupExpiredSessions() {
        Instant threshold = Instant.now().minusSeconds(securityProperties.getSession().getSessionTimeout());
        int before = sessionRisks.size();
        sessionRisks.entrySet().removeIf(entry ->
                entry.getValue().getLastActivity().isBefore(threshold));
        int after = sessionRisks.size();
        if (before > after) {
            log.info("[SessionRiskTracker] 清理过期会话: before={}, after={}, removed={}", before, after, before - after);
        }
    }

    // ==================== 会话风险数据结构 ====================

    /**
     * 会话风险数据
     * 记录单个会话的风险累积状态
     *
     * @param sessionId 会话ID
     * @param turnCount 对话轮次计数
     * @param riskScore 风险分数（0.0-1.0）
     * @param lastActivity 最后活动时间
     * @param consecutiveHighRiskTurns 连续高风险轮次
     * @param escalated 是否已升级需要干预
     */
    @Getter
    @AllArgsConstructor
    public static class SessionRiskData {
        private final String sessionId;
        private int turnCount;
        private double riskScore;
        private Instant lastActivity;
        private int consecutiveHighRiskTurns;
        private boolean escalated;

        /**
         * 增加轮次计数并更新活动时间
         */
        public void incrementTurnCount() {
            this.turnCount++;
            this.lastActivity = Instant.now();
        }

        /**
         * 累加风险分数
         * 分数上限为1.0，超过阈值会触发连续高风险计数
         */
        public void addRiskScore(double score) {
            this.riskScore = Math.min(1.0, this.riskScore + score);
            // 如果单次分数>=0.3，认为是高风险轮次
            if (score >= 0.3) {
                this.consecutiveHighRiskTurns++;
            } else {
                this.consecutiveHighRiskTurns = 0;
            }
        }

        /**
         * 重置风险分数
         * 用于周期性重置，防止误伤正常长对话
         */
        public void resetRiskScore() {
            this.riskScore = 0.0;
            this.consecutiveHighRiskTurns = 0;
        }

        /**
         * 设置干预标记
         */
        public void setEscalated(boolean escalated) {
            this.escalated = escalated;
        }
    }
}