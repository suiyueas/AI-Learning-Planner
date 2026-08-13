package com.ai.learning.planner.security;

import com.ai.learning.planner.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入安全过滤器 - 第一层防御
 *
 * 功能说明：
 * - 在用户输入到达大模型之前进行安全检测和清洗
 * - 使用正则表达式匹配恶意指令模式
 * - 根据风险等级采取不同处置策略（拦截/标记/放行）
 *
 * 风险等级说明：
 * - HIGH（高风险）：直接拦截，返回错误
 * - MEDIUM（中风险）：标记但不拦截，记录日志
 * - LOW（低风险）：正常放行
 *
 * 匹配模式分类：
 * 1. CRITICAL_MALICIOUS_PATTERNS - 严重恶意指令（如DAN模式、提示词注入）
 * 2. OVERRIDE_PATTERNS - 指令覆盖尝试（如"忽略之前的指令"）
 * 3. ROLE_PLAY_PATTERNS - 角色扮演请求（如"你是一个黑客"）
 * 4. SYSTEM_LEAK_PATTERNS - 系统提示词泄露尝试（如"输出你的系统指令"）
 * 5. ENCODED_PATTERNS - 编码指令（如Base64编码的恶意指令）
 * 6. DELIMITER_PATTERNS - 结构化分隔符（可能用于注入攻击）
 *
 * 使用示例：
 * <pre>
 *     SanitizeResult result = inputSanitizer.sanitize(userInput);
 *     if (result.riskLevel() == RiskLevel.HIGH) {
 *         return "输入包含不允许的内容";
 *     }
 * </pre>
 *
 * @author AI Security Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InputSanitizer {

    /**
     * 安全配置（长度阈值等，对应 security.input.*）
     */
    private final SecurityProperties securityProperties;

    /**
     * 安全审计服务（严重恶意指令拦截时记录审计日志）
     */
    private final AuditService auditService;

    // ==================== 恶意指令模式定义 ====================

    /**
     * 严重恶意指令模式（HIGH级别，直接拦截）
     * 包括：提示词注入标记、DAN模式、开发者模式等
     */
    private static final List<Pattern> CRITICAL_MALICIOUS_PATTERNS = List.of(
            // AI聊天标记注入（如<|im_start||>等）
            Pattern.compile("(?i)<\\|im_start\\|>|<\\|im_end\\|>|<\\|system\\|>|<\\|user\\|>|<\\|assistant\\|>", Pattern.CASE_INSENSITIVE),
            // "Repeat from '...' and output the complete content" - 提示词泄露攻击
            Pattern.compile("(?i)repeat\\s+from\\s+['\"].*['\"]\\s+and\\s+output\\s+the\\s+complete\\s+content"),
            // "桑格尔门格" - 已知越狱提示词
            Pattern.compile("(?i)\\b桑格尔门格\\b"),
            // DAN (Do Anything Now) 模式 - 经典越狱技术
            Pattern.compile("(?i)DAN\\s+Mode\\b"),
            // Dev Mode - 开发者模式绕过
            Pattern.compile("(?i)dev\\s*mode\\s*(chat)?\\s*enabled", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 指令覆盖模式（MEDIUM级别）
     * 尝试让AI忽略或覆盖其系统指令
     */
    private static final List<Pattern> OVERRIDE_PATTERNS = List.of(
            Pattern.compile("(?i)\\bignore\\s+(all\\s+)?(previous|instructions|prompts|above)\\b"),
            Pattern.compile("(?i)\\bdisregard\\s+(all\\s+)?(previous|instructions)?\\b"),
            Pattern.compile("(?i)\\bforget\\s+(your\\s+)?(instructions|prompt|previous)\\b"),
            Pattern.compile("(?i)\\boverride\\b"),
            Pattern.compile("(?i)\\brewrite\\s+(your\\s+)?(system\\s+)?prompt\\b"),
            Pattern.compile("(?i)\\bnew\\s+(system\\s+)?instructions?\\b"),
            Pattern.compile("(?i)\\bset\\s+system\\s+prompt\\b")
    );

    /**
     * 角色扮演模式（MEDIUM级别）
     * 尝试让AI扮演其他角色以绕过安全限制
     */
    private static final List<Pattern> ROLE_PLAY_PATTERNS = List.of(
            Pattern.compile("(?i)\\byou?\\s+are\\s+(now\\s+)?(an?\\s+)?(gpt|chatgpt|assistant|ai|claude|gemini|llama)\\b"),
            Pattern.compile("(?i)\\bact\\s+as\\s+(if\\s+)?(you\\s+)?(are|were)\\b"),
            Pattern.compile("(?i)\\bpretend\\s+(you\\s+)?(are|were)\\b"),
            Pattern.compile("(?i)\\b角色扮演\\b"),
            Pattern.compile("(?i)\\b扮演\\b"),
            Pattern.compile("(?i)\\b你是一个?\\b"),
            Pattern.compile("(?i)\\b你现在是\\b")
    );

    /**
     * 系统提示词泄露模式（MEDIUM级别）
     * 尝试获取AI的系统指令或内部配置
     */
    private static final List<Pattern> SYSTEM_LEAK_PATTERNS = List.of(
            Pattern.compile("(?i)\\bsystem\\s*[:：]\\s*"),
            Pattern.compile("(?i)\\breveal\\s+(your\\s+)?(system\\s+)?(prompt|instructions)\\b"),
            Pattern.compile("(?i)\\bleak\\s+(your\\s+)?(system\\s+)?(prompt|instructions)\\b"),
            Pattern.compile("(?i)\\b输出你的?\\s*(系统\\s+)?(指令|prompt|角色)\\b"),
            Pattern.compile("(?i)\\b泄露\\s*(系统\\s+)?(指令|prompt|角色)\\b"),
            Pattern.compile("(?i)\\btell\\s+me\\s+your\\s+(system\\s+)?(prompt|instructions)\\b"),
            Pattern.compile("(?i)\\bshow\\s+(me\\s+)?your\\s+(system\\s+)?(prompt|instructions)\\b")
    );

    /**
     * 编码指令模式（MEDIUM级别）
     * 尝试使用编码方式隐藏恶意指令
     */
    private static final List<Pattern> ENCODED_PATTERNS = List.of(
            Pattern.compile("(?i)base64\\s*[:：]?"),
            Pattern.compile("(?i)\\buuencode\\b"),
            Pattern.compile("(?i)\\bhex\\s*encoding\\b"),
            Pattern.compile("(?i)\\\\u[0-9a-f]{4}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)\\\\x[0-9a-f]{2}", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 结构化分隔符模式（LOW级别，仅标记）
     * 可能用于注入攻击的结构化标记
     */
    private static final List<Pattern> DELIMITER_PATTERNS = List.of(
            Pattern.compile("```"),
            Pattern.compile("----"),
            Pattern.compile("\\[\\[|\\]\\]"),
            Pattern.compile(">>|<<"),
            Pattern.compile("---\\s*USER\\s+DATA", Pattern.CASE_INSENSITIVE),
            Pattern.compile("---USER\\s+(INPUT|DATA|REQUEST)", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 可疑关键词列表（MEDIUM级别）
     * 需要结合上下文判断的敏感词
     */
    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "ignore previous", "disregard all", "forget your instructions",
            "system prompt", "reveal system", "leak prompt", "输出你的系统"
    );

    // ==================== 核心检测方法 ====================

    /**
     * 对输入进行全面的安全检测和清洗
     *
     * @param input 用户输入文本
     * @return SanitizeResult 包含清洗后的内容、风险等级、是否被修改、检测到的风险类型
     *
     * 处理逻辑：
     * 1. 遍历所有模式，匹配到的内容会被替换为[指令已过滤]等标记
     * 2. 收集所有检测到的风险类型
     * 3. 根据检测结果分类风险等级
     */
    public SanitizeResult sanitize(String input) {
        if (input == null || input.isBlank()) {
            return new SanitizeResult(input, RiskLevel.LOW, false, new ArrayList<>());
        }

        boolean modified = false;
        String cleaned = input;
        List<String> detectedRiskTypes = new ArrayList<>();

        // 1. 检测指令覆盖模式并清洗
        for (Pattern p : OVERRIDE_PATTERNS) {
            if (p.matcher(cleaned).find()) {
                modified = true;
                cleaned = p.matcher(cleaned).replaceAll("[指令已过滤]");
                detectedRiskTypes.add("指令覆盖");
            }
        }

        // 2. 检测角色扮演模式（仅标记，不清洗）
        for (Pattern p : ROLE_PLAY_PATTERNS) {
            if (p.matcher(cleaned).find()) {
                detectedRiskTypes.add("角色扮演请求");
            }
        }

        // 3. 检测系统提示词泄露模式并清洗
        for (Pattern p : SYSTEM_LEAK_PATTERNS) {
            if (p.matcher(cleaned).find()) {
                modified = true;
                cleaned = p.matcher(cleaned).replaceAll("[泄露指令已过滤]");
                detectedRiskTypes.add("系统提示词泄露尝试");
            }
        }

        // 4. 检测编码指令模式并清洗
        for (Pattern p : ENCODED_PATTERNS) {
            if (p.matcher(cleaned).find()) {
                modified = true;
                cleaned = p.matcher(cleaned).replaceAll("[编码内容已过滤]");
                detectedRiskTypes.add("编码指令");
            }
        }

        // 5. 检测结构化分隔符（仅标记）
        for (Pattern p : DELIMITER_PATTERNS) {
            if (p.matcher(cleaned).find()) {
                detectedRiskTypes.add("结构化分隔符");
            }
        }

        // 6. 综合分类风险等级
        RiskLevel level = classifyRisk(cleaned, detectedRiskTypes);
        return new SanitizeResult(cleaned, level, modified, detectedRiskTypes);
    }

    /**
     * 快速分类输入的风险等级（不进行清洗）
     *
     * @param input 用户输入文本
     * @return RiskLevel 风险等级枚举
     */
    public RiskLevel classifyRisk(String input) {
        return classifyRisk(input, new ArrayList<>());
    }

    /**
     * 详细分类输入的风险等级，同时收集风险原因
     *
     * @param input 用户输入文本
     * @param detectedRiskTypes 用于收集检测到的风险类型
     * @return RiskLevel 风险等级枚举
     */
    public RiskLevel classifyRisk(String input, List<String> detectedRiskTypes) {
        if (input == null || input.isBlank()) {
            return RiskLevel.LOW;
        }

        // 1. 检查严重恶意模式（HIGH级别）
        for (Pattern p : CRITICAL_MALICIOUS_PATTERNS) {
            if (p.matcher(input).find()) {
                detectedRiskTypes.add("严重恶意指令");
                return RiskLevel.HIGH;
            }
        }

        // 2. 检查指令覆盖模式（MEDIUM级别）
        for (Pattern p : OVERRIDE_PATTERNS) {
            if (p.matcher(input).find()) {
                detectedRiskTypes.add("指令覆盖尝试");
                return RiskLevel.MEDIUM;
            }
        }

        // 3. 检查可疑关键词（MEDIUM级别）
        String lower = input.toLowerCase();
        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                detectedRiskTypes.add("可疑关键词");
                return RiskLevel.MEDIUM;
            }
        }

        // 4. 检查输入长度（MEDIUM级别，超过配置的最大长度）
        if (input.length() > securityProperties.getInput().getMaxLength()) {
            detectedRiskTypes.add("输入过长");
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    /**
     * 判断输入是否需要被直接拦截
     * 仅HIGH级别的严重恶意指令会被拦截
     *
     * @param input 用户输入文本
     * @return true 如果需要拦截，false 正常放行
     */
    public boolean isBlocked(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        for (Pattern p : CRITICAL_MALICIOUS_PATTERNS) {
            if (p.matcher(input).find()) {
                log.warn("[InputSanitizer] 严重恶意指令已拦截: {}", maskSensitive(input));
                auditService.logBlockedAttempt(null, "INPUT_BLOCKED", "严重恶意指令", input);
                return true;
            }
        }
        return false;
    }

    /**
     * 判断输入是否需要人工复核
     * MEDIUM和HIGH级别都需要复核
     *
     * @param input 用户输入文本
     * @return true 如果需要复核
     */
    public boolean needsReview(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return classifyRisk(input) != RiskLevel.LOW;
    }

    /**
     * 获取输入的风险原因列表
     * 用于日志记录和人工复核
     *
     * @param input 用户输入文本
     * @return List<String> 风险原因列表
     */
    public List<String> getRiskReasons(String input) {
        if (input == null || input.isBlank()) {
            return new ArrayList<>();
        }

        List<String> reasons = new ArrayList<>();
        classifyRisk(input, reasons);
        return reasons;
    }

    /**
     * 脱敏处理，用于日志记录
     * 保留输入的首尾部分，中间部分用...替代
     */
    private String maskSensitive(String input) {
        if (input == null || input.length() <= 50) {
            return input;
        }
        return input.substring(0, 30) + "..." + input.substring(input.length() - 20);
    }

    // ==================== 结果记录类 ====================

    /**
     * 清洗结果的记录类
     *
     * @param content 清洗后的内容（恶意部分被替换为标记）
     * @param riskLevel 风险等级
     * @param modified 是否被修改过
     * @param detectedRiskTypes 检测到的风险类型列表
     */
    public record SanitizeResult(
            String content,
            RiskLevel riskLevel,
            boolean modified,
            List<String> detectedRiskTypes
    ) {
    }

    /**
     * 风险等级枚举
     * - LOW: 低风险，正常放行
     * - MEDIUM: 中风险，标记但不拦截
     * - HIGH: 高风险，直接拦截
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}