package com.ai.learning.planner.agent.orchestrator;

import com.ai.learning.planner.agent.base.BaseAgent;
import com.ai.learning.planner.agent.dto.TaskResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多 Agent 并行协作引擎
 * 支持：
 * 1. 并行调度：多个 Agent 同时执行不同子任务
 * 2. 结果聚合：合并多个 Agent 的输出
 * 3. 依赖执行：Agent A 完成后再调度 Agent B
 * 4. 投票决策：多个 Agent 对同一问题给出答案，取共识
 */
@Slf4j
public class CollaborativeOrchestrator {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 并行调度多个 Agent 执行不同子任务，等待全部完成
     *
     * @param tasks 任务列表：(Agent, 子任务描述)
     * @return 所有 Agent 的执行结果
     */
    public List<AgentResult> executeParallel(List<AgentTask> tasks) {
        log.info("[协作] 并行调度 {} 个 Agent", tasks.size());

        List<CompletableFuture<AgentResult>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> {
                    long start = System.currentTimeMillis();
                    try {
                        String result = task.agent().run(task.subTask());
                        long duration = System.currentTimeMillis() - start;
                        log.info("[协作] {} 完成，耗时 {}ms", task.agent().getName(), duration);
                        return new AgentResult(task.agent().getId(), task.agent().getName(),
                                result, duration, true, null);
                    } catch (Exception e) {
                        long duration = System.currentTimeMillis() - start;
                        log.error("[协作] {} 失败: {}", task.agent().getName(), e.getMessage());
                        return new AgentResult(task.agent().getId(), task.agent().getName(),
                                null, duration, false, e.getMessage());
                    }
                }, executor))
                .toList();

        // 等待全部完成
        return futures.stream()
                .map(f -> f.join())
                .toList();
    }

    /**
     * 链式依赖执行：按顺序执行，前一个 Agent 的输出作为下一个的输入
     *
     * @param chain 有序任务列表
     * @return 最后一个 Agent 的输出作为最终结果
     */
    public AgentResult executeChain(List<AgentTask> chain) {
        log.info("[协作] 链式执行 {} 个 Agent", chain.size());

        String currentInput = null;
        AgentResult lastResult = null;

        for (AgentTask task : chain) {
            String input = currentInput != null ? currentInput : task.subTask();
            long start = System.currentTimeMillis();
            try {
                String result = task.agent().run(input);
                long duration = System.currentTimeMillis() - start;
                lastResult = new AgentResult(task.agent().getId(), task.agent().getName(),
                        result, duration, true, null);
                currentInput = result; // 输出作为下一个的输入
                log.info("[协作] {} 完成，耗时 {}ms，输出长度 {}",
                        task.agent().getName(), duration, result.length());
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                lastResult = new AgentResult(task.agent().getId(), task.agent().getName(),
                        null, duration, false, e.getMessage());
                log.error("[协作] {} 失败，链式中断: {}", task.agent().getName(), e.getMessage());
                break;
            }
        }

        return lastResult;
    }

    /**
     * 投票决策：多个 Agent 对同一问题给出答案，汇总返回所有结果
     *
     * @param agents   参与投票的 Agent 列表
     * @param question 共同的问题
     * @return 所有 Agent 的回答
     */
    public List<AgentResult> vote(List<BaseAgent> agents, String question) {
        log.info("[协作] {} 个 Agent 投票: {}", agents.size(), question);

        List<AgentTask> tasks = agents.stream()
                .map(agent -> new AgentTask(agent, question))
                .toList();

        return executeParallel(tasks);
    }

    /**
     * 聚合多个 Agent 结果为单一文本
     */
    public String aggregateResults(List<AgentResult> results, String separator) {
        return results.stream()
                .filter(AgentResult::success)
                .map(r -> String.format("【%s】\n%s", r.agentName(), r.output()))
                .collect(Collectors.joining(separator));
    }

    /**
     * 任务定义
     */
    public record AgentTask(BaseAgent agent, String subTask) {
    }

    /**
     * 单个 Agent 执行结果
     */
    public record AgentResult(
            String agentId,
            String agentName,
            String output,
            long durationMs,
            boolean success,
            String error
    ) {
    }
}
