package com.ai.learning.planner.agent.memory;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 呼吸式上下文压缩器测试
 */
class ContextCompressorTest {

    private ContextCompressor compressor;
    private ContextWindow window;

    @BeforeEach
    void setUp() {
        // 小窗口（300 token）便于触发压缩；ModelManager 为 null 走规则截断兜底
        window = new ContextWindow(300, 0.7);
        compressor = new ContextCompressor(window, null, new SimpleMeterRegistry());
    }

    private List<Map<String, Object>> buildMessages(int rounds) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            messages.add(Map.of("role", "user", "content", "用户第" + i + "轮问题，包含一些较长的上下文内容用于撑大 Token 用量"));
            messages.add(Map.of("role", "assistant", "content", "助手第" + i + "轮回答，包含详细的解释和示例代码片段说明"));
        }
        return messages;
    }

    @Test
    void compress_notTriggeredBelowThreshold() {
        var result = compressor.compress(buildMessages(1), "系统指令", "目标");
        assertFalse(result.triggered());
        assertEquals(result.tokensBefore(), result.tokensAfter());
    }

    @Test
    void compress_triggeredKeepsRecentTurnsAndGoal() {
        List<Map<String, Object>> messages = buildMessages(8);
        var result = compressor.compress(messages, "系统指令ABC", "活跃目标XYZ");

        assertTrue(result.triggered());
        assertTrue(result.tokensAfter() < result.tokensBefore());
        assertTrue(result.compressionRatio() > 0);

        // 保留最近 3 轮（最后 3 对 user/assistant）
        long keptRecent = result.compressed().stream()
                .filter(m -> !Boolean.TRUE.equals(m.get("compressed")))
                .filter(m -> "user".equals(m.get("role")) || "assistant".equals(m.get("role")))
                .count();
        assertEquals(6, keptRecent, "应保留最近 3 轮共 6 条原始消息");

        // 保留系统指令与活跃目标
        assertTrue(result.compressed().stream().anyMatch(m ->
                "系统指令ABC".equals(m.get("content"))));
        assertTrue(result.compressed().stream().anyMatch(m ->
                String.valueOf(m.get("content")).contains("活跃目标XYZ")));
        // 历史转为结构化摘要
        assertTrue(result.summary() != null && !result.summary().isBlank());
    }

    @Test
    void compress_markerOnSummaryMessage() {
        var result = compressor.compress(buildMessages(8), "sys", "goal");
        assertTrue(result.compressed().stream().anyMatch(m ->
                Boolean.TRUE.equals(m.get("compressed")) && String.valueOf(m.get("content")).startsWith("【历史摘要】")));
    }
}
