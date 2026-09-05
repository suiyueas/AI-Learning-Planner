package com.ai.learning.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 会话阶段响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionPhaseDTO {

    private Long id;
    private String phaseId;
    private String agentId;
    private String status;
    private String outputJson;
    private Integer durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
