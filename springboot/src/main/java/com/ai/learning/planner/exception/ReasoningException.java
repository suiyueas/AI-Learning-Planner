package com.ai.learning.planner.exception;

import com.ai.learning.planner.agent.reasoning.ReasoningTrace;
import lombok.Getter;

/**
 * 推理链路异常
 * 任何无法自动修复的错误必须向上抛出携带 traceId 和可读错误描述的 ReasoningException，
 * 严禁吞掉异常返回空对象。
 */
@Getter
public class ReasoningException extends RuntimeException {

    /** 推理链路 traceId（定位全链路日志） */
    private final String traceId;

    /** 异常发生的推理阶段（plan/execute/evaluate/reflect/replan/tool） */
    private final String stage;

    /** 业务错误码（MCP 契约错误范围 -32000 ~ -32099 兼容） */
    private final int errorCode;

    public ReasoningException(String stage, String message) {
        this(stage, -32000, message, null);
    }

    public ReasoningException(String stage, String message, Throwable cause) {
        this(stage, -32000, message, cause);
    }

    public ReasoningException(String stage, int errorCode, String message, Throwable cause) {
        super(formatMessage(message, errorCode), cause);
        this.traceId = ReasoningTrace.current();
        this.stage = stage;
        this.errorCode = errorCode;
    }

    private static String formatMessage(String message, int errorCode) {
        return "[" + ReasoningTrace.current() + "] (code=" + errorCode + ") " + (message == null ? "" : message);
    }
}
