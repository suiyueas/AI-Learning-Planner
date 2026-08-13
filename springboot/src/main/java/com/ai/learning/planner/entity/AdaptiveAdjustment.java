package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 自适应调整记录表
 * 记录系统对用户学习路径的每次自适应调整（复习插入/进阶推荐/计划调整/资源推荐/难度调整），
 * 支撑自适应引擎页面的真实调整历史展示。
 */
@Entity
@Table(name = "adaptive_adjustment", indexes = {
    @Index(name = "idx_adj_user_created", columnList = "user_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveAdjustment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "path_id", nullable = false, length = 255)
    private String pathId;

    /** 调整类型：review_insert（复习插入）/ advance_recommend（进阶推荐）/ plan_adjust（计划调整）/ resource_recommend（资源推荐）/ difficulty_adjust（难度调整） */
    @Column(name = "adjustment_type", nullable = false, length = 50)
    private String adjustmentType;

    /** 触发原因（如：测评正确率由 85% 降至 45%） */
    @Column(name = "trigger_reason", nullable = false, length = 500)
    private String triggerReason;

    /** 调整详情（JSON 字符串：调整前后的任务ID、顺序、目标等） */
    @Column(name = "adjustment_detail", columnDefinition = "JSON")
    private String adjustmentDetail;

    /** 效果指标（如：正确率提升 20%） */
    @Column(name = "effect_metric", length = 200)
    private String effectMetric;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
