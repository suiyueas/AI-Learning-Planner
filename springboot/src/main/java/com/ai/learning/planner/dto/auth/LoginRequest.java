package com.ai.learning.planner.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名/邮箱不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}