package com.ai.learning.planner.mcp.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感信息脱敏测试
 */
class SensitiveDataMaskerTest {

    @Test
    void mask_bearerTokenRedacted() {
        String masked = SensitiveDataMasker.mask("Authorization: Bearer sk-abc1234567890def");
        assertTrue(masked.contains("[REDACTED]"));
        assertFalse(masked.contains("sk-abc1234567890def"));
    }

    @Test
    void mask_keyValuePatternRedacted() {
        String masked = SensitiveDataMasker.mask("api_key=sk-xxx-secret-123 password=pass123");
        assertTrue(masked.contains("[REDACTED]"));
        assertFalse(masked.contains("sk-xxx-secret-123"));
        assertFalse(masked.contains("pass123"));
    }

    @Test
    void mask_mapSensitiveKeysRedacted() {
        Map<String, Object> input = Map.of(
                "api_key", "sk-secret",
                "password", "pwd123",
                "query", "hello",
                "Authorization", "Bearer tok123"
        );
        Map<String, Object> masked = SensitiveDataMasker.mask(input);
        assertEquals("[REDACTED]", masked.get("api_key"));
        assertEquals("[REDACTED]", masked.get("password"));
        assertEquals("[REDACTED]", masked.get("Authorization"));
        assertEquals("hello", masked.get("query"));
    }

    @Test
    void mask_normalTextUntouched() {
        String text = "查询知识图谱节点，返回结果列表";
        assertEquals(text, SensitiveDataMasker.mask(text));
    }

    @Test
    void mask_nullSafe() {
        assertNull(SensitiveDataMasker.mask((Map<String, Object>) null));
        assertNull(SensitiveDataMasker.mask((String) null));
        assertEquals("", SensitiveDataMasker.mask(""));
    }

    @Test
    void isSensitiveKey_matchesVariants() {
        assertTrue(SensitiveDataMasker.isSensitiveKey("api_key"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("API_KEY"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("apiKey"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("password"));
        assertTrue(SensitiveDataMasker.isSensitiveKey("access_token"));
        assertFalse(SensitiveDataMasker.isSensitiveKey("query"));
        assertFalse(SensitiveDataMasker.isSensitiveKey(null));
    }
}
