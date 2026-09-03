package com.ai.learning.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话摘要 DTO
 * 用于返回会话列表（不含完整消息内容），减少数据传输量
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummary {
    private String sessionId;
    private String title;
    private int messageCount;
    private String lastMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
