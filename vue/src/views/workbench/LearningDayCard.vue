<template>
  <div class="learning-day">
    <div class="ld-header">
      <h2 class="ld-title">📖 学习中</h2>
      <p class="ld-desc">今日学习任务 — 保持专注，持续进步</p>
    </div>

    <!-- 加载 / 空状态 -->
    <div v-if="!task && !tasks.length" class="ld-empty">
      <div class="ld-empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40">
          <path d="M12 2a7 7 0 017 7c0 2.38-1.19 4.47-3 5.74V17a2 2 0 01-2 2h-4a2 2 0 01-2-2v-2.26C6.19 13.47 5 11.38 5 9a7 7 0 017-7z"/>
          <path d="M9 22h6"/>
        </svg>
      </div>
      <p>暂无学习任务，请等待 AI 安排...</p>
    </div>

    <template v-else>
      <!-- 今日进度 -->
      <div class="ld-progress-section">
        <div class="ld-progress-header">
          <span class="ld-progress-label">今日进度</span>
          <span class="ld-progress-value">{{ completedCount }}/{{ totalCount }} 项任务</span>
        </div>
        <div class="ld-progress-track">
          <div
            class="ld-progress-fill"
            :style="{ width: `${progressPercent}%` }"
          ></div>
        </div>
      </div>

      <!-- 番茄钟计时器 -->
      <div class="ld-timer-section">
        <div class="ld-timer-header">
          <span class="ld-timer-icon">🍅</span>
          <span class="ld-timer-label">番茄钟</span>
          <span class="ld-timer-status">{{ timerRunning ? '进行中' : '已暂停' }}</span>
        </div>
        <div class="ld-timer-display">
          <span class="ld-timer-time">{{ formattedTime }}</span>
        </div>
        <div class="ld-timer-actions">
          <button
            class="ld-timer-btn ld-timer-btn--primary"
            @click="toggleTimer"
          >
            {{ timerRunning ? '暂停' : '开始' }}
          </button>
          <button
            class="ld-timer-btn ld-timer-btn--secondary"
            @click="resetTimer"
          >
            重置
          </button>
        </div>
      </div>

      <!-- 当前任务详情 -->
      <div v-if="currentTask" class="ld-task-card">
        <div class="ld-task-header">
          <span class="ld-task-title">{{ currentTask.title }}</span>
          <UBadge :variant="currentTask.completed ? 'success' : 'info'" size="sm">
            {{ currentTask.estimatedMinutes || 15 }}分钟
          </UBadge>
        </div>
        <div class="ld-task-content">
          <p>{{ currentTask.content || '暂无详细内容' }}</p>
        </div>
        <div class="ld-task-actions">
          <button
            v-if="!currentTask.completed"
            class="ld-complete-btn"
            @click="completeTask(currentTask.id)"
          >
            ✓ 标记完成
          </button>
          <span v-else class="ld-completed-badge">✓ 已完成</span>
        </div>
      </div>

      <!-- 完整任务列表 -->
      <div class="ld-task-list">
        <div class="ld-task-list-header">
          <span class="ld-task-list-title">任务列表</span>
          <span class="ld-task-list-count">{{ completedCount }}/{{ totalCount }}</span>
        </div>
        <div
          v-for="t in tasks"
          :key="t.id"
          class="ld-task-item"
          :class="{
            'ld-task-item--completed': t.completed,
            'ld-task-item--active': t.id === currentTask?.id && !t.completed
          }"
          @click="switchTask(t.id)"
        >
          <span class="ld-task-check">
            <span v-if="t.completed" class="ld-check-icon">✓</span>
            <span v-else class="ld-check-circle"></span>
          </span>
          <div class="ld-task-info">
            <span class="ld-task-item-title">{{ t.title }}</span>
            <span class="ld-task-item-meta">{{ t.estimatedMinutes || '?' }}分钟</span>
          </div>
          <span v-if="t.id === currentTask?.id && !t.completed" class="ld-task-current">当前</span>
        </div>
      </div>

      <!-- 快速笔记 -->
      <div class="ld-notes-section">
        <div class="ld-notes-header">
          <span class="ld-notes-icon">📝</span>
          <span class="ld-notes-title">学习笔记</span>
        </div>
        <textarea
          v-model="notes"
          class="ld-notes-textarea"
          placeholder="记录你的学习心得、疑问或重点..."
          rows="3"
        ></textarea>
        <div class="ld-notes-footer">
          <span class="ld-notes-hint">笔记自动保存</span>
          <span v-if="notesSaved" class="ld-notes-saved">已保存</span>
        </div>
      </div>

      <!-- 完成学习按钮 -->
      <div class="ld-footer">
        <button
          class="ld-finish-btn"
          :class="{ 'ld-finish-btn--ready': allTasksCompleted }"
          :disabled="!allTasksCompleted"
          @click="finishLearning"
        >
          {{ allTasksCompleted ? '✓ 完成学习，进入下一阶段' : '请先完成所有学习任务' }}
        </button>
        <p v-if="!allTasksCompleted" class="ld-footer-hint">
          完成所有任务后，即可进入下一阶段
        </p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { UBadge } from '@/components/ui'

const props = defineProps({
  session: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['ask', 'complete'])

// ===== 任务管理 =====
const tasks = ref([])
const currentTaskId = ref(null)

// 初始化任务数据
const initTasks = () => {
  const taskData = Array.isArray(props.session.tasks) && props.session.tasks.length > 0
    ? props.session.tasks
    : props.session.currentTask
      ? [props.session.currentTask]
      : []
  tasks.value = taskData.map((t, i) => ({
    ...t,
    id: t.id || `task_${i}`,
    completed: t.completed || false
  }))
  if (tasks.value.length > 0 && !currentTaskId.value) {
    const firstIncomplete = tasks.value.find(t => !t.completed)
    currentTaskId.value = firstIncomplete?.id || tasks.value[0].id
  }
}

watch(() => props.session, () => initTasks(), { deep: true, immediate: true })

const currentTask = computed(() => tasks.value.find(t => t.id === currentTaskId.value))
const totalCount = computed(() => tasks.value.length)
const completedCount = computed(() => tasks.value.filter(t => t.completed).length)
const progressPercent = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((completedCount.value / totalCount.value) * 100)
})
const allTasksCompleted = computed(() => totalCount.value > 0 && completedCount.value === totalCount.value)

const completeTask = (id) => {
  const task = tasks.value.find(t => t.id === id)
  if (task) {
    task.completed = true
    // 自动切换到下一个未完成任务
    const next = tasks.value.find(t => !t.completed)
    if (next) {
      currentTaskId.value = next.id
    }
  }
}

const finishLearning = () => {
  if (allTasksCompleted.value) {
    emit('complete')
  }
}

const switchTask = (id) => {
  currentTaskId.value = id
}

// ===== 番茄钟 =====
const timerRunning = ref(false)
const timerSeconds = ref(25 * 60) // 25分钟
let timerInterval = null

const formattedTime = computed(() => {
  const mins = Math.floor(timerSeconds.value / 60)
  const secs = timerSeconds.value % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
})

const toggleTimer = () => {
  if (timerRunning.value) {
    clearInterval(timerInterval)
    timerInterval = null
    timerRunning.value = false
  } else {
    timerRunning.value = true
    timerInterval = setInterval(() => {
      if (timerSeconds.value > 0) {
        timerSeconds.value--
      } else {
        clearInterval(timerInterval)
        timerInterval = null
        timerRunning.value = false
        // 计时结束提示
      }
    }, 1000)
  }
}

const resetTimer = () => {
  clearInterval(timerInterval)
  timerInterval = null
  timerRunning.value = false
  timerSeconds.value = 25 * 60
}

// ===== 笔记 =====
const notes = ref('')
const notesSaved = ref(false)
let notesTimer = null

// 自动保存笔记（防抖 2 秒）
watch(notes, () => {
  notesSaved.value = false
  if (notesTimer) clearTimeout(notesTimer)
  notesTimer = setTimeout(() => {
    notesSaved.value = true
  }, 2000)
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
  if (notesTimer) clearTimeout(notesTimer)
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.learning-day {
  max-width: 700px;
  margin: 0 auto;
  padding-bottom: $space-4;
}

// ===== 头部 =====
.ld-header {
  margin-bottom: $space-5;
}

.ld-title {
  font-size: $text-2xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-1;
}

.ld-desc {
  color: $text-muted;
  margin: 0;
  font-size: $text-sm;
}

// ===== 空状态 =====
.ld-empty {
  text-align: center;
  padding: $space-12;
  color: $text-muted;

  p { margin: $space-3 0 0; }
}

.ld-empty-icon {
  opacity: 0.5;
}

// ===== 进度条 =====
.ld-progress-section {
  margin-bottom: $space-4;
  padding: $space-3 $space-4;
  background: rgba($bg-surface, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
}

.ld-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-2;
}

.ld-progress-label {
  font-size: $text-xs;
  font-weight: 600;
  color: $text-secondary;
}

.ld-progress-value {
  font-size: $text-xs;
  color: $text-muted;
  font-family: $font-data;
}

.ld-progress-track {
  height: 6px;
  background: rgba($bg-elevated, 0.5);
  border-radius: 3px;
  overflow: hidden;
}

.ld-progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, $accent-indigo, $accent-cyan);
  transition: width 0.6s ease;
}

// ===== 番茄钟 =====
.ld-timer-section {
  display: flex;
  align-items: center;
  gap: $space-4;
  padding: $space-3 $space-4;
  margin-bottom: $space-4;
  background: rgba($bg-surface, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
}

.ld-timer-header {
  display: flex;
  align-items: center;
  gap: $space-1;
  flex-shrink: 0;
}

.ld-timer-icon {
  font-size: 1.2rem;
}

.ld-timer-label {
  font-size: $text-xs;
  font-weight: 600;
  color: $text-secondary;
}

.ld-timer-status {
  font-size: 10px;
  padding: 1px 6px;
  background: rgba($accent-indigo, 0.1);
  color: $accent-indigo;
  border-radius: $radius-full;
  margin-left: $space-1;
}

.ld-timer-display {
  flex: 1;
  text-align: center;
}

.ld-timer-time {
  font-family: $font-data;
  font-size: $text-2xl;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: 2px;
}

.ld-timer-actions {
  display: flex;
  gap: $space-1;
  flex-shrink: 0;
}

.ld-timer-btn {
  padding: 5px 14px;
  border-radius: $radius-md;
  font-size: $text-xs;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &--primary {
    background: rgba($accent-indigo, 0.12);
    border: 1px solid rgba($accent-indigo, 0.25);
    color: $accent-indigo;

    &:hover {
      background: rgba($accent-indigo, 0.2);
    }
  }

  &--secondary {
    background: transparent;
    border: 1px solid $border-subtle;
    color: $text-muted;

    &:hover {
      border-color: $border-medium;
      color: $text-secondary;
    }
  }
}

// ===== 当前任务卡片 =====
.ld-task-card {
  background: rgba($bg-surface, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  padding: $space-5;
  margin-bottom: $space-4;
}

.ld-task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-4;
}

.ld-task-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
}

.ld-task-content {
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.6;
  padding: $space-4;
  background: rgba($bg-elevated, 0.3);
  border-radius: $radius-md;
  margin-bottom: $space-4;

  p { margin: 0; }
}

.ld-task-actions {
  text-align: center;
}

.ld-complete-btn {
  padding: 8px 24px;
  background: rgba($color-success, 0.12);
  border: 1px solid rgba($color-success, 0.3);
  border-radius: $radius-md;
  color: $color-success;
  font-size: $text-sm;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &:hover {
    background: rgba($color-success, 0.2);
  }
}

.ld-completed-badge {
  font-size: $text-sm;
  color: $color-success;
  font-weight: 600;
}

// ===== 任务列表 =====
.ld-task-list {
  margin-bottom: $space-4;
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  overflow: hidden;
}

.ld-task-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $space-3 $space-4;
  background: rgba($bg-surface, 0.3);
  border-bottom: 1px solid $border-subtle;
}

.ld-task-list-title {
  font-size: $text-xs;
  font-weight: 600;
  color: $text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ld-task-list-count {
  font-size: $text-xs;
  color: $text-muted;
  font-family: $font-data;
}

.ld-task-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  border-bottom: 1px solid $border-subtle;
  cursor: pointer;
  transition: all $transition-fast;

  &:last-child { border-bottom: none; }

  &:hover {
    background: rgba($accent-indigo, 0.03);
  }

  &--completed {
    opacity: 0.55;
    .ld-task-item-title { text-decoration: line-through; }
  }

  &--active {
    background: rgba($accent-indigo, 0.05);
  }
}

.ld-task-check {
  flex-shrink: 0;
}

.ld-check-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba($color-success, 0.15);
  color: $color-success;
  font-size: 11px;
  font-weight: 700;
}

.ld-check-circle {
  display: block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid $border-medium;
  transition: all $transition-fast;

  .ld-task-item:hover & {
    border-color: rgba($accent-indigo, 0.4);
  }
}

.ld-task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.ld-task-item-title {
  font-size: $text-sm;
  font-weight: 500;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ld-task-item-meta {
  font-size: 10px;
  color: $text-muted;
}

.ld-task-current {
  font-size: 10px;
  padding: 1px 8px;
  background: rgba($accent-indigo, 0.1);
  color: $accent-indigo;
  border-radius: $radius-full;
  flex-shrink: 0;
}

// ===== 笔记 =====
.ld-notes-section {
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  overflow: hidden;
}

.ld-notes-header {
  display: flex;
  align-items: center;
  gap: $space-1;
  padding: $space-3 $space-4;
  background: rgba($bg-surface, 0.3);
  border-bottom: 1px solid $border-subtle;
}

.ld-notes-icon {
  font-size: 1rem;
}

.ld-notes-title {
  font-size: $text-xs;
  font-weight: 600;
  color: $text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ld-notes-textarea {
  width: 100%;
  padding: $space-4;
  border: none;
  outline: none;
  resize: vertical;
  background: transparent;
  color: $text-primary;
  font-size: $text-sm;
  font-family: $font-sans;
  line-height: 1.6;
  min-height: 80px;

  &::placeholder {
    color: $text-placeholder;
  }
}

.ld-notes-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $space-2 $space-4;
  border-top: 1px solid $border-subtle;
  background: rgba($bg-surface, 0.2);
}

.ld-notes-hint {
  font-size: 10px;
  color: $text-muted;
}

.ld-notes-saved {
  font-size: 10px;
  color: $color-success;
  font-weight: 500;
}

// ===== 完成按钮 =====
.ld-footer {
  text-align: center;
  margin-top: $space-6;
  padding-top: $space-4;
  border-top: 1px solid $border-subtle;
}

.ld-finish-btn {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  padding: 10px 28px;
  border-radius: $radius-md;
  font-size: $text-sm;
  font-weight: 600;
  font-family: $font-sans;
  cursor: not-allowed;
  transition: all $transition-fast;
  background: rgba($bg-surface, 0.3);
  border: 1px solid $border-subtle;
  color: $text-muted;
  opacity: 0.6;

  &--ready {
    cursor: pointer;
    opacity: 1;
    background: rgba($color-success, 0.1);
    border-color: rgba($color-success, 0.3);
    color: $color-success;

    &:hover {
      background: rgba($color-success, 0.18);
      transform: translateY(-1px);
    }
  }
}

.ld-footer-hint {
  font-size: 11px;
  color: $text-muted;
  margin: $space-2 0 0;
}
</style>