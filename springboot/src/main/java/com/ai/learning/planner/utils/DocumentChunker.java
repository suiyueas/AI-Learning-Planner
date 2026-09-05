package com.ai.learning.planner.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class DocumentChunker {

    /**
     * 文档分块器（Markdown 结构感知）
     * <p>
     * 支持 Markdown 标题切分和代码块保护，确保：
     * - 代码块（```...```）不会被截断
     * - 标题（##, ###, ####）作为自然分块点
     * - 列表项尽量保持连续
     * 普通非 Markdown 文档按段落（双换行）切分，超长段落按固定大小重叠切块
     */
    
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;

    /** Markdown 代码块正则（反引号包裹） */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE);
    /** Markdown 标题正则 */
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s.+", Pattern.MULTILINE);
    /** 检测是否为 Markdown 内容的简单启发式 */
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("^(#{1,6}\\s|```|[*\\-+]\\s|\\d+\\.\\s|>\\s|\\|.+\\|)", Pattern.MULTILINE);

    public List<String> chunk(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 检测是否为 Markdown 内容
        if (isMarkdown(content)) {
            return chunkMarkdown(content);
        }
        return chunkPlainText(content);
    }

    /**
     * 检测内容是否包含 Markdown 特征
     */
    private boolean isMarkdown(String content) {
        Matcher matcher = MARKDOWN_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find() && count < 3) {
            count++;
        }
        return count >= 2;
    }

    // ==================== Markdown 分块 ====================

    /**
     * Markdown 感知分块：
     * 1. 先提取并保护代码块
     * 2. 按标题拆分为章节
     * 3. 每个章节内按段落切分，超长段落再拆分
     */
    private List<String> chunkMarkdown(String content) {
        // 第一步：提取代码块，用占位符替换
        List<String> codeBlocks = new ArrayList<>();
        String withoutCode = extractCodeBlocks(content, codeBlocks);

        // 第二步：按标题切分章节
        List<String> sections = splitByHeadings(withoutCode, codeBlocks);

        // 第三步：每个章节内按段落分块
        List<String> chunks = new ArrayList<>();
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            if (section.length() <= CHUNK_SIZE) {
                chunks.add(restoreCodeBlocks(section, codeBlocks).trim());
            } else {
                // 章节超长，按段落拆分
                String[] paragraphs = section.split("\n\n");
                StringBuilder currentChunk = new StringBuilder();

                for (String para : paragraphs) {
                    if (para.trim().isEmpty()) continue;
                    String restoredPara = restoreCodeBlocks(para, codeBlocks).trim();

                    // 如果当前块 + 新段落会超长，先保存当前块
                    if (currentChunk.length() + restoredPara.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                        currentChunk = new StringBuilder();
                    }

                    // 单个段落超长时单独拆分
                    if (restoredPara.length() > CHUNK_SIZE) {
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString().trim());
                            currentChunk = new StringBuilder();
                        }
                        chunks.addAll(splitBySize(restoredPara));
                    } else {
                        if (currentChunk.length() > 0) currentChunk.append("\n\n");
                        currentChunk.append(restoredPara);
                    }
                }

                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
            }
        }

        return chunks;
    }

    /**
     * 提取代码块并用占位符替换
     */
    private String extractCodeBlocks(String content, List<String> codeBlocks) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
        int index = 0;
        while (matcher.find()) {
            codeBlocks.add(matcher.group());
            matcher.appendReplacement(sb, "<!--CODE_BLOCK_" + index + "-->");
            index++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 恢复代码块占位符
     */
    private String restoreCodeBlocks(String text, List<String> codeBlocks) {
        String result = text;
        for (int i = 0; i < codeBlocks.size(); i++) {
            result = result.replace("<!--CODE_BLOCK_" + i + "-->", codeBlocks.get(i));
        }
        return result;
    }

    /**
     * 按 Markdown 标题（##, ###, ####）将内容拆分为章节
     */
    private List<String> splitByHeadings(String content, List<String> codeBlocks) {
        List<String> sections = new ArrayList<>();

        // 按标题行分割
        String[] lines = content.split("\n", -1);
        StringBuilder currentSection = new StringBuilder();
        boolean inHeading = false;

        for (String line : lines) {
            // 判断是否为标题行（排除被占位符包含的行）
            String trimmed = line.trim();
            boolean isHeading = !trimmed.isEmpty() && trimmed.charAt(0) == '#' 
                    && trimmed.length() > 1 && trimmed.charAt(1) == '#'
                    && !trimmed.contains("<!--CODE_BLOCK_");

            if (isHeading) {
                // 保存上一章节
                if (currentSection.length() > 0) {
                    sections.add(currentSection.toString());
                    currentSection = new StringBuilder();
                }
                inHeading = true;
            }

            if (currentSection.length() > 0) currentSection.append("\n");
            currentSection.append(line);
        }

        // 保存最后一章节
        if (currentSection.length() > 0) {
            sections.add(currentSection.toString());
        }

        // 如果没有提取到章节（纯 Markdown 无标题），整体作为一章
        if (sections.isEmpty()) {
            sections.add(content);
        }

        return sections;
    }

    // ==================== 纯文本分块 ====================

    /**
     * 纯文本按段落分块
     */
    private List<String> chunkPlainText(String content) {
        String[] paragraphs = content.split("\n\n");
        List<String> chunks = new ArrayList<>();

        for (String para : paragraphs) {
            if (para.trim().isEmpty()) continue;
            if (para.length() <= CHUNK_SIZE) {
                chunks.add(para.trim());
            } else {
                chunks.addAll(splitBySize(para));
            }
        }

        return chunks;
    }

    // ==================== 通用方法 ====================

    /**
     * 按字符大小拆分文本（保留句子完整性）
     */
    private List<String> splitBySize(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int textLength = text.length();
        int maxIterations = 10000;
        int iterations = 0;

        while (start < textLength && iterations < maxIterations) {
            iterations++;
            int end = Math.min(start + CHUNK_SIZE, textLength);

            if (end >= textLength - 1) {
                String chunk = text.substring(start).trim();
                if (!chunk.isEmpty()) {
                    chunks.add(chunk);
                }
                break;
            }

            int sentenceEnd = findSentenceEnd(text.substring(start, end));
            if (sentenceEnd > 10) {
                end = start + sentenceEnd + 1;
            }

            int newStart = Math.min(end, textLength) - OVERLAP;
            if (newStart <= start) {
                newStart = start + Math.min(CHUNK_SIZE / 2, textLength - start);
            }
            if (newStart <= start) {
                newStart = start + 1;
            }

            String chunk = text.substring(start, Math.min(end, textLength)).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start = newStart;
        }

        if (iterations >= maxIterations) {
            log.warn("splitBySize 达到最大迭代次数 {}，剩余文本: {} 字符", maxIterations, textLength - start);
        }

        return chunks;
    }

    /**
     * 查找句子结尾位置（句号、问号、感叹号、换行）
     */
    private int findSentenceEnd(String text) {
        int last = -1;
        for (char c : new char[]{'.', '。', '?', '？', '!', '！', '\n'}) {
            int idx = text.lastIndexOf(c);
            if (idx > last) last = idx;
        }
        return last;
    }
}