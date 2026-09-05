package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 会话阶段实体类
 * 记录学习会话中每个阶段的执行情况
 */
@Entity
@Table(name = "session_phases", indexes = {
    @Index(name = "idx_sp_session_id", columnList = "session_id"),
    @Index(name = "idx_sp_session_phase", columnList = "session_id, phase_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LearningSession session;

    /** 阶段ID: diagnosis/planning/learning/exercise/report */
    @Column(name = "phase_id", nullable = false, length = 20)
    private String phaseId;

    /** 负责该阶段的 Agent */
    @Column(name = "agent_id", length = 50)
    private String agentId;

    /** 输入参数（JSON） */
    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    /** 输出结果（JSON） */
    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    /** 阶段状态: pending/executing/completed/failed */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    /** 执行耗时（毫秒） */
    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
