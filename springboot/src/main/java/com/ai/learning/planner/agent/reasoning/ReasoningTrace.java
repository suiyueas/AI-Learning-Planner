package com.ai.learning.planner.agent.reasoning;

import java.util.UUID;

/**
 * 推理链路 Trace 上下文
 * 生成/透传 traceId，贯穿 规划 → 执行 → 评估 → 反思 全链路
 * 通过 MDC 注入日志，便于分布式排查
 */
public final class ReasoningTrace {

    /** MDC Key */
    public static final String MDC_TRACE_ID = "traceId";

    /** 线程级 traceId */
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ReasoningTrace() {
    }

    /**
     * 生成新的 traceId 并绑定到当前线程（同时写入 MDC）
     */
    public static String begin() {
        String traceId = "tr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        CURRENT.set(traceId);
        org.slf4j.MDC.put(MDC_TRACE_ID, traceId);
        return traceId;
    }

    /**
     * 绑定外部透传的 traceId（跨线程/跨服务时使用）
     */
    public static String bind(String traceId) {
        String resolved = (traceId == null || traceId.isBlank()) ? begin() : traceId;
        CURRENT.set(resolved);
        org.slf4j.MDC.put(MDC_TRACE_ID, resolved);
        return resolved;
    }

    /**
     * 获取当前 traceId（无则自动生成）
     */
    public static String current() {
        String traceId = CURRENT.get();
        if (traceId == null || traceId.isBlank()) {
            return begin();
        }
        return traceId;
    }

    /**
     * 清理当前线程的 traceId（链路结束时调用）
     */
    public static void clear() {
        CURRENT.remove();
        org.slf4j.MDC.remove(MDC_TRACE_ID);
    }

    /**
     * 执行完成后安全清理（配合 try-finally 使用）
     */
    public static void end() {
        clear();
    }
}
