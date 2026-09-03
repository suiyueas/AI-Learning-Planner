package com.ai.learning.planner.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 结构化思考过程DTO
 * 用于深度思考模式的完整推理链路展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThinkingProcess {

    /** 思考类型 */
    private String type;

    /** 思考内容 */
    private String content;

    /** 时间戳 */
    private long timestamp;

    /** 当前步骤序号 */
    private int step;

    /** 标签（可自定义显示标签） */
    private String label;

    /** 子步骤列表（用于嵌套结构） */
    private List<ThinkingProcess> subSteps;

    /** 额外元数据 */
    private Map<String, Object> metadata;

    /**
     * 创建理解阶段
     */
    public static ThinkingProcess understanding(String content, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.UNDERSTANDING.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.UNDERSTANDING.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建计划阶段
     */
    public static ThinkingProcess planning(String content, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.PLANNING.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.PLANNING.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建思考阶段
     */
    public static ThinkingProcess thinking(String content, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.THINKING.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.THINKING.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建行动阶段
     */
    public static ThinkingProcess action(String toolName, Object args, String result, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.ACTION.getValue())
                .content(String.format("调用工具 [%s]，参数: %s，结果: %s", toolName, args, result))
                .step(step)
                .label(ThinkingType.ACTION.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of("tool", toolName, "args", args, "result", result))
                .build();
    }

    /**
     * 创建观察阶段
     */
    public static ThinkingProcess observation(String content, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.OBSERVATION.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.OBSERVATION.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建反思阶段
     */
    public static ThinkingProcess reflection(String content, int step, Map<String, Object> metadata) {
        return ThinkingProcess.builder()
                .type(ThinkingType.REFLECTION.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.REFLECTION.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .metadata(metadata)
                .build();
    }

    /**
     * 创建备选方案阶段
     */
    public static ThinkingProcess alternative(String content, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.ALTERNATIVE.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.ALTERNATIVE.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建执行步骤
     */
    public static ThinkingProcess step(int stepNumber, String description, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.STEP.getValue())
                .content(String.format("Step %d: %s", stepNumber, description))
                .step(step)
                .label(ThinkingType.STEP.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of("stepNumber", stepNumber, "description", description))
                .build();
    }

    /**
     * 创建最终结论
     */
    public static ThinkingProcess result(String content, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.RESULT.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.RESULT.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误信息
     */
    public static ThinkingProcess error(String content, int step) {
        return ThinkingProcess.builder()
                .type(ThinkingType.ERROR.getValue())
                .content(content)
                .step(step)
                .label(ThinkingType.ERROR.getDefaultLabel())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}