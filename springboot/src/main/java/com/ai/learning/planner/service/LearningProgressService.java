package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.ActivePathDTO;
import com.ai.learning.planner.dto.PathProgressDTO;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.entity.LearningRecord;
import com.ai.learning.planner.exception.BusinessException;
import com.ai.learning.planner.repository.LearningPathRepository;
import com.ai.learning.planner.repository.LearningRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习进度服务
 * 管理学习路径进度、任务完成状态和节点完成情况
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningProgressService {

    private final LearningPathRepository learningPathRepository;
    private final LearningRecordRepository learningRecordRepository;
    private final AdaptiveEngineService adaptiveEngineService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 校验路径存在且归属当前用户（统一对象级授权入口）
     */
    private LearningPath requireOwnedPath(String pathId, String userId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("学习路径不存在: " + pathId));
        if (!path.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该学习路径");
        }
        return path;
    }

    /**
     * 计算学习路径进度
     */
    public PathProgressDTO calculateProgress(String userId, String pathId) {
        LearningPath path = requireOwnedPath(pathId, userId);

        List<LearningRecord> records = learningRecordRepository.findByUserIdAndPathId(userId, pathId);
        Map<String, LearningRecord> recordMap = records.stream()
                .collect(Collectors.toMap(r -> r.getNodeId(), r -> r, (a, b) -> b));

        List<Map<String, Object>> nodes = parseNodes(path.getNodes());
        if (nodes.isEmpty()) {
            return buildEmptyProgress(path);
        }

        return buildProgressDTO(path, nodes, recordMap);
    }

    /**
     * 完成学习任务
     */
    public LearningRecord completeTask(String userId, String pathId, String taskId) {
        requireOwnedPath(pathId, userId);
        LearningRecord record = learningRecordRepository
                .findByUserIdAndPathIdAndNodeId(userId, pathId, taskId)
                .orElseGet(() -> LearningRecord.builder()
                        .userId(userId)
                        .pathId(pathId)
                        .nodeId(taskId)
                        .nodeType("task")
                        .status("pending")
                        .masteryLevel(0f)
                        .timeSpent(0)
                        .build());

        record.setStatus("completed");
        record.setCompletedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        if (record.getId() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }

        LearningRecord saved = learningRecordRepository.save(record);
        updatePathProgress(pathId);
        // 推荐消费联动：完成节点后，匹配的个性化推荐自动置为已消费
        adaptiveEngineService.markRecommendationsConsumed(userId, pathId, taskId);
        return saved;
    }

    /**
     * 更新任务进度（学习时长和掌握度）
     */
    public LearningRecord updateTaskProgress(String userId, String pathId, String taskId, Integer timeSpent, Float masteryLevel) {
        requireOwnedPath(pathId, userId);
        LearningRecord record = learningRecordRepository
                .findByUserIdAndPathIdAndNodeId(userId, pathId, taskId)
                .orElseGet(() -> LearningRecord.builder()
                        .userId(userId)
                        .pathId(pathId)
                        .nodeId(taskId)
                        .nodeType("task")
                        .status("in_progress")
                        .masteryLevel(0f)
                        .timeSpent(0)
                        .build());

        if (timeSpent != null) {
            record.setTimeSpent(timeSpent);
        }
        if (masteryLevel != null) {
            record.setMasteryLevel(masteryLevel);
        }
        record.setStatus("in_progress");
        record.setUpdatedAt(LocalDateTime.now());

        if (record.getId() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }

        LearningRecord saved = learningRecordRepository.save(record);
        updatePathProgress(pathId);
        return saved;
    }

    private void updatePathProgress(String pathId) {
        learningPathRepository.findById(pathId).ifPresent(path -> {
            List<LearningRecord> records = learningRecordRepository.findByUserIdAndPathId(path.getUserId(), pathId);
            List<Map<String, Object>> nodes = parseNodes(path.getNodes());

            if (nodes.isEmpty()) return;

            int completedCount = 0;
            int totalCount = 0;

            for (Map<String, Object> node : nodes) {
                String nodeId = String.valueOf(node.get("id"));
                Optional<LearningRecord> record = records.stream()
                        .filter(r -> r.getNodeId().equals(nodeId))
                        .findFirst();

                if (record.isPresent() && "completed".equals(record.get().getStatus())) {
                    completedCount++;
                }
                totalCount++;
            }

            float progress = totalCount > 0 ? (float) completedCount / totalCount * 100 : 0;
            path.setCompletionPercentage(progress);
            path.setUpdatedAt(LocalDateTime.now());
            learningPathRepository.save(path);
        });
    }

    private List<Map<String, Object>> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.isBlank() || nodesJson.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(nodesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析路径节点JSON失败", e);
            return List.of();
        }
    }

    private PathProgressDTO buildEmptyProgress(LearningPath path) {
        return PathProgressDTO.builder()
                .pathId(path.getId())
                .name(path.getName())
                .description(path.getDescription())
                .progress(0)
                .totalModules(0)
                .completedModules(0)
                .phases(List.of())
                .estimatedHours(0.0)
                .spentHours(0.0)
                .rating(0.0)
                .createdAt(formatDateTime(path.getCreatedAt()))
                .updatedAt(formatDateTime(path.getUpdatedAt()))
                .build();
    }

    private PathProgressDTO buildProgressDTO(LearningPath path, List<Map<String, Object>> nodes, Map<String, LearningRecord> recordMap) {
        List<PathProgressDTO.PhaseDTO> phases = new ArrayList<>();
        int totalTasks = 0;
        int completedTasks = 0;
        double totalSpentHours = 0;
        double totalEstimatedHours = 0;

        Map<String, List<Map<String, Object>>> phaseMap = new LinkedHashMap<>();
        for (Map<String, Object> node : nodes) {
            String phaseId = String.valueOf(node.getOrDefault("phaseId", "default"));
            phaseMap.computeIfAbsent(phaseId, k -> new ArrayList<>()).add(node);
        }

        int phaseIndex = 0;
        for (Map.Entry<String, List<Map<String, Object>>> entry : phaseMap.entrySet()) {
            List<Map<String, Object>> phaseNodes = entry.getValue();

            List<PathProgressDTO.WeekDTO> weeks = new ArrayList<>();
            Map<Integer, List<Map<String, Object>>> weekMap = new LinkedHashMap<>();

            for (Map<String, Object> node : phaseNodes) {
                int weekNum = getIntValue(node, "weekNumber", 1);
                weekMap.computeIfAbsent(weekNum, k -> new ArrayList<>()).add(node);
            }

            for (Map.Entry<Integer, List<Map<String, Object>>> weekEntry : weekMap.entrySet()) {
                List<PathProgressDTO.TaskDTO> tasks = new ArrayList<>();
                int weekTasks = 0;
                int weekCompleted = 0;

                for (Map<String, Object> node : weekEntry.getValue()) {
                    String nodeId = String.valueOf(node.get("id"));
                    LearningRecord record = recordMap.get(nodeId);

                    String status = "pending";
                    double spentHours = 0;
                    double estimatedHours = getDoubleValue(node, "estimatedHours", 1.0);

                    if (record != null) {
                        status = record.getStatus() != null ? record.getStatus() : "pending";
                        spentHours = record.getTimeSpent() != null ? record.getTimeSpent() / 60.0 : 0;
                        if ("completed".equals(status)) {
                            weekCompleted++;
                        }
                    }

                    totalSpentHours += spentHours;
                    totalEstimatedHours += estimatedHours;

                    tasks.add(PathProgressDTO.TaskDTO.builder()
                            .id(nodeId)
                            .title(String.valueOf(node.getOrDefault("title", "")))
                            .description(String.valueOf(node.getOrDefault("description", "")))
                            .status(status)
                            .estimatedHours(estimatedHours)
                            .spentHours(spentHours)
                            .build());
                    weekTasks++;
                    totalTasks++;
                }

                if (weekTasks > 0) {
                    completedTasks += weekCompleted;
                }

                int weekProgress = weekTasks > 0 ? (int) ((double) weekCompleted / weekTasks * 100) : 0;

                weeks.add(PathProgressDTO.WeekDTO.builder()
                        .id("week-" + weekEntry.getKey())
                        .weekNumber(weekEntry.getKey())
                        .title(String.format("第 %d 周", weekEntry.getKey()))
                        .progress(weekProgress)
                        .tasks(tasks)
                        .build());
            }

            int phaseTasks = phaseNodes.size();
            int phaseCompleted = 0;
            for (Map<String, Object> node : phaseNodes) {
                String nodeId = String.valueOf(node.get("id"));
                LearningRecord record = recordMap.get(nodeId);
                if (record != null && "completed".equals(record.getStatus())) {
                    phaseCompleted++;
                }
            }

            int phaseProgress = phaseTasks > 0 ? (int) ((double) phaseCompleted / phaseTasks * 100) : 0;
            String phaseStatus = phaseProgress == 0 ? "pending" : (phaseProgress == 100 ? "completed" : "in_progress");

            phases.add(PathProgressDTO.PhaseDTO.builder()
                    .id("phase-" + phaseIndex)
                    .title(String.valueOf(phaseNodes.get(0).getOrDefault("phaseTitle", "阶段 " + (phaseIndex + 1))))
                    .description(String.valueOf(phaseNodes.get(0).getOrDefault("phaseDescription", "")))
                    .progress(phaseProgress)
                    .status(phaseStatus)
                    .weeks(weeks)
                    .build());
            phaseIndex++;
        }

        int totalProgress = totalTasks > 0 ? (int) ((double) completedTasks / totalTasks * 100) : 0;

        // 下一个待完成任务：按 阶段 → 周 → 任务 顺序取第一个未完成项
        String nextNodeName = null;
        outer:
        for (PathProgressDTO.PhaseDTO phase : phases) {
            for (PathProgressDTO.WeekDTO week : phase.getWeeks()) {
                for (PathProgressDTO.TaskDTO task : week.getTasks()) {
                    if (!"completed".equals(task.getStatus())) {
                        nextNodeName = task.getTitle();
                        break outer;
                    }
                }
            }
        }

        return PathProgressDTO.builder()
                .pathId(path.getId())
                .name(path.getName())
                .description(path.getDescription())
                .difficulty("中级")
                .progress(totalProgress)
                .totalModules(phaseMap.size())
                .completedModules((int) phases.stream().filter(p -> "completed".equals(p.getStatus())).count())
                .nextNodeName(nextNodeName)
                .phases(phases)
                .estimatedHours(totalEstimatedHours)
                .spentHours(totalSpentHours)
                .learnerCount(128)
                .rating(4.7)
                .createdAt(formatDateTime(path.getCreatedAt()))
                .updatedAt(formatDateTime(path.getUpdatedAt()))
                .build();
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(FORMATTER);
    }

    /**
     * 获取当前活跃路径的统一数据（解决卡片与详情页数据不一致问题）
     * 包含：路径信息、实时进度（从learning_record计算）、下一节点、路径状态
     */
    public ActivePathDTO getActivePathProgress(String userId) {
        Optional<LearningPath> pathOpt = learningPathRepository.findByUserIdAndIsActive(userId, true);

        if (pathOpt.isEmpty()) {
            return ActivePathDTO.builder()
                    .hasPath(false)
                    .status(ActivePathDTO.PathStatus.EMPTY)
                    .build();
        }

        LearningPath path = pathOpt.get();
        List<LearningRecord> records = learningRecordRepository.findByUserIdAndPathId(userId, path.getId());
        Map<String, LearningRecord> recordMap = records.stream()
                .collect(Collectors.toMap(r -> r.getNodeId(), r -> r, (a, b) -> b));

        List<Map<String, Object>> nodes = parseNodes(path.getNodes());

        // 计算进度
        int totalNodes = nodes.size();
        int completedNodes = 0;
        double totalSpentHours = 0;
        double totalEstimatedHours = 0;
        Map<String, Object> nextNodeMap = null;

        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> node = nodes.get(i);
            String nodeId = String.valueOf(node.get("id"));
            LearningRecord record = recordMap.get(nodeId);

            double estimatedHours = getDoubleValue(node, "estimatedHours", 1.0);
            totalEstimatedHours += estimatedHours;

            if (record != null && "completed".equals(record.getStatus())) {
                completedNodes++;
                totalSpentHours += record.getTimeSpent() != null ? record.getTimeSpent() / 60.0 : 0;
            } else if (nextNodeMap == null) {
                // 找到第一个未完成的节点作为下一节点
                nextNodeMap = node;
            }
        }

        int percentage = totalNodes > 0 ? (int) ((double) completedNodes / totalNodes * 100) : 0;

        // 判断路径状态
        ActivePathDTO.PathStatus status;
        if (nodes.isEmpty() || percentage == 0) {
            status = ActivePathDTO.PathStatus.ACTIVE;
        } else if (percentage == 100) {
            status = ActivePathDTO.PathStatus.COMPLETED;
        } else {
            status = ActivePathDTO.PathStatus.IN_PROGRESS;
        }

        // 构建路径信息
        ActivePathDTO.PathInfo pathInfo = ActivePathDTO.PathInfo.builder()
                .id(path.getId())
                .name(path.getName())
                .description(path.getDescription())
                .createdAt(formatDateTime(path.getCreatedAt()))
                .build();

        // 构建进度信息
        ActivePathDTO.ProgressInfo progressInfo = ActivePathDTO.ProgressInfo.builder()
                .percentage(percentage)
                .completedNodes(completedNodes)
                .totalNodes(totalNodes)
                .estimatedHours(totalEstimatedHours)
                .spentHours(totalSpentHours)
                .build();

        // 构建下一节点信息
        ActivePathDTO.NextNodeInfo nextNodeInfo = null;
        if (nextNodeMap != null) {
            nextNodeInfo = ActivePathDTO.NextNodeInfo.builder()
                    .nodeId(String.valueOf(nextNodeMap.get("id")))
                    .nodeName(String.valueOf(nextNodeMap.getOrDefault("title", "未知节点")))
                    .nodeType(String.valueOf(nextNodeMap.getOrDefault("type", "task")))
                    .phaseTitle(String.valueOf(nextNodeMap.getOrDefault("phaseTitle", "")))
                    .build();
        }

        return ActivePathDTO.builder()
                .hasPath(true)
                .status(status)
                .path(pathInfo)
                .progress(progressInfo)
                .nextNode(nextNodeInfo)
                .adjustHistory(List.of())
                .build();
    }
}