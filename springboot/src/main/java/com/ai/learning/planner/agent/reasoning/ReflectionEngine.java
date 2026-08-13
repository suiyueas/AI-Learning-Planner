package com.ai.learning.planner.agent.reasoning;

import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 反思引擎（Reflection Engine）
 * 每个大步骤（3-5 个 Tool Calls）结束后强制插入一次反思调用，
 * 输出 status / what_worked / what_missing / next_action_adjustment 结构化 JSON
 */
@Slf4j
public class ReflectionEngine {

    /** 触发间隔：每 N 次工具调用后反思 */
    private final int interval;

    private final ModelManager modelManager;

    private final ReasoningMonitor monitor;

    public ReflectionEngine(int interval, ModelManager modelManager, ReasoningMonitor monitor) {
        this.interval = Math.max(interval, 1);
        this.modelManager = modelManager;
        this.monitor = monitor;
    }

    /**
     * 判断是否应触发反思（工具调用计数达到间隔倍数）
     */
    public boolean shouldReflect(int toolCallCount) {
        return toolCallCount > 0 && toolCallCount % interval == 0;
    }

    /**
     * 执行反思
     *
     * @param goal        当前目标
     * @param recentSteps 最近执行的步骤摘要列表
     * @return 结构化反思结果
     */
    public ReflectionResult reflect(String goal, List<String> recentSteps) {
        try {
            if (modelManager != null) {
                String prompt = """
                        你是推理过程的自省者。请回顾最近执行的推理步骤，输出严格 JSON（不要其他内容）：
                        {
                          "status": "on_track | deviating | stalled",
                          "what_worked": "...",
                          "what_missing": "...",
                          "next_action_adjustment": "..."
                        }

                        当前目标：%s
                        最近步骤：
                        %s
                        """.formatted(goal == null ? "" : goal,
                        recentSteps == null || recentSteps.isEmpty() ? "（无）" : String.join("\n", recentSteps));
                String response = modelManager.createChatClient().prompt().user(prompt).call().content();
                ReflectionResult result = ReflectionResult.parse(response);
                if (monitor != null) {
                    monitor.recordReflection(result.status());
                }
                log.info("[Reflection] 状态={}, 调整={}", result.status(), result.nextActionAdjustment());
                return result;
            }
        } catch (Exception e) {
            log.warn("[Reflection] LLM 反思失败，返回 on_track 占位: {}", e.getMessage());
        }
        ReflectionResult fallback = new ReflectionResult(ReflectionResult.Status.on_track,
                "步骤已执行", "无", "继续按计划推进");
        if (monitor != null) {
            monitor.recordReflection(fallback.status());
        }
        return fallback;
    }

    public int getInterval() {
        return interval;
    }
}
