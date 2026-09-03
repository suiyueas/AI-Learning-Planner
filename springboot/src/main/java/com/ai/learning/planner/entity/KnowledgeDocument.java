package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体
 * 存储用户上传的知识库文档元数据
 */
@Entity
@Table(name = "knowledge_documents", indexes = {
    @Index(name = "idx_kd_status", columnList = "status"),
    @Index(name = "idx_kd_created_at", columnList = "uploaded_at"),
    @Index(name = "idx_kd_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private String id;

    /** 文档标题（原始文件名） */
    @Column(nullable = false)
    private String title;

    /** 文件类型（PDF、Word、Markdown、TXT等） */
    @Column(length = 20)
    private String type;

    /** 文件大小（格式化后的可读字符串） */
    @Column(length = 20)
    private String size;

    /** 处理状态：processing / ready / error */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "processing";

    /** 文件存储路径 */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** 知识块数量 */
    @Builder.Default
    private Integer chunks = 0;

    /** 文档描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 上传时间 */
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    /** 处理完成时间 */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** 用户ID - 数据隔离字段 */
    @Column(name = "user_id", length = 50)
    private String userId;

    /** 来源类型：UPLOAD（用户上传）/ SYSTEM（系统内置）/ EXTERNAL（外部来源） */
    @Column(name = "source_type", length = 20)
    @Builder.Default
    private String sourceType = "UPLOAD";

    /** 信任等级 0-10，10 为完全可信（管理员上传为 10，普通用户为 5） */
    @Column(name = "trust_level")
    @Builder.Default
    private Integer trustLevel = 5;

    /** 是否经过人工审核 */
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    /** 审核时间 */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** 处理失败原因（status=error 时保存） */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}