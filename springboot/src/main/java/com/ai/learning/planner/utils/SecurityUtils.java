package com.ai.learning.planner.utils;

import com.ai.learning.planner.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;

/**
 * 安全上下文工具类
 * 统一从 Authentication 提取当前用户ID，未认证时抛出 401 异常（而非返回默认值）
 */
public final class SecurityUtils {

    private static final String ANONYMOUS_USER = "anonymousUser";

    private SecurityUtils() {
    }

    /**
     * 获取当前用户ID（字符串形式）
     *
     * @throws UnauthorizedException 未认证或 principal 无效时抛出（由全局异常处理器转 401）
     */
    public static String requireUserId(Authentication authentication) {
        Object principal = resolvePrincipal(authentication);
        return String.valueOf(principal);
    }

    /**
     * 获取当前用户ID（Long 形式）
     *
     * @throws UnauthorizedException 未认证或 principal 无效时抛出（由全局异常处理器转 401）
     */
    public static Long requireLongUserId(Authentication authentication) {
        return Long.valueOf(requireUserId(authentication));
    }

    private static Object resolvePrincipal(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("未登录或登录已过期，请重新登录");
        }
        Object principal = authentication.getPrincipal();
        // 匿名用户（未携带有效 Token 时 Spring Security 注入的占位主体）
        if (principal instanceof String s && (s.isBlank() || ANONYMOUS_USER.equals(s))) {
            throw new UnauthorizedException("未登录或登录已过期，请重新登录");
        }
        if (principal instanceof Long || principal instanceof Integer) {
            return principal;
        }
        throw new UnauthorizedException("无效的用户认证信息");
    }
}
