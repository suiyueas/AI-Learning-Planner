package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.GeneratePathRequest;
import com.ai.learning.planner.dto.PathRequest;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.entity.LearningRecord;
import com.ai.learning.planner.exception.BusinessException;
import com.ai.learning.planner.repository.LearningPathRepository;
import com.ai.learning.planner.repository.LearningRecordRepository;
import com.ai.learning.planner.utils.PathOutlineGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningPathService {

    /**
     * 学习路径服务
     * 路径的创建/查询/更新/删除/重置/优化；所有写操作与对象级读取均校验路径归属（requireOwnedPath），
     * 管理员可通过 Controller 层豁免读取校验
     */

    private final LearningPathRepository learningPathRepository;
    private final LearningRecordRepository learningRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 校验路径存在且归属当前用户（统一对象级授权入口）
     *
     * @throws BusinessException 路径不存在或非本人路径时抛出
     */
    private LearningPath requireOwnedPath(String pathId, String userId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException("学习路径不存在: " + pathId));
        if (!path.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该学习路径");
        }
        return path;
    }

    /**
     * 创建学习路径（userId 取自认证上下文，忽略请求体中的 userId 字段）
     */
    public LearningPath createPath(PathRequest request, String userId) {
        String durationStr = request.getDurationWeeks() != null ? request.getDurationWeeks() + "周" : "8周";
        LearningPath path = LearningPath.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name("学习路径 - " + request.getGoal())
                .description("目标：" + request.getGoal() + " | 周期：" + durationStr + " | 领域：通用")
                .version(1)
                .isActive(true)
                .completionPercentage(0f)
                .nodes(PathOutlineGenerator.generateOutline(request.getGoal(), null, 8, objectMapper))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return learningPathRepository.save(path);
    }

    public List<LearningPath> getUserPaths(String userId) {
        return learningPathRepository.findByUserId(userId);
    }

    public Optional<LearningPath> getActivePath(String userId) {
        return learningPathRepository.findByUserIdAndIsActive(userId, true);
    }

    public Optional<LearningPath> getPath(String pathId, String userId, boolean isAdmin) {
        Optional<LearningPath> path = learningPathRepository.findById(pathId);
        if (path.isPresent() && !isAdmin && !path.get().getUserId().equals(userId)) {
            throw new BusinessException("无权访问该学习路径");
        }
        return path;
    }

    /**
     * 更新学习路径（先校验归属；保留原 userId 与 createdAt，防止被篡改或覆盖丢失）
     * 乐观锁：客户端提交的版本号须与数据库当前版本一致（CAS 比对），
     * 不一致说明路径已被其他操作并发修改，直接拒绝本次覆盖，防止并发更新互相覆盖丢失。
     */
    public LearningPath updatePath(LearningPath path, String userId) {
        LearningPath existing = requireOwnedPath(path.getId(), userId);
        Integer incomingVersion = path.getVersion();
        Integer currentVersion = existing.getVersion();
        if (incomingVersion != null && currentVersion != null && !incomingVersion.equals(currentVersion)) {
            throw new BusinessException(
                    "学习路径已被其他操作更新（当前版本 v" + currentVersion + "），请刷新后重试");
        }
        path.setUserId(existing.getUserId());
        path.setCreatedAt(existing.getCreatedAt());
        path.setUpdatedAt(LocalDateTime.now());
        path.setVersion(currentVersion != null ? currentVersion + 1 : 1);
        return learningPathRepository.save(path);
    }

    /**
     * 获取路径中的节点列表（从 JSON 字段解析，校验归属）
     */
    public List<Map<String, Object>> getPathNodes(String pathId, String userId, boolean isAdmin) {
        requireOwnedPathForRead(pathId, userId, isAdmin);
        return getPathNodesUnchecked(pathId);
    }

    /**
     * 读取场景的归属校验（管理员可读任意路径）
     */
    private void requireOwnedPathForRead(String pathId, String userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        requireOwnedPath(pathId, userId);
    }

    /**
     * 获取路径中的节点列表（内部无校验版本，调用方必须已校验归属）
     */
    private List<Map<String, Object>> getPathNodesUnchecked(String pathId) {
        return learningPathRepository.findById(pathId)
                .map(path -> {
                    String nodesJson = path.getNodes();
                    if (nodesJson == null || nodesJson.isBlank() || nodesJson.equals("[]")) {
                        return List.<Map<String, Object>>of();
                    }
                    try {
                        return objectMapper.readValue(nodesJson, new TypeReference<List<Map<String, Object>>>() {});
                    } catch (Exception e) {
                        log.error("解析路径节点JSON失败: pathId={}", pathId, e);
                        Map<String, Object> fallbackNode = new HashMap<>();
                        fallbackNode.put("_error", true);
                        fallbackNode.put("message", "路径节点数据解析失败，请联系管理员");
                        fallbackNode.put("pathId", pathId);
                        return List.<Map<String, Object>>of(fallbackNode);
                    }
                })
                .orElseGet(() -> {
                    log.warn("路径不存在: pathId={}", pathId);
                    Map<String, Object> fallbackNode = new HashMap<>();
                    fallbackNode.put("_error", true);
                    fallbackNode.put("message", "学习路径不存在");
                    fallbackNode.put("pathId", pathId);
                    return List.<Map<String, Object>>of(fallbackNode);
                });
    }

    /**
     * 添加节点到路径的 JSON 列表中
     */
    public LearningPath addPathNode(String pathId, Map<String, Object> nodeData) {
        return learningPathRepository.findById(pathId).map(path -> {
            List<Map<String, Object>> nodes = getPathNodesUnchecked(pathId);
            nodes.add(nodeData);
            try {
                path.setNodes(objectMapper.writeValueAsString(nodes));
            } catch (Exception e) {
                log.error("序列化路径节点失败: pathId={}", pathId, e);
                throw new BusinessException("添加节点失败：数据序列化异常，请稍后重试");
            }
            path.setUpdatedAt(LocalDateTime.now());
            return learningPathRepository.save(path);
        }).orElseThrow(() -> {
            log.warn("添加节点失败：路径不存在: pathId={}", pathId);
            return new BusinessException("学习路径不存在，无法添加节点");
        });
    }

    /**
     * 获取或创建默认活跃路径（当用户无活跃路径时自动创建）
     */
    public Optional<LearningPath> getCurrentPath(String userId) {
        return learningPathRepository.findByUserIdAndIsActive(userId, true);
    }

    /**
     * 获取用户的所有学习路径列表
     */
    public List<LearningPath> getPathList(String userId) {
        return learningPathRepository.findByUserId(userId);
    }

    /**
     * 切换当前激活的学习路径
     */
    @Transactional
    public LearningPath switchPath(String userId, String pathId) {
        List<LearningPath> allPaths = learningPathRepository.findByUserId(userId);
        allPaths.forEach(path -> path.setIsActive(false));
        learningPathRepository.saveAll(allPaths);

        LearningPath target = learningPathRepository.findById(pathId)
                .orElseGet(() -> {
                    log.warn("切换学习路径失败：路径不存在: pathId={}, userId={}", pathId, userId);
                    throw new BusinessException("切换失败：学习路径不存在");
                });
        // 归属校验：只能切换自己的路径
        if (!target.getUserId().equals(userId)) {
            throw new BusinessException("无权切换该学习路径");
        }
        target.setIsActive(true);
        target.setUpdatedAt(LocalDateTime.now());
        return learningPathRepository.save(target);
    }

    /**
     * AI 优化学习路径（重置进度，保持节点结构）
     */
    public LearningPath optimizePath(String pathId, String userId) {
        LearningPath path = requireOwnedPath(pathId, userId);
        path.setCompletionPercentage(0f);
        path.setVersion(path.getVersion() != null ? path.getVersion() + 1 : 1);
        path.setUpdatedAt(LocalDateTime.now());
        log.info("优化学习路径: pathId={}", pathId);
        return learningPathRepository.save(path);
    }

    /**
     * 根据请求生成新的学习路径
     */
    @Transactional(rollbackFor = Exception.class)
    public LearningPath generatePath(String userId, GeneratePathRequest request) {
        // 取消当前激活路径
        learningPathRepository.findByUserIdAndIsActive(userId, true)
                .ifPresent(path -> {
                    path.setIsActive(false);
                    learningPathRepository.save(path);
                });

        String durationStr = request.getDuration() != null
                ? request.getDuration() + "个月"
                : (request.getDurationWeeks() != null ? request.getDurationWeeks() + "周" : "3个月");
        String fieldStr = request.getTargetField() != null && !request.getTargetField().isBlank()
                ? "【" + request.getTargetField() + "】" : "";
        // 总周数：按月 x4 或直接取周数，至少 4 周
        int totalWeeks = request.getDurationWeeks() != null
                ? request.getDurationWeeks()
                : (request.getDuration() != null ? request.getDuration() * 4 : 12);
        totalWeeks = Math.max(totalWeeks, 4);

        LearningPath path = LearningPath.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name(fieldStr + request.getGoal())
                .description("目标：" + request.getGoal() + " | 周期：" + durationStr + " | 领域：" + (request.getTargetField() != null ? request.getTargetField() : "通用"))
                .version(1)
                .isActive(true)
                .completionPercentage(0f)
                .source(request.getSource() != null ? request.getSource() : "manual")
                .nodes(PathOutlineGenerator.generateOutline(request.getGoal(), request.getTargetField(), totalWeeks, objectMapper))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        log.info("为用户生成新学习路径: userId={}, goal={}, field={}, duration={}, source={}",
                userId, request.getGoal(), request.getTargetField(), request.getDuration(), request.getSource());
        return learningPathRepository.save(path);
    }

    public LearningPath getOrCreateDefaultPath(String userId) {
        return learningPathRepository.findByUserIdAndIsActive(userId, true)
                .orElseGet(() -> {
                    LearningPath path = LearningPath.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .name("我的学习计划")
                            .description("通用学习计划：基础入门 → 核心进阶 → 综合应用 → 巩固提升")
                            .version(1)
                            .isActive(true)
                            .completionPercentage(0f)
                            .nodes(PathOutlineGenerator.generateOutline(null, null, 8, objectMapper))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    log.info("为用户创建默认学习路径: userId={}", userId);
                    return learningPathRepository.save(path);
                });
    }

    /**
     * 为存量路径补生成学习大纲（路径已存在且无节点时调用，不覆盖已有内容）
     *
     * @return 生成后的节点数量；已存在内容时返回 -1
     */
    @Transactional
    public int generateOutlineForPath(String pathId, String userId) {
        LearningPath path = requireOwnedPath(pathId, userId);
        List<Map<String, Object>> existing = getPathNodesUnchecked(pathId);
        if (!existing.isEmpty()) {
            log.info("路径已有大纲，跳过补生成: pathId={}, nodes={}", pathId, existing.size());
            return -1;
        }
        String outline = PathOutlineGenerator.generateOutline(path.getName(), null, 12, objectMapper);
        path.setNodes(outline);
        path.setVersion(path.getVersion() != null ? path.getVersion() + 1 : 1);
        path.setUpdatedAt(LocalDateTime.now());
        learningPathRepository.save(path);
        log.info("为路径补生成学习大纲: pathId={}", pathId);
        return getPathNodesUnchecked(pathId).size();
    }

    /**
     * 自动调整学习路径（重置进度、重新生成大纲）
     * 清空旧节点后立即用模板重新生成大纲，避免调整后路径永久空白（曾导致存量路径 nodes=[]）
     */
    public LearningPath autoAdjustPath(String pathId, String userId) {
        LearningPath path = requireOwnedPath(pathId, userId);
        path.setNodes(PathOutlineGenerator.generateOutline(path.getName(), null, 12, objectMapper));
        path.setCompletionPercentage(0f);
        path.setVersion(path.getVersion() != null ? path.getVersion() + 1 : 1);
        path.setUpdatedAt(LocalDateTime.now());
        log.info("自动调整学习路径并重新生成大纲: pathId={}", pathId);
        return learningPathRepository.save(path);
    }

    /**
     * 重置学习路径（重置进度、重新生成大纲）
     * 清空旧节点后立即用模板重新生成大纲，避免重置后路径永久空白（曾导致存量路径 nodes=[]）
     */
    public LearningPath resetPath(String pathId, String userId) {
        LearningPath path = requireOwnedPath(pathId, userId);
        path.setNodes(PathOutlineGenerator.generateOutline(path.getName(), null, 12, objectMapper));
        path.setCompletionPercentage(0f);
        path.setVersion(1);
        path.setUpdatedAt(LocalDateTime.now());
        log.info("重置学习路径并重新生成大纲: pathId={}", pathId);
        return learningPathRepository.save(path);
    }

    /**
     * 删除学习路径（级联删除关联的学习记录）
     * 先删子表 learning_records，再删主表 learning_paths
     */
    @Transactional
    public void deletePath(String pathId, String userId) {
        // 1. 校验路径存在且归属当前用户
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("路径不存在: " + pathId));
        if (!path.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该路径");
        }

        log.info("删除学习路径: pathId={}, userId={}", pathId, userId);

        // 2. 级联删除学习记录
        List<LearningRecord> records = learningRecordRepository.findByPathId(pathId);
        if (!records.isEmpty()) {
            learningRecordRepository.deleteAll(records);
            log.info("已删除关联学习记录: pathId={}, count={}", pathId, records.size());
        }

        // 3. 删除主表
        learningPathRepository.delete(path);
        log.info("学习路径已删除: pathId={}", pathId);
    }
}