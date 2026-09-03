package com.ai.learning.planner.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 积分余额 DTO
 * 返回用户当前积分信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsBalanceDTO {
    /** 用户ID */
    private Long userId;
    /** 可用积分 */
    private Long availablePoints;
    /** 累计获得积分 */
    private Long totalEarned;
    /** 冻结积分 */
    private Long frozenPoints;
    /** 当前连续签到天数 */
    private Integer continuousDays;
    /** 今日是否已签到 */
    private Boolean todayChecked;
}
