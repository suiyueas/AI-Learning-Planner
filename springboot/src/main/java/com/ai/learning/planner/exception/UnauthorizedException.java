package com.ai.learning.planner.exception;

/**
 * 未认证/未登录异常
 * 由 GlobalExceptionHandler 统一转换为 401 响应
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
