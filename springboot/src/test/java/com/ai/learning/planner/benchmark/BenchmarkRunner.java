package com.ai.learning.planner.benchmark;

import com.ai.learning.planner.agent.memory.ContextCompressor;
import com.ai.learning.planner.agent.memory.ContextWindow;
import com.ai.learning.planner.service.ModelManager;
import com.ai.learning.planner.vectorstore.InMemoryVectorStoreWrapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 真实性能基准实验（手动指定运行：mvn test -Dtest=BenchmarkRunner）
 * 使用真实 DeepSeek API（.env 配置）与真实 MySQL 知识分块数据。
 * 不参与常规测试套件（类名不以 Test 结尾）。
 */
public class BenchmarkRunner {

    private static final Path ENV = Path.of("..", ".env");

    private String env(String key) throws IOException {
        for (String line : Files.readAllLines(ENV, StandardCharsets.UTF_8)) {
            if (line.startsWith(key + "=")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return "";
    }

    private OpenAiChatModel buildChatModel() throws IOException {
        String key = env("DEEPSEEK_API_KEY");
        String base = env("DEEPSEEK_BASE_URL");
        OpenAiApi api = OpenAiApi.builder().baseUrl(base).apiKey(key).build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("deepseek-chat").temperature(0.7).build();
        return OpenAiChatModel.builder()
                .openAiApi(api).defaultOptions(options)
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private EmbeddingModel buildEmbeddingModel() throws IOException {
        String key = env("DEEPSEEK_API_KEY");
        String base = env("DEEPSEEK_BASE_URL");
        OpenAiApi api = OpenAiApi.builder().baseUrl(base).apiKey(key).build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder().dimensions(1536).build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options,
                RetryTemplate.defaultInstance(), ObservationRegistry.NOOP);
    }

    private List<String> loadChunks() throws Exception {
        String host = env("MYSQL_HOST");
        String port = env("MYSQL_PORT");
        String db = env("MYSQL_DATABASE");
        String user = env("MYSQL_USERNAME");
        String pass = env("MYSQL_PASSWORD");
        List<String> chunks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
                user, pass);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT content FROM knowledge_chunks ORDER BY RAND() LIMIT 60")) {
            while (rs.next()) {
                chunks.add(rs.getString(1));
            }
        }
        return chunks;
    }

    /** 用真实知识分块构造长链任务对话（模拟诊断→规划→答疑多轮交互） */
    private List<Map<String, Object>> buildLongConversation(List<String> chunks, int rounds) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            String q = "第" + (i + 1) + "轮提问：请结合以下资料讲解相关概念并给出学习建议。资料：" + chunks.get(i % chunks.size());
            String a = "第" + (i + 1) + "轮回答：基于检索结果可以总结如下要点。" + chunks.get((i + 7) % chunks.size());
            messages.add(Map.of("role", "user", "content", q));
            messages.add(Map.of("role", "assistant", "content", a));
        }
        return messages;
    }

    // ============ 实验 A：上下文压缩（真实 LLM 摘要 vs 规则截断） ============

    @Test
    void experimentA_contextCompression() throws Exception {
        System.out.println("\n========== 实验A: 上下文压缩 ==========");
        List<String> chunks = loadChunks();
        System.out.println("[数据] 真实知识分块: " + chunks.size() + " 条（MySQL knowledge_chunks）");

        for (int rounds : new int[]{20, 30, 40, 50}) {
            List<Map<String, Object>> messages = buildLongConversation(chunks, rounds);
            ContextWindow window = new ContextWindow(30720, 0.7);
            int tokens = ContextWindow.estimateTokens(messages);
            System.out.println("\n[场景] 对话轮数=" + rounds + "，总token=" + tokens
                    + "，使用率=" + String.format("%.1f%%", tokens * 100.0 / 30720));

            try {
                ContextCompressor llmCompressor = new ContextCompressor(window,
                        new ModelManager(java.util.Map.of("deepSeekChatModel", buildChatModel())), new SimpleMeterRegistry());
                var llmResult = llmCompressor.compress(messages, "你是一名学习规划助手，负责诊断、规划、答疑。", "完成 Java 学习路径规划");
                System.out.println("  [LLM摘要路径] " + llmResult.tokensBefore() + " -> " + llmResult.tokensAfter()
                        + " tokens, 压缩率=" + String.format("%.1f%%", llmResult.compressionRatio() * 100)
                        + ", 触发=" + llmResult.triggered());
            } catch (Exception e) {
                System.out.println("  [LLM摘要路径] 失败: " + e.getMessage());
            }

            ContextCompressor ruleCompressor = new ContextCompressor(window, null, new SimpleMeterRegistry());
            var ruleResult = ruleCompressor.compress(messages, "你是一名学习规划助手，负责诊断、规划、答疑。", "完成 Java 学习路径规划");
            System.out.println("  [规则截断路径] " + ruleResult.tokensBefore() + " -> " + ruleResult.tokensAfter()
                    + " tokens, 压缩率=" + String.format("%.1f%%", ruleResult.compressionRatio() * 100)
                    + ", 触发=" + ruleResult.triggered());
        }
    }

    // ============ 实验 B：SSE 流式首字响应 ============

    @Test
    void experimentB_firstTokenLatency() throws Exception {
        System.out.println("\n========== 实验B: SSE 流式首字响应（真实 DeepSeek） ==========");
        OpenAiChatModel model = buildChatModel();
        ChatClient client = ChatClient.builder(model).build();
        String question = "请简述 Java 中 HashMap 的原理，并给出使用建议。";

        List<Long> latencies = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            String first = client.prompt().user(question)
                    .stream().content()
                    .blockFirst(java.time.Duration.ofSeconds(60));
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            latencies.add(ms);
            System.out.println("  第" + (i + 1) + "次: 首字 " + ms + "ms, 首个片段长度="
                    + (first == null ? 0 : first.length()) + " 字符");
        }
        latencies.sort(Comparator.naturalOrder());
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.println(String.format("  [结果] 5次: min=%dms, avg=%.0fms, max=%dms",
                latencies.get(0), avg, latencies.get(latencies.size() - 1)));
    }

    // ============ 实验 C：RAG 检索耗时（真实数据, ES 不可达降级场景） ============

    @Test
    void experimentC_retrievalLatency() throws Exception {
        System.out.println("\n========== 实验C: RAG 检索耗时（ES 不可达 → 降级路径） ==========");
        List<String> chunks = loadChunks();
        List<String> queries = List.of("AI", "学习", "知识库", "模型", "平台");

        // 降级一：内存向量检索（真实 Embedding，网络调用）
        System.out.println("\n[降级-内存向量] embedding=DeepSeek(真实调用)");
        try {
            InMemoryVectorStoreWrapper vectorStore = new InMemoryVectorStoreWrapper(buildEmbeddingModel());
            List<Document> docs = new ArrayList<>();
            int idx = 0;
            for (String c : chunks) {
                docs.add(new Document("chunk-" + (idx++), c, Map.of("docTitle", "真实知识文档")));
            }
            vectorStore.add(docs);
            System.out.println("  文档数=" + vectorStore.getDocumentCount() + ", embeddingEnabled=" + vectorStore.isEmbeddingEnabled());

            List<Long> times = new ArrayList<>();
            for (String q : queries) {
                for (int i = 0; i < 5; i++) {
                    long start = System.nanoTime();
                    var hits = vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(5).similarityThreshold(0.7).build());
                    times.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                }
            }
            times.sort(Comparator.naturalOrder());
            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
            System.out.println(String.format("  [结果] 25次: min=%dms, avg=%.0fms, p95=%dms, max=%dms, hits=%d",
                    times.get(0), avg, times.get((int) (times.size() * 0.95)), times.get(times.size() - 1),
                    vectorStore.similaritySearch(SearchRequest.builder().query(queries.get(0)).topK(5).build()).size()));
        } catch (Exception e) {
            System.out.println("  内存向量实验失败: " + e.getMessage());
        }

        // 降级二：内存关键词检索（无 Embedding，纯本地）
        System.out.println("\n[降级-关键词] 无 Embedding（纯本地匹配）");
        InMemoryVectorStoreWrapper kwStore = new InMemoryVectorStoreWrapper(null);
        List<Document> docs = new ArrayList<>();
        int idx = 0;
        for (String c : chunks) {
            docs.add(new Document("chunk-" + (idx++), c, Map.of("docTitle", "真实知识文档")));
        }
        kwStore.add(docs);
        List<Long> times = new ArrayList<>();
        for (String q : queries) {
            for (int i = 0; i < 10; i++) {
                long start = System.nanoTime();
                kwStore.similaritySearch(SearchRequest.builder().query(q).topK(5).build());
                times.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            }
        }
        times.sort(Comparator.naturalOrder());
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.println(String.format("  [结果] 50次: min=%dms, avg=%.1fms, p95=%dms, max=%dms",
                times.get(0), avg, times.get((int) (times.size() * 0.95)), times.get(times.size() - 1)));

        // 降级三：MySQL 关键词 LIKE（模拟 KnowledgeService.fallbackKeywordSearch 的真实 SQL）
        System.out.println("\n[降级-数据库关键词] MySQL LIKE 查询（真实 SQL）");
        String host = env("MYSQL_HOST");
        String port = env("MYSQL_PORT");
        String db = env("MYSQL_DATABASE");
        String user = env("MYSQL_USERNAME");
        String pass = env("MYSQL_PASSWORD");
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
                user, pass)) {
            List<Long> sqlTimes = new ArrayList<>();
            for (String q : queries) {
                for (int i = 0; i < 10; i++) {
                    long start = System.nanoTime();
                    try (Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery(
                                 "SELECT id, content FROM knowledge_chunks WHERE content LIKE '%" + q + "%' LIMIT 10")) {
                        int n = 0;
                        while (rs.next()) n++;
                        if (n > 0) {
                            sqlTimes.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                        }
                    }
                }
            }
            sqlTimes.sort(Comparator.naturalOrder());
            if (sqlTimes.isEmpty()) {
                System.out.println("  [结果] 查询无命中（未计入耗时统计）");
            } else {
                double sqlAvg = sqlTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                System.out.println(String.format("  [结果] %d次: min=%dms, avg=%.1fms, p95=%dms, max=%dms",
                        sqlTimes.size(), sqlTimes.get(0), sqlAvg,
                        sqlTimes.get((int) (sqlTimes.size() * 0.95)), sqlTimes.get(sqlTimes.size() - 1)));
            }
        }
    }

    }