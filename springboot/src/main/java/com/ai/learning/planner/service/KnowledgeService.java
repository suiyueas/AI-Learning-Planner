package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeChunk;
import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.repository.KnowledgeChunkRepository;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeService {

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.rag.top-k:10}")
    private int defaultTopK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Autowired
    @Qualifier("primaryVectorStore")
    private VectorStore vectorStore;

    private static final String RAG_CACHE_PREFIX = "cache:rag:search:";
    private static final long RAG_CACHE_TTL_MINUTES = 10;

    /**
     * 语义检索（带用户隔离）：只检索当前用户上传的文档
     */
    public List<Document> searchSimilar(String query, int topK, List<String> selectedDocIds, String userId) {
        int maxTopK = defaultTopK > 0 ? defaultTopK : 10;
        int effectiveTopK = topK > 0 ? Math.min(topK, maxTopK) : maxTopK;

        String cacheKey = buildRagCacheKey(query, effectiveTopK, similarityThreshold, userId);
        if (selectedDocIds == null || selectedDocIds.isEmpty()) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null && cached instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Document> cachedDocs = (List<Document>) cached;
                    log.debug("[RAG] 缓存命中: query={}, topK={}, userId={}", query, effectiveTopK, userId);
                    return cachedDocs;
                }
            } catch (Exception e) {
                log.warn("[RAG] 缓存读取失败，继续执行检索: {}", e.getMessage());
            }
        }

        List<Document> results = doSearchSimilar(query, effectiveTopK, userId);

        if ((selectedDocIds == null || selectedDocIds.isEmpty()) && !results.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, results, RAG_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("[RAG] 缓存写入失败: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * 不指定文档范围检索的便捷方法（带用户隔离）
     */
    public List<Document> searchSimilar(String query, int topK, String userId) {
        return searchSimilar(query, topK, null, userId);
    }

    /**
     * 兼容旧调用方（无 userId），仅用于 admin 或公共场景
     */
    public List<Document> searchSimilar(String query, int topK) {
        return searchSimilar(query, topK, null, null);
    }

    /**
     * 执行实际的相似度检索（向量优先 + MySQL降级），按 userId 隔离
     */
    private List<Document> doSearchSimilar(String query, int effectiveTopK, String userId) {
        if (vectorStore == null) {
            log.warn("VectorStore 未配置，降级为数据库关键词搜索");
            return fallbackKeywordSearch(query, effectiveTopK, userId);
        }

        long start = System.currentTimeMillis();
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(effectiveTopK)
                    .similarityThreshold(similarityThreshold);

            if (userId != null && !userId.isEmpty()) {
                builder.filterExpression(new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("userId"),
                        new Filter.Value(userId)));
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());
            long elapsed = System.currentTimeMillis() - start;
            log.info("[RAG] 检索完成: query={}, topK={}, hits={}, userId={}, 耗时={}ms",
                    query, effectiveTopK, results.size(), userId, elapsed);
            return normalizeMetadata(results);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("向量检索失败(耗时{}ms)，降级为数据库关键词搜索: {}", elapsed, e.getMessage());
            return fallbackKeywordSearch(query, effectiveTopK, userId);
        }
    }

    private String buildRagCacheKey(String query, int topK, double threshold, String userId) {
        String raw = query.trim().toLowerCase() + ":" + topK + ":" + threshold + ":" + (userId != null ? userId : "all");
        int hash = raw.hashCode();
        return RAG_CACHE_PREFIX + Integer.toHexString(hash);
    }

    private List<Document> normalizeMetadata(List<Document> results) {
        for (Document doc : results) {
            if (doc.getMetadata() == null) continue;
            Object docTitle = doc.getMetadata().get("docTitle");
            Object title = doc.getMetadata().get("title");
            if (docTitle == null && title != null) {
                doc.getMetadata().put("docTitle", title);
            } else if (title == null && docTitle != null) {
                doc.getMetadata().put("title", docTitle);
            }
            if (!doc.getMetadata().containsKey("source")) {
                doc.getMetadata().put("source", "vector_search");
            }
            if (!doc.getMetadata().containsKey("searchMode")) {
                doc.getMetadata().put("searchMode", "vector");
            }
        }
        return results;
    }

    /**
     * 关键词降级搜索，按 userId 隔离：只搜索当前用户文档的 chunk
     */
    private List<Document> fallbackKeywordSearch(String query, int topK, String userId) {
        log.debug("使用数据库关键词降级搜索: query={}, topK={}, userId={}", query, topK, userId);

        List<KnowledgeChunk> chunks;
        if (userId != null && !userId.isEmpty()) {
            List<String> userIdDocIds = knowledgeDocumentRepository.findByUserIdOrderByUploadedAtDesc(userId)
                    .stream().map(KnowledgeDocument::getId).collect(Collectors.toList());
            if (userIdDocIds.isEmpty()) {
                return List.of();
            }
            chunks = knowledgeChunkRepository.findByDocIdInAndContentContaining(userIdDocIds, query);
        } else {
            chunks = knowledgeChunkRepository.findByContentContaining(query);
        }

        List<String> docIds = chunks.stream().map(KnowledgeChunk::getDocId).distinct().collect(Collectors.toList());
        Map<String, String> docTitles = docIds.isEmpty()
                ? Map.of()
                : knowledgeDocumentRepository.findAllById(docIds).stream()
                        .collect(Collectors.toMap(KnowledgeDocument::getId, KnowledgeDocument::getTitle, (a, b) -> a));

        return chunks.stream()
                .limit(topK)
                .map(chunk -> {
                    Document doc = new Document(chunk.getContent());
                    doc.getMetadata().put("chunkId", chunk.getId());
                    doc.getMetadata().put("docId", chunk.getDocId());
                    doc.getMetadata().put("docTitle", docTitles.getOrDefault(chunk.getDocId(), "未知文档"));
                    doc.getMetadata().put("title", docTitles.getOrDefault(chunk.getDocId(), "未知文档"));
                    doc.getMetadata().put("chunkIndex", chunk.getChunkIndex());
                    doc.getMetadata().put("source", "mysql_fallback");
                    doc.getMetadata().put("searchMode", "keyword_fallback");
                    return doc;
                })
                .collect(Collectors.toList());
    }
}
