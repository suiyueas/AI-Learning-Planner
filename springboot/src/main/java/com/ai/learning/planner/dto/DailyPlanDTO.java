package com.ai.learning.planner.dto;

import lombok.*;
import java.util.List;

/**
 * 每日计划 DTO - 某天的完整任务列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPlanDTO {
    private String date;
    private int totalEstimatedMinutes;
    private int totalCompleted;
    private int totalTasks;
    private boolean allCompleted;
    private List<DailyTaskDTO> tasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTaskDTO {
        private String id;
        private String title;
        private String type; // read, video, practice, review
        private int estimatedMinutes;
        private String status; // pending, in_progress, completed, skipped
        private String description;
        private int sortOrder;
        private String resourceId;
        private String sourceNodeId;
    }
}
