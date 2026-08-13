package com.ai.learning.planner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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