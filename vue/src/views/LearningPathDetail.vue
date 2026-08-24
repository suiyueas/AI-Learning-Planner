<template>
  <div class="path-detail-page">
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
    </div>

    <!-- 返回导航 -->
    <div class="nav-bar">
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg>
        返回
      </button>
      <h1 class="nav-title">{{ pathDetail.name || '学习路径详情' }}</h1>
      <div class="nav-actions">
        <button class="icon-btn" :class="{ favorited: isFavorited }" title="收藏" @click="isFavorited = !isFavorited">
          <svg width="16" height="16" viewBox="0 0 24 24" :fill="isFavorited ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" /></svg>
        </button>
        <button class="icon-btn" title="分享" @click="handleShare">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" /></svg>
        </button>
        <div class="more-menu-wrapper">
          <button class="icon-btn" title="更多" @click="toggleMoreMenu">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1" /><circle cx="12" cy="5" r="1" /><circle cx="12" cy="19" r="1" /></svg>
          </button>
          <div v-if="showMoreMenu" class="more-menu">
            <button class="more-menu-item danger" @click="handleDeleteClick">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
              删除路线
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 路径不存在空态（无效/已删除的路径 ID） -->
    <div v-if="notFound" class="not-found-box">
      <div class="not-found-icon">🧭</div>
      <h2 class="not-found-title">路径不存在或已被删除</h2>
      <p class="not-found-desc">您访问的学习路径可能已被删除，或链接地址有误</p>
      <button class="not-found-btn" @click="goBack">返回学习路径列表</button>
    </div>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="showDeleteModal" class="delete-modal-overlay" @click.self="handleCancelDelete">
        <div class="delete-modal">
          <div class="delete-modal-header">
            <svg class="delete-modal-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <h3 class="delete-modal-title">确认删除</h3>
          </div>
          <p class="delete-modal-content">
            确定要永久删除路线「<span class="delete-modal-name">{{ pathDetail.name }}</span>」吗？此操作无法撤销。
          </p>
          <div class="delete-modal-actions">
            <button class="btn-cancel" @click="handleCancelDelete">取消</button>
            <button class="btn-confirm-delete" :disabled="isDeleting" @click="handleConfirmDelete">
              {{ isDeleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <div class="detail-container">
      <!-- ===== 薄弱点上下文横幅（从薄弱点专项/能力概览进入） ===== -->
      <div v-if="weaknessCtx" class="weakness-context-banner">
        <div class="wc-icon">💡</div>
        <div class="wc-content">
          <div class="wc-title">本路径基于您的薄弱点生成</div>
          <div class="wc-subjects">{{ weaknessCtx.subjects.join('、') }}</div>
          <div class="wc-desc">已为您规划针对性学习任务，专注攻克短板，完成后整体掌握度有望提升</div>
        </div>
      </div>

      <!-- ===== 概览信息 ===== -->
      <div class="overview-card">
        <div class="overview-grid">
          <div class="overview-item">
            <span class="overview-label">难度</span>
            <span class="overview-value difficulty" :class="difficultyClass">{{ pathDetail.difficulty || '-' }}</span>
          </div>
          <div class="overview-item">
            <span class="overview-label">学习者</span>
            <span class="overview-value">{{ formatLearnerCount(pathDetail.learnerCount) }}</span>
          </div>
          <div class="overview-item">
            <span class="overview-label">评分</span>
            <span class="overview-value rating">⭐ {{ pathDetail.rating || '-' }}</span>
          </div>
          <div class="overview-item">
            <span class="overview-label">学习进度</span>
            <span class="overview-value progress-text">{{ pathDetail.progress }}%</span>
          </div>
        </div>

        <!-- AI 进度解读 -->
        <div v-if="aiProgressInsight" class="ai-progress-insight">
          <div class="insight-icon">💡</div>
          <div class="insight-content">
            <p class="insight-message">{{ aiProgressInsight.message }}</p>
            <button v-if="aiProgressInsight.nextAction" class="insight-action-btn" @click="handleInsightAction">
              {{ aiProgressInsight.nextAction.label }} →
            </button>
          </div>
        </div>

        <div class="progress-track">
          <div class="progress-fill" :style="{ width: pathDetail.progress + '%' }"></div>
        </div>
        <div class="overview-stats">
          <span class="stat-item">已完成 {{ pathDetail.completedModules }} / {{ pathDetail.totalModules }} 个模块</span>
          <span class="stat-divider">|</span>
          <span class="stat-item">已学 {{ formatHours(pathDetail.spentHours) }} / 预估 {{ formatHours(pathDetail.estimatedHours) }}</span>
        </div>
      </div>

      <!-- ===== 加载状态 ===== -->
      <div v-if="isLoading" class="loading-card">
        <div class="loading-spinner"></div>
        <span>正在加载学习进度...</span>
      </div>

      <!-- ===== 阶段进度 ===== -->
      <template v-else>
        <div v-if="pathDetail.phases && pathDetail.phases.length > 0" class="phases-container">
          <div v-for="phase in pathDetail.phases" :key="phase.id" class="phase-card">
            <div class="phase-header">
              <div class="phase-title-row">
                <span class="phase-icon">📘</span>
                <span class="phase-title">{{ phase.title }}</span>
                <span class="phase-badge" :class="phase.status">{{ phaseStatusText(phase.status) }}</span>
              </div>
              <div class="phase-meta-row">
                <span class="phase-task-count">共 {{ phaseTotalCount(phase) }} 个任务 ｜ 已完成 {{ phaseCompletedCount(phase) }}</span>
                <span class="phase-progress-label">{{ phase.progress }}%</span>
              </div>
              <div class="phase-progress-row">
                <div class="phase-progress-track">
                  <div class="phase-progress-fill" :style="{ width: phase.progress + '%' }"></div>
                </div>
                <span class="phase-progress-text">{{ phase.progress }}%</span>
              </div>
            </div>

            <!-- 周任务 -->
            <div class="weeks-container">
              <div v-for="week in phase.weeks" :key="week.id" class="week-section">
                <div class="week-header">
                  <span class="week-title">{{ week.title }}</span>
                  <span class="week-progress-label">{{ week.progress }}%</span>
                </div>
                <div class="week-progress-track">
                  <div class="week-progress-fill" :style="{ width: week.progress + '%' }"></div>
                </div>

                <!-- 任务列表 -->
                <div class="tasks-container">
                  <div
                    v-for="task in week.tasks"
                    :key="task.id"
                    class="task-item"
                    :class="{ 'task-current': task.id === currentTaskId }"
                    :data-task-id="task.id"
                  >
                    <div class="task-status-icon" :class="task.status">
                      <span v-if="task.status === 'completed'">✅</span>
                      <span v-else-if="task.status === 'in_progress'">🔄</span>
                      <span v-else>⬜</span>
                    </div>
                    <div class="task-content">
                      <span class="task-title" :class="task.status">{{ task.title }}</span>
                      <span v-if="task.id === currentTaskId" class="task-current-badge">⭐ 当前任务</span>
                      <span v-if="weaknessCtx && task.id === firstTaskId" class="task-weakness-badge">🔍 基于薄弱点「{{ weaknessCtx.subjects[0] }}」生成</span>
                      <span class="task-meta">预计 {{ formatHours(task.estimatedHours) }} / 已学 {{ formatHours(task.spentHours) }}</span>
                    </div>
                    <button
                      v-if="task.status !== 'completed' && task.id === currentTaskId"
                      class="task-continue-btn"
                      @click="scrollToTask"
                    >
                      继续学习 →
                    </button>
                    <button
                      v-else-if="task.status !== 'completed'"
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
          </div>
        </div>

        <!-- 全部完成 -->
        <div v-if="allCompleted" class="all-done-banner">
          <span class="all-done-icon">🎉</span>
          <span class="all-done-text">恭喜完成！你已经学完了这条路径的全部任务</span>
          <button class="all-done-btn" @click="router.push('/learning-path')">查看下一路径推荐 →</button>
        </div>

        <!-- 暂无阶段数据 -->
        <div v-else class="empty-card">
          <div class="empty-icon">📚</div>
          <template v-if="weaknessCtx">
            <p class="empty-title">已根据您的薄弱点规划任务</p>
            <p class="empty-desc">本路径基于「{{ weaknessCtx.subjects.join('、') }}」生成，点击下方按钮生成针对性学习大纲（章节与任务）</p>
          </template>
          <template v-else>
            <p class="empty-title">暂无学习任务</p>
            <p class="empty-desc">点击下方按钮，为这条路径生成学习大纲（章节与任务）</p>
          </template>
          <button class="generate-outline-btn" :disabled="generatingOutline" @click="generateOutline">
            <span v-if="generatingOutline" class="outline-spinner"></span>
            <span>{{ generatingOutline ? '生成中...' : '✨ 生成学习大纲' }}</span>
          </button>
        </div>
      </template>

      <!-- ===== 路径描述 ===== -->
      <div v-if="pathDetail.description" class="content-card">
        <div class="content-header">
          <span class="content-title-icon">📝</span>
          <span class="content-title-text">路径说明</span>
        </div>
        <div class="content-body markdown-body" v-html="pathDescriptionHtml"></div>
      </div>

      <!-- ===== AI 学习推荐 ===== -->
      <div v-if="aiRecommendation" class="dynamic-planning-guide">
        <div class="guide-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
        </div>
        <div class="guide-content">
          <div class="guide-title">🤖 AI 学习推荐</div>
          <div class="guide-desc">
根据你的进度，建议今天优先完成「{{ aiRecommendation.title }}」
            预计需要 {{ aiRecommendation.minutes }} 分钟，完成后可进入下一阶段。
</div>
        </div>
        <button class="guide-arrow-btn" @click="scrollToTask">立即学习 →</button>
      </div>

      <!-- ===== 动态规划引导 ===== -->
      <div class="dynamic-planning-guide" @click="goToDynamicPlanning">
        <div class="guide-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
        </div>
        <div class="guide-content">
          <div class="guide-title">💡 动态规划</div>
          <div class="guide-desc">根据学习进度实时调整计划，生成每日学习任务</div>
        </div>
        <div class="guide-arrow">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <button class="btn-primary" @click="handlePrimaryAction">
        {{ allCompleted ? '🎉 全部完成' : (firstUnfinishedTask ? '🚀 继续学习' : '📖 开始学习') }}
      </button>
      <button class="btn-secondary" @click="router.push('/knowledge')">
        📚 查看知识块
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPathProgress, completeTask as apiCompleteTask, generatePathOutline } from '@/api/learningPath'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()
const pathId = computed(() => route.params.id)

const isLoading = ref(true)
const isFavorited = ref(false)
const taskCompleting = ref(null)
const showMoreMenu = ref(false)
const showDeleteModal = ref(false)
const isDeleting = ref(false)
// 路径不存在（404）标记，用于展示空态
const notFound = ref(false)

const pathDetail = reactive({
  pathId: null,
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

const difficultyClass = computed(() => {
  const d = (pathDetail.difficulty || '').toLowerCase()
  if (d.includes('入门') || d === 'beginner' || d === '初级') return 'beginner'
  if (d.includes('中级') || d === 'intermediate') return 'intermediate'
  if (d.includes('高级') || d === 'advanced') return 'advanced'
  return ''
})

// 路径描述渲染为 Markdown
const pathDescriptionHtml = computed(() => {
  if (!pathDetail.description) return ''
  return renderMarkdown(pathDetail.description)
})

const phaseStatusText = (status) => {
  const map = { pending: '未开始', in_progress: '进行中', completed: '已完成' }
  return map[status] || status || '未知'
}

// 章节任务统计
const phaseTotalCount = (phase) => {
  return (phase.weeks || []).reduce((s, w) => s + (w.tasks || []).length, 0)
}

const phaseCompletedCount = (phase) => {
  return (phase.weeks || []).reduce((s, w) => s + (w.tasks || []).filter(t => t.status === 'completed').length, 0)
}

// 第一个未完成任务（用于继续学习定位/AI 推荐）
const firstUnfinishedTask = computed(() => {
  for (const phase of pathDetail.phases || []) {
    for (const week of phase.weeks || []) {
      for (const task of week.tasks || []) {
        if (task.status !== 'completed') {
          return { task, phase, week }
        }
      }
    }
  }
  return null
})

// 全部任务是否完成（有内容且无未完成任务）
const allCompleted = computed(() => {
  const phases = pathDetail.phases || []
  if (phases.length === 0) return false
  return !phases.some(p => (p.weeks || []).some(w => (w.tasks || []).some(t => t.status !== 'completed')))
})

// 薄弱点上下文：从薄弱点专项/能力概览跳转进入时携带
// 例：/learning-path/1?from=weakness&subjects=面向对象编程,机器学习调参
const weaknessCtx = computed(() => {
  if (route.query.from !== 'weakness') return null
  const subjects = (route.query.subjects || route.query.subject || '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
  if (subjects.length === 0) return null
  return { subjects }
})

// 第一个任务的 ID（用于标注「基于薄弱点生成」）
const firstTaskId = computed(() => {
  for (const phase of pathDetail.phases || []) {
    for (const week of phase.weeks || []) {
      if ((week.tasks || []).length > 0) return week.tasks[0].id
    }
  }
  return null
})

// 当前高亮任务 ID
const currentTaskId = computed(() => firstUnfinishedTask.value?.task?.id || null)

// AI 学习推荐：基于第一个未完成任务生成具体建议
const aiRecommendation = computed(() => {
  const target = firstUnfinishedTask.value
  if (!target) return null
  return {
    title: target.task.title,
    minutes: Math.max(10, Math.round((target.task.estimatedHours || 1) * 60)),
    phaseTitle: target.phase.title
  }
})

// AI 进度解读：基于当前路径状态生成个性化解读
const aiProgressInsight = computed(() => {
  if (!pathDetail.name) return null

  const totalPhases = pathDetail.phases?.length || 0
  const totalTasks = (pathDetail.phases || []).reduce((s, p) => s + phaseTotalCount(p), 0)
  const completedTasks = (pathDetail.phases || []).reduce((s, p) => s + phaseCompletedCount(p), 0)
  const progress = pathDetail.progress || 0
  const estimatedHours = pathDetail.estimatedHours || 0

  // 场景1：进度为0，尚未开始
  if (progress === 0 && totalTasks === 0) {
    return {
      message: `根据你的学习目标，AI 规划了 ${totalPhases} 个章节共 ${totalTasks} 个任务，预计总耗时 ${estimatedHours.toFixed(1)} 小时。`,
      nextAction: null
    }
  }

  // 场景2：刚开始学习，推荐第一个任务
  if (progress > 0 && progress < 30) {
    const firstTask = firstUnfinishedTask.value?.task
    if (firstTask) {
      return {
        message: `学习进度 ${progress}%。建议今天从「${firstTask.title}」开始，这是后续章节的前置基础。`,
        nextAction: { label: '开始第一个任务', taskId: firstTask.id }
      }
    }
  }

  // 场景3：学习中期，给出阶段建议
  if (progress >= 30 && progress < 80) {
    const currentPhase = pathDetail.phases?.find(p => p.status === 'in_progress')
    if (currentPhase) {
      return {
        message: `你已完成 ${completedTasks} 个任务，正在进行「${currentPhase.title}」。继续保持当前节奏，预计还需 ${((estimatedHours * (100 - progress)) / 100).toFixed(1)} 小时完成全部内容。`,
        nextAction: { label: '继续学习', taskId: firstUnfinishedTask.value?.task?.id }
      }
    }
  }

  // 场景4：接近完成
  if (progress >= 80 && progress < 100) {
    return {
      message: `太棒了！学习进度已达 ${progress}%，即将完成这条路径。${totalTasks - completedTasks} 个任务等待你来攻克。`,
      nextAction: { label: '完成最后的冲刺', taskId: firstUnfinishedTask.value?.task?.id }
    }
  }

  // 场景5：全部完成
  if (progress === 100) {
    return {
      message: `🎉 恭喜！你已完成「${pathDetail.name}」的全部学习内容。`,
      nextAction: null
    }
  }

  return null
})

const handleInsightAction = () => {
  const action = aiProgressInsight.value?.nextAction
  if (action?.taskId) {
    scrollToTask()
  } else {
    generateOutline()
  }
}

// 滚动定位到第一个未完成任务
const scrollToTask = () => {
  const target = firstUnfinishedTask.value
  if (!target) {
    router.push('/goal-setting')
    return
  }
  const el = document.querySelector(`[data-task-id="${target.task.id}"]`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('task-flash')
    setTimeout(() => el.classList.remove('task-flash'), 1600)
  }
}

// 底部主按钮：继续学习 / 全部完成 / 开始学习
const handlePrimaryAction = () => {
  if (firstUnfinishedTask.value) {
    scrollToTask()
  } else if (pathDetail.phases?.length > 0) {
    // 全部完成 → 查看其他路径
    router.push('/learning-path')
  } else {
    // 无内容 → 引导生成大纲
    generateOutline()
  }
}

// 补生成学习大纲（存量空路径）
const generatingOutline = ref(false)
const generateOutline = async () => {
  generatingOutline.value = true
  try {
    await generatePathOutline(pathId.value)
    ElMessage.success('✨ 学习大纲已生成，共 4 个章节')
    await fetchProgress()
  } catch (e) {
    ElMessage.error('生成失败：' + (e?.response?.data?.message || e.message || '请稍后重试'))
  } finally {
    generatingOutline.value = false
  }
}

const formatHours = (hours) => {
  if (hours == null || hours === 0) return '0h'
  return hours % 1 === 0 ? `${hours}h` : `${hours.toFixed(1)}h`
}

const formatLearnerCount = (count) => {
  if (count == null) return '-'
  if (count >= 10000) return (count / 10000).toFixed(1) + ' 万人'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'K 人'
  return count + ' 人'
}

const fetchProgress = async () => {
  isLoading.value = true
  try {
    const res = await getPathProgress(pathId.value)
    const data = res?.data ?? res
    if (data && data.pathId) {
      Object.assign(pathDetail, data)
    } else {
      console.warn('进度接口返回数据异常:', data)
    }
  } catch (error) {
    // 路径不存在（后端返回 404）时展示空态，而非通用错误弹窗
    const msg = error?.message || ''
    if (msg.includes('不存在')) {
      notFound.value = true
      console.warn('学习路径不存在:', pathId.value)
    } else {
      console.error('获取进度数据失败:', error)
      ElMessage.error('获取学习进度失败')
    }
  } finally {
    isLoading.value = false
  }
}

const completeTask = async (taskId) => {
  taskCompleting.value = taskId
  try {
    await apiCompleteTask(pathId.value, taskId)
    ElMessage.success('任务已完成！进度已更新')
    await fetchProgress()
  } catch (error) {
    console.error('完成任务失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    taskCompleting.value = null
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/learning-path')
  }
}

const toggleMoreMenu = () => {
  showMoreMenu.value = !showMoreMenu.value
}

const handleDeleteClick = () => {
  showMoreMenu.value = false
  showDeleteModal.value = true
}

const handleCancelDelete = () => {
  if (isDeleting.value) return
  showDeleteModal.value = false
}

const handleConfirmDelete = async () => {
  if (isDeleting.value) return
  isDeleting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 800))
    ElMessage.success('已成功删除学习路线')
    router.push('/learning-path')
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败，请稍后重试')
  } finally {
    isDeleting.value = false
    showDeleteModal.value = false
  }
}

const handleShare = () => {
  ElMessage.success('链接已复制')
}

const goToDynamicPlanning = () => {
  router.push('/capability/planning')
}

onMounted(() => {
  fetchProgress()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;
.path-detail-page {
  min-height: 100vh;
  background: $bg-primary;
  position: relative;
  overflow: hidden;
  padding: 0;
  display: flex;
  flex-direction: column;
}

// ===== 背景 =====
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora {
  position: absolute; inset: 0;
  background:
    radial-gradient(ellipse at 70% 20%, rgba($accent-primary,0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift {
  0%,100% { transform: scale(1) rotate(0deg); }
  33% { transform: scale(1.1) rotate(1deg); }
  66% { transform: scale(0.95) rotate(-1deg); }
}
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba($accent-primary,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  animation: gridPulse 8s ease-in-out infinite alternate;
}
@keyframes gridPulse {
  0% { opacity: 0.3; transform: scale(1); }
  100% { opacity: 0.6; transform: scale(1.02); }
}

// ===== 导航栏 =====
.nav-bar {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; gap: 16px;
  padding: 16px 32px;
  background: rgba($bg-primary,0.85);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba($accent-secondary,0.06);
}
.back-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  background: rgba($accent-secondary,0.06);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 8px;
  color: $text-secondary; font-size: 0.82rem; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
  &:hover {
    border-color: rgba($accent-primary,0.2);
    color: $accent-primary;
    background: rgba($accent-primary,0.04);
    box-shadow: 0 0 14px rgba($accent-primary,0.08);
  }
}
.nav-title {
  flex: 1; font-size: 1.1rem; font-weight: 700;
  color: $text-primary; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.nav-actions { display: flex; gap: 8px; }
.icon-btn {
  width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  background: rgba($accent-secondary,0.06);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 8px;
  color: $text-secondary; cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: rgba($accent-primary,0.2); color: $accent-primary; }
  &.favorited { color: $accent-amber; border-color: rgba(245,158,11,0.2); }
}

// ===== 路径不存在空态 =====
.not-found-box {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; padding: 80px 24px;
  position: relative; z-index: 1;
}
.not-found-icon { font-size: 3.2rem; opacity: 0.85; }
.not-found-title { margin: 0; font-size: 1.25rem; font-weight: 700; color: $text-primary; }
.not-found-desc { margin: 0; font-size: 0.88rem; color: var(--text-placeholder); }
.not-found-btn {
  margin-top: 8px; padding: 9px 22px;
  background: linear-gradient(135deg, rgba($accent-primary,0.12), rgba(123,97,255,0.12));
  border: 1px solid rgba($accent-primary,0.25);
  border-radius: 10px;
  color: $accent-primary; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.25s;
  &:hover {
    box-shadow: 0 0 18px rgba($accent-primary,0.15);
    transform: translateY(-1px);
  }
}

// ===== 详情容器 =====
.detail-container {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 28px 32px 100px;
  position: relative; z-index: 1;
  display: flex; flex-direction: column; gap: 20px;
}

// ===== 薄弱点上下文横幅 =====
.weakness-context-banner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 22px;
  background: rgba(239,68,68,0.06);
  border: 1px solid rgba(239,68,68,0.18);
  border-radius: 14px;
  animation: wcIn 0.4s ease both;
}

@keyframes wcIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.wc-icon { font-size: 1.5rem; flex-shrink: 0; }

.wc-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.wc-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #f1f5f9;
}

.wc-subjects {
  font-size: 0.85rem;
  font-weight: 600;
  color: $accent-red;
}

.wc-desc {
  font-size: 0.82rem;
  color: $text-muted;
  line-height: 1.5;
}

// ===== 概览卡片 =====
.overview-card {
  padding: 24px 28px;
  background: rgba($bg-primary,0.55);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08);
  border-radius: 14px;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 14px;
}
.overview-item {
  display: flex; flex-direction: column; gap: 4px;
}
.overview-label { font-size: 0.72rem; color: $text-muted; font-weight: 500; }
.overview-value {
  font-size: 0.95rem; font-weight: 600; color: $text-primary;
  &.difficulty {
    &.beginner { color: $accent-emerald; }
    &.intermediate { color: $accent-blue; }
    &.advanced { color: #a855f7; }
  }
  &.rating { color: $accent-amber; }
  &.progress-text { color: $accent-primary; }
}
.progress-track {
  height: 4px;
  background: rgba($accent-secondary,0.08);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 10px;
}

// ===== AI 进度解读 =====
.ai-progress-insight {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  margin-top: 14px;
  background: rgba($accent-primary, 0.06);
  border: 1px solid rgba($accent-primary, 0.12);
  border-radius: 10px;
}
.insight-icon { font-size: 1.1rem; flex-shrink: 0; margin-top: 2px; }
.insight-content { flex: 1; }
.insight-message {
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 10px;
}
.insight-action-btn {
  padding: 6px 14px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 6px;
  color: $accent-primary;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-primary, 0.15);
  }
}

// ===== 概览卡片 =====
.overview-card {
  padding: 24px 28px;
  background: rgba($bg-primary,0.55);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08);
  border-radius: 14px;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 14px;
}
.overview-item {
  display: flex; flex-direction: column; gap: 4px;
}
.overview-label { font-size: 0.72rem; color: $text-muted; font-weight: 500; }
.overview-value {
  font-size: 0.95rem; font-weight: 600; color: $text-primary;
  &.difficulty {
    &.beginner { color: $accent-emerald; }
    &.intermediate { color: $accent-blue; }
    &.advanced { color: #a855f7; }
  }
  &.rating { color: $accent-amber; }
  &.progress-text { color: $accent-primary; }
}
.progress-track {
  height: 4px;
  background: rgba($accent-secondary,0.08);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 10px;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, $accent-emerald);
  border-radius: 2px;
  transition: width 0.8s ease;
}
.overview-stats {
  display: flex; align-items: center; gap: 10px;
  font-size: 0.78rem; color: $text-muted;
}
.stat-divider { opacity: 0.3; }

// ===== 加载 =====
.loading-card {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60px 20px; gap: 12px;
  color: $text-muted; font-size: 0.85rem;
}
.loading-spinner {
  width: 28px; height: 28px;
  border: 3px solid rgba($accent-primary,0.1);
  border-top-color: $accent-primary;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

// ===== 阶段卡片 =====
.phases-container {
  display: flex; flex-direction: column; gap: 16px;
}
.phase-card {
  background: rgba($bg-primary,0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08);
  border-radius: 14px;
  padding: 20px 24px;
}
.phase-header { margin-bottom: 14px; }
.phase-meta-row {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
}
.phase-task-count {
  font-size: 0.76rem; color: $text-muted;
}
.phase-progress-label {
  font-size: 0.76rem; font-weight: 700; color: $accent-primary;
}
.phase-title-row {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 10px;
}
.phase-icon { font-size: 1rem; }
.phase-title { font-size: 0.95rem; font-weight: 700; color: $text-primary; flex: 1; }
.phase-badge {
  font-size: 0.72rem; font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
  &.pending { background: rgba($accent-secondary,0.1); color: $text-muted; }
  &.in_progress { background: rgba($accent-primary,0.08); color: $accent-primary; }
  &.completed { background: rgba(16,185,129,0.1); color: $accent-emerald; }
}
.phase-progress-row {
  display: flex; align-items: center; gap: 12px;
}
.phase-progress-track {
  flex: 1; height: 6px;
  background: rgba($accent-secondary,0.08);
  border-radius: 3px;
  overflow: hidden;
}
.phase-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, $accent-blue);
  border-radius: 3px;
  transition: width 0.6s ease;
}
.phase-progress-text {
  font-size: 0.82rem; font-weight: 700; color: $accent-primary;
  min-width: 36px; text-align: right;
}

// ===== 周容器 =====
.weeks-container {
  display: flex; flex-direction: column; gap: 12px;
}
.week-section {
  background: rgba($accent-secondary,0.03);
  border: 1px solid rgba($accent-secondary,0.05);
  border-radius: 10px;
  padding: 14px 16px;
}
.week-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 6px;
}
.week-title { font-size: 0.85rem; font-weight: 600; color: $text-secondary; }
.week-progress-label { font-size: 0.78rem; font-weight: 600; color: #7dd3fc; }
.week-progress-track {
  height: 4px;
  background: rgba($accent-secondary,0.06);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 10px;
}
.week-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #7dd3fc, $accent-blue);
  border-radius: 2px;
  transition: width 0.5s ease;
}

// ===== 任务列表 =====
.tasks-container {
  display: flex; flex-direction: column; gap: 6px;
}
.task-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.2s, box-shadow 0.3s;
  &:hover { background: rgba($accent-secondary,0.05); }

  // 当前任务高亮
  &.task-current {
    background: rgba($accent-primary,0.06);
    border: 1px solid rgba($accent-primary,0.2);
    box-shadow: 0 0 14px rgba($accent-primary,0.06);
  }

  // 定位闪动动画
  &.task-flash {
    animation: taskFlash 1.5s ease;
  }
}
@keyframes taskFlash {
  0% { box-shadow: 0 0 0 0 rgba($accent-primary,0.5); }
  70% { box-shadow: 0 0 0 10px rgba($accent-primary,0); }
  100% { box-shadow: 0 0 0 0 rgba($accent-primary,0); }
}
.task-current-badge {
  font-size: 0.68rem; font-weight: 700; color: $accent-primary;
  margin-right: 6px;
}
// 基于薄弱点生成的任务徽标
.task-weakness-badge {
  display: inline-block;
  width: fit-content;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.68rem;
  font-weight: 600;
  color: $accent-red;
  border: 1px solid rgba(239,68,68,0.2);
  background: rgba(239,68,68,0.08);
}
.task-continue-btn {
  padding: 4px 14px;
  font-size: 0.75rem; font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, $accent-primary, #0ea5e9);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.2s;
  &:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba($accent-primary,0.25); }
}
.task-status-icon {
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.85rem;
  flex-shrink: 0;
}
.task-content {
  flex: 1;
  display: flex; flex-direction: column; gap: 2px;
  min-width: 0;
}
.task-title {
  font-size: 0.82rem; font-weight: 500; color: #d0d0f0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  &.completed { text-decoration: line-through; color: #7070a0; }
}
.task-meta {
  font-size: 0.72rem; color: #7070a0;
}
.task-complete-btn {
  padding: 4px 12px;
  font-size: 0.75rem; font-weight: 600;
  color: $accent-primary;
  background: rgba($accent-primary,0.06);
  border: 1px solid rgba($accent-primary,0.15);
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  flex-shrink: 0;
  &:hover:not(:disabled) {
    background: rgba($accent-primary,0.1);
    border-color: rgba($accent-primary,0.3);
    box-shadow: 0 0 10px rgba($accent-primary,0.08);
  }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

// ===== 空状态 =====
.empty-card {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60px 20px; text-align: center;
  background: rgba($bg-primary,0.4);
  border: 1px solid rgba($accent-secondary,0.06);
  border-radius: 14px;
}
.empty-icon { font-size: 3rem; margin-bottom: 12px; opacity: 0.4; }
.empty-title { font-size: 1rem; font-weight: 600; color: $text-primary; margin-bottom: 4px; }
.empty-desc { font-size: 0.85rem; color: $text-muted; margin-bottom: 18px; }

.generate-outline-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba($accent-primary,0.12), rgba(0,85,255,0.1));
  border: 1px solid rgba($accent-primary,0.25);
  border-radius: 10px;
  color: $accent-primary;
  font-size: 0.85rem; font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  &:hover:not(:disabled) { box-shadow: 0 0 18px rgba($accent-primary,0.15); transform: translateY(-1px); }
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

.outline-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba($accent-primary,0.2);
  border-top-color: $accent-primary;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

// ===== 全部完成横幅 =====
.all-done-banner {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(16,185,129,0.1), rgba($accent-primary,0.06));
  border: 1px solid rgba(16,185,129,0.2);
  border-radius: 12px;
}
.all-done-icon { font-size: 1.4rem; }
.all-done-text {
  flex: 1; font-size: 0.9rem; font-weight: 600; color: $accent-emerald;
}
.all-done-btn {
  padding: 6px 14px;
  background: rgba(16,185,129,0.1);
  border: 1px solid rgba(16,185,129,0.25);
  border-radius: 8px;
  color: $accent-emerald;
  font-size: 0.78rem; font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: rgba(16,185,129,0.18); }
}

// ===== 文档内容 =====
.content-card {
  background: rgba($bg-primary,0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08);
  border-radius: 14px;
  overflow: hidden;
}
.content-header {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba($accent-secondary,0.06);
}
.content-title-icon { font-size: 1rem; }
.content-title-text { font-size: 0.9rem; font-weight: 600; color: $text-primary; }
.content-meta { margin-left: auto; font-size: 0.75rem; color: $text-muted; }
.content-body {
  padding: 24px 28px;
  min-height: 200px;
}

// ===== 底部操作栏 =====
.action-bar {
  position: fixed; bottom: 0; left: 0; right: 0; z-index: 10;
  display: flex; align-items: center; justify-content: center; gap: 12px;
  padding: 14px 32px;
  background: rgba($bg-primary,0.88);
  backdrop-filter: blur(16px);
  border-top: 1px solid rgba($accent-secondary,0.06);
}
.btn-primary {
  padding: 12px 32px;
  background: linear-gradient(135deg, $accent-primary, #0055FF);
  border: none; border-radius: 10px;
  color: #fff; font-size: 0.9rem; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
  &:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba($accent-primary,0.2); }
}
.btn-secondary {
  padding: 12px 28px;
  background: rgba($accent-secondary,0.06);
  border: 1px solid rgba($accent-secondary,0.12);
  border-radius: 10px;
  color: $text-secondary; font-size: 0.9rem; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: rgba($accent-primary,0.2); color: $accent-primary; background: rgba($accent-primary,0.04); }
}

// ===== Markdown 渲染 =====
:deep(.markdown-body) {
  h1, h2, h3, h4 { color: $text-primary; font-weight: 700; margin: 16px 0 8px; line-height: 1.4; }
  h1 { font-size: 1.5rem; }
  h2 { font-size: 1.25rem; color: $accent-primary; border-bottom: 1px solid rgba($accent-primary,0.08); padding-bottom: 6px; }
  h3 { font-size: 1.1rem; color: #7dd3fc; }
  p { margin: 8px 0; line-height: 1.75; color: #d0d0f0; }
  ul, ol { padding-left: 24px; margin: 8px 0; }
  li { margin: 4px 0; line-height: 1.7; color: #d0d0f0; }
  strong { color: $accent-primary; font-weight: 700; }
  em { color: #a78bfa; font-style: italic; }
  code { background: rgba($accent-primary,0.12); border: 1px solid rgba($accent-primary,0.18); border-radius: 4px; padding: 1px 5px; font-size: 0.88em; color: #7ae0ff; font-family: 'Fira Code', 'Consolas', monospace; }
  pre {
    background: $bg-primary;
    border: 1px solid rgba($accent-secondary,0.15);
    border-radius: 8px;
    padding: 16px 18px;
    margin: 14px 0;
    overflow-x: auto;
    color: #e6edf3;
    position: relative;
    code {
      background: transparent;
      border: none;
      padding: 0;
      color: #e6edf3;
      font-size: 0.85rem;
      line-height: 1.6;
    }
  }
  blockquote { border-left: 3px solid rgba($accent-primary,0.3); margin: 10px 0; padding: 8px 16px; background: rgba($accent-primary,0.03); border-radius: 0 6px 6px 0; color: $text-secondary; }
  a { color: #7dd3fc; text-decoration: none; &:hover { text-decoration: underline; } }
  table { border-collapse: collapse; width: 100%; margin: 10px 0; th, td { border: 1px solid rgba($accent-secondary,0.15); padding: 8px 12px; text-align: left; } th { background: rgba($accent-primary,0.05); color: $accent-primary; } }
  hr { border: none; border-top: 1px solid rgba($accent-secondary,0.1); margin: 16px 0; }
  img { max-width: 100%; border-radius: 8px; }
}

// ===== 更多菜单 =====
.more-menu-wrapper {
  position: relative;
}

.more-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 140px;
  background: rgba($bg-primary, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 10px;
  padding: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  z-index: 100;
  animation: menuFadeIn 0.15s ease;
}

@keyframes menuFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.more-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: $text-secondary;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;

  &:hover {
    background: rgba($accent-secondary, 0.08);
    color: $text-primary;
  }

  &.danger {
    color: #EF4444;
    &:hover {
      background: rgba(239, 68, 68, 0.1);
    }
  }
}

// ===== 删除确认弹窗 =====
.delete-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.delete-modal {
  width: 90%;
  max-width: 420px;
  background: rgba($bg-primary, 0.95);
  backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.delete-modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.delete-modal-icon {
  color: #F59E0B;
  flex-shrink: 0;
}

.delete-modal-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.delete-modal-content {
  font-size: 0.9rem;
  color: #C0C0E0;
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.delete-modal-name {
  color: #00F5D4;
  font-weight: 500;
}

.delete-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  padding: 10px 24px;
  background: rgba($accent-secondary, 0.08);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba($accent-secondary, 0.12);
    border-color: rgba($accent-secondary, 0.25);
    color: #E2E8F0;
  }
}

.btn-confirm-delete {
  padding: 10px 24px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 8px;
  color: #EF4444;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: rgba(239, 68, 68, 0.2);
    border-color: rgba(239, 68, 68, 0.4);
    box-shadow: 0 0 16px rgba(239, 68, 68, 0.15);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

// ===== 动态规划引导 =====
.dynamic-planning-guide {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), rgba(16, 185, 129, 0.06));
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.12), rgba(16, 185, 129, 0.1));
    border-color: rgba(245, 158, 11, 0.3);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(245, 158, 11, 0.1);
  }

  .guide-icon {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(245, 158, 11, 0.1);
    border-radius: 10px;
    color: #F59E0B;
    flex-shrink: 0;
  }

  .guide-content {
    flex: 1;
    min-width: 0;
  }

  .guide-title {
    font-size: 0.95rem;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .guide-desc {
    font-size: 0.82rem;
    color: $text-muted;
  }

  .guide-arrow {
    color: $text-muted;
    flex-shrink: 0;
    transition: transform 0.2s ease;
  }

  .guide-arrow-btn {
    padding: 8px 18px;
    background: linear-gradient(135deg, $accent-primary, #0ea5e9);
    border: none;
    border-radius: 8px;
    color: #fff;
    font-size: 0.8rem;
    font-weight: 700;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.2s;
    &:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba($accent-primary,0.25); }
  }

  &:hover .guide-arrow {
    transform: translateX(4px);
    color: #F59E0B;
  }
}

// ===== 响应式 =====
@media (max-width: 768px) {
  .nav-bar { padding: 12px 16px; }
  .detail-container { padding: 16px 16px 90px; }
  .overview-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .phase-card { padding: 16px; }
  .content-body { padding: 16px 18px; }
  .action-bar { padding: 12px 16px; }
}
</style>