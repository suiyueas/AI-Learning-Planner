package com.ai.learning.planner.service;

import com.ai.learning.planner.repository.ToolExecutionRecordRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用统计服务
 * 负责记录和查询每个工具的调用次数（从 tool_execution_records 表实时聚合）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ToolStatsService {

    private final ToolExecutionRecordRepository recordRepository;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 获取所有工具的调用统计（全局统计，供管理员使用）
     * 返回格式: { toolId: { totalCalls: N, lastCalled: "2024-01-01T12:00:00" } }
     */
    public Map<String, Map<String, Object>> getAllStats() {
        List<Object[]> results = recordRepository.countGroupByToolId();
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Object[] row : results) {
            String toolId = (String) row[0];
            Long count = (Long) row[1];
            Map<String, Object> item = new HashMap<>();
            item.put("totalCalls", count != null ? count : 0);
            item.put("sessionCalls", 0);
            item.put("lastCalled", null);
            result.put(toolId, item);
        }
        return result;
    }

    /**
     * 获取当前用户的工具调用统计（按用户隔离；管理员返回全局统计）
     * 返回格式: { toolId: { totalCalls: N, lastCalled: "2024-01-01T12:00:00" } }
     */
    public Map<String, Map<String, Object>> getUserStats() {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of();
        }

        // 管理员全局视图：统计所有用户的调用次数
        List<Object[]> results = securityContextHolder.isAdmin()
                ? recordRepository.countGroupByToolId()
                : recordRepository.countGroupByToolIdForUser(userId);
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Object[] row : results) {
            String toolId = (String) row[0];
            Long count = (Long) row[1];
            Map<String, Object> item = new HashMap<>();
            item.put("totalCalls", count != null ? count : 0);
            item.put("sessionCalls", 0);
            item.put("lastCalled", null);
            result.put(toolId, item);
        }
        return result;
    }

    /**
     * 获取单个工具当前用户的调用次数（按用户隔离；管理员返回全局次数）
     */
    public long getUserToolUsageCount(String toolId) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return 0;
        }
        // 管理员全局视图：统计所有用户的调用次数
        return securityContextHolder.isAdmin()
                ? recordRepository.countByToolId(toolId)
                : recordRepository.countByUserIdAndToolId(userId, toolId);
    }

    /**
     * 获取单个工具详细统计（全局统计，供管理员使用）
     */
    public Map<String, Object> getToolStats(String toolId) {
        long totalCalls = recordRepository.countByToolId(toolId);
        return Map.of(
                "totalCalls", totalCalls,
                "sessionCalls", 0,
                "lastCalled", null
        );
    }

    /**
     * 记录一次工具调用（由于执行记录已在 executeTool 时保存，此处仅返回更新后的总数）
     * 仅接受注册表中的真实工具 ID，防止伪造 toolId 刷全局统计
     */
    public int recordall(String toolId) {
        return (int) recordRepository.countByToolId(toolId);
    }
}