package com.ai.learning.planner.interceptor;

import com.ai.learning.planner.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 幂等性拦截器
 * 检查请求是否重复，用于防止重复提交
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            // 使用认证 principal 作为用户维度（游客固定为 anonymous），不信任可伪造的 X-User-Id 头
            java.security.Principal principal = request.getUserPrincipal();
            String userId = (principal != null && !"anonymousUser".equals(principal.getName()))
                    ? principal.getName()
                    : "anonymous";

            String key = idempotencyService.generateKey(userId, request.getRequestURI(), idempotencyKey);

            if (idempotencyService.isDuplicate(key)) {
                log.warn("[Idempotency] 检测到重复请求: key={}, uri={}", key, request.getRequestURI());
                response.setStatus(409);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"重复请求，请稍后重试\",\"code\":409}");
                return false;
            }

            request.setAttribute("idempotencyKey", key);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String key = (String) request.getAttribute("idempotencyKey");
        if (key != null && response.getStatus() >= 400) {
            idempotencyService.remove(key);
        }
    }
}