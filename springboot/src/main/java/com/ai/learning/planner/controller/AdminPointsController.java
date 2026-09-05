package com.ai.learning.planner.controller;

import com.ai.learning.planner.dto.*;
import com.ai.learning.planner.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员积分管理控制器
 * 提供管理员手动发放/扣除积分、更新配置等 API 接口
 * 所有接口仅限管理员访问
 */
@RestController
@RequestMapping("/admin/points")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "管理员积分管理", description = "管理员手动发放/扣除积分、更新配置")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPointsController {

    private final PointsService pointsService;

    /**
     * 手动给指定用户发放积分
     */
    @PostMapping("/grant")
    @Operation(summary = "发放积分", description = "管理员手动给指定用户发放积分")
    public ResponseEntity<ApiResponse<Void>> grantPoints(
            Authentication authentication,
            @RequestBody PointsGrantRequest request) {
        log.info("管理员发放积分: admin={}, userId={}, points={}",
                authentication.getPrincipal(), request.getUserId(), request.getPoints());
        try {
            pointsService.grantPoints(request.getUserId(), request.getPoints(), request.getDescription());
            return ResponseEntity.ok(ApiResponse.success("积分发放成功", null));
        } catch (Exception e) {
            log.error("积分发放失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("积分发放失败：" + e.getMessage()));
        }
    }

    /**
     * 手动扣除指定用户积分
     */
    @PostMapping("/revoke")
    @Operation(summary = "扣除积分", description = "管理员手动扣除指定用户积分")
    public ResponseEntity<ApiResponse<Void>> revokePoints(
            Authentication authentication,
            @RequestBody PointsRevokeRequest request) {
        log.info("管理员扣除积分: admin={}, userId={}, points={}",
                authentication.getPrincipal(), request.getUserId(), request.getPoints());
        try {
            pointsService.revokePoints(request.getUserId(), request.getPoints(), request.getDescription());
            return ResponseEntity.ok(ApiResponse.success("积分扣除成功", null));
        } catch (Exception e) {
            log.error("积分扣除失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("积分扣除失败：" + e.getMessage()));
        }
    }

    /**
     * 获取当前积分配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取积分配置", description = "获取当前签到/消耗积分配置")
    public ResponseEntity<ApiResponse<PointsConfigDTO>> getConfig(Authentication authentication) {
        log.info("获取积分配置: admin={}", authentication.getPrincipal());
        try {
            PointsConfigDTO config = pointsService.getConfig();
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取积分配置失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取积分配置失败：" + e.getMessage()));
        }
    }

    /**
     * 更新签到/消耗配置
     */
    @PutMapping("/config")
    @Operation(summary = "更新积分配置", description = "更新签到/消耗积分配置，修改后即时生效")
    public ResponseEntity<ApiResponse<Void>> updateConfig(
            Authentication authentication,
            @RequestBody PointsConfigRequest request) {
        log.info("更新积分配置: admin={}, config={}", authentication.getPrincipal(), request);
        try {
            pointsService.updateConfig(request);
            return ResponseEntity.ok(ApiResponse.success("配置更新成功", null));
        } catch (Exception e) {
            log.error("更新积分配置失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("更新积分配置失败：" + e.getMessage()));
        }
    }

    /**
     * 查询指定用户积分余额
     */
    @GetMapping("/balance/{userId}")
    @Operation(summary = "查询用户积分", description = "管理员查询指定用户积分余额")
    public ResponseEntity<ApiResponse<PointsBalanceDTO>> getUserBalance(
            Authentication authentication,
            @PathVariable Long userId) {
        log.info("管理员查询用户积分: admin={}, userId={}", authentication.getPrincipal(), userId);
        try {
            PointsBalanceDTO balance = pointsService.getBalance(userId);
            return ResponseEntity.ok(ApiResponse.success(balance));
        } catch (Exception e) {
            log.error("查询用户积分失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("查询用户积分失败：" + e.getMessage()));
        }
    }
}