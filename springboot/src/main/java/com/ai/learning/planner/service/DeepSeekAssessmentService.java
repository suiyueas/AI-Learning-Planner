package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.Question;
import com.ai.learning.planner.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 调用 DeepSeek API 生成测评题目
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekAssessmentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.deepseek.api-key}")
    private String apiKey;

    @Value("${spring.ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    private static final Map<String, String> SUBJECT_NAMES = Map.ofEntries(
        Map.entry("python", "Python 编程"),
        Map.entry("java", "Java 编程"),
        Map.entry("cpp", "C++ 编程"),
        Map.entry("algorithm", "数据结构与算法"),
        Map.entry("database", "数据库"),
        Map.entry("network", "网络基础"),
        Map.entry("system_design", "系统设计")
    );

    /**
     * 调用 DeepSeek 生成指定科目的题目
     */
    public List<Question> generateQuestions(String subject, int count, String difficulty) {
        String subjectName = SUBJECT_NAMES.getOrDefault(subject, subject);
        String difficultyLabel = getDifficultyLabel(difficulty);
        String prompt = String.format(
            "你是一名专业的%s技术面试官。请生成%d道%s单选题，难度为%s。\n\n" +
            "题目要求：\n" +
            "- 难度等级：%s\n" +
            "- 简单：基础语法和概念题\n" +
            "- 中等：综合运用题\n" +
            "- 困难：进阶和复杂场景题\n\n" +
            "以严格的JSON数组格式返回，不要包含任何其他文字，格式如下：\n" +
            "[{\n" +
            "  \"question\": \"题目标题\",\n" +
            "  \"options\": [\"A选项\", \"B选项\", \"C选项\", \"D选项\"],\n" +
            "  \"correctAnswer\": 0,\n" +
            "  \"explanation\": \"解析内容\"\n" +
            "}]\n\n" +
            "注意：correctAnswer为0表示A选项正确，1表示B，2表示C，3表示D。必须包含4个选项。",
            subjectName, count, subjectName, difficultyLabel, difficultyLabel
        );

        String response = callDeepSeek(prompt);
        return parseQuestions(response, subject, difficulty);
    }

    private String getDifficultyLabel(String difficulty) {
        return switch (difficulty) {
            case "easy" -> "基础入门级";
            case "hard" -> "高级进阶";
            default -> "中等水平";
        };
    }

    /**
     * 重新生成题目（先删除旧题再生成）
     */
    public List<Question> regenerateQuestions(String subject, int count, String difficulty, QuestionRepository questionRepository) {
        questionRepository.deleteBySubjectAndDifficulty(subject, difficulty);
        List<Question> questions = generateQuestions(subject, count, difficulty);
        return questionRepository.saveAll(questions);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String callDeepSeek(String prompt) {
        try {
            String url = baseUrl + "/chat/completions";

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-v4-flash");
            requestBody.put("messages", List.of(message));
            requestBody.put("temperature", 0.8);
            requestBody.put("max_tokens", 4096);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> responseMessage = (Map<String, Object>) choice.get("message");
                    if (responseMessage != null) {
                        return (String) responseMessage.get("content");
                    }
                }
            }
            throw new RuntimeException("DeepSeek API 返回异常: " + response.getBody());
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败", e);
            throw new RuntimeException("调用 AI 出题服务失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Question> parseQuestions(String responseBody, String subject, String difficulty) {
        try {
            // 清理可能的 markdown 代码块标记
            String json = responseBody.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            } else if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            List<Map<String, Object>> rawQuestions = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            List<Question> questions = new ArrayList<>();

            for (Map<String, Object> raw : rawQuestions) {
                String questionText = (String) raw.get("question");
                List<String> options = (List<String>) raw.get("options");
                Object answerObj = raw.get("correctAnswer");
                String explanation = (String) raw.get("explanation");

                if (questionText == null || options == null || answerObj == null) {
                    log.warn("跳过格式不完整的问题: {}", raw);
                    continue;
                }

                int correctAnswer;
                if (answerObj instanceof Number) {
                    correctAnswer = ((Number) answerObj).intValue();
                } else {
                    correctAnswer = Integer.parseInt(answerObj.toString());
                }

                if (options.size() != 4) {
                    log.warn("选项数量不是4个，跳过: {}", questionText);
                    continue;
                }

                Question question = Question.builder()
                        .subject(subject)
                        .questionText(questionText)
                        .options(objectMapper.writeValueAsString(options))
                        .correctAnswer(String.valueOf(correctAnswer))
                        .difficulty(difficulty)
                        .explanation(explanation)
                        .createdAt(LocalDateTime.now())
                        .build();
                questions.add(question);
            }

            log.info("成功解析 {} 道题目 (科目: {}, 难度: {})", questions.size(), subject, difficulty);
            return questions;
        } catch (Exception e) {
            log.error("解析AI返回题目失败: response={}", responseBody, e);
            throw new RuntimeException("解析题目数据失败", e);
        }
    }
}