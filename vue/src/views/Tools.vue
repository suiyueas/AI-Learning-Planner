<template>
  <div ref="pageRef" class="tools-page">
    <!-- 深空动态背景 -->
    <div class="bg-deep">
      <div class="aurora-orb orb-cyan"></div>
      <div class="aurora-orb orb-purple"></div>
      <div class="aurora-orb orb-pink"></div>
    </div>
    <div class="grid-overlay"></div>

    <!-- 浮动粒子 -->
    <div class="particles-wrap">
      <div
        v-for="p in particles"
        :key="p.id"
        class="particle"
        :style="{
          left: p.x + '%',
          top: p.y + '%',
          width: p.size + 'px',
          height: p.size + 'px',
          animationDuration: p.duration + 's',
          animationDelay: p.delay + 's',
          opacity: p.opacity
        }"
      ></div>
    </div>

    <!-- ===== 顶部标题区 ===== -->
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">⚡</span>
            <span>MCP 工具中心</span>
          </h1>
          <p class="page-subtitle">
            {{ toolSummary.total }} 个工具 ·
            <span class="stat-ok">{{ toolSummary.available }} 个可用</span> ·
            <span class="stat-run">{{ toolSummary.executing }} 个执行中</span> ·
            <span class="stat-cfg">{{ configuringCount }} 个配置中</span>
          </p>
        </div>
        <div class="header-kpis">
          <div class="kpi">
            <span class="kpi-num">{{ toolSummary.total }}</span>
            <span class="kpi-label">工具总数</span>
          </div>
          <div class="kpi">
            <span class="kpi-num kpi-ok">{{ toolSummary.available }}</span>
            <span class="kpi-label">可用</span>
          </div>
          <div class="kpi">
            <span class="kpi-num kpi-accent">{{ totalUsage }}</span>
            <span class="kpi-label">总调用</span>
          </div>
          <div class="kpi">
            <span class="kpi-num">{{ executionHistory.length }}</span>
            <span class="kpi-label">执行日志</span>
          </div>
        </div>
      </div>

      <!-- 搜索栏 -->
      <div class="search-wrap">
        <div class="search-box">
          <svg class="search-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            v-model="searchQuery"
            type="text"
            class="search-field"
            placeholder="搜索工具名称、描述、分类..."
          />
          <span v-if="searchQuery" class="search-x" @click="searchQuery = ''">✕</span>
        </div>
      </div>
    </header>

    <!-- ===== 分类标签 ===== -->
    <nav class="cat-tabs">
      <button
        v-for="cat in categoriesWithCount"
        :key="cat.id"
        class="cat-tab"
        :class="{ active: activeCategory === cat.id }"
        :style="activeCategory === cat.id ? { '--tab-c': cat.color } : {}"
        @click="setCategory(cat.id)"
      >
        <span class="tab-ico">{{ cat.icon }}</span>
        <span class="tab-name">{{ cat.name }}</span>
        <span class="tab-num">{{ cat.count }}</span>
      </button>
    </nav>

    <!-- ===== 工具卡片网格 ===== -->
    <section class="tools-grid">
      <TransitionGroup name="card-trans">
        <!-- 旋转边框光效 -->
        <div
          v-for="(tool, idx) in displayTools"
          :key="tool.id"
          class="tool-card"
          :class="{
            'is-executing': tool.status === 'executing',
            'is-configuring': tool.status === 'configuring',
            'is-upgrading': tool.status === 'upgrading'
          }"
          :style="{
            '--c': getToolColor(tool),
            animationDelay: (idx * 0.06) + 's'
          }"
          @click="handleExecute(tool)"
>
          <div class="card-glow-border"></div>

          <!-- 顶部分类色条 -->
          <div class="card-stripe" :style="{ background: `linear-gradient(90deg, ${getToolColor(tool)}, transparent)` }"></div>

          <!-- 图标 + 状态指示 -->
          <div class="card-top">
            <div class="card-icon-box" :style="{ background: getToolColor(tool) + '15' }">
              <span class="card-emoji">{{ tool.icon }}</span>
              <div class="icon-pulse" :style="{ background: getToolColor(tool) }"></div>
            </div>
            <div class="card-status-badge" :class="tool.status">
              <span class="badge-dot"></span>
              <span class="badge-text">{{ getStatusLabel(tool.status) }}</span>
            </div>
          </div>

          <!-- 名称 & 描述 -->
          <div class="card-name-row">
            <h3 class="card-name">{{ tool.name }}</h3>
            <span v-if="tool.aiEnabled" class="ai-badge" title="由大模型驱动">🤖 AI</span>
          </div>
          <p class="card-desc">{{ tool.description }}</p>

          <!-- 使用率进度条 -->
          <div class="card-bar-wrap">
            <div class="card-bar-track">
              <div
                class="card-bar-fill"
                :style="{ width: barAnimated[idx] ? getUsagePercent(tool) + '%' : '0%' }"
              ></div>
            </div>
            <span class="bar-count">{{ getUsage(tool) }} 次调用</span>
          </div>

          <!-- 执行按钮 -->
          <button
            class="card-exec-btn"
            :class="{ running: tool.status === 'executing', disabled: tool.status !== 'available' && tool.status !== 'executing' }"
            :disabled="tool.status === 'executing' || tool.status === 'configuring' || tool.status === 'upgrading'"
            @click.stop="handleExecute(tool)"
          >
            <span v-if="tool.status === 'executing'" class="btn-spinner"></span>
            <span v-else-if="tool.status === 'configuring'" class="btn-ico">⚙️</span>
            <span v-else-if="tool.status === 'upgrading'" class="btn-ico">⬆️</span>
            <span v-else class="btn-ico">▶</span>
            <span>{{ getActionLabel(tool.status) }}</span>
          </button>
        </div>
      </TransitionGroup>

      <!-- 空状态 -->
      <div v-if="displayTools.length === 0" class="empty-box">
        <div class="empty-ring">
          <span class="empty-ico">🔍</span>
        </div>
        <p class="empty-msg">未找到匹配的工具</p>
        <p class="empty-hint">请尝试使用其他关键词搜索</p>
      </div>
    </section>

    <!-- ===== 执行日志面板（可展开） ===== -->
    <section class="log-panel">
      <div class="log-head" @click="logExpanded = !logExpanded">
        <div class="log-head-left">
          <span class="log-pulse-dot"></span>
          <span class="log-title">🔧 工具调用记录</span>
          <span v-if="executionHistory.length > 0" class="log-badge">{{ executionHistory.length }}</span>
        </div>
        <div class="log-head-right">
          <button
            class="log-btn link"
            title="查看完整执行历史（Agent + 工具）"
            @click.stop="goExecutionHistory"
          >
            📋 执行历史
          </button>
          <button
            class="log-btn"
            :disabled="executionHistory.length === 0"
            title="导出日志"
            @click.stop="handleExportLogs"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
          </button>
          <button
            v-if="authStore.isAdmin"
            class="log-btn danger"
            :disabled="executionHistory.length === 0"
            title="清空日志"
            @click.stop="handleClearLogs"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
          <span class="log-arrow" :class="{ open: logExpanded }">▾</span>
        </div>
      </div>

      <div v-show="logExpanded" ref="logBodyRef" class="log-body">
        <TransitionGroup name="log-trans" tag="div" class="log-list">
          <div
            v-for="log in executionHistory"
            :key="log.id"
            class="log-row log-expandable"
            :class="[log.status, { 'is-expanded': expandedLogs[log.id] }]"
            @click="toggleLogExpand(log.id)"
          >
            <div class="log-row-main">
              <span class="log-expand-arrow">{{ expandedLogs[log.id] ? '▼' : '▶' }}</span>
              <span class="log-time">{{ formatTime(log.createdAt) }}</span>
              <span class="log-tool">{{ log.toolName }}</span>
              <span v-if="log.params && Object.keys(log.params).length > 0" class="log-params" :title="formatParamsJson(log.params)">{{ formatParamText(log.params) }}</span>
              <span class="log-status-cell">
                <span class="log-status-dot" :class="log.status"></span>
                <span>{{ log.status === 'success' ? '✅ 完成' : log.status === 'error' ? '❌ 失败' : '⏳ 执行中' }}</span>
              </span>
              <span v-if="log.executionTime" class="log-dur">{{ formatDuration(log.executionTime) }}</span>
              <span class="log-del-btn" title="永久删除该记录" @click.stop="handleDeleteLog(log)">🗑️</span>
            </div>

            <!-- 展开详情 -->
            <div v-if="expandedLogs[log.id]" class="log-detail-panel" @click.stop>
              <div class="log-detail-section">
                <div class="log-detail-label">📌 执行参数</div>
                <div class="log-detail-params-grid">
                  <div v-if="!log.params || Object.keys(log.params).length === 0" class="log-detail-params-empty">无参数</div>
                  <div v-for="(val, key) in log.params" :key="key" class="log-detail-param-item">
                    <span class="param-key">{{ key }}</span>
                    <span class="param-colon">:</span>
                    <span class="param-val" :class="{ 'param-val-str': typeof val === 'string', 'param-val-num': typeof val === 'number' }">{{ val }}</span>
                  </div>
                </div>
              </div>
              <div class="log-detail-section">
                <div class="log-detail-label">📊 执行结果</div>
                <div class="log-detail-result-text">
                  <span v-if="log.status === 'error'" class="result-error-hint">❌ {{ getResultSummary(log.result, log.toolId) }}</span>
                  <template v-else>
                    <div class="result-summary-icon">{{ getResultTypeIcon(log.toolId) }}</div>
                    <div class="result-summary-text">{{ getResultSummary(log.result, log.toolId) }}</div>
                  </template>
                </div>
              </div>
              <div class="log-detail-info">
                <span class="log-detail-info-item">
                  <span class="info-label">⏱️</span>
                  <span class="info-value">{{ formatDuration(log.executionTime) }}</span>
                </span>
                <span class="log-detail-info-item">
                  <span class="info-label">📊</span>
                  <span class="info-value" :class="log.status">
                    {{ log.status === 'success' ? '✅ 成功' : log.status === 'error' ? '❌ 失败' : '⏳ 执行中' }}
                  </span>
                </span>
                <span v-if="log.result && log.result.type" class="log-detail-info-item">
                  <span class="info-label">🏷️</span>
                  <span class="info-value type-badge">{{ log.result.type }}</span>
                </span>
              </div>
              <div class="log-detail-actions">
                <button v-if="log.result && log.status === 'success'" class="log-detail-btn log-detail-btn-primary" @click.stop="viewLogResult(log)">
                  📄 查看完整结果
                </button>
                <button class="log-detail-btn" @click.stop="reExecuteFromLog(log)">
                  🔄 重新执行
                </button>
                <button class="log-detail-btn" @click.stop="copyLogResult(log)">
                  📋 复制结果
                </button>
                <button v-if="log.result" class="log-detail-btn" @click.stop="toggleRawJson(log)">
                  {{ showLogRawJson[log.id] ? '🎨 结构化' : '📄 原始 JSON' }}
                </button>
              </div>
              <!-- 原始JSON（可折叠） -->
              <div v-if="showLogRawJson[log.id] && log.result" class="log-detail-section" style="margin-top:8px">
                <div class="log-detail-label">📄 原始 JSON</div>
                <pre class="log-detail-json">{{ formatResult(log.result) }}</pre>
              </div>
            </div>
          </div>
        </TransitionGroup>
        <div v-if="logsLoading" class="log-empty">
          <span class="log-empty-ico">⏳</span> 正在加载执行记录...
        </div>
        <div v-else-if="logsError" class="log-empty">
          <span class="log-empty-ico">⚠️</span> 加载执行记录失败，请检查网络后重试
          <button class="log-retry-btn" @click="retryLoadLogs">🔄 重试</button>
        </div>
        <div v-else-if="executionHistory.length === 0" class="log-empty">
          <span class="log-empty-ico">📭</span>
          <div class="log-empty-main">暂无执行记录</div>
          <div class="log-empty-sub">试试运行一个工具，调用记录将在这里显示</div>
        </div>
      </div>
    </section>

    <!-- ===== 底部 MCP 状态栏 ===== -->
    <footer class="mcp-bar">
      <div class="mcp-inner">
        <span class="mcp-live-dot"></span>
        <span class="mcp-label">🔌 MCP 服务状态</span>
        <span class="mcp-status-tag online">🟢 已连接</span>
        <span class="mcp-meta">3 个服务 · 平均延迟 124ms</span>
      </div>
    </footer>

    <!-- ===== 执行参数配置弹窗 ===== -->
    <div v-if="showExecDialog" class="exec-dialog-overlay" @click="closeExecDialog">
      <div class="exec-dialog" @click.stop>
        <div class="exec-dialog-header">
          <div class="exec-dialog-icon">{{ executingTool?.icon }}</div>
          <div class="exec-dialog-info">
            <h3>{{ executingTool?.name }}</h3>
            <p>{{ executingTool?.description }}</p>
          </div>
          <button class="exec-dialog-close" :disabled="isExecuting" @click="closeExecDialog">✕</button>
        </div>
        <div class="exec-dialog-body">
          <div v-if="executingTool?.paramsSchema && executingTool.paramsSchema.length > 0" class="exec-params">
            <div class="exec-params-title">执行参数配置</div>
            <div v-for="(param, idx) in executingTool.paramsSchema" :key="idx" class="exec-param-row">
              <label class="exec-param-label">
                {{ param.label || param.name }}
                <span v-if="param.required" class="exec-param-required">*</span>
                <span class="exec-param-type">{{ getTypeLabel(param.type) }}</span>
              </label>
              <input
                v-if="param.type === 'string'"
                v-model="toolParams[param.name]"
                type="text"
                :placeholder="param.placeholder || '输入' + param.name"
                class="exec-param-input"
                :disabled="isExecuting"
              />
              <el-select
                v-else-if="param.type === 'select' && param.options"
                v-model="toolParams[param.name]"
                class="exec-param-select"
                :disabled="isExecuting"
                popper-class="exec-param-popper"
              >
                <el-option
                  v-for="opt in param.options"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <div v-else-if="param.type === 'boolean'" class="exec-param-boolean">
                <el-switch
                  v-model="toolParams[param.name]"
                  :disabled="isExecuting"
                  active-value="true"
                  inactive-value="false"
                  @change="val => toolParams[param.name] = val ? 'true' : 'false'"
                />
                <span class="exec-param-boolean-label">{{ toolParams[param.name] === 'true' ? '是' : '否' }}</span>
              </div>
              <textarea
                v-else-if="param.type === 'textarea'"
                v-model="toolParams[param.name]"
                :placeholder="param.placeholder || '输入' + param.name"
                class="exec-param-input exec-param-textarea"
                :disabled="isExecuting"
                rows="3"
              ></textarea>
              <input
                v-else-if="param.type === 'number'"
                v-model.number="toolParams[param.name]"
                type="number"
                :placeholder="param.placeholder || '输入数字'"
                class="exec-param-input"
                :disabled="isExecuting"
              />
            </div>
          </div>
          <div v-else class="exec-no-params">
            <span class="exec-no-params-icon">⚡</span>
            <span>此工具无需参数，点击执行即可运行</span>
          </div>
        </div>
        <div class="exec-dialog-footer">
          <button class="exec-btn exec-btn-cancel" :disabled="isExecuting" @click="closeExecDialog">取消</button>
          <button class="exec-btn exec-btn-exec" :disabled="isExecuting" @click="confirmExecute">
            <span v-if="isExecuting" class="exec-spinner"></span>
            <span v-else>▶</span>
            <span>{{ isExecuting ? '执行中...' : '执行' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 执行结果弹窗（结构化渲染） ===== -->
    <div v-if="showResultDialog" class="exec-dialog-overlay" @click="closeResultDialog">
      <div class="exec-dialog exec-result-dialog" @click.stop>
        <div class="exec-dialog-header">
          <div class="exec-dialog-icon">{{ executingTool?.icon }}</div>
          <div class="exec-dialog-info">
            <h3>{{ getResultTitle(executingTool?.id) }}</h3>
            <p>{{ executingTool?.name }}</p>
          </div>
          <button class="exec-dialog-close" @click="closeResultDialog">✕</button>
        </div>
        <div class="exec-dialog-body">
          <!-- 状态指示 -->
          <div class="exec-result-status" :class="{ error: execError }">
            <span class="exec-result-dot" :class="execError ? 'error' : 'success'"></span>
            <span class="exec-result-text" :class="{ error: execError }">{{ execError ? '❌ 执行失败' : '✅ 执行成功' }}</span>
            <span v-if="!execError && lastExecDuration" class="exec-result-duration">
              耗时 {{ formatDuration(lastExecDuration) }}
            </span>
          </div>

          <!-- 错误信息 -->
          <div v-if="execError" class="exec-result-content error">
            <div class="exec-result-title">错误信息</div>
            <pre class="exec-result-json">{{ execError }}</pre>
          </div>

          <!-- 结构化结果渲染 -->
          <div v-if="execResult && !execError" class="exec-result-render">
            <ResourceResult
              v-if="getResultComponent(executingTool?.id) === 'ResourceResult'"
              :data="execResult"
            />
            <QuizResult
              v-else-if="getResultComponent(executingTool?.id) === 'QuizResult'"
              :data="execResult"
            />
            <SummaryResult
              v-else-if="getResultComponent(executingTool?.id) === 'SummaryResult'"
              :data="execResult"
            />
            <KnowledgeGraphResult
              v-else-if="getResultComponent(executingTool?.id) === 'KnowledgeGraphResult'"
              :data="execResult"
            />
            <LearningPipelineResult
              v-else-if="getResultComponent(executingTool?.id) === 'LearningPipelineResult'"
              :data="execResult"
            />
            <JsonResult
              v-else
              :data="execResult"
            />
          </div>

          <!-- 一键串联快捷按钮：深度文献解析后可继续生成测验题 -->
          <div v-if="showQuizShortcut" class="quiz-shortcut-banner">
            <span class="shortcut-text">📎 基于此内容继续生成测验题？</span>
            <button class="shortcut-btn" @click="handleQuizShortcut">
              🧠 生成测验题
            </button>
          </div>

          <!-- 原始 JSON（可切换） -->
          <div v-if="execResult && showRawJson" class="exec-result-content">
            <div class="exec-result-title">原始 JSON</div>
            <pre class="exec-result-json">{{ formatResult(execResult) }}</pre>
          </div>
        </div>
        <div class="exec-dialog-footer">
          <div class="exec-dialog-footer-left">
            <button class="exec-btn exec-btn-ghost" :disabled="!execResult" @click="copyResult">📋 复制</button>
            <button class="exec-btn exec-btn-ghost" :disabled="!execResult" @click="exportResult">⬇️ 导出</button>
            <button class="exec-btn exec-btn-ghost" @click="showRawJson = !showRawJson">
              {{ showRawJson ? '🎨 结构化' : '📄 原始 JSON' }}
            </button>
          </div>
          <div class="exec-dialog-footer-right">
            <button class="exec-btn exec-btn-cancel" @click="closeResultDialog">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, onActivated, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToolsStore } from '@/stores/toolsStore'
import { useChatStore } from '@/stores/chatStore'
import { ElMessage } from 'element-plus'
import { ElSelect, ElOption } from 'element-plus'
import { deleteToolExecution, clearToolExecutions } from '@/api/toolsApi'
import { confirmAction, cleanupDialogs } from '@/utils/modalHelper'

// 结构化结果渲染组件
import ResourceResult from '@/components/tools/ResourceResult.vue'
import QuizResult from '@/components/tools/QuizResult.vue'
import SummaryResult from '@/components/tools/SummaryResult.vue'
import KnowledgeGraphResult from '@/components/tools/KnowledgeGraphResult.vue'
import LearningPipelineResult from '@/components/tools/LearningPipelineResult.vue'
import JsonResult from '@/components/tools/JsonResult.vue'

const router = useRouter()
const authStore = useAuthStore()
const toolsStore = useToolsStore()
const chatStore = useChatStore()
const pageRef = ref(null)
const logBodyRef = ref(null)

// 执行历史（从后端加载）
const executionHistory = ref([])
// 加载状态（区分“无数据”与“加载失败”）
const logsLoading = ref(false)
const logsError = ref(false)

// ===== 分类配置（学者视角精简版：4个分类，系统调试默认折叠） =====
const categoryConfig = [
  { id: 'all', name: '全部', icon: '📦', color: '#a78bfa' },
  { id: 'input_search', name: '输入与检索', icon: '📚', color: '#00f5d4' },
  { id: 'understanding_output', name: '理解与输出', icon: '✍️', color: '#ffb86c' },
  { id: 'assessment_loop', name: '评估与闭环', icon: '🧠', color: '#ff6b9d' },
  { id: 'system_debug', name: '系统调试', icon: '⚙️', color: '#a78bfa', defaultCollapsed: true }
]

// ===== Mock 工具数据（学者视角精简版：5个核心工具，与后端 visibleTools() 对齐） =====
const mockTools = [
  {
    id: 'unified_academic_search', name: '全域学术检索',
    description: '跨源统一检索：同时搜索内部知识库、外部联网资源、知识图谱概念，自动过滤无关网页',
    icon: '📚', category: 'input_search', status: 'available', aiEnabled: false,
    paramsSchema: [
      { name: 'query', label: '搜索关键词', type: 'string', required: true, placeholder: '输入搜索关键词' },
      { name: 'searchInternal', label: '搜索内部知识库', type: 'boolean', required: false, default: 'true' },
      { name: 'searchWeb', label: '联网搜索', type: 'boolean', required: false, default: 'true' },
      { name: 'searchGraph', label: '查询知识图谱', type: 'boolean', required: false, default: 'false' },
      { name: 'limit', label: '每类结果上限', type: 'number', required: false, default: 5 }
    ]
  },
  {
    id: 'deep_document_analysis', name: '深度文献解析',
    description: '一键生成：精华摘要 + 结构化知识卡片（核心术语、定义、关联），替代原来的文档摘要+知识点提取',
    icon: '✍️', category: 'understanding_output', status: 'available', aiEnabled: true,
    paramsSchema: [
      { name: 'documentId', label: '文档ID', type: 'string', required: false, placeholder: '知识库文档ID（与内容二选一）' },
      { name: 'content', label: '文档内容', type: 'textarea', required: false, placeholder: '输入或粘贴文档内容' },
      { name: 'summaryLength', label: '摘要长度', type: 'select', required: false, default: 'medium', options: [{ label: '简短', value: 'short' }, { label: '中等', value: 'medium' }, { label: '详细', value: 'long' }] },
      { name: 'keywordCount', label: '关键词数量', type: 'number', required: false, default: 8 },
      { name: 'includeGlossary', label: '生成术语对照表', type: 'boolean', required: false, default: 'true' }
    ]
  },
  {
    id: 'smart_quiz_generation', name: '智能测评出题',
    description: '基于学习内容，自动生成带考点解析的选择/填空/判断题，支持难度调节',
    icon: '🧠', category: 'assessment_loop', status: 'available', aiEnabled: true,
    paramsSchema: [
      { name: 'topic', label: '学习内容', type: 'textarea', required: true, placeholder: '输入学习内容或主题' },
      { name: 'questionType', label: '题型', type: 'select', required: false, default: 'mixed', options: [{ label: '选择题', value: 'choice' }, { label: '判断题', value: 'judgment' }, { label: '填空题', value: 'fill' }, { label: '混合', value: 'mixed' }] },
      { name: 'count', label: '题目数量', type: 'number', required: false, default: 5 },
      { name: 'difficulty', label: '难度', type: 'select', required: false, default: '中等', options: [{ label: '简单', value: '简单' }, { label: '中等', value: '中等' }, { label: '困难', value: '困难' }] }
    ]
  },
  {
    id: 'academic_translation', name: '学术翻译',
    description: '精准翻译学术资料，保留技术术语并生成中英术语对照表',
    icon: '🌍', category: 'understanding_output', status: 'available', aiEnabled: true,
    paramsSchema: [
      { name: 'text', label: '待翻译文本', type: 'textarea', required: true, placeholder: '输入待翻译文本' },
      { name: 'sourceLang', label: '源语言', type: 'string', required: false, default: 'auto' },
      { name: 'targetLang', label: '目标语言', type: 'select', required: false, default: '中文', options: [{ label: '中文', value: '中文' }, { label: '英文', value: '英文' }, { label: '日文', value: '日文' }] },
      { name: 'preserveTechTerms', label: '保留技术术语', type: 'boolean', required: false, default: 'true' }
    ]
  },
  {
    id: 'full_chain_learning', name: '全链路学习助手',
    description: '【场景入口】一键完成"文献解析→知识点提取→出题"全流程，生成完整学习报告',
    icon: '🎓', category: 'assessment_loop', status: 'available', aiEnabled: true,
    paramsSchema: [
      { name: 'document', label: '学习材料内容', type: 'textarea', required: false, placeholder: '学习材料内容（与文档ID二选一）' },
      { name: 'documentId', label: '文档ID', type: 'string', required: false, placeholder: '知识库文档ID' },
      { name: 'questionType', label: '题型', type: 'select', required: false, default: 'mixed', options: [{ label: '选择题', value: 'choice' }, { label: '判断题', value: 'judgment' }, { label: '填空题', value: 'fill' }, { label: '混合', value: 'mixed' }] },
      { name: 'quizCount', label: '题目数量', type: 'number', required: false, default: 5 }
    ]
  }
]

// ===== 工具执行日志持久化（从后端加载） =====
async function loadLogs() {
  logsLoading.value = true
  logsError.value = false
  try {
    await toolsStore.fetchExecutionHistory(0, 50)
    executionHistory.value = toolsStore.executionHistory
    if (executionHistory.value.length === 0 && toolsStore.historyTotal > 0) {
      console.warn('工具执行记录加载异常：计数 ' + toolsStore.historyTotal + ' 但列表为空')
    }
  } catch (e) {
    console.error('加载日志失败:', e)
    logsError.value = true
  } finally {
    logsLoading.value = false
  }
}

// 加载失败重试
const retryLoadLogs = () => { loadLogs() }

// ===== 执行弹窗状态 =====
const showExecDialog = ref(false)
const showResultDialog = ref(false)
const executingTool = ref(null)
const toolParams = ref({})
const execResult = ref(null)
const isExecuting = ref(false)
const execError = ref(null)
const showRawJson = ref(false)
const lastExecDuration = ref(null)

// ===== 一键串联快捷按钮状态 =====
const showQuizShortcut = ref(false)
const quizShortcutContext = ref(null)

// ===== 日志展开状态追踪 =====
const expandedLogs = ref({})

// ===== 原始 JSON 折叠状态（按 log.id 索引） =====
const showLogRawJson = ref({})

// ===== 浮动粒子 =====
const particles = ref([])
const generateParticles = () => {
  const arr = []
  for (let i = 0; i < 35; i++) {
    arr.push({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 3 + 1,
      duration: Math.random() * 25 + 15,
      delay: Math.random() * 12,
      opacity: Math.random() * 0.35 + 0.08
    })
  }
  particles.value = arr
}

// ===== 响应式状态 =====
const searchQuery = ref('')
const activeCategory = ref('all')
const logExpanded = ref(true)
const barAnimated = reactive({})

// ===== 从 store 获取数据，API 无数据时使用 Mock =====
const tools = computed(() => toolsStore.tools.length > 0 ? toolsStore.tools : mockTools)
const toolSummary = computed(() => toolsStore.toolSummary)

const configuringCount = computed(() =>
  tools.value.filter(t => t.status === 'configuring' || t.status === 'upgrading').length
)

const totalUsage = computed(() =>
  Object.values(toolsStore.toolStats).reduce((sum, s) => sum + (s.totalCalls || 0), 0)
)

// ===== 分类标签计数 =====
const categoriesWithCount = computed(() =>
  categoryConfig.map(cat => ({
    ...cat,
    count: cat.id === 'all'
      ? tools.value.length
      : tools.value.filter(t => t.category === cat.id).length
  }))
)

// ===== 工具过滤与显示 =====
const displayTools = computed(() => {
  // 排除隐藏工具（后端已过滤，此处双保险：兼容后端返回含 isHidden 的情况）
  let result = tools.value.filter(t => !t.isHidden)
  if (activeCategory.value !== 'all') {
    result = result.filter(t => t.category === activeCategory.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    result = result.filter(t =>
      t.name.toLowerCase().includes(q) ||
      t.description.toLowerCase().includes(q) ||
      t.category.toLowerCase().includes(q)
    )
  }
  return result
})

// ===== 工具属性计算 =====
const getToolColor = (tool) => {
  const cat = categoryConfig.find(c => c.id === tool.category)
  return cat ? cat.color : '#a78bfa'
}

const getUsage = (tool) => toolsStore.toolStats[tool.id]?.totalCalls || 0

const getUsagePercent = (tool) => {
  const values = tools.value.map(t => getUsage(t))
  const max = Math.max(...values, 1)
  return Math.round((getUsage(tool) / max) * 100)
}

const getStatusLabel = (status) => {
  const map = { available: '可用', executing: '执行中', configuring: '配置中', unavailable: '不可用', upgrading: '升级中' }
  return map[status] || '未知'
}

const getActionLabel = (status) => {
  const map = { available: '执行', executing: '执行中', configuring: '配置', unavailable: '不可用', upgrading: '升级' }
  return map[status] || '执行'
}

const getTypeLabel = (type) => {
  const map = {
    string: '文本',
    number: '数字',
    boolean: '开关',
    select: '选项',
    textarea: '多行文本'
  }
  return map[type] || type
}

// ===== 操作处理 =====
const setCategory = (catId) => {
  activeCategory.value = catId
}

// ===== 打开执行参数弹窗 =====
const handleExecute = (tool) => {
  if (tool.status !== 'available') {
    ElMessage.warning(`${tool.name} 当前不可用`)
    return
  }
  // 网页抓取功能开发中，跳转到占位页面
  if (tool.id === 'web_fetch') {
    router.push('/web-fetch')
    return
  }
  executingTool.value = tool
  // 初始化参数默认值
  const params = {}
  if (tool.paramsSchema) {
    tool.paramsSchema.forEach(p => {
      if (p.default !== undefined) {
        params[p.name] = p.default
      } else {
        params[p.name] = ''
      }
    })
  }
  toolParams.value = params
  execResult.value = null
  execError.value = null
  showRawJson.value = false
  showExecDialog.value = true
}

// ===== 关闭参数弹窗 =====
const closeExecDialog = () => {
  if (isExecuting.value) return
  showExecDialog.value = false
  executingTool.value = null
  toolParams.value = {}
}

// ===== 确认执行工具 =====
const confirmExecute = async () => {
  const tool = executingTool.value
  if (!tool) return

  // 验证必填参数
  if (tool.paramsSchema) {
    for (const p of tool.paramsSchema) {
      if (p.required && !toolParams.value[p.name] && toolParams.value[p.name] !== 0) {
        ElMessage.warning(`请填写「${p.name}」`)
        return
      }
    }
  }

  isExecuting.value = true
  execResult.value = null
  execError.value = null

  const params = { ...toolParams.value }
  // 清除空字符串参数
  Object.keys(params).forEach(k => { if (params[k] === '') delete params[k] })

  try {
    const result = await toolsStore.executeTool(tool.id, params)

    // 立即显示结果弹窗
    execResult.value = result
    showExecDialog.value = false
    showResultDialog.value = true
    showRawJson.value = false
    // 记录执行耗时
    lastExecDuration.value = result._duration || result.executionTime || null
    ElMessage.success(`${tool.name} 执行成功`)

    // 一键串联：深度文献解析成功后显示"继续生成测验题"快捷按钮
    if (tool.id === 'deep_document_analysis') {
      showQuizShortcut.value = true
      quizShortcutContext.value = result
    } else {
      showQuizShortcut.value = false
      quizShortcutContext.value = null
    }

    // 异步加载历史记录（不阻塞结果展示）
    loadLogs()
  } catch (error) {
    execError.value = error.message || '执行失败'
    showExecDialog.value = false
    showResultDialog.value = true
    showQuizShortcut.value = false
    quizShortcutContext.value = null
    ElMessage.error(`${tool.name} 执行失败`)
  } finally {
    isExecuting.value = false
  }
  scrollToLogTop()
}

// ===== 关闭结果弹窗 =====
const closeResultDialog = () => {
  showResultDialog.value = false
  execResult.value = null
  executingTool.value = null
  showQuizShortcut.value = false
  quizShortcutContext.value = null
}

// ===== 一键串联：基于文献解析结果生成测验题 =====
const handleQuizShortcut = () => {
  // 关闭当前结果弹窗
  showResultDialog.value = false
  showQuizShortcut.value = false
  
  // 找到测验出题工具
  const quizTool = tools.value.find(t => t.id === 'smart_quiz_generation')
  if (!quizTool) {
    ElMessage.warning('未找到测验出题工具')
    return
  }
  
  // 构建上下文：将文献解析的摘要作为测验题的主题
  let topicText = ''
  if (quizShortcutContext.value) {
    const result = quizShortcutContext.value
    if (result.summary) {
      topicText += '文档摘要：' + (typeof result.summary === 'string' ? result.summary : JSON.stringify(result.summary)) + '\n\n'
    }
    if (result.keywords && result.keywords.length > 0) {
      topicText += '关键词：' + result.keywords.join(', ') + '\n\n'
    }
    if (result.knowledgePoints) {
      topicText += '知识点：' + (typeof result.knowledgePoints === 'string' ? result.knowledgePoints : JSON.stringify(result.knowledgePoints))
    }
  }
  
  // 打开测验出题对话框，预填参数
  executingTool.value = quizTool
  toolParams.value = {
    topic: topicText || '根据文档内容生成测验题',
    questionType: 'mixed',
    count: 5,
    difficulty: '中等'
  }
  execResult.value = null
  execError.value = null
  showRawJson.value = false
  showExecDialog.value = true
  
  ElMessage.info('已基于文献解析结果生成测验题上下文')
}

// ===== 格式化执行结果 =====
// ===== 结构化结果渲染 =====

// 根据 toolId 获取结果组件
const getResultComponent = (toolId) => {
  const map = {
    'unified_academic_search': 'ResourceResult',
    'deep_document_analysis': 'SummaryResult',
    'smart_quiz_generation': 'QuizResult',
    'academic_translation': 'SummaryResult',
    'full_chain_learning': 'LearningPipelineResult',
    'search_resources': 'ResourceResult',
    'query_knowledge_graph': 'KnowledgeGraphResult',
    'summarize_document': 'SummaryResult',
    'generate_quiz': 'QuizResult',
    'extract_keywords': 'SummaryResult',
    'translate_text': 'SummaryResult',
    'learning_assistant': 'LearningPipelineResult'
  }
  return map[toolId] || 'JsonResult'
}

// 获取结果展示标题
const getResultTitle = (toolId) => {
  const tool = tools.value.find(t => t.id === toolId)
  return tool ? tool.name + ' 执行结果' : '执行结果'
}

// 根据 toolId 获取结果摘要文字（结构化展示）
const getResultSummary = (result, toolId) => {
  if (!result) return ''
  if (typeof result === 'string') return result.length > 120 ? result.substring(0, 120) + '...' : result

  try {
    switch (toolId) {
      case 'search_resources': {
        const resources = result.resources || result.items || result.data || []
        const total = result.total || resources.length
        const keyword = result.keyword || result.query || ''
        return `检索到 ${total} 条资源${keyword ? '（关键词：' + keyword + '）' : ''}`
      }
      case 'generate_quiz': {
        const questions = result.questions || result.items || result.data || []
        const count = result.count || questions.length
        const topic = result.topic || result.subject || ''
        const difficulty = result.difficulty || ''
        return `已生成 ${count} 道${difficulty ? '【' + difficulty + '】' : ''}测验题${topic ? '（主题：' + topic + '）' : ''}`
      }
      case 'summarize_document': {
        const summary = result.summary || result.content || ''
        const keywords = result.keywords || []
        const kwStr = Array.isArray(keywords) && keywords.length > 0 ? '关键词：' + keywords.slice(0, 5).join(', ') : ''
        const txt = typeof summary === 'string' ? summary : JSON.stringify(summary)
        return (txt.length > 80 ? txt.substring(0, 80) + '...' : txt) + (kwStr ? ' | ' + kwStr : '')
      }
      case 'extract_keywords': {
        const keywords = result.knowledgePoints || result.keywords || result.items || result.data || []
        const count = keywords.length
        const kwList = Array.isArray(keywords) ? keywords.slice(0, 8).map(k => typeof k === 'string' ? k : (k.keyword || k.name || k.word || k)).join(', ') : ''
        const linked = result.graphLinkedCount ? `，关联知识图谱 ${result.graphLinkedCount} 个节点` : ''
        return `提取到 ${count} 个知识点${linked}：${kwList}`
      }
      case 'query_knowledge_graph': {
        const nodes = result.nodes || result.items || result.data || []
        const nodeCount = nodes.length
        const relations = result.relations || result.edges || []
        const relCount = relations.length
        const name = result.name || result.nodeName || ''
        return `查询到 ${nodeCount} 个节点、${relCount} 条关系${name ? '（焦点：' + name + '）' : ''}`
      }
      case 'web_search': {
        const items = result.items || result.results || result.data || []
        const total = result.total || items.length
        const query = result.query || result.keyword || ''
        return `搜索到 ${total} 条结果${query ? '（查询：' + query + '）' : ''}`
      }
      case 'translate_text': {
        const translated = result.translated || result.translation || result.text || result.content || ''
        const sourceLang = result.sourceLang || result.source || ''
        const targetLang = result.targetLang || result.target || ''
        const terms = Array.isArray(result.termMappings) ? result.termMappings.length : 0
        const txt = typeof translated === 'string' ? translated : ''
        return `翻译完成${sourceLang && sourceLang !== 'auto' ? '（' + sourceLang : ''}→${targetLang || '中文'}）：${txt ? (txt.length > 60 ? txt.substring(0, 60) + '...' : txt) : JSON.stringify(translated)}${terms ? ' | 术语对照 ' + terms + ' 条' : ''}`
      }
      case 'learning_assistant': {
        const pipeline = Array.isArray(result.pipeline) ? result.pipeline.join(' → ') : '摘要 → 知识点 → 测验'
        const quiz = result.steps?.quiz
        const quizCount = quiz?.count || 0
        return `学习报告完成（${pipeline}）${quizCount ? '，含 ' + quizCount + ' 道测验题' : ''}`
      }
      case 'web_fetch': {
        const title = result.title || ''
        const content = result.content || result.text || ''
        const txt = typeof content === 'string' ? content.substring(0, 60) : ''
        return `已抓取${title ? '「' + title + '」' : '网页'}：${txt}${content.length > 60 ? '...' : ''}`
      }
      default: {
        // 通用：尝试从常用字段提取摘要
        const summary = result.summary || result.description || result.content || result.message || result.result || ''
        if (typeof summary === 'string' && summary.length > 0) {
          return summary.length > 120 ? summary.substring(0, 120) + '...' : summary
        }
        // 计算数组长度
        const arr = result.items || result.data || result.results || result.list || []
        if (Array.isArray(arr)) {
          return `共 ${arr.length} 条数据`
        }
        const str = JSON.stringify(result)
        return str.length > 80 ? str.substring(0, 80) + '...' : str
      }
    }
  } catch {
    const str = JSON.stringify(result)
    return str.length > 80 ? str.substring(0, 80) + '...' : str
  }
}

const formatResult = (result) => {
  if (!result) return ''
  if (typeof result === 'string') return result
  try {
    return JSON.stringify(result, null, 2)
  } catch {
    return String(result)
  }
}

// ===== 操作功能 =====

// 复制执行结果
const copyResult = () => {
  if (!execResult.value) return
  const text = typeof execResult.value === 'string'
    ? execResult.value
    : JSON.stringify(execResult.value, null, 2)
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('结果已复制到剪贴板')
  }).catch(() => {
    ElMessage.warning('复制失败，请手动选择复制')
  })
}

// 重新执行（复用当前参数）
const reExecute = (log) => {
  const tool = tools.value.find(t => t.id === log.toolId || t.id === executingTool.value?.id)
  if (!tool) {
    ElMessage.warning('未找到对应的工具')
    return
  }
  // 使用日志中的参数或当前参数重新打开执行弹窗
  executingTool.value = tool
  toolParams.value = log.params || {}
  execResult.value = null
  execError.value = null
  showResultDialog.value = false
  showExecDialog.value = true
  ElMessage.info('已恢复参数，请确认后执行')
}

// 导出执行结果
const exportResult = () => {
  if (!execResult.value) return
  const text = typeof execResult.value === 'string'
    ? execResult.value
    : JSON.stringify(execResult.value, null, 2)
  const toolName = executingTool.value?.name || '工具'
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${toolName}-result-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('结果已导出')
}

// 日志相关操作
const toggleLogExpand = (logId) => {
  expandedLogs.value[logId] = !expandedLogs.value[logId]
}

const reExecuteFromLog = (log) => {
  reExecute(log)
}

const copyLogResult = (log) => {
  if (!log.result) return
  const text = typeof log.result === 'string'
    ? log.result
    : JSON.stringify(log.result, null, 2)
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('日志结果已复制')
  }).catch(() => {
    ElMessage.warning('复制失败')
  })
}

// 从日志打开结果详情弹窗
const viewLogResult = (log) => {
  if (!log) return
  // 根据 log.toolId 查找对应的工具配置
  const tool = tools.value.find(t => t.id === log.toolId)
  if (tool) {
    executingTool.value = tool
  } else {
    // 如果没有找到工具配置，创建一个临时对象
    executingTool.value = {
      id: log.toolId,
      name: log.toolName || log.toolId,
      icon: getResultTypeIcon(log.toolId) || '📄',
      description: ''
    }
  }
  execResult.value = log.result || null
  execError.value = log.status === 'error' ? (typeof log.result === 'string' ? log.result : JSON.stringify(log.result)) : null
  lastExecDuration.value = log.executionTime || null
  showRawJson.value = false
  showResultDialog.value = true
}

// 切换日志中的原始 JSON 显示
const toggleRawJson = (log) => {
  showLogRawJson.value[log.id] = !showLogRawJson.value[log.id]
}

// 获取工具类型对应的图标
const getResultTypeIcon = (toolId) => {
  const map = {
    'search_resources': '📚',
    'query_knowledge_graph': '🔍',
    'web_search': '🌐',
    'web_fetch': '📄',
    'summarize_document': '📝',
    'extract_keywords': '🏷️',
    'generate_quiz': '✏️',
    'translate_text': '🌍'
  }
  return map[toolId] || '📄'
}

// 跳转到完整执行历史（人机协作指挥中心），携带定位参数：展开并滚动到执行历史面板
const goExecutionHistory = () => {
  router.push({ path: '/agents', query: { flow: 'history' } })
}

// 删除单条工具执行记录（永久删除，二次确认）
const handleDeleteLog = async (log) => {
  if (!log || !log.id) return
  try {
    await confirmAction(`确定要永久删除「${log.toolName || log.toolId}」这条执行记录吗？删除后不可恢复。`, '删除执行记录', {
      type: 'warning', confirmButtonText: '永久删除', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    await deleteToolExecution(log.id)
    executionHistory.value = executionHistory.value.filter(l => l.id !== log.id)
    delete expandedLogs.value[log.id]
    delete showLogRawJson.value[log.id]
    ElMessage.success('执行记录已删除')
  } catch (e) {
    ElMessage.error('删除失败：' + (e.message || '请稍后重试'))
  }
}

const handleClearLogs = async () => {
  if (executionHistory.value.length === 0) return
  try {
    await confirmAction(`确定要清空全部 ${executionHistory.value.length} 条工具执行记录吗？删除后不可恢复。`, '清空执行记录', {
      type: 'warning', confirmButtonText: '永久清空', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    // 后端物理删除，刷新页面后不会再次出现
    await clearToolExecutions()
    executionHistory.value = []
    expandedLogs.value = {}
    ElMessage.success('执行记录已清空')
  } catch (e) {
    ElMessage.error('清空失败：' + (e.message || '请稍后重试'))
  }
}

const handleExportLogs = () => {
  if (executionHistory.value.length === 0) return
  const lines = executionHistory.value.map(log => {
    const time = formatTime(log.createdAt)
    const st = log.status === 'success' ? '完成' : log.status === 'error' ? '失败' : '执行中'
    return `[${time}] ${log.toolName} ${st} ${log.executionTime ? formatDuration(log.executionTime) : ''}`
  })
  const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `tool-logs-${new Date().toISOString().slice(0, 10)}.txt`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('日志已导出')
}

// ===== 工具函数 =====
const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const formatDuration = (ms) => {
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

const formatParamText = (params) => {
  if (!params) return ''
  const str = typeof params === 'string' ? params : JSON.stringify(params)
  return str.length > 30 ? str.substring(0, 30) + '...' : str
}

const formatParamsJson = (params) => {
  if (!params) return '{}'
  if (typeof params === 'string') {
    try { return JSON.stringify(JSON.parse(params), null, 2) }
    catch { return params }
  }
  try { return JSON.stringify(params, null, 2) }
  catch { return String(params) }
}

const scrollToLogTop = () => {
  nextTick(() => {
    if (logBodyRef.value) {
      logBodyRef.value.scrollTop = 0
    }
  })
}

// ===== 生命周期 =====
onMounted(async () => {
  generateParticles()
  loadLogs()
  // 从 localStorage 恢复之前已经初始化过 usageData
  try {
    await toolsStore.fetchTools()
    await toolsStore.fetchToolStats()
    await toolsStore.fetchToolSummary()
    // 同步 MCP 状态到对话界面
    chatStore.fetchMcpStatus()
  } catch (error) {
    console.error('获取工具数据失败:', error)
  }
  // 触发进度条动画
  nextTick(() => {
    displayTools.value.forEach((tool, idx) => {
      setTimeout(() => {
        barAnimated[idx] = true
      }, 300 + idx * 80)
    })
  })
})

onUnmounted(() => {
  // 清理未关闭的确认弹窗，防止路由切换后残留堆叠
  cleanupDialogs()
})

// 页面被 keep-alive 缓存：从其他页面（如执行历史中删除了记录）切回时重新加载，保证列表与后端一致
onActivated(() => {
  loadLogs()
})
</script>

<style lang="scss" scoped>
/* ==============================================
   MCP 工具中心 — 极致动态科技感
   深空背景 · 旋转光效 · 毛玻璃 · 脉冲动画
   ============================================== */

/* ===== 页面容器 ===== */
.tools-page {
  position: relative;
  min-height: 100vh;
  padding: 32px 40px 130px;
  background: #0a0a1a;
  color: #f0f0ff;
  overflow-x: hidden;
  /* 确保任何白色残留被深色覆盖 */
  isolation: isolate;
}

/* ===== 深空动态背景 ===== */
.bg-deep {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(ellipse at 70% 20%, rgba(0, 245, 212, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 80%, rgba(123, 97, 255, 0.06) 0%, transparent 50%),
    #0a0a1a;
}
.aurora-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(130px);
  animation: floatOrb 22s ease-in-out infinite;
}
.orb-cyan {
  width: 650px; height: 650px;
  top: -220px; right: -120px;
  background: radial-gradient(circle, rgba(0, 245, 212, 0.12) 0%, transparent 70%);
}
.orb-purple {
  width: 550px; height: 550px;
  bottom: -180px; left: -100px;
  background: radial-gradient(circle, rgba(123, 97, 255, 0.1) 0%, transparent 70%);
  animation-delay: -8s;
}
.orb-pink {
  width: 420px; height: 420px;
  top: 40%; left: 42%;
  background: radial-gradient(circle, rgba(255, 0, 110, 0.06) 0%, transparent 70%);
  animation-delay: -15s;
}
@keyframes floatOrb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(35px, -35px) scale(1.1); }
  50% { transform: translate(-25px, 25px) scale(0.92); }
  75% { transform: translate(25px, 12px) scale(1.06); }
}

.grid-overlay {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 245, 212, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 245, 212, 0.025) 1px, transparent 1px);
  background-size: 64px 64px;
  pointer-events: none;
  z-index: 0;
}

/* ===== 浮动粒子 ===== */
.particles-wrap {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(0, 245, 212, 0.35);
  animation: particleRise linear infinite;
  will-change: transform;
}
@keyframes particleRise {
  0% { transform: translateY(0) translateX(0); }
  25% { transform: translateY(-30vh) translateX(18px); }
  50% { transform: translateY(-55vh) translateX(-12px); }
  75% { transform: translateY(-80vh) translateX(8px); }
  100% { transform: translateY(-100vh) translateX(0); }
}

/* ===== 顶部标题 ===== */
.page-header {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
  animation: slideUp 0.6s ease both;
}
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 22px;
}
.header-left { flex: 1; }

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 0 40px rgba(124, 107, 245, 0.1);
  line-height: 1.2;
}
.title-glyph {
  font-size: 1.9rem;
  filter: drop-shadow(0 0 12px rgba(124, 107, 245, 0.45));
}

.page-subtitle {
  margin: 0;
  font-size: 0.88rem;
  color: var(--text-placeholder);
  line-height: 1.6;
}
.stat-ok { color: #10b981; }
.stat-run { color: #f59e0b; }
.stat-cfg { color: #ff006e; }

/* KPI 卡片 */
.header-kpis {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}
.kpi {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 20px;
  background: rgba(100, 100, 180, 0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 14px;
  min-width: 76px;
}
.kpi-num {
  font-size: 1.4rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  color: #f0f0ff;
}
.kpi-num.kpi-ok { color: #10b981; text-shadow: 0 0 12px rgba(16, 185, 129, 0.3); }
.kpi-num.kpi-accent { color: #00f5d4; text-shadow: 0 0 12px rgba(0, 245, 212, 0.3); }
.kpi-label {
  font-size: 0.7rem;
  color: var(--text-sub);
  letter-spacing: 0.03em;
}

/* ===== 搜索栏 ===== */
.search-wrap { position: relative; z-index: 1; }
.search-box {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 500px;
}
.search-ico {
  position: absolute;
  left: 16px;
  width: 18px; height: 18px;
  color: var(--text-sub);
  pointer-events: none;
}
.search-field {
  width: 100%;
  padding: 11px 44px 11px 48px;
  background: var(--bg-ctrl);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-ctrl);
  border-radius: 14px;
  color: var(--text-input);
  caret-color: var(--title-status);
  font-size: 0.88rem;
  font-family: inherit;
  outline: none;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
  &::placeholder { color: var(--text-placeholder); }
  &:focus {
    border-color: var(--border-ctrl-hover);
    box-shadow: 0 0 0 3px rgba(124, 107, 245, 0.12), 0 0 30px rgba(124, 107, 245, 0.08);
  }
}
.search-x {
  position: absolute;
  right: 14px;
  cursor: pointer;
  color: var(--text-sub);
  font-size: 0.85rem;
  padding: 4px;
  transition: color 0.2s;
  &:hover { color: #f0f0ff; }
}

/* ===== 分类标签 ===== */
.cat-tabs {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 30px;
  animation: slideUp 0.6s ease 0.1s both;
}
.cat-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 22px;
  font-size: 0.82rem;
  font-family: inherit;
  color: #c0c8e0;
  background: var(--bg-ctrl);
  border: 1px solid var(--border-ctrl-soft);
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  &:hover {
    color: #00f5d4;
    border-color: var(--border-ctrl-hover);
    background: rgba(124, 107, 245, 0.08);
  }
  &.active {
    color: var(--tab-c, #00f5d4);
    border-color: var(--tab-c, #00f5d4);
    background: rgba(0, 245, 212, 0.06);
    font-weight: 600;
    box-shadow: 0 0 20px rgba(0, 245, 212, 0.08);
  }
  .tab-ico { font-size: 0.92rem; }
  .tab-num {
    font-size: 0.68rem;
    padding: 1px 7px;
    background: var(--badge-bg);
    border-radius: 9px;
    font-family: 'JetBrains Mono', monospace;
    font-weight: 700;
    color: #ffffff;
    .active & {
      background: rgba(124, 107, 245, 0.35);
      color: #ffffff;
    }
  }
}

/* ===== 工具卡片网格 ===== */
.tools-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
  margin-bottom: 30px;
  min-height: 200px;
}

/* ===== 卡片核心样式 ===== */
.tool-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 22px 20px 18px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 16px;
  cursor: default;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: cardAppear 0.55s ease both;
  overflow: hidden;

  /* 旋转边框光效 */
  .card-glow-border {
    position: absolute;
    top: -50%; left: -50%;
    width: 200%; height: 200%;
    background: conic-gradient(
      from 0deg,
      transparent,
      rgba(0, 245, 212, 0.04),
      transparent,
      rgba(123, 97, 255, 0.04),
      transparent
    );
    animation: rotateBorder 8s linear infinite;
    pointer-events: none;
    opacity: 0;
    transition: opacity 0.4s;
  }

  &:hover {
    transform: translateY(-8px) scale(1.01);
    border-color: rgba(0, 245, 212, 0.25);
    box-shadow:
      0 20px 60px rgba(0, 0, 0, 0.3),
      0 0 40px rgba(0, 245, 212, 0.05);
    .card-glow-border { opacity: 1; }
    .card-exec-btn { opacity: 1; transform: translateY(0); }
    .card-icon-box::after { opacity: 0.5; }
  }

  &.is-executing {
    border-color: rgba(245, 158, 11, 0.3);
    animation: cardAppear 0.55s ease both, executeGlow 2s ease-in-out infinite;
  }
  &.is-configuring,
  &.is-upgrading {
    opacity: 0.65;
    .card-exec-btn { opacity: 1; transform: translateY(0); }
  }
}

/* 分类色条 */
.card-stripe {
  position: absolute;
  top: 0; left: 24px; right: 24px;
  height: 2px;
  opacity: 0.6;
  border-radius: 0 0 2px 2px;
}

/* 图标区 */
.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}
.card-icon-box {
  position: relative;
  width: 50px; height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  &::after {
    content: '';
    position: absolute;
    inset: -10px;
    border-radius: 50%;
    background: var(--c, #00f5d4);
    filter: blur(14px);
    opacity: 0;
    transition: opacity 0.4s;
    pointer-events: none;
  }
}
.card-emoji { font-size: 1.6rem; z-index: 1; }
.icon-pulse {
  position: absolute;
  width: 10px; height: 10px;
  border-radius: 50%;
  top: -2px; right: -2px;
  animation: miniPulse 2s ease-in-out infinite;
}

/* 状态标签 */
.card-status-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 500;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid transparent;
  &.available {
    color: var(--status-ready-text);
    background: var(--status-ready-bg);
    border-color: var(--status-ready-border);
    .badge-dot { background: var(--status-ready-text); box-shadow: 0 0 8px rgba(52, 211, 153, 0.5); }
  }
  &.executing {
    color: #f59e0b;
    .badge-dot { background: #f59e0b; animation: pulseDot 1.2s ease-in-out infinite; box-shadow: 0 0 8px rgba(245, 158, 11, 0.5); }
  }
  &.configuring {
    color: #3a86ff;
    .badge-dot { background: #3a86ff; animation: pulseDot 1.8s ease-in-out infinite; box-shadow: 0 0 8px rgba(58, 134, 255, 0.4); }
  }
  &.upgrading {
    color: #7b61ff;
    .badge-dot { background: #7b61ff; animation: pulseDot 1.8s ease-in-out infinite; }
  }
  &.unavailable {
    color: var(--status-off-text);
    background: var(--status-off-bg);
    border-color: var(--status-off-border);
    .badge-dot { background: var(--status-off-text); }
  }
}
.badge-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 名称 & 描述 */
.card-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-badge {
  flex-shrink: 0;
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 600;
  color: #00f5d4;
  background: linear-gradient(135deg, rgba(0, 245, 212, 0.12), rgba(58, 134, 255, 0.12));
  border: 1px solid rgba(0, 245, 212, 0.25);
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.3px;
  animation: aiBadgePulse 2.4s ease-in-out infinite;
}

@keyframes aiBadgePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(0, 245, 212, 0); }
  50% { box-shadow: 0 0 6px 0 rgba(0, 245, 212, 0.25); }
}

.card-name {
  margin: 0 0 6px;
  font-size: 0.88rem;
  font-weight: 600;
  color: #f0f0ff;
  font-family: 'JetBrains Mono', monospace;
  word-break: break-all;
  line-height: 1.3;
}
.card-desc {
  margin: 0 0 14px;
  font-size: 0.78rem;
  color: var(--text-sub);
  line-height: 1.55;
  flex: 1;
}

/* 使用率进度条 */
.card-bar-wrap {
  margin-bottom: 14px;
}
.card-bar-track {
  height: 3px;
  background: rgba(100, 100, 180, 0.08);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 6px;
}
.card-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #00f5d4, #7b61ff);
  border-radius: 4px;
  transition: width 1.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.bar-count {
  font-size: 0.7rem;
  color: var(--text-data-blue);
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

/* 执行按钮 */
.card-exec-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 0;
  border: none;
  border-radius: 10px;
  background: var(--btn-gradient);
  color: #ffffff;
  font-weight: 600;
  font-size: 0.8rem;
  font-family: inherit;
  cursor: pointer;
  opacity: 0;
  transform: translateY(4px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: 2px;

  &:hover:not(:disabled) {
    transform: scale(1.04) translateY(-2px) !important;
    box-shadow: 0 4px 20px rgba(124, 107, 245, 0.4);
  }
  &.running {
    opacity: 1;
    transform: translateY(0);
    background: linear-gradient(135deg, #f59e0b, #f97316);
    animation: pulseBtn 1.5s ease-in-out infinite;
    color: #fff;
  }
  &.disabled {
    opacity: 0;
    cursor: not-allowed;
  }
  &:disabled { cursor: not-allowed; }
}
.btn-ico { font-size: 0.82rem; }
.btn-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 空状态 */
.empty-box {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 20px;
  margin: 8px;
  border: 1px dashed rgba(106, 112, 144, 0.35);
  border-radius: 16px;
  background: rgba(30, 38, 56, 0.25);
}
.empty-ring {
  width: 80px; height: 80px;
  border-radius: 50%;
  border: 2px dashed rgba(0, 245, 212, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  animation: ringPulse 3s ease-in-out infinite;
}
.empty-ico { font-size: 2.2rem; opacity: 0.5; }
.empty-msg { margin: 0 0 6px; font-size: 1rem; color: var(--text-placeholder); }
.empty-hint { margin: 0; font-size: 0.82rem; color: var(--text-placeholder); opacity: 0.85; }

/* ===== 执行日志面板 ===== */
.log-panel {
  position: relative;
  z-index: 1;
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 16px;
  overflow: hidden;
  animation: slideUp 0.6s ease 0.35s both;
}
.log-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.2s;
  user-select: none;
  &:hover { background: rgba(0, 245, 212, 0.025); }
}
.log-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.log-pulse-dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: #00f5d4;
  animation: pulseDot 2s ease-in-out infinite;
  box-shadow: 0 0 8px rgba(0, 245, 212, 0.4);
}
.log-title { font-size: 0.88rem; font-weight: 600; color: #ffffff; }
.log-badge {
  font-size: 0.68rem;
  padding: 2px 8px;
  background: rgba(0, 245, 212, 0.1);
  color: #00f5d4;
  border-radius: 9px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
}
.log-head-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.log-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px; height: 32px;
  border: 1px solid #6a6080;
  border-radius: 6px;
  background: rgba(30, 38, 56, 0.6);
  color: #c8c0d8;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  &:hover:not(:disabled) {
    color: #e8e6f0;
    border-color: var(--border-ctrl-hover);
    background: rgba(124, 107, 245, 0.1);
  }
  /* 文本按钮（如查看执行历史） */
  &.link {
    width: auto; padding: 0 16px;
    font-size: 13px; font-weight: 500; white-space: nowrap;
    color: #00f5d4; border-color: rgba(0, 245, 212, 0.25);
    &:hover:not(:disabled) { background: rgba(0, 245, 212, 0.08); border-color: rgba(0, 245, 212, 0.4); }
  }
  &.danger {
    color: #ff8080;
    &:hover:not(:disabled) {
      color: var(--status-off-text);
      border-color: rgba(239, 68, 68, 0.4);
      background: var(--status-off-bg);
    }
  }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.log-arrow {
  font-size: 1rem;
  color: var(--text-sub);
  transition: transform 0.3s;
  display: inline-block;
  &.open { transform: rotate(180deg); }
}

.log-body {
  max-height: 400px;
  overflow-y: auto;
  border-top: 1px solid rgba(100, 100, 180, 0.06);
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(100, 100, 180, 0.12); border-radius: 2px; }
}
.log-list { padding: 0; }
.log-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 20px;
  font-size: 0.8rem;
  font-family: 'JetBrains Mono', monospace;
  border-bottom: 1px solid rgba(100, 100, 180, 0.05);
  animation: slideInRight 0.35s ease both;
  transition: background 0.2s;
  &:hover { background: rgba(0, 245, 212, 0.025); }
  &:last-child { border-bottom: none; }
}
.log-time {
  color: var(--text-sub);
  min-width: 62px;
  font-size: 0.72rem;
}
.log-tool {
  color: #00f5d4;
  min-width: 110px;
  font-weight: 500;
}
.log-params {
  color: #ffbe0b;
  font-size: 0.72rem;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.log-status-cell {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-left: auto;
  font-size: 0.72rem;
}
.log-status-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  &.success { background: var(--status-ready-text); box-shadow: 0 0 6px rgba(52, 211, 153, 0.5); }
  &.error { background: #f87171; box-shadow: 0 0 6px rgba(248, 113, 113, 0.5); }
  &.executing { background: #f59e0b; animation: pulseDot 1s ease-in-out infinite; }
}
.log-dur {
  color: var(--text-sub);
  font-size: 0.72rem;
  min-width: 48px;
  text-align: right;
}
/* 单条删除按钮（悬停行时显示） */
.log-del-btn {
  flex-shrink: 0;
  font-size: 0.72rem;
  cursor: pointer;
  opacity: 0;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
  &:hover { background: rgba(255, 107, 107, 0.12); }
}
.log-row:hover .log-del-btn { opacity: 1; }
.log-empty {
  text-align: center;
  padding: 28px 16px;
  color: var(--text-placeholder);
  font-size: 0.85rem;
  margin: 8px 12px;
  border: 1px dashed rgba(106, 112, 144, 0.35);
  border-radius: 10px;
  background: rgba(30, 38, 56, 0.25);
}
.log-empty-ico { margin-right: 6px; }
.log-empty-main { font-size: 0.9rem; font-weight: 600; color: #c0c0d8; margin-top: 4px; }
.log-empty-sub { font-size: 0.75rem; color: #606090; margin-top: 4px; }
.log-retry-btn {
  display: inline-flex; align-items: center; gap: 4px;
  margin-top: 10px; padding: 6px 16px; height: 32px;
  border: 1px solid rgba(0, 245, 212, 0.3); border-radius: 6px;
  background: rgba(0, 245, 212, 0.06); color: #00f5d4;
  font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s;
  &:hover { background: rgba(0, 245, 212, 0.12); border-color: rgba(0, 245, 212, 0.5); }
}

/* ===== 底部 MCP 状态栏 ===== */
.mcp-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  z-index: 100;
  padding: 10px 40px;
  background: rgba(10, 10, 26, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(100, 100, 180, 0.08);
}
.mcp-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.78rem;
}
.mcp-live-dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
  animation: pulseDot 2.5s ease-in-out infinite;
}
.mcp-label { color: var(--text-sub); }
.mcp-status-tag {
  padding: 2px 10px;
  border-radius: 8px;
  font-size: 0.72rem;
  font-weight: 500;
  border: 1px solid transparent;
  &.online {
    background: var(--status-ready-bg);
    color: var(--status-ready-text);
    border-color: var(--status-ready-border);
  }
}
.mcp-meta { color: var(--text-sub); margin-left: 4px; }

/* ===== 批量执行覆盖层 ===== */
.batch-overlay {
  position: fixed;
  bottom: 56px; left: 50%;
  transform: translateX(-50%);
  z-index: 200;
}
.batch-box {
  padding: 18px 26px;
  background: rgba(10, 10, 26, 0.9);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 14px;
  min-width: 320px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}
.batch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 0.88rem;
  font-weight: 600;
  color: #f0f0ff;
}
.batch-spin {
  width: 16px; height: 16px;
  border: 2px solid rgba(0, 245, 212, 0.3);
  border-top-color: #00f5d4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.batch-ct {
  margin-left: auto;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.82rem;
  color: #606090;
}
.batch-track {
  height: 4px;
  background: rgba(100, 100, 180, 0.08);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 14px;
}
.batch-fill {
  height: 100%;
  background: linear-gradient(90deg, #00f5d4, #7b61ff);
  border-radius: 2px;
  transition: width 0.3s;
}
.batch-stop-btn {
  width: 100%;
  padding: 9px;
  background: rgba(255, 0, 110, 0.08);
  border: 1px solid rgba(255, 0, 110, 0.25);
  border-radius: 10px;
  color: #ff006e;
  font-size: 0.82rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: rgba(255, 0, 110, 0.14); }
}

/* ===== 执行弹窗 ===== */
.exec-dialog-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}
.exec-dialog {
  width: 480px; max-width: 90vw; max-height: 80vh;
  background: rgba(17, 17, 39, 0.96);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 16px;
  box-shadow: 0 16px 60px rgba(0, 0, 0, 0.5);
  animation: modalEnter 0.3s ease;
  display: flex; flex-direction: column;
}
.exec-dialog-header {
  display: flex; align-items: center; gap: 14px;
  padding: 20px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.exec-dialog-icon { font-size: 2rem; flex-shrink: 0; }
.exec-dialog-info { flex: 1; h3 { font-size: 15px; font-weight: 700; color: #e8e8ff; margin: 0 0 4px; } p { font-size: 13px; color: #a0a0c8; margin: 0; } }
.exec-dialog-close {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(100, 100, 180, 0.08); border: none; border-radius: 6px;
  font-size: 15px; cursor: pointer; color: #9090b8; transition: all 0.15s; flex-shrink: 0;
  &:hover:not(:disabled) { background: rgba(100, 100, 180, 0.15); color: #e8e8ff; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.exec-dialog-body {
  padding: 20px; overflow-y: auto; overflow-x: auto; flex: 1;
  max-height: calc(80vh - 140px);
  &::-webkit-scrollbar { width: 4px; height: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(100, 100, 180, 0.15); border-radius: 2px; }
}
.exec-params-title {
  font-size: 13px; font-weight: 600; color: #d8dce8;
  margin-bottom: 14px;
}
.exec-param-row {
  margin-bottom: 14px;
  &:last-child { margin-bottom: 0; }
}
.exec-param-label {
  display: block; font-size: 12px; font-weight: 600; color: #c0c8e0;
  margin-bottom: 6px;
}
.exec-param-required { color: #ff4060; margin-left: 2px; }
.exec-param-type {
  font-size: 10px; color: #606090; margin-left: 6px;
  font-family: 'JetBrains Mono', monospace;
  background: rgba(100, 100, 180, 0.08);
  padding: 1px 6px; border-radius: 4px;
}
.exec-param-input {
  width: 100%; padding: 10px 14px;
  background: var(--bg-ctrl);
  border: 1px solid var(--border-ctrl);
  border-radius: 10px;
  font-size: 13px; color: var(--text-input); outline: none;
  caret-color: var(--title-status);
  transition: all 0.2s; font-family: inherit;
  box-sizing: border-box;
  &:focus { border-color: var(--border-ctrl-hover); box-shadow: 0 0 0 3px rgba(124, 107, 245, 0.1); }
  &::placeholder { color: var(--text-placeholder); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.exec-param-textarea {
  resize: vertical;
  min-height: 60px;
  line-height: 1.5;
}

/* number 输入框 spinner 按钮深色主题 */
:deep(.exec-param-input[type="number"]::-webkit-inner-spin-button),
:deep(.exec-param-input[type="number"]::-webkit-outer-spin-button) {
  -webkit-appearance: none;
  appearance: none;
  margin: 0;
}

/* el-select 深色主题覆盖 */
.exec-param-select {
  width: 100%;
}
:deep(.exec-param-select .el-select__wrapper) {
  background: var(--bg-ctrl);
  border: 1px solid var(--border-ctrl);
  border-radius: 10px;
  box-shadow: none;
  padding: 0 12px;
  min-height: 38px;
  transition: all 0.2s;
  display: flex !important;
  align-items: center !important;
  &:hover {
    border-color: var(--border-ctrl-hover);
  }
  &.is-focused {
    border-color: var(--border-ctrl-hover);
    box-shadow: 0 0 0 3px rgba(124, 107, 245, 0.1);
  }
}
:deep(.exec-param-select .el-select__placeholder) {
  color: var(--text-placeholder);
}
:deep(.exec-param-select .el-select__selected-item) {
  color: var(--text-input);
  font-size: 13px;
}
:deep(.exec-param-select .el-select__caret) {
  color: #8080b0;
  font-size: 14px;
  background: transparent !important;
}
:deep(.exec-param-select .el-select__suffix) {
  background: transparent !important;
  flex-shrink: 0 !important;
  display: flex !important;
  align-items: center !important;
}
:deep(.exec-param-select .el-select__input) {
  background: transparent !important;
}
:deep(.exec-param-select .el-select__input-wrapper.is-hidden) {
  display: none !important;
}
:deep(.exec-param-select .el-select__selection) {
  background: transparent !important;
  flex: 1 !important;
  display: flex !important;
  align-items: center !important;
}
:deep(.exec-param-select.is-disabled .el-select__wrapper) {
  opacity: 0.5;
  cursor: not-allowed;
}

/* el-select 下拉菜单深色主题 */
:deep(.exec-param-popper) {
  background: rgba(22, 22, 50, 0.98) !important;
  border: 1px solid rgba(100, 100, 180, 0.15) !important;
  border-radius: 10px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5) !important;
  backdrop-filter: blur(12px);
  padding: 4px !important;
}
:deep(.exec-param-popper .el-select-dropdown__item) {
  color: #d8dce8;
  font-size: 13px;
  border-radius: 6px;
  padding: 6px 12px;
  margin: 2px 0;
  &:hover {
    background: rgba(124, 107, 245, 0.15);
    color: #ffffff;
  }
  &.selected {
    color: #00f5d4;
    background: rgba(0, 245, 212, 0.08);
    font-weight: 600;
  }
  &.hover {
    background: rgba(100, 100, 180, 0.12);
  }
}
:deep(.exec-param-popper .el-popper__arrow::before) {
  background: rgba(22, 22, 50, 0.98) !important;
  border-color: rgba(100, 100, 180, 0.15) !important;
}

/* 布尔类型开关样式 */
.exec-param-boolean {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.exec-param-boolean-label {
  font-size: 13px;
  color: var(--text-input);
}
:deep(.exec-param-boolean .el-switch) {
  --el-switch-off-color: rgba(100, 100, 180, 0.3);
  --el-switch-on-color: rgba(0, 245, 212, 0.6);
}

.exec-no-params {
  display: flex; align-items: center; gap: 10px;
  padding: 20px; background: rgba(30, 38, 56, 0.4);
  border: 1px dashed rgba(106, 112, 144, 0.35);
  border-radius: 10px; color: var(--text-sub); font-size: 13px;
}
.exec-no-params-icon { font-size: 1.3rem; }
.exec-dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.exec-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 20px; border-radius: 8px;
  font-weight: 600; font-size: 13px;
  cursor: pointer; transition: all 0.2s; border: none;
  font-family: inherit;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.exec-btn-cancel {
  background: rgba(100, 100, 180, 0.08); color: #c0c0e0;
  &:hover:not(:disabled) { background: rgba(100, 100, 180, 0.15); }
}
.exec-btn-exec {
  background: var(--btn-gradient); color: #fff;
  &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(124, 107, 245, 0.4); }
}
.exec-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* ===== 一键串联快捷按钮 ===== */
.quiz-shortcut-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 18px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12) 0%, rgba(124, 107, 245, 0.12) 100%);
  border: 1px solid rgba(255, 107, 157, 0.3);
  border-radius: 12px;
  animation: slideUp 0.3s ease both;
}
.shortcut-text {
  font-size: 13px;
  color: #f0f0ff;
  line-height: 1.5;
}
.shortcut-btn {
  flex-shrink: 0;
  padding: 8px 18px;
  background: linear-gradient(135deg, #ff6b9d 0%, #a78bfa 100%);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(255, 107, 157, 0.4);
  }
}

/* 结果弹窗 */
.exec-result-status {
  display: flex; align-items: center; gap: 8px;
  padding: 12px; background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.12);
  border-radius: 10px; margin-bottom: 16px;
}
.exec-result-dot {
  width: 8px; height: 8px; border-radius: 50%;
  &.success { background: #10b981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.4); }
  &.error { background: #ff4060; box-shadow: 0 0 8px rgba(255, 64, 96, 0.4); }
}
.exec-result-text { font-size: 13px; font-weight: 600; color: #10b981;
  &.error { color: #ff4060; } }
.exec-result-status {
  &.error { background: rgba(255, 64, 96, 0.06); border-color: rgba(255, 64, 96, 0.12); } }
.exec-result-content {
  margin-bottom: 12px;
  &.error {
    .exec-result-title { color: #ff4060; }
    .exec-result-json { border-color: rgba(255, 64, 96, 0.15); background: rgba(255, 64, 96, 0.04); }
  }
}
.exec-result-title {
  font-size: 12px; font-weight: 600; color: #a0a0c8;
  margin-bottom: 8px;
}
.exec-result-json {
  padding: 12px; border-radius: 8px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08);
  font-size: 12px; line-height: 1.6;
  color: #c0c0e0; overflow-x: auto;
  font-family: 'JetBrains Mono', monospace;
  white-space: pre-wrap; word-break: break-all;
  margin: 0;
}

/* ==============================================
   动画关键帧
   ============================================== */
@keyframes slideUp {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cardAppear {
  from { opacity: 0; transform: translateY(24px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes rotateBorder {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes executeGlow {
  0%, 100% { box-shadow: 0 0 0 rgba(245, 158, 11, 0); }
  50% { box-shadow: 0 0 24px rgba(245, 158, 11, 0.12); }
}

@keyframes pulseDot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@keyframes miniPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

@keyframes pulseBtn {
  0%, 100% { box-shadow: 0 0 0 rgba(245, 158, 11, 0); }
  50% { box-shadow: 0 0 20px rgba(245, 158, 11, 0.25); }
}

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(-16px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes ringPulse {
  0%, 100% { border-color: rgba(0, 245, 212, 0.1); transform: scale(1); }
  50% { border-color: rgba(0, 245, 212, 0.25); transform: scale(1.05); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalEnter {
  from { opacity: 0; transform: scale(0.95) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

/* ===== TransitionGroup ===== */
.card-trans-enter-active { transition: all 0.45s cubic-bezier(0.4, 0, 0.2, 1); }
.card-trans-leave-active { transition: all 0.3s ease; }
.card-trans-enter-from { opacity: 0; transform: translateY(20px) scale(0.94); }
.card-trans-leave-to { opacity: 0; transform: scale(0.9); }
.card-trans-move { transition: transform 0.45s cubic-bezier(0.4, 0, 0.2, 1); }

.log-trans-enter-active { transition: all 0.35s ease; }
.log-trans-leave-active { transition: all 0.2s ease; }
.log-trans-enter-from { opacity: 0; transform: translateX(-16px); }
.log-trans-leave-to { opacity: 0; transform: translateX(16px); }

/* ===== 响应式 ===== */
@media (max-width: 1400px) {
  .tools-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1024px) {
  .tools-grid { grid-template-columns: repeat(2, 1fr); }
  .header-row { flex-direction: column; gap: 18px; }
  .header-kpis { align-self: flex-start; }
  .tools-page { padding: 22px 24px 110px; }
}
@media (max-width: 640px) {
  .tools-grid { grid-template-columns: 1fr; }
  .tools-page { padding: 16px 16px 110px; }
  .page-title { font-size: 1.5rem; }
  .header-kpis { gap: 10px; flex-wrap: wrap; }
  .kpi { min-width: 60px; padding: 10px 14px; }
  .kpi-num { font-size: 1.1rem; }
  .cat-tabs { gap: 5px; }
  .cat-tab { padding: 6px 13px; font-size: 0.78rem; }
}

/* ===== 可展开日志行 ===== */
.log-expandable {
  cursor: pointer;
  transition: background 0.2s;
  flex-direction: column;
  padding: 0;

  &:hover {
    background: rgba(0, 245, 212, 0.025);
  }

  &.is-expanded {
    background: rgba(0, 245, 212, 0.03);
    border-bottom: 1px solid rgba(0, 245, 212, 0.08);
  }
}

.log-row-main {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 20px;
  font-size: 0.8rem;
  font-family: 'JetBrains Mono', monospace;
  width: 100%;
  box-sizing: border-box;
}

.log-expand-arrow {
  flex-shrink: 0;
  color: #606090;
  font-size: 8px;
  width: 12px;
  text-align: center;
  transition: transform 0.2s;
}

/* 日志展开详情面板 */
.log-detail-panel {
  padding: 0 20px 14px 46px;
  animation: slideDown 0.25s ease both;
}

.log-detail-section {
  margin-bottom: 12px;
}

.log-detail-label {
  font-size: 11px;
  font-weight: 600;
  color: #8080a8;
  margin-bottom: 6px;
}

.log-detail-json {
  padding: 10px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.06);
  border-radius: 8px;
  font-size: 11px;
  line-height: 1.5;
  color: #b0b0d0;
  font-family: 'JetBrains Mono', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 180px;
  overflow-y: auto;
}

/* 参数键值对网格 */
.log-detail-params-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 0;
  padding: 10px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.06);
  border-radius: 8px;
}
.log-detail-params-empty {
  font-size: 11px;
  color: #606090;
  font-style: italic;
}
.log-detail-param-item {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 10px 2px 8px;
  margin: 1px 4px 1px 0;
  background: rgba(100, 100, 180, 0.06);
  border-radius: 4px;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  line-height: 1.6;
}
.param-key {
  color: #7b61ff;
  font-weight: 600;
}
.param-colon {
  color: #606090;
  margin: 0 2px;
}
.param-val {
  color: #c0c0e0;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.param-val.param-val-str {
  color: #ffbe0b;
}
.param-val.param-val-num {
  color: #00f5d4;
}

/* 结果摘要增强 */
.log-detail-result-text {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.06);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #9090b8;
}
.result-summary-icon {
  flex-shrink: 0;
  font-size: 16px;
  margin-top: 1px;
}
.result-summary-text {
  flex: 1;
  color: #b0b0d0;
  word-break: break-word;
}
.result-error-hint {
  color: #ff4060;
  font-size: 12px;
}

.log-detail-info {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.log-detail-info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.06);
  border-radius: 6px;
  font-size: 11px;
}

.info-label {
  color: #606090;
  font-weight: 500;
  font-size: 12px;
}

.info-value {
  color: #b0b0d0;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;

  &.success { color: #10b981; }
  &.error { color: #ff006e; }
  &.executing { color: #f59e0b; }
}

.info-value.type-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(123, 97, 255, 0.1);
  color: #7b61ff;
  font-weight: 500;
}

.log-detail-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.log-detail-btn {
  padding: 6px 12px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 6px;
  color: #8080a8;
  font-size: 11px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    color: #00f5d4;
    border-color: rgba(0, 245, 212, 0.2);
    background: rgba(0, 245, 212, 0.06);
  }
}

.log-detail-btn-primary {
  background: rgba(0, 245, 212, 0.08);
  border-color: rgba(0, 245, 212, 0.15);
  color: #00f5d4;
  font-weight: 600;
  &:hover {
    background: rgba(0, 245, 212, 0.14);
    border-color: rgba(0, 245, 212, 0.3);
    box-shadow: 0 0 12px rgba(0, 245, 212, 0.1);
    transform: translateY(-1px);
  }
}

/* ===== 结果弹窗增强 ===== */
.exec-result-dialog {
  width: 620px;
  max-height: 88vh;
  min-height: 320px;
}

.exec-result-dialog .exec-dialog-body {
  max-height: calc(88vh - 140px);
  min-height: 180px;
}

.exec-result-duration {
  margin-left: auto;
  font-size: 11px;
  color: #8080a8;
  font-family: 'JetBrains Mono', monospace;
}

.exec-result-render {
  margin-bottom: 12px;
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
}

.exec-dialog-footer-left {
  display: flex;
  gap: 6px;
}

.exec-dialog-footer-right {
  display: flex;
  gap: 10px;
}

.exec-btn-ghost {
  padding: 6px 12px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 6px;
  color: #8080a8;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  white-space: nowrap;

  &:hover:not(:disabled) {
    color: #00f5d4;
    border-color: rgba(0, 245, 212, 0.2);
    background: rgba(0, 245, 212, 0.06);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

</style>