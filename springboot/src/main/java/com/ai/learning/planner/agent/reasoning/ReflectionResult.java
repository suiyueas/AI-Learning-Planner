package com.ai.learning.planner.agent.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 反思结果
 * 每个大步骤（3-5 个 Tool Calls）结束后强制插入一次 Reflection 调用，
 * 输出结构化 JSON（status / what_worked / what_missing / next_action_adjustment）
 */
public record ReflectionResult(Status status, String whatWorked, String whatMissing,
                               String nextActionAdjustment) {

    public enum Status {
        on_track, deviating, stalled
    }

    /**
     * 序列化为规范 JSON 输出格式
     */
    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    java.util.Map.of(
                            "status", status.name(),
                            "what_worked", whatWorked == null ? "" : whatWorked,
                            "what_missing", whatMissing == null ? "" : whatMissing,
                            "next_action_adjustment", nextActionAdjustment == null ? "" : nextActionAdjustment
                    ));
        } catch (Exception e) {
            return """
                    {
                      "status": "%s",
                      "what_worked": "%s",
                      "what_missing": "%s",
                      "next_action_adjustment": "%s"
                    }
                    """.formatted(status.name(), safe(whatWorked), safe(whatMissing), safe(nextActionAdjustment));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "'").replace("\n", " ");
    }

    /**
     * 解析 LLM 反思输出为结构化结果（解析失败时按文本推断状态）
     */
    public static ReflectionResult parse(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return new ReflectionResult(Status.stalled, "", "", "反思输出为空");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            var node = mapper.readTree(llmOutput);
            Status st = Status.valueOf(node.path("status").asText("stalled").trim());
            return new ReflectionResult(st,
                    node.path("what_worked").asText(""),
                    node.path("what_missing").asText(""),
                    node.path("next_action_adjustment").asText(""));
        } catch (Exception e) {
            // 非 JSON 输出：按关键词推断状态
            String lower = llmOutput.toLowerCase();
            Status st = lower.contains("stall") || lower.contains("停滞") || lower.contains("失败")
                    ? Status.stalled
                    : (lower.contains("devi") || lower.contains("偏离") ? Status.deviating : Status.on_track);
            return new ReflectionResult(st, "", "", llmOutput.length() > 200 ? llmOutput.substring(0, 200) : llmOutput);
        }
    }
}
