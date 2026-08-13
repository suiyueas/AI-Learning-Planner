package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 知识块实体
 * 对应 knowledge_chunks 表，文档分块后的知识片段，用于关键词降级搜索与向量化
 */
@Entity
@Table(name = "knowledge_chunks", indexes = {
    @Index(name = "idx_chunk_doc_id", columnList = "doc_id"),
    @Index(name = "idx_chunk_doc_id_index", columnList = "doc_id, chunk_index")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "doc_id", nullable = false, length = 255)
    private String docId;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "content_preview", length = 500)
    private String contentPreview;
    
    @Column(name = "char_count")
    private Integer charCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}