package com.ai.learning.planner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型管理器
 * 管理所有 ChatModel 实例，支持动态切换和按名称路由
 */
@Service
@Slf4j
public class ModelManager {

    private final Map<String, ChatModel> modelMap;
    private final AtomicReference<String> currentModelKey = new AtomicReference<>("deepSeekChatModel");

    /**
     * Spring 会自动注入所有 ChatModel Bean，key 为 Bean 名称
     */
    public ModelManager(Map<String, ChatModel> modelMap) {
        this.modelMap = new ConcurrentHashMap<>(modelMap);
        
        // 检查是否有可用的模型
        if (this.modelMap.isEmpty()) {
            log.error("没有可用的 ChatModel Bean！请检查 MultiModelConfig 配置。");
            throw new IllegalStateException("没有可用的 ChatModel Bean，请检查 MultiModelConfig 是否正确初始化。");
        }

        // 设置默认模型（优先 deepSeekChatModel）
        if (this.modelMap.containsKey("deepSeekChatModel")) {
            currentModelKey.set("deepSeekChatModel");
        } else {
            currentModelKey.set(this.modelMap.keySet().iterator().next());
        }

        log.info("✅ ModelManager 初始化完成，可用模型: {} (默认: {})",
                this.modelMap.keySet(), currentModelKey.get());
    }

    /**
     * 获取当前模型
     */
    public ChatModel getCurrentModel() {
        return getModel(currentModelKey.get());
    }

    /**
     * 根据 key 获取模型，同时支持 Bean 名称和短名称
     */
    public ChatModel getModel(String key) {
        // 1. 直接按 key 查询
        ChatModel model = modelMap.get(key);
        if (model != null) return model;

        // 2. 尝试作为短名称转为 Bean 名称后查询
        String beanName = toBeanName(key);
        if (!beanName.equals(key)) {
            model = modelMap.get(beanName);
            if (model != null) return model;
        }

        // 3. 降级到默认
        log.warn("模型 {} 不存在（短名称: {}），降级到默认模型: {}", key, beanName, currentModelKey.get());
        return modelMap.get(currentModelKey.get());
    }

    /**
     * 切换当前模型
     */
    public String switchModel(String modelKey) {
        if (!modelMap.containsKey(modelKey)) {
            throw new IllegalArgumentException("不支持的模型: " + modelKey + "，可用模型: " + modelMap.keySet());
        }
        currentModelKey.set(modelKey);
        log.info("当前模型已切换到: {}", modelKey);
        return modelKey;
    }

    /**
     * 切换当前模型（接收短名称）
     */
    public String switchModelByShortName(String shortName) {
        return switchModel(toBeanName(shortName));
    }

    /**
     * 获取当前模型 key
     */
    public String getCurrentModelKey() {
        return currentModelKey.get();
    }

    /**
     * 获取当前模型 key（短名称）
     */
    public String getCurrentModelKeyShort() {
        return toShortName(currentModelKey.get());
    }

    /**
     * 获取当前模型显示名称
     */
    public String getCurrentModelDisplayName() {
        return getModelDisplayName(currentModelKey.get());
    }

    /**
     * 获取可用模型列表（返回 Bean 名称）
     */
    public List<String> getAvailableModelKeys() {
        return new ArrayList<>(modelMap.keySet());
    }

    /**
     * 获取模型显示名称（接收 Bean 名称）
     */
    public String getModelDisplayName(String key) {
        return switch (key) {
            case "qwenChatModel" -> "Qwen-Max（阿里云）";
            case "deepSeekChatModel" -> "DeepSeek-V4-Flash";
            case "miMoChatModel" -> "小米 MiMo-V2.5-Pro";
            default -> key;
        };
    }

    /**
     * 根据短名称获取模型显示名称
     */
    public String getModelDisplayNameByShortName(String shortName) {
        return getModelDisplayName(toBeanName(shortName));
    }

    /**
     * 从 ChatModel 实例获取模型名称字符串
     */
    public String getModelName(ChatModel model) {
        try {
            if (model instanceof org.springframework.ai.openai.OpenAiChatModel openAiModel) {
                return openAiModel.getDefaultOptions().getModel();
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Bean 名称 → 前端短名称
     */
    public String toShortName(String beanName) {
        return switch (beanName) {
            case "qwenChatModel" -> "qwen";
            case "deepSeekChatModel" -> "deepseek";
            case "miMoChatModel" -> "xiaomi";
            default -> beanName;
        };
    }

    /**
     * 短名称 → Bean 名称
     */
    public String toBeanName(String shortName) {
        return switch (shortName) {
            case "qwen" -> "qwenChatModel";
            case "deepseek" -> "deepSeekChatModel";
            case "xiaomi", "mimo" -> "miMoChatModel";
            default -> shortName;
        };
    }

    /**
     * 创建当前模型的 ChatClient（兼容 Spring AI 1.1.7 和 2.0.0）
     */
    public ChatClient createChatClient() {
        // Spring AI 1.1.7 使用 ChatClient.create(model)
        // Spring AI 2.0.0 使用 ChatClient.builder(model).build()
        // 这里尝试兼容两种写法
        try {
            // 优先使用 1.1.7 方式
            return ChatClient.create(getCurrentModel());
        } catch (NoSuchMethodError e) {
            // 如果 1.1.7 方式不可用，尝试 2.0.0 方式
            log.warn("ChatClient.create() 不可用，尝试使用 builder 方式");
            return ChatClient.builder(getCurrentModel()).build();
        }
    }

    /**
     * 创建指定模型的 ChatClient
     */
    public ChatClient createChatClient(String modelKey) {
        try {
            return ChatClient.create(getModel(modelKey));
        } catch (NoSuchMethodError e) {
            return ChatClient.builder(getModel(modelKey)).build();
        }
    }
}