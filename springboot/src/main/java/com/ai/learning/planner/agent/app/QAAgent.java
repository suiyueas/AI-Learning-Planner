package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 答疑Agent - 苏格拉底式问答
 */
@Slf4j
@Component
public class QAAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的学习辅导老师，采用苏格拉底式提问引导学习者思考。
        核心原则：
        1. 不直接给出答案，而是通过提问引导思考
        2. 帮助学习者发现自己的理解误区
        3. 提供适当的提示和引导
        4. 鼓励学习者独立思考
        回复格式：
        1. 先肯定学习者的提问
        2. 提出引导性问题
        3. 给出适当的提示（如果需要）
        4. 鼓励继续思考
        请用中文回复，语气温和友善。
        """;

    public QAAgent(ModelManager modelManager, AgentToolManager agentToolManager) {
        super("tutor", "答疑Agent", SYSTEM_PROMPT, modelManager, agentToolManager);
    }

    @Override
    protected void registerTools() {
        // 答疑Agent使用知识图谱和联网搜索工具
    }

    @Override
    public String getDescription() {
        return "基于知识库的智能问答，引导式教学";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("query_knowledge_graph", "search_knowledge", "web_search");
    }
}
