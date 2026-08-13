# AI 学习规划师：基于 Spring AI + MCP + Agent 的智能学习平台

  **技术栈**：Spring Boot 3.5 / Spring AI 1.1.7 / Vue 3 / **Elasticsearch** + Redis / MCP / ReAct Agent  
  **大模型支持**：Qwen-Max、DeepSeek-V4-Flush、小米 MiMo-V2.5-Pro（动态切换）

---

## 目录

1. [项目概述](#1-项目概述)
2. [详细需求功能文档](#2-详细需求功能文档)
3. [超级智能体模块（自主规划核心）](#3-超级智能体模块自主规划核心)
4. [系统架构设计](#4-系统架构设计)
5. [技术栈与大模型接入](#5-技术栈与大模型接入)
6. [核心技术实现要点](#6-核心技术实现要点)
7. [项目结构（Web 开发版）](#7-项目结构web-开发版)
8. [项目特色亮点总结](#8-项目特色亮点总结)
9. [附录：迁移与扩展指南](#9-附录迁移与扩展指南)

---

  ## 1. 项目概述

  ### 1.1 项目背景

  在信息爆炸的时代，学习者普遍面临知识体系碎片化、缺乏系统路径、自学效率低等问题。本项目旨在构建一个以 **AI 学习规划师** 为核心的智能学习平台，通过大模型与智能算法，精准诊断用户学习水平，动态生成专属学习路径，并提供7x24小时智能辅导与监督。

  ### 1.2 核心价值

  - **个性化**：基于用户画像与知识图谱，生成千人千面的学习路径
  - **自主规划**：智能体能够自主分解任务、调用工具、迭代优化计划
  - **引导式教学**：采用苏格拉底式提问，培养独立思考能力
  - **全周期陪伴**：从诊断、规划、学习到报告，覆盖完整学习闭环

  ### 1.3 核心能力矩阵

| 能力维度       | 描述                               | 核心技术            |
| :------------- | :--------------------------------- | :------------------ |
| 动态知识推理   | 结合上下文进行逻辑推导，引导学习者 | RAG + CoT           |
| 长期记忆与画像 | 存储学习轨迹，记录知识盲区与情绪   | Elasticsearch + Redis |
| 多模态理解     | 支持图文、语音、截图输入           | CLIP + Whisper      |
| 自主任务规划   | 目标拆解、工具调用、动态调整       | ReAct + MCP + Agent |

---

  ## 2. 详细需求功能文档

  ### 2.1 用户角色

| 角色       | 描述                       | 核心需求                   |
| :--------- | :------------------------- | :------------------------- |
| 普通学习者 | 需要学习指引的学生或职场人 | 个性化计划、答疑、进度追踪 |
| 系统管理员 | 平台运维                   | 监控、知识库管理           |

  > MVP 阶段仅实现学习者角色。

  ### 2.2 功能模块清单

| 模块编号 | 功能                   | 优先级 |
| :------- | :--------------------- | :----- |
| M1       | 用户管理与画像         | P0     |
| M2       | 智能能力测评           | P0     |
| M3       | 学习目标设定           | P0     |
| M4       | 个性化学习路径生成     | P0     |
  | M5       | 资源智能推荐           | P0     |
  | M6       | 互动答疑（苏格拉底式） | P0     |
  | M7       | 学习进度追踪与看板     | P0     |
  | M8       | 动态路径调整           | P1     |
  | M9       | 自动习题生成           | P1     |
  | M10      | 智能学情报告           | P1     |
  | M11      | 知识图谱可视化         | P2     |
  | **M12**  | **学习干预与激励**     | **P1** |
  | **M13**  | **学习成就与打卡**     | **P2** |

  ### 2.3 关键用例

  #### UC-01：制定学习路径

  1. 用户注册登录 → 完成能力测评 → 输入学习目标
  2. 系统生成候选路径 → 用户确认 → 保存并展示

  #### UC-02：互动答疑

  1. 用户输入问题 → 系统检索知识库（RAG）
  2. 大模型生成引导式回答（不直接给答案）
  3. 支持多轮追问，保存对话历史

  #### UC-03：动态调整路径

  - 若用户某知识点正确率 < 60% → 自动添加前置复习节点
  - 若正确率 > 90% 且耗时低于平均 → 跳过重复练习

  #### UC-04（新增）：学习干预

  - 系统监测到用户连续3次答题错误或学习时长过短 → 主动发送鼓励消息并推荐更简单的学习资料或调整计划

  ### 2.4 核心数据模型（Elasticsearch 索引设计）

  本项目采用 Elasticsearch 作为主数据存储，不再使用关系型表。数据按业务聚合设计为以下索引（Index）：

| 索引名            | 说明                                                         | 主要字段（Mapping）                                                                 |
| :---------------- | :----------------------------------------------------------- | :---------------------------------------------------------------------------------- |
| `users`           | 用户基础信息 + 画像（合并）                                  | `id`(keyword), `username`, `password_hash`, `email`, `learning_style`, `level`, `active_hours` |
| `knowledge_nodes` | 知识图谱节点，包含嵌入向量用于语义检索                       | `id`(keyword), `name`(text), `prerequisites`(keyword数组), `difficulty`(integer), `estimated_hours`(float), `embedding`(dense_vector) |
| `learning_paths`  | 用户学习路径，包含节点序列（嵌套对象）                       | `id`(keyword), `user_id`(keyword), `node_sequence`(nested), `version`(integer)      |
| `learning_records`| 学习记录，按时间分索引（如 `records-2026.07`）便于滚动       | `user_id`(keyword), `node_id`(keyword), `status`(keyword), `mastery`(float), `time_spent`(float), `created_at`(date) |
| `resources`       | 学习资源，支持全文搜索                                       | `id`(keyword), `title`(text), `type`(keyword), `url`, `node_id`(keyword), `avg_rating`(float) |
| `chat_histories`  | 对话历史，按 `session_id` 路由                               | `session_id`(keyword), `role`(keyword), `content`(text), `created_at`(date)         |
| **`interventions`**| **学习干预记录**                                             | `user_id`(keyword), `type`(keyword), `trigger`(text), `content`(text), `created_at`(date) |
| **`achievements`**| **学习成就与打卡**                                           | `user_id`(keyword), `badge`(keyword), `unlocked_at`(date)                           |

  > **说明**：Elasticsearch 为 NoSQL 文档存储，采用“宽表”设计，将关联数据冗余存储，减少跨索引查询。对于依赖关系（如 prerequisites）使用数组字段，支持 `terms` 查询。

  ### 2.5 详细功能规格说明（仅列新增及关键变更）

  #### M12 学习干预与激励（新增）

  | 项目         | 内容                                                         |
  | :----------- | :----------------------------------------------------------- |
  | **功能描述** | 系统自动监测学习行为，当检测到挫败信号（如连续错误、长时间停滞）时，主动提供鼓励、调整计划或推荐更简单内容。 |
  | **前置条件** | 用户有学习记录。                                             |
  | **后置条件** | 干预记录存储到 `interventions` 索引，并可能触发路径调整。    |
  | **输入**     | 用户学习行为数据（正确率、时长、情绪指标）。                 |
  | **输出**     | 干预消息（系统通知或聊天消息）。                             |
  | **业务规则** | 1. 连续3次答题错误 → 发送鼓励语并推荐前置知识点； 2. 当日学习时长<10分钟 → 推送学习提醒； 3. 每周干预次数≤3次。 |
  | **异常处理** | 干预过于频繁 → 暂停自动干预，提醒用户可主动寻求帮助。        |

  #### M13 学习成就与打卡（新增）

  | 项目         | 内容                                                         |
  | :----------- | :----------------------------------------------------------- |
  | **功能描述** | 为用户设立学习里程碑（如完成第一个知识点、连续学习7天），解锁徽章，增加学习趣味性。 |
  | **前置条件** | 用户有学习行为。                                             |
  | **后置条件** | 成就记录存储到 `achievements` 索引。                         |
  | **输入**     | 学习记录、打卡行为。                                         |
  | **输出**     | 成就解锁通知及徽章展示。                                     |
  | **业务规则** | 1. 完成路径中20%节点 → 解锁“初露锋芒”； 2. 连续7天学习 → 解锁“持之以恒”； 3. 掌握度≥90%知识点达到5个 → 解锁“学霸”徽章。 |
  | **异常处理** | 成就重复触发 → 去重处理。                                    |

---

  ## 3. 超级智能体模块（自主规划核心）

  ### 3.1 模块定位与设计理念

  超级智能体模块是系统的 **“大脑”** ，它将传统的“问答式AI”升级为“**代理式AI（Agentic AI）**”，使系统具备独立的思考、规划与执行能力。

  **设计理念**：

  - 模拟人类解决问题的思维方式：理解目标 → 分解任务 → 执行操作 → 观察结果 → 调整策略
  - 将 LLM 作为推理引擎，而非仅作为对话模型
  - 通过 ReAct（Reasoning + Acting）模式实现“边思考边行动”的闭环

  **与普通 AI 助手的区别**：

  | 对比维度 | 普通 AI 助手       | 超级智能体                     |
  | :------- | :----------------- | :----------------------------- |
  | 交互方式 | 单轮问答           | 多轮推理-行动循环              |
  | 任务处理 | 被动回答问题       | 主动分解并执行复杂任务         |
  | 工具使用 | 人工指定           | 自主发现和调用                 |
  | 计划调整 | 需人工干预         | 基于观察结果自动调整           |
  | 适用场景 | 信息查询、简单问答 | 任务规划、多步骤执行、动态决策 |

  ### 3.2 核心能力

  #### 3.2.1 目标理解与自主任务分解

  超级智能体能够接收高层次、模糊的用户目标，并自主将其拆解为可执行的子任务序列。

  **示例**：

  > 用户输入：“帮我制定一个 3 个月学会 Python 数据分析和机器学习的计划”

  智能体自主分解为：
  1. 获取用户当前水平（调用诊断Agent）
  2. 构建知识图谱（调用知识库检索）
  3. 识别前置依赖关系（分析依赖）
  4. 生成学习路径（调用规划Agent）
  5. 推荐学习资源（调用资源检索）
  6. 制定时间表（生成日历计划）

  #### 3.2.2 推理-行动-观察闭环（ReAct）

  超级智能体内置 **ReAct（Reasoning + Acting）** 循环：
  ```
  ┌─────────────────────────────────────────────────────────┐
  │ ReAct 循环 │
  │ │
  │ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
  │ │ 推理 │ ──▶│ 行动 │ ──▶│ 观察 │ ──┐ │
  │ │(Think) │ │(Act) │ │(Observe) │ │ │
  │ └─────────┘ └─────────┘ └─────────┘ │ │
  │ ▲ │ │
  │ └─────────────────────────────────────────┘ │
  │ │
  │ - 推理：当前状态是什么？下一步该做什么？ │
  │ - 行动：调用工具、查询数据、执行操作 │
  │ - 观察：分析行动结果，判断是否达成目标 │
  │ - 迭代：基于观察结果更新状态，进入下一轮 │
  └─────────────────────────────────────────────────────────┘
  ```
  #### 3.2.3 工具发现与智能调用

  通过 MCP 协议，超级智能体能够：
  - 动态发现可用工具（工具注册与发现机制）
  - 根据任务需求自主选择工具（工具选择策略）
  - 按顺序组合调用多个工具（工具编排能力）

  **工具集（借鉴开源项目扩展）**：

  | 工具名称               | 功能描述                         | MCP Server               |
  | :--------------------- | :------------------------------- | :----------------------- |
  | `query_knowledge_graph`| 知识图谱节点查询                 | `knowledge-mcp`          |
  | `search_resources`     | 检索学习资源                     | `resource-mcp`           |
  | `get_user_profile`     | 获取用户画像                     | `user-mcp`               |
  | `terminate`            | 任务完成终止                     | `core-mcp`               |
  | `send_email`           | 发送学习提醒或报告               | `notification-mcp`       |
  | `generate_pdf_report`  | 生成学习报告PDF                  | `report-mcp`             |
  | `web_search`           | 联网搜索补充资料                 | `search-mcp`             |
  | `web_fetch`            | 抓取指定网页内容                 | `search-mcp`             |
  | `file_operation`       | 上传/下载学习资料                 | `file-mcp`               |
  | `todo_manager`         | 管理学习任务清单                 | `todo-mcp`               |
  | `intervention_trigger` | 主动干预用户（发消息/推荐内容）  | `intervention-mcp`       |
  | `achievement_unlock`   | 解锁学习成就                     | `gamification-mcp`       |

  #### 3.2.4 多智能体协同

  超级智能体作为主控 Agent，协调多个专业化子 Agent 协同工作：

  | 子 Agent  | 职责                   | 调用时机                       |
  | :-------- | :--------------------- | :----------------------------- |
  | 诊断Agent | 用户能力测评、画像构建 | 用户首次进入或主动请求测评     |
  | 规划Agent | 学习路径生成与动态调整 | 用户设定目标后、学习表现变化时 |
  | 答疑Agent | 苏格拉底式引导问答     | 用户提问时                     |
  | 报告Agent | 学情报告生成           | 定期生成或用户主动请求         |
  | 搜索Agent | 联网搜索补充信息       | 本地知识库不足时               |
  | **干预Agent** | 学习行为监测与主动干预 | 定时触发或行为触发             |
  | **激励Agent** | 成就解锁与打卡管理     | 学习里程碑达成时               |

  #### 3.2.5 长链推理与动态调整

  参考 ExpertAgent 框架的设计，超级智能体具备 **长链推理（Long-chain Reasoning）** 能力：

  - 基于持续更新的用户模型动态规划学习内容
  - 在实时交互中不断优化行动策略
  - 克服传统静态规划的局限性

  ### 3.3 模块内部架构
  ```
  ┌─────────────────────────────────────────────────────────────────┐
  │ 🧠 超级智能体模块架构 │
  ├─────────────────────────────────────────────────────────────────┤
  │ │
  │ ┌────────────────────────────────────────────────────────────┐ │
  │ │ 主控 Agent（Orchestrator） │ │
  │ │ · 意图识别 · 任务分解 · 子Agent调度 · 上下文管理 │ │
  │ └────────────────────────────────────────────────────────────┘ │
  │ │ │
  │ ┌────────────────────────────────────────────────────────────┐ │
  │ │ ReAct 循环引擎 │ │
  │ │ ┌──────────┐ ┌──────────┐ ┌──────────┐ │ │
  │ │ │ 推理引擎 │→│ 行动执行 │→│ 观察分析 │ ──┐ │ │
  │ │ │ (Think) │ │ (Act) │ │ (Observe) │ │ │ │
  │ │ └──────────┘ └──────────┘ └──────────┘ │ │ │
  │ │ ▲ │ │ │
  │ │ └──────────────────────────────────────┘ │ │
  │ └────────────────────────────────────────────────────────────┘ │
  │ │ │
  │ ┌────────────────────────────────────────────────────────────┐ │
  │ │ 子 Agent 矩阵 │ │
  │ │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │ │
  │ │ │诊断Agent │ │规划Agent │ │答疑Agent │ │报告Agent │ │ │
  │ │ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │ │
  │ │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │ │
  │ │ │搜索Agent │ │干预Agent │ │激励Agent │ │代码Agent │ │ │
  │ │ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │ │
  │ └────────────────────────────────────────────────────────────┘ │
  │ │ │
  │ ┌────────────────────────────────────────────────────────────┐ │
  │ │ MCP 工具注册表 │ │
  │ │ · 知识图谱查询 · 资源检索 · 联网搜索 · 邮件发送 │ │
  │ │ · PDF生成 · 文件操作 · 待办管理 · 干预触发 │ │
  │ │ · 成就解锁 · 代码执行 · 数据库查询 · 定时任务 │ │
  │ └────────────────────────────────────────────────────────────┘ │
  │ │
  └─────────────────────────────────────────────────────────────────┘
  ```
  ### 3.4 ReAct 循环引擎详解

  #### 3.4.1 循环执行流程

  ```java
  public Mono<String> run(String input) {
      return Flux.range(0, MAX_ITERATIONS)           // 1. 限制最大轮数
          .flatMap(iteration -> 
              // 2. 推理阶段：调用 LLM 分析当前状态
              think(input, history)
                  .flatMap(thought -> 
                      // 3. 行动阶段：执行 LLM 决定的动作
                      act(thought)
                          .flatMap(actionResult -> 
                              // 4. 观察阶段：分析执行结果
                              observe(actionResult)
                          )
                  )
          )
          .takeUntil(result -> result.isTerminated()) // 5. 终止条件检测
          .collectList()
          .map(results -> buildFinalAnswer(results)); // 6. 汇总输出
  }
  ```

  

  #### 3.4.2 各阶段实现细节

  **① 推理阶段（Think）**：

  ```java
  private Mono<Thought> think(String userInput, List<Message> history) {
      String thinkPrompt = """
          你是一个智能学习规划师，请根据当前状态决定下一步行动。
          
          【用户原始需求】
          %s
          
          【当前执行状态】
          %s
          
          【已完成的步骤】
          %s
          
          【可用工具列表】
          %s
          
          请以 JSON 格式输出你的思考结果：
          {
              "reasoning": "你的推理过程",
              "action": "要执行的工具名称",
              "action_input": "工具参数",
              "thought": "你对当前情况的判断"
          }
          """.formatted(userInput, currentState, history, toolList);
      
      return chatModel.call(thinkPrompt)
          .map(this::parseThought);
  }
  ```

  

  **② 行动阶段（Act）**：

  ```java
  private Mono<ActionResult> act(Thought thought) {
      if ("terminate".equals(thought.getAction())) {
          return Mono.just(ActionResult.terminate(thought.getThought()));
      }
      
      McpTool tool = mcpRegistry.getTool(thought.getAction());
      if (tool == null) {
          return Mono.just(ActionResult.error("工具不存在: " + thought.getAction()));
      }
      
      return tool.execute(thought.getActionInput())
          .map(result -> ActionResult.success(result))
          .onErrorResume(e -> Mono.just(ActionResult.error(e.getMessage())));
  }
  ```

  

  **③ 观察阶段（Observe）**：

  ```java
  private Mono<Observation> observe(ActionResult result) {
      if (result.isTerminated()) {
          return Mono.just(Observation.terminated());
      }
      
      if (result.isError()) {
          return Mono.just(Observation.error(result.getErrorMessage()));
      }
      
      String observePrompt = """
          请分析以下工具执行结果，判断任务进展：
          
          【执行结果】
          %s
          
          【当前任务目标】
          %s
          
          请输出 JSON：
          {
              "progress": "已完成/进行中/阻塞",
              "insight": "你对结果的分析",
              "need_retry": false,
              "next_action_suggestion": "后续建议"
          }
          """.formatted(result.getData(), currentGoal);
      
      return chatModel.call(observePrompt)
          .map(this::parseObservation);
  }
  ```

  

  #### 3.4.3 终止条件

  | 终止类型 | 触发条件                       | 处理方式            |
  | :------- | :----------------------------- | :------------------ |
  | 正常完成 | Agent 主动调用 `TerminateTool` | 输出最终结果        |
  | 最大迭代 | 达到 MAX_ITERATIONS 上限       | 输出部分结果 + 提示 |
  | 重复检测 | 最近 5 步推理内容相似度 > 90%  | 强制注入干预指令    |
  | 时间超时 | 单步执行超过 30 秒             | 中断当前步骤        |

  ### 3.5 多智能体协同机制

  #### 3.5.1 主控 Agent 的调度逻辑

  ```java
  @Service
  public class OrchestratorAgent {
      
      private final Map<String, SubAgent> subAgents;
      private final ReActEngine reactEngine;
      
      public Mono<String> orchestrate(String userInput, String sessionId) {
          String intent = identifyIntent(userInput);
          
          return switch (intent) {
              case "diagnosis" -> handleDiagnosis(userInput, sessionId);
              case "planning" -> handlePlanning(userInput, sessionId);
              case "qa" -> handleQA(userInput, sessionId);
              case "report" -> handleReport(userInput, sessionId);
              case "intervention" -> handleIntervention(userInput, sessionId);
              default -> handleUnknown(userInput, sessionId);
          };
      }
      
      private Mono<String> handlePlanning(String userInput, String sessionId) {
          return reactEngine.run(userInput)
              .flatMap(result -> {
                  if (result.needDiagnosis()) {
                      return subAgents.get("diagnosis").process(userInput, sessionId)
                          .thenReturn(result);
                  }
                  return Mono.just(result);
              })
              .flatMap(result -> {
                  if (result.needPath()) {
                      return subAgents.get("planning").process(result.getData(), sessionId)
                          .map(path -> result.withPath(path));
                  }
                  return Mono.just(result);
              });
      }
  }
  ```

  

  #### 3.5.2 子 Agent 定义

  ```java
  public interface SubAgent {
      String getName();
      Mono<String> process(String input, String sessionId);
      List<McpTool> getTools();
  }
  
  @Component
  public class InterventionAgent implements SubAgent {
      @Override
      public String getName() { return "intervention"; }
      
      @Override
      public Mono<String> process(String input, String sessionId) {
          // 分析用户行为，生成干预策略
          return analyzeBehavior(sessionId)
              .flatMap(strategy -> sendIntervention(strategy));
      }
  }
  ```

  

  ### 3.6 工具调用与 MCP 集成

  #### 3.6.1 MCP 工具注册

  ```java
  @SpringBootApplication
  @McpServer
  public class LearningPlannerApplication {
      
      @McpTool(name = "query_knowledge_graph", description = "查询知识图谱中的节点及其依赖关系")
      public KnowledgeGraphResult queryGraph(@ToolParam String nodeId, @ToolParam Integer depth) {
          return knowledgeService.queryGraph(nodeId, depth);
      }
      
      @McpTool(name = "web_search", description = "联网搜索学习资料")
      public String webSearch(@ToolParam String query) {
          return searchService.searchWeb(query);
      }
      
      @McpTool(name = "send_email", description = "发送学习提醒邮件")
      public void sendEmail(@ToolParam String to, @ToolParam String subject, @ToolParam String body) {
          mailService.send(to, subject, body);
      }
      
      @McpTool(name = "intervention_trigger", description = "向用户发送干预消息")
      public String triggerIntervention(@ToolParam String userId, @ToolParam String message) {
          return interventionService.sendIntervention(userId, message);
      }
      
      @McpTool(name = "terminate", description = "任务完成时调用")
      public String terminate(@ToolParam String summary) {
          return "TASK_COMPLETED: " + summary;
      }
  }
  ```

  

  #### 3.6.2 MCP 客户端配置

  ```yaml
  spring:
    ai:
      mcp:
        client:
          enabled: true
          servers:
            knowledge-server:
              url: http://localhost:8081/mcp/knowledge
              transport: STREAMABLE
            resource-server:
              url: http://localhost:8081/mcp/resource
              transport: STREAMABLE
            search-server:
              url: http://localhost:8081/mcp/search
              transport: SSE
            mail-server:
              url: http://localhost:8081/mcp/mail
              transport: STREAMABLE
            pdf-server:
              url: http://localhost:8081/mcp/pdf
              transport: STREAMABLE
            intervention-server:
              url: http://localhost:8081/mcp/intervention
              transport: STREAMABLE
            gamification-server:
              url: http://localhost:8081/mcp/gamification
              transport: STREAMABLE
  ```

  

  #### 3.6.3 工具调用流程

  ```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                        工具调用流程                              │
  ├─────────────────────────────────────────────────────────────────┤
  │  1. Agent 推理阶段决定调用工具                                     │
  │     ↓                                                            │
  │  2. 从 MCP 注册表中查找匹配的工具                                  │
  │     ↓                                                            │
  │  3. 生成工具调用参数（由LLM填充）                                 │
  │     ↓                                                            │
  │  4. 通过 MCP 协议发送调用请求                                     │
  │     ↓                                                            │
  │  5. MCP Server 执行工具逻辑                                       │
  │     ↓                                                            │
  │  6. 返回执行结果给 Agent                                         │
  │     ↓                                                            │
  │  7. Agent 分析结果，决定下一步                                    │
  └─────────────────────────────────────────────────────────────────┘
  ```

  

  ### 3.7 死循环防治方案

  #### 3.7.1 问题识别

  Agent 死循环的典型表现：

  - 反复调用相同工具并传入相同参数
  - 推理内容重复，无实质进展
  - 状态卡在某一阶段无法推进

  #### 3.7.2 防治策略

  **策略一：显式终止工具**

  ```java
  @McpTool(name = "terminate", description = "完成任务时调用")
  public String terminate(@ToolParam String summary) {
      return "COMPLETED: " + summary;
  }
  ```

  

  **策略二：重复检测机制**

  ```java
  @Component
  public class LoopDetector {
      private static final int WINDOW_SIZE = 5;
      private static final double SIMILARITY_THRESHOLD = 0.9;
      private final Queue<String> recentActions = new LinkedList<>();
      
      public boolean isLoopDetected(String currentAction) {
          recentActions.offer(currentAction);
          if (recentActions.size() > WINDOW_SIZE) recentActions.poll();
          if (recentActions.size() == WINDOW_SIZE) {
              double similarity = calculateSimilarity(recentActions);
              if (similarity > SIMILARITY_THRESHOLD) return true;
          }
          return false;
      }
  }
  ```

  

  **策略三：状态流转规则**

  ```java
  private static final String SYSTEM_PROMPT = """
      你是一个智能学习规划师，每次执行任务时，必须明确你的状态：
      
      状态流转规则：
      1. 初始状态: "planning" - 正在规划如何完成任务
      2. 执行状态: "executing" - 正在执行具体步骤
      3. 验证状态: "verifying" - 验证执行结果
      4. 完成状态: "completed" - 任务已完成
      
      你必须在每次推理输出中包含当前状态，且状态不能回退！
      （例如：不能从 "executing" 退回到 "planning"）
      
      如果状态卡在同一阶段超过 3 轮，你必须主动调整策略。
      """;
  ```

  

  **策略四：最大迭代限制**

  ```java
  public class ReActAgent {
      private static final int MAX_ITERATIONS = 30;
      public Mono<String> run(String input) {
          return Flux.range(0, MAX_ITERATIONS)
              .flatMap(iteration -> executeStep(input))
              .takeUntil(result -> result.isTerminated())
              .collectList()
              .map(results -> {
                  if (results.size() >= MAX_ITERATIONS) {
                      return buildPartialResult(results);
                  }
                  return buildFinalAnswer(results);
              });
      }
  }
  ```

  

  ### 3.8 核心代码实现

  #### 3.8.1 ReAct Agent 完整实现

  ```java
  @Component
  @Slf4j
  public class ReActAgent {
      private final ChatModel chatModel;
      private final McpClient mcpClient;
      private final LoopDetector loopDetector;
      private static final int MAX_ITERATIONS = 30;
      
      public Mono<String> run(String userInput) {
          ReActContext context = ReActContext.builder()
              .userInput(userInput)
              .status("planning")
              .history(new ArrayList<>())
              .build();
          
          return Flux.range(0, MAX_ITERATIONS)
              .flatMap(iteration -> executeStep(context, iteration))
              .takeUntil(result -> result.isTerminated())
              .collectList()
              .map(results -> buildFinalAnswer(results, context));
      }
      
      private Mono<StepResult> executeStep(ReActContext context, int iteration) {
          if (loopDetector.isLoopDetected(context.getHistory())) {
              log.warn("检测到死循环，强制执行干预");
              return injectIntervention(context);
          }
          
          return think(context)
              .flatMap(thought -> {
                  if ("terminate".equals(thought.getAction())) {
                      return Mono.just(StepResult.terminated(thought));
                  }
                  return act(thought)
                      .flatMap(actionResult -> observe(actionResult, context)
                          .map(observation -> {
                              context.update(thought, actionResult, observation);
                              return StepResult.continueExecution(observation);
                          }));
              });
      }
      // ... 其他私有方法
  }
  ```

  

  #### 3.8.2 主控 Agent 完整实现

  ```java
  @Service
  @Slf4j
  public class OrchestratorAgent {
      private final ReActAgent reactAgent;
      private final Map<String, SubAgent> subAgents;
      private final ChatModel chatModel;
      
      public Mono<String> process(String userInput, String sessionId) {
          return identifyIntent(userInput)
              .flatMap(intent -> {
                  log.info("识别到意图: {}", intent);
                  switch (intent) {
                      case "diagnosis": return subAgents.get("diagnosis").process(userInput, sessionId);
                      case "planning": return handlePlanning(userInput, sessionId);
                      case "qa": return subAgents.get("qa").process(userInput, sessionId);
                      case "report": return subAgents.get("report").process(userInput, sessionId);
                      case "intervention": return subAgents.get("intervention").process(userInput, sessionId);
                      default: return reactAgent.run(userInput);
                  }
              });
      }
      // ... 其他方法
  }
  ```

  

  ### 3.9 模块特性总结

  | 特性         | 描述                                           |
  | :----------- | :--------------------------------------------- |
  | **自主性**   | 无需人工干预，自动分解任务、选择工具、调整计划 |
  | **可扩展性** | 新增能力只需添加 MCP Server 或子Agent          |
  | **可观测性** | 每次推理和行动步骤均可记录，便于调试           |
  | **容错性**   | 内置重试、超时、回退策略，保证系统鲁棒性       |
  | **安全性**   | 工具调用受权限控制，敏感操作需确认             |
  | **灵活性**   | 支持同步/异步执行，可配置最大迭代次数          |

  ### 3.10 与学习规划师的协作关系

  ```text
  ┌─────────────────────────────────────────────────────────────────┐
  │                  学习规划师 ↔ 超级智能体 协作                      │
  ├─────────────────────────────────────────────────────────────────┤
  │                                                                  │
  │  ┌─────────────────────┐         ┌─────────────────────────┐   │
  │  │    AI学习规划师       │         │     超级智能体           │   │
  │  │    （传统功能层）     │ ◀─────▶ │     （自主规划层）       │   │
  │  ├─────────────────────┤         ├─────────────────────────┤   │
  │  │  · 用户画像管理      │         │  · 意图识别             │   │
  │  │  · 能力测评          │         │  · 任务分解             │   │
  │  │  · 路径生成          │         │  · 工具调用             │   │
  │  │  · 资源推荐          │         │  · 多Agent协同          │   │
  │  │  · 进度看板          │         │  · 动态调整             │   │
  │  │  · 学情报告          │         │  · 死循环防治           │   │
  │  └─────────────────────┘         └─────────────────────────┘   │
  │            │                               │                    │
  │            └───────────────┬───────────────┘                    │
  │                            │                                    │
  │                  ┌─────────────────────┐                       │
  │                  │   MCP 协议 + 工具层  │                       │
  │                  │  （能力基础设施）     │                       │
  │                  └─────────────────────┘                       │
  └─────────────────────────────────────────────────────────────────┘
  
  协作模式：
  1. 简单任务：由学习规划师直接处理（如查询进度、查看报告）
  2. 复杂任务：由超级智能体自主规划执行（如制定完整学习计划）
  3. 混合模式：超级智能体调用学习规划师的功能作为子任务
  ```

  

------

  ## 4. 系统架构设计

  ### 4.1 整体分层架构

  ```text
  ┌─────────────────────────────────────────────────────────────┐
  │                        客户端（Web）                         │
  │               Vue3 + SSE/WebSocket + REST API               │
  ├─────────────────────────────────────────────────────────────┤
  │                    网关与安全层                               │
  │         前置拦截器 · 安全守卫 · CORS · 日志                   │
  ├─────────────────────────────────────────────────────────────┤
  │                       应用层                                  │
  │              AI学习规划师 · AI超级智能体                      │
  ├─────────────────────────────────────────────────────────────┤
  │                   Spring AI 核心服务层                        │
  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐    │
  │  │ChatMemory│ │   RAG    │ │  Tools   │ │MCP Client    │    │
  │  │ (Redis)  │ │(ES向量)  │ │(Function)│ │(外部工具)     │    │
  │  └──────────┘ └──────────┘ └──────────┘ └──────────────┘    │
  ├─────────────────────────────────────────────────────────────┤
  │                    智能体编排层                               │
  │  主控Agent · 诊断Agent · 规划Agent · 答疑Agent · 报告Agent   │
  │  · 干预Agent · 激励Agent                                    │
  ├─────────────────────────────────────────────────────────────┤
  │                      基础设施层                               │
  │   Elasticsearch · Redis · Docker/Serverless · 大模型API      │
  └─────────────────────────────────────────────────────────────┘
  ```

  

  ### 4.2 多智能体协作架构

  | 智能体        | 职责                            | 可用工具               |
  | :------------ | :------------------------------ | :--------------------- |
  | 主控Agent     | 意图识别、任务分解、子Agent调度 | TaskTool、ChatMemory   |
  | 诊断Agent     | 能力测评、画像构建、薄弱点挖掘  | 测评工具库             |
  | 规划Agent     | 路径生成与动态调整、资源推荐    | 知识图谱查询、路径优化 |
  | 答疑Agent     | 苏格拉底式引导、RAG检索         | 知识库检索、引导模板   |
  | 报告Agent     | 学情分析、报告生成              | 数据聚合、PDF导出      |
  | **干预Agent** | **学习行为监测与主动干预**      | **干预工具、消息发送** |
  | **激励Agent** | **成就解锁与打卡管理**          | **成就工具、打卡记录** |

  ### 4.3 MCP 协议集成架构

  ```text
  Spring AI 应用
       │
       ▼
  MCP Client (STDIO/SSE/Streamable HTTP)
       │
       ├── Knowledge MCP Server (知识图谱查询)
       ├── Resource MCP Server (学习资源检索)
       ├── Search MCP Server (联网搜索)
       ├── File MCP Server (文件操作)
       ├── Mail MCP Server (邮件通知)
       ├── PDF MCP Server (报告生成)
       ├── Todo MCP Server (任务管理)
       ├── Intervention MCP Server (干预管理)
       └── Gamification MCP Server (成就/打卡)
  ```

  

------

  ## 5. 技术栈与大模型接入

  ### 5.1 完整技术栈明细

  | 分类           | 技术组件                                              | 版本       | 说明                     |
  | :------------- | :---------------------------------------------------- | :--------- | :----------------------- |
  | **后端框架**   | Spring Boot                                           | 3.5.x      | Java应用框架             |
  | **AI 核心**    | Spring AI                                             | 1.1.7      | 统一AI模型调用与工具集成 |
  | **大模型**     | Qwen-Max / DeepSeek-V4-Flush / MiMo-V2.5-Pro / Ollama | -          | 动态切换                 |
  | **MCP协议**    | Spring AI MCP Boot Starters                           | 1.1.x      | 模型上下文协议           |
  | **搜索与向量** | **Elasticsearch**（含向量插件）                       | 8.10+      | 全文检索 + 向量相似度    |
  | **对话记忆**   | Redis                                                 | 7.x        | 多轮对话缓存             |
  | **前端框架**   | Vue 3 + Vite                                          | 3.4+ / 5.x | 现代前端                 |
  | **UI 组件库**  | Element Plus                                          | 2.x        | 企业级组件               |
  | **状态管理**   | Pinia                                                 | 2.x        | Vue3 状态管理            |
  | **流式通信**   | Server-Sent Events (SSE)                              | -          | AI打字机效果             |

  ### 5.2 大模型选型与对比

  | 模型                  | 提供商           | 接入方式        | 适用场景                     | 推荐程度 |
  | :-------------------- | :--------------- | :-------------- | :--------------------------- | :------- |
  | **Qwen-Max**          | 阿里云 DashScope | OpenAI 兼容协议 | 生产主力，中文理解优秀       | ⭐⭐⭐⭐⭐    |
  | **DeepSeek-V4-Flush** | 深度求索         | OpenAI 兼容协议 | 高性价比、长上下文、快速响应 | ⭐⭐⭐⭐⭐    |
  | **MiMo-V2.5-Pro**     | 小米             | OpenAI 兼容协议 | 复杂推理、多Agent任务        | ⭐⭐⭐⭐     |
  | **Ollama 本地模型**   | 开源社区         | Ollama HTTP     | 开发测试、离线环境           | ⭐⭐⭐      |

  ### 5.3 四种模型 API 接入实现

  #### 5.3.1 API Key 申请指南

  | 模型      | 申请地址                                          | 说明                         |
  | :-------- | :------------------------------------------------ | :--------------------------- |
  | Qwen-Max  | [阿里云百炼](https://bailian.console.aliyun.com/) | 创建 API Key，保存 AccessKey |
  | DeepSeek  | [DeepSeek平台](https://platform.deepseek.com/)    | 注册后创建 API Key           |
  | 小米 MiMo | 内部申请（限时）                                  | 已有 API Key 可直接使用      |
  | Ollama    | 本地安装                                          | 无需 API Key                 |

  #### 5.3.2 Maven 依赖

  ```xml
  <dependencyManagement>
      <dependencies>
          <dependency>
              <groupId>org.springframework.ai</groupId>
              <artifactId>spring-ai-bom</artifactId>
              <version>1.1.7</version>
              <type>pom</type>
              <scope>import</scope>
          </dependency>
      </dependencies>
  </dependencyManagement>
  
  <dependencies>
      <!-- OpenAI 兼容模式（用于 Qwen / DeepSeek / MiMo） -->
      <dependency>
          <groupId>org.springframework.ai</groupId>
          <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
      </dependency>
      <!-- Ollama 本地模型 -->
      <dependency>
          <groupId>org.springframework.ai</groupId>
          <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
      </dependency>
      <!-- Elasticsearch 集成 -->
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
      </dependency>
      <dependency>
          <groupId>org.springframework.ai</groupId>
          <artifactId>spring-ai-elasticsearch-store</artifactId>
          <version>1.1.7</version>
      </dependency>
      <!-- Web 支持 -->
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-web</artifactId>
      </dependency>
  </dependencies>
  ```

  

  #### 5.3.3 application.yml 统一配置

  ```yaml
  spring:
    application:
      name: ai-learning-planner
    ai:
      model:
        provider: qwen
      openai:
        api-key: ${MODEL_API_KEY}
        base-url: ${MODEL_BASE_URL}
        chat:
          options:
            model: ${MODEL_NAME}
            temperature: 0.7
            max-tokens: 2048
      ollama:
        base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
        chat:
          options:
            model: ${OLLAMA_MODEL:deepseek-r1:7b}
    data:
      elasticsearch:
        client:
          reactive:
            endpoints: ${ELASTICSEARCH_HOST:localhost}:${ELASTICSEARCH_PORT:9200}
            connection-timeout: 5000
            socket-timeout: 30000
        repositories:
          enabled: true
    ai:
      vectorstore:
        elasticsearch:
          index-name: knowledge_vectors
          similarity: cosine
          dimensions: 1536
  ```

  

  #### 5.3.4 各模型具体配置参数

  | 模型               | provider | MODEL_API_KEY     | MODEL_BASE_URL                                   | MODEL_NAME           |
  | :----------------- | :------- | :---------------- | :----------------------------------------------- | :------------------- |
  | Qwen-Max           | qwen     | DashScope API Key | `https://dashscope.aliyuncs.com/compatible-mode` | `qwen-max`           |
  | DeepSeek-V4-Flush  | deepseek | DeepSeek API Key  | `https://api.deepseek.com`                       | `deepseek-v4-flush`  |
  | 小米 MiMo-V2.5-Pro | xiaomi   | 小米 API Key      | `https://api.xiaomimimo.com/v1`                  | `xiaomi/mimo-v2-pro` |
  | Ollama             | ollama   | 无需设置          | `http://localhost:11434`                         | `deepseek-r1:7b`     |

  #### 5.3.5 Java 配置类（动态切换）

  ```java
  @Configuration
  @RefreshScope
  public class AiModelConfig {
  
      @Value("${spring.ai.model.provider:qwen}")
      private String modelProvider;
  
      @Bean
      @Primary
      public ChatModel chatModel(
              @Qualifier("openAiChatModel") ChatModel openAiChatModel,
              @Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
          if ("ollama".equals(modelProvider)) {
              return ollamaChatModel;
          }
          return openAiChatModel;
      }
  
      @Bean
      public ChatModel openAiChatModel() {
          return new OpenAiChatModel(openAiApi());
      }
  
      private OpenAiApi openAiApi() {
          return OpenAiApi.builder()
                  .baseUrl(System.getenv("MODEL_BASE_URL"))
                  .apiKey(System.getenv("MODEL_API_KEY"))
                  .build();
      }
  
      @Bean
      @ConditionalOnProperty(name = "spring.ai.model.provider", havingValue = "ollama")
      public ChatModel ollamaChatModel() {
          return new OllamaChatModel(OllamaApi.builder()
                  .baseUrl(System.getenv("OLLAMA_BASE_URL"))
                  .build());
      }
  }
  ```

  

  ### 5.4 统一对话服务

  ```java
  @Service
  public class LearningPlannerChatService {
  
      private final ChatClient chatClient;
  
      public LearningPlannerChatService(ChatModel chatModel) {
          this.chatClient = ChatClient.builder(chatModel)
              .defaultSystem("你是专业的AI学习规划师，通过苏格拉底式提问引导学习者思考")
              .build();
      }
  
      public String chat(String userMessage) {
          return chatClient.prompt(userMessage).call().content();
      }
  
      public Flux<String> streamChat(String userMessage) {
          return chatClient.prompt(userMessage).stream().content();
      }
  }
  ```

  

------

  ## 6. 核心技术实现要点

  ### 6.1 RAG 检索优化（解决幻觉问题）

  采用 **混合检索 + 重排序 + 答案验证** 策略，底层使用 Elasticsearch：

  - **文档预处理**：语义边界切片 + 元数据标注（来源、难度）
  - **查询增强**：HyDE（生成假设性答案再检索）或查询重写
  - **混合检索**：Elasticsearch 的 `multi_match`（BM25 关键词） + `knn`（向量检索），通过 `script_score` 动态加权
  - **重排序**：使用 `rescore` 功能或应用层 Cross-Encoder 精排
  - **答案验证**：生成后再检索原文核对关键事实

  **Elasticsearch 向量配置**：

  ```json
  {
    "mappings": {
      "properties": {
        "content": { "type": "text" },
        "embedding": {
          "type": "dense_vector",
          "dims": 1536,
          "index": true,
          "similarity": "cosine"
        }
      }
    }
  }
  ```

  

  **查询示例（混合检索）**：

  ```json
  {
    "query": {
      "bool": {
        "should": [
          { "match": { "content": "机器学习" } },
          { "knn": { "field": "embedding", "query_vector": [0.1, -0.2, ...], "k": 10 } }
        ]
      }
    }
  }
  ```

  

  ### 6.2 Agent 死循环解决方案

  （参见 3.7 节）

  ### 6.3 ReAct 模式与分层智能体架构

  （参见 3.4 节）

  ### 6.4 Spring AI 关键特性应用

  - **ChatClient**：统一对话接口，支持流式输出
  - **Advisors**：自定义日志、记忆、护栏校验
  - **Recursive Advisors**：实现工具调用循环和输出验证重试
  - **Prompt 缓存**：降低 90% 成本

  ### 6.5 MCP 客户端与服务端实现

  **MCP 工具定义（扩展）**：

  ```java
  @McpTool(name = "query-resources", description = "查询学习资源")
  public List<Resource> queryResources(@ToolParam String nodeId) {
      return resourceRepository.findByNodeId(nodeId);
  }
  
  @McpTool(name = "send_email", description = "发送学习提醒")
  public void sendEmail(@ToolParam String to, @ToolParam String subject, @ToolParam String body) {
      mailService.send(to, subject, body);
  }
  ```

  

  **MCP 客户端配置**（参见 3.6.2）

  ### 6.6 Elasticsearch 集成配置

  **依赖**（已在 5.3.2 中包含）
  **实体类示例**：

  ```java
  @Document(indexName = "knowledge_nodes")
  public class KnowledgeNode {
      @Id
      private String id;
      private String name;
      private List<String> prerequisites;
      private Integer difficulty;
      private Float estimatedHours;
      @Field(type = FieldType.Dense_Vector, dims = 1536)
      private List<Float> embedding;
  }
  ```

  

  **Repository 示例**：

  ```java
  public interface KnowledgeNodeRepository extends ElasticsearchRepository<KnowledgeNode, String> {
      @Query("{\"match\": {\"name\": \"?0\"}}")
      List<KnowledgeNode> findByName(String name);
  }
  ```

  

  **向量检索服务**：

  ```java
  @Service
  public class VectorSearchService {
      private final ElasticsearchVectorStore vectorStore;
  
      public List<Document> search(String query, int topK) {
          return vectorStore.similaritySearch(
              SearchRequest.builder()
                  .query(query)
                  .topK(topK)
                  .build()
          );
      }
  }
  ```

  

  ### 6.7 事务与一致性处理

  Elasticsearch 不支持 ACID 事务，采用**最终一致性**模型：

  - **写操作**：使用 `@EventListener` 或异步任务 + Spring Retry 重试
  - **版本控制**：利用 `_version` 字段实现乐观锁
  - **关键操作**（如用户注册）使用 `refresh=true` 强制刷新
  - **跨索引更新**：使用 `update_by_query` 或应用层分批更新

  **示例：带版本控制的更新**：

  ```java
  public void updateProfile(String userId, UserProfile profile) {
      UpdateRequest request = new UpdateRequest("users", userId)
          .doc(profile, XContentType.JSON)
          .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
          .version(profile.getVersion());
      // 执行并捕获 VersionConflictException 重试
  }
  ```

  

  ### 6.8 学习干预与激励实现

  **干预引擎**：

  ```java
  @Component
  public class InterventionEngine {
      @Scheduled(cron = "0 0 20 * * ?") // 每晚8点检查
      public void checkAndIntervene() {
          List<User> atRiskUsers = userService.findAtRiskUsers();
          for (User user : atRiskUsers) {
              String suggestion = generateSuggestion(user);
              interventionService.sendIntervention(user.getId(), suggestion);
          }
      }
  
      private String generateSuggestion(User user) {
          return chatModel.call("用户" + user.getName() + "最近学习遇挫，请写一段鼓励语");
      }
  }
  ```

  

  **成就解锁逻辑**：

  ```java
  @Service
  public class AchievementService {
      public void checkAchievements(String userId) {
          long masteredCount = recordService.countMastered(userId);
          long continuousDays = recordService.continuousLearningDays(userId);
          if (masteredCount >= 5) unlockBadge(userId, "学霸");
          if (continuousDays >= 7) unlockBadge(userId, "持之以恒");
      }
  }
  ```

  

------

  ## 7. 项目结构（Web 开发版）

  ### 7.1 后端项目结构（Maven 多模块）

  ```text
  ai-learning-planner-backend/
  ├── pom.xml (父工程)
  ├── ai-core/               # Spring AI 核心配置
  │   ├── ChatModelConfig.java
  │   ├── McpClientConfig.java
  │   └── ElasticsearchConfig.java
  ├── ai-agent/              # 智能体层
  │   ├── orchestrator/      # 主控Agent
  │   ├── diagnosis/         # 诊断Agent
  │   ├── planning/          # 规划Agent
  │   ├── qa/                # 答疑Agent
  │   ├── report/            # 报告Agent
  │   ├── intervention/      # 干预Agent（新增）
  │   └── gamification/      # 激励Agent（新增）
  ├── ai-service/            # 业务服务层
  │   ├── UserService.java
  │   ├── LearningPathService.java
  │   ├── ResourceService.java
  │   ├── InterventionService.java
  │   └── AchievementService.java
  ├── ai-mcp/                # MCP Server 实现
  │   ├── CourseMcpServer.java
  │   ├── SearchMcpServer.java
  │   ├── MailMcpServer.java
  │   ├── PdfMcpServer.java
  │   ├── TodoMcpServer.java
  │   ├── InterventionMcpServer.java
  │   └── GamificationMcpServer.java
  ├── web-api/               # 控制器层
  │   ├── ChatController.java (SSE)
  │   ├── PathController.java
  │   └── InterventionController.java
  └── infrastructure/        # 基础设施
      ├── repository/        # ElasticsearchRepository 接口
      └── redis/             # Redis 配置
  ```

  

  ### 7.2 前端项目结构（Vue3）

  ```text
  ai-learning-planner-web/
  ├── src/
  │   ├── views/
  │   │   ├── Home.vue
  │   │   ├── LearningMap.vue
  │   │   ├── Chat.vue
  │   │   ├── Report.vue
  │   │   ├── Interventions.vue（新增）
  │   │   └── Achievements.vue（新增）
  │   ├── components/
  │   │   ├── ChatBox.vue
  │   │   ├── PathGraph.vue
  │   │   ├── Dashboard.vue
  │   │   ├── InterventionCard.vue（新增）
  │   │   └── BadgeList.vue（新增）
  │   ├── api/               # API 调用（SSE + REST）
  │   ├── stores/            # Pinia 状态管理
  │   └── utils/
  ├── .env.development
  └── package.json
  ```

  

  ### 7.3 开发环境快速启动

  ```bash
  # 后端
  git clone https://github.com/your-repo/ai-learning-planner.git
  # 配置环境变量（见下）
  mvn spring-boot:run
  
  # 前端
  cd ai-learning-planner-web
  pnpm install
  pnpm dev
  ```

  

  ### 7.4 环境变量配置

  ```bash
  # 模型选择
  MODEL_PROVIDER=qwen
  
  # Qwen-Max
  DASHSCOPE_API_KEY=sk-xxx
  
  # DeepSeek-V4-Flush
  DEEPSEEK_API_KEY=sk-xxx
  DEEPSEEK_BASE_URL=https://api.deepseek.com
  
  # 小米 MiMo
  XIAOMI_API_KEY=sk-xxx
  XIAOMI_BASE_URL=https://api.xiaomimimo.com/v1
  
  # Ollama
  OLLAMA_BASE_URL=http://localhost:11434
  OLLAMA_MODEL=deepseek-r1:7b
  
  # Elasticsearch
  ELASTICSEARCH_HOST=localhost
  ELASTICSEARCH_PORT=9200
  
  # Redis
  REDIS_HOST=localhost
  REDIS_PORT=6379
  
  # 邮件配置（用于发送提醒）
  MAIL_HOST=smtp.example.com
  MAIL_PORT=587
  MAIL_USERNAME=xxx
  MAIL_PASSWORD=xxx
  ```

  

------

  ## 8. 项目特色亮点总结

  - ✅ **选题新颖**：AI学习规划师 + 超级智能体，区别于传统增删改查项目
  - ✅ **技术前沿**：Spring AI 1.1.7 + MCP + ReAct + RAG，覆盖当前AI开发主流技术栈
  - ✅ **多模型支持**：同时支持 Qwen-Max、DeepSeek-V4-Flush、小米 MiMo-V2.5-Pro、Ollama，可动态切换
  - ✅ **自主智能体**：基于 ReAct 模式实现真正的自主规划，具备任务分解、工具调用、动态调整能力
  - ✅ **海量数据处理**：采用 Elasticsearch 作为核心存储，天然支持分布式、全文搜索和向量检索，满足大规模学习平台的扩展需求
  - ✅ **丰富的工具生态**：集成邮件、PDF、搜索、文件、待办、干预、成就等MCP工具，智能体能力全面
  - ✅ **完整的业务闭环**：从测评、规划、学习、答疑到报告，并增加干预与激励，覆盖学习全生命周期
  - ✅ **工程落地**：提供完整需求文档、架构图、代码结构，可直接运行展示
  - ✅ **个人全栈**：独立完成前后端开发，体现从需求分析到技术实现的完整闭环