package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Redis缓存服务 - 配置数据缓存
 * 用于缓存知识库索引、Agent配置等数据，加快启动速度
 * 支持Redis不可用时的降级处理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigDataCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;


    private static final String PREFIX = "cache:config:";
    private static final String AGENT_CONFIG_KEY = PREFIX + "agent:configs";
    private static final String KNOWLEDGE_DOCS_KEY = PREFIX + "knowledge:docs";
    private static final String SUBJECTS_KEY = PREFIX + "subjects:list";
    private static final long DEFAULT_TTL_HOURS = 24;

    private volatile boolean redisAvailable = true;

    /**
     * 检查Redis是否可用
     */
    public boolean isRedisAvailable() {
        if (!redisAvailable) return false;
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            redisAvailable = true;
            return true;
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] Redis不可用，切换到降级模式: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 缓存Agent配置列表
     */
    public void cacheAgentConfigs(List<Map<String, Object>> agents) {
        if (!isRedisAvailable()) return;
        try {
            String key = AGENT_CONFIG_KEY;
            redisTemplate.opsForValue().set(key, agents, Duration.ofHours(DEFAULT_TTL_HOURS));
            log.debug("[Cache] 已缓存 {} 个Agent配置", agents.size());
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 缓存Agent配置失败，切换降级: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的Agent配置
     */
    @SuppressWarnings("unchecked")
    public Optional<List<Map<String, Object>>> getCachedAgentConfigs() {
        if (!isRedisAvailable()) return Optional.empty();
        try {
            Object cached = redisTemplate.opsForValue().get(AGENT_CONFIG_KEY);
            if (cached != null) {
                log.debug("[Cache] 从缓存获取Agent配置");
                return Optional.of((List<Map<String, Object>>) cached);
            }
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 获取Agent配置缓存失败，切换降级: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 缓存知识文档列表
     */
    public void cacheKnowledgeDocuments(List<KnowledgeDocument> docs) {
        if (!isRedisAvailable()) return;
        try {
            String key = KNOWLEDGE_DOCS_KEY;
            redisTemplate.opsForValue().set(key, docs, Duration.ofHours(DEFAULT_TTL_HOURS));
            log.debug("[Cache] 已缓存 {} 个知识文档", docs.size());
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 缓存知识文档失败，切换降级: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的知识文档
     */
    @SuppressWarnings("unchecked")
    public Optional<List<KnowledgeDocument>> getCachedKnowledgeDocuments() {
        if (!isRedisAvailable()) {
            log.warn("[Cache] Redis不可用，降级到MySQL查询知识文档");
            try {
                List<KnowledgeDocument> docs = knowledgeDocumentRepository.findAll();
                return Optional.of(docs);
            } catch (Exception e) {
                log.error("[Cache] MySQL降级查询知识文档失败: {}", e.getMessage());
                return Optional.empty();
            }
        }
        try {
            Object cached = redisTemplate.opsForValue().get(KNOWLEDGE_DOCS_KEY);
            if (cached != null) {
                log.debug("[Cache] 从缓存获取知识文档");
                return Optional.of((List<KnowledgeDocument>) cached);
            }
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 获取知识文档缓存失败，切换降级: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 缓存科目列表（用于题库）
     */
    public void cacheSubjects(List<String> subjects) {
        if (!isRedisAvailable()) return;
        try {
            String key = SUBJECTS_KEY;
            redisTemplate.opsForValue().set(key, subjects, Duration.ofHours(DEFAULT_TTL_HOURS));
            log.debug("[Cache] 已缓存 {} 个科目", subjects.size());
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 缓存科目列表失败，切换降级: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的科目列表
     */
    @SuppressWarnings("unchecked")
    public Optional<List<String>> getCachedSubjects() {
        if (!isRedisAvailable()) return Optional.empty();
        try {
            Object cached = redisTemplate.opsForValue().get(SUBJECTS_KEY);
            if (cached != null) {
                log.debug("[Cache] 从缓存获取科目列表");
                return Optional.of((List<String>) cached);
            }
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 获取科目列表缓存失败，切换降级: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 通用缓存设置
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (!isRedisAvailable()) return;
        try {
            redisTemplate.opsForValue().set(PREFIX + key, value, Duration.ofMillis(unit.toMillis(timeout)));
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 设置缓存失败 [{}]，切换降级: {}", key, e.getMessage());
        }
    }

    /**
     * 通用缓存获取
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        if (!isRedisAvailable()) return Optional.empty();
        try {
            Object cached = redisTemplate.opsForValue().get(PREFIX + key);
            if (cached != null && type.isInstance(cached)) {
                return Optional.of((T) cached);
            }
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 获取缓存失败 [{}]，切换降级: {}", key, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        if (!isRedisAvailable()) return;
        try {
            redisTemplate.delete(PREFIX + key);
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 删除缓存失败 [{}]，切换降级: {}", key, e.getMessage());
        }
    }

    /**
     * 清空所有配置缓存
     */
    public void clearAllConfigCache() {
        if (!isRedisAvailable()) return;
        try {
            Set<String> keys = redisTemplate.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("[Cache] 已清空 {} 个配置缓存", keys.size());
            }
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 清空配置缓存失败，切换降级: {}", e.getMessage());
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        if (!isRedisAvailable()) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + key));
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 检查缓存存在失败，切换降级: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取缓存剩余TTL（秒），key 不存在或不可用时返回 -1
     */
    public long getTTL(String key) {
        if (!isRedisAvailable()) return -1;
        try {
            Long ttl = redisTemplate.getExpire(PREFIX + key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("[Cache] 获取缓存TTL失败，切换降级: {}", e.getMessage());
            return -1;
        }
    }
}
