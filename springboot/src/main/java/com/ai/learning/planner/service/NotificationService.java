package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.UserNotification;
import com.ai.learning.planner.repository.UserNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 智能通知中心服务
 * 统一管理三类通知：EMERGENCY（紧急干预）/ WARNING（预警提醒）/ INFO（普通消息）
 * 提供聚合去重、排序（P0 > P1 > P2）、已读/已处理状态流转能力
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /** 优先级排序权重：EMERGENCY(0) > WARNING(1) > INFO(2) */
    private static final Map<String, Integer> PRIORITY_WEIGHT = Map.of(
            "EMERGENCY", 0,
            "WARNING", 1,
            "INFO", 2
    );

    /**
     * 创建通知
     *
     * @param actionData 快捷操作参数（JSON 字符串，可为 null）
     */
    @Transactional
    public UserNotification createNotification(Long userId, String title, String content,
                                               String priority, String category,
                                               String actionType, String actionData) {
        UserNotification notification = UserNotification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .priority(priority != null ? priority : "INFO")
                .category(category != null ? category : "SYSTEM")
                .actionType(actionType)
                .actionData(actionData)
                .isRead(false)
                .isHandled(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * 获取当前用户通知列表（已按优先级 P0 > P1 > P2 排序，同级按时间倒序）
     */
    @Transactional(readOnly = true)
    public List<UserNotification> getUserNotifications(Long userId) {
        List<UserNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.sort(Comparator
                .comparingInt((UserNotification n) -> PRIORITY_WEIGHT.getOrDefault(
                        n.getPriority() != null ? n.getPriority() : "INFO", 2))
                .thenComparing(UserNotification::getCreatedAt, Comparator.reverseOrder()));
        return notifications;
    }

    /**
     * 获取未读数量及未读紧急数（用于前端角标：存在 P0 未读时显示红色数字角标）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadStats(Long userId) {
        long total = notificationRepository.countByUserIdAndIsReadFalse(userId);
        long emergency = notificationRepository.countByUserIdAndIsReadFalseAndPriority(userId, "EMERGENCY");
        return Map.of(
                "total", total,
                "emergency", emergency
        );
    }

    /**
     * 标记单条通知已读
     */
    @Transactional
    public boolean markAsRead(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId).map(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
            return true;
        }).orElse(false);
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<UserNotification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    /**
     * 标记干预类通知为已处理（处理后不再重复提醒）
     */
    @Transactional
    public boolean markAsHandled(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId).map(n -> {
            n.setIsHandled(true);
            n.setHandledAt(LocalDateTime.now());
            n.setIsRead(true);
            notificationRepository.save(n);
            return true;
        }).orElse(false);
    }

    /**
     * 删除单条通知
     */
    @Transactional
    public boolean deleteNotification(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId).map(n -> {
            notificationRepository.delete(n);
            return true;
        }).orElse(false);
    }

    /**
     * 清空当前用户所有通知
     */
    @Transactional
    public void clearNotifications(Long userId) {
        notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /**
     * 序列化快捷操作参数为 JSON 字符串
     */
    public String buildActionData(Map<String, Object> data) {
        try {
            return data == null || data.isEmpty() ? null : objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("序列化通知快捷操作参数失败: {}", e.getMessage());
            return null;
        }
    }
}
