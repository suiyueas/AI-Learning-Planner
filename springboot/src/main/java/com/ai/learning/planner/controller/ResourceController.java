package com.ai.learning.planner.controller;

import com.ai.learning.planner.entity.Resource;
import com.ai.learning.planner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源控制器
 * 提供学习资源的查询和热门资源推荐等功能
 */
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceController {

    private final ResourceRepository resourceRepository;

    /**
     * 获取资源列表
     * @return 资源列表
     */
    @GetMapping
    public Map<String, Object> getResources() {
        log.info("获取资源列表");
        List<Resource> resources = resourceRepository.findAll();
        return Map.of("success", true, "data", resources);
    }

    /**
     * 获取热门资源
     * @return 热门资源列表
     */
    @GetMapping("/hot")
    public Map<String, Object> getHotResources() {
        log.info("获取热门资源列表");
        List<Resource> resources = resourceRepository.findAll();
        // 按评分排序并取前10个
        List<Resource> hotResources = resources.stream()
                .sorted((a, b) -> Float.compare(
                        b.getAvgRating() != null ? b.getAvgRating() : 0f,
                        a.getAvgRating() != null ? a.getAvgRating() : 0f))
                .limit(10)
                .toList();
        return Map.of("success", true, "data", hotResources);
    }

    /**
     * 获取资源详情
     * @param id 资源ID
     * @return 资源详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getResource(@PathVariable String id) {
        log.info("获取资源详情: {}", id);
        return resourceRepository.findById(id)
                .map(resource -> Map.<String, Object>of("success", true, "data", resource))
                .orElse(Map.of("success", false, "message", "资源不存在"));
    }

    /**
     * 根据节点ID获取资源
     * @param nodeId 节点ID
     * @return 资源列表
     */
    @GetMapping("/node/{nodeId}")
    public Map<String, Object> getResourcesByNode(@PathVariable String nodeId) {
        log.info("获取节点资源: {}", nodeId);
        List<Resource> resources = resourceRepository.findByNodeId(nodeId);
        return Map.of("success", true, "data", resources);
    }
}