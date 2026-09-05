package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.LearningSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主动推送服务
 * 定时扫描活跃用户状态，通过 WebSocket 推送学习提醒/鼓励/调整建议
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final LearningSessionService sessionService;

    // TODO: 注入 InterventionAgent 用于分析用户状态
    // private final InterventionAgent interventionAgent;

    // TODO: 注入 SimpMessagingTemplate 用于 WebSocket 推送
    // private final SimpMessagingTemplate messagingTemplate;

    /**
     * 每 60 秒扫描一次活跃用户
     */
    @Scheduled(fixedRate = 60_000)
    public void scanAndPush() {
        log.debug("扫描活跃用户状态...");

        // TODO: 获取所有活跃用户 ID
        // List<Long> activeUserIds = sessionService.getActiveUserIds();
        //
        // for (Long userId : activeUserIds) {
        //     try {
        //         // 分析用户状态
        //         PushDecision decision = analyzeUserState(userId);
        //
        //         if (decision.shouldPush()) {
        //             // 通过 WebSocket 推送
        //             messagingTemplate.convertAndSendToUser(
        //                     userId.toString(),
        //                     "/queue/push",
        //                     decision.toMap()
        //             );
        //             log.info("推送通知: userId={}, type={}", userId, decision.getType());
        //         }
        //     } catch (Exception e) {
        //         log.error("推送检查失败: userId={}, error={}", userId, e.getMessage());
        //     }
        // }
    }

    /**
     * 分析用户状态并决定是否推送
     * 推送类型: ENCOURAGE(鼓励) / REMIND(提醒) / ADJUST(调整建议) / CHALLENGE(挑战)
     */
    private Map<String, Object> analyzeUserState(String userId) {
        Map<String, Object> decision = new HashMap<>();
        decision.put("shouldPush", false);

        try {
            List<LearningSession> sessions = sessionService.listSessions(userId);
            LearningSession activeSession = sessions.stream()
                    .filter(s -> "active".equals(s.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (activeSession == null) {
                return decision;
            }

            // 检查会话是否停滞（进度为 0 且创建超过 1 天）
            if (activeSession.getProgress() == 0
                    && activeSession.getCreatedAt() != null
                    && activeSession.getCreatedAt().plusDays(1).isBefore(java.time.LocalDateTime.now())) {
                decision.put("shouldPush", true);
                decision.put("type", "REMIND");
                decision.put("title", "学习提醒");
                decision.put("message", "你的学习会话还没有开始，准备好开始了吗？");
                decision.put("action", Map.of("route", "/workbench"));
            }

            // 检查进度是否停滞（连续多天无进展）
            // TODO: 更复杂的分析逻辑
        } catch (Exception e) {
            log.error("分析用户状态失败: userId={}", userId);
        }

        return decision;
    }
}
