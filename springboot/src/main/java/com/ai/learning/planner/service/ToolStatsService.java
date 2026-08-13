package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.ToolCallStats;
import com.ai.learning.planner.repository.ToolCallStatsRepository;
import com.ai.learning.planner.repository.ToolExecutionRecordRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具调用统计服务
 * 负责记录和查询每个工具的调用次数（总调用次数和会话调用次数）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ToolStatsService {

    private final ToolCallStatsRepository repository;
    private final ToolExecutionRecordRepository recordRepository;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 获取所有工具的调用统计（全局统计，供管理员使用）
     * 返回格式: { toolId: { totalCalls: N, lastCalled: "2024-01-01T12:00:00" } }
     */
    public Map<String, Map<String, Object>> getAllStats() {
        List<ToolCallStats> allStats = repository.findAll();
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (ToolCallStats stats : allStats) {
            Map<String, Object> item = new HashMap<>();
            item.put("totalCalls", stats.getTotalCalls() != null ? stats.getTotalCalls() : 0);
            item.put("sessionCalls", stats.getSessionCalls() != null ? stats.getSessionCalls() : 0);
            item.put("lastCalled", stats.getLastCalledAt() != null ? stats.getLastCalledAt().toString() : null);
            result.put(stats.getToolId(), item);
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
        Optional<ToolCallStats> statsOpt = repository.findByToolId(toolId);
        if (statsOpt.isEmpty()) {
            return Map.of(
                    "totalCalls", 0,
                    "sessionCalls", 0,
                    "lastCalled", null
            );
        }
        ToolCallStats stats = statsOpt.get();
        return Map.of(
                "totalCalls", stats.getTotalCalls() != null ? stats.getTotalCalls() : 0,
                "sessionCalls", stats.getSessionCalls() != null ? stats.getSessionCalls() : 0,
                "lastCalled", stats.getLastCalledAt() != null ? stats.getLastCalledAt().toString() : null
        );
    }

    /**
     * 记录一次工具调用
     * 总调用次数+1，会话调用次数+1
     * 如果该工具尚无统计记录，则自动创建
     *
     * @param toolId 工具ID
     * @return 更新后的总调用次数
     */
    @Transactional
    public int recordCall(String toolId) {
        ToolCallStats stats = repository.findByToolId(toolId).orElse(null);
        if (stats == null) {
            String toolName = getToolDisplayName(toolId);
            stats = ToolCallStats.builder()
                    .toolId(toolId)
                    .toolName(toolName)
                    .totalCalls(1)
                    .sessionCalls(1)
                    .lastCalledAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            stats.setTotalCalls((stats.getTotalCalls() != null ? stats.getTotalCalls() : 0) + 1);
            stats.setSessionCalls((stats.getSessionCalls() != null ? stats.getSessionCalls() : 0) + 1);
            stats.setLastCalledAt(LocalDateTime.now());
            stats.setUpdatedAt(LocalDateTime.now());
        }
        repository.save(stats);
        return stats.getTotalCalls() != null ? stats.getTotalCalls() : 0;
    }

    /**
     * 重置所有工具的会话调用次数（通常在用户登录或新会话开始时调用）
     */
    @Transactional
    public void resetSessionCalls() {
        List<ToolCallStats> allStats = repository.findAll();
        for (ToolCallStats stats : allStats) {
            stats.setSessionCalls(0);
            stats.setUpdatedAt(LocalDateTime.now());
        }
        repository.saveAll(allStats);
        log.info("已重置所有工具的会话调用次数");
    }

    /**
     * 获取工具显示名称
     */
    private String getToolDisplayName(String toolId) {
        return switch (toolId) {
            case "search_resources" -> "资源检索";
            case "web_search" -> "联网搜索";
            case "web_fetch" -> "网页抓取";
            case "query_knowledge_graph" -> "知识图谱查询";
            case "summarize_document" -> "文档摘要";
            case "extract_keywords" -> "知识点提取";
            case "generate_quiz" -> "生成测验题";
            case "translate_text" -> "文本翻译";
            default -> toolId;
        };
    }
}