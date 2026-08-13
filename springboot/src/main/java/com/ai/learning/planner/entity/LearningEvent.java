package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 学习行为事件（合并 achievements + interventions）
 * event_type 区分：'achievement'（成就）/ 'intervention'（干预）
 */
@Entity
@Table(name = "learning_events", indexes = {
    @Index(name = "idx_event_user", columnList = "user_id"),
    @Index(name = "idx_event_type", columnList = "event_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningEvent {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** 事件类型：achievement（成就）/ intervention（干预） */
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    /** 事件标识键：成就则为 badge 名称，干预则为干预类型 */
    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 扩展数据（JSON），存储原始表的特定字段 */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
