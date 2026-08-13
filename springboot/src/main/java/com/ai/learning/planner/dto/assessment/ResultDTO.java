package com.ai.learning.planner.dto.assessment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 提交答题结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO {
    private int totalScore;
    private int correctCount;
    private int wrongCount;
    private int accuracy;
    private String level;
    /** 题目ID -> 详细结果 */
    private List<QuestionResult> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private Long questionId;
        private String questionText;
        private List<String> options;
        private int correctAnswer;
        private int userAnswer;
        private boolean correct;
        private String explanation;
    }
}
