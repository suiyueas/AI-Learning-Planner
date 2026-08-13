package com.ai.learning.planner.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户学习偏好请求/响应 DTO
 * 对应 AdaptiveDetail.vue 中的偏好设置
 */
@Data
@Builder
public class UserPreferencesDTO {
    private Long userId;
    private String learningStyle;   // 学习风格: visual/auditory/reading/kinesthetic
    private String difficulty;      // 难度偏好: gradual/moderate/challenge
    private Integer dailyHours;     // 每日学习时长
    private List<String> timeSlots; // 偏好时间段: morning/afternoon/evening/night
    private String targetField;     // 目标领域
    private String weakPoints;      // 薄弱知识点（JSON数组字符串）

    // ===== 智能干预阈值（智能通知中心专用） =====
    private Boolean interventionEnabled;              // 干预提醒总开关
    private Float interventionProgressThreshold;      // 进度提醒阈值（%）：完成率低于该值生成预警
    private Float interventionScoreDeclineThreshold;  // 测评降幅阈值（%）：降幅超过该值生成预警
    private Integer interventionInactiveDays;         // 连续未登录预警天数
}