package com.ai.learning.planner.mcp.client;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地降级资源注册表（Fallback Registry）
 * 当核心 MCP Server 不可用时，自动降级调用本地缓存的静态 MCP Resource
 * 降级结果通过响应头 X-Fallback: true 标记
 */
@Slf4j
public class FallbackRegistry {

    /** 静态降级资源（toolName -> 静态内容） */
    private final Map<String, String> staticResources = new ConcurrentHashMap<>();

    /**
     * 注册静态降级资源
     *
     * @param toolName 工具名（与 MCP 工具一一对应）
     * @param content  静态资源内容（JSON/文本）
     */
    public void register(String toolName, String content) {
        staticResources.put(toolName, content);
        log.info("[FallbackRegistry] 注册降级资源: {} ({} 字符)", toolName, content == null ? 0 : content.length());
    }

    /**
     * 获取降级资源（不存在返回 empty）
     */
    public Optional<String> get(String toolName) {
        return Optional.ofNullable(staticResources.get(toolName));
    }

    /**
     * 是否存在降级资源
     */
    public boolean hasFallback(String toolName) {
        return staticResources.containsKey(toolName);
    }

    /**
     * 获取所有已注册降级资源名
     */
    public Map<String, String> getAll() {
        return Map.copyOf(staticResources);
    }

    /**
     * 清除全部降级资源
     */
    public void clear() {
        staticResources.clear();
    }
}
