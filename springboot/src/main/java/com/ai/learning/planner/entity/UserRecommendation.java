package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 用户推荐记录表
 * 记录基于用户学习进度生成的个性化推荐内容（资源/课程/知识块/学习路径），
 * 支撑自适应引擎页面的推荐展示与点击/消费状态流转。
 */
@Entity
@Table(name = "user_recommendation", indexes = {
    @Index(name = "idx_rec_user_status", columnList = "user_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRecommendation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "path_id", length = 255)
    private String pathId;

    /** 内容类型：resource（资源）/ course（课程）/ knowledge_block（知识块）/ learning_path（学习路径） */
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "content_id", nullable = false, length = 255)
    private String contentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 匹配度 0-1 */
    @Column(name = "match_score")
    private Float matchScore;

    /** 匹配原因（规则化引擎生成的可解释说明） */
    @Column(name = "match_reason", length = 255)
    private String matchReason;

    /** 优先级：high / normal / low */
    @Column(length = 20)
    @Builder.Default
    private String priority = "normal";

    /** 状态：pending（未点击）/ clicked（已点击）/ dismissed（已忽略）/ consumed（已消费） */
    @Column(length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (priority == null) {
            priority = "normal";
        }
        if (status == null) {
            status = "pending";
        }
    }
}
