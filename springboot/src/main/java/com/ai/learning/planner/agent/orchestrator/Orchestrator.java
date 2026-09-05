package com.ai.learning.planner.agent.orchestrator;

import com.ai.learning.planner.agent.app.*;
import com.ai.learning.planner.agent.base.BaseAgent;
import com.ai.learning.planner.agent.dto.AgentInfo;
import com.ai.learning.planner.agent.dto.ReasoningLevel;
import com.ai.learning.planner.agent.dto.TaskResult;
import com.ai.learning.planner.agent.dto.ThinkingProcess;
import com.ai.learning.planner.interceptor.PointsInterceptor;
import com.ai.learning.planner.service.AgentService;
import com.ai.learning.planner.service.ModelManager;
import com.ai.learning.planner.service.ReasoningTraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 编排器
 * 统一管理所有子Agent，根据用户意图分配任务
 * 支持同步执行和流式执行
 * 支持思考深度模式：快速/标准/深度
 */
@Slf4j
@Service
public class Orchestrator {

    private final ModelManager modelManager;
    private final AgentService agentService;
    private final PointsInterceptor pointsInterceptor;
    private final ReasoningTraceService reasoningTraceService;
    private final Map<String, BaseAgent> agentMap = new ConcurrentHashMap<>();
    private final List<BaseAgent> agentList = new ArrayList<>();
    private final ExecutorService orchestrationExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /** 主控Agent自身信息 */
    private final BaseAgent selfAgent;

    public Orchestrator(
            ModelManager modelManager,
            AgentService agentService,
            PointsInterceptor pointsInterceptor,
            ReasoningTraceService reasoningTraceService,
            DiagnosisAgent diagnosisAgent,
            AdvancedPlanningAgent advancedPlanningAgent,
            QAAgent qaAgent,
            ReportAgent reportAgent,
            InterventionAgent interventionAgent,
            GamificationAgent gamificationAgent,
            ExerciseAgent exerciseAgent) {

        this.modelManager = modelManager;
        this.agentService = agentService;
        this.pointsInterceptor = pointsInterceptor;
        this.reasoningTraceService = reasoningTraceService;

        // 注册所有子Agent（高级规划Agent替代原PlanningAgent，启用完整推理闭环）
        List<BaseAgent> agentsToRegister = List.of(
                diagnosisAgent,
                advancedPlanningAgent,
                qaAgent,
                reportAgent,
                interventionAgent,
                gamificationAgent,
                exerciseAgent
        );

        for (BaseAgent agent : agentsToRegister) {
            agent.setReasoningTraceService(reasoningTraceService);
            agentMap.put(agent.getId(), agent);
            agentList.add(agent);
        }

        // 汇总日志：一次性输出所有注册的 Agent
        String agentSummary = agentsToRegister.stream()
                .map(a -> a.getName() + "(" + a.getId() + ")")
                .collect(java.util.stream.Collectors.joining(", "));
        log.info("✅ 已注册 {} 个 Agent: [{}]", agentsToRegister.size(), agentSummary);

        // 创建主控Agent自身
        this.selfAgent = new BaseAgent("orchestrator", "编排Agent",
                "你是一个智能学习编排器，负责协调多个专业Agent完成学习任务。", modelManager) {
            @Override
            public String step(String input) {
                return identifyIntent(input);
            }

            @Override
            public String getDescription() {
                return "统一调度所有子Agent，协调多个专业智能体完成任务分配与回收";
            }

            @Override
            public List<String> getAvailableTools() {
                return List.copyOf(agentMap.keySet());
            }
        };
    }

    /**
     * 注册Agent（内部方法，保留用于动态注册场景）
     */
    private void registerAgent(BaseAgent agent) {
        agentMap.put(agent.getId(), agent);
        agentList.add(agent);
        log.debug("Agent已注册: {} ({})", agent.getName(), agent.getId());
    }

    /**
     * 获取所有Agent信息（包括主控）
     */
    public List<AgentInfo> getAllAgents() {
        List<AgentInfo> agents = new ArrayList<>();

        // 主控Agent
        agents.add(AgentInfo.builder()
                .id(selfAgent.getId())
                .name(selfAgent.getName())
                .description(selfAgent.getDescription())
                .type("orchestrator")
                .status(selfAgent.getState().name())
                .icon("brain")
                .role("统一调度·意图识别")
                .tools(List.of())
                .currentStep(selfAgent.getCurrentStep().get())
                .maxSteps(selfAgent.getMaxSteps())
                .build());

        // 子Agent
        for (BaseAgent agent : agentList) {
            agents.add(AgentInfo.builder()
                    .id(agent.getId())
                    .name(agent.getName())
                    .description(agent.getDescription())
                    .type("app")
                    .status(agent.getState().name())
                    .icon(getAgentIcon(agent.getId()))
                    .role(getAgentRole(agent.getId()))
                    .tools(agent.getAvailableTools())
                    .currentStep(agent.getCurrentStep().get())
                    .maxSteps(agent.getMaxSteps())
                    .build());
        }

        return agents;
    }

    /**
     * 执行任务（同步）
     * @param agentId 目标Agent ID
     * @param input 用户输入
     * @param reasoningLevel 思考深度模式
     * @return 任务执行结果
     */
    public TaskResult executeTask(String agentId, String input, ReasoningLevel reasoningLevel) {
        BaseAgent agent = resolveAgent(agentId);
        if (agent == null) {
            return TaskResult.builder()
                    .agentId(agentId)
                    .status("ERROR")
                    .error("Agent不存在: " + agentId)
                    .build();
        }

        log.info("[Orchestrator] 分配任务给 {} (思考模式: {}): {}", agent.getName(), reasoningLevel.getDescription(), input);

        // 如果agentId是orchestrator，走意图识别流程
        if ("orchestrator".equals(agentId)) {
            String intent = identifyIntent(input);
            BaseAgent targetAgent = agentMap.get(intent);
            if (targetAgent != null) {
                log.info("[Orchestrator] 意图识别结果: {}，转发给 {}", intent, targetAgent.getName());
                return executeTask(intent, input, reasoningLevel);
            }
        }

        // 设置思考深度模式
        agent.setReasoningLevel(reasoningLevel);

        long startTime = System.currentTimeMillis();
        String output = agent.run(input);
        long duration = System.currentTimeMillis() - startTime;

        // 保存执行日志到数据库
        try {
            List<Map<String, Object>> steps = agent.getMessageList();
            for (int i = 0; i < steps.size(); i++) {
                Map<String, Object> step = steps.get(i);
                String role = (String) step.getOrDefault("role", "system");
                String stepType = switch (role) {
                    case "assistant" -> "think";
                    default -> role;
                };
                String content = (String) step.getOrDefault("content", "");
                if (content != null && !content.isEmpty()) {
                    agentService.saveLog(agent.getId(), input, stepType, i + 1, content, "success");
                }
            }
            // 保存执行结果
            agentService.saveResult(agent.getId(), agent.getName(), input, "plan", output, "", null, "completed");
            log.info("[Orchestrator] 执行日志和结果已保存到数据库");
        } catch (Exception e) {
            log.warn("[Orchestrator] 保存执行日志/结果失败: {}", e.getMessage());
        }

        // 构建思考过程列表
        List<ThinkingProcess> thinkingProcess = agent.getThinkingProcessList();

        return TaskResult.builder()
                .agentId(agent.getId())
                .agentName(agent.getName())
                .status(agent.getState().name())
                .output(output)
                .steps(new ArrayList<>(agent.getMessageList()))
                .duration(duration)
                .totalSteps(agent.getCurrentStep().get())
                .reasoningLevel(reasoningLevel.getValue())
                .thinkingProcess(thinkingProcess)
                .traceId(agent.getCurrentTraceId())
                .build();
    }

    /**
     * 执行任务（同步）- 默认标准模式
     */
    public TaskResult executeTask(String agentId, String input) {
        return executeTask(agentId, input, ReasoningLevel.STANDARD);
    }

    /**
     * 执行任务（流式）
     * @param agentId 目标Agent ID
     * @param input 用户输入
     * @param emitter SSE发射器
     * @param reasoningLevel 思考深度模式
     */
    public void executeTaskStream(String agentId, String input, SseEmitter emitter, ReasoningLevel reasoningLevel) {
        BaseAgent agent = resolveAgent(agentId);
        if (agent == null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", "Agent不存在: " + agentId)));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE发送失败", e);
            }
            return;
        }

        log.info("[Orchestrator] 流式分配任务给 {} (思考模式: {}): {}", agent.getName(), reasoningLevel.getDescription(), input);

        // 设置思考深度模式
        agent.setReasoningLevel(reasoningLevel);

        // 发送思考模式信息
        try {
            emitter.send(SseEmitter.event()
                    .name("reasoning_mode")
                    .data(Map.of(
                            "level", reasoningLevel.getValue(),
                            "description", reasoningLevel.getDescription(),
                            "agentId", agent.getId(),
                            "agentName", agent.getName()
                    )));
        } catch (Exception e) {
            log.error("SSE发送思考模式信息失败", e);
        }

        // 如果agentId是orchestrator，走意图识别并转发
        if ("orchestrator".equals(agentId)) {
            String intent = identifyIntent(input);
            BaseAgent targetAgent = agentMap.get(intent);
            if (targetAgent != null) {
                log.info("[Orchestrator] 意图识别结果: {}，转发给 {}", intent, targetAgent.getName());
                try {
                    emitter.send(SseEmitter.event()
                            .name("intent_detected")
                            .data(Map.of("content", "分析意图: " + input + " -> 分配给 " + targetAgent.getName(),
                                    "step", 0)));
                } catch (Exception e) {
                    log.error("SSE发送失败", e);
                }
                executeTaskStream(intent, input, emitter, reasoningLevel);
                return;
            }
        }

        // 流式执行
        agent.runStream(input, emitter);

        // 流式执行完毕后保存日志和结果到数据库
        try {
            List<Map<String, Object>> steps = agent.getMessageList();
            for (int i = 0; i < steps.size(); i++) {
                Map<String, Object> step = steps.get(i);
                String role = (String) step.getOrDefault("role", "system");
                String stepType = switch (role) {
                    case "assistant" -> "think";
                    default -> role;
                };
                String content = (String) step.getOrDefault("content", "");
                if (content != null && !content.isEmpty()) {
                    agentService.saveLog(agent.getId(), input, stepType, i + 1, content, "success");
                }
            }
            log.info("[Orchestrator] 流式执行日志已保存到数据库");
        } catch (Exception e) {
            log.warn("[Orchestrator] 保存流式执行日志失败: {}", e.getMessage());
        }
    }

    /**
     * 执行任务（流式）- 默认标准模式
     */
    public void executeTaskStream(String agentId, String input, SseEmitter emitter) {
        executeTaskStream(agentId, input, emitter, ReasoningLevel.STANDARD);
    }

    // ==================== 🆕 多Agent编排核心 ====================

    public record SubTask(String agentId, String agentName, String description, String input) {}
    public record SubTaskResult(String agentId, String agentName, String description, String output, String status, String error) {}

    /**
     * 多Agent编排执行 - 流式
     * 1. 任务拆解 → 2. 并行派发 → 3. 结果聚合
     */
    public void executeMultiAgentStream(String input, SseEmitter emitter) {
        log.info("[Orchestrator] 多Agent编排启动: {}", input);
        try {
            // 1. 任务拆解
            emitter.send(SseEmitter.event().name("orchestration_start")
                    .data(Map.of("content", "🧠 开始编排任务: " + input)));
            List<SubTask> subTasks = decomposeTask(input);
            emitter.send(SseEmitter.event().name("decomposition")
                    .data(Map.of("content", "📋 拆解为 " + subTasks.size() + " 个子任务",
                            "subTasks", subTasks.stream().<Map<String, Object>>map(st -> Map.of("agentId", st.agentId(), "agentName", st.agentName(), "description", st.description())).collect(Collectors.toList()))));

            // 2. 并行派发
            List<CompletableFuture<SubTaskResult>> futures = new ArrayList<>();
            for (SubTask subTask : subTasks) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("subtask_start")
                                .data(Map.of("agentId", subTask.agentId(), "agentName", subTask.agentName(), "description", subTask.description())));
                        BaseAgent agent = agentMap.get(subTask.agentId());
                        if (agent == null) return new SubTaskResult(subTask.agentId(), subTask.agentName(), subTask.description(), "", "ERROR", "Agent不存在");
                        agent.setReasoningLevel(ReasoningLevel.STANDARD);
                        String output = agent.run(subTask.input());
                        emitter.send(SseEmitter.event().name("subtask_done")
                                .data(Map.of("agentId", subTask.agentId(), "agentName", subTask.agentName(), "description", subTask.description(), "outputPreview", output.length() > 200 ? output.substring(0, 200) + "..." : output)));
                        return new SubTaskResult(subTask.agentId(), subTask.agentName(), subTask.description(), output, "SUCCESS", null);
                    } catch (Exception e) {
                        log.error("子任务失败: {}", subTask.agentId(), e);
                        try { emitter.send(SseEmitter.event().name("subtask_error").data(Map.of("agentId", subTask.agentId(), "agentName", subTask.agentName(), "error", e.getMessage()))); } catch (Exception ignored) {}
                        return new SubTaskResult(subTask.agentId(), subTask.agentName(), subTask.description(), "", "ERROR", e.getMessage());
                    }
                }, orchestrationExecutor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            List<SubTaskResult> results = futures.stream().map(f -> { try { return f.get(); } catch (Exception e) { return new SubTaskResult("unknown", "未知", "", "", "ERROR", e.getMessage()); } }).collect(Collectors.toList());

            // 3. 结果聚合
            emitter.send(SseEmitter.event().name("aggregating").data(Map.of("content", "🔄 正在聚合 " + results.size() + " 个子任务结果...")));
            String aggregated = aggregateResults(input, results);
            emitter.send(SseEmitter.event().name("orchestration_done").data(Map.of("content", aggregated, "subTaskCount", subTasks.size(), "successCount", results.stream().filter(r -> "SUCCESS".equals(r.status())).count(), "errorCount", results.stream().filter(r -> "ERROR".equals(r.status())).count())));
            log.info("[Orchestrator] 编排完成: {} 个子任务", subTasks.size());
        } catch (Exception e) {
            log.error("编排执行失败", e);
            try { emitter.send(SseEmitter.event().name("error").data(Map.of("message", "编排失败: " + e.getMessage()))); } catch (Exception ex) { log.warn("错误发送失败", ex); }
        }
    }

    /** 任务拆解：LLM将复杂任务拆解为子任务 */
    private List<SubTask> decomposeTask(String input) {
        String agentListStr = agentList.stream().map(a -> "- " + a.getId() + ": " + a.getName() + " - " + a.getDescription()).collect(Collectors.joining("\n"));
        String prompt = """
            你是一个任务拆解器。请将用户的学习任务拆解为1-5个子任务。
            可用Agent：%s
            用户输入：%s
            只返回JSON数组：[{"agentId":"xxx","description":"xxx","input":"xxx"}]
            """.formatted(agentListStr, input);
        try {
            String response = modelManager.createChatClient().prompt().system(prompt).user(input).call().content();
            String json = extractJsonArray(response);
            if (json == null) return List.of(new SubTask("planner", "规划Agent", "制定学习计划", input));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> taskList = mapper.readValue(json, List.class);
            List<SubTask> subTasks = new ArrayList<>();
            for (Map<String, String> task : taskList) {
                BaseAgent agent = agentMap.get(task.get("agentId"));
                if (agent == null) continue;
                subTasks.add(new SubTask(task.get("agentId"), agent.getName(), task.get("description"), task.get("input") != null ? task.get("input") : input));
            }
            return subTasks.isEmpty() ? List.of(new SubTask("planner", "规划Agent", "制定学习计划", input)) : subTasks;
        } catch (Exception e) {
            log.error("任务拆解失败", e);
            return List.of(new SubTask("planner", "规划Agent", "制定学习计划", input));
        }
    }

    /** 结果聚合：LLM整合所有子任务结果 */
    private String aggregateResults(String originalInput, List<SubTaskResult> results) {
        String resultsStr = results.stream().map(r -> "【" + r.agentName() + "】\n状态: " + r.status() + "\n输出: " + (r.output() != null ? r.output() : "(无)")).collect(Collectors.joining("\n---\n"));
        String prompt = """
            请将以下Agent执行结果整合为一份综合报告。
            用户需求：%s
            执行结果：%s
            格式：Markdown，包含概述、详细内容、总结建议。
            """.formatted(originalInput, resultsStr);
        try {
            String aggregated = modelManager.createChatClient().prompt().system(prompt).user(originalInput).call().content();
            return aggregated != null ? aggregated : "聚合失败";
        } catch (Exception e) { return "聚合失败: " + e.getMessage() + "\n\n原始输出:\n" + resultsStr; }
    }

    private String extractJsonArray(String text) {
        if (text == null) return null;
        int start = text.indexOf('['); int end = text.lastIndexOf(']');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : null;
    }

    // ==================== 原有方法 ====================

    /**
     * 获取指定Agent状态
     */
    public AgentInfo getAgentStatus(String agentId) {
        BaseAgent agent = resolveAgent(agentId);
        if (agent == null) {
            return null;
        }

        return AgentInfo.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .status(agent.getState().name())
                .currentStep(agent.getCurrentStep().get())
                .maxSteps(agent.getMaxSteps())
                .build();
    }

    /**
     * 停止Agent执行
     */
    public void stopAgent(String agentId) {
        BaseAgent agent = resolveAgent(agentId);
        if (agent != null) {
            log.info("[Orchestrator] 停止Agent: {}", agent.getName());
            agent.reset();
        }
    }

    /**
     * 解析Agent
     */
    private BaseAgent resolveAgent(String agentId) {
        if ("orchestrator".equals(agentId)) {
            return selfAgent;
        }
        return agentMap.get(agentId);
    }

    /**
     * 意图识别
     */
    private String identifyIntent(String userInput) {
        String intentPrompt = """
            请分析用户输入的意图，从以下选项中选择一个：
            - diagnosis: 能力测评、诊断水平、评估
            - planner: 学习计划、规划路径、制定目标
            - tutor: 问题、答疑、解释、什么是
            - reporter: 学习报告、查看进度、统计
            - searcher: 联网搜索、查找资料、搜索
            - intervention: 鼓励、支持、帮助、提醒
            - motivator: 成就、打卡、徽章、激励
            - exercise: 习题、练习、做题、测试
            - general: 其他
            只返回意图名称（小写英文单词），不要返回其他内容。
            """;

        String intent = modelManager.createChatClient().prompt()
                .system(intentPrompt)
                .user(userInput)
                .call()
                .content()
                .trim()
                .toLowerCase();

        // 验证意图是否合法
        List<String> validIntents = List.of("diagnosis", "planner", "tutor", "reporter",
                "searcher", "intervention", "motivator", "exercise");

        if (!validIntents.contains(intent)) {
            intent = "general";
        }

        return intent;
    }

    /**
     * 获取Agent图标（供前端使用）
     */
    private String getAgentIcon(String agentId) {
        return switch (agentId) {
            case "diagnosis" -> "search";
            case "planner" -> "map";
            case "tutor" -> "message-square";
            case "reporter" -> "bar-chart";
            case "searcher" -> "globe";
            case "intervention" -> "bell";
            case "motivator" -> "award";
            case "exercise" -> "edit-3";
            default -> "cpu";
        };
    }

    /**
     * 获取Agent角色描述
     */
    private String getAgentRole(String agentId) {
        return switch (agentId) {
            case "diagnosis" -> "能力测评·画像构建";
            case "planner" -> "路径生成·动态调整";
            case "tutor" -> "苏格拉底·RAG";
            case "reporter" -> "学情分析·PDF导出";
            case "searcher" -> "联网搜索·补充资料";
            case "intervention" -> "行为监测·主动干预";
            case "motivator" -> "成就解锁·打卡管理";
            case "exercise" -> "习题生成·智能批改";
            default -> "";
        };
    }
}