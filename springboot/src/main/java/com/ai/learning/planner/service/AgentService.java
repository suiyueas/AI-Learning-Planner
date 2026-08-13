package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.AgentExecution;
import com.ai.learning.planner.repository.AgentExecutionRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent服务
 * 管理Agent执行日志和结果的保存与查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentExecutionRepository agentExecutionRepository;
    private final SecurityContextHolder securityContextHolder;

    private String getCurrentUserId() {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            log.warn("[AgentService] 无法获取当前用户ID，部分功能可能受限");
        }
        return userId;
    }

    @Transactional
    public AgentExecution saveLog(String agentId, String taskDescription,
                                  String stepType, Integer stepNumber,
                                  String message, String status) {
        String userId = getCurrentUserId();
        AgentExecution entry = AgentExecution.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .recordType("LOG")
                .agentId(agentId)
                .taskDescription(taskDescription)
                .title(stepType)
                .content(message)
                .type(stepType)
                .stepNumber(stepNumber)
                .status(status)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        AgentExecution saved = agentExecutionRepository.save(entry);
        log.debug("[AgentService] saveLog: agentId={}, stepType={}, stepNumber={}", agentId, stepType, stepNumber);
        return saved;
    }

    @Transactional
    public List<AgentExecution> saveLogs(List<AgentExecution> logs) {
        return agentExecutionRepository.saveAll(logs);
    }

    // ===== 用户隔离的查询方法（管理员全局视图） =====

    public List<AgentExecution> getLogsByAgentId(String agentId) {
        // 管理员查看所有用户的日志
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findActiveByAgentIdAndRecordType(agentId, "LOG");
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findByUserIdAndAgentIdAndRecordTypeOrderByCreatedAtDesc(userId, agentId, "LOG");
        }
        return Collections.emptyList();
    }

    public List<AgentExecution> getLogsBySessionId(String sessionId) {
        // 管理员查看所有用户的日志
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findBySessionIdOrderByStepOrderAsc(sessionId);
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findActiveByUserIdAndSessionId(userId, sessionId);
        }
        return Collections.emptyList();
    }

    public List<AgentExecution> getLogsByExecutionId(String executionId) {
        // 管理员查看所有用户的日志
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findByExecutionIdOrderByStepOrderAsc(executionId);
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findActiveByUserIdAndExecutionId(userId, executionId);
        }
        return Collections.emptyList();
    }

    @Transactional
    public List<AgentExecution> saveExecutionLogs(String sessionId, String executionId,
                                                   String agentId, String agentName,
                                                   String taskDescription,
                                                   List<Map<String, Object>> steps,
                                                   String output) {
        String userId = getCurrentUserId();
        List<AgentExecution> logs = new ArrayList<>();
        if (steps == null) return logs;
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String phase = (String) step.getOrDefault("phase", "info");
            String content = (String) step.getOrDefault("content", "");
            if (content == null || content.isEmpty()) continue;
            AgentExecution log = AgentExecution.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .recordType("LOG")
                    .sessionId(sessionId)
                    .executionId(executionId)
                    .agentId(agentId)
                    .agentName(agentName)
                    .taskDescription(taskDescription)
                    .title(phase)
                    .content(content)
                    .type(phase)
                    .stepOrder(i + 1)
                    .phase(phase)
                    .status("success")
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            logs.add(log);
        }
        List<AgentExecution> saved = agentExecutionRepository.saveAll(logs);
        if (output != null && !output.isEmpty() && !saved.isEmpty()) {
            AgentExecution lastLog = saved.get(saved.size() - 1);
            lastLog.setOutput(output);
            agentExecutionRepository.save(lastLog);
        }
        return saved;
    }

    public List<AgentExecution> getAllLogs() {
        // 管理员查看所有用户的日志
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findActiveByRecordType("LOG");
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findActiveByUserIdAndRecordType(userId, "LOG");
        }
        return Collections.emptyList();
    }

    @Transactional
    public void deleteAllLogs() {
        String userId = getCurrentUserId();
        if (userId != null) {
            List<AgentExecution> logs = agentExecutionRepository.findActiveByUserIdAndRecordType(userId, "LOG");
            agentExecutionRepository.deleteAll(logs);
        }
    }

    public Optional<AgentExecution> getResultById(String id) {
        // 管理员可读取任意用户的执行结果
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findActiveById(id);
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            // 用户隔离：只能读取属于自己的执行结果
            return agentExecutionRepository.findActiveByIdAndUserId(id, userId);
        }
        return Optional.empty();
    }

    @Transactional
    public AgentExecution saveResult(String agentId, String agentName, String taskDescription,
                                      String resultType, String resultContent, String resultSummary,
                                      Long duration, String status) {
        String userId = getCurrentUserId();
        AgentExecution result = AgentExecution.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .recordType("RESULT")
                .agentId(agentId)
                .agentName(agentName)
                .taskDescription(taskDescription)
                .resultType(resultType)
                .resultContent(resultContent)
                .resultSummary(resultSummary)
                // 输出内容与结果内容一致，供前端任务执行流直接展示（避免“输出：无返回内容”）
                .output(resultContent)
                .duration(duration)
                .status(status != null ? status : "completed")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        AgentExecution saved = agentExecutionRepository.save(result);
        log.info("[AgentService] saveResult: agentId={}, type={}", agentId, resultType);
        return saved;
    }

    public List<AgentExecution> getResultsByAgentId(String agentId) {
        // 管理员查看所有用户的执行结果
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findActiveByAgentIdAndRecordType(agentId, "RESULT");
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findActiveByUserIdAndAgentIdAndRecordType(userId, agentId, "RESULT");
        }
        return Collections.emptyList();
    }

    public Optional<AgentExecution> getLatestResultByAgentId(String agentId) {
        // 管理员查看所有用户的执行结果
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findTopByAgentIdAndRecordTypeOrderByCreatedAtDesc(agentId, "RESULT");
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findTopByUserIdAndAgentIdAndRecordTypeOrderByCreatedAtDesc(userId, agentId, "RESULT");
        }
        return Optional.empty();
    }

    public List<AgentExecution> getResultsByType(String resultType) {
        // 管理员查看所有用户的执行结果
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findByResultTypeOrderByCreatedAtDesc(resultType);
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findByUserIdAndResultTypeOrderByCreatedAtDesc(userId, resultType);
        }
        return Collections.emptyList();
    }

    public List<AgentExecution> getAllResults() {
        // 管理员查看所有用户的执行结果
        if (securityContextHolder.isAdmin()) {
            return agentExecutionRepository.findActiveByRecordType("RESULT");
        }
        String userId = getCurrentUserId();
        if (userId != null) {
            return agentExecutionRepository.findActiveByUserIdAndRecordType(userId, "RESULT");
        }
        return Collections.emptyList();
    }

    /**
     * 软删除单条执行结果（is_deleted=1，仅限当前用户自己的记录）
     */
    @Transactional
    public boolean deleteResultById(String id) {
        String userId = getCurrentUserId();
        if (userId == null) return false;
        return agentExecutionRepository.softDeleteByIdAndUserId(id, userId) > 0;
    }

    /**
     * 批量软删除执行结果（is_deleted=1，仅限当前用户自己的记录）
     */
    @Transactional
    public int deleteResultsByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        String userId = getCurrentUserId();
        if (userId == null) return 0;
        return agentExecutionRepository.softDeleteByIdsAndUserId(ids, userId);
    }

    /**
     * 清空当前用户所有执行结果（软删除，便于审计与恢复）
     */
    @Transactional
    public void deleteAllResults() {
        String userId = getCurrentUserId();
        if (userId != null) {
            List<AgentExecution> results = agentExecutionRepository.findActiveByUserIdAndRecordType(userId, "RESULT");
            agentExecutionRepository.softDeleteByIds(results.stream().map(AgentExecution::getId).toList());
        }
    }

    /**
     * 硬删除当前用户所有执行结果（物理删除，不可恢复）
     */
    @Transactional
    public void hardDeleteAllResults() {
        String userId = getCurrentUserId();
        if (userId == null) return;
        List<AgentExecution> results = agentExecutionRepository.findActiveByUserIdAndRecordType(userId, "RESULT");
        agentExecutionRepository.hardDeleteByIdsAndUserId(results.stream().map(AgentExecution::getId).toList(), userId);
    }

    /**
     * 当前用户活动执行结果计数（与列表查询同数据源，保证计数与列表一致）
     */
    public long countActiveResults() {
        String userId = getCurrentUserId();
        if (userId == null) return 0;
        return agentExecutionRepository.countActiveByUserIdAndRecordType(userId, "RESULT");
    }

    /**
     * 回收站列表（软删除的执行结果，仅当前用户）
     */
    public List<AgentExecution> getTrashResults() {
        String userId = getCurrentUserId();
        if (userId == null) return Collections.emptyList();
        return agentExecutionRepository.findDeletedByUserIdAndRecordType(userId, "RESULT");
    }

    /**
     * 恢复软删除的执行结果（仅限当前用户自己的记录）
     */
    @Transactional
    public boolean restoreResult(String id) {
        String userId = getCurrentUserId();
        if (userId == null) return false;
        return agentExecutionRepository.restoreByIdAndUserId(id, userId) > 0;
    }

    /**
     * 硬删除（物理删除，不可恢复，仅限当前用户自己的记录）
     */
    @Transactional
    public boolean hardDeleteResult(String id) {
        String userId = getCurrentUserId();
        if (userId == null) return false;
        return agentExecutionRepository.hardDeleteByIdAndUserId(id, userId) > 0;
    }

    /**
     * 批量硬删除（物理删除，不可恢复，仅限当前用户自己的记录）
     */
    @Transactional
    public int hardDeleteResults(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        String userId = getCurrentUserId();
        if (userId == null) return 0;
        return agentExecutionRepository.hardDeleteByIdsAndUserId(ids, userId);
    }
}