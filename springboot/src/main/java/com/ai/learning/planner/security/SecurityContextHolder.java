package com.ai.learning.planner.security;

import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityContextHolder {

    /**
     * 安全上下文工具
     * 从 Spring Security 上下文提取当前用户 ID，并提供管理员实时判定（每次查库，保证历史 Token 的角色变更即时生效）
     */

    private final UserRepository userRepository;

    public String getCurrentUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[SecurityContextHolder] 用户未认证");
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId().toString();
        }

        // JwtAuthenticationFilter 将 Long 类型的 userId 作为 principal
        if (principal instanceof Number number) {
            return number.toString();
        }

        if (principal instanceof String principalStr) {
            if (!"anonymousUser".equals(principalStr)) {
                return principalStr;
            }
        }

        log.warn("[SecurityContextHolder] 无法获取用户信息, principal={}", principal);
        return null;
    }

    public String getRequiredUserId() {
        String userId = getCurrentUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("用户未登录或无法获取用户信息");
        }
        return userId;
    }

    public boolean isAuthenticated() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * 判断当前用户是否为管理员（角色为 ADMIN）
     * 从数据库实时读取角色，保证历史 Token（无角色信息）也能正确判断
     */
    public boolean isAdmin() {
        String userId = getCurrentUserId();
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

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}