-- ============================================
-- AI 学习规划师 - 数据库完整初始化脚本（整合版 v3.0）
-- ============================================
-- 本文件由原 sql 目录下 8 个脚本整合而成：
--   init.sql（核心 11 表 + 视图）
--   assessment_records.sql（测评记录表）
--   migrate_checkin.sql（打卡表 + 用户打卡字段）
--   migrate_compact.sql（v2.0 历史数据迁移，见第四部分）
--   migration_v2__assessment_enhancement.sql（测评增强，见第四部分）
--   migration_v3__user_notification.sql（智能通知中心表）
--   migration_v4__intervention_preferences.sql（用户干预阈值字段）
--   rag_mcp_tables.sql（RAG/MCP 视图，已合并至第三部分）
--
-- 执行说明：
--   - 新环境：直接执行本文件（第一~三部分）
--   - 旧库升级：先执行第一部分，再按需执行第四部分历史迁移
--   - JPA ddl-auto: update 也会自动建表/加列，本脚本留档与手工初始化用
-- ============================================

-- ============================================
-- 第一部分：核心表
-- 合并前：17 张表 → 合并后：11 张表（2026-07 精简）
--   合并变更：
--     user_profiles -> users（字段并入）
--     learning_path_nodes -> learning_paths.nodes（JSON 列）
--     learning_records -> learning_paths.nodes（JSON 列）
--     execution_logs + execution_results -> agent_executions
--     achievements + interventions -> learning_events
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
    -- 以下字段原属于 user_profiles 表，已合并到 users
    learning_style    VARCHAR(50)                                    COMMENT '学习风格',
    level             INT             DEFAULT 1                      COMMENT '等级',
    active_hours      VARCHAR(100)                                   COMMENT '活跃时间段',
    target_field      VARCHAR(255)                                   COMMENT '目标领域',
    interests         TEXT                                           COMMENT '兴趣标签',
    weak_points       TEXT                                           COMMENT '薄弱项',
    -- 打卡统计字段（原 migrate_checkin.sql）
    continuous_checkin_days INT        DEFAULT 0                     COMMENT '连续打卡天数',
    total_checkin_days       INT        DEFAULT 0                    COMMENT '总打卡天数',
    -- 智能干预阈值字段（原 migration_v4__intervention_preferences.sql）
    intervention_enabled                  TINYINT(1)   DEFAULT 1     COMMENT '干预提醒总开关：1开启 0关闭',
    intervention_progress_threshold       FLOAT        DEFAULT 65    COMMENT '进度提醒阈值（%）：完成率低于该值生成预警提醒',
    intervention_score_decline_threshold  FLOAT        DEFAULT 10    COMMENT '测评降幅阈值（%）：分数降幅超过该值生成预警提醒',
    intervention_inactive_days            INT          DEFAULT 3     COMMENT '连续未登录预警天数',
    created_at        DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    updated_at        DATETIME        DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP    COMMENT '更新时间',
    last_login_at     DATETIME                                       COMMENT '最近登录时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 学习路径表（含 nodes JSON 列，合并原 learning_path_nodes + learning_records）
CREATE TABLE IF NOT EXISTS learning_paths (
    id                   VARCHAR(255)    PRIMARY KEY                 COMMENT '路径ID',
    user_id              VARCHAR(255)    NOT NULL                    COMMENT '用户ID',
    name                 VARCHAR(255)    NOT NULL                    COMMENT '路径名称',
    description          TEXT                                        COMMENT '路径描述',
    version              INT             DEFAULT 1                   COMMENT '版本号',
    is_active            BOOLEAN         DEFAULT TRUE                COMMENT '是否激活',
    completion_percentage FLOAT          DEFAULT 0                   COMMENT '完成百分比',
    nodes                JSON                                        COMMENT '节点列表JSON（含nodeId, nodeName, sequenceOrder, status, mastery, estimatedHours, timeSpent等）',
    source               VARCHAR(20)     DEFAULT 'manual'            COMMENT '路径来源：manual-手动创建, ai_chat-对话生成, ai_assessment-测评生成, ai_quick-一键生成',
    created_at           DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    updated_at           DATETIME        DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_lp_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习路径表';

-- 3. Agent 执行记录（合并 execution_logs + execution_results）
CREATE TABLE IF NOT EXISTS agent_executions (
    id               VARCHAR(255)    PRIMARY KEY                    COMMENT '执行记录ID',
    user_id          VARCHAR(255)                                   COMMENT '用户ID',
    agent_id         VARCHAR(255)    NOT NULL                       COMMENT 'Agent ID',
    agent_name       VARCHAR(255)                                   COMMENT 'Agent名称',
    record_type      VARCHAR(20)     NOT NULL                       COMMENT '记录类型：LOG（步骤日志）/ RESULT（执行结果）',
    -- 日志相关字段（LOG）
    session_id       VARCHAR(100)                                   COMMENT '会话ID',
    execution_id     VARCHAR(100)                                   COMMENT '执行ID',
    type             VARCHAR(50)                                    COMMENT '日志类型（think/act/observe等）',
    title            VARCHAR(255)                                   COMMENT '日志标题',
    content          TEXT                                           COMMENT '日志内容',
    step_number      INT                                            COMMENT '步骤编号',
    step_order       INT                                            COMMENT '步骤顺序',
    phase            VARCHAR(20)                                    COMMENT '执行阶段',
    -- 结果相关字段（RESULT）
    task_description TEXT                                           COMMENT '任务描述',
    result_type      VARCHAR(50)                                    COMMENT '结果类型（exercise/plan/explanation等）',
    result_content   TEXT                                           COMMENT '结果内容（JSON）',
    result_summary   TEXT                                           COMMENT '结果摘要',
    duration         BIGINT                                         COMMENT '执行耗时（ms）',
    -- 通用字段
    status           VARCHAR(20)                                    COMMENT '状态',
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    is_deleted       TINYINT(1)      DEFAULT 0                      COMMENT '软删除标记：0未删除 1已删除',

    INDEX idx_agent_session (session_id),
    INDEX idx_agent_execution (execution_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_agent_user (user_id),
    INDEX idx_record_type (record_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent执行记录表';

-- 4. 学习行为事件表（合并 achievements + interventions）
CREATE TABLE IF NOT EXISTS learning_events (
    id          VARCHAR(255)    PRIMARY KEY                         COMMENT '事件ID',
    user_id     VARCHAR(255)    NOT NULL                            COMMENT '用户ID',
    event_type  VARCHAR(30)     NOT NULL                            COMMENT '事件类型：achievement（成就）/ intervention（干预）',
    event_key   VARCHAR(255)    NOT NULL                            COMMENT '事件标识键：成就则为badge名称，干预则为干预类型',
    description TEXT                                                COMMENT '事件描述',
    metadata    TEXT                                                COMMENT '扩展数据（JSON）',
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',

    INDEX idx_event_user (user_id),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习行为事件表';

-- 5. 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id            VARCHAR(255)    PRIMARY KEY                      COMMENT '文档ID',
    title         VARCHAR(255)    NOT NULL                         COMMENT '文档标题（原始文件名）',
    type          VARCHAR(20)                                      COMMENT '文件类型（PDF/Word/Markdown/TXT）',
    size          VARCHAR(20)                                      COMMENT '文件大小（格式化后）',
    status        VARCHAR(20)     NOT NULL DEFAULT 'processing'    COMMENT '处理状态：processing/ready/error',
    file_path     VARCHAR(500)                                     COMMENT '文件存储路径',
    chunks        INT             DEFAULT 0                        COMMENT '知识块数量',
    description   TEXT                                             COMMENT '文档描述',
    uploaded_at   DATETIME        DEFAULT CURRENT_TIMESTAMP        COMMENT '上传时间',
    processed_at  DATETIME                                         COMMENT '处理完成时间',
    user_id       VARCHAR(50)                                      COMMENT '用户ID（数据隔离）',

    INDEX idx_kd_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- 7. 知识块表
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT      COMMENT '块ID',
    doc_id          VARCHAR(255)    NOT NULL                        COMMENT '所属文档ID',
    chunk_index     INT             NOT NULL                        COMMENT '块序号',
    content         TEXT            NOT NULL                        COMMENT '块内容',
    content_preview VARCHAR(500)                                    COMMENT '内容预览',
    char_count      INT                                             COMMENT '字符数',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP     COMMENT '更新时间',

    INDEX idx_chunk_doc_id (doc_id),
    INDEX idx_chunk_doc_id_index (doc_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识块表';

-- 6. 对话历史表
CREATE TABLE IF NOT EXISTS chat_histories (
    id          VARCHAR(255)    PRIMARY KEY                        COMMENT '聊天ID',
    session_id  VARCHAR(255)    NOT NULL                           COMMENT '会话ID',
    user_id     VARCHAR(255)                                       COMMENT '用户ID',
    role        VARCHAR(255)    NOT NULL                           COMMENT '角色',
    content     TEXT            NOT NULL                           COMMENT '内容',
    agent_type  VARCHAR(50)                                        COMMENT 'AI体类型',
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',

    INDEX idx_chat_session (session_id),
    INDEX idx_chat_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史表';

-- 10. 工具执行记录表
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
-- 第二部分：JPA 实体补充表（ddl-auto: update 会自动创建，此脚本留档/手工初始化用）
-- ============================================

-- 12. 测评答题记录表（原 assessment_records.sql + migration_v2 合并）
CREATE TABLE IF NOT EXISTS assessment_records (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    subject     VARCHAR(100) NOT NULL COMMENT '科目名称（支持自定义）',
    difficulty  VARCHAR(20) DEFAULT 'medium' COMMENT '难度：easy/medium/hard',
    score       INT DEFAULT 0 COMMENT '得分',
    total       INT DEFAULT 0 COMMENT '总题数',
    details     JSON COMMENT '详情JSON（题目答案等）',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '测评时间',
    INDEX idx_ar_user (user_id),
    INDEX idx_ar_subject (subject),
    INDEX idx_ar_user_subject (user_id, subject),
    INDEX idx_ar_user_createdat (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测评答题记录表';

-- 13. 打卡记录表（原 migrate_checkin.sql）
CREATE TABLE IF NOT EXISTS checkin_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联 users 表',
    checkin_date DATE NOT NULL COMMENT '打卡日期（格式：YYYY-MM-DD）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    UNIQUE KEY uk_user_date (user_id, checkin_date) COMMENT '唯一索引：防止同一用户同一天重复打卡'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户每日打卡记录表';

-- 14. 用户通知表（智能通知中心，原 migration_v3__user_notification.sql）
-- 优先级：EMERGENCY（紧急干预）/ WARNING（预警提醒）/ INFO（普通消息）
-- 类别：PROGRESS（进度）/ KNOWLEDGE（知识点）/ SYSTEM（系统）
CREATE TABLE IF NOT EXISTS user_notifications (
    id           BIGINT          PRIMARY KEY AUTO_INCREMENT          COMMENT '通知ID',
    user_id      BIGINT          NOT NULL                            COMMENT '用户ID',
    title        VARCHAR(255)    NOT NULL                            COMMENT '通知标题',
    content      TEXT                                                COMMENT '通知内容（支持换行）',
    priority     VARCHAR(20)     NOT NULL DEFAULT 'INFO'             COMMENT '优先级：EMERGENCY（紧急）/ WARNING（预警）/ INFO（普通）',
    category     VARCHAR(30)     NOT NULL DEFAULT 'SYSTEM'           COMMENT '类别：PROGRESS（进度）/ KNOWLEDGE（知识点）/ SYSTEM（系统）',
    action_type  VARCHAR(30)                                         COMMENT '快捷操作类型：ADJUST_PLAN / START_REVIEW / VIEW_WEAKNESS / VIEW_DETAIL',
    action_data  TEXT                                                COMMENT '快捷操作参数（JSON，如 {"pathId":"xxx","knowledgePoint":"动态规划"}）',
    is_read      TINYINT(1)      DEFAULT 0                           COMMENT '是否已读：0未读 1已读',
    is_handled   TINYINT(1)      DEFAULT 0                           COMMENT '是否已处理（干预类专用）：0未处理 1已处理',
    created_at   DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    handled_at   DATETIME                                            COMMENT '处理时间',

    INDEX idx_notif_user (user_id),
    INDEX idx_notif_user_read (user_id, is_read),
    INDEX idx_notif_priority (priority),
    INDEX idx_notif_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表（智能通知中心）';

-- 15. 题库表（question_bank，对应 Question 实体，原 migration_v2 涉及）
CREATE TABLE IF NOT EXISTS question_bank (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT       COMMENT '题目ID',
    subject         VARCHAR(50)     NOT NULL                         COMMENT '科目：python/java/cpp/algorithm/database/network/system_design',
    question_text   TEXT            NOT NULL                         COMMENT '题目文本',
    options         JSON            NOT NULL                         COMMENT '选项（JSON数组）',
    correct_answer  VARCHAR(255)    NOT NULL                         COMMENT '正确答案（选项索引 A=0, B=1, C=2, D=3）',
    difficulty      VARCHAR(20)     DEFAULT 'medium'                 COMMENT '难度：easy/medium/hard',
    explanation     TEXT                                            COMMENT '解析',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',

    INDEX idx_qb_subject (subject),
    INDEX idx_qb_subject_difficulty (subject, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

-- 16. 学习记录表（learning_records，对应 LearningRecord 实体）
CREATE TABLE IF NOT EXISTS learning_records (
    id            BIGINT          PRIMARY KEY AUTO_INCREMENT         COMMENT '记录ID',
    user_id       VARCHAR(255)    NOT NULL                           COMMENT '用户ID',
    path_id       VARCHAR(255)    NOT NULL                           COMMENT '学习路径ID',
    node_id       VARCHAR(255)    NOT NULL                           COMMENT '路径节点ID（任务ID）',
    node_type     VARCHAR(50)                                        COMMENT '节点类型（task等）',
    status        VARCHAR(20)     NOT NULL DEFAULT 'pending'         COMMENT '状态：pending/in_progress/completed',
    mastery_level FLOAT                                              COMMENT '掌握度（0-5分）',
    time_spent    INT             DEFAULT 0                          COMMENT '学习时长（分钟）',
    completed_at  DATETIME                                           COMMENT '完成时间',
    created_at    DATETIME        DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    updated_at    DATETIME        DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP        COMMENT '更新时间',

    INDEX idx_lr_user_created (user_id, created_at),
    INDEX idx_lr_path_id (path_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表（路径任务进度）';

-- 17. 每日任务表（daily_tasks，对应 DailyTask 实体）
CREATE TABLE IF NOT EXISTS daily_tasks (
    id                VARCHAR(255)    PRIMARY KEY                    COMMENT '任务ID',
    path_id           VARCHAR(255)    NOT NULL                       COMMENT '所属学习路径ID',
    user_id           VARCHAR(255)    NOT NULL                       COMMENT '用户ID',
    task_date         DATE            NOT NULL                       COMMENT '任务日期',
    title             VARCHAR(255)    NOT NULL                       COMMENT '任务标题',
    type              VARCHAR(50)     NOT NULL                       COMMENT '任务类型：read/video/practice/review',
    estimated_minutes INT                                            COMMENT '预计学习时长（分钟）',
    status            VARCHAR(20)     NOT NULL DEFAULT 'pending'     COMMENT '状态：pending/in_progress/completed/skipped',
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

-- 18. 自适应调整记录表（adaptive_adjustment，对应 AdaptiveAdjustment 实体）
-- 2026-08 新增：记录系统对用户学习路径的每次自适应调整（复习插入/进阶推荐/计划调整/资源推荐/难度调整）
CREATE TABLE IF NOT EXISTS adaptive_adjustment (
    id                VARCHAR(36)     PRIMARY KEY                    COMMENT '调整记录ID（UUID）',
    user_id           VARCHAR(50)     NOT NULL                       COMMENT '用户ID',
    path_id           VARCHAR(255)    NOT NULL                       COMMENT '学习路径ID',
    adjustment_type   VARCHAR(50)     NOT NULL                       COMMENT '调整类型：review_insert（复习插入）/advance_recommend（进阶推荐）/plan_adjust（计划调整）/resource_recommend（资源推荐）/difficulty_adjust（难度调整）',
    trigger_reason    VARCHAR(500)    NOT NULL                       COMMENT '触发原因（如：测评正确率由85%降至45%）',
    adjustment_detail JSON                                           COMMENT '调整详情（JSON：调整前后的任务ID、顺序、目标等）',
    effect_metric     VARCHAR(200)                                   COMMENT '效果指标（如：正确率提升20%）',
    created_at        DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',

    INDEX idx_adj_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自适应调整记录表';

-- 19. 用户推荐记录表（user_recommendation，对应 UserRecommendation 实体）
-- 2026-08 新增：记录基于用户学习进度生成的个性化推荐（资源/课程/知识块/学习路径）与点击/消费状态
CREATE TABLE IF NOT EXISTS user_recommendation (
    id            VARCHAR(36)     PRIMARY KEY                        COMMENT '推荐ID（UUID）',
    user_id       VARCHAR(50)     NOT NULL                           COMMENT '用户ID',
    path_id       VARCHAR(255)                                       COMMENT '关联学习路径ID',
    content_type  VARCHAR(50)     NOT NULL                           COMMENT '内容类型：resource（资源）/course（课程）/knowledge_block（知识块）/learning_path（学习路径）',
    content_id    VARCHAR(255)    NOT NULL                           COMMENT '内容ID（资源ID/节点ID/路径ID）',
    title         VARCHAR(255)    NOT NULL                           COMMENT '推荐标题',
    description   TEXT                                               COMMENT '推荐描述',
    match_score   FLOAT           DEFAULT 0                          COMMENT '匹配度 0-1',
    match_reason  VARCHAR(255)                                       COMMENT '匹配原因',
    priority      VARCHAR(20)     DEFAULT 'normal'                   COMMENT '优先级：high/normal/low',
    status        VARCHAR(20)     DEFAULT 'pending'                  COMMENT '状态：pending（未点击）/clicked（已点击）/dismissed（已忽略）/consumed（已消费）',
    generated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP          COMMENT '生成时间',
    consumed_at   DATETIME                                           COMMENT '消费时间',

    INDEX idx_rec_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推荐记录表';

-- 20. 学习笔记表（notes，对应 Note 实体）
-- 2026-08 新增：学习笔记的增删改查与导出（markdown/text）
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
-- 2026-08 新增：安全模块记录“谁在什么时间执行了什么操作”（工具调用/Agent执行/聊天/安全拦截等）
-- 请求/响应内容均经脱敏与截断后入库，供安全追溯
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
-- 第三部分：视图（原 init.sql + rag_mcp_tables.sql 合并去重）
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
-- 第四部分：历史迁移记录（仅供旧库升级参考，新环境无需执行）
-- ============================================

-- ------------------------------------------------------------
-- 4.1 旧库补列（原 migrate_checkin.sql 与 migration_v4 的 ALTER）
-- 注意：新库 users 表定义已含以下字段，仅当旧库缺少时手动执行
-- ------------------------------------------------------------
-- ALTER TABLE users ADD COLUMN continuous_checkin_days INT DEFAULT 0 COMMENT '连续打卡天数';
-- ALTER TABLE users ADD COLUMN total_checkin_days INT DEFAULT 0 COMMENT '总打卡天数';
-- ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'USER' COMMENT '用户角色：USER普通用户 ADMIN管理员';
-- ALTER TABLE users ADD COLUMN intervention_enabled TINYINT(1) DEFAULT 1 COMMENT '干预提醒总开关';
-- ALTER TABLE users ADD COLUMN intervention_progress_threshold FLOAT DEFAULT 65 COMMENT '进度提醒阈值（%）';
-- ALTER TABLE users ADD COLUMN intervention_score_decline_threshold FLOAT DEFAULT 10 COMMENT '测评降幅阈值（%）';
-- ALTER TABLE users ADD COLUMN intervention_inactive_days INT DEFAULT 3 COMMENT '连续未登录预警天数';

-- ------------------------------------------------------------
-- 4.2 测评模块增强（原 migration_v2__assessment_enhancement.sql，幂等可执行）
-- 适用于旧库：question_bank / assessment_records 表补充 difficulty 字段与索引
-- ------------------------------------------------------------
-- SET @dbname = DATABASE();
-- SET @tablename = 'question_bank';
-- SET @columnname = 'difficulty';
-- SET @preparedStatement = (SELECT IF(
--     (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--      WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
--     'SELECT 1',
--     'ALTER TABLE question_bank ADD COLUMN difficulty VARCHAR(20) DEFAULT ''medium'' COMMENT ''难度: easy/medium/hard'''
-- ));
-- PREPARE stmt FROM @preparedStatement;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 4.2.1 跨用户数据隔离升级（2026-08）：旧库 knowledge_documents 补 user_id 列
-- 新库建表已包含 user_id 列，仅旧库升级时手动执行（MySQL 不支持 ADD COLUMN IF NOT EXISTS）
-- ALTER TABLE knowledge_documents ADD COLUMN user_id VARCHAR(50) COMMENT '用户ID（数据隔离）' AFTER processed_at;
-- ALTER TABLE knowledge_documents ADD INDEX idx_kd_user_id (user_id);
-- 历史数据归属迁移（默认归入 admin 用户，请按实际情况调整）：
-- UPDATE knowledge_documents SET user_id = '1' WHERE user_id IS NULL;
-- ------------------------------------------------------------
-- 4.4 数据隔离历史数据归属迁移（2026-08）：将 user_id 为 NULL/空的历史数据归属到 admin
-- 适用于实施数据隔离前已存在的历史数据；新数据已正确记录 user_id，无需执行
-- 注意：knowledge_chunks 表无 user_id 列，知识块跟随所属文档（doc_id）归属，无需迁移
-- ------------------------------------------------------------
-- SET @admin_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
--
-- -- 工具执行记录
-- UPDATE tool_execution_records SET user_id = @admin_id WHERE user_id IS NULL OR user_id = '';
--
-- -- 知识库文档
-- UPDATE knowledge_documents SET user_id = @admin_id WHERE user_id IS NULL OR user_id = '';
--
-- -- Agent 执行记录（日志 + 结果）
-- UPDATE agent_executions SET user_id = @admin_id WHERE user_id IS NULL OR user_id = '';

-- ------------------------------------------------------------
-- 4.3 v2.0 数据迁移（原 migrate_compact.sql）
-- 适用于 v1.0 旧库升级：合并执行日志/成就/干预表到新表
-- 新环境执行会因旧表不存在而报错，请勿执行！
-- ------------------------------------------------------------
-- Step 1: Drop new tables (clean state for re-run safety)
-- DROP TABLE IF EXISTS agent_executions;
-- DROP TABLE IF EXISTS learning_events;
--
-- Step 2: Create new tables（结构见第一部分，旧库可先执行第一部分）
--
-- Step 3: Migrate execution_results -> agent_executions (RESULT)
-- INSERT IGNORE INTO agent_executions (id, user_id, agent_id, agent_name, record_type,
--     task_description, result_type, result_content, result_summary, duration, status, created_at)
-- SELECT id, user_id, agent_id, agent_name, 'RESULT',
--     task_description, result_type, result_content, result_summary, duration, status, created_at
-- FROM execution_results;
--
-- Migrate execution_logs -> agent_executions (LOG)
-- INSERT IGNORE INTO agent_executions (id, user_id, agent_id, agent_name, record_type,
--     session_id, execution_id, type, title, content, step_number, step_order, phase,
--     task_description, status, created_at)
-- SELECT id, user_id, agent_id, agent_name, 'LOG',
--     session_id, execution_id, type, title, content, step_number, step_order, phase,
--     task_description, status, created_at
-- FROM execution_logs;
--
-- Migrate achievements -> learning_events
-- INSERT IGNORE INTO learning_events (id, user_id, event_type, event_key, description, created_at)
-- SELECT id, user_id, 'achievement', badge, description, unlocked_at
-- FROM achievements;
--
-- Migrate interventions -> learning_events
-- INSERT IGNORE INTO learning_events (id, user_id, event_type, event_key, description, metadata, created_at)
-- SELECT id, user_id, 'intervention', type, content,
--        CASE WHEN `trigger` IS NOT NULL THEN JSON_OBJECT('trigger', `trigger`) ELSE NULL END,
--        created_at
-- FROM interventions;
--
-- Step 4: Alter existing tables（旧库 users/learning_paths 补列）
-- ALTER TABLE users
--     ADD COLUMN learning_style  VARCHAR(50),
--     ADD COLUMN level           INT DEFAULT 1,
--     ADD COLUMN active_hours    VARCHAR(100),
--     ADD COLUMN target_field    VARCHAR(255),
--     ADD COLUMN interests       TEXT,
--     ADD COLUMN weak_points     TEXT;
--
-- ALTER TABLE learning_paths
--     ADD COLUMN nodes JSON;
--
-- Step 5: Drop old tables
-- DROP TABLE IF EXISTS user_preferences;
-- DROP TABLE IF EXISTS user_profiles;
-- DROP TABLE IF EXISTS achievements;
-- DROP TABLE IF EXISTS interventions;
-- DROP TABLE IF EXISTS execution_logs;
-- DROP TABLE IF EXISTS execution_results;
-- DROP TABLE IF EXISTS learning_path_nodes;
-- DROP TABLE IF EXISTS learning_records;

-- 16. 用户积分总表（user_points）
CREATE TABLE IF NOT EXISTS user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID，关联 users 表',
    total_earned BIGINT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    available_points BIGINT NOT NULL DEFAULT 0 COMMENT '可用积分',
    frozen_points BIGINT NOT NULL DEFAULT 0 COMMENT '冻结积分（用于处理中的消耗）',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_up_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分总表';

-- 17. 积分流水表（point_transactions）
CREATE TABLE IF NOT EXISTS point_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    transaction_type VARCHAR(30) NOT NULL COMMENT '交易类型：CHECKIN(签到), CHECKIN_BONUS(连续奖励), CONSUME(消耗), ADMIN_GRANT(管理员发放), ADMIN_REVOKE(管理员扣除)',
    points BIGINT NOT NULL COMMENT '积分变动数量（正数增加，负数减少）',
    balance_before BIGINT NOT NULL COMMENT '变动前余额',
    balance_after BIGINT NOT NULL COMMENT '变动后余额',
    source VARCHAR(50) COMMENT '来源说明（如：CHAT, AGENT, LEARNING_PATH, ADMIN）',
    reference_id BIGINT COMMENT '关联的业务ID（如：签到记录ID、对话ID等）',
    description VARCHAR(255) COMMENT '备注信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_pt_user_id (user_id),
    INDEX idx_pt_created_at (created_at),
    INDEX idx_pt_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水表';

-- 18. 签到配置表（checkin_config）
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
