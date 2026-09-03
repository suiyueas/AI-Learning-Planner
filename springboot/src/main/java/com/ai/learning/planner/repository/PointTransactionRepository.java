package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分流水表 Repository
 */
@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<PointTransaction> findByUserId(Long userId, Pageable pageable);

    List<PointTransaction> findByUserIdAndTransactionType(Long userId, String transactionType);

    @Query("SELECT t FROM PointTransaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<PointTransaction> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.points), 0) FROM PointTransaction t WHERE t.userId = :userId AND t.transactionType = :type AND t.createdAt BETWEEN :start AND :end")
    Long sumPointsByUserAndTypeAndDateRange(@Param("userId") Long userId,
                                             @Param("type") String type,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
