package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日任务实体类
 * 对应数据库中的daily_tasks表，存储用户每日的学习任务
 */
@Entity
@Table(name = "daily_tasks", indexes = {
    @Index(name = "idx_daily_path_date", columnList = "path_id,task_date"),
    @Index(name = "idx_daily_user_date", columnList = "user_id,task_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTask {
    @Id
    private String id;

    @Column(name = "path_id", nullable = false)
    private String pathId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type; // read, video, practice, review

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(nullable = false)
    private String status; // pending, in_progress, completed, skipped

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "source_node_id")
    private String sourceNodeId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}