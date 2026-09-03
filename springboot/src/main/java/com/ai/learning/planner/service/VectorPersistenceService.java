package com.ai.learning.planner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 向量持久化服务
 * 将 Embedding 向量存入 Redis，应用重启后可快速恢复，避免重复调用 Embedding API
 *
 * Redis 存储结构：
 *   Key:   vector:{docId}:{chunkId}
 *   Value: float[] 序列化为 byte[]
 *   TTL:   永不过期（与文档生命周期一致）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorPersistenceService {

    private static final String VECTOR_KEY_PREFIX = "vector:";
    private static final String ALL_VECTORS_KEY = "vector:all_keys";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 保存单个向量到 Redis
     */
    public void saveVector(String docId, Long chunkId, float[] vector) {
        if (vector == null || vector.length == 0) {
            return;
        }
        String key = buildKey(docId, chunkId);
        try {
            byte[] bytes = floatArrayToBytes(vector);
            redisTemplate.opsForValue().set(key, bytes);
            // 记录 key 到集合中，用于启动时批量加载
            redisTemplate.opsForSet().add(ALL_VECTORS_KEY, key);
            log.debug("向量已保存到 Redis: key={}, dimensions={}", key, vector.length);
        } catch (Exception e) {
            log.warn("保存向量到 Redis 失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 批量保存向量到 Redis
     */
    public void saveVectors(String docId, Map<Long, float[]> vectorMap) {
        if (vectorMap == null || vectorMap.isEmpty()) {
            return;
        }
        int count = 0;
        for (Map.Entry<Long, float[]> entry : vectorMap.entrySet()) {
            try {
                saveVector(docId, entry.getKey(), entry.getValue());
                count++;
            } catch (Exception e) {
                log.warn("批量保存向量部分失败: docId={}, chunkId={}", docId, entry.getKey());
            }
        }
        log.info("批量保存向量完成: docId={}, count={}/{}", docId, count, vectorMap.size());
    }

    /**
     * 从 Redis 加载单个向量
     */
    public float[] loadVector(String docId, Long chunkId) {
        String key = buildKey(docId, chunkId);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof byte[] bytes) {
                return bytesToFloatArray(bytes);
            }
        } catch (Exception e) {
            log.warn("从 Redis 加载向量失败: key={}, error={}", key, e.getMessage());
        }
        return null;
    }

    /**
     * 批量加载指定文档的所有向量
     */
    public Map<Long, float[]> loadVectorsByDoc(String docId) {
        Map<Long, float[]> result = new HashMap<>();
        Set<Object> allKeys = redisTemplate.opsForSet().members(ALL_VECTORS_KEY);

        if (allKeys == null || allKeys.isEmpty()) {
            return result;
        }

        String prefix = VECTOR_KEY_PREFIX + docId + ":";
        for (Object keyObj : allKeys) {
            String key = keyObj.toString();
            if (key.startsWith(prefix)) {
                try {
                    Long chunkId = Long.parseLong(key.substring(prefix.length()));
                    float[] vector = loadVector(docId, chunkId);
                    if (vector != null) {
                        result.put(chunkId, vector);
                    }
                } catch (NumberFormatException e) {
                    log.debug("忽略无效的向量 key: {}", key);
                }
            }
        }
        log.debug("从 Redis 加载向量: docId={}, count={}", docId, result.size());
        return result;
    }

    /**
     * 加载所有向量（启动时使用）
     */
    public Map<String, Map<Long, float[]>> loadAllVectors() {
        Map<String, Map<Long, float[]>> allVectors = new HashMap<>();
        Set<Object> allKeys = redisTemplate.opsForSet().members(ALL_VECTORS_KEY);

        if (allKeys == null || allKeys.isEmpty()) {
            log.info("Redis 中无向量数据");
            return allVectors;
        }

        int totalCount = 0;
        for (Object keyObj : allKeys) {
            String key = keyObj.toString();
            if (!key.startsWith(VECTOR_KEY_PREFIX)) {
                continue;
            }
            try {
                // 解析 key: vector:{docId}:{chunkId}
                String remaining = key.substring(VECTOR_KEY_PREFIX.length());
                int lastColon = remaining.lastIndexOf(':');
                if (lastColon <= 0) continue;

                String docId = remaining.substring(0, lastColon);
                Long chunkId = Long.parseLong(remaining.substring(lastColon + 1));

                float[] vector = loadVector(docId, chunkId);
                if (vector != null) {
                    allVectors.computeIfAbsent(docId, k -> new HashMap<>()).put(chunkId, vector);
                    totalCount++;
                }
            } catch (Exception e) {
                log.debug("解析向量 key 失败: {}", key);
            }
        }

        log.info("从 Redis 加载所有向量完成: 文档数={}, 向量总数={}", allVectors.size(), totalCount);
        return allVectors;
    }

    /**
     * 删除指定文档的所有向量
     */
    public void deleteVectorsByDoc(String docId) {
        Set<Object> allKeys = redisTemplate.opsForSet().members(ALL_VECTORS_KEY);
        if (allKeys == null) return;

        String prefix = VECTOR_KEY_PREFIX + docId + ":";
        List<String> toDelete = new ArrayList<>();
        for (Object keyObj : allKeys) {
            String key = keyObj.toString();
            if (key.startsWith(prefix)) {
                toDelete.add(key);
            }
        }

        if (!toDelete.isEmpty()) {
            redisTemplate.delete(toDelete);
            redisTemplate.opsForSet().remove(ALL_VECTORS_KEY, toDelete.toArray());
            log.info("删除文档向量: docId={}, count={}", docId, toDelete.size());
        }
    }

    /**
     * 检查指定文档是否有向量数据
     */
    public boolean hasVectors(String docId) {
        Set<Object> allKeys = redisTemplate.opsForSet().members(ALL_VECTORS_KEY);
        if (allKeys == null) return false;

        String prefix = VECTOR_KEY_PREFIX + docId + ":";
        return allKeys.stream().anyMatch(key -> key.toString().startsWith(prefix));
    }

    /**
     * 获取 Redis 中的向量统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        Set<Object> allKeys = redisTemplate.opsForSet().members(ALL_VECTORS_KEY);

        int totalVectors = allKeys != null ? allKeys.size() : 0;
        Set<String> docIds = new HashSet<>();

        if (allKeys != null) {
            for (Object key : allKeys) {
                String keyStr = key.toString();
                if (keyStr.startsWith(VECTOR_KEY_PREFIX)) {
                    String remaining = keyStr.substring(VECTOR_KEY_PREFIX.length());
                    int lastColon = remaining.lastIndexOf(':');
                    if (lastColon > 0) {
                        docIds.add(remaining.substring(0, lastColon));
                    }
                }
            }
        }

        stats.put("totalVectors", totalVectors);
        stats.put("totalDocs", docIds.size());
        stats.put("docIds", docIds);
        return stats;
    }

    // ========== 工具方法 ==========

    private String buildKey(String docId, Long chunkId) {
        return VECTOR_KEY_PREFIX + docId + ":" + chunkId;
    }

    /**
     * float[] 转 byte[]（使用 Little-Endian 字节序）
     */
    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    /**
     * byte[] 转 float[]
     */
    private float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length % 4 != 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }
}
