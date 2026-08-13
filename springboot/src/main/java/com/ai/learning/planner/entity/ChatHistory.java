package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 聊天历史实体
 * 对应 chat_histories 表，存储用户与 AI 的对话记录；userId 为 null 表示游客匿名会话
 */
@Entity
@Table(name = "chat_histories", indexes = {
    @Index(name = "idx_ch_session", columnList = "session_id"),
    @Index(name = "idx_ch_user", columnList = "user_id"),
    @Index(name = "idx_ch_user_created", columnList = "user_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {
    @Id
    private String id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "agent_type")
    private String agentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}