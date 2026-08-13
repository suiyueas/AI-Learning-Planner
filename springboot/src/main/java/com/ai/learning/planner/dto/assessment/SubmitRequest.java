package com.ai.learning.planner.dto.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 提交答题请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequest {
    private Long userId;

    @NotBlank(message = "科目不能为空")
    private String subject;

    private String difficulty;

    /** 题目ID列表（用于查询完整题目信息） */
    private List<Long> questionIds;

    /** 题目ID -> 用户答案索引（0=A, 1=B, 2=C, 3=D） */
    @NotNull(message = "答案不能为空")
    private Map<Long, Integer> answers;
}