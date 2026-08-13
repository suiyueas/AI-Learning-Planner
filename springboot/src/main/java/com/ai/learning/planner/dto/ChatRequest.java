package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

/**
 * 聊天请求DTO
 * 包含聊天消息、会话ID、模型选择、知识库和工具调用选项等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 10000, message = "消息内容不能超过 10000 字符")
    private String message;
    private String sessionId;
    private String role;
    private String agentType;
    /** 指定使用的模型 key，为空时使用当前默认模型 */
    private String model;
    /** 用户ID，用于保存对话历史 */
    private Long userId;
    /** 是否启用联网搜索 */
    private boolean webSearch;
    /** 是否启用知识库检索 */
    @Default
    private boolean useKnowledge = true;
    /** 是否启用 MCP 工具调用 */
    @Default
    private boolean useTools = true;
}