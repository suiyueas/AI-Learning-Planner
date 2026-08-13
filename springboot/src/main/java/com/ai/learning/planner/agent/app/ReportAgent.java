package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 报告Agent - 学情分析与报告生成
 */
@Slf4j
@Component
public class ReportAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的学情分析师，负责生成详细的学习报告。
        报告内容应包括：
        1. 学习概况 - 学习时长统计、完成的知识点数量、平均掌握度
        2. 知识掌握分析 - 已掌握、正在学习、薄弱知识点
        3. 学习趋势 - 学习频率、进步情况
        4. 建议与规划 - 下一步学习重点、调整建议
        输出格式要结构清晰，使用适当的标题和列表。
        请用中文回复。
        """;

    public ReportAgent(ModelManager modelManager, AgentToolManager agentToolManager) {
        super("reporter", "报告Agent", SYSTEM_PROMPT, modelManager, agentToolManager);
    }

    @Override
    protected void registerTools() {
        // 报告Agent使用PDF生成和用户画像工具
    }

    @Override
    public String getDescription() {
        return "生成学习报告，分析学习进度和效果";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("query_knowledge_graph", "web_search", "search_resources");
    }
}
