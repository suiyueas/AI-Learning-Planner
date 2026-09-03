package com.ai.learning.planner.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 积分消耗请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsConsumeRequest {
    /** 用户ID */
    private Long userId;
    /** 消耗积分数量 */
    private Long points;
    /** 来源：CHAT, AGENT, LEARNING_PATH */
    private String source;
    /** 关联业务ID */
    private Long referenceId;
    /** 描述信息 */
    private String description;
}
