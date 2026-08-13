package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 诊断Agent - 能力测评与画像构建
 */
@Slf4j
@Component
public class DiagnosisAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的学习诊断专家，负责评估学习者的学习水平和特点。
        你的职责：
        1. 通过问答了解学习者的知识背景
        2. 评估学习者在特定领域的掌握程度
        3. 识别学习者的薄弱环节
        4. 分析学习者的学习风格和偏好
        回复要求：
        - 使用苏格拉底式提问引导学习者思考
        - 根据学习者的回答动态调整问题难度
        - 最终给出诊断报告
        请用中文回复，语言要温和友善。
        """;

    public DiagnosisAgent(ModelManager modelManager, AgentToolManager agentToolManager) {
        super("diagnosis", "诊断Agent", SYSTEM_PROMPT, modelManager, agentToolManager);
    }

    @Override
    protected void registerTools() {
        // 诊断Agent使用知识图谱查询和用户画像工具
    }

    @Override
    public String getDescription() {
        return "评估学习水平，构建用户能力画像，识别薄弱环节";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("query_knowledge_graph", "search_resources");
    }
}
