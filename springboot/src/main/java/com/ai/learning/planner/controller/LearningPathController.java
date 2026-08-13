package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.ApiResponse;
import com.ai.learning.planner.dto.ActivePathDTO;
import com.ai.learning.planner.dto.GeneratePathRequest;
import com.ai.learning.planner.dto.LearningPathUpdateRequest;
import com.ai.learning.planner.dto.PathAdjustRequest;
import com.ai.learning.planner.dto.PathProgressDTO;
import com.ai.learning.planner.dto.PathRequest;
import com.ai.learning.planner.dto.TaskProgressRequest;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.entity.LearningRecord;
import com.ai.learning.planner.security.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.ai.learning.planner.service.LearningPathService;
import com.ai.learning.planner.service.LearningProgressService;
import com.ai.learning.planner.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 学习路径控制器
 * 提供学习路径的创建、查询、更新和节点管理等功能
 */
@RestController
@RequestMapping("/learning-path")
@RequiredArgsConstructor
@Slf4j
public class LearningPathController {

    private final LearningPathService learningPathService;
    private final LearningProgressService learningProgressService;
    private final SecurityContextHolder securityContextHolder;

    /**
     * 创建学习路径（userId 强制取自认证上下文，忽略请求体字段）
     */
    @PostMapping
    public LearningPath createPath(@Valid @RequestBody PathRequest request, Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("创建学习路径: userId={}, goal={}", userId, request.getGoal());
        return learningPathService.createPath(request, userId);
    }

    /**
     * 获取指定用户的所有学习路径（仅本人或管理员可查）
     */
    @GetMapping("/user/{userId}")
    public List<LearningPath> getUserPaths(@PathVariable String userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        return learningPathService.getUserPaths(userId);
    }

    /**
     * 获取指定用户的激活状态学习路径（仅本人或管理员可查）
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<Optional<LearningPath>> getActivePath(@PathVariable String userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        return ResponseEntity.ok(learningPathService.getActivePath(userId));
    }

    /**
     * 校验目标 userId 为当前用户本人，或当前用户是管理员
     */
    private void requireSelfOrAdmin(String targetUserId, Authentication authentication) {
        String currentUserId = SecurityUtils.requireUserId(authentication);
        if (!currentUserId.equals(targetUserId) && !securityContextHolder.isAdmin()) {
            throw new AccessDeniedException("无权访问该用户的学习路径");
        }
    }

    /**
     * 获取当前用户激活的学习路径
     * GET /api/learning-path/current
     * 无活跃路径时返回 null，前端展示空状态
     */
    @GetMapping("/current")
    public ResponseEntity<LearningPath> getCurrentPath(Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("获取当前用户学习路径: userId={}", userId);
        Optional<LearningPath> pathOpt = learningPathService.getCurrentPath(userId);
        if (pathOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pathOpt.get());
    }

    /**
     * 获取用户所有学习路径列表
     * GET /api/learning-path/list
     */
    @GetMapping("/list")
    public ResponseEntity<List<LearningPath>> getPathList(Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("获取用户学习路径列表: userId={}", userId);
        List<LearningPath> paths = learningPathService.getPathList(userId);
        return ResponseEntity.ok(paths);
    }

    /**
     * 切换当前激活的学习路径
     * POST /api/learning-path/switch/{id}
     */
    @PostMapping("/switch/{id}")
    public ResponseEntity<LearningPath> switchPath(
            @PathVariable String id,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("切换学习路径: userId={}, pathId={}", userId, id);
        try {
            LearningPath path = learningPathService.switchPath(userId, id);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            log.error("切换学习路径失败: pathId={}, error={}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * AI 优化学习路径
     * POST /api/learning-path/optimize/{id}
     */
    @PostMapping("/optimize/{id}")
    public ResponseEntity<LearningPath> optimizePath(
            @PathVariable String id,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("优化学习路径: pathId={}, userId={}", id, userId);
        try {
            LearningPath path = learningPathService.optimizePath(id, userId);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            log.error("优化学习路径失败: pathId={}, error={}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重置学习路径（通过路径变量）
     * POST /api/learning-path/reset/{id}
     */
    @PostMapping("/reset/{id}")
    public ResponseEntity<LearningPath> resetPathById(
            @PathVariable String id,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("重置学习路径: pathId={}, userId={}", id, userId);
        try {
            LearningPath path = learningPathService.resetPath(id, userId);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            log.error("重置学习路径失败: pathId={}, error={}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 生成新的学习路径
     * POST /api/learning-path/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<LearningPath> generatePath(
            @Valid @RequestBody GeneratePathRequest request,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("生成新学习路径: userId={}, goal={}", userId, request.getGoal());
        try {
            LearningPath path = learningPathService.generatePath(userId, request);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            log.error("生成学习路径失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{pathId}")
    public ResponseEntity<LearningPath> getPath(@PathVariable String pathId, Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        boolean isAdmin = securityContextHolder.isAdmin();
        log.info("获取学习路径详情: pathId={}, userId={}", pathId, userId);
        return learningPathService.getPath(pathId, userId, isAdmin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除学习路径
     * DELETE /api/learning-path/{id}
     * 级联删除关联的学习记录，需校验路径归属权
     */
    @DeleteMapping("/{pathId}")
    public ResponseEntity<ApiResponse<Void>> deletePath(
            @PathVariable String pathId,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("删除学习路径: pathId={}, userId={}", pathId, userId);
        try {
            learningPathService.deletePath(pathId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("无权")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(403, msg));
            }
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, msg));
            }
            log.error("删除学习路径失败: pathId={}, error={}", pathId, msg, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("删除失败：" + msg));
        }
    }

    @PutMapping("/{pathId}")
    public LearningPath updatePath(@PathVariable String pathId,
                                   @Valid @RequestBody LearningPathUpdateRequest request,
                                   Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        LearningPath path = LearningPath.builder()
                .id(pathId)
                .name(request.getName())
                .description(request.getDescription())
                .version(request.getVersion())
                .isActive(request.getIsActive())
                .completionPercentage(request.getCompletionPercentage())
                .nodes(request.getNodes())
                .build();
        return learningPathService.updatePath(path, userId);
    }

    @GetMapping("/{pathId}/nodes")
    public List<Map<String, Object>> getPathNodes(@PathVariable String pathId, Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        boolean isAdmin = securityContextHolder.isAdmin();
        return learningPathService.getPathNodes(pathId, userId, isAdmin);
    }

    /**
     * 为存量空路径补生成学习大纲（章节-周-任务）
     * POST /api/learning-path/{pathId}/outline/generate
     */
    @PostMapping("/{pathId}/outline/generate")
    public ResponseEntity<Map<String, Object>> generateOutline(@PathVariable String pathId, Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("补生成学习路径大纲: pathId={}, userId={}", pathId, userId);
        try {
            int nodeCount = learningPathService.generateOutlineForPath(pathId, userId);
            return ResponseEntity.ok(Map.of("success", true, "nodeCount", nodeCount));
        } catch (RuntimeException e) {
            log.error("补生成学习路径大纲失败: pathId={}, error={}", pathId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 自动调整学习路径
     * POST /api/learning-path/auto-adjust
     */
    @PostMapping("/auto-adjust")
    public ResponseEntity<LearningPath> autoAdjustPath(@Valid @RequestBody PathAdjustRequest request, Authentication authentication) {
        String pathId = request.getPathId();
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("自动调整学习路径: pathId={}, userId={}", pathId, userId);
        try {
            return ResponseEntity.ok(learningPathService.autoAdjustPath(pathId, userId));
        } catch (Exception e) {
            log.error("自动调整学习路径失败: pathId={}, error={}", pathId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重置学习路径
     * POST /api/learning-path/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<LearningPath> resetPath(@Valid @RequestBody PathAdjustRequest request, Authentication authentication) {
        String pathId = request.getPathId();
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("重置学习路径: pathId={}, userId={}", pathId, userId);
        try {
            return ResponseEntity.ok(learningPathService.resetPath(pathId, userId));
        } catch (Exception e) {
            log.error("重置学习路径失败: pathId={}, error={}", pathId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取学习路径进度
     * GET /api/learning-path/{pathId}/progress
     */
    @GetMapping("/{pathId}/progress")
    public ResponseEntity<ApiResponse<PathProgressDTO>> getPathProgress(
            @PathVariable String pathId,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("获取学习路径进度: pathId={}, userId={}", pathId, userId);
        try {
            if (pathId == null || pathId.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("路径ID不能为空"));
            }
            PathProgressDTO progress = learningProgressService.calculateProgress(userId, pathId);
            return ResponseEntity.ok(ApiResponse.success(progress));
        } catch (IllegalArgumentException e) {
            log.warn("获取学习路径进度参数错误: pathId={}, error={}", pathId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("获取学习路径进度失败: pathId={}, error={}", pathId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("获取学习路径进度异常: pathId={}, error={}", pathId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("获取进度失败：" + e.getMessage()));
        }
    }

    /**
     * 标记任务为已完成
     * POST /api/learning-path/{pathId}/task/{taskId}/complete
     */
    @PostMapping("/{pathId}/task/{taskId}/complete")
    public ResponseEntity<LearningRecord> completeTask(
            @PathVariable String pathId,
            @PathVariable String taskId,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("标记任务完成: pathId={}, taskId={}, userId={}", pathId, taskId, userId);
        try {
            LearningRecord record = learningProgressService.completeTask(userId, pathId, taskId);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            log.error("标记任务完成失败: pathId={}, taskId={}, error={}", pathId, taskId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新任务学习进度
     * POST /api/learning-path/{pathId}/task/{taskId}/progress
     */
    @PostMapping("/{pathId}/task/{taskId}/progress")
    public ResponseEntity<LearningRecord> updateTaskProgress(
            @PathVariable String pathId,
            @PathVariable String taskId,
            @Valid @RequestBody TaskProgressRequest body,
            Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        Integer timeSpent = body.getTimeSpent();
        Float masteryLevel = body.getMasteryLevel();
        log.info("更新任务进度: pathId={}, taskId={}, userId={}, timeSpent={}, masteryLevel={}",
                pathId, taskId, userId, timeSpent, masteryLevel);
        try {
            LearningRecord record = learningProgressService.updateTaskProgress(userId, pathId, taskId, timeSpent, masteryLevel);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            log.error("更新任务进度失败: pathId={}, taskId={}, error={}", pathId, taskId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前活跃路径的统一数据（解决卡片与详情页数据不一致问题）
     * GET /api/learning-path/active
     * 返回：路径信息、实时进度（从learning_record计算）、下一节点、路径状态
     */
    @GetMapping("/active")
    public ResponseEntity<ActivePathDTO> getActivePath(Authentication authentication) {
        String userId = SecurityUtils.requireUserId(authentication);
        log.info("获取当前活跃路径统一数据: userId={}", userId);
        try {
            ActivePathDTO activePath = learningProgressService.getActivePathProgress(userId);
            return ResponseEntity.ok(activePath);
        } catch (Exception e) {
            log.error("获取当前活跃路径失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}