package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.service.ModelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档摘要工具（AI 赋能）
 * 支持传入文档ID（自动读取知识库文件）或直接传入文本内容，
 * 由大模型生成摘要并提取关键词，附带阅读时间估算。
 */
@Component
@Slf4j
public class DocumentSummaryTool extends AbstractAiTool {

    private static final int MAX_CONTENT_LENGTH = 8000;

    private final KnowledgeDocumentRepository documentRepository;

    @Value("${file.upload.root:./uploads}")
    private String uploadRoot;

    public DocumentSummaryTool(ModelManager modelManager, ObjectMapper objectMapper,
                               KnowledgeDocumentRepository documentRepository, McpAiProperties properties) {
        super(modelManager, objectMapper, properties);
        this.documentRepository = documentRepository;
    }

    @Override
    protected String toolId() {
        return "summarize_document";
    }

    public Map<String, Object> execute(Map<String, Object> params, AiToolContext ctx) {
        String documentId = paramString(params, "documentId", "");
        String content = paramString(params, "content", "");
        String length = normalizeLength(paramString(params, "length", "medium"));

        // 1. 解析文档内容（documentId 优先，二者都传时以 documentId 为准）
        String docTitle = null;
        if (!documentId.isBlank()) {
            Optional<KnowledgeDocument> docOpt = documentRepository.findById(documentId);
            if (docOpt.isEmpty()) {
                return error("文档不存在: " + documentId + "，请检查文档ID或改为直接传入content");
            }
            KnowledgeDocument doc = docOpt.get();
            docTitle = doc.getTitle();
            content = readDocumentFile(doc);
            if (content == null || content.isBlank()) {
                return error("无法读取文档内容（" + docTitle + "），请改为直接传入content参数");
            }
        }
        if (content.isBlank()) {
            return error("文档内容不能为空，请传入documentId或content");
        }

        // 2. 调用大模型生成摘要（结构化输出：摘要+关键词）
        String prompt = buildPrompt(content, length);
        Map<String, Object> result = new HashMap<>();
        Optional<String> rawOpt = callLlm(prompt);
        String summary = null;
        List<String> keywords = List.of();

        if (rawOpt.isPresent()) {
            Map<String, Object> parsed = parseJsonObject(rawOpt.get());
            Object sumObj = parsed.get("summary");
            if (sumObj != null && !sumObj.toString().isBlank()) {
                summary = sumObj.toString().trim();
            }
            Object kwObj = parsed.get("keywords");
            if (kwObj instanceof List<?> kwList && !kwList.isEmpty()) {
                keywords = kwList.stream().map(Object::toString).toList();
            }
        }

        // 3. LLM 失败时降级：摘要取内容首段截断，关键词取高频名词占位
        if (summary == null || summary.isBlank()) {
            log.warn("[summarize_document] LLM 摘要为空，使用降级方案");
            markFallback();
            summary = fallbackSummary(content, length);
            result.put("fallback", true);
        }

        // 4. 组装结果
        result.put("summary", summary);
        result.put("keywords", keywords);
        result.put("readTimeEstimate", estimateReadTime(summary));
        result.put("documentTitle", docTitle);
        result.put("originalLength", content.length());
        result.put("length", length);
        result.put("suggestedNextTools", List.of("extract_keywords", "generate_quiz"));
        result.put("message", "文档摘要生成成功");
        return result;
    }

    /** 长度参数归一化：兼容 short/medium/long 与 简短/中等/详细 */
    private String normalizeLength(String length) {
        return switch (length) {
            case "short", "简短" -> "short";
            case "long", "详细" -> "long";
            default -> "medium";
        };
    }

    private String buildPrompt(String content, String length) {
        String lengthDesc = switch (length) {
            case "short" -> "简短（100字以内）";
            case "long" -> "详细（500字以上）";
            default -> "中等长度（200-300字）";
        };
        return """
                请对以下学习文档生成%s摘要，要求语言简洁、突出重点、保留关键术语。
                同时提取 3-5 个最能代表文档主题的关键词。
                只返回 JSON 对象（不要任何其他文字），格式如下：
                {"summary": "摘要内容", "keywords": ["关键词1", "关键词2"]}

                文档内容：
                %s
                """.formatted(lengthDesc, truncate(content, MAX_CONTENT_LENGTH));
    }

    /** 估算阅读时间（中文按 300字/分钟） */
    private String estimateReadTime(String summary) {
        int chars = summary == null ? 0 : summary.length();
        int minutes = Math.max(1, (int) Math.ceil(chars / 300.0));
        return minutes + "分钟";
    }

    /** 降级摘要：截取内容首段 */
    private String fallbackSummary(String content, String length) {
        String clean = content.replaceAll("[\\r\\n]+", " ").trim();
        int maxLen = properties.getFallback().getSummaryMaxLength();
        int limit = switch (length) {
            case "short" -> Math.min(100, maxLen);
            case "long" -> maxLen;
            default -> Math.min(280, maxLen);
        };
        if (clean.length() <= limit) return clean;
        int cut = clean.lastIndexOf('。', limit);
        if (cut > limit * 0.5) {
            return clean.substring(0, cut + 1);
        }
        return clean.substring(0, limit) + "...";
    }

    /**
     * 根据文档实体读取文件内容（支持多种路径格式）
     */
    private String readDocumentFile(KnowledgeDocument doc) {
        String filePath = doc.getFilePath();
        if (filePath == null || filePath.isBlank()) return null;

        String fileName = filePath;
        int lastSlash = Math.max(filePath.lastIndexOf("/"), filePath.lastIndexOf("\\"));
        if (lastSlash >= 0) {
            fileName = filePath.substring(lastSlash + 1);
        }
        String projectRoot = System.getProperty("user.dir");

        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(uploadRoot, fileName).normalize());
        candidates.add(Paths.get(projectRoot, uploadRoot, fileName).normalize());
        if (filePath.startsWith("/")) {
            String relativePath = filePath.startsWith("/uploads/")
                    ? filePath.substring("/uploads/".length())
                    : filePath.substring(1);
            candidates.add(Paths.get(uploadRoot, relativePath).normalize());
            candidates.add(Paths.get(projectRoot, uploadRoot, relativePath).normalize());
        }
        candidates.add(Paths.get(filePath).normalize());
        candidates.add(Paths.get(projectRoot, filePath).normalize());

        for (Path candidate : candidates) {
            try {
                Path abs = candidate.isAbsolute() ? candidate : Paths.get(projectRoot, candidate.toString()).normalize();
                if (Files.exists(abs) && Files.isReadable(abs)) {
                    String content = Files.readString(abs, StandardCharsets.UTF_8);
                    log.info("[summarize_document] 读取文档成功: {} ({} 字符)", abs, content.length());
                    return content;
                }
            } catch (Exception ignored) {
            }
        }
        log.warn("[summarize_document] 无法定位文档文件: {}", filePath);
        return null;
    }

    private Map<String, Object> error(String message) {
        log.warn("[summarize_document] {}", message);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
