/**
 * 前端安全过滤器 - 同步后端 InputSanitizer 规则
 * 
 * 功能说明：
 * - 在用户输入到达后端之前进行前端安全检测
 * - 同步后端 InputSanitizer 的检测规则
 * - 提供即时反馈，减少无效后端请求
 * 
 * 风险等级：
 * - HIGH（高风险）：直接拦截，返回错误
 * - MEDIUM（中风险）：标记但不拦截，记录日志
 * - LOW（低风险）：正常放行
 * 
 * 使用示例：
 * <pre>
 *     import { securityFilter } from '@/utils/securityUtils'
 *     
 *     const result = securityFilter.sanitize(userInput)
 *     if (result.riskLevel === 'HIGH') {
 *         ElMessage.error('输入包含不允许的内容')
 *         return
 *     }
 * </pre>
 */

const RISK_LEVEL = {
    HIGH: 'HIGH',
    MEDIUM: 'MEDIUM',
    LOW: 'LOW'
}

const ACTION = {
    BLOCK: 'BLOCK',
    WARN: 'WARN',
    PASS: 'PASS'
}

// 严重恶意指令模式（HIGH级别，直接拦截）
const CRITICAL_MALICIOUS_PATTERNS = [
    /<\||im_start\|>|<\|im_end\|>|<\|system\|>|<\|user\|>|<\|assistant\|>/gi,
    /repeat\s+from\s+['"].*['"]\s+and\s+output\s+the\s+complete\s+content/gi,
    /桑格尔门格/gi,
    /DAN\s+Mode\b/gi,
    /dev\s*mode\s*(chat)?\s*enabled/gi
]

// 指令覆盖模式（MEDIUM级别）
const OVERRIDE_PATTERNS = [
    /ignore\s+(all\s+)?(previous|instructions|prompts|above)/gi,
    /disregard\s+(all\s+)?(previous|instructions)?/gi,
    /forget\s+(your\s+)?(instructions|prompt|previous)/gi,
    /override/gi,
    /rewrite\s+(your\s+)?(system\s+)?prompt/gi,
    /new\s+(system\s+)?instructions?/gi,
    /set\s+system\s+prompt/gi
]

// 角色扮演模式（MEDIUM级别）
const ROLE_PLAY_PATTERNS = [
    /you?\s+are\s+(now\s+)?(an?\s+)?(gpt|chatgpt|assistant|ai|claude|gemini|llama)/gi,
    /act\s+as\s+(if\s+)?(you\s+)?(are|were)/gi,
    /pretend\s+(you\s+)?(are|were)/gi,
    /角色扮演/gi,
    /扮演/gi,
    /你是一个?/gi,
    /你现在是/gi
]

// 系统提示词泄露模式（MEDIUM级别）
const SYSTEM_LEAK_PATTERNS = [
    /system\s*[:：]\s*/gi,
    /reveal\s+(your\s+)?(system\s+)?(prompt|instructions)/gi,
    /leak\s+(your\s+)?(system\s+)?(prompt|instructions)/gi,
    /输出你的?\s*(系统\s+)?(指令|prompt|角色)/gi,
    /泄露\s*(系统\s+)?(指令|prompt|角色)/gi,
    /tell\s+me\s+your\s+(system\s+)?(prompt|instructions)/gi,
    /show\s+(me\s+)?your\s+(system\s+)?(prompt|instructions)/gi
]

// 编码指令模式（MEDIUM级别）
const ENCODED_PATTERNS = [
    /base64\s*[:：]?/gi,
    /uuencode/gi,
    /hex\s*encoding/gi,
    /\\u[0-9a-f]{4}/gi,
    /\\x[0-9a-f]{2}/gi
]

// 同形字混淆模式（MEDIUM级别）
const HOMOGLYPH_PATTERNS = [
    /[＜＞＜＞]/g,  // 全角<>替代半角
    /[ａ-ｚ]/g,     // 全角英文字母
    /[Ａ-Ｚ]/g,
    /[０-９]/g,     // 全角数字
    /[（）]/g,      // 全角括号
    /[【】]/g       // 方括号全角
]

// 可疑关键词列表
const SUSPICIOUS_KEYWORDS = [
    'ignore previous', 'disregard all', 'forget your instructions',
    'system prompt', 'reveal system', 'leak prompt', '输出你的系统'
]

// 超长输入阈值
const MAX_INPUT_LENGTH = 50000

// XSS危险模式
const XSS_PATTERNS = [
    /<script[^>]*>.*?<\/script>/gi,
    /javascript:/gi,
    /onerror\s*=/gi,
    /onload\s*=/gi,
    /onclick\s*=/gi,
    /<iframe[^>]*>.*?<\/iframe>/gi,
    /eval\s*\(/gi,
    /expression\s*\(/gi
]

/**
 * 检测文本是否包含同形字混淆
 */
function detectHomoglyph(text) {
    for (const pattern of HOMOGLYPH_PATTERNS) {
        if (pattern.test(text)) {
            return true
        }
    }
    return false
}

/**
 * 检测XSS
 */
function detectXSS(text) {
    for (const pattern of XSS_PATTERNS) {
        if (pattern.test(text)) {
            return true
        }
    }
    return false
}

/**
 * 检测HIGH级别恶意指令
 */
function detectCriticalMalicious(text) {
    for (const pattern of CRITICAL_MALICIOUS_PATTERNS) {
        if (pattern.test(text)) {
            return true
        }
    }
    return false
}

/**
 * 检测指令覆盖模式
 */
function detectOverride(text) {
    for (const pattern of OVERRIDE_PATTERNS) {
        if (pattern.test(text)) {
            return true
        }
    }
    return false
}

/**
 * 检测角色扮演模式
 */
function detectRolePlay(text) {
    for (const pattern of ROLE_PLAY_PATTERNS) {
        if (pattern.test(text)) {
            return true
        }
    }
    return false
}

/**
 * 检测系统提示词泄露
 */
function detectSystemLeak(text) {
    for (const pattern of SYSTEM_LEAK_PATTERNS) {
        if (pattern.test(text)) {
            return true
        }
    }
    return false
}

/**
 * 检测编码指令
 */
function detectEncoded(text) {
    for (const pattern of ENCODED_PATTERNS) {
        if (pattern.test(text)) {
            return true
        }
    }
    return false
}

/**
 * 清洗危险内容
 */
function sanitizeText(text) {
    let cleaned = text
    
    // 清洗指令覆盖模式
    for (const pattern of OVERRIDE_PATTERNS) {
        cleaned = cleaned.replace(pattern, '[指令已过滤]')
    }
    
    // 清洗系统泄露模式
    for (const pattern of SYSTEM_LEAK_PATTERNS) {
        cleaned = cleaned.replace(pattern, '[泄露指令已过滤]')
    }
    
    // 清洗编码指令
    for (const pattern of ENCODED_PATTERNS) {
        cleaned = cleaned.replace(pattern, '[编码内容已过滤]')
    }
    
    return cleaned
}

/**
 * 主安全过滤函数
 * @param {string} input 用户输入
 * @returns {object} { riskLevel, action, message, cleaned, detectedTypes }
 */
function sanitizeInput(input) {
    // 空输入
    if (!input || input.trim() === '') {
        return {
            riskLevel: RISK_LEVEL.LOW,
            action: ACTION.PASS,
            message: null,
            cleaned: input,
            detectedTypes: []
        }
    }
    
    const detectedTypes = []
    let cleaned = input
    
    // 1. 检测超长输入
    if (input.length > MAX_INPUT_LENGTH) {
        detectedTypes.push('超长输入')
    }
    
    // 2. 检测HIGH级别恶意指令
    if (detectCriticalMalicious(input)) {
        detectedTypes.push('严重恶意指令')
        return {
            riskLevel: RISK_LEVEL.HIGH,
            action: ACTION.BLOCK,
            message: '输入包含不允许的恶意指令',
            cleaned: null,
            detectedTypes
        }
    }
    
    // 3. 检测XSS
    if (detectXSS(input)) {
        detectedTypes.push('XSS注入')
        return {
            riskLevel: RISK_LEVEL.HIGH,
            action: ACTION.BLOCK,
            message: '输入包含潜在的安全风险内容',
            cleaned: null,
            detectedTypes
        }
    }
    
    // 4. 检测指令覆盖
    if (detectOverride(input)) {
        detectedTypes.push('指令覆盖尝试')
        cleaned = sanitizeText(cleaned)
    }
    
    // 5. 检测角色扮演
    if (detectRolePlay(input)) {
        detectedTypes.push('角色扮演请求')
    }
    
    // 6. 检测系统提示词泄露
    if (detectSystemLeak(input)) {
        detectedTypes.push('系统提示词泄露尝试')
        cleaned = sanitizeText(cleaned)
    }
    
    // 7. 检测编码指令
    if (detectEncoded(input)) {
        detectedTypes.push('编码指令')
        cleaned = sanitizeText(cleaned)
    }
    
    // 8. 检测同形字混淆
    if (detectHomoglyph(input)) {
        detectedTypes.push('同形字混淆')
    }
    
    // 9. 检查可疑关键词
    for (const keyword of SUSPICIOUS_KEYWORDS) {
        if (input.toLowerCase().includes(keyword.toLowerCase())) {
            if (!detectedTypes.includes('可疑关键词')) {
                detectedTypes.push('可疑关键词')
            }
            break
        }
    }
    
    // 判断风险等级
    if (detectedTypes.includes('超长输入')) {
        return {
            riskLevel: RISK_LEVEL.HIGH,
            action: ACTION.BLOCK,
            message: `输入过长，请控制在${MAX_INPUT_LENGTH}字符以内`,
            cleaned: null,
            detectedTypes
        }
    }
    
    if (detectedTypes.length > 0) {
        return {
            riskLevel: RISK_LEVEL.MEDIUM,
            action: ACTION.WARN,
            message: '输入可能包含特殊指令，已记录',
            cleaned,
            detectedTypes
        }
    }
    
    return {
        riskLevel: RISK_LEVEL.LOW,
        action: ACTION.PASS,
        message: null,
        cleaned: input,
        detectedTypes: []
    }
}

/**
 * 快速检测函数 - 仅返回是否通过
 */
function isSafe(input) {
    const result = sanitizeInput(input)
    return result.action !== ACTION.BLOCK
}

/**
 * 检测是否为高风险输入
 */
function isHighRisk(input) {
    const result = sanitizeInput(input)
    return result.riskLevel === RISK_LEVEL.HIGH
}

/**
 * 过滤HTML特殊字符（用于XSS防护）
 */
function escapeHtml(text) {
    if (!text) return text
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }
    return text.replace(/[&<>"']/g, m => map[m])
}

/**
 * 规范化输入（处理同形字）
 */
function normalizeInput(text) {
    if (!text) return text
    // 将全角转换为半角
    return text
        .replace(/[Ａ-Ｚ]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xFEE0))
        .replace(/[ａ-ｚ]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xFEE0))
        .replace(/[０-９]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xFEE0))
        .replace(/[（）]/g, c => c === '（' ? '(' : ')')
        .replace(/[【】]/g, c => c === '【' ? '[' : ']')
        .replace(/[＜＞]/g, c => c === '＜' ? '<' : '>')
}

export const securityFilter = {
    sanitize: sanitizeText,
    isSafe,
    isHighRisk,
    escapeHtml,
    normalizeInput,
    MAX_INPUT_LENGTH,
    RISK_LEVEL,
    ACTION
}

export default securityFilter