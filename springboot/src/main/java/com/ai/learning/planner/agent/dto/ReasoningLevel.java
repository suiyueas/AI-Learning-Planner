package com.ai.learning.planner.agent.dto;

/**
 * 智能体思考深度模式枚举
 */
public enum ReasoningLevel {
    /** 快速模式：只展示最终结果，不展示推理过程 */
    FAST("fast", "快速模式"),

    /** 标准模式：展示关键推理步骤 */
    STANDARD("standard", "标准模式"),

    /** 深度思考：展示完整推理链路，含备选方案、反思、决策依据 */
    DEEP("deep", "深度思考");

    private final String value;
    private final String description;

    ReasoningLevel(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static ReasoningLevel fromValue(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }
        for (ReasoningLevel level : values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        return STANDARD;
    }
}