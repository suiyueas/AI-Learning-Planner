package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.dto.ChatRequest;
import com.ai.learning.planner.dto.ChatResponse;
import com.ai.learning.planner.dto.CodeAnalysisRequest;
import com.ai.learning.planner.dto.CodeAnalysisResponse;
import com.ai.learning.planner.entity.ChatHistory;
import org.springframework.security.access.AccessDeniedException;
import com.ai.learning.planner.security.AuditService;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import com.ai.learning.planner.utils.SecurityUtils;

/**
 * 聊天控制器
 * 提供AI对话接口，包含普通对话、流式对话和聊天历史查询
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final AuditService auditService;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 处理普通对话请求
     */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        log.info("收到对话请求: {}", request.getMessage());
        fillUserId(request, authentication);
        long start = System.currentTimeMillis();
        try {
            ChatResponse response = chatService.chat(request);
            auditService.logChat(String.valueOf(request.getUserId()), request.getSessionId(),
                    request.getMessage(), true, System.currentTimeMillis() - start, null);
            return response;
        } catch (Exception e) {
            auditService.logChat(String.valueOf(request.getUserId()), request.getSessionId(),
                    request.getMessage(), false, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    /**
     * 处理流式对话请求
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        log.info("收到流式对话请求: {}", request.getMessage());
        fillUserId(request, authentication);
        auditService.logChat(String.valueOf(request.getUserId()), request.getSessionId(),
                request.getMessage(), true, 0, null);
        return chatService.streamChat(request);
    }

    /**
     * 获取指定会话的聊天历史
     * 仅能查询当前用户自己的会话记录（游客仅能查询匿名会话）
     */
    @GetMapping("/history/{sessionId}")
    public List<ChatHistory> getChatHistory(@PathVariable String sessionId, Authentication authentication) {
        return chatService.getChatHistory(sessionId, resolveCurrentUserId(authentication));
    }

    /**
     * 获取指定用户的聊天历史（仅本人可查，管理员可查任意用户）
     */
    @GetMapping("/history/user/{userId}")
    public List<ChatHistory> getUserChatHistory(@PathVariable String userId, Authentication authentication) {
        String currentUserId = SecurityUtils.requireUserId(authentication);
        if (!currentUserId.equals(userId) && !securityContextHolder.isAdmin()) {
            throw new AccessDeniedException("无权查看该用户的聊天历史");
        }
        return chatService.getUserChatHistory(userId);
    }

    /**
     * 删除指定会话的聊天历史（仅本人可删，管理员可删任意用户）
     */
    @DeleteMapping("/history/{sessionId}")
    public ApiResponse<Void> deleteChatHistory(@PathVariable String sessionId, Authentication authentication) {
        String currentUserId = SecurityUtils.requireUserId(authentication);
        chatService.deleteChatHistory(sessionId, currentUserId);
        return ApiResponse.success("会话已删除", null);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "chat");
    }

    /**
     * 从认证上下文中提取 userId 填充到请求中
     * - 已认证：强制使用认证身份，不信任请求体中的 userId（防止伪造归属）
     * - 游客：强制置 null，避免匿名对话被挂到他人名下
     */
    private void fillUserId(ChatRequest request, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            request.setUserId(userId);
        } else {
            request.setUserId(null);
        }
    }

    /**
     * 解析当前用户ID（未认证返回 null，表示游客）
     */
    private String resolveCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return String.valueOf(userId);
        }
        return null;
    }

    /**
     * 代码分析接口
     */
    @PostMapping("/code-analyze")
    public ApiResponse<CodeAnalysisResponse> analyzeCode(@Valid @RequestBody CodeAnalysisRequest request) {
        log.info("收到代码分析请求: language={}", request.getLanguage());
        CodeAnalysisResponse response = chatService.analyzeCode(request);
        return ApiResponse.success(response);
    }
}