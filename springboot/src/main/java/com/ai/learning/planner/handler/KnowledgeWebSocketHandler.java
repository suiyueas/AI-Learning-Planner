package com.ai.learning.planner.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 知识库 WebSocket 处理器
 * 管理客户端连接，支持广播消息到所有已连接客户端
 */
@Component
@Slf4j
public class KnowledgeWebSocketHandler extends TextWebSocketHandler {

    /**
     * 存储所有已连接的 WebSocket 会话
     */
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 连接已建立: sessionId={}, 总连接数={}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket 连接已关闭: sessionId={}, 总连接数={}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端可以发送 ping 消息保持连接
        log.debug("收到 WebSocket 消息: sessionId={}, payload={}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
        sessions.remove(session);
    }

    /**
     * 广播消息到所有已连接客户端
     * @param message JSON 格式的消息内容
     */
    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("发送 WebSocket 消息失败: sessionId={}", session.getId(), e);
                }
            }
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectedCount() {
        return sessions.size();
    }
}
