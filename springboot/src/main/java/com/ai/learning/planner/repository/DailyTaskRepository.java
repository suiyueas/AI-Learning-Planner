package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日任务仓储
 * 按路径/用户/日期维度查询每日学习任务，支持整周任务区间查询与按路径删除重建
 */
@Repository
public interface DailyTaskRepository extends JpaRepository<DailyTask, String> {
    List<DailyTask> findByPathIdAndTaskDateOrderBySortOrderAsc(String pathId, LocalDate taskDate);
    List<DailyTask> findByUserIdAndTaskDateOrderBySortOrderAsc(String userId, LocalDate taskDate);
    List<DailyTask> findByUserIdOrderByTaskDateDesc(String userId);
    List<DailyTask> findByPathIdAndTaskDateBetweenOrderByTaskDateAscSortOrderAsc(String pathId, LocalDate start, LocalDate end);
    List<DailyTask> findByUserIdAndTaskDateBetweenOrderByTaskDateAscSortOrderAsc(String userId, LocalDate start, LocalDate end);
    long countByPathIdAndTaskDateAndStatus(String pathId, LocalDate taskDate, String status);
    long countByUserIdAndTaskDateAndStatus(String userId, LocalDate taskDate, String status);
    void deleteByPathId(String pathId);

    @Query("SELECT DISTINCT t.title FROM DailyTask t WHERE t.pathId = :pathId")
    List<String> findDistinctTitlesByPathId(@Param("pathId") String pathId);
}