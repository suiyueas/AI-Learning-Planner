package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 知识库状态查询服务
 * 聚合知识库文档和片段统计数据，供对话界面使用
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeStatusService {

    private final KnowledgeDocumentRepository documentRepository;
    private final SecurityContextHolder securityContextHolder;

    private String getCurrentUserId() {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            log.warn("[KnowledgeStatusService] 无法获取当前用户ID");
        }
        return userId;
    }

    /**
     * 获取知识库状态（仅返回当前用户的知识库数据；管理员返回全部数据）
     * @return 包含 connected, documentCount, chunkCount 的 Map
     */
    public Map<String, Object> getStatus() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Map.of(
                    "connected", false,
                    "documentCount", 0,
                    "chunkCount", 0,
                    "readyCount", 0
            );
        }

        // 管理员全局视图：统计所有用户的文档
        List<KnowledgeDocument> docs = securityContextHolder.isAdmin()
                ? documentRepository.findAllByOrderByUploadedAtDesc()
                : documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
        long docCount = docs.stream()
                .filter(d -> "ready".equals(d.getStatus()) || "processing".equals(d.getStatus()))
                .count();
        long chunkCount = docs.stream()
                .mapToLong(d -> d.getChunks() != null ? d.getChunks() : 0)
                .sum();
        long readyCount = docs.stream()
                .filter(d -> "ready".equals(d.getStatus()))
                .count();

        return Map.of(
                "connected", docCount > 0,
                "documentCount", docCount,
                "chunkCount", chunkCount,
                "readyCount", readyCount
        );
    }
}