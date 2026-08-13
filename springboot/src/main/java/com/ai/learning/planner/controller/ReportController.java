package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学情报告控制器
 * 提供学情报告生成API接口
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * 生成学情报告
     * @param startDate 报告起始日期
     * @param endDate 报告结束日期
     * @param sections 报告模块（overview/matrix/recommendations）
     * @param style 报告样式
     */
    @GetMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReport(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "overview,matrix,recommendations") List<String> sections,
            @RequestParam(required = false, defaultValue = "standard") String style) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("生成学情报告: userId={}, period={}~{}, sections={}", userId, startDate, endDate, sections);

        try {
            Map<String, Object> report = reportService.generateReport(userId, startDate, endDate, sections, style);
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            log.error("生成学情报告失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("生成学情报告失败: " + e.getMessage()));
        }
    }
}