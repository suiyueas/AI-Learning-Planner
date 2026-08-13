package com.ai.learning.planner.dto.assessment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 历史测评记录概要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryRecordDTO {
    private Long id;
    private String subject;
    private String difficulty;
    private Integer score;
    private Integer total;
    private LocalDateTime createdAt;

    public int getAccuracy() {
        if (total == null || total == 0) return 0;
        return (int) Math.round(score * 100.0 / total);
    }
}