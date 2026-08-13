package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识文档仓储
 * 文档按 userId 归属；支持按标题、用户维度查询与删除，以及按用户计数（成就统计用）
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findAllByOrderByUploadedAtDesc();

    Optional<KnowledgeDocument> findByTitle(String title);

    Optional<KnowledgeDocument> findByTitleAndUserId(String title, String userId);

    List<KnowledgeDocument> findByUserIdOrderByUploadedAtDesc(String userId);

    Optional<KnowledgeDocument> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}