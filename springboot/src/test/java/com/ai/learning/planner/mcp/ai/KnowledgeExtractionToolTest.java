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
 * 知识点提取工具测试
 * 验证：结构化知识点输出、降级方案
 */
class KnowledgeExtractionToolTest {

    private ModelManager modelManager;
    private ChatClient chatClient;
    private KnowledgeExtractionTool tool;

    @BeforeEach
    void setUp() {
        modelManager = mock(ModelManager.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(modelManager.createChatClient(anyString())).thenReturn(chatClient);
        tool = new KnowledgeExtractionTool(modelManager, new ObjectMapper(), new McpAiProperties());
    }

    @Test
    void execute_parsesStructuredKnowledgePoints() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("""
                        [{"keyword": "Spring Boot", "description": "简化Spring应用的开发框架", "category": "框架", "importance": 5},
                         {"keyword": "依赖注入", "description": "由容器管理对象依赖关系的模式", "category": "概念", "importance": 4}]
                        """);

        Map<String, Object> params = new HashMap<>();
        params.put("text", "Spring Boot 基于依赖注入实现自动配置。");
        params.put("domain", "Java");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> points = (List<?>) result.get("knowledgePoints");
        assertEquals(2, points.size());
        Map<?, ?> first = (Map<?, ?>) points.get(0);
        assertEquals("Spring Boot", first.get("keyword"));
        assertTrue(first.containsKey("importance"));
        assertEquals("Java", result.get("domain"));
    }

    @Test
    void execute_llmFails_returnsFallbackPoints() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("无法提取");

        Map<String, Object> params = new HashMap<>();
        params.put("text", "面向对象编程。封装继承多态。这是核心内容。");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> points = (List<?>) result.get("knowledgePoints");
        assertFalse(points.isEmpty(), "降级方案应提取出知识点");
        assertEquals(Boolean.TRUE, result.get("fallback"));
    }

    @Test
    void execute_emptyText_returnsError() {
        Map<String, Object> params = new HashMap<>();
        params.put("text", "");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertEquals(false, result.get("success"));
        assertTrue(result.get("message").toString().contains("不能为空"));
    }
}
