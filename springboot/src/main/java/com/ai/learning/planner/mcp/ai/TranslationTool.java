package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.service.ModelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本翻译工具（AI 赋能，学习材料专用）
 * 相比传统翻译 API：可保留技术术语原文并生成术语对照表，
 * 同时保持上下文风格，适合翻译外文技术文献。
 */
@Component
@Slf4j
public class TranslationTool extends AbstractAiTool {

    private static final int MAX_CONTENT_LENGTH = 6000;

    public TranslationTool(ModelManager modelManager, ObjectMapper objectMapper, McpAiProperties properties) {
        super(modelManager, objectMapper, properties);
    }

    @Override
    protected String toolId() {
        return "translate_text";
    }

    public Map<String, Object> execute(Map<String, Object> params, AiToolContext ctx) {
        String text = paramString(params, "text", "");
        String sourceLang = paramString(params, "sourceLang", "auto");
        String targetLang = paramString(params, "targetLang", "中文");
        boolean preserveTechTerms = paramBool(params, "preserveTechTerms", true);

        if (text.isBlank()) {
            return error("待翻译文本不能为空");
        }

        // 1. 调用大模型翻译
        String prompt = buildPrompt(text, sourceLang, targetLang, preserveTechTerms);
        Map<String, Object> result = new HashMap<>();
        String translation = "";
        List<Map<String, Object>> termMappings = new ArrayList<>();

        if (preserveTechTerms) {
            // 结构化输出：翻译 + 术语对照表
            Map<String, Object> parsed = callLlm(prompt).map(this::parseJsonObject).orElse(Map.of());
            Object transObj = parsed.get("translation");
            if (transObj != null && !transObj.toString().isBlank()) {
                translation = transObj.toString().trim();
            }
            Object termsObj = parsed.get("termMappings");
            if (termsObj instanceof List<?> terms) {
                for (Object term : terms) {
                    if (term instanceof Map<?, ?> tm && tm.containsKey("source")) {
                        Map<String, Object> mapping = new HashMap<>();
                        Object source = tm.get("source");
                        Object translated = tm.get("translated");
                        mapping.put("source", source != null ? source.toString() : "");
                        mapping.put("translated", translated != null ? translated.toString() : "");
                        termMappings.add(mapping);
                    }
                }
            }
        } else {
            translation = callLlm(prompt).orElse("");
        }

        // 2. LLM 失败时降级
        boolean fallback = translation.isBlank();
        if (fallback) {
            log.warn("[translate_text] LLM 翻译为空，使用降级方案");
            markFallback();
            translation = "[降级翻译] " + text;
            result.put("fallback", true);
        }

        result.put("translation", translation);
        result.put("termMappings", termMappings);
        result.put("sourceLang", sourceLang);
        result.put("targetLang", targetLang);
        result.put("preserveTechTerms", preserveTechTerms);
        result.put("originalLength", text.length());
        result.put("suggestedNextTools", List.of("summarize_document", "extract_keywords"));
        result.put("message", "翻译成功");
        return result;
    }

    private String buildPrompt(String text, String sourceLang, String targetLang, boolean preserveTechTerms) {
        String langDesc = "auto".equalsIgnoreCase(sourceLang) ? "自动检测源语言" : "源语言：" + sourceLang;
        if (!preserveTechTerms) {
            return """
                    请将以下文本翻译成%s（%s），要求语义准确、表达自然、符合目标语言的阅读习惯。
                    直接返回翻译结果，不要添加任何解释。

                    待翻译文本：
                    %s
                    """.formatted(targetLang, langDesc, truncate(text, MAX_CONTENT_LENGTH));
        }
        return """
                你是专业的技术文献翻译专家。请将以下文本翻译成%s（%s），要求：
                1. 语义准确、表达自然，保持原文的上下文风格
                2. 专业术语（技术名词、专有名词、API名、品牌名等）保留英文原文不翻译
                3. 同时提取文中的关键术语对照表

                只返回 JSON 对象（不要任何其他文字），格式如下：
                {"translation": "翻译后的完整文本", "termMappings": [{"source": "原术语", "translated": "翻译/保留说明"}]}

                待翻译文本：
                %s
                """.formatted(targetLang, langDesc, truncate(text, MAX_CONTENT_LENGTH));
    }

    private Map<String, Object> error(String message) {
        log.warn("[translate_text] {}", message);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
