<template>
  <div class="scheduling-overlay" @click="$emit('close')">
    <div class="scheduling-dialog" @click.stop>
      <div class="scheduling-header">
        <div class="scheduling-header-left">
          <div class="scheduling-avatar">
            <svg viewBox="0 0 64 64" width="40" height="40">
              <circle cx="32" cy="32" r="28" fill="none" stroke="#00f5d4" stroke-width="2" opacity="0.5" />
              <circle cx="32" cy="32" r="5" fill="#00f5d4">
                <animate attributeName="r" values="5;6;5" dur="2s" repeatCount="indefinite" />
              </circle>
            </svg>
          </div>
          <div class="scheduling-info">
            <h3>🔄 调度中心</h3>
            <p>编排 Agent · 统一调度与任务分配中枢</p>
          </div>
        </div>
        <button class="scheduling-close" @click="$emit('close')">✕</button>
      </div>

      <div class="scheduling-body">
        <!-- 系统状态 -->
        <div class="scheduling-status">
          <div class="status-item"><span class="status-dot idle"></span>系统运行：<strong class="text-ok">🟢 正常</strong></div>
          <div class="status-item">可用 Agent：<strong class="text-ok">{{ availableCount }}/{{ totalCount }}</strong></div>
          <div class="status-item">执行中：<strong class="text-run">{{ executingCount }}</strong></div>
          <div class="status-item">今日调度：<strong class="text-accent">{{ todayScheduleCount }} 次</strong></div>
        </div>

        <!-- 任务分配列表 -->
        <div class="scheduling-task-list">
          <div class="section-label">📋 任务分配</div>
          <div class="task-list-body">
            <div
              v-for="agent in subAgents"
              :key="agent.id"
              class="task-item"
              :class="{ selected: selectedTasks[agent.id] }"
              @click="toggleAgentSelection(agent.id)"
            >
              <div class="task-item-check">
                <span class="check-custom" :class="{ checked: selectedTasks[agent.id] }">
                  <svg v-if="selectedTasks[agent.id]" class="check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </span>
              </div>
              <span class="task-item-icon" :style="{ color: agentColor(agent.id) }">{{ agent.icon }}</span>
              <span class="task-item-name">{{ agent.name }}</span>
              <input
                v-model="taskDescriptions[agent.id]"
                class="task-item-input"
                :placeholder="'为 ' + agent.name + ' 分配任务...'"
                :disabled="isExecuting || !selectedTasks[agent.id]"
                @click.stop="handleInputClick(agent.id)"
                @input="ensureAgentSelected(agent.id)"
              />
            </div>
            <!-- 添加任务提示 -->
            <div v-if="!hasAnySelected" class="task-list-hint">点击输入框或勾选复选框来添加任务</div>
          </div>
        </div>

        <!-- 最近调度记录 -->
        <div class="scheduling-history">
          <div class="section-label scheduling-history-label">
            <span>📝 最近调度记录</span>
            <button class="history-view-all" @click="$emit('viewAll')">查看全部</button>
          </div>
          <div v-if="recentLogs.length > 0" class="scheduling-history-list">
            <div v-for="(log, idx) in recentLogs" :key="idx" class="history-item">
              <span class="history-time">{{ log.time }}</span>
              <span class="history-agent" :style="{ color: agentColor(log.agentId) }">{{ log.agentName }}</span>
              <span class="history-arrow">→</span>
              <span class="history-desc">{{ truncateText(log.description, 28) }}</span>
              <span class="history-status" :class="log.status === 'success' ? 'ok' : 'running'">
                {{ log.status === 'success' ? '✅ 完成' : '⏳ 执行中' }}
              </span>
              <span v-if="log.duration" class="history-duration">{{ formatDuration(log.duration) }}</span>
            </div>
          </div>
          <div v-else class="scheduling-history-empty">暂无调度记录，创建任务后将在下方显示</div>
        </div>
      </div>

      <div class="scheduling-footer">
        <div class="scheduling-actions-left">
          <button class="scheduling-select-all" @click="toggleSelectAll">
            {{ allSelected ? '☐ 取消全选' : '☑ 全选' }}
          </button>
        </div>
        <div class="scheduling-actions-right">
          <button class="scheduling-btn scheduling-btn-cancel" @click="$emit('close')">取消</button>
          <button
            class="scheduling-btn scheduling-btn-exec"
            :class="{ running: isExecuting }"
            :disabled="isExecuting || !hasAnyTask"
            @click="executeAll"
          >
            <span v-if="isExecuting" class="btn-spinner"></span>
            <span v-else>🚀</span>
            <span>{{ isExecuting ? '执行中...' : '并行执行' }}</span>
            <span v-if="selectedCount > 0 && !isExecuting" class="exec-count"> ({{ selectedCount }})</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  subAgents: { type: Array, default: () => [] },
  availableCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  executingCount: { type: Number, default: 0 },
  todayScheduleCount: { type: Number, default: 0 },
  isExecuting: { type: Boolean, default: false },
  recentLogs: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'executeBatch', 'viewAll'])

const selectedTasks = ref({})
const taskDescriptions = ref({})

// 初始化
watch(() => props.subAgents, (agents) => {
  const sel = {}
  const desc = {}
  agents.forEach(a => { sel[a.id] = false; desc[a.id] = '' })
  selectedTasks.value = sel
  taskDescriptions.value = desc
}, { immediate: true })

const hasAnySelected = computed(() => Object.values(selectedTasks.value).some(v => v))
const selectedCount = computed(() => Object.values(selectedTasks.value).filter(v => v).length)
const allSelected = computed(() => {
  const ids = Object.keys(selectedTasks.value)
  return ids.length > 0 && ids.every(id => selectedTasks.value[id])
})
const hasAnyTask = computed(() => {
  return Object.entries(selectedTasks.value).some(([id, sel]) => sel && taskDescriptions.value[id]?.trim())
})

function toggleSelectAll() {
  const newVal = !allSelected.value
  Object.keys(selectedTasks.value).forEach(id => { selectedTasks.value[id] = newVal })
}

function executeAll() {
  const tasks = Object.entries(selectedTasks.value)
    .filter(([id, sel]) => sel && taskDescriptions.value[id]?.trim())
    .map(([id]) => ({ agentId: id, description: taskDescriptions.value[id].trim() }))

  if (tasks.length === 0) {
    ElMessage.warning('请至少勾选一个 Agent 并填写任务描述')
    return
  }

  emit('executeBatch', tasks)
}

const agentColors = {
  diagnosis: '#00f5d4', planner: '#7b61ff', tutor: '#3a86ff',
  reporter: '#f59e0b', exercise: '#ff006e', search: '#10b981', knowledge: '#a78bfa'
}

function agentColor(id) { return agentColors[id] || '#8080a8' }

function truncateText(text, max) {
  if (!text) return ''
  return text.length > max ? text.substring(0, max) + '...' : text
}

function formatDuration(ms) {
  if (!ms) return ''
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

function toggleAgentSelection(id) {
  if (props.isExecuting) return
  selectedTasks.value[id] = !selectedTasks.value[id]
}

function handleInputClick(id) {
  if (!selectedTasks.value[id]) {
    selectedTasks.value[id] = true
  }
}

function ensureAgentSelected(id) {
  if (!selectedTasks.value[id]) {
    selectedTasks.value[id] = true
  }
}
</script>

<style lang="scss" scoped>
.scheduling-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}
.scheduling-dialog {
  width: 700px; max-width: 90vw; max-height: 88vh;
  background: rgba(17, 17, 39, 0.96);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 16px;
  box-shadow: 0 16px 60px rgba(0, 0, 0, 0.5);
  display: flex; flex-direction: column;
  animation: modalEnter 0.3s ease;
}
.scheduling-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.scheduling-header-left { display: flex; align-items: center; gap: 12px; }
.scheduling-avatar { flex-shrink: 0; display: flex; align-items: center; }
.scheduling-info {
  h3 { margin: 0 0 3px; font-size: 15px; font-weight: 700; color: #e8e8ff; }
  p { margin: 0; font-size: 12px; color: #a0a0c8; }
}
.scheduling-close {
  width: 26px; height: 26px; display: flex; align-items: center; justify-content: center;
  background: rgba(100, 100, 180, 0.08); border: none; border-radius: 6px;
  font-size: 14px; cursor: pointer; color: #9090b8; transition: all 0.15s; flex-shrink: 0;
  &:hover { background: rgba(100, 100, 180, 0.15); color: #e8e8ff; }
}
.scheduling-body { padding: 18px 22px; overflow-y: auto; flex: 1;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(100, 100, 180, 0.12); border-radius: 2px; }
}
.scheduling-status {
  display: flex; flex-wrap: wrap; gap: 8px;
  padding: 14px; background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.06); border-radius: 10px;
  margin-bottom: 18px;
}
.status-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: #8080a8; min-width: calc(50% - 8px);
  strong { font-weight: 600; }
}
.status-dot {
  width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0;
  &.idle { background: #10b981; box-shadow: 0 0 8px rgba(16,185,129,0.4); }
  &.running { background: #f59e0b; animation: pulseDot 1s ease-in-out infinite; }
}
.text-ok { color: #10b981; }
.text-run { color: #f59e0b; }
.text-accent { color: #00f5d4; }

.section-label { font-size: 12px; font-weight: 600; color: #a0a0c8; margin-bottom: 10px; }

/* 任务分配列表 */
.scheduling-task-list { margin-bottom: 18px; }
.task-list-body {
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.06);
  border-radius: 10px;
  padding: 8px;
}
.task-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 8px;
  transition: all 0.2s;
  cursor: pointer;
  &:hover { background: rgba(255, 255, 255, 0.03); }
  &.selected { background: rgba(0, 245, 212, 0.05); border: 1px solid rgba(0, 245, 212, 0.1); }
  & + & { margin-top: 2px; }
}
.task-item-check {
  position: relative; display: flex; align-items: center; flex-shrink: 0;
  .check-custom {
    width: 20px; height: 20px; border-radius: 5px;
    border: 2px solid rgba(100, 100, 180, 0.25);
    background: rgba(100, 100, 180, 0.06);
    cursor: pointer; transition: all 0.2s;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
    &.checked {
      background: linear-gradient(135deg, #00f5d4, #3a86ff);
      border-color: #00f5d4;
      box-shadow: 0 0 10px rgba(0, 245, 212, 0.3);
    }
    &:hover { border-color: rgba(0, 245, 212, 0.5); transform: scale(1.05); }
    .check-icon {
      width: 12px; height: 12px;
      color: #0a0a1a;
      stroke: #0a0a1a;
    }
  }
}
.task-item-icon { font-size: 1.1rem; flex-shrink: 0; width: 20px; text-align: center; }
.task-item-name {
  font-size: 12px; font-weight: 600; color: #c0c0e0;
  flex-shrink: 0; width: 60px;
}
.task-item-input {
  flex: 1; min-width: 0;
  padding: 7px 12px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 7px;
  font-size: 12px; color: #e0e0f0;
  outline: none; font-family: inherit;
  transition: all 0.2s;
  &:focus { border-color: rgba(0, 245, 212, 0.25); box-shadow: 0 0 0 3px rgba(0, 245, 212, 0.06); }
  &::placeholder { color: #606090; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.task-list-hint {
  text-align: center; padding: 16px; color: #606090; font-size: 11px;
}

/* 调度记录 */
.scheduling-history { margin-bottom: 4px; }
.scheduling-history-label { display: flex; align-items: center; justify-content: space-between; }
.history-view-all {
  font-size: 11px; color: #606090; background: none;
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 6px;
  padding: 3px 10px; cursor: pointer; font-family: inherit;
  &:hover { color: #00f5d4; border-color: rgba(0, 245, 212, 0.2); }
}
.scheduling-history-list { display: flex; flex-direction: column; gap: 3px; }
.history-item {
  display: flex; align-items: center; gap: 6px; padding: 6px 10px;
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.05); border-radius: 7px;
  font-size: 11px; color: #8080a8; font-family: 'JetBrains Mono', monospace;
}
.history-time { color: #606090; flex-shrink: 0; }
.history-agent { font-weight: 500; flex-shrink: 0; }
.history-arrow { color: #606090; flex-shrink: 0; }
.history-desc { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #a0a0c8; }
.history-status { flex-shrink: 0; font-size: 10px; &.ok { color: #10b981; } &.running { color: #f59e0b; } }
.history-duration { flex-shrink: 0; color: #606090; font-size: 10px; }
.scheduling-history-empty {
  text-align: center; padding: 16px; color: #606090; font-size: 12px;
  background: rgba(100, 100, 180, 0.03);
  border: 1px dashed rgba(100, 100, 180, 0.08); border-radius: 8px;
}

/* 底部 */
.scheduling-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 22px;
  border-top: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.scheduling-select-all {
  padding: 6px 14px; background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 6px;
  color: #8080a8; font-size: 12px; font-family: inherit; cursor: pointer;
  &:hover { color: #00f5d4; border-color: rgba(0, 245, 212, 0.2); }
}
.scheduling-actions-right { display: flex; gap: 8px; }
.scheduling-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 20px; border-radius: 8px; font-weight: 600; font-size: 13px;
  cursor: pointer; transition: all 0.2s; border: none; font-family: inherit;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.scheduling-btn-cancel {
  background: rgba(100, 100, 180, 0.08); color: #c0c0e0;
  &:hover { background: rgba(100, 100, 180, 0.15); }
}
.scheduling-btn-exec {
  background: linear-gradient(135deg, #00f5d4, #3a86ff); color: #fff;
  &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(0, 245, 212, 0.25); }
  &.running { background: linear-gradient(135deg, #f59e0b, #f97316); animation: pulseBtn 1.5s ease-in-out infinite; }
}
.exec-count { font-size: 11px; opacity: 0.8; }
.btn-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff; border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes modalEnter { from { opacity: 0; transform: scale(0.95) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }
@keyframes pulseDot { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
@keyframes pulseBtn { 0%,100% { box-shadow: 0 0 0 rgba(245,158,11,0); } 50% { box-shadow: 0 0 14px rgba(245,158,11,0.2); } }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>