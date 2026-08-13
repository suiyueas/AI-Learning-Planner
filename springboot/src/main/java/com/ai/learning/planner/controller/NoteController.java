package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.entity.Note;
import com.ai.learning.planner.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 笔记控制器
 * 提供笔记的RESTful API接口
 */
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteController {

    private final NoteService noteService;

    /**
     * 获取用户所有笔记
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Note>>> getUserNotes(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取用户笔记列表: userId={}", userId);
        try {
            List<Note> notes = noteService.getUserNotes(userId);
            return ResponseEntity.ok(ApiResponse.success(notes));
        } catch (Exception e) {
            log.error("获取笔记列表失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取笔记列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取笔记详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Note>> getNoteById(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取笔记详情: userId={}, noteId={}", userId, id);
        try {
            return noteService.getNoteById(id, userId)
                    .map(note -> ResponseEntity.ok(ApiResponse.success(note)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("获取笔记详情失败: userId={}, noteId={}", userId, id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取笔记详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建新笔记
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Note>> createNote(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("创建笔记: userId={}, title={}", userId, request.get("title"));
        try {
            Note note = noteService.createNote(
                    userId,
                    request.get("title"),
                    request.get("content"),
                    request.get("tags")
            );
            return ResponseEntity.ok(ApiResponse.success("笔记创建成功", note));
        } catch (Exception e) {
            log.error("创建笔记失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("创建笔记失败: " + e.getMessage()));
        }
    }

    /**
     * 更新笔记
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Note>> updateNote(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("更新笔记: userId={}, noteId={}", userId, id);
        try {
            Note note = noteService.updateNote(
                    id,
                    userId,
                    request.get("title"),
                    request.get("content"),
                    request.get("tags")
            );
            return ResponseEntity.ok(ApiResponse.success("笔记更新成功", note));
        } catch (Exception e) {
            log.error("更新笔记失败: userId={}, noteId={}", userId, id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("更新笔记失败: " + e.getMessage()));
        }
    }

    /**
     * 删除笔记
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("删除笔记: userId={}, noteId={}", userId, id);
        try {
            noteService.deleteNote(id, userId);
            return ResponseEntity.ok(ApiResponse.success("笔记删除成功", null));
        } catch (Exception e) {
            log.error("删除笔记失败: userId={}, noteId={}", userId, id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("删除笔记失败: " + e.getMessage()));
        }
    }

    /**
     * 导出笔记
     */
    @PostMapping("/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportNotes(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("导出笔记: userId={}, format={}", userId, request.get("format"));

        try {
            // 安全转换：JSON 数字数组默认反序列化为 Integer，统一转为 Long 避免 JPA 参数类型不匹配
            List<Long> noteIds = null;
            Object rawIds = request.get("noteIds");
            if (rawIds instanceof List<?> rawList && !rawList.isEmpty()) {
                noteIds = rawList.stream()
                        .filter(Objects::nonNull)
                        .map(v -> v instanceof Number n ? n.longValue() : Long.valueOf(v.toString()))
                        .collect(java.util.stream.Collectors.toList());
            }
            String format = (String) request.getOrDefault("format", "markdown");
            boolean includeCodeBlocks = Boolean.TRUE.equals(request.get("includeCodeBlocks"));
            boolean includeTags = Boolean.TRUE.equals(request.get("includeTags"));

            Map<String, Object> result = noteService.exportNotes(
                    noteIds, format, userId, includeCodeBlocks, includeTags
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("导出笔记失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("导出笔记失败: " + e.getMessage()));
        }
    }

    @GetMapping("/export/download")
    public ResponseEntity<byte[]> downloadExport(
            Authentication authentication,
            @RequestParam String content,
            @RequestParam(defaultValue = "markdown") String format) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("下载导出文件: userId={}, format={}", userId, format);

        try {
            String filename = "notes_export." + (format.equalsIgnoreCase("text") ? "txt" : "md");
            String mimeType = format.equalsIgnoreCase("text") ? "text/plain" : "text/markdown";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(content.getBytes("UTF-8"));
        } catch (Exception e) {
            log.error("下载导出文件失败: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}