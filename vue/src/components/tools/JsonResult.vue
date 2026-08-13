<template>
  <div class="json-result">
    <pre class="json-pre" v-html="highlightedJson"></pre>
    <div v-if="formattedJson" class="json-actions">
      <button class="json-copy-btn" @click="copyJson">📋 复制 JSON</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  data: {
    type: [Object, Array, String],
    default: null
  }
})

const formattedJson = computed(() => {
  if (!props.data) return ''
  if (typeof props.data === 'string') {
    try {
      return JSON.stringify(JSON.parse(props.data), null, 2)
    } catch {
      return props.data
    }
  }
  try {
    return JSON.stringify(props.data, null, 2)
  } catch {
    return String(props.data)
  }
})

const highlightedJson = computed(() => {
  const json = formattedJson.value
  if (!json) return ''
  // 简单语法高亮
  return json
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // 字符串值
    .replace(/"([^"]+)":/g, '<span class="json-key">"$1"</span>:')
    // 字符串值
    .replace(/: "([^"]*)"/g, ': <span class="json-string">"$1"</span>')
    // 数字
    .replace(/: (\d+\.?\d*)/g, ': <span class="json-number">$1</span>')
    // 布尔/null
    .replace(/: (true|false|null)/g, ': <span class="json-bool">$1</span>')
})

function copyJson() {
  navigator.clipboard.writeText(formattedJson.value).then(() => {
    ElMessage.success('JSON 已复制到剪贴板')
  }).catch(() => {
    ElMessage.warning('复制失败')
  })
}
</script>

<style lang="scss" scoped>
.json-result {
  position: relative;
}

.json-pre {
  padding: 14px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #c0c0e0;
  overflow-x: auto;
  font-family: 'JetBrains Mono', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  max-height: 400px;
  overflow-y: auto;

  :deep(.json-key) { color: #7b61ff; }
  :deep(.json-string) { color: #10b981; }
  :deep(.json-number) { color: #f59e0b; }
  :deep(.json-bool) { color: #3a86ff; }
}

.json-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.json-copy-btn {
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
    color: #00f5d4;
    border-color: rgba(0, 245, 212, 0.2);
    background: rgba(0, 245, 212, 0.06);
  }
}
</style>
