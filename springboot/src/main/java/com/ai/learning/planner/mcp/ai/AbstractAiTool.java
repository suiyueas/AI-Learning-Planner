package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.service.ModelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 工具基类
 * 提供 ChatClient 获取（按 default-model 路由）、JSON 结构化解析、
 * 降级计数与内容截断等公共能力
 */
@Slf4j
public abstract class AbstractAiTool {

    /** 全局降级触发次数（用于降级触发率监控） */
    private static final AtomicLong FALLBACK_TOTAL = new AtomicLong(0);

    protected final ModelManager modelManager;
    protected final ObjectMapper objectMapper;
    protected final McpAiProperties properties;

    protected AbstractAiTool(ModelManager modelManager, ObjectMapper objectMapper, McpAiProperties properties) {
        this.modelManager = modelManager;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** 全局降级触发次数 */
    public static long getFallbackTotalCount() {
        return FALLBACK_TOTAL.get();
    }

    /** AI 工具层是否启用 */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** 记录一次降级触发 */
    protected void markFallback() {
        FALLBACK_TOTAL.incrementAndGet();
    }

    /** 当前 AI 工具使用的模型显示名 */
    public String getModelDisplayName() {
        try {
            return modelManager.getModelDisplayNameByShortName(properties.getDefaultModel());
        } catch (Exception e) {
            return properties.getDefaultModel();
        }
    }

    /** 创建 ChatClient（按 default-model 路由），失败返回 empty */
    protected Optional<ChatClient> getChatClient() {
        try {
            return Optional.ofNullable(modelManager.createChatClient(properties.getDefaultModel()));
        } catch (Exception e) {
            log.warn("创建 ChatClient 失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 调用大模型获取纯文本回复
     *
     * @param prompt 提示词
     * @return 回复内容（调用失败或为空返回 empty）
     */
    protected Optional<String> callLlm(String prompt) {
        if (!properties.isEnabled()) {
            log.warn("[{}] AI 工具层已禁用（app.mcp.ai.enabled=false），跳过 LLM 调用", toolId());
            return Optional.empty();
        }
        Optional<ChatClient> clientOpt = getChatClient();
        if (clientOpt.isEmpty()) {
            return Optional.empty();
        }
        long start = System.currentTimeMillis();
        try {
            String content = clientOpt.get().prompt().user(prompt).call().content();
            if (content == null || content.isBlank()) {
                log.warn("[{}] LLM 返回空内容", toolId());
                return Optional.empty();
            }
            log.debug("[{}] LLM 调用完成，耗时 {}ms", toolId(), System.currentTimeMillis() - start);
            return Optional.of(content.trim());
        } catch (Exception e) {
            log.warn("[{}] LLM 调用失败（{}ms）: {}", toolId(), System.currentTimeMillis() - start, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 从 LLM 回复中提取 JSON 数组字符串
     * 兼容 ```json ... ``` markdown 包裹、前后缀文本等情况
     */
    protected String extractJsonArrayText(String raw) {
        if (raw == null || raw.isBlank()) return "[]";
        String trimmed = raw.trim();

        int codeBlockStart = trimmed.indexOf("```");
        if (codeBlockStart != -1) {
            int contentStart = trimmed.indexOf('\n', codeBlockStart) + 1;
            int codeBlockEnd = trimmed.lastIndexOf("```");
            if (contentStart > 0 && codeBlockEnd > contentStart) {
                trimmed = trimmed.substring(contentStart, codeBlockEnd).trim();
            }
        }

        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart != -1 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }
        return "[]";
    }

    /**
     * 从 LLM 回复中提取 JSON 对象字符串
     */
    protected String extractJsonObjectText(String raw) {
        if (raw == null || raw.isBlank()) return "{}";
        String trimmed = raw.trim();

        int codeBlockStart = trimmed.indexOf("```");
        if (codeBlockStart != -1) {
            int contentStart = trimmed.indexOf('\n', codeBlockStart) + 1;
            int codeBlockEnd = trimmed.lastIndexOf("```");
            if (contentStart > 0 && codeBlockEnd > contentStart) {
                trimmed = trimmed.substring(contentStart, codeBlockEnd).trim();
            }
        }

        int objStart = trimmed.indexOf('{');
        int objEnd = trimmed.lastIndexOf('}');
        if (objStart != -1 && objEnd > objStart) {
            return trimmed.substring(objStart, objEnd + 1);
        }
        return "{}";
    }

    /** 解析 LLM 返回的 JSON 数组，失败返回空列表 */
    protected List<Map<String, Object>> parseJsonArray(String raw) {
        try {
            String json = extractJsonArrayText(raw);
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("[{}] 解析 JSON 数组失败: {}", toolId(), e.getMessage());
            return List.of();
        }
    }

    /** 解析 LLM 返回的 JSON 对象，失败返回空 Map */
    protected Map<String, Object> parseJsonObject(String raw) {
        try {
            String json = extractJsonObjectText(raw);
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[{}] 解析 JSON 对象失败: {}", toolId(), e.getMessage());
            return Map.of();
        }
    }

    protected String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    /** 参数安全取值 */
    protected String paramString(Map<String, Object> params, String key, String defaultValue) {
        Object v = params.get(key);
        if (v == null || v.toString().isBlank()) return defaultValue;
        return v.toString().trim();
    }

    protected int paramInt(Map<String, Object> params, String key, int defaultValue, int min, int max) {
        int v = params.get(key) instanceof Number n ? n.intValue() : defaultValue;
        return Math.max(min, Math.min(max, v));
    }

    protected boolean paramBool(Map<String, Object> params, String key, boolean defaultValue) {
        Object v = params.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(v.toString()) || "1".equals(v.toString()) || "yes".equalsIgnoreCase(v.toString());
    }

    /** 工具ID（子类实现，用于日志标识） */
    protected abstract String toolId();
}
