package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeNode;
import com.ai.learning.planner.repository.KnowledgeChunkRepository;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.repository.KnowledgeNodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 知识服务单元测试
 * 覆盖节点 CRUD、前置知识 JSON 解析与向量检索降级逻辑
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeNodeRepository knowledgeNodeRepository;

    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private ConfigDataCacheService configDataCacheService;

    @Mock
    private VectorStore vectorStore;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(knowledgeNodeRepository, knowledgeChunkRepository,
                knowledgeDocumentRepository, new ObjectMapper(), configDataCacheService);
        ReflectionTestUtils.setField(knowledgeService, "defaultTopK", 10);
        ReflectionTestUtils.setField(knowledgeService, "similarityThreshold", 0.7);
    }

    private KnowledgeNode buildNode(String id, String name) {
        return KnowledgeNode.builder().id(id).name(name).category("编程").build();
    }

    @Test
    void saveNode_delegatesToRepository() {
        KnowledgeNode node = buildNode("n1", "Java基础");
        when(knowledgeNodeRepository.save(node)).thenReturn(node);

        KnowledgeNode saved = knowledgeService.saveNode(node);

        assertEquals("n1", saved.getId());
        verify(knowledgeNodeRepository).save(node);
    }

    @Test
    void getNode_existing_returnsNode() {
        KnowledgeNode node = buildNode("n1", "Java基础");
        when(knowledgeNodeRepository.findById("n1")).thenReturn(Optional.of(node));

        Optional<KnowledgeNode> result = knowledgeService.getNode("n1");

        assertTrue(result.isPresent());
        assertEquals("Java基础", result.get().getName());
    }

    @Test
    void getNode_missing_returnsEmpty() {
        when(knowledgeNodeRepository.findById("n-missing")).thenReturn(Optional.empty());

        Optional<KnowledgeNode> result = knowledgeService.getNode("n-missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchByName_delegatesToRepository() {
        when(knowledgeNodeRepository.findByNameContaining("Java")).thenReturn(List.of(buildNode("n1", "Java基础")));

        List<KnowledgeNode> result = knowledgeService.searchByName("Java");

        assertEquals(1, result.size());
        verify(knowledgeNodeRepository).findByNameContaining("Java");
    }

    @Test
    void getPrerequisites_validJson_returnsNodes() {
        KnowledgeNode node = buildNode("n1", "Spring");
        node.setPrerequisites("[\"n2\",\"n3\"]");
        KnowledgeNode prereq2 = buildNode("n2", "Java基础");
        KnowledgeNode prereq3 = buildNode("n3", "Maven");
        when(knowledgeNodeRepository.findById("n1")).thenReturn(Optional.of(node));
        when(knowledgeNodeRepository.findAllById(List.of("n2", "n3"))).thenReturn(List.of(prereq2, prereq3));

        List<KnowledgeNode> result = knowledgeService.getPrerequisites("n1");

        assertEquals(2, result.size());
        assertEquals("Java基础", result.get(0).getName());
    }

    @Test
    void getPrerequisites_invalidJson_returnsEmpty() {
        KnowledgeNode node = buildNode("n1", "Spring");
        node.setPrerequisites("not-json{");
        when(knowledgeNodeRepository.findById("n1")).thenReturn(Optional.of(node));

        List<KnowledgeNode> result = knowledgeService.getPrerequisites("n1");

        assertTrue(result.isEmpty());
    }

    @Test
    void getPrerequisites_blank_returnsEmpty() {
        KnowledgeNode node = buildNode("n1", "Spring");
        node.setPrerequisites(null);
        when(knowledgeNodeRepository.findById("n1")).thenReturn(Optional.of(node));

        List<KnowledgeNode> result = knowledgeService.getPrerequisites("n1");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchSimilar_vectorStoreNotConfigured_returnsEmpty() {
        List<Document> result = knowledgeService.searchSimilar("Java", 5);

        assertTrue(result.isEmpty());
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void searchSimilar_vectorStoreConfigured_delegates() {
        ReflectionTestUtils.setField(knowledgeService, "vectorStore", vectorStore);
        List<Document> docs = List.of(new Document("Java 基础教程"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(docs);

        List<Document> result = knowledgeService.searchSimilar("Java", 5);

        assertEquals(1, result.size());
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void searchSimilar_appliesThresholdAndTopKCap() {
        ReflectionTestUtils.setField(knowledgeService, "vectorStore", vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("doc")));

        knowledgeService.searchSimilar("Java", 50); // 超过 defaultTopK=10，应被截断

        org.mockito.ArgumentCaptor<SearchRequest> captor =
                org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertEquals(10, request.getTopK());
        assertEquals(0.7, request.getSimilarityThreshold());
    }
}
