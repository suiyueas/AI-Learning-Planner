-- ============================================
-- AI 学习规划师 - 数据库完整初始化脚本（v4.0）
-- ============================================
-- 本文件整合了项目所有表定义，统一编号，供新环境初始化和手工建库使用。
-- JPA ddl-auto: update 也会自动建表/加列，本脚本留档与手工初始化用。
--
-- 表清单（24 张表 + 2 个视图）：
--   1.  users                       用户表
--   2.  learning_paths              学习路径表（含 nodes JSON）
--   3.  agent_executions            Agent 执行记录表
--   4.  learning_events             学习行为事件表
--   5.  knowledge_documents         知识库文档表
--   6.  knowledge_chunks            知识块表
--   7.  chat_histories              对话历史表
--   8.  tool_execution_records      工具执行记录表
--   9.  assessment_records          测评答题记录表
--  10.  checkin_records             打卡记录表
--  11.  user_notifications          用户通知表
--  12.  question_bank               题库表
--  13.  learning_records            学习记录表
--  14.  daily_tasks                 每日学习任务表
--  15.  learning_sessions           学习会话表（新增·新工作台模式）
--  16.  session_phases              会话阶段表（新增·新工作台模式）
--  17.  reasoning_trace             Agent思考轨迹表（扩展功能）
--  18.  adaptive_adjustment         自适应调整记录表（扩展功能）
--  19.  user_recommendation        用户推荐记录表（扩展功能）
--  20.  notes                       学习笔记表
--  21.  audit_log                   安全审计日志表
--  22.  user_points                 用户积分总表
--  23.  point_transactions          积分流水表
--  24.  checkin_config              签到配置表
--  视图：knowledge_status_view, tool_stats_view
-- ============================================

-- ============================================
-- 第一部分：核心表
-- ============================================

-- 1. 用户表（含原 user_profiles 字段 + 打卡统计字段 + 智能干预阈值字段）
CREATE TABLE IF NOT EXISTS users (
    id                BIGINT          PRIMARY KEY AUTO_INCREMENT     COMMENT '用户ID',
    username          VARCHAR(50)     NOT NULL UNIQUE                COMMENT '用户名',
    email             VARCHAR(100)    NOT NULL UNIQUE                COMMENT '邮箱',
    password_hash     VARCHAR(255)    NOT NULL                       COMMENT '密码哈希',
    nickname          VARCHAR(50)                                    COMMENT '昵称',
    role              VARCHAR(20)     DEFAULT 'USER'                 COMMENT '用户角色：USER普通用户 ADMIN管理员',
    avatar_url        VARCHAR(255)                                   COMMENT '头像URL',
    learning_goal     VARCHAR(255)                                   COMMENT '学习目标',
    bio               TEXT                                           COMMENT '个人简介',
    learning_style    VARCHAR(50)                                    COMMENT '学习风格',
    level             INT             DEFAULT 1                      COMMENT '等级',
    active_hours      VARCHAR(100)                                   COMMENT '活跃时间段',
    target_field      VARCHAR(255)                                   COMMENT '目标领域',
    interests         TEXT                                           COMMENT '兴趣标签',
    weak_points       TEXT                                           COMMENT '薄弱项',
    continuous_checkin_days INT        DEFAULT 0                     COMMENT '连续打卡天数',
    total_checkin_days       INT        DEFAULT 0                    COMMENT '总打卡天数',
    intervention_enabled                  TINYINT(1)   DEFAULT 1     COMMENT '干预提醒总开关：1开启 0关闭',
    intervention_progress_threshold       FLOAT        DEFAULT 65    COMMENT '进度提醒阈值（%）',
    intervention_score_decline_threshold  FLOAT        DEFAULT 10    COMMENT '测评降幅阈值（%）',
    intervention_inactive_days            INT          DEFAULT 3     COMMENT '连续未登录预警天数',
    created_at        DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    updated_at        DATETIME        DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP    COMMENT '更新时间',
    last_login_at     DATETIME                                       COMMENT '最近登录时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 学习路径表（含 nodes JSON 列）
CREATE TABLE IF NOT EXISTS learning_paths (
    id                   VARCHAR(255)    PRIMARY KEY                 COMMENT '路径ID',
    user_id              VARCHAR(255)    NOT NULL                    COMMENT '用户ID',
    name                 VARCHAR(255)    NOT NULL                    COMMENT '路径名称',
    description          TEXT                                        COMMENT '路径描述',
    version              INT             DEFAULT 1                   COMMENT '版本号',
    is_active            BOOLEAN         DEFAULT TRUE                COMMENT '是否激活',
    completion_percentage FLOAT          DEFAULT 0                   COMMENT '完成百分比',
    nodes                JSON                                        COMMENT '节点列表JSON',
    source               VARCHAR(20)     DEFAULT 'manual'            COMMENT '路径来源',
    created_at           DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    updated_at           DATETIME        DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_lp_user_id (user_id),
    INDEX idx_lp_user_active (user_id, is_active),
    INDEX idx_lp_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习路径表';

-- 3. Agent 执行记录表
CREATE TABLE IF NOT EXISTS agent_executions (
    id               VARCHAR(255)    PRIMARY KEY                    COMMENT '执行记录ID',
    user_id          VARCHAR(255)                                   COMMENT '用户ID',
    agent_id         VARCHAR(255)    NOT NULL                       COMMENT 'Agent ID',
    agent_name       VARCHAR(255)                                   COMMENT 'Agent名称',
    record_type      VARCHAR(20)     NOT NULL                       COMMENT '记录类型：LOG/RESULT',
    session_id       VARCHAR(100)                                   COMMENT '会话ID',
    execution_id     VARCHAR(100)                                   COMMENT '执行ID',
    type             VARCHAR(50)                                    COMMENT '日志类型',
    title            VARCHAR(255)                                   COMMENT '日志标题',
    content          TEXT                                           COMMENT '日志内容',
    step_number      INT                                            COMMENT '步骤编号',
    step_order       INT                                            COMMENT '步骤顺序',
    phase            VARCHAR(20)                                    COMMENT '执行阶段',
    task_description TEXT                                           COMMENT '任务描述',
    result_type      VARCHAR(50)                                    COMMENT '结果类型',
    result_content   TEXT                                           COMMENT '结果内容（JSON）',
    result_summary   TEXT                                           COMMENT '结果摘要',
    output           TEXT                                           COMMENT '输出内容（JSON）',
    duration         BIGINT                                         COMMENT '执行耗时（ms）',
    status           VARCHAR(20)                                    COMMENT '状态',
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    is_deleted       TINYINT(1)      DEFAULT 0                      COMMENT '软删除标记',
    INDEX idx_agent_session (session_id),
    INDEX idx_agent_execution (execution_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_agent_user (user_id),
    INDEX idx_record_type (record_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent执行记录表';

-- 4. 学习行为事件表
CREATE TABLE IF NOT EXISTS learning_events (
    id          VARCHAR(255)    PRIMARY KEY                         COMMENT '事件ID',
    user_id     VARCHAR(255)    NOT NULL                            COMMENT '用户ID',
    event_type  VARCHAR(30)     NOT NULL                            COMMENT '事件类型：achievement/intervention',
    event_key   VARCHAR(255)    NOT NULL                            COMMENT '事件标识键',
    description TEXT                                                COMMENT '事件描述',
    metadata    TEXT                                                COMMENT '扩展数据（JSON）',
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    INDEX idx_event_user (user_id),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习行为事件表';

-- 5. 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id            VARCHAR(255)    PRIMARY KEY                      COMMENT '文档ID',
    title         VARCHAR(255)    NOT NULL                         COMMENT '文档标题',
    type          VARCHAR(20)                                      COMMENT '文件类型',
    size          VARCHAR(20)                                      COMMENT '文件大小',
    status        VARCHAR(20)     NOT NULL DEFAULT 'processing'    COMMENT '处理状态',
    file_path     VARCHAR(500)                                     COMMENT '文件存储路径',
    chunks        INT             DEFAULT 0                        COMMENT '知识块数量',
    description   TEXT                                             COMMENT '文档描述',
    uploaded_at   DATETIME        DEFAULT CURRENT_TIMESTAMP        COMMENT '上传时间',
    processed_at  DATETIME                                         COMMENT '处理完成时间',
    user_id       VARCHAR(50)                                      COMMENT '用户ID（数据隔离）',
    INDEX idx_kd_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- 6. 知识块表
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT      COMMENT '块ID',
    doc_id          VARCHAR(255)    NOT NULL                        COMMENT '所属文档ID',
    chunk_index     INT             NOT NULL                        COMMENT '块序号',
    content         TEXT            NOT NULL                        COMMENT '块内容',
    content_preview VARCHAR(500)                                    COMMENT '内容预览',
    char_count      INT                                             COMMENT '字符数',
    embedding       VECTOR                                          COMMENT '向量嵌入（用于向量检索）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP     COMMENT '更新时间',
    INDEX idx_chunk_doc_id (doc_id),
    INDEX idx_chunk_doc_id_index (doc_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识块表';

-- 7. 对话历史表
CREATE TABLE IF NOT EXISTS chat_histories (
    id          VARCHAR(255)    PRIMARY KEY                        COMMENT '聊天ID',
    session_id  VARCHAR(255)    NOT NULL                           COMMENT '会话ID',
    user_id     VARCHAR(255)                                       COMMENT '用户ID',
    role        VARCHAR(255)    NOT NULL                           COMMENT '角色',
    content     TEXT            NOT NULL                           COMMENT '内容',
    agent_type  VARCHAR(50)                                        COMMENT 'Agent类型',
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    INDEX idx_chat_session (session_id),
    INDEX idx_chat_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史表';

-- 8. 工具执行记录表
CREATE TABLE IF NOT EXISTS tool_execution_records (
    id             BIGINT          PRIMARY KEY AUTO_INCREMENT       COMMENT '主键ID',
    tool_id        VARCHAR(50)     NOT NULL                        COMMENT '工具ID',
    tool_name      VARCHAR(100)    NOT NULL                        COMMENT '工具名称',
    params         JSON                                            COMMENT '执行参数',
    result         JSON                                            COMMENT '执行结果',
    status         VARCHAR(20)     DEFAULT 'pending'               COMMENT '执行状态',
    execution_time BIGINT                                          COMMENT '执行耗时(ms)',
    user_id        VARCHAR(50)                                     COMMENT '用户ID',
    created_at     DATETIME        DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
    INDEX idx_tool_id (tool_id),
    INDEX idx_tool_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具执行记录表';

-- ============================================
-- 第二部分：业务扩展表
-- ============================================

-- 9. 测评答题记录表
CREATE TABLE IF NOT EXISTS assessment_records (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    subject     VARCHAR(100) NOT NULL COMMENT '科目名称',
    difficulty  VARCHAR(20) DEFAULT 'medium' COMMENT '难度：easy/medium/hard',
    score       INT DEFAULT 0 COMMENT '得分',
    total       INT DEFAULT 0 COMMENT '总题数',
    details     JSON COMMENT '详情JSON',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '测评时间',
    INDEX idx_ar_user (user_id),
    INDEX idx_ar_subject (subject),
    INDEX idx_ar_user_subject (user_id, subject),
    INDEX idx_ar_user_createdat (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测评答题记录表';

-- 10. 打卡记录表
CREATE TABLE IF NOT EXISTS checkin_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    checkin_date DATE NOT NULL COMMENT '打卡日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_date (user_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户每日打卡记录表';

-- 11. 用户通知表
CREATE TABLE IF NOT EXISTS user_notifications (
    id           BIGINT          PRIMARY KEY AUTO_INCREMENT          COMMENT '通知ID',
    user_id      BIGINT          NOT NULL                            COMMENT '用户ID',
    title        VARCHAR(255)    NOT NULL                            COMMENT '通知标题',
    content      TEXT                                                COMMENT '通知内容',
    priority     VARCHAR(20)     NOT NULL DEFAULT 'INFO'             COMMENT '优先级：EMERGENCY/WARNING/INFO',
    category     VARCHAR(30)     NOT NULL DEFAULT 'SYSTEM'           COMMENT '类别：PROGRESS/KNOWLEDGE/SYSTEM',
    action_type  VARCHAR(30)                                         COMMENT '快捷操作类型',
    action_data  TEXT                                                COMMENT '快捷操作参数（JSON）',
    is_read      TINYINT(1)      DEFAULT 0                           COMMENT '是否已读',
    is_handled   TINYINT(1)      DEFAULT 0                           COMMENT '是否已处理',
    created_at   DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    handled_at   DATETIME                                            COMMENT '处理时间',
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_user_read (user_id, is_read),
    INDEX idx_notif_priority (priority),
    INDEX idx_notif_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';

-- 12. 题库表
CREATE TABLE IF NOT EXISTS question_bank (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT       COMMENT '题目ID',
    subject         VARCHAR(50)     NOT NULL                         COMMENT '科目',
    question_text   TEXT            NOT NULL                         COMMENT '题目文本',
    options         JSON            NOT NULL                         COMMENT '选项（JSON数组）',
    correct_answer  VARCHAR(255)    NOT NULL                         COMMENT '正确答案',
    difficulty      VARCHAR(20)     DEFAULT 'medium'                 COMMENT '难度',
    explanation     TEXT                                            COMMENT '解析',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
    INDEX idx_qb_subject (subject),
    INDEX idx_qb_subject_difficulty (subject, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

-- 13. 学习记录表
CREATE TABLE IF NOT EXISTS learning_records (
    id            BIGINT          PRIMARY KEY AUTO_INCREMENT         COMMENT '记录ID',
    user_id       VARCHAR(255)    NOT NULL                           COMMENT '用户ID',
    path_id       VARCHAR(255)    NOT NULL                           COMMENT '学习路径ID',
    node_id       VARCHAR(255)    NOT NULL                           COMMENT '路径节点ID',
    node_type     VARCHAR(50)                                        COMMENT '节点类型',
    status        VARCHAR(20)     NOT NULL DEFAULT 'pending'         COMMENT '状态',
    mastery_level FLOAT                                              COMMENT '掌握度（0-5分）',
    time_spent    INT             DEFAULT 0                          COMMENT '学习时长（分钟）',
    completed_at  DATETIME                                           COMMENT '完成时间',
    created_at    DATETIME        DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    updated_at    DATETIME        DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP        COMMENT '更新时间',
    INDEX idx_lr_user_created (user_id, created_at),
    INDEX idx_lr_path_id (path_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表';

-- 14. 每日学习任务表
CREATE TABLE IF NOT EXISTS daily_tasks (
    id                VARCHAR(255)    PRIMARY KEY                    COMMENT '任务ID',
    path_id           VARCHAR(255)    NOT NULL                       COMMENT '所属学习路径ID',
    user_id           VARCHAR(255)    NOT NULL                       COMMENT '用户ID',
    task_date         DATE            NOT NULL                       COMMENT '任务日期',
    title             VARCHAR(255)    NOT NULL                       COMMENT '任务标题',
    type              VARCHAR(50)     NOT NULL                       COMMENT '任务类型',
    estimated_minutes INT                                            COMMENT '预计学习时长（分钟）',
    status            VARCHAR(20)     NOT NULL DEFAULT 'pending'     COMMENT '状态',
    description       TEXT                                           COMMENT '任务描述',
    resource_id       VARCHAR(255)                                   COMMENT '关联资源ID',
    source_node_id    VARCHAR(255)                                   COMMENT '来源路径节点ID',
    sort_order        INT             DEFAULT 0                      COMMENT '排序号',
    created_at        DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    updated_at        DATETIME        DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP    COMMENT '更新时间',
    INDEX idx_daily_path_date (path_id, task_date),
    INDEX idx_daily_user_date (user_id, task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日学习任务表';

-- 15. 学习会话表（新增 - 新工作台模式核心表）
-- 记录用户的一次完整学习旅程：诊断 -> 规划 -> 学习 -> 习题 -> 报告
CREATE TABLE IF NOT EXISTS learning_sessions (
    id               BIGINT          PRIMARY KEY AUTO_INCREMENT       COMMENT '会话ID',
    user_id          VARCHAR(255)    NOT NULL                         COMMENT '用户ID',
    goal             TEXT            NOT NULL                         COMMENT '学习目标',
    phase            VARCHAR(20)     NOT NULL DEFAULT 'diagnosis'     COMMENT '当前阶段：diagnosis/planning/learning/exercise/report',
    status           VARCHAR(20)     NOT NULL DEFAULT 'active'        COMMENT '会话状态：active/paused/completed/abandoned',
    progress         INT             NOT NULL DEFAULT 0               COMMENT '进度 0-100',
    context_json     TEXT                                             COMMENT '会话上下文（JSON）',
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
    updated_at       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP      COMMENT '更新时间',
    completed_at     DATETIME                                         COMMENT '完成时间',
    INDEX idx_ls_user_id (user_id),
    INDEX idx_ls_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习会话表';

-- 16. 会话阶段表（新增 - 记录每个阶段执行过程）
CREATE TABLE IF NOT EXISTS session_phases (
    id               BIGINT          PRIMARY KEY AUTO_INCREMENT       COMMENT '阶段ID',
    session_id       BIGINT          NOT NULL                         COMMENT '所属会话ID',
    phase_id         VARCHAR(20)     NOT NULL                         COMMENT '阶段类型：diagnosis/planning/learning/exercise/report',
    agent_id         VARCHAR(50)                                      COMMENT '负责该阶段的Agent',
    input_json       TEXT                                             COMMENT '输入参数（JSON）',
    output_json      TEXT                                             COMMENT '输出结果（JSON）',
    status           VARCHAR(20)     NOT NULL DEFAULT 'pending'       COMMENT '阶段状态：pending/executing/completed/failed',
    duration_ms      INT                                              COMMENT '执行耗时（毫秒）',
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
    completed_at     DATETIME                                         COMMENT '完成时间',
    INDEX idx_sp_session_id (session_id),
    INDEX idx_sp_session_phase (session_id, phase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话阶段表';

-- ============================================
-- 第三部分：扩展功能表
-- ============================================

-- 17. Agent思考轨迹表（reasoning_trace，对应 ReasoningTraceEntity 实体）
-- 记录智能体的完整推理链路，支撑 Agent 编排器（Orchestrator）的历史追溯与前端展示
CREATE TABLE IF NOT EXISTS reasoning_trace (
    id                  VARCHAR(64)     PRIMARY KEY                  COMMENT '轨迹ID',
    execution_id        VARCHAR(64)                                  COMMENT '执行ID（关联 agent_executions）',
    user_id             VARCHAR(64)                                  COMMENT '用户ID',
    agent_id            VARCHAR(64)                                  COMMENT 'Agent ID',
    reasoning_level     VARCHAR(32)                                  COMMENT '推理层级',
    user_input          TEXT                                         COMMENT '用户输入',
    thinking_steps_json TEXT                                         COMMENT '思考步骤JSON',
    output              TEXT                                         COMMENT '输出内容',
    duration            BIGINT                                       COMMENT '执行耗时（ms）',
    status              VARCHAR(32)                                  COMMENT '状态',
    is_deleted          TINYINT(1)      DEFAULT 0                    COMMENT '软删除标记',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    INDEX idx_rt_execution (execution_id),
    INDEX idx_rt_user (user_id),
    INDEX idx_rt_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent思考轨迹表';

-- 18. 自适应调整记录表（adaptive_adjustment，对应 AdaptiveAdjustment 实体）
-- 记录系统对用户学习路径的每次自适应调整（复习插入/进阶推荐/计划调整/资源推荐/难度调整），
-- 由 AdaptiveEngineService 驱动，支撑学习路径的动态优化
CREATE TABLE IF NOT EXISTS adaptive_adjustment (
    id                  VARCHAR(36)     PRIMARY KEY                  COMMENT '调整记录ID（UUID）',
    user_id             VARCHAR(50)     NOT NULL                     COMMENT '用户ID',
    path_id             VARCHAR(255)    NOT NULL                     COMMENT '关联学习路径ID',
    adjustment_type     VARCHAR(50)     NOT NULL                     COMMENT '调整类型：review_insert/advance_recommend/plan_adjust/resource_recommend/difficulty_adjust',
    trigger_reason      VARCHAR(500)    NOT NULL                     COMMENT '触发原因（如：测评正确率由85%降至45%）',
    adjustment_detail   JSON                                         COMMENT '调整详情（JSON：调整前后的任务ID、顺序、目标等）',
    effect_metric       VARCHAR(200)                                 COMMENT '效果指标（如：正确率提升20%）',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    INDEX idx_adj_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自适应调整记录表';

-- 19. 用户推荐记录表（user_recommendation，对应 UserRecommendation 实体）
-- 记录基于用户学习进度生成的个性化推荐（资源/课程/知识块/学习路径），
-- 支撑未来个性化推荐页面的点击/消费状态流转
CREATE TABLE IF NOT EXISTS user_recommendation (
    id              VARCHAR(36)     PRIMARY KEY                      COMMENT '推荐记录ID（UUID）',
    user_id         VARCHAR(50)     NOT NULL                         COMMENT '用户ID',
    path_id         VARCHAR(255)                                     COMMENT '关联学习路径ID',
    content_type    VARCHAR(50)     NOT NULL                         COMMENT '内容类型：resource/course/knowledge_block/learning_path',
    content_id      VARCHAR(255)    NOT NULL                         COMMENT '内容ID',
    title           VARCHAR(255)    NOT NULL                         COMMENT '推荐标题',
    description     TEXT                                             COMMENT '推荐描述',
    match_score     FLOAT           DEFAULT 0                        COMMENT '匹配度（0-1）',
    match_reason    VARCHAR(255)                                     COMMENT '匹配原因说明',
    priority        VARCHAR(20)     DEFAULT 'normal'                 COMMENT '优先级：high/normal/low',
    status          VARCHAR(20)     DEFAULT 'pending'                COMMENT '状态：pending/clicked/dismissed/consumed',
    generated_at    DATETIME        DEFAULT CURRENT_TIMESTAMP        COMMENT '生成时间',
    consumed_at     DATETIME                                         COMMENT '消费时间',

    INDEX idx_rec_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推荐记录表';

-- ============================================
-- 第四部分：业务功能表
-- ============================================

-- 20. 学习笔记表（notes，对应 Note 实体）
CREATE TABLE IF NOT EXISTS notes (
    id           BIGINT          PRIMARY KEY AUTO_INCREMENT          COMMENT '笔记ID',
    user_id      BIGINT          NOT NULL                            COMMENT '用户ID',
    title        VARCHAR(255)    NOT NULL                            COMMENT '笔记标题',
    content      TEXT                                                COMMENT '笔记正文',
    code_blocks  TEXT                                                COMMENT '代码块内容',
    tags         VARCHAR(255)                                        COMMENT '标签（逗号分隔）',
    summary      VARCHAR(1000)                                       COMMENT '摘要',
    category     VARCHAR(100)                                        COMMENT '分类',
    word_count   INT             DEFAULT 0                           COMMENT '字数',
    created_at   DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    updated_at   DATETIME        DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP          COMMENT '更新时间',
    INDEX idx_notes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习笔记表';

-- 21. 安全审计日志表（audit_log，对应 AuditLog 实体）
-- 安全模块记录谁在什么时间执行了什么操作，请求/响应脱敏后入库，供安全追溯
CREATE TABLE IF NOT EXISTS audit_log (
    id            VARCHAR(36)     PRIMARY KEY                        COMMENT '审计ID（UUID）',
    user_id       VARCHAR(50)     NOT NULL                           COMMENT '操作用户ID',
    action        VARCHAR(50)     NOT NULL                           COMMENT '操作类型：LOGIN/CHAT/TOOL_CALL/AGENT_EXEC/INPUT_BLOCKED 等',
    resource_type VARCHAR(50)                                        COMMENT '资源类型：PATH/TASK/KNOWLEDGE/REPORT/tool 等',
    resource_id   VARCHAR(100)                                       COMMENT '资源ID（Agent ID、工具名、文档ID等）',
    request       TEXT                                               COMMENT '请求内容（脱敏+截断后）',
    response      TEXT                                               COMMENT '响应摘要（脱敏+截断后）',
    status        VARCHAR(20)                                        COMMENT '状态：SUCCESS/FAILURE/BLOCKED',
    ip_address    VARCHAR(50)                                        COMMENT '客户端IP',
    user_agent    VARCHAR(255)                                       COMMENT '客户端UA',
    duration_ms   INT                                                COMMENT '执行耗时（毫秒）',
    error_message TEXT                                               COMMENT '错误信息（如有）',
    created_at    DATETIME                                           COMMENT '创建时间',
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全审计日志表';

-- ============================================
-- 第五部分：积分与签到配置
-- ============================================

-- 22. 用户积分总表（user_points，对应 UserPoints 实体）
-- 记录每个用户的积分余额，使用乐观锁保证并发安全
CREATE TABLE IF NOT EXISTS user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID，关联 users 表',
    total_earned BIGINT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    available_points BIGINT NOT NULL DEFAULT 0 COMMENT '可用积分',
    frozen_points BIGINT NOT NULL DEFAULT 0 COMMENT '冻结积分（用于处理中的消耗）',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁，防并发）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_up_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分总表';

-- 23. 积分流水表（point_transactions，对应 PointTransaction 实体）
-- 记录每一笔积分变动的明细，用于对账和追溯
CREATE TABLE IF NOT EXISTS point_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    transaction_type VARCHAR(30) NOT NULL COMMENT '交易类型：CHECKIN/CHECKIN_BONUS/CONSUME/ADMIN_GRANT/ADMIN_REVOKE',
    points BIGINT NOT NULL COMMENT '积分变动数量（正数增加，负数减少）',
    balance_before BIGINT NOT NULL COMMENT '变动前余额',
    balance_after BIGINT NOT NULL COMMENT '变动后余额',
    source VARCHAR(50) COMMENT '来源说明（如：CHAT/AGENT/LEARNING_PATH/ADMIN）',
    reference_id BIGINT COMMENT '关联业务ID（签到记录ID、对话ID等）',
    description VARCHAR(255) COMMENT '备注信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_pt_user_id (user_id),
    INDEX idx_pt_created_at (created_at),
    INDEX idx_pt_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表';

-- 24. 签到配置表（checkin_config，对应 CheckinConfig 实体）
-- 配置各项签到和消费积分规则，支持运行时动态调整
CREATE TABLE IF NOT EXISTS checkin_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键名',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
    description VARCHAR(255) COMMENT '配置描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到配置表';

-- 初始化签到配置默认值
INSERT INTO checkin_config (config_key, config_value, description) VALUES
('daily_checkin_points', '10', '每日签到基础积分'),
('consecutive_days', '7', '连续签到奖励周期（天数）'),
('consecutive_bonus_points', '20', '连续签到奖励积分'),
('chat_consume_points', '5', 'AI对话消耗积分'),
('agent_consume_points', '10', '智能体调用消耗积分'),
('learning_path_consume_points', '20', '学习路径生成消耗积分')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ============================================
-- 第六部分：视图
-- ============================================

-- 知识库状态视图
CREATE OR REPLACE VIEW knowledge_status_view AS
SELECT
    COUNT(*) AS document_count,
    COALESCE(SUM(CASE WHEN status IN ('ready','processing') THEN 1 ELSE 0 END), 0) AS active_document_count,
    COALESCE(SUM(chunks), 0) AS chunk_count,
    COALESCE(SUM(CASE WHEN status = 'ready' THEN 1 ELSE 0 END), 0) AS ready_count,
    COUNT(CASE WHEN status = 'ready' THEN 1 END) > 0 AS connected
FROM knowledge_documents;

-- 工具统计视图
CREATE OR REPLACE VIEW tool_stats_view AS
SELECT
    COUNT(*) AS total_calls,
    COUNT(DISTINCT tool_id) AS tool_count,
    COUNT(CASE WHEN status = 'success' THEN 1 END) AS success_count
FROM tool_execution_records;

-- ============================================
-- 第五部分：废弃表清理
-- adaptive_adjustment 和 user_recommendation 已于 v3.2 重新启用，
-- 用于支撑自适应引擎的调整记录与推荐功能。
-- ============================================