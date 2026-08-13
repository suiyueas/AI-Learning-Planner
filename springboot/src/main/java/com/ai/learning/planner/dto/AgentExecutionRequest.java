package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Agent执行结果保存请求DTO
 */
@Data
public class AgentExecutionRequest {

    @NotBlank(message = "agentId不能为空")
    private String agentId;

    private String agentName;

    private String taskDescription;

    private String sessionId;

    private String executionId;

    private Map<String, Object> result;

    private List<Map<String, Object>> logs;

    private Long duration;

    private String status;
}