package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.LearningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 学习记录仓储
 * 记录用户在路径中的任务完成/进度数据，支撑进度计算、学习曲线聚合与时长/掌握度统计
 */
@Repository
public interface LearningRecordRepository extends JpaRepository<LearningRecord, Long> {
    List<LearningRecord> findByPathId(String pathId);
    List<LearningRecord> findByUserIdAndPathId(String userId, String pathId);
    Optional<LearningRecord> findByUserIdAndPathIdAndNodeId(String userId, String pathId, String nodeId);
    List<LearningRecord> findByUserIdAndPathIdAndNodeType(String userId, String pathId, String nodeType);
    List<LearningRecord> findByUserIdAndPathIdAndStatus(String userId, String pathId, String status);
    long countByUserIdAndPathIdAndStatus(String userId, String pathId, String status);

    List<LearningRecord> findByUserId(String userId);

    /** 用户已完成的学习记录（按完成时间倒序，供学习曲线/记录列表聚合） */
    List<LearningRecord> findByUserIdAndStatusAndCompletedAtIsNotNullOrderByCompletedAtDesc(String userId, String status);

    /** 用户某时间范围内的已完成学习记录 */
    List<LearningRecord> findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(
            String userId, String status, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndStatus(String userId, String status);

    @Query("SELECT COALESCE(SUM(l.timeSpent), 0) FROM LearningRecord l WHERE l.userId = :userId")
    Long sumTimeSpentByUserId(@Param("userId") String userId);

    @Query("SELECT AVG(l.masteryLevel) FROM LearningRecord l WHERE l.userId = :userId AND l.masteryLevel IS NOT NULL")
    Float avgMasteryLevelByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(DISTINCT l.pathId) FROM LearningRecord l WHERE l.userId = :userId")
    long countDistinctPathIdByUserId(@Param("userId") String userId);
}