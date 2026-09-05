<template>
  <div class="session-panel">
    <!-- 阶段进度条 -->
    <div class="sp-stepper">
      <div
        v-for="(step, i) in steps"
        :key="step.id"
        class="sp-step"
        :class="{
          completed: i < reachedPhaseIndex && i !== stepIndex,
          active: stepIndex === i,
          reached: i === reachedPhaseIndex && i !== stepIndex,
          pending: i > reachedPhaseIndex
        }"
        @click="handleStepClick(step.id)"
        :title="step.hint"
      >
        <span class="sp-step-icon">{{ step.icon }}</span>
        <span class="sp-step-label">{{ step.label }}</span>
        <span class="sp-step-status">{{ getStepStatus(i) }}</span>
      </div>
    </div>

    <!-- 当前阶段提示 + 已完成阶段概览 -->
    <div class="sp-phase-hint">
      <span class="sph-icon">{{ currentStep?.icon }}</span>
      <div class="sph-info">
        <span class="sph-title">{{ currentStep?.label }}阶段</span>
        <span class="sph-desc">{{ currentStep?.hint }}</span>
      </div>
      <span v-if="!isLastStep" class="sph-next">
        下一步：{{ nextStep?.label }}
        <span class="sph-next-icon">{{ nextStep?.icon }}</span>
      </span>
      <span v-else class="sph-next">当前阶段已完成所有步骤</span>
      <!-- 操作按钮组 -->
      <div class="sph-actions">
        <button class="sph-action-btn sph-pause-btn" @click="handlePauseSession" title="暂停学习，保留当前进度">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <rect x="6" y="4" width="4" height="16" rx="1"/>
            <rect x="14" y="4" width="4" height="16" rx="1"/>
          </svg>
          <span>暂停</span>
        </button>
        <button class="sph-action-btn sph-back-btn" @click="goBackToWorkbench" title="返回学习工作台">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          <span>返回</span>
        </button>
      </div>
    </div>

    <!-- 已完成阶段摘要卡片 -->
    <div v-if="completedPhases.length > 0" class="sp-completed-summary">
      <div
        v-for="phase in completedPhases"
        :key="phase.id"
        class="sp-completed-chip"
        :title="phase.label"
      >
        <span class="scc-icon">{{ phase.icon }}</span>
        <span class="scc-label">{{ phase.label }}</span>
        <span class="scc-check">✓</span>
      </div>
    </div>

    <!-- 阶段内容（根据当前阶段动态渲染） -->
    <div class="sp-content">
      <DiagnosisQuizCard
        v-if="currentPhase === 'diagnosis'"
        :session="session"
        @answer="handleAnswer"
        @complete="handleDiagnosisComplete"
      />

      <PlanPreviewCard
        v-else-if="currentPhase === 'planning'"
        :session="session"
        @confirm="handlePlanConfirm"
        @adjust="handlePlanAdjust"
      />

      <LearningDayCard
        v-else-if="currentPhase === 'learning'"
        :session="session"
        @ask="handleAsk"
        @complete="handleTaskComplete"
      />

      <ExerciseCard
        v-else-if="currentPhase === 'exercise'"
        :session="session"
        @submit="handleExerciseSubmit"
      />

      <ReportCard
        v-else-if="currentPhase === 'report'"
        :session="session"
      />
    </div>

    <!-- 上一步 / 下一步 导航 -->
    <div class="sp-step-nav">
      <button
        class="sp-step-nav-btn sp-step-prev"
        :disabled="stepIndex <= 0"
        @click="handlePrevStep"
        title="上一步"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        <span>上一步</span>
      </button>
      <div class="sp-step-nav-info">
        <span class="sp-step-nav-label">{{ currentStep?.label }}</span>
        <span class="sp-step-nav-count">{{ stepIndex + 1 }} / {{ steps.length }}</span>
      </div>
      <button
        class="sp-step-nav-btn sp-step-next"
        :disabled="stepIndex >= steps.length - 1 || stepIndex + 1 > reachedPhaseIndex"
        @click="handleNextStep"
        title="下一步"
      >
        <span>下一步</span>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <path d="M5 12h14M12 5l7 7-7 7"/>
        </svg>
      </button>
    </div>

    <!-- 底部提示：使用全局 AI 助手 -->
    <div class="sp-chat-hint">
      <span class="sp-chat-hint-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
          <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
        </svg>
      </span>
      <span>需要帮助？按 <kbd>Ctrl+K</kbd> 或点击右下角打开 AI 助手</span>
    </div>

    <!-- 暂停遮罩层 -->
    <div v-if="sessionPaused" class="sp-paused-overlay">
      <div class="sp-paused-card">
        <div class="sp-paused-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="48" height="48">
            <rect x="6" y="4" width="4" height="16" rx="1"/>
            <rect x="14" y="4" width="4" height="16" rx="1"/>
          </svg>
        </div>
        <h3 class="sp-paused-title">学习已暂停</h3>
        <p class="sp-paused-desc">当前进度已保存，你可以随时恢复学习</p>
        <div class="sp-paused-actions">
          <button class="sp-paused-btn sp-paused-resume" @click="handleResumeSession">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
            <span>恢复学习</span>
          </button>
          <button class="sp-paused-btn sp-paused-exit" @click="goBackToWorkbench">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            <span>返回工作台</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import DiagnosisQuizCard from './DiagnosisQuizCard.vue'
import PlanPreviewCard from './PlanPreviewCard.vue'
import LearningDayCard from './LearningDayCard.vue'
import ExerciseCard from './ExerciseCard.vue'
import ReportCard from './ReportCard.vue'
import { submitSessionAnswer, submitPhaseData, connectSessionStream } from '@/api/sessionApi'

const router = useRouter()
const route = useRoute()
const activeSession = inject('activeSession', ref(null))
const sessions = inject('sessions', ref([]))
const setActiveSessionId = inject('setActiveSessionId', (id) => {})

const session = computed(() => activeSession.value || {})

const steps = [
  { id: 'diagnosis', label: '诊断', icon: '🔍', hint: 'AI 通过问答评估你的当前知识水平，找出薄弱环节' },
  { id: 'planning', label: '规划', icon: '🗺️', hint: '基于诊断结果，AI 为你制定个性化学习路径' },
  { id: 'learning', label: '学习', icon: '📖', hint: '按照计划逐步学习，可随时向 AI 提问解惑' },
  { id: 'exercise', label: '习题', icon: '✏️', hint: '完成 AI 生成的练习题，巩固所学知识' },
  { id: 'report', label: '报告', icon: '📊', hint: '查看学习数据报告，了解进步轨迹和学习成果' }
]

const currentPhase = computed(() => session.value.phase || 'diagnosis')

const stepIndex = computed(() => {
  const idx = steps.findIndex(s => s.id === currentPhase.value)
  return idx >= 0 ? idx : 0
})

const currentStep = computed(() => steps[stepIndex.value])

const nextStep = computed(() => {
  return stepIndex.value < steps.length - 1 ? steps[stepIndex.value + 1] : null
})

const isLastStep = computed(() => stepIndex.value >= steps.length - 1)

// 计算已完成阶段
const completedPhases = computed(() => {
  return steps.filter((_, i) => i < reachedPhaseIndex.value)
})

const chatInput = ref('')

const getStepStatus = (i) => {
  if (i < reachedPhaseIndex.value) return '✓'
  if (i === reachedPhaseIndex.value && i !== stepIndex.value) return '●'
  if (i === stepIndex.value) return '●'
  return '○'
}

const sseClient = ref(null)
const sessionQuestions = ref([])
const reconnectCount = ref(0)
const maxReconnectAttempts = 3
const sessionCompleted = ref(false) // 会话已完成标记，防止完成后再重连
const sessionPaused = ref(false) // 会话暂停标记
const isFinished = ref(false) // ★ 最终结束标志，一旦设为 true，永久阻断所有重连

// 记录用户已到达的最高阶段索引（用于导航控制）
const reachedPhaseIndex = ref(0)

// ===== SSE 连接管理 =====
const connectPhaseSSE = () => {
  // ★ 如果已经结束，直接返回，绝不再次连接
  if (isFinished.value || sessionCompleted.value) {
    console.log('[SessionPanel] 会话已结束，阻断 SSE 连接')
    return
  }

  const sessionId = session.value?.id
  const phase = currentPhase.value
  if (!sessionId || !phase) return

  // 如果处于暂停状态，不重新连接
  if (sessionPaused.value) return

  // 断开旧连接
  if (sseClient.value) {
    sseClient.value.close()
    sseClient.value = null
  }

  // 如果是已完成阶段或会话已结束，不需要连接 SSE
  const stepIdx = steps.findIndex(s => s.id === phase)
  if (stepIdx < 0 || sessionCompleted.value) return

  // 重置重连计数
  reconnectCount.value = 0

  sseClient.value = connectSessionStream(sessionId, phase, {
    onQuestions: (questions) => {
      sessionQuestions.value = questions
      if (session.value) {
        session.value.questions = questions
      }
    },
    onPhaseData: (data) => {
      // 接收各阶段推送的数据（plan / report / exercises / tasks 等）
      console.log('阶段数据:', data)
      if (!session.value) return
      const phase = data.phase
      if (phase === 'planning' && data.plan) {
        session.value.plan = data.plan
      } else if (phase === 'learning' && data.tasks) {
        // 统一使用 tasks 字段，与 LearningDayCard 读取的 props.session.tasks 一致
        session.value.tasks = data.tasks
        session.value.learningMessage = data.message
      } else if (phase === 'exercise' && data.exercises) {
        session.value.exercises = data.exercises
      } else if (phase === 'report' && data.report) {
        session.value.report = data.report
      }
    },
    onAnswerFeedback: (data) => {
      console.log('答案反馈:', data)
    },
    onPhaseResult: (phaseName, data) => {
      console.log('阶段完成:', phaseName, data)
      // 仅保存结果数据，不做阶段推进
      // 阶段推进由 onPhaseTransition 事件驱动
      if (!session.value) return

      // 如果是报告阶段，保存报告数据
      if (phaseName === 'report' && data.report) {
        session.value.report = data.report
      }
    },
    onPhaseTransition: (from, to, message) => {
      console.log('阶段过渡:', from, '->', to, message)
      if (!session.value) return

      // ★ 防护：from === to 时不触发重连，避免同阶段死循环（如 report→report）
      if (from === to) {
        console.log('同阶段过渡，跳过重连')
        return
      }

      // 由后端驱动的阶段过渡事件来更新当前阶段
      session.value.phase = to
      // 更新 sessions 列表中的记录
      const idx = sessions.value.findIndex(s => s.id === sessionId)
      if (idx >= 0) {
        sessions.value[idx] = { ...sessions.value[idx], ...session.value }
      }

      // 更新已到达的最高阶段索引
      const toIdx = steps.findIndex(s => s.id === to)
      if (toIdx > reachedPhaseIndex.value) {
        reachedPhaseIndex.value = toIdx
      }

      // 直接在此处重连 SSE，避免依赖 watch 触发的延迟
      // 使用 setTimeout 确保当前事件处理完成后才重连
      setTimeout(() => {
        connectPhaseSSE()
      }, 100)
    },
    onSessionComplete: (data) => {
      console.log('会话完成:', data)
      // ★ 设置最终结束标志，永久阻断所有重连路径
      isFinished.value = true
      sessionCompleted.value = true
      // ★ 立即关闭 EventSource，阻止浏览器内置自动重连机制
      //   EventSource 在连接关闭后会自动重试（5-6秒间隔），必须主动 close() 才能终止
      if (sseClient.value) {
        sseClient.value.close(true) // 传递 true 标记为完成关闭
        sseClient.value = null
      }
      if (session.value && data) {
        // data 是完整事件数据: { sessionId, report, message }
        if (data.report) {
          session.value.report = data.report
        }
        session.value.phase = 'report'
        session.value.status = 'completed'
      }
    },
    onError: (data) => {
      console.error('SSE 错误:', data)
      // ★ 如果会话已结束，不再重连
      if (isFinished.value || sessionCompleted.value) {
        console.log('[SessionPanel] 会话已结束，不重连')
        return
      }
      // 限制自动重连次数
      if (reconnectCount.value >= maxReconnectAttempts) {
        console.warn('SSE 自动重连已达上限，请手动刷新')
        return
      }
      // 如果是连接错误，尝试自动重连
      const currentPhase = session.value?.phase
      const sessionId = session.value?.id
      if (currentPhase && sessionId && currentPhase !== 'report') {
        reconnectCount.value++
        console.log(`SSE 连接错误，5秒后自动重连 (${reconnectCount.value}/${maxReconnectAttempts})...`)
        setTimeout(() => {
          connectPhaseSSE()
        }, 5000)
      }
    },
    onAuthExpired: (data) => {
      console.warn('SSE 认证过期:', data.message)
      // Token 过期，跳转登录页
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
  })
}

const getNextPhase = (currentPhase) => {
  const idx = steps.findIndex(s => s.id === currentPhase)
  return idx >= 0 && idx < steps.length - 1 ? steps[idx + 1].id : null
}

// 监听阶段变化，自动重连 SSE
watch(currentPhase, (newPhase, oldPhase) => {
  if (newPhase && newPhase !== oldPhase) {
    // ★ 如果会话已结束，不再重连
    if (isFinished.value || sessionCompleted.value) {
      console.log('[SessionPanel] 会话已结束，watch 阻断重连')
      return
    }
    connectPhaseSSE()
  }
})

// 监听路由变化（组件复用时重新连接 SSE）
watch(() => route.path, () => {
  // ★ 如果会话已结束，不再重连
  if (isFinished.value || sessionCompleted.value) {
    console.log('[SessionPanel] 会话已结束，路由 watch 阻断重连')
    return
  }
  connectPhaseSSE()
})

// 组件挂载时连接 SSE
onMounted(() => {
  // 如果会话已结束，不再连接
  if (isFinished.value || sessionCompleted.value) return
  // 初始化已到达的最高阶段索引
  const initIdx = steps.findIndex(s => s.id === currentPhase.value)
  if (initIdx > reachedPhaseIndex.value) {
    reachedPhaseIndex.value = initIdx
  }
  connectPhaseSSE()
})

// 组件卸载时断开 SSE
onUnmounted(() => {
  // ★ 组件销毁时必须断开连接并标记结束
  isFinished.value = true
  sessionCompleted.value = true
  if (sseClient.value) {
    sseClient.value.close(true)
    sseClient.value = null
  }
})

// ===== 阶段事件处理 =====
const handleAnswer = async (questionId, answer) => {
  try {
    await submitSessionAnswer(session.value.id, questionId, answer)
  } catch (e) {
    console.error('提交答案失败:', e.message)
  }
}

const handleDiagnosisComplete = async () => {
  // 提交诊断完成信号到后端，后端 SSE 流检测到阶段完成时会推送阶段结果和阶段过渡事件
  try {
    await submitPhaseData(session.value.id, {
      type: 'diagnosis_complete',
      status: 'completed',
      completedAt: new Date().toISOString()
    })
    console.log('诊断阶段完成，等待后端推送下一阶段...')
  } catch (e) {
    console.error('提交诊断完成失败:', e.message)
  }
}

const handleStepClick = (phaseId) => {
  console.log('切换到阶段:', phaseId)
  // 允许跳转到已到达的任何阶段（包括当前阶段）
  const stepIdx = steps.findIndex(s => s.id === phaseId)
  if (stepIdx < 0 || stepIdx > reachedPhaseIndex.value) return

  // 如果已经是当前阶段，不做任何操作
  if (stepIdx === stepIndex.value) return

  // 更新当前阶段并重连 SSE
  if (session.value) {
    session.value.phase = phaseId
    connectPhaseSSE()
  }
}

const handlePrevStep = () => {
  const prevIdx = stepIndex.value - 1
  if (prevIdx >= 0) {
    handleStepClick(steps[prevIdx].id)
  }
}

const handleNextStep = () => {
  const nextIdx = stepIndex.value + 1
  if (nextIdx < steps.length && nextIdx <= reachedPhaseIndex.value) {
    handleStepClick(steps[nextIdx].id)
  }
}

const handlePlanConfirm = async () => {
  // 确认规划，标记完成
  try {
    await submitPhaseData(session.value.id, {
      type: 'phase_complete',
      status: 'completed',
      completedAt: new Date().toISOString()
    })
    console.log('规划阶段完成')
  } catch (e) {
    console.error('提交规划完成失败:', e.message)
  }
}

const handlePlanAdjust = () => {
  // 调整规划
  console.log('调整规划')
}

const handleAsk = (question) => {
  // 发送问题给 AI
  console.log('提问:', question)
}

const handleTaskComplete = async () => {
  // 完成当前任务，标记学习阶段完成
  try {
    await submitPhaseData(session.value.id, {
      type: 'phase_complete',
      status: 'completed',
      completedAt: new Date().toISOString()
    })
    console.log('学习阶段完成')
  } catch (e) {
    console.error('提交学习完成失败:', e.message)
  }
}

const handleExerciseSubmit = async (answers) => {
  // 提交习题答案并标记完成
  try {
    await submitPhaseData(session.value.id, {
      type: 'phase_complete',
      status: 'completed',
      answers,
      completedAt: new Date().toISOString()
    })
    console.log('习题阶段完成')
  } catch (e) {
    console.error('提交习题完成失败:', e.message)
  }
}

// ===== 导航 =====
const goBackToWorkbench = () => {
  // 断开 SSE 连接并标记结束
  isFinished.value = true
  sessionCompleted.value = true
  if (sseClient.value) {
    sseClient.value.close(true)
    sseClient.value = null
  }
  // 清除当前活动会话，返回工作台首页
  setActiveSessionId(null)
  router.push('/workbench')
}

const handlePauseSession = () => {
  // 暂停当前会话，停留在当前页面，不跳转
  if (sseClient.value) {
    sseClient.value.close()
    sseClient.value = null
  }
  // 标记暂停状态
  sessionPaused.value = true
  // 将会话状态标记为 paused（通过 API）
  if (session.value?.id) {
    submitPhaseData(session.value.id, {
      type: 'pause',
      status: 'paused',
      phase: session.value.phase,
      pausedAt: new Date().toISOString()
    }).catch(e => console.warn('暂停标记失败:', e.message))
  }
}

const handleResumeSession = () => {
  // 恢复会话，重新连接 SSE
  sessionPaused.value = false
  connectPhaseSSE()
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.session-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  position: relative;
}

// ===== 阶段进度条 =====
.sp-stepper {
  display: flex;
  gap: $space-1;
  padding: $space-4 $space-6;
  background: rgba($bg-surface, 0.5);
  border-bottom: 1px solid $border-subtle;
}

.sp-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: $space-2;
  border-radius: $radius-md;
  transition: all $transition-fast;
  position: relative;
  cursor: default;

  &.completed { cursor: pointer; }
  &.active { cursor: default; }
  &.pending { cursor: default; }

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    right: -50%;
    width: 100%;
    height: 1px;
    background: $border-subtle;
    z-index: -1;
  }

  &:last-child::after { display: none; }

  &.completed {
    .sp-step-icon { opacity: 0.5; }
    .sp-step-status { color: $color-success; }
  }

  &.active {
    background: rgba($accent-indigo, 0.1);
    .sp-step-label { color: $accent-indigo; font-weight: 600; }
    .sp-step-status { color: $accent-indigo; }
  }

  &.pending {
    opacity: 0.5;
  }

  &.reached {
    cursor: pointer;
    .sp-step-icon { opacity: 0.7; }
    .sp-step-status { color: $color-success; }
  }
}

.sp-step-icon { font-size: 1.1rem; }

.sp-step-label {
  font-size: $text-xs;
  color: $text-secondary;
  font-weight: 500;
}

.sp-step-status {
  font-size: 10px;
  color: $text-muted;
}

// ===== 当前阶段提示 =====
.sp-phase-hint {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-6;
  background: rgba($accent-indigo, 0.06);
  border-bottom: 1px solid $border-subtle;
}

.sph-icon {
  font-size: 1.3rem;
  flex-shrink: 0;
}

.sph-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
}

.sph-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
}

.sph-desc {
  font-size: 11px;
  color: $text-muted;
  line-height: 1.4;
}

// ===== 阶段提示栏操作按钮 =====
.sph-actions {
  display: flex;
  align-items: center;
  gap: $space-2;
  flex-shrink: 0;
}

.sph-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  background: rgba($bg-surface, 0.6);
  color: $text-secondary;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;
  white-space: nowrap;

  &:hover {
    background: rgba($bg-elevated, 0.8);
    border-color: $border-default;
    color: $text-primary;
  }

  &:active {
    transform: scale(0.97);
  }

  svg {
    flex-shrink: 0;
  }
}

.sph-pause-btn {
  &:hover {
    border-color: rgba($accent-indigo, 0.3);
    color: $accent-indigo;
    background: rgba($accent-indigo, 0.06);
  }
}

.sph-back-btn {
  &:hover {
    border-color: rgba($color-danger, 0.3);
    color: $color-danger;
    background: rgba($color-danger, 0.06);
  }
}

.sph-next {
  display: flex;
  align-items: center;
  gap: $space-1;
  font-size: 11px;
  color: $text-muted;
  white-space: nowrap;
  flex-shrink: 0;
}

.sph-next-icon {
  font-size: 1rem;
}

// ===== 已完成阶段摘要 =====
.sp-completed-summary {
  display: flex;
  gap: $space-2;
  padding: $space-2 $space-6;
  background: rgba($color-success, 0.03);
  border-bottom: 1px solid $border-subtle;
  flex-wrap: wrap;
}

.sp-completed-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px 3px 6px;
  background: rgba($color-success, 0.08);
  border: 1px solid rgba($color-success, 0.15);
  border-radius: $radius-full;
  font-size: 11px;
  color: $color-success;
  white-space: nowrap;
}

.scc-icon { font-size: 12px; }
.scc-label { font-weight: 500; }
.scc-check {
  font-weight: 700;
  font-size: 10px;
}

// ===== 内容区 =====
.sp-content {
  flex: 1;
  overflow-y: auto;
  padding: $space-6;
}

// ===== 底部 AI 提示 =====
.sp-chat-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-2 $space-6;
  border-top: 1px solid $border-subtle;
  background: rgba($bg-surface, 0.3);
  font-size: 11px;
  color: $text-muted;

  kbd {
    display: inline-flex;
    align-items: center;
    padding: 1px 6px;
    background: rgba($bg-elevated, 0.6);
    border: 1px solid $border-subtle;
    border-radius: 4px;
    font-family: $font-mono;
    font-size: 10px;
    color: $text-secondary;
  }
}

.sp-chat-hint-icon {
  display: flex;
  opacity: 0.6;
}

// ===== 上一步 / 下一步 导航 =====
.sp-step-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $space-4;
  padding: $space-3 $space-6;
  border-top: 1px solid $border-subtle;
  background: rgba($bg-surface, 0.4);
}

.sp-step-nav-btn {
  display: flex;
  align-items: center;
  gap: $space-1;
  padding: $space-2 $space-4;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  background: rgba($bg-surface, 0.7);
  color: $text-secondary;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &:hover:not(:disabled) {
    background: rgba($bg-elevated, 0.9);
    border-color: $border-default;
    color: $text-primary;
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }

  svg {
    flex-shrink: 0;
  }
}

.sp-step-prev {
  &:hover:not(:disabled) {
    border-color: rgba($accent-indigo, 0.3);
    color: $accent-indigo;
    background: rgba($accent-indigo, 0.06);
  }
}

.sp-step-next {
  &:hover:not(:disabled) {
    border-color: rgba($accent-teal, 0.3);
    color: $accent-teal;
    background: rgba($accent-teal, 0.06);
  }
}

.sp-step-nav-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.sp-step-nav-label {
  font-size: 12px;
  font-weight: 600;
  color: $text-primary;
}

.sp-step-nav-count {
  font-size: 10px;
  color: $text-muted;
  font-family: $font-mono;
}

// ===== 暂停遮罩层 =====
.sp-paused-overlay {
  position: absolute;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($bg-canvas, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  animation: spPausedFadeIn 0.3s ease;
}

@keyframes spPausedFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.sp-paused-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-4;
  padding: $space-10 $space-12;
  background: rgba($bg-surface, 0.95);
  border: 1px solid $border-subtle;
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
  max-width: 360px;
  text-align: center;
  animation: spPausedCardIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes spPausedCardIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.sp-paused-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba($accent-indigo, 0.1);
  color: $accent-indigo;
}

.sp-paused-title {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.sp-paused-desc {
  font-size: 13px;
  color: $text-muted;
  line-height: 1.5;
  margin: 0;
}

.sp-paused-actions {
  display: flex;
  flex-direction: column;
  gap: $space-2;
  width: 100%;
  margin-top: $space-2;
}

.sp-paused-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  width: 100%;
  padding: $space-3 $space-6;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;
  border: 1px solid transparent;
}

.sp-paused-resume {
  background: $accent-indigo;
  color: #fff;
  border-color: $accent-indigo;

  &:hover {
    background: darken($accent-indigo, 8%);
    border-color: darken($accent-indigo, 8%);
  }

  &:active {
    transform: scale(0.98);
  }
}

.sp-paused-exit {
  background: transparent;
  color: $text-secondary;
  border-color: $border-subtle;

  &:hover {
    background: rgba($color-danger, 0.06);
    border-color: rgba($color-danger, 0.3);
    color: $color-danger;
  }

  &:active {
    transform: scale(0.98);
  }
}

</style>