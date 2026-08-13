package com.ai.learning.planner.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输出安全过滤器 - 第四层防御
 *
 * 功能说明：
 * - 在模型回复返回用户之前进行安全检测
 * - 防止系统提示词通过模型输出泄露
 * - 防止敏感数据（PII、凭证等）通过模型输出泄露
 * - 防止代码注入攻击（XSS、iframe注入等）
 *
 * 检测类型：
 * 1. SYSTEM_PROMPT_LEAK_PATTERNS - 系统提示词泄露
 * 2. SENSITIVE_DATA_PATTERNS - 敏感数据（PII、凭证）
 * 3. CODE_INJECTION_PATTERNS - 代码注入（XSS、iframe等）
 *
 * 处理策略：
 * - 检测到问题时使用脱敏标记替换敏感内容
 * - 记录详细日志供安全审计
 * - 不阻断正常输出，仅过滤敏感内容
 *
 * @author AI Security Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutputFilter {

    // ==================== 检测模式定义 ====================

    /**
     * 系统提示词泄露模式
     * 检测模型可能输出的系统配置、角色设定等敏感信息
     */
    private static final List<Pattern> SYSTEM_PROMPT_LEAK_PATTERNS = List.of(
            // system: 开头的配置行
            Pattern.compile("(?i)(system\\s*[:：].*)", Pattern.MULTILINE),
            // 中文角色设定
            Pattern.compile("(?i)(你是一个.*角色|你是.*助手.*规则)", Pattern.MULTILINE),
            // 常见的prompt前缀
            Pattern.compile("(?i)(instruction:\\s*|prompt:\\s*|system\\s*prompt:\\s*)", Pattern.MULTILINE),
            // 【重要】标记的系统指令
            Pattern.compile("(?i)(【重要】.*系统.*指令)", Pattern.MULTILINE),
            // AI学习规划师角色设定
            Pattern.compile("(?i)(你是.*AI学习规划师.*仅回答)", Pattern.MULTILINE),
            // 透露系统指令的警告
            Pattern.compile("(?i)(绝不要.*透露.*系统)", Pattern.MULTILINE)
    );

    /**
     * 敏感数据模式
     * 检测可能泄露的用户隐私数据和系统凭证
     */
    private static final List<Pattern> SENSITIVE_DATA_PATTERNS = List.of(
            // 各类Token凭证
            Pattern.compile("(?i)(token\\s*[:=]\\s*)\\S+"),
            Pattern.compile("(?i)(secret\\s*[:=]\\s*)\\S+"),
            Pattern.compile("(?i)(password\\s*[:=]\\s*)\\S+"),
            Pattern.compile("(?i)(api[_-]?key\\s*[:=]\\s*)\\S+"),
            Pattern.compile("(?i)(bearer\\s+)\\S+", Pattern.CASE_INSENSITIVE),
            // 身份证号（15位或18位）
            Pattern.compile("\\b\\d{15,18}\\b"),
            // 银行卡号（标准4-4-4-4格式）
            Pattern.compile("\\b\\d{3,4}[- ]?\\d{3,4}[- ]?\\d{3,4}[- ]?\\d{3,4}\\b"),
            // 中文提及的邮箱/手机
            Pattern.compile("(?i)(邮箱|手机).*\\d{3,4}[- ]?\\d{3,4}[- ]?\\d{3,4}"),
            // 身份证（中文）
            Pattern.compile("(?i)身份证.*\\d{17}[\\dXx]")
    );

    /**
     * 代码注入模式
     * 防止模型输出被用于XSS攻击或iframe注入
     */
    private static final List<Pattern> CODE_INJECTION_PATTERNS = List.of(
            // script标签
            Pattern.compile("(?i)<script[^>]*>.*?</script>", Pattern.DOTALL),
            // javascript: 协议
            Pattern.compile("(?i)javascript:"),
            // iframe标签
            Pattern.compile("(?i)<iframe[^>]*>.*?</iframe>", Pattern.DOTALL),
            // HTML事件处理器 onerror
            Pattern.compile("(?i)onerror\\s*=", Pattern.CASE_INSENSITIVE),
            // HTML事件处理器 onload
            Pattern.compile("(?i)onload\\s*=", Pattern.CASE_INSENSITIVE),
            // HTML事件处理器 onclick
            Pattern.compile("(?i)onclick\\s*=", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 依赖注入的输入过滤器（用于联动检测）
     */
    private final InputSanitizer inputSanitizer;

    // ==================== 核心过滤方法 ====================

    /**
     * 对模型输出进行全面的安全检测和过滤
     *
     * @param output 模型原始输出
     * @param userId 当前用户ID（用于日志记录）
     * @return FilteredResult 过滤后的结果及检测状态
     *
     * 处理流程：
     * 1. 检测系统提示词泄露，如有则替换为脱敏标记
     * 2. 检测敏感数据（PII、凭证），如有则替换为脱敏标记
     * 3. 检测代码注入（XSS等），如有则替换为脱敏标记
     * 4. 返回过滤结果和各项检测状态
     */
    public FilteredResult filter(String output, String userId) {
        if (output == null || output.isBlank()) {
            return new FilteredResult(output, false, false, false);
        }

        boolean containsSystemPrompt = containsSystemPrompt(output);
        boolean containsSensitiveData = containsSensitiveData(output);
        boolean containsCodeInjection = containsCodeInjection(output);

        String cleaned = output;

        // 1. 处理系统提示词泄露
        if (containsSystemPrompt) {
            cleaned = cleanSystemPromptLeak(cleaned);
            log.warn("[OutputFilter] 检测到系统提示词泄露, userId={}", userId);
        }

        // 2. 处理敏感数据泄露
        if (containsSensitiveData) {
            cleaned = maskSensitiveData(cleaned);
            log.warn("[OutputFilter] 检测到敏感数据, userId={}", userId);
        }

        // 3. 处理代码注入
        if (containsCodeInjection) {
            cleaned = cleanCodeInjection(cleaned);
            log.warn("[OutputFilter] 检测到代码注入, userId={}", userId);
        }

        boolean wasFiltered = containsSystemPrompt || containsSensitiveData || containsCodeInjection;

        return new FilteredResult(cleaned, wasFiltered, containsSystemPrompt, containsSensitiveData);
    }

    // ==================== 各类型检测方法 ====================

    /**
     * 检测输出是否包含系统提示词泄露
     *
     * @param output 模型输出
     * @return true 如果包含系统提示词
     */
    public boolean containsSystemPrompt(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        for (Pattern p : SYSTEM_PROMPT_LEAK_PATTERNS) {
            if (p.matcher(output).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测输出是否包含敏感数据
     *
     * @param output 模型输出
     * @return true 如果包含敏感数据
     */
    public boolean containsSensitiveData(String output) {
        if ( output == null || output.isBlank()) {
            return false;
        }
        for (Pattern p : SENSITIVE_DATA_PATTERNS) {
            if (p.matcher(output).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测输出是否包含代码注入攻击
     *
     * @param output 模型输出
     * @return true 如果包含代码注入
     */
    public boolean containsCodeInjection(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        for (Pattern p : CODE_INJECTION_PATTERNS) {
            if (p.matcher(output).find()) {
                return true;
            }
        }
        return false;
    }

    // ==================== 清洗方法 ====================

    /**
     * 清洗系统提示词泄露内容
     * 将匹配到的系统配置、角色设定等替换为脱敏标记
     */
    private String cleanSystemPromptLeak(String output) {
        String cleaned = output;
        for (Pattern p : SYSTEM_PROMPT_LEAK_PATTERNS) {
            cleaned = p.matcher(cleaned).replaceAll("[系统配置已过滤]");
        }
        return cleaned;
    }

    /**
     * 脱敏敏感数据
     * 将Token、身份证、银行卡等替换为脱敏标记
     */
    private String maskSensitiveData(String output) {
        String cleaned = output;

        // Token凭证脱敏
        cleaned = Pattern.compile("(?i)(token\\s*[:=]\\s*)\\S+")
                .matcher(cleaned).replaceAll("$1[已脱敏]");

        cleaned = Pattern.compile("(?i)(secret\\s*[:=]\\s*)\\S+")
                .matcher(cleaned).replaceAll("$1[已脱敏]");

        cleaned = Pattern.compile("(?i)(password\\s*[:=]\\s*)\\S+")
                .matcher(cleaned).replaceAll("$1[已脱敏]");

        cleaned = Pattern.compile("(?i)(api[_-]?key\\s*[:=]\\s*)\\S+")
                .matcher(cleaned).replaceAll("$1[已脱敏]");

        cleaned = Pattern.compile("(?i)(bearer\\s+)\\S+", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned).replaceAll("$1[已脱敏]");

        // 身份证号脱敏
        cleaned = Pattern.compile("\\b\\d{15,18}\\b")
                .matcher(cleaned).replaceAll("[身份证号已脱敏]");

        // 银行卡号脱敏
        cleaned = Pattern.compile("\\b\\d{3,4}[- ]?\\d{3,4}[- ]?\\d{3,4}[- ]?\\d{3,4}\\b")
                .matcher(cleaned).replaceAll("[银行卡号已脱敏]");

        return cleaned;
    }

    /**
     * 清洗代码注入内容
     * 将script标签、javascript:协议、iframe等替换为脱敏标记
     */
    private String cleanCodeInjection(String output) {
        String cleaned = output;

        // script标签
        cleaned = Pattern.compile("(?i)<script[^>]*>.*?</script>", Pattern.DOTALL)
                .matcher(cleaned).replaceAll("[脚本已过滤]");

        // javascript: 协议
        cleaned = Pattern.compile("(?i)javascript:")
                .matcher(cleaned).replaceAll("[链接已过滤]");

        // iframe标签
        cleaned = Pattern.compile("(?i)<iframe[^>]*>.*?</iframe>", Pattern.DOTALL)
                .matcher(cleaned).replaceAll("[嵌入内容已过滤]");

        // HTML事件处理器
        cleaned = Pattern.compile("(?i)onerror\\s*=", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned).replaceAll("[事件处理器已过滤]");
        cleaned = Pattern.compile("(?i)onload\\s*=", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned).replaceAll("[事件处理器已过滤]");
        cleaned = Pattern.compile("(?i)onclick\\s*=", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned).replaceAll("[事件处理器已过滤]");

        return cleaned;
    }

    // ==================== 结果记录类 ====================

    /**
     * 过滤结果的记录类
     *
     * @param content 过滤后的内容
     * @param wasFiltered 是否被过滤过
     * @param containedSystemPrompt 是否包含系统提示词泄露
     * @param containedSensitiveData 是否包含敏感数据
     */
    public record FilteredResult(
            String content,
            boolean wasFiltered,
            boolean containedSystemPrompt,
            boolean containedSensitiveData
    ) {
    }
}