package com.ai.learning.planner.controller;

import com.ai.learning.planner.service.ModelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具健康检查控制器
 * 提供 MCP 工具服务的健康状态和可用性信息
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class MCPHealthController {

    private final ModelManager modelManager;

    /**
     * MCP 服务健康检查
     * 返回整体健康状态、可用模型列表和工具列表
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        List<String> availableModels = modelManager.getAvailableModelKeys();
        String currentModel = modelManager.getCurrentModelKey();

        boolean modelAvailable = !availableModels.isEmpty();
        String status = modelAvailable ? "UP" : "DEGRADED";

        Map<String, Object> healthInfo = Map.of(
                "status", status,
                "service", "learning-planner-mcp",
                "version", "1.0.0",
                "model", Map.of(
                        "current", currentModel,
                        "available", availableModels,
                        "displayName", modelManager.getCurrentModelDisplayName()
                ),
                "tools", List.of(
                        Map.of("id", "search_resources", "name", "资源检索"),
                        Map.of("id", "web_search", "name", "联网搜索"),
                        Map.of("id", "web_fetch", "name", "网页抓取"),
                        Map.of("id", "query_knowledge_graph", "name", "知识图谱查询"),
                        Map.of("id", "summarize_document", "name", "文档摘要", "aiEnabled", true),
                        Map.of("id", "extract_keywords", "name", "知识点提取", "aiEnabled", true),
                        Map.of("id", "generate_quiz", "name", "生成测验题", "aiEnabled", true),
                        Map.of("id", "translate_text", "name", "文本翻译", "aiEnabled", true),
                        Map.of("id", "learning_assistant", "name", "智能学习助手", "aiEnabled", true, "orchestrated", true),
                        Map.of("id", "search_tools", "name", "工具搜索"),
                        Map.of("id", "get_tool_detail", "name", "工具详情")
                ),
                "timestamp", System.currentTimeMillis()
        );

        log.debug("MCP 健康检查: status={}, models={}, currentModel={}", status, availableModels, currentModel);
        return ResponseEntity.ok(Map.of("success", true, "data", healthInfo));
    }
}
