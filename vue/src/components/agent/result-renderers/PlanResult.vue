<template>
  <div class="plan-result">
    <!-- 文本兜底：无结构化数据时展示 outputText -->
    <div v-if="!hasStructured && text" class="pr-fallback" v-html="renderedText"></div>

    <template v-else>
      <!-- 计划总览 -->
      <div v-if="overviewText" class="pr-overview">
        <span class="pr-overview-title">{{ data.title || '学习计划' }}</span>
        <span v-if="totalDuration" class="pr-overview-meta">{{ totalDuration }}</span>
        <div v-if="data.overview" class="pr-overview-desc">{{ data.overview }}</div>
      </div>

      <!-- 阶段时间轴 -->
      <div v-if="phases.length > 0" class="pr-phases">
        <div v-for="(phase, idx) in phases" :key="idx" class="pr-phase">
          <div class="pr-phase-timeline">
            <span class="pr-phase-dot"></span>
            <span v-if="idx < phases.length - 1" class="pr-phase-line"></span>
          </div>
          <div class="pr-phase-card">
            <div class="pr-phase-header">
              <span class="pr-phase-num">Phase {{ phase.phase || idx + 1 }}</span>
              <span class="pr-phase-title">{{ phase.title }}</span>
              <span class="pr-phase-duration">{{ phase.duration }}</span>
            </div>

            <!-- 阶段目标 -->
            <div v-if="goalsOf(phase).length > 0" class="pr-block">
              <span class="pr-block-label">🎯 目标</span>
              <div v-for="(g, gi) in goalsOf(phase)" :key="gi" class="pr-goal-item">{{ g }}</div>
            </div>

            <!-- 学习内容 -->
            <div v-if="contentOf(phase).length > 0" class="pr-block">
              <span class="pr-block-label">📚 学习内容</span>
              <div v-for="(c, ci) in contentOf(phase)" :key="ci" class="pr-content-item">• {{ c }}</div>
            </div>

            <!-- 实践任务 -->
            <div v-if="tasksOf(phase).length > 0" class="pr-block">
              <span class="pr-block-label">📝 实践任务</span>
              <div v-for="(t, ti) in tasksOf(phase)" :key="ti" class="pr-task-item">
                <span class="pr-task-icon">🛠</span>{{ t }}
              </div>
            </div>

            <!-- 里程碑 -->
            <div v-if="milestonesOf(phase).length > 0" class="pr-block">
              <span class="pr-block-label">🏁 里程碑</span>
              <div v-for="(m, mi) in milestonesOf(phase)" :key="mi" class="pr-milestone-item">{{ m }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 推荐资源 -->
      <div v-if="resources.length > 0" class="pr-resources">
        <div class="pr-section-title">🔗 推荐资源</div>
        <div v-for="(r, idx) in resources" :key="idx" class="pr-resource-item">
          <span v-if="r.type" class="pr-resource-type">{{ r.type }}</span>
          <span class="pr-resource-text">{{ r.name || r.title || r }}</span>
          <a v-if="r.url" class="pr-resource-link" :href="r.url" target="_blank" rel="noopener">↗</a>
        </div>
      </div>

      <!-- 总结 -->
      <div v-if="data.summary" class="pr-summary">
        <div class="pr-section-title">📌 总结</div>
        <div class="pr-summary-text">{{ data.summary }}</div>
      </div>

      <div v-if="!hasStructured" class="pr-empty">暂无详细计划数据</div>
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

const source = computed(() => props.data?.data && !props.data?.phases ? props.data.data : (props.data || {}))
const phases = computed(() => {
  const p = source.value.phases || source.value.stages || []
  return Array.isArray(p) ? p : []
})
const resources = computed(() => {
  const r = source.value.resources || source.value.recommendations || []
  return Array.isArray(r) ? r : []
})
const totalDuration = computed(() => {
  const s = source.value
  const parts = []
  if (s.totalWeeks || s.duration) parts.push(`总时长：${s.totalWeeks ? s.totalWeeks + ' 周' : s.duration}`)
  if (s.totalHours) parts.push(`预估投入：${s.totalHours} 小时`)
  return parts.join(' | ')
})
const overviewText = computed(() => dataTitle.value || totalDuration.value)
const dataTitle = computed(() => source.value.title || '')
const hasStructured = computed(() => phases.value.length > 0 || resources.value.length > 0 || totalDuration.value)

const renderedText = computed(() => renderMarkdown(props.text || ''))

const goalsOf = (p) => {
  const g = p.goals || p.objectives || []
  return Array.isArray(g) ? g : (typeof g === 'string' ? [g] : [])
}
const contentOf = (p) => {
  const c = p.content || p.topics || p.learnings || []
  return Array.isArray(c) ? c : (typeof c === 'string' ? [c] : [])
}
const tasksOf = (p) => {
  const t = p.tasks || p.practices || []
  return Array.isArray(t) ? t : (typeof t === 'string' ? [t] : [])
}
const milestonesOf = (p) => {
  const m = p.milestones || []
  return Array.isArray(m) ? m : []
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.plan-result { padding: 4px 0; }
.pr-fallback {
  padding: 14px; background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.08); border-radius: $radius-md;
  font-size: 13px; line-height: 1.8; color: $text-secondary;
  max-height: 420px; overflow-y: auto;
  :deep(pre) { background: rgba(0, 0, 0, 0.3); padding: 12px; border-radius: $radius-sm; overflow-x: auto; }
  :deep(code) { background: rgba($accent-primary, 0.12); padding: 2px 6px; border-radius: 4px; font-family: $font-mono; font-size: 12px; color: $accent-emerald; }
  :deep(pre code) { background: none; padding: 0; color: $text-secondary; }
}
.pr-overview {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 12px 14px; background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.08); border-radius: $radius-md;
  font-size: 13px; color: $text-secondary; margin-bottom: 14px; line-height: 1.6;
}
.pr-overview-title { font-size: 14px; font-weight: 700; color: $text-primary; }
.pr-overview-meta { font-size: 11px; color: $text-muted; font-family: $font-mono; }
.pr-overview-desc { width: 100%; font-size: 12px; color: $text-muted; }
.pr-phases { display: flex; flex-direction: column; margin-bottom: 16px; }
.pr-phase { display: flex; gap: 12px; }
.pr-phase-timeline { display: flex; flex-direction: column; align-items: center; width: 14px; flex-shrink: 0; padding-top: 20px; }
.pr-phase-dot { width: 10px; height: 10px; border-radius: 50%; background: $accent-primary; box-shadow: 0 0 0 3px rgba($accent-primary, 0.15); flex-shrink: 0; }
.pr-phase-line { flex: 1; width: 2px; background: rgba($accent-primary, 0.15); margin: 2px 0; }
.pr-phase-card {
  flex: 1; padding: 14px; background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06); border-radius: $radius-lg;
  margin-bottom: 10px;
}
.pr-phase-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.pr-phase-num {
  font-size: 10px; padding: 2px 8px; border-radius: 4px;
  background: rgba($accent-primary, 0.1); color: $accent-primary; font-weight: 600; font-family: $font-mono;
}
.pr-phase-title { flex: 1; font-size: 13px; font-weight: 600; color: $text-primary; min-width: 100px; }
.pr-phase-duration { font-size: 11px; color: $text-muted; font-family: $font-mono; }
.pr-block { margin-bottom: 8px; &:last-child { margin-bottom: 0; } }
.pr-block-label { display: inline-block; font-size: 11px; font-weight: 600; color: $text-secondary; margin-bottom: 4px; }
.pr-goal-item { font-size: 12px; color: $text-secondary; line-height: 1.5; padding: 2px 0; }
.pr-content-item { font-size: 12px; color: $text-muted; line-height: 1.6; padding: 2px 0; }
.pr-task-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: $text-secondary; padding: 4px 8px; background: rgba($accent-primary, 0.03); border-radius: 6px;
  margin-bottom: 3px;
}
.pr-task-icon { flex-shrink: 0; font-size: 10px; }
.pr-milestone-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: $accent-amber; padding: 3px 0;
  &::before { content: '🏁'; font-size: 10px; }
}
.pr-section-title { font-size: 12px; font-weight: 600; color: $text-muted; margin-bottom: 8px; }
.pr-resources { margin-bottom: 16px; }
.pr-resource-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; margin-bottom: 3px; font-size: 12px; color: $text-muted;
  border-left: 2px solid rgba($accent-primary, 0.2); line-height: 1.4;
}
.pr-resource-type { font-size: 10px; padding: 1px 6px; border-radius: 3px; background: rgba($accent-primary, 0.1); color: $accent-primary; flex-shrink: 0; }
.pr-resource-text { flex: 1; }
.pr-resource-link { color: $accent-emerald; text-decoration: none; flex-shrink: 0; }
.pr-summary { margin-bottom: 4px; }
.pr-summary-text { padding: 10px; background: rgba($accent-primary, 0.04); border-radius: $radius-sm; font-size: 12px; line-height: 1.6; color: $text-secondary; }
.pr-empty { text-align: center; padding: 24px; color: $text-muted; font-size: 13px; }
</style>