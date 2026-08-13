package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.entity.KnowledgeNode;
import com.ai.learning.planner.service.KnowledgeService;
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
 * 验证：结构化知识点输出、知识图谱关联、降级方案
 */
class KnowledgeExtractionToolTest {

    private ModelManager modelManager;
    private ChatClient chatClient;
    private KnowledgeService knowledgeService;
    private KnowledgeExtractionTool tool;

    @BeforeEach
    void setUp() {
        modelManager = mock(ModelManager.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        knowledgeService = mock(KnowledgeService.class);
        when(modelManager.createChatClient(anyString())).thenReturn(chatClient);
        tool = new KnowledgeExtractionTool(modelManager, new ObjectMapper(), knowledgeService, new McpAiProperties());
    }

    @Test
    void execute_parsesStructuredKnowledgePoints() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("""
                        [{"keyword": "Spring Boot", "description": "简化Spring应用的开发框架", "category": "框架", "importance": 5},
                         {"keyword": "依赖注入", "description": "由容器管理对象依赖关系的模式", "category": "概念", "importance": 4}]
                        """);
        // 知识图谱无关联节点
        when(knowledgeService.searchByName(anyString())).thenReturn(List.of());

        Map<String, Object> params = new HashMap<>();
        params.put("text", "Spring Boot 基于依赖注入实现自动配置。");
        params.put("domain", "Java");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> points = (List<?>) result.get("knowledgePoints");
        assertEquals(2, points.size());
        Map<?, ?> first = (Map<?, ?>) points.get(0);
        assertEquals("Spring Boot", first.get("keyword"));
        assertTrue(first.containsKey("importance"));
        assertTrue(first.containsKey("relatedNodes"), "每个知识点应附带知识图谱关联结果");
        assertEquals("Java", result.get("domain"));
        assertEquals(0, result.get("graphLinkedCount"));
    }

    @Test
    void execute_linksRelatedGraphNodes() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("[{\"keyword\": \"Python\", \"description\": \"解释型编程语言\", \"category\": \"语言\", \"importance\": 5}]");
        // 知识图谱中存在匹配节点
        KnowledgeNode node = KnowledgeNode.builder()
                .id("python-101").name("Python入门").category("编程语言").difficulty(2)
                .build();
        when(knowledgeService.searchByName("Python")).thenReturn(List.of(node));

        Map<String, Object> params = new HashMap<>();
        params.put("text", "Python 是一种解释型语言。");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> points = (List<?>) result.get("knowledgePoints");
        Map<?, ?> point = (Map<?, ?>) points.get(0);
        List<?> related = (List<?>) point.get("relatedNodes");
        assertEquals(1, related.size(), "应关联到知识图谱节点");
        assertEquals(1, result.get("graphLinkedCount"));
    }

    @Test
    void execute_llmFails_returnsFallbackPoints() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("无法提取");
        when(knowledgeService.searchByName(anyString())).thenReturn(List.of());

        Map<String, Object> params = new HashMap<>();
        params.put("text", "面向对象编程。封装继承多态。这是核心内容。");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        List<?> points = (List<?>) result.get("knowledgePoints");
        assertFalse(points.isEmpty(), "降级方案应提取出知识点");
        assertEquals(Boolean.TRUE, result.get("fallback"));
    }

    @Test
    void execute_emptyText_returnsError() {
        Map<String, Object> result = tool.execute(new HashMap<>(), AiToolContext.anonymous());
        assertEquals(Boolean.FALSE, result.get("success"));
    }
}
