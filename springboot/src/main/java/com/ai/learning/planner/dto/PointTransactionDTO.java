package com.ai.learning.planner.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 积分流水 DTO
 * 返回积分变动记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionDTO {
    /** 交易ID */
    private Long id;
    /** 交易类型 */
    private String transactionType;
    /** 积分变动数量 */
    private Long points;
    /** 变动前余额 */
    private Long balanceBefore;
    /** 变动后余额 */
    private Long balanceAfter;
    /** 来源 */
    private String source;
    /** 关联业务ID */
    private Long referenceId;
    /** 描述 */
    private String description;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
