package com.ai.learning.planner.agent.dto;

/**
 * SSE流式输出内容类型枚举
 * 用于区分不同类型的思考过程输出
 */
public enum ThinkingType {
    /** 任务理解阶段 */
    UNDERSTANDING("understanding", "🤔 正在理解你的需求..."),

    /** 执行计划阶段 */
    PLANNING("planning", "📋 制定执行计划"),

    /** 思考阶段 */
    THINKING("thinking", "🧠 推理"),

    /** 行动阶段 */
    ACTION("action", "🔧 行动"),

    /** 观察阶段 */
    OBSERVATION("observation", "👁️ 观察"),

    /** 反思阶段 */
    REFLECTION("reflection", "🔄 反思与修正"),

    /** 备选方案阶段 */
    ALTERNATIVE("alternative", "🔀 备选方案"),

    /** 最终结论 */
    RESULT("result", "🎯 最终结论"),

    /** 执行步骤 */
    STEP("step", "📝 执行步骤"),

    /** 错误信息 */
    ERROR("error", "❌ 错误"),

    /** 完成状态 */
    COMPLETE("complete", "✅ 完成");

    private final String value;
    private final String defaultLabel;

    ThinkingType(String value, String defaultLabel) {
        this.value = value;
        this.defaultLabel = defaultLabel;
    }

    public String getValue() {
        return value;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    public static ThinkingType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return THINKING;
        }
        for (ThinkingType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return THINKING;
    }
}