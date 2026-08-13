package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 工具调用统计实体
 * 记录每个工具的调用次数（总调用次数和会话调用次数），数据持久化到数据库
 */
@Entity
@Table(name = "tool_call_stats")
@Comment("工具调用统计表")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    @Column(name = "tool_id", nullable = false, length = 50, unique = true,
            columnDefinition = "VARCHAR(50) NOT NULL COMMENT '工具ID'")
    private String toolId;

    @Column(name = "tool_name", nullable = false, length = 100,
            columnDefinition = "VARCHAR(100) NOT NULL COMMENT '工具名称'")
    private String toolName;

    @Column(name = "total_calls",
            columnDefinition = "INT DEFAULT 0 COMMENT '总调用次数'")
    @Builder.Default
    private Integer totalCalls = 0;

    @Column(name = "session_calls",
            columnDefinition = "INT DEFAULT 0 COMMENT '本次会话调用次数'")
    @Builder.Default
    private Integer sessionCalls = 0;

    @Column(name = "last_called_at",
            columnDefinition = "DATETIME COMMENT '最后调用时间'")
    private LocalDateTime lastCalledAt;

    @Column(name = "created_at", updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at",
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
