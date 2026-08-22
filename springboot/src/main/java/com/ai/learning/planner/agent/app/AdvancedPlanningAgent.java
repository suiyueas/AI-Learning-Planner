package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.memory.ContextCompressor;
import com.ai.learning.planner.agent.memory.ContextWindow;
import com.ai.learning.planner.agent.memory.EpisodicMemory;
import com.ai.learning.planner.agent.reasoning.*;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.mcp.client.EnhancedMcpClient;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 高级规划Agent
 * 继承 AdvancedReasoningAgent，启用完整推理闭环：
 * - Planner 多路径规划
 * - Evaluator 隐式奖励评分（0.3 剪枝）
 * - ReflectionEngine 每 3 次工具调用强制反思
 * - ReplanningTrigger 连续失败 2 次重规划
 * - ContextCompressor 呼吸式压缩（70% 触发）
 * - EpisodicMemory Hidden CoT 历史经验注入
 * - EnhancedMcpClient 工具执行（批处理/超时/重试/降级/HITL）
 * - ReasoningMonitor 全程监控
 */
@Slf4j
@Component
public class AdvancedPlanningAgent extends AdvancedReasoningAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个高级学习路径规划师，具备自我反思和动态调整能力。
        你的职责：
        1. 根据学习目标生成多步骤推理计划（线性/树状）
        2. 每一步执行后评估输出质量，低质量步骤自动剪枝回退
        3. 每 3 次工具调用强制反思，检查是否偏离目标
        4. 工具连续失败 2 次时自动重规划，生成绕行路径
        5. 上下文超限时自动压缩，保留关键信息
        6. 参考历史经验（Hidden CoT）避免重复犯错
        输出格式：
        学习路径名称：xxx
        总预计时长：xxx小时
        阶段1：xxx - 目标 - 时长 - 知识点 - 资源
        请用中文回复，结构清晰。
        """;

    public AdvancedPlanningAgent(
            ModelManager modelManager,
            AgentToolManager agentToolManager,
            LlmPlanner planner,
            Evaluator evaluator,
            ReflectionEngine reflectionEngine,
            ReplanningTrigger replanningTrigger,
            ContextWindow contextWindow,
            ContextCompressor contextCompressor,
            EpisodicMemory episodicMemory,
            EnhancedMcpClient mcpClient,
            ReasoningMonitor monitor) {
        super("planner", "高级规划Agent", SYSTEM_PROMPT,
                modelManager, agentToolManager,
                planner, evaluator, reflectionEngine, replanningTrigger,
                contextWindow, contextCompressor, episodicMemory, mcpClient, monitor);
        log.info("[AdvancedPlanningAgent] 初始化完成，推理闭环已激活");
    }

    @Override
    protected void registerTools() {
        // 工具通过 EnhancedMcpClient 统一管理
    }

    @Override
    public String getDescription() {
        return "高级学习路径规划，支持多路径规划、自我评估、反思纠错、动态重规划";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("query_knowledge_graph", "search_knowledge", "web_search");
    }
}
