package com.ai.learning.planner.controller;

import com.ai.learning.planner.entity.UserNotification;
import org.springframework.security.access.AccessDeniedException;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.InterventionNotificationService;
import com.ai.learning.planner.service.NotificationService;
import com.ai.learning.planner.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能通知中心接口
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final InterventionNotificationService interventionNotificationService;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 获取当前用户通知列表（P0 > P1 > P2 排序）
     */
    @GetMapping
    public List<UserNotification> getUserNotifications(Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        return notificationService.getUserNotifications(userId);
    }

    /**
     * 获取未读统计（total 总数 + emergency 未读紧急数，用于角标逻辑）
     */
    @GetMapping("/unread-stats")
    public Map<String, Object> getUnreadStats(Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        return notificationService.getUnreadStats(userId);
    }

    /**
     * 标记单条通知已读
     */
    @PutMapping("/{id}/read")
    public boolean markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        return notificationService.markAsRead(id, userId);
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    public void markAllAsRead(Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        notificationService.markAllAsRead(userId);
    }

    /**
     * 标记干预类通知为已处理（处理后不再重复提醒）
     */
    @PutMapping("/{id}/handled")
    public boolean markAsHandled(@PathVariable Long id, Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        return notificationService.markAsHandled(id, userId);
    }

    /**
     * 删除单条通知
     */
    @DeleteMapping("/{id}")
    public boolean deleteNotification(@PathVariable Long id, Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        return notificationService.deleteNotification(id, userId);
    }

    /**
     * 清空当前用户所有通知
     */
    @DeleteMapping
    public void clearNotifications(Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        notificationService.clearNotifications(userId);
    }

    /**
     * 手动触发干预扫描（进度滞后 + 知识点下降 + 连续未登录）
     */
    @PostMapping("/scan")
    public Map<String, Integer> scanInterventions() {
        // 全站干预扫描为管理操作（全表扫描 + 批量写通知），仅管理员可触发
        if (!securityContextHolder.isAdmin()) {
            throw new AccessDeniedException("仅管理员可触发干预扫描");
        }
        int progress = interventionNotificationService.scanProgressInterventions();
        int knowledge = interventionNotificationService.scanKnowledgeDecline();
        int inactive = interventionNotificationService.scanInactiveUsers();
        log.info("手动触发干预扫描：进度={}，知识点={}，未登录={}", progress, knowledge, inactive);
        return Map.of("progress", progress, "knowledge", knowledge, "inactive", inactive);
    }
}
