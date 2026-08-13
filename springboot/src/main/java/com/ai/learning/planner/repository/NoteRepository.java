package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学习笔记仓储
 * 笔记按 userId 归属；支持按标签查询、批量查询与按用户删除
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Note> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.id IN :ids")
    List<Note> findByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.tags LIKE %:tag%")
    List<Note> findByUserIdAndTagsContaining(@Param("userId") Long userId, @Param("tag") String tag);

    void deleteByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}