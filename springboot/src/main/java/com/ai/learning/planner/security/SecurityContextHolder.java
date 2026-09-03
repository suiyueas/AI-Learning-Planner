package com.ai.learning.planner.security;

import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityContextHolder {

    /**
     * 安全上下文工具
     * 从 Spring Security 上下文提取当前用户 ID，并提供管理员判定：
     * 1. 优先从 JWT claims 中读取 role（无 DB 查询）
     * 2. 回退到 Redis 缓存（60s TTL）
     * 3. 最终回退到数据库查询
     */

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ADMIN_ROLE_CACHE_KEY = "cache:user:role:";
    private static final long ADMIN_ROLE_CACHE_TTL_SECONDS = 60;

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
     * 优先级：JWT claims → Redis 缓存 → 数据库
     */
    public boolean isAdmin() {
        String userId = getCurrentUserId();
        if (userId == null || userId.isBlank()) {
            return false;
        }

        // 1. 尝试从 JWT claims 中获取 role（零 DB 查询）
        try {
            jakarta.servlet.http.HttpServletRequest request =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof
                    org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest() : null;
            if (request != null) {
                Object claimsAttr = request.getAttribute("jwtClaims");
                if (claimsAttr instanceof io.jsonwebtoken.Claims claims) {
                    String role = claims.get("role", String.class);
                    if ("ADMIN".equals(role)) return true;
                    if ("USER".equals(role)) return false;
                }
            }
        } catch (Exception e) {
            log.debug("[SecurityContextHolder] 读取 JWT Claims 失败: {}", e.getMessage());
        }

        // 2. 尝试从 Redis 缓存获取
        try {
            String cachedRole = stringRedisTemplate.opsForValue().get(ADMIN_ROLE_CACHE_KEY + userId);
            if (cachedRole != null) {
                return "ADMIN".equals(cachedRole);
            }
        } catch (Exception e) {
            log.debug("[SecurityContextHolder] Redis 读取失败，回退到 DB: {}", e.getMessage());
        }

        // 3. 回退到数据库查询
        try {
            boolean isAdmin = userRepository.findById(Long.valueOf(userId))
                    .map(u -> "ADMIN".equals(u.getRole()))
                    .orElse(false);

            // 写入 Redis 缓存
            try {
                stringRedisTemplate.opsForValue().set(
                        ADMIN_ROLE_CACHE_KEY + userId,
                        isAdmin ? "ADMIN" : "USER",
                        ADMIN_ROLE_CACHE_TTL_SECONDS,
                        TimeUnit.SECONDS);
            } catch (Exception cacheEx) {
                log.debug("[SecurityContextHolder] Redis 写入失败: {}", cacheEx.getMessage());
            }

            return isAdmin;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 清除指定用户的管理员角色缓存（角色变更时调用）
     */
    public void evictAdminCache(String userId) {
        try {
            stringRedisTemplate.delete(ADMIN_ROLE_CACHE_KEY + userId);
        } catch (Exception e) {
            log.debug("[SecurityContextHolder] 缓存清除失败: {}", e.getMessage());
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}