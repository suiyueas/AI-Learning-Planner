package com.ai.learning.planner.mcp.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 统一工具描述注册表
 * 所有工具的完整定义集中于此，供以下场景使用：
 * 1. ChatService 构建 LLM 工具选择清单（含别名、使用时机、参数说明）
 * 2. ToolsController 返回前端工具列表与参数表单
 * 3. search_tools / get_tool_detail 渐进式工具发现
 * 
 * 重构说明（学者视角精简版）：
 * - 11个工具精简为6个核心工具 + 1个调试面板
 * - 合并高度相关的工具减少认知负担
 * - 元工具（调试用）移至后台隐藏
 */
public final class ToolDefinitionRegistry {

    /**
     * 学者视角：前端展示的6个核心工具（不含调试面板）
     * 调试工具（search_tools, get_tool_detail, web_fetch）标记 isHidden=true
     */
    private static final List<ToolDefinition> TOOLS = List.of(
            // ===== 1. 全域学术检索（合并：资源检索 + 联网搜索 + 知识图谱） =====
            new ToolDefinition(
                    "unified_academic_search", "全域学术检索",
                    "跨源统一检索：同时搜索内部知识库、外部联网资源、知识图谱概念，自动过滤无关网页，返回标注来源的整合结果",
                    List.of("搜索", "查询", "全网搜索", "概念搜索", "找资料"),
                    "input_search",
                    "用户需要查找学习资料、搜索全网最新信息、查询概念定义时使用，一站式跨源查找",
                    List.of(
                            new ToolDefinition.ParamDef("query", "搜索关键词", "string", true),
                            new ToolDefinition.ParamDef("searchInternal", "是否搜索内部知识库，默认true", "boolean", false, "true"),
                            new ToolDefinition.ParamDef("searchWeb", "是否联网搜索，默认true", "boolean", false, "true"),
                            new ToolDefinition.ParamDef("searchGraph", "是否查询知识图谱，默认false", "boolean", false, "false"),
                            new ToolDefinition.ParamDef("limit", "每类结果上限，默认5", "number", false, "5")
                    )
            ),

            // ===== 2. 深度文献解析（合并：文档摘要 + 知识点提取） =====
            new ToolDefinition(
                    "deep_document_analysis", "深度文献解析",
                    "上传文档或输入文本，一键生成：精华摘要 + 结构化知识卡片（核心术语、定义、关联）",
                    List.of("摘要", "总结", "提炼重点", "知识卡片", "文档解析"),
                    "understanding_output",
                    "用户需要快速把握文档核心内容、提取学习重点、生成知识卡片时使用",
                    List.of(
                            new ToolDefinition.ParamDef("documentId", "知识库文档ID（与content二选一）", "string", false),
                            new ToolDefinition.ParamDef("content", "文档文本内容（与documentId二选一）", "textarea", false),
                            new ToolDefinition.ParamDef("summaryLength", "摘要长度", "select", false, "medium",
                                    List.of("short", "medium", "long")),
                            new ToolDefinition.ParamDef("keywordCount", "提取关键词数量，默认8", "number", false, "8"),
                            new ToolDefinition.ParamDef("includeGlossary", "是否生成术语对照表，默认true", "boolean", false, "true")
                    )
            ),

            // ===== 3. 智能测评出题 =====
            new ToolDefinition(
                    "smart_quiz_generation", "智能测评出题",
                    "基于当前学习内容，自动生成带考点解析的选择/填空/判断题，支持难度调节",
                    List.of("出题", "测验", "练习题", "考试", "自测"),
                    "assessment_loop",
                    "用户想要测试学习效果、做练习题、准备考试时使用",
                    List.of(
                            new ToolDefinition.ParamDef("topic", "学习内容或知识点", "textarea", true),
                            new ToolDefinition.ParamDef("questionType", "题型", "select", false, "mixed",
                                    List.of("choice", "judgment", "fill", "mixed")),
                            new ToolDefinition.ParamDef("count", "题目数量，默认5", "number", false, "5"),
                            new ToolDefinition.ParamDef("difficulty", "难度", "select", false, "中等",
                                    List.of("简单", "中等", "困难"))
                    )
            ),

            // ===== 4. 学术翻译 =====
            new ToolDefinition(
                    "academic_translation", "学术翻译",
                    "精准翻译学术资料与外语文献，自动保留技术术语原义，并生成中英术语对照表辅助理解",
                    List.of("翻译", "译", "术语对照", "中英对照"),
                    "understanding_output",
                    "用户需要翻译外文文献、理解英文技术资料、生成术语对照表时使用",
                    List.of(
                            new ToolDefinition.ParamDef("text", "待翻译文本", "textarea", true),
                            new ToolDefinition.ParamDef("sourceLang", "源语言，auto为自动检测", "string", false, "auto"),
                            new ToolDefinition.ParamDef("targetLang", "目标语言", "string", false, "中文"),
                            new ToolDefinition.ParamDef("preserveTechTerms", "保留技术术语并生成对照表", "boolean", false, "true")
                    )
            ),

            // ===== 5. 全链路学习助手（场景入口） =====
            new ToolDefinition(
                    "full_chain_learning", "全链路学习助手",
                    "【场景入口】一键完成“文献解析→知识点提取→出题”全流程，生成完整学习报告",
                    List.of("学习助手", "学习闭环", "一站式学习", "完整学习流程"),
                    "assessment_loop",
                    "用户想要系统性学习一份材料并验证效果时的入口工具，自动串联深度文献解析和测评出题",
                    List.of(
                            new ToolDefinition.ParamDef("document", "学习材料内容（与documentId二选一）", "textarea", false),
                            new ToolDefinition.ParamDef("documentId", "知识库文档ID（与document二选一）", "string", false),
                            new ToolDefinition.ParamDef("questionType", "测验题型", "select", false, "mixed",
                                    List.of("choice", "judgment", "fill", "mixed")),
                            new ToolDefinition.ParamDef("quizCount", "测验题数量，默认5", "number", false, "5")
                    )
            ),

            // ===== 6. 工具调试面板（后台隐藏，仅管理员可用）=====
            new ToolDefinition(
                    "tool_debug_panel", "工具调试面板",
                    "【仅面向管理员/高级用户】提供工具搜索、工具详情、网页抓取等调试功能",
                    List.of("调试", "工具搜索", "工具详情", "网页抓取"),
                    "system_debug",
                    "管理员调试或需要使用底层工具时访问",
                    List.of(
                            new ToolDefinition.ParamDef("toolName", "要调试的工具名称（可选）", "string", false)
                    ),
                    true, true
            ),

            // ===== 以下为已合并/淘汰工具，标记 isHidden=true，不在前端列表展示 =====
            
            // 资源检索 → 已合并至 unified_academic_search
            new ToolDefinition(
                    "search_resources", "资源检索",
                    "（已合并至「全域学术检索」）在资源库和知识库中搜索学习材料",
                    List.of("找资源", "推荐课程", "学习资料"),
                    "input_search",
                    "内部使用，已被 unified_academic_search 替代",
                    List.of(
                            new ToolDefinition.ParamDef("keyword", "搜索关键词", "string", true),
                            new ToolDefinition.ParamDef("type", "资源类型过滤", "select", false, "all",
                                    List.of("all", "video", "article", "course", "book")),
                            new ToolDefinition.ParamDef("limit", "返回数量上限", "number", false, "10")
                    ),
                    true // isHidden
            ),

            // 知识图谱查询 → 已合并至 unified_academic_search
            new ToolDefinition(
                    "query_knowledge_graph", "知识图谱查询",
                    "（已合并至「全域学术检索」）查询知识图谱节点详情和学习路径",
                    List.of("图谱", "依赖关系", "学习路径"),
                    "input_search",
                    "内部使用，已被 unified_academic_search 替代",
                    List.of(
                            new ToolDefinition.ParamDef("nodeId", "节点ID或名称", "string", true),
                            new ToolDefinition.ParamDef("depth", "关联展开深度", "number", false, "2")
                    ),
                    true // isHidden
            ),

            // 文档摘要 → 已合并至 deep_document_analysis
            new ToolDefinition(
                    "summarize_document", "文档摘要",
                    "（已合并至「深度文献解析」）使用大模型生成学习文档摘要",
                    List.of("总结", "概括", "提炼"),
                    "understanding_output",
                    "内部使用，已被 deep_document_analysis 替代",
                    List.of(
                            new ToolDefinition.ParamDef("documentId", "知识库文档ID", "string", false),
                            new ToolDefinition.ParamDef("content", "文档文本内容", "textarea", false),
                            new ToolDefinition.ParamDef("length", "摘要长度", "select", false, "medium",
                                    List.of("short", "medium", "long"))
                    ),
                    true // isHidden
            ),

            // 知识点提取 → 已合并至 deep_document_analysis
            new ToolDefinition(
                    "extract_keywords", "知识点提取",
                    "（已合并至「深度文献解析」）从学习材料中提取核心知识点",
                    List.of("知识点", "关键词", "重点"),
                    "understanding_output",
                    "内部使用，已被 deep_document_analysis 替代",
                    List.of(
                            new ToolDefinition.ParamDef("text", "学习材料内容", "textarea", true),
                            new ToolDefinition.ParamDef("domain", "学科/领域", "string", false),
                            new ToolDefinition.ParamDef("count", "提取数量", "number", false, "10")
                    ),
                    true // isHidden
            ),

            // 联网搜索 → 已合并至 unified_academic_search
            new ToolDefinition(
                    "web_search", "联网搜索",
                    "（已合并至「全域学术检索」）通过互联网搜索引擎获取最新信息",
                    List.of("搜索", "查资料", "最新消息"),
                    "input_search",
                    "内部使用，已被 unified_academic_search 替代",
                    List.of(
                            new ToolDefinition.ParamDef("query", "搜索内容", "string", true),
                            new ToolDefinition.ParamDef("numResults", "返回结果数量", "number", false, "5")
                    ),
                    true // isHidden
            ),

            // 网页抓取 → 移至调试面板
            new ToolDefinition(
                    "web_fetch", "网页抓取",
                    "（移至调试面板）抓取指定网页的文本内容用于学习分析",
                    List.of("抓网页", "爬取", "网页内容"),
                    "system_debug",
                    "仅管理员调试使用",
                    List.of(
                            new ToolDefinition.ParamDef("url", "网页URL地址", "string", true)
                    ),
                    true // isHidden
            ),

            // 工具搜索 → 移至调试面板
            new ToolDefinition(
                    "search_tools", "工具搜索",
                    "（移至调试面板）按关键词搜索可用工具清单",
                    List.of("工具列表", "找工具"),
                    "system_debug",
                    "仅管理员调试使用",
                    List.of(
                            new ToolDefinition.ParamDef("keyword", "功能关键词", "string", true)
                    ),
                    true // isHidden
            ),

            // 工具详情 → 移至调试面板
            new ToolDefinition(
                    "get_tool_detail", "工具详情",
                    "（移至调试面板）获取指定工具的完整定义与参数说明",
                    List.of("工具参数", "工具说明"),
                    "system_debug",
                    "仅管理员调试使用",
                    List.of(
                            new ToolDefinition.ParamDef("toolName", "工具ID", "string", true)
                    ),
                    true // isHidden
            )
    );

    private static final Map<String, ToolDefinition> BY_ID = new LinkedHashMap<>();
    private static final Map<String, ToolDefinition> BY_ALIAS = new LinkedHashMap<>();

    static {
        for (ToolDefinition def : TOOLS) {
            BY_ID.put(def.id(), def);
            BY_ALIAS.put(def.name(), def);
            for (String alias : def.aliases()) {
                BY_ALIAS.put(alias, def);
            }
        }
    }

    private ToolDefinitionRegistry() {
    }

    /** 全部工具定义 */
    public static List<ToolDefinition> all() {
        return TOOLS;
    }

    /** 前端可见工具列表（排除隐藏的调试工具） */
    public static List<ToolDefinition> visibleTools() {
        return TOOLS.stream()
                .filter(def -> !def.isHidden())
                .toList();
    }

    /** 按工具ID查找 */
    public static Optional<ToolDefinition> byId(String toolId) {
        return Optional.ofNullable(BY_ID.get(toolId));
    }

    /** 按名称/别名/ID 模糊查找 */
    public static Optional<ToolDefinition> findByNameOrAlias(String name) {
        Optional<ToolDefinition> exact = Optional.ofNullable(BY_ALIAS.get(name));
        if (exact.isPresent()) return exact;
        String lower = name.toLowerCase();
        return TOOLS.stream()
                .filter(t -> t.id().contains(lower)
                        || t.name().contains(name)
                        || t.aliases().stream().anyMatch(a -> a.contains(name)))
                .findFirst();
    }

    /** 按关键词搜索（匹配ID/名称/描述/别名） */
    public static List<ToolDefinition> search(String keyword) {
        String lower = keyword.toLowerCase();
        return TOOLS.stream()
                .filter(t -> t.id().toLowerCase().contains(lower)
                        || t.name().contains(keyword)
                        || t.description().contains(keyword)
                        || t.category().contains(keyword)
                        || t.aliases().stream().anyMatch(a -> a.contains(keyword)))
                .toList();
    }

    /**
     * 转换为前端展示用的 Map（含参数表单结构）
     * 字段与前端 Tools.vue / toolsApi.js 约定一致：
     * id / name / description / icon / category / status / usageCount / paramsSchema / aiEnabled / isHidden
     */
    public static Map<String, Object> toFrontendMap(ToolDefinition def, long usageCount) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", def.id());
        map.put("name", def.name());
        map.put("description", def.description());
        map.put("icon", defaultIcon(def.category()));
        map.put("category", def.category());
        map.put("status", "available");
        map.put("usageCount", usageCount);
        map.put("aliases", def.aliases());
        map.put("aiEnabled", isAiTool(def.id()));
        map.put("isHidden", def.isHidden());
        map.put("paramsSchema", def.params().stream().map(p -> {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("name", p.name());
            pm.put("label", chineseLabel(def.id(), p.name()));
            pm.put("type", p.type());
            pm.put("required", p.required());
            pm.put("default", p.defaultValue());
            pm.put("description", p.description());
            pm.put("placeholder", chinesePlaceholder(def.id(), p.name()));
            if (!p.options().isEmpty()) {
                pm.put("options", p.options().stream()
                        .map(o -> Map.of("label", chineseOptionLabel(p.name(), o), "value", o))
                        .toList());
            }
            return pm;
        }).toList());
        return map;
    }

    /**
     * 参数中文标签映射
     */
    private static String chineseLabel(String toolId, String paramName) {
        Map<String, Map<String, String>> labels = Map.of(
                "unified_academic_search", Map.of(
                        "query", "搜索关键词",
                        "searchInternal", "搜索内部知识库",
                        "searchWeb", "联网搜索",
                        "searchGraph", "查询知识图谱",
                        "limit", "每类结果上限"
                ),
                "deep_document_analysis", Map.of(
                        "documentId", "文档ID",
                        "content", "文档内容",
                        "summaryLength", "摘要长度",
                        "keywordCount", "关键词数量",
                        "includeGlossary", "生成术语对照表"
                ),
                "smart_quiz_generation", Map.of(
                        "topic", "学习内容",
                        "questionType", "题型",
                        "count", "题目数量",
                        "difficulty", "难度"
                ),
                "academic_translation", Map.of(
                        "text", "待翻译文本",
                        "sourceLang", "源语言",
                        "targetLang", "目标语言",
                        "preserveTechTerms", "保留技术术语"
                ),
                "full_chain_learning", Map.of(
                        "document", "学习材料内容",
                        "documentId", "文档ID",
                        "questionType", "题型",
                        "quizCount", "题目数量"
                )
        );
        return labels.getOrDefault(toolId, Map.of()).getOrDefault(paramName, paramName);
    }

    /**
     * 参数中文占位符映射
     */
    private static String chinesePlaceholder(String toolId, String paramName) {
        Map<String, Map<String, String>> placeholders = Map.of(
                "unified_academic_search", Map.of(
                        "query", "输入搜索关键词"
                ),
                "deep_document_analysis", Map.of(
                        "documentId", "知识库文档ID（与内容二选一）",
                        "content", "输入或粘贴文档内容"
                ),
                "smart_quiz_generation", Map.of(
                        "topic", "输入学习内容或主题"
                ),
                "academic_translation", Map.of(
                        "text", "输入待翻译文本"
                ),
                "full_chain_learning", Map.of(
                        "document", "学习材料内容（与文档ID二选一）",
                        "documentId", "知识库文档ID"
                )
        );
        return placeholders.getOrDefault(toolId, Map.of()).getOrDefault(paramName, "请输入" + chineseLabel(toolId, paramName));
    }

    /**
     * 下拉选项中文标签映射
     */
    private static String chineseOptionLabel(String paramName, String value) {
        Map<String, Map<String, String>> optionLabels = Map.of(
                "summaryLength", Map.of(
                        "short", "简短",
                        "medium", "中等",
                        "long", "详细"
                ),
                "questionType", Map.of(
                        "choice", "选择题",
                        "judgment", "判断题",
                        "fill", "填空题",
                        "mixed", "混合"
                ),
                "difficulty", Map.of(
                        "简单", "简单",
                        "中等", "中等",
                        "困难", "困难"
                ),
                "targetLang", Map.of(
                        "中文", "中文",
                        "英文", "英文",
                        "日文", "日文"
                )
        );
        return optionLabels.getOrDefault(paramName, Map.of()).getOrDefault(value, value);
    }

    /** 分类默认图标（学者视角新分类） */
    public static String defaultIcon(String category) {
        return switch (category) {
            case "input_search" -> "📚";
            case "understanding_output" -> "✍️";
            case "assessment_loop" -> "🧠";
            case "system_debug" -> "⚙️";
            case "knowledge" -> "🔍";
            case "analysis" -> "📝";
            case "assessment" -> "✏️";
            case "assistance" -> "🌐";
            case "discovery" -> "🧭";
            default -> "🔧";
        };
    }

    /** 分类中文名（学者视角新分类） */
    public static String categoryName(String category) {
        return switch (category) {
            case "input_search" -> "输入与检索";
            case "understanding_output" -> "理解与输出";
            case "assessment_loop" -> "评估与闭环";
            case "system_debug" -> "系统调试";
            case "knowledge" -> "知识检索";
            case "analysis" -> "学习分析";
            case "assessment" -> "学习评估";
            case "assistance" -> "学习辅助";
            case "discovery" -> "工具发现";
            default -> "其他";
        };
    }

    /** 全部分类列表（前端分类 tab 使用，学者视角新分类） */
    public static List<Map<String, Object>> categories() {
        return List.of(
                Map.of("id", "all", "name", "全部", "icon", "📦", "color", "#a78bfa"),
                Map.of("id", "input_search", "name", "输入与检索", "icon", "📚", "color", "#00f5d4"),
                Map.of("id", "understanding_output", "name", "理解与输出", "icon", "✍️", "color", "#ffb86c"),
                Map.of("id", "assessment_loop", "name", "评估与闭环", "icon", "🧠", "color", "#ff6b9d"),
                Map.of("id", "system_debug", "name", "系统调试", "icon", "⚙️", "color", "#a78bfa")
        );
    }

    /** AI 赋能工具集合（由大模型驱动，非纯数据查询） */
    private static final List<String> AI_TOOL_IDS = List.of(
            "deep_document_analysis", "smart_quiz_generation", "academic_translation", "full_chain_learning",
            "summarize_document", "extract_keywords", "generate_quiz", "translate_text", "learning_assistant"
    );

    /** 判断工具是否为 AI 赋能工具 */
    public static boolean isAiTool(String toolId) {
        return AI_TOOL_IDS.contains(toolId);
    }
}