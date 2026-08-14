package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeChunk;
import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.repository.KnowledgeChunkRepository;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.utils.DocumentChunker;
import com.ai.learning.planner.utils.DocumentContentExtractor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeChunkService {

    /**
     * 知识块服务
     * 文档上传后执行分块（DocumentChunker）与向量化入库；
     * 支持单文档生成、全量重建与文档删除时的级联清理
     */

    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunker documentChunker;
    private final DocumentContentExtractor documentContentExtractor;

    @Autowired(required = false)
    @Qualifier("primaryVectorStore")
    private VectorStore vectorStore;

    @Value("${file.upload.root:./uploads}")
    private String uploadRoot;

    @Value("${file.upload.knowledge-path:./uploads/knowledges}")
    private String knowledgePath;

    @PostConstruct
    public void init() {
        log.info("启动时检查知识块状态...");
        long docCount = documentRepository.count();
        long chunkCount = chunkRepository.count();
        log.info("文档数量: {}, 已有知识块数量: {}", docCount, chunkCount);

        if (docCount > 0 && chunkCount == 0 && docCount <= 50) {
            log.info("检测到文档已存在但知识块为空，开始生成...");
            generateAllChunksUnsafe();
        } else if (docCount > 50 && chunkCount == 0) {
            log.warn("文档数量较多 ({} 个)，建议通过 API 手动触发生成，避免启动超时", docCount);
        } else if (docCount > chunkCount) {
            log.info("检测到部分文档缺少知识块，开始补充生成...");
            generateMissingChunks();
        } else {
            log.info("知识块已存在或无需生成，跳过初始化");
        }
    }

   @Transactional
    public void generateChunksForDocument(String docId) {
        log.info("为文档生成知识块: {}", docId);

        KnowledgeDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + docId));

        generateChunksForDocument(document);
    }

   @Transactional
    public void generateChunksForDocument(KnowledgeDocument doc) {
        log.info("生成知识块: docId={}, filePath={}", doc.getId(), doc.getFilePath());

        String content = extractFileContent(doc.getFilePath());
        if (content == null || content.trim().isEmpty()) {
            log.error("文件内容为空或读取失败，跳过该文档: {}", doc.getId());
            doc.setStatus("error");
            doc.setProcessedAt(LocalDateTime.now());
            documentRepository.save(doc);
            return;
        }

        List<String> chunkList = documentChunker.chunk(content);
        if (chunkList.isEmpty()) {
            log.warn("分块结果为空，跳过: {}", doc.getId());
            doc.setStatus("error");
            doc.setProcessedAt(LocalDateTime.now());
            documentRepository.save(doc);
            return;
        }

        chunkRepository.deleteByDocId(doc.getId());

        List<KnowledgeChunk> chunks = new ArrayList<>();

        for (int i = 0; i < chunkList.size(); i++) {
            String chunkContent = chunkList.get(i);
            chunks.add(KnowledgeChunk.builder()
                    .docId(doc.getId())
                    .chunkIndex(i + 1)
                    .content(chunkContent)
                    .contentPreview(chunkContent.length() > 200 ? chunkContent.substring(0, 200) : chunkContent)
                    .charCount(chunkContent.length())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }

        // 先落库生成 chunkId，再构建向量文档（保证来源追溯字段 chunkId 非空）
        chunkRepository.saveAll(chunks);

        List<Document> vectorDocuments = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkId", chunk.getId());
            metadata.put("docId", doc.getId());
            metadata.put("docTitle", doc.getTitle());
            metadata.put("title", doc.getTitle());
            metadata.put("chunkIndex", chunk.getChunkIndex());
            metadata.put("source", "knowledge_chunk");

            vectorDocuments.add(new Document(chunk.getContent(), metadata));
        }

        if (vectorStore != null && !vectorDocuments.isEmpty()) {
            try {
                vectorStore.add(vectorDocuments);
                log.info("知识块已存入向量存储: docId={}, count={}", doc.getId(), vectorDocuments.size());
            } catch (Exception e) {
                log.warn("向量存储失败，仅保留数据库记录: docId={}, error={}", doc.getId(), e.getMessage());
            }
        } else {
            log.info("VectorStore 未配置，知识块仅存入数据库: docId={}", doc.getId());
        }

        doc.setChunks(chunks.size());
        doc.setStatus("ready");
        doc.setProcessedAt(LocalDateTime.now());
        documentRepository.save(doc);

        log.info("知识块生成完成: docId={}, 块数={}", doc.getId(), chunks.size());
    }

   @Transactional
    public void generateAllChunks() {
        log.info("开始为所有就绪文档生成知识块");

        List<KnowledgeDocument> documents = documentRepository.findAll();
        int count = 0;

        for (KnowledgeDocument doc : documents) {
            if ("ready".equals(doc.getStatus()) || "processing".equals(doc.getStatus())) {
                try {
                    generateChunksForDocument(doc);
                    count++;
                } catch (Exception e) {
                    log.error("为文档 {} 生成知识块失败", doc.getId(), e);
                }
            }
        }

        log.info("完成，共处理 {} 个文档", count);
    }

    /**
     * 无条件为所有文档生成知识块（不检查状态，用于一键全量修复）
     */
   @Transactional
    public void generateAllChunksUnsafe() {
        log.info("开始为所有文档强制生成知识块（忽略状态）");

        List<KnowledgeDocument> documents = documentRepository.findAll();
        int count = 0;

        for (KnowledgeDocument doc : documents) {
            try {
                log.info("处理文档: id={}, title={}, status={}", doc.getId(), doc.getTitle(), doc.getStatus());
                generateChunksForDocument(doc);
                count++;
            } catch (Exception e) {
                log.error("为文档 {} 生成知识块失败", doc.getId(), e);
            }
        }

        log.info("强制生成完成，共处理 {} 个文档", count);
    }

    @Transactional
    public void generateMissingChunks() {
        log.info("开始为缺失知识块的文档生成...");

        List<KnowledgeDocument> documents = documentRepository.findAll();
        int count = 0;

        for (KnowledgeDocument doc : documents) {
            long chunkCount = chunkRepository.countByDocId(doc.getId());
            if (chunkCount == 0) {
                try {
                    log.info("为文档 {} 生成知识块（当前为 0 块）", doc.getId());
                    generateChunksForDocument(doc);
                    count++;
                } catch (Exception e) {
                    log.error("为文档 {} 生成知识块失败", doc.getId(), e);
                }
            }
        }

        log.info("缺失补全完成，共处理 {} 个文档", count);
    }

    public List<KnowledgeChunk> getChunksByDocId(String docId) {
        log.debug("从数据库加载知识块: docId={}", docId);
        return chunkRepository.findByDocIdOrderByChunkIndexAsc(docId);
    }

    /**
     * 提取文档内容：探测文件实际路径后，按扩展名分发到多格式解析器
     */
    private String extractFileContent(String filePath) {
        Path foundPath = findFilePath(filePath);
        if (foundPath == null) {
            log.error("所有候选路径均未找到文件，filePath={}, projectRoot={}", filePath, System.getProperty("user.dir"));
            return null;
        }

        String content = documentContentExtractor.extract(foundPath);
        if (content != null) {
            log.info("文件解析成功: path={}, 大小={} 字符", foundPath, content.length());
        }
        return content;
    }

    /**
     * 探测文件实际路径，支持相对路径、绝对路径、URL 风格路径（/uploads/...）等多种格式
     */
    private Path findFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.error("filePath 为空");
            return null;
        }

        log.info("原始 filePath: {}", filePath);

        // 从 filePath 提取纯文件名（去掉目录前缀）
        String fileName = filePath;
        int lastSlash = Math.max(filePath.lastIndexOf("/"), filePath.lastIndexOf("\\"));
        if (lastSlash >= 0) {
            fileName = filePath.substring(lastSlash + 1);
        }
        log.info("提取的文件名: {}", fileName);

        // 确定项目根目录
        String projectRoot = System.getProperty("user.dir");

        // ===== 尝试多个候选路径 =====
        List<Path> candidates = new ArrayList<>();

        // 1. knowledgePath + fileName（相对路径）
        candidates.add(Paths.get(knowledgePath, fileName).normalize());

        // 2. projectRoot + knowledgePath + fileName
        candidates.add(Paths.get(projectRoot, knowledgePath, fileName).normalize());

        // 3. if filePath is a URL path like /uploads/knowledges/xxx.md,
        //    try uploadRoot + /knowledges/ + fileName
        if (filePath.startsWith("/")) {
            String relativePath = filePath.startsWith("/uploads/") ? filePath.substring("/uploads/".length()) : filePath.substring(1);
            candidates.add(Paths.get(uploadRoot, relativePath).normalize());
            candidates.add(Paths.get(projectRoot, uploadRoot, relativePath).normalize());
        }

        // 4. Try the raw filePath directly
        candidates.add(Paths.get(filePath).normalize());

        // 5. projectRoot + raw filePath
        candidates.add(Paths.get(projectRoot, filePath).normalize());

        // 6. uploadRoot + /knowledges + fileName
        candidates.add(Paths.get(uploadRoot, "knowledges", fileName).normalize());
        candidates.add(Paths.get(projectRoot, uploadRoot, "knowledges", fileName).normalize());

        // 去重并逐一尝试
        for (Path candidate : candidates) {
            try {
                Path absPath = candidate.isAbsolute() ? candidate : Paths.get(projectRoot, candidate.toString()).normalize();
                log.debug("尝试路径: {} → 存在: {}", absPath, Files.exists(absPath));
                if (Files.exists(absPath) && Files.isReadable(absPath)) {
                    log.info("找到文件: {}", absPath);
                    return absPath;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}