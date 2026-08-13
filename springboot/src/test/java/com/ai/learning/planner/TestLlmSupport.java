package com.ai.learning.planner;

import com.ai.learning.planner.service.ModelManager;
import org.springframework.ai.chat.client.ChatClient;

import java.util.LinkedList;
import java.util.Queue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试工具：构造 ChatClient 链式 mock（Spring AI 1.1.7 接口）
 * 通过响应队列按调用顺序返回预设的 LLM 输出，支持集成测试驱动完整推理闭环
 */
public final class TestLlmSupport {

    /** 预设 LLM 响应队列（按调用顺序消费，耗尽后返回 [FINISH]） */
    private static final Queue<String> RESPONSES = new LinkedList<>();

    private TestLlmSupport() {
    }

    /**
     * 绑定 ModelManager mock：所有 LLM 调用（system+user 链）走同一 mock
     */
    public static void bindChatClient(ModelManager modelManager) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(modelManager.createChatClient()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenAnswer(inv ->
                RESPONSES.isEmpty() ? "[FINISH] 任务完成" : RESPONSES.poll());
    }

    /**
     * 预置一条 LLM 响应（按调用顺序消费）
     */
    public static void queue(String response) {
        RESPONSES.add(response);
    }

    /**
     * 清空响应队列
     */
    public static void reset() {
        RESPONSES.clear();
    }
}
