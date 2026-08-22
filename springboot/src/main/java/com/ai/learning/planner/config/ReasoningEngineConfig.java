package com.ai.learning.planner.config;

import com.ai.learning.planner.agent.memory.ContextCompressor;
import com.ai.learning.planner.agent.memory.ContextWindow;
import com.ai.learning.planner.agent.memory.EpisodicMemory;
import com.ai.learning.planner.agent.memory.InMemoryEpisodicMemory;
import com.ai.learning.planner.agent.orchestrator.CollaborativeOrchestrator;
import com.ai.learning.planner.agent.reasoning.*;
import com.ai.learning.planner.mcp.client.*;
import com.ai.learning.planner.mcp.hitl.HitlApprovalGate;
import com.ai.learning.planner.service.ModelManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 高级推理引擎配置
 * 注册 Planner-Evaluator-Reflection-Compression 全链路 Bean
 * 只有 app.reasoning.enabled=true 时才激活
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({ReasoningProperties.class, McpProperties.class})
@ConditionalOnProperty(prefix = "app.reasoning", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReasoningEngineConfig {

    @Bean
    public ReasoningMonitor reasoningMonitor(MeterRegistry meterRegistry) {
        log.info("[ReasoningEngine] 注册 ReasoningMonitor");
        return new ReasoningMonitor(meterRegistry);
    }

    @Bean
    public ContextWindow contextWindow(ReasoningProperties props) {
        log.info("[ReasoningEngine] 注册 ContextWindow: totalTokens={}, compressThreshold={}",
                props.getContextWindowTokens(), props.getContextCompressionThreshold());
        return new ContextWindow(props.getContextWindowTokens(), props.getContextCompressionThreshold());
    }

    @Bean
    public ContextCompressor contextCompressor(ContextWindow contextWindow,
                                               ModelManager modelManager,
                                               MeterRegistry meterRegistry) {
        log.info("[ReasoningEngine] 注册 ContextCompressor");
        return new ContextCompressor(contextWindow, modelManager, meterRegistry);
    }

    @Bean
    public EpisodicMemory episodicMemory() {
        log.info("[ReasoningEngine] 注册 InMemoryEpisodicMemory");
        return new InMemoryEpisodicMemory();
    }

    @Bean
    public LlmPlanner llmPlanner(ModelManager modelManager) {
        log.info("[ReasoningEngine] 注册 LlmPlanner");
        return new LlmPlanner(modelManager);
    }

    @Bean
    public Evaluator evaluator(ModelManager modelManager) {
        log.info("[ReasoningEngine] 注册 Evaluator: pruneThreshold={}", 0.3);
        return new Evaluator(modelManager);
    }

    @Bean
    public ReflectionEngine reflectionEngine(ReasoningProperties props,
                                             ModelManager modelManager,
                                             ReasoningMonitor monitor) {
        log.info("[ReasoningEngine] 注册 ReflectionEngine: interval={}", props.getReflectionInterval());
        return new ReflectionEngine(props.getReflectionInterval(), modelManager, monitor);
    }

    @Bean
    public ReplanningTrigger replanningTrigger(ReasoningProperties props) {
        log.info("[ReasoningEngine] 注册 ReplanningTrigger");
        return new ReplanningTrigger();
    }

    @Bean
    public McpTimeoutPolicy mcpTimeoutPolicy(com.ai.learning.planner.config.McpProperties mcpProperties) {
        java.time.Duration readTimeout = java.time.Duration.ofMillis(mcpProperties.getReadTimeoutMs());
        java.time.Duration writeTimeout = java.time.Duration.ofMillis(mcpProperties.getWriteTimeoutMs());
        log.info("[ReasoningEngine] 注册 McpTimeoutPolicy: read={}ms, write={}ms",
                mcpProperties.getReadTimeoutMs(), mcpProperties.getWriteTimeoutMs());
        return new McpTimeoutPolicy(readTimeout, writeTimeout);
    }

    @Bean
    public McpRetryPolicy mcpRetryPolicy() {
        return new McpRetryPolicy();
    }

    @Bean
    public FallbackRegistry fallbackRegistry() {
        return new FallbackRegistry();
    }

    @Bean
    public HitlApprovalGate hitlApprovalGate() {
        return new HitlApprovalGate();
    }

    @Bean
    public EnhancedMcpClient enhancedMcpClient(
            com.ai.learning.planner.agent.tool.AgentToolManager toolManager,
            McpTimeoutPolicy timeoutPolicy,
            McpRetryPolicy retryPolicy,
            FallbackRegistry fallbackRegistry,
            HitlApprovalGate approvalGate) {
        log.info("[ReasoningEngine] 注册 EnhancedMcpClient");
        return new EnhancedMcpClient(toolManager, timeoutPolicy, retryPolicy, fallbackRegistry, approvalGate);
    }

    @Bean
    public CollaborativeOrchestrator collaborativeOrchestrator() {
        log.info("[ReasoningEngine] 注册 CollaborativeOrchestrator（多 Agent 协作引擎）");
        return new CollaborativeOrchestrator();
    }
}
