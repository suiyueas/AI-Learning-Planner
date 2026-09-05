package com.ai.learning.planner.controller;

import com.ai.learning.planner.agent.app.*;
import com.ai.learning.planner.agent.base.BaseAgent;
import com.ai.learning.planner.agent.orchestrator.CollaborativeOrchestrator;
import com.ai.learning.planner.interceptor.PointsInterceptor;
import com.ai.learning.planner.security.AuditService;
import com.ai.learning.planner.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 多 Agent 协作 API
 * 提供并行调度、链式执行、投票决策等协作模式
 */
@RestController
@RequestMapping("/agents/collaborate")
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
    private final PointsInterceptor pointsInterceptor;
    private final AuditService auditService;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 并行模式：多个 Agent 同时执行不同子任务
     * POST /api/agents/collaborate/parallel
     * Body: { "tasks": [{ "agentId": "planner", "subTask": "..." }, ...] }
     */
    @PostMapping("/parallel")
    public ResponseEntity<Map<String, Object>> executeParallel(
            @RequestBody Map<String, Object> request, Authentication authentication) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> tasks = (List<Map<String, String>>) request.get("tasks");
        String userId = securityContextHolder.getCurrentUserId();
        long start = System.currentTimeMillis();

        List<CollaborativeOrchestrator.AgentTask> agentTasks = tasks.stream()
                .map(t -> new CollaborativeOrchestrator.AgentTask(
                        resolveAgent(t.get("agentId")),
                        t.get("subTask")))
                .toList();

        // 积分检查：按任务数量扣费
        if (userId != null) {
            try {
                Long userIdLong = Long.parseLong(userId);
                // 每个子任务消耗一次 AGENT 积分
                for (int i = 0; i < agentTasks.size(); i++) {
                    pointsInterceptor.checkAndConsumeByFeature(userIdLong, "AGENT");
                }
            } catch (RuntimeException e) {
                auditService.logAgentExecution(userId, "collaboration-parallel",
                        "多Agent协作并行执行", false, System.currentTimeMillis() - start, e.getMessage());
                return ResponseEntity.badRequest().body(Map.of(
                        "error", e.getMessage(),
                        "code", "INSUFFICIENT_POINTS"
                ));
            }
        }

        List<CollaborativeOrchestrator.AgentResult> results = orchestrator.executeParallel(agentTasks);
        String aggregated = orchestrator.aggregateResults(results, "\n\n---\n\n");
        long duration = System.currentTimeMillis() - start;
        auditService.logAgentExecution(userId, "collaboration-parallel",
                "多Agent协作并行执行 " + agentTasks.size() + " 个任务", true, duration, null);

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
            @RequestBody Map<String, Object> request, Authentication authentication) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> steps = (List<Map<String, String>>) request.get("steps");
        String userId = securityContextHolder.getCurrentUserId();
        long start = System.currentTimeMillis();

        List<CollaborativeOrchestrator.AgentTask> chain = steps.stream()
                .map(s -> new CollaborativeOrchestrator.AgentTask(
                        resolveAgent(s.get("agentId")),
                        s.get("subTask")))
                .toList();

        // 积分检查：按步骤数量扣费
        if (userId != null) {
            try {
                Long userIdLong = Long.parseLong(userId);
                for (int i = 0; i < chain.size(); i++) {
                    pointsInterceptor.checkAndConsumeByFeature(userIdLong, "AGENT");
                }
            } catch (RuntimeException e) {
                auditService.logAgentExecution(userId, "collaboration-chain",
                        "多Agent协作链式执行", false, System.currentTimeMillis() - start, e.getMessage());
                return ResponseEntity.badRequest().body(Map.of(
                        "error", e.getMessage(),
                        "code", "INSUFFICIENT_POINTS"
                ));
            }
        }

        CollaborativeOrchestrator.AgentResult result = orchestrator.executeChain(chain);
        long duration = System.currentTimeMillis() - start;
        auditService.logAgentExecution(userId, "collaboration-chain",
                "多Agent协作链式执行 " + chain.size() + " 步", true, duration, null);

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
            @RequestBody Map<String, Object> request, Authentication authentication) {
        @SuppressWarnings("unchecked")
        List<String> agentIds = (List<String>) request.get("agentIds");
        String question = (String) request.get("question");
        String userId = securityContextHolder.getCurrentUserId();
        long start = System.currentTimeMillis();

        List<BaseAgent> agents = agentIds.stream()
                .map(this::resolveAgent)
                .toList();

        // 积分检查：按 Agent 数量扣费
        if (userId != null) {
            try {
                Long userIdLong = Long.parseLong(userId);
                for (int i = 0; i < agents.size(); i++) {
                    pointsInterceptor.checkAndConsumeByFeature(userIdLong, "AGENT");
                }
            } catch (RuntimeException e) {
                auditService.logAgentExecution(userId, "collaboration-vote",
                        "多Agent投票决策", false, System.currentTimeMillis() - start, e.getMessage());
                return ResponseEntity.badRequest().body(Map.of(
                        "error", e.getMessage(),
                        "code", "INSUFFICIENT_POINTS"
                ));
            }
        }

        List<CollaborativeOrchestrator.AgentResult> results = orchestrator.vote(agents, question);
        String aggregated = orchestrator.aggregateResults(results, "\n\n===\n\n");
        long duration = System.currentTimeMillis() - start;
        auditService.logAgentExecution(userId, "collaboration-vote",
                "多Agent投票决策 " + agents.size() + " 个Agent", true, duration, null);

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