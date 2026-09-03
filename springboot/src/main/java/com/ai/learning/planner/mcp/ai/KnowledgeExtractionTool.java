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
 * 知识点提取工具（AI 赋能）
 * 由大模型从学习材料中提取结构化知识点（关键词+定义+分类+重要度）。
 */
@Component
@Slf4j
public class KnowledgeExtractionTool extends AbstractAiTool {

    private static final int MAX_CONTENT_LENGTH = 8000;

    public KnowledgeExtractionTool(ModelManager modelManager, ObjectMapper objectMapper,
                                   McpAiProperties properties) {
        super(modelManager, objectMapper, properties);
    }

    @Override
    protected String toolId() {
        return "extract_keywords";
    }

    public Map<String, Object> execute(Map<String, Object> params, AiToolContext ctx) {
        String text = paramString(params, "text", "");
        String domain = paramString(params, "domain", "");
        int count = paramInt(params, "count", 10, 1, 20);

        if (text.isBlank()) {
            return error("学习材料内容不能为空");
        }

        // 1. 调用大模型提取结构化知识点
        String prompt = buildPrompt(text, domain, count);
        List<Map<String, Object>> knowledgePoints = new ArrayList<>();

        callLlm(prompt).ifPresent(raw -> {
            List<Map<String, Object>> parsed = parseJsonArray(raw);
            if (!parsed.isEmpty()) {
                knowledgePoints.addAll(parsed);
            }
        });

        // 2. LLM 失败时降级：按标点切分短句作为知识点
        boolean fallback = knowledgePoints.isEmpty();
        if (fallback) {
            log.warn("[extract_keywords] LLM 提取为空，使用降级方案");
            markFallback();
            knowledgePoints.addAll(fallbackExtract(text, count));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("knowledgePoints", knowledgePoints);
        result.put("count", knowledgePoints.size());
        result.put("totalRequested", count);
        result.put("domain", domain.isBlank() ? null : domain);
        result.put("suggestedNextTools", List.of("generate_quiz", "summarize_document"));
        result.put("message", "知识点提取成功");
        if (fallback) {
            result.put("fallback", true);
        }
        return result;
    }

    private String buildPrompt(String text, String domain, int count) {
        String domainHint = domain.isBlank() ? "自动判断" : domain;
        return """
                你是%s领域的知识分析专家。请从以下学习材料中提取 %d 个核心知识点。
                每个知识点必须包含：
                - keyword：知识点名称（术语）
                - description：一句话定义/解释（不超过50字）
                - category：所属分类（如 概念/原理/方法/工具/案例）
                - importance：重要度 1-5（数字，5为最重要）

                示例：
                [{"keyword": "面向对象编程", "description": "以对象为核心组织代码的编程范式", "category": "概念", "importance": 5}]

                只返回 JSON 数组（不要任何其他文字）。

                学习材料：
                %s
                """.formatted(domainHint, count, truncate(text, MAX_CONTENT_LENGTH));
    }

    /** 降级方案：按标点切分短句提取知识点 */
    private List<Map<String, Object>> fallbackExtract(String text, int count) {
        List<Map<String, Object>> result = new ArrayList<>();
        String clean = text.replaceAll("<[^>]+>", "").replaceAll("[\\r\\n]+", "。");
        String[] sentences = clean.split("[。；;！!？?]");
        for (String sentence : sentences) {
            if (result.size() >= count) break;
            String trimmed = sentence.trim();
            if (trimmed.length() < 4 || trimmed.length() > 60) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("keyword", trimmed.length() > 20 ? trimmed.substring(0, 20) + "…" : trimmed);
            item.put("description", "");
            item.put("category", "未知");
            item.put("importance", 3);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> error(String message) {
        log.warn("[extract_keywords] {}", message);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
