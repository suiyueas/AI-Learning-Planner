package com.ai.learning.planner.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 测试用桩 ChatModel：可配置同步/流式调用失败
 */
public class StubChatModel implements ChatModel {

    private final boolean fail;
    private final String content;

    public StubChatModel(boolean fail, String content) {
        this.fail = fail;
        this.content = content;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        if (fail) throw new RuntimeException("模型不可用（模拟 API Key 失效）");
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        if (fail) return Flux.error(new RuntimeException("模型不可用（模拟 API Key 失效）"));
        return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage(content)))));
    }

    public static String contentOf(ChatResponse response) {
        return response.getResult().getOutput().getText();
    }
}