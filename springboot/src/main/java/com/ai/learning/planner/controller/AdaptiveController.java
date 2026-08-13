package com.ai.learning.planner.controller;

import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.AdaptiveEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 自适应引擎控制器
 * 引擎状态聚合 / 调整历史 / 个性化推荐 / 推荐点击与消费
 */
@RestController
@RequestMapping("/adaptive")
@RequiredArgsConstructor
@Slf4j
public class AdaptiveController {

    private final AdaptiveEngineService adaptiveEngineService;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 引擎状态聚合：策略 / 调整次数 / 效率提升 / 知识掌握率 / 归因分析
     * GET /api/adaptive/status
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }
        return Map.of("success", true, "data", adaptiveEngineService.getStatus(userId));
    }

    /**
     * 自适应调整历史（分页 + 类型筛选）
     * GET /api/adaptive/adjustments?page=0&size=20&type=review_insert
     */
    @GetMapping("/adjustments")
    public Map<String, Object> getAdjustments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }
        return Map.of("success", true, "data",
                adaptiveEngineService.getAdjustments(userId, page, size, type));
    }

    /**
     * 个性化推荐列表（不足时规则化生成并落库）
     * GET /api/adaptive/recommendations
     */
    @GetMapping("/recommendations")
    public Map<String, Object> getRecommendations() {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }
        return Map.of("success", true, "data", adaptiveEngineService.getRecommendations(userId));
    }

    /**
     * 标记推荐为已点击（pending → clicked）
     * POST /api/adaptive/recommendations/{id}/click
     */
    @PostMapping("/recommendations/{id}/click")
    public Map<String, Object> clickRecommendation(@PathVariable String id) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }
        boolean ok = adaptiveEngineService.clickRecommendation(userId, id);
        return ok
                ? Map.of("success", true, "message", "已记录点击")
                : Map.of("success", false, "message", "推荐不存在");
    }

    /**
     * 标记推荐为已消费（→ consumed）
     * POST /api/adaptive/recommendations/{id}/consume
     */
    @PostMapping("/recommendations/{id}/consume")
    public Map<String, Object> consumeRecommendation(@PathVariable String id) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }
        boolean ok = adaptiveEngineService.consumeRecommendation(userId, id);
        return ok
                ? Map.of("success", true, "message", "已标记为已学习")
                : Map.of("success", false, "message", "推荐不存在");
    }
}
