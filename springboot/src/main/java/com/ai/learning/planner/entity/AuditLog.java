package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全审计日志实体
 *
 * 记录"谁在什么时间执行了什么操作"，用于安全追溯：
 * - 工具调用、Agent 执行、聊天、安全拦截等操作
 * - 请求/响应内容均经过脱敏与截断后入库
 *
 * @author AI Security Team
 * @version 1.0
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 36)
    private String id;

    /** 操作用户ID */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /** 操作类型：LOGIN/CHAT/TOOL_CALL/AGENT_EXEC/INPUT_BLOCKED 等 */
    @Column(nullable = false, length = 50)
    private String action;

    /** 资源类型：PATH/TASK/KNOWLEDGE/REPORT/tool 等 */
    @Column(name = "resource_type", length = 50)
    private String resourceType;

    /** 资源ID（Agent ID、工具名、文档ID等） */
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    /** 请求内容（脱敏+截断后） */
    @Column(columnDefinition = "TEXT")
    private String request;

    /** 响应摘要（脱敏+截断后） */
    @Column(columnDefinition = "TEXT")
    private String response;

    /** 状态：SUCCESS/FAILURE/BLOCKED */
    @Column(length = 20)
    private String status;

    /** 客户端IP */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** 客户端UA */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** 执行耗时（毫秒） */
    @Column(name = "duration_ms")
    private Integer durationMs;

    /** 错误信息（如有） */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
