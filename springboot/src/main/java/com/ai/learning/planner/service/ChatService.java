package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.ChatRequest;
import com.ai.learning.planner.dto.ChatResponse;
import com.ai.learning.planner.dto.CodeAnalysisRequest;
import com.ai.learning.planner.dto.CodeAnalysisResponse;
import com.ai.learning.planner.entity.ChatHistory;
import com.ai.learning.planner.mcp.ai.ToolDefinition;
import com.ai.learning.planner.mcp.ai.ToolDefinitionRegistry;
import com.ai.learning.planner.repository.ChatHistoryRepository;
import com.ai.learning.planner.security.OutputFilter;
import com.ai.learning.planner.security.PromptBoundaryMarker;
import com.ai.learning.planner.security.RagSecurityService;
import com.ai.learning.planner.security.SessionRiskTracker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天服务
 * 提供AI对话、流式对话、聊天历史管理等功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ModelManager modelManager;
    private final ChatHistoryRepository chatHistoryRepository;
    private final RedisChatMemoryService chatMemoryService;
    private final TavilySearchService tavilySearchService;
    private final KnowledgeService knowledgeService;
    private final KnowledgeStatusService knowledgeStatusService;
    private final ToolExecutionService toolExecutionService;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private final OutputFilter outputFilter;
    private final PromptBoundaryMarker promptBoundaryMarker;
    private final RagSecurityService ragSecurityService;
    private final SessionRiskTracker sessionRiskTracker;

    /**
     * 处理用户聊天请求
     */
    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        String chatContext = chatMemoryService.getChatContext(sessionId);
        ChatClient chatClient = buildChatClient(request.getModel());

        String systemPrompt = buildSystemPrompt(request.getRole(), chatContext, false, null,
                request.isUseKnowledge(), request.getMessage(), request.isUseTools());
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .call()
                .content();

        // 输出安全过滤
        String userId = request.getUserId() != null ? String.valueOf(request.getUserId()) : "anonymous";
        OutputFilter.FilteredResult filteredResult = outputFilter.filter(response, userId);
        if (filteredResult.wasFiltered()) {
            log.warn("[chat] 输出已过滤, userId={}, containedSystemPrompt={}, containedSensitiveData={}",
                    userId, filteredResult.containedSystemPrompt(), filteredResult.containedSensitiveData());
            response = filteredResult.content();
        }

        saveChatHistory(sessionId, request.getUserId(), request.getMessage(), "user", null);
        saveChatHistory(sessionId, request.getUserId(), response, "assistant", request.getAgentType());

        chatMemoryService.addToMemory(sessionId, "user", request.getMessage());
        chatMemoryService.addToMemory(sessionId, "assistant", response);

        // 保存执行日志到 execution_logs 和 execution_results
        try {
            String agentId = request.getAgentType() != null ? request.getAgentType() : "planner";
            String taskDesc = request.getMessage();
            agentService.saveLog(agentId, taskDesc, "chat", 1, "user: " + request.getMessage(), "success");
            agentService.saveLog(agentId, taskDesc, "response", 2, response, "success");
            agentService.saveResult(agentId, request.getRole() != null ? request.getRole() : "planner", taskDesc, "chat", response, "", null, "completed");
        } catch (Exception e) {
            log.warn("保存执行日志失败: {}", e.getMessage());
        }

        return ChatResponse.builder()
                .id(UUID.randomUUID().toString())
                .content(response)
                .role("assistant")
                .agentType(request.getAgentType())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public Flux<String> streamChat(ChatRequest request) {
        final String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        final Long userId = request.getUserId();

        log.info("[streamChat] sessionId={}, model={}, userId={}, webSearch={}, useKnowledge={}, useTools={}",
                sessionId, request.getModel(), userId, request.isWebSearch(),
                request.isUseKnowledge(), request.isUseTools());

        try {
            String chatContext = chatMemoryService.getChatContext(sessionId);
            ChatClient chatClient = buildChatClient(request.getModel());

            // ===== 1. 知识库检索（RAG） =====
            List<Map<String, Object>> knowledgeSources = new ArrayList<>();
            if (request.isUseKnowledge()) {
                try {
                    List<Document> similarDocs = knowledgeService.searchSimilar(request.getMessage(), 5);
                    for (int i = 0; i < similarDocs.size(); i++) {
                        Document doc = similarDocs.get(i);
                        Map<String, Object> source = new HashMap<>();
                        source.put("id", "src_" + i);
                        source.put("docName", doc.getMetadata() != null ?
                                doc.getMetadata().getOrDefault("docTitle",
                                        doc.getMetadata().getOrDefault("title",
                                                doc.getMetadata().getOrDefault("source", "未知文档"))) : "未知文档");
                        source.put("chunkIndex", doc.getMetadata() != null ?
                                doc.getMetadata().getOrDefault("chunkIndex", "") : "");
                        source.put("snippet", doc.getText() != null ?
                                doc.getText().substring(0, Math.min(doc.getText().length(), 150)) : "");
                        source.put("relevance", Math.round((1.0 - i * 0.08) * 100));
                        source.put("searchMode", doc.getMetadata() != null ?
                                doc.getMetadata().getOrDefault("searchMode", "vector") : "vector");
                        knowledgeSources.add(source);
                    }
                    log.info("[streamChat] 知识库检索完成, 找到 {} 个相关片段", knowledgeSources.size());
                } catch (Exception e) {
                    log.warn("[streamChat] 知识库检索失败: {}", e.getMessage());
                }
            }

            // 构建 system prompt（含 RAG 上下文和工具列表）
            String systemPrompt = buildSystemPrompt(request.getRole(), chatContext, request.isWebSearch(),
                    request.getMessage(), request.isUseKnowledge(), request.getMessage(), request.isUseTools());

            // 先保存用户消息
            saveChatHistory(sessionId, userId, request.getMessage(), "user", null);
            chatMemoryService.addToMemory(sessionId, "user", request.getMessage());

            final StringBuilder fullResponse = new StringBuilder();
            final List<Map<String, Object>> toolCallResults = new ArrayList<>();

            // ===== 3. 构建结构化 SSE 事件流 =====
            List<Flux<String>> eventFluxes = new ArrayList<>();

            // 3.1 知识库引用事件
            if (!knowledgeSources.isEmpty()) {
                Map<String, Object> refEvent = new HashMap<>();
                refEvent.put("type", "knowledgeRef");
                refEvent.put("sources", knowledgeSources);
                eventFluxes.add(Flux.just("data: " + toJson(refEvent) + "\n\n"));
            }

            // 3.2 工具状态事件（当前可用工具数）
            try {
                Map<String, Object> mcpStatusMap = new HashMap<>();
                mcpStatusMap.put("type", "mcpStatus");
                mcpStatusMap.put("mcpStatus", knowledgeStatusService.getStatus());
                // MCP 状态信息（可用工具数按前端可见列表统计，与 GET /tools/status 保持一致）
                Map<String, Object> mcpInfo = new HashMap<>();
                mcpInfo.put("availableCount", ToolDefinitionRegistry.visibleTools().size());
                // 传 null 时服务内部使用当前登录用户统计，避免固定 "anonymous" 导致统计恒为 0
                mcpInfo.put("totalCalls", toolExecutionService.getExecutionHistory(0, 1, null)
                        .get("total") instanceof Number n ? n.longValue() : 0);
                mcpStatusMap.put("toolStatus", mcpInfo);
                eventFluxes.add(Flux.just("data: " + toJson(mcpStatusMap) + "\n\n"));
            } catch (Exception e) {
                log.warn("[streamChat] 获取 MCP 状态失败: {}", e.getMessage());
            }

            // 3.3 AI 内容流
            Flux<String> contentStream = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getMessage())
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                    })
                    // 发送前对每个 chunk 进行输出安全过滤（doOnComplete 中的全量过滤仅保证落库内容，
                    // 此处确保客户端实际收到的流内容同样经过过滤）
                    .map(chunk -> {
                        String filterUserId = userId != null ? String.valueOf(userId) : "anonymous";
                        OutputFilter.FilteredResult filtered = outputFilter.filter(chunk, filterUserId);
                        if (filtered.wasFiltered()) {
                            log.warn("[streamChat] 输出已过滤, userId={}, containedSystemPrompt={}, containedSensitiveData={}",
                                    filterUserId, filtered.containedSystemPrompt(), filtered.containedSensitiveData());
                        }
                        return filtered.content();
                    })
                    .doOnComplete(() -> {
                        String completeResponse = fullResponse.toString();
                        log.debug("[streamChat] AI 流式输出完成, responseLength={}", completeResponse.length());

                        // 输出安全过滤
                        String filterUserId = userId != null ? String.valueOf(userId) : "anonymous";
                        OutputFilter.FilteredResult filteredResult = outputFilter.filter(completeResponse, filterUserId);
                        if (filteredResult.wasFiltered()) {
                            log.warn("[streamChat] 输出已过滤, userId={}, containedSystemPrompt={}, containedSensitiveData={}",
                                    filterUserId, filteredResult.containedSystemPrompt(), filteredResult.containedSensitiveData());
                            completeResponse = filteredResult.content();
                            fullResponse.setLength(0);
                            fullResponse.append(completeResponse);
                        }

                        // ===== 4. 工具调用解析与执行 =====
                        if (request.isUseTools()) {
                            List<Map<String, Object>> toolCalls = parseToolCalls(completeResponse);
                            for (Map<String, Object> tc : toolCalls) {
                                String toolName = (String) tc.get("name");
                                @SuppressWarnings("unchecked")
                                Map<String, Object> params = tc.get("params") instanceof Map ?
                                        (Map<String, Object>) tc.get("params") : new HashMap<>();
                                log.info("[streamChat] AI 请求调用工具: {}, params={}", toolName, params);
                                try {
                                    Map<String, Object> result = toolExecutionService.executeTool(
                                            toolName, params, userId != null ? String.valueOf(userId) : null);
                                    result.put("toolName", toolName);
                                    toolCallResults.add(result);
                                } catch (Exception e) {
                                    log.error("[streamChat] 工具执行失败: {}", e.getMessage());
                                    Map<String, Object> errResult = new HashMap<>();
                                    errResult.put("toolName", toolName);
                                    errResult.put("success", false);
                                    errResult.put("message", "执行失败: " + e.getMessage());
                                    toolCallResults.add(errResult);
                                }
                            }
                        }

                        // 保存 AI 回复
                        saveChatHistory(sessionId, userId, completeResponse, "assistant", request.getRole());
                        chatMemoryService.addToMemory(sessionId, "assistant", completeResponse);

                        // 保存执行日志到 execution_logs 和 execution_results
                        try {
                            String agentId = request.getRole() != null ? request.getRole() : "planner";
                            String taskDesc = request.getMessage();
                            agentService.saveLog(agentId, taskDesc, "chat", 1, "user: " + request.getMessage(), "success");
                            agentService.saveLog(agentId, taskDesc, "response", 2, completeResponse, "success");
                            agentService.saveResult(agentId, request.getRole() != null ? request.getRole() : "planner", taskDesc, "chat", completeResponse, "", null, "completed");
                        } catch (Exception e) {
                            log.warn("保存执行日志失败: {}", e.getMessage());
                        }
                    })
                    .doOnError(error -> log.error("[streamChat] 流式输出异常, sessionId={}", sessionId, error))
                    .onErrorResume(error -> {
                        log.error("[streamChat] AI调用失败, sessionId={}", sessionId, error);
                        String errorResponse = "抱歉，AI服务暂时不可用，请稍后重试。";
                        saveChatHistory(sessionId, userId, errorResponse, "assistant", request.getRole());
                        return Flux.just(errorResponse);
                    })
                    // 将内容块包装成 SSE JSON 格式
                    .map(chunk -> {
                        Map<String, Object> contentEvent = new HashMap<>();
                        contentEvent.put("type", "content");
                        contentEvent.put("content", chunk);
                        return "data: " + toJson(contentEvent) + "\n\n";
                    });

            eventFluxes.add(contentStream);

            // 3.4 工具调用结果事件（在内容流之后）
            // 注意：Flux.concat 顺序订阅，toolResultFlux 被订阅时 contentStream 已完成，
            // toolCallResults 已填充完毕，因此在订阅回调中直接发送结果与完成事件
            Flux<String> toolResultFlux = Flux.create(sink -> {
                if (!toolCallResults.isEmpty()) {
                    for (Map<String, Object> toolResult : toolCallResults) {
                        Map<String, Object> event = new HashMap<>();
                        event.put("type", "toolCall");
                        event.put("toolCall", toolResult);
                        sink.next("data: " + toJson(event) + "\n\n");
                    }
                }
                // 发送完成事件
                Map<String, Object> doneEvent = new HashMap<>();
                doneEvent.put("type", "done");
                sink.next("data: " + toJson(doneEvent) + "\n\n");
                sink.complete();
            });

            eventFluxes.add(toolResultFlux);

            // 合并所有事件流
            return Flux.concat(eventFluxes);

        } catch (Exception e) {
            log.error("[streamChat] 构建对话流失败, sessionId={}", sessionId, e);
            String errorResponse = "抱歉，处理请求时出现错误，请稍后重试。";
            saveChatHistory(sessionId, userId, errorResponse, "assistant", request.getRole());

            Map<String, Object> errorEvent = new HashMap<>();
            errorEvent.put("type", "content");
            errorEvent.put("content", errorResponse);
            return Flux.just("data: " + toJson(errorEvent) + "\n\n");
        }
    }

    private ChatClient buildChatClient(String modelKey) {
        if (modelKey != null && !modelKey.isEmpty()) {
            return modelManager.createChatClient(modelKey);
        }
        return modelManager.createChatClient();
    }

    /**
     * 构建系统提示词，整合角色定义、对话上下文、联网搜索、RAG 知识库和工具列表
     */
    private String buildSystemPrompt(String role, String chatContext, boolean webSearch, String webSearchQuery,
                                     boolean useKnowledge, String userMessage, boolean useTools) {
        String roleDefinition = switch (role != null ? role : "planner") {
            case "expert" -> "你是一位专业的学习答疑专家，擅长深入浅出地讲解各学科知识概念。";
            case "partner" -> "你是一位友善的学习伙伴，用轻松鼓励的语气陪伴用户学习，善于提问引导思考。";
            default -> "你是一位智能学习规划师，擅长根据用户的学习目标和当前水平，制定个性化、可执行的学习计划和路径。";
        };

        StringBuilder prompt = new StringBuilder(roleDefinition).append("\n\n");

        // 安全边界提醒
        prompt.append(promptBoundaryMarker.getBoundaryReminder()).append("\n\n");

        // 联网搜索上下文（外部不可信数据，加边界包裹防止其中的指令被模型执行）
        if (webSearch && webSearchQuery != null && !webSearchQuery.isBlank()) {
            try {
                String searchResults = tavilySearchService.search(webSearchQuery);
                prompt.append("【联网搜索结果】\n用户的问题已经通过互联网搜索获取了最新信息，请优先参考以下搜索结果来回答：\n\n")
                        .append(promptBoundaryMarker.wrapExternalContent(searchResults, "联网搜索结果"))
                        .append("\n\n");
            } catch (Exception e) {
                log.warn("[buildSystemPrompt] 联网搜索失败: {}", e.getMessage());
                prompt.append("【提示】用户希望联网搜索，但搜索服务暂时不可用，请基于你的知识尽力回答。\n\n");
            }
        }

        // RAG 知识库上下文（使用 RAG 安全检测）
        if (useKnowledge && userMessage != null && !userMessage.isBlank()) {
            try {
                List<Document> similarDocs = knowledgeService.searchSimilar(userMessage, 5);
                if (!similarDocs.isEmpty()) {
                    // 对检索结果进行安全检测
                    List<Document> safeDocs = ragSecurityService.sanitizeRetrievedDocuments(similarDocs);
                    if (!safeDocs.isEmpty()) {
                        prompt.append("【知识库参考信息】\n以下是与用户问题相关的知识库文档片段，请优先参考这些信息来回答：\n\n");
                        StringBuilder ragContent = new StringBuilder();
                        for (int i = 0; i < safeDocs.size(); i++) {
                            Document doc = safeDocs.get(i);
                            String source = doc.getMetadata() != null ?
                                    doc.getMetadata().getOrDefault("title", doc.getMetadata().getOrDefault("source", "未知来源")).toString() : "未知来源";
                            String text = doc.getText() != null ? doc.getText() : "";
                            ragContent.append("--- 片段 ").append(i + 1).append("（来源：").append(source).append("）---\n")
                                    .append(text).append("\n\n");
                        }
                        prompt.append(promptBoundaryMarker.wrapExternalContent(ragContent.toString(), "知识库文档片段"))
                                .append("\n\n");
                    }
                }
            } catch (Exception e) {
                log.warn("[buildSystemPrompt] 知识库检索失败: {}", e.getMessage());
            }
        }

        // 工具列表（由统一注册表生成，保证与后端实现一致；仅注入可见工具，隐藏/管理员专属工具不暴露给 LLM）
        if (useTools) {
            prompt.append("【可用工具列表】\n")
                    .append("你可以根据需要调用以下工具来辅助回答。如果需要调用工具，请在回答末尾添加工具调用标记。\n\n")
                    .append("工具列表（按分类）：\n");
            for (ToolDefinition def : ToolDefinitionRegistry.visibleTools()) {
                prompt.append("- ").append(def.id())
                        .append("（").append(ToolDefinitionRegistry.categoryName(def.category())).append("）：")
                        .append(def.description())
                        .append("。使用时机：").append(def.usageHint())
                        .append("。参数：");
                prompt.append(def.params().stream()
                        .map(p -> p.name() + (p.required() ? "(必填)" : "(可选)")
                                + (p.defaultValue() != null && !p.defaultValue().isBlank() ? "=" + p.defaultValue() : ""))
                        .collect(Collectors.joining(", ")));
                if (!def.aliases().isEmpty()) {
                    prompt.append("。别名：").append(String.join("/", def.aliases()));
                }
                prompt.append("\n");
            }
            prompt.append("\n工具调用规则：\n")
                    .append("1. 优先使用最小必要参数调用工具\n")
                    .append("2. 需要多个能力时优先使用 learning_assistant 完成学习闭环，或按 摘要→知识点→测验 顺序串联调用\n")
                    .append("3. 不确定有哪些工具时，可先调用 search_tools 查询\n")
                    .append("4. 需要在回答末尾添加如下格式的标记（不要省略）：\n")
                    .append("```tool_call\n{\"name\":\"generate_quiz\",\"params\":{\"topic\":\"Python基础\",\"count\":5}}\n```\n\n");
        }

        // 对话上下文（含用户历史输入，外部不可信数据，加边界包裹）
        if (chatContext != null && !chatContext.isBlank()) {
            prompt.append("【对话上下文】\n")
                    .append(promptBoundaryMarker.wrapExternalContent(chatContext, "对话历史"))
                    .append("\n\n");
        }

        // 使用边界标记包装用户输入部分
        String finalPrompt = promptBoundaryMarker.buildSystemPrompt(
                prompt.toString(),
                userMessage,
                null
        );

        // 格式要求
        finalPrompt += "【回答格式要求】\n"
                + "请使用 Markdown 格式组织回答，遵循以下规范：\n"
                + "1. 使用 ## 和 ### 标题划分内容层次\n"
                + "2. 使用有序列表（1. 2. 3.）或无无序列表（-）列举要点\n"
                + "3. 关键概念使用 **加粗** 强调\n"
                + "4. 适当使用 > 引用块突出重要提示\n"
                + "5. 分段清晰，每段聚焦一个主题\n"
                + "6. 回答要条理清晰、内容详实、易于阅读\n\n"
                + "请基于以上角色定位和参考信息，回答用户的问题。\n"
                + "【重要格式要求】请务必使用自然语言（纯文本+Markdown）回答，不要输出JSON格式的响应。";

        return finalPrompt;
    }

    /**
     * 从 AI 回复中解析工具调用标记
     */
    private List<Map<String, Object>> parseToolCalls(String response) {
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        if (response == null || response.isBlank()) return toolCalls;

        try {
            // 匹配 ```tool_call { "name": "...", "params": {...} } ``` 格式
            // 非贪婪匹配到最近的 }``` 序列，兼容 params 中含嵌套对象的 JSON
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "```tool_call\\s*(\\{[\\s\\S]*?\\})\\s*```", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                String jsonStr = matcher.group(1);
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> call = objectMapper.readValue(jsonStr, Map.class);
                    String name = call.getOrDefault("name", "").toString();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = call.get("params") instanceof Map ?
                            (Map<String, Object>) call.get("params") : new HashMap<>();
                    if (!name.isEmpty()) {
                        Map<String, Object> tc = new HashMap<>();
                        tc.put("name", name);
                        tc.put("params", params);
                        toolCalls.add(tc);
                    }
                } catch (Exception e) {
                    log.warn("[parseToolCalls] 解析工具调用 JSON 失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[parseToolCalls] 解析工具调用失败: {}", e.getMessage());
        }

        return toolCalls;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 获取指定会话的聊天历史（仅当前用户的记录；游客只能查看匿名会话）
     */
    public List<ChatHistory> getChatHistory(String sessionId, String userId) {
        if (userId == null) {
            return chatHistoryRepository.findBySessionIdAndUserIdIsNullOrderByCreatedAtAsc(sessionId);
        }
        return chatHistoryRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId);
    }

    public List<ChatHistory> getUserChatHistory(String userId) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void saveChatHistory(String sessionId, Long userId, String content, String role, String agentType) {
        try {
            ChatHistory history = ChatHistory.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .userId(userId != null ? String.valueOf(userId) : null)
                    .content(content)
                    .role(role)
                    .agentType(agentType)
                    .createdAt(LocalDateTime.now())
                    .build();
            chatHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("保存对话历史失败: sessionId={}, role={}", sessionId, role, e);
        }
    }

    public CodeAnalysisResponse analyzeCode(CodeAnalysisRequest request) {
        String code = request.getCode();
        String language = request.getLanguage() != null ? request.getLanguage() : detectLanguage(code);

        log.info("[analyzeCode] 分析代码: language={}, codeLength={}", language, code.length());

        String systemPrompt = buildCodeAnalysisSystemPrompt(language);
        ChatClient chatClient = buildChatClient(null);

        String analysisResult = chatClient.prompt()
                .system(systemPrompt)
                .user("请分析以下" + language + "代码：\n\n" + code)
                .call()
                .content();

        return parseCodeAnalysisResult(analysisResult, code, language);
    }

    private String buildCodeAnalysisSystemPrompt(String language) {
        return """
            你是一位专业的代码审计专家，擅长分析代码的性能、安全性和可维护性问题。
            请分析用户提供的代码，返回结构化的分析结果。

            输出格式要求（必须严格遵循）：
            1. 首先分析代码存在的问题
            2. 给出优化建议
            3. 计算时间/空间复杂度
            4. 如果可以，给出优化后的代码

            问题类型分类：
            - performance: 性能问题（如算法复杂度高、重复计算等）
            - security: 安全问题（如SQL注入、XSS等）
            - style: 代码风格问题
            - bug: 潜在的bug
            - maintainability: 可维护性问题

            严重程度：
            - critical: 严重
            - warning: 警告
            - info: 提示

            请用JSON格式返回，格式如下：
            ```json
            {
              "issues": [
                {"type": "performance", "message": "...", "line": 3, "severity": "warning"}
              ],
              "suggestions": ["建议1", "建议2"],
              "complexity": "O(n^2)",
              "summary": "总体评价"
            }
            ```
            如果优化代码可用，请在 optimizedCode 字段提供优化后的完整代码。

            注意：
            1. 只分析%s语言代码，请勿分析其他语言的代码
            2. issues 中的 line 字段如果无法确定可以省略或设为 null
            3. optimizedCode 只在确实有明确优化方案时才提供
            4. 用中文回复
            """.formatted(language);
    }

    private String detectLanguage(String code) {
        if (code == null || code.isBlank()) return "unknown";
        code = code.trim().toLowerCase();
        if (code.contains("def ") && code.contains(":")) return "python";
        if (code.contains("function ") || code.contains("const ") || code.contains("let ") || code.contains("=>")) return "javascript";
        if (code.contains("public class") || code.contains("public static void main")) return "java";
        if (code.contains("#include") || code.contains("int main")) return "cpp";
        if (code.contains("fn ") && code.contains("->")) return "rust";
        if (code.contains("func ") && code.contains("package ")) return "go";
        return "unknown";
    }

    private CodeAnalysisResponse parseCodeAnalysisResult(String analysisResult, String originalCode, String language) {
        List<CodeAnalysisResponse.CodeIssue> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        String complexity = "未知";
        String optimizedCode = null;
        String summary = "";

        try {
            int jsonStart = analysisResult.indexOf("```json");
            int jsonEnd = analysisResult.lastIndexOf("```");
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                String jsonStr = analysisResult.substring(jsonStart + 7, jsonEnd).trim();
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});

                if (parsed.containsKey("issues")) {
                    Object issuesObj = parsed.get("issues");
                    if (issuesObj instanceof List) {
                        for (Object issue : (List<?>) issuesObj) {
                            if (issue instanceof Map) {
                                Map<String, Object> issueMap = (Map<String, Object>) issue;
                                issues.add(CodeAnalysisResponse.CodeIssue.builder()
                                        .type(getStringValue(issueMap, "type"))
                                        .message(getStringValue(issueMap, "message"))
                                        .line(getIntegerValue(issueMap, "line"))
                                        .severity(getStringValue(issueMap, "severity"))
                                        .build());
                            }
                        }
                    }
                }

                if (parsed.containsKey("suggestions")) {
                    Object sugObj = parsed.get("suggestions");
                    if (sugObj instanceof List) {
                        for (Object s : (List<?>) sugObj) {
                            if (s != null) suggestions.add(s.toString());
                        }
                    }
                }

                if (parsed.containsKey("complexity")) {
                    complexity = parsed.get("complexity").toString();
                }

                if (parsed.containsKey("summary")) {
                    summary = parsed.get("summary").toString();
                }

                if (parsed.containsKey("optimizedCode") && parsed.get("optimizedCode") != null) {
                    optimizedCode = parsed.get("optimizedCode").toString();
                }
            } else {
                summary = analysisResult;
            }
        } catch (Exception e) {
            log.warn("[parseCodeAnalysisResult] 解析分析结果失败: {}", e.getMessage());
            summary = analysisResult;
        }

        if (issues.isEmpty() && summary.isEmpty()) {
            summary = "代码分析完成，未发现明显问题。";
        }

        return CodeAnalysisResponse.builder()
                .issues(issues)
                .suggestions(suggestions)
                .complexity(complexity)
                .optimizedCode(optimizedCode)
                .summary(summary)
                .build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}