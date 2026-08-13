package com.ai.learning.planner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全防护配置属性（外部化）
 *
 * 对应 application.yml 中 security.* 配置段。
 * 所有安全组件阈值均通过本类读取，调整配置无需重新编译。
 *
 * @author AI Security Team
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /** 输入检测配置 */
    private Input input = new Input();

    /** 会话风险追踪配置 */
    private Session session = new Session();

    /** 熔断器配置 */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /** 审计日志配置 */
    private Audit audit = new Audit();

    @Data
    public static class Input {
        /** 输入最大长度（字符），超过该长度标记为中风险 */
        private int maxLength = 10000;
        /** 是否启用编码检测（Base64/hex/Unicode 转义等） */
        private boolean enableEncodingDetection = true;
        /** 是否启用 Unicode 规范化（同形字混淆检测） */
        private boolean enableUnicodeNormalization = true;
    }

    @Data
    public static class Session {
        /** 风险累积阈值，达到后会话标记为需要干预 */
        private double riskThreshold = 0.7;
        /** 高风险输入单次风险加分 */
        private double highRiskScore = 0.4;
        /** 中风险输入单次风险加分 */
        private double mediumRiskScore = 0.2;
        /** 每 N 轮对话自动重置风险分数，防止误伤正常长对话 */
        private int resetAfterTurns = 10;
        /** 会话最大轮次，超过建议重置会话 */
        private int maxTurns = 50;
        /** 会话无活动清理超时（秒） */
        private int sessionTimeout = 3600;
    }

    @Data
    public static class CircuitBreaker {
        /** 每分钟最大请求次数 */
        private int maxRequestsPerMinute = 100;
        /** 每分钟最大高风险输入次数 */
        private int maxHighRiskPerMinute = 5;
        /** 熔断器打开的失败次数阈值 */
        private int openThreshold = 10;
        /** 熔断器恢复时间（分钟） */
        private int resetMinutes = 5;
        /** 用户状态清理超时（秒） */
        private int cleanupAfterSeconds = 300;
    }

    @Data
    public static class Audit {
        /** 是否启用审计日志 */
        private boolean enabled = true;
        /** 审计日志级别（INFO/WARN） */
        private String logLevel = "INFO";
        /** 审计日志保留天数 */
        private int retentionDays = 180;
        /** 请求/响应内容最大记录长度（字符），超长截断 */
        private int maxContentLength = 1000;
    }
}
