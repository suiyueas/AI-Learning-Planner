package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 打卡记录仓储
 * 提供打卡去重校验、按月查询打卡日期、最近打卡与总打卡数统计，支撑连续打卡成就计算
 */
public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {
    boolean existsByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);
    List<CheckinRecord> findByUserIdAndCheckinDateBetween(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT c.checkinDate FROM CheckinRecord c WHERE c.userId = :userId AND YEAR(c.checkinDate) = :year AND MONTH(c.checkinDate) = :month")
    List<LocalDate> findCheckinDatesByUserIdAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    Optional<CheckinRecord> findTopByUserIdOrderByCheckinDateDesc(Long userId);

    long countByUserId(Long userId);

    /**
     * 获取用户所有打卡记录（按日期降序）
     * 用于成就系统计算打卡相关统计
     */
    List<CheckinRecord> findByUserIdOrderByCheckinDateDesc(Long userId);
}