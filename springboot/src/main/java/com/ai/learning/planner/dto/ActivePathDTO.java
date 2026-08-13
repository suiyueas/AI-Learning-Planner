package com.ai.learning.planner.dto;

import lombok.*;
import java.util.List;

/**
 * 当前活跃路径 DTO（统一数据源）
 * 解决功能卡片与详情页数据不一致问题
 * 包含：路径基本信息、实时进度、下一节点、路径状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivePathDTO {
    /** 路径状态枚举 */
    public enum PathStatus {
        EMPTY,      // 无路径
        ACTIVE,     // 有路径未开始
        IN_PROGRESS, // 进行中
        GENERATING,  // 大纲生成中
        COMPLETED   // 已完成
    }

    /** 是否存在活跃路径 */
    private boolean hasPath;

    /** 路径状态 */
    private PathStatus status;

    /** 路径基本信息（无路径时为null） */
    private PathInfo path;

    /** 实时进度（从learning_record计算，非字段） */
    private ProgressInfo progress;

    /** 下一节点信息（无路径或已完成时为null） */
    private NextNodeInfo nextNode;

    /** 路径调整历史 */
    private List<AdjustRecord> adjustHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathInfo {
        private String id;
        private String name;
        private String description;
        private String difficulty;
        private Integer learnerCount;
        private Double rating;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfo {
        /** 计算得出的进度百分比（0-100） */
        private int percentage;
        /** 已完成节点数 */
        private int completedNodes;
        /** 总节点数 */
        private int totalNodes;
        /** 预估总时长（小时） */
        private double estimatedHours;
        /** 已学习时长（小时） */
        private double spentHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextNodeInfo {
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private String phaseTitle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustRecord {
        private String date;
        private String reason;
        private String description;
    }
}