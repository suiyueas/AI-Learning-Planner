<template>
  <div class="report-result">
    <!-- 文本兜底：无结构化数据时展示 outputText -->
    <div v-if="!hasStructured && text" class="rr-fallback" v-html="renderedText"></div>

    <template v-else>
      <!-- 报告摘要 -->
      <div v-if="data.summary || data.period" class="rr-summary">
        <span class="rr-summary-title">{{ data.title || '学习报告' }}</span>
        <span v-if="data.period" class="rr-summary-period">{{ data.period }}</span>
        <div v-if="data.summary" class="rr-summary-text">{{ data.summary }}</div>
      </div>

      <!-- 概览数字卡片 -->
      <div v-if="metrics.length > 0" class="rr-metrics">
        <div v-for="(m, idx) in metrics" :key="idx" class="rr-metric-card">
          <span class="rr-metric-label">{{ m.label }}</span>
          <span class="rr-metric-value">{{ m.value }}</span>
          <span v-if="m.change" class="rr-metric-change" :class="{ up: String(m.change).includes('+'), down: String(m.change).includes('-') }">{{ m.change }}</span>
        </div>
      </div>

      <!-- 能力成长 -->
      <div v-if="capabilities.length > 0" class="rr-capabilities">
        <div class="rr-section-title">📊 能力成长</div>
        <div v-for="(cap, idx) in capabilities" :key="idx" class="rr-cap-item">
          <span class="rr-cap-name">{{ cap.name || cap.label }}</span>
          <div class="rr-cap-track">
            <div class="rr-cap-fill" :style="{ width: Math.min(100, cap.score || 0) + '%', background: capColor(cap.score) }"></div>
          </div>
          <span class="rr-cap-score">{{ cap.score || 0 }}%</span>
        </div>
      </div>

      <!-- 学习详情表格 -->
      <div v-if="details.length > 0" class="rr-details">
        <div class="rr-section-title">📋 学习详情</div>
        <div class="rr-table">
          <div class="rr-table-header">
            <span class="rr-td rr-td-date">日期</span>
            <span class="rr-td rr-td-content">学习内容</span>
            <span class="rr-td rr-td-status">状态</span>
            <span class="rr-td rr-td-duration">时长</span>
          </div>
          <div v-for="(d, idx) in details" :key="idx" class="rr-table-row">
            <span class="rr-td rr-td-date">{{ d.date || d.day || '—' }}</span>
            <span class="rr-td rr-td-content">{{ d.content || d.name || '—' }}</span>
            <span class="rr-td rr-td-status">
              <span :class="d.status === '完成' || d.status === 'completed' || d.done ? 'rr-status-ok' : 'rr-status-pending'">
                {{ d.status === '完成' || d.status === 'completed' || d.done ? '✅' : '⏳' }}
              </span>
            </span>
            <span class="rr-td rr-td-duration">{{ d.duration || d.time || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 下周计划 -->
      <div v-if="nextPlan.length > 0" class="rr-next-plan">
        <div class="rr-section-title">🎯 下周计划</div>
        <div v-for="(p, idx) in nextPlan" :key="idx" class="rr-next-item">• {{ p }}</div>
      </div>

      <!-- 建议 -->
      <div v-if="suggestions.length > 0" class="rr-suggestions">
        <div class="rr-section-title">💡 学习建议</div>
        <div v-for="(s, idx) in suggestions" :key="idx" class="rr-suggestion">• {{ s }}</div>
      </div>

      <div v-if="!hasStructured" class="rr-empty">暂无详细报告数据</div>
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

const source = computed(() => props.data?.data && !props.data?.metrics ? props.data.data : (props.data || {}))
const metrics = computed(() => {
  const m = source.value.metrics || source.value.overview || []
  return Array.isArray(m) ? m : []
})
const capabilities = computed(() => {
  const c = source.value.capabilities || source.value.skills || source.value.ability || []
  return Array.isArray(c) ? c : []
})
const details = computed(() => {
  const d = source.value.details || source.value.records || []
  return Array.isArray(d) ? d : []
})
const suggestions = computed(() => {
  const s = source.value.suggestions || source.value.advice || []
  return Array.isArray(s) ? s : []
})
const nextPlan = computed(() => {
  const p = source.value.nextPlan || source.value.next_plan || source.value.plan || []
  return Array.isArray(p) ? p : []
})
const hasStructured = computed(() => metrics.value.length > 0 || capabilities.value.length > 0 || details.value.length > 0 || suggestions.value.length > 0 || nextPlan.value.length > 0)

const renderedText = computed(() => renderMarkdown(props.text || ''))

function capColor(score) {
  if (score >= 80) return 'linear-gradient(90deg, #f59e0b, #ffbe0b)'
  if (score >= 60) return 'linear-gradient(90deg, #3a86ff, #00c2ff)'
  return 'linear-gradient(90deg, #ff006e, #ff4060)'
}
</script>

<style lang="scss" scoped>
.report-result { padding: 4px 0; }
.rr-fallback {
  padding: 14px; background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 10px;
  font-size: 13px; line-height: 1.8; color: #c0c0e0;
  max-height: 420px; overflow-y: auto;
  :deep(pre) { background: rgba(0, 0, 0, 0.3); padding: 12px; border-radius: 8px; overflow-x: auto; }
  :deep(code) { background: rgba(100, 100, 180, 0.12); padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 12px; color: #00f5d4; }
  :deep(pre code) { background: none; padding: 0; color: #c0c0e0; }
}
.rr-summary {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 10px 14px; background: rgba(245, 158, 11, 0.04);
  border: 1px solid rgba(245, 158, 11, 0.08); border-radius: 10px;
  font-size: 13px; color: #c0c0e0; margin-bottom: 14px; line-height: 1.5;
}
.rr-summary-title { font-size: 14px; font-weight: 700; color: #e8e8ff; }
.rr-summary-period { font-size: 11px; color: #f59e0b; font-family: 'JetBrains Mono', monospace; }
.rr-summary-text { width: 100%; font-size: 12px; color: #9090b8; }
.rr-metrics { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; margin-bottom: 16px;
  @media (min-width: 900px) { grid-template-columns: repeat(4, 1fr); }
}
.rr-metric-card {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 14px 8px; background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06); border-radius: 10px;
}
.rr-metric-label { font-size: 11px; color: #8080a8; text-align: center; }
.rr-metric-value { font-size: 1.1rem; font-weight: 700; color: #f59e0b; font-family: 'JetBrains Mono', monospace; }
.rr-metric-change { font-size: 10px; font-weight: 500;
  &.up { color: #10b981; } &.down { color: #ff006e; }
}
.rr-section-title { font-size: 12px; font-weight: 600; color: #8080a8; margin-bottom: 8px; }
.rr-capabilities { margin-bottom: 16px; }
.rr-cap-item { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.rr-cap-name { width: 90px; flex-shrink: 0; font-size: 12px; color: #c0c0e0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rr-cap-track { flex: 1; height: 8px; background: rgba(100, 100, 180, 0.08); border-radius: 4px; overflow: hidden; }
.rr-cap-fill { height: 100%; border-radius: 4px; transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1); }
.rr-cap-score { width: 42px; flex-shrink: 0; text-align: right; font-size: 11px; font-weight: 600; color: #b0b0d0; font-family: 'JetBrains Mono', monospace; }
.rr-details { margin-bottom: 16px; }
.rr-table { background: rgba(255, 255, 255, 0.03); border-radius: 10px; border: 1px solid rgba(255, 255, 255, 0.06); overflow: hidden; }
.rr-table-header {
  display: flex; padding: 8px 12px;
  background: rgba(100, 100, 180, 0.06); border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  font-size: 11px; font-weight: 600; color: #8080a8;
}
.rr-table-row {
  display: flex; padding: 8px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03); font-size: 12px; color: #b0b0d0;
  &:last-child { border-bottom: none; }
  &:hover { background: rgba(255, 255, 255, 0.02); }
}
.rr-td { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rr-td-date { flex: 0 0 80px; }
.rr-td-content { flex: 2; }
.rr-td-status { flex: 0 0 40px; text-align: center; }
.rr-td-duration { flex: 0 0 60px; text-align: right; color: #8080a8; font-family: 'JetBrains Mono', monospace; }
.rr-next-plan { margin-bottom: 16px; }
.rr-next-item { font-size: 12px; color: #b0b0d0; line-height: 1.7; padding: 2px 0; }
.rr-suggestions { margin-bottom: 4px; }
.rr-suggestion { padding: 5px 12px; font-size: 12px; color: #b0b0d0; line-height: 1.5; border-left: 2px solid rgba(245, 158, 11, 0.2); margin-bottom: 3px; }
.rr-empty { text-align: center; padding: 24px; color: #606090; font-size: 13px; }
</style>
