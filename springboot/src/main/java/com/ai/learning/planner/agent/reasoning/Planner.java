package com.ai.learning.planner.agent.reasoning;

import java.util.List;

/**
 * 推理规划器（Planner）
 * 支持生成线性 / 树状（ToT）/ 图状（GoT）推理结构；
 * 在路径受阻时生成绕行计划（generateAlternativePath）
 */
public interface Planner {

    /**
     * 生成推理计划
     *
     * @param goal       推理目标
     * @param tools      可用工具
     * @param topology   推理拓扑（LINEAR/TREE/GRAPH）
     * @return 规划结果（含根节点树）
     */
    PlanningResult generatePlan(String goal, List<String> tools, PlanningResult.Topology topology);

    /**
     * 动态重规划：生成绕行计划
     * 当工具连续调用失败或上下文超阈值时强制中断当前路径，调用本方法生成替代路径
     *
     * @param goal        原始目标
     * @param failedNode  失败节点（当前路径断点）
     * @param failureReason 失败原因（工具失败/上下文超限）
     * @param tools       可用工具
     * @return 绕行计划（作为新根节点）
     */
    PlanNode generateAlternativePath(String goal, PlanNode failedNode, String failureReason, List<String> tools);

    /**
     * 剪枝：将分支节点标记为 PRUNED（评估得分低于阈值时由 Evaluator 触发）
     */
    void pruneBranch(PlanNode node);
}
