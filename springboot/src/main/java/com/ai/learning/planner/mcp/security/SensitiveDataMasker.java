package com.ai.learning.planner.mcp.security;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 敏感信息脱敏器
 * 在日志和调试输出中自动遮盖 Authorization、API_KEY、password 等字段，替换为 [REDACTED]
 */
public final class SensitiveDataMasker {

    /** 敏感字段名（大小写不敏感，支持 snake_case / kebab-case / camelCase） */
    private static final List<Pattern> SENSITIVE_KEYS = List.of(
            Pattern.compile("(?i)^(authorization|auth|api[_-]?key|apikey|password|passwd|pwd|secret|token|access[_-]?token|refresh[_-]?token|private[_-]?key)$"),
            Pattern.compile("(?i)(.*(key|secret|token|password).*)$")
    );

    /** 文本中的 Bearer / Basic 凭证模式 */
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(Bearer\\s+|Basic\\s+)([A-Za-z0-9._~+/=-]{8,})");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(?i)(\"?[a-z0-9_\\-]*(?:key|token|password|secret|authorization)\"?\\s*[:=]\\s*\"?)([^\\s,}\"']+)");

    private SensitiveDataMasker() {
    }

    /**
     * 遮盖 Map 中的敏感字段（原地不可变，返回新 Map 的浅拷贝视图）
     */
    public static Map<String, Object> mask(Map<String, Object> map) {
        if (map == null) return map;
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        map.forEach((k, v) -> result.put(k, isSensitiveKey(k) ? "[REDACTED]" : v));
        return result;
    }

    /**
     * 遮盖任意文本中的凭证模式
     */
    public static String mask(String text) {
        if (text == null || text.isEmpty()) return text;
        String masked = BEARER_PATTERN.matcher(text).replaceAll("$1[REDACTED]");
        masked = KEY_VALUE_PATTERN.matcher(masked).replaceAll("$1[REDACTED]");
        return masked;
    }

    /**
     * 判断字段名是否为敏感字段
     */
    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isEmpty()) return false;
        return SENSITIVE_KEYS.stream().anyMatch(p -> p.matcher(key).matches());
    }
}
