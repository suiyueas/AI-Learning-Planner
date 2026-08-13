package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.CheckinResultDTO;
import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 打卡功能控制器
 * <p>
 * 提供用户每日打卡、打卡统计查询、月度打卡日期列表等 API 接口。
 * 所有接口均需认证，通过 {@link Authentication#getPrincipal()} 获取当前用户 ID。
 * </p>
 */
@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
@Slf4j
public class CheckinController {

    private final CheckinService checkinService;

    /**
     * 用户打卡
     * <p>
     * 记录当前用户当天打卡，更新连续打卡天数和总打卡天数。
     * 如果当天已打卡，则返回错误提示。
     * </p>
     *
     * @param authentication Spring Security 认证对象，用于获取当前用户 ID
     * @return 包含打卡结果（今日已打卡、连续天数、总天数、本月打卡日期）的响应
     *         成功时返回 {@code 200}，内部错误返回 {@code 500}
     */
    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<CheckinResultDTO>> checkin(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("用户打卡请求: userId={}", userId);
        try {
            CheckinResultDTO result = checkinService.checkin(userId);
            return ResponseEntity.ok(ApiResponse.success("打卡成功！", result));
        } catch (Exception e) {
            log.error("打卡失败, userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取当前用户打卡统计信息
     * <p>
     * 返回今日是否已打卡、连续打卡天数、总打卡天数以及本月所有打卡日期列表。
     * </p>
     *
     * @param authentication Spring Security 认证对象，用于获取当前用户 ID
     * @return 包含完整打卡统计数据的响应
     *         成功时返回 {@code 200}，内部错误返回 {@code 500}
     */
    @GetMapping("/checkin/stats")
    public ResponseEntity<ApiResponse<CheckinResultDTO>> getCheckinStats(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取打卡统计: userId={}", userId);
        try {
            CheckinResultDTO result = checkinService.getCheckinStats(userId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取打卡统计失败, userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取打卡统计失败：" + e.getMessage()));
        }
    }

    /**
     * 获取指定月份的打卡日期列表
     * <p>
     * 返回当前用户在指定年份和月份的所有打卡日期（仅日期，不含时间）。
     * 可用于前端绘制打卡日历。
     * </p>
     *
     * @param authentication Spring Security 认证对象，用于获取当前用户 ID
     * @param year           年份（如 2026）
     * @param month          月份（1-12）
     * @return 包含该月所有打卡日期的列表
     *         成功时返回 {@code 200}，内部错误返回 {@code 500}
     */
    @GetMapping("/checkin/month")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getMonthCheckinDates(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取月份打卡日期: userId={}, year={}, month={}", userId, year, month);
        try {
            List<LocalDate> dates = checkinService.getMonthCheckinDates(userId, year, month);
            return ResponseEntity.ok(ApiResponse.success(dates));
        } catch (Exception e) {
            log.error("获取月份打卡日期失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败：" + e.getMessage()));
        }
    }

    @GetMapping("/calendar/monthly-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyStats(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取月度统计: userId={}, year={}, month={}", userId, year, month);
        try {
            Map<String, Object> stats = checkinService.getMonthlyStats(userId, year, month);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取月度统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取月度统计失败：" + e.getMessage()));
        }
    }
}