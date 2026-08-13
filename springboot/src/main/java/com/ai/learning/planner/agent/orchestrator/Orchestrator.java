package com.ai.learning.planner.agent.orchestrator;

import com.ai.learning.planner.agent.app.*;
import com.ai.learning.planner.agent.base.BaseAgent;
import com.ai.learning.planner.agent.dto.AgentInfo;
import com.ai.learning.planner.agent.dto.TaskResult;
import com.ai.learning.planner.service.AgentService;
import com.ai.learning.planner.service.ModelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 编排器
 * 统一管理所有子Agent，根据用户意图分配任务
 * 支持同步执行和流式执行
 */
@Slf4j
@Service
public class Orchestrator {

    private final ModelManager modelManager;
    private final AgentService agentService;
    private final Map<String, BaseAgent> agentMap = new ConcurrentHashMap<>();
    private final List<BaseAgent> agentList = new ArrayList<>();

    /** 主控Agent自身信息 */
    private final BaseAgent selfAgent;

    public Orchestrator(
            ModelManager modelManager,
            AgentService agentService,
            DiagnosisAgent diagnosisAgent,
            PlanningAgent planningAgent,
            QAAgent qaAgent,
            ReportAgent reportAgent,
            InterventionAgent interventionAgent,
            GamificationAgent gamificationAgent,
            ExerciseAgent exerciseAgent) {

        this.modelManager = modelManager;
        this.agentService = agentService;

        // 注册所有子Agent（暂存到列表，最后统一汇总日志）
        List<BaseAgent> agentsToRegister = List.of(
                diagnosisAgent,
                planningAgent,
                qaAgent,
                reportAgent,
                interventionAgent,
                gamificationAgent,
                exerciseAgent
        );

        for (BaseAgent agent : agentsToRegister) {
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
     */
    public TaskResult executeTask(String agentId, String input) {
        BaseAgent agent = resolveAgent(agentId);
        if (agent == null) {
            return TaskResult.builder()
                    .agentId(agentId)
                    .status("ERROR")
                    .error("Agent不存在: " + agentId)
                    .build();
        }

        log.info("[Orchestrator] 分配任务给 {}: {}", agent.getName(), input);

        // 如果agentId是orchestrator，走意图识别流程
        if ("orchestrator".equals(agentId)) {
            String intent = identifyIntent(input);
            BaseAgent targetAgent = agentMap.get(intent);
            if (targetAgent != null) {
                log.info("[Orchestrator] 意图识别结果: {}，转发给 {}", intent, targetAgent.getName());
                return executeTask(intent, input);
            }
        }

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

        return TaskResult.builder()
                .agentId(agent.getId())
                .agentName(agent.getName())
                .status(agent.getState().name())
                .output(output)
                .steps(new ArrayList<>(agent.getMessageList()))
                .duration(duration)
                .totalSteps(agent.getCurrentStep().get())
                .build();
    }

    /**
     * 执行任务（流式）
     */
    public void executeTaskStream(String agentId, String input, SseEmitter emitter) {
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

        log.info("[Orchestrator] 流式分配任务给 {}: {}", agent.getName(), input);

        // 如果agentId是orchestrator，走意图识别并转发
        if ("orchestrator".equals(agentId)) {
            String intent = identifyIntent(input);
            BaseAgent targetAgent = agentMap.get(intent);
            if (targetAgent != null) {
                log.info("[Orchestrator] 意图识别结果: {}，转发给 {}", intent, targetAgent.getName());
                try {
                    emitter.send(SseEmitter.event()
                            .name("think")
                            .data(Map.of("content", "分析意图: " + input + " -> 分配给" + targetAgent.getName(),
                                    "step", 0)));
                } catch (Exception e) {
                    log.error("SSE发送失败", e);
                }
                executeTaskStream(intent, input, emitter);
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