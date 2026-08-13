package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.Note;
import com.ai.learning.planner.exception.BusinessException;
import com.ai.learning.planner.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 笔记服务
 * 提供笔记的创建、查询、更新、删除和导出功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 获取用户所有笔记（按创建时间倒序）
     */
    public List<Note> getUserNotes(Long userId) {
        return noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 根据ID获取笔记（需校验归属）
     */
    public Optional<Note> getNoteById(Long noteId, Long userId) {
        return noteRepository.findByIdAndUserId(noteId, userId);
    }

    /**
     * 创建新笔记
     */
    @Transactional
    public Note createNote(Long userId, String title, String content, String tags) {
        Note note = Note.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .tags(tags)
                .wordCount(content != null ? content.length() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return noteRepository.save(note);
    }

    /**
     * 更新笔记（仅更新非空字段）
     */
    @Transactional
    public Note updateNote(Long noteId, Long userId, String title, String content, String tags) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new BusinessException("笔记不存在或无权访问"));
        if (title != null) note.setTitle(title);
        if (content != null) {
            note.setContent(content);
            note.setWordCount(content.length());
        }
        if (tags != null) note.setTags(tags);
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    /**
     * 删除笔记（需校验归属）
     */
    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new BusinessException("笔记不存在或无权访问"));
        noteRepository.delete(note);
    }

    /**
     * 导出笔记（支持Markdown和TXT格式）
     */
    public Map<String, Object> exportNotes(List<Long> noteIds, String format, Long userId,
                                           boolean includeCodeBlocks, boolean includeTags) {
        log.info("[exportNotes] 导出笔记: userId={}, noteIds={}, format={}", userId, noteIds, format);

        List<Note> notes;
        if (noteIds == null || noteIds.isEmpty()) {
            notes = noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            notes = noteRepository.findByUserIdAndIdIn(userId, noteIds);
        }

        if (notes.isEmpty()) {
            throw new BusinessException("没有可导出的笔记");
        }

        String content;
        String filename;

        switch (format.toLowerCase()) {
            case "markdown":
            case "md":
                content = buildMarkdownContent(notes, includeCodeBlocks, includeTags);
                filename = generateFilename("md");
                break;
            case "text":
            case "txt":
                content = buildTextContent(notes, includeCodeBlocks, includeTags);
                filename = generateFilename("txt");
                break;
            case "pdf":
                content = buildMarkdownContent(notes, includeCodeBlocks, includeTags);
                filename = generateFilename("md");
                break;
            default:
                throw new BusinessException("不支持的导出格式: " + format);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("filename", filename);
        result.put("format", format);
        result.put("noteCount", notes.size());

        log.info("[exportNotes] 导出完成: userId={}, noteCount={}, format={}", userId, notes.size(), format);
        return result;
    }

    private String buildMarkdownContent(List<Note> notes, boolean includeCodeBlocks, boolean includeTags) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 学习笔记导出\n\n");
        sb.append("> 导出时间: ").append(LocalDateTime.now().format(DISPLAY_DATE_FORMAT)).append("\n\n");
        sb.append("> 共 ").append(notes.size()).append(" 篇笔记\n\n");
        sb.append("---\n\n");

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            sb.append("## ").append(i + 1).append(". ").append(note.getTitle()).append("\n\n");
            sb.append("**创建时间**: ").append(note.getCreatedAt() != null ?
                    note.getCreatedAt().format(DISPLAY_DATE_FORMAT) : "未知").append("\n\n");

            if (includeTags && note.getTags() != null && !note.getTags().isEmpty()) {
                sb.append("**标签**: ");
                String[] tags = note.getTags().split(",");
                for (String tag : tags) {
                    sb.append("`").append(tag.trim()).append("` ");
                }
                sb.append("\n\n");
            }

            if (note.getSummary() != null && !note.getSummary().isEmpty()) {
                sb.append("**摘要**: ").append(note.getSummary()).append("\n\n");
            }

            sb.append("### 正文\n\n");
            sb.append(note.getContent() != null ? note.getContent() : "");
            sb.append("\n\n");

            if (includeCodeBlocks && note.getCodeBlocks() != null && !note.getCodeBlocks().isEmpty()) {
                sb.append("### 代码\n\n");
                sb.append("```\n");
                sb.append(note.getCodeBlocks());
                sb.append("\n```\n\n");
            }

            sb.append("---\n\n");
        }

        return sb.toString();
    }

    private String buildTextContent(List<Note> notes, boolean includeCodeBlocks, boolean includeTags) {
        StringBuilder sb = new StringBuilder();
        sb.append("学习笔记导出\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append("导出时间: ").append(LocalDateTime.now().format(DISPLAY_DATE_FORMAT)).append("\n");
        sb.append("共 ").append(notes.size()).append(" 篇笔记\n\n");
        sb.append("=".repeat(50)).append("\n\n");

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            sb.append((i + 1)).append(". ").append(note.getTitle()).append("\n");
            sb.append("-".repeat(30)).append("\n");
            sb.append("创建时间: ").append(note.getCreatedAt() != null ?
                    note.getCreatedAt().format(DISPLAY_DATE_FORMAT) : "未知").append("\n");

            if (includeTags && note.getTags() != null && !note.getTags().isEmpty()) {
                sb.append("标签: ").append(note.getTags()).append("\n");
            }

            if (note.getSummary() != null && !note.getSummary().isEmpty()) {
                sb.append("摘要: ").append(note.getSummary()).append("\n");
            }

            sb.append("\n正文:\n");
            sb.append(note.getContent() != null ? note.getContent() : "");
            sb.append("\n");

            if (includeCodeBlocks && note.getCodeBlocks() != null && !note.getCodeBlocks().isEmpty()) {
                sb.append("\n代码:\n");
                sb.append(note.getCodeBlocks());
                sb.append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private String generateFilename(String extension) {
        return "notes_export_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + "." + extension;
    }
}