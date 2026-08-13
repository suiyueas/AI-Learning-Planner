package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.entity.LearningEvent;
import org.springframework.security.access.AccessDeniedException;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 成就系统控制器
 * <p>
 * 提供用户成就的查询、自动检查与更新、手动解锁等功能。
 * 所有接口均需认证，通过 {@link Authentication#getPrincipal()} 获取当前用户 ID。
 * </p>
 * <p>
 * 成就类型包括：首次打卡、连续打卡（7/14/30天）、早起/夜猫子打卡、
 * 全勤、累计打卡100天、连续学习、使用工具、学习时长、上传文档等。
 * </p>
 */
@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
@Slf4j
public class AchievementController {

    private final AchievementService achievementService;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 获取当前用户的所有成就列表
     * <p>
     * 返回用户所有成就的完整信息，包括成就ID、名称、描述、是否已解锁、解锁时间等。
     * </p>
     *
     * @param authentication Spring Security 认证对象，用于获取当前用户 ID
     * @return 包含成就列表的响应，数据结构为 Map，key 为成就ID，value 为成就详情
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getAllAchievements(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取用户成就列表: userId={}", userId);
        Map<String, Object> achievements = achievementService.getAllAchievements(String.valueOf(userId));
        return ApiResponse.success(achievements);
    }

    /**
     * 检查并自动更新用户成就
     * <p>
     * 根据用户当前的学习数据（打卡记录、学习时长、工具使用等）自动检查所有成就条件，
     * 如果满足条件则自动解锁对应成就。
     * </p>
     * <p>
     * 通常在用户打卡、完成学习任务等操作后调用，用于触发成就的自动解锁。
     * </p>
     *
     * @param authentication Spring Security 认证对象，用于获取当前用户 ID
     * @return 包含本次新解锁的成就列表和已有成就列表的响应
     */
    @PostMapping("/check")
    public ApiResponse<Map<String, Object>> checkAchievements(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("检查并更新用户成就: userId={}", userId);
        Map<String, Object> result = achievementService.checkAndUpdateAchievements(String.valueOf(userId));
        return ApiResponse.success(result);
    }

    /**
     * 手动解锁指定成就（管理员或调试功能）
     * <p>
     * 根据传入的成就ID，手动将指定成就标记为已解锁。
     * 主要用于后台管理或开发调试场景。
     * </p>
     *
     * @param authentication Spring Security 认证对象，用于获取当前用户 ID
     * @param body           请求体，包含 {@code achievementId} 字段
     * @return 包含学习事件（成就解锁记录）的响应
     */
    @PostMapping("/unlock")
    public ApiResponse<LearningEvent> unlockAchievement(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        Long userId = (Long) authentication.getPrincipal();
        String achievementId = body.get("achievementId");

        // 仅管理员可手动解锁成就（绕过条件判定）
        if (!securityContextHolder.isAdmin()) {
            throw new AccessDeniedException("仅管理员可手动解锁成就");
        }
        // 成就ID必须存在于定义表中，防止写入无效 eventKey 记录
        String[] achievementInfo = getAchievementInfo(achievementId);
        if (achievementInfo == null) {
            throw new IllegalArgumentException("无效的成就ID: " + achievementId);
        }
        log.info("手动解锁成就: userId={}, achievementId={}", userId, achievementId);

        LearningEvent event = achievementService.unlockBadge(
                String.valueOf(userId),
                achievementId,
                achievementInfo[0]
        );
        return ApiResponse.success(event);
    }

    @PostMapping("/share")
    public ApiResponse<Map<String, Object>> shareAchievement(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        Long userId = (Long) authentication.getPrincipal();
        String achievementId = body.get("achievementId");
        String format = body.getOrDefault("format", "link");
        log.info("分享成就: userId={}, achievementId={}, format={}", userId, achievementId, format);

        try {
            Map<String, Object> shareData = achievementService.shareAchievement(userId, achievementId, format);
            return ApiResponse.success(shareData);
        } catch (Exception e) {
            log.error("分享成就失败: userId={}, achievementId={}", userId, achievementId, e);
            return ApiResponse.error("分享成就失败: " + e.getMessage());
        }
    }

    /**
     * 根据成就ID获取对应的成就描述信息
     * <p>
     * 私有辅助方法，用于将成就ID映射为成就名称和描述。
     * </p>
     *
     * @param achievementId 成就标识符
     * @return 包含成就名称和描述的字符串数组，格式为 [名称, 描述]；
     *         若成就ID无效则返回 {@code null}
     */
    private String[] getAchievementInfo(String achievementId) {
        return switch (achievementId) {
            case "first_checkin" -> new String[]{"首次打卡", "完成首次学习打卡"};
            case "week_streak" -> new String[]{"一周达人", "连续打卡7天"};
            case "half_month_streak" -> new String[]{"半月坚持", "连续打卡14天"};
            case "month_streak" -> new String[]{"月度之星", "连续打卡30天"};
            case "early_bird" -> new String[]{"早起鸟", "在8:00前打卡5次"};
            case "night_owl" -> new String[]{"夜猫子", "在23:00后打卡3次"};
            case "perfect_attendance" -> new String[]{"全勤奖", "当月每天打卡"};
            case "century_checkins" -> new String[]{"学习勇士", "累计打卡100天"};
            case "persistent_learner" -> new String[]{"坚持者", "连续学习7天"};
            case "explorer" -> new String[]{"探索者", "使用3个不同工具"};
            case "learning_master" -> new String[]{"学习达人", "学习时长超过100小时"};
            case "knowledgeable" -> new String[]{"知识渊博", "上传10个文档"};
            default -> null;
        };
    }
}