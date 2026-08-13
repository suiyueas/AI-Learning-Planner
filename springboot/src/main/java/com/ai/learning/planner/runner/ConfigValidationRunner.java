package com.ai.learning.planner.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动配置验证Runner
 * 检查必要的环境变量和配置是否正确设置
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigValidationRunner implements ApplicationRunner {

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${spring.ai.qwen.api-key:}")
    private String qwenApiKey;

    @Override
    public void run(ApplicationArguments args) {
        List<String> warnings = new ArrayList<>();

        if (dbPassword == null || dbPassword.isBlank()) {
            warnings.add("MySQL密码未配置（使用空密码）");
        }

        if (redisPassword == null || redisPassword.isBlank()) {
            warnings.add("Redis密码未配置（使用无密码模式）");
        }

        boolean hasAiKey = (deepseekApiKey != null && !deepseekApiKey.isBlank())
                || (qwenApiKey != null && !qwenApiKey.isBlank());

        if (!hasAiKey) {
            warnings.add("所有AI API密钥均未配置，AI功能将不可用");
        } else {
            // 避免 emoji 在 Windows 控制台（GBK）下显示为 ?，使用纯文本状态并附带密钥长度便于核验
            log.info("✅ AI API密钥状态: DeepSeek={}({}字符), Qwen={}({}字符)",
                    deepseekApiKey != null && !deepseekApiKey.isBlank() ? "已配置" : "未配置",
                    deepseekApiKey != null ? deepseekApiKey.length() : 0,
                    qwenApiKey != null && !qwenApiKey.isBlank() ? "已配置" : "未配置",
                    qwenApiKey != null ? qwenApiKey.length() : 0);
        }

        if (warnings.isEmpty()) {
            log.info("✅ 所有必要配置验证通过");
        } else {
            log.warn("⚠️ 配置警告:");
            warnings.forEach(w -> log.warn("  - {}", w));
        }
    }
}