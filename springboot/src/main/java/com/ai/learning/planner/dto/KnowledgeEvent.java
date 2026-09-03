package com.ai.learning.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库事件 - 用于 WebSocket 推送
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeEvent {

    /**
     * 事件类型：doc_uploaded / doc_ready / doc_error / doc_deleted / chunks_generated
     */
    private String type;

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 文档状态
     */
    private String status;

    /**
     * 知识块数量
     */
    private Integer chunks;

    /**
     * 总文档数
     */
    private Long totalDocs;

    /**
     * 总知识块数
     */
    private Long totalChunks;

    /**
     * 就绪文档数
     */
    private Long readyDocs;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 创建文档上传事件
     */
    public static KnowledgeEvent docUploaded(String docId, String docTitle) {
        return KnowledgeEvent.builder()
                .type("doc_uploaded")
                .docId(docId)
                .docTitle(docTitle)
                .status("processing")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建文档就绪事件
     */
    public static KnowledgeEvent docReady(String docId, String docTitle, int chunks,
                                          long totalDocs, long totalChunks, long readyDocs) {
        return KnowledgeEvent.builder()
                .type("doc_ready")
                .docId(docId)
                .docTitle(docTitle)
                .status("ready")
                .chunks(chunks)
                .totalDocs(totalDocs)
                .totalChunks(totalChunks)
                .readyDocs(readyDocs)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建文档删除事件
     */
    public static KnowledgeEvent docDeleted(String docId, long totalDocs, long totalChunks, long readyDocs) {
        return KnowledgeEvent.builder()
                .type("doc_deleted")
                .docId(docId)
                .totalDocs(totalDocs)
                .totalChunks(totalChunks)
                .readyDocs(readyDocs)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建文档错误事件（分块生成失败）
     */
    public static KnowledgeEvent docError(String docId, String docTitle, String reason,
                                          long totalDocs, long totalChunks, long readyDocs) {
        return KnowledgeEvent.builder()
                .type("doc_error")
                .docId(docId)
                .docTitle(docTitle)
                .status("error")
                .totalDocs(totalDocs)
                .totalChunks(totalChunks)
                .readyDocs(readyDocs)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建全量生成完成事件
     */
    public static KnowledgeEvent chunksGenerated(long totalDocs, long totalChunks, long readyDocs) {
        return KnowledgeEvent.builder()
                .type("chunks_generated")
                .totalDocs(totalDocs)
                .totalChunks(totalChunks)
                .readyDocs(readyDocs)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
