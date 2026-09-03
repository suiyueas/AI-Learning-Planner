package com.ai.learning.planner.config;

import com.ai.learning.planner.handler.KnowledgeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 - 原生 WebSocket（无 STOMP 依赖）
 * 用于实时推送知识库状态变更到前端
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final KnowledgeWebSocketHandler knowledgeWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(knowledgeWebSocketHandler, "/ws/knowledge")
                .setAllowedOriginPatterns("*");
    }
}
