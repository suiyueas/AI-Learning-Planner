package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.dto.auth.PasswordChangeRequest;
import com.ai.learning.planner.dto.auth.ProfileUpdateRequest;
import com.ai.learning.planner.dto.auth.UserPreferencesDTO;
import com.ai.learning.planner.dto.auth.UserProfileResponse;
import com.ai.learning.planner.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户控制器
 * 提供用户信息查询、更新、头像上传和偏好设置等功能
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 更新用户信息
     * PUT /api/user/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", profile));
    }

    /**
     * 上传用户头像
     * POST /api/user/avatar
     */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("请选择要上传的头像文件"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("只支持上传图片文件"));
        }

        Long userId = (Long) authentication.getPrincipal();
        String avatarUrl = userService.updateAvatar(userId, file);
        return ResponseEntity.ok(ApiResponse.success("头像上传成功",
                Map.of("avatarUrl", avatarUrl)));
    }

    /**
     * 获取用户学习偏好
     * GET /api/user/preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesDTO>> getPreferences(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserPreferencesDTO preferences = userService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * 更新用户学习偏好
     * PUT /api/user/preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesDTO>> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UserPreferencesDTO preferences) {
        Long userId = (Long) authentication.getPrincipal();
        UserPreferencesDTO saved = userService.savePreferences(userId, preferences);
        return ResponseEntity.ok(ApiResponse.success("偏好设置已保存", saved));
    }

    /**
     * 修改密码
     * PUT /api/user/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("两次输入的新密码不一致"));
        }

        Long userId = (Long) authentication.getPrincipal();
        try {
            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取用户统计数据
     * GET /api/user/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = userService.getUserStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取用户学习统计
     * GET /api/user/learning-stats
     */
    @GetMapping("/learning-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLearningStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = userService.getLearningStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取用户成就列表
     * GET /api/user/achievements
     */
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAchievements(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> achievements = userService.getAchievements(userId);
        return ResponseEntity.ok(ApiResponse.success(achievements));
    }
}