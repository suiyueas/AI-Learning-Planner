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
 * 智能学习助手（工具编排）
 * 一键串联 文档摘要 → 知识点提取 → 生成测验题 的完整学习闭环，
 * 输出结构化学习报告与下一步学习建议。
 */
@Component
@Slf4j
public class LearningAssistantTool extends AbstractAiTool {

    private final DocumentSummaryTool documentSummaryTool;
    private final KnowledgeExtractionTool knowledgeExtractionTool;
    private final QuizGenerationTool quizGenerationTool;

    public LearningAssistantTool(ModelManager modelManager, ObjectMapper objectMapper,
                                 DocumentSummaryTool documentSummaryTool,
                                 KnowledgeExtractionTool knowledgeExtractionTool,
                                 QuizGenerationTool quizGenerationTool,
                                 McpAiProperties properties) {
        super(modelManager, objectMapper, properties);
        this.documentSummaryTool = documentSummaryTool;
        this.knowledgeExtractionTool = knowledgeExtractionTool;
        this.quizGenerationTool = quizGenerationTool;
    }

    @Override
    protected String toolId() {
        return "learning_assistant";
    }

    public Map<String, Object> execute(Map<String, Object> params, AiToolContext ctx) {
        String document = paramString(params, "document", "");
        String documentId = paramString(params, "documentId", "");
        String questionType = paramString(params, "questionType", "mixed");
        int quizCount = paramInt(params, "quizCount", 5, 1, 10);

        if (document.isBlank() && documentId.isBlank()) {
            return error("请提供学习材料：传入document（内容）或documentId（知识库文档ID）");
        }

        long startTime = System.currentTimeMillis();
        Map<String, Object> steps = new HashMap<>();
        List<String> pipeline = new ArrayList<>();

        // ===== Step 1: 文档摘要 =====
        Map<String, Object> summaryParams = new HashMap<>(params);
        summaryParams.put("length", "medium");
        Map<String, Object> summaryResult = documentSummaryTool.execute(summaryParams, ctx);
        steps.put("summary", summaryResult);
        pipeline.add("summarize_document");

        // ===== Step 2: 知识点提取（基于摘要，控制输入规模） =====
        Object summaryObj = summaryResult.get("summary");
        String material = summaryObj != null ? summaryObj.toString() : document;
        if (material.isBlank()) {
            material = document;
        }
        Map<String, Object> extractParams = new HashMap<>();
        extractParams.put("text", material);
        extractParams.put("count", 10);
        Map<String, Object> extractResult = knowledgeExtractionTool.execute(extractParams, ctx);
        steps.put("knowledgePoints", extractResult);
        pipeline.add("extract_keywords");

        // ===== Step 3: 生成测验题（基于提取的知识点） =====
        Object pointsObj = extractResult.get("knowledgePoints");
        String quizTopic;
        if (pointsObj instanceof List<?> points && !points.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object point : points) {
                if (point instanceof Map<?, ?> pm && pm.get("keyword") != null) {
                    sb.append(pm.get("keyword")).append("：").append(pm.get("description") == null ? "" : pm.get("description")).append("\n");
                }
            }
            quizTopic = sb.length() > 0 ? sb.toString() : material;
        } else {
            quizTopic = material;
        }
        Map<String, Object> quizParams = new HashMap<>();
        quizParams.put("topic", quizTopic);
        quizParams.put("questionType", questionType);
        quizParams.put("count", quizCount);
        Map<String, Object> quizResult = quizGenerationTool.execute(quizParams, ctx);
        steps.put("quiz", quizResult);
        pipeline.add("generate_quiz");

        // ===== Step 4: 组装学习报告 =====
        long elapsed = System.currentTimeMillis() - startTime;
        Map<String, Object> result = new HashMap<>();
        result.put("pipeline", pipeline);
        result.put("steps", steps);
        result.put("nextSteps", buildNextSteps(summaryResult, extractResult, quizResult));
        result.put("executionTimeMs", elapsed);
        result.put("message", "学习报告生成完成（摘要 → 知识点 → 测验题）");
        return result;
    }

    /** 基于各步骤结果生成下一步学习建议 */
    private List<String> buildNextSteps(Map<String, Object> summary, Map<String, Object> extract,
                                        Map<String, Object> quiz) {
        List<String> nextSteps = new ArrayList<>();

        Object fallbackFlag = summary.get("fallback");
        if (Boolean.TRUE.equals(fallbackFlag)) {
            nextSteps.add("摘要由降级方案生成，建议检查 LLM 服务配置后重试");
        }

        Object graphLinked = extract.get("graphLinkedCount");
        if (graphLinked instanceof Number n && n.intValue() > 0) {
            nextSteps.add("已关联知识图谱中的 " + n.intValue() + " 个既有节点，可前往知识图谱深入学习");
        }

        Object quizCount = quiz.get("count");
        if (quizCount instanceof Number n && n.intValue() > 0) {
            nextSteps.add("完成 " + n.intValue() + " 道测验题自测后，建议对错题对应的知识点进行复习");
        }

        nextSteps.add("可调用 query_knowledge_graph 查询相关知识点依赖，规划下一步学习路径");
        if (nextSteps.size() > 3) {
            return nextSteps.subList(0, 3);
        }
        return nextSteps;
    }

    private Map<String, Object> error(String message) {
        log.warn("[learning_assistant] {}", message);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
