package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.AssessmentRecord;
import com.ai.learning.planner.entity.DailyTask;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.entity.UserNotification;
import com.ai.learning.planner.repository.AssessmentRecordRepository;
import com.ai.learning.planner.repository.DailyTaskRepository;
import com.ai.learning.planner.repository.LearningPathRepository;
import com.ai.learning.planner.repository.UserNotificationRepository;
import com.ai.learning.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 干预通知自动生成服务
 * 定时检测学习进度偏差、知识点掌握度下降、异常行为（连续未登录），
 * 自动生成 P0（紧急干预）/ P1（预警提醒）通知并写入智能通知中心。
 * 同类别未处理的干预通知聚合去重，避免重复推送打扰用户。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterventionNotificationService {

    private final LearningPathRepository learningPathRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final AssessmentRecordRepository assessmentRecordRepository;
    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final AdaptiveEngineService adaptiveEngineService;

    /** 本周目标完成率 */
    private static final double TARGET_COMPLETION_RATE = 0.8;
    /** 默认进度提醒阈值（%）：完成率低于该值生成预警提醒 */
    private static final float DEFAULT_PROGRESS_THRESHOLD = 65f;
    /** 默认测评降幅阈值（%）：降幅超过该值生成预警提醒 */
    private static final float DEFAULT_SCORE_DECLINE_THRESHOLD = 10f;
    /** 默认连续未登录预警天数 */
    private static final int DEFAULT_INACTIVE_DAYS = 3;
    /** 严重滞后判定：进度低于阈值减 15 个百分点视为紧急（默认 65-15=50，与历史行为一致） */
    private static final float EMERGENCY_PROGRESS_GAP = 15f;
    /** 紧急降幅判定：降幅超过阈值 3 倍视为紧急（默认 10%*3=30%，与历史行为一致） */
    private static final int EMERGENCY_SCORE_MULTIPLIER = 3;

    /**
     * 每日 8:00 定时扫描所有干预维度（进度滞后 + 知识点下降 + 连续未登录）
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scanAllInterventions() {
        log.info("开始每日干预通知扫描");
        int progressCount = scanProgressInterventions();
        int knowledgeCount = scanKnowledgeDecline();
        int inactiveCount = scanInactiveUsers();
        log.info("干预通知扫描完成：进度={}，知识点={}，未登录={}", progressCount, knowledgeCount, inactiveCount);
    }

    /**
     * 进度干预检测：统计每个活跃路径用户的本周任务完成率，
     * 完成率 < 50% 生成 P0 紧急干预，50%-65% 生成 P1 预警提醒。
     */
    public int scanProgressInterventions() {
        int created = 0;
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<LearningPath> activePaths = learningPathRepository.findByIsActive(Boolean.TRUE);
        for (LearningPath path : activePaths) {
            Long userId = parseUserId(path.getUserId());
            if (userId == null) continue;

            // 读取用户自定义干预阈值（未设置时使用默认值）
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;
            if (Boolean.FALSE.equals(user.getInterventionEnabled())) continue;
            float progressThreshold = user.getInterventionProgressThreshold() != null
                    ? user.getInterventionProgressThreshold() : DEFAULT_PROGRESS_THRESHOLD;
            int inactiveDays = user.getInterventionInactiveDays() != null
                    ? user.getInterventionInactiveDays() : DEFAULT_INACTIVE_DAYS;

            List<DailyTask> weekTasks = dailyTaskRepository.findByUserIdAndTaskDateBetweenOrderByTaskDateAscSortOrderAsc(
                    path.getUserId(), weekStart, weekEnd);
            if (weekTasks.isEmpty()) continue;

            long total = weekTasks.size();
            long completed = weekTasks.stream().filter(t -> "completed".equals(t.getStatus())).count();
            double rate = (double) completed / total;
            // 连续未完成任务天数（当天有任务但全部未完成）
            long idleDays = weekTasks.stream()
                    .collect(Collectors.groupingBy(DailyTask::getTaskDate))
                    .values().stream()
                    .filter(dayTasks -> dayTasks.stream().noneMatch(t -> "completed".equals(t.getStatus())))
                    .count();

            double thresholdRate = progressThreshold / 100.0;
            double emergencyRate = Math.max((progressThreshold - EMERGENCY_PROGRESS_GAP) / 100.0, 0.05);
            String priority = null;
            if (rate < emergencyRate || idleDays >= inactiveDays) {
                priority = "EMERGENCY";
            } else if (rate < thresholdRate || idleDays >= inactiveDays - 1) {
                priority = "WARNING";
            }
            if (priority == null) continue;
            if (hasUnhandled(userId, "PROGRESS")) continue;

            String actionData = notificationService.buildActionData(Map.of(
                    "pathId", path.getId(),
                    "pathName", path.getName() != null ? path.getName() : "",
                    "completionRate", Math.round(rate * 100),
                    "idleDays", idleDays
            ));
            if ("EMERGENCY".equals(priority)) {
                notificationService.createNotification(userId,
                        "学习进度严重滞后",
                        String.format("本周学习计划完成率仅 %.0f%%，远低于目标 %.0f%%。\n连续 %d 天未完成每日学习任务。",
                                rate * 100, TARGET_COMPLETION_RATE * 100, idleDays),
                        "EMERGENCY", "PROGRESS", "ADJUST_PLAN", actionData);
            } else {
                notificationService.createNotification(userId,
                        "学习进度滞后预警",
                        String.format("本周学习计划完成率 %.0f%%，低于你的提醒阈值 %.0f%%，建议尽快调整学习节奏。",
                                rate * 100, progressThreshold),
                        "WARNING", "PROGRESS", "ADJUST_PLAN", actionData);
            }
            // 同步落库自适应调整记录（计划调整）：为自适应引擎页提供真实调整轨迹
            String detail = String.format(
                    "{\"pathId\":\"%s\",\"pathName\":\"%s\",\"completionRate\":%d,\"idleDays\":%d,\"targetRate\":%.0f}",
                    path.getId(), path.getName() != null ? path.getName() : "",
                    Math.round(rate * 100), idleDays, TARGET_COMPLETION_RATE * 100);
            adaptiveEngineService.recordAdjustment(path.getUserId(), path.getId(), "plan_adjust",
                    String.format("本周学习计划完成率仅 %.0f%%，低于目标 %.0f%%", rate * 100, TARGET_COMPLETION_RATE * 100),
                    detail, String.format("目标完成率 %.0f%%", TARGET_COMPLETION_RATE * 100));
            created++;
        }
        return created;
    }

    /**
     * 知识点下降检测：对比用户最近两次测评分数，降幅 > 10% 生成 P1，
     * 降幅 > 30% 生成 P0。识别具体下降知识点（最近一次测评薄弱科目）。
     */
    public int scanKnowledgeDecline() {
        int created = 0;
        for (User user : userRepository.findAll()) {
            if (Boolean.FALSE.equals(user.getInterventionEnabled())) continue;
            float declineThreshold = user.getInterventionScoreDeclineThreshold() != null
                    ? user.getInterventionScoreDeclineThreshold() : DEFAULT_SCORE_DECLINE_THRESHOLD;

            List<AssessmentRecord> records = assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            if (records.size() < 2) continue;
            // 仅对比最近 7 天内产生的测评，避免历史久远数据误报
            AssessmentRecord latest = records.get(0);
            AssessmentRecord previous = records.stream()
                    .skip(1)
                    .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                    .findFirst()
                    .orElse(null);
            if (previous == null || latest.getScore() == null || previous.getScore() == null
                    || previous.getScore() == 0) continue;

            int latestScore = latest.getScore();
            int previousScore = previous.getScore();
            double decline = (double) (previousScore - latestScore) / previousScore;
            if (decline <= declineThreshold / 100.0) continue;
            if (hasUnhandled(user.getId(), "KNOWLEDGE")) continue;

            String subject = latest.getSubject() != null ? latest.getSubject() : "综合能力";
            String actionData = notificationService.buildActionData(Map.of(
                    "subject", subject,
                    "latestScore", latestScore,
                    "previousScore", previousScore
            ));
            if (decline > declineThreshold * EMERGENCY_SCORE_MULTIPLIER / 100.0) {
                notificationService.createNotification(user.getId(),
                        "知识点掌握度严重下降",
                        String.format("最近一次《%s》测评得分 %d 分，较上次下降 %.0f%%，薄弱环节急需补强。",
                                subject, latestScore, decline * 100),
                        "EMERGENCY", "KNOWLEDGE", "START_REVIEW", actionData);
            } else {
                notificationService.createNotification(user.getId(),
                        "知识点掌握度下降",
                        String.format("最近一次《%s》测评得分 %d 分，相比上次下降 %.0f%%。",
                                subject, latestScore, decline * 100),
                        "WARNING", "KNOWLEDGE", "START_REVIEW", actionData);
            }
            // 同步落库自适应调整记录（复习插入）：为自适应引擎页提供真实调整轨迹
            String reviewDetail = String.format(
                    "{\"subject\":\"%s\",\"latestScore\":%d,\"previousScore\":%d,\"declineRate\":%.0f}",
                    subject, latestScore, previousScore, decline * 100);
            adaptiveEngineService.recordAdjustment(user.getId().toString(), "-", "review_insert",
                    String.format("检测到《%s》测评得分由 %d 分降至 %d 分（降幅 %.0f%%）",
                            subject, previousScore, latestScore, decline * 100),
                    reviewDetail, String.format("目标：复习巩固《%s》", subject));
            created++;
        }
        return created;
    }

    /**
     * 异常行为检测：连续 3 天未登录生成 P1 预警提醒
     */
    public int scanInactiveUsers() {
        int created = 0;
        for (User user : userRepository.findAll()) {
            if (Boolean.FALSE.equals(user.getInterventionEnabled())) continue;
            int inactiveDays = user.getInterventionInactiveDays() != null
                    ? user.getInterventionInactiveDays() : DEFAULT_INACTIVE_DAYS;
            LocalDateTime threshold = LocalDateTime.now().minusDays(inactiveDays);
            if (user.getLastLoginAt() == null || user.getLastLoginAt().isAfter(threshold)) continue;
            if (hasUnhandled(user.getId(), "PROGRESS")) continue;

            notificationService.createNotification(user.getId(),
                    "连续多日未登录",
                    String.format("你已连续 %d 天未登录学习，学习节奏可能中断，建议尽快回归。", inactiveDays),
                    "WARNING", "PROGRESS", "VIEW_DETAIL", null);
            created++;
        }
        return created;
    }

    /**
     * 聚合去重：同类别存在未处理通知时不再重复生成（仅保留最新一条提醒）
     */
    private boolean hasUnhandled(Long userId, String category) {
        List<UserNotification> existing = notificationRepository
                .findByUserIdAndCategoryAndIsHandledFalseOrderByCreatedAtDesc(userId, category);
        return existing.stream().anyMatch(n -> n.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1)));
    }

    private Long parseUserId(String userId) {
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            log.warn("学习路径用户ID无法解析: {}", userId);
            return null;
        }
    }
}
