package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户通知仓储
 * 通知按 userId 归属；支持未读统计、优先级角标统计与干预类通知聚合去重
 */
@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    /** 统计指定优先级的未读数量（用于前端角标逻辑） */
    long countByUserIdAndIsReadFalseAndPriority(Long userId, String priority);

    /** 查询同类别下仍未处理的干预类通知（用于聚合去重） */
    List<UserNotification> findByUserIdAndCategoryAndIsHandledFalseOrderByCreatedAtDesc(Long userId, String category);
}
