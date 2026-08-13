<template>
  <div class="resource-result">
    <div v-if="summary" class="result-summary">
      <span class="summary-icon">📚</span>
      <span class="summary-text">{{ summary }}</span>
    </div>

    <div v-if="resources && resources.length > 0" class="resource-list">
      <div
        v-for="(res, idx) in resources"
        :key="idx"
        class="resource-item"
        @click="toggleDetail(idx)"
      >
        <div class="resource-item-header">
          <span class="res-icon">{{ getFileIcon(res.type) }}</span>
          <span class="res-title">{{ res.title || res.name || '未命名资源' }}</span>
          <span v-if="res.relevance !== undefined" class="res-relevance">
            {{ formatRelevance(res.relevance) }}
          </span>
        </div>
        <div class="resource-item-meta">
          <span class="res-tag">{{ res.type || res.fileType || '未知类型' }}</span>
          <span v-if="res.chunks !== undefined || res.chunkCount !== undefined" class="res-chunks">
            {{ res.chunks || res.chunkCount || 0 }} 个片段
          </span>
          <span v-if="res.status" class="res-status">
            {{ res.status === 'ready' || res.status === '就绪' ? '✅ 已就绪' : res.status }}
          </span>
        </div>
        <div v-if="res.description || res.summary" class="resource-item-desc">
          {{ truncateText(res.description || res.summary, 120) }}
        </div>
        <div class="resource-item-footer">
          <button class="res-action-btn" @click.stop="viewDetail(res)">查看详情</button>
          <button class="res-action-btn" @click.stop="copyResource(res)">📋 复制</button>
        </div>
      </div>
    </div>

    <div v-else-if="showRaw && rawContent" class="raw-content">
      <div class="raw-label">返回数据</div>
      <pre class="raw-json">{{ rawContent }}</pre>
    </div>

    <div v-else-if="!resources || resources.length === 0" class="result-empty">
      ⚠️ 未检索到相关资源
    </div>
  </div>

  <!-- 资源详情弹窗 -->
  <div v-if="showResourceDetail && selectedResource" class="resource-detail-overlay" @click="closeResourceDetail">
    <div class="resource-detail-dialog" @click.stop>
      <div class="rd-header">
        <div class="rd-header-left">
          <span class="rd-type-icon">{{ getFileIcon(selectedResource.type) }}</span>
          <div class="rd-header-info">
            <h3>{{ selectedResource.title || selectedResource.name || '未命名资源' }}</h3>
            <p>{{ selectedResource.type || selectedResource.fileType || '未知类型' }}</p>
          </div>
        </div>
        <button class="rd-close" @click="closeResourceDetail">✕</button>
      </div>

      <div class="rd-body">
        <div class="rd-meta">
          <span class="rd-meta-item"><span class="meta-lbl">资源类型：</span>{{ selectedResource.type || selectedResource.fileType || '未知' }}</span>
          <span class="rd-meta-item"><span class="meta-lbl">文件大小：</span>{{ selectedResource.size || selectedResource.fileSize || '未知' }}</span>
          <span class="rd-meta-item"><span class="meta-lbl">相关度：</span><span class="meta-highlight">{{ formatRelevance(selectedResource.relevance) }}</span></span>
          <span class="rd-meta-item"><span class="meta-lbl">知识块：</span>{{ selectedResource.chunks || selectedResource.chunkCount || 0 }} 个</span>
          <span class="rd-meta-item"><span class="meta-lbl">处理状态：</span><span class="meta-status" :class="selectedResource.status">{{ selectedResource.status === 'ready' || selectedResource.status === '就绪' ? '已就绪' : (selectedResource.status || '未知') }}</span></span>
        </div>

        <div class="rd-section">
          <div class="rd-section-title">📝 内容预览</div>
          <div class="rd-preview">{{ truncateText(selectedResource.description || selectedResource.summary || selectedResource.content || '无预览内容', 200) }}</div>
        </div>

        <div v-if="selectedResource.chunksList && selectedResource.chunksList.length > 0" class="rd-section">
          <div class="rd-section-title">📚 知识块列表 ({{ selectedResource.chunksList.length }} 个)</div>
          <div class="rd-chunks-list">
            <div v-for="(chunk, idx) in selectedResource.chunksList" :key="idx" class="rd-chunk-item">
              <span class="chunk-index">#{{ idx + 1 }}</span>
              <span class="chunk-content">{{ chunk.content || chunk }}</span>
            </div>
          </div>
        </div>
        <div v-else-if="selectedResource.chunks && selectedResource.chunks > 0" class="rd-section">
          <div class="rd-section-title">📚 片段列表</div>
          <div class="rd-chunks-list">
            <div v-for="i in Math.min(selectedResource.chunks, 10)" :key="i" class="rd-chunk-item">
              <span class="chunk-index">#{{ i }}</span>
              <span class="chunk-content">知识块片段 {{ i }}</span>
            </div>
            <div v-if="selectedResource.chunks > 10" class="more-hint">还有 {{ selectedResource.chunks - 10 }} 个片段...</div>
          </div>
        </div>
      </div>

      <div class="rd-footer">
        <button class="rd-btn rd-btn-ghost" @click="copyResource(selectedResource)">📋 复制资源</button>
        <button class="rd-btn rd-btn-cancel" @click="closeResourceDetail">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  data: {
    type: [Object, Array],
    default: null
  }
})

const expandedItems = ref({})
const showResourceDetail = ref(false)
const selectedResource = ref(null)

const resources = computed(() => {
  if (!props.data) return null
  // 支持多种数据格式
  if (Array.isArray(props.data)) return props.data
  if (props.data.resources) return props.data.resources
  if (props.data.records) return props.data.records
  if (props.data.items) return props.data.items
  if (props.data.data && Array.isArray(props.data.data)) return props.data.data
  return null
})

const summary = computed(() => {
  if (!props.data) return null
  if (props.data.summary) return props.data.summary
  if (props.data.message) return props.data.message
  if (resources.value && resources.value.length > 0) {
    return `共检索到 ${resources.value.length} 个资源`
  }
  return null
})

const showRaw = ref(false)
const rawContent = ref('')

onMounted(() => {
  if (!resources.value && props.data) {
    showRaw.value = true
    try {
      rawContent.value = typeof props.data === 'string'
        ? props.data
        : JSON.stringify(props.data, null, 2)
    } catch {
      rawContent.value = String(props.data)
    }
  }
})

function getFileIcon(type) {
  const map = {
    'Markdown': '📄',
    'md': '📄',
    'PDF': '📕',
    'pdf': '📕',
    'article': '📰',
    'course': '🎓',
    'book': '📚',
    'video': '🎬',
    'image': '🖼️',
    'link': '🔗',
    '代码': '💻',
    'code': '💻'
  }
  return map[type] || '📄'
}

function formatRelevance(val) {
  if (typeof val === 'number') {
    return `相关度: ${Math.round(val * 100)}%`
  }
  if (typeof val === 'string' && val.includes('%')) return val
  return `相关度: ${val}`
}

function truncateText(text, maxLen) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

function toggleDetail(idx) {
  expandedItems.value[idx] = !expandedItems.value[idx]
}

function viewDetail(res) {
  selectedResource.value = res
  showResourceDetail.value = true
}

function closeResourceDetail() {
  showResourceDetail.value = false
  selectedResource.value = null
}

function copyResource(res) {
  const text = `[${res.title || res.name}](${res.url || '#'}) - ${res.description || res.summary || ''}`
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制资源信息')
  }).catch(() => {
    ElMessage.warning('复制失败')
  })
}
</script>

<style lang="scss" scoped>
.resource-result {
  padding: 4px 0;
}

.result-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(0, 245, 212, 0.04);
  border: 1px solid rgba(0, 245, 212, 0.08);
  border-radius: 10px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #b0b0d8;
  .summary-icon { font-size: 1.1rem; }
  .summary-text { font-weight: 500; }
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-item {
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(0, 245, 212, 0.15);
    transform: translateY(-1px);
  }
}

.resource-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.res-icon { font-size: 1.2rem; flex-shrink: 0; }

.res-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #f0f0ff;
  font-family: 'JetBrains Mono', monospace;
  word-break: break-all;
  line-height: 1.4;
  overflow-wrap: break-word;
}

.res-relevance {
  flex-shrink: 0;
  font-size: 11px;
  color: #00f5d4;
  background: rgba(0, 245, 212, 0.08);
  padding: 2px 8px;
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
  white-space: nowrap;
}

.resource-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 11px;
  color: #8080a8;
}

.res-tag {
  background: rgba(100, 100, 180, 0.08);
  padding: 1px 8px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
}

.res-chunks {
  font-family: 'JetBrains Mono', monospace;
}

.res-status {
  margin-left: auto;
}

.resource-item-desc {
  font-size: 12px;
  color: #606090;
  line-height: 1.5;
  margin-bottom: 8px;
}

.resource-item-footer {
  display: flex;
  gap: 8px;
}

.res-action-btn {
  padding: 4px 12px;
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

.resource-detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease;
}

.resource-detail-dialog {
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  background: rgba(17, 17, 39, 0.95);
  border: 1px solid rgba(0, 245, 212, 0.2);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.rd-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.1);
  background: rgba(0, 245, 212, 0.04);
}

.rd-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rd-type-icon {
  font-size: 2rem;
}

.rd-header-info h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #f0f0ff;
}

.rd-header-info p {
  margin: 4px 0 0;
  font-size: 0.8rem;
  color: #8080a8;
}

.rd-close {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(100, 100, 180, 0.1);
  border-radius: 8px;
  color: #8080a8;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: rgba(239, 68, 68, 0.2);
    color: #ef4444;
  }
}

.rd-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.rd-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
  margin-bottom: 16px;
}

.rd-meta-item {
  font-size: 0.8rem;
  color: #9090b8;
}

.meta-lbl {
  color: #606090;
}

.meta-highlight {
  color: #00f5d4;
  font-weight: 600;
}

.meta-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  &.ready, &.已就绪 {
    background: rgba(16, 185, 129, 0.15);
    color: #10b981;
  }
  &.processing {
    background: rgba(245, 158, 11, 0.15);
    color: #f59e0b;
  }
}

.rd-section {
  margin-bottom: 16px;
}

.rd-section-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #e8e8ff;
  margin-bottom: 10px;
}

.rd-preview {
  font-size: 0.85rem;
  color: #b0b0d8;
  line-height: 1.6;
  padding: 12px;
  background: rgba(100, 100, 180, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(100, 100, 180, 0.08);
}

.rd-chunks-list {
  max-height: 200px;
  overflow-y: auto;
  padding-right: 8px;
  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(0, 245, 212, 0.2);
    border-radius: 2px;
  }
}

.rd-chunk-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(100, 100, 180, 0.04);
  border-radius: 8px;
  margin-bottom: 6px;
  border: 1px solid rgba(100, 100, 180, 0.06);
}

.chunk-index {
  flex-shrink: 0;
  font-size: 0.75rem;
  font-weight: 600;
  color: #00f5d4;
  background: rgba(0, 245, 212, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.chunk-content {
  font-size: 0.8rem;
  color: #b0b0d8;
  line-height: 1.5;
}

.more-hint {
  font-size: 0.75rem;
  color: #606090;
  text-align: center;
  padding: 8px;
}

.rd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid rgba(100, 100, 180, 0.1);
  background: rgba(17, 17, 39, 0.6);
}

.rd-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.rd-btn-ghost {
  background: rgba(100, 100, 180, 0.1);
  color: #b0b0d8;
  &:hover {
    background: rgba(0, 245, 212, 0.1);
    color: #00f5d4;
  }
}

.rd-btn-cancel {
  background: rgba(100, 100, 180, 0.15);
  color: #e8e8ff;
  &:hover {
    background: rgba(100, 100, 180, 0.25);
  }
}
</style>