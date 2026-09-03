package com.ai.learning.planner.interceptor;

import com.ai.learning.planner.dto.PointsConfigDTO;
import com.ai.learning.planner.exception.BusinessException;
import com.ai.learning.planner.exception.InsufficientPointsException;
import com.ai.learning.planner.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 积分拦截器
 * 封装了管理员判断、余额检查、扣费、流水记录
 * 在 Service 层统一调用
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointsInterceptor {

    private final PointsService pointsService;

    /**
     * 检查并消耗积分
     * 管理员用户免积分，普通用户检查余额并扣除
     *
     * @param userId 用户ID
     * @param cost   消耗积分数
     * @param source 来源（如 CHAT, AGENT, LEARNING_PATH）
     */
    public void checkAndConsume(Long userId, Long cost, String source) {
        // 管理员免积分
        if (pointsService.isAdmin(userId)) {
            log.debug("管理员用户 {} 免积分使用: {}", userId, source);
            return;
        }

        // 获取用户当前积分
        var balance = pointsService.getBalance(userId);
        if (balance.getAvailablePoints() < cost) {
            throw new InsufficientPointsException(balance.getAvailablePoints(), cost, source);
        }

        // 扣除积分
        pointsService.consumePoints(userId, cost, source);
        log.info("用户 {} 消耗 {} 积分，来源: {}", userId, cost, source);
    }

    /**
     * 检查积分是否足够（不扣除）
     *
     * @param userId 用户ID
     * @param cost   消耗积分数
     * @return 是否足够
     */
    public boolean checkBalance(Long userId, Long cost) {
        // 管理员免积分
        if (pointsService.isAdmin(userId)) {
            return true;
        }

        var balance = pointsService.getBalance(userId);
        return balance.getAvailablePoints() >= cost;
    }

    /**
     * 获取功能所需的积分数
     *
     * @param feature 功能名称（CHAT, AGENT, LEARNING_PATH）
     * @return 所需积分数
     */
    public Long getRequiredPoints(String feature) {
        PointsConfigDTO config = pointsService.getConfig();
        return switch (feature.toUpperCase()) {
            case "CHAT" -> config.getChatConsumePoints();
            case "AGENT" -> config.getAgentConsumePoints();
            case "LEARNING_PATH" -> config.getLearningPathConsumePoints();
            default -> throw new BusinessException("未知的功能类型: " + feature);
        };
    }

    /**
     * 便捷方法：检查并消耗积分（使用配置的积分数）
     *
     * @param userId  用户ID
     * @param feature 功能名称（CHAT, AGENT, LEARNING_PATH）
     */
    public void checkAndConsumeByFeature(Long userId, String feature) {
        Long cost = getRequiredPoints(feature);
        checkAndConsume(userId, cost, feature);
    }
}
