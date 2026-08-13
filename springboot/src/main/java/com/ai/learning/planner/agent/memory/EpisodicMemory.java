package com.ai.learning.planner.agent.memory;

import java.time.Instant;
import java.util.List;

/**
 * 情景记忆（Episodic Memory）
 * 将已完成的长任务存储至记忆库，在新推理链启动时通过语义相似度检索相关历史经验，
 * 注入为“隐藏思维链（Hidden CoT）”。
 */
public interface EpisodicMemory {

    /**
     * 经验条目
     *
     * @param id          唯一标识
     * @param taskType    任务类型（planning/diagnosis/qa/report...）
     * @param taskSummary 任务摘要（用于检索）
     * @param experience  沉淀的经验（策略、结论、教训）
     * @param tags        标签（辅助检索）
     * @param timestamp   存储时间
     */
    record Episode(String id, String taskType, String taskSummary, String experience,
                   List<String> tags, Instant timestamp) {
    }

    /**
     * 存储一条完成任务的沉淀经验
     */
    void store(Episode episode);

    /**
     * 语义相似度检索相关历史经验
     *
     * @param query 当前任务描述
     * @param topK  返回数量
     * @return 按相似度降序的经验列表
     */
    List<Episode> search(String query, int topK);

    /**
     * 检索并格式化为 Hidden CoT 注入片段（无相关经验时返回空字符串）
     */
    String buildHiddenCoT(String query, int topK);

    /**
     * 当前存储的经验数量
     */
    int size();

    /**
     * 清空记忆
     */
    void clear();
}
