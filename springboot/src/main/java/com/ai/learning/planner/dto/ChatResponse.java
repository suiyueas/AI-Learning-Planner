package com.ai.learning.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天响应 DTO
 * 兼容普通对话响应与流式 SSE 结构化事件（content/knowledgeRef/toolCall/toolResult/done）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String id;
    private String content;
    private String role;
    private String agentType;
    private Long timestamp;

    // ===== 结构化 SSE 事件类型 =====
    /** 事件类型：content / knowledgeRef / toolCall / toolResult / done */
    private String type;

    // ===== 知识库引用 =====
    private List<Map<String, Object>> sources;

    // ===== 工具调用 =====
    private Map<String, Object> toolCall;

    // ===== MCP/知识库 状态快照 =====
    private Map<String, Object> mcpStatus;
}
