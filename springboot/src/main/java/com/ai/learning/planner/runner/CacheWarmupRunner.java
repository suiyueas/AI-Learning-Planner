package com.ai.learning.planner.runner;

import com.ai.learning.planner.entity.KnowledgeDocument;
import com.ai.learning.planner.entity.KnowledgeNode;
import com.ai.learning.planner.repository.KnowledgeDocumentRepository;
import com.ai.learning.planner.repository.KnowledgeNodeRepository;
import com.ai.learning.planner.repository.QuestionRepository;
import com.ai.learning.planner.service.ConfigDataCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热Runner
 * 应用启动时异步加载常用数据到Redis缓存，加快首次访问速度
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupRunner implements ApplicationRunner {

    private final ConfigDataCacheService configDataCacheService;
    private final KnowledgeNodeRepository knowledgeNodeRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final QuestionRepository questionRepository;

    private static final int ASYNC_TIMEOUT_SECONDS = 30;

    @Override
    public void run(ApplicationArguments args) {
        log.debug("[Warmup] ========== 开始缓存预热 ==========");

        CompletableFuture.runAsync(() -> {
            int docCount = 0;
            int nodeCount = 0;
            int subjectCount = 0;

            try {
                nodeCount = warmupKnowledgeNodes();
                docCount = warmupKnowledgeDocuments();
                subjectCount = warmupSubjects();

                // 汇总日志：一次性输出预热结果
                log.info("✅ 缓存预热完成: {} 个文档, {} 个节点, {} 个科目", docCount, nodeCount, subjectCount);
            } catch (Exception e) {
                log.error("[Warmup] 缓存预热异常: {}", e.getMessage(), e);
            }
        }).orTimeout(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .exceptionally(ex -> {
              log.warn("[Warmup] 缓存预热超时或失败: {}", ex.getMessage());
              return null;
          });

        log.debug("[Warmup] 缓存预热已启动 (异步执行，预计 {} 秒内完成)", ASYNC_TIMEOUT_SECONDS);
    }

    private int warmupKnowledgeNodes() {
        try {
            List<KnowledgeNode> nodes = knowledgeNodeRepository.findAll();

            if (!nodes.isEmpty()) {
                configDataCacheService.cacheKnowledgeNodes(nodes);
                return nodes.size();
            }
        } catch (Exception e) {
            log.warn("[Warmup] 知识节点预热失败: {}", e.getMessage());
        }
        return 0;
    }

    private int warmupKnowledgeDocuments() {
        try {
            List<KnowledgeDocument> docs = knowledgeDocumentRepository.findAll();

            if (!docs.isEmpty()) {
                configDataCacheService.cacheKnowledgeDocuments(docs);
                return docs.size();
            }
        } catch (Exception e) {
            log.warn("[Warmup] 知识文档预热失败: {}", e.getMessage());
        }
        return 0;
    }

    private int warmupSubjects() {
        try {
            Set<String> subjects = new HashSet<>();

            questionRepository.findAll().forEach(q -> {
                if (q.getSubject() != null) {
                    subjects.add(q.getSubject());
                }
            });

            if (!subjects.isEmpty()) {
                configDataCacheService.cacheSubjects(new ArrayList<>(subjects));
                return subjects.size();
            }
        } catch (Exception e) {
            log.warn("[Warmup] 科目列表预热失败: {}", e.getMessage());
        }
        return 0;
    }
}