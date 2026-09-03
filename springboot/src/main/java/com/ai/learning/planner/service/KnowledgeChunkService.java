package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.KnowledgeChunk;
import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.repository.KnowledgeChunkRepository;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.utils.DocumentChunker;
import com.ai.learning.planner.utils.DocumentContentExtractor;
import com.ai.learning.planner.vectorstore.InMemoryVectorStoreWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeChunkService {

    /**
     * 知识块服务
     * 文档上传后执行分块（DocumentChunker）与向量化入库；
     * 支持单文档生成、全量重建与文档删除时的级联清理；
     * 使用 Redis 分布式锁防止并发分块导致重复数据
     */

    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunker documentChunker;
    private final DocumentContentExtractor documentContentExtractor;
    private final WebSocketPushService webSocketPushService;

    @Autowired(required = false)
    @Qualifier("primaryVectorStore")
    private VectorStore vectorStore;

    @Autowired(required = false)
    private InMemoryVectorStoreWrapper inMemoryVectorStore;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Value("${file.upload.root:./uploads}")
    private String uploadRoot;

    @Value("${file.upload.knowledge-path:./uploads/knowledges}")
    private String knowledgePath;

    private static final String LOCK_PREFIX = "lock:knowledge:chunk:";
    private static final long LOCK_TIMEOUT_MS = 120_000;

    /**
     * 统一初始化：先解析路径为绝对路径，再检查知识块状态
     * 所有文档（用户上传 + 系统内置）统一存放在 knowledgePath 目录下
     * 两个步骤合并到一个 @PostConstruct 中，避免执行顺序问题
     */
    @PostConstruct
    public void init() {
        // 第一步：将配置路径转为绝对路径
        uploadRoot = toAbsolutePath(uploadRoot);
        knowledgePath = toAbsolutePath(knowledgePath);
        log.info("知识块服务路径初始化完成: uploadRoot={}, knowledgePath={}", uploadRoot, knowledgePath);

        // 第二步：启动时检查知识块状态（所有文档已统一到 knowledgePath 目录）
        initKnowledgeChunkService();
    }

    private String toAbsolutePath(String path) {
        Path p = Paths.get(path);
        return p.isAbsolute() ? p.toAbsolutePath().normalize().toString()
                : Paths.get(System.getProperty("user.dir"), path).toAbsolutePath().normalize().toString();
    }

    private void initKnowledgeChunkService() {
        log.info("启动时检查知识块状态...");
        long docCount = documentRepository.count();
        long chunkCount = chunkRepository.count();
        log.info("文档数量: {}, 已有知识块数量: {}", docCount, chunkCount);

        // 优先从 Redis 加载向量数据（避免重新 Embedding）
        loadVectorsFromRedis();

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

    /**
     * 从 Redis 加载向量数据到内存（启动时调用）
     * 避免应用重启后重新调用 Embedding API
     */
    private void loadVectorsFromRedis() {
        if (inMemoryVectorStore == null) {
            log.debug("InMemoryVectorStore 不可用，跳过从 Redis 加载向量");
            return;
        }

        try {
            // 构建 docId -> title 映射
            Map<String, String> docIdToTitleMap = new HashMap<>();
            List<KnowledgeDocument> documents = documentRepository.findAll();
            for (KnowledgeDocument doc : documents) {
                docIdToTitleMap.put(doc.getId(), doc.getTitle());
            }

            // 从 Redis 加载向量
            inMemoryVectorStore.loadFromRedis(docIdToTitleMap);
            log.info("Redis 向量加载完成，内存向量数: {}", inMemoryVectorStore.getVectorCount());
        } catch (Exception e) {
            log.warn("从 Redis 加载向量失败（将使用关键词搜索降级）: {}", e.getMessage());
        }
    }

   @Transactional
    public void generateChunksForDocument(String docId) {
        log.info("为文档生成知识块: {}", docId);

        KnowledgeDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + docId));

        generateChunksForDocument(document);
    }

    /**
     * Redis 分布式锁：防止并发对同一文档进行分块
     * @return true 表示成功获取锁，false 表示已被其他进程持有
     */
    private boolean tryLock(String docId) {
        if (stringRedisTemplate == null) return true;
        String key = LOCK_PREFIX + docId;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    private void unlock(String docId) {
        if (stringRedisTemplate == null) return;
        stringRedisTemplate.delete(LOCK_PREFIX + docId);
    }

   @Transactional
    public void generateChunksForDocument(KnowledgeDocument doc) {
        if (!tryLock(doc.getId())) {
            log.info("文档 {} 正在被其他进程处理，跳过", doc.getId());
            return;
        }
        try {
            doGenerateChunks(doc);
        } finally {
            unlock(doc.getId());
        }
    }

    private void doGenerateChunks(KnowledgeDocument doc) {
        log.info("生成知识块: docId={}, filePath={}", doc.getId(), doc.getFilePath());

        String content = extractFileContent(doc.getFilePath());
        if (content == null || content.trim().isEmpty()) {
            String errorMsg = "文件内容为空或读取失败（文件路径不存在）";
            log.error("{}, docId={}", errorMsg, doc.getId());
            doc.setStatus("error");
            doc.setErrorMessage(errorMsg);
            doc.setProcessedAt(LocalDateTime.now());
            documentRepository.save(doc);
            pushDocError(doc, errorMsg);
            return;
        }

        List<String> chunkList = documentChunker.chunk(content);
        if (chunkList.isEmpty()) {
            String errorMsg = "分块结果为空（文档内容可能无法被分割）";
            log.warn("{}, docId={}", errorMsg, doc.getId());
            doc.setStatus("error");
            doc.setErrorMessage(errorMsg);
            doc.setProcessedAt(LocalDateTime.now());
            documentRepository.save(doc);
            pushDocError(doc, errorMsg);
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
            metadata.put("userId", doc.getUserId());

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

        // 推送 WebSocket 事件通知前端
        try {
            long totalDocs = documentRepository.count();
            long totalChunks = chunkRepository.count();
            long readyDocs = documentRepository.countByStatus("ready");
            webSocketPushService.pushDocReady(doc.getId(), doc.getTitle(), chunks.size(), totalDocs, totalChunks, readyDocs);
        } catch (Exception e) {
            log.warn("推送 WebSocket 事件失败: {}", e.getMessage());
        }
    }

    private void pushDocError(KnowledgeDocument doc, String reason) {
        try {
            long totalDocs = documentRepository.count();
            long totalChunks = chunkRepository.count();
            long readyDocs = documentRepository.countByStatus("ready");
            webSocketPushService.pushDocError(doc.getId(), doc.getTitle(), reason, totalDocs, totalChunks, readyDocs);
        } catch (Exception e) {
            log.warn("推送文档错误事件失败: {}", e.getMessage());
        }
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
     * 获取总知识块数量
     */
    public long getTotalChunkCount() {
        return chunkRepository.count();
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
     * 探测文件实际路径（所有文档已统一到 knowledgePath 目录下）
     * <p>
     * 所有文档（用户上传 + 系统内置）统一存放在 knowledgePath 目录下，即 ./uploads/knowledges/
     * 兼容旧路径格式，自动去除 knowledge-docs/ 前缀
     */
    private Path findFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.error("filePath 为空");
            return null;
        }

        log.info("原始 filePath: {}", filePath);
        String projectRoot = System.getProperty("user.dir");
        List<Path> candidates = new ArrayList<>();

        // 兼容旧路径：自动去除 knowledge-docs/ 前缀（因为文件已移到 knowledgePath 根下）
        String cleanedPath = filePath;
        if (cleanedPath.contains("knowledge-docs/")) {
            cleanedPath = cleanedPath.replaceAll("(^|/)knowledge-docs/", "$1");
            log.info("路径包含 knowledge-docs/ 前缀，已清理: {} → {}", filePath, cleanedPath);
        }

        // ===== 1. 直接使用清理后的 filePath 本身 =====
        Path directPath = Paths.get(cleanedPath);
        if (!directPath.isAbsolute()) {
            directPath = Paths.get(projectRoot, cleanedPath).toAbsolutePath().normalize();
        }
        candidates.add(directPath);

        // ===== 2. 从 filePath 提取文件名，统一到 knowledgePath 下搜索 =====
        int lastSlash = Math.max(filePath.lastIndexOf("/"), filePath.lastIndexOf("\\"));
        String fileName = lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
        if (!fileName.isEmpty()) {
            // 核心路径：knowledgePath（已转为绝对路径，即 ./uploads/knowledges/） + 文件名
            candidates.add(Paths.get(knowledgePath, fileName).toAbsolutePath().normalize());
        }

        // ===== 3. 如果清理后的路径包含子目录（如 00-入门与概览/xxx.md），尝试完整相对路径 =====
        if (cleanedPath.contains("/") && !fileName.equals(cleanedPath)) {
            candidates.add(Paths.get(knowledgePath, cleanedPath).toAbsolutePath().normalize());
        }

        // ===== 4. 处理 URL 风格路径 /uploads/knowledges/xxx.md → 去除前缀后到 knowledgePath 下找 =====
        if (cleanedPath.startsWith("/") || cleanedPath.startsWith("uploads/")) {
            String relativePath = cleanedPath.startsWith("/") ? cleanedPath.substring(1) : cleanedPath;
            relativePath = relativePath.startsWith("uploads/") ? relativePath.substring("uploads/".length()) : relativePath;
            // 如果去掉前缀后仍包含路径分隔符，说明有子目录
            if (relativePath.contains("/") || relativePath.contains("\\")) {
                candidates.add(Paths.get(knowledgePath, relativePath).toAbsolutePath().normalize());
            } else {
                // 没有子目录说明就是文件名本身，直接添加到 candidates
                candidates.add(Paths.get(knowledgePath, relativePath).toAbsolutePath().normalize());
            }
        }

        // ===== 特别处理：/uploads/knowledges/xxx.md → 最后一段一定是文件名，直接在 knowledgePath 根找 =====
        if (filePath.contains("/uploads/knowledges/") || filePath.contains("\\uploads\\knowledges\\")) {
            int lastSlashIdx = Math.max(filePath.lastIndexOf("/"), filePath.lastIndexOf("\\"));
            if (lastSlashIdx >= 0) {
                String pureFileName = filePath.substring(lastSlashIdx + 1);
                if (!pureFileName.isEmpty() && !candidates.contains(Paths.get(knowledgePath, pureFileName).toAbsolutePath().normalize())) {
                    candidates.add(Paths.get(knowledgePath, pureFileName).toAbsolutePath().normalize());
                }
            }
        }

        // ===== 逐一尝试 =====
        for (Path candidate : candidates) {
            try {
                if (Files.exists(candidate) && Files.isReadable(candidate)) {
                    log.info("找到文件: {}", candidate);
                    return candidate;
                } else {
                    log.info("路径不存在: {}", candidate);
                }
            } catch (Exception e) {
                log.info("尝试路径异常: {}, error: {}", candidate, e.getMessage());
            }
        }

        // ===== 兜底：按文件名模糊搜索 knowledgePath 下所有子目录 =====
        log.info("精确路径未找到，尝试在 knowledgePath 下按文件名模糊搜索: {}", fileName);
        try (Stream<Path> stream = Files.walk(Paths.get(knowledgePath), 3)) {
            List<Path> matches = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
                    .toList();
            if (!matches.isEmpty()) {
                log.info("模糊搜索找到文件: {}", matches.get(0));
                return matches.get(0);
            }
        } catch (IOException e) {
            log.info("模糊搜索异常: {}", e.getMessage());
        }

        log.error("所有路径尝试失败，找不到文件: {}，搜索根目录: {}", filePath, knowledgePath);
        return null;
    }

    /**
     * 公开的文件路径解析方法，供外部调用（如 KnowledgeController 预览文档）
     * 内部委托给 findFilePath 实现
     */
    public Path resolveFilePath(String filePath) {
        return findFilePath(filePath);
    }
}