package com.ai.learning.planner.service;

import com.ai.learning.planner.config.FailoverChatModel;
import com.ai.learning.planner.config.StubChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelManager 多模型故障转移测试
 */
class ModelManagerTest {

    private ModelManager buildManager(String... modelKeys) {
        Map<String, ChatModel> map = new LinkedHashMap<>();
        for (String key : modelKeys) {
            map.put(key, new StubChatModel(false, key));
        }
        return new ModelManager(map);
    }

    @Test
    void defaultModel_isFailover_whenMultipleModelsExist() {
        ModelManager manager = buildManager("deepSeekChatModel", "qwenChatModel", "miMoChatModel");

        assertEquals(ModelManager.FAILOVER_MODEL_KEY, manager.getCurrentModelKey());
        assertInstanceOf(FailoverChatModel.class, manager.getCurrentModel());
    }

    @Test
    void defaultModel_isDeepSeek_whenOnlyOneModel() {
        ModelManager manager = buildManager("deepSeekChatModel");

        assertEquals("deepSeekChatModel", manager.getCurrentModelKey());
        assertFalse(manager.getCurrentModel() instanceof FailoverChatModel);
    }

    @Test
    void availableKeys_excludeFailover() {
        ModelManager manager = buildManager("deepSeekChatModel", "qwenChatModel");

        assertFalse(manager.getAvailableModelKeys().contains(ModelManager.FAILOVER_MODEL_KEY));
        assertEquals(2, manager.getAvailableModelKeys().size());
    }

    @Test
    void shortNameAuto_resolvesToFailover() {
        ModelManager manager = buildManager("deepSeekChatModel", "qwenChatModel");

        assertInstanceOf(FailoverChatModel.class, manager.getModel("auto"));
        assertEquals(ModelManager.FAILOVER_MODEL_KEY, manager.toBeanName("auto"));
        assertEquals("auto", manager.toShortName(ModelManager.FAILOVER_MODEL_KEY));
    }

    @Test
    void switchToFailover_reEnablesAutoDegradation() {
        ModelManager manager = buildManager("deepSeekChatModel", "qwenChatModel");
        manager.switchModel("qwenChatModel");
        assertEquals("qwenChatModel", manager.getCurrentModelKey());

        manager.switchModel(ModelManager.FAILOVER_MODEL_KEY);
        assertEquals(ModelManager.FAILOVER_MODEL_KEY, manager.getCurrentModelKey());
        assertInstanceOf(FailoverChatModel.class, manager.getCurrentModel());
    }

    @Test
    void unknownModel_fallsBackToFailover() {
        ModelManager manager = buildManager("deepSeekChatModel", "qwenChatModel");

        assertInstanceOf(FailoverChatModel.class, manager.getModel("not-exist"));
    }
}