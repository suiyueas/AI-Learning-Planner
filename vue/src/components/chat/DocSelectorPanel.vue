<template>
  <div v-show="visible" class="selector-overlay" @click="handleOverlayClick">
    <div class="selector-panel" @click.stop>
      <!-- 头部 -->
      <div class="panel-header">
        <div class="panel-title">
          <span class="panel-icon">📚</span>
          <span>知识库文档</span>
        </div>
        <button class="close-btn" @click="close">✕</button>
      </div>
      <div class="panel-subtitle">选择文档以启用 RAG 检索，AI 将基于选中文档回答问题</div>

      <!-- 统计 -->
      <div class="panel-stats">
        已接入 {{ documents.length }} 个文档 · 共 {{ totalChunks }} 个片段
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="panel-loading">加载中...</div>

      <!-- 文档列表 -->
      <div v-else class="panel-body">
        <div
          v-for="doc in documents"
          :key="doc.id"
          class="item-card"
          :class="{ selected: isSelected(doc.id) }"
          @click="toggleItem(doc.id)"
        >
          <div class="checkbox-wrap" @click.stop>
            <input
              type="checkbox"
              :checked="isSelected(doc.id)"
              @change="toggleItem(doc.id)"
            />
          </div>
          <div class="item-info">
            <div class="item-name">{{ doc.title || doc.fileName || '未命名文档' }}</div>
            <div class="item-meta">
              {{ doc.size || '-' }} · {{ doc.chunks || 0 }} 个片段
            </div>
          </div>
          <div class="item-status" :class="doc.status">
            <span class="status-dot-sm" :class="statusColor(doc.status)"></span>
            {{ statusText(doc.status) }}
          </div>
        </div>
        <div v-if="documents.length === 0" class="empty-hint">暂无文档，请前往知识库上传</div>
      </div>

      <!-- 底部操作栏 -->
      <div class="panel-footer">
        <button class="btn-ghost" @click="selectAll">全选</button>
        <button class="btn-ghost" @click="deselectAll">取消全选</button>
        <button class="btn-primary" @click="apply">
          应用选中 ({{ selectedCount }})
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useContextStore } from '@/stores/contextStore'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'apply'])

const contextStore = useContextStore()

const loading = ref(false)

// 从 store 加载数据
watch(() => props.visible, async (val) => {
  if (val && contextStore.documents.length === 0) {
    loading.value = true
    await contextStore.fetchDocuments()
    loading.value = false
  }
}, { immediate: true })

const documents = computed(() => contextStore.documents)
const totalChunks = computed(() =>
  contextStore.documents.reduce((sum, d) => sum + (d.chunks || 0), 0)
)
const selectedCount = computed(() => contextStore.selectedDocCount)

function isSelected(id) { return contextStore.isDocSelected(id) }
function toggleItem(id) { contextStore.toggleDoc(id) }
function selectAll() { contextStore.selectAllDocs() }
function deselectAll() { contextStore.deselectAllDocs() }

function statusColor(status) {
  if (status === 'ready') return 'green'
  if (status === 'processing') return 'yellow'
  return 'gray'
}
function statusText(status) {
  if (status === 'ready') return '已就绪'
  if (status === 'processing') return '处理中'
  return '未知'
}

function close() { emit('close') }
function handleOverlayClick(e) {
  if (e.target === e.currentTarget) close()
}
function apply() {
  emit('apply', { selectedDocIds: contextStore.selectedDocIds })
  close()
}
</script>

<style lang="scss" scoped>
.selector-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.selector-panel {
  width: 420px;
  max-width: 100vw;
  height: 100vh;
  background: rgba(18, 18, 42, 0.97);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-left: 1px solid rgba(100, 100, 180, 0.1);
  display: flex;
  flex-direction: column;
  animation: slideInRight 0.25s ease;
}

@keyframes slideInRight {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 20px 12px;
  flex-shrink: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #f0f0ff;
}

.panel-icon { font-size: 18px; }

.close-btn {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(100,100,180,0.06);
  border: 1px solid rgba(100,100,180,0.1);
  border-radius: 6px;
  color: #8080a8;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
  &:hover { background: rgba(255,64,96,0.1); color: #ff4060; border-color: rgba(255,64,96,0.2); }
}

.panel-subtitle {
  padding: 0 20px 14px;
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.5;
  border-bottom: 1px solid rgba(100,100,180,0.07);
}

.panel-stats {
  padding: 10px 20px;
  font-size: 12px;
  color: #d8dce8;
  font-weight: 500;
  flex-shrink: 0;
}

.panel-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--text-sub);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0,245,212,0.12) transparent;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,245,212,0.12); border-radius: 2px; }
}

.item-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(100,100,180,0.04);
  border: 1px solid var(--border-ctrl-soft);
  border-radius: 10px;
  transition: all 0.2s;
  cursor: pointer;
  &:hover {
    background: rgba(100,100,180,0.08);
    border-color: rgba(0,245,212,0.2);
  }
  &.selected {
    border-color: rgba(124, 107, 245, 0.45);
    background: rgba(124, 107, 245, 0.08);
  }
}

.checkbox-wrap {
  flex-shrink: 0;
  input[type="checkbox"] {
    accent-color: #00f5d4;
    width: 16px; height: 16px;
    cursor: pointer;
  }
}

.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 13px;
  font-weight: 600;
  color: #e8e8ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 3px;
}
.item-meta {
  font-size: 11px;
  color: var(--text-sub);
}
.item-status {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  color: #c8d0e8;
  &.ready { color: var(--status-ready-text); font-weight: 600; }
  &.processing { color: #F59E0B; }
}

.status-dot-sm {
  width: 5px; height: 5px;
  border-radius: 50%;
  display: inline-block;
  &.green { background: #10B981; }
  &.yellow { background: #F59E0B; }
  &.gray { background: #606090; }
}

.empty-hint {
  text-align: center;
  padding: 40px 20px;
  font-size: 13px;
  color: var(--text-placeholder);
  border: 1px dashed rgba(106, 112, 144, 0.35);
  border-radius: 10px;
  background: rgba(30, 38, 56, 0.25);
}

.panel-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid rgba(100,100,180,0.07);
  flex-shrink: 0;
}

.btn-ghost {
  padding: 8px 14px;
  background: transparent;
  border: 1px solid #6a6080;
  border-radius: 8px;
  color: #c8c0d8;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  &:hover { background: rgba(124,107,245,0.1); border-color: var(--border-ctrl-hover); color: #ffffff; }
}

.btn-primary {
  margin-left: auto;
  padding: 8px 18px;
  background: var(--btn-gradient);
  border: none;
  border-radius: 8px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(124, 107, 245, 0.4);
  }
}
</style>
