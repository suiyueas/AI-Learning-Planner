package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeAnalysisRequest {
    @NotBlank(message = "代码内容不能为空")
    private String code;

    /** 代码语言（不传时自动识别） */
    private String language;
}