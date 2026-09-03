package com.ai.learning.planner.controller;

import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.entity.KnowledgeChunk;
import com.ai.learning.planner.dto.KnowledgeAskRequest;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.FileUploadService;
import com.ai.learning.planner.service.KnowledgeChunkService;
import com.ai.learning.planner.service.KnowledgeService;
import com.ai.learning.planner.service.ChatService;
import com.ai.learning.planner.service.ModelManager;
import com.ai.learning.planner.interceptor.PointsInterceptor;
import com.ai.learning.planner.security.OutputFilter;
import com.ai.learning.planner.security.PromptBoundaryMarker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 知识库控制器
 * 提供文档上传、相似内容搜索等功能
 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final FileUploadService fileUploadService;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkService chunkService;
    private final org.springframework.core.io.ResourceLoader resourceLoader;
    private final SecurityContextHolder securityContextHolder;
    private final Executor taskExecutor;
    private final ChatService chatService;
    private final ModelManager modelManager;
    private final PointsInterceptor pointsInterceptor;
    private final OutputFilter outputFilter;
    private final PromptBoundaryMarker promptBoundaryMarker;
    private final com.ai.learning.planner.service.WebSocketPushService webSocketPushService;

    @Value("${file.upload.root:./uploads}")
    private String uploadRoot;

    @Value("${file.upload.knowledge-path:./uploads/knowledges}")
    private String knowledgePath;

    @jakarta.annotation.PostConstruct
    public void init() {
        uploadRoot = toAbsolutePath(uploadRoot);
        knowledgePath = toAbsolutePath(knowledgePath);
    }

    private String toAbsolutePath(String path) {
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        return p.isAbsolute() ? p.toAbsolutePath().normalize().toString()
                : java.nio.file.Paths.get(System.getProperty("user.dir"), path).toAbsolutePath().normalize().toString();
    }

    /**
     * 搜索相似内容（仅搜索当前用户上传的文档；管理员可搜索全部）
     */
    @GetMapping("/similar")
    public List<Document> searchSimilar(@RequestParam String query,
                                        @RequestParam(defaultValue = "10") int topK) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        // 管理员搜索全部文档，普通用户仅搜索自己的文档
        return securityContextHolder.isAdmin()
                ? knowledgeService.searchSimilar(query, topK)
                : knowledgeService.searchSimilar(query, topK, userId);
    }

    /**
     * 获取文档列表（仅返回当前用户的文档；管理员返回全部文档）
     * @return 文档列表
     */
    @GetMapping("/documents")
    public Map<String, Object> getDocuments() {
        log.info("获取文档列表");
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录", "data", Collections.emptyList());
        }
        // 管理员全局视图：查看所有用户的文档
        List<KnowledgeDocument> documents = securityContextHolder.isAdmin()
                ? documentRepository.findAllByOrderByUploadedAtDesc()
                : documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
        return Map.of("success", true, "data", documents);
    }

    /**
     * 上传文档
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(@RequestParam("file") MultipartFile file) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        log.info("上传文档: {}, userId={}", file.getOriginalFilename(), userId);

        if (file.isEmpty()) {
            return Map.of("success", false, "message", "文件不能为空");
        }

        try {
            String docId = "doc_" + System.currentTimeMillis() + "_" + userId;
            String fileName = file.getOriginalFilename();
            String fileType = getFileType(fileName);
            long fileSize = file.getSize();

            // 通过 FileUploadService 将文件保存到磁盘
            String filePath = fileUploadService.uploadKnowledge(file, docId);

            // 保存文档元数据到数据库（绑定用户ID）
            // 来源标注：管理员上传的文档信任等级为10（完全可信），普通用户为5
            int trustLevel = securityContextHolder.isAdmin() ? 10 : 5;
            KnowledgeDocument document = KnowledgeDocument.builder()
                    .id(docId)
                    .title(fileName)
                    .type(fileType)
                    .size(formatFileSize(fileSize))
                    .status("processing")
                    .filePath(filePath)
                    .chunks(0)
                    .description("")
                    .uploadedAt(LocalDateTime.now())
                    .userId(userId)
                    .sourceType("UPLOAD")
                    .trustLevel(trustLevel)
                    .isVerified(false)
                    .build();

            documentRepository.save(document);

            // 推送文档上传事件
            try {
                webSocketPushService.pushDocUploaded(docId, fileName);
            } catch (Exception e) {
                log.warn("推送上传事件失败: {}", e.getMessage());
            }

            // 异步生成知识块
            final String savedDocId = docId;
            taskExecutor.execute(() -> {
                try {
                    chunkService.generateChunksForDocument(savedDocId);
                } catch (Exception e) {
                    log.error("生成知识块失败: docId={}", savedDocId, e);
                    try {
                        long totalDocs = documentRepository.count();
                        long totalChunks = chunkService.getTotalChunkCount();
                        long readyDocs = documentRepository.countByStatus("ready");
                        webSocketPushService.pushDocError(savedDocId, fileName, e.getMessage(),
                                totalDocs, totalChunks, readyDocs);
                    } catch (Exception ex) {
                        log.warn("推送文档错误事件失败: {}", ex.getMessage());
                    }
                }
            });

            return Map.of("success", true, "data", document);
        } catch (Exception e) {
            log.error("文档上传失败", e);
            return Map.of("success", false, "message", "文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档（仅能删除属于当前用户的文档）
     * @param id 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteDocument(@PathVariable String id) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        log.info("删除文档: id={}, userId={}", id, userId);

        // 校验文档归属：只能删除属于自己的文档
        Optional<KnowledgeDocument> docOpt = documentRepository.findByIdAndUserId(id, userId);
        if (docOpt.isEmpty()) {
            return Map.of("success", false, "message", "文档不存在或无权删除");
        }

        try {
            documentRepository.deleteByIdAndUserId(id, userId);

            // 推送文档删除事件
            try {
                long totalDocs = documentRepository.count();
                long totalChunks = chunkService.getTotalChunkCount();
                long readyDocs = documentRepository.countByStatus("ready");
                webSocketPushService.pushDocDeleted(id, totalDocs, totalChunks, readyDocs);
            } catch (Exception e) {
                log.warn("推送删除事件失败: {}", e.getMessage());
            }

            return Map.of("success", true, "message", "文档已删除");
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return Map.of("success", false, "message", "删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档知识块列表
     * @param id 文档ID
     * @return 知识块列表
     */
    @GetMapping("/documents/{id}/chunks")
    public Map<String, Object> getDocumentChunks(@PathVariable String id) {
        log.info("获取文档知识块: id={}", id);
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        // 校验文档归属：只能查看属于自己的文档知识块（管理员可查看任意文档）
        Optional<KnowledgeDocument> docOpt = securityContextHolder.isAdmin()
                ? documentRepository.findById(id)
                : documentRepository.findByIdAndUserId(id, userId);
        if (docOpt.isEmpty()) {
            return Map.of("success", false, "message", "文档不存在或无权访问");
        }
        
        KnowledgeDocument doc = docOpt.get();
        List<KnowledgeChunk> chunks = chunkService.getChunksByDocId(id);
        final String docTitle = doc.getTitle();
        List<Map<String, Object>> chunkData = chunks.stream().map(chunk -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", chunk.getId());
            map.put("docId", chunk.getDocId());
            map.put("docTitle", docTitle);
            map.put("index", chunk.getChunkIndex());
            map.put("content", chunk.getContent());
            map.put("preview", chunk.getContentPreview());
            map.put("fullContent", chunk.getContent());
            map.put("charCount", chunk.getCharCount());
            return map;
        }).toList();
        
        return Map.of("success", true, "data", chunkData);
    }

    /**
     * 根据文档标题获取文档内容
     * @param filename 文档原始文件名（含扩展名）
     * @return 文档内容和元信息
     */
    @GetMapping("/document/content")
    public Map<String, Object> getDocumentContent(@RequestParam String filename) {
        log.info("获取文档内容: {}", filename);
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("data", null);
            err.put("message", "用户未登录");
            return err;
        }

        try {
            // ========== 方式一：从数据库记录读取（仅当前用户的文档，管理员可读任意文档） ==========
            String content = null;
            Map<String, Object> data = new HashMap<>();

            boolean isAdmin = securityContextHolder.isAdmin();
            Optional<KnowledgeDocument> docOpt = isAdmin
                    ? documentRepository.findByTitle(filename)
                    : documentRepository.findByTitleAndUserId(filename, userId);
            if (docOpt.isEmpty()) {
                // 模糊匹配（管理员匹配全部文档，普通用户限自己的文档）
                String searchName = filename;
                if (searchName.contains("/")) {
                    searchName = searchName.substring(searchName.lastIndexOf("/") + 1);
                }
                if (searchName.contains("\\")) {
                    searchName = searchName.substring(searchName.lastIndexOf("\\") + 1);
                }
                List<KnowledgeDocument> candidates = isAdmin
                        ? documentRepository.findAllByOrderByUploadedAtDesc()
                        : documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
                for (KnowledgeDocument d : candidates) {
                    String title = d.getTitle();
                    if (title != null && (title.equalsIgnoreCase(searchName)
                            || title.replace(" ", "").equalsIgnoreCase(searchName.replace(" ", ""))
                            || title.contains(searchName.replaceFirst("\\.\\w+$", ""))
                            || searchName.contains(title.replaceFirst("\\.\\w+$", "")))) {
                        docOpt = Optional.of(d);
                        break;
                    }
                }
            }

            if (docOpt.isPresent()) {
                KnowledgeDocument doc = docOpt.get();
                String filePath = doc.getFilePath();
                if (filePath != null && !filePath.isBlank()) {
                    // 使用 KnowledgeChunkService 的路径解析方法（兼容旧路径格式）
                    Path resolvedPath = chunkService.resolveFilePath(filePath);
                    if (resolvedPath != null && Files.exists(resolvedPath)) {
                        content = Files.readString(resolvedPath, StandardCharsets.UTF_8);
                        data.put("id", doc.getId());
                        data.put("type", doc.getType());
                        data.put("size", doc.getSize());
                        data.put("chunks", doc.getChunks());
                        data.put("status", doc.getStatus());
                    }
                }
            }

            // ========== 方式二：在 knowledgePath 下按文件名搜索（降级方案） ==========
            if (content == null) {
                log.info("数据库记录读取失败，尝试按文件名搜索: {}", filename);
                String searchName = filename;
                if (searchName.contains("/")) searchName = searchName.substring(searchName.lastIndexOf("/") + 1);
                if (searchName.contains("\\")) searchName = searchName.substring(searchName.lastIndexOf("\\") + 1);
                final String finalSearchName = searchName;

                Path knowledgeDir = Paths.get(knowledgePath);
                if (!knowledgeDir.isAbsolute()) {
                    knowledgeDir = Paths.get(System.getProperty("user.dir"), knowledgePath).toAbsolutePath().normalize();
                }
                try (java.util.stream.Stream<Path> stream = Files.walk(knowledgeDir, 3)) {
                    java.util.Optional<Path> match = stream.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equalsIgnoreCase(finalSearchName))
                            .findFirst();
                    if (match.isPresent()) {
                        Path path = match.get();
                        content = Files.readString(path, StandardCharsets.UTF_8);
                        log.info("按文件名搜索找到: {}", path);
                        data.put("title", path.getFileName().toString());
                        try {
                            data.put("size", formatFileSize(Files.size(path)));
                        } catch (IOException ex) {
                            log.warn("获取文件大小失败: {}", ex.getMessage());
                            data.put("size", "未知");
                        }
                    }
                } catch (IOException e) {
                    log.warn("按文件名搜索异常: {}", e.getMessage());
                }
            }

            // ========== 仍然没有内容 ==========
            if (content == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("data", null);
                err.put("message", "文档不存在: " + filename);
                return err;
            }

            String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            data.put("content", content);
            data.put("preview", preview);
            if (!data.containsKey("title")) data.put("title", filename);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", data);
            return result;
        } catch (Exception e) {
            log.error("获取文档内容异常: filename={}", filename, e);
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("data", null);
            err.put("message", "获取文档内容失败: " + e.getMessage());
            return err;
        }
    }

    /**
     * 获取知识库状态（文档数 + 片段数，仅统计当前用户的文档；管理员统计全部文档）
     */
    @GetMapping("/status")
    public Map<String, Object> getKnowledgeStatus() {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }
        // 管理员全局视图：统计所有用户的文档
        List<KnowledgeDocument> docs = securityContextHolder.isAdmin()
                ? documentRepository.findAllByOrderByUploadedAtDesc()
                : documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
        long docCount = docs.stream().filter(d -> "ready".equals(d.getStatus()) || "processing".equals(d.getStatus())).count();
        long chunkCount = docs.stream().mapToLong(d -> d.getChunks() != null ? d.getChunks() : 0).sum();
        long readyCount = docs.stream().filter(d -> "ready".equals(d.getStatus())).count();
        return Map.of(
                "success", true,
                "data", Map.of(
                        "connected", docCount > 0,
                        "documentCount", docCount,
                        "chunkCount", chunkCount,
                        "readyCount", readyCount
                )
        );
    }

    /**
     * 知识库问答（RAG：相似片段检索 + LLM 生成）
     * 返回 { answer, sources }，供知识库对话界面展示回答与引用来源
     */
    @PostMapping("/ask")
    public Map<String, Object> ask(@Valid @RequestBody KnowledgeAskRequest request) {
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        String question = request.getQuestion();
        log.info("知识库问答: userId={}, question={}", userId, question);

        // 积分检查：普通用户每次问答消耗积分（与 /chat 一致），管理员免积分
        try {
            pointsInterceptor.checkAndConsumeByFeature(Long.valueOf(userId), "CHAT");
        } catch (Exception e) {
            log.warn("知识库问答积分检查失败: {}", e.getMessage());
        }

        // ===== 1. RAG 检索相似片段（带用户隔离） =====
        List<Map<String, Object>> sources = new ArrayList<>();
        List<Document> similarDocs = knowledgeService.searchSimilar(question, 5, userId);
        StringBuilder ragContent = new StringBuilder();
        for (int i = 0; i < similarDocs.size(); i++) {
            Document doc = similarDocs.get(i);
            String title = doc.getMetadata() != null ?
                    doc.getMetadata().getOrDefault("docTitle",
                            doc.getMetadata().getOrDefault("title",
                                    doc.getMetadata().getOrDefault("source", "未知文档"))).toString() : "未知文档";
            String text = doc.getText() != null ? doc.getText() : "";
            ragContent.append("--- 片段 ").append(i + 1).append("（来源：").append(title).append("）---\n")
                    .append(text).append("\n\n");
            Map<String, Object> source = new HashMap<>();
            source.put("documentTitle", title);
            source.put("title", title);
            source.put("page", null);
            source.put("relevance", Math.round((1.0 - i * 0.08) * 100));
            sources.add(source);
        }

        // ===== 2. 构建 RAG 提示（外部检索内容加边界包裹） =====
        StringBuilder systemPrompt = new StringBuilder("你是一位智能知识库问答助手，请严格基于以下【知识库参考信息】回答用户问题。")
                .append("\n若参考信息不足以回答问题，请如实说明，不要编造内容。\n\n");
        if (ragContent.length() > 0) {
            systemPrompt.append("【知识库参考信息】\n")
                    .append(promptBoundaryMarker.wrapExternalContent(ragContent.toString(), "知识库文档片段"))
                    .append("\n\n");
        }
        systemPrompt.append("【回答格式要求】请使用 Markdown 格式组织回答，条理清晰、内容详实。");

        // ===== 3. 调用 LLM 生成回答 =====
        String answer;
        try {
            ChatClient chatClient = modelManager.createChatClient();
            answer = chatClient.prompt()
                    .system(systemPrompt.toString())
                    .user(question)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("知识库问答生成失败", e);
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。";
        }

        // 输出安全过滤
        if (answer != null) {
            OutputFilter.FilteredResult filtered = outputFilter.filter(answer, userId);
            if (filtered.wasFiltered()) {
                log.warn("[knowledge/ask] 输出已过滤, userId={}", userId);
                answer = filtered.content();
            }
        }

        // ===== 4. 落库（复用对话历史，支持会话回溯） =====
        String sessionId = "kb_" + UUID.randomUUID();
        chatService.saveChatHistory(sessionId, Long.valueOf(userId), question, "user", null);
        chatService.saveChatHistory(sessionId, Long.valueOf(userId), answer, "assistant", "knowledge-qa");

        return Map.of("success", true, "data", Map.of(
                "answer", answer != null ? answer : "",
                "sources", sources
        ));
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String filename) {
        if (filename == null) return "Unknown";
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "PDF";
            case "doc", "docx" -> "Word";
            case "md" -> "Markdown";
            case "txt" -> "TXT";
            case "html" -> "HTML";
            default -> "Unknown";
        };
    }
    
    /**
     * 触发全量生成知识块
     * @return 生成结果
     */
    @PostMapping("/chunks/generate-all")
    public Map<String, Object> generateAllChunks() {
        // 全量重生成会删除重建所有文档知识块并触发向量化（消耗 API 配额），仅管理员可触发
        if (!securityContextHolder.isAdmin()) {
            throw new AccessDeniedException("仅管理员可触发全量知识块生成");
        }
        log.info("触发全量知识块生成");
        
        taskExecutor.execute(() -> {
            try {
                log.info("开始全量知识块生成...");
                chunkService.generateAllChunksUnsafe();
                log.info("全量知识块生成完成");
            } catch (Exception e) {
                log.error("全量生成知识块失败", e);
            }
        });
        
        return Map.of("success", true, "message", "全量生成已启动，后台处理中");
    }
    
    /**
     * 为单个文档生成知识块
     * @param docId 文档ID
     * @return 生成结果
     */
    @PostMapping("/documents/{docId}/chunks/generate")
    public Map<String, Object> generateChunksForDocument(@PathVariable String docId) {
        log.info("为文档生成知识块: docId={}", docId);
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        // 校验文档归属：只能为自己的文档触发知识块生成
        Optional<KnowledgeDocument> docOpt = documentRepository.findByIdAndUserId(docId, userId);
        if (docOpt.isEmpty()) {
            return Map.of("success", false, "message", "文档不存在或无权访问");
        }
        
        taskExecutor.execute(() -> {
            try {
                chunkService.generateChunksForDocument(docId);
            } catch (Exception e) {
                log.error("生成知识块失败: docId={}", docId, e);
            }
        });
        
        return Map.of("success", true, "message", "知识块生成已启动");
    }

    /**
     * 重试失败的文档（重新生成知识块）
     * @return 重试结果
     */
    @PostMapping("/retry-failed")
    public Map<String, Object> retryFailedDocuments() {
        log.info("重试所有失败文档");
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        List<KnowledgeDocument> failedDocs = documentRepository.findByStatus("error");
        if (failedDocs.isEmpty()) {
            return Map.of("success", true, "message", "没有需要重试的文档", "data", Map.of("count", 0));
        }

        int count = 0;
        for (KnowledgeDocument doc : failedDocs) {
            if (!userId.equals(doc.getUserId()) && !securityContextHolder.isAdmin()) {
                continue;
            }
            doc.setStatus("processing");
            documentRepository.save(doc);

            final String docId = doc.getId();
            final String fileName = doc.getTitle();
            taskExecutor.execute(() -> {
                try {
                    chunkService.generateChunksForDocument(docId);
                } catch (Exception e) {
                    log.error("重试生成知识块失败: docId={}", docId, e);
                    try {
                        long totalDocs = documentRepository.count();
                        long totalChunks = chunkService.getTotalChunkCount();
                        long readyDocs = documentRepository.countByStatus("ready");
                        webSocketPushService.pushDocError(docId, fileName, e.getMessage(),
                                totalDocs, totalChunks, readyDocs);
                    } catch (Exception ex) {
                        log.warn("推送文档错误事件失败: {}", ex.getMessage());
                    }
                }
            });
            count++;
        }

        return Map.of("success", true, "message", "已启动 " + count + " 个文档的重试", "data", Map.of("count", count));
    }

    /**
     * 修复错误文档：扫描 uploads/knowledges 目录，按标题匹配文件，更新 filePath 后重新生成
     */
    @PostMapping("/fix-error-docs")
    public Map<String, Object> fixErrorDocuments() {
        log.info("开始修复错误文档的文件路径");
        String userId = securityContextHolder.getCurrentUserId();
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        Path uploadDir = Paths.get(knowledgePath);
        if (!Files.exists(uploadDir)) {
            return Map.of("success", false, "message", "上传目录不存在: " + uploadDir);
        }

        Map<String, Path> titleToFile = new HashMap<>();
        try (var walk = Files.walk(uploadDir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".pdf") || name.endsWith(".docx");
                })
                .forEach(p -> {
                    String fileName = p.getFileName().toString();
                    int dotIdx = fileName.lastIndexOf('.');
                    String baseName = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
                    titleToFile.put(baseName, p);
                });
        } catch (Exception e) {
            return Map.of("success", false, "message", "扫描上传目录失败: " + e.getMessage());
        }

        log.info("扫描到 {} 个文件", titleToFile.size());

        List<KnowledgeDocument> failedDocs = documentRepository.findByStatus("error");
        int fixed = 0;
        List<String> notFound = new ArrayList<>();

        for (KnowledgeDocument doc : failedDocs) {
            if (!userId.equals(doc.getUserId()) && !securityContextHolder.isAdmin()) {
                continue;
            }

            String title = doc.getTitle();
            if (title == null) {
                notFound.add(doc.getId() + " (title=null)");
                continue;
            }

            int dotIdx = title.lastIndexOf('.');
            String baseTitle = dotIdx > 0 ? title.substring(0, dotIdx) : title;

            Path matchedFile = titleToFile.get(baseTitle);
            if (matchedFile == null) {
                String finalBaseTitle = baseTitle;
                matchedFile = titleToFile.keySet().stream()
                        .filter(k -> k.contains(finalBaseTitle) || finalBaseTitle.contains(k))
                        .map(titleToFile::get)
                        .findFirst()
                        .orElse(null);
            }

            if (matchedFile == null) {
                notFound.add(doc.getId() + " (" + title + ")");
                continue;
            }

            try {
                String absPath = matchedFile.toAbsolutePath().normalize().toString();
                doc.setFilePath(absPath);
                doc.setStatus("processing");
                documentRepository.save(doc);
                fixed++;
                log.info("已匹配: docId={}, title={}, file={}", doc.getId(), title, absPath);
            } catch (Exception e) {
                log.error("更新 filePath 失败: docId={}", doc.getId(), e);
                notFound.add(doc.getId() + " (update error: " + e.getMessage() + ")");
            }
        }

        if (fixed > 0) {
            List<KnowledgeDocument> toRetry = documentRepository.findByStatus("processing");
            for (KnowledgeDocument doc : toRetry) {
                if (!userId.equals(doc.getUserId()) && !securityContextHolder.isAdmin()) continue;
                final String docId = doc.getId();
                final String docTitle = doc.getTitle();
                taskExecutor.execute(() -> {
                    try {
                        chunkService.generateChunksForDocument(docId);
                    } catch (Exception e) {
                        log.error("重试生成知识块失败: docId={}", docId, e);
                        try {
                            long totalDocs = documentRepository.count();
                            long totalChunks = chunkService.getTotalChunkCount();
                            long readyDocs = documentRepository.countByStatus("ready");
                            webSocketPushService.pushDocError(docId, docTitle, e.getMessage(),
                                    totalDocs, totalChunks, readyDocs);
                        } catch (Exception ex) {
                            log.warn("推送文档错误事件失败: {}", ex.getMessage());
                        }
                    }
                });
            }
        }

        return Map.of("success", true,
                "message", "修复完成: " + fixed + " 个文件已匹配",
                "data", Map.of("fixed", fixed, "notFound", notFound));
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }
}