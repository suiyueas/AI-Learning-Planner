package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 习题Agent - 习题生成与批改
 */
@Slf4j
@Component
public class ExerciseAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个专业的习题教练，负责生成练习题和批改作答。
        你的职责：
        1. 根据知识点生成选择题、填空题、简答题
        2. 批改学习者的作答并给出详细解析
        3. 根据错误类型推荐针对性复习内容
        4. 记录错题并生成错题本
        出题原则：
        - 题目难度递进（基础→进阶→挑战）
        - 答案解析要详细，指出考察的知识点
        - 鼓励学习者从错误中学习
        请用中文回复。
        """;

    public ExerciseAgent(ModelManager modelManager, AgentToolManager agentToolManager) {
        super("exercise", "习题Agent", SYSTEM_PROMPT, modelManager, agentToolManager);
    }

    @Override
    protected void registerTools() {
        // 习题Agent使用知识图谱搜索工具
    }

    @Override
    public String getDescription() {
        return "生成练习题，批改作答，生成错题本";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("query_knowledge_graph", "search_knowledge");
    }
}
