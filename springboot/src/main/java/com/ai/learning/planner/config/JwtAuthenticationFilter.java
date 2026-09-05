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
        log.debug("JWT Filter: uri={}, hasToken={}", request.getRequestURI(), token != null);

        if (StringUtils.hasText(token)) {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);

                log.debug("JWT 认证成功: userId={}, username={}, uri={}", userId, username, request.getRequestURI());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, username, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将 JWT Claims 存入 request attribute，供 SecurityContextHolder.isAdmin() 读取 role
                try {
                    io.jsonwebtoken.Claims claims = jwtUtil.parseToken(token);
                    request.setAttribute("jwtClaims", claims);
                } catch (Exception e) {
                    log.debug("无法解析 JWT Claims: {}", e.getMessage());
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Token 脱敏：仅打印前 8 位 + 后 4 位，避免敏感信息泄露到日志
                String maskedToken = token.substring(0, Math.min(8, token.length())) + "..." + token.substring(Math.max(0, token.length() - 4));
                log.warn("JWT Token 验证失败: uri={}, token={}", request.getRequestURI(), maskedToken);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
                return;
            }
        } else {
            log.debug("JWT Filter: 未携带 Token: uri={}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头或查询参数中提取 JWT Token
     * 支持 SSE 连接（EventSource 不支持自定义请求头，通过 query param _token 传递）
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 优先从 Authorization Header 提取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // 2. 降级从查询参数提取（SSE EventSource 专用）
        String tokenParam = request.getParameter("_token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        return null;
    }
}