package com.ai.learning.planner.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO
 * 校验旧密码并设置新密码（新密码需含字母和数字，长度 8-50）
 */
@Data
public class PasswordChangeRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度需要在8-50个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String newPassword;

    @NotBlank(message = "确认新密码不能为空")
    private String confirmNewPassword;
}