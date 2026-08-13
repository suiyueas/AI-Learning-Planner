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
     * 执行工具调用
     * @param name 工具名称
     * @param args 参数Map
     * @return 执行结果
     */
    public String execute(String name, Map<String, Object> args) {
        ToolEntry entry = toolRegistry.get(name);
        if (entry == null) {
            String error = "工具不存在: " + name + "，可用工具: " + toolRegistry.keySet();
            log.warn(error);
            notifyListeners(name, args, null, error);
            return error;
        }

        log.info("执行工具: {}，参数: {}", name, args);
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
    }

    /**
     * 批量执行工具
     * @param calls 工具调用列表
     * @return 执行结果列表
     */
    public List<String> executeAll(List<ToolCall> calls) {
        List<String> results = new ArrayList<>();
        for (ToolCall call : calls) {
            String result = execute(call.name(), call.args());
            results.add(result);
        }
        return results;
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
