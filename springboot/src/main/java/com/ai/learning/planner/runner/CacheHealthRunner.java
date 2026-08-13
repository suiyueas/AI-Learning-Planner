package com.ai.learning.planner.runner;

import com.ai.learning.planner.mcp.ai.ToolDefinition;
import com.ai.learning.planner.mcp.ai.ToolDefinitionRegistry;
import com.ai.learning.planner.service.ConfigDataCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 缓存健康检查Runner
 * 定期检查Redis连接状态和缓存命中率
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheHealthRunner implements ApplicationRunner {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConfigDataCacheService configDataCacheService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 缓存与组件初始化检查 ==========");

        checkRedisConnection();
        logMcpToolsSummary();
        logCacheStats();

        log.info("========================================");
        log.info(" AI 学习规划器启动成功！");
        log.info("========================================");
    }

    private void checkRedisConnection() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            if ("PONG".equals(pong)) {
                log.info("✅ Redis 连接状态: 正常");
            } else {
                log.warn("⚠️ Redis 连接状态: 异常，响应: {}", pong);
            }
        } catch (Exception e) {
            log.error("❌ Redis 连接状态: 失败 - {}", e.getMessage());
        }
    }

    private void logMcpToolsSummary() {
        try {
            var visibleTools = ToolDefinitionRegistry.visibleTools();
            var allTools = ToolDefinitionRegistry.all();
            String toolIds = visibleTools.stream()
                    .map(ToolDefinition::id)
                    .collect(java.util.stream.Collectors.joining(", "));
            log.info("✅ MCP 工具注册: {} 个 [{}]", visibleTools.size(), toolIds);
            log.debug("[MCP] 调试工具 {} 个（后台隐藏）", allTools.size() - visibleTools.size());
        } catch (Exception e) {
            log.warn("[MCP] 工具注册信息获取失败: {}", e.getMessage());
        }
    }

    private void logCacheStats() {
        try {
            Set<String> configKeys = redisTemplate.keys("cache:config:*");
            Set<String> queryKeys = redisTemplate.keys("cache:query:*");
            Set<String> chatKeys = redisTemplate.keys("chat:*");

            // 合并为单条日志
            log.debug("[Cache] 缓存统计 - 配置: {}, 查询: {}, 聊天记忆: {}",
                    configKeys != null ? configKeys.size() : 0,
                    queryKeys != null ? queryKeys.size() : 0,
                    chatKeys != null ? chatKeys.size() : 0);

            long configTTL = configDataCacheService.getTTL("agent:configs");
            if (configTTL > 0) {
                log.debug("[Cache] Agent配置缓存剩余TTL: {} 秒", configTTL);
            }
        } catch (Exception e) {
            log.debug("[Cache] 获取缓存统计失败: {}", e.getMessage());
        }
    }
}