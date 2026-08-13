package com.ai.learning.planner.agent.reasoning;

import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 基于 LLM 的推理规划器（默认实现）
 * - generatePlan：LLM 生成步骤列表（线性）或树状分支；LLM 不可用时规则兜底
 * - generateAlternativePath：绕过失败节点生成绕行计划
 * - pruneBranch：剪枝标记
 */
@Slf4j
public class LlmPlanner implements Planner {

    private final ModelManager modelManager;

    public LlmPlanner(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    @Override
    public PlanningResult generatePlan(String goal, List<String> tools, PlanningResult.Topology topology) {
        log.info("[Planner] 生成{}计划: {}", topology, goal);
        try {
            if (topology == PlanningResult.Topology.TREE) {
                return generateTreePlan(goal, tools);
            }
            // 线性/图状默认按步骤链处理
            List<String> steps = llmGenerateSteps(goal, tools);
            return PlanningResult.linear(goal, steps, tools);
        } catch (Exception e) {
            log.warn("[Planner] LLM 规划失败，使用规则兜底: {}", e.getMessage());
            return PlanningResult.linear(goal, fallbackSteps(goal), tools);
        }
    }

    @Override
    public PlanNode generateAlternativePath(String goal, PlanNode failedNode, String failureReason, List<String> tools) {
        log.warn("[Planner] 生成绕行计划: 目标={}, 失败节点={}, 原因={}",
                goal, failedNode == null ? "?" : failedNode.getDescription(), failureReason);
        PlanNode altRoot = new PlanNode(
                "绕行方案: " + goal + "（避开: " + (failedNode == null ? "未知" : failedNode.getDescription()) + "）",
                PlanNode.NodeType.ROOT, tools);
        altRoot.setStatus(PlanNode.NodeStatus.COMPLETED);

        // 兜底路径：分解为更小粒度步骤，绕过失败点
        PlanNode prev = altRoot;
        List<String> fallback = fallbackSteps(goal);
        for (int i = 0; i < fallback.size(); i++) {
            String step = fallback.get(i);
            if (failedNode != null && step.equals(failedNode.getDescription())) {
                step = "（绕行）改用替代方式完成: " + step;
            }
            prev = prev.addChild(step, PlanNode.NodeType.STEP);
        }
        return altRoot;
    }

    @Override
    public void pruneBranch(PlanNode node) {
        if (node == null) return;
        node.setStatus(PlanNode.NodeStatus.PRUNED);
        log.info("[Planner] 剪枝节点: {}（得分 {}）", node.getDescription(), node.getScore());
    }

    /**
     * LLM 生成线性步骤列表
     */
    private List<String> llmGenerateSteps(String goal, List<String> tools) throws Exception {
        String prompt = """
                请为以下目标制定一个分步推理计划。
                可用工具：%s
                要求：3-6 步，每步一句话，直接输出步骤列表（每行一个步骤，不要编号以外的任何格式）。

                目标：%s
                """.formatted(tools == null ? "无" : String.join(", ", tools), goal);
        String response = modelManager.createChatClient().prompt().user(prompt).call().content();
        if (response == null || response.isBlank()) {
            return fallbackSteps(goal);
        }
        return response.lines()
                .map(l -> l.replaceAll("^\\s*\\d+[.、)]?\\s*", "").trim())
                .filter(l -> !l.isBlank() && !l.matches("(?i)(步骤|steps?|plan|计划)\\s*[:：]?\\s*$"))
                .limit(6)
                .toList();
    }

    /**
     * LLM 生成树状计划（根 → 分支 → 子步骤）
     */
    private PlanningResult generateTreePlan(String goal, List<String> tools) throws Exception {
        PlanNode root = new PlanNode(goal, PlanNode.NodeType.ROOT, tools);
        root.setStatus(PlanNode.NodeStatus.COMPLETED);

        String prompt = """
                请为以下目标设计树状推理计划（ToT），包含 2-3 个候选分支策略。
                格式：每行一个分支，格式为「分支名 || 策略描述」
                可用工具：%s

                目标：%s
                """.formatted(tools == null ? "无" : String.join(", ", tools), goal);
        String response = modelManager.createChatClient().prompt().user(prompt).call().content();
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("LLM 树状规划返回为空");
        }
        response.lines()
                .map(String::trim)
                .filter(l -> l.contains("||"))
                .limit(3)
                .forEach(l -> {
                    String[] parts = l.split("\\|\\|", 2);
                    PlanNode branch = root.addChild(parts[1].trim(), PlanNode.NodeType.BRANCH);
                    branch.addChild("执行并验证分支: " + parts[1].trim(), PlanNode.NodeType.STEP);
                });
        return PlanningResult.tree(goal, root, tools);
    }

    /**
     * 规则兜底步骤（LLM 不可用时的简化分解）
     */
    private List<String> fallbackSteps(String goal) {
        return List.of(
                "明确目标与约束: " + goal,
                "收集相关信息与数据",
                "制定候选方案并比较",
                "执行最优方案",
                "验证结果并总结"
        );
    }
}
