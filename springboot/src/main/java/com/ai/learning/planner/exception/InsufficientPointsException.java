package com.ai.learning.planner.exception;

import lombok.Getter;

/**
 * 积分不足异常
 * 当用户积分不足以执行某项操作时抛出，返回 HTTP 402 状态码
 */
@Getter
public class InsufficientPointsException extends RuntimeException {
    private final Long currentPoints;
    private final Long requiredPoints;
    private final String feature;

    public InsufficientPointsException(Long currentPoints, Long requiredPoints, String feature) {
        super("积分不足，无法使用" + feature);
        this.currentPoints = currentPoints;
        this.requiredPoints = requiredPoints;
        this.feature = feature;
    }
}
