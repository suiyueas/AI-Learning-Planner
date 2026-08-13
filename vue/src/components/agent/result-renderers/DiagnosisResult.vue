<template>
  <div class="diagnosis-result">
    <!-- 文本兜底：无结构化数据时展示 outputText -->
    <div v-if="!hasStructured && text" class="dr-fallback" v-html="renderedText"></div>

    <template v-else>
      <!-- 整体水平 -->
      <div v-if="data.summary || data.level" class="dr-summary">
        <span class="dr-level-badge" :class="levelClass">{{ levelText }}</span>
        <span class="dr-summary-text">{{ data.summary || `整体水平：${levelText}` }}</span>
        <span v-if="score !== null" class="dr-score">{{ score }}<span class="dr-score-unit">分</span></span>
      </div>

      <!-- 维度评分 -->
      <div v-if="dimensions.length > 0" class="dr-dimensions">
        <div class="dr-section-title">📈 能力维度评估</div>
        <div v-for="(dim, idx) in dimensions" :key="idx" class="dr-dim">
          <div class="dr-dim-header">
            <span class="dr-dim-name">{{ dim.name || dim.label }}</span>
            <span class="dr-dim-level" :class="dimLevelClass(dim)">{{ dim.level || getLevelByScore(dim.score) }}</span>
            <span v-if="dim.score !== undefined && dim.score !== null" class="dr-dim-score">{{ dim.score }}<span class="dr-dim-unit">%</span></span>
          </div>
          <div class="dr-dim-bar-track">
            <div class="dr-dim-bar-fill" :style="{ width: Math.min(100, dim.score || 0) + '%', background: barColor(dim.score) }"></div>
          </div>
        </div>
      </div>

      <!-- 优势领域 -->
      <div v-if="strengths.length > 0 || weaknesses.length > 0" class="dr-columns">
        <div v-if="strengths.length > 0" class="dr-col">
          <div class="dr-section-title">✅ 优势领域</div>
          <div v-for="(s, idx) in strengths" :key="idx" class="dr-strength-item">
            <span class="dr-strength-name">{{ s.name || s }}</span>
            <span v-if="s.score !== undefined" class="dr-strength-score">{{ s.score }}%</span>
            <span v-if="s.desc" class="dr-strength-desc">{{ s.desc }}</span>
          </div>
        </div>
        <div v-if="weaknesses.length > 0" class="dr-col">
          <div class="dr-section-title">⚠️ 薄弱环节</div>
          <div v-for="(w, idx) in weaknesses" :key="idx" class="dr-weak-item">
            <span class="dr-weak-name">{{ w.name || w }}</span>
            <span v-if="w.priority" class="dr-weak-priority">{{ w.priority }}</span>
          </div>
        </div>
      </div>

      <!-- 改进建议 -->
      <div v-if="suggestions.length > 0" class="dr-suggestions">
        <div class="dr-section-title">💡 改进建议</div>
        <div v-for="(s, idx) in suggestions" :key="idx" class="dr-suggestion-item">
          <span class="dr-suggestion-num">{{ idx + 1 }}</span>
          <span class="dr-suggestion-text">{{ s }}</span>
        </div>
      </div>

      <div v-if="!hasStructured" class="dr-empty">暂无详细诊断数据</div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  data: { type: [Object, Array], default: () => ({}) },
  text: { type: String, default: '' }
})

// 兼容输出结构嵌套（outputJson 直传或 { data } 包装）
const source = computed(() => props.data?.data && !props.data?.dimensions ? props.data.data : (props.data || {}))
const dimensions = computed(() => {
  const d = source.value.dimensions || source.value.dims || []
  return Array.isArray(d) ? d : []
})
const strengths = computed(() => {
  const s = source.value.strengths || source.value.advantages || []
  return Array.isArray(s) ? s : []
})
const weaknesses = computed(() => {
  const w = source.value.weaknesses || source.value.gaps || []
  return Array.isArray(w) ? w : []
})
const suggestions = computed(() => {
  const s = source.value.suggestions || source.value.advice || source.value.recommendations || []
  return Array.isArray(s) ? s : []
})
const score = computed(() => (source.value.score !== undefined && source.value.score !== null) ? source.value.score : null)
const levelText = computed(() => source.value.level || getLevelByScore(score.value) || '')
const levelClass = computed(() => {
  const map = { '良好': 'good', '需加强': 'medium', '薄弱': 'weak', '需提升': 'weak' }
  return map[levelText.value] || ''
})
const hasStructured = computed(() => dimensions.value.length > 0 || strengths.value.length > 0 || weaknesses.value.length > 0 || suggestions.value.length > 0 || score.value !== null)

const renderedText = computed(() => renderMarkdown(props.text || ''))

function getLevelByScore(s) {
  if (s === null || s === undefined) return ''
  if (s >= 80) return '良好'
  if (s >= 60) return '需加强'
  return '薄弱'
}
function dimLevelClass(dim) {
  const lv = dim.level || getLevelByScore(dim.score)
  const map = { '良好': 'good', '需加强': 'medium', '薄弱': 'weak', '需提升': 'weak' }
  return map[lv] || ''
}
function barColor(score) {
  if (score >= 80) return 'linear-gradient(90deg, #10b981, #00f5d4)'
  if (score >= 60) return 'linear-gradient(90deg, #f59e0b, #ffbe0b)'
  return 'linear-gradient(90deg, #ff006e, #ff4060)'
}
</script>

<style lang="scss" scoped>
.diagnosis-result { padding: 4px 0; }
.dr-fallback {
  padding: 14px; background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 10px;
  font-size: 13px; line-height: 1.8; color: #c0c0e0;
  max-height: 420px; overflow-y: auto;
  :deep(pre) { background: rgba(0, 0, 0, 0.3); padding: 12px; border-radius: 8px; overflow-x: auto; }
  :deep(code) { background: rgba(100, 100, 180, 0.12); padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 12px; color: #00f5d4; }
  :deep(pre code) { background: none; padding: 0; color: #c0c0e0; }
}
.dr-summary {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 10px 14px; background: rgba(0, 245, 212, 0.04);
  border: 1px solid rgba(0, 245, 212, 0.08); border-radius: 10px;
  font-size: 13px; color: #c0c0e0; margin-bottom: 14px; line-height: 1.5;
}
.dr-level-badge {
  font-size: 11px; padding: 2px 10px; border-radius: 5px; font-weight: 600; flex-shrink: 0;
  &.good { background: rgba(16, 185, 129, 0.12); color: #10b981; }
  &.medium { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
  &.weak { background: rgba(255, 0, 110, 0.12); color: #ff006e; }
}
.dr-summary-text { flex: 1; min-width: 120px; }
.dr-score { font-size: 16px; font-weight: 700; color: #f0f0ff; font-family: 'JetBrains Mono', monospace; }
.dr-score-unit { font-size: 10px; font-weight: 400; color: #606090; margin-left: 2px; }
.dr-dimensions { display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; }
.dr-section-title { font-size: 12px; font-weight: 600; color: #8080a8; margin-bottom: 8px; }
.dr-dim-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.dr-dim-name { font-size: 13px; font-weight: 600; color: #e0e0f0; flex: 1; }
.dr-dim-level {
  font-size: 10px; padding: 2px 8px; border-radius: 4px; font-weight: 500;
  &.good { background: rgba(16, 185, 129, 0.1); color: #10b981; }
  &.medium { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
  &.weak { background: rgba(255, 0, 110, 0.1); color: #ff006e; }
}
.dr-dim-score { font-size: 13px; font-weight: 700; color: #f0f0ff; font-family: 'JetBrains Mono', monospace; }
.dr-dim-unit { font-size: 10px; font-weight: 400; color: #606090; }
.dr-dim-bar-track { height: 6px; background: rgba(100, 100, 180, 0.08); border-radius: 3px; overflow: hidden; }
.dr-dim-bar-fill { height: 100%; border-radius: 3px; transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1); }
.dr-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px;
  @media (max-width: 720px) { grid-template-columns: 1fr; }
}
.dr-strength-item {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 8px 12px; background: rgba(16, 185, 129, 0.04);
  border: 1px solid rgba(16, 185, 129, 0.08); border-radius: 8px;
  margin-bottom: 4px; font-size: 12px; color: #c0c0e0;
}
.dr-strength-name { flex: 1; min-width: 80px; }
.dr-strength-score { font-size: 11px; font-weight: 600; color: #10b981; font-family: 'JetBrains Mono', monospace; }
.dr-strength-desc { width: 100%; font-size: 11px; color: #8080a8; }
.dr-weak-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: rgba(255, 0, 110, 0.04);
  border: 1px solid rgba(255, 0, 110, 0.08); border-radius: 8px;
  margin-bottom: 4px; font-size: 12px; color: #c0c0e0;
}
.dr-weak-name { flex: 1; }
.dr-weak-priority { font-size: 10px; padding: 1px 8px; border-radius: 4px; background: rgba(255, 0, 110, 0.1); color: #ff006e; }
.dr-suggestions { margin-bottom: 4px; }
.dr-suggestion-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 6px 12px; font-size: 12px; color: #b0b0d0;
  border-left: 2px solid rgba(0, 245, 212, 0.2);
  margin-bottom: 4px; line-height: 1.5;
}
.dr-suggestion-num {
  flex-shrink: 0; width: 18px; height: 18px; display: flex; align-items: center; justify-content: center;
  background: rgba(0, 245, 212, 0.1); color: #00f5d4; border-radius: 4px;
  font-size: 10px; font-weight: 700; font-family: 'JetBrains Mono', monospace;
}
.dr-suggestion-text { flex: 1; }
.dr-empty { text-align: center; padding: 24px; color: #606090; font-size: 13px; }
</style>
