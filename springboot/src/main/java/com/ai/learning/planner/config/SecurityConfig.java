package com.ai.learning.planner.config;

import com.ai.learning.planner.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Spring Security 配置
     * 无状态 JWT 认证：/auth、/chat、/public、/uploads、/actuator、/mcp 等公开路径 permitAll，
     * 其余接口需认证；认证/鉴权失败统一返回 JSON 401/403
     */

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF
                .csrf(csrf -> csrf.disable())

                // 启用 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 无状态会话
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ★ 核心修复：将 SecurityContext 存储到 Request Attribute（而非 Session）
                // 原因：SSE/Spring MVC 异步分发时，新线程无法从 ThreadLocal 获取 SecurityContext;
                //       Request Attribute 随 Request 对象传递，异步分发时仍可恢复认证信息。
                .securityContext(ctx -> ctx
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))

                // 配置请求权限
                .authorizeHttpRequests(auth -> auth
                        // 公开接口（无需认证）
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        // 对话接口（支持无登录对话，也可携带 Token 使用用户上下文）
                        .requestMatchers("/chat/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/home", "/error").permitAll()
                        // 静态资源
                        .requestMatchers("/uploads/**").permitAll()
                        // 健康检查
                        .requestMatchers("/actuator/**").permitAll()
                        // OpenAPI/Swagger 接口文档
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // MCP 健康检查
                        .requestMatchers("/mcp/**").permitAll()
                        // 推理链监控 + Agent 协作 API（开发/演示用）
                        .requestMatchers("/reasoning/**").permitAll()
                        .requestMatchers("/agents/collaborate/**").permitAll()
                        // 其他接口需要认证（含知识库、工具管理、用户数据等）
                        .anyRequest().authenticated()
                )

                // 添加 JWT 过滤器（直接实例化而非注册 Bean，避免 Filter 被容器二次注册导致重复执行）
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)

                // 统一认证/鉴权失败响应：未认证返回 401，已认证但无权限返回 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期，请重新登录\",\"data\":null}");
                        })
                        .accessDeniedHandler(sseAwareAccessDeniedHandler())
                );

        return http.build();
    }

    /**
     * SSE 容错的 AccessDeniedHandler：
     * 当响应已提交（SSE 流已开启）时，尝试发送 SSE 错误事件而非写 JSON 响应，
     * 避免 "Cannot render error page as the response has already been committed" 错误。
     */
    private AccessDeniedHandler sseAwareAccessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            if (response.isCommitted()) {
                // 响应已提交（SSE 流已开启），尝试通过已提交的流发送错误事件
                try {
                    response.getOutputStream().write(
                            ("event: error\ndata: {\"code\":403,\"message\":\"权限不足\"}\n\n")
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    response.getOutputStream().flush();
                } catch (IOException ignored) {
                    // 流已关闭，无法写入，忽略
                }
            } else {
                // 正常响应，返回 JSON 403
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                try {
                    response.getWriter().write("{\"code\":403,\"message\":\"权限不足\",\"data\":null}");
                } catch (IOException ignored) {
                }
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}