package com.ai.learning.planner.vectorstore;

import com.ai.learning.planner.service.VectorPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.document.Document;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class InMemoryVectorStoreWrapper implements VectorStore {

    /**
     * 内存向量存储（VectorStore 接口实现）
     * 开发环境替代 ES/Redis 向量库：文档存 ConcurrentHashMap，向量化可用时按余弦相似度检索；
     * 未配置 Embedding 模型时退化为全文包含匹配
     * 支持 Redis 持久化：应用重启后可从 Redis 恢复向量，避免重新 Embedding
     */

    private final EmbeddingModel embeddingModel;
    private final VectorPersistenceService persistenceService;
    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();
    private final Map<String, float[]> vectorStore = new ConcurrentHashMap<>();
    private final boolean embeddingEnabled;

    /**
     * 构造函数（兼容无持久化服务的场景）
     */
    public InMemoryVectorStoreWrapper(EmbeddingModel embeddingModel) {
        this(embeddingModel, null);
    }

    /**
     * 构造函数（支持 Redis 持久化）
     */
    public InMemoryVectorStoreWrapper(EmbeddingModel embeddingModel, VectorPersistenceService persistenceService) {
        this.embeddingModel = embeddingModel;
        this.persistenceService = persistenceService;
        this.embeddingEnabled = (embeddingModel != null);
        log.info("InMemoryVectorStoreWrapper 初始化: embeddingEnabled={}, persistenceEnabled={}",
                embeddingEnabled, persistenceService != null);
    }

    /**
     * 添加文档（Spring AI 1.1.7：接口仅提供 add(List)，单文档由调用方包装）
     * 支持 Redis 持久化：向量生成后同步写入 Redis
     */
    @Override
    public void add(@NonNull List<Document> documents) {
        for (Document doc : documents) {
            String id = doc.getId();
            documentStore.put(id, doc);

            if (embeddingEnabled && embeddingModel != null) {
                try {
                    // Spring AI 1.1.7：embed(String) 直接返回 float[]
                    float[] vector = embeddingModel.embed(doc.getText());
                    vectorStore.put(id, vector);

                    // 同步写入 Redis 持久化
                    if (persistenceService != null) {
                        String docId = doc.getMetadata() != null ? (String) doc.getMetadata().get("docId") : null;
                        Object chunkIdObj = doc.getMetadata() != null ? doc.getMetadata().get("chunkId") : null;
                        if (docId != null && chunkIdObj != null) {
                            Long chunkId = chunkIdObj instanceof Long ? (Long) chunkIdObj : Long.parseLong(chunkIdObj.toString());
                            persistenceService.saveVector(docId, chunkId, vector);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Embedding 生成失败，文档 ID={}: {}", id, e.getMessage());
                }
            }
        }
        log.info("InMemoryVectorStore 添加文档: count={}, embeddingEnabled={}", documents.size(), embeddingEnabled);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull List<Document> similaritySearch(@NonNull SearchRequest request) {
        String query = request.getQuery();
        int topK = (int) request.getTopK();

        if (!embeddingEnabled || embeddingModel == null) {
            return fallbackKeywordSearch(query, topK, request.getFilterExpression());
        }

        try {
            // Spring AI 1.1.7：embed(String) 直接返回 float[]
            float[] queryVector = embeddingModel.embed(query);

            Map<String, Double> scores = new HashMap<>();
            for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
                String id = entry.getKey();
                float[] docVector = entry.getValue();

                if (docVector == null || docVector.length != queryVector.length) {
                    continue;
                }

                double similarity = cosineSimilarity(queryVector, docVector);
                scores.put(id, similarity);
            }

            return scores.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(topK)
                    .map(entry -> {
                        Document originalDoc = documentStore.get(entry.getKey());
                        if (originalDoc != null) {
                            Document resultDoc = new Document(
                                    originalDoc.getId(),
                                    originalDoc.getText(),
                                    new java.util.HashMap<>(originalDoc.getMetadata())
                            );
                            resultDoc.getMetadata().put("score", entry.getValue());
                            return resultDoc;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("向量检索失败，降级为关键词搜索: {}", e.getMessage());
            return fallbackKeywordSearch(query, topK, request.getFilterExpression());
        }
    }

    private List<Document> fallbackKeywordSearch(String query, int topK, Filter.Expression filterExpression) {
        log.debug("使用关键词降级搜索: query={}, topK={}", query, topK);

        String[] keywords = query.toLowerCase().split("\\s+");
        Map<String, Integer> scoreMap = new HashMap<>();

        for (Document doc : documentStore.values()) {
            if (!matchesFilter(doc, filterExpression)) {
                continue;
            }

            String text = doc.getText().toLowerCase();
            int score = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    score++;
                }
            }

            if (score > 0) {
                scoreMap.put(doc.getId(), score);
            }
        }

        return scoreMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(entry -> {
                    Document originalDoc = documentStore.get(entry.getKey());
                    if (originalDoc != null) {
                        Document resultDoc = new Document(
                                originalDoc.getId(),
                                originalDoc.getText(),
                                new java.util.HashMap<>(originalDoc.getMetadata())
                        );
                        resultDoc.getMetadata().put("searchMode", "keyword_fallback");
                        resultDoc.getMetadata().put("keywordScore", scoreMap.get(entry.getKey()));
                        return resultDoc;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unlikely-arg-type")
    private boolean matchesFilter(Document doc, Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return true;
        }

        // 内存存储仅支持简单的等于比较，复杂表达式记录日志并放行
        if (filterExpression instanceof Filter.Expression eq && "=".equals(eq.type())) {
            Object left = eq.left();
            Object right = eq.right();
            if (left instanceof String key) {
                Object docValue = doc.getMetadata().get(key);
                return docValue != null && docValue.toString().equals(String.valueOf(right));
            }
        }
        log.debug("忽略不支持的 Filter 表达式: {}", filterExpression);
        return true;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public void delete(@NonNull String id) {
        documentStore.remove(id);
        vectorStore.remove(id);
        // Redis 中的向量由 KnowledgeChunkService 统一管理删除
    }

    @Override
    public void delete(@NonNull List<String> ids) {
        for (String id : ids) {
            delete(id);
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        // 内存存储不支持按 Filter 表达式删除（无元数据索引），记录日志避免静默失败
        log.warn("InMemoryVectorStore 不支持按 Filter 表达式删除: {}", filterExpression);
    }

    public int getDocumentCount() {
        return documentStore.size();
    }

    public boolean isEmbeddingEnabled() {
        return embeddingEnabled;
    }

    /**
     * 从 Redis 加载向量到内存（启动时调用）
     * @param docIdToTitleMap 文档ID到标题的映射，用于重建 Document 对象
     */
    public void loadFromRedis(Map<String, String> docIdToTitleMap) {
        if (persistenceService == null) {
            log.debug("持久化服务不可用，跳过从 Redis 加载向量");
            return;
        }

        long startTime = System.currentTimeMillis();
        Map<String, Map<Long, float[]>> allVectors = persistenceService.loadAllVectors();
        int loadedCount = 0;

        for (Map.Entry<String, Map<Long, float[]>> docEntry : allVectors.entrySet()) {
            String docId = docEntry.getKey();
            String title = docIdToTitleMap.getOrDefault(docId, docId);

            for (Map.Entry<Long, float[]> chunkEntry : docEntry.getValue().entrySet()) {
                Long chunkId = chunkEntry.getKey();
                float[] vector = chunkEntry.getValue();

                // 重建 Document 对象并存入内存
                String docKey = docId + ":" + chunkId;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("docId", docId);
                metadata.put("chunkId", chunkId);
                metadata.put("docTitle", title);
                metadata.put("title", title);
                metadata.put("source", "knowledge_chunk");

                // 从数据库加载的内容可能为空，仅存向量用于检索
                documentStore.putIfAbsent(docKey, new Document("", metadata));
                vectorStore.put(docKey, vector);
                loadedCount++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("从 Redis 加载向量完成: count={}, 耗时={}ms", loadedCount, elapsed);
    }

    /**
     * 获取内存中的向量数量
     */
    public int getVectorCount() {
        return vectorStore.size();
    }
}