package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习路径操作请求（自动调整/重置共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathAdjustRequest {

    @NotBlank(message = "路径ID不能为空")
    private String pathId;
}
