package com.ai.learning.planner.dto.assessment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 历史测评记录详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryDetailDTO {
    private Long id;
    private String subject;
    private String difficulty;
    private Integer score;
    private Integer total;
    private Integer accuracy;
    private String level;
    private LocalDateTime createdAt;
    private List<QuestionDetail> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetail {
        private Long questionId;
        private String questionText;
        private List<String> options;
        private Integer correctAnswer;
        private Integer userAnswer;
        private boolean correct;
        private String explanation;
    }
}