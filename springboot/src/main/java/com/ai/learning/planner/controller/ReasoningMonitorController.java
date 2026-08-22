package com.ai.learning.planner.controller;

import com.ai.learning.planner.agent.reasoning.ReasoningMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 推理链监控端点
 * 暴露 ReasoningMonitor 的 Micrometer 指标，供前端可视化或运维观测
 */
@RestController
@RequestMapping("/api/reasoning")
@RequiredArgsConstructor
public class ReasoningMonitorController {

    private final ReasoningMonitor monitor;
    private final MeterRegistry meterRegistry;

    /**
     * 获取推理链监控指标快照
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("steps", monitor.getStepCount());
        metrics.put("pruneCount", monitor.getPruneCount());
        metrics.put("replanCount", monitor.getReplanCount());
        metrics.put("toolFailureCount", monitor.getToolFailureCount());
        metrics.put("toolFallbackCount", monitor.getToolFallbackCount());
        metrics.put("stalledReflectionCount", monitor.getStalledReflectionCount());
        metrics.put("toolDurationP95", getToolDurationP95());
        metrics.put("snapshot", monitor.snapshot());
        return ResponseEntity.ok(metrics);
    }

    private double getToolDurationP95() {
        Timer timer = meterRegistry.find("reasoning.tool.duration").timer();
        if (timer == null) return 0.0;
        io.micrometer.core.instrument.distribution.ValueAtPercentile[] percentiles = timer.takeSnapshot().percentileValues();
        for (var pv : percentiles) {
            if (pv.percentile() == 0.95) return pv.value();
        }
        return percentiles.length > 0 ? percentiles[percentiles.length - 1].value() : 0.0;
    }
}
