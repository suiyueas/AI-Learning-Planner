package com.ai.learning.planner.service;

import com.ai.learning.planner.entity.Resource;
import com.ai.learning.planner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 资源服务
 * 提供学习资源的查询和管理功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public List<Resource> getResourcesByNodeId(String nodeId) {
        return resourceRepository.findByNodeId(nodeId);
    }

    public List<Resource> getResourcesByType(String type) {
        return resourceRepository.findByType(type);
    }

    public Resource saveResource(Resource resource) {
        return resourceRepository.save(resource);
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }
}