package com.ai.learning.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习会话响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSessionDTO {

    private Long id;
    private String goal;
    private String phase;
    private String status;
    private Integer progress;
    private String contextJson;
    private List<SessionPhaseDTO> phases;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
