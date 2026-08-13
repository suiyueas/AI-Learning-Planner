package com.ai.learning.planner.agent.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 情景记忆（内存实现）测试
 */
class InMemoryEpisodicMemoryTest {

    private InMemoryEpisodicMemory memory;

    @BeforeEach
    void setUp() {
        memory = new InMemoryEpisodicMemory();
    }

    private EpisodicMemory.Episode episode(String id, String type, String summary, String exp, String... tags) {
        return new EpisodicMemory.Episode(id, type, summary, exp, List.of(tags), Instant.now());
    }

    @Test
    void search_returnsRelevantEpisodes() {
        memory.store(episode("e1", "planning", "制定Python学习计划", "先评估基础，再分阶段安排", "python", "计划"));
        memory.store(episode("e2", "qa", "解释机器学习概念", "用类比解释，配实例", "ml", "答疑"));

        var hits = memory.search("如何制定Python学习计划", 3);
        assertFalse(hits.isEmpty());
        assertEquals("e1", hits.get(0).id(), "相似度最高的经验应排第一");
        assertTrue(hits.get(0).taskSummary().contains("Python"));
    }

    @Test
    void search_unrelatedReturnsEmpty() {
        memory.store(episode("e1", "planning", "制定Python学习计划", "分阶段安排", "python"));
        assertTrue(memory.search("查询数据库性能优化方案", 3).isEmpty());
    }

    @Test
    void buildHiddenCoT_formatsInjection() {
        memory.store(episode("e1", "planning", "制定Java学习计划", "先学基础语法再框架", "java"));
        String hiddenCot = memory.buildHiddenCoT("制定Java学习路线", 2);
        assertTrue(hiddenCot.contains("【历史经验参考】"));
        assertTrue(hiddenCot.contains("e1") == false && hiddenCot.contains("制定Java学习计划"));
    }

    @Test
    void buildHiddenCoT_noMatchReturnsEmpty() {
        assertEquals("", memory.buildHiddenCoT("无关内容abc", 2));
    }

    @Test
    void storeAndSize() {
        assertEquals(0, memory.size());
        memory.store(episode("e1", "planning", "计划", "经验", "tag"));
        memory.store(episode("e2", "planning", "计划2", "经验2", "tag"));
        assertEquals(2, memory.size());
        memory.clear();
        assertEquals(0, memory.size());
    }
}
