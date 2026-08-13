package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学习资源仓储
 * 与学习路径节点关联的推荐资源（视频/文章等），支持按节点与类型查询
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, String> {
    List<Resource> findByNodeId(String nodeId);
    List<Resource> findByType(String type);
}
