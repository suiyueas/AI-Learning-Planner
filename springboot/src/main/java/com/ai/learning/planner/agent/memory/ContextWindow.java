package com.ai.learning.planner.agent.memory;

import java.util.List;
import java.util.Map;

/**
 * 上下文窗口
 * Token 估算与使用率计算：中文按 1 字 ≈ 1 Token，英文按 4 字符 ≈ 1 Token
 */
public class ContextWindow {

    /** 窗口总容量（Token） */
    private final int totalTokens;

    /** 压缩触发阈值（使用率 > 该值时触发摘要压缩） */
    private final double compressThreshold;

    public ContextWindow() {
        this(30720, 0.7);
    }

    public ContextWindow(int totalTokens, double compressThreshold) {
        this.totalTokens = totalTokens;
        this.compressThreshold = compressThreshold;
    }

    /**
     * 估算文本 Token 数
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int ascii = 0;
        int nonAscii = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < 128) ascii++; else nonAscii++;
        }
        return (int) Math.ceil(ascii / 4.0 + nonAscii);
    }

    /**
     * 估算消息列表 Token 数
     */
    public static int estimateTokens(List<Map<String, Object>> messages) {
        if (messages == null) return 0;
        return messages.stream()
                .mapToInt(m -> {
                    Object content = m.get("content");
                    return estimateTokens(content == null ? "" : content.toString());
                })
                .sum();
    }

    /**
     * 计算使用率（0-1）
     */
    public double usageRatio(List<Map<String, Object>> messages) {
        return (double) estimateTokens(messages) / totalTokens;
    }

    /**
     * 是否需要压缩
     */
    public boolean needsCompression(List<Map<String, Object>> messages) {
        return usageRatio(messages) > compressThreshold;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public double getCompressThreshold() {
        return compressThreshold;
    }
}
