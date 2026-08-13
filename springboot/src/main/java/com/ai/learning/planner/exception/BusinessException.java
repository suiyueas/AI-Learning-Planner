package com.ai.learning.planner.exception;

import lombok.Getter;

/**
 * 业务异常
 * 携带可直接展示给用户的业务提示信息，由 GlobalExceptionHandler 转换为 400 响应
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }
}