# 性能基准实验报告（Benchmark）

> 本报告所有数据均为**真实测量**，非估算：使用真实 DeepSeek API（`.env` 配置）与真实 MySQL 知识分块数据（60 条）。
> 实验代码：`springboot/src/test/java/com/ai/learning/planner/benchmark/BenchmarkRunner.java`
> 运行时间：2026-08-16 ｜ 环境：Java 21 / Spring AI 1.1.7 / MySQL 8.0 / Redis 7.x（ES 未部署）

## 复现命令

```bash
cd springboot
mvn test -Dtest=BenchmarkRunner          # 三个实验全部运行
mvn test -Dtest=BenchmarkRunner#experimentA_contextCompression
mvn test -Dtest=BenchmarkRunner#experimentB_firstTokenLatency
mvn test -Dtest=BenchmarkRunner#experimentC_retrievalLatency
```

要求：`.env` 中配置有效的 `DEEPSEEK_API_KEY`、MySQL 连接信息；数据库 `knowledge_chunks` 表有数据（测试数据 `ORDER BY RAND() LIMIT 60` 采样）。

## 实验 A：上下文压缩（真实 LLM 摘要）

**方法**：从 MySQL 真实 `knowledge_chunks`（60 条）采样构造 20~50 轮"诊断→规划→答疑"长对话；`ContextWindow(30720, 0.7)`（生产默认窗口，70% 阈值触发）；分别走 LLM 结构化摘要路径（真实 DeepSeek 调用）与规则截断降级路径。

| 对话轮数 | 压缩前 Token | 使用率 | 触发 | LLM 摘要后 Token | LLM 路径压缩率 | 规则截断压缩率 |
|---|---|---|---|---|---|---|
| 20 | 10,306 | 33.5% | 否 | - | 0% | 0% |
| 30 | 16,156 | 52.6% | 否 | - | 0% | 0% |
| 40 | 22,499 | 73.2% | **是** | 3,037 | **86.5%** | 89.9% |
| 50 | 28,612 | 93.1% | **是** | 2,837 | **90.1%** | 92.8% |

**结论**：阈值以下不触发（正确性验证）；触发后 Token 压缩率 86.5%~90.1%（LLM 摘要主路径）。

## 实验 B：SSE 流式首字响应（真实 DeepSeek）

**方法**：`ChatClient.stream()` 流式调用真实 DeepSeek（deepseek-chat），计时首个内容片段到达（blockFirst）。

| 次数 | 1 | 2 | 3 | 4 | 5 | min | **avg** | max |
|---|---|---|---|---|---|---|---|---|
| 首字耗时(ms) | 571 | 582 | 681 | 612 | 591 | 571 | **607** | 681 |

**结论**：首字响应平均 607ms，满足 800ms 以内目标。

## 实验 C：RAG 检索耗时（ES 不可达 → 降级路径实测）

**环境**：ES 未部署（`9200` 不可达），验证三级降级中的降级路径；数据为真实 60 条知识分块。

| 降级层级 | 实现 | 次数 | min | **avg** | p95 | max |
|---|---|---|---|---|---|---|
| 内存关键词 | `InMemoryVectorStoreWrapper`（无 Embedding，纯本地） | 50 | 0ms | **0.0ms** | 0ms | 0ms |
| 数据库关键词 | MySQL `LIKE`（模拟 `KnowledgeService.fallbackKeywordSearch` 真实 SQL） | 50 | 0ms | **0.1ms** | 1ms | 2ms |
| 向量路径（Embedding 故障） | 尝试 DeepSeek Embedding 失败（404）→ 自动降级关键词 | 25 | 227ms | 299ms | 407ms | 413ms |

**结论**：
- 关键词降级路径亚毫秒级，ES 故障时服务不中断（降级机制真实触发）；
- DeepSeek 官方 API 无 Embedding 接口（404），运行时自动降级为关键词检索并正常返回结果（`hits=5`），日志可见降级链路：`Embedding 生成失败 → 向量检索失败 → 关键词降级`。

## 回归测试基线

`mvn clean test`：24 个测试类 / 135 个用例全部通过（0 失败 0 错误）。

## 面试口径（诚实表述）

- 上下文压缩：对比实验实测 Token 压缩 **86.5%~90.1%**（真实 LLM + 真实知识库数据）；
- 首字响应：实测 **avg 607ms**（真实流式调用 5 次）；
- 检索：ES 不可达降级路径实测 **<1ms**（内存关键词 / MySQL 关键词）；向量链路因 Embedding 接口不可用自动降级，服务不中断；
- 未声称的数据：长链任务"完成率提升"、真实用户"继续学习率"——本环境无真实用户与规模化评测集，未做实验即不写入。