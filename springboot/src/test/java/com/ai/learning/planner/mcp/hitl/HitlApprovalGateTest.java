package com.ai.learning.planner.mcp.hitl;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 人工审批门禁（HITL）测试
 */
class HitlApprovalGateTest {

    @Test
    void requiresApproval_highRiskPrefixes() {
        HitlApprovalGate gate = new HitlApprovalGate();
        assertTrue(gate.requiresApproval("delete_user"));
        assertTrue(gate.requiresApproval("write_file"));
        assertTrue(gate.requiresApproval("exec_command"));
        assertTrue(gate.requiresApproval("remove_record"));
        assertFalse(gate.requiresApproval("search_resources"));
        assertFalse(gate.requiresApproval("query_knowledge_graph"));
        assertFalse(gate.requiresApproval(null));
    }

    @Test
    void requiresApproval_explicitRegistration() {
        HitlApprovalGate gate = new HitlApprovalGate();
        gate.registerHighRiskTool("generate_quiz");
        assertTrue(gate.requiresApproval("generate_quiz"));
    }

    @Test
    void waitForApproval_approvedByExternalSignal() throws Exception {
        HitlApprovalGate gate = new HitlApprovalGate();
        // 后台线程模拟外部审批信号
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            // 等待请求挂起后批准（轮询第一个挂起请求）
            for (int i = 0; i < 100 && gate.pendingCount() == 0; i++) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            String requestId = gate.pendingRequests().iterator().next();
            return gate.approve(requestId);
        });

        boolean approved = gate.waitForApproval("delete_user", Map.of("id", "1"), Duration.ofSeconds(5));
        assertTrue(approved);
        assertTrue(result.get());
        assertEquals(0, gate.pendingCount());
    }

    @Test
    void waitForApproval_denied() throws Exception {
        HitlApprovalGate gate = new HitlApprovalGate();
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < 100 && gate.pendingCount() == 0; i++) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            String requestId = gate.pendingRequests().iterator().next();
            return gate.deny(requestId);
        });

        boolean approved = gate.waitForApproval("exec_script", Map.of(), Duration.ofSeconds(5));
        assertFalse(approved);
        assertTrue(result.get());
    }

    @Test
    void waitForApproval_timeoutDenies() {
        HitlApprovalGate gate = new HitlApprovalGate();
        boolean approved = gate.waitForApproval("write_file", Map.of(), Duration.ofMillis(100));
        assertFalse(approved, "超时应默认拒绝");
        assertEquals(0, gate.pendingCount());
    }

    @Test
    void denyAll_clearsPending() {
        HitlApprovalGate gate = new HitlApprovalGate();
        CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < 100 && gate.pendingCount() == 0; i++) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            gate.denyAll();
            return true;
        });

        boolean approved = gate.waitForApproval("delete_x", Map.of(), Duration.ofSeconds(5));
        assertFalse(approved);
        assertEquals(0, gate.pendingCount());
    }
}
