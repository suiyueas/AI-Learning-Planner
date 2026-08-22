package com.ai.learning.planner.controller;

import com.ai.learning.planner.agent.app.*;
import com.ai.learning.planner.agent.base.BaseAgent;
import com.ai.learning.planner.agent.orchestrator.CollaborativeOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 多 Agent 协作 API
 * 提供并行调度、链式执行、投票决策等协作模式
 */
@RestController
@RequestMapping("/api/agents/collaborate")
@RequiredArgsConstructor
public class AgentCollaborationController {

    private final CollaborativeOrchestrator orchestrator;
    private final DiagnosisAgent diagnosisAgent;
    private final PlanningAgent planningAgent;
    private final QAAgent qaAgent;
    private final ReportAgent reportAgent;
    private final ExerciseAgent exerciseAgent;
    private final InterventionAgent interventionAgent;
    private final GamificationAgent gamificationAgent;

    /**
     * 并行模式：多个 Agent 同时执行不同子任务
     * POST /api/agents/collaborate/parallel
     * Body: { "tasks": [{ "agentId": "planner", "subTask": "..." }, ...] }
     */
    @PostMapping("/parallel")
    public ResponseEntity<Map<String, Object>> executeParallel(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> tasks = (List<Map<String, String>>) request.get("tasks");

        List<CollaborativeOrchestrator.AgentTask> agentTasks = tasks.stream()
                .map(t -> new CollaborativeOrchestrator.AgentTask(
                        resolveAgent(t.get("agentId")),
                        t.get("subTask")))
                .toList();

        List<CollaborativeOrchestrator.AgentResult> results = orchestrator.executeParallel(agentTasks);
        String aggregated = orchestrator.aggregateResults(results, "\n\n---\n\n");

        return ResponseEntity.ok(Map.of(
                "mode", "parallel",
                "results", results,
                "aggregated", aggregated
        ));
    }

    /**
     * 链式模式：Agent A → B → C，前一个输出作为后一个输入
     * POST /api/agents/collaborate/chain
     * Body: { "steps": [{ "agentId": "diagnosis", "subTask": "测评Java水平" }, ...] }
     */
    @PostMapping("/chain")
    public ResponseEntity<Map<String, Object>> executeChain(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> steps = (List<Map<String, String>>) request.get("steps");

        List<CollaborativeOrchestrator.AgentTask> chain = steps.stream()
                .map(s -> new CollaborativeOrchestrator.AgentTask(
                        resolveAgent(s.get("agentId")),
                        s.get("subTask")))
                .toList();

        CollaborativeOrchestrator.AgentResult result = orchestrator.executeChain(chain);

        return ResponseEntity.ok(Map.of(
                "mode", "chain",
                "finalResult", result,
                "steps", steps.size()
        ));
    }

    /**
     * 投票模式：多个 Agent 对同一问题给出答案
     * POST /api/agents/collaborate/vote
     * Body: { "agentIds": ["planner", "tutor", "diagnosis"], "question": "..." }
     */
    @PostMapping("/vote")
    public ResponseEntity<Map<String, Object>> vote(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> agentIds = (List<String>) request.get("agentIds");
        String question = (String) request.get("question");

        List<BaseAgent> agents = agentIds.stream()
                .map(this::resolveAgent)
                .toList();

        List<CollaborativeOrchestrator.AgentResult> results = orchestrator.vote(agents, question);
        String aggregated = orchestrator.aggregateResults(results, "\n\n===\n\n");

        return ResponseEntity.ok(Map.of(
                "mode", "vote",
                "question", question,
                "results", results,
                "aggregated", aggregated
        ));
    }

    private BaseAgent resolveAgent(Object id) {
        return switch (String.valueOf(id)) {
            case "diagnosis" -> diagnosisAgent;
            case "planner" -> planningAgent;
            case "tutor" -> qaAgent;
            case "reporter" -> reportAgent;
            case "exercise" -> exerciseAgent;
            case "intervention" -> interventionAgent;
            case "motivator" -> gamificationAgent;
            default -> throw new IllegalArgumentException("未知 Agent: " + id);
        };
    }
}
