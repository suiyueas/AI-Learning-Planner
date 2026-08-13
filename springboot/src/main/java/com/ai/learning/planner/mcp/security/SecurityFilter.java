package com.ai.learning.planner.mcp.security;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 工具注入防御过滤器
 * 所有从 MCP Server 返回的 description 字段，在注入 LLM 上下文前必须经过本过滤器清洗，
 * 移除 SYSTEM:、IGNORE 等危险指令模式及提示注入载体。
 */
public final class SecurityFilter {

    /** 危险指令模式（大小写不敏感） */
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
            Pattern.compile("(?i)\\bSYSTEM\\s*[:：]?\\s*[^\\n。；;]*", Pattern.MULTILINE),
            Pattern.compile("(?i)\\bIGNORE\\s*(?:ALL)?\\s*(?:PREVIOUS|INSTRUCTIONS|PROMPTS|ABOVE)?\\s*[^\\n。；;]*", Pattern.MULTILINE),
            Pattern.compile("(?i)\\bDISREGARD\\s*(?:ALL)?\\s*[^\\n。；;]*", Pattern.MULTILINE),
            Pattern.compile("(?i)\\bYOU ARE (?:NOW )?(?:AN? )?(?:GPT|CHATGPT|ASSISTANT|AI)[^\\n。；;]*", Pattern.MULTILINE),
            Pattern.compile("(?i)<\\|im_start\\|>|<\\|im_end\\|>|<\\|system\\|>|<\\|user\\|>|<\\|assistant\\|>"),
            Pattern.compile("(?i)<!--[\\s\\S]*?-->")
    );

    /** 检测是否含危险指令（不修改原文） */
    private static final Pattern DETECT_PATTERN = Pattern.compile(
            "(?i)(SYSTEM\\s*[:：]|IGNORE\\s+(ALL\\s+)?(PREVIOUS|INSTRUCTIONS)|DISREGARD|YOU ARE (NOW )?(A|AN) (GPT|CHATGPT|ASSISTANT|AI)|<\\|im_start\\|>|<\\|im_end\\|>|<!--)"
    );

    private SecurityFilter() {
    }

    /**
     * 清洗工具 description，移除危险指令模式
     *
     * @param description 原始描述
     * @return 清洗结果（含是否被清洗标记）
     */
    public static SanitizedResult sanitize(String description) {
        if (description == null || description.isEmpty()) {
            return new SanitizedResult("", false);
        }
        String cleaned = description;
        boolean sanitized = false;
        for (Pattern p : DANGEROUS_PATTERNS) {
            String before = cleaned;
            cleaned = p.matcher(cleaned).replaceAll("").trim();
            if (!before.equals(cleaned)) {
                sanitized = true;
            }
        }
        return new SanitizedResult(cleaned, sanitized);
    }

    /**
     * 快速检测文本是否包含可疑注入模式
     */
    public static boolean isSuspicious(String text) {
        return text != null && DETECT_PATTERN.matcher(text).find();
    }

    /**
     * 清洗结果
     */
    public record SanitizedResult(String content, boolean sanitized) {
    }
}
