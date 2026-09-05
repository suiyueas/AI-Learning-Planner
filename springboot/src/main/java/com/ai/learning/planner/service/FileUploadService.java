package com.ai.learning.planner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 统一文件上传服务
 * 负责头像、知识库文档、MCP工具文件的上传与管理
 */
@Service
@Slf4j
public class FileUploadService {

    @Value("${file.upload.root:./uploads}")
    private String uploadRoot;

    @Value("${file.upload.avatar-path:./uploads/avatar}")
    private String avatarPath;

    @Value("${file.upload.knowledge-path:./uploads/knowledges}")
    private String knowledgePath;

    @Value("${file.upload.mcp-path:./uploads/mcp}")
    private String mcpPath;

    // 头像白名单不含 .svg：SVG 可内嵌脚本，上传后经 /uploads/** 静态访问会形成存储型 XSS
    @Value("${file.upload.allowed-extensions.avatar:.jpg,.jpeg,.png,.gif,.webp}")
    private String allowedAvatarExtensions;

    @Value("${file.upload.allowed-extensions.knowledge:.pdf,.docx,.doc,.md,.txt,.pptx,.xlsx,.xls,.ppt}")
    private String allowedKnowledgeExtensions;

    @Value("${file.upload.allowed-extensions.mcp:.json,.yaml,.yml,.xml,.js,.ts}")
    private String allowedMcpExtensions;

    /**
     * 初始化：确保上传目录存在，路径统一转为绝对路径避免工作目录漂移
     */
    @PostConstruct
    public void init() {
        uploadRoot = toAbsolutePath(uploadRoot);
        avatarPath = toAbsolutePath(avatarPath);
        knowledgePath = toAbsolutePath(knowledgePath);
        mcpPath = toAbsolutePath(mcpPath);

        createDirectoryIfNotExists(uploadRoot);
        createDirectoryIfNotExists(avatarPath);
        createDirectoryIfNotExists(knowledgePath);
        createDirectoryIfNotExists(mcpPath);
        log.info("文件上传服务初始化完成，上传根目录: {}", uploadRoot);
    }

    private String toAbsolutePath(String path) {
        Path p = Paths.get(path);
        return p.isAbsolute() ? p.toAbsolutePath().normalize().toString()
                : Paths.get(System.getProperty("user.dir"), path).toAbsolutePath().normalize().toString();
    }

    /**
     * 上传用户头像
     *
     * @param file   头像文件
     * @param userId 用户ID
     * @return 文件访问路径，如 /uploads/avatar/avatar_1_20260711120000123_abc12345.jpg
     */
    public String uploadAvatar(MultipartFile file, Long userId) {
        validateFile(file, allowedAvatarExtensions, "头像");
        String filename = generateFileName("avatar_" + userId, file.getOriginalFilename());
        saveFile(file, avatarPath, filename);
        log.info("头像上传成功: userId={}, file={}", userId, filename);
        return "/uploads/avatar/" + filename;
    }

    /**
     * 上传知识库文档
     *
     * @param file 文档文件
     * @param docId 文档ID
     * @return 文件访问路径，如 /uploads/knowledges/doc_123_20260711120000123_abc12345.pdf
     */
    public String uploadKnowledge(MultipartFile file, String docId) {
        validateFile(file, allowedKnowledgeExtensions, "知识库文档");
        String filename = generateFileName(docId, file.getOriginalFilename());
        String absolutePath = saveFile(file, knowledgePath, filename);
        log.info("知识库文档上传成功: docId={}, file={}, absolutePath={}", docId, filename, absolutePath);
        return absolutePath;
    }

    /**
     * 上传MCP工具文件
     *
     * @param file   工具文件
     * @param toolId 工具ID
     * @return 文件访问路径，如 /uploads/mcp/tool_123_20260711120000123_abc12345.json
     */
    public String uploadMcpFile(MultipartFile file, String toolId) {
        validateFile(file, allowedMcpExtensions, "MCP工具文件");
        String filename = generateFileName(toolId, file.getOriginalFilename());
        saveFile(file, mcpPath, filename);
        log.info("MCP工具文件上传成功: toolId={}, file={}", toolId, filename);
        return "/uploads/mcp/" + filename;
    }

    /**
     * 删除文件
     *
     * @param relativePath 文件相对路径，如 /uploads/avatar/xxx.jpg
     * @return 是否删除成功
     */
    public boolean deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        try {
            // 去掉开头的 / 和 uploads/ 前缀，得到实际文件路径
            String cleanPath = relativePath;
            if (cleanPath.startsWith("/")) {
                cleanPath = cleanPath.substring(1);
            }
            Path filePath = Paths.get(uploadRoot).resolve(
                    cleanPath.startsWith("uploads/") ? cleanPath.substring("uploads/".length()) : cleanPath
            ).normalize();

            // 安全校验：确保路径不会跳出 uploads 目录
            Path uploadDir = Paths.get(uploadRoot).toAbsolutePath().normalize();
            if (!filePath.toAbsolutePath().normalize().startsWith(uploadDir)) {
                log.warn("文件路径安全校验失败，拒绝删除: {}", relativePath);
                return false;
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", relativePath);
                return true;
            }
            log.warn("文件不存在: {}", relativePath);
            return false;
        } catch (IOException e) {
            log.error("文件删除失败: {}", relativePath, e);
            return false;
        }
    }

    // ========== 私有方法 ==========

    /**
     * 校验文件类型
     */
    private void validateFile(MultipartFile file, String allowedExtensions, String fileTypeDesc) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fileTypeDesc + "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException(fileTypeDesc + "文件格式无效");
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException(
                    fileTypeDesc + "不支持该文件格式: " + ext + "，允许的格式: " + allowedExtensions
            );
        }
    }

    /**
     * 生成唯一文件名：{prefix}_{yyyyMMddHHmmssSSS}_{uuid8}.{ext}
     */
    private String generateFileName(String prefix, String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        return prefix + "_" + timestamp + "_" + uuid + ext;
    }

    /**
     * 保存文件到指定目录
     */
    private String saveFile(MultipartFile file, String targetDir, String filename) {
        try {
            Path dirPath = Paths.get(targetDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(filename);
            // MultipartFile 流需显式关闭，Files.copy 不会自动释放传入的流
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath);
            }
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("文件保存失败: dir={}, filename={}", targetDir, filename, e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 如果目录不存在则创建
     */
    private void createDirectoryIfNotExists(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建上传目录: {}", dirPath);
            }
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", dirPath, e);
        }
    }
}