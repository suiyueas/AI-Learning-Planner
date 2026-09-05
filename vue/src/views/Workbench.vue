<template>
  <div class="workbench" :class="{ 'workbench--collapsed': sidebarCollapsed, 'orch-panel--open': orchestratorPanelOpen }">
    <!-- 左侧: 会话列表 -->
    <aside class="wb-sidebar" :class="{ 'wb-sidebar--collapsed': sidebarCollapsed }">
      <div class="wb-sidebar-header">
        <h3 v-if="!sidebarCollapsed" class="wb-sidebar-title">学习会话</h3>
        <button class="wb-new-btn" @click="handleCreateSession" title="新建会话">
          <Plus :size="sidebarCollapsed ? 16 : 18" />
          <span v-if="!sidebarCollapsed">新建</span>
        </button>
      </div>
      <div class="wb-session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="wb-session-item"
          :class="{ active: activeSessionId === session.id }"
          @click="switchSession(session.id)"
        >
          <span class="session-icon">{{ getSessionIcon(session.phase) }}</span>
          <div v-if="!sidebarCollapsed" class="session-info">
            <span class="session-name">{{ session.goal }}</span>
            <span class="session-status">{{ getPhaseLabel(session.phase) }} · {{ session.progress }}%</span>
          </div>
          <button
            v-if="!sidebarCollapsed"
            class="wb-session-del"
            title="删除会话"
            @click.stop="handleDeleteSession(session.id)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
            </svg>
          </button>
        </div>
        <div v-if="sessions.length === 0 && !sidebarCollapsed" class="wb-empty-hint">
          暂无会话
        </div>
      </div>
      <!-- 底部伸缩按钮 -->
      <div class="wb-sidebar-footer">
        <button class="wb-collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开' : '收起'">
          <ChevronLeft v-if="!sidebarCollapsed" :size="16" />
          <ChevronRight v-else :size="16" />
        </button>
      </div>
    </aside>
    <!-- 侧边栏伸缩按钮 (右侧边缘) -->
    <button class="wb-collapse-toggle" :class="{ 'wb-collapse-toggle--collapsed': sidebarCollapsed }" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
      <ChevronLeft v-if="!sidebarCollapsed" :size="16" />
      <ChevronRight v-else :size="16" />
    </button>

    <!-- 中间: 主内容区 -->
    <main class="wb-main">
      <router-view v-if="activeSessionId" />
      <WelcomeGuide v-else @start="handleCreateSession" />
    </main>

    <!-- 右侧: Orchestrator 编排对话面板 -->
    <aside class="wb-orchestrator-panel" :class="{ 'wb-orchestrator--open': orchestratorPanelOpen }">
      <!-- 面板伸缩标签（始终可见） -->
      <button class="wb-orch-tab" :class="{ 'wb-orch-tab--open': orchestratorPanelOpen }" @click="orchestratorPanelOpen = !orchestratorPanelOpen" :title="orchestratorPanelOpen ? '收起编排面板' : '打开编排面板'">
        <ChevronLeft v-if="!orchestratorPanelOpen" :size="16" />
        <ChevronRight v-else :size="16" />
        <span v-if="!orchestratorPanelOpen" class="wb-orch-tab-label">编排</span>
      </button>
      <div class="wb-orch-header">
        <div class="wb-orch-title">
          <span class="wb-orch-icon">🧠</span>
          <span>智能编排</span>
        </div>
        <button class="wb-orch-toggle" @click="orchestratorPanelOpen = !orchestratorPanelOpen" title="收起编排面板">
          <ChevronRight />
        </button>
      </div>
      <div class="wb-orch-messages" ref="orchestratorMessagesRef">
        <div v-if="orchestrationMessages.length === 0 && !isOrchestrating" class="wb-orch-empty">
          <p>在这里输入复杂任务，Orchestrator 会自动拆解任务，调用多个 Agent <strong>并行执行</strong>，最后聚合结果</p>
          <div class="wb-orch-hints">
            <span class="hint">示例：帮我规划一个 30 天的 Java 学习路线，并评估我的当前水平，最后生成练习题</span>
          </div>
        </div>
        <div v-for="msg in orchestrationMessages" :key="msg.id" class="wb-orch-message" :class="msg.type">
          <div class="wb-orch-msg-header">
            <span class="wb-orch-msg-icon">{{ getOrchestrationIcon(msg.type) }}</span>
            <span class="wb-orch-msg-title">{{ getOrchestrationTitle(msg) }}</span>
            <span v-if="msg.agentName" class="wb-orch-msg-agent">{{ msg.agentName }}</span>
          </div>
          <div v-if="msg.content" class="wb-orch-msg-content" v-html="formatOrchestrationContent(msg.content)"></div>
          <div v-if="msg.subTasks" class="wb-orch-subtasks">
            <div v-for="task in msg.subTasks" :key="task.agentId" class="wb-orch-subtask" :class="getSubTaskClass(task.agentId)">
              <span class="st-agent">{{ task.agentName }}</span>
              <span class="st-desc">{{ task.description }}</span>
              <span class="st-status" :class="subTaskStatusMap[task.agentId] || 'pending'">
                {{ getSubTaskStatusLabel(subTaskStatusMap[task.agentId]) }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="isOrchestrating" class="wb-orch-loading">
          <div class="spinner"></div>
          <span>编排执行中...</span>
        </div>
      </div>
      <div class="wb-orch-input-area">
        <input
          v-model="orchestratorInput"
          class="wb-orch-input"
          placeholder="输入复杂任务，Orchestrator 会自动多Agent调度..."
          @keydown.enter="sendOrchestration"
          :disabled="isOrchestrating"
        />
        <button class="wb-orch-send" @click="sendOrchestration" :disabled="isOrchestrating || !orchestratorInput.trim()">
          {{ isOrchestrating ? '执行中...' : '发送' }}
        </button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, provide, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import WelcomeGuide from './workbench/WelcomeGuide.vue'
import { listSessions, createSession, deleteSession } from '@/api/sessionApi'
import { postOrchestrateExecution } from '@/api/agentApi'

const router = useRouter()

const sessions = ref([])
const activeSessionId = ref(null)
const sidebarCollapsed = ref(true)

const activeSession = computed(() =>
  sessions.value.find(s => s.id === activeSessionId.value)
)

const setActiveSessionId = (id) => {
  activeSessionId.value = id
}

provide('activeSession', activeSession)
provide('sessions', sessions)
provide('setActiveSessionId', setActiveSessionId)

const phaseIcons = {
  diagnosis: '🔍',
  planning: '🗺️',
  learning: '📖',
  exercise: '✏️',
  report: '📊'
}

const phaseLabels = {
  diagnosis: '诊断中',
  planning: '规划中',
  learning: '学习中',
  exercise: '练习中',
  report: '报告中'
}

const getSessionIcon = (phase) => phaseIcons[phase] || '📋'
const getPhaseLabel = (phase) => phaseLabels[phase] || '进行中'

const switchSession = (id) => {
  activeSessionId.value = id
  const session = sessions.value.find(s => s.id === id)
  if (session) {
    router.push(`/workbench/${session.phase || 'active'}`)
  }
}

const handleCreateSession = async () => {
  // 通过对话框让用户输入学习目标，而不是传空字符串
  const goal = prompt('请输入你的学习目标：', '三个月学会 Python 数据分析')
  if (!goal || !goal.trim()) return

  try {
    const res = await createSession({ goal: goal.trim() })
    const data = res?.data ?? res
    // 处理后端统一响应包装 ApiResponse<LearningSessionDTO>
    const session = data?.data?.id ? data.data : (data?.id ? data : null)
    if (session) {
      sessions.value.unshift(session)
      switchSession(session.id)
    }
  } catch (e) {
    console.error('创建会话失败:', e.message)
  }
}

const handleDeleteSession = async (id) => {
  try {
    await deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (activeSessionId.value === id) {
      activeSessionId.value = null
      router.push('/workbench')
    }
  } catch (e) {
    console.error('删除会话失败:', e.message)
  }
}

onMounted(async () => {
  try {
    const res = await listSessions()
    const data = res?.data ?? res
    if (Array.isArray(data)) {
      sessions.value = data
    }
  } catch (e) {
    console.warn('加载会话列表失败:', e.message)
  }
})

// ===== Orchestrator 编排面板逻辑 =====
const orchestratorPanelOpen = ref(false)
const orchestratorInput = ref('')
const isOrchestrating = ref(false)
const orchestrationMessages = ref([])
const orchestratorMessagesRef = ref(null)
const subTaskStatusMap = ref({})
let messageIdCounter = 0

const getOrchestrationIcon = (type) => {
  switch (type) {
    case 'user': return '👤'
    case 'orchestration_start': return '🧠'
    case 'decomposition': return '📋'
    case 'subtask_start': return '⚡'
    case 'subtask_done': return '✅'
    case 'subtask_error': return '❌'
    case 'aggregating': return '🔄'
    case 'orchestration_done': return '🎯'
    case 'error': return '⚠️'
    default: return '💬'
  }
}

const getOrchestrationTitle = (msg) => {
  switch (msg.type) {
    case 'user': return '你的任务'
    case 'orchestration_start': return '🧠 Orchestrator 启动'
    case 'decomposition': return '📋 任务拆解'
    case 'subtask_start': return '⚡ 子任务执行'
    case 'subtask_done': return '✅ 子任务完成'
    case 'subtask_error': return '❌ 子任务失败'
    case 'aggregating': return '🔄 结果聚合'
    case 'orchestration_done': return '🎯 编排结果'
    case 'error': return '⚠️ 错误'
    default: return '💬 消息'
  }
}

const getSubTaskClass = (agentId) => {
  const status = subTaskStatusMap.value[agentId]
  return status === 'running' ? 'st-running' : status === 'done' ? 'st-done' : status === 'error' ? 'st-error' : ''
}

const getSubTaskStatusLabel = (status) => {
  switch (status) {
    case 'pending': return '⏳ 等待中'
    case 'running': return '🔄 执行中...'
    case 'done': return '✅ 完成'
    case 'error': return '❌ 失败'
    default: return '⏳ 等待中'
  }
}

const formatOrchestrationContent = (content) => {
  if (!content) return ''
  // 把 Markdown 风格的代码块和粗体进行简单转换
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
  return html
}

const addOrchestrationMessage = (type, data) => {
  const msg = { id: ++messageIdCounter, type, ...data, time: new Date().toLocaleTimeString() }
  orchestrationMessages.value.push(msg)
  scrollOrchestrationToBottom()
}

const scrollOrchestrationToBottom = () => {
  nextTick(() => {
    if (orchestratorMessagesRef.value) {
      orchestratorMessagesRef.value.scrollTop = orchestratorMessagesRef.value.scrollHeight
    }
  })
}

const sendOrchestration = async () => {
  const input = orchestratorInput.value.trim()
  if (!input || isOrchestrating.value) return

  // 添加用户消息
  addOrchestrationMessage('user', { content: input })
  orchestratorInput.value = ''

  // 如果面板未打开，自动打开
  if (!orchestratorPanelOpen.value) {
    orchestratorPanelOpen.value = true
  }

  // 开始编排
  isOrchestrating.value = true
  subTaskStatusMap.value = {}
  addOrchestrationMessage('orchestration_start', { content: `正在分析任务并拆解...` })

  try {
    await postOrchestrateExecution(input, {
      onDecomposition: (data) => {
        const subTasks = data.subTasks || []
        subTasks.forEach(t => {
          subTaskStatusMap.value[t.agentId] = 'pending'
        })
        addOrchestrationMessage('decomposition', {
          content: data.content || `已拆解为 ${subTasks.length} 个子任务`,
          subTasks
        })
      },
      onSubtaskStart: (data) => {
        subTaskStatusMap.value[data.agentId] = 'running'
        addOrchestrationMessage('subtask_start', {
          content: data.description || '',
          agentName: data.agentName,
          agentId: data.agentId
        })
      },
      onSubtaskDone: (data) => {
        subTaskStatusMap.value[data.agentId] = 'done'
        addOrchestrationMessage('subtask_done', {
          content: data.outputPreview || `${data.agentName} 执行完成`,
          agentName: data.agentName,
          agentId: data.agentId
        })
      },
      onSubtaskError: (data) => {
        subTaskStatusMap.value[data.agentId] = 'error'
        addOrchestrationMessage('subtask_error', {
          content: data.error || '执行失败',
          agentName: data.agentName,
          agentId: data.agentId
        })
      },
      onAggregating: (data) => {
        addOrchestrationMessage('aggregating', {
          content: data.content || '正在聚合多个 Agent 的结果...'
        })
      },
      onOrchestrationDone: (data) => {
        addOrchestrationMessage('orchestration_done', {
          content: data.content || '编排完成',
          stats: {
            total: data.subTaskCount,
            success: data.successCount,
            error: data.errorCount
          }
        })
        isOrchestrating.value = false
      },
      onError: (data) => {
        addOrchestrationMessage('error', {
          content: data.message || data.error || '编排执行失败'
        })
        isOrchestrating.value = false
      }
    })
  } catch (e) {
    addOrchestrationMessage('error', {
      content: `编排执行异常: ${e.message}`
    })
    isOrchestrating.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.workbench {
  display: flex;
  height: calc(100vh - $topbar-height);
  margin: -$space-6;
  overflow: visible;
  position: relative;
}

// ===== 左侧栏 =====
.wb-sidebar {
  width: 260px;
  background: rgba($bg-surface, 0.85);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid rgba($border-default, 0.4);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width $transition-normal;
  overflow: hidden;

  &--collapsed {
    width: 60px;
  }
}

.wb-sidebar-header {
  height: $topbar-height;
  padding: $space-4;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  border-bottom: 1px solid rgba($border-subtle, 0.5);
  flex-shrink: 0;

  // 展开状态：左对齐
  .wb-sidebar:not(.wb-sidebar--collapsed) & {
    justify-content: flex-start;
  }
}

.wb-sidebar-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
  flex: 1;
}

.wb-new-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-1;
  padding: 6px 12px;
  background: rgba($accent-indigo, 0.12);
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-xs;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;
  flex-shrink: 0;

  &:hover {
    background: rgba($accent-indigo, 0.2);
    border-color: rgba($accent-indigo, 0.4);
  }

  // 收缩状态：缩小并居中
  .wb-sidebar--collapsed & {
    padding: 4px;
    width: 32px;
    height: 32px;
    border-radius: $radius-sm;
  }
}

.wb-session-list {
  flex: 1;
  overflow-y: auto;
  padding: $space-2;
}

.wb-session-item {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: 10px $space-3;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  margin-bottom: 2px;
  position: relative;

  &:hover { background: rgba($accent-indigo, 0.06); }

  &.active {
    background: rgba($accent-indigo, 0.12);
    border-left: 2px solid $accent-indigo;
  }
}

.wb-session-del {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: none;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: $radius-sm;
  background: transparent;
  color: $text-muted;
  cursor: pointer;
  transition: all $transition-fast;
  padding: 0;

  &:hover {
    background: rgba($color-danger, 0.15);
    color: $color-danger;
  }
}

.wb-session-item:hover .wb-session-del {
  display: flex;
}

.session-icon { font-size: 1.1rem; flex-shrink: 0; }

.session-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.session-name {
  font-size: $text-sm;
  color: $text-primary;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-status {
  font-size: 11px;
  color: $text-muted;
}

.wb-empty-hint {
  text-align: center;
  padding: $space-8 $space-4;
  color: $text-muted;
  font-size: $text-sm;
}

/* 侧边栏右侧伸缩按钮 - 最佳实践设计 */
.wb-collapse-toggle {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translate(50%, -50%);
  z-index: 46;
  width: 20px;
  height: 56px;
  background: rgba($bg-surface, 0.95);
  border: 1px solid rgba($accent-indigo, 0.3);
  border-radius: $radius-md;
  color: $accent-indigo;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-fast;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
  opacity: 0.7;

  &:hover {
    opacity: 1;
    background: rgba($accent-indigo, 0.15);
    border-color: rgba($accent-indigo, 0.5);
    transform: translate(50%, -50%) scale(1.1);
    box-shadow: 0 4px 16px rgba($accent-indigo, 0.25);
  }

  &:active {
    transform: translate(50%, -50%) scale(0.95);
  }

  /* 收起状态 - 按钮贴在侧边栏右侧 */
  &.wb-collapse-toggle--collapsed {
    right: auto;
    left: 100%;
    transform: translate(50%, -50%);
    width: 24px;
    height: 64px;
    border-radius: 0 $radius-lg $radius-lg 0;
    background: rgba($accent-indigo, 0.2);
    border-color: rgba($accent-indigo, 0.4);
    opacity: 0.85;

    &:hover {
      width: 32px;
      background: rgba($accent-indigo, 0.3);
      border-color: rgba($accent-indigo, 0.6);
    }
  }
}

/* 侧边栏底部伸缩按钮（备用方案） */
.wb-sidebar-footer {
  padding: $space-2;
  border-top: 1px solid rgba($border-subtle, 0.5);
  flex-shrink: 0;
}

.wb-collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 36px;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.2);
  border-radius: $radius-md;
  color: $accent-indigo;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.35);
  }
}

// ===== 中间主内容 =====
.wb-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  height: 100%;
}

// ===== 右侧: Orchestrator 编排面板 =====
.wb-orchestrator-panel {
  width: 0;
  overflow: hidden;
  background: rgba($bg-surface, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-left: 1px solid rgba($border-default, 0.4);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width $transition-normal;
  position: relative;

  &.wb-orchestrator--open {
    width: 380px;
  }
}

/* 面板伸缩标签 - 始终固定在视图右侧边缘 */
.wb-orch-tab {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 51;
  width: 28px;
  height: 80px;
  background: rgba($accent-teal, 0.2);
  border: 1px solid rgba($accent-teal, 0.4);
  border-right: none;
  border-radius: $radius-md 0 0 $radius-md;
  color: $accent-teal;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all $transition-normal;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.2);
  opacity: 0.8;

  &:hover {
    opacity: 1;
    width: 32px;
    background: rgba($accent-teal, 0.3);
    border-color: rgba($accent-teal, 0.6);
    box-shadow: -4px 0 20px rgba($accent-teal, 0.25);
  }

  .wb-orch-tab-label {
    font-size: 10px;
    font-weight: 600;
    writing-mode: vertical-rl;
    letter-spacing: 2px;
  }

  /* 面板打开时，标签移动到面板左侧边缘 */
  &.wb-orch-tab--open {
    right: 380px;
    border-radius: 0 $radius-md $radius-md 0;
    border: 1px solid rgba($accent-teal, 0.4);
    border-left: none;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.2);
  }
}

.wb-orch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  border-bottom: 1px solid rgba($border-subtle, 0.5);
  flex-shrink: 0;
  height: 48px;
}

.wb-orch-title {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;

  .wb-orch-icon { font-size: 1.2rem; }
}

.wb-orch-toggle {
  background: none;
  border: none;
  color: $text-muted;
  cursor: pointer;
  padding: 4px;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover { color: $accent-indigo; background: rgba($accent-indigo, 0.08); }
}

.wb-orch-messages {
  flex: 1;
  overflow-y: auto;
  padding: $space-3;
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.wb-orch-empty {
  text-align: center;
  padding: $space-8 $space-4;
  color: $text-muted;
  font-size: $text-xs;
  line-height: 1.6;

  p { margin: 0 0 $space-4; }
  strong { color: $accent-indigo; }
}

.wb-orch-hints {
  .hint {
    display: block;
    font-size: $text-xs;
    color: $text-placeholder;
    background: rgba($accent-indigo, 0.05);
    border: 1px dashed rgba($accent-indigo, 0.15);
    border-radius: $radius-md;
    padding: $space-3;
    margin-top: $space-3;
  }
}

.wb-orch-message {
  background: $bg-elevated;
  border: 1px solid rgba($border-default, 0.3);
  border-radius: $radius-lg;
  padding: $space-3;
  animation: fadeInUp 0.3s ease;

  &.user {
    border-color: rgba($accent-indigo, 0.2);
    background: rgba($accent-indigo, 0.04);
  }

  &.orchestration_start {
    border-color: rgba($accent-teal, 0.2);
    background: rgba($accent-teal, 0.04);
  }

  &.decomposition {
    border-color: rgba($accent-amber, 0.2);
    background: rgba($accent-amber, 0.04);
  }

  &.subtask_start { border-left: 3px solid $accent-indigo; }
  &.subtask_done { border-left: 3px solid $accent-emerald; }
  &.subtask_error { border-left: 3px solid $accent-red; }
  &.aggregating { border-color: rgba($accent-teal, 0.2); }
  &.orchestration_done { border-color: rgba($accent-emerald, 0.3); background: rgba($accent-emerald, 0.04); }
  &.error { border-color: rgba($accent-red, 0.3); background: rgba($accent-red, 0.04); }
}

.wb-orch-msg-header {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-2;
}

.wb-orch-msg-icon { font-size: 1rem; }

.wb-orch-msg-title {
  font-size: $text-xs;
  font-weight: 600;
  color: $text-primary;
  flex: 1;
}

.wb-orch-msg-agent {
  font-size: 10px;
  color: $accent-indigo;
  background: rgba($accent-indigo, 0.08);
  padding: 2px 8px;
  border-radius: $radius-full;
}

.wb-orch-msg-content {
  font-size: $text-xs;
  color: $text-secondary;
  line-height: 1.6;

  code {
    background: rgba($accent-indigo, 0.08);
    border-radius: 3px;
    padding: 1px 5px;
    font-size: 11px;
    font-family: 'Fira Code', monospace;
  }
}

.wb-orch-subtasks {
  display: flex;
  flex-direction: column;
  gap: $space-2;
  margin-top: $space-3;
}

.wb-orch-subtask {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-3;
  background: rgba($bg-surface, 0.5);
  border-radius: $radius-md;
  border: 1px solid rgba($border-default, 0.2);
  font-size: $text-xs;

  .st-agent {
    font-weight: 600;
    color: $accent-indigo;
    min-width: 70px;
  }

  .st-desc {
    flex: 1;
    color: $text-secondary;
  }

  .st-status {
    font-size: 10px;
    white-space: nowrap;
    color: $text-muted;

    &.pending { color: $text-muted; }
    &.running { color: $accent-indigo; }
    &.done { color: $accent-emerald; }
    &.error { color: $accent-red; }
  }

  &.st-running { border-color: rgba($accent-indigo, 0.3); }
  &.st-done { border-color: rgba($accent-emerald, 0.3); }
  &.st-error { border-color: rgba($accent-red, 0.3); }
}

.wb-orch-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-4;
  color: $text-muted;
  font-size: $text-xs;

  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba($accent-indigo, 0.2);
    border-top-color: $accent-indigo;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
}

.wb-orch-input-area {
  display: flex;
  gap: $space-2;
  padding: $space-3;
  border-top: 1px solid rgba($border-subtle, 0.5);
  flex-shrink: 0;
}

.wb-orch-input {
  flex: 1;
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: $space-2 $space-3;
  font-size: $text-xs;
  color: $text-primary;
  outline: none;
  font-family: $font-sans;
  transition: border-color $transition-fast;

  &:focus { border-color: $accent-indigo; }
  &:disabled { opacity: 0.5; }
  &::placeholder { color: $text-placeholder; }
}

.wb-orch-send {
  padding: $space-2 $space-4;
  background: rgba($accent-indigo, 0.15);
  border: 1px solid rgba($accent-indigo, 0.3);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-xs;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;
  font-family: $font-sans;

  &:hover:not(:disabled) {
    background: rgba($accent-indigo, 0.25);
    border-color: rgba($accent-indigo, 0.5);
  }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: $breakpoint-md) {
  .wb-sidebar {
    position: fixed;
    left: 0;
    top: $topbar-height;
    bottom: 0;
    z-index: 50;
    transform: translateX(-100%);

    &:not(.wb-sidebar--collapsed) { transform: translateX(0); }
  }
}
</style>