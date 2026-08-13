package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 用户通知（智能通知中心）
 * priority 区分：EMERGENCY（紧急干预，P0）/ WARNING（预警提醒，P1）/ INFO（普通消息，P2）
 * category 区分：PROGRESS（学习进度）/ KNOWLEDGE（知识点掌握）/ SYSTEM（系统消息）
 * action_type 区分：ADJUST_PLAN（立即调整计划）/ START_REVIEW（开始复习）/
 *                  VIEW_WEAKNESS（查看薄弱点）/ VIEW_DETAIL（查看详情）
 */
@Entity
@Table(name = "user_notifications", indexes = {
    @Index(name = "idx_notif_user", columnList = "user_id"),
    @Index(name = "idx_notif_user_read", columnList = "user_id, is_read"),
    @Index(name = "idx_notif_priority", columnList = "priority"),
    @Index(name = "idx_notif_category", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** 优先级：EMERGENCY（紧急）/ WARNING（预警）/ INFO（普通） */
    @Column(nullable = false, length = 20)
    private String priority;

    /** 类别：PROGRESS（进度）/ KNOWLEDGE（知识点）/ SYSTEM（系统） */
    @Column(nullable = false, length = 30)
    private String category;

    /** 快捷操作类型：ADJUST_PLAN / START_REVIEW / VIEW_WEAKNESS / VIEW_DETAIL */
    @Column(name = "action_type", length = 30)
    private String actionType;

    /** 快捷操作参数（JSON） */
    @Column(name = "action_data", columnDefinition = "TEXT")
    private String actionData;

    /** 是否已读 */
    @Column(name = "is_read")
    private Boolean isRead;

    /** 是否已处理（干预类专用） */
    @Column(name = "is_handled")
    private Boolean isHandled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
        if (isHandled == null) {
            isHandled = false;
        }
        if (priority == null) {
            priority = "INFO";
        }
        if (category == null) {
            category = "SYSTEM";
        }
    }
}
