package com.ai.learning.planner.mcp.client;

import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * MCP 调用重试策略
 * 网络超时/连接异常触发指数退避重试：初始 500ms、倍数 2、最大 3 次
 * 业务异常（参数错误等）不重试
 */
@Slf4j
public class McpRetryPolicy {

    /** 初始退避延迟（毫秒） */
    private final long initialDelayMs;

    /** 退避倍数 */
    private final double multiplier;

    /** 最大重试次数 */
    private final int maxAttempts;

    public McpRetryPolicy() {
        this(500, 2.0, 3);
    }

    public McpRetryPolicy(long initialDelayMs, double multiplier, int maxAttempts) {
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
        this.maxAttempts = maxAttempts;
    }

    /**
     * 计算第 attempt 次重试前的退避延迟（attempt 从 1 开始）
     */
    public long backoffDelayMs(int attempt) {
        return (long) (initialDelayMs * Math.pow(multiplier, attempt - 1));
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    /**
     * 判断异常是否可重试（网络/超时/IO 类异常）
     */
    public static boolean isRetryable(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof TimeoutException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof java.net.ConnectException
                    || cause instanceof java.net.UnknownHostException
                    || cause instanceof java.io.IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 执行带重试的调用
     *
     * @param attempt    当前尝试次数（0 开始）
     * @param executable 实际执行逻辑
     * @param toolName   工具名（日志用）
     * @return 执行结果
     */
    public <T> T executeWithRetry(String toolName, int attempt, Retryable<T> executable) throws Throwable {
        int current = Math.max(attempt, 0);
        while (true) {
            try {
                return executable.run();
            } catch (Throwable e) {
                if (!isRetryable(e) || current >= maxAttempts - 1) {
                    throw e;
                }
                long delay = backoffDelayMs(current + 1);
                log.warn("[McpRetry] 工具 {} 第 {} 次调用失败: {}，{}ms 后重试",
                        toolName, current + 1, e.getMessage(), delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断: " + toolName, ie);
                }
                current++;
            }
        }
    }

    /**
     * 可重试执行块（允许抛出受检异常，由 executeWithRetry 统一处理）
     */
    @FunctionalInterface
    public interface Retryable<T> {
        T run() throws Throwable;
    }
}
