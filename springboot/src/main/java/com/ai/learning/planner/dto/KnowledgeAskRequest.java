package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库问答请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeAskRequest {
    @NotBlank(message = "问题不能为空")
    @Size(max = 10000, message = "问题内容不能超过 10000 字符")
    private String question;
}
