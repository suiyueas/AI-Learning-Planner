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
 * 文本翻译工具测试
 * 验证：术语保留模式的结构化输出（翻译+术语对照表）、普通模式、降级方案、旧参数兼容
 */
class TranslationToolTest {

    private ModelManager modelManager;
    private ChatClient chatClient;
    private TranslationTool tool;

    @BeforeEach
    void setUp() {
        modelManager = mock(ModelManager.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(modelManager.createChatClient(anyString())).thenReturn(chatClient);
        tool = new TranslationTool(modelManager, new ObjectMapper(), new McpAiProperties());
    }

    @Test
    void execute_preserveTechTerms_parsesTranslationAndTermMappings() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("""
                        {"translation": "Java虚拟机是运行Java字节码的运行时环境。", "termMappings": [{"source": "JVM", "translated": "Java虚拟机"}]}
                        """);

        Map<String, Object> params = new HashMap<>();
        params.put("text", "The JVM is a runtime environment that executes Java bytecode.");
        params.put("targetLang", "中文");
        params.put("preserveTechTerms", true);
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertEquals("Java虚拟机是运行Java字节码的运行时环境。", result.get("translation"));
        List<?> mappings = (List<?>) result.get("termMappings");
        assertEquals(1, mappings.size(), "应生成术语对照表");
        Map<?, ?> mapping = (Map<?, ?>) mappings.get(0);
        assertEquals("JVM", mapping.get("source"));
        assertEquals(Boolean.TRUE, result.get("preserveTechTerms"));
        assertNull(result.get("fallback"));
    }

    @Test
    void execute_plainMode_returnsRawTranslation() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("你好，世界！");

        Map<String, Object> params = new HashMap<>();
        params.put("text", "Hello World!");
        params.put("preserveTechTerms", false);
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertEquals("你好，世界！", result.get("translation"));
        assertEquals(Boolean.FALSE, result.get("preserveTechTerms"));
    }

    @Test
    void execute_llmFails_returnsFallback() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("无法翻译");

        Map<String, Object> params = new HashMap<>();
        params.put("text", "Hello");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());

        assertTrue(result.get("translation").toString().contains("Hello"));
        assertEquals(Boolean.TRUE, result.get("fallback"));
    }

    @Test
    void execute_legacyTargetLangParam_supported() {
        // 兼容旧参数 target_lang（下划线）——由 ToolExecutionService.normalizeTranslateParams 处理，
        // 工具层直接接收 targetLang
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("{\"translation\": \"你好\", \"termMappings\": []}");

        Map<String, Object> params = new HashMap<>();
        params.put("text", "Hello");
        params.put("targetLang", "中文");
        Map<String, Object> result = tool.execute(params, AiToolContext.anonymous());
        assertEquals("你好", result.get("translation"));
    }

    @Test
    void execute_emptyText_returnsError() {
        Map<String, Object> result = tool.execute(new HashMap<>(), AiToolContext.anonymous());
        assertEquals(Boolean.FALSE, result.get("success"));
    }
}
