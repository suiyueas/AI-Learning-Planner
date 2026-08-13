package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Agent 执行记录（合并 execution_logs + execution_results）
 * record_type 区分：'LOG' 为步骤日志，'RESULT' 为执行结果
 */
@Entity
@Table(name = "agent_executions", indexes = {
    @Index(name = "idx_agent_session", columnList = "session_id"),
    @Index(name = "idx_agent_execution", columnList = "execution_id"),
    @Index(name = "idx_agent_id", columnList = "agent_id"),
    @Index(name = "idx_agent_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecution {

    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "agent_name")
    private String agentName;

    /** 记录类型：LOG（步骤日志）/ RESULT（执行结果） */
    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    // ===== 日志相关字段（LOG） =====

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "execution_id", length = 100)
    private String executionId;

    /** 日志类型（think/act/observe/tool/system/complete/error） */
    @Column(length = 50)
    private String type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "step_number")
    private Integer stepNumber;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(length = 20)
    private String phase;

    // ===== 结果相关字段（RESULT） =====

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Column(name = "result_type", length = 50)
    private String resultType;

    @Column(name = "result_content", columnDefinition = "TEXT")
    private String resultContent;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    /** 执行输出内容（JSON格式），用于前端直接展示 */
    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    private Long duration;

    // ===== 通用字段 =====

    @Column(length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 软删除标记：true 表示已删除，查询默认过滤 */
    @Column(name = "is_deleted")
    private Boolean isDeleted;
}