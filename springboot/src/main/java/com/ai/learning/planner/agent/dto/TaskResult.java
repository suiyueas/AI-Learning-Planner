package com.ai.learning.planner.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 任务执行结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {
    /** 目标Agent ID */
    private String agentId;

    /** Agent名称 */
    private String agentName;

    /** 执行状态 */
    private String status;

    /** 输出内容 */
    private String output;

    /** 执行步骤列表 */
    private List<Map<String, Object>> steps;

    /** 执行时长（毫秒） */
    private long duration;

    /** 总步数 */
    private int totalSteps;

    /** 错误信息 */
    private String error;

    /** 思考深度模式 */
    private String reasoningLevel;

    /** 结构化思考过程列表（深度思考模式使用） */
    private List<ThinkingProcess> thinkingProcess;

    /** 思考轨迹ID（用于追溯） */
    private String traceId;
}