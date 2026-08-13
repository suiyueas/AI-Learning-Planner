package com.ai.learning.planner.mcp.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具动态超时策略
 * 每个工具可配置独立超时，原则：读超时 > 写超时（写操作耗时更长）
 */
public class McpTimeoutPolicy {

    /** 默认读超时 */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(15);

    /** 默认写超时 */
    public static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(30);

    /** 写操作工具名前缀（exec/write/delete/update/create/save） */
    private static final String[] WRITE_PREFIXES = {"exec", "write", "delete", "update", "create", "save", "remove", "insert"};

    /** 工具级自定义超时（name -> [readTimeoutMs, writeTimeoutMs]） */
    private final Map<String, long[]> toolTimeouts = new ConcurrentHashMap<>();

    /**
     * 为指定工具配置自定义超时
     *
     * @param toolName        工具名
     * @param readTimeoutMs   读超时（毫秒，必须小于写超时）
     * @param writeTimeoutMs  写超时（毫秒）
     */
    public void configure(String toolName, long readTimeoutMs, long writeTimeoutMs) {
        if (readTimeoutMs >= writeTimeoutMs) {
            throw new IllegalArgumentException("读超时必须小于写超时: " + toolName);
        }
        toolTimeouts.put(toolName, new long[]{readTimeoutMs, writeTimeoutMs});
    }

    /**
     * 获取工具读超时（毫秒）
     */
    public long readTimeoutMs(String toolName) {
        long[] t = toolTimeouts.get(toolName);
        if (t != null) return t[0];
        return isWriteTool(toolName) ? DEFAULT_READ_TIMEOUT.toMillis() : DEFAULT_READ_TIMEOUT.toMillis();
    }

    /**
     * 获取工具写超时（毫秒）
     */
    public long writeTimeoutMs(String toolName) {
        long[] t = toolTimeouts.get(toolName);
        if (t != null) return t[1];
        return isWriteTool(toolName) ? DEFAULT_WRITE_TIMEOUT.toMillis() : DEFAULT_READ_TIMEOUT.toMillis();
    }

    /**
     * 根据工具名判断是否为写操作（写操作使用更长超时）
     */
    public static boolean isWriteTool(String toolName) {
        if (toolName == null || toolName.isBlank()) return false;
        String lower = toolName.toLowerCase();
        for (String prefix : WRITE_PREFIXES) {
            if (lower.startsWith(prefix)) return true;
        }
        return false;
    }
}
