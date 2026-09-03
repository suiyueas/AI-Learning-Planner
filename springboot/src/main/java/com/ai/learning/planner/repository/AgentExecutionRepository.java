package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.AgentExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Agent 执行日志/结果仓储
 * 提供执行日志（LOG）与执行结果（RESULT）两类记录的查询、删除接口，
 * 所有操作均支持按 userId 隔离（管理员可查全量）
 */
@Repository
public interface AgentExecutionRepository extends JpaRepository<AgentExecution, String> {

    List<AgentExecution> findByUserIdOrderByCreatedAtDesc(String userId);

    List<AgentExecution> findByAgentIdOrderByCreatedAtDesc(String agentId);

    List<AgentExecution> findBySessionIdOrderByStepOrderAsc(String sessionId);

    List<AgentExecution> findByExecutionIdOrderByStepOrderAsc(String executionId);

    List<AgentExecution> findByRecordTypeOrderByCreatedAtDesc(String recordType);

    Optional<AgentExecution> findTopByAgentIdAndRecordTypeOrderByCreatedAtDesc(String agentId, String recordType);

    List<AgentExecution> findByAgentIdAndRecordTypeOrderByCreatedAtDesc(String agentId, String recordType);

    List<AgentExecution> findByResultTypeOrderByCreatedAtDesc(String resultType);

    void deleteByUserId(String userId);

    // ===== 用户隔离方法（数据安全） =====

    List<AgentExecution> findByUserIdAndAgentIdOrderByCreatedAtDesc(String userId, String agentId);

    List<AgentExecution> findByUserIdAndRecordTypeOrderByCreatedAtDesc(String userId, String recordType);

    List<AgentExecution> findByUserIdAndAgentIdAndRecordTypeOrderByCreatedAtDesc(String userId, String agentId, String recordType);

    List<AgentExecution> findByUserIdAndSessionIdOrderByStepOrderAsc(String userId, String sessionId);

    List<AgentExecution> findByUserIdAndExecutionIdOrderByStepOrderAsc(String userId, String executionId);

    List<AgentExecution> findByUserIdAndResultTypeOrderByCreatedAtDesc(String userId, String resultType);

    Optional<AgentExecution> findTopByUserIdAndAgentIdAndRecordTypeOrderByCreatedAtDesc(String userId, String agentId, String recordType);

    @Query("select a from AgentExecution a where a.userId = :userId and a.recordType = :recordType and coalesce(a.isDeleted, false) = false order by a.createdAt desc")
    List<AgentExecution> findActiveByUserIdAndRecordType(@Param("userId") String userId, @Param("recordType") String recordType);

    @Query("select a from AgentExecution a where a.userId = :userId and a.agentId = :agentId and a.recordType = :recordType and coalesce(a.isDeleted, false) = false order by a.createdAt desc")
    List<AgentExecution> findActiveByUserIdAndAgentIdAndRecordType(@Param("userId") String userId, @Param("agentId") String agentId, @Param("recordType") String recordType);

    @Query("select a from AgentExecution a where a.userId = :userId and a.sessionId = :sessionId and coalesce(a.isDeleted, false) = false order by a.stepOrder asc")
    List<AgentExecution> findActiveByUserIdAndSessionId(@Param("userId") String userId, @Param("sessionId") String sessionId);

    @Query("select a from AgentExecution a where a.userId = :userId and a.executionId = :executionId and coalesce(a.isDeleted, false) = false order by a.stepOrder asc")
    List<AgentExecution> findActiveByUserIdAndExecutionId(@Param("userId") String userId, @Param("executionId") String executionId);

    // ===== 软删除支持：仅返回未删除（is_deleted 为 false 或 null 兼容历史数据）的记录 =====

    @Query("select a from AgentExecution a where a.recordType = :recordType and coalesce(a.isDeleted, false) = false order by a.createdAt desc")
    List<AgentExecution> findActiveByRecordType(@Param("recordType") String recordType);

    @Query("select a from AgentExecution a where a.agentId = :agentId and a.recordType = :recordType and coalesce(a.isDeleted, false) = false order by a.createdAt desc")
    List<AgentExecution> findActiveByAgentIdAndRecordType(@Param("agentId") String agentId, @Param("recordType") String recordType);

    @Query("select a from AgentExecution a where a.agentId = :agentId and a.recordType = :recordType and coalesce(a.isDeleted, false) = false order by a.createdAt desc")
    List<AgentExecution> findLatestActiveByAgentIdAndRecordType(@Param("agentId") String agentId, @Param("recordType") String recordType);

    @Query("select a from AgentExecution a where a.id = :id and coalesce(a.isDeleted, false) = false")
    Optional<AgentExecution> findActiveById(@Param("id") String id);

    @Query("select a from AgentExecution a where a.id = :id and a.userId = :userId and coalesce(a.isDeleted, false) = false")
    Optional<AgentExecution> findActiveByIdAndUserId(@Param("id") String id, @Param("userId") String userId);

    // ===== 软删除操作（更新 is_deleted 标记，不物理删除） =====

    @Modifying
    @Query("update AgentExecution a set a.isDeleted = true where a.id = :id")
    int softDeleteById(@Param("id") String id);

    @Modifying
    @Query("update AgentExecution a set a.isDeleted = true where a.id = :id and a.userId = :userId")
    int softDeleteByIdAndUserId(@Param("id") String id, @Param("userId") String userId);

    @Modifying
    @Query("update AgentExecution a set a.isDeleted = true where a.id in :ids")
    int softDeleteByIds(@Param("ids") Collection<String> ids);

    @Modifying
    @Query("update AgentExecution a set a.isDeleted = true where a.id in :ids and a.userId = :userId")
    int softDeleteByIdsAndUserId(@Param("ids") Collection<String> ids, @Param("userId") String userId);

    @Modifying
    @Query("update AgentExecution a set a.isDeleted = true where a.recordType = :recordType")
    int softDeleteByRecordType(@Param("recordType") String recordType);

    /** 将记录归属到当前用户并软删除（兼容 userId 不匹配的历史数据） */
    @Modifying
    @Query("update AgentExecution a set a.userId = :userId, a.isDeleted = true where a.id = :id")
    int updateUserIdAndSoftDelete(@Param("id") String id, @Param("userId") String userId);

    /** 批量将记录归属到当前用户并软删除 */
    @Modifying
    @Query("update AgentExecution a set a.userId = :userId, a.isDeleted = true where a.id in :ids")
    int updateUserIdsAndSoftDelete(@Param("ids") Collection<String> ids, @Param("userId") String userId);

    // ===== 回收站与硬删除（物理删除） =====

    @Query("select a from AgentExecution a where a.userId = :userId and a.recordType = :recordType and coalesce(a.isDeleted, false) = true order by a.createdAt desc")
    List<AgentExecution> findDeletedByUserIdAndRecordType(@Param("userId") String userId, @Param("recordType") String recordType);

    @Modifying
    @Query("update AgentExecution a set a.isDeleted = false where a.id = :id and a.userId = :userId")
    int restoreByIdAndUserId(@Param("id") String id, @Param("userId") String userId);

    @Modifying
    @Query("update AgentExecution a set a.isDeleted = false where a.id = :id")
    int restoreById(@Param("id") String id);

    @Modifying
    @Query("delete from AgentExecution a where a.id = :id and a.userId = :userId")
    int hardDeleteByIdAndUserId(@Param("id") String id, @Param("userId") String userId);

    @Modifying
    @Query("delete from AgentExecution a where a.id = :id")
    int hardDeleteById(@Param("id") String id);

    @Modifying
    @Query("delete from AgentExecution a where a.id in :ids and a.userId = :userId")
    int hardDeleteByIdsAndUserId(@Param("ids") Collection<String> ids, @Param("userId") String userId);

    @Modifying
    @Query("delete from AgentExecution a where a.id in :ids")
    int hardDeleteByIds(@Param("ids") Collection<String> ids);

    @Query("select count(a) from AgentExecution a where a.userId = :userId and a.recordType = :recordType and coalesce(a.isDeleted, false) = false")
    long countActiveByUserIdAndRecordType(@Param("userId") String userId, @Param("recordType") String recordType);

    // ===== 分页查询（避免全量加载） =====

    @Query("select a from AgentExecution a where a.userId = :userId and a.recordType = :recordType and coalesce(a.isDeleted, false) = false order by a.createdAt desc")
    Page<AgentExecution> findActiveByUserIdAndRecordTypePaged(@Param("userId") String userId, @Param("recordType") String recordType, Pageable pageable);

    @Query("select a from AgentExecution a where a.userId = :userId and coalesce(a.isDeleted, false) = false order by a.createdAt desc")
    Page<AgentExecution> findActiveByUserIdPaged(@Param("userId") String userId, Pageable pageable);

    // ===== 归档：查询超过指定时间的软删除记录 =====

    @Query("select a from AgentExecution a where a.isDeleted = true and a.createdAt < :cutoffDate")
    List<AgentExecution> findArchivableRecords(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    @Modifying
    @Query("delete from AgentExecution a where a.isDeleted = true and a.createdAt < :cutoffDate")
    int hardDeleteArchivedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}