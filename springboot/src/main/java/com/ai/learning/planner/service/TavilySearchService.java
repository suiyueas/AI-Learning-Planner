package com.ai.learning.planner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Tavily 联网搜索服务
 * 通过 Tavily Search API 获取互联网实时搜索结果
 */
@Service
@Slf4j
public class TavilySearchService {

    private final WebClient webClient;
    private final String apiKey;
    private final int maxResults;
    private final String searchDepth;

    public TavilySearchService(
            @Value("${tavily.api-key}") String apiKey,
            @Value("${tavily.base-url:https://api.tavily.com}") String baseUrl,
            @Value("${tavily.max-results:5}") int maxResults,
            @Value("${tavily.search-depth:basic}") String searchDepth) {
        this.apiKey = apiKey;
        this.maxResults = maxResults;
        this.searchDepth = searchDepth;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        log.info("TavilySearchService 初始化完成, baseUrl={}, maxResults={}", baseUrl, maxResults);
    }

    /**
     * 执行联网搜索
     *
     * @param query 搜索关键词
     * @return 格式化的搜索结果文本，供 AI 模型参考
     */
    public String search(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        log.info("[Tavily] 执行联网搜索: query={}", query);

        try {
            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "api_key", apiKey,
                    "search_depth", searchDepth,
                    "max_results", maxResults,
                    "include_answer", true
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("[Tavily] 搜索返回空响应");
                return "搜索未返回结果";
            }

            return formatResults(response);
        } catch (Exception e) {
            log.error("[Tavily] 联网搜索失败: {}", e.getMessage(), e);
            return "搜索服务暂时不可用: " + e.getMessage();
        }
    }

    /**
     * 将 Tavily API 响应格式化为 AI 可读的文本
     */
    @SuppressWarnings("unchecked")
    private String formatResults(Map<String, Object> response) {
        StringBuilder sb = new StringBuilder();

        // Tavily 直接提供的 AI 摘要
        String answer = (String) response.get("answer");
        if (answer != null && !answer.isBlank()) {
            sb.append("【搜索摘要】\n").append(answer).append("\n\n");
        }

        // 搜索结果列表
        Object resultsObj = response.get("results");
        if (resultsObj instanceof List<?> results && !results.isEmpty()) {
            sb.append("【搜索来源】\n");
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i) instanceof Map<?, ?> rawItem) {
                    Map<String, Object> item = (Map<String, Object>) rawItem;
                    String title = (String) item.getOrDefault("title", "未知标题");
                    String url = (String) item.getOrDefault("url", "");
                    String content = (String) item.getOrDefault("content", "");
                    double score = item.get("score") instanceof Number n ? n.doubleValue() : 0.0;

                    sb.append(String.format("%d. **%s**\n", i + 1, title));
                    if (!url.isBlank()) {
                        sb.append("   链接: ").append(url).append("\n");
                    }
                    if (!content.isBlank()) {
                        // 截取前300字符避免上下文过长
                        String snippet = content.length() > 300 ? content.substring(0, 300) + "..." : content;
                        sb.append("   摘要: ").append(snippet).append("\n");
                    }
                    sb.append(String.format("   相关度: %.0f%%\n\n", score * 100));
                }
            }
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? "搜索未返回有效结果" : result;
    }
}
