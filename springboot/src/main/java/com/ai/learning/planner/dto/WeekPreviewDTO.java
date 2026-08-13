package com.ai.learning.planner.dto;

import lombok.*;
import java.util.List;

/**
 * 本周预览 DTO - 展示本周每天的汇总
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekPreviewDTO {
    private String weekStart;
    private String weekEnd;
    private int totalMinutes;
    private int totalCompletedTasks;
    private int totalTasks;
    private List<DaySummaryDTO> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySummaryDTO {
        private String date;
        private String dayOfWeek;
        private int totalMinutes;
        private int completedTasks;
        private int totalTasks;
        private String topic;
    }
}
