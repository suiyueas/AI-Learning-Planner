package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.dto.assessment.*;
import com.ai.learning.planner.service.AssessmentService;
import com.ai.learning.planner.service.QuestionService;
import com.ai.learning.planner.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 测评控制器
 * 提供题目获取、生成、答案提交和历史记录查询等功能
 */
@Slf4j
@RestController
@RequestMapping("/assessment")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final AssessmentService assessmentService;

    /**
     * 获取题目：优先从数据库读取，不足则调用 DeepSeek 生成
     * GET /api/assessment/questions?subject=Python&count=10&difficulty=medium
     */
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getQuestions(
            @RequestParam String subject,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "medium") String difficulty,
            Authentication authentication) {
        log.info("获取测评题目: subject={}, count={}, difficulty={}", subject, count, difficulty);

        Long userId = SecurityUtils.requireLongUserId(authentication);
        // 题目数量上限 50，防止巨额 AI 生成费用
        int safeCount = Math.min(Math.max(count, 1), 50);
        List<QuestionDTO> questions = questionService.getQuestions(subject, safeCount, difficulty, userId);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    /**
     * 重新生成题目
     * POST /api/assessment/regenerate?subject=Python&count=10&difficulty=medium
     */
    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> regenerateQuestions(
            @RequestParam String subject,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "medium") String difficulty,
            Authentication authentication) {
        log.info("重新生成测评题目: subject={}, count={}, difficulty={}", subject, count, difficulty);

        Long userId = SecurityUtils.requireLongUserId(authentication);
        List<QuestionDTO> questions = questionService.regenerateQuestions(subject, count, difficulty, userId);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    /**
     * 提交答案并批改
     * POST /api/assessment/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ResultDTO>> submitAnswers(
            @Valid @RequestBody SubmitRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.requireLongUserId(authentication);
        request.setUserId(userId);
        log.info("提交答案: subject={}, difficulty={}, answers={}", request.getSubject(), request.getDifficulty(), request.getAnswers().size());

        ResultDTO result = questionService.submitAnswers(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取支持的科目列表
     */
    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getSubjects() {
        List<Map<String, String>> subjects = List.of(
            Map.of("value", "python", "label", "Python"),
            Map.of("value", "java", "label", "Java"),
            Map.of("value", "cpp", "label", "C++"),
            Map.of("value", "javascript", "label", "JavaScript"),
            Map.of("value", "algorithm", "label", "数据结构与算法"),
            Map.of("value", "database", "label", "数据库"),
            Map.of("value", "network", "label", "网络基础"),
            Map.of("value", "ml", "label", "机器学习"),
            Map.of("value", "frontend", "label", "前端开发"),
            Map.of("value", "system_design", "label", "系统设计")
        );
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    /**
     * 获取历史测评记录（分页）
     * GET /api/assessment/history?page=1&size=10&subject=Python
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResult<HistoryRecordDTO>>> getHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String subject) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : 0L;
        log.info("获取测评历史: userId={}, page={}, size={}, subject={}", userId, page, size, subject);

        try {
            // 参数边界：page >= 1，size 限制在 1~50，防止异常或超大数据量查询
            int safePage = Math.max(page, 1);
            int safeSize = Math.min(Math.max(size, 1), 50);
            PageRequest pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<HistoryRecordDTO> records = assessmentService.getHistory(userId, pageable, subject);
            return ResponseEntity.ok(ApiResponse.success(PageResult.from(records)));
        } catch (Exception e) {
            log.error("获取测评历史失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取历史记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取历史测评详情
     * GET /api/assessment/history/{id}
     */
    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<HistoryDetailDTO>> getHistoryDetail(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : 0L;
        log.info("获取测评详情: id={}, userId={}", id, userId);

        try {
            HistoryDetailDTO detail = assessmentService.getHistoryDetail(id, userId);
            return ResponseEntity.ok(ApiResponse.success(detail));
        } catch (Exception e) {
            log.error("获取测评详情失败: id={}, userId={}, error={}", id, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取详情失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户历史测评科目列表
     * GET /api/assessment/history/subjects
     */
    @GetMapping("/history/subjects")
    public ResponseEntity<ApiResponse<List<String>>> getHistorySubjects(Authentication authentication) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : 0L;
        log.info("获取历史科目列表: userId={}", userId);

        try {
            List<String> subjects = assessmentService.getHistorySubjects(userId);
            return ResponseEntity.ok(ApiResponse.success(subjects));
        } catch (Exception e) {
            log.error("获取历史科目列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取科目列表失败: " + e.getMessage()));
        }
    }

    /**
     * 删除历史测评记录
     * DELETE /api/assessment/history/{id}
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : 0L;
        log.info("删除测评记录: id={}, userId={}", id, userId);

        try {
            assessmentService.deleteHistory(id, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("删除测评记录失败: id={}, userId={}, error={}", id, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "删除记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取自适应难度配置
     * GET /api/assessment/quiz/adaptive-config?subject=Python
     */
    @GetMapping("/quiz/adaptive-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdaptiveConfig(
            Authentication authentication,
            @RequestParam String subject) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : 0L;
        log.info("获取自适应难度配置: userId={}, subject={}", userId, subject);

        try {
            Map<String, Object> config = questionService.getAdaptiveQuizConfig(userId, subject);
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取自适应配置失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取配置失败: " + e.getMessage()));
        }
    }

    /**
     * 生成自适应难度的题目
     * GET /api/assessment/quiz/generate?subject=Python&adaptive=true&count=5
     */
    @GetMapping("/quiz/generate")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> generateAdaptiveQuiz(
            Authentication authentication,
            @RequestParam String subject,
            @RequestParam(defaultValue = "true") boolean adaptive,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "5") int count) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : 0L;
        log.info("生成自适应题目: userId={}, subject={}, adaptive={}, difficulty={}, count={}",
                userId, subject, adaptive, difficulty, count);
        try {
            String actualDifficulty;
            int actualCount;
            // 题目数量上限 50，防止巨额 AI 生成费用
            int safeCount = Math.min(Math.max(count, 1), 50);

            if (adaptive) {
                Map<String, Object> config = questionService.getAdaptiveQuizConfig(userId, subject);
                actualDifficulty = difficulty != null ? difficulty : (String) config.get("difficulty");
                actualCount = safeCount;
                log.info("自适应难度计算: difficulty={}, count={}", actualDifficulty, actualCount);
            } else {
                actualDifficulty = difficulty != null ? difficulty : "medium";
                actualCount = safeCount;
            }

            List<QuestionDTO> questions = questionService.getQuestions(subject, actualCount, actualDifficulty, userId);
            return ResponseEntity.ok(ApiResponse.success(questions));
        } catch (Exception e) {
            log.error("生成题目失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "生成题目失败: " + e.getMessage()));
        }
    }
}