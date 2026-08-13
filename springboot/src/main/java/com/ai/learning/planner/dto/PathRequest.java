package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习路径创建请求 DTO
 * 创建路径时归属 userId 强制取自认证上下文，请求体中的 userId 字段已废弃
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathRequest {
    /** 用户ID（已废弃：归属强制取自认证上下文，保留字段仅为兼容旧前端） */
    private String userId;
    @NotBlank
    private String goal;
    private Integer durationWeeks;
    private String difficulty;
}
