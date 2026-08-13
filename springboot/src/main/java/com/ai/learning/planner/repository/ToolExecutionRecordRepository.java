package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.ToolExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工具执行记录仓储
 * 记录每次工具调用的明细（用户、工具、耗时），支持按用户/工具统计与用户隔离删除
 */
@Repository
public interface ToolExecutionRecordRepository extends JpaRepository<ToolExecutionRecord, Long> {

    List<ToolExecutionRecord> findByToolIdOrderByCreatedAtDesc(String toolId);

    long countByToolId(String toolId);

    List<ToolExecutionRecord> findAllByOrderByCreatedAtDesc();

    Page<ToolExecutionRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ToolExecutionRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndToolId(String userId, String toolId);

    @Query("SELECT t.toolId, COUNT(t) FROM ToolExecutionRecord t WHERE t.userId = :userId GROUP BY t.toolId")
    List<Object[]> countGroupByToolIdForUser(@Param("userId") String userId);

    @Query("SELECT t.toolId, COUNT(t) FROM ToolExecutionRecord t GROUP BY t.toolId")
    List<Object[]> countGroupByToolId();

    @Query("SELECT COUNT(t) FROM ToolExecutionRecord t WHERE t.userId = :userId")
    long countTotalByUserId(@Param("userId") String userId);

    // ===== 删除（用户隔离） =====

    @Modifying
    @Query("delete from ToolExecutionRecord t where t.id = :id and t.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    @Modifying
    @Query("delete from ToolExecutionRecord t where t.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}