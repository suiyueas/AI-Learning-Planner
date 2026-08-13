<template>
  <div ref="pageRef" class="agents-page">
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
        v-for="p in particles" :key="p.id" class="particle"
        :style="{
          left: p.x + '%', top: p.y + '%',
          width: p.size + 'px', height: p.size + 'px',
          animationDuration: p.duration + 's', animationDelay: p.delay + 's',
          opacity: p.opacity
        }"
      ></div>
    </div>

    <!-- ===== 顶部标题区 ===== -->
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">🧠</span>
            <span>人机协作指挥中心</span>
          </h1>
          <p class="page-subtitle">
            {{ subAgents.length + 1 }} 个智能体 ·
            <span class="stat-ok">{{ availableCount }} 个在线</span> ·
            <span class="stat-run">{{ executingCount > 0 ? executingCount + ' 个执行中' : '全部待命' }}</span> ·
            <span class="stat-total">总执行 {{ totalExecCount }} 次</span>
          </p>
        </div>
        <div class="header-kpis">
          <div class="kpi"><span ref="kpiTotalRef" class="kpi-num">{{ agents.length + 1 }}</span><span class="kpi-label">总智能体</span></div>
          <div class="kpi"><span class="kpi-num kpi-ok">{{ availableCount }}</span><span class="kpi-label">在线待命</span></div>
          <div class="kpi"><span class="kpi-num kpi-run">{{ executingCount }}</span><span class="kpi-label">执行中</span></div>
          <div class="kpi"><span ref="kpiExecRef" class="kpi-num kpi-accent">{{ displayTotalExec }}</span><span class="kpi-label">总执行</span></div>
        </div>
      </div>
    </header>

    <!-- ===== 主控 Agent (Orchestrator) ===== -->
    <section class="orchestrator-section" @click="toggleOrchExpand">
      <div class="section-label"><span class="label-dot"></span> 主控智能体 · Orchestrator</div>
      <div class="orch-card" :class="{ expanded: orchExpanded, active: isAnyExecuting }">
        <div class="orch-glow-border"></div>

        <div class="orch-main">
          <!-- 左侧：头像 & 状态 -->
          <div class="orch-left">
            <div class="orch-avatar">
              <svg viewBox="0 0 64 64" width="60" height="60">
                <circle cx="32" cy="32" r="28" fill="none" stroke="url(#orchGrad)" stroke-width="2.5" opacity="0.6" />
                <circle cx="32" cy="32" r="14" fill="none" stroke="url(#orchGrad)" stroke-width="1.5" opacity="0.4" />
                <circle cx="32" cy="32" r="5" fill="#00f5d4">
                  <animate attributeName="r" values="5;6;5" dur="2s" repeatCount="indefinite" />
                  <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" repeatCount="indefinite" />
                </circle>
                <defs>
                  <linearGradient id="orchGrad" x1="0" y1="0" x2="64" y2="64">
                    <stop offset="0%" stop-color="#00f5d4" /><stop offset="100%" stop-color="#7b61ff" />
                  </linearGradient>
                </defs>
              </svg>
              <div class="orch-pulse-ring" :class="{ active: isAnyExecuting }"></div>
            </div>
            <div class="orch-status-info">
              <h2 class="orch-name">编排 Agent</h2>
              <div class="orch-status-line">
                <span class="status-dot" :class="orchDotClass"></span>
                <span class="status-text">{{ orchStatusText }}</span>
              </div>
            </div>
          </div>

          <!-- 中间：ReAct 循环可视化 -->
          <div class="orch-react">
            <div class="react-title">
              <span class="react-pulse" :class="{ active: isAnyExecuting }"></span>
              <span>ReAct 推理循环</span>
              <span v-if="isAnyExecuting" class="react-badge">运行中</span>
            </div>
            <div class="react-steps">
              <div
                v-for="(step, si) in reactSteps" :key="step.id"
                class="react-step"
                :class="{
                  active: reactActiveStep === si,
                  completed: reactActiveStep > si,
                  idle: reactActiveStep < si && !isAnyExecuting
                }"
              >
                <!-- 连接线 -->
                <div v-if="si < reactSteps.length - 1" class="step-connector" :class="{ active: reactActiveStep > si, flowing: reactActiveStep === si && isAnyExecuting }">
                  <div class="connector-fill"></div>
                </div>

                <!-- 步骤图标 -->
                <div class="step-icon-wrap" :class="{ pulse: reactActiveStep === si && isAnyExecuting }">
                  <div class="step-icon-ring"></div>
                  <span class="step-icon">{{ step.icon }}</span>
                  <div v-if="reactActiveStep === si && isAnyExecuting" class="step-ripple"></div>
                </div>

                <!-- 步骤内容 -->
                <div class="step-body">
                  <div class="step-name">{{ step.name }}</div>
                  <div class="step-desc">{{ step.desc }}</div>
                  <div class="step-status-tag">{{ reactActiveStep < si ? '就绪' : reactActiveStep === si ? '进行中…' : '已完成' }}</div>
                </div>
              </div>
            </div>
            <!-- 迭代计数 -->
            <div v-if="isAnyExecuting" class="react-iter">
              <span class="iter-lbl">迭代</span>
              <span class="iter-num">{{ reactIteration }}</span>
            </div>
          </div>

          <!-- 右侧：展开指示 -->
          <div class="orch-right">
            <div class="orch-expand-hint">
              <svg viewBox="0 0 24 24" width="18" height="18" :class="{ rotated: orchExpanded }"><path d="M7 10l5 5 5-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
              <span>{{ orchExpanded ? '收起调度中心' : '展开调度中心' }}</span>
            </div>
          </div>
        </div>

        <!-- 展开详情 - 调度中心入口 -->
        <div v-show="orchExpanded" class="orch-detail">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-icon">🧩</span>
              <span class="detail-label">调度策略</span>
              <span class="detail-value">智能分配</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">⚡</span>
              <span class="detail-label">最大迭代</span>
              <span class="detail-value">30 步</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">🔗</span>
              <span class="detail-label">关联 Agent</span>
              <span class="detail-value">{{ subAgents.length }} 个</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📊</span>
              <span class="detail-label">系统状态</span>
              <span class="detail-value">🟢 正常</span>
            </div>
          </div>
          <button class="orch-sched-btn" @click.stop="openSchedulingCenter">
            <span>🚀</span>
            <span>打开调度中心</span>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>
    </section>

    <!-- ===== 子 Agent 卡片网格 ===== -->
    <section class="agents-section">
      <div class="section-label"><span class="label-dot"></span> 子智能体 ({{ subAgents.length }})</div>
      <div class="agents-grid">
        <TransitionGroup name="card-trans">
          <div
            v-for="(agent, idx) in subAgents" :key="agent.id"
            class="agent-card"
            :class="{ 'is-executing': agent.status === 'executing' }"
            :style="{ animationDelay: (idx * 0.06) + 's' }"
          >
            <div class="card-glow-border"></div>

            <!-- 顶部：图标 + 名称 + 迷你状态 -->
            <div class="card-header">
              <div class="card-icon-wrap" :class="{ executing: agent.status === 'executing' }">
                <span class="card-icon">{{ agent.icon }}</span>
                <div class="icon-status-dot" :class="agent.status"></div>
              </div>
              <div class="card-name-area">
                <h3 class="card-name">{{ agent.name }}</h3>
                <p class="card-role">{{ agent.role }}</p>
              </div>
            </div>

            <!-- 统计 & 工具行 -->
            <div class="card-meta">
              <span class="meta-stat">📊 {{ agent.execCount }} 次</span>
              <span class="meta-stat">⏱️ {{ agent.avgTime }}</span>
            </div>

            <!-- 工具标签 -->
            <div class="card-tools">
              <span v-for="tool in agent.tools" :key="tool" class="tool-tag">{{ tool }}</span>
            </div>

            <!-- 示例预览 -->
            <div class="card-example" :title="'点击执行查看更多示例'">
              <span class="example-text">{{ agent.example }}</span>
            </div>

            <!-- 按钮行 -->
            <div class="card-actions">
              <button
                class="exec-btn"
                :class="{ running: agent.status === 'executing' }"
                :disabled="agent.status === 'executing'"
                @click="openTaskDialog(agent)"
              >
                <span v-if="agent.status === 'executing'" class="btn-spinner-mini"></span>
                <span v-else class="exec-ico">▶</span>
                <span>{{ agent.status === 'executing' ? '执行中' : '执行' }}</span>
              </button>
            </div>
          </div>
        </TransitionGroup>
      </div>
    </section>

    <!-- ===== 任务执行流面板（执行历史统一入口） ===== -->
    <section class="flow-panel" :class="{ 'has-active': executionLogs.length > 0 }">
      <div class="flow-head" @click="flowExpanded = !flowExpanded">
        <div class="flow-head-left">
          <div class="flow-indicator" :class="{ active: isAnyExecuting }"></div>
          <span class="flow-title">📋 执行历史</span>
          <!-- 视图切换：全部记录 / 回收站 -->
          <div class="flow-tabs" @click.stop>
            <span class="flow-tab" :class="{ active: flowView === 'list' }" @click="switchFlowView('list')">
              全部 <span v-if="successLogCount > 0" class="flow-badge">{{ successLogCount }}</span>
            </span>
            <span class="flow-tab" :class="{ active: flowView === 'trash' }" @click="switchFlowView('trash')">
              回收站 <span v-if="trashLogs.length > 0" class="flow-badge trash">{{ trashLogs.length }}</span>
            </span>
          </div>
          <span v-if="isAnyExecuting" class="flow-live">● 实时</span>
        </div>
        <div class="flow-head-right" @click.stop>
          <template v-if="flowView === 'list'">
            <input v-model="flowKeyword" class="flow-search" placeholder="🔍 搜索记录..." @input="applyFlowFilter" />
            <select v-model="flowFilterStatus" class="flow-filter-select" @change="applyFlowFilter">
              <option value="all">全部状态</option>
              <option value="success">✅ 成功</option>
              <option value="error">❌ 失败</option>
              <option value="executing">⏳ 执行中</option>
            </select>
            <select v-model="flowFilterAgent" class="flow-filter-select" @change="applyFlowFilter">
              <option value="all">全部Agent</option>
              <option value="tool">🔧 工具调用</option>
              <option v-for="agent in subAgents" :key="agent.id" :value="agent.id">{{ agent.icon }} {{ agent.name }}</option>
            </select>
            <button v-if="selectedLogIds.size > 0" class="flow-btn batch" :title="'批量删除已选 ' + selectedLogIds.size + ' 条'" @click.stop="batchDeleteSelected">
              🗑️ 批量({{ selectedLogIds.size }})
            </button>
            <button class="flow-btn danger" :disabled="executionLogs.length === 0" title="清空日志" @click.stop="handleClearLogs">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
            </button>
          </template>
          <template v-else>
            <button class="flow-btn danger text" :disabled="trashLogs.length === 0" title="清空回收站（永久删除）" @click.stop="emptyTrash">
              🧹 清空回收站
            </button>
          </template>
          <span class="flow-arrow" :class="{ open: flowExpanded }">▾</span>
        </div>
      </div>

      <div v-show="flowExpanded" ref="flowBodyRef" class="flow-body">
        <!-- ===== 回收站视图（软删除记录） ===== -->
        <div v-if="flowView === 'trash'" class="flow-list-wrap trash-wrap">
          <div v-if="trashLoading" class="flow-empty"><span class="flow-empty-ico">⏳</span> 正在加载回收站...</div>
          <div v-else-if="trashError" class="flow-empty">
            <span class="flow-empty-ico">⚠️</span> 加载回收站失败，请检查网络后重试
            <button class="flow-retry-btn" @click="loadTrashLogs">🔄 重试</button>
          </div>
          <template v-else>
            <div class="flow-list">
              <div v-for="log in trashLogs" :key="log.id" class="flow-row trash-row">
                <div class="flow-row-main">
                  <div class="flow-time">{{ log.time }}</div>
                  <div class="flow-line">
                    <div class="flow-agent">{{ log.agentName }}</div>
                    <div class="flow-step-badge task">{{ log.stepLabel }}</div>
                    <div class="flow-msg">{{ log.description }}</div>
                  </div>
                  <div class="trash-actions">
                    <button class="flow-action-btn restore-btn" title="恢复到执行记录" @click="restoreTrashLog(log)">↩️ 恢复</button>
                    <button class="flow-action-btn delete-btn" title="彻底删除（不可恢复）" @click="hardDeleteTrashLog(log)">🔥 彻底删除</button>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="trashLogs.length === 0" class="flow-empty">
              <span class="flow-empty-ico">🗑️</span> 回收站是空的，软删除的记录会出现在这里
            </div>
          </template>
        </div>

        <!-- ===== 全部记录视图（仅作索引/导航） ===== -->
        <div v-else ref="flowListWrapRef" class="flow-list-wrap">
          <!-- 加载中 -->
          <div v-if="flowLoading" class="flow-empty">
            <span class="flow-empty-ico">⏳</span> 正在加载执行历史...
          </div>
          <!-- 加载失败（与“无数据”区分） -->
          <div v-else-if="flowError" class="flow-empty">
            <span class="flow-empty-ico">⚠️</span> 加载执行历史失败，请检查网络后重试
            <button class="flow-retry-btn" @click="retryLoadHistory">🔄 重试</button>
          </div>
          <template v-else>
            <div v-if="filteredLogs.length > 0" class="flow-list-toolbar">
            <label class="flow-check all" @click.stop>
              <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
              <span class="flow-check-text">全选</span>
            </label>
            <span class="flow-list-count">共 {{ successLogCount }} 条执行记录</span>
          </div>
          <TransitionGroup name="log-trans" tag="div" class="flow-list">
            <div
              v-for="log in filteredLogs" :key="log.id"
              class="flow-row"
              :class="[log.stepType, log.status, { selected: selectedLogId === log.id }]"
              :title="'点击查看详情' + (log.output ? '（' + getAgentIcon(log.agentId) + ' ' + log.agentName + '）' : '')"
              @click="selectLog(log)"
            >
              <div class="flow-row-main">
                <label v-if="log.resultId || log.sourceType === 'tool'" class="flow-check" @click.stop>
                  <input type="checkbox" :checked="selectedLogIds.has(log.id)" @change="toggleSelectLog(log)" />
                </label>
                <div class="flow-time">{{ log.time }}</div>
                <div class="flow-line">
                  <div class="flow-agent" :style="{ color: agentColor(log.agentId) }">
                    <span v-if="log.sourceType === 'tool'" class="flow-type-tag tool">🔧 工具</span>
                    <span v-else class="flow-type-tag agent">🤖 Agent</span>
                    {{ log.agentName }}
                  </div>
                  <div class="flow-step-badge" :class="log.stepType">{{ log.stepLabel }}</div>
                  <div class="flow-msg">{{ log.description }}</div>
                </div>
              </div>
            </div>
          </TransitionGroup>
          <div v-if="filteredLogs.length === 0 && executionLogs.length > 0" class="flow-empty">
            <span class="flow-empty-ico">🔍</span> 没有符合条件的执行记录，请调整筛选条件
          </div>
          <div v-if="executionLogs.length === 0" class="flow-empty">
            <span class="flow-empty-ico">📭</span>
            <div class="flow-empty-main">暂无执行记录</div>
            <div class="flow-empty-sub">试试运行一个 Agent 或工具，记录将在这里显示</div>
          </div>
          </template>
        </div>
</div>
    </section>

    <!-- ===== 执行详情弹窗（统一：元信息 + 用户输入 + 按结果类型渲染 + 操作栏） ===== -->
    <ExecutionDetailModal
      v-if="selectedLog"
      :log="selectedLog"
      :list="filteredLogs"
      :index="currentLogIndex"
      :fallback-generator="regenerateResultForLog"
      @close="closeDetailPanel"
      @navigate="navigateLog"
      @delete="confirmDeleteLog"
    />

    <!-- ===== 调度中心弹窗 ===== -->
    <SchedulingCenter
      v-if="showScheduling"
      :sub-agents="subAgentsForScheduling"
      :available-count="availableCount"
      :total-count="subAgentsForScheduling.length"
      :executing-count="executingCount"
      :orchestrator-status="orchestratorStatus"
      :today-schedule-count="todayScheduleCount"
      :is-executing="schedulingExecuting"
      :recent-logs="recentSchedulingLogs"
      @close="closeSchedulingCenter"
      @execute-batch="handleSchedulingBatch"
      @view-all="viewAllSchedulingLogs"
    />

    <!-- ===== 结果详情弹窗 ===== -->
    <ResultDetailDialog
      v-if="showResultDetail"
      :result="selectedResult"
      :loading="resultLoading"
      @close="showResultDetail = false"
      @re-execute="reExecuteFromResult"
    />

    <!-- ===== 任务分配对话框 ===== -->
    <div v-if="showTaskDialog" class="dialog-overlay" @click="closeTaskDialog">
      <div class="task-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-header-left">
            <span class="dialog-agent-icon">{{ currentAgent?.icon }}</span>
            <div class="dialog-agent-info">
              <h3>{{ currentAgent?.name }}</h3>
              <p>{{ currentAgent?.role }}</p>
            </div>
          </div>
          <button class="dialog-close" :disabled="isTaskExecuting" @click="closeTaskDialog">✕</button>
        </div>

        <div class="dialog-body">
          <!-- 示例任务（按类别分组展示） -->
          <div class="example-tasks">
            <div class="example-label">💡 示例任务：<span class="example-hint">点击自动填充</span></div>
            <div class="example-scroll">
              <div
                v-for="(example, ei) in currentAgent?.examples || []" :key="ei"
                class="example-item"
                :class="{ active: taskInput === example.text }"
                @click="taskInput = example.text"
              >
                <span class="example-cat-tag">{{ example.cat }}</span>
                <span class="example-text">{{ example.text }}</span>
              </div>
            </div>
          </div>

          <!-- 专属元素 -->
          <div v-if="currentAgent?.id === 'diagnosis'" class="agent-specific">
            <button class="quick-btn" :disabled="isTaskExecuting" @click="quickDiagnosis">⚡ 快速诊断</button>
          </div>
          <div v-if="currentAgent?.id === 'planner'" class="agent-specific">
            <div class="specific-label">学习时长：</div>
            <div class="option-group">
              <button v-for="opt in plannerOptions" :key="opt.value" class="option-btn" :class="{ active: plannerDuration === opt.value }" :disabled="isTaskExecuting" @click="plannerDuration = opt.value; taskInput = opt.label">{{ opt.label }}</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'tutor'" class="agent-specific">
            <label class="switch-row"><span>🔍 知识检索增强</span><div class="toggle-switch" :class="{ active: tutorRagEnabled }" @click="tutorRagEnabled = !tutorRagEnabled"><div class="toggle-knob"></div></div></label>
          </div>
          <div v-if="currentAgent?.id === 'reporter'" class="agent-specific">
            <div class="specific-label">报告类型：</div>
            <div class="option-group">
              <button v-for="opt in reporterOptions" :key="opt.value" class="option-btn" :class="{ active: reporterType === opt.value }" :disabled="isTaskExecuting" @click="reporterType = opt.value; taskInput = opt.label">{{ opt.label }}</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'exercise'" class="agent-specific">
            <div class="specific-label">难度选择：</div>
            <div class="option-group">
              <button v-for="opt in difficultyOptions" :key="opt.value" class="option-btn" :class="{ active: exerciseDifficulty === opt.value }" :disabled="isTaskExecuting" @click="exerciseDifficulty = opt.value">{{ opt.label }}</button>
            </div>
            <div class="specific-label" style="margin-top:12px">题目数量：</div>
            <div class="count-input-wrap">
              <button class="count-btn" :disabled="isTaskExecuting || exerciseCount <= 1" @click="exerciseCount--">−</button>
              <span class="count-value">{{ exerciseCount }}</span>
              <button class="count-btn" :disabled="isTaskExecuting || exerciseCount >= 20" @click="exerciseCount++">+</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'search'" class="agent-specific">
            <div class="specific-label">搜索范围：</div>
            <div class="option-group">
              <button v-for="opt in searchRangeOptions" :key="opt.value" class="option-btn" :class="{ active: searchRange === opt.value }" :disabled="isTaskExecuting" @click="searchRange = opt.value">{{ opt.label }}</button>
            </div>
          </div>
          <div v-if="currentAgent?.id === 'knowledge'" class="agent-specific">
            <div class="specific-label">文档筛选：</div>
            <div class="option-group">
              <button v-for="opt in docFilterOptions" :key="opt.value" class="option-btn" :class="{ active: docFilter === opt.value }" :disabled="isTaskExecuting" @click="docFilter = opt.value">{{ opt.label }}</button>
            </div>
          </div>

          <!-- 任务输入 -->
          <div class="task-input-wrap">
            <label class="input-label">请输入任务描述：</label>
            <textarea v-model="taskInput" class="task-textarea" :placeholder="'为 ' + (currentAgent?.name || '') + ' 分配任务...'" rows="3" :disabled="isTaskExecuting"></textarea>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="dialog-btn dialog-btn-cancel" :disabled="isTaskExecuting" @click="closeTaskDialog">取消</button>
          <button class="dialog-btn dialog-btn-exec" :disabled="isTaskExecuting || !taskInput.trim()" @click="submitTask">
            <span v-if="isTaskExecuting" class="btn-spinner-small"></span>
            <span v-else>🚀</span>
            <span>{{ isTaskExecuting ? '执行中...' : '分配任务' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, onActivated, onDeactivated, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmAction, cleanupDialogs } from '@/utils/modalHelper'
import { saveAgentExecution, getAllResults, clearAllResults, deleteResultById, deleteResultsBatch, getTrashResults, restoreResult } from '@/api/agentApi'
import { getToolExecutionHistory, deleteToolExecution } from '@/api/toolsApi'
import { parseResultContent } from '@/utils/markdown'
import { securityFilter } from '@/utils/securityUtils'
import SchedulingCenter from '@/components/agent/SchedulingCenter.vue'
import ResultDetailDialog from '@/components/agent/ResultDetailDialog.vue'
import ExecutionDetailModal from '@/components/agent/ExecutionDetailModal.vue'

const route = useRoute()
const pageRef = ref(null)
const flowBodyRef = ref(null)
const flowListWrapRef = ref(null)
const flowExpanded = ref(true)
const orchExpanded = ref(false)
const orchestratorStatus = ref('IDLE')
const showScheduling = ref(false)
const schedulingExecuting = ref(false)
const todayScheduleCount = ref(0)
const flowFilterStatus = ref('all')
const flowFilterAgent = ref('all')
const filteredLogs = ref([])
const selectedLogId = ref(null)
const selectedLog = computed(() => filteredLogs.value.find(log => log.id === selectedLogId.value) || null)
const currentLogIndex = computed(() => filteredLogs.value.findIndex(log => log.id === selectedLogId.value))
const recentSchedulingLogs = ref([])
const currentSessionId = ref(null)
const showResultDetail = ref(false)
const selectedResult = ref(null)
const resultLoading = ref(false)

// ===== 浮动粒子 =====
const particles = ref([])
const generateParticles = () => {
  const arr = []
  for (let i = 0; i < 35; i++) {
    arr.push({ id: i, x: Math.random() * 100, y: Math.random() * 100, size: Math.random() * 3 + 1, duration: Math.random() * 25 + 15, delay: Math.random() * 12, opacity: Math.random() * 0.35 + 0.08 })
  }
  particles.value = arr
}

// ===== ReAct 步骤配置 =====
const reactSteps = [
  { id: 'think', icon: '🤔', name: '思考', desc: '分析任务，拆解问题' },
  { id: 'act', icon: '⚡', name: '行动', desc: '调用工具，获取信息' },
  { id: 'observe', icon: '👁️', name: '观察', desc: '整合结果，优化输出' }
]
const reactActiveStep = ref(-1)
const reactIteration = ref(0)

// ===== Agent 配置（含丰富示例） =====
const agentConfigs = [
  {
    id: 'diagnosis', name: '诊断Agent', icon: '🔍',
    role: '能力测评 · 画像构建 · 薄弱点挖掘',
    tools: ['智能测评出题', '全域学术检索'], avgTime: '2.3s',
    example: '💡 诊断我的 Python 学习水平',
    categories: ['🏷️ 能力测评', '🏷️ 画像构建', '🏷️ 薄弱分析'],
    examples: [
      { cat: '能力测评', text: '诊断我的 Python 学习水平，找出薄弱点' },
      { cat: '能力测评', text: '评估我的数据分析能力，给出评分和建议' },
      { cat: '画像构建', text: '生成完整的用户画像报告，包含学习风格分析' },
      { cat: '薄弱分析', text: '分析我当前的知识盲区，给出提升优先级' },
      { cat: '能力测评', text: '对我进行前端开发能力综合测评' }
    ]
  },
  {
    id: 'planner', name: '规划Agent', icon: '🗺️',
    role: '路径生成 · 动态调整 · 资源推荐',
    tools: ['全域学术检索', '全链路学习助手'], avgTime: '3.1s',
    example: '💡 制定3个月的Java学习计划',
    categories: ['🏷️ 路径规划', '🏷️ 动态调整', '🏷️ 资源推荐'],
    examples: [
      { cat: '路径规划', text: '制定3个月的Java学习计划，从入门到进阶' },
      { cat: '路径规划', text: '规划Python数据分析完整学习路径' },
      { cat: '动态调整', text: '根据我当前的进度，动态调整学习计划' },
      { cat: '资源推荐', text: '推荐适合初学者的机器学习入门资源' },
      { cat: '路径规划', text: '制定从零开始学前端开发的6个月路线图' }
    ]
  },
  {
    id: 'tutor', name: '答疑Agent', icon: '💬',
    role: '苏格拉底引导 · RAG检索 · 知识解答',
    tools: ['全域学术检索', '学术翻译'], avgTime: '2.8s',
    example: '💡 解释一下什么是闭包？',
    categories: ['🏷️ 概念解释', '🏷️ 引导式教学', '🏷️ 代码答疑'],
    examples: [
      { cat: '概念解释', text: '解释一下什么是闭包？用通俗的语言说明' },
      { cat: '概念解释', text: '帮我理解Python装饰器的工作原理' },
      { cat: '引导式教学', text: '用苏格拉底式引导法教我理解排序算法' },
      { cat: '代码答疑', text: '这段代码为什么会报错？帮我排查问题' },
      { cat: '概念解释', text: '讲清楚RESTful API的设计原则' }
    ]
  },
  {
    id: 'reporter', name: '报告Agent', icon: '📊',
    role: '学情分析 · 报告生成 · PDF导出',
    tools: ['深度文献解析', '全域学术检索'], avgTime: '4.2s',
    example: '💡 生成我本月的学习报告',
    categories: ['🏷️ 周报', '🏷️ 月报', '🏷️ 分析建议'],
    examples: [
      { cat: '周报', text: '生成本周学习报告，包含时长和完成率' },
      { cat: '月报', text: '生成我本月的学习报告，分析进步趋势' },
      { cat: '分析建议', text: '分析本周学习进度和效果，给出改进建议' },
      { cat: '月报', text: '对比上个月和这个月的学习数据变化' },
      { cat: '分析建议', text: '导出我的完整学习报告为PDF文件' }
    ]
  },
  {
    id: 'exercise', name: '习题Agent', icon: '✏️',
    role: '习题生成 · 智能批改 · 错题本',
    tools: ['智能测评出题'], avgTime: '2.5s',
    example: '💡 生成5道Python基础练习题',
    categories: ['🏷️ 习题生成', '🏷️ 智能批改', '🏷️ 错题巩固'],
    examples: [
      { cat: '习题生成', text: '生成5道Python基础练习题，包含难度标注' },
      { cat: '习题生成', text: '出10道Java面试高频算法题' },
      { cat: '智能批改', text: '批改我提交的代码作业，指出优化方向' },
      { cat: '错题巩固', text: '根据我的错题记录，生成针对性练习' },
      { cat: '习题生成', text: '生成一套SQL查询语句练习题，包含答案' }
    ]
  },
  {
    id: 'search', name: '搜索Agent', icon: '🌐',
    role: '联网搜索 · 资料检索 · 资源发现',
    tools: ['全域学术检索'], avgTime: '1.8s',
    example: '💡 搜索最新的Python教程',
    categories: ['🏷️ 全网搜索', '🏷️ 学术搜索', '🏷️ 教程搜索'],
    examples: [
      { cat: '全网搜索', text: '搜索2026年最新的Python学习教程推荐' },
      { cat: '教程搜索', text: '查找高质量的机器学习入门教程和课程' },
      { cat: '学术搜索', text: '搜索Java面试常见问题及解答思路' },
      { cat: '全网搜索', text: '查找Spring Boot 3.x 最佳实践资料' },
      { cat: '教程搜索', text: '寻找适合初学者的数据结构与算法视频教程' }
    ]
  },
  {
    id: 'knowledge', name: '知识检索Agent', icon: '📚',
    role: '文档检索 · 语义搜索 · 知识问答',
    tools: ['全域学术检索', '深度文献解析'], avgTime: '1.5s',
    example: '💡 查找Python基础相关内容',
    categories: ['🏷️ 文档检索', '🏷️ 语义搜索', '🏷️ 知识问答'],
    examples: [
      { cat: '文档检索', text: '从知识库中查找Python基础语法相关内容' },
      { cat: '文档检索', text: '搜索关于Spring Boot核心配置的文档片段' },
      { cat: '语义搜索', text: '检索「如何优化SQL查询性能」的相关知识' },
      { cat: '知识问答', text: '根据知识库回答：什么是数据库事务？' },
      { cat: '文档检索', text: '查找最近上传的所有关于机器学习的文档' }
    ]
  }
]

// ===== localStorage 持久化（仅保留执行次数统计） =====
const LS_KEYS = { STATS: 'agent_stats' }
const loadStats = () => { try { const r = localStorage.getItem(LS_KEYS.STATS); return r ? JSON.parse(r) : {} } catch { return {} } }
const saveStats = (s) => { try { localStorage.setItem(LS_KEYS.STATS, JSON.stringify(s)) } catch {} }

const agentStats = reactive(loadStats())
const executionLogs = ref([])

// 子 Agent 列表
const agents = ref(agentConfigs.map(cfg => ({ ...cfg, status: 'available', execCount: agentStats[cfg.id] || 0 })))
const subAgents = computed(() => agents.value)

// 用于调度中心的子智能体列表（排除编排Agent）
// 编排Agent是调度中枢，不应出现在任务分配列表中
const subAgentsForScheduling = computed(() => {
  return agents.value.filter(agent => {
    const id = agent.id?.toLowerCase() || ''
    const name = agent.name || ''
    return !id.includes('orchestrator') && !name.includes('编排')
  })
})

// ===== 计算属性 =====
const availableCount = computed(() => agents.value.filter(a => a.status === 'available').length)
const executingCount = computed(() => agents.value.filter(a => a.status === 'executing').length)
const isAnyExecuting = computed(() => executingCount.value > 0)
// 总执行次数 = 执行历史中的完成记录数（成功/失败），与列表同数据源，保证“计数与列表一致”
const totalExecCount = computed(() => executionLogs.value.filter(l => l.stepType === 'success' || l.stepType === 'error').length)
const displayTotalExec = computed(() => totalExecCount.value)

// ===== 执行历史视图（全部/回收站）与筛选 =====
const flowView = ref('list')
const flowKeyword = ref('')

// ===== 批量选择（仅可删除记录：已持久化的 Agent 结果 / 工具执行记录） =====
const selectedLogIds = ref(new Set())
const deletableLogs = computed(() => filteredLogs.value.filter(l => l.resultId || l.sourceType === 'tool'))
const allSelected = computed(() => deletableLogs.value.length > 0 && deletableLogs.value.every(l => selectedLogIds.value.has(l.id)))

// 面板徽章计数（与 KPI 同口径）
const successLogCount = computed(() => executionLogs.value.filter(l => l.stepType === 'success' || l.stepType === 'error').length)

// ===== 回收站 =====
const trashLogs = ref([])
const trashLoading = ref(false)
const trashError = ref(false)

// ===== 执行历史加载状态（区分“无数据”与“加载失败”） =====
const flowLoading = ref(false)
const flowError = ref(false)

// 后端状态 → 前端流日志状态（兼容 completed/failed/running 与 success/error/executing 值域）
const mapBackendStatus = (status) => {
  const s = String(status || 'completed').toLowerCase()
  if (['failed', 'error', 'failure'].includes(s)) {
    return { status: 'error', stepType: 'error', stepLabel: '❌ 失败' }
  }
  if (['running', 'pending', 'executing', 'processing'].includes(s)) {
    return { status: 'executing', stepType: 'observe', stepLabel: '⏳ 执行中' }
  }
  return { status: 'success', stepType: 'success', stepLabel: '✅ 任务完成' }
}

// 加载失败重试
const retryLoadHistory = () => { loadAllResultsFromBackend() }

// 主控 Agent 状态
const orchDotClass = computed(() => isAnyExecuting.value ? 'running' : 'idle')
const orchStatusText = computed(() => isAnyExecuting.value ? `协调中 (${executingCount.value} 个子任务)` : '在线待命')

// ===== 对话框 =====
const showTaskDialog = ref(false)
const currentAgent = ref(null)
const taskInput = ref('')
const isTaskExecuting = ref(false)

const plannerDuration = ref('1个月')
const tutorRagEnabled = ref(true)
const reporterType = ref('周报')
const exerciseDifficulty = ref('中等')
const exerciseCount = ref(5)
const searchRange = ref('全网')
const docFilter = ref('全部')

const plannerOptions = [
  { label: '1个月学习计划', value: '1个月' },
  { label: '3个月学习计划', value: '3个月' },
  { label: '6个月学习计划', value: '6个月' }
]
const reporterOptions = [
  { label: '周报', value: '周报' },
  { label: '月报', value: '月报' }
]
const difficultyOptions = [
  { label: '简单', value: '简单' },
  { label: '中等', value: '中等' },
  { label: '困难', value: '困难' }
]
const searchRangeOptions = [
  { label: '全网', value: '全网' },
  { label: '学术', value: '学术' },
  { label: '教程', value: '教程' }
]
const docFilterOptions = [
  { label: '全部', value: '全部' },
  { label: '最近上传', value: '最近上传' }
]

const openTaskDialog = (agent) => {
  currentAgent.value = agent
  taskInput.value = ''
  plannerDuration.value = '1个月'
  tutorRagEnabled.value = true
  reporterType.value = '周报'
  exerciseDifficulty.value = '中等'
  exerciseCount.value = 5
  searchRange.value = '全网'
  docFilter.value = '全部'
  showTaskDialog.value = true
}
const closeTaskDialog = () => {
  if (isTaskExecuting.value) return
  showTaskDialog.value = false
  currentAgent.value = null
  taskInput.value = ''
}
const quickDiagnosis = () => { taskInput.value = '快速诊断我的学习水平，生成完整的能力画像报告' }

const formatTime = (dateStr) => {
  const d = dateStr ? new Date(dateStr) : new Date()
  if (isNaN(d.getTime())) return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const agentColorMap = {
  diagnosis: '#00f5d4', planner: '#7b61ff', tutor: '#3a86ff',
  reporter: '#f59e0b', exercise: '#ff006e', search: '#10b981', knowledge: '#a78bfa'
}
const agentColor = (id) => agentColorMap[id] || '#8080a8'

// ===== ReAct 循环同步 =====
const startReactCycle = async () => {
  for (let i = 0; i < reactSteps.length; i++) {
    reactActiveStep.value = i
    await sleep(400 + Math.random() * 200)
  }
}
const resetReactCycle = () => {
  reactActiveStep.value = -1
  reactIteration.value = 0
}

// ===== 提交任务 =====
const submitTask = async () => {
  if (!currentAgent.value || !taskInput.value.trim()) return
  
  // 前端安全检查
  const safetyResult = securityFilter.sanitize(taskInput.value)
  if (safetyResult.action === 'BLOCK') {
    ElMessage.error(safetyResult.message || '输入包含不允许的内容')
    return
  }
  if (safetyResult.riskLevel === 'MEDIUM') {
    console.warn('[Security] 检测到潜在风险输入:', safetyResult.detectedTypes)
  }
  
  const agent = agents.value.find(a => a.id === currentAgent.value.id)
  if (!agent) return

  isTaskExecuting.value = true
  agent.status = 'executing'
  showTaskDialog.value = false
  flowExpanded.value = true
  reactIteration.value++

  let fullDescription = taskInput.value.trim()
  if (agent.id === 'exercise') fullDescription = `[${exerciseDifficulty.value}] ${fullDescription} (${exerciseCount.value}题)`
  else if (agent.id === 'search') fullDescription = `[${searchRange.value}] ${fullDescription}`
  else if (agent.id === 'knowledge') fullDescription = `[${docFilter.value}] ${fullDescription}`

  // 启动 ReAct 循环
  startReactCycle()

  // 添加提交日志
  addFlowLog(agent, 'task', '📋 任务已提交', `→ ${fullDescription}`)

  // 收集本次执行的日志
  const taskLogs = []

  // 模拟执行（思考→行动→观察→完成）
  await simulateExecution(agent, fullDescription, taskLogs)

  // === 后端持久化：写入 execution_logs 和 execution_results 表 ===
  let savedResultId = null
  const resultPayload = generateResultPayload(agent, fullDescription)
  // 规范化输出结构：{ content, fullData, raw }，content 优先取真正的任务结果
  const normalizedOutput = normalizeOutput(resultPayload)
  const singleExecId = 'exec_' + Date.now() + '_' + agent.id
  const singleSessionId = 'session_' + Date.now()
  // 转换 taskLogs 为带有 phase 的格式
  const phaseLogs = taskLogs.map((t) => ({
    phase: t.type || 'info',
    content: t.content || ''
  }))

  // 先更新本地结果缓存（确保即使后端保存失败也能查看结果）
  latestResultsCache[agent.id] = {
    agentId: agent.id,
    agentName: agent.name,
    taskDescription: fullDescription,
    status: 'completed',
    resultType: resultPayload.type || 'default',
    result: resultPayload,
    duration: null,
    createdAt: new Date().toISOString(),
    id: savedResultId
  }

  try {
    const res = await saveAgentExecution({
      agentId: agent.id,
      agentName: agent.name,
      taskDescription: fullDescription,
      sessionId: singleSessionId,
      executionId: singleExecId,
      result: resultPayload,
      duration: null,
      logs: phaseLogs
    })
    savedResultId = res?.data?.resultId || null
    // 更新本地缓存的 resultId
    latestResultsCache[agent.id].id = savedResultId
    console.debug(`[持久化] ${agent.name} 执行数据已保存到数据库, resultId=${savedResultId}`)
  } catch (err) {
    console.warn('[持久化] 保存执行数据失败:', err)
    // 不影响前端体验，静默失败
  }

  // 完成
  agent.execCount = (agent.execCount || 0) + 1
  agentStats[agent.id] = agent.execCount
  saveStats({ ...agentStats })

  addFlowLog(agent, 'success', '✅ 任务完成', `${agent.name} 成功执行：${fullDescription.length > 40 ? fullDescription.substring(0, 40) + '...' : fullDescription}`, null, { resultId: savedResultId }, normalizedOutput)
  ElMessage.success(`${agent.name} 任务执行成功`)

  agent.status = 'available'
  isTaskExecuting.value = false

  // 延迟重置 ReAct
  await sleep(800)
  resetReactCycle()
  scrollToBottom()
}

// ===== 生成执行结果载荷 =====
// 统一结构：{ type, displayTitle, summary, outputText, outputJson, data: { query, result, source, confidence }, message }
// outputText：人类可读内容（优先展示）；outputJson：结构化数据（供前端组件差异化渲染）
// data.result 与旧结构保持兼容，历史解析链路（normalizeOutput）仍可正常提取
const generateResultPayload = (agent, task) => {
  const now = new Date().toISOString()
  const query = task
  const summary = (text) => text.length > 50 ? text.substring(0, 50) + '...' : text
  const trimQuery = (t) => t.replace(/^\[[^\]]*\]\s*/, '')

  // 知识检索Agent：返回知识块列表 + 人类可读摘要
  if (agent.id === 'knowledge') {
    const q = trimQuery(task)
    const blocks = [
      {
        source: 'Python入门指南.md', title: '变量与数据类型', score: 95,
        content: 'Python 变量不需要显式声明类型，赋值即声明。\n• 整数：a = 10\n• 浮点数：b = 3.14\n• 字符串：c = "Hello"\n• 布尔值：d = True'
      },
      {
        source: 'Python核心语法.md', title: '条件判断语句', score: 88,
        content: 'if-elif-else 依次检查条件：\nif condition:\n    # 代码块\nelif condition2:\n    # 代码块\nelse:\n    # 代码块'
      },
      {
        source: 'Python常见问题.md', title: '常量定义约定', score: 76,
        content: 'Q：Python 中如何定义常量？\nA：Python 没有真正的常量，约定使用大写字母命名表示常量。'
      }
    ]
    const result = `关于「${q}」的知识库检索结果：\n\n${blocks.map((b, i) => `${i + 1}. 📄 来源：${b.source}\n${b.content.split('\n').join('\n   ')}\n   🔗 相关度：${b.score}%`).join('\n\n')}\n\n📌 建议：优先阅读「${blocks[0].source}」了解基础知识`
    return {
      type: 'knowledge',
      displayTitle: '知识库检索结果',
      summary: `检索到 ${blocks.length} 个相关知识块`,
      outputText: result,
      outputJson: { query: q, total: blocks.length, blocks, recommendation: `优先阅读「${blocks[0].source}」了解基础知识` },
      generatedAt: now,
      status: 'success',
      data: { query: q, result, source: 'Python官方文档 | 廖雪峰教程 | 《Python编程：从入门到实践》', confidence: 0.95 },
      message: '任务已成功执行'
    }
  }

  // 搜索Agent：返回搜索结果列表 + 人类可读摘要
  if (agent.id === 'search') {
    const q = trimQuery(task)
    const results = [
      { title: 'Python 3 官方教程', url: 'docs.python.org/zh-cn/3/tutorial/', source: 'docs.python.org', snippet: 'Python 官方文档，涵盖基础语法、标准库与最佳实践。', relevance: 0.92 },
      { title: 'Python基础语法入门指南', url: 'runoob.com/python3/', source: 'runoob.com', snippet: '包含变量、条件、循环、函数等基础语法详解与在线练习，适合初学者系统入门。', relevance: 0.88 },
      { title: '廖雪峰Python教程', url: 'liaoxuefeng.com/wiki/1016959663602400', source: 'liaoxuefeng.com', snippet: '通俗易懂的 Python 入门教程，附带大量实战项目。', relevance: 0.75 },
      { title: 'Real Python 教程', url: 'realpython.com', source: 'realpython.com', snippet: '高质量 Python 学习资源，覆盖进阶主题与最佳实践。', relevance: 0.62 },
      { title: 'Python 100 天从新手到大师', url: 'github.com/jackfrued/Python-100-Days', source: 'github.com', snippet: '完整的 Python 学习路线，包含 100 天的分阶段学习计划。', relevance: 0.55 }
    ]
    const result = `「${q}」联网搜索结果：\n\n${results.map((r, i) => `${i + 1}. 📄 ${r.title}\n   📎 ${r.source}\n   💡 ${r.snippet}\n   ⭐ 相关度：${r.relevance >= 0.8 ? '高' : r.relevance >= 0.6 ? '中' : '低'}`).join('\n\n')}\n\n📌 推荐：优先阅读「${results[0].title}」作为系统学习参考`
    return {
      type: 'search',
      displayTitle: '联网搜索结果',
      summary: `共找到 ${results.length} 条相关结果`,
      outputText: result,
      outputJson: { query: q, total: results.length, results, recommendation: `优先阅读「${results[0].title}」作为系统学习参考` },
      generatedAt: now,
      status: 'success',
      data: { query: q, result, source: '菜鸟教程 | Python官方文档 | 廖雪峰教程', confidence: 0.88 },
      message: '任务已成功执行'
    }
  }


  // 诊断Agent：返回能力维度评分 + 优势/薄弱点 + 改进建议
  if (agent.id === 'diagnosis') {
    const dimensions = [
      { name: 'Python基础', score: 85, level: '良好' },
      { name: '数据结构', score: 60, level: '需加强' },
      { name: '算法思维', score: 45, level: '薄弱' },
      { name: '项目实践', score: 30, level: '薄弱' }
    ]
    const strengths = [
      { name: 'Python基础语法', score: 85, desc: '掌握良好，可继续进阶' },
      { name: '数据分析基础', score: 72, desc: '具备基本数据处理能力' }
    ]
    const weaknesses = [
      { name: '面向对象编程', priority: '高' },
      { name: '算法与数据结构', priority: '高' }
    ]
    const suggestions = [
      '优先学习「Python面向对象编程」专题（预计 2 小时）',
      '完成「算法基础」练习集（预计 3 小时）',
      '建议每周完成 3 道编程题巩固'
    ]
    const result = `📊 诊断报告 - Python学习水平\n\n📈 整体水平：中级（62分）\n\n✅ 优势领域：\n${strengths.map(s => `  • ${s.name}（${s.score}%）- ${s.desc}`).join('\n')}\n\n⚠️ 薄弱环节：\n${weaknesses.map(w => `  • ${w.name} - 需加强`).join('\n')}\n\n📝 改进建议：\n${suggestions.map((s, i) => `  ${i + 1}. ${s}`).join('\n')}\n\n🔗 推荐路径：Python进阶 → 数据结构与算法`
    return {
      type: 'diagnosis',
      displayTitle: '学习水平诊断报告',
      summary: `整体水平：中级（${dimensions.reduce((a, d) => a + d.score, 0) / dimensions.length}分），发现 ${weaknesses.length} 个薄弱环节`,
      outputText: result,
      outputJson: { level: '中级', score: 62, dimensions, strengths, weaknesses, suggestions },
      generatedAt: now,
      status: 'success',
      data: { query, result, source: '能力测评题库', confidence: 0.92 },
      message: '任务已成功执行'
    }
  }

  // 规划Agent：返回阶段时间轴 + 目标/内容/任务 + 推荐资源
  if (agent.id === 'planner') {
    const phases = [
      {
        phase: 1, title: '基础入门', duration: '第1-4周',
        goals: ['掌握Java核心语法'],
        content: ['Java语言基础（变量、运算符、控制流）', '面向对象基础（类、对象、继承、多态）', '数组与集合框架'],
        tasks: ['完成 10 个基础编程练习', '搭建第一个Java项目']
      },
      {
        phase: 2, title: '进阶实践', duration: '第5-8周',
        goals: ['具备独立开发能力'],
        content: ['异常处理与日志', 'IO流与文件操作', '多线程与并发'],
        tasks: ['开发一个图书管理系统', '完成 5 个算法题']
      },
      {
        phase: 3, title: '框架与项目', duration: '第9-12周',
        goals: ['掌握主流框架'],
        content: ['Spring Boot入门', 'RESTful API开发', '数据库操作（JPA/MyBatis）'],
        tasks: ['完成一个Web项目', '部署到云服务器']
      }
    ]
    const resources = [
      { type: '视频课程', name: 'Java核心技术（30小时）' },
      { type: '练习平台', name: 'LeetCode（每周3题）' }
    ]
    const result = `${agent.name} · 学习计划（${plannerDuration.value}）：\n\n${phases.map(p => `📌 Phase ${p.phase}：${p.title}（${p.duration}）\n  🎯 目标：${p.goals.join('；')}\n  📚 学习内容：\n${p.content.map(c => `    • ${c}`).join('\n')}\n  📝 实践任务：\n${p.tasks.map(t => `    • ${t}`).join('\n')}`).join('\n\n')}\n\n🔗 推荐资源：\n${resources.map(r => `  • ${r.type}：${r.name}`).join('\n')}`
    return {
      type: 'plan',
      displayTitle: `${agent.name} · 学习计划`, 
      summary: `${plannerDuration.value}（${phases.length} 个阶段，共 12 周 / 120 小时）`,
      outputText: result,
      outputJson: { duration: plannerDuration.value, totalWeeks: 12, totalHours: 120, phases, resources },
      generatedAt: now,
      status: 'success',
      data: { query, result, source: '学习路径算法', confidence: 0.9 },
      message: '任务已成功执行'
    }
  }

  // 答疑Agent：返回结构化问答内容（人类可读 Markdown）
  if (agent.id === 'tutor') {
    const topic = trimQuery(task)
    const result = `💡 问题：${summary(topic)}\n\n📖 概念解释：\n\n该概念属于编程核心知识，基于知识库检索到的相关内容：\n\n🔍 核心要点：\n  1. 定义与原理：结合官方文档给出准确定义，并用通俗类比帮助理解\n  2. 实际应用：附 2-3 个可直接运行的代码示例\n  3. 常见误区：总结初学者容易混淆的点及最佳实践建议\n\n📝 代码示例：\n\n\`\`\`python\n# 示例代码\ndef example():\n    return "Hello, World!"\n\`\`\`\n\n📌 相关知识点：\n  • 作用域链\n  • 变量提升\n  • 内存管理\n\n📚 推荐阅读：\n  • 官方文档与经典教材相关章节\n  • 知识库中关联文档（可到「对话」页选择知识库文档后提问获取更精确解答）`
    return {
      type: 'qa',
      displayTitle: '答疑解惑',
      summary: summary(topic),
      outputText: result,
      outputJson: null,
      generatedAt: now,
      status: 'success',
      data: { query: task, result, source: '知识库 | Python官方文档', confidence: 0.85 },
      message: '任务已成功执行'
    }
  }
  // 报告Agent：返回概览指标 + 能力成长 + 学习详情 + 建议
  if (agent.id === 'reporter') {
    const metrics = [
      { label: '总学习时长', value: '42.5h', change: '+15%' },
      { label: '完成率', value: '87%', change: '' },
      { label: '平均测评分', value: '85分', change: '+5%' },
      { label: '连续学习天数', value: '7天', change: '' }
    ]
    const capabilities = [
      { name: 'Python基础', score: 85 },
      { name: '数据分析', score: 72 },
      { name: '机器学习', score: 38 },
      { name: '数据库', score: 55 },
      { name: 'Web开发', score: 48 }
    ]
    const details = [
      { date: '08-04', content: 'Python数据分析（Pandas、Matplotlib）', status: '完成', duration: '2h' },
      { date: '08-05', content: '数据清洗实战（缺失值处理、标准化）', status: '完成', duration: '1.5h' },
      { date: '08-06', content: '数据可视化报告制作', status: '完成', duration: '1h' }
    ]
    const nextPlan = [
      '机器学习入门（线性回归、逻辑回归）',
      '完成 3 个数据分析实战项目'
    ]
    const suggestions = [
      '继续保持每日 1 小时学习节奏',
      '增加算法练习（每天 1 题）',
      '建议完成「机器学习基础」测评'
    ]
    const result = `${reporterType.value}学习报告：\n\n📈 学习概览\n${metrics.map(m => `  • ${m.label}：${m.value}${m.change ? `（较上周 ${m.change}）` : ''}`).join('\n')}\n\n📊 能力成长\n${capabilities.map(c => `  • ${c.name}：${c.score}%`).join('\n')}\n\n📋 本周完成内容\n${details.map(d => `  ✅ ${d.content}（${d.duration}）`).join('\n')}\n\n🎯 下周计划\n${nextPlan.map(p => `  • ${p}`).join('\n')}\n\n📝 建议\n${suggestions.map(s => `  • ${s}`).join('\n')}`
    return {
      type: 'report',
      displayTitle: `${reporterType.value}学习报告`,
      summary: `${reporterType.value}学习报告：${metrics[0].value}学习时长，完成率 ${metrics[1].value}`,
      outputText: result,
      outputJson: { title: `${reporterType.value}学习报告`, metrics, capabilities, details, nextPlan, suggestions },
      generatedAt: now,
      status: 'success',
      data: { query, result, source: '学习行为数据', confidence: 0.93 },
      message: '任务已成功执行'
    }
  }

  // 习题Agent：返回结构化题目（题干/选项/答案/解析）
  if (agent.id === 'exercise') {
    const questions = [
      {
        question: '在Python中，下列哪个关键字用于定义一个函数？',
        options: [
          { label: 'A', text: 'class' },
          { label: 'B', text: 'def', isCorrect: true },
          { label: 'C', text: 'function' },
          { label: 'D', text: 'lambda' }
        ],
        answer: 'B',
        explanation: 'def 是 Python 定义函数的关键字；class 定义类；lambda 创建匿名函数。'
      },
      {
        question: '执行 `print(type([1, 2, 3]))` 会输出什么？',
        options: [
          { label: 'A', text: "<class 'list'>", isCorrect: true },
          { label: 'B', text: "<class 'tuple'>" },
          { label: 'C', text: "<class 'set'>" },
          { label: 'D', text: "<class 'dict'>" }
        ],
        answer: 'A',
        explanation: '[1, 2, 3] 是列表字面量，type() 返回 <class \'list\'>。'
      },
      {
        question: 'Python 中 for 循环与 while 循环的区别是什么？',
        options: [
          { label: 'A', text: 'for 用于遍历已知序列，while 适用于条件控制迭代', isCorrect: true },
          { label: 'B', text: 'while 用于遍历已知序列，for 适用于条件控制迭代' },
          { label: 'C', text: '两者完全相同，可以互换' },
          { label: 'D', text: 'for 只能用于数字循环' }
        ],
        answer: 'A',
        explanation: 'for 遍历可迭代对象（已知序列）；while 在条件满足时反复执行（未知次数迭代）。'
      }
    ]
    const result = `已生成 ${questions.length} 道${exerciseDifficulty.value}难度练习题：\n\n${questions.map((q, i) => `#${i + 1} ${q.question}\n${q.options.map(o => `  ${o.label}. ${o.text}`).join('\n')}\n\n  🔍 答案：${q.answer}（${q.explanation}）`).join('\n\n')}`
    return {
      type: 'exercise',
      displayTitle: `${agent.name}练习题（${questions.length}道）`,
      summary: `已生成 ${questions.length} 道${exerciseDifficulty.value}难度练习题`,
      outputText: result,
      outputJson: { questions },
      generatedAt: now,
      status: 'success',
      data: { query, result, source: '智能出题引擎', confidence: 0.9 },
      message: '任务已成功执行'
    }
  }
  // 默认分支：不再返回固定成功文案，输出任务执行摘要
  return {
    type: 'default',
    displayTitle: `${agent.name} 执行完成`,
    summary: `任务「${summary(task)}」已完成执行`,
    outputText: `任务「${summary(task)}」已完成执行，输出类型：${agent.name} 执行结果。`,
    outputJson: null,
    generatedAt: now,
    status: 'success',
    data: {
      query, result: `任务「${summary(task)}」已完成执行，输出类型：${agent.name} 执行结果。`,
      source: '本地执行引擎', confidence: 0.7
    },
    message: '任务已成功执行'
  }
}

// ===== 模拟执行（同时收集日志用于后端持久化） =====
const simulateExecution = async (agent, task, logCollector) => {
  const desc = task.length > 35 ? task.substring(0, 35) + '...' : task

  // 思考
  reactActiveStep.value = 0
  await sleep(600 + Math.random() * 400)
  addFlowLog(agent, 'think', '🤔 思考', `分析任务需求：「${desc}」`, logCollector)
  await sleep(800 + Math.random() * 500)

  // 行动
  reactActiveStep.value = 1
  addFlowLog(agent, 'act', '⚡ 行动', `调用「${agent.tools[0] || '关联工具'}」获取数据...`, logCollector)
  await sleep(900 + Math.random() * 500)

  // 观察
  reactActiveStep.value = 2
  addFlowLog(agent, 'observe', '👁️ 观察', '整合分析结果，校验数据完整性...', logCollector)
  await sleep(600 + Math.random() * 400)

  // 完成步骤
  reactActiveStep.value = 3
  await sleep(400)
}

// ===== 执行流日志（同步收集到 logCollector 用于后端持久化） =====
const addFlowLog = (agent, stepType, stepLabel, description, logCollector, extraData, output) => {
  const log = {
    id: 'log_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
    time: formatTime(),
    timestamp: new Date().toISOString(),
    agentName: agent.name,
    agentId: agent.id,
    agentIcon: agent.icon,
    stepType,
    stepLabel,
    description,
    status: stepType === 'success' ? 'success' : stepType === 'error' ? 'error' : 'executing',
    output: output || null,
    ...(extraData || {})
  }
  // 加入UI列表（追加到末尾，最新在底部）
  executionLogs.value.push(log)
  // 更新筛选后的日志列表
  applyFlowFilter()
  // 加入收集器（用于后端持久化）
  if (logCollector) logCollector.push({
    type: stepType,
    title: stepLabel,
    content: description,
    createdAt: log.timestamp
  })
  // 最多保留100条
  if (executionLogs.value.length > 100) executionLogs.value = executionLogs.value.slice(0, 100)
  nextTick(() => scrollToBottom())
  return log
}

const handleClearLogs = async () => {
  if (executionLogs.value.length === 0) return
  // 双模式：移至回收站（软删，可恢复）/ 永久删除（硬删，不可恢复）
  let mode = null
  try {
    await confirmAction(
      '清空后可在「回收站」中恢复；也可以选择永久删除，不可恢复。',
      '清空执行记录',
      {
        type: 'warning',
        confirmButtonText: '移至回收站',
        cancelButtonText: '永久删除',
        distinguishCancelAndClose: true
      }
    )
    mode = 'soft'
  } catch (action) {
    if (action !== 'cancel') return
    try {
      await confirmAction('永久删除所有执行记录后不可恢复，确定继续？', '永久清空确认', {
        type: 'error', confirmButtonText: '永久删除', cancelButtonText: '取消'
      })
      mode = 'hard'
    } catch { return }
  }
  try {
    await clearAllResults(mode)
    executionLogs.value = []
    filteredLogs.value = []
    selectedLogId.value = null
    selectedLogIds.value.clear()
    ElMessage.success(mode === 'hard' ? '执行记录已永久清空' : '执行记录已清空（可在回收站恢复）')
  } catch (e) {
    ElMessage.error('清空失败：' + (e.message || '请稍后重试'))
  }
}

// ===== 单条删除：二次确认，双模式（移至回收站 / 永久删除） =====
const confirmDeleteLog = (log) => {
  if (!log) return
  const isTool = log.sourceType === 'tool'
  if (!log.resultId && !log.toolRecordId) {
    ElMessage.warning('该记录尚未持久化，无法删除')
    return
  }
  if (isTool) {
    // 工具执行记录无软删除，仅支持永久删除
    confirmAction('工具执行记录仅支持永久删除，删除后不可恢复。确定继续？', '删除执行记录', {
      type: 'warning', confirmButtonText: '永久删除', cancelButtonText: '取消'
    }).then(() => doDeleteLog(log, 'hard')).catch(() => {})
    return
  }
  confirmAction(
    '「移至回收站」可在回收站中恢复；「永久删除」不可恢复。',
    '删除执行记录',
    {
      type: 'warning',
      confirmButtonText: '移至回收站',
      cancelButtonText: '永久删除',
      distinguishCancelAndClose: true
    }
  ).then(() => doDeleteLog(log, 'soft')).catch(action => {
    if (action === 'cancel') doDeleteLog(log, 'hard')
  })
}

// 按模式执行删除（Agent 记录支持 soft/hard；工具记录仅 hard）
const doDeleteLog = async (log, mode) => {
  try {
    if (log.sourceType === 'tool') {
      await deleteToolExecution(log.toolRecordId)
    } else {
      await deleteResultById(log.resultId, mode)
    }
    executionLogs.value = executionLogs.value.filter(l => l.id !== log.id)
    selectedLogIds.value.delete(log.id)
    if (selectedLogId.value === log.id) {
      selectedLogId.value = null
    }
    applyFlowFilter()
    ElMessage.success(mode === 'hard' || log.sourceType === 'tool' ? '记录已永久删除' : '已移至回收站')
  } catch (e) {
    const msg = e.message || ''
    // 记录已在别处删除（如工具页）：后端返回“不存在或无权删除”，本地同步移除脏数据
    if (msg.includes('不存在') || msg.includes('无权') || msg.includes('404')) {
      executionLogs.value = executionLogs.value.filter(l => l.id !== log.id)
      selectedLogIds.value.delete(log.id)
      if (selectedLogId.value === log.id) {
        selectedLogId.value = null
      }
      applyFlowFilter()
      ElMessage.warning('该记录已在其他页面被删除，已从列表移除')
      return
    }
    ElMessage.error('删除失败：' + (msg || '请稍后重试'))
  }
}

// ===== 批量选择与批量删除 =====
const toggleSelectLog = (log) => {
  if (selectedLogIds.value.has(log.id)) selectedLogIds.value.delete(log.id)
  else selectedLogIds.value.add(log.id)
}

const toggleSelectAll = () => {
  if (allSelected.value) selectedLogIds.value.clear()
  else deletableLogs.value.forEach(l => selectedLogIds.value.add(l.id))
}

const batchDeleteSelected = () => {
  const list = filteredLogs.value.filter(l => selectedLogIds.value.has(l.id))
  if (list.length === 0) return
  confirmAction(
    `已选择 ${list.length} 条记录。「移至回收站」可恢复（工具记录将直接永久删除）；「永久删除」不可恢复。`,
    '批量删除',
    {
      type: 'warning',
      confirmButtonText: '移至回收站',
      cancelButtonText: '永久删除',
      distinguishCancelAndClose: true
    }
  ).then(() => doBatchDelete(list, 'soft')).catch(action => {
    if (action === 'cancel') doBatchDelete(list, 'hard')
  })
}

const doBatchDelete = async (list, mode) => {
  const agentLogs = list.filter(l => l.sourceType !== 'tool' && l.resultId)
  const toolLogs = list.filter(l => l.sourceType === 'tool' && l.toolRecordId)
  try {
    if (agentLogs.length > 0) await deleteResultsBatch(agentLogs.map(l => l.resultId), mode)
    for (const t of toolLogs) await deleteToolExecution(t.toolRecordId)
    const ids = new Set(list.map(l => l.id))
    executionLogs.value = executionLogs.value.filter(l => !ids.has(l.id))
    if (selectedLogId.value && ids.has(selectedLogId.value)) {
      selectedLogId.value = null
    }
    selectedLogIds.value.clear()
    applyFlowFilter()
    ElMessage.success(`已删除 ${list.length} 条记录`)
  } catch (e) {
    ElMessage.error('批量删除失败：' + (e.message || '请稍后重试'))
  }
}

// ===== 回收站（软删除记录的管理） =====
const switchFlowView = (view) => {
  flowView.value = view
  selectedLogId.value = null
  if (view === 'trash') loadTrashLogs()
}

const loadTrashLogs = async () => {
  trashLoading.value = true
  trashError.value = false
  try {
    const res = await getTrashResults()
    const data = Array.isArray(res?.data) ? res.data : (res?.data?.records || [])
    trashLogs.value = data.map(r => {
      const cfg = agentConfigs.find(a => a.id === r.agentId)
      const desc = r.taskDescription || ''
      return {
        id: r.id,
        time: formatTime(r.createdAt),
        timestamp: r.createdAt,
        agentName: r.agentName || r.agentId,
        agentId: r.agentId,
        agentIcon: cfg?.icon || '🤖',
        stepType: 'task',
        stepLabel: '🗑️ 已删除',
        description: `${r.agentName || r.agentId}：${desc.length > 40 ? desc.substring(0, 40) + '...' : desc || '(无描述)'}`,
        status: 'error',
        resultId: r.id
      }
    })
  } catch (e) {
    console.warn('加载回收站失败:', e)
    trashLogs.value = []
    trashError.value = true
  } finally {
    trashLoading.value = false
  }
}

const restoreTrashLog = async (log) => {
  try {
    await restoreResult(log.resultId)
    trashLogs.value = trashLogs.value.filter(l => l.id !== log.id)
    ElMessage.success('已恢复到执行记录')
    loadAllResultsFromBackend()
  } catch (e) {
    ElMessage.error('恢复失败：' + (e.message || '请稍后重试'))
  }
}

const hardDeleteTrashLog = async (log) => {
  try {
    await confirmAction(`彻底删除「${log.agentName}」的这条记录后不可恢复，确定继续？`, '彻底删除', {
      type: 'error', confirmButtonText: '彻底删除', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    await deleteResultById(log.resultId, 'hard')
    trashLogs.value = trashLogs.value.filter(l => l.id !== log.id)
    ElMessage.success('已彻底删除')
  } catch (e) {
    ElMessage.error('删除失败：' + (e.message || '请稍后重试'))
  }
}

const emptyTrash = async () => {
  if (trashLogs.value.length === 0) return
  try {
    await confirmAction(`将彻底删除回收站中的 ${trashLogs.value.length} 条记录，不可恢复。确定继续？`, '清空回收站', {
      type: 'error', confirmButtonText: '彻底删除', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    await deleteResultsBatch(trashLogs.value.map(l => l.resultId), 'hard')
    trashLogs.value = []
    ElMessage.success('回收站已清空')
  } catch (e) {
    ElMessage.error('清空失败：' + (e.message || '请稍后重试'))
  }
}

const applyFlowFilter = () => {
  filteredLogs.value = executionLogs.value.filter(log => {
    const statusMatch = flowFilterStatus.value === 'all' || log.status === flowFilterStatus.value
    const agentMatch = flowFilterAgent.value === 'all'
      ? true
      : flowFilterAgent.value === 'tool'
        ? log.sourceType === 'tool'
        : log.agentId === flowFilterAgent.value
    const kw = (flowKeyword.value || '').trim().toLowerCase()
    const kwMatch = !kw || [log.agentName, log.description, log.taskDescription].some(t => (t || '').toLowerCase().includes(kw))
    return statusMatch && agentMatch && kwMatch
  })
}

const selectLog = (log) => {
  selectedLogId.value = selectedLogId.value === log.id ? null : log.id
}

const navigateLog = (direction) => {
  const idx = currentLogIndex.value
  if (direction === 'prev' && idx > 0) {
    selectedLogId.value = filteredLogs.value[idx - 1].id
  } else if (direction === 'next' && idx < filteredLogs.value.length - 1) {
    selectedLogId.value = filteredLogs.value[idx + 1].id
  }
}

// 关闭详情面板（同时重置展开状态）
const closeDetailPanel = () => {
  selectedLogId.value = null
}

// 旧版占位记录补救：保存时未含真实输出，按 agentId + 任务描述重新生成真实结果（仅用于展示）
const regenerateResultForLog = (agentId, taskDescription) => {
  try {
    const agent = agents.value.find(a => a.id === agentId)
    if (!agent) return null
    const payload = generateResultPayload(agent, taskDescription || '')
    // 仅当命中了真实业务分支（非 default 兜底）才认为可补救
    if (!payload || payload.type === 'default' || !payload.outputText) return null
    return payload
  } catch {
    return null
  }
}

const getAgentIcon = (agentId) => {
  // 注意：subAgents 是 computed，在 JS 函数内必须用 .value 解包（模板中会自动解包）
  // 加 try/catch 兜底，避免渲染函数抛错导致整个列表中断渲染
  try {
    if (agentId && agentId.startsWith('tool:')) return '🔧'
    const agent = subAgents.value.find(a => a.id === agentId)
    return agent?.icon || '🤖'
  } catch {
    return '🤖'
  }
}

// ===== 输出内容规范化与提取（优先真实任务结果，严禁固定成功文案） =====

/**
 * 规范化输出结构：{ content, fullData, raw }
 * content 优先取真正的任务结果（data.result），而不是 message 固定文案
 * 兼容：字符串 / 后端实体 output/resultContent（JSON 或纯文本） / 已有规范化结构
 */
const normalizeOutput = (payload) => {
  if (!payload) return null
  if (typeof payload === 'string') return { content: payload, fullData: null, raw: payload }
  // 已是规范化结构 { content, fullData, raw }，直接透传
  if ('content' in payload && 'fullData' in payload) return payload
  // 优先取真正的任务结果，兼容平铺 JSON（{type,title,phases,summary} 等直接业务字段）
  const result = payload.data?.result || payload.result || payload.output
      || payload.summary || payload.content || ''
  return {
    content: result || payload.message || '',
    fullData: payload.data || payload,
    raw: payload
  }
}

const scrollToBottom = () => {
  // 优先滚动执行日志列表到底部，保证最新记录可见
  if (flowListWrapRef.value) {
    flowListWrapRef.value.scrollTop = flowListWrapRef.value.scrollHeight
  }
  if (flowBodyRef.value) {
    flowBodyRef.value.scrollTop = flowBodyRef.value.scrollHeight
  }
}

const toggleOrchExpand = () => { orchExpanded.value = !orchExpanded.value }

// ===== 调度中心操作 =====
const openSchedulingCenter = () => {
  showScheduling.value = true
  todayScheduleCount.value = totalExecCount.value
}

const closeSchedulingCenter = () => {
  showScheduling.value = false
}

// 批量执行（多Agent并行）
const handleSchedulingBatch = (tasks) => {
  showScheduling.value = false
  orchExpanded.value = false
  
  // 生成本次并行执行的 sessionId
  currentSessionId.value = 'session_' + Date.now()
  
  // 展开执行流面板
  flowExpanded.value = true
  
  // 逐个启动任务执行
  tasks.forEach((task, idx) => {
    setTimeout(() => {
      const agent = agents.value.find(a => a.id === task.agentId)
      if (agent) {
        agent.status = 'executing'
        const fullDesc = task.description
        addFlowLog(agent, 'task', '📋 任务已提交', `→ ${fullDesc}`)
        
        // 模拟执行
        simulateAgentExecution(agent, fullDesc)
      }
    }, idx * 300) // 每个 Agent 延迟 300ms 启动
  })
  
  todayScheduleCount.value += tasks.length
}

// 模拟单个 Agent 执行
const simulateAgentExecution = async (agent, description) => {
  const desc = description.length > 35 ? description.substring(0, 35) + '...' : description
  
  // 生成执行标识
  const executionId = 'exec_' + Date.now() + '_' + agent.id
  const sessionId = currentSessionId.value || ('session_' + Date.now())
  const stepLogs = []
  
  // 思考
  addFlowLog(agent, 'think', '🤔 思考', `分析任务需求：「${desc}」`)
  stepLogs.push({ phase: 'think', content: `分析任务需求：「${desc}」` })
  await sleep(400 + Math.random() * 300)
  
  // 行动
  addFlowLog(agent, 'act', '⚡ 行动', `调用「${agent.tools[0] || '关联工具'}」获取数据...`)
  stepLogs.push({ phase: 'act', content: `调用「${agent.tools[0] || '关联工具'}」获取数据...` })
  await sleep(500 + Math.random() * 300)
  
  // 观察
  addFlowLog(agent, 'observe', '👁️ 观察', '整合分析结果，校验数据完整性...')
  stepLogs.push({ phase: 'observe', content: '整合分析结果，校验数据完整性...' })
  await sleep(400 + Math.random() * 200)
  
  // 生成执行结果载荷
  const result = generateResultPayload(agent, description)

  const execResult = {
    agentId: agent.id,
    agentName: agent.name,
    taskDescription: description,
    status: 'completed',
    resultType: result.type || 'default',
    result: result,
    duration: Math.floor(Math.random() * 3000) + 1000,
    createdAt: new Date().toISOString()
  }

  // 先更新本地结果缓存（确保即使后端保存失败也能查看结果）
  latestResultsCache[agent.id] = { ...execResult }

  // 先持久化到后端数据库，获取 resultId
  let savedResultId = null
  try {
    const res = await saveAgentExecution({
      agentId: agent.id,
      agentName: agent.name,
      taskDescription: description,
      sessionId: sessionId,
      executionId: executionId,
      result: result,
      duration: execResult.duration,
      logs: stepLogs
    })
    savedResultId = res?.data?.resultId || null
    // 更新本地缓存的 resultId
    latestResultsCache[agent.id].id = savedResultId
  } catch (e) {
    console.warn('保存执行结果到数据库失败:', e)
  }
  
  // 添加完成日志（携带 resultId 与任务描述，供详情面板展示用户输入）
  addFlowLog(agent, 'success', '✅ 任务完成', `${agent.name} 成功执行：${desc}`, null, { resultId: savedResultId, taskDescription: description }, result)
  stepLogs.push({ phase: 'complete', content: `${agent.name} 成功执行：${desc}` })
  
  // 更新统计
  agent.execCount = (agent.execCount || 0) + 1
  agentStats[agent.id] = agent.execCount
  saveStats({ ...agentStats })
  
  // 更新最近调度日志
  recentSchedulingLogs.value.unshift({
    time: formatTime(),
    agentName: agent.name,
    agentId: agent.id,
    description: description,
    status: 'success',
    duration: execResult.duration
  })
  
  agent.status = 'available'
  isTaskExecuting.value = false
  scrollToBottom()
}

const viewAllSchedulingLogs = () => {
  showScheduling.value = false
  flowExpanded.value = true
}

// ===== 结果查看操作 =====
const reExecuteFromResult = (result) => {
  showResultDetail.value = false
  const agent = agents.value.find(a => a.id === result.agentId)
  if (agent) {
    openTaskDialog(agent)
    nextTick(() => {
      taskInput.value = result.taskDescription || ''
    })
  }
}

// 结果缓存（从后端加载后同步使用）
const latestResultsCache = reactive({})
const allResultsFromBackend = ref([])

// 工具执行记录 → 执行流日志格式（sourceType: 'tool'，agentId 加 tool: 前缀避免与 Agent 冲突）
const toToolLog = (r) => {
  const failed = r.status === 'error' || r.status === 'failed'
  const running = r.status === 'running' || r.status === 'pending'
  const status = failed ? 'error' : running ? 'executing' : 'success'
  const stepType = failed ? 'error' : running ? 'observe' : 'success'
  const stepLabel = failed ? '❌ 失败' : running ? '⏳ 执行中' : '✅ 完成'
  let paramText = ''
  if (r.params && typeof r.params === 'object' && Object.keys(r.params).length > 0) {
    const first = Object.entries(r.params).slice(0, 2)
      .map(([k, v]) => `${k}=${typeof v === 'string' ? v : JSON.stringify(v)}`).join(', ')
    paramText = first.length > 30 ? first.substring(0, 30) + '...' : first
  }
  return {
    id: 'tool_' + r.id,
    time: formatTime(r.createdAt),
    timestamp: r.createdAt,
    agentName: r.toolName || r.toolId || '工具',
    agentId: 'tool:' + (r.toolId || 'unknown'),
    agentIcon: '🔧',
    stepType,
    stepLabel,
    description: `${r.toolName || r.toolId} 执行${paramText ? '：' + paramText : ''}`,
    status,
    output: r.result ? normalizeOutput(r.result) : null,
    resultId: null,
    sourceType: 'tool',
    toolRecordId: r.id
  }
}

// 从后端加载所有结果（同时构建任务执行流历史记录，复用同一张表保证两面板数据一致）
const loadAllResultsFromBackend = async () => {
  flowLoading.value = true
  flowError.value = false
  try {
    const res = await getAllResults()
    // 兼容不同后端返回结构：data 可能是数组，也可能是 { content } / { records } 分页包装
    const list = Array.isArray(res?.data)
      ? res.data
      : (res?.data?.content || res?.data?.records || [])
    if (res && list.length >= 0) {
      const formatted = list.map(r => ({
        agentId: r.agentId,
        agentName: r.agentName || r.agentId,
        taskDescription: r.taskDescription || '',
        status: r.status || 'completed',
        resultType: r.resultType || r.type || 'default',
        result: parseResultContent(r.resultContent || r.result, r.resultType || r.type),
        duration: r.duration || 0,
        createdAt: r.createdAt || r.created_at
      }))
      allResultsFromBackend.value = formatted
      // 更新缓存
      formatted.forEach(r => {
        if (!latestResultsCache[r.agentId]) {
          latestResultsCache[r.agentId] = r
        }
      })
      // 更新 agents.execCount（按用户隔离的后端数据）
      const execCountByAgent = {}
      list.forEach(r => {
        execCountByAgent[r.agentId] = (execCountByAgent[r.agentId] || 0) + 1
      })
      agents.value.forEach(a => {
        a.execCount = execCountByAgent[a.id] || 0
        agentStats[a.id] = a.execCount
      })
      saveStats(agentStats)
      // 构建任务执行流历史（数据库记录 → 流日志格式，全量加载保证计数与列表同源）
      // 后端已按 createdAt 倒序返回，反转成正序，最新记录在底部，与实时追加顺序一致
      const historyLogs = list.slice().reverse().map(r => {
        let output = null
        // 优先取后端保存的 output 字段，其次解析 resultContent（兼容 JSON 与纯文本）
        const rawContent = r.output || r.resultContent
        if (rawContent) {
          try { output = normalizeOutput(JSON.parse(rawContent)) } catch { output = normalizeOutput(rawContent) }
        }
        const cfg = agentConfigs.find(a => a.id === r.agentId)
        const desc = r.taskDescription || ''
        // 使用后端真实状态（completed/failed/running），避免历史记录状态失真
        const st = mapBackendStatus(r.status)
        // 提取完整的 outputText 用于详情展示
        const outputText = output?.content || (typeof rawContent === 'string' ? rawContent : '')
        return {
          id: r.id,
          time: formatTime(r.createdAt),
          timestamp: r.createdAt,
          agentName: r.agentName || r.agentId,
          agentId: r.agentId,
          agentIcon: cfg?.icon || '🤖',
          stepType: st.stepType,
          stepLabel: st.stepLabel,
          description: `${r.agentName || r.agentId} ${st.status === 'success' ? '成功执行' : st.status === 'error' ? '执行失败' : '执行中'}：${desc.length > 40 ? desc.substring(0, 40) + '...' : desc}`,
          taskDescription: desc,
          status: st.status,
          output,
          outputText,
          resultId: r.id
        }
      })
      // 合并工具执行记录（统一入口：Agent 执行 + 工具执行）
      let toolLogs = []
      try {
        const toolRes = await getToolExecutionHistory(0, 200)
        const records = toolRes?.data?.records || []
        toolLogs = records.map(toToolLog)
      } catch (e) {
        console.warn('加载工具执行记录失败:', e)
      }
      // 合并 Agent + 工具记录，执行中置顶，其余按时间倒序排列（全量展示，保证计数与列表一致）
      const mergedHistory = [...historyLogs, ...toolLogs]
        .sort((a, b) => {
          // 执行中的任务优先置顶
          const aRunning = a.status === 'executing'
          const bRunning = b.status === 'executing'
          if (aRunning && !bRunning) return -1
          if (!aRunning && bRunning) return 1
          // 按时间倒序（最新在前）
          return String(b.timestamp || '').localeCompare(String(a.timestamp || ''))
        })
      // 先移除旧的后端记录（按 resultId/toolRecordId 识别），避免重复加载时累积重复，保留本地实时日志
      executionLogs.value = executionLogs.value.filter(l => !l.resultId && !l.toolRecordId)
      executionLogs.value = [...mergedHistory, ...executionLogs.value]
      // 计数与列表一致性告警：后端返回了数据但列表未渲染时提示排查
      if (executionLogs.value.length === 0 && list.length > 0) {
        console.warn('执行历史数据加载异常：后端返回 ' + list.length + ' 条记录但列表为空，请检查渲染逻辑', list[0])
      }
      // 更新 recentSchedulingLogs（按用户隔离的后端数据）
      recentSchedulingLogs.value = list.slice(0, 20).map(r => {
        return {
          time: formatTime(r.createdAt),
          agentName: r.agentName || r.agentId,
          agentId: r.agentId,
          description: r.taskDescription || '',
          status: r.status === 'completed' ? 'success' : 'running',
          duration: r.duration
        }
      })
      applyFlowFilter()
      return formatted
    }
  } catch (e) {
    console.warn('从后端加载结果失败:', e)
    flowError.value = true
  } finally {
    flowLoading.value = false
  }
  return allResultsFromBackend.value
}

// 同步获取最新结果（从缓存）
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

// ===== 轮询实时更新执行状态 =====
let pollInterval = null
const startPolling = () => {
  if (pollInterval) return
  pollInterval = setInterval(() => {
    // 仅在有执行中的任务时才需要轮询更新状态
    if (isAnyExecuting.value) {
      loadAllResultsFromBackend()
    }
  }, 3000) // 每3秒轮询一次
}
const stopPolling = () => {
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
}

// ===== 生命周期 =====
onMounted(() => {
  generateParticles()
  loadAllResultsFromBackend()
  startPolling()
  // 从工具中心「查看全部执行历史」跳转：展开并滚动定位到执行历史面板
  if (route.query.flow === 'history') {
    flowExpanded.value = true
    nextTick(() => {
      flowBodyRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }
  // 深链接：直接切换到回收站视图（当前面板内切换，不跳转页面）
  if (route.query.tab === 'trash') {
    switchFlowView('trash')
  }
})

onUnmounted(() => {
  // 清理未关闭的确认弹窗，防止路由切换后残留堆叠
  cleanupDialogs()
  stopPolling()
})

// 页面被 keep-alive 缓存时：切走时停止轮询，避免后台空轮询
onDeactivated(() => {
  stopPolling()
})

// 页面被 keep-alive 缓存：从其他页面（如工具页删除了记录）切回时重新加载，保证列表与后端一致
onActivated(() => {
  loadAllResultsFromBackend()
  startPolling()
  if (flowView.value === 'trash') loadTrashLogs()
})
</script>

<style lang="scss" scoped>
/* ===== 页面容器 ===== */
.agents-page {
  position: relative; min-height: 100vh; padding: 32px 40px 80px;
  background: #0a0a1a; color: #f0f0ff; overflow-x: hidden;
}

/* ===== 深空动态背景 ===== */
.bg-deep {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba(0,245,212,0.06) 0%, transparent 50%),
              radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.06) 0%, transparent 50%),
              #0a0a1a;
}
.aurora-orb { position: absolute; border-radius: 50%; filter: blur(130px); animation: floatOrb 22s ease-in-out infinite; }
.orb-cyan { width: 650px; height: 650px; top: -220px; right: -120px; background: radial-gradient(circle, rgba(0,245,212,0.12) 0%, transparent 70%); }
.orb-purple { width: 550px; height: 550px; bottom: -180px; left: -100px; background: radial-gradient(circle, rgba(123,97,255,0.1) 0%, transparent 70%); animation-delay: -8s; }
.orb-pink { width: 420px; height: 420px; top: 40%; left: 42%; background: radial-gradient(circle, rgba(255,0,110,0.06) 0%, transparent 70%); animation-delay: -15s; }
@keyframes floatOrb {
  0%,100% { transform: translate(0,0) scale(1); }
  25% { transform: translate(35px,-35px) scale(1.1); }
  50% { transform: translate(-25px,25px) scale(0.92); }
  75% { transform: translate(25px,12px) scale(1.06); }
}
.grid-overlay {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background-image: linear-gradient(rgba(0,245,212,0.025) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(0,245,212,0.025) 1px, transparent 1px);
  background-size: 64px 64px;
}
.particles-wrap { position: fixed; inset: 0; pointer-events: none; z-index: 0; overflow: hidden; }
.particle { position: absolute; border-radius: 50%; background: rgba(0,245,212,0.35); animation: particleRise linear infinite; will-change: transform; }
@keyframes particleRise {
  0% { transform: translateY(0) translateX(0); }
  25% { transform: translateY(-30vh) translateX(18px); }
  50% { transform: translateY(-55vh) translateX(-12px); }
  75% { transform: translateY(-80vh) translateX(8px); }
  100% { transform: translateY(-100vh) translateX(0); }
}

/* ===== 顶部标题 ===== */
.page-header { position: relative; z-index: 1; margin-bottom: 24px; animation: slideUp 0.6s ease both; }
.header-row { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 4px; }
.header-left { flex: 1; }
.page-title {
  display: flex; align-items: center; gap: 12px; margin: 0 0 10px;
  font-size: 24px; font-weight: 600;
  color: #ffffff;
  text-shadow: 0 0 40px rgba(124, 107, 245, 0.1);
  line-height: 1.2;
}
.title-glyph { font-size: 1.9rem; filter: drop-shadow(0 0 12px rgba(124, 107, 245, 0.45)); }
.page-subtitle { margin: 0; font-size: 0.88rem; color: var(--text-placeholder); line-height: 1.6; }
.stat-ok { color: #10b981; }
.stat-run { color: #f59e0b; }
.stat-total { color: #a78bfa; }
.header-kpis { display: flex; gap: 14px; flex-shrink: 0; }
.kpi {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 12px 18px; background: rgba(100,100,180,0.04);
  backdrop-filter: blur(12px); border: 1px solid rgba(100,100,180,0.08);
  border-radius: 14px; min-width: 68px;
}
.kpi-num { font-size: 1.35rem; font-weight: 700; font-family: 'JetBrains Mono', monospace; color: #f0f0ff; }
.kpi-num.kpi-ok { color: #10b981; text-shadow: 0 0 12px rgba(16,185,129,0.3); }
.kpi-num.kpi-run { color: #f59e0b; text-shadow: 0 0 12px rgba(245,158,11,0.3); }
.kpi-num.kpi-accent { color: #00f5d4; text-shadow: 0 0 12px rgba(0,245,212,0.3); }
.kpi-label { font-size: 0.68rem; color: var(--text-sub); letter-spacing: 0.03em; }

/* ===== 通用 Section 标签 ===== */
.section-label {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--title-section);
  padding-left: 12px;
  margin-bottom: 14px;
  letter-spacing: 0.04em;
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 14px;
    border-radius: 2px;
    background: linear-gradient(180deg, #7c6bf5, #5b4bd5);
    box-shadow: 0 0 8px rgba(124, 107, 245, 0.4);
  }
}
.label-dot { display: none; }

/* ===== 主控 Agent 卡片 ===== */
.orchestrator-section { position: relative; z-index: 1; margin-bottom: 28px; cursor: pointer; }
.orch-card {
  position: relative;
  background: rgba(17, 17, 39, 0.5);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(0, 245, 212, 0.12);
  border-radius: 18px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: slideUp 0.6s ease 0.08s both;
  &:hover { border-color: rgba(0,245,212,0.25); box-shadow: 0 0 40px rgba(0,245,212,0.05); }
  &.active { border-color: rgba(0,245,212,0.3); box-shadow: 0 0 30px rgba(0,245,212,0.08); }
}
.orch-glow-border {
  position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
  background: conic-gradient(from 0deg, transparent, rgba(0,245,212,0.06), transparent, rgba(123,97,255,0.06), transparent);
  animation: rotateBorder 10s linear infinite; pointer-events: none; opacity: 0.5;
}
.orch-main {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 24px;
  padding: 22px 28px;
}
.orch-left { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.orch-avatar { position: relative; flex-shrink: 0; }
.orch-pulse-ring {
  position: absolute; inset: -8px; border-radius: 50%;
  border: 2px solid rgba(0,245,212,0.2);
  animation: ringPulse 3s ease-in-out infinite;
  &.active { border-color: rgba(0,245,212,0.4); animation: ringPulse 1.5s ease-in-out infinite; }
}
.orch-status-info { h2 { margin: 0 0 4px; font-size: 1.05rem; color: #f0f0ff; } }
.orch-status-line { display: flex; align-items: center; gap: 6px; font-size: 0.78rem; }
.status-dot {
  width: 8px; height: 8px; border-radius: 50%;
  &.idle { background: #10b981; box-shadow: 0 0 10px rgba(16,185,129,0.5); animation: pulseDot 2.5s ease-in-out infinite; }
  &.running { background: #f59e0b; animation: pulseDot 1s ease-in-out infinite; box-shadow: 0 0 10px rgba(245,158,11,0.5); }
}
.status-text { color: var(--text-sub); }

/* ReAct 循环 */
.orch-react { flex: 1; min-width: 0; }
.react-title { display: flex; align-items: center; gap: 8px; font-size: 0.8rem; color: var(--text-sub); margin-bottom: 14px; }
.react-pulse {
  width: 6px; height: 6px; border-radius: 50%; background: #606090;
  &.active { background: #00f5d4; animation: pulseDot 1.2s ease-in-out infinite; box-shadow: 0 0 8px rgba(0,245,212,0.5); }
}
.react-badge {
  font-size: 0.6rem; padding: 1px 8px; border-radius: 6px;
  background: rgba(245,158,11,0.12); color: #f59e0b; font-weight: 600;
  animation: pulseBadge 1.5s ease-in-out infinite;
}
.react-steps { display: flex; gap: 0; position: relative; }
.react-step {
  flex: 1; display: flex; align-items: flex-start; gap: 10px;
  position: relative; padding: 0 4px;
  &.idle { .step-icon { opacity: 0.35; } .step-name, .step-desc { opacity: 0.3; } }
  &.completed { .step-icon-ring { border-color: #10b981; } .step-icon { color: #10b981; } .step-status-tag { color: #10b981; } }
  &.active { .step-icon-ring { border-color: #00f5d4; box-shadow: 0 0 16px rgba(0,245,212,0.3); } }
}
.step-connector {
  position: absolute; top: 18px; left: -60%; width: 120%; height: 2px;
  background: rgba(100,100,180,0.06);
  &.active { .connector-fill { width: 100%; } }
  &.flowing { .connector-fill { width: 100%; background: linear-gradient(90deg, #00f5d4, #7b61ff); } }
}
.connector-fill { height: 100%; background: rgba(0,245,212,0.2); transition: width 0.6s; width: 0; border-radius: 2px; }
.step-icon-wrap {
  position: relative; flex-shrink: 0; width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  &.pulse { animation: stepPulse 1.5s ease-in-out infinite; }
}
.step-icon-ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 2px solid rgba(100,100,180,0.1);
  transition: all 0.4s;
}
.step-icon { font-size: 1.1rem; z-index: 1; position: relative; }
.step-ripple {
  position: absolute; inset: -8px; border-radius: 50%;
  border: 1.5px solid #00f5d4; opacity: 0;
  animation: rippleExpand 1.8s ease-out infinite;
}
.step-body { flex: 1; min-width: 0; }
.step-name { font-size: 0.78rem; font-weight: 600; color: #d8dce8; margin-bottom: 2px; }
.step-desc { font-size: 0.68rem; color: var(--text-sub); margin-bottom: 2px; }
.step-status-tag { font-size: 0.6rem; color: var(--text-sub); font-family: 'JetBrains Mono', monospace; }
.react-iter {
  position: absolute; right: 28px; top: 50%; transform: translateY(-50%);
  display: flex; align-items: center; gap: 6px;
}
.iter-lbl { font-size: 0.68rem; color: var(--text-sub); }
.iter-num { font-size: 1.2rem; font-weight: 700; font-family: 'JetBrains Mono', monospace; color: #00f5d4; animation: iterBump 0.3s ease; }

.orch-right { flex-shrink: 0; }
.orch-expand-hint {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  color: var(--text-sub); font-size: 0.68rem; transition: color 0.2s;
  svg { transition: transform 0.3s; &.rotated { transform: rotate(180deg); } }
  &:hover { color: #00f5d4; }
}

/* 展开详情 */
.orch-detail {
  border-top: 1px solid rgba(100,100,180,0.05); padding: 16px 28px;
  animation: slideDown 0.3s ease;
}
.detail-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.detail-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 12px; background: rgba(100,100,180,0.03); border-radius: 10px;
}
.detail-icon { font-size: 1.2rem; }
.detail-label { font-size: 0.68rem; color: var(--text-sub); }
.detail-value { font-size: 0.78rem; font-weight: 600; color: #d8dce8; }

.orch-sched-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; margin-top: 14px;
  padding: 10px 0;
  background: var(--btn-gradient);
  border: none;
  border-radius: 10px;
  color: #ffffff;
  font-size: 0.82rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(124, 107, 245, 0.4);
  }
  svg { transition: transform 0.3s; }
  &:hover svg { transform: translateX(4px); }
}

/* ===== 子 Agent 网格 ===== */
.agents-section { position: relative; z-index: 1; margin-bottom: 24px; }
.agents-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; min-height: 160px; }

/* ===== 子 Agent 卡片 ===== */
.agent-card {
  position: relative; display: flex; flex-direction: column;
  padding: 22px 18px 18px;
  background: rgba(100,100,180,0.04); backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.08);
  border-radius: 14px; cursor: default;
  transition: all 0.4s cubic-bezier(0.4,0,0.2,1);
  animation: cardAppear 0.55s ease both; overflow: hidden;
  min-height: 210px;

  .card-glow-border {
    position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
    background: conic-gradient(from 0deg, transparent, rgba(0,245,212,0.04), transparent, rgba(123,97,255,0.04), transparent);
    animation: rotateBorder 8s linear infinite; pointer-events: none; opacity: 0; transition: opacity 0.4s;
  }
  &:hover {
    transform: translateY(-5px); border-color: rgba(0,245,212,0.2);
    box-shadow: 0 16px 48px rgba(0,0,0,0.3), 0 0 30px rgba(0,245,212,0.04);
    .card-glow-border { opacity: 1; }
    .exec-btn { opacity: 1; }
  }
  &.is-executing {
    border-color: rgba(245,158,11,0.3);
    animation: cardAppear 0.55s ease both, executeGlow 2s ease-in-out infinite;
  }
}
.card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.card-icon-wrap {
  position: relative; width: 44px; height: 44px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 12px; background: rgba(0,245,212,0.05);
  &.executing { background: rgba(245,158,11,0.08); }
}
.card-icon { font-size: 1.3rem; z-index: 1; }
.icon-status-dot {
  position: absolute; width: 7px; height: 7px; border-radius: 50%;
  top: -1px; right: -1px;
  &.available { background: #10b981; box-shadow: 0 0 6px rgba(16,185,129,0.4); }
  &.executing { background: #f59e0b; animation: pulseDot 1s ease-in-out infinite; box-shadow: 0 0 6px rgba(245,158,11,0.4); }
}
.card-name-area { flex: 1; min-width: 0; }
.card-name { margin: 0 0 4px; font-size: 0.92rem; font-weight: 600; color: #f0f0ff; line-height: 1.3; }
.card-role {
  margin: 0; font-size: 0.7rem; color: var(--text-sub);
  line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2;
  -webkit-box-orient: vertical; line-clamp: 2; overflow: hidden;
}

.card-meta { display: flex; gap: 12px; font-size: 0.7rem; color: var(--text-sub); margin-bottom: 10px; }
.meta-stat { display: flex; align-items: center; gap: 3px; }

.card-tools { display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 10px; min-height: 22px; }
.tool-tag {
  font-size: 0.62rem; padding: 3px 8px;
  background: rgba(0,245,212,0.07); color: #2dd4bf;
  border-radius: 5px; font-family: 'JetBrains Mono', monospace; white-space: nowrap;
}

/* 卡片示例预览 */
.card-example {
  margin-bottom: 10px; min-height: 18px;
  display: flex; align-items: center;
  .example-text {
    font-size: 0.64rem; color: var(--text-sub);
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    cursor: help; transition: color 0.2s;
    &:hover { color: #b8c0d8; }
  }
}
.card-actions { margin-top: auto; }
.exec-btn {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  width: 100%; padding: 9px 0; border: none; border-radius: 8px;
  background: var(--btn-gradient); color: #ffffff;
  font-weight: 600; font-size: 0.78rem; font-family: inherit;
  cursor: pointer; opacity: 0.9;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  &:hover:not(:disabled) { opacity: 1; transform: translateY(-2px); box-shadow: 0 4px 16px rgba(124,107,245,0.4); }
  &.running {
    opacity: 1; background: linear-gradient(135deg, #f59e0b, #f97316); color: #fff;
    animation: pulseBtn 1.5s ease-in-out infinite;
  }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.exec-ico { font-size: 0.75rem; }
.btn-spinner-mini {
  width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.2); border-top-color: #f59e0b;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}

/* ===== 任务执行流面板 ===== */
.flow-panel {
  position: relative; z-index: 1;
  background: rgba(100,100,180,0.03); border: 1px solid rgba(100,100,180,0.08);
  border-radius: 16px; overflow: hidden;
  animation: slideUp 0.6s ease 0.35s both;
  &.has-active { border-color: rgba(0,245,212,0.08); }
}
.flow-head {
  display: flex; align-items: center; justify-content: space-between;
  flex-wrap: wrap; gap: 8px 12px;
  padding: 14px 20px; cursor: pointer; transition: background 0.2s; user-select: none;
  &:hover { background: rgba(0,245,212,0.025); }
}
.flow-head-left { display: flex; align-items: center; gap: 10px; }
.flow-indicator {
  width: 7px; height: 7px; border-radius: 50%; background: #606090;
  transition: all 0.3s;
  &.active { background: #00f5d4; box-shadow: 0 0 10px rgba(0,245,212,0.5); animation: pulseDot 1.2s ease-in-out infinite; }
}
.flow-title { font-size: 0.88rem; font-weight: 600; color: #f0f0ff; }
.flow-badge {
  font-size: 0.65rem; padding: 2px 7px; background: rgba(0,245,212,0.1);
  color: #00f5d4; border-radius: 9px; font-family: 'JetBrains Mono', monospace; font-weight: 600;
}
.flow-live {
  font-size: 0.6rem; color: #f59e0b; font-weight: 600;
  animation: pulseBadge 1.2s ease-in-out infinite;
}
.flow-head-right { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.flow-filter-select {
  height: 32px; padding: 0 8px;
  background: rgba(30, 38, 56, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 6px;
  color: #c0c0e0;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
  &:hover { border-color: rgba(0, 245, 212, 0.3); }
  &:focus { border-color: rgba(0, 245, 212, 0.4); box-shadow: 0 0 0 2px rgba(0, 245, 212, 0.08); }
  option { background: #1a1a2e; color: #e0e0f0; }
}
/* 搜索框 */
.flow-search {
  width: 140px; height: 32px; padding: 0 10px;
  background: rgba(30, 38, 56, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 6px; color: #c0c0e0; font-size: 12px;
  font-family: inherit; outline: none; transition: all 0.2s;
  &::placeholder { color: #606090; }
  &:focus { border-color: rgba(0, 245, 212, 0.4); box-shadow: 0 0 0 2px rgba(0, 245, 212, 0.08); }
}
/* 视图切换 tabs */
.flow-tabs { display: flex; align-items: center; gap: 4px; margin-left: 4px; }
.flow-tab {
  display: flex; align-items: center; gap: 6px;
  height: 32px; padding: 0 14px; border-radius: 6px;
  font-size: 0.78rem; font-weight: 500;
  color: #8080a8; cursor: pointer; transition: all 0.2s; user-select: none;
  border: 1px solid transparent; white-space: nowrap;
  &:hover { color: #c0c0e0; background: rgba(100,100,180,0.08); }
  &.active { color: #00f5d4; background: rgba(0,245,212,0.08); border-color: rgba(0,245,212,0.2); }
}
.flow-badge.trash { background: rgba(255,123,123,0.1); color: #ff7b7b; }
/* 批量删除按钮（文本按钮） */
.flow-btn.batch {
  width: auto; height: 32px; padding: 0 14px;
  color: #00f5d4; border-color: rgba(0, 245, 212, 0.3);
  font-size: 12px; font-weight: 600; white-space: nowrap;
  &:hover:not(:disabled) { background: rgba(0, 245, 212, 0.1); }
}
.flow-btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border: 1px solid #6a6080;
  border-radius: 6px; background: rgba(30, 38, 56, 0.6);
  color: #c8c0d8; cursor: pointer; transition: all 0.2s; flex-shrink: 0;
  &:hover:not(:disabled) { color: #e8e6f0; border-color: var(--border-ctrl-hover); background: rgba(124, 107, 245, 0.1); }
  &.danger {
    color: #ff8080;
    &:hover:not(:disabled) { color: #EF4444; border-color: rgba(239, 68, 68, 0.4); background: rgba(239, 68, 68, 0.1); }
  }
  /* 文本按钮（如清空回收站） */
  &.text { width: auto; padding: 0 16px; font-size: 13px; font-weight: 500; white-space: nowrap; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.flow-arrow { font-size: 1rem; color: var(--text-sub); transition: transform 0.3s; display: inline-block; &.open { transform: rotate(180deg); } }

.flow-body {
  display: flex; max-height: min(60vh, 620px); border-top: 1px solid rgba(100,100,180,0.06);
}
.flow-list-wrap {
  flex: 1; min-width: 0; overflow-y: auto; max-height: min(60vh, 620px);
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,245,212,0.12); border-radius: 2px; }
}
.flow-list { padding: 0; }
/* 列表工具条（全选） */
.flow-list-toolbar {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 20px; border-bottom: 1px solid rgba(100,100,180,0.05);
  font-size: 0.7rem; color: #8080a8;
  position: sticky; top: 0; z-index: 2;
  background: rgba(18, 18, 42, 0.92); backdrop-filter: blur(6px);
}
.flow-list-count { color: #606090; }
/* 复选框 */
.flow-check {
  display: flex; align-items: center; gap: 4px; flex-shrink: 0; cursor: pointer;
  input[type="checkbox"] { accent-color: #00f5d4; width: 13px; height: 13px; cursor: pointer; }
  &.all { .flow-check-text { font-size: 0.68rem; color: #8080a8; } }
}
/* 类型标签（Agent / 工具） */
.flow-type-tag {
  font-size: 0.55rem; padding: 1px 5px; border-radius: 4px;
  font-weight: 600; flex-shrink: 0; margin-right: 2px;
  &.tool { background: rgba(123,97,255,0.12); color: #a78bfa; }
  &.agent { background: rgba(0,245,212,0.08); color: #00f5d4; }
}
.flow-row {
  display: flex; flex-direction: column;
  padding: 8px 20px; font-size: 0.78rem;
  font-family: 'JetBrains Mono', monospace;
  border-bottom: 1px solid rgba(100,100,180,0.05);
  border-right: 3px solid transparent;
  animation: slideInRight 0.35s ease both;
  transition: background 0.2s, border-color 0.2s;
  cursor: pointer;
  &:hover { background: rgba(0,245,212,0.02); }
  &:last-child { border-bottom: none; }
  &.selected { background: rgba(0,245,212,0.04); border-right-color: #00f5d4; }

  /* 思考 → 青色标记 */
  &.think { border-left: 2px solid #00f5d4; .flow-step-badge.think { background: rgba(0,245,212,0.1); color: #00f5d4; } }
  /* 行动 → 紫色标记 */
  &.act { border-left: 2px solid #7b61ff; .flow-step-badge.act { background: rgba(123,97,255,0.1); color: #7b61ff; } }
  /* 观察 → 金色标记 */
  &.observe { border-left: 2px solid #f59e0b; .flow-step-badge.observe { background: rgba(245,158,11,0.1); color: #f59e0b; } }
  /* 系统/任务 → 白色标记 */
  &.task { border-left: 2px solid #a0a0c8; .flow-step-badge.task { background: rgba(255,255,255,0.05); color: #a0a0c8; } }
  /* 成功 → 绿色标记 */
  &.success { border-left: 2px solid #10b981; .flow-step-badge.success { background: rgba(16,185,129,0.1); color: #10b981; } }
}
.flow-row-main {
  display: flex; align-items: center; gap: 10px; min-width: 0;
}
.flow-time { color: var(--text-sub); font-size: 0.68rem; min-width: 56px; flex-shrink: 0; }
.flow-line { flex: 1; display: flex; align-items: center; gap: 8px; min-width: 0; }
.flow-agent { font-weight: 500; font-size: 0.72rem; min-width: 64px; flex-shrink: 0; }
.flow-step-badge {
  font-size: 0.6rem; padding: 1px 7px; border-radius: 5px;
  font-weight: 500; flex-shrink: 0; white-space: nowrap;
}
.flow-msg { color: #b8c0d8; font-size: 0.72rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* 回收站行与操作 */
.trash-row { opacity: 0.75; }
.trash-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.flow-action-btn {
  padding: 2px 8px;
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 4px;
  background: rgba(100, 100, 180, 0.05);
  color: #8080a8;
  font-size: 10px;
  font-family: 'JetBrains Mono', monospace;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  line-height: 1.6;
  &:hover { color: #00f5d4; border-color: rgba(0, 245, 212, 0.25); background: rgba(0, 245, 212, 0.06); }
}
.flow-action-btn.restore-btn { color: #a0a0c8; &:hover { color: #00f5d4; } }
.flow-action-btn.delete-btn {
  color: #a0a0c8;
  &:hover { color: #ff7b7b; border-color: rgba(255, 123, 123, 0.35); background: rgba(255, 107, 107, 0.08); }
}
.flow-empty { text-align: center; padding: 28px 16px; color: var(--text-placeholder); font-size: 0.82rem; margin: 8px 12px; border: 1px dashed rgba(106, 112, 144, 0.35); border-radius: 10px; background: rgba(30, 38, 56, 0.25); }
.flow-empty-ico { margin-right: 6px; }
.flow-empty-main { font-size: 0.9rem; font-weight: 600; color: #c0c0d8; margin-top: 4px; }
.flow-empty-sub { font-size: 0.75rem; color: #606090; margin-top: 4px; }
.flow-retry-btn {
  display: inline-flex; align-items: center; gap: 4px;
  margin-top: 10px; padding: 6px 16px; height: 32px;
  border: 1px solid rgba(0, 245, 212, 0.3); border-radius: 6px;
  background: rgba(0, 245, 212, 0.06); color: #00f5d4;
  font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s;
  &:hover { background: rgba(0, 245, 212, 0.12); border-color: rgba(0, 245, 212, 0.5); }
}

/* ===== 任务分配对话框 ===== */
.dialog-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.55);
  backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: center;
  z-index: 1000; animation: fadeIn 0.2s ease;
}
.task-dialog {
  width: 500px; max-width: 90vw; max-height: 80vh;
  background: rgba(17,17,39,0.96); border: 1px solid rgba(100,100,180,0.12);
  border-radius: 16px; box-shadow: 0 16px 60px rgba(0,0,0,0.5);
  animation: modalEnter 0.3s ease; display: flex; flex-direction: column;
}
.dialog-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px; border-bottom: 1px solid rgba(100,100,180,0.08); flex-shrink: 0;
}
.dialog-header-left { display: flex; align-items: center; gap: 12px; }
.dialog-agent-icon { font-size: 1.8rem; flex-shrink: 0; }
.dialog-agent-info { h3 { font-size: 14px; font-weight: 700; color: #e8e8ff; margin: 0 0 3px; } p { font-size: 12px; color: #a0a0c8; margin: 0; } }
.dialog-close {
  width: 26px; height: 26px; display: flex; align-items: center; justify-content: center;
  background: rgba(100,100,180,0.08); border: none; border-radius: 6px;
  font-size: 14px; cursor: pointer; color: #9090b8; transition: all 0.15s; flex-shrink: 0;
  &:hover:not(:disabled) { background: rgba(100,100,180,0.15); color: #e8e8ff; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.dialog-body { padding: 18px 20px; overflow-y: auto; flex: 1; }
.example-tasks { margin-bottom: 14px; }
.example-hint { font-size: 10px; color: var(--text-placeholder); font-weight: 400; margin-left: 6px; }
.example-scroll {
  display: flex; flex-direction: column; gap: 4px;
  max-height: 160px; overflow-y: auto;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,245,212,0.1); border-radius: 2px; }
}
.example-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 11px; color: #8080a8; padding: 6px 10px; border-radius: 7px;
  cursor: pointer; transition: all 0.2s; line-height: 1.4;
  &:hover { background: rgba(0,245,212,0.04); color: #c0c0e0; .example-cat-tag { border-color: rgba(0,245,212,0.2); } }
  &.active { background: rgba(0,245,212,0.08); .example-text { color: #00f5d4; } .example-cat-tag { border-color: #00f5d4; color: #00f5d4; } }
}
.example-cat-tag {
  font-size: 9px; padding: 1px 6px; border-radius: 4px;
  background: rgba(100,100,180,0.04); border: 1px solid rgba(100,100,180,0.08);
  color: #606090; flex-shrink: 0; white-space: nowrap;
  font-family: 'JetBrains Mono', monospace;
}
.example-text { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.agent-specific { margin-bottom: 14px; }
.specific-label { font-size: 11px; font-weight: 600; color: #a0a0c8; margin-bottom: 6px; }
.option-group { display: flex; gap: 6px; flex-wrap: wrap; }
.option-btn {
  padding: 5px 14px; border-radius: 7px; font-size: 11px; font-weight: 500;
  font-family: inherit; cursor: pointer; transition: all 0.2s;
  background: var(--bg-ctrl); border: 1px solid var(--border-ctrl); color: #c8d0e8;
  &:hover:not(:disabled) { border-color: var(--border-ctrl-hover); color: #00f5d4; background: rgba(124, 107, 245, 0.08); }
  &.active { background: rgba(124, 107, 245, 0.2); border-color: #7c6bf5; color: #ffffff; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.quick-btn {
  padding: 7px 18px; border-radius: 7px; font-size: 12px; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: all 0.2s;
  background: var(--btn-gradient); border: none; color: #fff;
  &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(124, 107, 245, 0.4); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.switch-row { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #c0c0e0; }
.toggle-switch {
  width: 38px; height: 20px; background: #2a3040;
  border: 1px solid var(--border-ctrl);
  border-radius: 10px; position: relative; cursor: pointer; transition: background 0.25s;
  &.active { background: #7c6bf5; border-color: #7c6bf5; box-shadow: 0 0 8px rgba(124, 107, 245, 0.35); }
}
.toggle-knob {
  width: 16px; height: 16px; border-radius: 50%; background: #9aa0b0;
  position: absolute; top: 1px; left: 1px; transition: all 0.25s;
  .active & { left: 19px; background: #ffffff; box-shadow: 0 0 6px rgba(255, 255, 255, 0.5); }
}
.count-input-wrap { display: flex; align-items: center; gap: 10px; }
.count-btn {
  width: 28px; height: 28px; border-radius: 7px;
  border: 1px solid rgba(100,100,180,0.12); background: rgba(100,100,180,0.08);
  color: #a0a0c8; font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.2s;
  &:hover:not(:disabled) { border-color: #00f5d4; color: #00f5d4; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.count-value { font-size: 14px; font-weight: 700; font-family: 'JetBrains Mono', monospace; color: #e8e8ff; min-width: 20px; text-align: center; }
.task-input-wrap { margin-top: 4px; }
.input-label { display: block; font-size: 11px; font-weight: 600; color: #a0a0c8; margin-bottom: 6px; }
.task-textarea {
  width: 100%; padding: 9px 12px; background: var(--bg-ctrl);
  border: 1px solid var(--border-ctrl); border-radius: 9px;
  font-size: 12px; color: var(--text-input); outline: none; transition: all 0.2s;
  font-family: inherit; box-sizing: border-box; resize: vertical;
  min-height: 54px; line-height: 1.5;
  &:focus { border-color: var(--border-ctrl-hover); box-shadow: 0 0 0 3px rgba(124,107,245,0.1); }
  &::placeholder { color: var(--text-placeholder); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.dialog-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 12px 20px; border-top: 1px solid rgba(100,100,180,0.08); flex-shrink: 0;
}
.dialog-btn {
  display: flex; align-items: center; gap: 5px;
  padding: 7px 18px; border-radius: 7px; font-weight: 600; font-size: 12px;
  cursor: pointer; transition: all 0.2s; border: none; font-family: inherit;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.dialog-btn-cancel { background: rgba(100,100,180,0.08); color: #c0c0e0; &:hover:not(:disabled) { background: rgba(100,100,180,0.15); } }
.dialog-btn-exec { background: var(--btn-gradient); color: #fff; &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(124,107,245,0.4); } }
.btn-spinner-small { width: 13px; height: 13px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }

/* ===== 动画关键帧 ===== */
@keyframes slideUp { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideDown { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes cardAppear { from { opacity: 0; transform: translateY(20px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes rotateBorder { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes executeGlow { 0%,100% { box-shadow: 0 0 0 rgba(245,158,11,0); } 50% { box-shadow: 0 0 20px rgba(245,158,11,0.1); } }
@keyframes pulseDot { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
@keyframes pulseBadge { 0%,100% { opacity: 0.6; } 50% { opacity: 1; } }
@keyframes pulseBtn { 0%,100% { box-shadow: 0 0 0 rgba(245,158,11,0); } 50% { box-shadow: 0 0 14px rgba(245,158,11,0.2); } }
@keyframes slideInRight { from { opacity: 0; transform: translateX(-12px); } to { opacity: 1; transform: translateX(0); } }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes modalEnter { from { opacity: 0; transform: scale(0.95) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }
@keyframes stepPulse { 0%,100% { transform: scale(1); } 50% { transform: scale(1.08); } }
@keyframes rippleExpand {
  0% { transform: scale(1); opacity: 0.5; }
  100% { transform: scale(2); opacity: 0; }
}
@keyframes ringPulse { 0%,100% { transform: scale(1); opacity: 0.5; } 50% { transform: scale(1.05); opacity: 1; } }
@keyframes iterBump { 0% { transform: scale(1.3); } 100% { transform: scale(1); } }

/* ===== TransitionGroup ===== */
.card-trans-enter-active { transition: all 0.45s cubic-bezier(0.4,0,0.2,1); }
.card-trans-leave-active { transition: all 0.3s ease; }
.card-trans-enter-from { opacity: 0; transform: translateY(20px) scale(0.94); }
.card-trans-leave-to { opacity: 0; transform: scale(0.9); }
.card-trans-move { transition: transform 0.45s cubic-bezier(0.4,0,0.2,1); }
.log-trans-enter-active { transition: all 0.35s ease; }
.log-trans-leave-active { transition: all 0.2s ease; }
.log-trans-enter-from { opacity: 0; transform: translateX(-12px); }
.log-trans-leave-to { opacity: 0; transform: translateX(12px); }

/* ===== 响应式 ===== */
@media (max-width: 1400px) { .agents-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 1024px) {
  .agents-grid { grid-template-columns: repeat(2, 1fr); }
  .header-row { flex-direction: column; gap: 16px; }
  .header-kpis { align-self: flex-start; }
  .agents-page { padding: 22px 24px 80px; }
  .orch-main { flex-direction: column; align-items: stretch; gap: 16px; padding: 18px 20px; }
  .orch-left { justify-content: center; }
  .orch-react { min-height: 100px; }
  .detail-grid { grid-template-columns: repeat(2, 1fr); }
  .react-steps { gap: 8px; }
  .step-connector { display: none; }
}
@media (max-width: 640px) {
  .agents-grid { grid-template-columns: 1fr; }
  .agents-page { padding: 16px 16px 80px; }
  .page-title { font-size: 1.5rem; }
  .header-kpis { gap: 8px; flex-wrap: wrap; }
  .kpi { min-width: 56px; padding: 8px 12px; }
  .kpi-num { font-size: 1rem; }
  .react-steps { flex-direction: column; gap: 12px; }
  .detail-grid { grid-template-columns: 1fr 1fr; }
}
</style>