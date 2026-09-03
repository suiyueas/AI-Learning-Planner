package com.ai.learning.planner.service;

import com.ai.learning.planner.agent.dto.ReasoningLevel;
import com.ai.learning.planner.agent.dto.ThinkingProcess;
import com.ai.learning.planner.agent.dto.ThinkingType;
import com.ai.learning.planner.entity.ReasoningTraceEntity;
import com.ai.learning.planner.repository.ReasoningTraceRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 思考轨迹服务
 * 管理智能体推理链路的跟踪、保存和查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReasoningTraceService {

    private final ReasoningTraceRepository reasoningTraceRepository;
    private final ObjectMapper objectMapper;
    private final SecurityContextHolder securityContextHolder;

    /** 当前会话的思考过程列表（线程安全） */
    private final Map<String, List<ThinkingProcess>> sessionThinkingCache = new HashMap<>();

    /**
     * 开始新的思考轨迹会话
     */
    public String startTrace(String sessionId, String userInput, String agentId, ReasoningLevel level) {
        String traceId = UUID.randomUUID().toString();
        List<ThinkingProcess> processes = new CopyOnWriteArrayList<>();
        sessionThinkingCache.put(traceId, processes);

        log.info("[ReasoningTrace] 开始思考轨迹: traceId={}, sessionId={}, agentId={}, level={}",
                traceId, sessionId, agentId, level);
        return traceId;
    }

    /**
     * 添加思考步骤
     */
    public void addThinkingStep(String traceId, ThinkingProcess step) {
        List<ThinkingProcess> processes = sessionThinkingCache.get(traceId);
        if (processes != null) {
            processes.add(step);
            log.debug("[ReasoningTrace] 添加思考步骤: traceId={}, type={}, step={}",
                    traceId, step.getType(), step.getStep());
        }
    }

    /**
     * 添加理解阶段
     */
    public void addUnderstanding(String traceId, String content, int step) {
        addThinkingStep(traceId, ThinkingProcess.understanding(content, step));
    }

    /**
     * 添加计划阶段
     */
    public void addPlanning(String traceId, String content, int step) {
        addThinkingStep(traceId, ThinkingProcess.planning(content, step));
    }

    /**
     * 添加思考
     */
    public void addThinking(String traceId, String content, int step) {
        addThinkingStep(traceId, ThinkingProcess.thinking(content, step));
    }

    /**
     * 添加行动
     */
    public void addAction(String traceId, String toolName, Object args, String result, int step) {
        addThinkingStep(traceId, ThinkingProcess.action(toolName, args, result, step));
    }

    /**
     * 添加观察
     */
    public void addObservation(String traceId, String content, int step) {
        addThinkingStep(traceId, ThinkingProcess.observation(content, step));
    }

    /**
     * 添加反思
     */
    public void addReflection(String traceId, String content, int step, Map<String, Object> metadata) {
        addThinkingStep(traceId, ThinkingProcess.reflection(content, step, metadata));
    }

    /**
     * 添加备选方案
     */
    public void addAlternative(String traceId, String content, int step) {
        addThinkingStep(traceId, ThinkingProcess.alternative(content, step));
    }

    /**
     * 添加执行步骤
     */
    public void addStep(String traceId, int stepNumber, String description, int step) {
        addThinkingStep(traceId, ThinkingProcess.step(stepNumber, description, step));
    }

    /**
     * 添加最终结论
     */
    public void addResult(String traceId, String content, int step) {
        addThinkingStep(traceId, ThinkingProcess.result(content, step));
    }

    /**
     * 添加错误
     */
    public void addError(String traceId, String content, int step) {
        addThinkingStep(traceId, ThinkingProcess.error(content, step));
    }

    /**
     * 获取当前思考轨迹
     */
    public List<ThinkingProcess> getThinkingProcess(String traceId) {
        return sessionThinkingCache.getOrDefault(traceId, new ArrayList<>());
    }

    /**
     * 保存思考轨迹到数据库
     */
    @Transactional
    public ReasoningTraceEntity saveTrace(String traceId, String executionId, String agentId,
                                          String userInput, String output, Long duration, String status) {
        List<ThinkingProcess> processes = sessionThinkingCache.remove(traceId);
        if (processes == null || processes.isEmpty()) {
            log.warn("[ReasoningTrace] 思考轨迹为空，跳过保存: traceId={}", traceId);
            return null;
        }

        String userId = securityContextHolder.getCurrentUserId();
        ReasoningLevel level = detectLevel(processes);

        String thinkingStepsJson;
        try {
            thinkingStepsJson = objectMapper.writeValueAsString(processes);
        } catch (JsonProcessingException e) {
            log.error("[ReasoningTrace] 序列化思考步骤失败: {}", e.getMessage());
            thinkingStepsJson = "[]";
        }

        ReasoningTraceEntity entity = ReasoningTraceEntity.builder()
                .id(traceId)
                .executionId(executionId)
                .userId(userId)
                .agentId(agentId)
                .reasoningLevel(level.getValue())
                .userInput(userInput)
                .thinkingStepsJson(thinkingStepsJson)
                .output(output)
                .duration(duration)
                .status(status)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        ReasoningTraceEntity saved = reasoningTraceRepository.save(entity);
        log.info("[ReasoningTrace] 保存思考轨迹: traceId={}, steps={}", traceId, processes.size());
        return saved;
    }

    /**
     * 根据思考过程检测使用的思考级别
     */
    private ReasoningLevel detectLevel(List<ThinkingProcess> processes) {
        Set<String> types = new HashSet<>();
        for (ThinkingProcess p : processes) {
            types.add(p.getType());
        }

        if (types.contains(ThinkingType.REFLECTION.getValue()) ||
            types.contains(ThinkingType.ALTERNATIVE.getValue()) ||
            types.contains(ThinkingType.UNDERSTANDING.getValue())) {
            return ReasoningLevel.DEEP;
        } else if (types.size() > 3) {
            return ReasoningLevel.STANDARD;
        }
        return ReasoningLevel.FAST;
    }

    /**
     * 获取用户的思考轨迹历史
     */
    public List<ReasoningTraceEntity> getTraceHistory(int page, int size) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ReasoningTraceEntity> result = reasoningTraceRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return result.getContent();
    }

    /**
     * 获取指定的思考轨迹
     */
    public Optional<ReasoningTraceEntity> getTraceById(String traceId) {
        return reasoningTraceRepository.findByIdAndIsDeletedFalse(traceId);
    }

    /**
     * 删除思考轨迹
     */
    @Transactional
    public void deleteTrace(String traceId) {
        reasoningTraceRepository.findById(traceId).ifPresent(entity -> {
            entity.setIsDeleted(true);
            reasoningTraceRepository.save(entity);
            log.info("[ReasoningTrace] 删除思考轨迹: traceId={}", traceId);
        });
    }

    /**
     * 根据执行ID获取思考轨迹
     */
    public Optional<ReasoningTraceEntity> getTraceByExecutionId(String executionId) {
        return reasoningTraceRepository.findByExecutionIdAndIsDeletedFalse(executionId);
    }

    /**
     * 解析思考步骤JSON为对象列表
     */
    public List<ThinkingProcess> parseThinkingSteps(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ThinkingProcess>>() {});
        } catch (JsonProcessingException e) {
            log.error("[ReasoningTrace] 解析思考步骤失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}