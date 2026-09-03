package com.ai.learning.planner.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 积分配置更新请求 DTO（管理员使用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsConfigRequest {
    /** 每日签到基础积分 */
    private Long dailyCheckinPoints;
    /** 连续签到奖励周期（天数） */
    private Integer consecutiveDays;
    /** 连续签到奖励积分 */
    private Long consecutiveBonusPoints;
    /** AI对话消耗积分 */
    private Long chatConsumePoints;
    /** 智能体调用消耗积分 */
    private Long agentConsumePoints;
    /** 学习路径生成消耗积分 */
    private Long learningPathConsumePoints;
}
