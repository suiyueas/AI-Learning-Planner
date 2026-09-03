<template>
  <div class="execution-detail-overlay" @click="handleClose">
    <div class="execution-detail-modal" @click.stop>
      <!-- ===== 头部：Agent 图标 / 名称 / 状态 / 耗时 ===== -->
      <div class="edm-header">
        <div class="edm-title">
          <span class="edm-icon">{{ agentIcon }}</span>
          <span class="edm-name">{{ log.agentName || '执行记录' }}</span>
          <span class="edm-status" :class="statusClass">{{ statusText }}</span>
          <span v-if="log.duration" class="edm-duration">⏱ {{ formatDuration(log.duration) }}</span>
          <span v-if="log.sourceType === 'tool'" class="edm-type-tag">🔧 工具记录</span>
        </div>
        <button class="edm-close" title="关闭 (ESC)" @click="handleClose">✕</button>
      </div>

      <!-- ===== 元信息 ===== -->
      <div class="edm-meta">
        <span class="edm-meta-item">🕐 {{ log.time || formatTime(log.timestamp) }}</span>
        <span v-if="log.resultId" class="edm-meta-item">#{{ String(log.resultId).slice(-8) }}</span>
        <span v-if="log.toolRecordId" class="edm-meta-item">#{{ String(log.toolRecordId).slice(-8) }}</span>
        <span v-if="view.displayTitle" class="edm-meta-item">📄 {{ view.displayTitle }}</span>
      </div>

      <!-- ===== 用户输入 ===== -->
      <div v-if="log.taskDescription || log.input" class="edm-input">
        <span class="edm-label">📝 用户输入</span>
        <div class="edm-input-text">{{ log.taskDescription || log.input }}</div>
      </div>

      <!-- ===== 执行结果（按 resultType 动态组件） ===== -->
      <div ref="bodyRef" class="edm-body">
        <span class="edm-label">📄 执行结果</span>

        <div v-if="loading" class="edm-loading">
          <div class="edm-loading-spinner"></div>
          <span>正在加载结果...</span>
        </div>

        <template v-else>
          <component
            :is="resultComponent"
            :data="view.outputJson"
            :text="view.outputText"
          />

          <div v-if="view.regenerated" class="edm-regenerated-hint">
            ⚠️ 该历史记录保存时未包含真实输出，已按当前能力重新生成结果展示（原始数据可在「原始JSON」中查看）
          </div>
        </template>
      </div>

      <!-- ===== 原始 JSON（折叠） ===== -->
      <div v-show="showRaw" class="edm-raw">
        <pre>{{ rawJson }}</pre>
      </div>

      <!-- ===== 底部操作栏 ===== -->
      <div class="edm-footer">
        <div class="edm-footer-left">
          <button class="edm-btn" title="复制结果（纯文本）" @click="copyContent">📋 复制</button>
          <button class="edm-btn" title="下载完整执行记录 JSON" @click="exportJson">📥 导出JSON</button>
          <button class="edm-btn" title="查看原始 JSON 数据" @click="showRaw = !showRaw">
            {{ showRaw ? '🎨 结构化' : '📄 原始JSON' }}
          </button>
          <button
            v-if="log.resultId || log.sourceType === 'tool'"
            class="edm-btn danger"
            title="删除该执行记录"
            @click="$emit('delete', log)"
          >
            🗑️ 删除
          </button>
        </div>
        <div class="edm-footer-right">
          <button class="edm-btn" :disabled="!hasPrev" title="← 上一条" @click="navigate('prev')">← 上一条</button>
          <span class="edm-nav-info">{{ currentIndex + 1 }} / {{ listLength }}</span>
          <button class="edm-btn" :disabled="!hasNext" title="下一条 →" @click="navigate('next')">下一条 →</button>
          <button class="edm-btn ghost" title="关闭 (ESC)" @click="handleClose">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { resolveResultView } from '@/utils/resultView'
import DiagnosisResult from './result-renderers/DiagnosisResult.vue'
import PlanResult from './result-renderers/PlanResult.vue'
import MarkdownResult from './result-renderers/MarkdownResult.vue'
import ReportResult from './result-renderers/ReportResult.vue'
import QuizResult from './result-renderers/QuizResultRenderer.vue'
import SearchResult from './result-renderers/SearchResult.vue'
import KnowledgeResult from './result-renderers/KnowledgeResult.vue'

const props = defineProps({
  /** 执行记录对象（含 output / taskDescription / agentName 等） */
  log: { type: Object, default: null },
  /** 导航列表（用于上一条/下一条） */
  list: { type: Array, default: () => [] },
  /** 当前记录在列表中的索引 */
  index: { type: Number, default: 0 },
  /** 加载中状态（从后端拉取详情时） */
  loading: { type: Boolean, default: false },
  /** 可选：旧版占位记录补救生成器 (agentId, taskDescription) => 结果载荷，用于生成真实学习成果 */
  fallbackGenerator: { type: Function, default: null }
})

const emit = defineEmits(['close', 'navigate', 'delete'])

const showRaw = ref(false)
const bodyRef = ref(null)

// 固定占位文案检测（旧版前端保存的占位结果，如“任务「xxx」已成功执行。”，不含真实输出）
const isFixedPlaceholderText = (text) => /任务「.+」已成功执行|「.+」已成功执行。|已成功执行。$/.test(text || '')

/** 统一视图：{ resultType, outputText, outputJson, displayTitle, summary, regenerated } */
const view = computed(() => {
  const v = resolveResultView(props.log?.output || props.log)
  // 旧版占位文案补救：保存时未含真实输出，按 agentId + 任务描述重新生成真实结果（仅用于展示）
  if (!v.regenerated && props.fallbackGenerator && props.log
      && typeof v.outputText === 'string' && isFixedPlaceholderText(v.outputText.trim())) {
    const payload = props.fallbackGenerator(props.log.agentId, props.log.taskDescription || props.log.description || '')
    if (payload && payload.type !== 'default' && payload.outputText) {
      return { ...resolveResultView(payload), regenerated: true }
    }
  }
  return v
})

/** 结果类型 → 渲染组件映射 */
const resultComponent = computed(() => {
  const map = {
    diagnosis: DiagnosisResult,
    plan: PlanResult,
    qa: MarkdownResult,
    report: ReportResult,
    quiz: QuizResult,
    search: SearchResult,
    knowledge: KnowledgeResult,
    default: MarkdownResult
  }
  return map[view.value.resultType] || MarkdownResult
})

const currentIndex = computed(() => {
  if (props.index >= 0) return props.index
  return Math.max(0, props.list.findIndex(l => l.id === props.log?.id))
})
const listLength = computed(() => props.list.length)
const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < listLength.value - 1)

const agentIcon = computed(() => {
  const id = props.log?.agentId || ''
  if (id.startsWith('tool:')) return '🔧'
  const iconMap = {
    diagnosis: '🔍', planner: '🗺️', tutor: '💬', reporter: '📊',
    exercise: '✏️', search: '🌐', knowledge: '📚'
  }
  return iconMap[id] || (props.log?.agentIcon || '🤖')
})

const statusClass = computed(() => props.log?.stepType || props.log?.status || 'success')
const statusText = computed(() => {
  const stepType = props.log?.stepType
  const map = {
    success: '✅ 任务完成',
    error: '❌ 执行失败',
    executing: '⏳ 执行中',
    observe: '⏳ 执行中',
    task: '📋 已提交'
  }
  return map[stepType] || props.log?.stepLabel || '✅ 成功'
})

const rawJson = computed(() => {
  try {
    return JSON.stringify(props.log, null, 2)
  } catch {
    return String(props.log)
  }
})

const formatDuration = (ms) => {
  if (!ms) return ''
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

/** 复制内容：优先人类可读文本，无文本时复制原始 JSON */
const copyContent = async () => {
  const text = view.value.outputText || rawJson.value || ''
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('内容已复制')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('内容已复制')
  }
}

/** 导出完整记录 JSON */
const exportJson = () => {
  const data = rawJson.value || '{}'
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `execution_${props.log?.id || Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已开始下载 JSON 文件')
}

const handleClose = () => {
  showRaw.value = false
  emit('close')
}

const navigate = (direction) => {
  if (direction === 'prev' && hasPrev.value) emit('navigate', 'prev')
  if (direction === 'next' && hasNext.value) emit('navigate', 'next')
  showRaw.value = false
  nextTick(() => { if (bodyRef.value) bodyRef.value.scrollTop = 0 })
}

/** 键盘快捷键：ESC 关闭，← → 切换记录 */
const handleKeydown = (e) => {
  const tag = e.target?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || e.target?.isContentEditable) return
  if (e.key === 'Escape') {
    handleClose()
  } else if (e.key === 'ArrowLeft') {
    e.preventDefault()
    navigate('prev')
  } else if (e.key === 'ArrowRight') {
    e.preventDefault()
    navigate('next')
  }
}

onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))
</script>

<style lang="scss" scoped>
.execution-detail-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1100;
  animation: fadeIn 0.2s ease;
}
.execution-detail-modal {
  width: 720px; max-width: 92vw; max-height: 88vh;
  background: rgba(17, 17, 39, 0.97);
  border: 1px solid rgba(100, 100, 180, 0.14);
  border-radius: 16px;
  box-shadow: 0 16px 60px rgba(0, 0, 0, 0.5);
  display: flex; flex-direction: column;
  animation: modalEnter 0.3s ease;
}
.edm-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.edm-title { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.edm-icon { font-size: 1.5rem; flex-shrink: 0; }
.edm-name { font-size: 15px; font-weight: 700; color: #e8e8ff; }
.edm-status {
  font-size: 10px; padding: 2px 8px; border-radius: 4px; font-weight: 600;
  &.success { background: rgba(16, 185, 129, 0.1); color: #10b981; }
  &.error { background: rgba(255, 0, 110, 0.1); color: #ff006e; }
  &.executing, &.observe { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
  &.task { background: rgba(100, 100, 180, 0.1); color: #8080a8; }
}
.edm-duration { font-size: 11px; color: #8080a8; font-family: 'JetBrains Mono', monospace; }
.edm-type-tag { font-size: 10px; padding: 2px 8px; border-radius: 4px; background: rgba(16, 185, 129, 0.1); color: #10b981; }
.edm-close {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(100, 100, 180, 0.08); border: none; border-radius: 6px;
  font-size: 14px; cursor: pointer; color: #9090b8; transition: all 0.15s; flex-shrink: 0;
  &:hover { background: rgba(100, 100, 180, 0.15); color: #e8e8ff; }
}
.edm-meta {
  display: flex; flex-wrap: wrap; gap: 8px;
  padding: 10px 20px;
  background: rgba(100, 100, 180, 0.03);
  border-bottom: 1px solid rgba(100, 100, 180, 0.06);
  flex-shrink: 0;
}
.edm-meta-item { font-size: 11px; color: #8080a8; font-family: 'JetBrains Mono', monospace; }
.edm-input {
  padding: 12px 20px 0;
  flex-shrink: 0;
}
.edm-label { display: block; font-size: 11px; font-weight: 600; color: #8080a8; margin-bottom: 6px; }
.edm-input-text {
  padding: 10px 14px; background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 8px;
  font-size: 13px; color: #c0c0e0; line-height: 1.6;
  max-height: 80px; overflow-y: auto; word-break: break-word;
}
.edm-body { padding: 14px 20px; overflow-y: auto; flex: 1;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(100, 100, 180, 0.12); border-radius: 2px; }
}
.edm-loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 50px 20px; gap: 14px; color: #8080a8; font-size: 13px;
}
.edm-loading-spinner {
  width: 30px; height: 30px;
  border: 3px solid rgba(100, 100, 180, 0.1);
  border-top-color: #00f5d4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.edm-regenerated-hint {
  margin-top: 10px; padding: 8px 12px;
  background: rgba(123, 97, 255, 0.08); border: 1px solid rgba(123, 97, 255, 0.25);
  border-radius: 6px; color: #a78bfa; font-size: 12px; line-height: 1.5;
}
.edm-raw {
  flex-shrink: 0; max-height: 220px; overflow-y: auto;
  margin: 0 20px 10px;
  padding: 12px; background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(100, 100, 180, 0.1); border-radius: 8px;
  pre { margin: 0; font-size: 11px; line-height: 1.6; color: #9090b8; font-family: 'JetBrains Mono', monospace; white-space: pre-wrap; word-break: break-all; }
}
.edm-footer {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.edm-footer-left, .edm-footer-right { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.edm-btn {
  padding: 6px 12px; background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 6px;
  color: #8080a8; font-size: 12px; font-weight: 500; cursor: pointer;
  transition: all 0.2s; font-family: inherit;
  &:hover { color: #00f5d4; border-color: rgba(0, 245, 212, 0.2); background: rgba(0, 245, 212, 0.06); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
  &.danger:hover { color: #ff006e; border-color: rgba(255, 0, 110, 0.2); background: rgba(255, 0, 110, 0.06); }
  &.ghost { background: transparent; border-color: rgba(100, 100, 180, 0.15); }
}
.edm-nav-info { font-size: 11px; color: #606090; font-family: 'JetBrains Mono', monospace; }

@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes modalEnter { from { opacity: 0; transform: scale(0.95) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }
</style>