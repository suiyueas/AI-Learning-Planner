package com.ai.learning.planner.agent.react;

import com.ai.learning.planner.agent.base.AgentState;
import com.ai.learning.planner.agent.base.BaseAgent;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * ReAct模式智能体抽象类
 * 实现推理-行动闭环：think() → act() → observe()
 * step()方法依次调用think()判断是否需要行动，act()执行行动，observe()观察结果
 */
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /** 当前思考内容 */
    protected String currentThought;

    /** 当前行动内容 */
    protected String currentAction;

    /** 当前观察结果 */
    protected String currentObservation;

    /** 是否需要继续执行 */
    protected boolean shouldContinue = true;

    protected ReActAgent(String id, String name, String systemPrompt, ModelManager modelManager) {
        super(id, name, systemPrompt, modelManager);
    }

    protected ReActAgent(String id, String name, String systemPrompt, ModelManager modelManager, int maxSteps) {
        super(id, name, systemPrompt, modelManager, maxSteps);
    }

    @Override
    public String step(String input) {
        // 1. 思考阶段：分析输入，决定下一步行动
        log.debug("[{}] 步骤{} - 思考阶段", name, currentStep.get());
        pushThink("正在分析: " + input + " (步骤 " + currentStep.get() + ")");
        currentThought = think(input);
        pushThink(currentThought);

        if (!shouldContinue || currentThought == null || currentThought.contains("[FINISH]")) {
            log.info("[{}] 思考决定结束执行", name);
            state = AgentState.FINISHED;
            return "[完成] " + (currentThought != null ? currentThought : "任务完成");
        }

        // 2. 行动阶段：根据思考结果执行行动
        log.debug("[{}] 步骤{} - 行动阶段", name, currentStep.get());
        currentAction = act(currentThought);
        if (currentAction != null) {
            pushAction("action", Map.of("thought", currentThought), currentAction);
        }

        // 3. 观察阶段：观察行动结果
        log.debug("[{}] 步骤{} - 观察阶段", name, currentStep.get());
        currentObservation = observe(currentAction);
        if (currentObservation != null) {
            pushObserve(currentObservation);
        }

        return formatStepResult();
    }

    /**
     * 推理阶段：分析输入，决定下一步行动
     * @param input 用户输入或当前上下文
     * @return 思考结果（包含推理过程和行动决策）
     */
    public abstract String think(String input);

    /**
     * 行动阶段：根据思考结果执行具体行动
     * @param thought 思考结果
     * @return 行动结果
     */
    public abstract String act(String thought);

    /**
     * 观察阶段：观察行动结果，提取关键信息
     * @param actionResult 行动结果
     * @return 观察结果
     */
    public String observe(String actionResult) {
        if (actionResult == null || actionResult.isEmpty()) {
            return "行动未返回结果";
        }
        // 默认截取前200字符作为观察摘要
        if (actionResult.length() > 200) {
            return actionResult.substring(0, 200) + "... (共" + actionResult.length() + "字符)";
        }
        return actionResult;
    }

    /**
     * 格式化步骤结果
     */
    private String formatStepResult() {
        StringBuilder sb = new StringBuilder();
        sb.append("【思考】").append(currentThought != null ? currentThought : "无").append("\n");
        sb.append("【行动】").append(currentAction != null ? currentAction : "无").append("\n");
        sb.append("【观察】").append(currentObservation != null ? currentObservation : "无");
        return sb.toString();
    }

    /**
     * 停止继续执行
     */
    protected void finish() {
        this.shouldContinue = false;
    }

    @Override
    public void reset() {
        super.reset();
        currentThought = null;
        currentAction = null;
        currentObservation = null;
        shouldContinue = true;
    }
}
