package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.security.SecurityContextHolder;
import com.ai.learning.planner.service.FileUploadService;
import com.ai.learning.planner.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 统一文件上传控制器
 * 提供头像、知识库文档、MCP工具文件的上传和删除接口
 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 上传用户头像
     * POST /api/upload/avatar
     */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            String avatarUrl = fileUploadService.uploadAvatar(file, userId);
            return ResponseEntity.ok(ApiResponse.success("头像上传成功",
                    Map.of("avatarUrl", avatarUrl)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("头像上传失败: " + e.getMessage()));
        }
    }

    /**
     * 上传知识库文档
     * POST /api/upload/knowledge
     */
    @PostMapping("/knowledge")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadKnowledge(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "docId", required = false) String docId) {
        try {
            if (docId == null || docId.isBlank()) {
                docId = "doc_" + System.currentTimeMillis();
            }
            String filePath = fileUploadService.uploadKnowledge(file, docId);
            return ResponseEntity.ok(ApiResponse.success("文档上传成功",
                    Map.of("filePath", filePath, "docId", docId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("知识库文档上传失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("文档上传失败: " + e.getMessage()));
        }
    }

    /**
     * 上传MCP工具文件
     * POST /api/upload/mcp
     */
    @PostMapping("/mcp")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadMcp(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "toolId", required = false) String toolId) {
        try {
            if (toolId == null || toolId.isBlank()) {
                toolId = "tool_" + System.currentTimeMillis();
            }
            String filePath = fileUploadService.uploadMcpFile(file, toolId);
            return ResponseEntity.ok(ApiResponse.success("MCP工具文件上传成功",
                    Map.of("filePath", filePath, "toolId", toolId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("MCP工具文件上传失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 删除文件
     * DELETE /api/upload/{type}/{filename}
     */
    @DeleteMapping("/{type}/{filename}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            Authentication authentication,
            @PathVariable String type,
            @PathVariable String filename) {
        // 校验文件类型
        if (!"avatar".equals(type) && !"knowledge".equals(type) && !"mcp".equals(type)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的文件类型: " + type));
        }

        String userId = SecurityUtils.requireUserId(authentication);

        // 归属校验：只能删除自己的文件（MCP 工具文件仅管理员可删）
        switch (type) {
            case "avatar" -> {
                // 头像文件名格式 avatar_{userId}_{ts}_{uuid}.ext
                if (!filename.startsWith("avatar_" + userId + "_")) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("只能删除自己的头像"));
                }
            }
            case "knowledge" -> {
                // 仅能删除自己文档对应的物理文件（按 filePath 反查归属）
                boolean owned = knowledgeDocumentRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                        .anyMatch(doc -> doc.getFilePath() != null && doc.getFilePath().endsWith(filename));
                if (!owned) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("只能删除自己的文档文件"));
                }
            }
            case "mcp" -> {
                if (!securityContextHolder.isAdmin()) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("仅管理员可删除 MCP 工具文件"));
                }
            }
            default -> {
            }
        }

        String relativePath = "/uploads/" + type + "/" + filename;
        boolean deleted = fileUploadService.deleteFile(relativePath);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("文件删除成功", null));
        } else {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("文件删除失败，文件可能不存在"));
        }
    }
}
