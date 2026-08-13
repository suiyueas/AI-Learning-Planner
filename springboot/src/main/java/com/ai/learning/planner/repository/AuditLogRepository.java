package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 安全审计日志仓储
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
}
