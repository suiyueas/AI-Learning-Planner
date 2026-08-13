package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 题库实体（对应 question_bank 表）
 */
@Entity
@Table(name = "question_bank", indexes = {
    @Index(name = "idx_qb_subject", columnList = "subject"),
    @Index(name = "idx_qb_subject_difficulty", columnList = "subject, difficulty")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 科目：python / java / cpp / algorithm / database / network / system_design */
    @Column(nullable = false, length = 50)
    private String subject;

    /** 题目文本 */
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** 选项（JSON 数组） */
    @Column(nullable = false, columnDefinition = "JSON")
    private String options;

    /** 正确答案（选项索引 A=0, B=1, C=2, D=3） */
    @Column(name = "correct_answer", nullable = false, length = 255)
    private String correctAnswer;

    /** 难度：easy / medium / hard */
    @Builder.Default
    @Column(length = 20)
    private String difficulty = "medium";

    /** 解析 */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}