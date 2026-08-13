package com.ai.learning.planner.dto.assessment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 返回给前端的题目 DTO（隐藏正确答案和解析，提交前不返回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private String subject;
    private String questionText;
    private List<String> options;
    private String difficulty;
}
