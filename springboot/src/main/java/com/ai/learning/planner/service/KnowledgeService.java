package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeNode;
import com.ai.learning.planner.entity.KnowledgeChunk;
import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.repository.KnowledgeNodeRepository;
import com.ai.learning.planner.repository.KnowledgeChunkRepository;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeService {

    /**
     * 知识库服务
     * 知识节点 CRUD、前置知识 JSON 解析、向量检索（未配置 VectorStore 时降级为数据库关键词搜索）
     */

    private final KnowledgeNodeRepository knowledgeNodeRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final ObjectMapper objectMapper;
    private final ConfigDataCacheService configDataCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.rag.top-k:10}")
    private int defaultTopK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Autowired
    @Qualifier("primaryVectorStore")
    private VectorStore vectorStore;

    public KnowledgeNode saveNode(KnowledgeNode node) {
        return knowledgeNodeRepository.save(node);
    }

    public Optional<KnowledgeNode> getNode(String nodeId) {
        return knowledgeNodeRepository.findById(nodeId);
    }

    public List<KnowledgeNode> searchByName(String name) {
        return knowledgeNodeRepository.findByNameContaining(name);
    }

    public List<KnowledgeNode> getNodesByCategory(String category) {
        return knowledgeNodeRepository.findByCategory(category);
    }

    private static final String RAG_CACHE_PREFIX = "cache:rag:search:";
    private static final long RAG_CACHE_TTL_MINUTES = 10;

    /**
     * 语义检索：向量存储优先（ES 主 / 内存降级），失败时回退数据库关键词搜索。
     * topK 受 app.rag.top-k 上限约束，相似度阈值取 app.rag.similarity-threshold；
     * Redis 缓存热门查询结果（TTL 10分钟），命中缓存时直接返回
     */
    public List<Document> searchSimilar(String query, int topK) {
        int maxTopK = defaultTopK > 0 ? defaultTopK : 10;
        int effectiveTopK = topK > 0 ? Math.min(topK, maxTopK) : maxTopK;

        // 1. Redis 缓存命中检查
        String cacheKey = buildRagCacheKey(query, effectiveTopK, similarityThreshold);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && cached instanceof List) {
                @SuppressWarnings("unchecked")
                List<Document> cachedDocs = (List<Document>) cached;
                log.debug("[RAG] 缓存命中: query={}, topK={}", query, effectiveTopK);
                return cachedDocs;
            }
        } catch (Exception e) {
            log.warn("[RAG] 缓存读取失败，继续执行检索: {}", e.getMessage());
        }

        // 2. 缓存未命中，执行检索
        List<Document> results = doSearchSimilar(query, effectiveTopK);

        // 3. 写入缓存（TTL 10分钟）
        if (!results.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, results, RAG_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("[RAG] 缓存写入失败: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * 执行实际的相似度检索（向量优先 + MySQL降级）
     */
    private List<Document> doSearchSimilar(String query, int effectiveTopK) {
        if (vectorStore == null) {
            log.warn("VectorStore 未配置，降级为数据库关键词搜索");
            return fallbackKeywordSearch(query, effectiveTopK);
        }

        long start = System.currentTimeMillis();
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(effectiveTopK)
                            .similarityThreshold(similarityThreshold)
                            .build()
            );
            long elapsed = System.currentTimeMillis() - start;
            log.info("[RAG] 检索完成: query={}, topK={}, hits={}, 耗时={}ms {}",
                    query, effectiveTopK, results.size(), elapsed,
                    elapsed <= 200 ? "(<200ms ✓)" : "(⚠️ 超过 200ms 目标)");
            return normalizeMetadata(results);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("向量检索失败(耗时{}ms)，降级为数据库关键词搜索: {}", elapsed, e.getMessage());
            return fallbackKeywordSearch(query, effectiveTopK);
        }
    }

    /**
     * 构建 RAG 缓存 Key：query + topK + threshold 组合哈希，避免不同参数命中同一缓存
     */
    private String buildRagCacheKey(String query, int topK, double threshold) {
        String raw = query.trim().toLowerCase() + ":" + topK + ":" + threshold;
        int hash = raw.hashCode();
        return RAG_CACHE_PREFIX + Integer.toHexString(hash);
    }

    /**
     * 归一化检索结果来源元数据：保证 docTitle/title/source 键一致，供前端来源追溯展示
     */
    private List<Document> normalizeMetadata(List<Document> results) {
        for (Document doc : results) {
            if (doc.getMetadata() == null) {
                continue;
            }
            Object docTitle = doc.getMetadata().get("docTitle");
            Object title = doc.getMetadata().get("title");
            if (docTitle == null && title != null) {
                doc.getMetadata().put("docTitle", title);
            } else if (title == null && docTitle != null) {
                doc.getMetadata().put("title", docTitle);
            }
            // 标注向量检索来源，与 mysql_fallback 路径对称
            if (!doc.getMetadata().containsKey("source")) {
                doc.getMetadata().put("source", "vector_search");
            }
            if (!doc.getMetadata().containsKey("searchMode")) {
                doc.getMetadata().put("searchMode", "vector");
            }
        }
        return results;
    }

    private List<Document> fallbackKeywordSearch(String query, int topK) {
        log.debug("使用数据库关键词降级搜索: query={}, topK={}", query, topK);

        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByContentContaining(query);
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

    public List<KnowledgeNode> getPrerequisites(String nodeId) {
        return knowledgeNodeRepository.findById(nodeId)
                .map(node -> {
                    String prereqs = node.getPrerequisites();
                    if (prereqs == null || prereqs.isBlank()) return List.<KnowledgeNode>of();
                    try {
                        List<String> prereqIds = objectMapper.readValue(prereqs, new TypeReference<List<String>>() {});
                        List<KnowledgeNode> prereqNodes = knowledgeNodeRepository.findAllById(prereqIds);
                        return prereqNodes;
                    } catch (Exception e) {
                        log.error("Failed to parse prerequisites JSON for node {}: {}", nodeId, prereqs, e);
                        return List.<KnowledgeNode>of();
                    }
                })
                .orElse(List.<KnowledgeNode>of());
    }

    public List<KnowledgeNode> getAllNodes() {
        Optional<List<KnowledgeNode>> cached = configDataCacheService.getCachedKnowledgeNodes();
        if (cached.isPresent()) {
            log.debug("从缓存获取知识节点");
            return cached.get();
        }

        List<KnowledgeNode> nodes = knowledgeNodeRepository.findAll();
        configDataCacheService.cacheKnowledgeNodes(nodes);
        return nodes;
    }
}