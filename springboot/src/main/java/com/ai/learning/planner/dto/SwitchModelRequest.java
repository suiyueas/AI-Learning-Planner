package com.ai.learning.planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型切换请求DTO
 */
@Data
public class SwitchModelRequest {

    @NotBlank(message = "provider不能为空")
    private String provider;
}