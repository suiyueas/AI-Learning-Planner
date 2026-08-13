package com.ai.learning.planner.security;

import com.ai.learning.planner.entity.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG知识库安全服务 - 知识库检索防护
 *
 * 功能说明：
 * - 对知识库检索返回的文档进行安全检测
 * - 防止污染的文档（携带恶意指令）进入模型上下文
 * - 在文档入库前和检索后进行双重检测
 *
 * 安全检测流程：
 * 1. 空文档/空白文档直接过滤
 * 2. 调用 InputSanitizer 检测恶意指令
 * 3. 调用 OutputFilter 检测代码注入（XSS等）
 * 4. HIGH风险文档直接丢弃
 * 5. MEDIUM风险文档进行清洗后返回
 *
 * 防护重要性：
 * - RAG场景下，用户问题会与检索到的文档拼接后发给模型
 * - 如果文档中包含恶意指令，可能绕过输入层的检测
 * - 这种攻击称为"间接注入"，危害不亚于直接注入
 *
 * 使用场景：
 * 1. 文档入库前检测（sanitizeDocument）
 * 2. 检索结果返回前检测（sanitizeRetrievedDocuments）
 * 3. RAG上下文构建时检测（sanitizeRagContext）
 * 4. 知识块安全校验（isChunkSafe）
 *
 * @author AI Security Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagSecurityService {

    /**
     * 输入安全过滤器依赖
     */
    private final InputSanitizer inputSanitizer;

    /**
     * 输出安全过滤器依赖
     */
    private final OutputFilter outputFilter;

    // ==================== 核心检测方法 ====================

    /**
     * 批量检测知识库检索返回的文档列表
     * 用于检索结果返回给模型之前
     *
     * @param documents 检索到的文档列表
     * @return 经过安全检测后的文档列表（危险文档已过滤）
     */
    public List<Document> sanitizeRetrievedDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        return documents.stream()
                .map(this::sanitizeDocument)
                .filter(doc -> doc != null)
                .collect(Collectors.toList());
    }

    /**
     * 检测单个知识库文档是否安全
     * 包含两层检测：
     * 1. InputSanitizer - 检测恶意指令注入
     * 2. OutputFilter - 检测代码注入（XSS等）
     *
     * @param doc 待检测的文档
     * @return 检测后的文档（危险文档返回null）
     */
    public Document sanitizeDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        String content = doc.getText();

        // 空/空白文档无意义，直接过滤，避免空片段进入知识库上下文
        if (content == null || content.isBlank()) {
            log.debug("[RagSecurityService] 空文档已过滤, docId={}", doc.getId());
            return null;
        }

        // 第一层检测：InputSanitizer（恶意指令）
        // 清洗危险子串并评估风险等级
        InputSanitizer.SanitizeResult sanitizeResult = inputSanitizer.sanitize(content);
        if (sanitizeResult.riskLevel() == InputSanitizer.RiskLevel.HIGH) {
            log.warn("[RagSecurityService] 知识库片段风险等级高，已过滤, docId={}", doc.getId());
            return null;
        }

        // 第二层检测：OutputFilter（代码注入）
        // 检测XSS、iframe等前端攻击代码
        if (outputFilter.containsCodeInjection(sanitizeResult.content())) {
            log.warn("[RagSecurityService] 知识库片段包含代码注入，已过滤, docId={}", doc.getId());
            return null;
        }

        // 如果文档被清洗过，创建新的文档对象
        if (sanitizeResult.modified()) {
            log.info("[RagSecurityService] 知识库片段已清洗, docId={}, reasons={}",
                    doc.getId(), sanitizeResult.detectedRiskTypes());
            return new Document(
                    doc.getId(),
                    sanitizeResult.content(),
                    doc.getMetadata()
            );
        }

        return doc;
    }

    /**
     * 快速检测知识块是否安全
     * 用于知识块入库前的快速校验
     *
     * @param chunk 待检测的知识块
     * @return true 如果安全，false 如果存在风险
     */
    public boolean isChunkSafe(KnowledgeChunk chunk) {
        if (chunk == null || chunk.getContent() == null) {
            return true; // 空内容视为安全
        }

        // 检测是否包含被拦截的恶意指令
        if (inputSanitizer.isBlocked(chunk.getContent())) {
            log.warn("[RagSecurityService] 知识块包含恶意指令, chunkId={}", chunk.getId());
            return false;
        }

        // 检测是否包含代码注入
        if (outputFilter.containsCodeInjection(chunk.getContent())) {
            log.warn("[RagSecurityService] 知识块包含代码注入, chunkId={}", chunk.getId());
            return false;
        }

        // 防御性复核：确保不是HIGH风险
        return inputSanitizer.classifyRisk(chunk.getContent()) != InputSanitizer.RiskLevel.HIGH;
    }

    /**
     * 清洗RAG上下文字符串
     * 用于在构建RAG提示词时对检索内容进行最终检测
     *
     * @param ragContext RAG上下文字符串
     * @return 清洗后的上下文（危险内容已替换为提示语）
     */
    public String sanitizeRagContext(String ragContext) {
        if (ragContext == null || ragContext.isBlank()) {
            return ragContext;
        }

        // 清洗并检测风险等级
        InputSanitizer.SanitizeResult result = inputSanitizer.sanitize(ragContext);
        if (result.riskLevel() == InputSanitizer.RiskLevel.HIGH) {
            log.warn("[RagSecurityService] RAG上下文包含恶意指令，已拦截");
            return "[知识库内容已过滤，因包含潜在风险]";
        }

        if (result.modified()) {
            log.info("[RagSecurityService] RAG上下文已清洗, reasons={}", result.detectedRiskTypes());
        }
        return result.content();
    }
}