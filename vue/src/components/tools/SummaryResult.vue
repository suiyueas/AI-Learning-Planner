<template>
  <div class="summary-result">
    <div v-if="title" class="summary-header">
      <span class="summary-icon">📝</span>
      <span class="summary-title">{{ title }}</span>
    </div>

    <div v-if="summaryText" class="summary-content">
      <div class="content-text">{{ summaryText }}</div>
    </div>

    <div v-if="keywords && keywords.length > 0" class="keywords-section">
      <div class="keywords-label">🏷️ 关键词</div>
      <div class="keywords-list">
        <span v-for="(kw, idx) in keywords" :key="idx" class="keyword-tag">{{ kw }}</span>
      </div>
    </div>

    <div v-if="showOrigin && originText" class="origin-section">
      <button class="origin-toggle" @click="toggleOrigin">
        {{ showOriginContent ? '🙈 收起原文' : '📄 查看原文' }}
      </button>
      <div v-if="showOriginContent" class="origin-content">
        <div class="origin-label">原文摘要</div>
        <div class="origin-text">{{ originText }}</div>
      </div>
    </div>

    <div v-if="showRaw && rawContent" class="raw-content">
      <div class="raw-label">返回数据</div>
      <pre class="raw-json">{{ rawContent }}</pre>
    </div>

    <div v-else-if="!summaryText && !rawContent" class="result-empty">
      ⚠️ 未获取到摘要内容
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  data: {
    type: [Object, Array],
    default: null
  }
})

const showOriginContent = ref(false)
const showOrigin = ref(false)

const title = computed(() => {
  if (!props.data) return null
  return props.data.title || props.data.name || props.data.topic || '文档摘要'
})

const summaryText = computed(() => {
  if (!props.data) return null
  if (typeof props.data === 'string') return props.data
  return props.data.summary || props.data.content || props.data.abstract || props.data.text || null
})

const keywords = computed(() => {
  if (!props.data) return []
  if (Array.isArray(props.data.keywords)) return props.data.keywords
  if (Array.isArray(props.data.tags)) return props.data.tags
  if (typeof props.data.keywords === 'string') return props.data.keywords.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
  return []
})

const originText = computed(() => {
  if (!props.data) return null
  return props.data.original || props.data.origin || props.data.source || null
})

const showRaw = ref(false)
const rawContent = ref('')

onMounted(() => {
  if (!summaryText.value && !keywords.value.length && props.data) {
    showRaw.value = true
    try {
      rawContent.value = typeof props.data === 'string'
        ? props.data
        : JSON.stringify(props.data, null, 2)
    } catch {
      rawContent.value = String(props.data)
    }
  }
  if (originText.value) {
    showOrigin.value = true
  }
})

function toggleOrigin() {
  showOriginContent.value = !showOriginContent.value
}
</script>

<style lang="scss" scoped>
.summary-result {
  padding: 4px 0;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.summary-icon { font-size: 1.2rem; }

.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: #f0f0ff;
  font-family: 'JetBrains Mono', monospace;
}

.summary-content {
  padding: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  margin-bottom: 14px;
}

.content-text {
  font-size: 13px;
  line-height: 1.7;
  color: #c0c0e0;
  white-space: pre-wrap;
}

.keywords-section {
  margin-bottom: 14px;
}

.keywords-label {
  font-size: 12px;
  font-weight: 600;
  color: #8080a8;
  margin-bottom: 8px;
}

.keywords-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-tag {
  padding: 3px 10px;
  background: rgba(0, 245, 212, 0.06);
  border: 1px solid rgba(0, 245, 212, 0.1);
  border-radius: 6px;
  font-size: 11px;
  color: #00f5d4;
  font-family: 'JetBrains Mono', monospace;
}

.origin-section {
  margin-bottom: 8px;
}

.origin-toggle {
  padding: 6px 14px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 6px;
  color: #8080a8;
  font-size: 11px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba(0, 245, 212, 0.2);
    color: #00f5d4;
    background: rgba(0, 245, 212, 0.06);
  }
}

.origin-content {
  margin-top: 10px;
  padding: 12px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 8px;
}

.origin-label {
  font-size: 11px;
  font-weight: 600;
  color: #8080a8;
  margin-bottom: 6px;
}

.origin-text {
  font-size: 12px;
  line-height: 1.6;
  color: #9090b8;
  white-space: pre-wrap;
}

.raw-content {
  margin-top: 8px;
  .raw-label {
    font-size: 12px;
    font-weight: 600;
    color: #8080a8;
    margin-bottom: 8px;
  }
  .raw-json {
    padding: 12px;
    background: rgba(100, 100, 180, 0.04);
    border: 1px solid rgba(100, 100, 180, 0.08);
    border-radius: 8px;
    font-size: 12px;
    line-height: 1.6;
    color: #c0c0e0;
    overflow-x: auto;
    font-family: 'JetBrains Mono', monospace;
    white-space: pre-wrap;
    word-break: break-all;
    margin: 0;
    max-height: 300px;
    overflow-y: auto;
  }
}

.result-empty {
  text-align: center;
  padding: 24px;
  color: #8080a8;
  font-size: 13px;
}
</style>
