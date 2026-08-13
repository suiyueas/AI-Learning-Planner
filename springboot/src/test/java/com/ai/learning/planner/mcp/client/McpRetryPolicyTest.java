package com.ai.learning.planner.mcp.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 指数退避重试策略测试
 */
class McpRetryPolicyTest {

    @Test
    void backoffDelay_exponentialGrowth() {
        McpRetryPolicy policy = new McpRetryPolicy(500, 2.0, 3);
        assertEquals(500, policy.backoffDelayMs(1));
        assertEquals(1000, policy.backoffDelayMs(2));
        assertEquals(2000, policy.backoffDelayMs(3));
    }

    @Test
    void isRetryable_networkTimeoutsTrue() {
        assertTrue(McpRetryPolicy.isRetryable(new TimeoutException("timeout")));
        assertTrue(McpRetryPolicy.isRetryable(new SocketTimeoutException("socket")));
        assertTrue(McpRetryPolicy.isRetryable(new ConnectException("connect")));
        assertTrue(McpRetryPolicy.isRetryable(new IOException("io")));
        assertTrue(McpRetryPolicy.isRetryable(
                new RuntimeException("wrap", new TimeoutException("nested"))));
    }

    @Test
    void isRetryable_businessErrorsFalse() {
        assertFalse(McpRetryPolicy.isRetryable(new IllegalArgumentException("bad args")));
        assertFalse(McpRetryPolicy.isRetryable(new NullPointerException()));
    }

    @Test
    void executeWithRetry_succeedsAfterRetries() throws Throwable {
        McpRetryPolicy policy = new McpRetryPolicy(1, 2.0, 3); // 1ms 退避加速测试
        AtomicInteger calls = new AtomicInteger();

        String result = policy.executeWithRetry("search_resources", 0, () -> {
            if (calls.incrementAndGet() < 3) {
                throw new SocketTimeoutException("模拟网络超时");
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, calls.get(), "应重试 2 次后成功，共执行 3 次");
    }

    @Test
    void executeWithRetry_givesUpAfterMaxAttempts() {
        McpRetryPolicy policy = new McpRetryPolicy(1, 2.0, 3);
        AtomicInteger calls = new AtomicInteger();

        assertThrows(SocketTimeoutException.class, () ->
                policy.executeWithRetry("fail_tool", 0, () -> {
                    calls.incrementAndGet();
                    throw new SocketTimeoutException("持续超时");
                }));
        assertEquals(3, calls.get(), "达到最大尝试次数 3 次后放弃");
    }

    @Test
    void executeWithRetry_businessErrorNoRetry() {
        McpRetryPolicy policy = new McpRetryPolicy(1, 2.0, 3);
        AtomicInteger calls = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () ->
                policy.executeWithRetry("bad_tool", 0, () -> {
                    calls.incrementAndGet();
                    throw new IllegalArgumentException("参数错误");
                }));
        assertEquals(1, calls.get(), "业务异常不重试");
    }
}
