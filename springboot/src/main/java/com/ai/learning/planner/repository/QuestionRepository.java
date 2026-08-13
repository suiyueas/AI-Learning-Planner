package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 测评题目仓储
 * 存储 AI 生成/缓存的题目，支持按科目、难度查询与按科目删除（重新生成时清理）
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySubject(String subject);
    List<Question> findBySubjectAndDifficulty(String subject, String difficulty);
    long countBySubject(String subject);
    void deleteBySubject(String subject);
    void deleteBySubjectAndDifficulty(String subject, String difficulty);
}