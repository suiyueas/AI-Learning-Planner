<template>
  <div class="knowledge-result">
    <!-- 文本兜底：无结构化数据时展示 outputText -->
    <div v-if="!hasStructured && text" class="kr-fallback" v-html="renderedText"></div>

    <template v-else>
      <!-- 检索头部 -->
      <div v-if="headerText" class="kr-header">
        <span class="kr-header-query">📚 {{ data.query || '知识检索结果' }}</span>
        <span v-if="total > 0" class="kr-header-count">共 {{ total }} 个知识块</span>
      </div>

      <!-- 知识块列表 -->
      <div v-if="blocks.length > 0" class="kr-list">
        <div v-for="(b, idx) in blocks" :key="idx" class="kr-item">
          <div class="kr-item-header">
            <span class="kr-item-num">{{ idx + 1 }}</span>
            <span class="kr-item-source">📄 {{ b.source || b.document || b.docName || '知识文档' }}</span>
            <span v-if="b.title" class="kr-item-title">{{ b.title }}</span>
            <span v-if="b.score !== undefined && b.score !== null" class="kr-item-score">{{ formatScore(b.score) }}</span>
          </div>
          <div v-if="b.content" class="kr-item-content">{{ b.content }}</div>
        </div>
      </div>

      <!-- 推荐 -->
      <div v-if="data.recommendation" class="kr-recommend">
        <span class="kr-recommend-label">📌 建议</span>
        <span class="kr-recommend-text">{{ data.recommendation }}</span>
      </div>

      <div v-if="!hasStructured" class="kr-empty">暂无检索结果</div>
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

const source = computed(() => props.data?.data && !props.data?.blocks ? props.data.data : (props.data || {}))
const blocks = computed(() => {
  const b = source.value.blocks || source.value.knowledgeBlocks || source.value.chunks || source.value.items || []
  return Array.isArray(b) ? b : []
})
const total = computed(() => source.value.total || blocks.value.length)
const headerText = computed(() => source.value.query || blocks.value.length > 0)
const hasStructured = computed(() => blocks.value.length > 0)

const renderedText = computed(() => renderMarkdown(props.text || ''))

/** 相关度：支持 0-1 / 0-100 / 百分比字符串 */
function formatScore(score) {
  if (typeof score === 'number') {
    return score <= 1 ? `${Math.round(score * 100)}%` : `${score}%`
  }
  const s = String(score)
  return s.includes('%') ? s : `${s}%`
}
</script>

<style lang="scss" scoped>
.knowledge-result { padding: 4px 0; }
.kr-fallback {
  padding: 14px; background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 10px;
  font-size: 13px; line-height: 1.8; color: #c0c0e0;
  max-height: 420px; overflow-y: auto;
  :deep(pre) { background: rgba(0, 0, 0, 0.3); padding: 12px; border-radius: 8px; overflow-x: auto; }
  :deep(code) { background: rgba(100, 100, 180, 0.12); padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 12px; color: #00f5d4; }
  :deep(pre code) { background: none; padding: 0; color: #c0c0e0; }
}
.kr-header {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 10px 14px; background: rgba(167, 139, 250, 0.05);
  border: 1px solid rgba(167, 139, 250, 0.1); border-radius: 10px;
  margin-bottom: 12px; font-size: 13px; color: #c0c0e0;
}
.kr-header-query { flex: 1; min-width: 120px; font-weight: 600; color: #e0e0f0; }
.kr-header-count { font-size: 11px; color: #8080a8; font-family: 'JetBrains Mono', monospace; }
.kr-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.kr-item {
  padding: 12px 14px; background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06); border-radius: 10px;
  transition: border-color 0.2s;
  &:hover { border-color: rgba(167, 139, 250, 0.2); }
}
.kr-item-header { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 6px; }
.kr-item-num {
  flex-shrink: 0; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;
  background: rgba(167, 139, 250, 0.12); color: #a78bfa; border-radius: 5px;
  font-size: 10px; font-weight: 700; font-family: 'JetBrains Mono', monospace;
}
.kr-item-source { flex-shrink: 0; font-size: 11px; font-weight: 600; color: #a78bfa; }
.kr-item-title { flex: 1; min-width: 80px; font-size: 12px; color: #b0b0d0; }
.kr-item-score {
  flex-shrink: 0; font-size: 10px; padding: 1px 8px; border-radius: 4px;
  background: rgba(167, 139, 250, 0.1); color: #a78bfa; font-family: 'JetBrains Mono', monospace;
}
.kr-item-content {
  font-size: 12px; color: #9090b8; line-height: 1.7;
  white-space: pre-wrap; word-break: break-word;
  max-height: 160px; overflow-y: auto;
  padding: 8px 10px; background: rgba(100, 100, 180, 0.03); border-radius: 6px;
}
.kr-recommend {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 10px 14px; background: rgba(167, 139, 250, 0.05);
  border: 1px solid rgba(167, 139, 250, 0.1); border-radius: 10px;
  font-size: 12px; line-height: 1.6;
}
.kr-recommend-label { flex-shrink: 0; font-weight: 700; color: #a78bfa; }
.kr-recommend-text { color: #b0b0d0; }
.kr-empty { text-align: center; padding: 24px; color: #606090; font-size: 13px; }
</style>
