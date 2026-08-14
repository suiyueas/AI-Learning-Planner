<p align="center">
  <h1 align="center">AI Learning Planner</h1>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-007396.svg?style=flat-square" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.1-6DB33F.svg?style=flat-square" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Spring%20AI-1.1.7-6DB33F.svg?style=flat-square" alt="Spring AI">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D.svg?style=flat-square" alt="Vue">
  <img src="https://img.shields.io/badge/License-Apache%202.0-FF5722.svg?style=flat-square" alt="License">
</p>

<p align="center">
  <strong>基于 Spring AI + MCP + Agent 的智能学习规划平台</strong>
</p>

<p align="center">
  <a href="#project-intro">项目简介</a> •
  <a href="#tech-stack">技术栈</a> •
  <a href="#core-features">核心功能</a> •
  <a href="#quick-start">快速开始</a> •
  <a href="#project-structure">项目结构</a> •
  <a href="#documentation">文档</a>
</p>

---

## 项目简介

<a id="project-intro"></a>

AI 学习规划师是一个基于大语言模型与智能算法的智能学习平台，能够精准诊断用户学习水平、动态生成专属学习路径，并提供 7x24 小时的智能辅导与监督。

### 核心价值

- **个性化规划**：基于用户画像与知识图谱，生成千人千面的学习路径
- **自主 Agent**：7 类专业智能体 + ReAct 推理闭环 + 长链推理引擎，自主分解任务、调用工具、迭代优化计划
- **MCP 工具生态**：标准化工具协议接入，覆盖文档摘要、知识点提取、测验生成、翻译等 AI 能力
- **全周期陪伴**：从诊断、规划、学习、练习、干预到报告，覆盖完整学习闭环

---

## 技术栈

### 后端

| 技术            | 版本  | 说明                                   |
| --------------- | ----- | -------------------------------------- |
| Java            | 21    | LTS 版本                               |
| Spring Boot     | 3.5.1 | 核心框架                               |
| Spring AI       | 1.1.7 | AI 集成框架（多模型 + 向量存储 + MCP） |
| Spring Security | 6.x   | 安全认证（JWT）                        |
| JPA / Hibernate | -     | ORM 持久层                             |
| MySQL           | 8.0   | 关系数据库                             |
| Redis           | 7.x   | 缓存与会话                             |
| Elasticsearch   | 8.x   | RAG 向量检索主存储（ES 优先，不可达自动降级内存） |

### 前端

| 技术         | 版本 | 说明       |
| ------------ | ---- | ---------- |
| Vue          | 3.4+ | 渐进式框架 |
| Vite         | 5.x  | 构建工具   |
| Pinia        | 2.x  | 状态管理   |
| Element Plus | 2.5+ | UI 组件库  |
| ECharts      | 5.6+ | 数据可视化 |
| vue-router   | 4.x  | 路由管理   |

### AI 集成

| 模型              | 提供商   | 用途                          |
| ----------------- | -------- | ----------------------------- |
| DeepSeek V4 Flash | DeepSeek | 默认工作模型（对话 + 向量化） |
| Qwen-Max          | 阿里云   | 高性能推理                    |
| MiMo-V2.5-Pro     | 小米     | 多模态支持                    |

---

## 核心功能

<a id="core-features"></a>

### 1. 智能能力诊断

- 多维度测评题目自动生成
- 知识薄弱点精准识别与画像构建
- 诊断结果可视化（DiagnosisDetail / WeaknessDetail）

### 2. 个性化学习路径

- 基于知识图谱的依赖分析与动态规划
- 每日任务（DailyTask）生成、打卡与进度追踪
- 路径自动调整（auto-adjust）与 AI 优化（optimize）

### 3. 超级智能体 (Agent)

- **7 类应用级 Agent**：诊断、规划、答疑、报告、干预、激励、习题
- **ReAct 推理引擎**：think → act → observe 行动闭环（`ReActAgent`）
- **长链推理**：Planner → Evaluator → Reflection → Replanning 迭代优化（`AdvancedReasoningAgent`）
- **记忆系统**：情景记忆（EpisodicMemory）+ 上下文窗口管理 + 上下文压缩
- **编排器**：主控 Agent 统一调度子 Agent，支持同步与 SSE 流式输出
- **MCP 工具调用**：AgentToolManager 工具注册与 ToolCallAgent 执行

### 4. 知识管理与 RAG

- 文档智能解析、分块与全量知识块生成
- 向量语义检索（ES 主存储，不可达自动降级内存向量 + 关键词）
- 知识库问答（来源标注）、知识图谱可视化

### 5. 习题与测评

- AI 习题生成（QuizGenerationTool）与自动批改
- 每日一练、答题记录与错题分析
- 自适应练习（AdaptiveHistory / AdaptiveDetail）

### 6. 学习干预与激励

- 进度滞后、知识点掌握度下降、连续未登录自动预警（通知中心）
- 成就徽章系统与每日打卡
- 智能通知中心（干预扫描 + 实时推送）

### 7. 学习数据分析与报告

- 多维度统计看板与学习行为分析
- 智能学情报告（PDF 导出）
- 学习日历、学习记录、学习笔记（StudyNotes）

### 8. 工具与 MCP 生态

- 工具执行中心（Tools）：文档摘要、知识点提取、测验生成、翻译、联网搜索
- 代码分析器（CodeAnalyzer）
- MCP 客户端超时/重试/降级策略，敏感数据脱敏与安全过滤

---

## 快速开始

<a id="quick-start"></a>

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis 7.x
- Elasticsearch 8.x（推荐，缺失时自动降级内存向量存储）

### 1. 克隆项目

```bash
# 替换为你的实际仓库地址
git clone https://github.com/<your-github-username>/ai-learning-planner.git
cd ai-learning-planner
```

### 2. 配置环境变量

```bash
# 复制配置模板
cp springboot/.env.example springboot/.env
# - JWT 密钥
```

### 3. 初始化数据库

```sql
-- 创建数据库
CREATE DATABASE ai_learning_planner DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

建表语句参考 `springboot/src/main/resources/sql/init.sql`（实体表结构由 JPA 自动维护）。

### 4. 启动后端

```bash
cd springboot
mvn spring-boot:run
```

后端启动后访问：`http://localhost:8080/api`

API 文档：`http://localhost:8080/api/swagger-ui.html`

### 5. 启动前端

```bash
cd vue
npm install
npm run dev
```

前端启动后访问：`http://localhost:3000`

---

## 项目结构

<a id="project-structure"></a>

```
ai-learning-planner/
├── springboot/                    # Spring Boot 后端
│   └── src/main/java/com/ai/learning/planner/
│       ├── agent/                  # 智能体模块
│       │   ├── base/              # Agent 基类与状态机
│       │   │   ├── BaseAgent.java # Agent 抽象基类（步骤控制/状态流转）
│       │   │   └── AgentState.java# Agent 状态（IDLE/RUNNING/FINISHED...）
│       │   ├── react/             # ReAct 推理引擎
│       │   │   └── ReActAgent.java# think → act → observe 行动闭环
│       │   ├── app/               # 应用级 Agent（7 类）
│       │   │   ├── DiagnosisAgent.java    # 诊断 Agent
│       │   │   ├── PlanningAgent.java     # 规划 Agent
│       │   │   ├── QAAgent.java          # 答疑 Agent
│       │   │   ├── ReportAgent.java      # 报告 Agent
│       │   │   ├── InterventionAgent.java # 干预 Agent
│       │   │   ├── GamificationAgent.java # 激励 Agent
│       │   │   └── ExerciseAgent.java    # 习题 Agent
│       │   ├── reasoning/          # 长链推理引擎
│       │   │   ├── AdvancedReasoningAgent.java # 计划→执行→评估→反思→重规划
│       │   │   ├── Planner.java / LlmPlanner.java  # 计划生成
│       │   │   ├── Evaluator.java  # 执行评估
│       │   │   ├── ReflectionEngine.java / ReflectionResult.java # 反思
│       │   │   ├── ReplanningTrigger.java # 重规划触发
│       │   │   └── ReasoningMonitor.java / ReasoningTrace.java   # 推理可观测
│       │   ├── memory/             # 记忆系统
│       │   │   ├── EpisodicMemory.java / InMemoryEpisodicMemory.java # 情景记忆
│       │   │   ├── ContextWindow.java     # 上下文窗口管理
│       │   │   └── ContextCompressor.java # 上下文压缩
│       │   ├── tool/               # Agent 工具
│       │   │   ├── AgentToolManager.java # 工具注册与调度
│       │   │   └── ToolCallAgent.java    # 工具调用 Agent
│       │   ├── orchestrator/       # Agent 编排器（主控调度 + SSE 流式）
│       │   └── dto/                # AgentInfo / TaskRequest / TaskResult
│       ├── mcp/                    # MCP 协议实现
│       │   ├── client/             # MCP 客户端
│       │   │   ├── EnhancedMcpClient.java # 增强客户端
│       │   │   ├── McpTimeoutPolicy.java / McpRetryPolicy.java # 超时与重试
│       │   │   └── FallbackRegistry.java # 降级注册
│       │   ├── ai/                 # AI 工具层
│       │   │   ├── DocumentSummaryTool.java   # 文档摘要
│       │   │   ├── KnowledgeExtractionTool.java # 知识点提取
│       │   │   ├── QuizGenerationTool.java    # 测验生成
│       │   │   ├── TranslationTool.java       # 翻译
│       │   │   ├── LearningAssistantTool.java # 学习助手
│       │   │   └── ToolDefinitionRegistry.java # 工具定义注册
│       │   ├── security/           # 安全过滤与敏感数据脱敏
│       │   ├── hitl/               # 人工审批（HitlApprovalGate）
│       │   └── resource/           # 资源模板（ResourceTemplateRegistry）
│       ├── controller/             # REST API（21 个控制器）
│       ├── service/                # 业务服务
│       ├── repository/             # 数据访问
│       ├── entity/                 # 实体模型
│       ├── dto/                    # 数据传输对象
│       ├── config/                 # 配置类（多模型/向量存储/JWT 等）
│       └── security/               # 安全组件（JWT 过滤/权限/审计）
│
├── vue/                           # Vue 3 前端
│   └── src/
│       ├── api/                   # API 调用封装
│       ├── components/             # 公共组件
│       │   ├── agent/             # Agent 相关组件
│       │   ├── chat/              # 聊天组件
│       │   └── tools/             # 工具结果组件
│       ├── views/                 # 页面视图
│       │   ├── capability/        # 能力详情（诊断/规划/进度/薄弱点/自适应）
│       │   ├── Chat.vue           # 聊天主页
│       │   ├── Agents.vue         # Agent 中心
│       │   ├── Knowledge.vue      # 知识库
│       │   ├── Tools.vue          # 工具中心
│       │   ├── Exercise.vue       # 习题练习
│       │   ├── Statistics.vue     # 数据统计
│       │   └── ...                # 更多页面
│       ├── stores/                # Pinia 状态
│       └── router/                # 路由配置
│
├── docs/                          # 设计文档
│   ├── springai+agent+mcp.md      # 架构设计
│   ├── advanced_reasoning_and_mcp_upgrade.md  # 高级推理与 MCP 升级
│   ├── UI设计.html                # UI 设计稿
│   └── 功能架构设计.html           # 功能架构图
│
├── springboot/.env.example        # 环境变量模板
├── CHANGELOG.md                   # 版本变更日志
└── LICENSE                        # 开源协议
```

---

## 文档

<a id="documentation"></a>

- [技术架构文档](docs/springai+agent+mcp.md)
- [高级推理与 MCP 升级](docs/advanced_reasoning_and_mcp_upgrade.md)
- [UI 设计稿](docs/UI设计.html)
- [功能架构设计](docs/功能架构设计.html)
- [版本变更日志](CHANGELOG.md)
- [API 接口文档](http://localhost:8080/api/swagger-ui.html) (启动后访问)

---

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源。

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！详细规范（代码风格、PR 流程、测试要求）请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

<p align="center">
  <strong>Made with ❤️ by AI Learning Planner Team</strong>
</p>
