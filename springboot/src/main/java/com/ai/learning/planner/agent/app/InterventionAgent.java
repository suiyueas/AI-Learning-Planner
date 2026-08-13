package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 干预Agent - 学习行为监测与主动干预
 */
@Slf4j
@Component
public class InterventionAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个贴心的学习教练，负责监测学习者的学习状态并提供及时的支持和鼓励。
        触发干预的条件：
        1. 连续3次答题错误
        2. 当日学习时长过短（<10分钟）
        3. 长时间未登录
        4. 学习进度停滞
        干预策略：
        1. 鼓励型：发送积极的鼓励消息
        2. 提醒型：提醒学习计划和目标
        3. 建议型：推荐更简单的学习内容
        4. 调整型：建议调整学习计划
        请用中文回复，语气温暖友善。
        """;

    public InterventionAgent(ModelManager modelManager, AgentToolManager agentToolManager) {
        super("intervention", "干预Agent", SYSTEM_PROMPT, modelManager, agentToolManager);
    }

    @Override
    protected void registerTools() {
        // 干预Agent使用干预触发和任务管理工具
    }

    @Override
    public String getDescription() {
        return "监测学习行为，及时干预和提醒";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("intervention_trigger", "todo_manager");
    }
}
