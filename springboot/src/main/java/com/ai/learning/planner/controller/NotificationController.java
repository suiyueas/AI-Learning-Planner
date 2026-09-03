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

    /**
     * 为新用户初始化示例通知（首次打开通知中心时调用）
     * 生成 3 条示例通知帮助用户了解通知中心功能
     */
    @PostMapping("/seed")
    public List<UserNotification> seedSampleNotifications(Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        // 已有通知则不重复生成
        List<UserNotification> existing = notificationService.getUserNotifications(userId);
        if (!existing.isEmpty()) {
            return existing;
        }
        notificationService.createNotification(userId,
                "欢迎使用知途学习平台 🎉",
                "这里是通知中心，你可以在这里查看学习提醒、系统消息和AI学习建议。开始你的学习之旅吧！",
                "INFO", "SYSTEM", "VIEW_DETAIL", null);
        notificationService.createNotification(userId,
                "AI 学习建议：开启今日学习",
                "根据你的学习计划，建议今天完成「Python基础」的练习任务，保持学习连续性。",
                "INFO", "PROGRESS", "VIEW_DETAIL", null);
        notificationService.createNotification(userId,
                "提示：你可以自定义学习提醒",
                "前往「个人中心」-「通知设置」可以调整学习提醒阈值和通知偏好，让AI更好地为你服务。",
                "INFO", "SYSTEM", "VIEW_DETAIL", null);
        log.info("为用户 {} 初始化示例通知", userId);
        return notificationService.getUserNotifications(userId);
    }
}