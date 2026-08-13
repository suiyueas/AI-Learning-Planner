<template>
  <div class="result-detail-overlay" @click="$emit('close')">
    <div class="result-detail-dialog" @click.stop>
      <div class="rd-header">
        <div class="rd-header-left">
          <span class="rd-type-icon">{{ typeIcon }}</span>
          <div class="rd-header-info">
            <h3>{{ displayTitle }}</h3>
            <p>{{ result.taskDescription || result.content || '' }}</p>
          </div>
        </div>
        <button class="rd-close" @click="$emit('close')">✕</button>
      </div>

      <div class="rd-body">
        <!-- 加载中状态 -->
        <div v-if="loading" class="rd-loading">
          <div class="rd-loading-spinner"></div>
          <span>正在加载结果...</span>
        </div>

        <template v-else>
          <!-- 执行信息 -->
          <div class="rd-meta">
            <span class="rd-meta-item"><span class="meta-lbl">执行 Agent：</span>{{ result.agentName || '—' }}</span>
            <span class="rd-meta-item"><span class="meta-lbl">耗时：</span>{{ formatDuration(result.duration) }}</span>
            <span class="rd-meta-item"><span class="meta-lbl">状态：</span><span class="meta-ok">✅ 成功</span></span>
            <span class="rd-meta-item"><span class="meta-lbl">时间：</span>{{ formatTime(result.createdAt) }}</span>
          </div>

          <!-- 结果内容（按结果类型动态组件渲染） -->
          <div class="rd-content">
            <div class="rd-content-title">📄 {{ resultTypeLabel }}</div>
            <div class="rd-content-body">
              <component :is="resultComponent" :data="view.outputJson" :text="view.outputText" />
            </div>
          </div>
        </template>
      </div>

      <div class="rd-footer">
        <button class="rd-btn rd-btn-ghost" :disabled="loading" @click="copyContent">📋 复制内容</button>
        <button class="rd-btn rd-btn-ghost" :disabled="loading" @click="$emit('reExecute', result)">🔄 重新生成</button>
        <button class="rd-btn rd-btn-cancel" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { resolveResultView } from '@/utils/resultView'
import DiagnosisResult from './result-renderers/DiagnosisResult.vue'
import PlanResult from './result-renderers/PlanResult.vue'
import MarkdownResult from './result-renderers/MarkdownResult.vue'
import ReportResult from './result-renderers/ReportResult.vue'
import QuizResult from './result-renderers/QuizResult.vue'
import SearchResult from './result-renderers/SearchResult.vue'
import KnowledgeResult from './result-renderers/KnowledgeResult.vue'

const props = defineProps({
  result: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false }
})

defineEmits(['close', 'reExecute'])

/** 统一视图解析（兼容新统一结构 / 旧 {type, data:{result}} 结构 / 后端实体） */
const view = computed(() => resolveResultView(props.result))

/** 结果类型 → 渲染组件映射（agentId 与 resultType 双通道识别） */
const resultComponent = computed(() => {
  const agentMap = {
    '诊断Agent': DiagnosisResult,
    '规划Agent': PlanResult,
    '答疑Agent': MarkdownResult,
    '报告Agent': ReportResult,
    '习题Agent': QuizResult,
    '搜索Agent': SearchResult,
    '知识检索Agent': KnowledgeResult,
    diagnosis: DiagnosisResult,
    planner: PlanResult,
    tutor: MarkdownResult,
    reporter: ReportResult,
    exercise: QuizResult,
    search: SearchResult,
    knowledge: KnowledgeResult
  }
  const key = props.result.agentName || view.value.resultType
  return agentMap[key] || agentMap[view.value.resultType] || MarkdownResult
})

const displayTitle = computed(() => {
  const t = view.value.displayTitle
  if (t) return t
  const desc = props.result.taskDescription || props.result.content
  if (desc) return desc.length > 40 ? desc.substring(0, 40) + '...' : desc
  return '执行结果'
})

const typeIcon = computed(() => {
  const map = { diagnosis: '🔍', plan: '🗺️', qa: '💬', report: '📊', quiz: '✏️', search: '🌐', knowledge: '📚' }
  return map[view.value.resultType] || '📄'
})

const resultTypeLabel = computed(() => {
  const map = { diagnosis: '诊断报告', plan: '学习计划', qa: '答疑解惑', report: '学习报告', quiz: '测验题', search: '搜索结果', knowledge: '知识检索' }
  return map[view.value.resultType] || '执行结果'
})

function formatDuration(ms) {
  if (!ms) return '—'
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

function formatTime(t) {
  if (!t) return '—'
  const d = new Date(t)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function copyContent() {
  const text = view.value.outputText || ''
  if (!text) {
    ElMessage.warning('无可复制的内容')
    return
  }
  navigator.clipboard.writeText(text).then(() => ElMessage.success('内容已复制'))
    .catch(() => ElMessage.warning('复制失败'))
}
</script>

<style lang="scss" scoped>
.result-detail-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1100;
  animation: fadeIn 0.2s ease;
}
.result-detail-dialog {
  width: 680px; max-width: 90vw; max-height: 88vh;
  background: rgba(17, 17, 39, 0.96);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 16px;
  box-shadow: 0 16px 60px rgba(0, 0, 0, 0.5);
  display: flex; flex-direction: column;
  animation: modalEnter 0.3s ease;
}
.rd-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.rd-header-left { display: flex; align-items: center; gap: 12px; }
.rd-type-icon { font-size: 1.8rem; flex-shrink: 0; }
.rd-header-info {
  h3 { margin: 0 0 3px; font-size: 15px; font-weight: 700; color: #e8e8ff; }
  p { margin: 0; font-size: 12px; color: #8080a8; max-width: 460px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.rd-close {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(100, 100, 180, 0.08); border: none; border-radius: 6px;
  font-size: 14px; cursor: pointer; color: #9090b8; transition: all 0.15s; flex-shrink: 0;
  &:hover { background: rgba(100, 100, 180, 0.15); color: #e8e8ff; }
}
.rd-body { padding: 18px 22px; overflow-y: auto; flex: 1;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(100, 100, 180, 0.12); border-radius: 2px; }
}
.rd-meta {
  display: flex; flex-wrap: wrap; gap: 8px;
  padding: 12px 16px;
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.06);
  border-radius: 10px;
  margin-bottom: 18px;
}
.rd-meta-item {
  font-size: 12px; color: #8080a8;
  .meta-lbl { color: #606090; }
  .meta-ok { color: #10b981; font-weight: 600; }
}
.rd-content { margin-bottom: 4px; }
.rd-content-title { font-size: 13px; font-weight: 600; color: #a0a0c8; margin-bottom: 12px; }
.rd-content-body { padding: 0; }
.rd-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 14px 22px;
  border-top: 1px solid rgba(100, 100, 180, 0.08);
  flex-shrink: 0;
}
.rd-btn {
  padding: 8px 18px; border-radius: 8px; font-weight: 600; font-size: 12px;
  cursor: pointer; transition: all 0.2s; border: none; font-family: inherit;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.rd-btn-cancel { background: rgba(100, 100, 180, 0.08); color: #c0c0e0; &:hover { background: rgba(100, 100, 180, 0.15); } }
.rd-btn-ghost {
  padding: 6px 14px; background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 6px;
  color: #8080a8; font-size: 12px; font-weight: 500;
  &:hover { color: #00f5d4; border-color: rgba(0, 245, 212, 0.2); background: rgba(0, 245, 212, 0.06); }
}

/* 加载中状态 */
.rd-loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60px 20px; gap: 16px; color: #8080a8; font-size: 13px;
}
.rd-loading-spinner {
  width: 32px; height: 32px;
  border: 3px solid rgba(100, 100, 180, 0.1);
  border-top-color: #00f5d4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes modalEnter { from { opacity: 0; transform: scale(0.95) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }
</style>
