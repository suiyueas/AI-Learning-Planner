package com.ai.learning.planner.mcp.ai;

import com.ai.learning.planner.service.ModelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测验题生成工具（AI 赋能）
 * 支持选择题/判断题/填空题/混合题型，使用 Few-shot 提示词保证输出质量，
 * 每道题自动补全答案与解析，用于自我评估。
 */
@Component
@Slf4j
public class QuizGenerationTool extends AbstractAiTool {

    private static final int MAX_CONTENT_LENGTH = 6000;

    public QuizGenerationTool(ModelManager modelManager, ObjectMapper objectMapper, McpAiProperties properties) {
        super(modelManager, objectMapper, properties);
    }

    @Override
    protected String toolId() {
        return "generate_quiz";
    }

    public Map<String, Object> execute(Map<String, Object> params, AiToolContext ctx) {
        String topic = paramString(params, "topic", "");
        String questionType = normalizeType(paramString(params, "questionType", "mixed"));
        String difficulty = paramString(params, "difficulty", "中等");
        int count = paramInt(params, "count", 5, 1, 10);

        if (topic.isBlank()) {
            return error("学习内容不能为空");
        }

        // 1. 调用大模型生成测验题（Few-shot）
        String prompt = buildPrompt(topic, questionType, difficulty, count);
        List<Map<String, Object>> questions = new ArrayList<>();

        callLlm(prompt).ifPresent(raw -> {
            List<Map<String, Object>> parsed = parseJsonArray(raw);
            // 过滤缺题干或答案的无效题目
            for (Map<String, Object> q : parsed) {
                String question = q.get("question") == null ? "" : q.get("question").toString().trim();
                String answer = q.get("answer") == null ? "" : q.get("answer").toString().trim();
                if (question.isBlank() || answer.isBlank()) continue;
                q.put("type", q.getOrDefault("type", questionType));
                questions.add(q);
            }
        });

        // 2. LLM 失败或产出不足时降级
        boolean fallback = questions.isEmpty();
        if (fallback) {
            log.warn("[generate_quiz] LLM 生成为空，使用降级方案");
            markFallback();
            questions.addAll(fallbackQuiz(topic, questionType, difficulty, count));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("questions", questions);
        result.put("questionType", questionType);
        result.put("difficulty", difficulty);
        result.put("count", questions.size());
        result.put("totalRequested", count);
        result.put("suggestedNextTools", List.of("extract_keywords", "translate_text"));
        result.put("message", "测验题生成成功");
        if (fallback) {
            result.put("fallback", true);
        }
        return result;
    }

    /** 题型归一化：兼容 choice/judgment/fill/mixed 与中文 */
    private String normalizeType(String type) {
        return switch (type) {
            case "选择题", "choice" -> "choice";
            case "判断题", "judgment" -> "judgment";
            case "填空题", "fill" -> "fill";
            default -> "mixed";
        };
    }

    private String buildPrompt(String topic, String questionType, String difficulty, int count) {
        String typeDesc = switch (questionType) {
            case "choice" -> "全部为单项选择题（4个选项）";
            case "judgment" -> "全部为判断题（对/错）";
            case "fill" -> "全部为填空题";
            default -> "混合题型（选择题/判断题/填空题穿插）";
        };
        return """
                你是学习评估专家。请基于以下学习内容生成 %d 道%s难度的测验题，题型要求：%s。
                每道题必须包含：
                - question：题干
                - options：选项数组（选择题4个选项；判断题固定为["对","错"]；填空题传空数组）
                - answer：正确答案（选择题为选项文本；判断题"对"或"错"；填空题为应填内容）
                - explanation：答案解析（说明为什么正确，30字左右）
                - type：题型标识（choice/judgment/fill）

                输出示例（选择题）：
                [{"question": "Java中用于定义常量的关键字是？", "options": ["static", "final", "const", "readonly"], "answer": "final", "explanation": "final修饰的变量只能赋值一次，是Java定义常量的标准方式。", "type": "choice"}]

                只返回 JSON 数组（不要任何其他文字），题目难度要循序渐进。

                学习内容：
                %s
                """.formatted(count, difficulty, typeDesc, truncate(topic, MAX_CONTENT_LENGTH));
    }

    /** 降级方案：生成带答案与解析的模板题 */
    private List<Map<String, Object>> fallbackQuiz(String topic, String questionType, String difficulty, int count) {
        List<Map<String, Object>> questions = new ArrayList<>();
        int num = Math.min(count, 3);
        for (int i = 1; i <= num; i++) {
            Map<String, Object> q = new HashMap<>();
            switch (questionType) {
                case "choice" -> {
                    q.put("question", "关于「" + topic + "」的核心概念，以下哪个描述最准确？（第" + i + "题）");
                    q.put("options", List.of("正确的概念描述", "明显错误的描述", "部分正确但不完整", "与主题无关的描述"));
                    q.put("answer", "正确的概念描述");
                    q.put("type", "choice");
                }
                case "judgment" -> {
                    q.put("question", "「" + topic + "」是学习该领域必须掌握的基础知识。");
                    q.put("options", List.of("对", "错"));
                    q.put("answer", "对");
                    q.put("type", "judgment");
                }
                case "fill" -> {
                    q.put("question", "「" + topic + "」的核心要点是______。");
                    q.put("options", List.of());
                    q.put("answer", "理解并掌握其基本概念与应用场景");
                    q.put("type", "fill");
                }
                default -> {
                    String[][] templates = {
                            {"关于「" + topic + "」以下哪个说法正确？", "核心概念描述", "type-choice"},
                            {"「" + topic + "」是相关领域的重要知识点。", "对", "type-judgment"},
                            {"「" + topic + "」的关键在于______。", "理解概念并灵活应用", "type-fill"}
                    };
                    String[] t = templates[i - 1];
                    q.put("question", t[0]);
                    if (t[2].equals("type-choice")) {
                        q.put("options", List.of("核心概念描述", "错误描述", "不完整描述", "无关描述"));
                        q.put("answer", "核心概念描述");
                        q.put("type", "choice");
                    } else if (t[2].equals("type-judgment")) {
                        q.put("options", List.of("对", "错"));
                        q.put("answer", "对");
                        q.put("type", "judgment");
                    } else {
                        q.put("options", List.of());
                        q.put("answer", "理解概念并灵活应用");
                        q.put("type", "fill");
                    }
                }
            }
            q.put("explanation", "根据「" + topic + "」（" + difficulty + "难度）的核心知识点，本题考察对关键概念的理解与掌握。");
            q.put("difficulty", difficulty);
            questions.add(q);
        }
        return questions;
    }

    private Map<String, Object> error(String message) {
        log.warn("[generate_quiz] {}", message);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
