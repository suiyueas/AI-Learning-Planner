package com.ai.learning.planner.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 签到结果 DTO
 * 返回签到后的结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResultDTO {
    /** 本次签到获得的基础积分 */
    private Long basePoints;
    /** 连续签到奖励积分（0表示未触发） */
    private Long bonusPoints;
    /** 本次签到总获得积分 */
    private Long totalPoints;
    /** 签到后的连续天数 */
    private Integer continuousDays;
    /** 签到后的总天数 */
    private Integer totalDays;
    /** 签到后的可用积分 */
    private Long availablePoints;
    /** 是否触发连续奖励 */
    private Boolean bonusTriggered;
    /** 签到日期 */
    private String checkinDate;
    /** 签到成功消息 */
    private String message;
    /** 今日是否已签到 */
    private Boolean todayChecked;
    /** 本月签到日期列表 */
    private java.util.List<java.time.LocalDate> monthDays;
    /** 本次打卡新解锁的成就列表 */
    private java.util.List<java.util.Map<String, Object>> newlyUnlockedAchievements;
}