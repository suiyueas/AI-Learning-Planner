package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录实体类
 * 对应数据库中的checkin_records表，记录用户每日的打卡信息
 */
@Entity
@Table(name = "checkin_records", indexes = {
    @Index(name = "idx_cr_user_date", columnList = "user_id, checkin_date")
})
@Data
public class CheckinRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}