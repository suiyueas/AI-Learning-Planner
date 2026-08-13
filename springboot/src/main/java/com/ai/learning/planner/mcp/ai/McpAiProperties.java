package com.ai.learning.planner.mcp.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 赋能工具层配置（app.mcp.ai.*）
 *
 * <pre>
 * app:
 *   mcp:
 *     ai:
 *       enabled: true              # 是否启用 AI 工具层
 *       default-model: deepseek    # AI 工具使用的模型短名称
 *       timeout: 30000             # AI 调用超时（毫秒）
 *       fallback:
 *         summary-max-length: 500  # 降级摘要最大截取长度
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "app.mcp.ai")
public class McpAiProperties {

    /** 是否启用 AI 工具层（false 时 AI 工具直接走降级方案，不调用 LLM） */
    private boolean enabled = true;

    /** AI 工具默认模型短名称（deepseek / qwen / xiaomi），对应 ModelManager 的 Bean */
    private String defaultModel = "deepseek";

    /** AI 调用超时（毫秒），用于监控与日志展示 */
    private long timeout = 30000;

    /** 降级方案配置 */
    private final Fallback fallback = new Fallback();

    @Data
    public static class Fallback {
        /** 降级时摘要最大截取长度（字符） */
        private int summaryMaxLength = 500;
    }
}
