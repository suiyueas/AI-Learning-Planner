package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.dto.DailyPlanDTO;
import com.ai.learning.planner.dto.WeekPreviewDTO;
import com.ai.learning.planner.service.DailyTaskService;
import com.ai.learning.planner.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 每日任务控制器
 * 提供路径下每日任务的查询（今日/指定日期/本周预览）、状态更新与重新生成接口；
 * 所有操作均校验路径与任务的用户归属
 */
@RestController
@RequestMapping("/learning-path/{pathId}/daily-tasks")
@RequiredArgsConstructor
@Slf4j
public class DailyTaskController {

    private final DailyTaskService dailyTaskService;

    /**
     * 获取今日任务
     * GET /api/learning-path/{pathId}/daily-tasks/today
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<DailyPlanDTO>> getTodayTasks(
            @PathVariable String pathId,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("获取今日任务: pathId={}, userId={}", pathId, userId);
        try {
            DailyPlanDTO plan = dailyTaskService.getDailyTasks(pathId, userId, LocalDate.now());
            return ResponseEntity.ok(ApiResponse.success(plan));
        } catch (Exception e) {
            log.error("获取今日任务失败: pathId={}, error={}", pathId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("获取今日任务失败：" + e.getMessage()));
        }
    }

    /**
     * 获取指定日期的任务
     * GET /api/learning-path/{pathId}/daily-tasks?date=2026-07-20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DailyPlanDTO>> getDailyTasks(
            @PathVariable String pathId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("获取每日任务: pathId={}, userId={}, date={}", pathId, userId, targetDate);
        try {
            DailyPlanDTO plan = dailyTaskService.getDailyTasks(pathId, userId, targetDate);
            return ResponseEntity.ok(ApiResponse.success(plan));
        } catch (Exception e) {
            log.error("获取每日任务失败: pathId={}, date={}, error={}", pathId, targetDate, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("获取每日任务失败：" + e.getMessage()));
        }
    }

    /**
     * 获取本周预览
     * GET /api/learning-path/{pathId}/daily-tasks/week
     */
    @GetMapping("/week")
    public ResponseEntity<ApiResponse<WeekPreviewDTO>> getWeekPreview(
            @PathVariable String pathId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("获取本周预览: pathId={}, userId={}, date={}", pathId, userId, targetDate);
        try {
            WeekPreviewDTO preview = dailyTaskService.getWeekPreview(pathId, userId, targetDate);
            return ResponseEntity.ok(ApiResponse.success(preview));
        } catch (Exception e) {
            log.error("获取本周预览失败: pathId={}, error={}", pathId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("获取本周预览失败：" + e.getMessage()));
        }
    }

    /**
     * 更新任务状态
     * PUT /api/learning-path/{pathId}/daily-tasks/{taskId}
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<DailyPlanDTO.DailyTaskDTO>> updateTaskStatus(
            @PathVariable String pathId,
            @PathVariable String taskId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("状态不能为空"));
        }
        log.info("更新任务状态: pathId={}, taskId={}, userId={}, status={}", pathId, taskId, userId, status);
        try {
            DailyPlanDTO.DailyTaskDTO task = dailyTaskService.updateTaskStatus(taskId, status, userId);
            return ResponseEntity.ok(ApiResponse.success(task));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(404).body(ApiResponse.notFound(msg));
            }
            log.error("更新任务状态失败: taskId={}, error={}", taskId, msg, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("更新失败：" + msg));
        }
    }

    /**
     * 重新生成每日任务
     * POST /api/learning-path/{pathId}/daily-tasks/regenerate
     */
    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<Void>> regenerateTasks(
            @PathVariable String pathId,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("重新生成每日任务: pathId={}, userId={}", pathId, userId);
        try {
            dailyTaskService.regenerateTasks(pathId, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("重新生成每日任务失败: pathId={}, error={}", pathId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.serverError("重新生成失败：" + e.getMessage()));
        }
    }
}
