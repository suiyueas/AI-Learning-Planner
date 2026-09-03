package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.CheckinRecord;
import com.ai.learning.planner.entity.LearningEvent;
import com.ai.learning.planner.entity.ToolExecutionRecord;
import com.ai.learning.planner.repository.CheckinRecordRepository;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.repository.LearningEventRepository;
import com.ai.learning.planner.repository.ToolExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成就服务
 * 管理用户成就的解锁、查询和自动检查
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final LearningEventRepository learningEventRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final ToolExecutionRecordRepository toolExecutionRecordRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    private static final Map<String, String[]> ACHIEVEMENT_DEFINITIONS = Map.ofEntries(
            Map.entry("first_checkin", new String[]{"初次打卡", "完成首次学习打卡", "🌱"}),
            Map.entry("week_streak", new String[]{"一周达人", "连续打卡7天", "🔥"}),
            Map.entry("half_month_streak", new String[]{"半月坚持", "连续打卡14天", "💪"}),
            Map.entry("month_streak", new String[]{"月度之星", "连续打卡30天", "⭐"}),
            Map.entry("early_bird", new String[]{"早起鸟", "在8:00前打卡5次", "🐦"}),
            Map.entry("night_owl", new String[]{"夜猫子", "在23:00后打卡3次", "🦉"}),
            Map.entry("perfect_attendance", new String[]{"全勤奖", "当月每天打卡", "🏆"}),
            Map.entry("century_checkins", new String[]{"学习勇士", "累计打卡100天", "⚔️"}),
            Map.entry("persistent_learner", new String[]{"坚持者", "连续学习7天", "📖"}),
            Map.entry("explorer", new String[]{"探索者", "使用3个不同工具", "🔍"}),
            Map.entry("learning_master", new String[]{"学习达人", "学习时长超过100小时", "📚"}),
            Map.entry("knowledgeable", new String[]{"知识渊博", "上传10个文档", "🧠"})
    );

    /**
     * 解锁成就（如果已解锁则直接返回）
     */
    public LearningEvent unlockBadge(String userId, String badge, String description) {
        var existing = learningEventRepository.findByUserIdAndEventKey(userId, badge);
        if (existing.isPresent()) {
            log.info("成就已解锁: userId={}, badge={}", userId, badge);
            return existing.get();
        }

        LearningEvent event = LearningEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .userId(userId)
                .eventType("achievement")
                .eventKey(badge)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        log.info("解锁新成就: userId={}, badge={}", userId, badge);
        return learningEventRepository.save(event);
    }

    /**
     * 获取用户已解锁的成就列表
     */
    public List<LearningEvent> getUserAchievements(String userId) {
        return learningEventRepository.findByUserIdAndEventTypeOrderByCreatedAtDesc(userId, "achievement");
    }

    /**
     * 检查并自动更新用户成就
     */
    public Map<String, Object> checkAndUpdateAchievements(String userId) {
        List<LearningEvent> newlyUnlocked = new ArrayList<>();
        Set<String> unlockedKeys = getUserAchievements(userId).stream()
                .map(event -> event.getEventKey())
                .collect(Collectors.toSet());

        // 从 checkin_records 表获取打卡数据（统一数据源）
        List<CheckinRecord> checkinRecords = getCheckinRecords(userId);
        int totalCheckins = checkinRecords.size();
        int streak = calculateCheckinStreak(checkinRecords);
        int earlyBirdCount = countEarlyBirdCheckins(checkinRecords);
        int nightOwlCount = countNightOwlCheckins(checkinRecords);
        int distinctTools = countDistinctTools(userId);
        long documentCount = countUserDocuments(userId);

        if (totalCheckins >= 1 && !unlockedKeys.contains("first_checkin")) {
            newlyUnlocked.add(unlockBadge(userId, "first_checkin", "完成首次学习打卡"));
        }
        if (streak >= 7 && !unlockedKeys.contains("week_streak")) {
            newlyUnlocked.add(unlockBadge(userId, "week_streak", "连续打卡7天"));
        }
        if (streak >= 14 && !unlockedKeys.contains("half_month_streak")) {
            newlyUnlocked.add(unlockBadge(userId, "half_month_streak", "连续打卡14天"));
        }
        if (streak >= 30 && !unlockedKeys.contains("month_streak")) {
            newlyUnlocked.add(unlockBadge(userId, "month_streak", "连续打卡30天"));
        }
        if (earlyBirdCount >= 5 && !unlockedKeys.contains("early_bird")) {
            newlyUnlocked.add(unlockBadge(userId, "early_bird", "在8:00前打卡5次"));
        }
        if (nightOwlCount >= 3 && !unlockedKeys.contains("night_owl")) {
            newlyUnlocked.add(unlockBadge(userId, "night_owl", "在23:00后打卡3次"));
        }
        if (totalCheckins >= 100 && !unlockedKeys.contains("century_checkins")) {
            newlyUnlocked.add(unlockBadge(userId, "century_checkins", "累计打卡100天"));
        }
        if (streak >= 7 && !unlockedKeys.contains("persistent_learner")) {
            newlyUnlocked.add(unlockBadge(userId, "persistent_learner", "连续学习7天"));
        }
        if (distinctTools >= 3 && !unlockedKeys.contains("explorer")) {
            newlyUnlocked.add(unlockBadge(userId, "explorer", "使用3个不同工具"));
        }
        if (documentCount >= 10 && !unlockedKeys.contains("knowledgeable")) {
            newlyUnlocked.add(unlockBadge(userId, "knowledgeable", "上传10个文档"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("updated", !newlyUnlocked.isEmpty());
        result.put("newlyUnlocked", newlyUnlocked.stream()
                .map(e -> Map.of("id", e.getEventKey(), "name", e.getDescription()))
                .collect(Collectors.toList()));
        return result;
    }

    /**
     * 获取用户打卡记录（从 checkin_records 表）
     */
    public List<CheckinRecord> getCheckinRecords(Long userId) {
        return checkinRecordRepository.findByUserIdOrderByCheckinDateDesc(userId);
    }

    /**
     * 获取用户打卡记录（内部方法，String userId 兼容）
     */
    private List<CheckinRecord> getCheckinRecords(String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            return checkinRecordRepository.findByUserIdOrderByCheckinDateDesc(userIdLong);
        } catch (NumberFormatException e) {
            log.warn("Invalid userId format: {}", userId);
            return Collections.emptyList();
        }
    }

    /**
     * 计算连续打卡天数（从 checkin_records 表）
     */
    private int calculateCheckinStreak(List<CheckinRecord> checkinRecords) {
        if (checkinRecords.isEmpty()) return 0;

        Set<LocalDate> checkinDates = checkinRecords.stream()
                .map(CheckinRecord::getCheckinDate)
                .collect(Collectors.toSet());

        List<LocalDate> sortedDates = new ArrayList<>(checkinDates);
        Collections.sort(sortedDates, Comparator.reverseOrder());

        int streak = 0;
        LocalDate expectedDate = LocalDate.now();

        for (LocalDate date : sortedDates) {
            if (date.equals(expectedDate) || date.equals(expectedDate.minusDays(1))) {
                streak++;
                expectedDate = date.minusDays(1);
            } else if (date.isBefore(expectedDate.minusDays(1))) {
                break;
            }
        }
        return streak;
    }

    /**
     * 统计早起打卡次数（从 checkin_records 表）
     * 注意：checkin_records 表只有日期没有时间，这里简化处理返回0
     * 如需精确统计早起/夜猫子，需要在 checkin_records 表中添加时间字段
     */
    private int countEarlyBirdCheckins(List<CheckinRecord> checkinRecords) {
        // checkin_records 表只有日期，无法判断具体打卡时间
        // 暂时返回0，如需此功能需要扩展表结构
        return 0;
    }

    /**
     * 统计夜猫子打卡次数（从 checkin_records 表）
     */
    private int countNightOwlCheckins(List<CheckinRecord> checkinRecords) {
        // checkin_records 表只有日期，无法判断具体打卡时间
        // 暂时返回0，如需此功能需要扩展表结构
        return 0;
    }

    private int countDistinctTools(String userId) {
        List<ToolExecutionRecord> records = toolExecutionRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return records.stream()
                .map(record -> record.getToolId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();
    }

    private long countUserDocuments(String userId) {
        // 按用户统计，避免全站文档数导致成就无条件解锁
        return knowledgeDocumentRepository.countByUserId(userId);
    }

    public Map<String, Object> getAllAchievements(String userId) {
        List<LearningEvent> unlockedEvents = getUserAchievements(userId);
        Set<String> unlockedKeys = unlockedEvents.stream()
                .map(event -> event.getEventKey())
                .collect(Collectors.toSet());

        List<Map<String, Object>> achievementList = new ArrayList<>();
        ACHIEVEMENT_DEFINITIONS.forEach((id, info) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            item.put("name", info[0]);
            item.put("description", info[1]);
            item.put("icon", info[2]);
            item.put("unlocked", unlockedKeys.contains(id));
            achievementList.add(item);
        });

        return Map.of(
                "unlocked", unlockedEvents.size(),
                "total", ACHIEVEMENT_DEFINITIONS.size(),
                "list", achievementList
        );
    }

    public Map<String, Object> shareAchievement(Long userId, String achievementId, String format) {
        log.info("[shareAchievement] 分享成就: userId={}, achievementId={}, format={}", userId, achievementId, format);

        List<LearningEvent> achievements = getUserAchievements(String.valueOf(userId));
        LearningEvent targetAchievement = achievements.stream()
                .filter(a -> achievementId.equals(a.getEventKey()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("成就不存在或未解锁"));

        String achievementName = targetAchievement.getDescription();
        String[] achievementInfo = ACHIEVEMENT_DEFINITIONS.get(achievementId);
        String icon = achievementInfo != null ? achievementInfo[2] : "🏆";

        Map<String, Object> shareData = new HashMap<>();
        shareData.put("achievementId", achievementId);
        shareData.put("achievementName", achievementName);
        shareData.put("icon", icon);
        shareData.put("unlockedAt", targetAchievement.getCreatedAt() != null ?
                targetAchievement.getCreatedAt().toLocalDate().toString() : "未知");
        shareData.put("format", format);

        String shareText = buildShareText(achievementName, icon);

        Map<String, Object> result = new HashMap<>();
        result.put("shareData", shareData);
        result.put("shareText", shareText);
        result.put("shareUrl", "/achievements/" + achievementId);

        log.info("[shareAchievement] 分享数据准备完成: userId={}, achievementId={}", userId, achievementId);
        return result;
    }

    private String buildShareText(String achievementName, String icon) {
        return String.format("%s 我在 AI Learning Planner 解锁了【%s】成就！%n%s 学习路上，与你同行！%n#AI学习 #学习成就",
                icon, achievementName, icon);
    }
}
