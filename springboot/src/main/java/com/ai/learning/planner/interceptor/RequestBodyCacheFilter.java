package com.ai.learning.planner.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * 请求体缓存过滤器
 * 将 JSON 请求体缓存到 CachedBodyRequestWrapper，供 SecurityInterceptor 等读取，
 * 包装器支持重复读取，不会破坏后续 @RequestBody 的流解析。
 */
@Component
public class RequestBodyCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        // 仅缓存 JSON 请求体；multipart 文件上传等保持原样
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            filterChain.doFilter(new CachedBodyRequestWrapper(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
