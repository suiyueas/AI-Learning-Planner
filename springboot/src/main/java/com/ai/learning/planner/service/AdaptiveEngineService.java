package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.*;
import com.ai.learning.planner.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自适应引擎服务
 * 聚合真实学习数据支撑自适应引擎页面：
 * 1. 引擎状态（策略/调整次数/效率提升/知识掌握率）
 * 2. 自适应调整历史（adaptive_adjustment 表）
 * 3. 个性化推荐（规则化引擎：测评薄弱科目 + 用户薄弱项/兴趣 + 路径进度）
 * 4. 推荐点击/消费状态流转
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveEngineService {

    private final AdaptiveAdjustmentRepository adjustmentRepository;
    private final UserRecommendationRepository recommendationRepository;
    private final LearningRecordRepository learningRecordRepository;
    private final AssessmentRecordRepository assessmentRecordRepository;
    private final LearningPathRepository learningPathRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String COMPLETED = "completed";
    /** 推荐列表期望条数 */
    private static final int RECOMMEND_TARGET = 5;
    /** 低分判定阈值（%）：最近测评低于该值视为薄弱科目 */
    private static final double WEAK_SCORE_THRESHOLD = 70.0;

    /** 调整类型中文名映射 */
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "review_insert", "复习插入",
            "advance_recommend", "进阶推荐",
            "plan_adjust", "计划调整",
            "resource_recommend", "资源推荐",
            "difficulty_adjust", "难度调整"
    );

    // ==================== 引擎状态聚合 ====================

    /**
     * 聚合自适应引擎状态：策略/调整次数/效率提升/掌握率/归因分析
     */
    public Map<String, Object> getStatus(String userId) {
        User user = findUser(userId);
        boolean enabled = user == null || !Boolean.FALSE.equals(user.getInterventionEnabled());
        long totalAdjustments = adjustmentRepository.countByUserId(userId);
        int efficiency = calculateEfficiencyImprovement(userId);
        int mastery = calculateKnowledgeMastery(userId);
        AdaptiveAdjustment latest = adjustmentRepository.findTopByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", enabled ? "running" : "paused");
        result.put("currentStrategy", buildStrategyText(user));
        result.put("totalAdjustments", totalAdjustments);
        result.put("efficiencyImprovement", efficiency);
        result.put("knowledgeMastery", mastery);
        result.put("lastAdjustmentAt", latest != null && latest.getCreatedAt() != null
                ? latest.getCreatedAt().toString() : null);
        result.put("attribution", buildAttribution(userId, efficiency));
        return result;
    }

    /** 动态策略描述：学习风格 + 自适应开关 */
    private String buildStrategyText(User user) {
        if (user == null) return "个性化推荐 + 难度自适应";
        String styleLabel = switch (user.getLearningStyle() != null ? user.getLearningStyle() : "") {
            case "visual" -> "视觉型";
            case "auditory" -> "听觉型";
            case "reading" -> "阅读型";
            case "kinesthetic" -> "动觉型";
            default -> "个性化";
        };
        String base = styleLabel + "推荐 + 难度自适应";
        return Boolean.FALSE.equals(user.getInterventionEnabled()) ? base + "（已暂停）" : base;
    }

    /** 掌握度归一化：mastery_level 为 0-5 分制时按 20 倍放大，兼容 0-100 分制 */
    private double normalizeMastery(Float masteryLevel) {
        if (masteryLevel == null) return 0;
        double v = masteryLevel;
        return v <= 5 ? Math.min(v * 20, 100) : Math.min(v, 100);
    }

    private int calculateKnowledgeMastery(String userId) {
        List<LearningRecord> records = learningRecordRepository.findByUserId(userId);
        if (records.isEmpty()) return 0;
        double avg = records.stream()
                .mapToDouble(r -> normalizeMastery(r.getMasteryLevel()))
                .average().orElse(0);
        return (int) Math.round(avg);
    }

    private int calculateEfficiencyImprovement(String userId) {
        List<LearningRecord> records = learningRecordRepository.findByUserId(userId);
        if (records.size() < 2) return 0;
        int half = records.size() / 2;
        double early = records.subList(0, half).stream()
                .mapToDouble(r -> normalizeMastery(r.getMasteryLevel())).average().orElse(0);
        double late = records.subList(half, records.size()).stream()
                .mapToDouble(r -> normalizeMastery(r.getMasteryLevel())).average().orElse(0);
        if (early == 0) return 0;
        return (int) Math.round((late - early) / early * 100);
    }

    private String buildAttribution(String userId, int efficiency) {
        long total = learningRecordRepository.findByUserId(userId).size();
        long completed = learningRecordRepository.countByUserIdAndStatus(userId, COMPLETED);
        if (total == 0) return "暂无学习数据，无法归因";
        String base = String.format("基于 %d 条学习记录（完成率 %d%%）", total, (int)(completed * 100.0 / total));
        if (efficiency > 0) return base + "，效率提升显著";
        if (efficiency < 0) return base + "，建议调整学习策略";
        return base + "，效率保持稳定";
    }

    // ==================== 调整历史 ====================

    public Map<String, Object> getAdjustments(String userId, int page, int size, String type) {
        List<AdaptiveAdjustment> all = adjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (type != null && !type.isBlank()) {
            all = all.stream().filter(a -> type.equals(a.getAdjustmentType())).collect(Collectors.toList());
        }
        int total = all.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<Map<String, Object>> items = all.subList(from, to).stream()
                .map(this::toAdjustmentMap).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private Map<String, Object> toAdjustmentMap(AdaptiveAdjustment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("type", a.getAdjustmentType());
        m.put("typeLabel", TYPE_LABELS.getOrDefault(a.getAdjustmentType(), a.getAdjustmentType()));
        m.put("pathId", a.getPathId());
        m.put("triggerReason", a.getTriggerReason());
        m.put("detail", a.getAdjustmentDetail());
        m.put("effect", a.getEffectMetric());
        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
        return m;
    }

    // ==================== 个性化推荐 ====================

    public Map<String, Object> getRecommendations(String userId) {
        List<UserRecommendation> recs = generateRecommendations(userId);
        long pending = recs.stream().filter(r -> "pending".equals(r.getStatus())).count();
        long clicked = recs.stream().filter(r -> "clicked".equals(r.getStatus())).count();
        long consumed = recs.stream().filter(r -> "consumed".equals(r.getStatus())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", recs.stream().map(this::toRecommendationMap).collect(Collectors.toList()));
        result.put("stats", Map.of("pending", pending, "clicked", clicked, "consumed", consumed));
        return result;
    }

    private Map<String, Object> toRecommendationMap(UserRecommendation rec) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rec.getId());
        m.put("type", rec.getContentType());
        m.put("title", rec.getTitle());
        m.put("description", rec.getDescription());
        m.put("matchScore", rec.getMatchScore() != null ? rec.getMatchScore() : 0f);
        m.put("matchReason", rec.getMatchReason());
        m.put("priority", rec.getPriority());
        m.put("status", rec.getStatus());
        m.put("generatedAt", rec.getGeneratedAt() != null ? rec.getGeneratedAt().toString() : "");
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("contentType", rec.getContentType());
        target.put("contentId", rec.getContentId());
        target.put("pathId", rec.getPathId());
        m.put("target", target);
        return m;
    }

    /**
     * 规则化推荐引擎：
     * ① 测评薄弱科目 → 匹配知识文档
     * ② 用户薄弱项/兴趣关键词 → 匹配知识文档
     * ③ 活跃路径下一个未完成节点 → 路径推荐
     */
    private List<UserRecommendation> generateRecommendations(String userId) {
        List<UserRecommendation> active = recommendationRepository
                .findByUserIdAndStatusInOrderByGeneratedAtDesc(userId, List.of("pending", "clicked"));
        Set<String> existingKeys = active.stream()
                .map(r -> r.getContentType() + ":" + r.getContentId())
                .collect(Collectors.toSet());
        List<UserRecommendation> generated = new ArrayList<>(active);

        // 知识文档全量缓存（供关键词匹配）
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findAll();
        User user = findUser(userId);
        List<LearningPath> myPaths = learningPathRepository.findByUserId(userId);

        // ---- 数据源①：测评薄弱科目 ----
        List<AssessmentRecord> assessments = listAssessments(userId);
        Set<String> weakSubjects = new LinkedHashSet<>();
        Map<String, AssessmentRecord> latestBySubject = new LinkedHashMap<>();
        for (AssessmentRecord ar : assessments) {
            latestBySubject.putIfAbsent(ar.getSubject(), ar);
        }
        latestBySubject.forEach((subject, ar) -> {
            if (ar.getTotal() != null && ar.getTotal() > 0 && ar.getScore() != null
                    && ar.getScore() * 100.0 / ar.getTotal() < WEAK_SCORE_THRESHOLD) {
                weakSubjects.add(subject);
            }
        });
        for (String subject : weakSubjects) {
            if (generated.size() >= RECOMMEND_TARGET) break;
            // 匹配知识文档
            KnowledgeDocument matchedDoc = documents.stream()
                    .filter(d -> d.getTitle() != null && (d.getTitle().contains(subject) || subject.contains(d.getTitle())))
                    .findFirst().orElse(null);
            if (matchedDoc != null && existingKeys.add("knowledge_document:" + matchedDoc.getId())) {
                generated.add(buildRecommendation(userId, matchedDoc.getId(), "knowledge_document",
                        "补强「" + matchedDoc.getTitle() + "」",
                        "测评显示《" + subject + "》掌握度偏低，建议优先学习该文档", 0.9f, "high"));
            }
        }

        // ---- 数据源②：用户薄弱项 / 兴趣关键词 ----
        List<String> keywords = extractKeywords(user);
        for (String kw : keywords) {
            if (generated.size() >= RECOMMEND_TARGET) break;
            KnowledgeDocument doc = documents.stream()
                    .filter(d -> d.getTitle() != null && d.getTitle().contains(kw))
                    .findFirst().orElse(null);
            if (doc != null && existingKeys.add("knowledge_document:" + doc.getId())) {
                generated.add(buildRecommendation(userId, doc.getId(), "knowledge_document",
                        "学习「" + doc.getTitle() + "」",
                        "与你关注的方向「" + kw + "」高度匹配", 0.8f, "normal"));
            }
        }

        // ---- 数据源③：活跃路径下一未完成节点 ----
        if (generated.size() < RECOMMEND_TARGET) {
            for (LearningPath path : myPaths) {
                if (Boolean.FALSE.equals(path.getIsActive()) || path.getNodes() == null) continue;
                String nextNodeName = findNextPendingNodeName(path);
                String key = "learning_path:" + path.getId();
                if (existingKeys.add(key)) {
                    generated.add(buildRecommendation(userId, path.getId(), "learning_path",
                            "继续「" + path.getName() + "」" + (nextNodeName != null ? "：" + nextNodeName : ""),
                            "基于你的学习进度，推荐继续完成当前路径", 0.85f, "high"));
                    break;
                }
            }
        }

        // 落库新生成的推荐
        List<UserRecommendation> saved = new ArrayList<>();
        for (UserRecommendation rec : generated) {
            if (rec.getId() != null) {
                saved.add(rec); // 已有记录
            } else {
                saved.add(recommendationRepository.save(rec));
            }
        }
        return saved;
    }

    private UserRecommendation buildRecommendation(String userId, String contentId, String contentType,
                                                   String title, String reason, float score, String priority) {
        return UserRecommendation.builder()
                .userId(userId)
                .contentType(contentType)
                .contentId(contentId)
                .title(title)
                .description(reason)
                .matchScore(score)
                .matchReason(reason)
                .priority(priority)
                .status("pending")
                .build();
    }

    /** 解析路径 nodes JSON，返回第一个未完成节点的名称 */
    private String findNextPendingNodeName(LearningPath path) {
        try {
            List<Map<String, Object>> nodes = objectMapper.readValue(path.getNodes(),
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> node : nodes) {
                Object status = node.get("status");
                if (status == null || "pending".equals(status.toString()) || "in_progress".equals(status.toString())) {
                    Object name = node.get("nodeName");
                    if (name != null && !name.toString().isBlank()) return name.toString();
                }
            }
        } catch (Exception e) {
            log.warn("[AdaptiveEngine] 路径节点解析失败: pathId={}, error={}", path.getId(), e.getMessage());
        }
        return null;
    }

    /** 提取用户薄弱项/兴趣关键词（逗号/顿号分隔） */
    private List<String> extractKeywords(User user) {
        Set<String> keywords = new LinkedHashSet<>();
        if (user == null) return new ArrayList<>();
        String weak = user.getWeakPoints();
        String interests = user.getInterests();
        for (String raw : List.of(weak, interests)) {
            if (raw == null || raw.isBlank()) continue;
            String normalized = raw.replace("[", "").replace("]", "").replace("\"", "");
            for (String part : normalized.split("[,，;；]")) {
                String kw = part.trim();
                if (kw.length() >= 2 && kw.length() <= 20) keywords.add(kw);
            }
        }
        return new ArrayList<>(keywords);
    }

    private List<AssessmentRecord> listAssessments(String userId) {
        try {
            return assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(Long.valueOf(userId));
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }

    private User findUser(String userId) {
        try {
            return userRepository.findById(Long.valueOf(userId)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 推荐状态流转 ====================

    /** 标记推荐为已点击（pending → clicked） */
    public boolean clickRecommendation(String userId, String id) {
        Optional<UserRecommendation> opt = recommendationRepository.findById(id);
        if (opt.isEmpty()) return false;
        UserRecommendation rec = opt.get();
        if (!rec.getUserId().equals(userId)) return false;
        if ("pending".equals(rec.getStatus())) {
            rec.setStatus("clicked");
            recommendationRepository.save(rec);
        }
        return true;
    }

    /** 标记推荐为已消费（→ consumed，记录消费时间） */
    public boolean consumeRecommendation(String userId, String id) {
        Optional<UserRecommendation> opt = recommendationRepository.findById(id);
        if (opt.isEmpty()) return false;
        UserRecommendation rec = opt.get();
        if (!rec.getUserId().equals(userId)) return false;
        if (!"consumed".equals(rec.getStatus())) {
            rec.setStatus("consumed");
            rec.setConsumedAt(LocalDateTime.now());
            recommendationRepository.save(rec);
        }
        return true;
    }

    /**
     * 完成任务联动消费：节点对应的推荐（content_id 匹配）或路径推荐（path_id 匹配）自动置为已消费。
     * 由 LearningProgressService.completeTask 调用。
     */
    public void markRecommendationsConsumed(String userId, String pathId, String nodeId) {
        try {
            List<String> statuses = List.of("pending", "clicked");
            List<UserRecommendation> matched = recommendationRepository
                    .findByUserIdAndStatusInAndContentIdOrUserIdAndStatusInAndPathId(
                            userId, statuses, nodeId, userId, statuses, pathId);
            for (UserRecommendation rec : matched) {
                rec.setStatus("consumed");
                rec.setConsumedAt(LocalDateTime.now());
                recommendationRepository.save(rec);
            }
            if (!matched.isEmpty()) {
                log.info("[AdaptiveEngine] 推荐消费联动: userId={}, nodeId={}, count={}", userId, nodeId, matched.size());
            }
        } catch (Exception e) {
            log.warn("[AdaptiveEngine] 推荐消费联动失败: {}", e.getMessage());
        }
    }

    /**
     * 写入自适应调整记录（供干预扫描等场景调用）。
     * 同一用户同一天同类型仅保留一条，避免重复。
     */
    public void recordAdjustment(String userId, String pathId, String type,
                                 String triggerReason, String detailJson, String effect) {
        try {
            List<AdaptiveAdjustment> existing = adjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            boolean hasToday = existing.stream().anyMatch(a -> type.equals(a.getAdjustmentType())
                    && a.getCreatedAt() != null && a.getCreatedAt().toLocalDate().equals(LocalDate.now()));
            if (hasToday) return;
            adjustmentRepository.save(AdaptiveAdjustment.builder()
                    .userId(userId)
                    .pathId(pathId != null && !pathId.isBlank() ? pathId : "-")
                    .adjustmentType(type)
                    .triggerReason(triggerReason)
                    .adjustmentDetail(detailJson)
                    .effectMetric(effect)
                    .build());
            log.info("[AdaptiveEngine] 自适应调整已记录: userId={}, type={}", userId, type);
        } catch (Exception e) {
            log.warn("[AdaptiveEngine] 调整记录写入失败: {}", e.getMessage());
        }
    }
}
