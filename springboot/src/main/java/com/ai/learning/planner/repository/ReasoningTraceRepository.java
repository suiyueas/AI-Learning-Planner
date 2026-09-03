package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.ReasoningTraceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 思考轨迹Repository
 */
@Repository
public interface ReasoningTraceRepository extends JpaRepository<ReasoningTraceEntity, String> {

    /**
     * 根据用户ID查询思考轨迹（按时间倒序）
     */
    List<ReasoningTraceEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据用户ID查询思考轨迹（按时间倒序，分页）
     */
    Page<ReasoningTraceEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 根据执行ID查询思考轨迹
     */
    Optional<ReasoningTraceEntity> findByExecutionIdAndIsDeletedFalse(String executionId);

    /**
     * 根据ID和未删除状态查询
     */
    Optional<ReasoningTraceEntity> findByIdAndIsDeletedFalse(String id);

    /**
     * 根据用户ID和Agent ID查询
     */
    List<ReasoningTraceEntity> findByUserIdAndAgentIdOrderByCreatedAtDesc(String userId, String agentId);
}