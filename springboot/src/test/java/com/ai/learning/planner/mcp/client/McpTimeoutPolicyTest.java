package com.ai.learning.planner.mcp.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 动态超时策略测试（读超时 < 写超时）
 */
class McpTimeoutPolicyTest {

    @Test
    void writeTool_usesLongerTimeout() {
        assertTrue(McpTimeoutPolicy.isWriteTool("delete_user"));
        assertTrue(McpTimeoutPolicy.isWriteTool("write_file"));
        assertTrue(McpTimeoutPolicy.isWriteTool("exec_command"));
        assertFalse(McpTimeoutPolicy.isWriteTool("search_resources"));
        assertFalse(McpTimeoutPolicy.isWriteTool("query_knowledge_graph"));
    }

    @Test
    void writeTimeout_greaterThanReadTimeout() {
        McpTimeoutPolicy policy = new McpTimeoutPolicy();
        assertTrue(policy.writeTimeoutMs("delete_user") > policy.readTimeoutMs("delete_user"),
                "写操作写超时必须大于读超时");
    }

    @Test
    void customTimeout_configuredPerTool() {
        McpTimeoutPolicy policy = new McpTimeoutPolicy();
        policy.configure("search_resources", 3000, 8000);
        assertEquals(3000, policy.readTimeoutMs("search_resources"));
        assertEquals(8000, policy.writeTimeoutMs("search_resources"));
    }

    @Test
    void customTimeout_rejectsInvalidConfig() {
        McpTimeoutPolicy policy = new McpTimeoutPolicy();
        assertThrows(IllegalArgumentException.class,
                () -> policy.configure("bad", 5000, 3000),
                "读超时 >= 写超时应拒绝配置");
    }
}
