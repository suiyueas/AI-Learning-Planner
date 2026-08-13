package com.ai.learning.planner.agent.reasoning;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 推理过程监控（Micrometer 埋点）
 * - reasoning.steps.total：推理步数
 * - reasoning.tool.duration：工具调用耗时
 * - reasoning.context.compression.ratio：上下文压缩率
 * - reasoning.prune.count / replan.count / reflection.count：纠错行为计数
 */
@Slf4j
public class ReasoningMonitor {

    private final MeterRegistry meterRegistry;

    private final Counter stepCounter;
    private final Counter pruneCounter;
    private final Counter replanCounter;
    private final Counter toolFailCounter;
    private final Counter toolFallbackCounter;
    private final Counter stalledReflectionCounter;
    private final Timer toolTimer;

    public ReasoningMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.stepCounter = meterRegistry.counter("reasoning.steps.total");
        this.pruneCounter = meterRegistry.counter("reasoning.prune.count");
        this.replanCounter = meterRegistry.counter("reasoning.replan.count");
        this.toolFailCounter = meterRegistry.counter("reasoning.tool.failure.count");
        this.toolFallbackCounter = meterRegistry.counter("reasoning.tool.fallback.count");
        this.stalledReflectionCounter = meterRegistry.counter("reasoning.reflection.stalled.count");
        this.toolTimer = Timer.builder("reasoning.tool.duration")
                .description("MCP 工具调用耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /**
     * 记录推理步数
     */
    public void recordStep() {
        stepCounter.increment();
    }

    /**
     * 记录剪枝事件
     */
    public void recordPrune() {
        pruneCounter.increment();
    }

    /**
     * 记录动态重规划事件
     */
    public void recordReplan() {
        replanCounter.increment();
    }

    /**
     * 记录工具调用耗时（含重试总耗时）
     */
    public void recordToolDuration(long durationMs) {
        toolTimer.record(Duration.ofMillis(durationMs));
    }

    /**
     * 记录工具失败（触发重试/降级的根因）
     */
    public void recordToolFailure() {
        toolFailCounter.increment();
    }

    /**
     * 记录工具降级执行（X-Fallback）
     */
    public void recordToolFallback() {
        toolFallbackCounter.increment();
    }

    /**
     * 记录反思状态
     */
    public void recordReflection(ReflectionResult.Status status) {
        if (status == ReflectionResult.Status.stalled) {
            stalledReflectionCounter.increment();
        }
    }

    /**
     * 上报上下文压缩率（gauge）
     */
    public void recordCompressionRatio(double ratio) {
        meterRegistry.gauge("reasoning.context.compression.ratio", ratio);
    }

    /**
     * 获取指标快照（调试/测试用）
     */
    public String snapshot() {
        return "steps=%s prune=%s replan=%s toolFail=%s fallback=%s stalledReflect=%s toolDur(p95)=%sms".formatted(
                getStepCount(), getPruneCount(), getReplanCount(), getToolFailureCount(),
                getToolFallbackCount(), getStalledReflectionCount(),
                toolTimer.takeSnapshot().percentileValues().length > 0
                        ? toolTimer.takeSnapshot().percentileValues()[0].value() : 0);
    }

    public long getStepCount() {
        return (long) stepCounter.count();
    }

    public long getPruneCount() {
        return (long) pruneCounter.count();
    }

    public long getReplanCount() {
        return (long) replanCounter.count();
    }

    public long getToolFailureCount() {
        return (long) toolFailCounter.count();
    }

    public long getToolFallbackCount() {
        return (long) toolFallbackCounter.count();
    }

    public long getStalledReflectionCount() {
        return (long) stalledReflectionCounter.count();
    }
}
