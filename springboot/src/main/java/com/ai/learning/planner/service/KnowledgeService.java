package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeNode;
import com.ai.learning.planner.entity.KnowledgeChunk;
import com.ai.learning.planner.repository.KnowledgeNodeRepository;
import com.ai.learning.planner.repository.KnowledgeChunkRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ObjectMapper objectMapper;
    private final ConfigDataCacheService configDataCacheService;

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

    public List<Document> searchSimilar(String query, int topK) {
        if (vectorStore == null) {
            log.warn("VectorStore 未配置，降级为数据库关键词搜索");
            return fallbackKeywordSearch(query, topK);
        }

        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build()
            );
        } catch (Exception e) {
            log.warn("向量检索失败，降级为数据库关键词搜索: {}", e.getMessage());
            return fallbackKeywordSearch(query, topK);
        }
    }

    private List<Document> fallbackKeywordSearch(String query, int topK) {
        log.debug("使用数据库关键词降级搜索: query={}, topK={}", query, topK);

        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByContentContaining(query);

        return chunks.stream()
                .limit(topK)
                .map(chunk -> {
                    Document doc = new Document(chunk.getContent());
                    doc.getMetadata().put("chunkId", chunk.getId());
                    doc.getMetadata().put("docId", chunk.getDocId());
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