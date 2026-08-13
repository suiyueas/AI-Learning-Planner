package com.ai.learning.planner.agent.reasoning;

import java.time.Instant;
import java.util.List;

/**
 * 规划结果：支持线性 / 树状（ToT）/ 图状（GoT）拓扑
 */
public class PlanningResult {

    /** 目标描述 */
    private final String goal;

    /** 推理拓扑 */
    private final Topology topology;

    /** 计划根节点 */
    private final PlanNode root;

    /** 绕行计划（动态重规划产物） */
    private final List<PlanNode> alternativePaths;

    /** 生成时间 */
    private final Instant generatedAt;

    public PlanningResult(String goal, Topology topology, PlanNode root, List<PlanNode> alternativePaths) {
        this.goal = goal;
        this.topology = topology;
        this.root = root;
        this.alternativePaths = alternativePaths == null ? List.of() : alternativePaths;
        this.generatedAt = Instant.now();
    }

    /**
     * 构建线性计划：根 → 唯一子节点链
     */
    public static PlanningResult linear(String goal, List<String> steps, List<String> tools) {
        PlanNode root = new PlanNode(goal, PlanNode.NodeType.ROOT, tools);
        root.setStatus(PlanNode.NodeStatus.COMPLETED);
        PlanNode prev = root;
        for (String step : steps) {
            prev = prev.addChild(step, PlanNode.NodeType.STEP);
        }
        return new PlanningResult(goal, Topology.LINEAR, root, List.of());
    }

    /**
     * 构建树状计划：每个分支节点下挂多个候选子步骤（ToT）
     */
    public static PlanningResult tree(String goal, PlanNode root, List<String> tools) {
        return new PlanningResult(goal, Topology.TREE, root, List.of());
    }

    public String getGoal() {
        return goal;
    }

    public Topology getTopology() {
        return topology;
    }

    public PlanNode getRoot() {
        return root;
    }

    public List<PlanNode> getAlternativePaths() {
        return alternativePaths;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    /**
     * 推理拓扑
     */
    public enum Topology {
        LINEAR, TREE, GRAPH
    }
}
