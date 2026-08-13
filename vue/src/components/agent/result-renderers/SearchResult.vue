<template>
  <div class="search-result">
    <!-- 文本兜底：无结构化数据时展示 outputText -->
    <div v-if="!hasStructured && text" class="sr-fallback" v-html="renderedText"></div>

    <template v-else>
      <!-- 搜索头部 -->
      <div v-if="headerText" class="sr-header">
        <span class="sr-header-query">🔍 {{ data.query || '搜索结果' }}</span>
        <span v-if="total > 0" class="sr-header-count">共 {{ total }} 条结果</span>
      </div>

      <!-- 结果列表 -->
      <div v-if="results.length > 0" class="sr-list">
        <div v-for="(r, idx) in results" :key="idx" class="sr-item">
          <div class="sr-item-header">
            <span class="sr-item-num">{{ idx + 1 }}</span>
            <a v-if="r.url" class="sr-item-title" :href="r.url" target="_blank" rel="noopener">{{ r.title || r.name }}</a>
            <span v-else class="sr-item-title">{{ r.title || r.name }}</span>
            <span class="sr-item-relevance" :class="relevanceClass(r)">{{ relevanceText(r) }}</span>
          </div>
          <div v-if="r.url || r.source" class="sr-item-source">{{ r.source || r.url }}</div>
          <div v-if="r.snippet || r.summary || r.desc" class="sr-item-snippet">{{ r.snippet || r.summary || r.desc }}</div>
        </div>
      </div>

      <!-- 推荐 -->
      <div v-if="data.recommendation" class="sr-recommend">
        <span class="sr-recommend-label">📌 推荐</span>
        <span class="sr-recommend-text">{{ data.recommendation }}</span>
      </div>

      <div v-if="!hasStructured" class="sr-empty">暂无搜索结果</div>
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

const source = computed(() => props.data?.data && !props.data?.results ? props.data.data : (props.data || {}))
const results = computed(() => {
  const r = source.value.results || source.value.items || source.value.list || []
  return Array.isArray(r) ? r : []
})
const total = computed(() => source.value.total || results.value.length)
const headerText = computed(() => source.value.query || results.value.length > 0)
const hasStructured = computed(() => results.value.length > 0)

const renderedText = computed(() => renderMarkdown(props.text || ''))

/** 相关度：支持 高/中/低 与 0-1 数值两种形态 */
function relevanceText(r) {
  const rel = r.relevance ?? r.related ?? r.rating
  if (rel === undefined || rel === null || rel === '') return ''
  if (typeof rel === 'number') {
    if (rel >= 0.8 || rel >= 80) return '相关度高'
    if (rel >= 0.5 || rel >= 50) return '相关度中'
    return '相关度低'
  }
  const map = { '高': '相关度高', '中': '相关度中', '低': '相关度低', 'high': '相关度高', 'medium': '相关度中', 'low': '相关度低' }
  return map[rel] || ''
}
function relevanceClass(r) {
  const rel = r.relevance ?? r.related ?? r.rating
  if (typeof rel === 'number') {
    if (rel >= 0.8 || rel >= 80) return 'high'
    if (rel >= 0.5 || rel >= 50) return 'medium'
    return 'low'
  }
  const map = { '高': 'high', '中': 'medium', '低': 'low', 'high': 'high', 'medium': 'medium', 'low': 'low' }
  return map[rel] || ''
}
</script>

<style lang="scss" scoped>
.search-result { padding: 4px 0; }
.sr-fallback {
  padding: 14px; background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 10px;
  font-size: 13px; line-height: 1.8; color: #c0c0e0;
  max-height: 420px; overflow-y: auto;
  :deep(pre) { background: rgba(0, 0, 0, 0.3); padding: 12px; border-radius: 8px; overflow-x: auto; }
  :deep(code) { background: rgba(100, 100, 180, 0.12); padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 12px; color: #00f5d4; }
  :deep(pre code) { background: none; padding: 0; color: #c0c0e0; }
}
.sr-header {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 10px 14px; background: rgba(16, 185, 129, 0.04);
  border: 1px solid rgba(16, 185, 129, 0.08); border-radius: 10px;
  margin-bottom: 12px; font-size: 13px; color: #c0c0e0;
}
.sr-header-query { flex: 1; min-width: 120px; font-weight: 600; color: #e0e0f0; }
.sr-header-count { font-size: 11px; color: #8080a8; font-family: 'JetBrains Mono', monospace; }
.sr-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.sr-item {
  padding: 12px 14px; background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06); border-radius: 10px;
  transition: border-color 0.2s;
  &:hover { border-color: rgba(16, 185, 129, 0.15); }
}
.sr-item-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.sr-item-num {
  flex-shrink: 0; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;
  background: rgba(16, 185, 129, 0.1); color: #10b981; border-radius: 5px;
  font-size: 10px; font-weight: 700; font-family: 'JetBrains Mono', monospace;
}
.sr-item-title {
  flex: 1; font-size: 13px; font-weight: 600; color: #e0e0f0; text-decoration: none; min-width: 0;
  &:hover { color: #10b981; text-decoration: underline; }
}
.sr-item-relevance {
  flex-shrink: 0; font-size: 10px; padding: 1px 8px; border-radius: 4px; font-weight: 500;
  &.high { background: rgba(16, 185, 129, 0.1); color: #10b981; }
  &.medium { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
  &.low { background: rgba(100, 100, 180, 0.1); color: #8080a8; }
}
.sr-item-source { font-size: 11px; color: #3a86ff; font-family: 'JetBrains Mono', monospace; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sr-item-snippet { font-size: 12px; color: #9090b8; line-height: 1.6; }
.sr-recommend {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 10px 14px; background: rgba(16, 185, 129, 0.04);
  border: 1px solid rgba(16, 185, 129, 0.1); border-radius: 10px;
  font-size: 12px; line-height: 1.6;
}
.sr-recommend-label { flex-shrink: 0; font-weight: 700; color: #10b981; }
.sr-recommend-text { color: #b0b0d0; }
.sr-empty { text-align: center; padding: 24px; color: #606090; font-size: 13px; }
</style>
