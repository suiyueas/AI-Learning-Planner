package com.ai.learning.planner.mcp.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一工具注册表测试
 * 验证：工具描述覆盖率 100%、参数必填定义、搜索与查找能力
 */
class ToolDefinitionRegistryTest {

    @Test
    void allTools_haveCompleteDefinitions() {
        List<ToolDefinition> tools = ToolDefinitionRegistry.all();
        // 6 个核心工具 + 8 个已合并/隐藏工具共 14 个
        assertEquals(14, tools.size(), "工具数量应为 14 个");

        for (ToolDefinition tool : tools) {
            assertFalse(tool.id().isBlank(), "工具ID不能为空: " + tool);
            assertFalse(tool.name().isBlank(), "工具名称不能为空: " + tool.id());
            // 描述覆盖率验收标准：展示工具 ≥30 字；隐藏/已合并工具（仅内部追溯）≥20 字
            int minLen = tool.isHidden() ? 20 : 30;
            assertTrue(tool.description().length() >= minLen,
                    "工具描述过短（<" + minLen + "字）: " + tool.id());
            assertFalse(tool.usageHint().isBlank(), "使用时机提示不能为空: " + tool.id());
            assertFalse(tool.params().isEmpty(), "工具参数定义不能为空: " + tool.id());
            // 参数必须有说明
            for (ToolDefinition.ParamDef p : tool.params()) {
                assertFalse(p.description().isBlank(), "参数说明不能为空: " + tool.id() + "." + p.name());
            }
        }
    }

    @Test
    void allTools_haveValidCategories() {
        // 学者视角新分类体系：输入与检索/理解与输出/评估与闭环/系统调试
        for (ToolDefinition tool : ToolDefinitionRegistry.all()) {
            assertTrue(List.of("input_search", "understanding_output", "assessment_loop", "system_debug")
                            .contains(tool.category()),
                    "无效分类: " + tool.id() + " -> " + tool.category());
        }
    }

    @Test
    void requiredParams_areCorrect() {
        // 必填参数覆盖验收：核心入参必须 required=true
        ToolDefinition quiz = ToolDefinitionRegistry.byId("smart_quiz_generation").orElseThrow();
        assertTrue(quiz.params().stream().anyMatch(p -> p.name().equals("topic") && p.required()),
                "smart_quiz_generation.topic 必须为必填");

        ToolDefinition translate = ToolDefinitionRegistry.byId("academic_translation").orElseThrow();
        assertTrue(translate.params().stream().anyMatch(p -> p.name().equals("text") && p.required()),
                "academic_translation.text 必须为必填");

        ToolDefinition search = ToolDefinitionRegistry.byId("search_tools").orElseThrow();
        assertTrue(search.params().stream().anyMatch(p -> p.name().equals("keyword") && p.required()),
                "search_tools.keyword 必须为必填");
    }

    @Test
    void byId_findsExactMatch() {
        assertTrue(ToolDefinitionRegistry.byId("summarize_document").isPresent());
        assertTrue(ToolDefinitionRegistry.byId("full_chain_learning").isPresent());
        assertTrue(ToolDefinitionRegistry.byId("get_tool_detail").isPresent());
        assertTrue(ToolDefinitionRegistry.byId("not_exist_tool").isEmpty());
    }

    @Test
    void findByNameOrAlias_matchesAliasAndChineseNames() {
        // 中文别名匹配（LLM 可能用中文描述找工具）
        assertEquals("smart_quiz_generation", ToolDefinitionRegistry.findByNameOrAlias("出题").orElseThrow().id());
        assertEquals("academic_translation", ToolDefinitionRegistry.findByNameOrAlias("翻译").orElseThrow().id());
        assertEquals("summarize_document", ToolDefinitionRegistry.findByNameOrAlias("文档摘要").orElseThrow().id());
        assertEquals("extract_keywords", ToolDefinitionRegistry.findByNameOrAlias("知识点").orElseThrow().id());
    }

    @Test
    void search_matchesKeywordAcrossFields() {
        assertFalse(ToolDefinitionRegistry.search("摘要").isEmpty(), "搜索'摘要'应命中文档摘要工具");
        assertFalse(ToolDefinitionRegistry.search("测验").isEmpty(), "搜索'测验'应命中出题工具");
        assertTrue(ToolDefinitionRegistry.search("知识图谱").stream()
                .anyMatch(t -> t.id().equals("query_knowledge_graph")), "搜索'知识图谱'应命中图谱查询");
        assertTrue(ToolDefinitionRegistry.search("不存在功能xyz").isEmpty());
    }

    @Test
    void toFrontendMap_hasFrontendRequiredFields() {
        ToolDefinition def = ToolDefinitionRegistry.byId("smart_quiz_generation").orElseThrow();
        Map<String, Object> map = ToolDefinitionRegistry.toFrontendMap(def, 42L);
        assertEquals("smart_quiz_generation", map.get("id"));
        assertEquals("available", map.get("status"));
        assertEquals(42L, map.get("usageCount"));
        assertEquals("assessment_loop", map.get("category"));
        assertTrue(map.get("paramsSchema") instanceof List<?> schema && !((List<?>) schema).isEmpty(),
                "paramsSchema 必须包含参数定义");
    }

    @Test
    void categories_containsAll() {
        List<Map<String, Object>> categories = ToolDefinitionRegistry.categories();
        assertEquals(5, categories.size(), "分类应包含 全部+4 类");
        assertTrue(categories.stream().anyMatch(c -> c.get("id").equals("input_search")));
        assertTrue(categories.stream().anyMatch(c -> c.get("id").equals("assessment_loop")));
    }
}
