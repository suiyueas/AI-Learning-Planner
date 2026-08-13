package com.ai.learning.planner.agent.reasoning;

import com.ai.learning.planner.TestLlmSupport;
import com.ai.learning.planner.service.ModelManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 步骤评估器测试（隐式奖励模型 + 规则兜底）
 */
class EvaluatorTest {

    private Evaluator evaluator;
    private ModelManager modelManager;

    @BeforeEach
    void setUp() {
        TestLlmSupport.reset();
    }

    private Evaluator withMockModel() {
        modelManager = mock(ModelManager.class);
        TestLlmSupport.bindChatClient(modelManager);
        return new Evaluator(modelManager);
    }

    @Test
    void evaluate_emptyOutputScoresZero() {
        evaluator = withMockModel();
        PlanNode node = new PlanNode("步骤", PlanNode.NodeType.STEP, List.of());
        var score = evaluator.evaluate(node, "");
        assertEquals(0.0, score.score());
        assertTrue(score.needsPruning());
    }

    @Test
    void evaluate_failureMarkerPrunes() {
        evaluator = withMockModel();
        PlanNode node = new PlanNode("步骤", PlanNode.NodeType.STEP, List.of());
        var score = evaluator.evaluate(node, "【工具失败】search_resources: 连接超时");
        assertTrue(score.needsPruning());
        assertEquals(PlanNode.NodeStatus.PRUNED, node.getStatus());
    }

    @Test
    void evaluate_ruleBasedFallback() {
        evaluator = new Evaluator(null); // 无 LLM → 规则兜底
        PlanNode node = new PlanNode("步骤", PlanNode.NodeType.STEP, List.of());
        var score = evaluator.evaluate(node, "已获取到10条学习资源，包括书籍3本、视频5个、课程2门，覆盖了入门到进阶的全部阶段");
        assertFalse(score.needsPruning());
        assertEquals(PlanNode.NodeStatus.COMPLETED, node.getStatus());
    }

    @Test
    void evaluate_shortOutputLowScore() {
        evaluator = new Evaluator(null);
        PlanNode node = new PlanNode("步骤", PlanNode.NodeType.STEP, List.of());
        var score = evaluator.evaluate(node, "完成了");
        assertTrue(score.needsPruning());
    }

    @Test
    void evaluate_llmScoreParsed() {
        evaluator = withMockModel();
        TestLlmSupport.queue("{\"score\": 0.88, \"reason\": \"步骤达成良好\"}");
        PlanNode node = new PlanNode("步骤", PlanNode.NodeType.STEP, List.of());
        var score = evaluator.evaluate(node, "输出内容比较充分，包含结构化结果");
        assertEquals(0.88, score.score(), 0.001);
        assertFalse(score.needsPruning());
        assertEquals(PlanNode.NodeStatus.COMPLETED, node.getStatus());
    }

    @Test
    void evaluate_lowLlmScorePrunes() {
        evaluator = withMockModel();
        TestLlmSupport.queue("{\"score\": 0.15, \"reason\": \"输出与目标无关\"}");
        PlanNode node = new PlanNode("步骤", PlanNode.NodeType.STEP, List.of());
        var score = evaluator.evaluate(node, "一些输出");
        assertTrue(score.needsPruning());
        assertEquals(PlanNode.NodeStatus.PRUNED, node.getStatus());
    }
}
