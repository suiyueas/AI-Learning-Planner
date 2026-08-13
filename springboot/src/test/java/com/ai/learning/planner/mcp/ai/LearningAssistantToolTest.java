package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.service.ModelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 智能学习助手编排工具测试
 * 验证：摘要→知识点→测验题 的完整闭环串联、参数透传、下一步建议
 */
class LearningAssistantToolTest {

    private DocumentSummaryTool summaryTool;
    private KnowledgeExtractionTool extractTool;
    private QuizGenerationTool quizTool;
    private LearningAssistantTool tool;

    @BeforeEach
    void setUp() {
        summaryTool = mock(DocumentSummaryTool.class);
        extractTool = mock(KnowledgeExtractionTool.class);
        quizTool = mock(QuizGenerationTool.class);
        ModelManager modelManager = mock(ModelManager.class);
        tool = new LearningAssistantTool(modelManager, new ObjectMapper(), summaryTool, extractTool, quizTool, new McpAiProperties());
    }

    @Test
    void execute_runsFullPipeline() {
        // 摘要结果
        when(summaryTool.execute(anyMap(), any(AiToolContext.class))).thenAnswer(inv -> {
            Map<String, Object> r = new HashMap<>();
            r.put("summary", "这是文档摘要");
            r.put("fallback", false);
            return r;
        });
        // 知识点结果（含知识图谱关联）
        when(extractTool.execute(anyMap(), any(AiToolContext.class))).thenAnswer(inv -> {
            Map<String, Object> r = new HashMap<>();
            r.put("knowledgePoints", List.of(
                    Map.of("keyword", "Spring Boot", "description", "Java框架"),
                    Map.of("keyword", "依赖注入", "description", "设计模式")
            ));
            r.put("graphLinkedCount", 2);
            return r;
        });
        // 测验题结果
        when(quizTool.execute(anyMap(), any(AiToolContext.class))).thenAnswer(inv -> {
            Map<String, Object> r = new HashMap<>();
            r.put("questions", List.of(Map.of("question", "q1")));
            r.put("count", 1);
            return r;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("document", "这是一份学习材料。");
        params.put("quizCount", 3);
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        // 1. 三个子工具按顺序被调用
        verify(summaryTool).execute(anyMap(), any(AiToolContext.class));
        verify(extractTool).execute(anyMap(), any(AiToolContext.class));
        verify(quizTool).execute(anyMap(), any(AiToolContext.class));

        // 2. 流水线记录
        assertEquals(List.of("summarize_document", "extract_keywords", "generate_quiz"), result.get("pipeline"));

        // 3. 结果包含各步骤
        Map<?, ?> steps = (Map<?, ?>) result.get("steps");
        assertTrue(steps.containsKey("summary"));
        assertTrue(steps.containsKey("knowledgePoints"));
        assertTrue(steps.containsKey("quiz"));

        // 4. 下一步建议非空
        List<?> nextSteps = (List<?>) result.get("nextSteps");
        assertFalse(nextSteps.isEmpty(), "应生成下一步学习建议");
        assertTrue(nextSteps.stream().anyMatch(s -> s.toString().contains("知识图谱")));
    }

    @Test
    void execute_quizUsesExtractedKeywordsAsTopic() {
        when(summaryTool.execute(anyMap(), any(AiToolContext.class))).thenReturn(Map.of("summary", "摘要内容"));
        when(extractTool.execute(anyMap(), any(AiToolContext.class))).thenReturn(Map.of(
                "knowledgePoints", List.of(Map.of("keyword", "Python", "description", "编程语言"))));
        when(quizTool.execute(anyMap(), any(AiToolContext.class))).thenReturn(Map.of("questions", List.of(), "count", 0));

        Map<String, Object> params = new HashMap<>();
        params.put("document", "Python 教程内容");
        tool.execute(params, AiToolContext.anonymous());

        // 验证 quiz 的 topic 来自提取的知识点
        verify(quizTool).execute(argThat(p -> p.containsKey("topic") && p.get("topic").toString().contains("Python")),
                any(AiToolContext.class));
    }

    @Test
    void execute_missingInput_returnsError() {
        Map<String, Object> result = tool.execute(new HashMap<>(), AiToolContext.anonymous());
        assertEquals(Boolean.FALSE, result.get("success"));
        // 未传材料时不应调用任何子工具
        verify(summaryTool, never()).execute(anyMap(), any(AiToolContext.class));
    }

    @Test
    void execute_passesQuestionTypeAndQuizCount() {
        when(summaryTool.execute(anyMap(), any(AiToolContext.class))).thenReturn(Map.of("summary", "s"));
        when(extractTool.execute(anyMap(), any(AiToolContext.class))).thenReturn(Map.of("knowledgePoints", List.of()));
        when(quizTool.execute(anyMap(), any(AiToolContext.class))).thenReturn(Map.of("questions", List.of(), "count", 0));

        Map<String, Object> params = new HashMap<>();
        params.put("documentId", "doc-123");
        params.put("questionType", "choice");
        params.put("quizCount", 8);
        tool.execute(params, AiToolContext.anonymous());

        // 参数透传到测验生成
        verify(quizTool).execute(argThat(p -> "choice".equals(p.get("questionType")) && Integer.valueOf(8).equals(p.get("count"))),
                any(AiToolContext.class));
        // documentId 透传到摘要
        verify(summaryTool).execute(argThat(p -> "doc-123".equals(p.get("documentId"))), any(AiToolContext.class));
    }
}
