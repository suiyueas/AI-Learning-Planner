package com.ai.learning.planner.agent.reasoning;

import com.ai.learning.planner.agent.memory.ContextCompressor;
import com.ai.learning.planner.agent.memory.ContextWindow;
import com.ai.learning.planner.agent.memory.EpisodicMemory;
import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.agent.tool.ToolCallAgent;
import com.ai.learning.planner.mcp.client.EnhancedMcpClient;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高级推理智能体（Advanced Reasoning Agent）
 * 在 ToolCallAgent 基础上编排生产级长链推理闭环：
 * 1. Planner 生成多路径计划（线性/树状），动态重规划生成绕行计划
 * 2. Evaluator 对每步输出隐式奖励评分，得分 < 0.3 剪枝回退上一步
 * 3. ReflectionEngine 每 N 次工具调用强制反思（结构化 JSON）
 * 4. ReplanningTrigger 连续失败 2 次 / 上下文超阈值 → 强制重规划
 * 5. ContextCompressor 呼吸式上下文压缩（Token >70% 触发）
 * 6. EpisodicMemory 注入 Hidden CoT 历史经验
 * 7. EnhancedMcpClient 执行工具（批处理/超时/重试/降级/HITL）
 * 8. ReasoningMonitor 全程监控 + traceId 透传
 */
@Slf4j
public class AdvancedReasoningAgent extends ToolCallAgent {

    /** 工具调用标记解析（与 ToolCallAgent 一致） */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\[TOOL_CALL\\]\\s*\\{\\s*\"name\":\\s*\"([^\"]+)\",\\s*\"args\":\\s*\\{([^}]*)\\}\\s*\\}",
            Pattern.DOTALL
    );

    /** 工具失败结果前缀 */
    private static final String FAILURE_PREFIX = "【工具失败】";

    private final Planner planner;
    private final Evaluator evaluator;
    private final ReflectionEngine reflectionEngine;
    private final ReplanningTrigger replanningTrigger;
    private final ContextWindow contextWindow;
    private final ContextCompressor contextCompressor;
    private final EpisodicMemory episodicMemory;
    private final EnhancedMcpClient mcpClient;
    private final ReasoningMonitor monitor;

    /** 当前计划根节点 */
    private PlanNode planRoot;

    /** 当前执行节点 */
    private PlanNode currentPlanNode;

    /** 目标描述 */
    private String goal;

    /** 已执行工具调用计数 */
    private int toolCallCount = 0;

    /** 上次反思结果（注入下次 think） */
    private ReflectionResult lastReflection;

    /** 当前绕行提示（重规划后注入） */
    private String replanNote = "";

    /** 最近步骤摘要（供反思使用） */
    private final List<String> recentStepSummaries = new ArrayList<>();

    protected AdvancedReasoningAgent(String id, String name, String systemPrompt,
                                     ModelManager modelManager, AgentToolManager agentToolManager,
                                     Planner planner, Evaluator evaluator,
                                     ReflectionEngine reflectionEngine, ReplanningTrigger replanningTrigger,
                                     ContextWindow contextWindow, ContextCompressor contextCompressor,
                                     EpisodicMemory episodicMemory, EnhancedMcpClient mcpClient,
                                     ReasoningMonitor monitor) {
        super(id, name, systemPrompt, modelManager, agentToolManager, 30);
        this.planner = planner;
        this.evaluator = evaluator;
        this.reflectionEngine = reflectionEngine;
        this.replanningTrigger = replanningTrigger;
        this.contextWindow = contextWindow;
        this.contextCompressor = contextCompressor;
        this.episodicMemory = episodicMemory;
        this.mcpClient = mcpClient;
        this.monitor = monitor;
    }

    /**
     * 推理闭环单步执行：压缩 → 规划 → 思考 → 行动 → 观察 → 评估 → 反思/重规划
     */
    @Override
    public String step(String input) {
        // 0. 呼吸式上下文：Token 使用率超阈值时压缩
        maybeCompressContext();

        // 1. 首次执行生成计划
        if (planRoot == null) {
            initializePlan(input);
        }

        // 记录本次 step 前的观察值，用于判断是否产生新观察（FINISH 步骤不重复评估）
        String prevObservation = currentObservation;
        String stepResult = super.step(input);

        // 2. 后置钩子：评估 + 反思 + 重规划（仅在有新观察时评估）
        afterStep(input, currentObservation != prevObservation);

        if (monitor != null) monitor.recordStep();
        return stepResult;
    }

    /**
     * 思考阶段：注入 Hidden CoT、计划步骤、反思调整、绕行提示
     */
    @Override
    public String think(String input) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【当前目标】").append(goal == null ? input : goal).append("\n\n");

        // Hidden CoT：检索历史经验注入
        String hiddenCot = episodicMemory.buildHiddenCoT(input, 3);
        if (!hiddenCot.isBlank()) {
            prompt.append(hiddenCot).append('\n');
        }

        // 当前计划路径
        if (planRoot != null) {
            prompt.append("【当前计划】\n");
            List<String> path = currentPlanNode != null
                    ? currentPlanNode.pathDescriptions()
                    : List.of(planRoot.getDescription());
            for (int i = 0; i < path.size(); i++) {
                prompt.append(i + 1).append(". ").append(path.get(i)).append('\n');
            }
        }

        // 绕行提示（重规划后）
        if (!replanNote.isBlank()) {
            prompt.append('\n').append(replanNote).append('\n');
        }

        // 上次反思的调整建议
        if (lastReflection != null) {
            prompt.append("\n【上次反思调整】").append(lastReflection.nextActionAdjustment()).append('\n');
        }

        prompt.append("\n【可用工具与用户输入】\n");
        return super.think(prompt.toString());
    }

    /**
     * 行动阶段：通过 EnhancedMcpClient 执行工具（超时/重试/降级/HITL）
     */
    @Override
    public String act(String thought) {
        if (thought == null || !thought.contains("[TOOL_CALL]")) {
            return thought;
        }
        Matcher matcher = TOOL_CALL_PATTERN.matcher(thought);
        StringBuilder result = new StringBuilder();
        int matched = 0;

        while (matcher.find()) {
            matched++;
            String toolName = matcher.group(1);
            Map<String, Object> args = parseArgs(matcher.group(2));
            log.info("[{}] 调用工具: {}，参数: {}", name, toolName, args);

            long start = System.currentTimeMillis();
            var callResult = mcpClient.call(toolName, args);
            long duration = System.currentTimeMillis() - start;
            if (monitor != null) monitor.recordToolDuration(duration);

            // 监控：失败/降级
            if (!callResult.success()) {
                if (monitor != null) monitor.recordToolFailure();
                result.append(FAILURE_PREFIX).append(toolName).append(": ")
                        .append(callResult.error() == null ? "未知错误" : callResult.error()).append('\n');
            } else {
                if (callResult.fallback() && monitor != null) {
                    monitor.recordToolFallback();
                }
                result.append(callResult.result()).append('\n');
            }
        }

        if (matched == 0) {
            return thought;
        }
        return result.toString().trim();
    }

    /**
     * 观察阶段：解析失败标记，驱动重规划触发器
     */
    @Override
    public String observe(String actionResult) {
        String observation = super.observe(actionResult);
        boolean failed = actionResult != null && actionResult.contains(FAILURE_PREFIX);
        replanningTrigger.recordToolResult(!failed);
        if (failed) {
            if (monitor != null) monitor.recordToolFailure();
            toolCallCount = 0; // 失败不累计反思计数
        } else if (actionResult != null && !actionResult.isBlank()) {
            toolCallCount++;
        }
        return observation;
    }

    /**
     * 后置钩子：评估当前步骤 → 反思 → 重规划
     *
     * @param newObservation 本次 step 是否产生了新的观察结果（FINISH 步骤为 false）
     */
    private void afterStep(String input, boolean newObservation) {
        // 1. 步骤评估（隐式奖励模型）
        if (newObservation && currentPlanNode != null && currentObservation != null && !currentObservation.isBlank()
                && !currentObservation.contains(FAILURE_PREFIX)) {
            var score = evaluator.evaluate(currentPlanNode, currentObservation);
            if (score.needsPruning() && monitor != null) {
                monitor.recordPrune();
            }
            recentStepSummaries.add(currentPlanNode.getDescription() + " -> 得分 " + String.format("%.2f", score.score()));
        }

        // 2. 强制反思：每 N 次工具调用
        if (reflectionEngine.shouldReflect(toolCallCount)) {
            lastReflection = reflectionEngine.reflect(goal, recentStepSummaries);
            pushEvent("reflection", Map.of("result", lastReflection.toJson()));
            recentStepSummaries.clear();
            // stalled 状态触发重规划
            if (lastReflection.status() == ReflectionResult.Status.stalled) {
                triggerReplan("反思判定停滞: " + lastReflection.whatMissing());
            }
        }

        // 3. 动态重规划：连续失败 2 次 或 上下文超阈值
        double contextRatio = contextWindow.usageRatio(messageList);
        if (replanningTrigger.shouldReplan(contextRatio)) {
            String reason = replanningTrigger.shouldReplanByFailure()
                    ? "工具连续失败 " + replanningTrigger.getConsecutiveFailures() + " 次"
                    : "上下文使用率 " + String.format("%.0f%%", contextRatio * 100) + " 超过阈值";
            triggerReplan(reason);
        }
    }

    /**
     * 触发动态重规划：生成绕行计划并切换当前路径
     */
    private void triggerReplan(String reason) {
        log.warn("[{}] 触发动态重规划: {}", name, reason);
        PlanNode failed = currentPlanNode;
        PlanNode altRoot = planner.generateAlternativePath(
                goal == null ? "未知目标" : goal, failed, reason, getAvailableTools());
        this.planRoot = altRoot;
        this.currentPlanNode = altRoot.getChildren().isEmpty() ? null : altRoot.getChildren().get(0);
        this.replanNote = "【动态重规划】" + reason + "，已生成绕行计划，请按新路径继续。";
        this.replanningTrigger.reset();
        if (monitor != null) monitor.recordReplan();
        pushEvent("replan", Map.of("reason", reason, "alternative", planRoot.getDescription()));
    }

    /**
     * 首次执行生成计划（含 Hidden CoT 前置注入）
     */
    private void initializePlan(String input) {
        this.goal = input;
        PlanningResult result = planner.generatePlan(
                input, getAvailableTools(), PlanningResult.Topology.LINEAR);
        this.planRoot = result.getRoot();
        List<PlanNode> leaves = planRoot.pendingLeaves();
        this.currentPlanNode = leaves.isEmpty() ? null : leaves.get(0);
        log.info("[{}] 计划生成完成（{}）: {} 个待执行步骤",
                name, result.getTopology(), leaves.size());
        pushEvent("plan", Map.of("topology", result.getTopology().name(), "steps", leaves.size()));
    }

    /**
     * 呼吸式上下文压缩
     */
    private void maybeCompressContext() {
        if (contextCompressor == null || !contextWindow.needsCompression(messageList)) {
            return;
        }
        var result = contextCompressor.compress(messageList, systemPrompt, goal);
        if (result.triggered()) {
            log.info("[{}] 上下文已压缩: {} -> {} tokens（压缩率 {}）",
                    name, result.tokensBefore(), result.tokensAfter(),
                    String.format("%.2f%%", result.compressionRatio() * 100));
            messageList.clear();
            messageList.addAll(result.compressed());
            if (monitor != null) monitor.recordCompressionRatio(result.compressionRatio());
        }
    }

    /**
     * 解析简单参数（与 ToolCallAgent 一致）
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

    @Override
    public void reset() {
        super.reset();
        this.planRoot = null;
        this.currentPlanNode = null;
        this.goal = null;
        this.toolCallCount = 0;
        this.lastReflection = null;
        this.replanNote = "";
        this.recentStepSummaries.clear();
        this.replanningTrigger.reset();
    }

    /**
     * 注册工具集：直接使用 EnhancedMcpClient 托管的所有已注册工具
     */
    @Override
    protected void registerTools() {
        // 工具已由 EnhancedMcpClient 包装 AgentToolManager 统一托管，无需重复注册
    }

    @Override
    public String getDescription() {
        return "高级推理智能体：多路径规划 + 评估剪枝 + 自我反思 + 动态重规划 + 呼吸式上下文";
    }
}
