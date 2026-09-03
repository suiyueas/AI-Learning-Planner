package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 积分流水表实体类
 * 记录每笔积分变动（签到收入、功能消耗、管理员手动调整）
 */
@Entity
@Table(name = "point_transactions", indexes = {
    @Index(name = "idx_pt_user_id", columnList = "user_id"),
    @Index(name = "idx_pt_created_at", columnList = "created_at"),
    @Index(name = "idx_pt_type", columnList = "transaction_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 交易类型：CHECKIN(签到), CHECKIN_BONUS(连续奖励), CONSUME(消耗), ADMIN_GRANT(管理员发放), ADMIN_REVOKE(管理员扣除) */
    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    /** 积分变动数量（正数增加，负数减少） */
    @Column(name = "points", nullable = false)
    private Long points;

    /** 变动前余额 */
    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    /** 变动后余额 */
    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    /** 来源说明（如：CHAT, AGENT, LEARNING_PATH, ADMIN操作等） */
    @Column(name = "source", length = 50)
    private String source;

    /** 关联的业务ID（如：签到记录ID、对话ID等） */
    @Column(name = "reference_id")
    private Long referenceId;

    /** 备注信息 */
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
