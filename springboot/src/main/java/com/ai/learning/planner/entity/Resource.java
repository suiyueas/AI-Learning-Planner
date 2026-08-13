package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 学习资源实体
 * 对应 resources 表，与学习路径节点关联的推荐学习材料（标题、链接、评分）
 */
@Entity
@Table(name = "resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "avg_rating")
    private Float avgRating;

    @Column(columnDefinition = "TEXT")
    private String description;
}
