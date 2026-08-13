package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 测评答题记录表
 */
@Entity
@Table(name = "assessment_records", indexes = {
    @Index(name = "idx_ar_user", columnList = "user_id"),
    @Index(name = "idx_ar_subject", columnList = "subject"),
    @Index(name = "idx_ar_user_subject", columnList = "user_id, subject"),
    @Index(name = "idx_ar_user_createdat", columnList = "user_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String subject;

    /** 难度：easy / medium / hard（可为空，兼容旧数据） */
    @Column(length = 20)
    private String difficulty;

    /** 得分 */
    private Integer score;

    /** 总题数 */
    private Integer total;

    /** 题目ID和用户答案（JSON） */
    @Column(columnDefinition = "JSON")
    private String details;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (difficulty == null) {
            difficulty = "medium";
        }
    }

    public String getDifficulty() {
        return difficulty != null ? difficulty : "medium";
    }
}