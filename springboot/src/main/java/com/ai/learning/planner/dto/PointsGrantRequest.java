package com.ai.learning.planner.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 积分发放请求 DTO（管理员使用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsGrantRequest {
    /** 用户ID */
    private Long userId;
    /** 发放积分数量 */
    private Long points;
    /** 描述信息 */
    private String description;
}
