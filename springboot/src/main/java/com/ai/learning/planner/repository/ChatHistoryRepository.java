package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天历史仓储
 * 会话记录按 userId 归属存储；游客会话（userId 为 null）与登录用户会话隔离查询，防止越权读取
 */
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, String> {
    List<ChatHistory> findBySessionIdAndUserId(String sessionId, String userId);
    List<ChatHistory> findBySessionIdAndUserIdOrderByCreatedAtAsc(String sessionId, String userId);
    List<ChatHistory> findBySessionIdAndUserIdIsNullOrderByCreatedAtAsc(String sessionId);
    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 获取用户的会话摘要列表（按更新时间倒序）
     * 只返回 sessionId、消息数量、最早和最新时间，不加载消息内容
     */
    @Query("SELECT h.sessionId, COUNT(h), MIN(h.createdAt), MAX(h.createdAt) " +
           "FROM ChatHistory h WHERE h.userId = :userId " +
           "GROUP BY h.sessionId ORDER BY MAX(h.createdAt) DESC")
    List<Object[]> findConversationSummaries(@Param("userId") String userId);

    /**
     * 获取指定会话的第一条用户消息（作为标题）
     */
    @Query("SELECT h.content FROM ChatHistory h WHERE h.sessionId = :sessionId AND h.role = 'user' ORDER BY h.createdAt ASC LIMIT 1")
    String findFirstUserMessage(@Param("sessionId") String sessionId);

    /**
     * 获取指定会话的最后一条消息（作为预览）
     */
    @Query("SELECT h.content FROM ChatHistory h WHERE h.sessionId = :sessionId ORDER BY h.createdAt DESC LIMIT 1")
    String findLastMessage(@Param("sessionId") String sessionId);

    /**
     * 统计用户会话数量
     */
    @Query("SELECT COUNT(DISTINCT h.sessionId) FROM ChatHistory h WHERE h.userId = :userId")
    long countConversationsByUserId(@Param("userId") String userId);
}