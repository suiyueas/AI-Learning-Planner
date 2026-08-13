package com.ai.learning.planner.agent.reasoning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态重规划触发器测试
 */
class ReplanningTriggerTest {

    @Test
    void consecutiveFailures_twoTriggersReplan() {
        ReplanningTrigger trigger = new ReplanningTrigger();
        trigger.recordToolResult(false);
        assertFalse(trigger.shouldReplanByFailure());
        trigger.recordToolResult(false);
        assertTrue(trigger.shouldReplanByFailure());
        assertEquals(2, trigger.getConsecutiveFailures());
    }

    @Test
    void success_resetsFailureCounter() {
        ReplanningTrigger trigger = new ReplanningTrigger();
        trigger.recordToolResult(false);
        trigger.recordToolResult(false);
        trigger.recordToolResult(true);
        assertFalse(trigger.shouldReplanByFailure());
        assertEquals(0, trigger.getConsecutiveFailures());
    }

    @Test
    void contextOverThreshold_triggersReplan() {
        ReplanningTrigger trigger = new ReplanningTrigger(0.85);
        assertFalse(trigger.shouldReplanByContext(0.8));
        assertTrue(trigger.shouldReplanByContext(0.9));
    }

    @Test
    void reset_clearsState() {
        ReplanningTrigger trigger = new ReplanningTrigger();
        trigger.recordToolResult(false);
        trigger.recordToolResult(false);
        trigger.reset();
        assertFalse(trigger.shouldReplanByFailure());
        assertEquals(0, trigger.getConsecutiveFailures());
    }
}
