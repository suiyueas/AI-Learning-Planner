<template>
  <div class="path-detail-page">
    <!-- 返回导航 -->
    <div class="nav-bar">
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg>
        返回
      </button>
      <h1 class="page-title">
        <span class="title-glyph">🧭</span>
        <span>{{ pathDetail.name || '学习路径详情' }}</span>
      </h1>
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
  router.push('/learning-path')
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
  background: $bg-base;
  position: relative;
  overflow-x: hidden;
  padding: 0;
  display: flex;
  flex-direction: column;
  color: $text-primary;
  font-family: $font-sans;
}

// ===== 导航栏 =====
.nav-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: $space-4;
  padding: $space-4 $space-8;
  background: $bg-surface;
  border-bottom: 1px solid $border-default;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: $space-2 $space-3;
  background: transparent;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $bg-muted;
    border-color: $border-medium;
    color: $accent-indigo;
  }
}

.page-title { @include page-title-base; }

.nav-actions {
  display: flex;
  gap: $space-2;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $bg-muted;
    border-color: $border-medium;
    color: $text-primary;
  }

  &.favorited {
    color: $color-warning;
    border-color: rgba($color-warning, 0.3);
  }
}

// ===== 路径不存在空态 =====
.not-found-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-3;
  padding: $space-16 $space-6;
}

.not-found-icon {
  font-size: 3rem;
  opacity: 0.85;
}

.not-found-title {
  margin: 0;
  font-size: $text-xl;
  font-weight: 700;
  color: $text-primary;
}

.not-found-desc {
  margin: 0;
  font-size: $text-base;
  color: $text-secondary;
}

.not-found-btn {
  margin-top: $space-2;
  padding: $space-3 $space-6;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  color: $accent-indigo;
  font-size: $text-sm;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $bg-muted;
    border-color: $border-medium;
  }
}

// ===== 详情容器 =====
.detail-container {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: $space-8 $space-8 calc($space-16 * 2 + 40px);
  display: flex;
  flex-direction: column;
  gap: $space-5;
}

// ===== 薄弱点上下文横幅 =====
.weakness-context-banner {
  display: flex;
  align-items: flex-start;
  gap: $space-4;
  padding: $space-5 $space-6;
  background: rgba($color-danger, 0.06);
  border: 1px solid rgba($color-danger, 0.2);
  border-radius: $radius-xl;
  animation: wcIn 0.3s ease both;
}

@keyframes wcIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.wc-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.wc-content {
  display: flex;
  flex-direction: column;
  gap: $space-1;
}

.wc-title {
  font-size: $text-md;
  font-weight: 600;
  color: $text-primary;
}

.wc-subjects {
  font-size: $text-sm;
  font-weight: 600;
  color: $color-danger;
}

.wc-desc {
  font-size: $text-sm;
  color: $text-secondary;
  line-height: $leading-normal;
}

// ===== 概览卡片 =====
.overview-card {
  padding: $space-6 $space-8;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $space-4;
  margin-bottom: $space-4;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: $space-1;
}

.overview-label {
  font-size: $text-xs;
  color: $text-muted;
  font-weight: 500;
}

.overview-value {
  font-size: $text-md;
  font-weight: 600;
  color: $text-primary;

  &.difficulty {
    &.beginner { color: $color-success; }
    &.intermediate { color: $color-info; }
    &.advanced { color: $accent-violet; }
  }

  &.rating { color: $color-warning; }
  &.progress-text { color: $accent-indigo; }
}

// ===== AI 进度解读 =====
.ai-progress-insight {
  display: flex;
  align-items: flex-start;
  gap: $space-3;
  padding: $space-4 $space-5;
  margin-top: $space-4;
  background: rgba($accent-indigo, 0.06);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: $radius-lg;
}

.insight-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
  margin-top: 2px;
}

.insight-content {
  flex: 1;
}

.insight-message {
  font-size: $text-sm;
  color: $text-secondary;
  line-height: $leading-relaxed;
  margin: 0 0 $space-3;
}

.insight-action-btn {
  padding: $space-2 $space-4;
  background: rgba($accent-indigo, 0.12);
  border: 1px solid rgba($accent-indigo, 0.2);
  border-radius: $radius-sm;
  color: $accent-indigo;
  font-size: $text-xs;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.3);
  }
}

// ===== 进度条 =====
.progress-track {
  height: 4px;
  background: $bg-muted;
  border-radius: $radius-xs;
  overflow: hidden;
  margin-bottom: $space-3;
}

.progress-fill {
  height: 100%;
  background: $gradient-brand;
  border-radius: $radius-xs;
  transition: width 0.8s ease;
}

.overview-stats {
  display: flex;
  align-items: center;
  gap: $space-3;
  font-size: $text-xs;
  color: $text-muted;
}

.stat-divider {
  opacity: 0.3;
}

// ===== 加载 =====
.loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $space-16 $space-5;
  gap: $space-3;
  color: $text-muted;
  font-size: $text-sm;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba($accent-indigo, 0.15);
  border-top-color: $accent-indigo;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// ===== 阶段卡片 =====
.phases-container {
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.phase-card {
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
  padding: $space-5 $space-6;
  box-shadow: $shadow-sm;

  &:has(.phase-badge.in_progress) {
    border-left: 3px solid $accent-indigo;
  }
}

.phase-header {
  margin-bottom: $space-4;
}

.phase-title-row {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.phase-icon {
  font-size: 1rem;
}

.phase-title {
  font-size: $text-md;
  font-weight: 700;
  color: $text-primary;
  flex: 1;
}

.phase-badge {
  font-size: $text-xs;
  font-weight: 600;
  padding: 2px $space-3;
  border-radius: $radius-full;

  &.pending {
    background: rgba($text-muted, 0.1);
    color: $text-muted;
  }

  &.in_progress {
    background: rgba($accent-indigo, 0.1);
    color: $accent-indigo;
  }

  &.completed {
    background: rgba($color-success, 0.1);
    color: $color-success;
  }
}

.phase-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-2;
}

.phase-task-count {
  font-size: $text-xs;
  color: $text-muted;
}

.phase-progress-label {
  font-size: $text-xs;
  font-weight: 700;
  color: $accent-indigo;
}

.phase-progress-row {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.phase-progress-track {
  flex: 1;
  height: 6px;
  background: $bg-muted;
  border-radius: $radius-sm;
  overflow: hidden;
}

.phase-progress-fill {
  height: 100%;
  background: $gradient-brand;
  border-radius: $radius-sm;
  transition: width 0.6s ease;
}

.phase-progress-text {
  font-size: $text-sm;
  font-weight: 700;
  color: $accent-indigo;
  min-width: 36px;
  text-align: right;
}

// ===== 周容器 =====
.weeks-container {
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.week-section {
  background: rgba($bg-muted, 0.3);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  padding: $space-4 $space-5;
}

.week-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-2;
}

.week-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-secondary;
}

.week-progress-label {
  font-size: $text-xs;
  font-weight: 600;
  color: $accent-indigo;
}

.week-progress-track {
  height: 4px;
  background: $bg-muted;
  border-radius: $radius-xs;
  overflow: hidden;
  margin-bottom: $space-3;
}

.week-progress-fill {
  height: 100%;
  background: $accent-indigo;
  border-radius: $radius-xs;
  transition: width 0.5s ease;
}

// ===== 任务列表 =====
.tasks-container {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.task-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-2 $space-3;
  border-radius: $radius-md;
  transition: background $transition-fast;

  &:hover {
    background: rgba($bg-muted, 0.5);
  }

  &.task-current {
    background: rgba($accent-indigo, 0.08);
    border-left: 3px solid $accent-indigo;
    border-radius: 0 $radius-md $radius-md 0;
  }

  &.task-flash {
    animation: taskFlash 1.5s ease;
  }
}

@keyframes taskFlash {
  0% { box-shadow: 0 0 0 0 rgba($accent-indigo, 0.5); }
  70% { box-shadow: 0 0 0 10px rgba($accent-indigo, 0); }
  100% { box-shadow: 0 0 0 0 rgba($accent-indigo, 0); }
}

.task-current-badge {
  font-size: $text-xs;
  font-weight: 700;
  color: $accent-indigo;
  margin-right: $space-2;
}

.task-weakness-badge {
  display: inline-block;
  width: fit-content;
  padding: 2px $space-2;
  border-radius: $radius-full;
  font-size: $text-xs;
  font-weight: 600;
  color: $color-danger;
  border: 1px solid rgba($color-danger, 0.2);
  background: rgba($color-danger, 0.08);
}

.task-continue-btn {
  padding: $space-1 $space-3;
  font-size: $text-xs;
  font-weight: 500;
  color: $accent-indigo;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.2);
  border-radius: $radius-sm;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.3s ease;

  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.35);
  }
}

.task-status-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $text-sm;
  flex-shrink: 0;
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.task-title {
  font-size: $text-sm;
  font-weight: 500;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &.completed {
    text-decoration: line-through;
    color: $text-muted;
  }
}

.task-meta {
  font-size: $text-xs;
  color: $text-muted;
}

.task-complete-btn {
  padding: $space-1 $space-3;
  font-size: $text-xs;
  font-weight: 600;
  color: $accent-indigo;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: $radius-sm;
  cursor: pointer;
  white-space: nowrap;
  transition: all $transition-fast;
  flex-shrink: 0;

  &:hover:not(:disabled) {
    background: rgba($accent-indigo, 0.12);
    border-color: rgba($accent-indigo, 0.25);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

// ===== 空状态 =====
.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $space-16 $space-5;
  text-align: center;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: $space-3;
  opacity: 0.4;
}

.empty-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $space-1;
}

.empty-desc {
  font-size: $text-sm;
  color: $text-secondary;
  margin-bottom: $space-5;
}

.generate-outline-btn {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-3 $space-6;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  color: $accent-indigo;
  font-size: $text-sm;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover:not(:disabled) {
    background: $bg-muted;
    border-color: $border-medium;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.outline-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba($accent-indigo, 0.2);
  border-top-color: $accent-indigo;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

// ===== 全部完成横幅 =====
.all-done-banner {
  display: flex;
  align-items: center;
  gap: $space-3;
  flex-wrap: wrap;
  padding: $space-5 $space-5;
  margin-bottom: $space-4;
  background: rgba($color-success, 0.08);
  border: 1px solid rgba($color-success, 0.2);
  border-radius: $radius-lg;
}

.all-done-icon {
  font-size: 1.4rem;
}

.all-done-text {
  flex: 1;
  font-size: $text-md;
  font-weight: 600;
  color: $color-success;
}

.all-done-btn {
  padding: $space-2 $space-4;
  background: rgba($color-success, 0.12);
  border: 1px solid rgba($color-success, 0.25);
  border-radius: $radius-md;
  color: $color-success;
  font-size: $text-xs;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($color-success, 0.2);
  }
}

// ===== 文档内容 =====
.content-card {
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
  overflow: hidden;
}

.content-header {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-4 $space-5;
  border-bottom: 1px solid $border-default;
}

.content-title-icon {
  font-size: 1rem;
}

.content-title-text {
  font-size: $text-md;
  font-weight: 600;
  color: $text-primary;
}

.content-meta {
  margin-left: auto;
  font-size: $text-xs;
  color: $text-muted;
}

.content-body {
  padding: $space-6 $space-8;
  min-height: 200px;
}

// ===== 底部操作栏 =====
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-3;
  padding: $space-4 $space-8;
  background: $bg-surface;
  border-top: 1px solid $border-default;
}

.btn-primary {
  padding: $space-2 $space-6;
  background: rgba($accent-indigo, 0.12);
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: $font-sans;

  &:hover {
    background: rgba($accent-indigo, 0.2);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
}

.btn-secondary {
  padding: $space-2 $space-6;
  background: rgba($bg-surface, 0.6);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: $font-sans;

  &:hover {
    background: rgba($accent-indigo, 0.08);
    border-color: rgba($accent-indigo, 0.2);
    color: $accent-indigo-light;
    transform: translateY(-1px);
  }
}

// ===== Markdown 渲染 =====
:deep(.markdown-body) {
  h1, h2, h3, h4 {
    color: $text-primary;
    font-weight: 700;
    margin: $space-4 0 $space-2;
    line-height: $leading-tight;
  }

  h1 { font-size: $text-2xl; }

  h2 {
    font-size: $text-xl;
    color: $accent-indigo;
    border-bottom: 1px solid $border-default;
    padding-bottom: $space-2;
  }

  h3 { font-size: $text-lg; }

  p {
    margin: $space-2 0;
    line-height: $leading-relaxed;
    color: $text-secondary;
  }

  ul, ol {
    padding-left: $space-6;
    margin: $space-2 0;
  }

  li {
    margin: $space-1 0;
    line-height: $leading-normal;
    color: $text-secondary;
  }

  strong {
    color: $text-primary;
    font-weight: 700;
  }

  em {
    color: $accent-violet;
    font-style: italic;
  }

  code {
    background: rgba($accent-indigo, 0.08);
    border: 1px solid rgba($accent-indigo, 0.12);
    border-radius: $radius-xs;
    padding: 1px $space-2;
    font-size: 0.88em;
    color: $accent-indigo-light;
    font-family: $font-mono;
  }

  pre {
    background: $bg-muted;
    border: 1px solid $border-default;
    border-radius: $radius-md;
    padding: $space-4 $space-5;
    margin: $space-4 0;
    overflow-x: auto;
    color: $text-primary;
    position: relative;

    code {
      background: transparent;
      border: none;
      padding: 0;
      color: $text-primary;
      font-size: $text-sm;
      line-height: $leading-relaxed;
    }
  }

  blockquote {
    border-left: 3px solid rgba($accent-indigo, 0.3);
    margin: $space-3 0;
    padding: $space-2 $space-5;
    background: rgba($accent-indigo, 0.03);
    border-radius: 0 $radius-md $radius-md 0;
    color: $text-secondary;
  }

  a {
    color: $accent-indigo;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  table {
    border-collapse: collapse;
    width: 100%;
    margin: $space-3 0;

    th, td {
      border: 1px solid $border-default;
      padding: $space-2 $space-3;
      text-align: left;
    }

    th {
      background: rgba($accent-indigo, 0.05);
      color: $accent-indigo;
    }
  }

  hr {
    border: none;
    border-top: 1px solid $border-default;
    margin: $space-4 0;
  }

  img {
    max-width: 100%;
    border-radius: $radius-md;
  }
}

// ===== 更多菜单 =====
.more-menu-wrapper {
  position: relative;
}

.more-menu {
  position: absolute;
  top: calc(100% + $space-2);
  right: 0;
  min-width: 140px;
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: $space-2;
  box-shadow: $shadow-lg;
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
  gap: $space-2;
  width: 100%;
  padding: $space-3 $space-3;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  color: $text-secondary;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  text-align: left;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }

  &.danger {
    color: $color-danger;

    &:hover {
      background: rgba($color-danger, 0.1);
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
  background: $bg-overlay;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.delete-modal {
  width: 90%;
  max-width: 420px;
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
  padding: $space-8;
  box-shadow: $shadow-xl;
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.delete-modal-header {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-5;
}

.delete-modal-icon {
  color: $color-warning;
  flex-shrink: 0;
}

.delete-modal-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.delete-modal-content {
  font-size: $text-base;
  color: $text-secondary;
  line-height: $leading-relaxed;
  margin: 0 0 $space-8;
}

.delete-modal-name {
  color: $accent-indigo;
  font-weight: 500;
}

.delete-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: $space-3;
}

.btn-cancel {
  padding: $space-3 $space-6;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: $text-base;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $bg-muted;
    border-color: $border-medium;
    color: $text-primary;
  }
}

.btn-confirm-delete {
  padding: $space-3 $space-6;
  background: rgba($color-danger, 0.12);
  border: 1px solid rgba($color-danger, 0.25);
  border-radius: $radius-md;
  color: $color-danger;
  font-size: $text-base;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover:not(:disabled) {
    background: rgba($color-danger, 0.2);
    border-color: rgba($color-danger, 0.4);
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
  gap: $space-5;
  padding: $space-5 $space-6;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all $transition-normal;

  &:hover {
    background: $bg-elevated;
    border-color: $border-medium;
    transform: translateY(-2px);
    box-shadow: $shadow-md;
  }

  .guide-icon {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba($accent-indigo, 0.1);
    border-radius: $radius-lg;
    color: $accent-indigo;
    flex-shrink: 0;
  }

  .guide-content {
    flex: 1;
    min-width: 0;
  }

  .guide-title {
    font-size: $text-md;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: $space-1;
  }

  .guide-desc {
    font-size: $text-sm;
    color: $text-secondary;
  }

  .guide-arrow {
    color: $text-muted;
    flex-shrink: 0;
    transition: transform $transition-fast;
  }

  .guide-arrow-btn {
    padding: $space-2 $space-4;
    background: rgba($accent-indigo, 0.1);
    border: 1px solid rgba($accent-indigo, 0.25);
    border-radius: $radius-md;
    color: $accent-indigo;
    font-size: $text-sm;
    font-weight: 500;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.3s ease;

    &:hover {
      background: rgba($accent-indigo, 0.18);
      border-color: rgba($accent-indigo, 0.4);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
    }
  }

  &:hover .guide-arrow {
    transform: translateX(4px);
    color: $accent-indigo;
  }
}

// ===== 响应式 =====
@media (max-width: $breakpoint-md) {
  .nav-bar {
    padding: $space-3 $space-4;
  }

  .detail-container {
    padding: $space-4 $space-4 calc($space-16 * 2 + 30px);
  }

  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: $space-3;
  }

  .phase-card {
    padding: $space-4;
  }

  .content-body {
    padding: $space-4 $space-5;
  }

  .action-bar {
    padding: $space-3 $space-4;
  }
}
</style>