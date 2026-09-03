package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户积分总表实体类
 * 记录用户累计获得积分、可用积分、冻结积分
 */
@Entity
@Table(name = "user_points", indexes = {
    @Index(name = "idx_up_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPoints {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** 累计获得积分 */
    @Column(name = "total_earned", nullable = false)
    @Builder.Default
    private Long totalEarned = 0L;

    /** 可用积分 */
    @Column(name = "available_points", nullable = false)
    @Builder.Default
    private Long availablePoints = 0L;

    /** 冻结积分（用于处理中的消耗） */
    @Column(name = "frozen_points", nullable = false)
    @Builder.Default
    private Long frozenPoints = 0L;

    /** 版本号，用于乐观锁 */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
