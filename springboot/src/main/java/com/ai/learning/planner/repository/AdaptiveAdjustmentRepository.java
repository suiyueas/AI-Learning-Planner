package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.AdaptiveAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 自适应调整记录仓储
 */
public interface AdaptiveAdjustmentRepository extends JpaRepository<AdaptiveAdjustment, String> {

    List<AdaptiveAdjustment> findByUserIdOrderByCreatedAtDesc(String userId);

    List<AdaptiveAdjustment> findByUserIdAndAdjustmentTypeOrderByCreatedAtDesc(String userId, String adjustmentType);

    long countByUserId(String userId);

    AdaptiveAdjustment findTopByUserIdOrderByCreatedAtDesc(String userId);
}
