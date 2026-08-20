package com.ai.learning.planner.service;

import com.ai.learning.planner.config.FailoverChatModel;
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
 * 管理所有 ChatModel 实例，支持动态切换和按名称路由；
 * 默认启用多模型故障转移（FailoverChatModel：DeepSeek → Qwen → MiMo），
 * 主模型 API Key 失效/调用异常时自动降级到下一个可用模型，服务不中断。
 */
@Service
@Slf4j
public class ModelManager {

    /** 故障转移模型 Bean key */
    public static final String FAILOVER_MODEL_KEY = "failoverChatModel";

    /** 故障转移模型前端短名称 */
    public static final String FAILOVER_SHORT_NAME = "auto";

    private final Map<String, ChatModel> modelMap;

    /** 多模型故障转移（主 → 备用按序降级），不足两个模型时为 null */
    private final ChatModel failoverModel;

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

        // 构建故障转移链（DeepSeek → Qwen → MiMo 优先级），仅存在多个模型时启用
        List<ChatModel> ordered = new ArrayList<>(3);
        for (String beanName : new String[]{"deepSeekChatModel", "qwenChatModel", "miMoChatModel"}) {
            ChatModel model = this.modelMap.get(beanName);
            if (model != null) ordered.add(model);
        }
        this.failoverModel = ordered.size() >= 2 ? new FailoverChatModel(ordered) : null;

        // 默认模型：优先故障转移（多模型高可用），否则 DeepSeek，再否则任意可用模型
        if (this.failoverModel != null) {
            currentModelKey.set(FAILOVER_MODEL_KEY);
        } else if (this.modelMap.containsKey("deepSeekChatModel")) {
            currentModelKey.set("deepSeekChatModel");
        } else {
            currentModelKey.set(this.modelMap.keySet().iterator().next());
        }

        log.info("✅ ModelManager 初始化完成，可用模型: {} (默认: {}{})",
                this.modelMap.keySet(), currentModelKey.get(),
                this.failoverModel != null ? "，已启用多模型故障转移降级" : "");
    }

    /**
     * 获取当前模型
     */
    public ChatModel getCurrentModel() {
        return getModel(currentModelKey.get());
    }

    /**
     * 根据 key 获取模型，同时支持 Bean 名称和短名称；故障转移模式返回 FailoverChatModel
     */
    public ChatModel getModel(String key) {
        // 0. 故障转移模式（Bean 名或短名称 auto）
        if (failoverModel != null) {
            String bean = toBeanName(key);
            if (FAILOVER_MODEL_KEY.equals(key) || FAILOVER_MODEL_KEY.equals(bean)) {
                return failoverModel;
            }
        }

        // 1. 直接按 key 查询
        ChatModel model = modelMap.get(key);
        if (model != null) return model;

        // 2. 尝试作为短名称转为 Bean 名称后查询
        String beanName = toBeanName(key);
        if (!beanName.equals(key)) {
            model = modelMap.get(beanName);
            if (model != null) return model;
        }

        // 3. 降级到默认（故障转移优先，保证应用始终可用）
        log.warn("模型 {} 不存在（短名称: {}），降级到默认模型: {}", key, beanName, currentModelKey.get());
        return failoverModel != null ? failoverModel : modelMap.get(currentModelKey.get());
    }

    /**
     * 切换当前模型
     */
    public String switchModel(String modelKey) {
        if (FAILOVER_MODEL_KEY.equals(modelKey)) {
            currentModelKey.set(modelKey);
            log.info("已切换到自动降级模式（多模型故障转移）");
            return modelKey;
        }
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
     * 获取可用模型列表（返回 Bean 名称，排除故障转移聚合模型）
     */
    public List<String> getAvailableModelKeys() {
        List<String> keys = new ArrayList<>(modelMap.keySet());
        keys.remove(FAILOVER_MODEL_KEY);
        return keys;
    }

    /**
     * 获取模型显示名称（接收 Bean 名称）
     */
    public String getModelDisplayName(String key) {
        return switch (key) {
            case "qwenChatModel" -> "Qwen-Max（阿里云）";
            case "deepSeekChatModel" -> "DeepSeek-V4-Flash";
            case "miMoChatModel" -> "小米 MiMo-V2.5-Pro";
            case FAILOVER_MODEL_KEY -> "自动降级（多模型）";
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
            if (model instanceof FailoverChatModel failover) {
                return failover.getPrimaryModelName();
            }
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
            case FAILOVER_MODEL_KEY -> FAILOVER_SHORT_NAME;
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
            case FAILOVER_SHORT_NAME -> FAILOVER_MODEL_KEY;
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