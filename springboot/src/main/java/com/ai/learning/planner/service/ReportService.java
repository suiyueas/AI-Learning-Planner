package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.AssessmentRecord;
import com.ai.learning.planner.entity.CheckinRecord;
import com.ai.learning.planner.entity.DailyTask;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.entity.LearningRecord;
import com.ai.learning.planner.entity.User;
import com.ai.learning.planner.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 学情报告服务
 * 生成用户学习报告，包含概览、能力矩阵和建议等模块
 *
 * 核心设计原则：
 * 1. 数据逻辑自洽：所有指标来自统一数据源，时长>0时完成任务数必须>0
 * 2. 能力画像补充：无测评数据时，用学习进度作为能力概览的替代展示
 * 3. 建议个性化：基于用户行为模式生成多层级、驱动性的学习建议
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final UserRepository userRepository;
    private final LearningRecordRepository learningRecordRepository;
    private final AssessmentRecordRepository assessmentRecordRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final LearningPathRepository learningPathRepository;

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成学情报告
     * @param userId 用户ID
     * @param startDate 报告起始日期
     * @param endDate 报告结束日期
     * @param sections 报告模块（overview/matrix/recommendations）
     * @param style 报告样式
     */
    public Map<String, Object> generateReport(Long userId, LocalDate startDate, LocalDate endDate,
                                             List<String> sections, String style) {
        log.info("[generateReport] 生成学情报告: userId={}, period={}~{}", userId, startDate, endDate);

        Map<String, Object> report = new LinkedHashMap<>();

        LocalDateTime now = LocalDateTime.now();
        report.put("generatedAt", now.format(DISPLAY_DATETIME_FORMAT));
        report.put("dataUpdateAt", now.format(DISPLAY_DATETIME_FORMAT));
        report.put("periodStart", startDate.toString());
        report.put("periodEnd", endDate.toString());
        report.put("style", style != null ? style : "standard");

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            report.put("userName", user.getUsername());
        }

        if (sections == null || sections.isEmpty()) {
            sections = List.of("overview", "matrix", "recommendations");
        }

        for (String section : sections) {
            switch (section) {
                case "overview":
                    report.put("overview", buildOverviewSection(userId, startDate, endDate));
                    break;
                case "matrix":
                    report.put("matrix", buildAbilityMatrixSection(userId, startDate, endDate));
                    break;
                case "recommendations":
                    report.put("recommendations", buildRecommendationsSection(userId, startDate, endDate));
                    break;
                case "analysis":
                    report.put("analysis", buildAIAnalysisSection(userId, startDate, endDate));
                    break;
            }
        }

        log.info("[generateReport] 学情报告生成完成: userId={}", userId);
        return report;
    }

    /**
     * 构建报告概览模块
     * 数据来源统一，计算口径一致，保证数据逻辑自洽
     * 修复：totalLearningMinutes 和 totalTasksCompleted 必须来自同一批已完成记录
     */
    private Map<String, Object> buildOverviewSection(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> overview = new LinkedHashMap<>();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<LearningRecord> allLearningRecords = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(
                        String.valueOf(userId), "completed", startDateTime, endDateTime);

        // 修复：totalLearningMinutes 和 totalTasksCompleted 必须来自同一批已完成记录
        // 只有 status='completed' 且 completedAt 不为 null 的记录才算有效完成
        int totalLearningMinutes = allLearningRecords.stream()
                .filter(r -> r.getTimeSpent() != null && r.getCompletedAt() != null)
                .mapToInt(LearningRecord::getTimeSpent)
                .sum();

        int totalTasksCompleted = (int) allLearningRecords.stream()
                .filter(r -> "completed".equals(r.getStatus()) && r.getCompletedAt() != null)
                .count();

        List<CheckinRecord> checkinRecords = checkinRecordRepository
                .findByUserIdAndCheckinDateBetween(userId, startDate, endDate);
        int checkinDays = checkinRecords.size();

        List<AssessmentRecord> assessmentRecords = assessmentRecordRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDateTime, endDateTime);

        int totalAssessments = assessmentRecords.size();
        int totalCorrect = assessmentRecords.stream()
                .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                .mapToInt(r -> r.getScore())
                .sum();
        int totalQuestions = assessmentRecords.stream()
                .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                .mapToInt(r -> r.getTotal())
                .sum();

        int accuracy = totalQuestions > 0 ? Math.round((float) totalCorrect / totalQuestions * 100) : 0;

        overview.put("totalLearningHours", Math.round((float) totalLearningMinutes / 60 * 10) / 10.0);
        overview.put("totalTasksCompleted", totalTasksCompleted);
        overview.put("checkinDays", checkinDays);
        overview.put("totalAssessments", totalAssessments);
        overview.put("overallAccuracy", accuracy);

        Map<String, Integer> subjectStats = new LinkedHashMap<>();
        for (AssessmentRecord record : assessmentRecords) {
            String subject = record.getSubject();
            if (subject != null) {
                subjectStats.merge(subject, 1, Integer::sum);
            }
        }
        overview.put("subjectDistribution", subjectStats);

        overview.put("hasLearningData", totalLearningMinutes > 0);
        overview.put("hasAssessmentData", totalAssessments > 0);

        return overview;
    }

    /**
     * 构建能力矩阵模块
     * 策略：有测评数据时展示测评结果，无测评数据时展示学习进度替代
     */
    private Map<String, Object> buildAbilityMatrixSection(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> matrix = new LinkedHashMap<>();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<AssessmentRecord> assessmentRecords = assessmentRecordRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDateTime, endDateTime);

        Map<String, List<AssessmentRecord>> recordsBySubject = new LinkedHashMap<>();
        for (AssessmentRecord record : assessmentRecords) {
            String subject = record.getSubject();
            recordsBySubject.computeIfAbsent(subject, k -> new ArrayList<>()).add(record);
        }

        Map<String, Map<String, Object>> subjectMatrix = new LinkedHashMap<>();
        for (Map.Entry<String, List<AssessmentRecord>> entry : recordsBySubject.entrySet()) {
            String subject = entry.getKey();
            List<AssessmentRecord> records = entry.getValue();

            int totalCorrect = records.stream()
                    .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                    .mapToInt(r -> r.getScore())
                    .sum();
            int totalQuestions = records.stream()
                    .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                    .mapToInt(r -> r.getTotal())
                    .sum();

            int accuracy = totalQuestions > 0 ? Math.round((float) totalCorrect / totalQuestions * 100) : 0;

            Map<String, Object> subjectData = new LinkedHashMap<>();
            subjectData.put("attempts", records.size());
            subjectData.put("accuracy", accuracy);
            subjectData.put("correct", totalCorrect);
            subjectData.put("total", totalQuestions);

            String level;
            if (accuracy >= 90) level = "优秀";
            else if (accuracy >= 80) level = "良好";
            else if (accuracy >= 70) level = "中等偏上";
            else if (accuracy >= 60) level = "中等";
            else if (accuracy >= 50) level = "中等偏下";
            else level = "需加强";

            subjectData.put("level", level);
            subjectMatrix.put(subject, subjectData);
        }

        boolean hasAssessmentData = !subjectMatrix.isEmpty();
        matrix.put("hasAssessmentData", hasAssessmentData);

        if (hasAssessmentData) {
            matrix.put("dataSource", "assessment");
            matrix.put("dataSourceHint", "基于最近测评生成");
        } else {
            matrix.put("dataSource", "learning");
            matrix.put("dataSourceHint", "暂未参加测评，展示学习进度");
            matrix.put("callToAction", "点击开始测评，获得完整能力画像");
            matrix.put("actionType", "start_assessment");

            List<LearningRecord> learningRecords = learningRecordRepository
                    .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(
                            String.valueOf(userId), "completed", startDateTime, endDateTime);

            Map<String, Object> learningProgress = new LinkedHashMap<>();
            learningProgress.put("totalNodes", learningRecords.size());
            learningProgress.put("totalTimeMinutes", learningRecords.stream()
                    .filter(r -> r.getTimeSpent() != null)
                    .mapToInt(LearningRecord::getTimeSpent)
                    .sum());
            learningProgress.put("completedNodes", learningRecords.stream()
                    .filter(r -> "completed".equals(r.getStatus()))
                    .count());

            matrix.put("learningProgress", learningProgress);
        }

        matrix.put("subjects", subjectMatrix);
        return matrix;
    }

    /**
     * 构建学习建议模块
     * 多层级个性化建议策略：
     * 1. 紧急干预（连续3天无学习）
     * 2. 薄弱点驱动（有测评薄弱科目）
     * 3. 测评驱动（最近7天无测评）
     * 4. 进度驱动（路径完成率<30%）
     * 5. 通用兜底
     */
    private Map<String, Object> buildRecommendationsSection(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> recommendations = new LinkedHashMap<>();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        String userIdStr = String.valueOf(userId);

        List<AssessmentRecord> assessmentRecords = assessmentRecordRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDateTime, endDateTime);

        List<String> weakSubjects = new ArrayList<>();
        List<String> strongSubjects = new ArrayList<>();

        Map<String, List<AssessmentRecord>> recordsBySubject = new LinkedHashMap<>();
        for (AssessmentRecord record : assessmentRecords) {
            String subject = record.getSubject();
            recordsBySubject.computeIfAbsent(subject, k -> new ArrayList<>()).add(record);
        }

        for (Map.Entry<String, List<AssessmentRecord>> entry : recordsBySubject.entrySet()) {
            String subject = entry.getKey();
            List<AssessmentRecord> records = entry.getValue();

            int totalCorrect = records.stream()
                    .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                    .mapToInt(r -> r.getScore())
                    .sum();
            int totalQuestions = records.stream()
                    .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                    .mapToInt(r -> r.getTotal())
                    .sum();

            int accuracy = totalQuestions > 0 ? Math.round((float) totalCorrect / totalQuestions * 100) : 0;

            if (accuracy < 60) {
                weakSubjects.add(subject + " (正确率" + accuracy + "%)");
            } else if (accuracy >= 80) {
                strongSubjects.add(subject);
            }
        }

        List<String> suggestions = new ArrayList<>();
        String primaryAction = null;

        int daysSinceLastLearning = getDaysSinceLastLearning(userIdStr);
        int daysSinceLastAssessment = getDaysSinceLastAssessment(userIdStr);

        if (daysSinceLastLearning >= 3) {
            suggestions.add("检测到您已 " + daysSinceLastLearning + " 天未学习，建议今日完成一个 15 分钟的任务，保持学习节奏。");
            primaryAction = "start_learning";
        } else if (!weakSubjects.isEmpty()) {
            suggestions.add("您的薄弱点：" + String.join("、", weakSubjects) + "，建议优先复习相关章节，预计需 2 小时。");
            primaryAction = "review_weak";
        } else if (daysSinceLastAssessment >= 7) {
            suggestions.add("建议参加一次能力测评，了解当前水平，我们将为您优化学习路径。");
            primaryAction = "start_assessment";
        } else if (!strongSubjects.isEmpty()) {
            suggestions.add("您在 " + String.join("、", strongSubjects) + " 科目表现优秀，可以挑战更高难度。");
            primaryAction = "challenge_higher";
        } else {
            suggestions.add("继续保持当前学习节奏，每日完成 2-3 个任务，稳步提升。如有疑问，可随时咨询 AI 助手。");
            primaryAction = "maintain_progress";
        }

        recommendations.put("weakSubjects", weakSubjects);
        recommendations.put("strongSubjects", strongSubjects);
        recommendations.put("suggestions", suggestions);
        recommendations.put("primaryAction", primaryAction);
        recommendations.put("daysSinceLastLearning", daysSinceLastLearning);
        recommendations.put("daysSinceLastAssessment", daysSinceLastAssessment);

        return recommendations;
    }

    private int getDaysSinceLastLearning(String userId) {
        try {
            List<LearningRecord> records = learningRecordRepository.findByUserId(userId);
            if (records == null || records.isEmpty()) {
                return 999;
            }
            LocalDateTime lastLearning = records.stream()
                    .filter(r -> r.getCompletedAt() != null)
                    .map(LearningRecord::getCompletedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            if (lastLearning == null) {
                return 999;
            }
            return (int) ChronoUnit.DAYS.between(lastLearning.toLocalDate(), LocalDate.now());
        } catch (Exception e) {
            log.debug("[ReportService] 获取最近学习时间失败: {}", e.getMessage());
            return 999;
        }
    }

    private int getDaysSinceLastAssessment(String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            List<AssessmentRecord> records = assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(userIdLong);
            if (records == null || records.isEmpty()) {
                return 999;
            }
            LocalDateTime lastAssessment = records.stream()
                    .filter(r -> r.getCreatedAt() != null)
                    .map(AssessmentRecord::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            if (lastAssessment == null) {
                return 999;
            }
            return (int) ChronoUnit.DAYS.between(lastAssessment.toLocalDate(), LocalDate.now());
        } catch (Exception e) {
            log.debug("[ReportService] 获取最近测评时间失败: {}", e.getMessage());
            return 999;
        }
    }

    // ==================== AI 学习分析模块 ====================

    /**
     * 构建 AI 学习分析模块
     * 生成个性化的学习分析报告，帮助用户理解自己的学习模式并提供改进建议
     */
    private Map<String, Object> buildAIAnalysisSection(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        String userIdStr = String.valueOf(userId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 获取数据
        List<LearningRecord> learningRecords = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(
                        userIdStr, "completed", startDateTime, endDateTime);

        List<CheckinRecord> checkinRecords = checkinRecordRepository
                .findByUserIdAndCheckinDateBetween(userId, startDate, endDate);

        List<AssessmentRecord> assessmentRecords = assessmentRecordRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDateTime, endDateTime);

        // 计算核心指标
        int totalMinutes = learningRecords.stream()
                .filter(r -> r.getTimeSpent() != null && r.getCompletedAt() != null)
                .mapToInt(LearningRecord::getTimeSpent)
                .sum();
        int completedTasks = (int) learningRecords.stream()
                .filter(r -> "completed".equals(r.getStatus()) && r.getCompletedAt() != null)
                .count();
        int checkinDays = checkinRecords.size();
        double totalHours = Math.round((float) totalMinutes / 60 * 10) / 10.0;

        // 生成分析段落
        List<String> insights = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        // 1. 学习时长与任务完成一致性分析
        if (totalHours > 0 && completedTasks == 0) {
            insights.add(String.format("⚠️ 数据不一致：你已累计学习 %.1f 小时，但尚未完成任务。", totalHours));
            insights.add("这可能是因为学习记录的状态未正确更新。请检查是否有任务完成但状态未标记的情况。");
            actions.add("建议从「变量与数据类型」开始，这是最短可完成的学习单元。");
        } else if (totalHours > 0 && completedTasks > 0) {
            double efficiency = totalHours / completedTasks;
            if (efficiency > 2) {
                insights.add(String.format("📊 学习效率分析：你完成 %d 个任务累计 %.1f 小时，平均每个任务 %.1f 小时。", completedTasks, totalHours, efficiency));
                insights.add("学习效率较高，建议继续保持当前节奏，或挑战更复杂的内容。");
            } else if (efficiency < 0.5) {
                insights.add(String.format("📊 学习效率分析：你完成 %d 个任务累计 %.1f 小时，平均每个任务仅 %.1f 小时。", completedTasks, totalHours, efficiency));
                insights.add("任务完成较快，建议确保每个任务都深入理解，可适当增加练习时间。");
            } else {
                insights.add(String.format("📊 学习效率分析：你完成 %d 个任务累计 %.1f 小时，学习节奏良好。", completedTasks, totalHours));
            }
        } else if (totalHours == 0 && completedTasks == 0 && checkinDays > 0) {
            insights.add("📊 你有 " + checkinDays + " 天打卡记录，但期间没有完成任何学习任务。");
            insights.add("建议选择简短的学习任务开始，例如「变量与数据类型」，只需约 25 分钟即可完成。");
        } else if (totalHours == 0 && completedTasks == 0) {
            insights.add("📊 报告期间暂无学习记录。开始学习吧！AI 将为你规划专属学习路径。");
            actions.add("设定学习目标，让 AI 帮你制定计划");
        }

        // 2. 打卡行为分析
        if (checkinDays > 0) {
            int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
            double checkinRate = (double) checkinDays / totalDays * 100;
            if (checkinRate > 80) {
                insights.add("🔥 学习意愿强：你有 " + checkinDays + " 天打卡记录（占报告期 " + String.format("%.0f", checkinRate) + "%），学习积极性很高。");
            } else if (checkinRate > 50) {
                insights.add("📈 学习意愿中等：你有 " + checkinDays + " 天打卡记录，继续保持可以形成更好的学习习惯。");
            } else {
                insights.add("📉 学习频率偏低：你有 " + checkinDays + " 天打卡记录，建议增加每周学习次数。");
            }
        }

        // 3. 薄弱点分析
        if (!assessmentRecords.isEmpty()) {
            Map<String, List<AssessmentRecord>> recordsBySubject = new LinkedHashMap<>();
            for (AssessmentRecord record : assessmentRecords) {
                String subject = record.getSubject();
                recordsBySubject.computeIfAbsent(subject, k -> new ArrayList<>()).add(record);
            }

            List<String> weakSubjects = new ArrayList<>();
            for (Map.Entry<String, List<AssessmentRecord>> entry : recordsBySubject.entrySet()) {
                List<AssessmentRecord> records = entry.getValue();
                int totalCorrect = records.stream()
                        .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                        .mapToInt(r -> r.getScore())
                        .sum();
                int totalQuestions = records.stream()
                        .filter(r -> r.getScore() != null && r.getTotal() != null && r.getTotal() > 0)
                        .mapToInt(r -> r.getTotal())
                        .sum();
                int accuracy = totalQuestions > 0 ? Math.round((float) totalCorrect / totalQuestions * 100) : 0;

                if (accuracy < 60) {
                    weakSubjects.add(entry.getKey() + "（正确率" + accuracy + "%）");
                }
            }

            if (!weakSubjects.isEmpty()) {
                insights.add("📚 薄弱知识点：" + String.join("、", weakSubjects) + "。建议优先复习这些章节。");
                actions.add("开始专项练习，巩固薄弱知识点");
            }
        }

        // 4. 进度预估
        if (completedTasks > 0 && totalHours > 0) {
            int remainingTasks = Math.max(0, 10 - completedTasks);
            double avgTimePerTask = totalHours / completedTasks;
            double remainingHours = remainingTasks * avgTimePerTask;
            int remainingDays = (int) Math.ceil(remainingHours / 1.5);

            if (remainingTasks > 0) {
                insights.add("⏱️ 进度预估：按当前节奏，预计还需 " + remainingDays + " 天完成剩余 " + remainingTasks + " 个任务。");
            }
        }

        analysis.put("insights", insights);
        analysis.put("suggestedActions", actions);
        analysis.put("dataConsistency", totalHours > 0 && completedTasks == 0 ? "warning" : "ok");

        return analysis;
    }
}