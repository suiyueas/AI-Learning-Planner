package com.ai.learning.planner.controller;

import com.ai.learning.planner.agent.dto.ThinkingProcess;
import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.entity.ReasoningTraceEntity;
import com.ai.learning.planner.service.ReasoningTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 思考轨迹控制器
 * 提供思考链路的查询、历史追溯等接口
 */
@RestController
@RequestMapping("/reasoning-trace")
@Slf4j
@RequiredArgsConstructor
public class ReasoningTraceController {

    private final ReasoningTraceService reasoningTraceService;

    /**
     * 获取思考轨迹历史
     */
    @GetMapping("/history")
    public ApiResponse<List<ReasoningTraceEntity>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("获取思考轨迹历史: page={}, size={}", page, size);
        List<ReasoningTraceEntity> history = reasoningTraceService.getTraceHistory(page, size);
        return ApiResponse.success(history);
    }

    /**
     * 获取指定的思考轨迹
     */
    @GetMapping("/{traceId}")
    public ApiResponse<Map<String, Object>> getTrace(@PathVariable String traceId) {
        log.info("获取思考轨迹: traceId={}", traceId);
        return reasoningTraceService.getTraceById(traceId)
                .map(entity -> {
                    List<ThinkingProcess> processes = reasoningTraceService.parseThinkingSteps(entity.getThinkingStepsJson());
                    return ApiResponse.success(Map.of(
                            "trace", entity,
                            "steps", processes
                    ));
                })
                .orElse(ApiResponse.notFound("思考轨迹不存在: " + traceId));
    }

    /**
     * 根据执行ID获取思考轨迹
     */
    @GetMapping("/execution/{executionId}")
    public ApiResponse<Map<String, Object>> getTraceByExecution(@PathVariable String executionId) {
        log.info("根据执行ID获取思考轨迹: executionId={}", executionId);
        return reasoningTraceService.getTraceByExecutionId(executionId)
                .map(entity -> {
                    List<ThinkingProcess> processes = reasoningTraceService.parseThinkingSteps(entity.getThinkingStepsJson());
                    return ApiResponse.success(Map.of(
                            "trace", entity,
                            "steps", processes
                    ));
                })
                .orElse(ApiResponse.notFound("执行对应的思考轨迹不存在: " + executionId));
    }

    /**
     * 删除思考轨迹
     */
    @DeleteMapping("/{traceId}")
    public ApiResponse<Void> deleteTrace(@PathVariable String traceId) {
        log.info("删除思考轨迹: traceId={}", traceId);
        reasoningTraceService.deleteTrace(traceId);
        return ApiResponse.success(null);
    }

    /**
     * 获取思考模式列表（供前端选择）
     */
    @GetMapping("/levels")
    public ApiResponse<List<Map<String, String>>> getReasoningLevels() {
        return ApiResponse.success(List.of(
                Map.of("value", "fast", "label", "快速模式", "description", "只展示最终结果，不展示推理过程（效率优先）"),
                Map.of("value", "standard", "label", "标准模式", "description", "展示关键推理步骤"),
                Map.of("value", "deep", "label", "深度思考", "description", "展示完整推理链路，含备选方案、反思、决策依据")
        ));
    }
}