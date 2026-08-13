<template>
  <div class="markdown-result">
    <div v-if="content" class="mr-body markdown-body" v-html="rendered"></div>
    <div v-else class="mr-empty">
      <span class="mr-empty-ico">📭</span>
      <span>无返回内容</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  data: { type: [Object, String], default: '' },
  text: { type: String, default: '' }
})

// 兼容：直接字符串 / { content | outputText | text } / { data: { result } }
const content = computed(() => {
  if (props.text && props.text.trim()) return props.text.trim()
  const d = props.data
  if (!d) return ''
  if (typeof d === 'string') return d.trim()
  if (typeof d.outputText === 'string' && d.outputText.trim()) return d.outputText.trim()
  if (typeof d.content === 'string' && d.content.trim()) return d.content.trim()
  if (typeof d.text === 'string' && d.text.trim()) return d.text.trim()
  if (typeof d.result === 'string' && d.result.trim()) return d.result.trim()
  if (typeof d.data?.result === 'string' && d.data.result.trim()) return d.data.result.trim()
  return ''
})

// 为 Markdown 渲染结果中的代码块逐行包裹 span，配合 CSS counter 显示行号（无额外依赖）
const addCodeLineNumbers = (html) => {
  if (!html) return html
  return html.replace(/<pre><code([^>]*)>([\s\S]*?)<\/code><\/pre>/g, (match, attrs, code) => {
    const lines = code.replace(/\n$/, '').split('\n')
    const wrapped = lines.map(line => `<span class="code-line">${line || ' '}</span>`).join('\n')
    return `<pre><code${attrs}>${wrapped}</code></pre>`
  })
}

const rendered = computed(() => addCodeLineNumbers(renderMarkdown(content.value)))
</script>

<style lang="scss" scoped>
.markdown-result { padding: 4px 0; }
.mr-body {
  padding: 14px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.8;
  color: #c0c0e0;
  max-height: 440px;
  overflow-y: auto;
}
.mr-empty {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 40px 20px; color: #606090; font-size: 13px;
}
.mr-empty-ico { font-size: 2rem; opacity: 0.5; }

:deep(h1), :deep(h2), :deep(h3), :deep(h4) {
  color: #e8e8ff; margin: 16px 0 8px; font-weight: 600;
}
:deep(h1) { font-size: 18px; }
:deep(h2) { font-size: 16px; }
:deep(h3) { font-size: 14px; }
:deep(p) { margin: 8px 0; }
:deep(ul), :deep(ol) { padding-left: 20px; margin: 8px 0; }
:deep(li) { margin: 4px 0; }
:deep(code) {
  background: rgba(100, 100, 180, 0.12); padding: 2px 6px; border-radius: 4px;
  font-family: 'JetBrains Mono', monospace; font-size: 12px; color: #00f5d4;
}
:deep(pre) {
  background: rgba(0, 0, 0, 0.3); padding: 12px; border-radius: 8px;
  overflow-x: auto; margin: 12px 0;
  counter-reset: code-line;
}
:deep(pre code) { background: none; padding: 0; color: #c0c0e0; }
:deep(pre .code-line) {
  display: block; counter-increment: code-line; padding-left: 32px; position: relative;
  &::before {
    content: counter(code-line);
    position: absolute; left: 0; width: 24px; text-align: right;
    color: rgba(128, 128, 168, 0.4); font-size: 11px; user-select: none;
  }
}
:deep(blockquote) {
  border-left: 3px solid #00E5FF; padding: 6px 14px; margin: 10px 0;
  background: rgba(0, 229, 255, 0.04); color: #a0a0c8;
}
:deep(strong) { color: #f0f0ff; }
:deep(a) { color: #00E5FF; text-decoration: none; &:hover { text-decoration: underline; } }
:deep(hr) { border: none; border-top: 1px solid rgba(100, 100, 180, 0.12); margin: 16px 0; }
:deep(table) { width: 100%; border-collapse: collapse; margin: 12px 0; }
:deep(th), :deep(td) { border: 1px solid rgba(100, 100, 180, 0.15); padding: 6px 10px; font-size: 12px; }
:deep(th) { background: rgba(100, 100, 180, 0.08); color: #e0e0f0; }
</style>
