package com.ai.learning.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

/**
 * 知识节点实体类
 * 对应数据库中的knowledge_nodes表，存储知识图谱中的节点信息
 */
@Entity
@Table(name = "knowledge_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeNode implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer difficulty;

    @Column(name = "estimated_hours")
    private Float estimatedHours;

    private String category;

    @Column(name = "prerequisites", columnDefinition = "JSON")
    private String prerequisites;
}