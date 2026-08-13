package com.ai.learning.planner.config;

import com.ai.learning.planner.utils.JwtUtil;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"权限不足\",\"data\":null}");
                        })
                );

        return http.build();
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
