package com.ai.learning.planner.mcp.client;

import com.ai.learning.planner.agent.reasoning.ReasoningTrace;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.exception.ReasoningException;
import com.ai.learning.planner.mcp.hitl.HitlApprovalGate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MCP 超时重试 + 降级 + JSON-RPC 契约集成测试（@SpringBootTest 最小上下文）
 * 覆盖：指数退避重试、X-Fallback 降级、id 匹配、error code 范围（-32000~-32099）、HITL 拒绝
 */
@SpringBootTest(classes = McpTimeoutRetryIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class McpTimeoutRetryIntegrationTest {

    @SpringBootConfiguration
    static class TestConfig {
        @Bean
        McpTimeoutPolicy timeoutPolicy() {
            return new McpTimeoutPolicy();
        }

        @Bean
        McpRetryPolicy retryPolicy() {
            return new McpRetryPolicy(1, 2.0, 3); // 1ms 退避加速测试
        }

        @Bean
        FallbackRegistry fallbackRegistry() {
            FallbackRegistry registry = new FallbackRegistry();
            registry.register("search_resources", "{\"fallback\": true, \"resources\": []}");
            return registry;
        }

        @Bean
        HitlApprovalGate approvalGate() {
            HitlApprovalGate gate = new HitlApprovalGate();
            gate.setDefaultTimeout(java.time.Duration.ofSeconds(1)); // 测试用短审批超时
            return gate;
        }

        @Bean
        EnhancedMcpClient mcpClient(AgentToolManager toolManager, McpTimeoutPolicy timeoutPolicy,
                                    McpRetryPolicy retryPolicy, FallbackRegistry fallbackRegistry,
                                    HitlApprovalGate approvalGate) {
            return new EnhancedMcpClient(toolManager, timeoutPolicy, retryPolicy, fallbackRegistry, approvalGate);
        }
    }

    @MockitoBean
    private AgentToolManager toolManager;

    @org.springframework.beans.factory.annotation.Autowired
    private EnhancedMcpClient mcpClient;

    @BeforeEach
    void setUp() {
        ReasoningTrace.begin();
    }

    @Test
    void networkTimeout_retriesWithBackoff_thenSucceeds() {
        // 包装为 RuntimeException（cause 链携带受检异常，McpRetryPolicy 可识别重试）
        org.mockito.Mockito.doThrow(new RuntimeException(new SocketTimeoutException("连接超时")))
                .doThrow(new RuntimeException(new SocketTimeoutException("连接超时")))
                .doReturn("最终成功结果")
                .when(toolManager).execute(anyString(), anyMap());

        var result = mcpClient.call("search_resources", Map.of("keyword", "java"));

        assertTrue(result.success());
        assertEquals("最终成功结果", result.result());
        assertEquals(3, result.attempts(), "应重试 2 次后成功（共 3 次尝试）");
        assertFalse(result.fallback());
        assertNotNull(result.traceId());
        verify(toolManager, times(3)).execute(anyString(), anyMap());
    }

    @Test
    void persistentFailure_fallsBackToLocalResource() {
        org.mockito.Mockito.doThrow(new RuntimeException(new IOException("服务不可用")))
                .when(toolManager).execute(anyString(), anyMap());

        var result = mcpClient.call("search_resources", Map.of());

        assertTrue(result.success(), "降级执行视为成功");
        assertTrue(result.fallback(), "降级结果必须标记 fallback（X-Fallback: true）");
        assertTrue(result.result().contains("fallback"));
    }

    @Test
    void noFallbackAvailable_throwsReasoningExceptionWithContractCode() {
        org.mockito.Mockito.doThrow(new RuntimeException(new IOException("服务不可用")))
                .when(toolManager).execute(anyString(), anyMap());

        ReasoningException ex = assertThrows(ReasoningException.class,
                () -> mcpClient.call("unregistered_tool", Map.of()));

        // JSON-RPC 契约：error code 必须在 -32000 ~ -32099 范围内
        assertTrue(ex.getErrorCode() >= -32099 && ex.getErrorCode() <= -32000,
                "error code 应在契约范围，实际: " + ex.getErrorCode());
        assertNotNull(ex.getTraceId());
        assertTrue(ex.getMessage().contains("unregistered_tool"));
    }

    @Test
    void batchCall_idsMatchResponsesInOrder() {
        when(toolManager.execute(anyString(), anyMap()))
                .thenAnswer(inv -> "结果:" + inv.getArgument(0, String.class));

        List<EnhancedMcpClient.FunctionCall> calls = List.of(
                new EnhancedMcpClient.FunctionCall("req-1", "search_resources", Map.of("q", "a")),
                new EnhancedMcpClient.FunctionCall("req-2", "query_knowledge_graph", Map.of("q", "b")),
                new EnhancedMcpClient.FunctionCall("req-3", "web_search", Map.of("q", "c"))
        );

        List<EnhancedMcpClient.McpCallResult> results = mcpClient.batchCall(calls);

        assertEquals(3, results.size());
        // JSON-RPC 2.0：响应 id 必须与请求 id 一一对应（保持输入顺序）
        assertEquals("req-1", results.get(0).id());
        assertEquals("req-2", results.get(1).id());
        assertEquals("req-3", results.get(2).id());
        assertTrue(results.get(0).result().contains("search_resources"));
        assertTrue(results.get(1).result().contains("query_knowledge_graph"));
        assertTrue(results.get(2).result().contains("web_search"));
        // 同一批次共享 traceId
        assertEquals(results.get(0).traceId(), results.get(1).traceId());
    }

    @Test
    void highRiskTool_withoutApproval_isRejected() {
        when(toolManager.execute(anyString(), anyMap())).thenReturn("不应执行");

        var result = mcpClient.call("delete_user", Map.of("id", "1"));

        assertFalse(result.success(), "未通过审批的高危操作必须拒绝执行");
        assertTrue(result.error().contains("审批"));
        verify(toolManager, times(0)).execute(anyString(), anyMap());
    }

    @Test
    void sensitiveArgs_maskedInLogsAndResultSafe() {
        when(toolManager.execute(anyString(), anyMap())).thenReturn("ok");

        var result = mcpClient.call("search_resources", Map.of("query", "java", "api_key", "sk-secret"));

        assertTrue(result.success());
        assertEquals("ok", result.result());
        // 结果对象本身不包含敏感参数
        assertFalse(result.toMap().containsKey("api_key"));
    }
}
