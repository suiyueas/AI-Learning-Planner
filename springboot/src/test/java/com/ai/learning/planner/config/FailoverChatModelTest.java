package com.ai.learning.planner.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FailoverChatModel 故障转移测试：主模型失效自动降级到备用模型
 */
class FailoverChatModelTest {

    private static final Prompt PROMPT = new Prompt("测试");

    @Test
    void call_primaryFails_usesBackup() {
        FailoverChatModel model = new FailoverChatModel(List.of(
                new StubChatModel(true, "主模型"),
                new StubChatModel(false, "备用模型")));

        ChatResponse response = model.call(PROMPT);

        assertEquals("备用模型", StubChatModel.contentOf(response));
    }

    @Test
    void call_primaryOk_returnsPrimary() {
        FailoverChatModel model = new FailoverChatModel(List.of(
                new StubChatModel(false, "主模型"),
                new StubChatModel(false, "备用模型")));

        ChatResponse response = model.call(PROMPT);

        assertEquals("主模型", StubChatModel.contentOf(response));
    }

    @Test
    void call_allFail_throws() {
        FailoverChatModel model = new FailoverChatModel(List.of(
                new StubChatModel(true, "a"),
                new StubChatModel(true, "b")));

        assertThrows(RuntimeException.class, () -> model.call(PROMPT));
    }

    @Test
    void stream_primaryError_fallsBack() {
        FailoverChatModel model = new FailoverChatModel(List.of(
                new StubChatModel(true, "主模型"),
                new StubChatModel(false, "备用模型")));

        String content = model.stream(PROMPT)
                .map(StubChatModel::contentOf)
                .collectList()
                .block()
                .stream().reduce("", String::concat);

        assertEquals("备用模型", content);
    }

    @Test
    void stream_allFail_errors() {
        FailoverChatModel model = new FailoverChatModel(List.of(
                new StubChatModel(true, "a"),
                new StubChatModel(true, "b")));

        assertThrows(RuntimeException.class,
                () -> model.stream(PROMPT).blockLast());
    }

    @Test
    void getPrimaryModelName_emptyDelegates_unknown() {
        FailoverChatModel model = new FailoverChatModel(List.of());
        assertEquals("unknown", model.getPrimaryModelName());
    }
}