package com.ai.learning.planner.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 学习路径大纲生成器
 * 根据学习目标/领域，生成结构化的章节-周-任务节点数据（nodes JSON）。
 * 与 AI 生成相比，模板化生成具有确定性、无需外部依赖，保证路径"生成即有内容"。
 * 节点结构与 LearningProgressService / 前端进度渲染解析逻辑保持一致：
 * {id, name, title, description, phaseId, phaseTitle, phaseDescription, weekNumber, estimatedHours, status, timeSpent}
 */
@Slf4j
public final class PathOutlineGenerator {

    private PathOutlineGenerator() {
    }

    /** 章节模板：{phaseTitle, phaseDescription, tasks[{title, description, hours}]} */
    private record PhaseTemplate(String phaseTitle, String phaseDescription, List<TaskTemplate> tasks) {
    }

    private record TaskTemplate(String title, String description, double hours) {
    }

    /**
     * 根据目标与领域生成大纲 JSON 字符串
     *
     * @param goal       学习目标（如 "学习python"）
     * @param targetField 目标领域（如 "数据分析"，可为空）
     * @param weeks      总周数（用于章节内周数分配，最少 1 周）
     */
    public static String generateOutline(String goal, String targetField, int weeks, ObjectMapper objectMapper) {
        String field = (targetField != null ? targetField : "") + " " + (goal != null ? goal : "");
        String lower = field.toLowerCase(Locale.ROOT);
        List<PhaseTemplate> phases = pickTemplate(lower);
        return buildNodes(phases, weeks, objectMapper);
    }

    private static List<PhaseTemplate> pickTemplate(String lower) {
        if (containsAny(lower, "python", "数据分析", "数据科学", "pandas", "numpy")) {
            return pythonTemplate();
        }
        if (containsAny(lower, "机器学习", "深度学习", "人工智能", "ai", "神经网络", "大模型")) {
            return mlTemplate();
        }
        if (containsAny(lower, "java", "spring", "后端", "微服务")) {
            return javaTemplate();
        }
        if (containsAny(lower, "前端", "web", "vue", "react", "javascript", "html")) {
            return frontendTemplate();
        }
        if (containsAny(lower, "云", "go", "golang", "k8s", "kubernetes", "docker", "运维")) {
            return cloudTemplate();
        }
        return generalTemplate();
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static String buildNodes(List<PhaseTemplate> phases, int weeks, ObjectMapper objectMapper) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        int nodeSeq = 1;
        // 每章分配周数：至少 1 周，超出部分平均分配
        int weeksPerPhase = Math.max(1, (int) Math.ceil((double) weeks / phases.size()));
        int globalWeek = 1;

        for (int pi = 0; pi < phases.size(); pi++) {
            PhaseTemplate phase = phases.get(pi);
            String phaseId = "phase-" + (pi + 1);
            List<TaskTemplate> tasks = phase.tasks();
            // 将章节任务按周切分（每章最多 weeksPerPhase 周）
            int phaseWeeks = Math.min(weeksPerPhase, Math.max(1, tasks.size() / 3));
            int chunkSize = (int) Math.ceil((double) tasks.size() / phaseWeeks);

            for (int w = 0; w < phaseWeeks; w++) {
                int from = w * chunkSize;
                int to = Math.min(from + chunkSize, tasks.size());
                List<TaskTemplate> weekTasks = tasks.subList(from, to);
                if (weekTasks.isEmpty()) continue;

                for (TaskTemplate task : weekTasks) {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", "node-" + nodeSeq++);
                    node.put("name", task.title());
                    node.put("title", task.title());
                    node.put("description", task.description());
                    node.put("phaseId", phaseId);
                    node.put("phaseTitle", phase.phaseTitle());
                    node.put("phaseDescription", phase.phaseDescription());
                    node.put("weekNumber", globalWeek);
                    node.put("estimatedHours", task.hours());
                    node.put("status", "pending");
                    node.put("timeSpent", 0);
                    nodes.add(node);
                }
                globalWeek++;
            }
        }

        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (Exception e) {
            log.error("序列化学习路径大纲失败", e);
            return "[]";
        }
    }

    private static List<PhaseTemplate> pythonTemplate() {
        return List.of(
                new PhaseTemplate("第一章 · Python 基础语法", "变量、数据类型、条件判断、循环等核心语法",
                        List.of(
                                new TaskTemplate("变量与数据类型", "学习数字、字符串、列表、字典等基础类型", 2.5),
                                new TaskTemplate("条件判断与循环", "掌握 if/else、for/while 的用法", 2.5),
                                new TaskTemplate("函数定义与调用", "学会编写可复用的函数", 2.0))),
                new PhaseTemplate("第二章 · 数据处理基础", "NumPy 数组操作与 Pandas 数据清洗",
                        List.of(
                                new TaskTemplate("NumPy 数组操作", "数组创建、索引、切片与广播", 3.0),
                                new TaskTemplate("Pandas 数据结构", "Series 与 DataFrame 的构建和操作", 3.0),
                                new TaskTemplate("数据清洗实战", "缺失值、重复值、异常值处理", 3.0))),
                new PhaseTemplate("第三章 · 数据分析与可视化", "统计分析、Matplotlib 与业务洞察",
                        List.of(
                                new TaskTemplate("描述性统计分析", "均值、方差、分布等统计指标", 2.5),
                                new TaskTemplate("Matplotlib 可视化", "折线图、柱状图、散点图绘制", 2.5),
                                new TaskTemplate("综合数据分析实战", "完成一个完整的数据分析报告", 3.5))),
                new PhaseTemplate("第四章 · 机器学习入门", "Scikit-learn 常用模型与评估",
                        List.of(
                                new TaskTemplate("线性回归模型", "理解回归原理并完成训练预测", 3.0),
                                new TaskTemplate("分类模型实践", "决策树/逻辑回归分类实战", 3.0),
                                new TaskTemplate("模型评估与优化", "交叉验证、混淆矩阵与调参", 3.0)))
        );
    }

    private static List<PhaseTemplate> mlTemplate() {
        return List.of(
                new PhaseTemplate("第一章 · 数学基础", "线性代数、概率论与微积分核心概念",
                        List.of(
                                new TaskTemplate("线性代数基础", "矩阵运算、特征值与特征向量", 3.0),
                                new TaskTemplate("概率与统计", "概率分布、期望与贝叶斯定理", 3.0),
                                new TaskTemplate("微积分与最优化", "导数、梯度与梯度下降", 3.0))),
                new PhaseTemplate("第二章 · 监督学习", "回归与分类经典算法",
                        List.of(
                                new TaskTemplate("线性模型与回归", "线性/岭回归原理与实现", 3.0),
                                new TaskTemplate("决策树与集成学习", "随机森林、梯度提升树", 3.5),
                                new TaskTemplate("支持向量机", "SVM 原理与核函数", 3.0))),
                new PhaseTemplate("第三章 · 神经网络", "深度学习核心结构与训练技巧",
                        List.of(
                                new TaskTemplate("感知机与前向传播", "神经网络基础结构与激活函数", 3.0),
                                new TaskTemplate("反向传播与优化", "链式法则、Adam 等优化器", 3.0),
                                new TaskTemplate("CNN 卷积神经网络", "卷积、池化与图像分类实战", 3.5))),
                new PhaseTemplate("第四章 · 项目实战", "端到端机器学习项目",
                        List.of(
                                new TaskTemplate("特征工程实战", "特征构造、选择与标准化", 3.0),
                                new TaskTemplate("模型训练与调优", "网格搜索与模型融合", 3.0),
                                new TaskTemplate("部署与评估报告", "模型部署与效果评估总结", 3.5)))
        );
    }

    private static List<PhaseTemplate> javaTemplate() {
        return List.of(
                new PhaseTemplate("第一章 · Java 核心语法", "面向对象基础与常用 API",
                        List.of(
                                new TaskTemplate("Java 基础语法", "变量、流程控制、数组", 2.5),
                                new TaskTemplate("面向对象编程", "类、继承、多态、接口", 3.0),
                                new TaskTemplate("集合与泛型", "List/Map/Set 与泛型机制", 2.5))),
                new PhaseTemplate("第二章 · 进阶特性", "异常、IO、并发与函数式编程",
                        List.of(
                                new TaskTemplate("异常处理与 IO", "异常体系、文件读写", 2.5),
                                new TaskTemplate("多线程与并发", "线程池、锁、并发容器", 3.0),
                                new TaskTemplate("Lambda 与 Stream", "函数式编程与流式处理", 2.5))),
                new PhaseTemplate("第三章 · Spring 生态", "Spring Boot 核心与数据访问",
                        List.of(
                                new TaskTemplate("Spring Boot 入门", "自动配置、依赖注入", 3.0),
                                new TaskTemplate("RESTful API 开发", "Controller、参数校验、异常处理", 3.0),
                                new TaskTemplate("MyBatis/JPA 数据访问", "ORM 映射与事务管理", 3.0))),
                new PhaseTemplate("第四章 · 工程化实战", "项目搭建、测试与部署",
                        List.of(
                                new TaskTemplate("项目结构与规范", "分层架构与代码规范", 2.5),
                                new TaskTemplate("单元测试与 Mock", "JUnit 5 与测试实践", 2.5),
                                new TaskTemplate("完整项目实战", "从零搭建一个 Spring Boot 应用", 4.0)))
        );
    }

    private static List<PhaseTemplate> frontendTemplate() {
        return List.of(
                new PhaseTemplate("第一章 · HTML/CSS 基础", "页面结构与样式布局",
                        List.of(
                                new TaskTemplate("HTML 语义化标签", "常用标签与表单元素", 2.0),
                                new TaskTemplate("CSS 选择器与盒模型", "选择器、Flex/Grid 布局", 2.5),
                                new TaskTemplate("响应式设计", "媒体查询与移动端适配", 2.5))),
                new PhaseTemplate("第二章 · JavaScript 核心", "语言基础与 DOM 操作",
                        List.of(
                                new TaskTemplate("JavaScript 语法", "变量、函数、作用域、闭包", 2.5),
                                new TaskTemplate("DOM 与事件", "节点操作与事件委托", 2.5),
                                new TaskTemplate("异步编程", "Promise、async/await", 2.5))),
                new PhaseTemplate("第三章 · Vue 框架", "组件化开发与状态管理",
                        List.of(
                                new TaskTemplate("Vue 基础", "模板语法、响应式数据、生命周期", 3.0),
                                new TaskTemplate("组件通信", "props、emit、provide/inject", 3.0),
                                new TaskTemplate("Vue Router 与 Pinia", "路由与状态管理实战", 3.0))),
                new PhaseTemplate("第四章 · 工程化实战", "构建工具与全栈联调",
                        List.of(
                                new TaskTemplate("Vite 构建与工程化", "构建配置、环境变量、代码分割", 2.5),
                                new TaskTemplate("Axios 与接口联调", "请求封装、拦截器、鉴权", 2.5),
                                new TaskTemplate("完整前端项目实战", "开发一个完整的业务页面", 4.0)))
        );
    }

    private static List<PhaseTemplate> cloudTemplate() {
        return List.of(
                new PhaseTemplate("第一章 · Linux 与网络基础", "服务器环境与网络协议",
                        List.of(
                                new TaskTemplate("Linux 常用命令", "文件、进程、权限管理", 2.5),
                                new TaskTemplate("网络与协议", "TCP/IP、HTTP、DNS", 2.5),
                                new TaskTemplate("Shell 脚本", "编写自动化运维脚本", 2.5))),
                new PhaseTemplate("第二章 · 容器化", "Docker 容器与镜像管理",
                        List.of(
                                new TaskTemplate("Docker 基础", "镜像、容器、数据卷", 2.5),
                                new TaskTemplate("Dockerfile 实践", "编写高效镜像构建脚本", 2.5),
                                new TaskTemplate("Docker Compose", "多服务编排与部署", 3.0))),
                new PhaseTemplate("第三章 · 容器编排", "Kubernetes 核心概念与实战",
                        List.of(
                                new TaskTemplate("K8s 核心对象", "Pod、Deployment、Service", 3.5),
                                new TaskTemplate("配置与存储", "ConfigMap、Secret、PV/PVC", 3.0),
                                new TaskTemplate("服务网格与监控", "Ingress、Prometheus 监控", 3.5))),
                new PhaseTemplate("第四章 · 云原生实战", "CI/CD 与云平台部署",
                        List.of(
                                new TaskTemplate("CI/CD 流水线", "GitLab CI / GitHub Actions", 3.0),
                                new TaskTemplate("云平台部署", "公有云服务与弹性伸缩", 3.0),
                                new TaskTemplate("完整项目上云", "从代码到生产环境的全流程", 4.0)))
        );
    }

    private static List<PhaseTemplate> generalTemplate() {
        return List.of(
                new PhaseTemplate("第一章 · 基础入门", "核心概念与基础技能",
                        List.of(
                                new TaskTemplate("学习路线与目标拆解", "明确目标、拆解学习阶段", 2.0),
                                new TaskTemplate("核心概念速览", "掌握领域核心名词与原理", 2.5),
                                new TaskTemplate("基础技能练习", "完成基础内容练习巩固", 2.5))),
                new PhaseTemplate("第二章 · 核心进阶", "关键知识点深度掌握",
                        List.of(
                                new TaskTemplate("进阶知识学习", "深入理解核心难点", 3.0),
                                new TaskTemplate("案例拆解", "分析经典案例的实现思路", 3.0),
                                new TaskTemplate("动手实践", "完成一次独立练习", 3.0))),
                new PhaseTemplate("第三章 · 综合应用", "项目实战与综合训练",
                        List.of(
                                new TaskTemplate("小型项目实战", "独立完成一个小型项目", 3.5),
                                new TaskTemplate("常见问题排查", "学习调试与问题定位", 2.5),
                                new TaskTemplate("成果复盘", "总结收获与不足", 2.0))),
                new PhaseTemplate("第四章 · 巩固提升", "查漏补缺与持续学习",
                        List.of(
                                new TaskTemplate("薄弱环节补强", "针对弱项专项训练", 2.5),
                                new TaskTemplate("扩展学习", "了解进阶方向与前沿", 2.5),
                                new TaskTemplate("学习总结与规划", "输出总结并规划下一步", 2.0)))
        );
    }
}
