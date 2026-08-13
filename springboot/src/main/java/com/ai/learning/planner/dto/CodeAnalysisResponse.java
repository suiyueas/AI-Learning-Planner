package com.ai.learning.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 代码分析响应 DTO
 * 包含 AI 审计出的问题列表、优化建议、复杂度评估与优化后代码
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeAnalysisResponse {
    private List<CodeIssue> issues;
    private List<String> suggestions;
    private String complexity;
    private String optimizedCode;
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeIssue {
        private String type;
        private String message;
        private Integer line;
        private String severity;
    }
}