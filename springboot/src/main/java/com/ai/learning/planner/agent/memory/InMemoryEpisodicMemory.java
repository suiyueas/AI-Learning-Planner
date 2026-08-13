package com.ai.learning.planner.agent.memory;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存版情景记忆实现
 * 基于词频向量的余弦相似度检索（开发环境无 Elasticsearch，生产可替换为 VectorStore 实现）
 */
@Slf4j
public class InMemoryEpisodicMemory implements EpisodicMemory {

    private final List<Episode> episodes = new CopyOnWriteArrayList<>();

    @Override
    public void store(Episode episode) {
        if (episode == null || episode.id() == null) return;
        episodes.add(episode);
        log.info("[EpisodicMemory] 存储经验: {} ({})", episode.taskSummary(), episode.taskType());
    }

    @Override
    public List<Episode> search(String query, int topK) {
        if (episodes.isEmpty()) return List.of();
        Map<String, Double> queryVector = vectorize(query);

        return episodes.stream()
                .map(e -> new Scored(e, cosine(queryVector, vectorize(e.taskSummary() + " " + String.join(" ", e.tags())))))
                .filter(s -> s.score() > 0.1)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(Math.max(topK, 1))
                .map(Scored::episode)
                .toList();
    }

    @Override
    public String buildHiddenCoT(String query, int topK) {
        List<Episode> hits = search(query, topK);
        if (hits.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【历史经验参考】(Hidden CoT)\n");
        for (int i = 0; i < hits.size(); i++) {
            Episode e = hits.get(i);
            sb.append(i + 1).append(". [").append(e.taskType()).append("] ")
                    .append(e.taskSummary()).append("\n   经验: ")
                    .append(e.experience()).append('\n');
        }
        return sb.toString();
    }

    @Override
    public int size() {
        return episodes.size();
    }

    @Override
    public void clear() {
        episodes.clear();
    }

    /**
     * 简易词频向量（中文按二元字符切分，英文按词切分）
     */
    private Map<String, Double> vectorize(String text) {
        Map<String, Double> vector = new HashMap<>();
        if (text == null || text.isBlank()) return vector;

        // 英文单词
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!word.isBlank() && word.length() > 1) {
                vector.merge(word, 1.0, Double::sum);
            }
        }
        // 中文二元字符
        String chinese = text.replaceAll("[^\\u4e00-\\u9fa5]", "");
        for (int i = 0; i < chinese.length() - 1; i++) {
            vector.merge(chinese.substring(i, i + 2), 1.0, Double::sum);
        }
        return vector;
    }

    /**
     * 余弦相似度
     */
    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        double dot = 0.0;
        for (Map.Entry<String, Double> entry : a.entrySet()) {
            Double bv = b.get(entry.getKey());
            if (bv != null) dot += entry.getValue() * bv;
        }
        double normA = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (normA * normB);
    }

    /**
     * 相似度评分包装
     */
    private record Scored(Episode episode, double score) {
    }
}
