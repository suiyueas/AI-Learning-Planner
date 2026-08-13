package com.ai.learning.planner.config;

import com.ai.learning.planner.interceptor.IdempotencyInterceptor;
import com.ai.learning.planner.interceptor.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web 配置 - CORS 跨域 + 静态资源映射 + 拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.root:./uploads}")
    private String uploadRoot;

    @Autowired
    private IdempotencyInterceptor idempotencyInterceptor;

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 映射到 uploads 目录
        String absolutePath = new File(uploadRoot).getAbsolutePath();
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/actuator/**");

        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/chat/**", "/chat")
                .excludePathPatterns("/health", "/actuator/**");
    }
}