package com.ai.learning.planner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 文本向量化服务
 * 封装 DeepSeek/Qwen 向量化模型，模型不可用时优雅降级（返回零向量/空列表）
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(
            @Autowired(required = false) @Qualifier("deepSeekEmbeddingModel") EmbeddingModel deepSeekEmbedding,
            @Autowired(required = false) @Qualifier("qwenEmbeddingModel") EmbeddingModel qwenEmbedding) {

        this.embeddingModel = deepSeekEmbedding != null ? deepSeekEmbedding : qwenEmbedding;

        if (this.embeddingModel == null) {
            log.warn("EmbeddingService 初始化: 未配置任何 Embedding 模型，向量生成功能不可用");
        } else {
            log.info("EmbeddingService 初始化成功，使用模型: {}",
                    deepSeekEmbedding != null ? "DeepSeek" : "Qwen");
        }
    }

    /**
     * 单条文本向量化（失败时返回零向量）
     */
    public float[] embed(String text) {
        if (embeddingModel == null) {
            log.warn("Embedding 模型不可用，返回零向量");
            return new float[0];
        }

        try {
            // Spring AI 1.1.7：embed(String) 直接返回 float[]
            return embeddingModel.embed(text);
        } catch (HttpClientErrorException e) {
            // 404: 模型端点不存在；429: 限流
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("Embedding 模型端点不存在(404)，请检查模型配置: {}", e.getMessage());
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Embedding API 限流(429)，稍后重试: {}", e.getMessage());
            } else {
                log.error("Embedding HTTP 客户端错误({}): {}", e.getStatusCode().value(), e.getMessage());
            }
            return new float[0];
        } catch (HttpServerErrorException e) {
            // 5xx: 服务端错误
            log.error("Embedding 服务端错误({}): {}", e.getStatusCode().value(), e.getMessage());
            return new float[0];
        } catch (ResourceAccessException e) {
            // 网络超时/连接失败
            log.error("Embedding 网络访问失败: {}", e.getMessage());
            return new float[0];
        } catch (Exception e) {
            log.error("Embedding 生成失败: {}", e.getMessage());
            return new float[0];
        }
    }

    /**
     * 批量文本向量化（失败时返回空列表）
     */
    public List<float[]> embed(List<String> texts) {
        if (embeddingModel == null) {
            log.warn("Embedding 模型不可用，返回空列表");
            return List.of();
        }

        try {
            // Spring AI 1.1.7：embed(List<String>) 直接返回 List<float[]>
            return embeddingModel.embed(texts);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("批量 Embedding 模型端点不存在(404): {}", e.getMessage());
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("批量 Embedding API 限流(429): {}", e.getMessage());
            } else {
                log.error("批量 Embedding HTTP 客户端错误({}): {}", e.getStatusCode().value(), e.getMessage());
            }
            return List.of();
        } catch (HttpServerErrorException e) {
            log.error("批量 Embedding 服务端错误({}): {}", e.getStatusCode().value(), e.getMessage());
            return List.of();
        } catch (ResourceAccessException e) {
            log.error("批量 Embedding 网络访问失败: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("批量 Embedding 生成失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 异步向量化（异步线程池中执行 embed）
     */
    public CompletableFuture<float[]> embedAsync(String text) {
        return CompletableFuture.supplyAsync(() -> embed(text));
    }

    /**
     * 向量化能力是否可用
     */
    public boolean isAvailable() {
        return embeddingModel != null;
    }

    /**
     * 获取向量维度（不可用时返回 0）
     */
    public int getDimensions() {
        if (embeddingModel == null) {
            return 0;
        }
        try {
            float[] testVector = embed("test");
            return testVector.length;
        } catch (Exception e) {
            return 0;
        }
    }
}