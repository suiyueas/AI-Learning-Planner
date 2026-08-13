package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学习路径仓储
 * 路径按 userId 归属；支持按用户查询路径列表与当前激活路径
 */
@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, String> {
    List<LearningPath> findByUserId(String userId);
    List<LearningPath> findByIsActive(Boolean isActive);
    Optional<LearningPath> findByUserIdAndIsActive(String userId, Boolean isActive);
}
