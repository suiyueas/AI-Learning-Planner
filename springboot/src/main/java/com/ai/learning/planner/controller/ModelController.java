package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.dto.SwitchModelRequest;
import org.springframework.security.access.AccessDeniedException;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.ModelManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型管理控制器
 * 查询当前/可用模型列表；切换全局模型为管理操作（仅管理员）
 */
@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
@Slf4j
public class ModelController {

    private final ModelManager modelManager;
    private final SecurityContextHolder securityContextHolder;

    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> getCurrentModel() {
        String shortName = modelManager.getCurrentModelKeyShort();
        ChatModel model = modelManager.getCurrentModel();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", shortName);
        data.put("displayName", modelManager.getModelDisplayNameByShortName(shortName));
        data.put("modelName", modelManager.getModelName(model));
        return ApiResponse.success(data);
    }

    @GetMapping("/available")
    public ApiResponse<Map<String, Object>> getAvailableModels() {
        Map<String, Object> configs = new LinkedHashMap<>();
        for (String beanName : modelManager.getAvailableModelKeys()) {
            String shortName = modelManager.toShortName(beanName);
            ChatModel model = modelManager.getModel(beanName);
            configs.put(shortName, Map.of(
                    "provider", shortName,
                    "displayName", modelManager.getModelDisplayNameByShortName(shortName),
                    "modelName", modelManager.getModelName(model)
            ));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("models", configs);
        data.put("current", modelManager.getCurrentModelKeyShort());
        return ApiResponse.success(data);
    }

    @PostMapping("/switch")
    public ApiResponse<Map<String, Object>> switchModel(@Valid @RequestBody SwitchModelRequest request) {
        // 切换的是应用级全局模型状态，影响所有用户，仅管理员可操作
        if (!securityContextHolder.isAdmin()) {
            throw new AccessDeniedException("仅管理员可切换全局模型");
        }
        String provider = request.getProvider();
        log.info("切换模型请求: provider={}", provider);
        modelManager.switchModelByShortName(provider);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", provider);
        data.put("displayName", modelManager.getModelDisplayNameByShortName(provider));
        data.put("message", "已切换到 " + modelManager.getModelDisplayNameByShortName(provider));
        return ApiResponse.success(data);
    }
}