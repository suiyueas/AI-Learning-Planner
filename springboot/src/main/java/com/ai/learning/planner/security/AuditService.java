package com.ai.learning.planner.security;

import com.ai.learning.planner.config.SecurityProperties;
import com.ai.learning.planner.entity.AuditLog;
import com.ai.learning.planner.mcp.security.SensitiveDataMasker;
import com.ai.learning.planner.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 安全审计服务
 *
 * 提供统一的"谁在什么时间执行了什么操作"审计记录能力：
 * - 补充上下文信息（IP、UA、时间）
 * - 请求/响应内容脱敏 + 截断，防止敏感信息入库
 * - 结构化日志 + 异步入库（非阻塞，不影响主业务）
 *
 * @author AI Security Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    /**
     * 记录审计日志（核心方法）
     */
    public void log(AuditEntry entry) {
        if (entry == null || !securityProperties.getAudit().isEnabled()) {
            return;
        }

        try {
            int maxLen = securityProperties.getAudit().getMaxContentLength();
            entry.setId(UUID.randomUUID().toString());
            entry.setCreatedAt(LocalDateTime.now());
            entry.setIpAddress(getClientIp());
            entry.setUserAgent(getUserAgent());

            // 脱敏 + 截断，防止敏感信息入库
            entry.setRequest(truncate(mask(entry.getRequest()), maxLen));
            entry.setResponse(truncate(mask(entry.getResponse()), maxLen));
            entry.setErrorMessage(truncate(mask(entry.getErrorMessage()), maxLen));

            // 结构化日志（审计线索）
            log.info("AUDIT: userId={} action={} status={} resource={} duration={}ms ip={}",
                    entry.getUserId(), entry.getAction(), entry.getStatus(),
                    entry.getResourceId(), entry.getDurationMs(), entry.getIpAddress());

            // 异步入库（非阻塞，失败不影响主业务）
            CompletableFuture.runAsync(() -> {
                try {
                    auditLogRepository.save(toEntity(entry));
                } catch (Exception e) {
                    log.error("审计日志写入失败: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("审计日志记录异常: {}", e.getMessage());
        }
    }

    /**
     * 记录安全拦截（熔断器/输入过滤触发时）
     */
    public void logBlockedAttempt(String userId, String action, String reason, String input) {
        log(AuditEntry.builder()
                .userId(userId)
                .action(action)
                .status("BLOCKED")
                .request(input)
                .errorMessage(reason)
                .build());
    }

    /**
     * 记录聊天操作
     */
    public void logChat(String userId, String sessionId, String message, boolean success, long durationMs, String error) {
        log(AuditEntry.builder()
                .userId(userId)
                .action("CHAT")
                .resourceType("session")
                .resourceId(sessionId)
                .request(message)
                .status(success ? "SUCCESS" : "FAILURE")
                .durationMs((int) durationMs)
                .errorMessage(error)
                .build());
    }

    /**
     * 记录 Agent 执行
     */
    public void logAgentExecution(String userId, String agentId, String message, boolean success, long durationMs, String error) {
        log(AuditEntry.builder()
                .userId(userId)
                .action("AGENT_EXEC")
                .resourceType("agent")
                .resourceId(agentId)
                .request(message)
                .status(success ? "SUCCESS" : "FAILURE")
                .durationMs((int) durationMs)
                .errorMessage(error)
                .build());
    }

    /**
     * 记录工具调用
     */
    public void logToolCall(String userId, String toolName, Object params, Object result, boolean success, long durationMs) {
        log(AuditEntry.builder()
                .userId(userId)
                .action("TOOL_CALL")
                .resourceType("tool")
                .resourceId(toolName)
                .request(toJson(params))
                .response(toJson(result))
                .status(success ? "SUCCESS" : "FAILURE")
                .durationMs((int) durationMs)
                .build());
    }

    /**
     * 记录通用操作
     */
    public void logAction(String userId, String action, String resourceType, String resourceId,
                          Object request, Object response, String status, long durationMs, String error) {
        log(AuditEntry.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .request(toJson(request))
                .response(toJson(response))
                .status(status)
                .durationMs((int) durationMs)
                .errorMessage(error)
                .build());
    }

    // ==================== 内部工具方法 ====================

    /**
     * 敏感信息脱敏（Bearer 凭证、key/value 等）
     */
    private String mask(String text) {
        if (text == null) return null;
        return SensitiveDataMasker.mask(text);
    }

    /**
     * 超长内容截断，防止审计表膨胀
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 对象序列化为 JSON，失败返回空对象
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 获取客户端 IP（支持反向代理 X-Forwarded-For）
     */
    private String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取客户端 UA
     */
    private String getUserAgent() {
        HttpServletRequest request = getRequest();
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }

    /**
     * 从请求上下文获取当前 HttpServletRequest（非 Web 线程返回 null）
     */
    private HttpServletRequest getRequest() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                return attrs.getRequest();
            }
        } catch (Exception ignored) {
            // 非 Web 上下文（如定时任务）直接返回 null
        }
        return null;
    }

    /**
     * 实体转换
     */
    private AuditLog toEntity(AuditEntry entry) {
        return AuditLog.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .action(entry.getAction())
                .resourceType(entry.getResourceType())
                .resourceId(entry.getResourceId())
                .request(entry.getRequest())
                .response(entry.getResponse())
                .status(entry.getStatus())
                .ipAddress(entry.getIpAddress())
                .userAgent(entry.getUserAgent())
                .durationMs(entry.getDurationMs())
                .errorMessage(entry.getErrorMessage())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    /**
     * 审计条目（内存传输对象）
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class AuditEntry {
        private String id;
        private String userId;
        private String action;
        private String resourceType;
        private String resourceId;
        private String request;
        private String response;
        private String status;
        private String ipAddress;
        private String userAgent;
        private Integer durationMs;
        private String errorMessage;
        private LocalDateTime createdAt;
    }
}
