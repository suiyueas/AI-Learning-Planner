package com.ai.learning.planner.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具调用二次确认服务 - 第三层防御（权限校验）
 *
 * 功能说明：
 * - 对高风险工具调用进行二次确认
 * - 生成带时效的确认令牌，防止CSRF和重放攻击
 * - 支持用户主动确认和令牌验证
 *
 * 风险等级分类：
 * - HIGH: 删除、导出、执行命令等高危操作（默认需要确认）
 * - MEDIUM: 修改、查询敏感数据等中等风险操作（默认需要确认）
 * - LOW: 普通查询、只读操作（无需确认）
 *
 * 令牌机制：
 * - 使用UUID作为令牌，保证唯一性
 * - 有效期5分钟，过期自动失效
 * - 每个用户只能确认自己的请求
 * - 确认后令牌立即失效，防止重复使用
 *
 * 使用流程：
 * 1. 调用 requiresConfirmation(toolName, userId) 判断是否需要确认
 * 2. 如需确认，调用 createConfirmationToken() 获取令牌
 * 3. 将令牌返回前端，显示确认对话框
 * 4. 用户确认后，调用 confirm(token, userId) 验证
 * 5. ToolExecutionService 检查 isConfirmed() 决定是否执行
 *
 * @author AI Security Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolCallConfirmationService {

    // ==================== 高危工具列表 ====================

    /**
     * 高危工具（按注册表真实工具 ID 精确匹配，执行期强制二次确认）
     * 涉及系统调试、外部资源获取等敏感操作
     */
    private static final List<String> HIGH_RISK_TOOL_IDS = List.of(
            // 系统调试面板（仅管理员可用，需二次确认防误操作）
            "tool_debug_panel",
            // 跨源检索（消耗外部 API 配额）
            "unified_academic_search",
            // 网页抓取
            "web_fetch"
    );

    /**
     * 中危工具（按注册表真实工具 ID 精确匹配）
     * 涉及 AI 生成消耗配额的操作
     */
    private static final List<String> MEDIUM_RISK_TOOL_IDS = List.of(
            "deep_document_analysis",
            "smart_quiz_generation",
            "academic_translation",
            "full_chain_learning"
    );

    /**
     * 高危工具关键词（兼容未注册的旧工具名，子串匹配）
     */
    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "delete", "remove", "drop", "truncate", "export", "download", "upload",
            "execute", "run", "shell", "bash", "cmd", "grant_permission",
            "send_email", "send_sms", "payment", "refund"
    );

    /**
     * 中危工具关键词（兼容未注册的旧工具名，子串匹配）
     */
    private static final List<String> MEDIUM_RISK_KEYWORDS = List.of(
            "query_user", "get_user", "list_users", "search_user",
            "read_file", "write_file", "create_file",
            "modify", "update", "edit", "change"
    );

    /**
     * 待确认请求的存储（内存缓存）
     * key: token, value: PendingConfirmation
     * 注意：生产环境应使用Redis等分布式缓存
     */
    private final Map<String, PendingConfirmation> pendingConfirmations = new ConcurrentHashMap<>();

    // ==================== 核心确认方法 ====================

    /**
     * 判断工具调用是否需要二次确认
     *
     * @param toolName 工具名称
     * @param userId 用户ID
     * @return true 如果需要确认
     */
    public boolean requiresConfirmation(String toolName, String userId) {
        if (toolName == null || toolName.isBlank()) {
            return false;
        }

        String lowerToolName = toolName.toLowerCase();

        // 优先按注册表工具 ID 精确匹配（修复旧版泛化关键词永远无法命中的问题）
        if (HIGH_RISK_TOOL_IDS.contains(toolName)) {
            log.debug("[ToolCallConfirmation] 高危工具需要确认: toolName={}, userId={}", toolName, userId);
            return true;
        }
        if (MEDIUM_RISK_TOOL_IDS.contains(toolName)) {
            log.debug("[ToolCallConfirmation] 中危工具需要确认: toolName={}, userId={}", toolName, userId);
            return true;
        }

        // 兼容未注册的旧工具名（关键词子串匹配）
        for (String highRisk : HIGH_RISK_KEYWORDS) {
            if (lowerToolName.contains(highRisk)) {
                log.debug("[ToolCallConfirmation] 高危工具需要确认: toolName={}, userId={}", toolName, userId);
                return true;
            }
        }

        for (String mediumRisk : MEDIUM_RISK_KEYWORDS) {
            if (lowerToolName.contains(mediumRisk)) {
                log.debug("[ToolCallConfirmation] 中危工具需要确认: toolName={}, userId={}", toolName, userId);
                return true;
            }
        }

        return false;
    }

    /**
     * 创建二次确认令牌
     *
     * @param toolName 工具名称
     * @param params 工具调用参数
     * @param userId 用户ID
     * @return 确认令牌（UUID格式）
     */
    public String createConfirmationToken(String toolName, Map<String, Object> params, String userId) {
        String token = java.util.UUID.randomUUID().toString();
        PendingConfirmation confirmation = new PendingConfirmation(
                token,
                toolName,
                params,
                userId,
                Instant.now(),
                false,
                null
        );
        pendingConfirmations.put(token, confirmation);
        log.info("[ToolCallConfirmation] 创建确认令牌: token={}, toolName={}, userId={}", token, toolName, userId);
        return token;
    }

    /**
     * 用户确认操作
     *
     * @param token 确认令牌
     * @param userId 用户ID（用于验证归属）
     * @return true 确认成功
     */
    public boolean confirm(String token, String userId) {
        PendingConfirmation confirmation = pendingConfirmations.get(token);
        if (confirmation == null) {
            log.warn("[ToolCallConfirmation] 确认失败-令牌不存在: token={}, userId={}", token, userId);
            return false;
        }

        // 验证用户归属
        if (!confirmation.userId().equals(userId)) {
            log.warn("[ToolCallConfirmation] 确认失败-用户不匹配: token={}, expected={}, actual={}", token, confirmation.userId(), userId);
            return false;
        }

        // 检查是否已确认
        if (confirmation.confirmed()) {
            log.warn("[ToolCallConfirmation] 确认失败-令牌已使用: token={}", token);
            return false;
        }

        // 检查是否过期
        if (isExpired(confirmation)) {
            pendingConfirmations.remove(token);
            log.warn("[ToolCallConfirmation] 确认失败-令牌已过期: token={}", token);
            return false;
        }

        // 更新为已确认状态
        PendingConfirmation updated = new PendingConfirmation(
                confirmation.token(),
                confirmation.toolName(),
                confirmation.params(),
                confirmation.userId(),
                confirmation.createdAt(),
                true,
                Instant.now()
        );
        pendingConfirmations.put(token, updated);
        log.info("[ToolCallConfirmation] 确认成功: token={}, toolName={}, userId={}", token, confirmation.toolName(), userId);
        return true;
    }

    /**
     * 检查令牌是否已确认
     *
     * @param token 确认令牌
     * @param userId 用户ID（用于验证归属）
     * @return true 如果已确认
     */
    public boolean isConfirmed(String token, String userId) {
        PendingConfirmation confirmation = pendingConfirmations.get(token);
        if (confirmation == null) {
            return false;
        }

        // 验证用户归属
        if (!confirmation.userId().equals(userId)) {
            return false;
        }

        // 校验令牌有效期（5 分钟），过期视为未确认
        if (isExpired(confirmation)) {
            pendingConfirmations.remove(token);
            return false;
        }

        return confirmation.confirmed();
    }

    /**
     * 检查令牌是否过期
     *
     * @param token 确认令牌
     * @return true 如果已过期
     */
    public boolean isExpired(String token) {
        PendingConfirmation confirmation = pendingConfirmations.get(token);
        return confirmation == null || isExpired(confirmation);
    }

    /**
     * 检查确认是否过期（内部方法）
     * 令牌有效期为5分钟
     */
    private boolean isExpired(PendingConfirmation confirmation) {
        if (confirmation == null) {
            return true;
        }
        long expirationMinutes = 5;
        return Instant.now().minusSeconds(expirationMinutes * 60).isAfter(confirmation.createdAt());
    }

    /**
     * 清理过期的确认令牌
     * 建议定期调用（如通过定时任务）
     */
    public void removeExpiredConfirmations() {
        int before = pendingConfirmations.size();
        pendingConfirmations.entrySet().removeIf(entry -> isExpired(entry.getValue()));
        int after = pendingConfirmations.size();
        if (before > after) {
            log.info("[ToolCallConfirmation] 清理过期令牌: before={}, after={}, removed={}", before, after, before - after);
        }
    }

    /**
     * 评估工具风险等级
     *
     * @param toolName 工具名称
     * @return ToolRiskLevel 风险等级
     */
    public ToolRiskLevel assessRisk(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return ToolRiskLevel.LOW;
        }

        if (HIGH_RISK_TOOL_IDS.contains(toolName)) {
            return ToolRiskLevel.HIGH;
        }
        if (MEDIUM_RISK_TOOL_IDS.contains(toolName)) {
            return ToolRiskLevel.MEDIUM;
        }

        String lowerToolName = toolName.toLowerCase();

        for (String highRisk : HIGH_RISK_KEYWORDS) {
            if (lowerToolName.contains(highRisk)) {
                return ToolRiskLevel.HIGH;
            }
        }

        for (String mediumRisk : MEDIUM_RISK_KEYWORDS) {
            if (lowerToolName.contains(mediumRisk)) {
                return ToolRiskLevel.MEDIUM;
            }
        }

        return ToolRiskLevel.LOW;
    }

    // ==================== 数据结构 ====================

    /**
     * 待确认请求的数据结构
     *
     * @param token 确认令牌
     * @param toolName 工具名称
     * @param params 工具调用参数
     * @param userId 用户ID
     * @param createdAt 创建时间
     * @param confirmed 是否已确认
     * @param confirmedAt 确认时间
     */
    public record PendingConfirmation(
            String token,
            String toolName,
            Map<String, Object> params,
            String userId,
            Instant createdAt,
            boolean confirmed,
            Instant confirmedAt
    ) {
    }

    /**
     * 工具风险等级枚举
     * - LOW: 低风险，无需确认
     * - MEDIUM: 中风险，需要确认
     * - HIGH: 高风险，需要确认
     */
    public enum ToolRiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}