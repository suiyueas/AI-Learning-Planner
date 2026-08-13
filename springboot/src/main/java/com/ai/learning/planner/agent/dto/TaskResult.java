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
}
