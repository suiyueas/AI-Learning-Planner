package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 学习路径实体类
 * 对应数据库中的learning_paths表，存储用户的学习路径信息
 */
@Entity
@Table(name = "learning_paths", indexes = {
    @Index(name = "idx_lp_user_id", columnList = "user_id"),
    @Index(name = "idx_lp_user_active", columnList = "user_id, is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPath {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Version
    private Integer version;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "completion_percentage")
    private Float completionPercentage;

    /** 节点列表（JSON），合并 learning_path_nodes + learning_records 数据 */
    @Column(columnDefinition = "JSON")
    private String nodes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}