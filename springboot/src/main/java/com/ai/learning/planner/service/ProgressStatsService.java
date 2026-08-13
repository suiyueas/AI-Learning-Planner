package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.AssessmentRecord;
import com.ai.learning.planner.entity.CheckinRecord;
import com.ai.learning.planner.entity.DailyTask;
import com.ai.learning.planner.entity.KnowledgeNode;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.entity.LearningRecord;
import com.ai.learning.planner.repository.AssessmentRecordRepository;
import com.ai.learning.planner.repository.CheckinRecordRepository;
import com.ai.learning.planner.repository.DailyTaskRepository;
import com.ai.learning.planner.repository.KnowledgeNodeRepository;
import com.ai.learning.planner.repository.LearningPathRepository;
import com.ai.learning.planner.repository.LearningRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习进度统计服务
 * 从 learning_records / daily_tasks / assessment_records / checkin_records 聚合真实学习数据，
 * 支撑学习概览（学习曲线、能力矩阵、学习记录列表）的真实化展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressStatsService {

    private final LearningRecordRepository learningRecordRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final AssessmentRecordRepository assessmentRecordRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final KnowledgeNodeRepository knowledgeNodeRepository;
    private final LearningPathRepository learningPathRepository;

    private static final String COMPLETED = "completed";

    /** 掌握度归一化：SQL 注释约定 mastery_level 为 0-5 分制，兼容 0-100 分制数据 */
    private double normalizeMastery(Float masteryLevel) {
        if (masteryLevel == null) return 0;
        double v = masteryLevel;
        return v <= 5 ? Math.min(v * 20, 100) : Math.min(v, 100);
    }

    // ==================== 学习概览 ====================

    public Map<String, Object> getOverview(String userId) {
        Long totalMinutes = Optional.ofNullable(learningRecordRepository.sumTimeSpentByUserId(userId)).orElse(0L);
        long completed = learningRecordRepository.countByUserIdAndStatus(userId, COMPLETED);
        long activePaths = learningRecordRepository.countDistinctPathIdByUserId(userId);
        double totalHours = Math.round(totalMinutes / 6.0) / 10.0;

        // 平均分：优先测评成绩（score/total），兜底学习记录掌握度
        double avgScore = calculateAvgAssessmentScore(userId);
        if (avgScore == 0) {
            Float avgMastery = learningRecordRepository.avgMasteryLevelByUserId(userId);
            if (avgMastery != null) avgScore = normalizeMastery(avgMastery);
        }
        avgScore = Math.round(avgScore);

        return Map.of(
                "streak", calculateStreak(userId),
                "totalHours", totalHours,
                "completed", (int) completed,
                "avgScore", (int) avgScore,
                "activePaths", (int) activePaths,
                "todayMinutes", calculateTodayMinutes(userId)
        );
    }

    /** 连续学习天数：合并打卡日期 + 学习完成日期，从最近一天向前连续计数 */
    private int calculateStreak(String userId) {
        try {
            Long userIdLong = Long.valueOf(userId);
            Optional<CheckinRecord> latestCheckin = checkinRecordRepository.findTopByUserIdOrderByCheckinDateDesc(userIdLong);
            Set<LocalDate> checkinDates = new HashSet<>();
            if (latestCheckin.isPresent()) {
                // 逐月回溯收集打卡日期（按月查询，直至没有更早记录）
                LocalDate cursor = latestCheckin.get().getCheckinDate();
                while (cursor.getYear() >= 2020) {
                    List<LocalDate> monthDates = checkinRecordRepository.findCheckinDatesByUserIdAndMonth(
                            userIdLong, cursor.getYear(), cursor.getMonthValue());
                    if (monthDates.isEmpty()) break;
                    checkinDates.addAll(monthDates);
                    cursor = cursor.minusMonths(1);
                }
            }
            // 学习记录完成日期兜底
            List<LearningRecord> records = learningRecordRepository
                    .findByUserIdAndStatusAndCompletedAtIsNotNullOrderByCompletedAtDesc(userId, COMPLETED);
            records.forEach(r -> {
                if (r.getCompletedAt() != null) checkinDates.add(r.getCompletedAt().toLocalDate());
            });
            if (checkinDates.isEmpty()) return 0;

            int streak = 0;
            LocalDate day = LocalDate.now();
            if (!checkinDates.contains(day)) {
                day = day.minusDays(1);
            }
            while (checkinDates.contains(day)) {
                streak++;
                day = day.minusDays(1);
            }
            return streak;
        } catch (Exception e) {
            log.warn("[ProgressStats] 连续天数计算失败: {}", e.getMessage());
            return 0;
        }
    }

    private int calculateTodayMinutes(String userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<LearningRecord> todayRecords = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(userId, COMPLETED, start, end);
        return todayRecords.stream().mapToInt(r -> r.getTimeSpent() != null ? r.getTimeSpent() : 0).sum();
    }

    /** 平均测评得分（百分制）：取各知识域最近一次测评成绩的均值 */
    private double calculateAvgAssessmentScore(String userId) {
        try {
            Long userIdLong = Long.valueOf(userId);
            List<AssessmentRecord> all = assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(userIdLong);
            if (all.isEmpty()) return 0;
            Map<String, AssessmentRecord> latestBySubject = new LinkedHashMap<>();
            for (AssessmentRecord ar : all) {
                latestBySubject.putIfAbsent(ar.getSubject(), ar);
            }
            double sum = 0;
            for (AssessmentRecord ar : latestBySubject.values()) {
                if (ar.getTotal() != null && ar.getTotal() > 0 && ar.getScore() != null) {
                    sum += ar.getScore() * 100.0 / ar.getTotal();
                }
            }
            return latestBySubject.isEmpty() ? 0 : sum / latestBySubject.size();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== 学习曲线 ====================

    /**
     * 学习曲线：按时间范围聚合学习时长与掌握度
     * range=7/30 按天分组；range=90/all 按周分组；无数据的时间点补 0
     */
    public Map<String, Object> getCurve(String userId, String range) {
        String r = range == null || range.isBlank() ? "30" : range.trim();
        int days = switch (r) {
            case "7" -> 7;
            case "90" -> 90;
            case "all" -> 3650;
            default -> 30;
        };
        boolean byWeek = "90".equals(r) || "all".equals(r);

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(days - 1L).atStartOfDay();

        // 已完成的真实学习记录（时间范围内）
        List<LearningRecord> records = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(userId, COMPLETED, start, LocalDateTime.now());

        // 时间范围内的测评（用于掌握度）
        List<AssessmentRecord> assessments = new ArrayList<>();
        try {
            Long userIdLong = Long.valueOf(userId);
            assessments = assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(userIdLong).stream()
                    .filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isBefore(start))
                    .toList();
        } catch (NumberFormatException ignored) {
        }

        // 构建分组键（周起始日 或 天）
        TreeMap<LocalDate, List<LearningRecord>> recordByBucket = new TreeMap<>();
        for (LearningRecord rec : records) {
            LocalDate key = byWeek ? rec.getCompletedAt().toLocalDate()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    : rec.getCompletedAt().toLocalDate();
            recordByBucket.computeIfAbsent(key, k -> new ArrayList<>()).add(rec);
        }
        TreeMap<LocalDate, List<AssessmentRecord>> assessByBucket = new TreeMap<>();
        for (AssessmentRecord ar : assessments) {
            LocalDate key = byWeek ? ar.getCreatedAt().toLocalDate()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    : ar.getCreatedAt().toLocalDate();
            assessByBucket.computeIfAbsent(key, k -> new ArrayList<>()).add(ar);
        }

        // 生成完整时间点序列（含无数据桶）
        List<String> labels = new ArrayList<>();
        List<Double> hours = new ArrayList<>();
        List<Integer> mastery = new ArrayList<>();
        LocalDate cursor = start.toLocalDate();
        int maxPoints = byWeek ? 200 : days;
        int points = 0;
        while (!cursor.isAfter(today) && points < maxPoints) {
            List<LearningRecord> bucketRecords = recordByBucket.getOrDefault(cursor, Collections.emptyList());
            double weekHours = bucketRecords.stream()
                    .mapToDouble(rec -> (rec.getTimeSpent() != null ? rec.getTimeSpent() : 0) / 60.0)
                    .sum();
            hours.add(Math.round(weekHours * 10.0) / 10.0);

            // 掌握度：优先该时段测评均值，兜底学习记录掌握度均值
            double bucketMastery = bucketMastery(assessByBucket.getOrDefault(cursor, Collections.emptyList()),
                    bucketRecords);
            mastery.add((int) Math.round(bucketMastery));

            labels.add(byWeek
                    ? String.format("%d-%02d", cursor.getMonthValue(), cursor.getDayOfMonth())
                    : String.format("%d-%02d", cursor.getMonthValue(), cursor.getDayOfMonth()));
            cursor = byWeek ? cursor.plusWeeks(1) : cursor.plusDays(1);
            points++;
        }

        return Map.of(
                "range", r,
                "byWeek", byWeek,
                "labels", labels,
                "hours", hours,
                "mastery", mastery
        );
    }

    private double bucketMastery(List<AssessmentRecord> assessments, List<LearningRecord> records) {
        if (!assessments.isEmpty()) {
            double sum = 0;
            int count = 0;
            for (AssessmentRecord ar : assessments) {
                if (ar.getTotal() != null && ar.getTotal() > 0 && ar.getScore() != null) {
                    sum += ar.getScore() * 100.0 / ar.getTotal();
                    count++;
                }
            }
            if (count > 0) return sum / count;
        }
        if (!records.isEmpty()) {
            double sum = 0;
            int count = 0;
            for (LearningRecord rec : records) {
                if (rec.getMasteryLevel() != null) {
                    sum += normalizeMastery(rec.getMasteryLevel());
                    count++;
                }
            }
            if (count > 0) return sum / count;
        }
        return 0;
    }

    // ==================== 能力矩阵 ====================

    /**
     * 能力矩阵：从测评科目（subject）与学习记录知识域（knowledge_nodes.category）聚合，
     * 返回 { name, mastery, level, pathId, recordCount }，按掌握度降序
     */
    public List<Map<String, Object>> getCompetency(String userId) {
        Map<String, DomainStat> domainMap = new LinkedHashMap<>();

        // 数据源1：测评记录（subject 作为知识域，取最近一次成绩）
        try {
            Long userIdLong = Long.valueOf(userId);
            List<AssessmentRecord> assessments = assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(userIdLong);
            Map<String, AssessmentRecord> latestBySubject = new LinkedHashMap<>();
            for (AssessmentRecord ar : assessments) {
                latestBySubject.putIfAbsent(ar.getSubject(), ar);
            }
            latestBySubject.forEach((subject, ar) -> {
                DomainStat stat = domainMap.computeIfAbsent(subject, DomainStat::new);
                if (ar.getTotal() != null && ar.getTotal() > 0 && ar.getScore() != null) {
                    stat.addMastery(ar.getScore() * 100.0 / ar.getTotal(), true);
                }
            });
        } catch (NumberFormatException ignored) {
        }

        // 数据源2：学习记录 → 知识节点 category（掌握度取 mastery 均值）
        List<LearningRecord> records = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtIsNotNullOrderByCompletedAtDesc(userId, COMPLETED);
        Map<String, KnowledgeNode> nodeCache = new HashMap<>();
        records.forEach(rec -> {
            String category = resolveNodeCategory(rec.getNodeId(), nodeCache);
            if (category == null || category.isBlank()) return;
            DomainStat stat = domainMap.computeIfAbsent(category, DomainStat::new);
            stat.addRecord(rec.getPathId());
            if (rec.getMasteryLevel() != null) {
                stat.addMastery(normalizeMastery(rec.getMasteryLevel()), false);
            }
        });

        List<Map<String, Object>> result = domainMap.values().stream()
                .filter(stat -> stat.masterySum > 0 || stat.recordCount > 0)
                .sorted(Comparator.comparingDouble(DomainStat::average).reversed())
                .map(stat -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", stat.name);
                    m.put("mastery", (int) Math.round(stat.average()));
                    m.put("level", levelLabel(stat.average()));
                    m.put("pathId", stat.pathId == null ? "" : stat.pathId);
                    m.put("recordCount", stat.recordCount);
                    return m;
                })
                .collect(Collectors.toList());

        // 学习过但既无测评又无掌握度数据的知识域（如 pending 记录）不计入；无任何数据时返回空列表
        return result;
    }

    private String resolveNodeCategory(String nodeId, Map<String, KnowledgeNode> cache) {
        if (nodeId == null || nodeId.isBlank()) return null;
        try {
            KnowledgeNode node = cache.computeIfAbsent(nodeId, id -> knowledgeNodeRepository.findById(id).orElse(null));
            return node == null ? null : node.getCategory();
        } catch (Exception e) {
            return null;
        }
    }

    private String levelLabel(double mastery) {
        if (mastery >= 80) return "高级";
        if (mastery >= 60) return "中级";
        return "初级";
    }

    /** 知识域统计累加器 */
    private static class DomainStat {
        final String name;
        double masterySum = 0;
        int masteryCount = 0;
        int recordCount = 0;
        String pathId = null;

        DomainStat(String name) {
            this.name = name;
        }

        void addMastery(double v, boolean fromAssessment) {
            // 测评成绩权重更高（视为一次性成绩），学习记录掌握度按均值累计
            if (fromAssessment) {
                masterySum += v;
                masteryCount++;
            } else {
                masterySum += v;
                masteryCount++;
            }
        }

        void addRecord(String pathId) {
            recordCount++;
            if (this.pathId == null && pathId != null && !pathId.isBlank()) {
                this.pathId = pathId;
            }
        }

        double average() {
            return masteryCount > 0 ? masterySum / masteryCount : 0;
        }
    }

    // ==================== 学习记录列表 ====================

    /**
     * 学习记录列表：合并 daily_tasks（任务标题/状态/预计时长）与 learning_records（节点完成记录），
     * 支持状态筛选、关键词搜索、日期范围，按日期降序 + 内存分页
     */
    public Map<String, Object> getRecords(String userId, int page, int size, String status, String keyword,
                                          String startDate, String endDate) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> all = new ArrayList<>();

        // 数据源1：每日任务（daily_tasks）
        try {
            List<DailyTask> tasks = dailyTaskRepository.findByUserIdOrderByTaskDateDesc(userId);
            tasks.forEach(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", "task_" + t.getId());
                m.put("date", t.getTaskDate() != null ? t.getTaskDate().toString() : "");
                m.put("title", t.getTitle());
                m.put("duration", t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 0);
                m.put("status", normalizeTaskStatus(t.getStatus()));
                m.put("type", t.getType());
                m.put("pathId", t.getPathId());
                m.put("source", "task");
                all.add(m);
            });
        } catch (Exception e) {
            log.warn("[ProgressStats] 每日任务加载失败: {}", e.getMessage());
        }

        // 数据源2：学习记录（learning_records 已完成项，标题取知识节点名称）
        try {
            List<LearningRecord> records = learningRecordRepository
                    .findByUserIdAndStatusAndCompletedAtIsNotNullOrderByCompletedAtDesc(userId, COMPLETED);
            Map<String, KnowledgeNode> nodeCache = new HashMap<>();
            records.forEach(r -> {
                String nodeName = null;
                if (r.getNodeId() != null) {
                    KnowledgeNode node = nodeCache.computeIfAbsent(r.getNodeId(),
                            id -> knowledgeNodeRepository.findById(id).orElse(null));
                    if (node != null) nodeName = node.getName();
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", "record_" + r.getId());
                m.put("date", r.getCompletedAt() != null ? r.getCompletedAt().toLocalDate().toString() : "");
                m.put("title", nodeName != null ? "完成「" + nodeName + "」" : "完成学习任务");
                m.put("duration", r.getTimeSpent() != null ? r.getTimeSpent() : 0);
                m.put("status", "completed");
                m.put("type", r.getNodeType() != null ? r.getNodeType() : "learn");
                m.put("pathId", r.getPathId());
                m.put("source", "record");
                all.add(m);
            });
        } catch (Exception e) {
            log.warn("[ProgressStats] 学习记录加载失败: {}", e.getMessage());
        }

        // 筛选：状态 / 关键词 / 日期范围
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        List<Map<String, Object>> filtered = all.stream()
                .filter(m -> {
                    if (status != null && !status.isBlank() && !"all".equals(status)
                            && !status.equals(m.get("status"))) {
                        return false;
                    }
                    if (!kw.isEmpty()) {
                        String title = String.valueOf(m.getOrDefault("title", "")).toLowerCase();
                        if (!title.contains(kw)) return false;
                    }
                    String dateStr = String.valueOf(m.getOrDefault("date", ""));
                    if (start != null && (dateStr.isEmpty() || start.isAfter(LocalDate.parse(dateStr)))) return false;
                    if (end != null && (dateStr.isEmpty() || end.isBefore(LocalDate.parse(dateStr)))) return false;
                    return true;
                })
                .sorted((a, b) -> String.valueOf(b.get("date")).compareTo(String.valueOf(a.get("date"))))
                .collect(Collectors.toList());

        int total = filtered.size();
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);
        List<Map<String, Object>> pageList = from < to ? new ArrayList<>(filtered.subList(from, to)) : Collections.emptyList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        result.put("page", p);
        result.put("size", s);
        result.put("totalPages", (total + s - 1) / s);
        return result;
    }

    private String normalizeTaskStatus(String status) {
        if (status == null) return "pending";
        return switch (status.toLowerCase()) {
            case "completed" -> "completed";
            case "skipped" -> "skipped";
            case "in_progress", "inprogress" -> "in_progress";
            default -> "pending";
        };
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
