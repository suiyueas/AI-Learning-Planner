package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePathRequest {
    @NotBlank(message = "学习目标不能为空")
    private String goal;

    /** 目标领域（如 Java、前端），用于生成针对性大纲 */
    private String targetField;

    /** 学习周期（月），与 durationWeeks 二选一 */
    private Integer duration;

    /** 学习周期（周），优先于 duration */
    private Integer durationWeeks;

    private String difficulty;
}