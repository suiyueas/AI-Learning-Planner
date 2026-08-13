package com.ai.learning.planner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 配置
 * 显式提供 RedisTemplate&lt;String, Object&gt;（Spring Boot 自动配置仅提供
 * RedisTemplate&lt;Object, Object&gt;，泛型不匹配导致服务注入失败）
 *
 * <p>序列化策略：
 * - key/hashKey 使用 String 序列化：Redis 中键明文可读，支持 keys/scan 模式匹配统计
 * - value/hashValue 保持 JDK 序列化：兼容既有缓存数据，且反序列化保留原始类型
 *   （ConfigDataCacheService 依赖强转还原 List&lt;KnowledgeNode&gt; 等类型）
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        return template;
    }
}
