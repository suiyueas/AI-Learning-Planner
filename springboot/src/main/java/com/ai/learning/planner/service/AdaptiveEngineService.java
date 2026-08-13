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
    private final KnowledgeNodeRepository knowledgeNodeRepository;
    private final ResourceRepository resourceRepository;
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

    /**
     * 学习效率提升：近 30 天 vs 前 30 天完成节点数增长率；
     * 前段无完成节点时对比学习时长，仍无基线则返回 0。
     */
    private int calculateEfficiencyImprovement(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentStart = now.minusDays(30);
        LocalDateTime prevStart = now.minusDays(60);

        long recentCount = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(userId, COMPLETED, recentStart, now).size();
        long prevCount = learningRecordRepository
                .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(userId, COMPLETED, prevStart, recentStart).size();

        double prev = prevCount;
        double recent = recentCount;
        // 前段无完成节点：改用学习时长对比
        if (prevCount == 0 && recentCount > 0) {
            int recentMinutes = learningRecordRepository
                    .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(userId, COMPLETED, recentStart, now).stream()
                    .mapToInt(r -> r.getTimeSpent() != null ? r.getTimeSpent() : 0).sum();
            int prevMinutes = learningRecordRepository
                    .findByUserIdAndStatusAndCompletedAtBetweenOrderByCompletedAtAsc(userId, COMPLETED, prevStart, recentStart).stream()
                    .mapToInt(r -> r.getTimeSpent() != null ? r.getTimeSpent() : 0).sum();
            if (prevMinutes > 0) {
                prev = prevMinutes;
                recent = recentMinutes;
            }
        }
        if (prev <= 0 || recent <= 0) return 0;
        return (int) Math.round((recent - prev) / prev * 100);
    }

    /**
     * 知识掌握率：各科目最近一次测评成绩的均值（百分制），
     * 无测评数据时兜底学习记录掌握度均值。
     */
    private int calculateKnowledgeMastery(String userId) {
        try {
            Long userIdLong = Long.valueOf(userId);
            List<AssessmentRecord> all = assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(userIdLong);
            if (!all.isEmpty()) {
                Map<String, AssessmentRecord> latestBySubject = new LinkedHashMap<>();
                for (AssessmentRecord ar : all) {
                    latestBySubject.putIfAbsent(ar.getSubject(), ar);
                }
                double sum = 0;
                int count = 0;
                for (AssessmentRecord ar : latestBySubject.values()) {
                    if (ar.getTotal() != null && ar.getTotal() > 0 && ar.getScore() != null) {
                        sum += ar.getScore() * 100.0 / ar.getTotal();
                        count++;
                    }
                }
                if (count > 0) return (int) Math.round(sum / count);
            }
        } catch (NumberFormatException ignored) {
        }
        Float avgMastery = learningRecordRepository.avgMasteryLevelByUserId(userId);
        return avgMastery != null ? (int) Math.round(normalizeMastery(avgMastery)) : 0;
    }

    /**
     * 归因分析：按调整类型统计占比，将效率提升按占比分摊到各机制。
     * 仅当存在调整记录且效率提升为正时返回。
     */
    private List<Map<String, Object>> buildAttribution(String userId, int efficiency) {
        if (efficiency <= 0) return Collections.emptyList();
        List<AdaptiveAdjustment> adjustments = adjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (adjustments.isEmpty()) return Collections.emptyList();

        Map<String, Long> countByType = adjustments.stream()
                .collect(Collectors.groupingBy(AdaptiveAdjustment::getAdjustmentType, Collectors.counting()));
        long total = adjustments.size();
        List<Map<String, Object>> attribution = new ArrayList<>();
        // 固定顺序展示：复习插入/计划调整/资源推荐/进阶推荐/难度调整
        List<String> order = List.of("review_insert", "plan_adjust", "resource_recommend", "advance_recommend", "difficulty_adjust");
        for (String type : order) {
            Long count = countByType.get(type);
            if (count == null || count == 0) continue;
            int contribution = (int) Math.round((double) count / total * efficiency);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", type);
            m.put("typeLabel", TYPE_LABELS.getOrDefault(type, type));
            m.put("count", count);
            m.put("contribution", contribution);
            attribution.add(m);
        }
        return attribution;
    }

    // ==================== 调整历史 ====================

    /**
     * 调整历史分页（按时间降序），支持类型筛选，路径名称关联解析
     */
    public Map<String, Object> getAdjustments(String userId, int page, int size, String type) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);

        List<AdaptiveAdjustment> all = (type == null || type.isBlank() || "all".equals(type))
                ? adjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : adjustmentRepository.findByUserIdAndAdjustmentTypeOrderByCreatedAtDesc(userId, type);

        // 路径名称缓存（避免逐条查询）
        Map<String, String> pathNameCache = new HashMap<>();
        int total = all.size();
        int from = Math.min(p * s, total);
        int to = Math.min(from + s, total);
        List<Map<String, Object>> content = new ArrayList<>();
        for (AdaptiveAdjustment adj : all.subList(from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", adj.getId());
            m.put("type", adj.getAdjustmentType());
            m.put("typeLabel", TYPE_LABELS.getOrDefault(adj.getAdjustmentType(), adj.getAdjustmentType()));
            m.put("triggerReason", adj.getTriggerReason());
            m.put("detail", parseDetail(adj.getAdjustmentDetail()));
            m.put("effect", adj.getEffectMetric());
            m.put("createdAt", adj.getCreatedAt() != null ? adj.getCreatedAt().toString() : "");
            m.put("pathId", adj.getPathId());
            m.put("pathName", resolvePathName(adj.getPathId(), pathNameCache));
            content.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("page", p);
        result.put("size", s);
        result.put("totalPages", (total + s - 1) / s);
        return result;
    }

    /** 调整详情 JSON 解析（解析失败返回空 Map，前端兼容展示） */
    private Map<String, Object> parseDetail(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[AdaptiveEngine] 调整详情 JSON 解析失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String resolvePathName(String pathId, Map<String, String> cache) {
        if (pathId == null || pathId.isBlank()) return "";
        return cache.computeIfAbsent(pathId, id -> learningPathRepository.findById(id)
                .map(LearningPath::getName).orElse(""));
    }

    // ==================== 个性化推荐 ====================

    /**
     * 获取推荐列表：优先返回库中活跃推荐（pending/clicked），
     * 不足目标条数时触发规则化生成并落库。
     */
    public Map<String, Object> getRecommendations(String userId) {
        List<UserRecommendation> active = recommendationRepository
                .findByUserIdAndStatusInOrderByGeneratedAtDesc(userId, List.of("pending", "clicked"));

        // 生成每日一次：仅当当天尚无任何推荐记录时触发（避免重复落库）
        boolean generatedToday = recommendationRepository
                .findByUserIdAndStatusInOrderByGeneratedAtDesc(userId, List.of("pending", "clicked", "consumed"))
                .stream().anyMatch(r -> r.getGeneratedAt() != null
                        && r.getGeneratedAt().toLocalDate().equals(LocalDate.now()));
        if (!generatedToday && active.size() < RECOMMEND_TARGET) {
            try {
                active = generateRecommendations(userId);
            } catch (Exception e) {
                log.warn("[AdaptiveEngine] 推荐生成失败: {}", e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", active.stream().map(this::toRecommendationMap).collect(Collectors.toList()));
        result.put("totalElements", active.size());
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
     * ① 测评薄弱科目 → 匹配知识点/资源（resource 或 knowledge_block 推荐）
     * ② 用户薄弱项/兴趣关键词 → 匹配资源/知识点
     * ③ 活跃路径下一个未完成节点 → 路径推荐
     */
    private List<UserRecommendation> generateRecommendations(String userId) {
        List<UserRecommendation> active = recommendationRepository
                .findByUserIdAndStatusInOrderByGeneratedAtDesc(userId, List.of("pending", "clicked"));
        Set<String> existingKeys = active.stream()
                .map(r -> r.getContentType() + ":" + r.getContentId())
                .collect(Collectors.toSet());
        List<UserRecommendation> generated = new ArrayList<>(active);

        // 知识节点/资源全量缓存（供关键词匹配）
        List<KnowledgeNode> nodes = knowledgeNodeRepository.findAll();
        List<Resource> resources = resourceRepository.findAll();
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
            // 匹配知识点（分类包含科目名）
            KnowledgeNode matched = nodes.stream()
                    .filter(n -> n.getCategory() != null && n.getCategory().contains(subject))
                    .findFirst().orElse(null);
            if (matched == null) {
                matched = nodes.stream()
                        .filter(n -> n.getName() != null && (n.getName().contains(subject) || subject.contains(n.getName())))
                        .findFirst().orElse(null);
            }
            if (matched != null && existingKeys.add("knowledge_block:" + matched.getId())) {
                generated.add(buildRecommendation(userId, matched.getId(), "knowledge_block",
                        "补强「" + matched.getName() + "」",
                        "测评显示《" + subject + "》掌握度偏低，建议优先学习该知识点", 0.9f, "high"));
            }
            // 匹配资源
            if (generated.size() < RECOMMEND_TARGET) {
                Resource matchedRes = resources.stream()
                        .filter(r -> r.getTitle() != null && (r.getTitle().contains(subject) || subject.contains(r.getTitle())))
                        .findFirst().orElse(null);
                if (matchedRes != null && existingKeys.add("resource:" + matchedRes.getId())) {
                    generated.add(buildRecommendation(userId, matchedRes.getId(), "resource",
                            "资源推荐：" + matchedRes.getTitle(),
                            "针对《" + subject + "》薄弱环节推荐该学习资源", 0.85f, "high"));
                }
            }
        }

        // ---- 数据源②：用户薄弱项 / 兴趣关键词 ----
        List<String> keywords = extractKeywords(user);
        for (String kw : keywords) {
            if (generated.size() >= RECOMMEND_TARGET) break;
            KnowledgeNode node = nodes.stream()
                    .filter(n -> n.getName() != null && n.getName().contains(kw))
                    .findFirst().orElse(null);
            if (node != null && existingKeys.add("knowledge_block:" + node.getId())) {
                generated.add(buildRecommendation(userId, node.getId(), "knowledge_block",
                        "掌握「" + node.getName() + "」",
                        "与你关注的方向「" + kw + "」高度匹配", 0.8f, "normal"));
                continue;
            }
            Resource res = resources.stream()
                    .filter(r -> r.getTitle() != null && r.getTitle().contains(kw))
                    .findFirst().orElse(null);
            if (res != null && existingKeys.add("resource:" + res.getId())) {
                generated.add(buildRecommendation(userId, res.getId(), "resource",
                        "资源推荐：" + res.getTitle(),
                        "基于你的兴趣方向「" + kw + "」推荐", 0.75f, "normal"));
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
