# 贡献指南（CONTRIBUTING）

欢迎为 **AI Learning Planner** 贡献代码、文档或 Issue！请遵循以下规范，确保协作顺畅。

## 一、环境准备

- JDK 21+、Maven 3.9+、Node.js 18+、MySQL 8.0+、Redis 7.x
- 前端依赖安装：`cd vue && npm install`
- 本地配置：复制 `springboot/.env.example` 为 `springboot/.env` 并填入真实凭据（**禁止提交 .env**）

## 二、代码规范

### 后端（Java / Spring Boot）

- 遵循项目既有风格：4 空格缩进、Javadoc 注释类与方法
- 新增接口必须：参数校验（`@Valid`）+ 用户归属校验（`SecurityContextHolder.getCurrentUserId()`）
- SQL 一律使用 JPA 参数化查询（`@Query("... :param ...")`），**禁止字符串拼接 SQL**
- 敏感信息（密钥/Token/密码）只允许经环境变量注入，禁止硬编码
- 异常统一抛 `BusinessException`，由 `GlobalExceptionHandler` 统一脱敏处理
- 新增依赖前确认许可证与 Apache 2.0 兼容

### 前端（Vue 3 / JavaScript）

- **仅使用 JavaScript（.js），禁止 TypeScript**
- 组件命名：PascalCase 文件 + 语义化（`LearningPathDetail.vue`）
- CSS 使用语义化类名，遵循 `variables.scss` 中的设计变量
- 用户/AI 内容渲染统一走 `utils/markdown.js` 的 `renderMarkdown()`（内置 DOMPurify 净化），**禁止直接 v-html 渲染未净化内容**
- 提交前运行 `npm run lint`，不允许新增 error

### 提交信息（Commit Message）

```text
feat: 新增 XX 功能
fix: 修复 XX 问题
docs: 更新 XX 文档
refactor: 重构 XX 模块
test: 补充 XX 测试
chore: 构建/依赖/工具链调整
```

## 三、分支与 PR 流程

1. Fork 本仓库，基于 `main` 创建特性分支：`git checkout -b feat/your-feature`
2. 开发完成后确保：
   - 后端：`cd springboot && mvn test` 全部通过
   - 前端：`cd vue && npm run lint && npm run build` 通过
3. 提交并推送，创建 Pull Request：
   - 标题遵循 Commit Message 规范
   - 描述中说明改动动机、影响范围、测试方式
4. 等待 CI 通过（GitHub Actions）与维护者评审

## 四、测试要求

- 后端新增/修改核心 Service 逻辑必须补充单元测试（参考 `springboot/src/test` 现有 129 个用例）
- 前端组件改动需人工验证主要交互路径（构建 + 本地运行）

## 五、Issue 规范

- Bug 报告：附复现步骤、环境信息（JDK/Node/MySQL 版本）、后端日志关键片段（注意脱敏）
- 功能建议：说明使用场景与期望行为

## 六、行为准则

- 尊重他人工作，Code Review 聚焦代码本身
- 不提交无关改动（格式化漂移、文件编码变更等）
- 许可证：贡献内容默认以 [Apache License 2.0](LICENSE) 授权
