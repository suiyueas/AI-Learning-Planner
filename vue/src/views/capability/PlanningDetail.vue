<template>
  <div class="detail-page">
    <!-- 顶部导航栏 -->
    <header class="detail-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <ArrowLeft :size="20" />
          <span>返回</span>
        </button>
        <h1 class="header-title">
          <FileText :size="24" class="header-icon" />
          {{ pathDetail.name || '学习规划' }}
        </h1>
      </div>
      <button class="btn-action" @click="adjustPlan">
        <Sliders :size="16" />
        调整计划
      </button>
    </header>

    <div v-loading="loading" class="detail-content" element-loading-text="加载中...">
      <!-- 骨架屏加载状态 -->
      <template v-if="initialLoading">
        <section class="info-section skeleton-section">
          <div class="skeleton-label"></div>
          <div v-for="i in 4" :key="i" class="skeleton-row">
            <div class="skeleton-line" :style="{ width: 60 + i * 10 + '%' }"></div>
          </div>
        </section>
        <section v-for="i in 2" :key="'phase-' + i" class="info-section skeleton-section">
          <div class="skeleton-label"></div>
          <div class="skeleton-row">
            <div class="skeleton-line" style="width: 100%"></div>
          </div>
          <div v-for="j in 3" :key="'task-' + j" class="skeleton-row">
            <div class="skeleton-line" style="width: 40%"></div>
          </div>
        </section>
      </template>

      <!-- 暂无学习计划 - 引导页 -->
      <template v-else-if="showEmptyState">
        <section class="info-section guidance-section">
          <div class="guidance-hero">
            <div class="guidance-icon">🧭</div>
            <h2 class="guidance-title">开启您的智能学习导航</h2>
            <p class="guidance-desc">完成一次 5 分钟的快速度测评，我将为您生成第一个专属动态计划</p>
          </div>
          <div class="guidance-steps">
            <div class="guidance-step">
              <div class="step-number">1</div>
              <div class="step-content">
                <div class="step-title">生成学习路径</div>
                <div class="step-desc">设置学习目标，获取专属路线图</div>
              </div>
            </div>
            <div class="guidance-step">
              <div class="step-number">2</div>
              <div class="step-content">
                <div class="step-title">开始学习任务</div>
                <div class="step-desc">按照计划完成任务，获取实时反馈</div>
              </div>
            </div>
            <div class="guidance-step">
              <div class="step-number">3</div>
              <div class="step-content">
                <div class="step-title">动态规划启动</div>
                <div class="step-desc">系统根据进度智能推荐下一步</div>
              </div>
            </div>
          </div>
          <button class="btn-generate" @click="goToGenerate">
            <Zap :size="16" />
            开始生成学习路径
          </button>
        </section>

        <!-- 示例推荐卡片 -->
        <section class="info-section sample-section">
          <div class="section-label">
            <Sparkles :size="16" />
            示例：智能推荐
          </div>
          <div class="sample-card">
            <div class="sample-badge">今日推荐</div>
            <h3 class="sample-title">Python 列表推导式</h3>
            <p class="sample-reason">
              <span class="reason-icon">💡</span>
              检测到您在「函数基础」章节表现良好，建议趁热打铁学习列表推导式，预计 25 分钟
            </p>
            <div class="sample-meta">
              <span class="sample-tag">中级</span>
              <span class="sample-time">⏱️ 约 25 分钟</span>
            </div>
            <button class="sample-btn" @click="goToGenerate">
              立即学习 →
            </button>
          </div>
        </section>
      </template>

      <!-- 计划详情 -->
      <template v-else>
        <!-- ==================== 今日任务看板 ==================== -->
        <section class="info-section today-dashboard">
          <div class="dashboard-header">
            <div class="dashboard-title-row">
              <span class="dashboard-icon">📅</span>
              <h2 class="dashboard-title">{{ selectedDate === todayDate || !selectedDate ? '今日学习任务' : selectedDate + ' 学习任务' }}</h2>
              <span class="dashboard-date">· {{ selectedDate || todayDate }}</span>
            </div>
            <div class="dashboard-summary">
              <span class="summary-item">
                预计 {{ dailyStore.todayPlan?.totalEstimatedMinutes || 0 }} 分钟
              </span>
              <span class="summary-divider">|</span>
              <span class="summary-item">
                已完成 {{ dailyStore.todayPlan?.totalCompleted || 0 }}/{{ dailyStore.todayPlan?.totalTasks || 0 }}
              </span>
            </div>
            <div v-if="dailyStore.todayPlan?.totalTasks > 0" class="dashboard-progress">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: dailyStore.todayCompletion + '%' }"></div>
              </div>
              <span class="progress-text">{{ dailyStore.todayCompletion }}%</span>
            </div>
            <button v-if="selectedDate && selectedDate !== todayDate" class="btn-back-today" @click="handleBackToToday">
              ← 返回今日
            </button>
          </div>

          <!-- 今日任务加载中 -->
          <div v-if="dailyStore.isLoading" class="tasks-loading">
            <div class="loading-dots"><span></span><span></span><span></span></div>
            <span>正在加载今日任务...</span>
          </div>

          <!-- 无任务 - 空状态 -->
          <div v-else-if="!dailyStore.todayPlan || !dailyStore.todayPlan.tasks || dailyStore.todayPlan.tasks.length === 0" class="tasks-empty">
            <div class="tasks-empty-icon">🎯</div>
            <p class="tasks-empty-text">系统正在为你生成第一期学习计划，请稍候...</p>
            <button class="btn-generate-tasks" @click="handleRegenerateTasks">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10" /><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" /></svg>
              生成本周计划
            </button>
          </div>

          <!-- 今日任务列表 -->
          <div v-else class="tasks-list">
            <div
              v-for="task in dailyStore.todayPlan.tasks"
              :key="task.id"
              class="task-card"
              :class="{ 'task-completed': task.status === 'completed', 'task-in-progress': task.status === 'in_progress' }"
            >
              <div class="task-check-col">
                <button
                  class="task-checkbox"
                  :class="{ checked: task.status === 'completed' }"
                  :disabled="task.status === 'completed' || dailyStore.updatingTaskId === task.id"
                  @click="handleToggleTask(task)"
                >
                  <svg v-if="task.status === 'completed'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12" /></svg>
                </button>
              </div>
              <div class="task-body">
                <div class="task-status-row">
                  <span class="task-status-badge" :class="'status-' + task.status">
                    {{ task.status === 'completed' ? '已完成' : (task.status === 'in_progress' ? '进行中' : '待开始') }}
                  </span>
                </div>
                <div class="task-title-row">
                  <span class="task-title" :class="{ 'title-done': task.status === 'completed' }">{{ task.title }}</span>
                  <span class="task-type-badge" :class="'type-' + (task.type || 'read')">
                    {{ typeLabel(task.type) }}
                  </span>
                </div>
                <div class="task-meta-row">
                  <span class="task-meta-item">⏱️ {{ task.estimatedMinutes }} 分钟</span>
                  <span v-if="task.description" class="task-meta-item" :title="task.description">📖 {{ truncate(task.description, 40) }}</span>
                </div>
              </div>
              <div class="task-action-col">
                <button
                  v-if="task.status !== 'completed'"
                  class="task-start-btn"
                  :disabled="dailyStore.updatingTaskId === task.id"
                  @click="handleToggleTask(task)"
                >
                  {{ dailyStore.updatingTaskId === task.id ? '处理中...' : (task.status === 'in_progress' ? '继续学习 →' : '开始学习 →') }}
                </button>
                <span v-else class="task-done-badge">已完成 ✓</span>
              </div>
            </div>

            <!-- 今日全部完成 -->
            <div v-if="dailyStore.allCompleted" class="all-done-banner">
              <span class="all-done-icon">🎉</span>
              <span class="all-done-text">今日任务全部完成！</span>
            </div>
          </div>
        </section>

        <!-- ==================== 本周预览 ==================== -->
        <section v-if="dailyStore.weekPreview?.days?.length > 0" class="info-section week-preview-section">
          <div class="section-label">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" /></svg>
            本周预览
            <span class="week-range">{{ dailyStore.weekPreview.weekStart }} ~ {{ dailyStore.weekPreview.weekEnd }}</span>
          </div>
          <div class="week-days-row">
            <div
              v-for="day in dailyStore.weekPreview.days"
              :key="day.date"
              class="week-day-card"
              :class="{ 'is-today': day.date === todayDate, 'has-tasks': day.totalTasks > 0, 'day-completed': day.completedTasks === day.totalTasks && day.totalTasks > 0, 'is-selected': day.date === selectedDate }"
              @click="handleSelectWeekDay(day)"
            >
              <div class="week-day-header">
                <span class="week-day-name">{{ day.dayOfWeek }}</span>
                <span class="week-day-num">{{ day.date.slice(-2) }}</span>
              </div>
              <div v-if="day.totalTasks > 0" class="week-day-body">
                <div class="week-day-progress">
                  <div class="week-day-bar">
                    <div class="week-day-fill" :style="{ width: (day.totalTasks > 0 ? (day.completedTasks / day.totalTasks * 100) : 0) + '%' }"></div>
                  </div>
                  <span class="week-day-count">{{ day.completedTasks }}/{{ day.totalTasks }}</span>
                </div>
                <span class="week-day-topic">{{ truncate(day.topic, 14) }}</span>
                <span class="week-day-minutes">{{ day.totalMinutes }}min</span>
              </div>
              <div v-else class="week-day-body empty-day">
                <span class="week-day-rest">休息</span>
              </div>
            </div>
          </div>
        </section>

        <!-- ==================== 进度对比 ==================== -->
        <section class="info-section progress-comparison">
          <div class="section-label">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" /></svg>
            进度对比
          </div>
          <div class="comparison-chart">
            <div class="comparison-bars">
              <div class="comparison-bar-group">
                <div class="bar-label">原定进度</div>
                <div class="bar-track">
                  <div class="bar-fill planned" :style="{ width: plannedProgress + '%' }"></div>
                </div>
                <div class="bar-value">{{ plannedProgress }}%</div>
              </div>
              <div class="comparison-bar-group">
                <div class="bar-label">实际进度</div>
                <div class="bar-track">
                  <div class="bar-fill actual" :style="{ width: pathDetail.progress + '%' }"></div>
                </div>
                <div class="bar-value">{{ pathDetail.progress }}%</div>
              </div>
            </div>
            <div class="comparison-rings">
              <div class="ring-item">
                <svg class="ring-svg" viewBox="0 0 100 100">
                  <circle class="ring-bg" cx="50" cy="50" r="40" />
                  <circle
class="ring-fill planned" cx="50" cy="50" r="40" 
                    :stroke-dasharray="`${2 * Math.PI * 40}`"
                    :stroke-dashoffset="`${2 * Math.PI * 40 * (1 - plannedProgress / 100)}`"
/>
                </svg>
                <div class="ring-label">原定</div>
                <div class="ring-value">{{ plannedProgress }}%</div>
              </div>
              <div class="ring-item">
                <svg class="ring-svg" viewBox="0 0 100 100">
                  <circle class="ring-bg" cx="50" cy="50" r="40" />
                  <circle
class="ring-fill actual" cx="50" cy="50" r="40"
                    :stroke-dasharray="`${2 * Math.PI * 40}`"
                    :stroke-dashoffset="`${2 * Math.PI * 40 * (1 - pathDetail.progress / 100)}`"
/>
                </svg>
                <div class="ring-label">实际</div>
                <div class="ring-value">{{ pathDetail.progress }}%</div>
              </div>
            </div>
            <!-- 进度偏差提示 -->
            <div v-if="pathDetail.progress < plannedProgress - 10" class="progress-alert behind">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></svg>
              实际进度落后于计划，建议加大每日学习量
            </div>
            <div v-else-if="pathDetail.progress >= plannedProgress && plannedProgress > 0" class="progress-alert ahead">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
              进度正常，继续保持！
            </div>
          </div>
        </section>

        <!-- ==================== 学习进度 ==================== -->
        <section class="info-section">
          <div class="section-label">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" /><rect x="8" y="2" width="8" height="4" rx="1" ry="1" /></svg>
            学习进度
          </div>
          <div class="plan-info-grid">
            <div class="plan-info-item">
              <span class="plan-info-label">总进度</span>
              <div class="plan-progress-row">
                <div class="plan-progress-track">
                  <div class="plan-progress-fill overall" :style="{ width: pathDetail.progress + '%' }"></div>
                </div>
                <span class="plan-progress-text">{{ pathDetail.progress }}%</span>
              </div>
            </div>
            <div class="plan-info-item">
              <span class="plan-info-label">已完成模块</span>
              <span class="plan-info-value">{{ pathDetail.completedModules }} / {{ pathDetail.totalModules }} 个</span>
            </div>
            <div class="plan-info-item">
              <span class="plan-info-label">已用时间</span>
              <span class="plan-info-value">{{ pathDetail.spentHours || 0 }} 小时 / 预估 {{ pathDetail.estimatedHours || 0 }} 小时</span>
            </div>
            <div class="plan-info-item">
              <span class="plan-info-label">今日已完成</span>
              <span class="plan-info-value accent">{{ dailyStore.todayPlan?.totalCompleted || 0 }} / {{ dailyStore.todayPlan?.totalTasks || 0 }} 个任务</span>
            </div>
            <div class="plan-info-item">
              <span class="plan-info-label">最后更新</span>
              <span class="plan-info-value date">{{ formatDateTime(pathDetail.updatedAt) }}</span>
            </div>
          </div>
        </section>

        <!-- ==================== 阶段进度 ==================== -->
        <section v-for="phase in pathDetail.phases" :key="phase.id" class="info-section">
          <div class="section-label">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="6" y1="3" x2="6" y2="15" /><circle cx="18" cy="6" r="3" /><circle cx="6" cy="18" r="3" /><path d="M18 9a9 9 0 0 1-9 9" /></svg>
            {{ phase.title }}
          </div>
          <div class="phase-progress-row">
            <div class="phase-progress-track">
              <div class="phase-progress-fill" :style="{ width: phase.progress + '%' }"></div>
            </div>
            <span class="phase-progress-text" :class="phase.status">{{ phase.progress }}%</span>
          </div>

          <div class="weeks-container">
            <div v-for="week in phase.weeks" :key="week.id" class="week-section">
              <div class="week-header">
                <span class="week-title">{{ week.title }}</span>
                <span class="week-progress">{{ week.progress }}%</span>
              </div>
              <div class="week-progress-track">
                <div class="week-progress-fill" :style="{ width: week.progress + '%' }"></div>
              </div>

              <div class="tasks-container">
                <div v-for="task in week.tasks" :key="task.id" class="task-item">
                  <div class="task-status-icon" :class="task.status">
                    <span v-if="task.status === 'completed'">✅</span>
                    <span v-else-if="task.status === 'in_progress'">🔄</span>
                    <span v-else>⬜</span>
                  </div>
                  <div class="task-content">
                    <span class="task-title" :class="task.status">{{ task.title }}</span>
                    <span class="task-meta">
                      预计 {{ task.estimatedHours }}h / 已学 {{ task.spentHours || 0 }}h
                    </span>
                  </div>
                  <button
                    v-if="task.status !== 'completed'"
                    class="task-complete-btn"
                    :disabled="taskCompleting === task.id"
                    @click="completeTask(task.id)"
                  >
                    {{ taskCompleting === task.id ? '处理中...' : '标记完成' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 暂无阶段数据（当路径阶段为空且没有每日任务时显示） -->
        <section v-if="!loading && !showEmptyState && pathDetail.phases?.length === 0 && (!dailyStore.todayPlan || !dailyStore.todayPlan.tasks || dailyStore.todayPlan.tasks.length === 0)" class="info-section empty-section">
          <div class="empty-content">
            <span class="empty-icon">📚</span>
            <p class="empty-text">暂无学习任务</p>
            <p class="empty-desc">开始学习后，任务将显示在这里</p>
          </div>
        </section>
      </template>
    </div>

    <AdjustPlanDialog
      v-model="showAdjustDialog"
      :plan-data="planData"
      @confirm="handleAdjustConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, FileText, Sliders,
  Zap, Sparkles
} from 'lucide-vue-next'
import AdjustPlanDialog from '@/components/AdjustPlanDialog.vue'
import { getCurrentPath, getPathProgress, completeTask as apiCompleteTask } from '@/api/learningPath'
import { useDailyTaskStore } from '@/stores/dailyTaskStore'

const router = useRouter()
const route = useRoute()
const dailyStore = useDailyTaskStore()

const showAdjustDialog = ref(false)
const showEmptyState = ref(false)
const currentPathId = ref(null)
const loading = ref(false)
const initialLoading = ref(true)
const taskCompleting = ref(null)

const pathDetail = reactive({
  id: null,
  name: '',
  description: '',
  difficulty: '',
  progress: 0,
  totalModules: 0,
  completedModules: 0,
  phases: [],
  estimatedHours: 0,
  spentHours: 0,
  learnerCount: 0,
  rating: 0,
  createdAt: '',
  updatedAt: ''
})

const planData = ref({
  id: null,
  name: '',
  progress: 0,
  completionPercentage: 0,
  currentStage: '',
  usedDays: 0,
  totalDays: 0,
  isActive: false,
  lastUpdate: ''
})

// 今日日期
const todayDate = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
})

const selectedDate = ref(null)

const handleSelectWeekDay = async (day) => {
  if (!day.totalTasks || day.totalTasks === 0) return
  selectedDate.value = day.date
  const tasksData = await dailyStore.fetchDailyTasks(currentPathId.value, day.date)
  if (tasksData && tasksData.tasks) {
    dailyStore.todayPlan = tasksData
  }
}

const handleBackToToday = async () => {
  selectedDate.value = null
  await dailyStore.fetchTodayTasks(currentPathId.value)
}

const plannedProgress = computed(() => {
  const daysUsed = planData.value.usedDays || 1
  const totalDays = planData.value.totalDays || 30
  const elapsed = daysUsed / totalDays
  return Math.min(Math.round(elapsed * 100), 100)
})

const fetchPathDetail = async () => {
  loading.value = true
  initialLoading.value = true
  showEmptyState.value = false
  try {
    const pathRes = await getCurrentPath()
    // 处理 axios 返回：response.data 即后端实体
    const pathRaw = pathRes?.data ?? pathRes

    // 无活跃路径时显示空状态
    if (!pathRaw || !pathRaw.id) {
      showEmptyState.value = true
      return
    }

    currentPathId.value = pathRaw.id
    planData.value = {
      id: pathRaw.id,
      name: pathRaw.name || '未命名学习计划',
      progress: pathRaw.completionPercentage != null ? Math.round(pathRaw.completionPercentage) : 0,
      completionPercentage: pathRaw.completionPercentage || 0,
      currentStage: '阶段一',
      usedDays: pathRaw.usedDays || 0,
      totalDays: pathRaw.totalDays || 0,
      isActive: pathRaw.isActive || false,
      lastUpdate: pathRaw.updatedAt || pathRaw.createdAt || formatDate(new Date())
    }

    // 获取完整进度数据（含阶段/周/任务）
    try {
      const progressRes = await getPathProgress(pathRaw.id)
      const progressData = progressRes?.data ?? progressRes
      if (progressData && progressData.pathId) {
        Object.assign(pathDetail, progressData)
        pathDetail.id = progressData.pathId
      } else {
        // progress 返回空但路径存在，使用节点数据生成
        pathDetail.id = pathRaw.id
        pathDetail.name = pathRaw.name || '学习规划'
        pathDetail.progress = planData.value.progress
        pathDetail.phases = generatePhasesFromNodes(pathRaw.nodes)
      }
    } catch (progressError) {
      console.warn('获取进度数据失败，使用节点数据:', progressError)
      pathDetail.id = pathRaw.id
      pathDetail.name = pathRaw.name || '学习规划'
      pathDetail.progress = planData.value.progress
      pathDetail.phases = generatePhasesFromNodes(pathRaw.nodes)
    }

    // 加载每日任务数据
    await Promise.all([
      dailyStore.fetchTodayTasks(pathRaw.id),
      dailyStore.fetchWeekPreview(pathRaw.id)
    ])
  } catch (error) {
    console.error('加载计划失败:', error)
    showEmptyState.value = true
    ElMessage.error('加载计划失败，请稍后重试')
  } finally {
    loading.value = false
    initialLoading.value = false
  }
}

const generatePhasesFromNodes = (nodesJson) => {
  if (!nodesJson) return []
  let nodes = []
  if (typeof nodesJson === 'string') {
    try { nodes = JSON.parse(nodesJson) } catch { nodes = [] }
  } else if (Array.isArray(nodesJson)) {
    nodes = nodesJson
  }
  if (nodes.length === 0) return []

  const phaseMap = {}
  nodes.forEach(node => {
    const phaseId = node.phaseId || 'phase-1'
    if (!phaseMap[phaseId]) {
      phaseMap[phaseId] = {
        id: phaseId,
        title: node.phaseTitle || '学习阶段',
        description: node.phaseDescription || '',
        progress: 0,
        status: 'pending',
        weeks: []
      }
    }
    const weekNum = node.weekNumber || 1
    let week = phaseMap[phaseId].weeks.find(w => w.weekNumber === weekNum)
    if (!week) {
      week = {
        id: `week-${weekNum}`,
        weekNumber: weekNum,
        title: `第 ${weekNum} 周`,
        progress: 0,
        tasks: []
      }
      phaseMap[phaseId].weeks.push(week)
    }
    const taskStatus = node.status || 'pending'
    week.tasks.push({
      id: node.id,
      title: node.name || node.title || '未命名任务',
      description: node.description || '',
      status: taskStatus,
      estimatedHours: node.estimatedHours || 1,
      spentHours: node.timeSpent ? node.timeSpent / 60 : 0
    })
    if (taskStatus === 'completed') {
      week.progress = Math.round(week.tasks.filter(t => t.status === 'completed').length / week.tasks.length * 100)
    }
  })

  Object.values(phaseMap).forEach(phase => {
    const totalTasks = phase.weeks.reduce((sum, w) => sum + w.tasks.length, 0)
    const completedTasks = phase.weeks.reduce((sum, w) => sum + w.tasks.filter(t => t.status === 'completed').length, 0)
    phase.progress = totalTasks > 0 ? Math.round(completedTasks / totalTasks * 100) : 0
    phase.status = phase.progress === 0 ? 'pending' : (phase.progress === 100 ? 'completed' : 'in_progress')
  })

  return Object.values(phaseMap)
}


const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const goBack = () => {
  router.push('/home')
}

const goToGenerate = () => {
  router.push('/goal-setting')
}

const adjustPlan = () => {
  if (!currentPathId.value && !pathDetail.id) {
    ElMessage.warning('当前没有可用的学习计划')
    return
  }
  showAdjustDialog.value = true
}

const handleAdjustConfirm = async (type) => {
  const id = currentPathId.value || pathDetail.id
  if (!id) {
    await fetchPathDetail()
    return
  }

  loading.value = true
  try {
    if (type === 'optimize') {
      ElMessage.success('✨ 计划已智能优化')
    } else if (type === 'reset') {
      ElMessage.success('🗑️ 计划已重置，新路径已就绪')
    } else if (type === 'switch') {
      ElMessage.success('🔄 计划切换成功')
    } else if (type === 'generate') {
      ElMessage.success('➕ 新计划已生成')
    } else if (type === 'manual') {
      return
    }
    await fetchPathDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('调整计划失败:', error)
      ElMessage.error('操作失败：' + (error?.response?.data?.message || error?.message || '请稍后重试'))
    }
  } finally {
    loading.value = false
  }
}

const completeTask = async (taskId) => {
  if (!currentPathId.value) {
    ElMessage.warning('当前没有可用的学习计划')
    return
  }
  taskCompleting.value = taskId
  try {
    await apiCompleteTask(currentPathId.value, taskId)
    ElMessage.success('任务已完成！')
    await fetchPathDetail()
  } catch (error) {
    console.error('标记任务失败:', error)
    ElMessage.error('标记失败：' + (error.message || '请稍后重试'))
  } finally {
    taskCompleting.value = null
  }
}

// ==================== 每日任务操作方法 ====================

/**
 * 切换任务完成状态
 */
const handleToggleTask = async (task) => {
  if (!currentPathId.value) {
    ElMessage.warning('当前没有可用的学习计划')
    return
  }
  try {
    const newStatus = task.status === 'completed' ? 'pending' : 'completed'
    await dailyStore.updateStatus(currentPathId.value, task.id, newStatus)
    if (newStatus === 'completed') {
      ElMessage.success('✅ 任务已完成！')
    }
  } catch (error) {
    ElMessage.error('操作失败：' + (error.message || '请稍后重试'))
  }
}

/**
 * 重新生成每日任务
 */
const handleRegenerateTasks = async () => {
  if (!currentPathId.value) {
    ElMessage.warning('当前没有可用的学习计划')
    return
  }
  try {
    await dailyStore.regenerate(currentPathId.value)
    ElMessage.success('📋 学习计划已生成！')
  } catch (error) {
    ElMessage.error('生成失败：' + (error.message || '请稍后重试'))
  }
}

/**
 * 获取任务类型标签
 */
const typeLabel = (type) => {
  const labels = {
    read: '📖 阅读',
    video: '📺 视频',
    practice: '✍️ 练习',
    review: '🔁 复习'
  }
  return labels[type] || '📖 阅读'
}

/**
 * 截断字符串
 */
const truncate = (str, maxLen) => {
  if (!str) return ''
  return str.length > maxLen ? str.slice(0, maxLen) + '...' : str
}

onMounted(async () => {
  await fetchPathDetail()

  // 从智能通知中心跳转（立即调整计划）：自动打开调整计划对话框，生成补救任务
  if (route.query.fromNotification) {
    if (currentPathId.value || pathDetail.id) {
      showAdjustDialog.value = true
      ElMessage.info('📋 检测到学习进度滞后，已为你打开计划调整，可一键生成补救任务')
    } else {
      ElMessage.warning('暂无可调整的学习计划，请先生成学习路径')
    }
  }
})
</script>

<style scoped>
@use '../styles/variables' as *;
.detail-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 80px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 48px;
  background: rgba($bg-primary, 0.8);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba($accent-secondary, 0.12);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-back:hover {
  color: $accent-primary;
  border-color: rgba($accent-primary, 0.2);
  background: rgba($accent-primary, 0.04);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.4rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.header-icon {
  color: $accent-primary;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  color: $accent-primary;
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 24px rgba($accent-primary, 0.15);
  border-color: rgba($accent-primary, 0.4);
}

.detail-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-section {
  background: rgba($bg-primary, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 16px;
  padding: 28px;
  transition: all 0.3s ease;
}

.info-section:hover {
  border-color: rgba($accent-primary, 0.15);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-secondary, 0.08);
}

.section-label svg {
  color: $accent-primary;
}

/* 计划信息 */
.plan-info-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.plan-info-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 8px 0;
}

.plan-info-label {
  font-size: 0.85rem;
  color: $text-secondary;
  min-width: 80px;
  flex-shrink: 0;
}

.plan-info-value {
  font-size: 0.95rem;
  color: $text-primary;
  font-weight: 500;
}

.plan-info-value.date {
  color: $text-secondary;
  font-size: 0.9rem;
}

.plan-progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.plan-progress-track {
  flex: 1;
  max-width: 200px;
  height: 8px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.plan-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, #10B981);
  border-radius: 4px;
  transition: width 1s ease;
}

.plan-progress-text {
  font-size: 0.9rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: #10B981;
}

/* 时间线 */
.timeline {
  padding: 20px 0 10px;
}

.timeline-bar {
  position: relative;
  height: 40px;
  margin-bottom: 16px;
}

.timeline-track {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 2px;
  transform: translateY(-50%);
}

.timeline-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, #0055FF);
  border-radius: 2px;
  transition: width 1s ease;
}

.timeline-dots {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  transform: translateY(-50%);
  padding: 0 0;
}

.timeline-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.timeline-dot.completed {
  background: rgba(16, 185, 129, 0.2);
  border: 2px solid #10B981;
}

.timeline-dot.active {
  background: rgba($accent-primary, 0.2);
  border: 2px solid $accent-primary;
  box-shadow: 0 0 12px rgba($accent-primary, 0.3);
}

.timeline-dot.future {
  background: rgba($accent-secondary, 0.06);
  border: 2px solid rgba($accent-secondary, 0.2);
}

.dot-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.timeline-dot.completed .dot-inner { background: #10B981; }
.timeline-dot.active .dot-inner { background: $accent-primary; }
.timeline-dot.future .dot-inner { background: rgba($accent-secondary, 0.3); }

.timeline-labels {
  display: flex;
  justify-content: space-between;
}

.timeline-label {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stage-name {
  font-size: 0.8rem;
  color: $text-primary;
  font-weight: 500;
}

.stage-status {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.stage-status.completed {
  color: #10B981;
  background: rgba(16, 185, 129, 0.1);
}

.stage-status.active {
  color: $accent-primary;
  background: rgba($accent-primary, 0.1);
}

.stage-status.future {
  color: $text-secondary;
  background: rgba($accent-secondary, 0.08);
}

/* 阶段详情 */
.stage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.stage-title {
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
}

.stage-period {
  font-size: 0.85rem;
  color: $accent-primary;
  padding: 4px 12px;
  background: rgba($accent-primary, 0.08);
  border-radius: 20px;
}

.stage-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stage-metric {
  padding: 16px;
  background: rgba($accent-secondary, 0.03);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
}

.stage-metric .metric-label {
  display: block;
  font-size: 0.8rem;
  color: $text-secondary;
  margin-bottom: 8px;
}

.stage-metric .metric-value {
  font-size: 0.95rem;
  color: $text-primary;
  font-weight: 500;
}

.metric-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.metric-progress-track {
  flex: 1;
  height: 6px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.metric-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, #10B981);
  border-radius: 3px;
  transition: width 1s ease;
}

.metric-progress-text {
  font-size: 0.85rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: #10B981;
}

/* 调整记录 */
.adjust-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
  position: relative;
}

.adjust-list::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: rgba($accent-secondary, 0.12);
}

.adjust-item {
  display: flex;
  gap: 16px;
  padding-left: 4px;
}

.adjust-dot {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: rgba($accent-primary, 0.15);
  border: 2px solid $accent-primary;
  flex-shrink: 0;
  margin-top: 3px;
  position: relative;
  z-index: 1;
}

.adjust-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.adjust-date {
  font-size: 0.8rem;
  color: $accent-primary;
  font-family: 'JetBrains Mono', monospace;
}

.adjust-desc {
  font-size: 0.9rem;
  color: $text-primary;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-primary, 0.06);
  border: 1px solid rgba($accent-primary, 0.12);
  border-radius: 8px;
  color: $accent-primary;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.link-btn:hover {
  background: rgba($accent-primary, 0.1);
  border-color: rgba($accent-primary, 0.25);
  transform: translateY(-1px);
}

/* ===== 引导页样式 ===== */
.guidance-section {
  padding: 40px 32px;
}

.guidance-hero {
  text-align: center;
  margin-bottom: 32px;
}

.guidance-icon {
  font-size: 3.5rem;
  margin-bottom: 16px;
}

.guidance-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 12px 0;
}

.guidance-desc {
  font-size: 0.95rem;
  color: $text-secondary;
  margin: 0;
  line-height: 1.6;
}

.guidance-steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 28px;
}

.guidance-step {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 12px;
}

.step-number {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 50%;
  color: $accent-primary;
  font-size: 0.9rem;
  font-weight: 700;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.step-desc {
  font-size: 0.82rem;
  color: $text-secondary;
  margin: 0;
}

/* ===== 示例推荐卡片 ===== */
.sample-section {
  padding: 24px 28px;
}

.sample-card {
  position: relative;
  padding: 20px 24px;
  background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.12);
  border-radius: 12px;
}

.sample-badge {
  display: inline-block;
  padding: 4px 10px;
  background: rgba($accent-primary, 0.1);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 600;
  color: $accent-primary;
  margin-bottom: 12px;
}

.sample-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 10px 0;
}

.sample-reason {
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 14px 0;
}

.reason-icon {
  margin-right: 4px;
}

.sample-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.sample-tag {
  display: inline-block;
  padding: 3px 8px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #3B82F6;
}

.sample-time {
  font-size: 0.82rem;
  color: $text-secondary;
}

.sample-btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 8px;
  color: $accent-primary;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.sample-btn:hover {
  background: linear-gradient(135deg, rgba($accent-primary, 0.25), rgba(0, 85, 255, 0.2));
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba($accent-primary, 0.15);
}

/* ===== 当前位置卡片 ===== */
.current-position-card {
  padding: 24px 28px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.06), rgba(16, 185, 129, 0.04));
  border: 1px solid rgba($accent-primary, 0.15);
}

.current-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.current-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-primary, 0.1);
  border-radius: 10px;
  color: $accent-primary;
}

.current-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.current-label {
  font-size: 0.72rem;
  color: $text-secondary;
  font-weight: 500;
}

.current-value {
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
}

.current-next {
  padding: 10px 14px;
  background: rgba($accent-secondary, 0.04);
  border-radius: 8px;
}

.next-label {
  font-size: 0.82rem;
  color: $text-secondary;
}

.next-value {
  font-size: 0.85rem;
  color: #10B981;
  font-weight: 500;
}

/* ===== 智能推荐卡片 ===== */
.recommendation-card {
  padding: 24px 28px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.06), rgba(16, 185, 129, 0.04));
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.recommendation-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.recommendation-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 8px;
  color: #F59E0B;
}

.recommendation-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: $text-primary;
}

.recommendation-content {
  padding-left: 46px;
}

.recommendation-task {
  font-size: 1.15rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 10px 0;
}

.recommendation-reason {
  font-size: 0.88rem;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 14px 0;
}

.reason-tag {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #F59E0B;
  margin-right: 8px;
}

.recommendation-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  color: $text-secondary;
}

.meta-item svg {
  color: $text-secondary;
}

.recommendation-btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(16, 185, 129, 0.1));
  border: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: 8px;
  color: #F59E0B;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.recommendation-btn:hover {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.25), rgba(16, 185, 129, 0.2));
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(245, 158, 11, 0.15);
}

/* ===== 进度对比 ===== */
.progress-comparison {
  padding: 24px 28px;
}

.comparison-chart {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.comparison-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comparison-bar-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  font-size: 0.78rem;
  color: $text-secondary;
  min-width: 60px;
}

.bar-track {
  flex: 1;
  height: 8px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.8s ease;

  &.planned {
    background: linear-gradient(90deg, $text-secondary, $text-muted);
  }

  &.actual {
    background: linear-gradient(90deg, $accent-primary, #10B981);
  }
}

.bar-value {
  font-size: 0.82rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: $text-primary;
  min-width: 40px;
  text-align: right;
}

.comparison-rings {
  display: flex;
  gap: 20px;
}

.ring-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.ring-svg {
  width: 80px;
  height: 80px;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba($accent-secondary, 0.08);
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.8s ease;

  &.planned {
    stroke: $text-secondary;
  }

  &.actual {
    stroke: $accent-primary;
  }
}

.ring-label {
  font-size: 0.72rem;
  color: $text-secondary;
}

.ring-value {
  font-size: 0.95rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  color: $text-primary;
}

@media (max-width: 768px) {
  .detail-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 12px;
  }
  .header-left { width: 100%; }
  .btn-action { width: 100%; justify-content: center; }
  .detail-content { padding: 20px; }
  .stage-metrics { grid-template-columns: 1fr; }
  .timeline-labels { gap: 4px; }
  .timeline-label .stage-name { font-size: 0.7rem; }
  .guidance-section { padding: 28px 20px; }
  .guidance-steps { gap: 12px; }
  .guidance-step { padding: 14px 16px; }
  .recommendation-content { padding-left: 0; }
  .comparison-chart { flex-direction: column; gap: 20px; }
  .comparison-rings { justify-content: center; width: 100%; }
}

/* 阶段进度 */
.phase-progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.phase-progress-track {
  flex: 1;
  height: 6px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.phase-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-purple, #a855f7);
  border-radius: 3px;
  transition: width 1s ease;
}

.phase-progress-text {
  font-size: 0.85rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: $accent-purple;
  min-width: 45px;
  text-align: right;
}

.phase-progress-text.completed { color: $accent-emerald; }
.phase-progress-text.in_progress { color: $accent-amber; }
.phase-progress-text.pending { color: $text-secondary; }

/* 周容器 */
.weeks-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.week-section {
  background: rgba($accent-secondary, 0.03);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 12px;
  padding: 16px;
}

.week-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.week-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: $text-primary;
}

.week-progress {
  font-size: 0.8rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: $accent-primary;
}

.week-progress-track {
  height: 4px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 14px;
}

.week-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, #10B981);
  border-radius: 2px;
  transition: width 1s ease;
}

/* 任务列表 */
.tasks-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: rgba($bg-primary, 0.4);
  border: 1px solid rgba($accent-secondary, 0.06);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.task-item:hover {
  border-color: rgba($accent-primary, 0.12);
  background: rgba($bg-primary, 0.6);
}

.task-status-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.task-title {
  font-size: 0.88rem;
  font-weight: 500;
  color: #d0d0f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-title.completed {
  color: $accent-emerald;
  text-decoration: line-through;
}

.task-title.in_progress {
  color: $accent-amber;
}

.task-title.pending {
  color: $text-secondary;
}

.task-meta {
  font-size: 0.75rem;
  color: $text-muted;
}

.task-complete-btn {
  padding: 6px 14px;
  background: rgba($accent-primary, 0.08);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 6px;
  color: $accent-primary;
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.task-complete-btn:hover:not(:disabled) {
  background: rgba($accent-primary, 0.15);
  border-color: rgba($accent-primary, 0.35);
  transform: translateY(-1px);
}

.task-complete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 整体进度条颜色（霓虹青） */
.plan-progress-fill.overall {
  background: linear-gradient(90deg, $accent-primary, $accent-emerald);
}

/* 空状态 */
.empty-section {
  padding: 60px 20px;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-icon {
  font-size: 3.5rem;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 0.9rem;
  color: $text-muted;
  margin: 0;
}

/* 生成计划按钮 */
.btn-generate {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding: 12px 28px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  color: $accent-primary;
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-generate:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 24px rgba($accent-primary, 0.15);
  border-color: rgba($accent-primary, 0.4);
}

/* 骨架屏 */
.skeleton-section {
  padding: 24px;
}

.skeleton-label {
  height: 18px;
  width: 120px;
  background: rgba($accent-secondary, 0.1);
  border-radius: 4px;
  margin-bottom: 20px;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

.skeleton-row {
  padding: 12px 0;
}

.skeleton-line {
  height: 14px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 4px;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

/* ===== 今日任务看板 ===== */
.today-dashboard {
  padding: 24px 28px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.06), rgba(16, 185, 129, 0.04));
  border: 1px solid rgba($accent-primary, 0.15);
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}

.dashboard-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dashboard-icon { font-size: 1.3rem; }

.dashboard-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.dashboard-date {
  font-size: 0.85rem;
  color: $text-secondary;
  font-weight: 400;
}

.dashboard-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
  color: $text-secondary;
}

.summary-divider { color: rgba($accent-secondary, 0.3); }

.summary-item { font-weight: 500; }

.dashboard-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.dashboard-progress .progress-track {
  flex: 1;
  height: 6px;
  background: rgba($accent-secondary, 0.15);
  border-radius: 3px;
  overflow: hidden;
}

.dashboard-progress .progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, #7C3AED);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.dashboard-progress .progress-text {
  font-size: 0.75rem;
  color: $accent-primary;
  font-weight: 600;
  min-width: 36px;
}

.btn-back-today {
  margin-top: 10px;
  padding: 6px 12px;
  font-size: 0.75rem;
  color: $accent-primary;
  background: rgba($accent-primary, 0.1);
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back-today:hover {
  background: rgba($accent-primary, 0.2);
  border-color: rgba($accent-primary, 0.4);
}

/* 今日任务列表 */
.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  background: rgba($bg-primary, 0.5);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 12px;
  transition: all 0.25s ease;
}

.task-card:hover {
  border-color: rgba($accent-primary, 0.15);
  background: rgba($bg-primary, 0.7);
}

.task-card.task-completed {
  opacity: 0.65;
  border-color: rgba(16, 185, 129, 0.15);
}

.task-card.task-in-progress {
  border-color: rgba(245, 158, 11, 0.2);
  background: rgba(245, 158, 11, 0.04);
}

.task-check-col {
  flex-shrink: 0;
  padding-top: 2px;
}

.task-checkbox {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid rgba($accent-secondary, 0.25);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 0;
}

.task-checkbox:hover:not(:disabled) {
  border-color: #10B981;
  background: rgba(16, 185, 129, 0.06);
}

.task-checkbox.checked {
  background: #10B981;
  border-color: #10B981;
  color: #fff;
}

.task-checkbox.checked svg { color: #fff; }

.task-checkbox:disabled { cursor: not-allowed; opacity: 0.5; }

.task-body {
  flex: 1;
  min-width: 0;
}

.task-status-row {
  margin-bottom: 6px;
}

.task-status-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 0.7rem;
  font-weight: 600;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.task-status-badge.status-pending {
  background: rgba(100, 116, 139, 0.15);
  color: $text-secondary;
  border: 1px solid rgba(100, 116, 139, 0.2);
}

.task-status-badge.status-in_progress {
  background: rgba(245, 158, 11, 0.15);
  color: #F59E0B;
  border: 1px solid rgba(245, 158, 11, 0.25);
}

.task-status-badge.status-completed {
  background: rgba(16, 185, 129, 0.15);
  color: #10B981;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.task-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.task-title {
  font-size: 0.92rem;
  font-weight: 600;
  color: #E2E8F0;
}

.task-title.title-done {
  text-decoration: line-through;
  color: $text-muted;
}

.task-type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 500;
  white-space: nowrap;
}

.task-type-badge.type-read {
  background: rgba(59, 130, 246, 0.1);
  color: #60A5FA;
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.task-type-badge.type-video {
  background: rgba(168, 85, 247, 0.1);
  color: #A78BFA;
  border: 1px solid rgba(168, 85, 247, 0.15);
}

.task-type-badge.type-practice {
  background: rgba(245, 158, 11, 0.1);
  color: #FBBF24;
  border: 1px solid rgba(245, 158, 11, 0.15);
}

.task-type-badge.type-review {
  background: rgba(16, 185, 129, 0.1);
  color: #34D399;
  border: 1px solid rgba(16, 185, 129, 0.15);
}

.task-meta-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.task-meta-item {
  font-size: 0.78rem;
  color: $text-secondary;
}

.task-action-col {
  flex-shrink: 0;
}

.task-start-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.12), rgba(0, 85, 255, 0.08));
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 8px;
  color: $accent-primary;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.task-start-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba($accent-primary, 0.2), rgba(0, 85, 255, 0.15));
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba($accent-primary, 0.1);
}

.task-start-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.task-done-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 8px;
  color: #10B981;
  font-size: 0.82rem;
  font-weight: 600;
}

.all-done-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.15);
  border-radius: 12px;
  animation: celebrate-bounce 0.5s ease;
}

@keyframes celebrate-bounce {
  0% { transform: scale(0.95); opacity: 0; }
  50% { transform: scale(1.02); }
  100% { transform: scale(1); opacity: 1; }
}

.all-done-icon { font-size: 1.5rem; }

.all-done-text {
  font-size: 1rem;
  font-weight: 700;
  color: #34D399;
}

/* 加载中的任务 */
.tasks-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 20px;
  color: $text-secondary;
  font-size: 0.9rem;
}

.loading-dots {
  display: flex;
  gap: 6px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $accent-primary;
  animation: dots-pulse 1.4s ease-in-out infinite;
}

.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dots-pulse {
  0%, 80%, 100% { transform: scale(0.4); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* 空任务状态 */
.tasks-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 40px 20px;
  text-align: center;
}

.tasks-empty-icon { font-size: 2.5rem; }

.tasks-empty-text {
  font-size: 0.9rem;
  color: $text-secondary;
  margin: 0;
}

.btn-generate-tasks {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.12), rgba(0, 85, 255, 0.08));
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 10px;
  color: $accent-primary;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-generate-tasks:hover {
  background: linear-gradient(135deg, rgba($accent-primary, 0.2), rgba(0, 85, 255, 0.15));
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba($accent-primary, 0.1);
}

/* ===== 本周预览 ===== */
.week-preview-section {
  padding: 24px 28px;
}

.week-range {
  margin-left: 8px;
  font-size: 0.78rem;
  font-weight: 400;
  color: $text-muted;
}

.week-days-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.week-day-card {
  padding: 12px 8px;
  background: rgba($bg-primary, 0.4);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
  text-align: center;
  transition: all 0.2s ease;
}

.week-day-card.is-today {
  border-color: rgba($accent-primary, 0.3);
  background: rgba($accent-primary, 0.04);
}

.week-day-card.has-tasks:hover {
  border-color: rgba($accent-primary, 0.15);
  transform: translateY(-2px);
}

.week-day-card.day-completed {
  border-color: rgba(16, 185, 129, 0.2);
  background: rgba(16, 185, 129, 0.04);
}

.week-day-card.is-selected {
  border-color: rgba(124, 58, 237, 0.5);
  background: rgba(124, 58, 237, 0.1);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.2);
}

.week-day-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  margin-bottom: 8px;
}

.week-day-name {
  font-size: 0.72rem;
  color: $text-secondary;
  font-weight: 500;
}

.is-today .week-day-name {
  color: $accent-primary;
  font-weight: 700;
}

.week-day-num {
  font-size: 0.85rem;
  font-weight: 700;
  color: #E2E8F0;
  font-family: 'JetBrains Mono', monospace;
}

.is-today .week-day-num {
  color: $accent-primary;
}

.week-day-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.week-day-body.empty-day {
  padding: 12px 0;
}

.week-day-rest {
  font-size: 0.75rem;
  color: $text-muted;
}

.week-day-progress {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.week-day-bar {
  flex: 1;
  height: 4px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 2px;
  overflow: hidden;
}

.week-day-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, #10B981);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.week-day-count {
  font-size: 0.65rem;
  font-weight: 600;
  color: $text-secondary;
  font-family: 'JetBrains Mono', monospace;
  flex-shrink: 0;
}

.week-day-topic {
  font-size: 0.72rem;
  color: #E2E8F0;
  display: -webkit-box;
  line-clamp: 1;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 100%;
}

.week-day-minutes {
  font-size: 0.65rem;
  color: $text-muted;
  font-family: 'JetBrains Mono', monospace;
}

/* 进度对比提示 */
.progress-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 500;
}

.progress-alert.behind {
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.15);
  color: #F87171;
}

.progress-alert.ahead {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.15);
  color: #34D399;
}

/* 入口颜色 */
.plan-info-value.accent {
  color: $accent-primary;
}

@media (max-width: 768px) {
  .week-days-row {
    grid-template-columns: repeat(4, 1fr);
  }
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .task-card {
    flex-direction: column;
    gap: 10px;
  }
  .task-action-col {
    width: 100%;
  }
  .task-start-btn {
    width: 100%;
    justify-content: center;
  }
  .task-done-badge {
    width: 100%;
    justify-content: center;
  }
}
</style>