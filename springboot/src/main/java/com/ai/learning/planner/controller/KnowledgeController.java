package com.ai.learning.planner.controller;

import com.ai.learning.planner.entity.KnowledgeChunk;
import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.entity.KnowledgeNode;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.FileUploadService;
import com.ai.learning.planner.service.KnowledgeChunkService;
import com.ai.learning.planner.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

/**
 * 知识库控制器
 * 提供知识节点管理、文档上传、相似内容搜索等功能
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

    @Value("${file.upload.root:./uploads}")
    private String uploadRoot;

    @Value("${file.upload.knowledge-path:./uploads/knowledges}")
    private String knowledgePath;

    /**
     * 保存知识节点
     */
    @PostMapping
    public KnowledgeNode saveNode(@RequestBody KnowledgeNode node) {
        return knowledgeService.saveNode(node);
    }

    /**
     * 根据ID获取知识节点
     */
    @GetMapping("/{nodeId}")
    public KnowledgeNode getNode(@PathVariable String nodeId) {
        return knowledgeService.getNode(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
    }

    /**
     * 根据名称搜索知识节点
     */
    @GetMapping("/search")
    public List<KnowledgeNode> searchByName(@RequestParam String name) {
        return knowledgeService.searchByName(name);
    }

    /**
     * 搜索相似内容
     */
    @GetMapping("/similar")
    public List<Document> searchSimilar(@RequestParam String query,
                                        @RequestParam(defaultValue = "10") int topK) {
        return knowledgeService.searchSimilar(query, topK);
    }

    /**
     * 根据分类获取知识节点
     */
    @GetMapping("/category/{category}")
    public List<KnowledgeNode> getByCategory(@PathVariable String category) {
        return knowledgeService.getNodesByCategory(category);
    }

    /**
     * 获取所有知识节点
     */
    @GetMapping
    public List<KnowledgeNode> getAllNodes() {
        return knowledgeService.getAllNodes();
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

            // 异步生成知识块
            final String savedDocId = docId;
            taskExecutor.execute(() -> {
                try {
                    chunkService.generateChunksForDocument(savedDocId);
                } catch (Exception e) {
                    log.error("生成知识块失败: docId={}", savedDocId, e);
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
                    String cleanPath = filePath;
                    if (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1);
                    Path resolvedPath = Paths.get(uploadRoot).resolve(
                            cleanPath.startsWith("uploads/") ? cleanPath.substring("uploads/".length()) : cleanPath
                    ).normalize();

                    if (Files.exists(resolvedPath)) {
                        content = Files.readString(resolvedPath, StandardCharsets.UTF_8);
                        data.put("id", doc.getId());
                        data.put("type", doc.getType());
                        data.put("size", doc.getSize());
                        data.put("chunks", doc.getChunks());
                        data.put("status", doc.getStatus());
                    }
                }
            }

            // ========== 方式二：从 classpath knowledge-docs 目录读取（降级方案） ==========
            if (content == null) {
                log.info("数据库记录读取失败，尝试从 knowledge-docs 目录读取: {}", filename);

                // 使用 PathMatchingResourcePatternResolver 扫描所有 .md 文件
                org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver =
                        new org.springframework.core.io.support.PathMatchingResourcePatternResolver(resourceLoader);
                org.springframework.core.io.Resource[] resources = resolver.getResources(
                        "classpath*:uploads/knowledges/knowledge-docs/**/*.md");

                String searchName = filename;
                if (searchName.contains("/")) searchName = searchName.substring(searchName.lastIndexOf("/") + 1);
                if (searchName.contains("\\")) searchName = searchName.substring(searchName.lastIndexOf("\\") + 1);

                for (org.springframework.core.io.Resource res : resources) {
                    if (res.exists() && res.getFilename() != null
                            && res.getFilename().equalsIgnoreCase(searchName)) {
                        content = Files.readString(Path.of(res.getURI()));
                        log.info("从 knowledge-docs 读取成功: {}", res.getFilename());
                        data.put("title", res.getFilename());
                        data.put("size", formatFileSize(res.contentLength()));
                        break;
                    }
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
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }
}