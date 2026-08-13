package com.ai.learning.planner.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户资料响应 DTO
 * 返回用户基本信息、角色、学习目标与注册时间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String learningGoal;
    private String bio;
    private LocalDateTime createdAt;

    // 学习统计
    private LearningStats learningStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningStats {
        private long totalLearningHours;
        private int completedNodes;
        private int achievementCount;
        private int continuousDays;
        private double averageScore;
        private float weeklyProgress;
        private float progress;
        private float totalTasks;
    }
}
