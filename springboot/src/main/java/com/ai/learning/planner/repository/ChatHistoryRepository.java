package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天历史仓储
 * 会话记录按 userId 归属存储；游客会话（userId 为 null）与登录用户会话隔离查询，防止越权读取
 */
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, String> {
    List<ChatHistory> findBySessionIdAndUserIdOrderByCreatedAtAsc(String sessionId, String userId);
    List<ChatHistory> findBySessionIdAndUserIdIsNullOrderByCreatedAtAsc(String sessionId);
    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(String userId);
}
