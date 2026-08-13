package com.ai.learning.planner.agent.reasoning;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 推理计划节点（树状 ToT / 图状 GoT 结构）
 * 每个节点代表一个推理步骤或分支，支持父/子关系与评分状态
 */
public class PlanNode {

    /** 节点唯一标识 */
    private final String id;

    /** 节点描述（该步要做什么） */
    private final String description;

    /** 节点类型：步骤 / 分支 / 合并 / 根 */
    private final NodeType type;

    /** 父节点 */
    private PlanNode parent;

    /** 子节点（分支展开） */
    private final List<PlanNode> children = new ArrayList<>();

    /** 评估得分（0-1，Evaluator 注入） */
    private volatile double score = -1;

    /** 节点状态 */
    private volatile NodeStatus status = NodeStatus.PENDING;

    /** 执行结果 */
    private volatile String result;

    /** 规划时的可用工具 */
    private final List<String> tools;

    public PlanNode(String description, NodeType type, List<String> tools) {
        this.id = "pn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        this.description = description;
        this.type = type;
        this.tools = tools == null ? List.of() : List.copyOf(tools);
    }

    /**
     * 添加子节点
     */
    public PlanNode addChild(String childDescription, NodeType childType) {
        PlanNode child = new PlanNode(childDescription, childType, tools);
        child.parent = this;
        children.add(child);
        return child;
    }

    /**
     * 获取祖先路径（从根到当前节点的描述链）
     */
    public List<String> pathDescriptions() {
        List<String> path = new ArrayList<>();
        PlanNode cur = this;
        while (cur != null) {
            path.add(0, cur.description);
            cur = cur.parent;
        }
        return path;
    }

    /**
     * 获取根节点
     */
    public PlanNode root() {
        PlanNode cur = this;
        while (cur.parent != null) {
            cur = cur.parent;
        }
        return cur;
    }

    /**
     * 收集所有 PENDING 状态的叶子节点（下一步候选）
     */
    public List<PlanNode> pendingLeaves() {
        List<PlanNode> leaves = new ArrayList<>();
        collectPendingLeaves(this, leaves);
        return leaves;
    }

    private void collectPendingLeaves(PlanNode node, List<PlanNode> acc) {
        if (node.children.isEmpty()) {
            if (node.status == NodeStatus.PENDING) {
                acc.add(node);
            }
            return;
        }
        for (PlanNode child : node.children) {
            collectPendingLeaves(child, acc);
        }
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public NodeType getType() {
        return type;
    }

    public PlanNode getParent() {
        return parent;
    }

    public List<PlanNode> getChildren() {
        return children;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<String> getTools() {
        return tools;
    }

    /**
     * 节点类型
     */
    public enum NodeType {
        ROOT, STEP, BRANCH, MERGE
    }

    /**
     * 节点状态
     */
    public enum NodeStatus {
        PENDING, RUNNING, COMPLETED, PRUNED, FAILED
    }
}
