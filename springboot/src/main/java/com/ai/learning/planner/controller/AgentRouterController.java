package com.ai.learning.planner.controller;

import com.ai.learning.planner.service.ModelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 智能路由控制器
 * 使用 LLM 进行意图识别，推荐最合适的 Agent
 */
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Slf4j
public class AgentRouterController {

    private final ModelManager modelManager;

    /**
     * 智能路由 - 使用 LLM 分析用户意图并推荐 Agent
     */
    @PostMapping("/route")
    public Map<String, Object> routeAgent(@RequestBody Map<String, String> request) {
        String input = request.getOrDefault("input", "").trim();
        if (input.isEmpty()) {
            return Map.of("success", false, "message", "输入不能为空");
        }

        log.info("智能路由请求: input={}", input);

        try {
            ChatClient chatClient = modelManager.createChatClient();

            String systemPrompt = """
                你是一个智能Agent路由器。根据用户的输入，判断用户需要哪个Agent来帮助。
                
                可用的Agent列表：
                - diagnosis: 诊断Agent - 用于能力测评、画像构建、薄弱点挖掘、水平评估
                - planner: 规划Agent - 用于路径生成、学习计划、路线规划、目标设定
                - tutor: 答疑Agent - 用于苏格拉底引导、知识解答、概念解释、问题答疑
                - reporter: 报告Agent - 用于学情分析、报告生成、数据总结、PDF导出
                - exercise: 习题Agent - 用于习题生成、智能批改、练习题、作业
                - search: 搜索Agent - 用于联网搜索、资料检索、资源发现、教程查找
                - knowledge: 知识检索Agent - 用于文档检索、语义搜索、知识问答、知识库查询
                
                请根据用户输入，返回最匹配的Agent ID（只返回ID，不要其他内容）。
                如果无法判断，返回 "planner"。
                """;

            String result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(input)
                    .call()
                    .content();

            String agentId = result.trim().toLowerCase();

            // 验证 agentId 是否有效
            String[] validAgents = {"diagnosis", "planner", "tutor", "reporter", "exercise", "search", "knowledge"};
            boolean isValid = false;
            for (String valid : validAgents) {
                if (valid.equals(agentId)) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                agentId = "planner";
            }

            log.info("智能路由结果: input={}, agentId={}", input, agentId);

            return Map.of("success", true, "data", Map.of("agentId", agentId));
        } catch (Exception e) {
            log.error("智能路由失败: {}", e.getMessage());
            // 降级为关键词匹配
            String fallbackAgent = fallbackRoute(input);
            return Map.of("success", true, "data", Map.of("agentId", fallbackAgent));
        }
    }

    /**
     * 降级路由 - 关键词匹配
     */
    private String fallbackRoute(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("诊断") || lower.contains("水平") || lower.contains("能力") || lower.contains("薄弱点") || lower.contains("测评") || lower.contains("评估")) {
            return "diagnosis";
        } else if (lower.contains("规划") || lower.contains("计划") || lower.contains("学习路径") || lower.contains("路线") || lower.contains("目标")) {
            return "planner";
        } else if (lower.contains("答疑") || lower.contains("解释") || lower.contains("教我") || lower.contains("问题") || lower.contains("不懂")) {
            return "tutor";
        } else if (lower.contains("报告") || lower.contains("总结") || lower.contains("分析") || lower.contains("导出")) {
            return "reporter";
        } else if (lower.contains("练习") || lower.contains("习题") || lower.contains("题目") || lower.contains("作业") || lower.contains("批改")) {
            return "exercise";
        } else if (lower.contains("搜索") || lower.contains("查找") || lower.contains("资源") || lower.contains("教程")) {
            return "search";
        } else if (lower.contains("知识") || lower.contains("文档") || lower.contains("检索") || lower.contains("知识库")) {
            return "knowledge";
        }
        return "planner";
    }
}
