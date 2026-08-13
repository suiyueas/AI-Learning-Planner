package com.ai.learning.planner.mcp.ai;

import java.util.List;

/**
 * 工具定义（统一描述规范）
 * 所有 MCP 工具必须在此注册完整定义，供 LLM 工具选择与前端展示使用
 *
 * @param id          工具唯一标识（snake_case）
 * @param name        工具中文名
 * @param description 详细功能描述（面向 LLM，说明能力边界与输出内容）
 * @param aliases     别名/同义词（帮助 LLM 联想匹配）
 * @param category    分类：input_search(输入与检索) / understanding_output(理解与输出) / assessment_loop(评估与闭环) / system_debug(系统调试)
 * @param usageHint   使用时机提示（告诉 LLM 何时该用这个工具）
 * @param params      参数定义列表
 * @param isHidden    是否隐藏（调试工具移至后台不显示在主界面）
 * @param adminOnly   是否仅管理员可用（执行期强制校验，未认证/普通用户调用直接拒绝）
 */
public record ToolDefinition(
        String id,
        String name,
        String description,
        List<String> aliases,
        String category,
        String usageHint,
        List<ParamDef> params,
        boolean isHidden,
        boolean adminOnly
) {

    /**
     * 兼容旧版构造：默认不隐藏、非管理员专属
     */
    public ToolDefinition(String id, String name, String description, List<String> aliases,
                         String category, String usageHint, List<ParamDef> params) {
        this(id, name, description, aliases, category, usageHint, params, false, false);
    }

    /**
     * 兼容旧版构造：默认非管理员专属
     */
    public ToolDefinition(String id, String name, String description, List<String> aliases,
                         String category, String usageHint, List<ParamDef> params, boolean isHidden) {
        this(id, name, description, aliases, category, usageHint, params, isHidden, false);
    }

    /**
     * 参数定义
     *
     * @param name          参数名（snake_case）
     * @param description   参数说明
     * @param type          类型：string / number / boolean / textarea / select
     * @param required      是否必填
     * @param defaultValue  默认值
     * @param options       可选值（select 类型）
     */
    public record ParamDef(
            String name,
            String description,
            String type,
            boolean required,
            String defaultValue,
            List<String> options
    ) {

        public ParamDef(String name, String description, String type, boolean required, String defaultValue) {
            this(name, description, type, required, defaultValue, List.of());
        }

        public ParamDef(String name, String description, String type, boolean required) {
            this(name, description, type, required, null, List.of());
        }
    }
}