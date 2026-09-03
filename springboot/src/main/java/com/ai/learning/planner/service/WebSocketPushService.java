package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.KnowledgeEvent;
import com.ai.learning.planner.handler.KnowledgeWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 推送服务
 * 负责向所有订阅客户端广播知识库状态变更事件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketPushService {

    private final KnowledgeWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * 推送知识库事件到所有订阅客户端
     */
    public void pushKnowledgeEvent(KnowledgeEvent event) {
        log.debug("推送 WebSocket 事件: type={}, docId={}", event.getType(), event.getDocId());
        try {
            String json = objectMapper.writeValueAsString(event);
            webSocketHandler.broadcast(json);
        } catch (Exception e) {
            log.error("序列化 WebSocket 事件失败: {}", e.getMessage());
        }
    }

    /**
     * 推送文档上传事件
     */
    public void pushDocUploaded(String docId, String docTitle) {
        pushKnowledgeEvent(KnowledgeEvent.docUploaded(docId, docTitle));
    }

    /**
     * 推送文档就绪事件（知识块生成完成）
     */
    public void pushDocReady(String docId, String docTitle, int chunks,
                             long totalDocs, long totalChunks, long readyDocs) {
        pushKnowledgeEvent(KnowledgeEvent.docReady(docId, docTitle, chunks, totalDocs, totalChunks, readyDocs));
    }

    /**
     * 推送文档删除事件
     */
    public void pushDocDeleted(String docId, long totalDocs, long totalChunks, long readyDocs) {
        pushKnowledgeEvent(KnowledgeEvent.docDeleted(docId, totalDocs, totalChunks, readyDocs));
    }

    /**
     * 推送文档错误事件（知识块生成失败）
     */
    public void pushDocError(String docId, String docTitle, String reason,
                             long totalDocs, long totalChunks, long readyDocs) {
        pushKnowledgeEvent(KnowledgeEvent.docError(docId, docTitle, reason, totalDocs, totalChunks, readyDocs));
    }

    /**
     * 推送全量生成完成事件
     */
    public void pushChunksGenerated(long totalDocs, long totalChunks, long readyDocs) {
        pushKnowledgeEvent(KnowledgeEvent.chunksGenerated(totalDocs, totalChunks, readyDocs));
    }
}
