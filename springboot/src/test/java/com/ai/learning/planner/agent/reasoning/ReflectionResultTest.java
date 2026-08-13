package com.ai.learning.planner.agent.reasoning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 反思结果 JSON 序列化/解析测试
 */
class ReflectionResultTest {

    @Test
    void toJson_containsRequiredFields() {
        ReflectionResult r = new ReflectionResult(ReflectionResult.Status.deviating,
                "完成工具调用", "缺少关键数据", "补充数据后重试");
        String json = r.toJson();
        assertTrue(json.contains("\"status\""));
        assertTrue(json.contains("\"what_worked\""));
        assertTrue(json.contains("\"what_missing\""));
        assertTrue(json.contains("\"next_action_adjustment\""));
        assertTrue(json.contains("deviating"));
        assertTrue(json.contains("补充数据后重试"));
    }

    @Test
    void parse_validJson() {
        String json = """
                {
                  "status": "on_track",
                  "what_worked": "步骤A完成",
                  "what_missing": "验证结果",
                  "next_action_adjustment": "下一步验证"
                }
                """;
        ReflectionResult r = ReflectionResult.parse(json);
        assertEquals(ReflectionResult.Status.on_track, r.status());
        assertEquals("步骤A完成", r.whatWorked());
        assertEquals("下一步验证", r.nextActionAdjustment());
    }

    @Test
    void parse_stalledByKeywords() {
        ReflectionResult r = ReflectionResult.parse("推理停滞，连续失败无法推进");
        assertEquals(ReflectionResult.Status.stalled, r.status());
    }

    @Test
    void parse_deviatingByKeywords() {
        ReflectionResult r = ReflectionResult.parse("当前方向偏离目标");
        assertEquals(ReflectionResult.Status.deviating, r.status());
    }

    @Test
    void parse_emptyReturnsStalled() {
        ReflectionResult r = ReflectionResult.parse("");
        assertEquals(ReflectionResult.Status.stalled, r.status());
    }

    @Test
    void parse_nullReturnsStalled() {
        ReflectionResult r = ReflectionResult.parse(null);
        assertEquals(ReflectionResult.Status.stalled, r.status());
    }
}
