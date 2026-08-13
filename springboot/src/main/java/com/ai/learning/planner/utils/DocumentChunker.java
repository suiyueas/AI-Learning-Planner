package com.ai.learning.planner.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class DocumentChunker {

    /**
     * 文档分块器
     * 按段落（双换行）切分文档，超长段落按固定大小（500 字符）重叠切块，供向量化入库
     */
    
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;
    
    public List<String> chunk(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
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
    
    private int findSentenceEnd(String text) {
        int last = -1;
        for (char c : new char[]{'.', '。', '?', '？', '!', '！', '\n'}) {
            int idx = text.lastIndexOf(c);
            if (idx > last) last = idx;
        }
        return last;
    }
}