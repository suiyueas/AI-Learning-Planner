package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.assessment.HistoryDetailDTO;
import com.ai.learning.planner.dto.assessment.HistoryRecordDTO;
import com.ai.learning.planner.entity.AssessmentRecord;
import com.ai.learning.planner.exception.BusinessException;
import com.ai.learning.planner.repository.AssessmentRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测评记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRecordRepository assessmentRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取历史测评记录（分页）
     */
    @Transactional(readOnly = true)
    public Page<HistoryRecordDTO> getHistory(Long userId, Pageable pageable, String subject) {
        Page<AssessmentRecord> records;
        if (subject != null && !subject.isBlank()) {
            records = assessmentRecordRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject, pageable);
        } else {
            records = assessmentRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return records.map(this::toRecordDTO);
    }

    /**
     * 获取历史测评详情
     */
    @Transactional(readOnly = true)
    public HistoryDetailDTO getHistoryDetail(Long id, Long userId) {
        AssessmentRecord record = assessmentRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        return toDetailDTO(record);
    }

    /**
     * 获取用户历史测评科目列表
     */
    @Transactional(readOnly = true)
    public List<String> getHistorySubjects(Long userId) {
        return assessmentRecordRepository.findDistinctSubjectsByUserId(userId);
    }

    /**
     * 保存测评记录
     */
    @Transactional
    public AssessmentRecord saveRecord(Long userId, String subject, String difficulty,
                                        int score, int total, Object details) {
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            AssessmentRecord record = AssessmentRecord.builder()
                    .userId(userId)
                    .subject(subject)
                    .difficulty(difficulty)
                    .score(score)
                    .total(total)
                    .details(detailsJson)
                    .build();
            return assessmentRecordRepository.save(record);
        } catch (Exception e) {
            log.error("保存测评记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存测评记录失败", e);
        }
    }

    /**
     * 删除历史测评记录（校验归属）
     */
    @Transactional
    public void deleteHistory(Long id, Long userId) {
        AssessmentRecord record = assessmentRecordRepository.findByIdAndUserId(id, userId)
                .orElseGet(() -> {
                    log.warn("删除测评记录失败：记录不存在或无权删除: id={}, userId={}", id, userId);
                    throw new BusinessException("记录不存在或无权删除");
                });
        assessmentRecordRepository.delete(record);
        log.info("删除测评记录成功: id={}, userId={}", id, userId);
    }

    private HistoryRecordDTO toRecordDTO(AssessmentRecord record) {
        return HistoryRecordDTO.builder()
                .id(record.getId())
                .subject(record.getSubject())
                .difficulty(record.getDifficulty())
                .score(record.getScore())
                .total(record.getTotal())
                .createdAt(record.getCreatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private HistoryDetailDTO toDetailDTO(AssessmentRecord record) {
        List<HistoryDetailDTO.QuestionDetail> questionDetails = new ArrayList<>();
        try {
            if (record.getDetails() != null && !record.getDetails().isBlank()) {
                List<Map<String, Object>> details = objectMapper.readValue(
                        record.getDetails(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                for (Map<String, Object> d : details) {
                    questionDetails.add(HistoryDetailDTO.QuestionDetail.builder()
                            .questionId(d.get("questionId") != null ? ((Number) d.get("questionId")).longValue() : null)
                            .questionText((String) d.get("questionText"))
                            .options(d.get("options") != null ? (List<String>) d.get("options") : new ArrayList<>())
                            .correctAnswer(d.get("correctAnswer") != null ? ((Number) d.get("correctAnswer")).intValue() : null)
                            .userAnswer(d.get("userAnswer") != null ? ((Number) d.get("userAnswer")).intValue() : null)
                            .correct(d.get("correct") != null && (Boolean) d.get("correct"))
                            .explanation((String) d.get("explanation"))
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("解析测评详情失败: {}", e.getMessage());
        }

        int accuracy = record.getTotal() > 0 ? (int) Math.round(record.getScore() * 100.0 / record.getTotal()) : 0;
        String level = getLevel(accuracy);

        return HistoryDetailDTO.builder()
                .id(record.getId())
                .subject(record.getSubject())
                .difficulty(record.getDifficulty())
                .score(record.getScore())
                .total(record.getTotal())
                .accuracy(accuracy)
                .level(level)
                .createdAt(record.getCreatedAt())
                .details(questionDetails)
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
}