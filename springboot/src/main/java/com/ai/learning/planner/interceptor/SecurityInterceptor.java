package com.ai.learning.planner.interceptor;

import com.ai.learning.planner.security.InputCircuitBreaker;
import com.ai.learning.planner.security.InputSanitizer;
import com.ai.learning.planner.security.SessionRiskTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 安全拦截器 - 统一安全检查入口
 *
 * 功能说明：
 * - 在请求到达Controller之前进行统一的安全检查
 * - 集成输入检测、熔断机制、会话追踪
 * - 对聊天接口（/chat/*）进行重点防护
 *
 * 检查流程：
 * 1. 检查路径是否豁免（监控端点、文档等）
 * 2. 检查是否为聊天接口
 * 3. 熔断器检查（频率限制、高风险限制）
 * 4. 输入过滤器检查（恶意指令检测）
 * 5. 会话风险追踪（多轮对话防护）
 *
 * 豁免路径：
 * - 监控端点：/actuator/health, /actuator/info, /actuator/prometheus
 * - API文档：/swagger-ui/**, /v3/api-docs/**
 * - 其他：/health, /favicon.ico
 *
 * 错误响应：
 * - 429: 请求过于频繁 / 会话异常
 * - 400: 输入包含不允许的内容
 *
 * @author AI Security Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityInterceptor implements HandlerInterceptor {

    /**
     * 豁免路径列表
     * 这些路径不需要进行安全检查
     */
    private static final List<String> EXCLUDED_PATHS = List.of(
            // 监控端点（运维监控需要）
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            // API文档（不需要认证）
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            // 其他静态资源
            "/health",
            "/favicon.ico"
    );

    /**
     * 输入安全过滤器
     */
    private final InputSanitizer inputSanitizer;

    /**
     * 输入熔断器
     */
    private final InputCircuitBreaker circuitBreaker;

    /**
     * 会话风险追踪器
     */
    private final SessionRiskTracker sessionRiskTracker;

    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 前置处理 - 在Controller执行前进行安全检查
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径（不含context-path）
        String path = getRequestPath(request);

        // ===== P2: 监控端点豁免 =====
        if (isExcludedPath(path)) {
            return true;
        }

        // 仅对聊天接口进行深度安全检查
        if (isChatEndpoint(path)) {
            String userId = getUserId(request);
            String sessionId = request.getHeader("X-Session-Id");
            String userInput = getUserInput(request);

            if (userInput != null && !userInput.isBlank()) {
                // 1. 熔断器检查
                if (circuitBreaker.check(userId, userInput).blocked()) {
                    log.warn("[SecurityInterceptor] 熔断器拦截, userId={}, path={}", userId, path);
                    sendErrorResponse(response, 429, "请求被拦截，请稍后重试");
                    return false;
                }

                // 2. 严重恶意指令拦截（HIGH级别）
                if (inputSanitizer.isBlocked(userInput)) {
                    log.warn("[SecurityInterceptor] 严重恶意指令已拦截, userId={}", userId);
                    sendErrorResponse(response, 400, "输入包含不允许的内容");
                    return false;
                }

                // 3. 中风险输入处理（标记但不拦截）
                InputSanitizer.SanitizeResult sanitizeResult = inputSanitizer.sanitize(userInput);
                if (sanitizeResult.modified()) {
                    log.warn("[SecurityInterceptor] 输入已清洗, userId={}, riskLevel={}, reasons={}",
                            userId, sanitizeResult.riskLevel(), sanitizeResult.detectedRiskTypes());
                } else if (sanitizeResult.riskLevel() == InputSanitizer.RiskLevel.MEDIUM) {
                    log.warn("[SecurityInterceptor] 输入需要关注, userId={}, reasons={}",
                            userId, sanitizeResult.detectedRiskTypes());
                }

                // 4. 会话风险追踪（多轮对话防护）
                if (sessionId != null) {
                    sessionRiskTracker.track(sessionId, userInput);
                    if (sessionRiskTracker.isEscalated(sessionId)) {
                        log.warn("[SecurityInterceptor] 会话风险升级, userId={}, sessionId={}", userId, sessionId);
                        sendErrorResponse(response, 429, "会话异常，请刷新页面后重试");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 检查是否为聊天接口
     */
    private boolean isChatEndpoint(String path) {
        return path.startsWith("/chat/") || path.equals("/chat");
    }

    /**
     * 获取不含context-path的请求路径
     * 例如：/api/chat -> /chat
     */
    private String getRequestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    /**
     * 检查路径是否豁免
     */
    private boolean isExcludedPath(String path) {
        if (path == null) {
            return false;
        }
        for (String excluded : EXCLUDED_PATHS) {
            if (excluded.endsWith("/**")) {
                // 通配符匹配
                String prefix = excluded.substring(0, excluded.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(excluded) || path.startsWith(excluded + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取用户标识（用于熔断/风控维度）
     * 优先级：认证 principal > 客户端 IP（游客），不再信任可伪造的 X-User-Id 请求头
     * - 已认证用户：使用真实 userId（不可伪造）
     * - 游客：使用 IP 维度，避免匿名用户共用一个 "anonymous" 桶互相影响
     */
    private String getUserId(HttpServletRequest request) {
        java.security.Principal principal = request.getUserPrincipal();
        if (principal != null && !"anonymousUser".equals(principal.getName())) {
            return principal.getName();
        }
        // 游客按 IP 维度限流（含 X-Forwarded-For 透传场景）
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * 从缓存请求体中提取用户输入
     * 使用RequestBodyCacheFilter预缓存的可重读请求体，避免破坏流
     */
    private String getUserInput(HttpServletRequest request) {
        if (request instanceof CachedBodyRequestWrapper) {
            CachedBodyRequestWrapper cachedRequest = (CachedBodyRequestWrapper) request;
            byte[] body = cachedRequest.getContentAsByteArray();
            if (body.length > 0) {
                try {
                    Map<String, Object> map = objectMapper.readValue(body, Map.class);
                    Object input = map.get("message");
                    if (input == null) {
                        input = map.get("input");
                    }
                    return input != null ? input.toString() : null;
                } catch (Exception e) {
                    log.debug("[SecurityInterceptor] 解析请求体失败: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", status, "message", message, "data", null)
        ));
    }
}