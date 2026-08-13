package com.ai.learning.planner.service;

import com.ai.learning.planner.dto.PathRequest;
import com.ai.learning.planner.entity.LearningPath;
import com.ai.learning.planner.repository.LearningPathRepository;
import com.ai.learning.planner.repository.LearningRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 学习路径服务单元测试
 * 覆盖路径创建、查询、激活路径与版本更新逻辑
 */
@ExtendWith(MockitoExtension.class)
class LearningPathServiceTest {

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private LearningRecordRepository learningRecordRepository;

    private LearningPathService learningPathService;

    @BeforeEach
    void setUp() {
        learningPathService = new LearningPathService(
                learningPathRepository, learningRecordRepository, new ObjectMapper());
    }

    private PathRequest buildPathRequest(String userId, String goal) {
        PathRequest request = new PathRequest();
        request.setUserId(userId);
        request.setGoal(goal);
        return request;
    }

    @Test
    void createPath_success_savesPathWithGeneratedOutline() {
        when(learningPathRepository.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        // userId 强制取自认证上下文（第二参数），请求体中的 userId 被忽略
        LearningPath path = learningPathService.createPath(buildPathRequest("user-1", "学习Java"), "user-1");

        assertNotNull(path.getId());
        assertEquals("user-1", path.getUserId());
        assertTrue(path.getName().contains("学习Java"));
        assertEquals(1, path.getVersion());
        assertTrue(path.getIsActive());
        assertEquals(0f, path.getCompletionPercentage());
        assertNotNull(path.getNodes());
        verify(learningPathRepository).save(any(LearningPath.class));
    }

    @Test
    void getUserPaths_returnsRepositoryResult() {
        LearningPath path = LearningPath.builder().id("path-1").userId("user-1").build();
        when(learningPathRepository.findByUserId("user-1")).thenReturn(List.of(path));

        List<LearningPath> paths = learningPathService.getUserPaths("user-1");

        assertEquals(1, paths.size());
        assertEquals("path-1", paths.get(0).getId());
    }

    @Test
    void getActivePath_returnsActivePathIfExists() {
        LearningPath active = LearningPath.builder().id("path-1").isActive(true).build();
        when(learningPathRepository.findByUserIdAndIsActive("user-1", true)).thenReturn(Optional.of(active));

        Optional<LearningPath> result = learningPathService.getActivePath("user-1");

        assertTrue(result.isPresent());
        assertEquals("path-1", result.get().getId());
    }

    @Test
    void getActivePath_noActivePath_returnsEmpty() {
        when(learningPathRepository.findByUserIdAndIsActive("user-1", true)).thenReturn(Optional.empty());

        Optional<LearningPath> result = learningPathService.getActivePath("user-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void updatePath_incrementsVersionAndUpdatesTimestamp() {
        // 归属校验：仓库中存在本人路径
        LearningPath existing = LearningPath.builder().id("path-1").userId("user-1").version(2).isActive(true).build();
        when(learningPathRepository.findById("path-1")).thenReturn(Optional.of(existing));

        LearningPath path = LearningPath.builder()
                .id("path-1").version(2).isActive(true).build();
        when(learningPathRepository.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        LearningPath updated = learningPathService.updatePath(path, "user-1");

        assertEquals(3, updated.getVersion());
        assertNotNull(updated.getUpdatedAt());
        // 归属字段保留原值，不允许通过更新请求篡改
        assertEquals("user-1", updated.getUserId());
        verify(learningPathRepository).save(path);
    }

    @Test
    void updatePath_notOwned_throws() {
        LearningPath existing = LearningPath.builder().id("path-1").userId("user-other").version(2).isActive(true).build();
        when(learningPathRepository.findById("path-1")).thenReturn(Optional.of(existing));

        LearningPath path = LearningPath.builder().id("path-1").version(2).isActive(true).build();

        assertThrows(RuntimeException.class, () -> learningPathService.updatePath(path, "user-1"));
        verify(learningPathRepository, never()).save(any(LearningPath.class));
    }

    @Test
    void updatePath_nullVersion_startsFromOne() {
        LearningPath existing = LearningPath.builder().id("path-1").userId("user-1").version(null).build();
        when(learningPathRepository.findById("path-1")).thenReturn(Optional.of(existing));

        LearningPath path = LearningPath.builder().id("path-1").version(null).build();
        when(learningPathRepository.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        LearningPath updated = learningPathService.updatePath(path, "user-1");

        assertEquals(1, updated.getVersion());
    }
}
