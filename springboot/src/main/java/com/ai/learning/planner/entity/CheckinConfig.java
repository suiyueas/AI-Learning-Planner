package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 签到配置表实体类
 * 存储每日积分、连续奖励天数、奖励积分、各功能消耗积分等
 * 支持热更新，无需重启服务
 */
@Entity
@Table(name = "checkin_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置键名（唯一） */
    @Column(name = "config_key", nullable = false, unique = true, length = 50)
    private String configKey;

    /** 配置值 */
    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    /** 配置描述 */
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 预定义的配置键名常量
    public static final String KEY_DAILY_CHECKIN_POINTS = "daily_checkin_points";
    public static final String KEY_CONSECUTIVE_DAYS = "consecutive_days";
    public static final String KEY_CONSECUTIVE_BONUS_POINTS = "consecutive_bonus_points";
    public static final String KEY_CHAT_CONSUME_POINTS = "chat_consume_points";
    public static final String KEY_AGENT_CONSUME_POINTS = "agent_consume_points";
    public static final String KEY_LEARNING_PATH_CONSUME_POINTS = "learning_path_consume_points";

    // 默认值常量
    public static final String DEFAULT_DAILY_CHECKIN_POINTS = "10";
    public static final String DEFAULT_CONSECUTIVE_DAYS = "7";
    public static final String DEFAULT_CONSECUTIVE_BONUS_POINTS = "20";
    public static final String DEFAULT_CHAT_CONSUME_POINTS = "5";
    public static final String DEFAULT_AGENT_CONSUME_POINTS = "10";
    public static final String DEFAULT_LEARNING_PATH_CONSUME_POINTS = "20";
}
