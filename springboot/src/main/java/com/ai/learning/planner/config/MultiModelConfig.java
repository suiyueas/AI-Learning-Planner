package com.ai.learning.planner.config;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@Slf4j
public class MultiModelConfig {

    /**
     * 多模型对话 ChatModel 配置
     * 按配置条件注册 Qwen / DeepSeek / 小米 MiMo 等 ChatModel Bean；
     * API Key 无效时创建不发送真实请求的兜底 Bean，保证应用可启动
     */

    /**
     * 校验 API Key 是否有效
     * 返回 true 表示 Key 有效，false 表示是占位符/空值
     */
    private boolean isValidApiKey(String apiKey, String sourceName) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("[{}] API Key 为空，该模型将不可用", sourceName);
            return false;
        }
        if (apiKey.startsWith("$")) {
            log.warn("[{}] API Key 为未解析的占位符: {}，请检查环境变量配置", sourceName, apiKey);
            return false;
        }
        if (apiKey.length() < 8) {
            log.warn("[{}] API Key 长度异常({}字符)，疑似无效", sourceName, apiKey.length());
            return false;
        }
        return true;
    }

    @Bean("qwenChatModel")
    @ConditionalOnProperty(name = "spring.ai.qwen.enabled", havingValue = "true", matchIfMissing = true)
    public ChatModel qwenChatModel(
            @Value("${spring.ai.qwen.api-key:}") String apiKey,
            @Value("${spring.ai.qwen.base-url:}") String baseUrl) {
        log.info("初始化 Qwen-Max ChatModel: baseUrl={}", baseUrl);

        if (!isValidApiKey(apiKey, "Qwen-Max")) {
            log.warn("Qwen-Max API Key 无效，创建一个兜底 Bean 防止注入失败");
            // 返回一个不发送真实请求的模拟模型，避免启动时抛出 Bean 创建异常
            return createFallbackChatModel("qwen-max", baseUrl);
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("qwen-max")
                .temperature(0.7)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    @Bean("deepSeekChatModel")
    @ConditionalOnProperty(name = "spring.ai.deepseek.enabled", havingValue = "true", matchIfMissing = true)
    public ChatModel deepSeekChatModel(
            @Value("${spring.ai.deepseek.api-key:}") String apiKey,
            @Value("${spring.ai.deepseek.base-url:}") String baseUrl) {
        log.info("初始化 DeepSeek ChatModel: baseUrl={}", baseUrl);

        if (!isValidApiKey(apiKey, "DeepSeek")) {
            log.warn("DeepSeek API Key 无效，创建一个兜底 Bean 防止注入失败");
            return createFallbackChatModel("deepseek-chat", baseUrl);
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("deepseek-chat")
                .temperature(0.7)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    @Bean("miMoChatModel")
    @ConditionalOnProperty(name = "spring.ai.mimo.enabled", havingValue = "true", matchIfMissing = true)
    public ChatModel miMoChatModel(
            @Value("${spring.ai.mimo.api-key:}") String apiKey,
            @Value("${spring.ai.mimo.base-url:}") String baseUrl) {
        log.info("初始化 MiMo ChatModel: baseUrl={}", baseUrl);

        if (!isValidApiKey(apiKey, "MiMo")) {
            log.warn("MiMo API Key 无效，创建一个兜底 Bean 防止注入失败");
            return createFallbackChatModel("xiaomi/mimo-v2-pro", baseUrl);
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("xiaomi/mimo-v2-pro")
                .temperature(0.7)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    /**
     * 创建一个兜底的 ChatModel，当 API Key 无效时使用
     * 这个 Bean 不会发送真实请求，但可以防止启动时注入失败
     */
    private ChatModel createFallbackChatModel(String modelName, String baseUrl) {
        try {
            // 使用一个默认的 API Key 构造，但实际请求会在运行时失败
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://api.deepseek.com")
                    .apiKey("sk-fallback-invalid-key")
                    .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(modelName)
                    .temperature(0.7)
                    .build();

            return OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .retryTemplate(RetryTemplate.defaultInstance())
                    .observationRegistry(ObservationRegistry.NOOP)
                    .build();
        } catch (Exception e) {
            log.error("创建兜底 ChatModel 失败: {}", e.getMessage(), e);
            throw new IllegalStateException("ChatModel 初始化失败，请检查 API Key 配置: " + modelName, e);
        }
    }
}