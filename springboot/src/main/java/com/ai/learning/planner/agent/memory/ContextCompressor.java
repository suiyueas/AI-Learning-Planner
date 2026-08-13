package com.ai.learning.planner.agent.memory;

import com.ai.learning.planner.service.ModelManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 呼吸式上下文压缩器（ContextCompressor）
 * 当 Token 使用率超过阈值（默认 70%）时自动触发摘要压缩：
 * - 必须保留：系统指令、当前活跃目标、最近 3 轮交互的原始内容
 * - 其余历史：转化为结构化摘要
 * - 输出压缩率指标（Micrometer）
 */
@Slf4j
public class ContextCompressor {

    /** 保留最近交互轮数 */
    public static final int KEEP_RECENT_TURNS = 3;

    private final ContextWindow contextWindow;
    private final ModelManager modelManager;
    private final MeterRegistry meterRegistry;

    public ContextCompressor(ContextWindow contextWindow, ModelManager modelManager, MeterRegistry meterRegistry) {
        this.contextWindow = contextWindow;
        this.modelManager = modelManager;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 压缩结果
     *
     * @param compressed    压缩后的消息列表
     * @param summary       历史摘要（结构化）
     * @param compressionRatio 压缩率（0-1，越大压缩越多）
     * @param triggered     是否触发压缩
     * @param tokensBefore  压缩前 Token
     * @param tokensAfter   压缩后 Token
     */
    public record CompressionResult(List<Map<String, Object>> compressed, String summary,
                                    double compressionRatio, boolean triggered,
                                    int tokensBefore, int tokensAfter) {
    }

    /**
     * 执行压缩（如使用率未超阈值则原样返回）
     *
     * @param messages    当前完整消息列表
     * @param systemPrompt 系统指令（始终保留）
     * @param activeGoal   当前活跃目标（始终保留）
     */
    public CompressionResult compress(List<Map<String, Object>> messages,
                                      String systemPrompt, String activeGoal) {
        int tokensBefore = ContextWindow.estimateTokens(messages);
        boolean triggered = contextWindow.needsCompression(messages);

        if (!triggered) {
            return new CompressionResult(new ArrayList<>(messages), "", 0.0, false, tokensBefore, tokensBefore);
        }

        log.info("[ContextCompressor] Token 使用率超阈值，触发压缩: {} tokens", tokensBefore);

        // 1. 保留最近 KEEP_RECENT_TURNS 轮（最后 3 对 user/assistant 消息）
        List<Map<String, Object>> recent = new ArrayList<>();
        int kept = 0;
        for (int i = messages.size() - 1; i >= 0 && kept < KEEP_RECENT_TURNS * 2; i--) {
            recent.add(0, messages.get(i));
            String role = String.valueOf(messages.get(i).getOrDefault("role", ""));
            if ("user".equals(role) || "assistant".equals(role)) {
                kept++;
            }
        }

        // 2. 其余历史 → 结构化摘要
        List<Map<String, Object>> historical = new ArrayList<>(messages);
        historical.removeAll(recent);
        String summary = summarize(historical, systemPrompt, activeGoal);

        // 3. 组装压缩后消息：系统指令 → 活跃目标 → 历史摘要 → 最近轮次
        List<Map<String, Object>> compressed = new ArrayList<>();
        compressed.add(Map.of("role", "system", "content", systemPrompt, "compressed", false));
        if (activeGoal != null && !activeGoal.isBlank()) {
            compressed.add(Map.of("role", "system", "content", "【当前活跃目标】" + activeGoal, "compressed", false));
        }
        if (!summary.isBlank()) {
            compressed.add(Map.of("role", "system", "content", "【历史摘要】" + summary, "compressed", true));
        }
        compressed.addAll(recent);

        int tokensAfter = ContextWindow.estimateTokens(compressed);
        double ratio = tokensBefore > 0 ? 1.0 - (double) tokensAfter / tokensBefore : 0.0;

        // 4. 监控埋点：压缩率 + 压缩次数
        if (meterRegistry != null) {
            meterRegistry.counter("reasoning.context.compression.count").increment();
            meterRegistry.gauge("reasoning.context.compression.ratio", ratio);
        }

        log.info("[ContextCompressor] 压缩完成: {} -> {} tokens，压缩率 {}", tokensBefore, tokensAfter, String.format("%.2f%%", ratio * 100));
        return new CompressionResult(compressed, summary, ratio, true, tokensBefore, tokensAfter);
    }

    /**
     * 历史消息转结构化摘要
     * 优先使用 LLM 摘要，LLM 不可用时降级为规则截断（保留首末要点）
     */
    private String summarize(List<Map<String, Object>> historical, String systemPrompt, String activeGoal) {
        if (historical.isEmpty()) return "";

        // 拼接历史文本（限制输入长度）
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> m : historical) {
            Object content = m.get("content");
            if (content != null && !content.toString().isBlank()) {
                sb.append('[').append(m.getOrDefault("role", "?")).append("] ")
                        .append(content).append('\n');
            }
        }
        String historyText = sb.toString();
        if (historyText.length() > 6000) {
            historyText = historyText.substring(0, 6000) + "...";
        }

        // 尝试 LLM 结构化摘要
        try {
            if (modelManager != null) {
                String prompt = """
                        请将以下对话历史压缩为结构化摘要，格式：
                        ## 关键结论
                        - ...
                        ## 已完成动作
                        - ...
                        ## 待办事项
                        - ...
                        ## 重要上下文（用户偏好/约束/数据）
                        - ...
                        要求：保留所有事实性信息，不添加新内容。
                        当前目标：%s

                        历史对话：
                        %s
                        """.formatted(activeGoal == null ? "" : activeGoal, historyText);
                String summary = modelManager.createChatClient().prompt().user(prompt).call().content();
                if (summary != null && !summary.isBlank()) {
                    return summary.trim();
                }
            }
        } catch (Exception e) {
            log.warn("[ContextCompressor] LLM 摘要失败，降级为规则截断: {}", e.getMessage());
        }

        // 规则截断兜底：保留首条结论与最后动作
        List<String> lines = historyText.lines()
                .filter(l -> !l.isBlank())
                .toList();
        if (lines.size() <= 6) {
            return String.join("\n", lines);
        }
        return "（截断摘要）\n" + lines.get(0) + "\n...\n" + lines.get(lines.size() - 1);
    }
}
