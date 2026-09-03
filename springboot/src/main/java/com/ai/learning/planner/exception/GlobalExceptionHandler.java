package com.ai.learning.planner.exception;

import com.ai.learning.planner.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局统一异常处理器
 *
 * 功能说明：
 * - 捕获并统一处理应用程序中抛出的各类异常
 * - 将异常信息转换为标准的 API 响应格式返回给前端
 * - 对敏感信息进行脱敏处理，防止泄露服务器内部细节
 * - 记录详细日志便于问题排查和监控
 *
 * 处理策略：
 * - 400 Bad Request：参数校验、类型不匹配、数据冲突等客户端错误
 * - 401 Unauthorized：未认证或登录过期
 * - 403 Forbidden：权限不足
 * - 404 Not Found：资源不存在
 * - 413 Payload Too Large：文件上传超限
 * - 500 Internal Server Error：服务器内部错误
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 参数校验失败（@Valid 请求体）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 参数不合法（业务侧抛出的 IllegalArgumentException）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 业务异常（携带明确的业务提示信息，直接透传给前端）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 未认证/登录过期
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException e) {
        log.warn("未认证访问: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
    }

    /**
     * 约束校验失败（方法参数上的校验注解）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 请求体解析失败（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("请求体格式错误，请检查 JSON 格式"));
    }

    /**
     * 路径参数/请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数 " + e.getName() + " 格式不正确"));
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("缺少必填参数: " + e.getParameterName()));
    }

    /**
     * 数据完整性冲突（唯一约束、外键约束等）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性冲突: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("数据冲突：请检查提交内容是否重复或存在未填写的必填项"));
    }

    /**
     * 文件上传超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件上传超限: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(HttpStatus.PAYLOAD_TOO_LARGE.value(), "文件大小超过限制（最大10MB）"));
    }

    /**
     * 积分不足异常（HTTP 402 Payment Required）
     */
    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientPoints(InsufficientPointsException e) {
        log.warn("积分不足: userId={}, current={}, required={}, feature={}",
                e.getMessage(), e.getCurrentPoints(), e.getRequiredPoints(), e.getFeature());
        String message = String.format("积分不足，当前积分: %d，需要积分: %d，功能: %s",
                e.getCurrentPoints(), e.getRequiredPoints(), e.getFeature());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponse.error(HttpStatus.PAYMENT_REQUIRED.value(), message));
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        // 透传业务侧写死的提示（如"仅管理员可…"），不包含敏感信息
        log.warn("权限不足: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.forbidden(e.getMessage()));
    }

    /**
     * 静态资源/路径未找到（翻译为 404，不回显底层路径细节）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("请求路径不存在: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("请求的接口不存在"));
    }

    /**
     * 空指针异常（返回更明确的提示）
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointer(NullPointerException e) {
        log.error("空指针异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("服务器内部错误：数据异常，请检查请求参数或联系管理员"));
    }

    /**
     * 运行时异常（统一脱敏文案，底层异常详情仅入日志，防止泄露 SQL/内部依赖信息）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("服务器内部错误，请稍后重试"));
    }

    /**
     * 未知异常（统一脱敏文案，底层异常详情仅入日志）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("未知异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError("服务器内部错误，请稍后重试"));
    }
}
