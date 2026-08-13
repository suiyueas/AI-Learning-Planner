package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识块（Chunk）仓储
 * 文档上传后经分块/向量化生成的知识片段，按文档 ID 查询、删除与计数
 */
@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findByDocIdOrderByChunkIndexAsc(String docId);
    void deleteByDocId(String docId);
    long countByDocId(String docId);

    List<KnowledgeChunk> findByContentContaining(String keyword);

    /**
     * 一次性查询所有文档的知识块数量（避免 N+1）
     * @return [docId, count] 数组列表
     */
    @Query("SELECT k.docId, COUNT(k) FROM KnowledgeChunk k GROUP BY k.docId")
    List<Object[]> countGroupByDocId();
}