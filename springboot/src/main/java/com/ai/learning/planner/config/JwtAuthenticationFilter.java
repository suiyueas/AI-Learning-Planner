package com.ai.learning.planner.config;

import com.ai.learning.planner.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT 认证过滤器
     * 从 Authorization: Bearer 头解析 Token，验证通过后以 userId 作为 principal 写入 SecurityContext；
     * Token 无效/过期时直接返回 401（不降级为匿名）；未携带 Token 的请求继续匿名放行
     */
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, username, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 认证成功: userId={}, username={}", userId, username);
            } else {
                // 携带了无效/过期 Token：直接返回 401，避免降级为匿名放行造成身份混淆
                log.warn("JWT Token 验证失败，即将过期或已过期");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 JWT Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}