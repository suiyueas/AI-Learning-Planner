package com.ai.learning.planner.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务学习进度更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgressRequest {

    /** 学习耗时（分钟） */
    @PositiveOrZero(message = "学习时长不能为负数")
    private Integer timeSpent;

    /** 掌握度（0-1） */
    @PositiveOrZero(message = "掌握度不能为负数")
    private Float masteryLevel;
}
