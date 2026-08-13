package com.ai.learning.planner.agent.reasoning;

import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 步骤评估器（Evaluator）
 * 在每一步推理分支执行后注入隐式奖励模型进行步骤评分（Score 0-1）。
 * 当分支得分低于 0.3 时自动触发剪枝（Pruning）并回退至上一步重试。
 */
@Slf4j
public class Evaluator {

    /** 剪枝阈值：得分低于该值触发剪枝 */
    public static final double PRUNE_THRESHOLD = 0.3;

    /** 分数提取正则（LLM 输出 "score: 0.85" 或 "0.85"） */
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?i)(?:score|得分)\\s*[:：]?\\s*(0?\\.\\d+|1\\.0|1|0)");

    private final ModelManager modelManager;

    public Evaluator(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    /**
     * 步骤评分结果
     *
     * @param score          评分（0-1）
     * @param rationale      评分理由
     * @param suggestedAction 建议动作（CONTINUE/PRUNE/REPLAN）
     */
    public record StepScore(double score, String rationale, SuggestedAction suggestedAction) {

        public boolean needsPruning() {
            return score < PRUNE_THRESHOLD;
        }

        public enum SuggestedAction {
            CONTINUE, PRUNE, REPLAN
        }
    }

    /**
     * 评估节点执行结果（LLM 隐式奖励模型 + 规则兜底）
     *
     * @param node       已执行的计划节点
     * @param stepOutput 节点执行输出
     */
    public StepScore evaluate(PlanNode node, String stepOutput) {
        // 1. 规则预检：输出为空/明显失败 → 直接低分（同步回写节点状态）
        if (stepOutput == null || stepOutput.isBlank()) {
            StepScore empty = new StepScore(0.0, "执行输出为空", StepScore.SuggestedAction.PRUNE);
            applyToNode(node, empty);
            return empty;
        }
        if (containsFailureMarker(stepOutput)) {
            StepScore failed = new StepScore(0.1, "执行输出包含失败标记", StepScore.SuggestedAction.PRUNE);
            applyToNode(node, failed);
            return failed;
        }

        // 2. LLM 隐式奖励模型评分
        try {
            if (modelManager != null) {
                String prompt = """
                        你是推理步骤的隐式奖励模型。请评估以下推理步骤的质量，给出 0-1 的得分。
                        评分标准：
                        - 0.9-1.0：步骤目标完全达成，输出与目标高度相关
                        - 0.6-0.89：部分达成，存在可修正的小偏差
                        - 0.3-0.59：明显偏离目标或信息不足
                        - 0-0.29：步骤失败或输出无价值（应剪枝）
                        只输出一行 JSON：{"score": 0.85, "reason": "简短理由"}

                        步骤目标：%s
                        步骤输出：%s
                        """.formatted(truncate(node == null ? "?" : node.getDescription(), 200),
                        truncate(stepOutput, 800));
                String response = modelManager.createChatClient().prompt().user(prompt).call().content();
                StepScore parsed = parseLlmScore(response);
                if (parsed != null) {
                    applyToNode(node, parsed);
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.warn("[Evaluator] LLM 评分失败，使用规则评分: {}", e.getMessage());
        }

        // 3. 规则兜底评分（基于输出长度与信息量）
        double score = ruleBasedScore(stepOutput);
        StepScore result = new StepScore(score, "规则评分: 输出长度 " + stepOutput.length() + " 字符",
                score < PRUNE_THRESHOLD ? StepScore.SuggestedAction.PRUNE : StepScore.SuggestedAction.CONTINUE);
        applyToNode(node, result);
        return result;
    }

    /**
     * 将评分回写节点并执行剪枝
     */
    private void applyToNode(PlanNode node, StepScore score) {
        if (node == null) return;
        node.setScore(score.score());
        if (score.needsPruning()) {
            node.setStatus(PlanNode.NodeStatus.PRUNED);
            log.warn("[Evaluator] 节点 {} 得分 {} 低于阈值 {}，触发剪枝回退",
                    node.getDescription(), String.format("%.2f", score.score()), String.format("%.2f", PRUNE_THRESHOLD));
        } else {
            node.setStatus(PlanNode.NodeStatus.COMPLETED);
        }
    }

    /**
     * 解析 LLM 的 JSON 评分输出
     */
    private StepScore parseLlmScore(String response) {
        if (response == null || response.isBlank()) return null;
        Matcher m = SCORE_PATTERN.matcher(response);
        double score;
        if (m.find()) {
            score = clamp(Double.parseDouble(m.group(1)));
        } else {
            // 尝试从 JSON 中提取
            Pattern jsonScore = Pattern.compile("\"score\"\\s*:\\s*(0?\\.\\d+|1\\.0|1|0)");
            Matcher jm = jsonScore.matcher(response);
            if (!jm.find()) return null;
            score = clamp(Double.parseDouble(jm.group(1)));
        }
        String reason = response.replaceAll("(?s).*?\"reason\"\\s*:\\s*\"([^\"]*)\".*", "$1");
        if (reason.equals(response)) reason = "LLM 评分: " + score;
        StepScore.SuggestedAction action = score < PRUNE_THRESHOLD
                ? StepScore.SuggestedAction.PRUNE
                : (score < 0.6 ? StepScore.SuggestedAction.REPLAN : StepScore.SuggestedAction.CONTINUE);
        return new StepScore(score, reason, action);
    }

    /**
     * 规则评分：基于输出信息量
     */
    private double ruleBasedScore(String output) {
        int len = output.length();
        if (len < 20) return 0.2;
        if (len < 60) return 0.45;
        if (len < 150) return 0.65;
        return 0.8;
    }

    private boolean containsFailureMarker(String output) {
        String lower = output.toLowerCase();
        return lower.contains("【工具失败】") || lower.contains("[tool_failed]")
                || lower.contains("工具不存在") || lower.contains("执行失败")
                || lower.contains("error");
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
