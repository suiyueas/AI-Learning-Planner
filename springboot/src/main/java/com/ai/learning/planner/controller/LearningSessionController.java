package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.dto.LearningSessionDTO;
import com.ai.learning.planner.entity.LearningSession;
import com.ai.learning.planner.entity.SessionPhase;
import com.ai.learning.planner.service.LearningSessionService;
import com.ai.learning.planner.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 学习会话控制器
 * 提供学习会话的创建、查询、SSE 流式执行、阶段提交等功能
 */
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
@Slf4j
public class LearningSessionController {

    private final LearningSessionService sessionService;

    /**
     * 并发控制锁：每个 sessionId 只能有一个线程在处理
     * 防止前端重复请求导致同一阶段被并发执行多次
     */
    private final ConcurrentHashMap<Long, AtomicBoolean> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 创建学习会话
     * POST /session/create
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<LearningSessionDTO>> createSession(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        String goal = body.getOrDefault("goal", "");

        log.info("创建学习会话: userId={}, goal={}", userId, goal);
        LearningSessionDTO dto = sessionService.createSessionDTO(userId, goal);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 获取会话详情
     * GET /session/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LearningSessionDTO>> getSession(
            @PathVariable Long id,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("获取学习会话: userId={}, sessionId={}", userId, id);

        LearningSession session = sessionService.getSession(id);
        if (!session.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权访问该会话"));
        }

        return ResponseEntity.ok(ApiResponse.success(sessionService.toDTO(session)));
    }

    /**
     * 获取用户会话列表
     * GET /session/list
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<LearningSessionDTO>>> listSessions(
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("获取用户会话列表: userId={}", userId);

        List<LearningSessionDTO> sessions = sessionService.listSessionDTOs(userId);

        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * 流式执行会话阶段
     * GET /session/{id}/stream?phase=diagnosis
     * SSE 推送：诊断题 → 用户答题 → 分析结果 → 自动进入下一阶段
     * 事件命名约定：使用下划线风格（如 session_completed），与前端 SSEClient 一致
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSession(
            @PathVariable Long id,
            @RequestParam String phase,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest request) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("SSE 流式执行: userId={}, sessionId={}, phase={}", userId, id, phase);
        log.info("SSE 请求详情: remoteAddr={}, requestURI={}", request.getRemoteAddr(), request.getRequestURI());

        LearningSession session = sessionService.getSession(id);
        if (!session.getUserId().equals(userId)) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("无权访问该会话"));
            return emitter;
        }

        // ★ 并发控制：使用 CAS 确保同一 sessionId 只有一个线程在处理
        AtomicBoolean isRunning = sessionLocks.computeIfAbsent(id, k -> new AtomicBoolean(false));
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("并发请求被拒绝: sessionId={}, phase={}, 该会话正在处理中", id, phase);
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of(
                                "code", "CONCURRENT_EXECUTION",
                                "message", "该会话报告正在生成中，请勿重复请求"
                        )));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        log.info("SSE 流式执行已获取锁: sessionId={}, phase={}", id, phase);

        SseEmitter emitter = new SseEmitter(600_000L); // 10 分钟超时

        // 在完成/超时/错误时释放锁
        Runnable releaseLock = () -> {
            AtomicBoolean lock = sessionLocks.get(id);
            if (lock != null) {
                lock.set(false);
                sessionLocks.remove(id);
            }
            log.info("SSE 流式执行释放锁: sessionId={}", id);
        };

        emitter.onCompletion(releaseLock);
        emitter.onTimeout(releaseLock);
        emitter.onError(e -> releaseLock.run());

        // 异步执行阶段逻辑
        Thread.startVirtualThread(() -> {
            try {
                // 仅在阶段真正切换时发送过渡事件（from != to），避免 report→report 等同阶段重复触发前端重连
                if (!session.getPhase().equals(phase)) {
                    emitter.send(SseEmitter.event()
                            .name("phase_transition")
                            .data(Map.of(
                                    "from", session.getPhase(),
                                    "to", phase,
                                    "message", "正在进入" + getPhaseLabel(phase) + "阶段..."
                            )));
                }

                // 根据阶段分发到对应的处理器
                switch (phase) {
                    case "diagnosis" -> handleDiagnosisPhase(id, session, emitter);
                    case "planning" -> handlePlanningPhase(id, emitter);
                    case "learning" -> handleLearningPhase(id, emitter);
                    case "exercise" -> handleExercisePhase(id, emitter);
                    case "report" -> handleReportPhase(id, session, emitter);
                    default -> {
                        log.warn("未知阶段: {}", phase);
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("code", "UNKNOWN_PHASE", "message", "未知阶段: " + phase)));
                    }
                }

                // 保持连接短暂存活，等待前端可能的重连
                Thread.sleep(2000);
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 流式执行失败: sessionId={}, error={}", id, e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                    "code", "EXECUTION_ERROR",
                                    "message", "执行失败：" + e.getMessage()
                            )));
                } catch (Exception ignored) {}
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
            } finally {
                releaseLock.run();
            }
        });

        return emitter;
    }

    /**
     * 诊断阶段处理：AI 生成诊断题目并推送
     */
    private void handleDiagnosisPhase(Long sessionId, LearningSession session, SseEmitter emitter) throws Exception {
        String goal = session.getGoal();

        // 使用 AI 模型生成诊断题目
        String questionsJson = sessionService.generateDiagnosisQuestions(goal);

        // 解析为对象列表，避免双重序列化
        List<Map<String, Object>> questionsList = new ObjectMapper()
                .readValue(questionsJson, new TypeReference<List<Map<String, Object>>>() {});

        // 推送题目事件
        emitter.send(SseEmitter.event()
                .name("question")
                .data(Map.of("questions", questionsList)));

        log.info("诊断题目已推送: sessionId={}, goal={}", sessionId, goal);

        // 等待用户提交答案（最长等待 8 分钟）
        long maxWaitMs = 480_000L;
        long pollIntervalMs = 2_000L;
        long waitedMs = 0L;

        while (waitedMs < maxWaitMs) {
            // 检查诊断阶段是否已完成（用户已提交答案）
            SessionPhase diagnosisPhase = sessionService.getPhase(sessionId, "diagnosis");
            if (diagnosisPhase != null && "completed".equals(diagnosisPhase.getStatus())) {
                log.info("诊断阶段已完成: sessionId={}", sessionId);

                // 推送阶段结果
                emitter.send(SseEmitter.event()
                        .name("phase_result")
                        .data(Map.of(
                                "phase", "diagnosis",
                                "status", "completed",
                                "message", "诊断完成，正在进入规划阶段..."
                        )));

                // 推进到下一阶段
                String nextPhase = sessionService.transitionPhase(sessionId);
                if (nextPhase != null) {
                    emitter.send(SseEmitter.event()
                            .name("phase_transition")
                            .data(Map.of(
                                    "from", "diagnosis",
                                    "to", nextPhase,
                                    "message", "诊断完成，正在进入" + getPhaseLabel(nextPhase) + "阶段..."
                            )));
                } else {
                    // 所有阶段已完成
                    emitter.send(SseEmitter.event()
                            .name("session_completed")
                            .data(Map.of(
                                    "sessionId", sessionId,
                                    "report", Map.of("message", "学习会话已完成！")
                            )));
                }
                return;
            }

            Thread.sleep(pollIntervalMs);
            waitedMs += pollIntervalMs;
        }

        log.warn("诊断阶段等待超时: sessionId={}", sessionId);
        // 超时，不关闭连接，让前端自行处理
    }

    /**
     * 规划阶段处理：生成学习计划并等待用户确认
     */
    private void handlePlanningPhase(Long sessionId, SseEmitter emitter) throws Exception {
        // 生成学习计划（调用 AI 或生成默认计划）
        Map<String, Object> plan = Map.of(
                "nodes", List.of(
                        Map.of("name", "基础入门", "description", "掌握核心基础概念", "duration", "1-2周", "difficulty", "初级"),
                        Map.of("name", "进阶提升", "description", "深入学习关键知识点", "duration", "2-3周", "difficulty", "中级"),
                        Map.of("name", "实战应用", "description", "通过项目实践巩固所学", "duration", "2-3周", "difficulty", "高级")
                ),
                "totalWeeks", "6-8周",
                "difficulty", "medium"
        );

        // 推送规划数据
        emitter.send(SseEmitter.event()
                .name("phase_data")
                .data(Map.of("phase", "planning", "plan", plan)));

        log.info("学习规划已推送: sessionId={}", sessionId);

        // 等待用户确认规划（最长等待 8 分钟）
        waitForPhaseCompletion(sessionId, "planning", emitter);
    }

    /**
     * 学习阶段处理：推送学习内容并等待用户完成
     */
    private void handleLearningPhase(Long sessionId, SseEmitter emitter) throws Exception {
        // 推送学习开始事件
        emitter.send(SseEmitter.event()
                .name("phase_data")
                .data(Map.of(
                        "phase", "learning",
                        "message", "学习阶段已开始，请按照学习计划逐步学习，可随时向 AI 提问解惑",
                        "tasks", List.of(
                                Map.of("title", "学习核心概念", "estimated", "30分钟"),
                                Map.of("title", "完成实践练习", "estimated", "45分钟"),
                                Map.of("title", "总结复盘", "estimated", "15分钟")
                        )
                )));

        log.info("学习阶段已开始: sessionId={}", sessionId);

        // 等待用户完成学习（最长等待 8 分钟）
        waitForPhaseCompletion(sessionId, "learning", emitter);
    }

    /**
     * 习题阶段处理：生成练习题并等待用户完成
     */
    private void handleExercisePhase(Long sessionId, SseEmitter emitter) throws Exception {
        // 生成练习题
        List<Map<String, Object>> exercises = List.of(
                Map.of("id", 1, "type", "choice", "content", "以下哪个选项描述了正确的概念？",
                        "options", List.of("选项 A 的描述", "选项 B 的描述", "选项 C 的描述", "选项 D 的描述"),
                        "correctIndex", 0, "explanation", "解析：A 是正确的，因为..."),
                Map.of("id", 2, "type", "choice", "content", "在实践应用中，以下哪种方法更高效？",
                        "options", List.of("方法一", "方法二", "方法三", "方法四"),
                        "correctIndex", 1, "explanation", "解析：方法二更高效，因为..."),
                Map.of("id", 3, "type", "choice", "content", "以下哪个是最佳实践？",
                        "options", List.of("最佳实践 A", "最佳实践 B", "最佳实践 C", "最佳实践 D"),
                        "correctIndex", 2, "explanation", "解析：C 是最佳实践，因为...")
        );

        emitter.send(SseEmitter.event()
                .name("phase_data")
                .data(Map.of("phase", "exercise", "exercises", exercises)));

        log.info("习题已推送: sessionId={}", sessionId);

        // 等待用户完成习题（最长等待 8 分钟）
        waitForPhaseCompletion(sessionId, "exercise", emitter);
    }

    /**
     * 报告阶段处理：生成学习报告数据
     * 幂等性：如果报告已生成，直接复用已有数据，不再重复生成
     */
    private void handleReportPhase(Long sessionId, LearningSession session, SseEmitter emitter) throws Exception {
        // ★ 幂等性检查：如果报告阶段已经完成且有输出数据，直接复用
        SessionPhase reportPhase = sessionService.getPhase(sessionId, "report");
        Map<String, Object> report = null;

        if (reportPhase != null && "completed".equals(reportPhase.getStatus()) && reportPhase.getOutputJson() != null) {
            log.info("报告已存在，直接复用: sessionId={}", sessionId);
            try {
                ObjectMapper mapper = new ObjectMapper();
                // outputJson 可能包含完整的 phase_data 包装，尝试提取 report 字段
                Map<String, Object> outputData = mapper.readValue(reportPhase.getOutputJson(),
                        new TypeReference<Map<String, Object>>() {});
                Object reportObj = outputData.get("report");
                if (reportObj instanceof Map) {
                    report = (Map<String, Object>) reportObj;
                } else if (reportObj instanceof String) {
                    report = mapper.readValue((String) reportObj,
                            new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception e) {
                log.warn("解析已有报告数据失败，重新生成: sessionId={}", sessionId);
            }
        }

        if (report == null) {
            // 生成报告数据
            report = Map.of(
                    "totalHours", 12,
                    "completedTasks", 8,
                    "accuracy", 85,
                    "streak", 5,
                    "trend", List.of(
                            Map.of("day", "周一", "hours", 1.5),
                            Map.of("day", "周二", "hours", 2.0),
                            Map.of("day", "周三", "hours", 1.0),
                            Map.of("day", "周四", "hours", 2.5),
                            Map.of("day", "周五", "hours", 1.5),
                            Map.of("day", "周六", "hours", 2.0),
                            Map.of("day", "周日", "hours", 1.5)
                    ),
                    "radar", List.of(
                            Map.of("name", "基础知识", "value", 85, "max", 100),
                            Map.of("name", "理解能力", "value", 78, "max", 100),
                            Map.of("name", "应用能力", "value", 72, "max", 100),
                            Map.of("name", "分析能力", "value", 68, "max", 100),
                            Map.of("name", "综合能力", "value", 75, "max", 100)
                    ),
                    "summary", "整体表现良好！你在基础知识方面掌握扎实，理解能力和综合能力处于中上水平。建议在分析能力和应用能力上多下功夫，通过更多实战项目来提升。",
                    "recommendations", List.of(
                            "多做综合性练习，提升知识迁移能力",
                            "尝试参与开源项目，积累实战经验",
                            "定期复习已学知识，巩固记忆",
                            "关注行业前沿动态，拓展视野"
                    )
            );
        }

        // 推送报告数据
        emitter.send(SseEmitter.event()
                .name("phase_data")
                .data(Map.of("phase", "report", "report", report)));

        // 标记报告阶段完成（仅在首次生成时标记）
        if (reportPhase == null || !"completed".equals(reportPhase.getStatus())) {
            sessionService.updatePhaseStatus(sessionId, "report", "completed", null);
        }
        String nextPhase = sessionService.transitionPhase(sessionId);

        // 推送阶段结果
        emitter.send(SseEmitter.event()
                .name("phase_result")
                .data(Map.of(
                        "phase", "report",
                        "status", "completed",
                        "report", report,
                        "message", "报告生成完成！"
                )));

        if (nextPhase == null) {
            // 所有阶段已完成
            emitter.send(SseEmitter.event()
                    .name("session_completed")
                    .data(Map.of(
                            "sessionId", sessionId,
                            "report", report,
                            "message", "学习会话已完成！"
                    )));
        }

        log.info("报告已生成推送: sessionId={}", sessionId);
    }

    /**
     * 通用等待阶段完成方法
     */
    private void waitForPhaseCompletion(Long sessionId, String phaseId, SseEmitter emitter) throws Exception {
        long maxWaitMs = 480_000L;
        long pollIntervalMs = 2_000L;
        long waitedMs = 0L;

        while (waitedMs < maxWaitMs) {
            SessionPhase phase = sessionService.getPhase(sessionId, phaseId);
            if (phase != null && "completed".equals(phase.getStatus())) {
                log.info("{} 阶段已完成: sessionId={}", phaseId, sessionId);

                // 推送阶段结果
                emitter.send(SseEmitter.event()
                        .name("phase_result")
                        .data(Map.of(
                                "phase", phaseId,
                                "status", "completed",
                                "message", getPhaseLabel(phaseId) + "阶段完成"
                        )));

                // 推进到下一阶段
                String nextPhase = sessionService.transitionPhase(sessionId);
                if (nextPhase != null) {
                    emitter.send(SseEmitter.event()
                            .name("phase_transition")
                            .data(Map.of(
                                    "from", phaseId,
                                    "to", nextPhase,
                                    "message", getPhaseLabel(phaseId) + "完成，正在进入" + getPhaseLabel(nextPhase) + "阶段..."
                            )));
                } else {
                    emitter.send(SseEmitter.event()
                            .name("session_completed")
                            .data(Map.of(
                                    "sessionId", sessionId,
                                    "message", "学习会话已完成！"
                            )));
                }
                return;
            }

            Thread.sleep(pollIntervalMs);
            waitedMs += pollIntervalMs;
        }

        log.warn("{} 阶段等待超时: sessionId={}", phaseId, sessionId);
    }

    /**
     * 提交阶段数据（如答题结果）
     * POST /session/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Void>> submitPhaseData(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("提交阶段数据: userId={}, sessionId={}", userId, id);

        LearningSession session = sessionService.getSession(id);
        if (!session.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权操作该会话"));
        }

        sessionService.submitPhaseData(id, body);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String getPhaseLabel(String phase) {
        return switch (phase) {
            case "diagnosis" -> "诊断";
            case "planning" -> "规划";
            case "learning" -> "学习";
            case "exercise" -> "习题";
            case "report" -> "报告";
            default -> phase;
        };
    }

    /**
     * 删除会话
     * DELETE /session/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable Long id,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("删除学习会话: userId={}, sessionId={}", userId, id);

        LearningSession session = sessionService.getSession(id);
        if (!session.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权删除该会话"));
        }

        sessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}