package com.ai.learning.planner.config;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@Slf4j
public class EmbeddingConfig {

    /**
     * DeepSeek 向量化模型 Bean（默认首选）
     * 标记 @Primary：当存在多个 EmbeddingModel Bean（如 Qwen 启用或 OpenAI 自动配置注册）时，
     * 无 @Qualifier 的注入点（VectorStoreConfig）默认选用 DeepSeek，避免 NoUniqueBeanDefinitionException
     * API Key 无效时返回 null Bean（由调用方做降级处理），不阻断应用启动
     */
    @Bean("deepSeekEmbeddingModel")
    @Primary
    @ConditionalOnProperty(name = "spring.ai.deepseek.embedding.enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingModel deepSeekEmbeddingModel(
            @Value("${spring.ai.deepseek.api-key:}") String apiKey,
            @Value("${spring.ai.deepseek.base-url:}") String baseUrl,
            @Value("${spring.ai.deepseek.embedding.dimensions:1536}") int dimensions) {

        log.info("初始化 DeepSeek EmbeddingModel（默认）: baseUrl={}, dimensions={}", baseUrl, dimensions);

        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("$") || apiKey.length() < 8) {
            log.warn("DeepSeek Embedding API Key 无效，返回 null Bean");
            return null;
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://api.deepseek.com")
                .apiKey(apiKey)
                .build();

        // Spring AI 1.1.7 API：OpenAiEmbeddingModel 使用构造函数（无 builder）
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .dimensions(dimensions)
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options,
                RetryTemplate.defaultInstance(), ObservationRegistry.NOOP);
    }

    /**
     * Qwen 向量化模型 Bean（默认关闭，通过 spring.ai.qwen.embedding.enabled 开启）
     * API Key 无效时返回 null Bean，不阻断应用启动
     */
    @Bean("qwenEmbeddingModel")
    @ConditionalOnProperty(name = "spring.ai.qwen.embedding.enabled", havingValue = "true", matchIfMissing = false)
    public EmbeddingModel qwenEmbeddingModel(
            @Value("${spring.ai.qwen.api-key:}") String apiKey,
            @Value("${spring.ai.qwen.base-url:}") String baseUrl,
            @Value("${spring.ai.qwen.embedding.dimensions:1536}") int dimensions) {

        log.info("初始化 Qwen EmbeddingModel: baseUrl={}, dimensions={}", baseUrl, dimensions);

        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("$") || apiKey.length() < 8) {
            log.warn("Qwen Embedding API Key 无效，返回 null Bean");
            return null;
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://dashscope.aliyuncs.com/compatible-mode")
                .apiKey(apiKey)
                .build();

        // Spring AI 1.1.7 API：OpenAiEmbeddingModel 使用构造函数（无 builder）
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .dimensions(dimensions)
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options,
                RetryTemplate.defaultInstance(), ObservationRegistry.NOOP);
    }
}