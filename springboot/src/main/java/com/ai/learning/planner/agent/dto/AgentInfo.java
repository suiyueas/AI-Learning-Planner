package com.ai.learning.planner.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInfo {
    /** 智能体ID */
    private String id;

    /** 智能体名称 */
    private String name;

    /** 智能体描述 */
    private String description;

    /** 智能体类型（orchestrator / app） */
    private String type;

    /** 当前状态 */
    private String status;

    /** 图标 */
    private String icon;

    /** 角色/职责简短描述 */
    private String role;

    /** 可用工具列表 */
    private List<String> tools;

    /** 当前迭代次数 */
    private int currentStep;

    /** 最大步数 */
    private int maxSteps;
}
