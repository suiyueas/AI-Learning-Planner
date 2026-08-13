package com.ai.learning.planner.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 功能模块控制器
 */
@RestController
@RequestMapping("/modules")
@Slf4j
public class ModuleController {

    /**
     * 获取功能模块列表
     * @return 功能模块列表
     */
    @GetMapping
    public Map<String, Object> getModules() {
        log.info("获取功能模块列表");
        return Map.of("success", true, "data", getModuleList());
    }

    /**
     * 获取功能模块分组列表
     * @return 模块分组列表
     */
    @GetMapping("/groups")
    public Map<String, Object> getModuleGroups() {
        log.info("获取功能模块分组列表");
        return Map.of("success", true, "data", getModuleGroupList());
    }

    /**
     * 获取模块列表数据（已移除冗余「个人中心」入口，用户可通过右上角下拉菜单进入）
     */
    private List<Map<String, Object>> getModuleList() {
        return List.of(
            Map.of("id", "M2", "icon", "bar-chart", "name", "能力测评", "group", "学习基础", "desc", "多维度评估水平"),
            Map.of("id", "M3", "icon", "target", "name", "目标设定", "group", "学习基础", "desc", "设定学习目标"),
            Map.of("id", "M4", "icon", "map", "name", "学习路径", "group", "学习基础", "desc", "规划学习路径"),
            Map.of("id", "M9", "icon", "calendar", "name", "学习日历", "group", "学习基础", "desc", "日历视图管理任务"),
            Map.of("id", "M7", "icon", "line-chart", "name", "进度追踪", "group", "学习辅助", "desc", "追踪学习进度"),
            Map.of("id", "M6", "icon", "message-square", "name", "智能答疑", "group", "学习辅助", "desc", "AI智能问答"),
            Map.of("id", "M12", "icon", "book", "name", "学习笔记", "group", "学习辅助", "desc", "记录整理笔记"),
            Map.of("id", "M10", "icon", "pen-tool", "name", "习题生成", "group", "学习进阶", "desc", "自动生成练习题"),
            Map.of("id", "M11", "icon", "clipboard", "name", "学情报告", "group", "学习进阶", "desc", "分析学习数据"),
            Map.of("id", "M14", "icon", "award", "name", "成就打卡", "group", "学习进阶", "desc", "记录学习成就"),
            Map.of("id", "M15", "icon", "code", "name", "代码解析", "group", "学习进阶", "desc", "AI分析代码质量")
        );
    }

    /**
     * 获取模块分组数据（已移除冗余「个人中心」入口）
     */
    private List<Map<String, Object>> getModuleGroupList() {
        return List.of(
            Map.of(
                "id", "basic",
                "icon", "diamond",
                "name", "学习基础",
                "modules", List.of(
                    Map.of("id", "M2", "icon", "bar-chart", "name", "能力测评", "desc", "多维度评估水平"),
                    Map.of("id", "M3", "icon", "target", "name", "目标设定", "desc", "设定学习目标"),
                    Map.of("id", "M4", "icon", "map", "name", "学习路径", "desc", "规划学习路径"),
                    Map.of("id", "M9", "icon", "calendar", "name", "学习日历", "desc", "日历视图管理任务")
                )
            ),
            Map.of(
                "id", "assist",
                "icon", "layers",
                "name", "学习辅助",
                "modules", List.of(
                    Map.of("id", "M7", "icon", "line-chart", "name", "进度追踪", "desc", "追踪学习进度"),
                    Map.of("id", "M6", "icon", "message-square", "name", "智能答疑", "desc", "AI智能问答"),
                    Map.of("id", "M12", "icon", "book", "name", "学习笔记", "desc", "记录整理笔记")
                )
            ),
            Map.of(
                "id", "advanced",
                "icon", "target",
                "name", "学习进阶",
                "modules", List.of(
                    Map.of("id", "M10", "icon", "pen-tool", "name", "习题生成", "desc", "自动生成练习题"),
                    Map.of("id", "M11", "icon", "clipboard", "name", "学情报告", "desc", "分析学习数据"),
                    Map.of("id", "M14", "icon", "award", "name", "成就打卡", "desc", "记录学习成就"),
                    Map.of("id", "M15", "icon", "code", "name", "代码解析", "desc", "AI分析代码质量")
                )
            )
        );
    }
}
