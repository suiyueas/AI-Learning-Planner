package com.ai.learning.planner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 高级推理引擎配置属性
 */
@Data
@ConfigurationProperties(prefix = "app.reasoning")
public class ReasoningProperties {

    /** 是否启用高级推理引擎 */
    private boolean enabled = true;

    /** 最大推理步数 */
    private int maxSteps = 30;

    /** Evaluator 剪枝阈值（0-1，低于该值触发剪枝） */
    private double pruneThreshold = 0.3;

    /** ReflectionEngine 反思间隔（每 N 次工具调用触发反思） */
    private int reflectionInterval = 3;

    /** 上下文压缩触发阈值（使用率 > 该值时触发压缩，0-1） */
    private double contextCompressionThreshold = 0.7;

    /** 上下文窗口总容量（Token） */
    private int contextWindowTokens = 30720;
}
