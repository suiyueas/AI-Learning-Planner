package com.ai.learning.planner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Redis 对话记忆服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisChatMemoryService {

    private final StringRedisTemplate redisTemplate;
    private static final String MEMORY_PREFIX = "chat:memory:";
    private static final long MEMORY_EXPIRE_HOURS = 24;

    public void addToMemory(String sessionId, String role, String content) {
        String key = MEMORY_PREFIX + sessionId;
        String entry = role + ": " + content;
        redisTemplate.opsForList().rightPush(key, entry);
        redisTemplate.expire(key, Duration.ofHours(MEMORY_EXPIRE_HOURS));
    }

    public String getChatContext(String sessionId) {
        String key = MEMORY_PREFIX + sessionId;
        List<String> history = redisTemplate.opsForList().range(key, 0, -1);
        if (history == null || history.isEmpty()) {
            return "无历史对话记录";
        }
        return String.join("\n", history);
    }

    public void clearMemory(String sessionId) {
        String key = MEMORY_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    public long getMemorySize(String sessionId) {
        String key = MEMORY_PREFIX + sessionId;
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }
}
