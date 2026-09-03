package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.entity.ToolExecutionRecord;
import com.ai.learning.planner.mcp.ai.AiToolContext;
import com.ai.learning.planner.mcp.ai.DocumentSummaryTool;
import com.ai.learning.planner.mcp.ai.KnowledgeExtractionTool;
import com.ai.learning.planner.mcp.ai.LearningAssistantTool;
import com.ai.learning.planner.mcp.ai.McpAiProperties;
import com.ai.learning.planner.mcp.ai.QuizGenerationTool;
import com.ai.learning.planner.mcp.ai.ToolDefinition;
import com.ai.learning.planner.mcp.ai.ToolDefinitionRegistry;
import com.ai.learning.planner.mcp.ai.TranslationTool;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.repository.ToolExecutionRecordRepository;
import com.ai.learning.planner.repository.UserRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.security.ToolCallConfirmationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具执行服务
 * 提供AI工具的注册、调用和执行记录管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ToolExecutionService {

    private final ToolExecutionRecordRepository recordRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeService knowledgeService;
    private final TavilySearchService tavilySearchService;
    private final ToolStatsService toolStatsService;
    private final ChatClient chatClient;
    private final ModelManager modelManager;
    private final ObjectMapper objectMapper;
    private final ToolCallConfirmationService toolCallConfirmationService;
    private final SecurityContextHolder securityContextHolder;
    private final UserRepository userRepository;

    // ===== AI 赋能工具（mcp.ai 包） =====
    private final DocumentSummaryTool documentSummaryTool;
    private final KnowledgeExtractionTool knowledgeExtractionTool;
    private final QuizGenerationTool quizGenerationTool;
    private final TranslationTool translationTool;
    private final LearningAssistantTool learningAssistantTool;
    private final McpAiProperties mcpAiProperties;

    /**
     * 验证所有依赖注入正确
     */
    @PostConstruct
    public void init() {
        // 依赖注入检查（降级为 DEBUG，避免日常日志冗余）
        log.debug("===== ToolExecutionService 初始化检查 =====");
        log.debug("recordRepository: {}", recordRepository != null ? "✓" : "✗");
        log.debug("knowledgeDocumentRepository: {}", knowledgeDocumentRepository != null ? "✓" : "✗");
        log.debug("knowledgeService: {}", knowledgeService != null ? "✓" : "✗");
        log.debug("tavilySearchService: {}", tavilySearchService != null ? "✓" : "✗");
        log.debug("toolStatsService: {}", toolStatsService != null ? "✓" : "✗");
        log.debug("chatClient: {}", chatClient != null ? "✓" : "✗");
        log.debug("modelManager: {}", modelManager != null ? "✓" : "✗");
        log.debug("objectMapper: {}", objectMapper != null ? "✓" : "✗");

        // 工具注册清单（启动时显示一次即可）
        List<String> toolIds = ToolDefinitionRegistry.visibleTools().stream()
                .map(ToolDefinition::id)
                .collect(Collectors.toList());
        log.info("✅ 已注册 {} 个 MCP 工具: {}", toolIds.size(), toolIds);
        log.info("✅ AI 工具层初始化完成，模型: {}（启用: {}，超时: {}ms，降级摘要上限: {}字）",
                documentSummaryTool.getModelDisplayName(),
                documentSummaryTool.isEnabled(),
                mcpAiProperties.getTimeout(),
                mcpAiProperties.getFallback().getSummaryMaxLength());
        log.info("✅ ToolExecutionService 分发器已就绪");
        log.info("===== ToolExecutionService 初始化完成 =====");
    }

    /**
     * 判断用户是否为管理员（基于 userId 实时查库，不依赖请求线程的 SecurityContext）
     */
    private boolean isAdminUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try {
            return userRepository.findById(Long.valueOf(userId))
                    .map(u -> "ADMIN".equals(u.getRole()))
                    .orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 获取 ChatClient，优先使用注入的 Bean，不可用时通过 ModelManager 创建
     */
    private ChatClient getChatClient() {
        if (chatClient != null) {
            return chatClient;
        }
        log.warn("注入的 ChatClient 为 null，通过 ModelManager 创建备用 ChatClient");
        try {
            return modelManager.createChatClient();
        } catch (Exception e) {
            log.error("创建备用 ChatClient 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 执行工具并保存记录
     * 包含多层异常保护：内部每个工具有独立try-catch，此处提供外层兜底
     */
    public Map<String, Object> executeTool(String toolId, Map<String, Object> params, String userId) {
        long startTime = System.currentTimeMillis();
        // 参数截断打印，避免完整用户内容/文档全文落入日志（日志脱敏）
        String paramsForLog = params != null ? String.valueOf(params) : "";
        if (paramsForLog.length() > 200) {
            paramsForLog = paramsForLog.substring(0, 200) + "...(截断)";
        }
        log.info("===== 工具执行开始: toolId={}, userId={}, params={} =====", toolId, userId, paramsForLog);
        Map<String, Object> result = new HashMap<>();

        // ===== P0: 管理员专属工具校验（基于可信 userId 实时查库，游客/普通用户直接拒绝） =====
        ToolDefinition toolDef = ToolDefinitionRegistry.byId(toolId).orElse(null);
        if (toolDef != null && toolDef.adminOnly() && !isAdminUser(userId)) {
            log.warn("[ToolExecutionService] 管理员专属工具被非管理员调用: toolId={}, userId={}", toolId, userId);
            return Map.of("success", false, "errorCode", "FORBIDDEN",
                    "message", "该工具仅管理员可用");
        }

        // ===== P0: 被禁用工具拦截（管理员通过 /tools/{id}/toggle 禁用后，禁止任何执行） =====
        if (toolDef != null && !ToolDefinitionRegistry.isEnabled(toolId)) {
            log.warn("[ToolExecutionService] 工具已被管理员禁用: toolId={}, userId={}", toolId, userId);
            return Map.of("success", false, "errorCode", "DISABLED",
                    "message", "该工具已被管理员禁用");
        }

        // ===== P0: 工具调用二次确认检查 =====
        if (toolCallConfirmationService.requiresConfirmation(toolId, userId)) {
            String confirmationToken = params != null ? params.get("_confirmationToken") != null ?
                    params.get("_confirmationToken").toString() : null : null;

            if (confirmationToken == null || !toolCallConfirmationService.isConfirmed(confirmationToken, userId)) {
                String token = toolCallConfirmationService.createConfirmationToken(toolId, params, userId);
                log.warn("[ToolExecutionService] 高风险工具调用需要二次确认: toolId={}, userId={}, token={}",
                        toolId, userId, token);
                return Map.of(
                        "success", false,
                        "requiresConfirmation", true,
                        "confirmationToken", token,
                        "message", "此操作需要二次确认，请回复\"确认执行\"以继续"
                );
            }
        }

        try {
            AiToolContext ctx = AiToolContext.ofUserId(userId);
            result = switch (toolId) {
                // ===== 新工具名称（精简版6大工具） =====
                case "unified_academic_search" -> executeUnifiedAcademicSearch(params);
                case "deep_document_analysis" -> executeDeepDocumentAnalysis(params, ctx);
                case "smart_quiz_generation" -> executeSmartQuizGeneration(params, ctx);
                case "academic_translation" -> executeAcademicTranslation(params, ctx);
                case "full_chain_learning" -> executeFullChainLearning(params, ctx);
                case "tool_debug_panel" -> executeToolDebugPanel(params);

                // ===== 旧工具名称（兼容模式，已标记 isHidden 但仍可通过旧名调用）=====
                case "web_search" -> executeWebSearch(params);
                case "web_fetch" -> executeWebFetch(params);
                // AI 赋能工具（委托 mcp.ai 包，统一走降级兜底）
                case "summarize_document" -> documentSummaryTool.execute(params, ctx);
                case "extract_keywords" -> knowledgeExtractionTool.execute(params, ctx);
                case "generate_quiz" -> quizGenerationTool.execute(params, ctx);
                case "translate_text" -> translationTool.execute(normalizeTranslateParams(params), ctx);
                // 工具编排
                case "learning_assistant" -> learningAssistantTool.execute(params, ctx);
                // 渐进式工具发现（元工具）
                case "search_tools" -> executeSearchTools(params);
                case "get_tool_detail" -> executeGetToolDetail(params);
                default -> {
                    log.warn("未知工具请求: toolId={}", toolId);
                    yield Map.of("success", false, "errorCode", "TOOL_NOT_FOUND",
                            "message", "工具 '" + toolId + "' 未注册或暂未上线，请稍后再试或联系管理员");
                }
            };

            // 🔴 关键修复：将不可变 Map.of() 结果包装为可变 HashMap
            result = new HashMap<>(result);

            // 只在result没有明确设置success字段时才标记为true
            if (!result.containsKey("success")) {
                result.put("success", true);
            }
        } catch (Exception e) {
            log.error("工具执行失败[外层兜底]: toolId={}, error={}", toolId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "执行失败: " + e.getMessage());
            // 添加降级数据（当AI API完全不可用时保证前端有数据展示）
            Map<String, Object> fallbackData = getFallbackData(toolId, params);
            if (fallbackData != null) {
                result.putAll(fallbackData);
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;

        // 保存执行记录（确保即使在catch后也执行）
        try {
            saveExecutionRecord(toolId, params, result, executionTime, userId);
        } catch (Exception e) {
            log.error("保存执行记录失败: toolId={}, error={}", toolId, e.getMessage(), e);
        }

        // 记录工具调用统计
        try {
            toolStatsService.recordall(toolId);
        } catch (Exception e) {
            log.warn("记录工具调用统计失败: toolId={}, error={}", toolId, e.getMessage());
        }

        return result;
    }

    /**
     * web_search - 调用 Tavily API 联网搜索
     */
    private Map<String, Object> executeWebSearch(Map<String, Object> params) {
        String query = params.getOrDefault("query", "").toString().trim();
        int numResults = params.get("numResults") instanceof Number n ? n.intValue() : 5;

        if (query.isEmpty()) {
            return Map.of("message", "搜索关键词不能为空", "results", List.of());
        }

        // 调用 Tavily 搜索服务
        String rawResults = tavilySearchService.search(query);

        return Map.of(
                "query", query,
                "numResults", numResults,
                "rawResults", rawResults,
                "message", "联网搜索完成"
        );
    }

    /**
     * web_fetch - 网页抓取（暂不实现，返回开发中提示）
     */
    private Map<String, Object> executeWebFetch(Map<String, Object> params) {
        String url = params.getOrDefault("url", "").toString().trim();
        log.info("executeWebFetch: url={} - 功能开发中，返回提示信息", url);
        return Map.of(
                "success", true,
                "message", "网页抓取功能正在开发中，敬请期待！",
                "url", url,
                "note", "当前版本暂不支持网页抓取，该功能将在后续版本中上线。"
        );
    }

    /**
     * tool_debug_panel - 工具调试面板（管理员功能）
     */
    private Map<String, Object> executeToolDebugPanel(Map<String, Object> params) {
        log.info("executeToolDebugPanel: 管理员调试面板访问");
        return Map.of(
                "success", true,
                "message", "工具调试面板正在开发中，敬请期待！",
                "note", "该功能仅面向管理员开放，用于调试和诊断工具执行问题。"
        );
    }

    /**
     * unified_academic_search - 全域学术检索（合并版）
     * 同时搜索内部知识库、外部联网资源
     */
    private Map<String, Object> executeUnifiedAcademicSearch(Map<String, Object> params) {
        String query = params.getOrDefault("query", "").toString().trim();
        boolean searchInternal = params.getOrDefault("searchInternal", "true").toString().equals("true");
        boolean searchWeb = params.getOrDefault("searchWeb", "true").toString().equals("true");
        int limit = params.get("limit") instanceof Number n ? n.intValue() : 5;

        log.info("executeUnifiedAcademicSearch: query={}, internal={}, web={}, limit={}",
                query, searchInternal, searchWeb, limit);

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> allResults = new ArrayList<>();

        // 1. 内部知识库检索（knowledge_documents 表）
        if (searchInternal) {
            try {
                List<KnowledgeDocument> docs = knowledgeDocumentRepository.findAllByOrderByUploadedAtDesc().stream()
                        .filter(d -> d.getTitle() != null && d.getTitle().toLowerCase().contains(query.toLowerCase()))
                        .limit(limit)
                        .collect(Collectors.toList());
                List<Map<String, Object>> internalResults = new ArrayList<>();
                for (KnowledgeDocument doc : docs) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("title", doc.getTitle());
                    item.put("description", doc.getDescription() != null ? doc.getDescription() : "");
                    item.put("type", doc.getType() != null ? doc.getType() : "document");
                    item.put("source", "internal_knowledge");
                    item.put("status", doc.getStatus());
                    item.put("chunks", doc.getChunks());
                    internalResults.add(item);
                }
                allResults.addAll(internalResults);
                result.put("internalResults", internalResults);
            } catch (Exception e) {
                log.warn("内部知识库检索失败: {}", e.getMessage());
            }
        }

        // 2. 联网搜索
        if (searchWeb) {
            try {
                Map<String, Object> webResult = executeWebSearch(
                        Map.of("query", query, "numResults", limit));
                if (webResult.containsKey("results")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> webResults = (List<Map<String, Object>>) webResult.get("results");
                    webResults.forEach(r -> r.put("source", "web_search"));
                    allResults.addAll(webResults);
                    result.put("webResults", webResults);
                }
            } catch (Exception e) {
                log.warn("联网搜索失败: {}", e.getMessage());
            }
        }

        result.put("success", true);
        result.put("query", query);
        result.put("totalResults", allResults.size());
        result.put("results", allResults);
        result.put("message", String.format("全域检索完成，共找到 %d 条结果（内部:%d, 联网:%d）",
                allResults.size(),
                ((List<?>) result.getOrDefault("internalResults", List.of())).size(),
                ((List<?>) result.getOrDefault("webResults", List.of())).size()));

        return result;
    }

    /**
     * deep_document_analysis - 深度文献解析（合并版）
     * 组合文档摘要 + 知识点提取，一键生成精华摘要 + 结构化知识卡片
     */
    private Map<String, Object> executeDeepDocumentAnalysis(Map<String, Object> params, AiToolContext ctx) {
        String documentId = params.getOrDefault("documentId", "").toString().trim();
        String content = params.getOrDefault("content", "").toString().trim();
        String summaryLength = params.getOrDefault("summaryLength", "medium").toString().trim();
        int keywordCount = params.get("keywordCount") instanceof Number n ? n.intValue() : 8;
        boolean includeGlossary = params.getOrDefault("includeGlossary", "true").toString().equals("true");

        log.info("executeDeepDocumentAnalysis: documentId={}, contentLen={}, summaryLength={}, keywordCount={}, includeGlossary={}",
                documentId, content.length(), summaryLength, keywordCount, includeGlossary);

        Map<String, Object> result = new LinkedHashMap<>();

        // 调用文档摘要
        Map<String, Object> summaryParams = new HashMap<>();
        if (!documentId.isEmpty()) summaryParams.put("documentId", documentId);
        if (!content.isEmpty()) summaryParams.put("content", content);
        summaryParams.put("length", summaryLength);
        Map<String, Object> summaryResult = documentSummaryTool.execute(summaryParams, ctx);

        // 调用知识点提取
        Map<String, Object> extractParams = new HashMap<>();
        if (!content.isEmpty()) extractParams.put("text", content);
        extractParams.put("count", keywordCount);
        Map<String, Object> extractResult = knowledgeExtractionTool.execute(extractParams, ctx);

        // 组合结果
        result.put("success", true);
        result.put("summary", summaryResult.getOrDefault("summary", summaryResult.getOrDefault("content", "")));
        result.put("keywords", extractResult.getOrDefault("keywords", List.of()));
        result.put("knowledgePoints", extractResult.getOrDefault("knowledgePoints", extractResult.getOrDefault("points", List.of())));

        // 生成术语对照表（如果需要）
        if (includeGlossary) {
            List<Map<String, String>> glossary = new ArrayList<>();
            Object keywordsObj = extractResult.get("keywords");
            if (keywordsObj instanceof List<?> keywords) {
                for (Object k : keywords) {
                    String term = k instanceof Map ? (String) ((Map<?, ?>) k).get("keyword") : k.toString();
                    if (!term.isEmpty()) {
                        glossary.add(Map.of("zh", term, "en", term + " (待翻译)"));
                    }
                }
            }
            result.put("glossary", glossary);
        }

        result.put("message", "深度文献解析完成，已生成摘要、关键词和知识卡片");
        return result;
    }

    /**
     * smart_quiz_generation - 智能测评出题
     */
    private Map<String, Object> executeSmartQuizGeneration(Map<String, Object> params, AiToolContext ctx) {
        String topic = params.getOrDefault("topic", "").toString().trim();
        String questionType = params.getOrDefault("questionType", "mixed").toString().trim();
        int count = params.get("count") instanceof Number n ? n.intValue() : 5;
        String difficulty = params.getOrDefault("difficulty", "中等").toString().trim();

        log.info("executeSmartQuizGeneration: topic={}, questionType={}, count={}, difficulty={}",
                topic, questionType, count, difficulty);

        // 转换为旧工具参数格式
        Map<String, Object> quizParams = new HashMap<>();
        quizParams.put("topic", topic);
        quizParams.put("questionType", questionType);
        quizParams.put("count", count);
        quizParams.put("difficulty", difficulty);

        return quizGenerationTool.execute(quizParams, ctx);
    }

    /**
     * academic_translation - 学术翻译
     */
    private Map<String, Object> executeAcademicTranslation(Map<String, Object> params, AiToolContext ctx) {
        String text = params.getOrDefault("text", "").toString().trim();
        String sourceLang = params.getOrDefault("sourceLang", "auto").toString().trim();
        String targetLang = params.getOrDefault("targetLang", "中文").toString().trim();
        boolean preserveTechTerms = params.getOrDefault("preserveTechTerms", "true").toString().equals("true");

        log.info("executeAcademicTranslation: textLen={}, sourceLang={}, targetLang={}, preserveTechTerms={}",
                text.length(), sourceLang, targetLang, preserveTechTerms);

        // 转换为旧工具参数格式
        Map<String, Object> translateParams = new HashMap<>();
        translateParams.put("text", text);
        translateParams.put("sourceLang", sourceLang);
        translateParams.put("targetLang", targetLang);
        translateParams.put("preserveTechTerms", preserveTechTerms);

        return translationTool.execute(normalizeTranslateParams(translateParams), ctx);
    }

    /**
     * full_chain_learning - 全链路学习助手
     */
    private Map<String, Object> executeFullChainLearning(Map<String, Object> params, AiToolContext ctx) {
        String document = params.getOrDefault("document", "").toString().trim();
        String documentId = params.getOrDefault("documentId", "").toString().trim();
        String questionType = params.getOrDefault("questionType", "mixed").toString().trim();
        int quizCount = params.get("quizCount") instanceof Number n ? n.intValue() : 5;

        log.info("executeFullChainLearning: documentId={}, documentLen={}, questionType={}, quizCount={}",
                documentId, document.length(), questionType, quizCount);

        // 转换为旧工具参数格式
        Map<String, Object> assistantParams = new HashMap<>();
        if (!document.isEmpty()) assistantParams.put("document", document);
        if (!documentId.isEmpty()) assistantParams.put("documentId", documentId);
        assistantParams.put("questionType", questionType);
        assistantParams.put("quizCount", quizCount);

        return learningAssistantTool.execute(assistantParams, ctx);
    }

    /**
     * search_tools - 渐进式工具发现（Layer 1）：按关键词搜索可用工具，返回轻量级清单
     */
    private Map<String, Object> executeSearchTools(Map<String, Object> params) {
        String keyword = params.getOrDefault("keyword", "").toString().trim();
        if (keyword.isEmpty()) {
            return Map.of("success", false, "message", "搜索关键词不能为空");
        }
        List<Map<String, Object>> tools = ToolDefinitionRegistry.search(keyword).stream()
                .map(t -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", t.id());
                    item.put("name", t.name());
                    item.put("category", ToolDefinitionRegistry.categoryName(t.category()));
                    item.put("description", t.description());
                    return item;
                })
                .collect(Collectors.toList());
        log.info("executeSearchTools: keyword={}, found={}", keyword, tools.size());
        return Map.of(
                "success", true,
                "keyword", keyword,
                "tools", tools,
                "count", tools.size(),
                "message", tools.isEmpty() ? "未找到匹配的工具，可尝试其他关键词（如：摘要/翻译/出题/搜索）" : "工具搜索完成"
        );
    }

    /**
     * get_tool_detail - 渐进式工具发现（Layer 2）：获取指定工具的完整定义
     */
    private Map<String, Object> executeGetToolDetail(Map<String, Object> params) {
        String toolName = params.getOrDefault("toolName", "").toString().trim();
        if (toolName.isEmpty()) {
            return Map.of("success", false, "message", "工具名称不能为空");
        }
        Optional<ToolDefinition> defOpt = ToolDefinitionRegistry.byId(toolName)
                .or(() -> ToolDefinitionRegistry.findByNameOrAlias(toolName));
        if (defOpt.isEmpty()) {
            return Map.of("success", false, "message", "工具不存在: " + toolName + "，可先调用 search_tools 查找工具");
        }
        ToolDefinition def = defOpt.get();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", def.id());
        detail.put("name", def.name());
        detail.put("description", def.description());
        detail.put("category", ToolDefinitionRegistry.categoryName(def.category()));
        detail.put("usageHint", def.usageHint());
        detail.put("aliases", def.aliases());
        detail.put("params", def.params().stream().map(p -> Map.of(
                "name", p.name(),
                "type", p.type(),
                "required", p.required(),
                "default", p.defaultValue() == null ? "" : p.defaultValue(),
                "description", p.description()
        )).collect(Collectors.toList()));
        log.info("executeGetToolDetail: toolName={}", toolName);
        return Map.of("success", true, "tool", detail, "message", "工具详情获取成功");
    }

    /**
     * 兼容翻译工具旧参数：target_lang → targetLang
     */
    private Map<String, Object> normalizeTranslateParams(Map<String, Object> params) {
        if (params.containsKey("target_lang") && !params.containsKey("targetLang")) {
            Map<String, Object> normalized = new HashMap<>(params);
            normalized.put("targetLang", params.get("target_lang"));
            return normalized;
        }
        return params;
    }

    /**
     * 保存执行记录到数据库
     */
    private void saveExecutionRecord(String toolId, Map<String, Object> params,
                                      Map<String, Object> result, long executionTime, String userId) {
        try {
            ToolExecutionRecord record = ToolExecutionRecord.builder()
                    .toolId(toolId)
                    .toolName(getToolName(toolId))
                    .params(objectMapper.writeValueAsString(params))
                    .result(objectMapper.writeValueAsString(result))
                    .status(Boolean.TRUE.equals(result.get("success")) ? "success" : "error")
                    .executionTime(executionTime)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            recordRepository.save(record);
        } catch (Exception e) {
            log.error("保存执行记录失败: {}", e.getMessage());
        }
    }

    /**
     * 获取执行历史（分页，仅返回当前用户的执行记录；管理员返回全部记录）
     */
    public Map<String, Object> getExecutionHistory(int page, int size, String userId) {
        if (userId == null || userId.isBlank()) {
            userId = securityContextHolder.getCurrentUserId();
        }
        if (userId == null) {
            return Map.of(
                    "records", Collections.emptyList(),
                    "total", 0,
                    "page", page,
                    "size", size,
                    "totalPages", 0
            );
        }
        // 管理员全局视图：查看所有用户的执行记录
        List<ToolExecutionRecord> allRecords = securityContextHolder.isAdmin()
                ? recordRepository.findAllByOrderByCreatedAtDesc()
                : recordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int total = allRecords.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<ToolExecutionRecord> pageRecords = fromIndex < total ? allRecords.subList(fromIndex, toIndex) : Collections.emptyList();
        List<Map<String, Object>> historyList = pageRecords.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("toolId", r.getToolId());
            item.put("toolName", r.getToolName());
            item.put("status", r.getStatus());
            item.put("executionTime", r.getExecutionTime());
            item.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            try {
                if (r.getParams() != null) {
                    item.put("params", objectMapper.readValue(r.getParams(), Map.class));
                }
                if (r.getResult() != null) {
                    item.put("result", objectMapper.readValue(r.getResult(), Map.class));
                }
            } catch (Exception e) {
                item.put("params", Map.of());
                item.put("result", Map.of());
            }
            return item;
        }).collect(Collectors.toList());

        return Map.of(
                "records", historyList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
        );
    }

    private String getToolName(String toolId) {
        return switch (toolId) {
            case "web_search" -> "联网搜索";
            case "web_fetch" -> "网页抓取";
            case "summarize_document" -> "文档摘要";
            case "extract_keywords" -> "知识点提取";
            case "generate_quiz" -> "生成测验题";
            case "translate_text" -> "文本翻译";
            case "learning_assistant" -> "智能学习助手";
            case "search_tools" -> "工具搜索";
            case "get_tool_detail" -> "工具详情";
            default -> toolId;
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    // ============ 降级方案（当 AI API 不可用时返回模拟数据） ============

    /**
     * 获取工具级别的降级数据（外层兜底使用）
     */
    private Map<String, Object> getFallbackData(String toolId, Map<String, Object> params) {
        return switch (toolId) {
            case "summarize_document" -> {
                String content = params.getOrDefault("content", "").toString().trim();
                String length = params.getOrDefault("length", "中等").toString().trim();
                yield getFallbackSummarizeResult(content.isEmpty() ? "文档内容" : content, length);
            }
            case "extract_keywords" -> {
                String text = params.getOrDefault("text", "").toString().trim();
                int count = params.get("count") instanceof Number n ? n.intValue() : 10;
                yield getFallbackKeywordsResult(text.isEmpty() ? "学习内容" : text, count);
            }
            case "generate_quiz" -> {
                String topic = params.getOrDefault("topic", "").toString().trim();
                String difficulty = params.getOrDefault("difficulty", "中等").toString().trim();
                int count = params.get("count") instanceof Number n ? n.intValue() : 5;
                yield getFallbackQuizResult(topic.isEmpty() ? "编程基础" : topic, difficulty, count);
            }
            case "translate_text" -> {
                String text = params.getOrDefault("text", "").toString().trim();
                String targetLang = params.getOrDefault("target_lang", "中文").toString().trim();
                yield getFallbackTranslateResult(text.isEmpty() ? "Hello World" : text, targetLang);
            }
            default -> null;
        };
    }

    /**
     * 降级方案：生成模拟摘要
     */
    private Map<String, Object> getFallbackSummarizeResult(String content, String length) {
        String summary;
        switch (length) {
            case "简短" -> summary = "本文主要讨论了" + (content.length() > 20 ? content.substring(0, 20) : content) + "等核心概念，对相关内容进行了系统性的梳理和总结。";
            case "详细" -> summary = "本文详细阐述了" + (content.length() > 50 ? content.substring(0, 50) : content) + "等多个方面的内容。文章从基础概念入手，逐步深入探讨了相关理论和技术要点，并提供了实际应用场景的分析。整体结构清晰，内容丰富，对学习者具有较高的参考价值。";
            default -> summary = "本文围绕" + (content.length() > 30 ? content.substring(0, 30) : content) + "等主题展开讨论，系统梳理了相关的知识体系和学习路径，为学习者提供了清晰的指导。";
        }
        log.info("降级方案: 返回模拟摘要, length={}", length);
        return Map.of(
                "summary", summary,
                "length", length,
                "originalLength", content.length(),
                "message", "文档摘要生成成功（降级方案）",
                "fallback", true
        );
    }

    /**
     * 降级方案：生成模拟关键词
     */
    private Map<String, Object> getFallbackKeywordsResult(String text, int count) {
        String[] defaultKeywords = {
            "数据结构", "算法分析", "程序设计", "系统架构",
            "数据库原理", "网络协议", "操作系统", "软件工程",
            "面向对象", "设计模式", "分布式系统", "云计算"
        };
        List<Map<String, Object>> keywords = new ArrayList<>();
        int num = Math.min(count, defaultKeywords.length);
        for (int i = 0; i < num; i++) {
            keywords.add(Map.of(
                    "keyword", defaultKeywords[i],
                    "description", "关于" + defaultKeywords[i] + "的核心知识点，是学习过程中需要重点掌握的内容"
            ));
        }
        log.info("降级方案: 返回 {} 个模拟关键词", keywords.size());
        return Map.of(
                "keywords", keywords,
                "count", keywords.size(),
                "totalRequested", count,
                "message", "知识点提取成功（降级方案）",
                "fallback", true
        );
    }

    /**
     * 降级方案：生成模拟测验题
     */
    private Map<String, Object> getFallbackQuizResult(String topic, String difficulty, int count) {
        List<Map<String, Object>> questions = new ArrayList<>();
        int num = Math.min(count, 3);
        for (int i = 1; i <= num; i++) {
            questions.add(Map.of(
                    "question", "关于「" + topic + "」的第" + i + "题：以下哪个选项正确描述了相关概念？",
                    "options", List.of("选项A：这是正确的描述", "选项B：这是错误的描述", "选项C：部分正确但不够完整", "选项D：以上都不对"),
                    "answer", "选项A：这是正确的描述",
                    "explanation", "根据「" + topic + "」的核心概念，选项A准确地描述了相关原理，其他选项存在不同程度的偏差。"
            ));
        }
        log.info("降级方案: 返回 {} 道模拟测验题, topic={}, difficulty={}", questions.size(), topic, difficulty);
        return Map.of(
                "questions", questions,
                "topic", topic,
                "difficulty", difficulty,
                "count", questions.size(),
                "totalRequested", count,
                "message", "测验题生成成功（降级方案）",
                "fallback", true
        );
    }

    /**
     * 降级方案：返回模拟翻译
     */
    private Map<String, Object> getFallbackTranslateResult(String text, String targetLang) {
        String translation = "[降级翻译] " + text + " （当前AI翻译服务暂不可用，此为占位翻译结果）";
        log.info("降级方案: 返回模拟翻译, targetLang={}", targetLang);
        return Map.of(
                "translation", translation,
                "targetLang", targetLang,
                "originalLength", text.length(),
                "message", "翻译成功（降级方案）",
                "fallback", true
        );
    }

    // ===== 执行记录删除（用户隔离） =====

    /**
     * 删除单条工具执行记录（物理删除，仅限当前用户自己的记录）
     */
    @org.springframework.transaction.annotation.Transactional
    public boolean deleteRecord(Long id) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) return false;
        return recordRepository.deleteByIdAndUserId(id, userId) > 0;
    }

    /**
     * 清空当前用户的工具执行记录（物理删除）
     */
    @org.springframework.transaction.annotation.Transactional
    public int clearRecords() {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) return 0;
        return recordRepository.deleteByUserId(userId);
    }
}