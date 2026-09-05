package com.ai.learning.planner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ai.learning.planner.dto.LearningSessionDTO;
import com.ai.learning.planner.dto.SessionPhaseDTO;
import com.ai.learning.planner.entity.LearningSession;
import com.ai.learning.planner.entity.SessionPhase;
import com.ai.learning.planner.repository.LearningSessionRepository;
import com.ai.learning.planner.repository.SessionPhaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习会话服务
 * 管理学习会话的创建、查询、阶段推进等核心逻辑
 */
@Service
@Slf4j
public class LearningSessionService {

    private final LearningSessionRepository sessionRepository;
    private final SessionPhaseRepository phaseRepository;
    private final ModelManager modelManager;

    public LearningSessionService(LearningSessionRepository sessionRepository,
                                   SessionPhaseRepository phaseRepository,
                                   ModelManager modelManager) {
        this.sessionRepository = sessionRepository;
        this.phaseRepository = phaseRepository;
        this.modelManager = modelManager;
    }

    /** 阶段执行顺序 */
    private static final List<String> PHASE_ORDER = List.of(
            "diagnosis", "planning", "learning", "exercise", "report"
    );

    /** 阶段对应的 Agent ID */
    private static final Map<String, String> PHASE_AGENT_MAP = Map.of(
            "diagnosis", "diagnosis",
            "planning", "planner",
            "learning", "tutor",
            "exercise", "exercise",
            "report", "reporter"
    );

    /**
     * 创建学习会话
     */
    @Transactional
    public LearningSession createSession(String userId, String goal) {
        LearningSession session = LearningSession.builder()
                .userId(userId)
                .goal(goal)
                .phase("diagnosis")
                .status("active")
                .progress(0)
                .build();

        session = sessionRepository.save(session);

        // 初始化所有阶段记录
        for (String phaseId : PHASE_ORDER) {
            SessionPhase phase = SessionPhase.builder()
                    .session(session)
                    .phaseId(phaseId)
                    .agentId(PHASE_AGENT_MAP.get(phaseId))
                    .status("pending")
                    .build();
            phaseRepository.save(phase);
        }

        log.info("创建学习会话: userId={}, sessionId={}, goal={}", userId, session.getId(), goal);
        return session;
    }

    /**
     * 获取会话详情（含阶段数据）
     */
    @Transactional(readOnly = true)
    public LearningSession getSession(Long sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));
        // 触发 phases 集合初始化，避免 LazyInitializationException
        session.getPhases().size();
        return session;
    }

    /**
     * 获取用户所有会话列表
     */
    @Transactional(readOnly = true)
    public List<LearningSession> listSessions(String userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取用户所有会话 DTO 列表（在事务内初始化 phases，避免 LazyInitializationException）
     */
    @Transactional(readOnly = true)
    public List<LearningSessionDTO> listSessionDTOs(String userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(session -> {
                    // 在事务内初始化 phases 集合
                    if (session.getPhases() != null) {
                        session.getPhases().size();
                    }
                    return toDTO(session);
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新阶段状态
     */
    @Transactional
    public void updatePhaseStatus(Long sessionId, String phaseId, String status, String outputJson) {
        SessionPhase phase = phaseRepository.findBySession_IdAndPhaseId(sessionId, phaseId)
                .orElseThrow(() -> new RuntimeException("阶段不存在: " + phaseId));

        phase.setStatus(status);
        if (outputJson != null) {
            phase.setOutputJson(outputJson);
        }
        if ("completed".equals(status) || "failed".equals(status)) {
            phase.setCompletedAt(LocalDateTime.now());
        }
        phaseRepository.save(phase);

        // 更新会话进度
        updateSessionProgress(sessionId);
    }

    /**
     * 自动进入下一阶段
     */
    @Transactional
    public String transitionPhase(Long sessionId) {
        LearningSession session = getSession(sessionId);
        String currentPhase = session.getPhase();
        int currentIndex = PHASE_ORDER.indexOf(currentPhase);

        if (currentIndex < 0 || currentIndex >= PHASE_ORDER.size() - 1) {
            // 已是最后阶段，标记完成
            session.setStatus("completed");
            session.setProgress(100);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("学习会话完成: sessionId={}", sessionId);
            return null;
        }

        String nextPhase = PHASE_ORDER.get(currentIndex + 1);
        session.setPhase(nextPhase);
        sessionRepository.save(session);

        log.info("会话阶段推进: sessionId={}, {} -> {}", sessionId, currentPhase, nextPhase);
        return nextPhase;
    }

    /**
     * 提交阶段数据
     * 仅当 type 为 diagnosis_complete / phase_complete 时标记阶段为 completed，
     * 普通 answer 提交只存储数据，不改变阶段状态
     */
    @Transactional
    public void submitPhaseData(Long sessionId, Map<String, Object> data) {
        LearningSession session = getSession(sessionId);
        String currentPhase = session.getPhase();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(data);

            // 仅当显式标记完成时，才更新阶段状态为 completed
            String type = data != null ? (String) data.get("type") : null;
            if ("diagnosis_complete".equals(type) || "phase_complete".equals(type)) {
                String status = data.containsKey("status") ? (String) data.get("status") : "completed";
                updatePhaseStatus(sessionId, currentPhase, status, json);
            } else {
                // 普通数据提交（如单题答案），只更新 outputJson 不改变状态
                SessionPhase phase = phaseRepository.findBySession_IdAndPhaseId(sessionId, currentPhase)
                        .orElseThrow(() -> new RuntimeException("阶段不存在: " + currentPhase));
                phase.setOutputJson(json);
                phaseRepository.save(phase);
            }
        } catch (Exception e) {
            log.error("序列化阶段数据失败: {}", e.getMessage());
            // 降级存储
            String type = data != null ? (String) data.get("type") : null;
            if ("diagnosis_complete".equals(type) || "phase_complete".equals(type)) {
                updatePhaseStatus(sessionId, currentPhase, "completed", data.toString());
            }
        }
    }

    /**
     * 更新会话整体进度（基于已完成阶段数）
     */
    private void updateSessionProgress(Long sessionId) {
        LearningSession session = getSession(sessionId);
        List<SessionPhase> phases = phaseRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);

        long completedCount = phases.stream()
                .filter(p -> "completed".equals(p.getStatus()))
                .count();

        int progress = (int) ((completedCount * 100) / PHASE_ORDER.size());
        session.setProgress(Math.min(progress, 100));
        sessionRepository.save(session);
    }

    /**
     * Entity -> DTO 转换
     */
    public LearningSessionDTO toDTO(LearningSession session) {
        List<SessionPhaseDTO> phaseDTOs = session.getPhases() != null
                ? session.getPhases().stream().map(this::toPhaseDTO).collect(Collectors.toList())
                : List.of();

        return LearningSessionDTO.builder()
                .id(session.getId())
                .goal(session.getGoal())
                .phase(session.getPhase())
                .status(session.getStatus())
                .progress(session.getProgress())
                .contextJson(session.getContextJson())
                .phases(phaseDTOs)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    private SessionPhaseDTO toPhaseDTO(SessionPhase phase) {
        return SessionPhaseDTO.builder()
                .id(phase.getId())
                .phaseId(phase.getPhaseId())
                .agentId(phase.getAgentId())
                .status(phase.getStatus())
                .outputJson(phase.getOutputJson())
                .durationMs(phase.getDurationMs())
                .createdAt(phase.getCreatedAt())
                .completedAt(phase.getCompletedAt())
                .build();
    }

    /**
     * 创建会话并返回 DTO（在事务内完成 DTO 转换，避免 LazyInitializationException）
     */
    @Transactional
    public LearningSessionDTO createSessionDTO(String userId, String goal) {
        LearningSession session = createSession(userId, goal);
        return toDTO(session);
    }

    /**
     * 获取会话详情并返回 DTO（在事务内初始化 phases）
     */
    @Transactional(readOnly = true)
    public LearningSessionDTO getSessionDTO(Long sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));
        session.getPhases().size();
        return toDTO(session);
    }

    /**
     * 删除会话（级联删除所有阶段数据）
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        sessionRepository.deleteById(sessionId);
        log.info("删除学习会话: sessionId={}", sessionId);
    }

    /**
     * 获取指定阶段
     */
    @Transactional(readOnly = true)
    public SessionPhase getPhase(Long sessionId, String phaseId) {
        return phaseRepository.findBySession_IdAndPhaseId(sessionId, phaseId)
                .orElse(null);
    }

    /**
     * 使用 AI 模型生成诊断题目
     */
    public String generateDiagnosisQuestions(String goal) {
        try {
            ChatClient chatClient = ChatClient.builder(modelManager.getCurrentModel())
                    .defaultSystem("""
                        你是一个专业的学习诊断专家。请根据用户的学习目标，生成5道诊断题目。
                        题目要求：
                        1. 包含选择题（4个选项）和问答题两种类型
                        2. 选择题要有正确答案索引和解析
                        3. 题目难度要循序渐进
                        4. 旨在评估用户当前的知识水平
                        5. 必须返回严格的 JSON 数组格式，不要包含任何其他文字
                        
                        返回格式示例：
                        [
                          {
                            "id": 1,
                            "type": "choice",
                            "content": "题目内容",
                            "options": ["选项A", "选项B", "选项C", "选项D"],
                            "correctIndex": 0,
                            "explanation": "解析内容"
                          },
                          {
                            "id": 2,
                            "type": "open",
                            "content": "问答题内容",
                            "explanation": "参考答案要点"
                          }
                        ]
                        """)
                    .build();

            String response = chatClient.prompt()
                    .user("用户的学习目标是：" + goal + "，请根据这个目标生成5道诊断题目。")
                    .call()
                    .content();

            log.info("AI 生成诊断题目成功: goal={}, responseLength={}", goal, response != null ? response.length() : 0);

            // 清理响应，确保是有效的 JSON
            if (response != null) {
                response = response.trim();
                // 移除可能的 markdown 代码块标记
                if (response.startsWith("```")) {
                    response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                }
                // 验证 JSON 格式
                try {
                    new ObjectMapper().readTree(response);
                } catch (JsonProcessingException e) {
                    log.warn("AI 生成的题目不是合法 JSON，尝试修复: {}", e.getMessage());
                    // 如果生成失败，返回默认题目
                    response = getDefaultDiagnosisQuestions(goal);
                }
            } else {
                response = getDefaultDiagnosisQuestions(goal);
            }

            return response;
        } catch (Exception e) {
            log.error("AI 生成诊断题目失败: {}", e.getMessage());
            return getDefaultDiagnosisQuestions(goal);
        }
    }

    /**
     * 获取默认诊断题目（AI 生成失败时的降级方案）
     */
    private String getDefaultDiagnosisQuestions(String goal) {
        return """
            [
              {
                "id": 1,
                "type": "choice",
                "content": "关于「%s」，你目前的知识水平如何？",
                "options": ["完全不了解", "了解基本概念", "有一定的实践经验", "熟练掌握并能够独立完成项目"],
                "correctIndex": 0,
                "explanation": "这个问题帮助评估你的起点水平，后续学习路径会根据你的选择进行调整。"
              },
              {
                "id": 2,
                "type": "choice",
                "content": "你每天大概能投入多少时间学习？",
                "options": ["少于30分钟", "30分钟-1小时", "1-2小时", "2小时以上"],
                "correctIndex": 0,
                "explanation": "了解你的时间安排有助于制定合理的学习计划。"
              },
              {
                "id": 3,
                "type": "choice",
                "content": "你偏向哪种学习方式？",
                "options": ["阅读文档", "观看视频", "动手实践", "与他人讨论交流"],
                "correctIndex": 0,
                "explanation": "了解你的学习偏好有助于推荐最适合的学习资源。"
              },
              {
                "id": 4,
                "type": "open",
                "content": "请简要描述你之前与「%s」相关的学习或工作经验。",
                "explanation": "了解你的背景有助于AI更精准地评估你的水平。"
              },
              {
                "id": 5,
                "type": "open",
                "content": "你希望通过学习「%s」达到什么样的具体目标？",
                "explanation": "明确的目标有助于制定更精准的学习路径。"
              }
            ]
            """.formatted(goal, goal, goal);
    }
}