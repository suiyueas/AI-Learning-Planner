package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.UserRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户推荐记录仓储
 */
public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, String> {

    List<UserRecommendation> findByUserIdAndStatusOrderByGeneratedAtDesc(String userId, String status);

    List<UserRecommendation> findByUserIdAndStatusInOrderByGeneratedAtDesc(String userId, List<String> statuses);

    List<UserRecommendation> findByUserIdAndStatusAndContentTypeOrderByGeneratedAtDesc(
            String userId, String status, String contentType);

    /** 完成节点时联动标记消费：匹配 content_id 或 path_id 的活跃推荐 */
    List<UserRecommendation> findByUserIdAndStatusInAndContentIdOrUserIdAndStatusInAndPathId(
            String userId, List<String> statuses, String contentId, String userId2, List<String> statuses2, String pathId);

    long countByUserIdAndStatus(String userId, String status);
}
