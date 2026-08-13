package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 规划Agent - 学习路径生成与动态调整
 */
@Slf4j
@Component
public class PlanningAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的学习路径规划师，负责为学习者制定个性化的学习计划。
        你的职责：
        1. 根据学习目标制定学习路径
        2. 分析知识点的前置依赖关系
        3. 合理安排学习顺序和时间
        4. 推荐合适的学习资源
        5. 根据学习进度动态调整计划
        输出格式：
        学习路径名称：xxx
        总预计时长：xxx小时
        阶段1：xxx - 目标 - 时长 - 知识点 - 资源
        请用中文回复，结构清晰。
        """;

    public PlanningAgent(ModelManager modelManager, AgentToolManager agentToolManager) {
        super("planner", "规划Agent", SYSTEM_PROMPT, modelManager, agentToolManager);
    }

    @Override
    protected void registerTools() {
        // 规划Agent使用知识图谱查询和资源检索工具
    }

    @Override
    public String getDescription() {
        return "生成个性化学习路径，动态调整学习计划";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("query_knowledge_graph", "search_knowledge", "web_search");
    }
}
