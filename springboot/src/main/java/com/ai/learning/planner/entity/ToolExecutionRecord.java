package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 工具执行记录实体
 * 记录每个工具的每次执行详情，包括参数、结果、执行耗时等
 */
@Entity
@Table(name = "tool_execution_records", indexes = {
    @Index(name = "idx_ter_user_id", columnList = "user_id"),
    @Index(name = "idx_ter_tool_id", columnList = "tool_id"),
    @Index(name = "idx_ter_created_at", columnList = "created_at")
})
@Comment("工具执行记录表")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    @Column(name = "tool_id", nullable = false, length = 50,
            columnDefinition = "VARCHAR(50) NOT NULL COMMENT '工具ID'")
    private String toolId;

    @Column(name = "tool_name", nullable = false, length = 100,
            columnDefinition = "VARCHAR(100) NOT NULL COMMENT '工具名称'")
    private String toolName;

    @Column(columnDefinition = "JSON COMMENT '执行参数'")
    private String params;

    @Column(columnDefinition = "JSON COMMENT '执行结果'")
    private String result;

    @Column(length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'pending' COMMENT '执行状态'")
    @Builder.Default
    private String status = "pending";

    @Column(name = "execution_time",
            columnDefinition = "BIGINT COMMENT '执行耗时(ms)'")
    private Long executionTime;

    @Column(name = "user_id", length = 50,
            columnDefinition = "VARCHAR(50) COMMENT '用户ID'")
    private String userId;

    @Column(name = "created_at",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}