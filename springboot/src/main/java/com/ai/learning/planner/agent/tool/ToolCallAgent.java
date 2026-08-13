package com.ai.learning.planner.agent.tool;

import com.ai.learning.planner.agent.react.ReActAgent;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具调用智能体
 * 继承ReActAgent，实现基于LLM的工具调用决策和执行
 * 禁用Spring AI内置工具调用，自行维护上下文和工具执行
 */
@Slf4j
public abstract class ToolCallAgent extends ReActAgent {

    /** 工具调用管理器 */
    protected final AgentToolManager agentToolManager;

    /** 是否禁用Spring AI内置工具调用 */
    protected boolean internalToolExecutionEnabled = false;

    /** 工具调用正则：解析LLM输出中的工具调用标记 */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\[TOOL_CALL\\]\\s*\\{\\s*\"name\":\\s*\"([^\"]+)\",\\s*\"args\":\\s*\\{([^}]*)\\}\\s*\\}",
            Pattern.DOTALL
    );

    protected ToolCallAgent(String id, String name, String systemPrompt,
                             ModelManager modelManager, AgentToolManager agentToolManager) {
        super(id, name, systemPrompt, modelManager);
        this.agentToolManager = agentToolManager;
    }

    protected ToolCallAgent(String id, String name, String systemPrompt,
                             ModelManager modelManager, AgentToolManager agentToolManager, int maxSteps) {
        super(id, name, systemPrompt, modelManager, maxSteps);
        this.agentToolManager = agentToolManager;
    }

    /**
     * 注册当前Agent的专属工具集
     * 子类重写此方法注册需要的工具
     */
    protected abstract void registerTools();

    @Override
    public String think(String input) {
        // 构建带工具描述的提示词
        String toolPrompt = buildToolPrompt();
        String fullPrompt = systemPrompt + "\n\n" + toolPrompt + "\n\n用户输入: " + input;

        log.debug("[{}] 调用LLM进行推理", name);
        pushEvent("llm_call", Map.of("phase", "think", "prompt", fullPrompt));

        try {
            String response = callLLM(fullPrompt);
            log.debug("[{}] LLM推理结果: {}", name, response);

            // 检查是否包含工具调用标记
            if (response.contains("[TOOL_CALL]")) {
                currentThought = response;
                shouldContinue = true;
            } else if (response.contains("[FINISH]")) {
                currentThought = response;
                shouldContinue = false;
            } else {
                currentThought = response;
                shouldContinue = true;
            }

            return currentThought;
        } catch (Exception e) {
            log.error("[{}] LLM推理失败: {}", name, e.getMessage());
            pushEvent("error", Map.of("phase", "think", "error", e.getMessage()));
            return "[FINISH] 推理过程出错: " + e.getMessage();
        }
    }

    @Override
    public String act(String thought) {
        // 解析工具调用
        if (thought == null || !thought.contains("[TOOL_CALL]")) {
            // 没有工具调用，直接返回思考结果作为行动
            return thought;
        }

        Matcher matcher = TOOL_CALL_PATTERN.matcher(thought);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String toolName = matcher.group(1);
            String argsStr = matcher.group(2);
            Map<String, Object> args = parseArgs(argsStr);

            log.info("[{}] 调用工具: {}，参数: {}", name, toolName, args);
            pushEvent("tool_call", Map.of("tool", toolName, "args", args));

            // 通过AgentToolManager执行工具
            String toolResult = agentToolManager.execute(toolName, args);
            result.append(toolResult).append("\n");

            pushEvent("tool_result", Map.of("tool", toolName, "result", toolResult));
        }

        if (result.isEmpty()) {
            // 没有匹配到工具调用格式，返回原始思考结果
            return thought;
        }

        return result.toString().trim();
    }

    /**
     * 调用LLM获取响应
     */
    protected String callLLM(String prompt) {
        return modelManager.createChatClient().prompt()
                .system(systemPrompt)
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 构建工具描述提示词，告知LLM可用工具
     */
    protected String buildToolPrompt() {
        Map<String, String> tools = agentToolManager.getRegisteredTools();
        if (tools.isEmpty()) {
            return "【可用工具】\n当前没有可用的工具，请直接回答问题。\n\n" +
                   "如果任务完成，请在最后加上 [FINISH] 标记。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【可用工具】\n");
        sb.append("你可以使用以下工具来帮助完成任务。如果需要调用工具，请使用以下格式：\n");
        sb.append("[TOOL_CALL] {\"name\": \"工具名\", \"args\": {\"参数1\": \"值1\"}}\n\n");
        sb.append("可用工具列表：\n");

        tools.forEach((name, desc) -> {
            sb.append("- ").append(name).append(": ").append(desc).append("\n");
        });

        sb.append("\n注意：\n");
        sb.append("1. 每次只能调用一个工具，等待工具返回结果后再决定下一步\n");
        sb.append("2. 工具调用结果会以【工具: xxx】的格式返回\n");
        sb.append("3. 如果不需要调用工具，直接输出你的回答即可\n");
        sb.append("4. 如果任务已完成，请在最后加上 [FINISH] 标记\n");

        return sb.toString();
    }

    /**
     * 解析简单参数
     * 格式: "key1": "value1", "key2": "value2"
     */
    private Map<String, Object> parseArgs(String argsStr) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        if (argsStr == null || argsStr.trim().isEmpty()) {
            return result;
        }

        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(argsStr);
        while (m.find()) {
            result.put(m.group(1), m.group(2));
        }
        return result;
    }

    /**
     * 禁用Spring AI内置工具调用
     */
    public void setInternalToolExecutionEnabled(boolean enabled) {
        this.internalToolExecutionEnabled = enabled;
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public List<String> getAvailableTools() {
        return List.copyOf(agentToolManager.getRegisteredTools().keySet());
    }
}
