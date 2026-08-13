package com.ai.learning.planner.agent.reasoning;

import com.ai.learning.planner.TestLlmSupport;
import com.ai.learning.planner.agent.memory.ContextCompressor;
import com.ai.learning.planner.agent.memory.ContextWindow;
import com.ai.learning.planner.agent.memory.InMemoryEpisodicMemory;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.mcp.client.EnhancedMcpClient;
import com.ai.learning.planner.mcp.client.FallbackRegistry;
import com.ai.learning.planner.mcp.client.McpRetryPolicy;
import com.ai.learning.planner.mcp.client.McpTimeoutPolicy;
import com.ai.learning.planner.mcp.hitl.HitlApprovalGate;
import com.ai.learning.planner.service.ModelManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 动态重规划集成测试（@SpringBootTest 最小上下文）
 * 覆盖：正常推理闭环 / 工具连续失败触发重规划 / 评估低分触发剪枝
 */
@SpringBootTest(classes = DynamicReplanningIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DynamicReplanningIntegrationTest {

    @SpringBootConfiguration
    static class TestConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

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
            return new FallbackRegistry();
        }

        @Bean
        HitlApprovalGate approvalGate() {
            return new HitlApprovalGate();
        }

        @Bean
        EnhancedMcpClient mcpClient(AgentToolManager toolManager, McpTimeoutPolicy timeoutPolicy,
                                    McpRetryPolicy retryPolicy, FallbackRegistry fallbackRegistry,
                                    HitlApprovalGate approvalGate) {
            return new EnhancedMcpClient(toolManager, timeoutPolicy, retryPolicy, fallbackRegistry, approvalGate);
        }

        @Bean
        LlmPlanner planner(ModelManager modelManager) {
            return new LlmPlanner(modelManager);
        }

        @Bean
        Evaluator evaluator(ModelManager modelManager) {
            return new Evaluator(modelManager);
        }

        @Bean
        ReasoningMonitor monitor(SimpleMeterRegistry meterRegistry) {
            return new ReasoningMonitor(meterRegistry);
        }

        @Bean
        ReflectionEngine reflectionEngine(ModelManager modelManager, ReasoningMonitor monitor) {
            return new ReflectionEngine(3, modelManager, monitor);
        }

        @Bean
        ReplanningTrigger replanningTrigger() {
            return new ReplanningTrigger();
        }

        @Bean
        ContextWindow contextWindow() {
            return new ContextWindow();
        }

        @Bean
        ContextCompressor contextCompressor(ContextWindow window, ModelManager modelManager,
                                            SimpleMeterRegistry meterRegistry) {
            return new ContextCompressor(window, modelManager, meterRegistry);
        }

        @Bean
        InMemoryEpisodicMemory episodicMemory() {
            return new InMemoryEpisodicMemory();
        }

        @Bean
        AdvancedReasoningAgent agent(ModelManager modelManager, AgentToolManager toolManager,
                                     LlmPlanner planner, Evaluator evaluator, ReflectionEngine reflectionEngine,
                                     ReplanningTrigger replanningTrigger, ContextWindow contextWindow,
                                     ContextCompressor contextCompressor, InMemoryEpisodicMemory episodicMemory,
                                     EnhancedMcpClient mcpClient, ReasoningMonitor monitor) {
            return new AdvancedReasoningAgent("reasoner", "高级推理Agent", "你是测试用高级推理智能体",
                    modelManager, toolManager, planner, evaluator, reflectionEngine, replanningTrigger,
                    contextWindow, contextCompressor, episodicMemory, mcpClient, monitor);
        }
    }


    @MockitoBean
    private ModelManager modelManager;

    @MockitoBean
    private AgentToolManager toolManager;

    @org.springframework.beans.factory.annotation.Autowired
    private AdvancedReasoningAgent agent;

    @org.springframework.beans.factory.annotation.Autowired
    private ReasoningMonitor monitor;

    @BeforeEach
    void setUp() {
        TestLlmSupport.reset();
        TestLlmSupport.bindChatClient(modelManager);
        agent.reset();
    }

    @Test
    void normalPath_completesWithoutReplan() {
        // 计划生成 → 工具调用 → 评分 → 结束
        TestLlmSupport.queue("步骤1: 检索资料\n步骤2: 分析总结");
        TestLlmSupport.queue("[TOOL_CALL] {\"name\": \"search_resources\", \"args\": {\"keyword\": \"python\"}}");
        TestLlmSupport.queue("{\"score\": 0.9, \"reason\": \"步骤达成\"}");
        TestLlmSupport.queue("[FINISH] 任务完成");

        when(toolManager.execute(anyString(), anyMap())).thenReturn("检索到10条Python学习资源");

        String result = agent.run("帮我规划Python学习");

        assertTrue(result.contains("完成") || result.contains("检索"));
        assertEquals(0, monitor.getReplanCount(), "正常路径不应触发重规划");
        assertEquals(0, monitor.getPruneCount(), "正常路径不应触发剪枝");
        assertTrue(monitor.getStepCount() >= 2);
    }

    @Test
    void consecutiveToolFailures_triggerReplan() {
        // 计划生成 → 连续 2 次工具失败 → 触发重规划 → 结束
        TestLlmSupport.queue("步骤1: 执行任务\n步骤2: 完成任务");
        TestLlmSupport.queue("[TOOL_CALL] {\"name\": \"fail_tool\", \"args\": {}}");
        TestLlmSupport.queue("[TOOL_CALL] {\"name\": \"fail_tool\", \"args\": {}}");
        TestLlmSupport.queue("[FINISH] 任务完成");

        // 工具返回失败标记（模拟工具层失败但调用本身成功返回）
        when(toolManager.execute(anyString(), anyMap()))
                .thenReturn("【工具失败】fail_tool: 模拟持续故障");

        String result = agent.run("执行需要外部服务的任务");

        assertTrue(result.contains("完成"));
        assertTrue(monitor.getReplanCount() >= 1, "连续失败 2 次应触发动态重规划");
        assertTrue(monitor.getToolFailureCount() >= 2, "应记录至少 2 次工具失败");
    }

    @Test
    void lowEvaluatorScore_prunesNode() {
        // 计划生成 → 工具调用成功但评估低分 → 剪枝 → 结束
        TestLlmSupport.queue("步骤1: 获取信息\n步骤2: 输出结论");
        TestLlmSupport.queue("[TOOL_CALL] {\"name\": \"query_knowledge_graph\", \"args\": {\"nodeId\": \"n1\"}}");
        TestLlmSupport.queue("{\"score\": 0.1, \"reason\": \"输出与目标无关\"}");
        TestLlmSupport.queue("[FINISH] 任务完成");

        when(toolManager.execute(anyString(), anyMap())).thenReturn("节点数据内容");

        String result = agent.run("查询知识图谱");

        assertTrue(result.contains("完成"));
        assertTrue(monitor.getPruneCount() >= 1, "评估得分低于 0.3 应触发剪枝");
    }
}
