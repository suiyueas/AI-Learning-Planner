package com.ai.learning.planner.agent.reasoning;

import lombok.extern.slf4j.Slf4j;

/**
 * 动态重规划触发器
 * 当工具调用连续失败 2 次，或上下文使用率超过窗口阈值时，
 * 强制中断当前路径，触发 Planner.generateAlternativePath 生成绕行计划
 */
@Slf4j
public class ReplanningTrigger {

    /** 连续失败触发阈值 */
    public static final int CONSECUTIVE_FAILURE_THRESHOLD = 2;

    /** 上下文使用率阈值（超出即强制重规划） */
    public static final double CONTEXT_RATIO_THRESHOLD = 0.85;

    private int consecutiveFailures = 0;

    private final double contextRatioThreshold;

    public ReplanningTrigger() {
        this(CONTEXT_RATIO_THRESHOLD);
    }

    public ReplanningTrigger(double contextRatioThreshold) {
        this.contextRatioThreshold = contextRatioThreshold;
    }

    /**
     * 记录一次工具调用结果
     */
    public void recordToolResult(boolean success) {
        consecutiveFailures = success ? 0 : consecutiveFailures + 1;
    }

    /**
     * 检查是否需要重规划（工具连续失败 ≥2 次）
     */
    public boolean shouldReplanByFailure() {
        return consecutiveFailures >= CONSECUTIVE_FAILURE_THRESHOLD;
    }

    /**
     * 检查是否需要重规划（上下文使用率超阈值）
     */
    public boolean shouldReplanByContext(double contextUsageRatio) {
        return contextUsageRatio > contextRatioThreshold;
    }

    /**
     * 综合检查
     */
    public boolean shouldReplan(double contextUsageRatio) {
        return shouldReplanByFailure() || shouldReplanByContext(contextUsageRatio);
    }

    /**
     * 获取当前连续失败次数
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * 重规划后重置状态
     */
    public void reset() {
        consecutiveFailures = 0;
        log.info("[ReplanningTrigger] 重规划状态已重置");
    }
}
