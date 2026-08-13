package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.AssessmentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 测评记录仓储
 * 提供用户测评历史的分页查询、按用户删除与计数能力
 */
@Repository
public interface AssessmentRecordRepository extends JpaRepository<AssessmentRecord, Long> {
    Page<AssessmentRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<AssessmentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<AssessmentRecord> findByUserIdAndSubjectOrderByCreatedAtDesc(Long userId, String subject, Pageable pageable);
    Optional<AssessmentRecord> findByIdAndUserId(Long id, Long userId);
    List<AssessmentRecord> findByUserIdAndSubjectOrderByCreatedAtDesc(Long userId, String subject);
    List<AssessmentRecord> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT DISTINCT ar.subject FROM AssessmentRecord ar WHERE ar.userId = :userId ORDER BY ar.subject")
    List<String> findDistinctSubjectsByUserId(@Param("userId") Long userId);
}