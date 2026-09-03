package com.ai.learning.planner.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Agent工具调用管理器
 * 负责工具的注册、调用和结果回调
 * 独立于Spring AI内置工具调用机制
 * 注意：名称避免与Spring AI的ToolCallingManager冲突
 */
@Slf4j
@Component
public class AgentToolManager {

    /** 工具注册表 */
    private final Map<String, ToolEntry> toolRegistry = new ConcurrentHashMap<>();

    /** 工具执行监听器 */
    private final List<ToolExecutionListener> listeners = new ArrayList<>();

    /** 当前工具调用上下文的用户ID（InheritableThreadLocal 支持子线程继承） */
    private static final InheritableThreadLocal<String> CURRENT_USER_ID = new InheritableThreadLocal<>();

    /**
     * 设置当前工具调用的用户上下文（必须在调用 execute 前设置）
     */
    public void setUserContext(String userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 清除当前用户上下文（工具执行完毕后调用）
     */
    public void clearUserContext() {
        CURRENT_USER_ID.remove();
    }

    /**
     * 获取当前工具调用上下文的用户ID（供工具内部调用）
     */
    public static String getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 注册工具
     * @param name 工具名称
     * @param function 工具执行函数
     * @param description 工具描述
     */
    public void registerTool(String name, Function<Map<String, Object>, String> function, String description) {
        toolRegistry.put(name, new ToolEntry(name, function, description));
        log.info("注册工具: {} - {}", name, description);
    }

    /**
     * 注册工具（含参数定义）
     */
    public void registerTool(String name, Function<Map<String, Object>, String> function,
                              String description, Map<String, String> parameters) {
        toolRegistry.put(name, new ToolEntry(name, function, description, parameters));
        log.info("注册工具(带参数): {} - {}", name, description);
    }

    /**
     * 执行工具调用（无用户上下文，游客模式）
     * @param name 工具名称
     * @param args 参数Map
     * @return 执行结果
     */
    public String execute(String name, Map<String, Object> args) {
        return execute(name, args, null);
    }

    /**
     * 执行工具调用（带用户上下文，用于数据隔离）
     * @param name 工具名称
     * @param args 参数Map
     * @param userId 当前操作用户ID（可为空表示游客）
     * @return 执行结果
     */
    public String execute(String name, Map<String, Object> args, String userId) {
        if (userId != null) {
            setUserContext(userId);
        }
        try {
            ToolEntry entry = toolRegistry.get(name);
            if (entry == null) {
                String error = "工具不存在: " + name + "，可用工具: " + toolRegistry.keySet();
                log.warn(error);
                notifyListeners(name, args, null, error);
                return error;
            }

            log.info("执行工具: {}，参数: {}，userId: {}", name, args, userId);
            long start = System.currentTimeMillis();

            try {
                String result = entry.function().apply(args);
                long duration = System.currentTimeMillis() - start;
                log.info("工具执行完成: {}，耗时: {}ms", name, duration);

                String resultWithMeta = String.format("【工具: %s】\n【耗时: %dms】\n%s", name, duration, result);
                notifyListeners(name, args, result, null);
                return resultWithMeta;
            } catch (Exception e) {
                String error = "工具执行失败: " + name + "，错误: " + e.getMessage();
                log.error(error, e);
                notifyListeners(name, args, null, error);
                return error;
            }
        } finally {
            if (userId != null) {
                clearUserContext();
            }
        }
    }

    /**
     * 批量执行工具（无用户上下文）
     * @param calls 工具调用列表
     * @return 执行结果列表
     */
    public List<String> executeAll(List<ToolCall> calls) {
        return executeAll(calls, null);
    }

    /**
     * 批量执行工具（带用户上下文，用于数据隔离）
     * @param calls 工具调用列表
     * @param userId 当前操作用户ID
     * @return 执行结果列表
     */
    public List<String> executeAll(List<ToolCall> calls, String userId) {
        if (userId != null) {
            setUserContext(userId);
        }
        try {
            List<String> results = new ArrayList<>();
            for (ToolCall call : calls) {
                String result = execute(call.name(), call.args());
                results.add(result);
            }
            return results;
        } finally {
            if (userId != null) {
                clearUserContext();
            }
        }
    }

    /**
     * 获取所有已注册工具
     */
    public Map<String, String> getRegisteredTools() {
        Map<String, String> result = new LinkedHashMap<>();
        toolRegistry.forEach((name, entry) -> result.put(name, entry.description()));
        return result;
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return toolRegistry.size();
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return toolRegistry.containsKey(name);
    }

    /**
     * 添加执行监听器
     */
    public void addListener(ToolExecutionListener listener) {
        listeners.add(listener);
    }

    /**
     * 通知监听器
     */
    private void notifyListeners(String toolName, Map<String, Object> args, String result, String error) {
        listeners.forEach(l -> {
            try {
                l.onToolExecuted(toolName, args, result, error);
            } catch (Exception e) {
                log.warn("监听器通知失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 工具调用记录
     */
    public record ToolCall(String name, Map<String, Object> args) {}

    /**
     * 工具注册条目
     */
    private record ToolEntry(
            String name,
            Function<Map<String, Object>, String> function,
            String description,
            Map<String, String> parameters
    ) {
        ToolEntry(String name, Function<Map<String, Object>, String> function, String description) {
            this(name, function, description, Map.of());
        }
    }

    /**
     * 工具执行监听器接口
     */
    @FunctionalInterface
    public interface ToolExecutionListener {
        void onToolExecuted(String toolName, Map<String, Object> args, String result, String error);
    }
}