package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.DailyPlanDTO;
import com.ai.learning.planner.dto.WeekPreviewDTO;
import com.ai.learning.planner.entity.DailyTask;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.repository.DailyTaskRepository;
import com.ai.learning.planner.repository.LearningPathRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 每日任务服务
 * 管理每日学习任务的生成、获取和进度更新
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyTaskService {

    private final DailyTaskRepository dailyTaskRepository;
    private final LearningPathRepository learningPathRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.deepseek.api-key}")
    private String apiKey;

    @Value("${spring.ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_DAILY_MINUTES = 60; // 每天最多 60 分钟
    private static final int TASKS_PER_DAY = 3; // 每天生成 3 个任务

    /**
     * 校验路径存在且归属当前用户（统一对象级授权入口）
     */
    private LearningPath requireOwnedPath(String pathId, String userId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("学习路径不存在: " + pathId));
        if (!path.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该学习路径");
        }
        return path;
    }

    /**
     * 获取指定日期的任务，如果当天没有任务则自动生成
     */
    @Transactional
    public DailyPlanDTO getDailyTasks(String pathId, String userId, LocalDate date) {
        requireOwnedPath(pathId, userId);
        // 检查当天是否有任务
        List<DailyTask> existingTasks = dailyTaskRepository.findByPathIdAndTaskDateOrderBySortOrderAsc(pathId, date);

        if (!existingTasks.isEmpty()) {
            return toDailyPlanDTO(date, existingTasks);
        }

        // 当天没有任务，尝试自动生成
        generateTasksForDate(pathId, userId, date);

        List<DailyTask> tasks = dailyTaskRepository.findByPathIdAndTaskDateOrderBySortOrderAsc(pathId, date);
        return toDailyPlanDTO(date, tasks);
    }

    /**
     * 获取本周预览（周一 ~ 周日）
     */
    public WeekPreviewDTO getWeekPreview(String pathId, String userId, LocalDate date) {
        requireOwnedPath(pathId, userId);
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<DailyTask> weekTasks = dailyTaskRepository.findByPathIdAndTaskDateBetweenOrderByTaskDateAscSortOrderAsc(
                pathId, weekStart, weekEnd);

        // 如果本周还没有任务，尝试生成
        if (weekTasks.isEmpty()) {
            generateWeekTasks(pathId, userId, weekStart);
            weekTasks = dailyTaskRepository.findByPathIdAndTaskDateBetweenOrderByTaskDateAscSortOrderAsc(
                    pathId, weekStart, weekEnd);
        }

        Map<LocalDate, List<DailyTask>> dayMap = weekTasks.stream()
                .collect(Collectors.groupingBy(DailyTask::getTaskDate));

        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<WeekPreviewDTO.DaySummaryDTO> days = new ArrayList<>();
        int totalMinutes = 0;
        int totalCompletedTasks = 0;
        int totalTasks = 0;

        LocalDate cursor = weekStart;
        int dayIdx = 0;
        while (!cursor.isAfter(weekEnd)) {
            List<DailyTask> dayTasks = dayMap.getOrDefault(cursor, Collections.emptyList());
            String topic = dayTasks.stream()
                    .map(DailyTask::getTitle)
                    .findFirst()
                    .orElse("休息日 / 自由学习");
            int completedCount = (int) dayTasks.stream().filter(t -> "completed".equals(t.getStatus())).count();
            int dayMinutes = dayTasks.stream().mapToInt(t -> t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 0).sum();

            if (!dayTasks.isEmpty()) {
                totalMinutes += dayMinutes;
                totalCompletedTasks += completedCount;
                totalTasks += dayTasks.size();
            }

            days.add(WeekPreviewDTO.DaySummaryDTO.builder()
                    .date(cursor.format(DATE_FMT))
                    .dayOfWeek(dayNames[dayIdx % 7])
                    .totalMinutes(dayMinutes)
                    .completedTasks(completedCount)
                    .totalTasks(dayTasks.size())
                    .topic(topic)
                    .build());

            cursor = cursor.plusDays(1);
            dayIdx++;
        }

        return WeekPreviewDTO.builder()
                .weekStart(weekStart.format(DATE_FMT))
                .weekEnd(weekEnd.format(DATE_FMT))
                .totalMinutes(totalMinutes)
                .totalCompletedTasks(totalCompletedTasks)
                .totalTasks(totalTasks)
                .days(days)
                .build();
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public DailyPlanDTO.DailyTaskDTO updateTaskStatus(String taskId, String status, String userId) {
        DailyTask task = dailyTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        // 归属校验：只能更新自己的任务
        if (!userId.equals(task.getUserId())) {
            throw new RuntimeException("无权操作该任务");
        }
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        dailyTaskRepository.save(task);
        return toDailyTaskDTO(task);
    }

    /**
     * 为指定路径生成一周的任务
     */
    @Transactional
    public void generateWeekTasks(String pathId, String userId, LocalDate weekStart) {
        // 删除本周已存在的任务
        LocalDate weekEnd = weekStart.plusDays(6);
        List<DailyTask> existing = dailyTaskRepository.findByPathIdAndTaskDateBetweenOrderByTaskDateAscSortOrderAsc(
                pathId, weekStart, weekEnd);
        if (!existing.isEmpty()) {
            log.info("本周已存在任务: pathId={}, weekStart={}, count={}", pathId, weekStart, existing.size());
            return;
        }

        // 从路径节点获取任务队列
        List<Map<String, Object>> taskQueue = buildTaskQueueFromPath(pathId);

        // 如果节点为空，使用降级方案：从路径元数据生成默认任务
        if (taskQueue.isEmpty()) {
            log.info("路径节点为空，从路径元数据生成默认每日任务: pathId={}", pathId);
            taskQueue = buildDefaultTaskQueue(pathId);
        }

        if (taskQueue.isEmpty()) {
            log.warn("无法生成每日任务，路径无可用数据: pathId={}", pathId);
            return;
        }

        Set<String> existingTitles = new HashSet<>(dailyTaskRepository.findDistinctTitlesByPathId(pathId));
        if (!existingTitles.isEmpty()) {
            taskQueue = taskQueue.stream()
                    .filter(task -> {
                        String title = task.get("title") != null ? (String) task.get("title") 
                                : (task.get("name") != null ? (String) task.get("name") : "");
                        return !existingTitles.contains(title);
                    })
                    .collect(Collectors.toList());
            log.info("去重后剩余任务数: pathId={}, original={}, remaining={}", pathId, existingTitles.size(), taskQueue.size());
        }

        if (taskQueue.isEmpty()) {
            log.warn("所有任务已存在，无需生成新任务: pathId={}", pathId);
            return;
        }

        LocalDate cursor = weekStart;
        int taskIndex = 0;
        int dayOffset = 0;

        while (!cursor.isAfter(weekEnd) && taskIndex < taskQueue.size()) {
            List<Map<String, Object>> dayTasks = new ArrayList<>();
            int dayMinutes = 0;

            // 每天分配 TASKS_PER_DAY 个任务或直到达到 MAX_DAILY_MINUTES
            while (taskIndex < taskQueue.size() && dayTasks.size() < TASKS_PER_DAY) {
                Map<String, Object> node = taskQueue.get(taskIndex);
                int estimatedMin = node.get("estimatedMinutes") != null
                        ? ((Number) node.get("estimatedMinutes")).intValue()
                        : 20;
                if (dayMinutes + estimatedMin > MAX_DAILY_MINUTES && !dayTasks.isEmpty()) {
                    break;
                }
                dayTasks.add(node);
                dayMinutes += estimatedMin;
                taskIndex++;
            }

            // 如果当天没分配到任务但还有节点，添加一个
            if (dayTasks.isEmpty() && taskIndex < taskQueue.size()) {
                dayTasks.add(taskQueue.get(taskIndex));
                taskIndex++;
            }

            // 生成当天任务
            for (int i = 0; i < dayTasks.size(); i++) {
                Map<String, Object> node = dayTasks.get(i);
                String title = node.get("title") != null ? (String) node.get("title")
                        : (node.get("name") != null ? (String) node.get("name") : "学习任务");
                String nodeId = node.get("id") != null ? (String) node.get("id")
                        : UUID.randomUUID().toString();
                int estMin = node.get("estimatedMinutes") != null
                        ? ((Number) node.get("estimatedMinutes")).intValue()
                        : 20;

                DailyTask task = DailyTask.builder()
                        .id(UUID.randomUUID().toString())
                        .pathId(pathId)
                        .userId(userId)
                        .taskDate(cursor)
                        .title(title)
                        .type(inferTaskType(node))
                        .estimatedMinutes(estMin)
                        .status("pending")
                        .description(node.get("description") != null ? (String) node.get("description") : "")
                        .sourceNodeId(nodeId)
                        .sortOrder(i + 1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                dailyTaskRepository.save(task);
            }

            cursor = cursor.plusDays(1);
            dayOffset++;
        }

        log.info("已生成每日任务: pathId={}, start={}, days={}, tasks={}",
                pathId, weekStart, dayOffset, taskIndex);
    }

    /**
     * 为指定日期生成任务（单日）
     */
    @Transactional
    public void generateTasksForDate(String pathId, String userId, LocalDate date) {
        // 先尝试生成当周的任务
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        generateWeekTasks(pathId, userId, weekStart);
    }

    /**
     * 根据路径手动重新生成任务
     */
    @Transactional
    public void regenerateTasks(String pathId, String userId) {
        requireOwnedPath(pathId, userId);
        // 删除所有已存在的每日任务
        dailyTaskRepository.deleteByPathId(pathId);
        // 重新生成本周任务
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        generateWeekTasks(pathId, userId, weekStart);
    }

    /**
     * 获取今日累计完成数
     */
    public long countCompletedToday(String userId) {
        return dailyTaskRepository.countByUserIdAndTaskDateAndStatus(userId, LocalDate.now(), "completed");
    }

    // ==================== 内部方法 ====================

    /**
     * 降级方案：当路径节点为空时，调用 DeepSeek AI 生成每周学习计划
     * AI 根据路径名称和描述智能生成每日任务，deepseek-v4-flash
     */
    private List<Map<String, Object>> buildDefaultTaskQueue(String pathId) {
        Optional<LearningPath> pathOpt = learningPathRepository.findById(pathId);
        if (pathOpt.isEmpty()) {
            return new ArrayList<>();
        }
        LearningPath path = pathOpt.get();
        String name = path.getName() != null ? path.getName() : "学习";
        String desc = path.getDescription() != null ? path.getDescription() : "";
        String goal = extractTrainingGoal(name, desc);

        String prompt = String.format(
            "你是一名专业的AI学习规划师。根据以下学习目标，生成一份7天的每日学习计划。\n\n" +
            "学习目标：%s\n" +
            "路径描述：%s\n\n" +
            "要求：\n" +
            "1. 每天生成2个学习任务（一个主要学习任务+一个练习/复习任务）\n" +
            "2. 每个任务包含：标题、描述、预估分钟数(20-40分钟)、任务类型(read/video/practice/review)\n" +
            "3. 任务之间应有递进关系：基础概念 → 核心操作 → 实践 → 进阶 → 综合 → 复习 → 项目\n" +
            "4. 标题要具体、可执行，例如\"Python变量与数据类型\"而不是\"学习Python\"\n" +
            "5. 必须用严格的JSON数组格式返回，不要包含markdown代码块标记或其他文字\n\n" +
            "返回格式（JSON数组，每天2个任务，共14个）：\n" +
            "[\n" +
            "  {\n" +
            "    \"title\": \"任务标题\",\n" +
            "    \"description\": \"任务详细描述\",\n" +
            "    \"estimatedMinutes\": 30,\n" +
            "    \"type\": \"read\",\n" +
            "    \"phaseTitle\": \"学习阶段\",\n" +
            "    \"weekNumber\": 1\n" +
            "  }\n" +
            "]",
            goal, desc
        );

        try {
            String response = callDeepSeek(prompt);
            List<Map<String, Object>> aiTasks = parseAiTasks(response, goal);
            if (!aiTasks.isEmpty()) {
                log.info("AI成功生成 {} 个每日任务 (目标: {})", aiTasks.size(), goal);
                return aiTasks;
            }
        } catch (Exception e) {
            log.error("AI生成任务失败，使用极简降级方案: {}", e.getMessage());
        }

        // AI 失败时极简降级
        return buildMinimalFallback(goal);
    }

    /**
     * 从路径名称和描述中提取训练目标
     */
    private String extractTrainingGoal(String name, String desc) {
        if (desc != null && desc.contains("目标：")) {
            int start = desc.indexOf("目标：") + 3;
            int end = desc.indexOf("|", start);
            if (end > start) {
                String goal = desc.substring(start, end).trim();
                if (!goal.isEmpty()) return goal;
            } else {
                String goal = desc.substring(start).trim();
                if (!goal.isEmpty()) return goal;
            }
        }
        if (desc != null && desc.contains("领域：")) {
            int start = desc.lastIndexOf("领域：") + 3;
            String field = desc.substring(start).trim();
            if (!field.isEmpty() && !field.equals("通用")) return field;
        }
        String cleaned = name.replaceAll("学习路径 - |\\[.*?\\]", "").trim();
        if (!cleaned.isEmpty() && !cleaned.equals(name)) return cleaned;
        if (name != null && !name.isBlank() && !name.equals("未命名学习计划")) return name;
        return "编程开发";
    }

    /**
     * 极简降级方案（仅供 AI 调用失败时使用）
     */
    private List<Map<String, Object>> buildMinimalFallback(String goal) {
        List<Map<String, Object>> queue = new ArrayList<>();
        String[][] topics = {
            {goal + "基础知识学习", "理解" + goal + "核心概念"},
            {goal + "核心概念掌握", "深入学习关键知识点"},
            {goal + "实践练习", "完成实践题目巩固所学"},
            {goal + "进阶学习", "探索高级特性和应用场景"},
            {goal + "综合练习", "综合运用所学知识"},
            {goal + "复习与总结", "复习本周内容整理笔记"},
            {goal + "项目实战", "完成小型实战项目"}
        };
        for (int d = 0; d < 7; d++) {
            Map<String, Object> t1 = new HashMap<>();
            t1.put("phaseId", "default-phase");
            t1.put("phaseTitle", "学习阶段");
            t1.put("weekNumber", 1);
            t1.put("id", UUID.randomUUID().toString());
            t1.put("title", topics[d][0]);
            t1.put("description", topics[d][1]);
            t1.put("estimatedMinutes", 25);
            t1.put("estimatedHours", 0.5);
            queue.add(t1);

            Map<String, Object> t2 = new HashMap<>();
            t2.put("phaseId", "default-phase");
            t2.put("phaseTitle", "学习阶段");
            t2.put("weekNumber", 1);
            t2.put("id", UUID.randomUUID().toString());
            t2.put("title", topics[d][0] + " - 练习");
            t2.put("description", "完成练习题巩固学习效果");
            t2.put("estimatedMinutes", 20);
            t2.put("estimatedHours", 0.3);
            queue.add(t2);
        }
        return queue;
    }

    /**
     * 调用 DeepSeek API
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String callDeepSeek(String prompt) {
        String url = baseUrl + "/chat/completions";

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-v4-flash");
        requestBody.put("messages", List.of(message));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 4096);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> responseMessage = (Map<String, Object>) choice.get("message");
                if (responseMessage != null) {
                    return (String) responseMessage.get("content");
                }
            }
        }
        throw new RuntimeException("DeepSeek API 返回异常: " + response.getBody());
    }

    /**
     * 解析 AI 返回的 JSON 任务数据
     */
    private List<Map<String, Object>> parseAiTasks(String responseBody, String goal) {
        try {
            String json = responseBody.trim();
            if (json.startsWith("```json")) json = json.substring(7);
            else if (json.startsWith("```")) json = json.substring(3);
            if (json.endsWith("```")) json = json.substring(0, json.length() - 3);
            json = json.trim();

            List<Map<String, Object>> rawTasks = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> tasks = new ArrayList<>();

            for (int i = 0; i < rawTasks.size(); i++) {
                Map<String, Object> raw = rawTasks.get(i);
                String title = (String) raw.get("title");
                if (title == null || title.isBlank()) continue;

                Map<String, Object> node = new HashMap<>();
                node.put("phaseId", raw.get("phaseTitle") != null ? raw.get("phaseTitle") : "default-phase");
                node.put("phaseTitle", raw.get("phaseTitle") != null ? raw.get("phaseTitle") : "学习阶段");
                node.put("weekNumber", raw.get("weekNumber") instanceof Number ? ((Number) raw.get("weekNumber")).intValue() : 1);
                node.put("id", UUID.randomUUID().toString());
                node.put("title", title);
                node.put("description", raw.get("description") != null ? raw.get("description") : "");
                node.put("estimatedMinutes", raw.get("estimatedMinutes") instanceof Number ? ((Number) raw.get("estimatedMinutes")).intValue() : 25);
                node.put("estimatedHours", raw.get("estimatedMinutes") instanceof Number ? ((Number) raw.get("estimatedMinutes")).doubleValue() / 60.0 : 0.5);

                // 推断任务类型
                String type = raw.get("type") instanceof String ? (String) raw.get("type") : inferTaskType(node);
                node.put("type", type);

                tasks.add(node);
            }

            log.info("AI任务解析完成: 原始{}个, 有效{}个", rawTasks.size(), tasks.size());
            return tasks;
        } catch (Exception e) {
            log.error("解析AI返回任务失败: response={}", responseBody, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从路径节点构建任务队列（扁平化）
     */
    private List<Map<String, Object>> buildTaskQueueFromPath(String pathId) {
        Optional<LearningPath> pathOpt = learningPathRepository.findById(pathId);
        if (pathOpt.isEmpty()) {
            return new ArrayList<>();
        }
        String nodesJson = pathOpt.get().getNodes();
        if (nodesJson == null || nodesJson.isBlank() || nodesJson.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<Map<String, Object>>>() {});
            // 按阶段、周、序号排序
            nodes.sort((a, b) -> {
                int phaseA = a.get("phaseId") != null ? a.get("phaseId").hashCode() : 0;
                int phaseB = b.get("phaseId") != null ? b.get("phaseId").hashCode() : 0;
                if (phaseA != phaseB) return Integer.compare(phaseA, phaseB);
                int weekA = a.get("weekNumber") instanceof Number ? ((Number) a.get("weekNumber")).intValue() : 0;
                int weekB = b.get("weekNumber") instanceof Number ? ((Number) b.get("weekNumber")).intValue() : 0;
                return Integer.compare(weekA, weekB);
            });
            return nodes;
        } catch (Exception e) {
            log.error("解析路径节点JSON失败: pathId={}", pathId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 推断任务类型
     */
    private String inferTaskType(Map<String, Object> node) {
        String title = node.get("title") != null ? (String) node.get("title")
                : (node.get("name") != null ? (String) node.get("name") : "");
        String desc = node.get("description") != null ? (String) node.get("description") : "";
        String combined = (title + " " + desc).toLowerCase();

        if (combined.contains("练习") || combined.contains("实践") || combined.contains("project")
                || combined.contains("代码") || combined.contains("coding") || combined.contains("implement")) {
            return "practice";
        }
        if (combined.contains("视频") || combined.contains("watch") || combined.contains("课程")
                || combined.contains("tutorial") || combined.contains("video")) {
            return "video";
        }
        if (combined.contains("复习") || combined.contains("review") || combined.contains("回顾")
                || combined.contains("recap")) {
            return "review";
        }
        return "read";
    }

    /**
     * 实体转 DTO（单任务）
     */
    private DailyPlanDTO.DailyTaskDTO toDailyTaskDTO(DailyTask task) {
        return DailyPlanDTO.DailyTaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .type(task.getType())
                .estimatedMinutes(task.getEstimatedMinutes() != null ? task.getEstimatedMinutes() : 0)
                .status(task.getStatus())
                .description(task.getDescription() != null ? task.getDescription() : "")
                .sortOrder(task.getSortOrder() != null ? task.getSortOrder() : 0)
                .resourceId(task.getResourceId())
                .sourceNodeId(task.getSourceNodeId())
                .build();
    }

    /**
     * 实体列表转 DailyPlanDTO
     */
    private DailyPlanDTO toDailyPlanDTO(LocalDate date, List<DailyTask> tasks) {
        int totalCompleted = (int) tasks.stream().filter(t -> "completed".equals(t.getStatus())).count();
        int totalMinutes = tasks.stream().mapToInt(t -> t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 0).sum();

        return DailyPlanDTO.builder()
                .date(date.format(DATE_FMT))
                .totalEstimatedMinutes(totalMinutes)
                .totalCompleted(totalCompleted)
                .totalTasks(tasks.size())
                .allCompleted(totalCompleted == tasks.size() && !tasks.isEmpty())
                .tasks(tasks.stream().map(this::toDailyTaskDTO).collect(Collectors.toList()))
                .build();
    }
}