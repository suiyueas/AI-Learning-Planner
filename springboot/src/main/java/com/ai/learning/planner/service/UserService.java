package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.auth.ProfileUpdateRequest;
import com.ai.learning.planner.dto.auth.UserPreferencesDTO;
import com.ai.learning.planner.dto.auth.UserProfileResponse;
import com.ai.learning.planner.entity.LearningEvent;
import com.ai.learning.planner.entity.LearningRecord;
import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.LearningEventRepository;
import com.ai.learning.planner.repository.LearningRecordRepository;
import com.ai.learning.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务类
 * 提供用户信息管理、统计、偏好设置、成就系统等功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final LearningEventRepository learningEventRepository;
    private final LearningRecordRepository learningRecordRepository;

    /**
     * 成就定义（统一管理，与前端保持一致）
     */
    private static final List<AchievementDefinition> ACHIEVEMENT_DEFINITIONS = List.of(
            new AchievementDefinition("first_checkin", "初次打卡", "完成首次学习打卡", "🌱"),
            new AchievementDefinition("week_streak", "一周达人", "连续打卡7天", "🔥"),
            new AchievementDefinition("half_month_streak", "半月坚持", "连续打卡14天", "💪"),
            new AchievementDefinition("month_streak", "月度之星", "连续打卡30天", "⭐"),
            new AchievementDefinition("early_bird", "早起鸟", "在8:00前打卡5次", "🐦"),
            new AchievementDefinition("night_owl", "夜猫子", "在23:00后打卡3次", "🦉"),
            new AchievementDefinition("perfect_attendance", "全勤奖", "当月每天打卡", "🏆"),
            new AchievementDefinition("century_checkins", "学习勇士", "累计打卡100天", "⚔️"),
            new AchievementDefinition("persistent_learner", "坚持者", "连续学习7天", "📖"),
            new AchievementDefinition("explorer", "探索者", "使用3个不同工具", "🔍"),
            new AchievementDefinition("learning_master", "学习达人", "学习时长超过100小时", "📚"),
            new AchievementDefinition("knowledgeable", "知识渊博", "上传10个文档", "🧠")
    );

    // ==================== 用户信息管理 ====================

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 获取用户完整个人信息（含统计）
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 获取统计数据
        Map<String, Object> stats = getUserStats(userId);
        Map<String, Object> learningStats = getLearningStats(userId);

        // 构建响应
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .learningGoal(user.getLearningGoal())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .learningStats(UserProfileResponse.LearningStats.builder()
                        .totalLearningHours(((Number) stats.getOrDefault("totalHours", 0)).intValue())
                        .completedNodes(((Number) learningStats.getOrDefault("completedNodes", 0)).intValue())
                        .achievementCount(((Number) stats.getOrDefault("achievementCount", 0)).intValue())
                        .continuousDays(((Number) stats.getOrDefault("streak", 0)).intValue())
                        .averageScore(((Number) learningStats.getOrDefault("averageScore", 0)).doubleValue())
                        .weeklyProgress(((Number) learningStats.getOrDefault("weeklyProgress", 0)).floatValue())
                        .progress(((Number) learningStats.getOrDefault("progress", 0)).floatValue())
                        .totalTasks(((Number) learningStats.getOrDefault("totalTasks", 0)).floatValue())
                        .build())
                .build();
    }

    /**
     * 更新用户基本信息
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getLearningGoal() != null) {
            user.setLearningGoal(request.getLearningGoal());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        userRepository.save(user);
        log.info("用户信息更新成功: userId={}", userId);

        return getProfile(userId);
    }

    /**
     * 更新用户头像
     */
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        try {
            String avatarUrl = fileUploadService.uploadAvatar(file, userId);
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            log.info("用户头像更新成功: userId={}, avatarUrl={}", userId, avatarUrl);
            return avatarUrl;
        } catch (Exception e) {
            log.error("头像上传失败: userId={}", userId, e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码不正确");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("用户密码修改成功: userId={}", userId);
    }

    // ==================== 统计服务 ====================

    /**
     * 获取用户统计数据（统计卡片用）
     */
    public Map<String, Object> getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String userIdStr = String.valueOf(userId);

        // 总学习时长（分钟→小时）
        Long totalMinutes = learningRecordRepository.sumTimeSpentByUserId(userIdStr);
        int totalHours = totalMinutes != null ? totalMinutes.intValue() / 60 : 0;

        // 已完成节点数
        long completedNodes = learningRecordRepository.countByUserIdAndStatus(userIdStr, "completed");

        // 成就数量
        long achievementCount = learningEventRepository.countByUserIdAndEventType(userIdStr, "achievement");

        // 连续打卡天数（直接从 User 实体读取，由打卡服务维护）
        int streak = user.getContinuousCheckinDays() != null ? user.getContinuousCheckinDays() : 0;

        // 完成率
        long totalRecords = learningRecordRepository.findByUserId(userIdStr).size();
        int completionRate = totalRecords > 0 ? (int) ((completedNodes * 100) / totalRecords) : 0;

        return Map.of(
                "streak", streak,
                "totalHours", totalHours,
                "completedNodes", completedNodes,
                "achievementCount", achievementCount,
                "completionRate", completionRate
        );
    }

    /**
     * 获取学习统计数据（学习统计面板用）
     */
    public Map<String, Object> getLearningStats(Long userId) {
        String userIdStr = String.valueOf(userId);

        List<LearningRecord> records = learningRecordRepository.findByUserId(userIdStr);
        long totalTasks = records.size();
        long completedNodes = records.stream()
                .filter(r -> "completed".equals(r.getStatus()))
                .count();

        // 平均掌握度（转换为百分制）
        Float avgMastery = learningRecordRepository.avgMasteryLevelByUserId(userIdStr);
        int averageScore = avgMastery != null ? Math.round(avgMastery * 20) : 0; // 假设 mastery 是 0-5 分

        // 本周进度
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long weeklyProgress = records.stream()
                .filter(r -> r.getCompletedAt() != null)
                .filter(r -> r.getCompletedAt().isAfter(weekAgo))
                .count();

        int progress = totalTasks > 0 ? (int) ((completedNodes * 100) / totalTasks) : 0;

        return Map.of(
                "completedNodes", completedNodes,
                "totalTasks", totalTasks,
                "averageScore", averageScore,
                "weeklyProgress", Math.min(weeklyProgress, 20), // 假设每周目标为 20
                "progress", progress,
                "weeklyGoal", 20
        );
    }

    // ==================== 成就系统 ====================

    /**
     * 获取用户成就列表
     */
    public Map<String, Object> getAchievements(Long userId) {
        String userIdStr = String.valueOf(userId);

        // 获取已解锁的成就事件
        List<LearningEvent> unlockedEvents = learningEventRepository
                .findByUserIdAndEventTypeOrderByCreatedAtDesc(userIdStr, "achievement");

        Set<String> unlockedKeys = unlockedEvents.stream()
                .map(LearningEvent::getEventKey)
                .collect(Collectors.toSet());

        // 构建成就列表
        List<Map<String, Object>> achievementList = ACHIEVEMENT_DEFINITIONS.stream()
                .map(def -> {
                    boolean isUnlocked = unlockedKeys.contains(def.id);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", def.id);
                    item.put("name", def.name);
                    item.put("description", def.description);
                    item.put("icon", def.icon);
                    item.put("unlocked", isUnlocked);
                    return item;
                })
                .collect(Collectors.toList());

        return Map.of(
                "unlocked", unlockedEvents.size(),
                "total", ACHIEVEMENT_DEFINITIONS.size(),
                "list", achievementList
        );
    }

    // ==================== 偏好设置 ====================

    public UserPreferencesDTO getPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return UserPreferencesDTO.builder()
                .userId(user.getId())
                .learningStyle(user.getLearningStyle())
                .difficulty(convertLevelToDifficulty(user.getLevel()))
                .dailyHours(2)
                .timeSlots(user.getActiveHours() != null && !user.getActiveHours().isBlank() ?
                        Arrays.asList(user.getActiveHours().split(",")) :
                        List.of("morning", "afternoon"))
                .targetField(user.getTargetField())
                .weakPoints(user.getWeakPoints())
                .interventionEnabled(user.getInterventionEnabled() != null ? user.getInterventionEnabled() : true)
                .interventionProgressThreshold(user.getInterventionProgressThreshold() != null ? user.getInterventionProgressThreshold() : 65f)
                .interventionScoreDeclineThreshold(user.getInterventionScoreDeclineThreshold() != null ? user.getInterventionScoreDeclineThreshold() : 10f)
                .interventionInactiveDays(user.getInterventionInactiveDays() != null ? user.getInterventionInactiveDays() : 3)
                .build();
    }

    @Transactional
    public UserPreferencesDTO savePreferences(Long userId, UserPreferencesDTO preferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (preferences.getLearningStyle() != null) {
            user.setLearningStyle(preferences.getLearningStyle());
        }
        if (preferences.getDifficulty() != null) {
            user.setLevel(convertDifficultyToLevel(preferences.getDifficulty()));
        }
        if (preferences.getTimeSlots() != null) {
            user.setActiveHours(String.join(",", preferences.getTimeSlots()));
        }
        if (preferences.getTargetField() != null) {
            user.setTargetField(preferences.getTargetField());
        }
        if (preferences.getWeakPoints() != null) {
            user.setWeakPoints(preferences.getWeakPoints());
        }

        // 智能干预阈值
        if (preferences.getInterventionEnabled() != null) {
            user.setInterventionEnabled(preferences.getInterventionEnabled());
        }
        if (preferences.getInterventionProgressThreshold() != null) {
            user.setInterventionProgressThreshold(
                    Math.max(20f, Math.min(95f, preferences.getInterventionProgressThreshold())));
        }
        if (preferences.getInterventionScoreDeclineThreshold() != null) {
            user.setInterventionScoreDeclineThreshold(
                    Math.max(1f, Math.min(50f, preferences.getInterventionScoreDeclineThreshold())));
        }
        if (preferences.getInterventionInactiveDays() != null) {
            user.setInterventionInactiveDays(
                    Math.max(1, Math.min(14, preferences.getInterventionInactiveDays())));
        }

        userRepository.save(user);
        log.info("用户偏好设置保存成功: userId={}", userId);

        return getPreferences(userId);
    }

    // ==================== 辅助方法 ====================

    private Integer convertDifficultyToLevel(String difficulty) {
        if (difficulty == null) return 5;
        return switch (difficulty) {
            case "gradual" -> 2;
            case "moderate" -> 5;
            case "challenge" -> 8;
            default -> 5;
        };
    }

    private String convertLevelToDifficulty(Integer level) {
        if (level == null) return "moderate";
        if (level <= 3) return "gradual";
        if (level <= 7) return "moderate";
        return "challenge";
    }

    /**
     * 成就定义内部类
     */
    @lombok.AllArgsConstructor
    @lombok.Getter
    private static class AchievementDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final String icon;
    }
}