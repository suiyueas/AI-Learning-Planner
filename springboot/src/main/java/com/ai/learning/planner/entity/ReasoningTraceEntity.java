package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 思考轨迹实体
 * 用于保存智能体的完整推理链路，支持历史追溯
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reasoning_trace")
public class ReasoningTraceEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "execution_id", length = 64)
    private String executionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "agent_id", length = 64)
    private String agentId;

    @Column(name = "reasoning_level", length = 32)
    private String reasoningLevel;

    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;

    @Column(name = "thinking_steps_json", columnDefinition = "TEXT")
    private String thinkingStepsJson;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}