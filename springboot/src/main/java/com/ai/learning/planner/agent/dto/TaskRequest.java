package com.ai.learning.planner.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务执行请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    /** 目标Agent ID */
    @NotBlank(message = "agentId不能为空")
    private String agentId;

    /** 任务描述 */
    @NotBlank(message = "任务描述不能为空")
    private String message;

    /** 会话ID */
    private String sessionId;

    /** 思考深度模式：fast(快速) / standard(标准) / deep(深度) */
    @Builder.Default
    private String reasoningLevel = "standard";
}