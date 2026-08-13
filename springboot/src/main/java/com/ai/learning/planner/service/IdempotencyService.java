package com.ai.learning.planner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性服务
 * 用于防止重复提交、数据重复处理等场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "idempotency:";
    private static final long DEFAULT_TTL_SECONDS = 3600;

    private final ConcurrentHashMap<String, Long> localCache = new ConcurrentHashMap<>();

    /**
     * 检查请求是否已处理（幂等性检查）
     *
     * @param idempotencyKey 幂等键（如：userId + operation + timestamp）
     * @return true=已存在（重复请求），false=不存在（新请求）
     */
    public boolean isDuplicate(String idempotencyKey) {
        String key = PREFIX + idempotencyKey;

        try {
            // SETNX 原子操作：并发下只有一个请求能写入成功，避免 check-then-set 竞态导致幂等失效
            Boolean firstSet = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(DEFAULT_TTL_SECONDS));
            if (!Boolean.TRUE.equals(firstSet)) {
                log.debug("[Idempotency] 检测到重复请求: {}", idempotencyKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("[Idempotency] Redis检查失败，使用本地缓存: {}", e.getMessage());
            return checkLocalCache(idempotencyKey);
        }
    }

    /**
     * 检查本地缓存（Redis不可用时降级）
     * 已过期条目会被移除并重建；缓存过大时清理过期条目，防止内存无限增长
     */
    private boolean checkLocalCache(String idempotencyKey) {
        long now = System.currentTimeMillis();
        Long lastTime = localCache.get(idempotencyKey);

        if (lastTime != null) {
            if (now - lastTime < TimeUnit.SECONDS.toMillis(DEFAULT_TTL_SECONDS)) {
                log.debug("[Idempotency] 本地缓存检测到重复请求: {}", idempotencyKey);
                return true;
            }
            // 过期条目移除后重新登记
            localCache.remove(idempotencyKey, lastTime);
        }

        // 偶发清理：缓存条目数超阈值时移除所有过期条目，控制内存增长
        if (localCache.size() > 10000) {
            localCache.entrySet().removeIf(entry -> now - entry.getValue() > TimeUnit.SECONDS.toMillis(DEFAULT_TTL_SECONDS));
        }

        localCache.putIfAbsent(idempotencyKey, now);
        return false;
    }

    /**
     * 标记请求已处理
     */
    public void markProcessed(String idempotencyKey) {
        String key = PREFIX + idempotencyKey;
        try {
            redisTemplate.opsForValue().set(key, "processed", Duration.ofSeconds(DEFAULT_TTL_SECONDS));
            log.debug("[Idempotency] 标记已处理: {}", idempotencyKey);
        } catch (Exception e) {
            log.warn("[Idempotency] 标记处理状态失败: {}", e.getMessage());
        }
    }

    /**
     * 生成幂等键
     */
    public String generateKey(String userId, String operation, String... extra) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId).append(":").append(operation);
        for (String s : extra) {
            sb.append(":").append(s);
        }
        return sb.toString();
    }

    /**
     * 移除幂等键（允许重新处理）
     */
    public void remove(String idempotencyKey) {
        String key = PREFIX + idempotencyKey;
        try {
            redisTemplate.delete(key);
            localCache.remove(idempotencyKey);
            log.debug("[Idempotency] 移除幂等键: {}", idempotencyKey);
        } catch (Exception e) {
            log.warn("[Idempotency] 移除幂等键失败: {}", e.getMessage());
        }
    }

    /**
     * 获取剩余TTL
     */
    public long getTTL(String idempotencyKey) {
        String key = PREFIX + idempotencyKey;
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}