package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.LearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学习事件仓储（成就系统数据源）
 * 记录打卡、工具使用、成就解锁等事件，支撑成就条件判定与历史查询
 */
@Repository
public interface LearningEventRepository extends JpaRepository<LearningEvent, String> {

    List<LearningEvent> findByUserIdOrderByCreatedAtDesc(String userId);

    List<LearningEvent> findByUserIdAndEventTypeOrderByCreatedAtDesc(String userId, String eventType);

    Optional<LearningEvent> findByUserIdAndEventKey(String userId, String eventKey);

    List<LearningEvent> findByEventType(String eventType);

    long countByUserIdAndEventType(String userId, String eventType);
}