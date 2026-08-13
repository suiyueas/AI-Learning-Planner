package com.ai.learning.planner.dto.auth;

import lombok.Data;

/**
 * 用户资料更新请求 DTO
 * 可更新昵称、学习目标与个人简介（仅更新非空字段）
 */
@Data
public class ProfileUpdateRequest {
    private String nickname;
    private String learningGoal;
    private String bio;
}