package com.ai.learning.planner.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 启动早期加载项目根目录 .env 文件（KEY=VALUE 格式）为配置属性源
 *
 * <p>背景：Spring Boot 的 spring.config.import 按文件扩展名分派解析器
 * （仅支持 .properties/.yml/.yaml），不支持 .env 扩展名；第三方 spring-dotenv
 * 依赖在 Boot 3.x 中已失效（其 spring.factories 注册的 ApplicationContextInitializer
 * 不再被识别）。故通过官方扩展点 EnvironmentPostProcessor 实现等效能力。
 *
 * <p>查找顺序（均可通过系统属性 -Dapp.dotenv.location 覆盖）：
 * 1. ../.env  —— mvn/IDE 以 springboot/ 为工作目录时对应项目根目录
 * 2. ./.env   —— 工作目录为项目根目录时
 * 文件不存在时静默跳过（optional 语义），生产环境请使用系统环境变量。
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /** 可通过 -Dapp.dotenv.location=绝对路径 显式指定 .env 位置 */
    public static final String LOCATION_PROPERTY = "app.dotenv.location";

    private static final String[] CANDIDATE_PATHS = { "../.env", "./.env" };
    private static final String SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String location = System.getProperty(LOCATION_PROPERTY);
        if (location == null || location.isBlank()) {
            location = null;
            for (String path : CANDIDATE_PATHS) {
                if (new FileSystemResource(path).exists()) {
                    location = path;
                    break;
                }
            }
        }
        if (location == null) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try (InputStream in = new FileSystemResource(location).getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String line : content.split("\r?\n")) {
                String trimmed = line.trim();
                // 跳过空行与 # 注释
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (!key.isEmpty()) {
                    props.put(key, value);
                }
            }
        } catch (IOException e) {
            // 文件读取失败视为未配置，静默跳过（不阻断启动）
            return;
        }

        if (!props.isEmpty()) {
            // 置于最前，使 application.yml 的 ${VAR:} 占位符可解析；显式文件配置优先
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, props));
        }
    }
}
