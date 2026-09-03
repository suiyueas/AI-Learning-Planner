package com.ai.learning.planner.runner;

import com.ai.learning.planner.agent.tool.AgentToolManager;
import com.ai.learning.planner.service.KnowledgeService;
import com.ai.learning.planner.service.TavilySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具注册器
 * 在应用启动时将实际业务工具注册到 AgentToolManager，
 * 使 Agent 的工具调用链路（think→act→observe）能够真正执行
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolRegistrationRunner implements ApplicationRunner {

    private final AgentToolManager agentToolManager;
    private final KnowledgeService knowledgeService;
    private final TavilySearchService tavilySearchService;

    @Override
    public void run(ApplicationArguments args) {
        registerSearchTools();
        log.info("[ToolRegistration] 工具注册完成，已注册 {} 个工具",
                agentToolManager.getRegisteredTools().size());
    }

    private void registerSearchTools() {
        // 知识库语义检索（向量 + MySQL 降级，带用户隔离）
        agentToolManager.registerTool("search_knowledge", params -> {
            String query = (String) params.getOrDefault("query", "");
            int topK = params.containsKey("topK") ? Integer.parseInt(params.get("topK").toString()) : 5;
            String userId = AgentToolManager.getCurrentUserId();
            List<Document> results = userId != null && !userId.isBlank()
                    ? knowledgeService.searchSimilar(query, topK, userId)
                    : knowledgeService.searchSimilar(query, topK);
            if (results.isEmpty()) return "未找到相关知识";
            return results.stream()
                    .map(d -> {
                        String source = (String) d.getMetadata().getOrDefault("source", "unknown");
                        String title = (String) d.getMetadata().getOrDefault("title", "未知");
                        return String.format("[%s:%s] %s", source, title, d.getText());
                    })
                    .collect(Collectors.joining("\n---\n"));
        }, "知识库语义检索（支持向量检索和关键词降级）");

        // 联网搜索
        agentToolManager.registerTool("web_search", params -> {
            String query = (String) params.getOrDefault("query", "");
            String result = tavilySearchService.search(query);
            return result != null ? result : "搜索无结果";
        }, "联网搜索，获取互联网实时信息");
    }
}