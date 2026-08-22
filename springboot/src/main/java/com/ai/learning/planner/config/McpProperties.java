package com.ai.learning.planner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 高级特性配置属性
 */
@Data
@ConfigurationProperties(prefix = "app.mcp")
public class McpProperties {

    /** 指数退避初始延迟（毫秒） */
    private long retryInitialDelayMs = 500;

    /** 退避倍数 */
    private double retryMultiplier = 2.0;

    /** 最大重试次数 */
    private int retryMaxAttempts = 3;

    /** 读超时（毫秒） */
    private long readTimeoutMs = 3000;

    /** 写超时（毫秒） */
    private long writeTimeoutMs = 8000;

    /** 人工审批默认等待超时（毫秒） */
    private long hitlDefaultTimeoutMs = 120000;
}
