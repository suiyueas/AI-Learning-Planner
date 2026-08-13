package com.ai.learning.planner.mcp.ai;

import java.util.List;

/**
 * 工具执行上下文
 * 携带用户与学习进度信息，使工具结果可以个性化。
 * 当前 userId 由调用方注入，其余字段为扩展预留。
 */
public record AiToolContext(
        String userId,
        String pathId,
        String currentModule,
        Double progressPercent,
        List<String> recentTopics
) {

    public static AiToolContext anonymous() {
        return new AiToolContext(null, null, null, null, List.of());
    }

    public static AiToolContext ofUserId(String userId) {
        return new AiToolContext(userId, null, null, null, List.of());
    }
}
