package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
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
 * 文档摘要工具测试
 * 验证：LLM 正常输出、降级方案、参数校验、关键词与阅读时间估算
 */
class DocumentSummaryToolTest {

    private ModelManager modelManager;
    private ChatClient chatClient;
    private KnowledgeDocumentRepository documentRepository;
    private DocumentSummaryTool tool;

    @BeforeEach
    void setUp() {
        modelManager = mock(ModelManager.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        documentRepository = mock(KnowledgeDocumentRepository.class);
        when(modelManager.createChatClient(anyString())).thenReturn(chatClient);
        tool = new DocumentSummaryTool(modelManager, new ObjectMapper(), documentRepository, new McpAiProperties());
    }

    @Test
    void execute_withValidContent_returnsSummaryKeywordsAndReadTime() {
        // LLM 返回标准 JSON 对象
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("{\"summary\": \"这是关于Java集合框架的摘要内容。\", \"keywords\": [\"Java\", \"集合\", \"HashMap\"]}");

        Map<String, Object> params = new HashMap<>();
        params.put("content", "Java集合框架包含List、Set、Map等接口，是日常开发最常用的数据结构库。");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertTrue(Boolean.TRUE.equals(result.get("success")) || !result.containsKey("success"));
        assertEquals("这是关于Java集合框架的摘要内容。", result.get("summary"));
        assertNotNull(result.get("keywords"));
        assertEquals(3, ((List<?>) result.get("keywords")).size());
        assertNotNull(result.get("readTimeEstimate"));
        assertEquals("medium", result.get("length"));
        // 结果应包含下一步建议
        assertTrue(((List<?>) result.get("suggestedNextTools")).contains("generate_quiz"));
        assertNull(result.get("fallback"), "LLM 正常输出时不应标记降级");
    }

    @Test
    void execute_llmReturnsMarkdownWrappedJson_stillParsed() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("好的，以下是摘要：\n```json\n{\"summary\": \"摘要文本\", \"keywords\": [\"A\", \"B\"]}\n```");

        Map<String, Object> params = new HashMap<>();
        params.put("content", "测试内容");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertEquals("摘要文本", result.get("summary"));
        assertEquals(2, ((List<?>) result.get("keywords")).size());
    }

    @Test
    void execute_llmFails_returnsFallbackSummary() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("这不是JSON，只是普通文字回复");

        Map<String, Object> params = new HashMap<>();
        params.put("content", "这是一段用于测试降级方案的学习文档内容，包含核心概念与实现细节。");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertNotNull(result.get("summary"));
        assertTrue(result.get("summary").toString().contains("这是一段用于测试降级方案的学习文档内容"),
                "降级摘要应保留原文内容");
        assertEquals(Boolean.TRUE, result.get("fallback"));
    }

    @Test
    void execute_llmThrows_fallsBack() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("API 超时"));

        Map<String, Object> params = new HashMap<>();
        params.put("content", "测试降级内容");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertNotNull(result.get("summary"));
        assertEquals(Boolean.TRUE, result.get("fallback"));
    }

    @Test
    void execute_emptyContent_returnsError() {
        Map<String, Object> result = tool.execute(new HashMap<>(), AiToolContext.anonymous());
        assertEquals(Boolean.FALSE, result.get("success"));
        assertNotNull(result.get("message"));
    }

    @Test
    void execute_shortLength_normalized() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("{\"summary\": \"简短摘要\", \"keywords\": []}");

        Map<String, Object> params = new HashMap<>();
        params.put("content", "内容");
        params.put("length", "简短"); // 兼容中文旧参数
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());
        assertEquals("short", result.get("length"));
    }
}
