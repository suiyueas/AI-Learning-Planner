package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习会话实体类
 * 记录用户的一次完整学习旅程：诊断 -> 规划 -> 学习 -> 习题 -> 报告
 */
@Entity
@Table(name = "learning_sessions", indexes = {
    @Index(name = "idx_ls_user_id", columnList = "user_id"),
    @Index(name = "idx_ls_user_status", columnList = "user_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** 学习目标（如"学 Python 数据分析"） */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String goal;

    /** 当前阶段: diagnosis/planning/learning/exercise/report */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String phase = "diagnosis";

    /** 会话状态: active/paused/completed/abandoned */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    /** 进度 0-100 */
    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0;

    /** 会话上下文（JSON，传递给 Agent） */
    @Column(columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 各阶段记录 */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionPhase> phases = new ArrayList<>();

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
