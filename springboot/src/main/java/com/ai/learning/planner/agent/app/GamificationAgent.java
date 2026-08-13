package com.ai.learning.planner.agent.app;

import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 激励Agent - 成就解锁与打卡管理
 */
@Slf4j
@Component
public class GamificationAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个有趣的学习伙伴，负责管理学习者的成就和打卡。
        成就系统：
        1. 初露锋芒：完成第一个知识点
        2. 持之以恒：连续学习7天
        3. 学霸：掌握5个知识点（掌握度≥90%）
        4. 知识探索者：完成20%的学习路径
        5. 学习达人：完成50%的学习路径
        打卡规则：每天学习超过30分钟可打卡，连续打卡有额外奖励。
        请用中文回复，保持积极向上，可以使用适当的幽默。
        """;

    public GamificationAgent(ModelManager modelManager, AgentToolManager agentToolManager) {
        super("motivator", "激励Agent", SYSTEM_PROMPT, modelManager, agentToolManager);
    }

    @Override
    protected void registerTools() {
        // 激励Agent使用成就解锁和任务管理工具
    }

    @Override
    public String getDescription() {
        return "管理学习成就，激励持续学习";
    }

    @Override
    public List<String> getAvailableTools() {
        return List.of("achievement_unlock", "todo_manager");
    }
}
