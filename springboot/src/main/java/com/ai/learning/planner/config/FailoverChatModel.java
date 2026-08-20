package com.ai.learning.planner.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 多模型故障转移 ChatModel（FailoverChatModel）
 * 按优先级顺序包装多个 ChatModel，主模型调用失败（如 API Key 失效/网络异常）时自动降级到下一个可用模型，
 * 保证单一模型故障不中断服务；同步 call 与流式 stream 均支持降级。
 */
@Slf4j
public class FailoverChatModel implements ChatModel {

    /** 按优先级排序的委托模型（index 0 为主模型） */
    private final List<ChatModel> delegates;

    public FailoverChatModel(List<ChatModel> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    /**
     * 同步调用：主模型失败自动切换下一个，全部失败抛出最后一次异常
     */
    @Override
    public ChatResponse call(Prompt prompt) {
        RuntimeException lastError = null;
        for (int i = 0; i < delegates.size(); i++) {
            ChatModel delegate = delegates.get(i);
            try {
                ChatResponse response = delegate.call(prompt);
                if (i > 0) {
                    log.warn("[FailoverChatModel] 主模型不可用，已降级到第 {} 个模型完成调用", i + 1);
                }
                return response;
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("[FailoverChatModel] 模型[{}]调用失败: {}，尝试下一个模型", i, e.getMessage());
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("所有模型均不可用");
    }

    /**
     * 流式调用：任一委托模型流中断（onError）时切换下一个模型续流
     */
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return streamFrom(0, prompt);
    }

    private Flux<ChatResponse> streamFrom(int index, Prompt prompt) {
        if (index >= delegates.size()) {
            return Flux.error(new IllegalStateException("所有模型均不可用"));
        }
        ChatModel delegate = delegates.get(index);
        return Flux.defer(() -> delegate.stream(prompt))
                .onErrorResume(e -> {
                    log.warn("[FailoverChatModel] 流式调用模型[{}]失败: {}，切换下一个模型", index, e.getMessage());
                    return streamFrom(index + 1, prompt);
                });
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return delegates.isEmpty() ? null : delegates.get(0).getDefaultOptions();
    }

    /**
     * 获取主模型的模型名称（供监控/展示使用）
     */
    public String getPrimaryModelName() {
        if (delegates.isEmpty()) return "unknown";
        if (delegates.get(0) instanceof OpenAiChatModel openAiModel) {
            return openAiModel.getDefaultOptions().getModel();
        }
        return "unknown";
    }

    public List<ChatModel> getDelegates() {
        return delegates;
    }
}