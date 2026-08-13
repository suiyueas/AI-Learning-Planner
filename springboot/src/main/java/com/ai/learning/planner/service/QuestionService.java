package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.assessment.QuestionDTO;
import com.ai.learning.planner.dto.assessment.ResultDTO;
import com.ai.learning.planner.dto.assessment.SubmitRequest;
import com.ai.learning.planner.entity.AssessmentRecord;
import com.ai.learning.planner.entity.Question;
import com.ai.learning.planner.repository.AssessmentRecordRepository;
import com.ai.learning.planner.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目服务
 * 提供题目获取、生成、提交答案和批改功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final DeepSeekAssessmentService deepSeekService;
    private final AssessmentService assessmentService;
    private final AssessmentRecordRepository assessmentRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取题目：优先从数据库读取，不足则调用 DeepSeek 生成
     */
    @Transactional
    public List<QuestionDTO> getQuestions(String subject, int count, String difficulty, Long userId) {
        // 1. 从数据库查询该科目已有题目（按难度筛选）
        List<Question> existing = questionRepository.findBySubjectAndDifficulty(subject, difficulty);
        if (existing.size() >= count) {
            log.info("题库已有 {} 道 {} 题目(难度{})，直接抽 {} 道", existing.size(), subject, difficulty, count);
            Collections.shuffle(existing);
            return toDTOList(existing.subList(0, count));
        }

        // 2. 不足则调用 DeepSeek 生成
        log.info("题库 {} 科目(难度{})只有 {} 道，调用 DeepSeek 生成 {} 道", subject, difficulty, existing.size(), count);
        List<Question> generated = deepSeekService.generateQuestions(subject, count, difficulty);
        questionRepository.saveAll(generated);

        // 返回新生成的题目
        return toDTOList(generated);
    }

    /**
     * 重新生成（覆盖旧题）
     */
    @Transactional
    public List<QuestionDTO> regenerateQuestions(String subject, int count, String difficulty, Long userId) {
        List<Question> generated = deepSeekService.regenerateQuestions(subject, count, difficulty, questionRepository);
        return toDTOList(generated);
    }

    /**
     * 提交答案并批改
     */
    @Transactional
    public ResultDTO submitAnswers(SubmitRequest request) {
        List<Long> questionIds = request.getQuestionIds() != null
                ? request.getQuestionIds()
                : new ArrayList<>(request.getAnswers().keySet());
        List<Question> questions = questionRepository.findAllById(questionIds);

        int correctCount = 0;
        List<ResultDTO.QuestionResult> details = new ArrayList<>();

        for (Question q : questions) {
            int correctAnswer = Integer.parseInt(q.getCorrectAnswer());
            int userAnswer = request.getAnswers().getOrDefault(q.getId(), -1);
            boolean correct = userAnswer == correctAnswer;

            if (correct) correctCount++;

            List<String> options;
            try {
                options = objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                options = List.of();
            }

            details.add(ResultDTO.QuestionResult.builder()
                    .questionId(q.getId())
                    .questionText(q.getQuestionText())
                    .options(options)
                    .correctAnswer(correctAnswer)
                    .userAnswer(userAnswer)
                    .correct(correct)
                    .explanation(q.getExplanation())
                    .build());
        }

        int total = questions.size();
        int accuracy = total > 0 ? Math.round((float) correctCount / total * 100) : 0;
        int totalScore = accuracy;
        String level = getLevel(accuracy);

        // 构建保存详情（包含完整题目信息用于历史记录展示）
        List<Map<String, Object>> recordDetails = details.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", d.getQuestionId());
            m.put("questionText", d.getQuestionText());
            m.put("options", d.getOptions());
            m.put("correctAnswer", d.getCorrectAnswer());
            m.put("userAnswer", d.getUserAnswer());
            m.put("correct", d.isCorrect());
            m.put("explanation", d.getExplanation());
            return m;
        }).collect(Collectors.toList());

        // 保存答题记录
        try {
            assessmentService.saveRecord(
                    request.getUserId(),
                    request.getSubject(),
                    request.getDifficulty() != null ? request.getDifficulty() : "medium",
                    correctCount,
                    total,
                    recordDetails
            );
        } catch (Exception e) {
            log.warn("保存答题记录失败: {}", e.getMessage());
        }

        return ResultDTO.builder()
                .totalScore(totalScore)
                .correctCount(correctCount)
                .wrongCount(total - correctCount)
                .accuracy(accuracy)
                .level(level)
                .details(details)
                .build();
    }

    private String getLevel(int accuracy) {
        if (accuracy >= 90) return "优秀";
        if (accuracy >= 80) return "良好";
        if (accuracy >= 70) return "中等偏上";
        if (accuracy >= 60) return "中等";
        if (accuracy >= 50) return "中等偏下";
        return "需加强";
    }

    private List<QuestionDTO> toDTOList(List<Question> questions) {
        return questions.stream().map(q -> {
            List<String> options;
            try {
                options = objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                options = List.of();
            }
            return QuestionDTO.builder()
                    .id(q.getId())
                    .subject(q.getSubject())
                    .questionText(q.getQuestionText())
                    .options(options)
                    .difficulty(q.getDifficulty())
                    .build();
        }).collect(Collectors.toList());
    }

    public String calculateAdaptiveDifficulty(Long userId, String subject) {
        if (userId == null) {
            return "medium";
        }

        List<AssessmentRecord> records = assessmentRecordRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject);

        if (records == null || records.isEmpty()) {
            log.info("[adaptive] 用户 {} 科目 {} 无答题记录，返回中等难度", userId, subject);
            return "medium";
        }

        int totalCorrect = 0;
        int totalQuestions = 0;

        for (AssessmentRecord record : records) {
            if (record.getScore() != null && record.getTotal() != null && record.getTotal() > 0) {
                totalCorrect += record.getScore();
                totalQuestions += record.getTotal();
            }
        }

        if (totalQuestions == 0) {
            return "medium";
        }

        int accuracy = Math.round((float) totalCorrect / totalQuestions * 100);
        log.info("[adaptive] 用户 {} 科目 {} 历史正确率: {}/{} = {}%", userId, subject, totalCorrect, totalQuestions, accuracy);

        if (accuracy >= 80) {
            return "hard";
        } else if (accuracy >= 60) {
            return "medium";
        } else {
            return "easy";
        }
    }

    public int getRecommendedQuestionCount(Long userId, String subject) {
        if (userId == null) {
            return 5;
        }

        List<AssessmentRecord> records = assessmentRecordRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject);
        if (records == null || records.isEmpty()) {
            return 5;
        }

        int recentRecords = Math.min(records.size(), 5);
        int totalCorrect = 0;
        int totalQuestions = 0;

        for (int i = 0; i < recentRecords; i++) {
            AssessmentRecord record = records.get(i);
            if (record.getScore() != null && record.getTotal() != null && record.getTotal() > 0) {
                totalCorrect += record.getScore();
                totalQuestions += record.getTotal();
            }
        }

        if (totalQuestions == 0) {
            return 5;
        }

        int accuracy = Math.round((float) totalCorrect / totalQuestions * 100);

        if (accuracy >= 80) {
            return 8;
        } else if (accuracy >= 60) {
            return 5;
        } else {
            return 3;
        }
    }

    public Map<String, Object> getAdaptiveQuizConfig(Long userId, String subject) {
        String difficulty = calculateAdaptiveDifficulty(userId, subject);
        int recommendedCount = getRecommendedQuestionCount(userId, subject);

        Map<String, Object> config = new HashMap<>();
        config.put("difficulty", difficulty);
        config.put("recommendedCount", recommendedCount);
        config.put("estimatedTime", recommendedCount * 3);

        List<AssessmentRecord> records = assessmentRecordRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject);
        if (records != null && !records.isEmpty()) {
            int totalCorrect = 0;
            int totalQuestions = 0;
            for (AssessmentRecord record : records) {
                if (record.getScore() != null && record.getTotal() != null && record.getTotal() > 0) {
                    totalCorrect += record.getScore();
                    totalQuestions += record.getTotal();
                }
            }
            if (totalQuestions > 0) {
                config.put("historicalAccuracy", Math.round((float) totalCorrect / totalQuestions * 100));
            }
            config.put("totalAttempts", records.size());
        }

        String level;
        int accuracy = (int) config.getOrDefault("historicalAccuracy", 65);
        if (accuracy >= 80) {
            level = "优秀";
        } else if (accuracy >= 70) {
            level = "良好";
        } else if (accuracy >= 60) {
            level = "中等偏上";
        } else if (accuracy >= 50) {
            level = "中等";
        } else {
            level = "需加强";
        }
        config.put("level", level);

        return config;
    }
}