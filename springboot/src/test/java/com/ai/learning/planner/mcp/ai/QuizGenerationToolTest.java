package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.service.ModelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 测验题生成工具测试
 * 验证：多题型支持、Few-shot 输出解析、无效题目过滤、降级方案
 */
class QuizGenerationToolTest {

    private ModelManager modelManager;
    private ChatClient chatClient;
    private QuizGenerationTool tool;

    @BeforeEach
    void setUp() {
        modelManager = mock(ModelManager.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(modelManager.createChatClient(anyString())).thenReturn(chatClient);
        tool = new QuizGenerationTool(modelManager, new ObjectMapper(), new McpAiProperties());
    }

    @Test
    void execute_parsesValidQuestions() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("""
                        [{"question": "Java中定义常量的关键字？", "options": ["static","final","const"], "answer": "final", "explanation": "final修饰的变量只能赋值一次。", "type": "choice"},
                         {"question": "Python是编译型语言。", "options": ["对","错"], "answer": "错", "explanation": "Python是解释型语言。", "type": "judgment"}]
                        """);

        Map<String, Object> params = new HashMap<>();
        params.put("topic", "编程基础");
        params.put("questionType", "mixed");
        params.put("count", 2);
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> questions = (List<?>) result.get("questions");
        assertEquals(2, questions.size());
        Map<?, ?> first = (Map<?, ?>) questions.get(0);
        assertEquals("final", first.get("answer"));
        assertEquals("choice", first.get("type"));
        assertEquals("mixed", result.get("questionType"));
        assertNull(result.get("fallback"));
    }

    @Test
    void execute_filtersInvalidQuestions_missingAnswer() {
        // 第二题缺 answer 应被过滤
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("""
                        [{"question": "有效题目", "options": ["A","B"], "answer": "A", "explanation": "x", "type": "choice"},
                         {"question": "缺答案的无效题目", "options": ["A","B"], "explanation": "x", "type": "choice"}]
                        """);

        Map<String, Object> params = new HashMap<>();
        params.put("topic", "测试");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> questions = (List<?>) result.get("questions");
        assertEquals(1, questions.size(), "缺答案的题目应被过滤");
    }

    @Test
    void execute_chineseQuestionType_normalized() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("[{\"question\":\"判断题\",\"options\":[\"对\",\"错\"],\"answer\":\"对\",\"explanation\":\"x\",\"type\":\"judgment\"}]");

        Map<String, Object> params = new HashMap<>();
        params.put("topic", "测试");
        params.put("questionType", "判断题");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());
        assertEquals("judgment", result.get("questionType"));
    }

    @Test
    void execute_llmFails_returnsFallbackQuiz() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("无法生成");

        Map<String, Object> params = new HashMap<>();
        params.put("topic", "数据结构");
        params.put("questionType", "choice");
        params.put("count", 3);
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> questions = (List<?>) result.get("questions");
        assertFalse(questions.isEmpty(), "降级方案应返回题目");
        Map<?, ?> q = (Map<?, ?>) questions.get(0);
        assertNotNull(q.get("question"));
        assertNotNull(q.get("answer"));
        assertNotNull(q.get("explanation"), "每道题必须带解析");
        assertEquals(Boolean.TRUE, result.get("fallback"));
    }

    @Test
    void execute_emptyTopic_returnsError() {
        Map<String, Object> result = tool.execute(new HashMap<>(), AiToolContext.anonymous());
        assertEquals(Boolean.FALSE, result.get("success"));
    }

    @Test
    void execute_count_limitedToMax() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("[]");

        Map<String, Object> params = new HashMap<>();
        params.put("topic", "测试");
        params.put("count", 999);
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());
        // count 超过上限时按 10 处理，且不会异常
        assertNotNull(result.get("questions"));
    }
}
