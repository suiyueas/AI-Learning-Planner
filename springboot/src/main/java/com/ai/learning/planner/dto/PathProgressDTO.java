package com.ai.learning.planner.dto;

import lombok.*;
import java.util.List;

/**
 * 学习路径进度 DTO
 * 按 阶段(Phase) → 周(Week) → 任务(Task) 三级结构返回路径进度、时长与掌握情况
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathProgressDTO {
    private String pathId;
    private String name;
    private String description;
    private String difficulty;
    private Integer progress;
    private Integer totalModules;
    private Integer completedModules;
    /** 下一个待完成任务名称（全部完成时为 null），供卡片/详情页展示 */
    private String nextNodeName;
    private List<PhaseDTO> phases;
    private Double estimatedHours;
    private Double spentHours;
    private Integer learnerCount;
    private Double rating;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseDTO {
        private String id;
        private String title;
        private String description;
        private Integer progress;
        private String status;
        private List<WeekDTO> weeks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekDTO {
        private String id;
        private Integer weekNumber;
        private String title;
        private Integer progress;
        private List<TaskDTO> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDTO {
        private String id;
        private String title;
        private String description;
        private String status;
        private Double estimatedHours;
        private Double spentHours;
    }
}