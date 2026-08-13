package com.ai.learning.planner.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习路径更新请求（避免 Controller 直接接收 Entity）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathUpdateRequest {

    @Size(max = 200, message = "路径名称不能超过200字符")
    private String name;

    private String description;

    private Integer version;

    private Boolean isActive;

    private Float completionPercentage;

    /** 节点列表（JSON），不传则不修改 */
    private String nodes;
}
