package com.ai.learning.planner.config;

import com.ai.learning.planner.vectorstore.InMemoryVectorStoreWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置
 *
 * Bean 结构（避免类型注入歧义）：
 * 1. elasticsearchVectorStore：仅当 spring.ai.vectorstore.elasticsearch.enabled=true 时创建（生产启用）
 * 2. inMemoryVectorStore：无条件创建的降级存储（关键词搜索/内存向量）
 * 3. primaryVectorStore：主存储入口，ES 可用时用 ES，否则降级内存
 *
 * 所有业务注入点必须使用 @Qualifier("primaryVectorStore") 显式指定，
 * 否则存在多个 VectorStore Bean 时会触发 NoUniqueBeanDefinitionException。
 */
@Configuration
@Slf4j
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:knowledge_vectors}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1536}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.elasticsearch.similarity:cosine}")
    private String similarity;

    @Bean
    @ConditionalOnProperty(name = "spring.ai.vectorstore.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
    public VectorStore elasticsearchVectorStore(
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String uris,
            @Autowired(required = false) EmbeddingModel embeddingModel) {

        if (embeddingModel == null) {
            log.warn("EmbeddingModel 不可用，无法创建 ElasticsearchVectorStore");
            return null;
        }

        try {
            // Spring AI 1.1.7 API：ElasticsearchVectorStore.builder(RestClient, EmbeddingModel)
            RestClient restClient = RestClient.builder(HttpHost.create(uris)).build();

            ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
            options.setIndexName(indexName);
            options.setDimensions(dimensions);
            options.setSimilarity(switch (similarity.toLowerCase()) {
                case "l2_norm" -> SimilarityFunction.l2_norm;
                case "dot_product" -> SimilarityFunction.dot_product;
                default -> SimilarityFunction.cosine;
            });

            ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore.builder(restClient, embeddingModel)
                    .options(options)
                    .initializeSchema(true)
                    .build();

            log.info("ElasticsearchVectorStore 初始化成功，index={}, dimensions={}, similarity={}",
                    indexName, dimensions, similarity);
            return vectorStore;
        } catch (Exception e) {
            log.error("ElasticsearchVectorStore 初始化失败: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public VectorStore inMemoryVectorStore(
            @Autowired(required = false) org.springframework.ai.embedding.EmbeddingModel embeddingModel) {

        if (embeddingModel == null) {
            log.warn("EmbeddingModel 不可用，InMemoryVectorStore 仅支持关键词降级模式");
            return new InMemoryVectorStoreWrapper(null);
        }

        InMemoryVectorStoreWrapper vectorStore = new InMemoryVectorStoreWrapper(embeddingModel);
        log.info("InMemoryVectorStore 初始化成功（降级模式）");
        return vectorStore;
    }

    @Bean
    public VectorStore primaryVectorStore(
            @Autowired(required = false) VectorStore elasticsearchVectorStore,
            @Qualifier("inMemoryVectorStore") VectorStore inMemoryVectorStore) {

        if (elasticsearchVectorStore != null) {
            log.info("使用 ElasticsearchVectorStore 作为主向量存储");
            return elasticsearchVectorStore;
        }

        log.info("ES 未启用或不可用，使用 InMemoryVectorStore 作为降级方案");
        return inMemoryVectorStore;
    }
}