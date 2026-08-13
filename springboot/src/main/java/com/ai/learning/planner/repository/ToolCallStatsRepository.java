package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.ToolCallStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 工具调用统计仓储
 * 按工具 ID 聚合的全局调用统计，支持会话级计数归零
 */
@Repository
public interface ToolCallStatsRepository extends JpaRepository<ToolCallStats, Long> {

    Optional<ToolCallStats> findByToolId(String toolId);

    @Modifying
    @Query("UPDATE ToolCallStats t SET t.sessionCalls = 0")
    void resetAllSessionCalls();
}
