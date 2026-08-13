package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库中的users表，包含用户基本信息和学习相关配置
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** 用户角色：USER（普通用户）/ ADMIN（管理员） */
    @Column(length = 20)
    @Builder.Default
    private String role = "USER";

    @Column(length = 50)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "learning_goal", length = 255)
    private String learningGoal;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // ===== 用户配置字段（原 user_profiles，已合并到 users） =====

    @Column(name = "learning_style", length = 50)
    private String learningStyle;

    @Builder.Default
    private Integer level = 1;

    @Column(name = "active_hours", length = 100)
    private String activeHours;

    @Column(name = "target_field", length = 255)
    private String targetField;

    @Column(columnDefinition = "TEXT")
    private String interests;

    @Column(name = "weak_points", columnDefinition = "TEXT")
    private String weakPoints;

    @Column(name = "continuous_checkin_days")
    @Builder.Default
    private Integer continuousCheckinDays = 0;

    @Column(name = "total_checkin_days")
    @Builder.Default
    private Integer totalCheckinDays = 0;

    // ===== 智能干预阈值（智能通知中心专用，null 时使用默认值） =====

    /** 干预提醒总开关 */
    @Column(name = "intervention_enabled")
    @Builder.Default
    private Boolean interventionEnabled = true;

    /** 进度提醒阈值（%）：完成率低于该值生成预警提醒，默认 65 */
    @Column(name = "intervention_progress_threshold")
    @Builder.Default
    private Float interventionProgressThreshold = 65f;

    /** 测评降幅阈值（%）：分数降幅超过该值生成预警提醒，默认 10 */
    @Column(name = "intervention_score_decline_threshold")
    @Builder.Default
    private Float interventionScoreDeclineThreshold = 10f;

    /** 连续未登录预警天数，默认 3 */
    @Column(name = "intervention_inactive_days")
    @Builder.Default
    private Integer interventionInactiveDays = 3;

    // ===== 时间字段 =====

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}