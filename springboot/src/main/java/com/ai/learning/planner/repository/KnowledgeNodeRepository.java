package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.KnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识节点仓储
 * 知识图谱中的节点（前置知识 JSON 中的关联节点），支持按名称模糊搜索与分类查询
 */
@Repository
public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, String> {
    List<KnowledgeNode> findByNameContaining(String name);
    List<KnowledgeNode> findByCategory(String category);
}
