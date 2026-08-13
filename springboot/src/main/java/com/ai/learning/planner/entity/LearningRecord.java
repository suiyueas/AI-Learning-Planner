package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 学习记录实体类
 * 对应数据库中的learning_records表，记录用户学习路径中每个节点的学习情况
 */
@Entity
@Table(name = "learning_records", indexes = {
    @Index(name = "idx_lr_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_lr_path_id", columnList = "path_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "path_id", nullable = false)
    private String pathId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "node_type")
    private String nodeType;

    @Column(nullable = false)
    private String status;

    @Column(name = "mastery_level")
    private Float masteryLevel;

    @Column(name = "time_spent")
    private Integer timeSpent;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}