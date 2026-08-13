package com.ai.learning.planner.agent.base;

import com.ai.learning.planner.service.ModelManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基础智能体抽象基类
 * 定义智能体的核心生命周期：run() → step() 循环 → FINISHED/ERROR
 * 支持同步执行和流式执行（SseEmitter）
 */
@Slf4j
@Getter
public abstract class BaseAgent {

    /** 智能体唯一标识 */
    protected final String id;

    /** 智能体名称 */
    protected final String name;

    /** 当前状态 */
    protected volatile AgentState state = AgentState.IDLE;

    /** 消息列表（记录执行过程中的所有消息；Agent 为单例 Bean，并发执行时需线程安全） */
    protected final List<Map<String, Object>> messageList = new CopyOnWriteArrayList<>();

    /** 最大执行步数 */
    protected final int maxSteps;

    /** 当前迭代次数 */
    protected final AtomicInteger currentStep = new AtomicInteger(0);

    /** 系统提示词 */
    protected final String systemPrompt;

    /** 模型管理器 */
    protected final ModelManager modelManager;

    /** 执行开始时间 */
    protected long startTime;

    /** SseEmitter 实例（流式输出用） */
    protected SseEmitter sseEmitter;

    /** 子Agent列表（用于Orchestrator） */
    protected final List<BaseAgent> subAgents = new ArrayList<>();

    protected BaseAgent(String id, String name, String systemPrompt, ModelManager modelManager) {
        this(id, name, systemPrompt, modelManager, 30);
    }

    protected BaseAgent(String id, String name, String systemPrompt, ModelManager modelManager, int maxSteps) {
        this.id = id;
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.modelManager = modelManager;
        this.maxSteps = maxSteps;
    }

    /**
     * 同步执行入口
     * @param input 用户输入
     * @return 执行结果文本
     */
    public String run(String input) {
        state = AgentState.RUNNING;
        startTime = System.currentTimeMillis();
        currentStep.set(0);
        messageList.clear();

        addMessage("system", "开始执行任务: " + input);
        log.info("[{}] 开始执行: {}", name, input);

        StringBuilder result = new StringBuilder();

        while (state == AgentState.RUNNING && currentStep.get() < maxSteps) {
            try {
                String stepResult = step(input);
                if (stepResult != null) {
                    result.append(stepResult).append("\n");
                }
                currentStep.incrementAndGet();
            } catch (Exception e) {
                log.error("[{}] 执行出错: {}", name, e.getMessage(), e);
                addMessage("error", "执行出错: " + e.getMessage());
                state = AgentState.ERROR;
                break;
            }
        }

        if (state == AgentState.RUNNING) {
            state = AgentState.FINISHED;
        }

        long duration = System.currentTimeMillis() - startTime;
        addMessage("system", "任务执行完成，耗时: " + duration + "ms，步数: " + currentStep.get());
        log.info("[{}] 任务执行完成，耗时: {}ms，步数: {}", name, duration, currentStep.get());

        return result.toString();
    }

    /**
     * 流式执行入口（基于SseEmitter）
     * @param input 用户输入
     * @param emitter SseEmitter实例
     */
    public void runStream(String input, SseEmitter emitter) {
        this.sseEmitter = emitter;
        state = AgentState.RUNNING;
        startTime = System.currentTimeMillis();
        currentStep.set(0);
        messageList.clear();

        try {
            pushEvent("status", Map.of("state", "RUNNING", "message", "开始执行任务: " + input));
            addMessage("system", "开始执行任务: " + input);
            log.info("[{}] 开始流式执行: {}", name, input);

            while (state == AgentState.RUNNING && currentStep.get() < maxSteps) {
                try {
                    String stepResult = step(input);
                    if (stepResult != null) {
                        pushEvent("step_result", Map.of("step", currentStep.get(), "result", stepResult));
                    }
                    currentStep.incrementAndGet();
                } catch (Exception e) {
                    log.error("[{}] 执行出错: {}", name, e.getMessage(), e);
                    addMessage("error", "执行出错: " + e.getMessage());
                    pushEvent("error", Map.of("message", e.getMessage(), "step", currentStep.get()));
                    state = AgentState.ERROR;
                    break;
                }
            }

            if (state == AgentState.RUNNING) {
                state = AgentState.FINISHED;
            }

            long duration = System.currentTimeMillis() - startTime;
            pushEvent("complete", Map.of(
                    "state", state.name(),
                    "duration", duration,
                    "steps", currentStep.get(),
                    "message", "任务执行完成"
            ));
            log.info("[{}] 流式执行完成，耗时: {}ms", name, duration);

        } catch (Exception e) {
            log.error("[{}] 流式执行异常: {}", name, e.getMessage(), e);
            pushEvent("error", Map.of("message", e.getMessage()));
            state = AgentState.ERROR;
        } finally {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 单步执行（由子类实现）
     * @param input 用户输入
     * @return 当前步骤的结果文本
     */
    public abstract String step(String input);

    /**
     * 添加消息到消息列表
     */
    protected void addMessage(String role, String content) {
        messageList.add(Map.of(
                "role", role,
                "content", content,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 推送SSE事件
     */
    protected void pushEvent(String event, Object data) {
        if (sseEmitter != null) {
            try {
                sseEmitter.send(SseEmitter.event()
                        .name(event)
                        .data(data));
            } catch (IOException e) {
                log.warn("[{}] SSE推送失败: {}", name, e.getMessage());
            }
        }
    }

    /**
     * 推送思考步骤事件
     */
    protected void pushThink(String content) {
        pushEvent("think", Map.of("content", content, "step", currentStep.get()));
        addMessage("assistant", "[思考] " + content);
    }

    /**
     * 推进行动步骤事件
     */
    protected void pushAction(String toolName, Object args, Object result) {
        pushEvent("act", Map.of(
                "tool", toolName,
                "args", args,
                "result", result,
                "step", currentStep.get()
        ));
        addMessage("assistant", "[行动] 调用工具: " + toolName);
    }

    /**
     * 推送观察步骤事件
     */
    protected void pushObserve(String observation) {
        pushEvent("observe", Map.of("content", observation, "step", currentStep.get()));
        addMessage("assistant", "[观察] " + observation);
    }

    /**
     * 获取状态文本
     */
    public String getStateText() {
        return switch (state) {
            case IDLE -> "空闲待命";
            case RUNNING -> "执行中";
            case FINISHED -> "执行完成";
            case ERROR -> "执行出错";
        };
    }

    /**
     * 重置智能体状态
     */
    public void reset() {
        state = AgentState.IDLE;
        currentStep.set(0);
        messageList.clear();
        sseEmitter = null;
    }

    /**
     * 获取执行时长（毫秒）
     */
    public long getDuration() {
        if (startTime == 0) return 0;
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取Agent描述信息
     */
    public abstract String getDescription();

    /**
     * 获取可用工具列表
     */
    public abstract List<String> getAvailableTools();
}
