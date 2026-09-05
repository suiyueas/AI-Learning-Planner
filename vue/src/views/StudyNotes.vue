<template>
  <div ref="pageRef" class="study-notes">
    <!-- ===== 顶部导航 ===== -->
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">✏️</span>
            <span>学习笔记</span>
            <span class="title-sub">记录、整理与回顾知识</span>
          </h1>
        </div>
        <div class="header-right">
          <button class="btn-export" :disabled="exporting || notes.length === 0" title="导出全部笔记为 Markdown" @click="handleExport">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" /></svg>
            <span>{{ exporting ? '导出中...' : '导出笔记' }}</span>
          </button>
          <button class="btn-create" @click="openCreateModal">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            <span>新建笔记</span>
          </button>
        </div>
      </div>
    </header>

    <!-- ===== 搜索栏 ===== -->
    <div class="search-bar">
      <div class="search-inner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索笔记标题、内容或标签..."
          class="search-input"
        />
        <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
        </button>
      </div>
      <div class="search-stats">
        <template v-if="searchQuery && filteredNotes.length === 0">
          <span class="no-results">未找到匹配的笔记</span>
        </template>
        <template v-else>
          <span class="result-count">{{ filteredNotes.length }} 条笔记</span>
        </template>
      </div>
    </div>

    <!-- ===== 笔记网格 ===== -->
    <div class="notes-container">
      <TransitionGroup name="note-list" tag="div" class="notes-grid">
        <div
          v-for="note in filteredNotes"
          :key="note.id"
          class="note-card"
          :class="{ expanded: expandedId === note.id }"
          @click="toggleExpand(note.id)"
        >
          <!-- 卡片顶部装饰光效 -->
          <div class="card-glow-line"></div>

          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="card-date">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" /></svg>
              <span>{{ formatDate(note.createdAt) }}</span>
            </div>
            <div class="card-actions" @click.stop>
              <button class="icon-btn edit" title="编辑笔记" @click="openEditModal(note)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
              </button>
              <button class="icon-btn delete" title="删除笔记" @click="confirmDelete(note)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" /></svg>
              </button>
            </div>
          </div>

          <!-- 卡片标题 -->
          <h3 class="card-title">{{ note.title }}</h3>

          <!-- 内容预览 / 全文 -->
          <div class="card-content" :class="{ expanded: expandedId === note.id }">
            <p>{{ expandedId === note.id ? note.content : note.preview }}</p>
            <div v-if="expandedId !== note.id && note.content.length > 80" class="content-fade"></div>
          </div>

          <!-- 展开/折叠按钮 -->
          <button v-if="note.content.length > 80" class="toggle-btn" @click.stop="toggleExpand(note.id)">
            <span>{{ expandedId === note.id ? '收起' : '展开全文' }}</span>
            <svg
              width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
              :class="{ rotated: expandedId === note.id }"
            >
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </button>

          <!-- 标签 -->
          <div v-if="note.tags && note.tags.length > 0" class="card-tags">
            <span v-for="tag in note.tags" :key="tag" class="tag-chip">{{ tag }}</span>
          </div>

          <!-- 展开时显示时间戳 -->
          <div v-if="expandedId === note.id" class="card-footer">
            <span class="footer-updated">最后编辑: {{ formatDate(note.updatedAt) }}</span>
          </div>
        </div>
      </TransitionGroup>

      <!-- 空状态 -->
      <div v-if="filteredNotes.length === 0" class="empty-state">
        <div class="empty-illustration">
          <div class="empty-ring r1"></div>
          <div class="empty-ring r2"></div>
          <span class="empty-icon">📝</span>
        </div>
        <h3 class="empty-title">{{ searchQuery ? '没有找到匹配的笔记' : '还没有笔记' }}</h3>
        <p class="empty-desc">{{ searchQuery ? '试试其他关键词吧' : '点击上方按钮创建你的第一篇学习笔记' }}</p>
        <button v-if="!searchQuery" class="btn-create-empty" @click="openCreateModal">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
          创建笔记
        </button>
      </div>
    </div>

    <!-- ===== 创建/编辑 模态框 ===== -->
    <transition name="modal-fade">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <h2 class="modal-title">
              <span class="modal-icon">{{ isEditing ? '✏️' : '🆕' }}</span>
              {{ isEditing ? '编辑笔记' : '新建笔记' }}
            </h2>
            <button class="modal-close" @click="closeModal">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>

          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">笔记标题</label>
              <input
                ref="titleInputRef"
                v-model="form.title"
                type="text"
                placeholder="输入笔记标题..."
                class="form-input"
                maxlength="60"
              />
              <span class="form-counter">{{ form.title.length }}/60</span>
            </div>

            <div class="form-group">
              <label class="form-label">笔记内容</label>
              <textarea
                v-model="form.content"
                placeholder="写下你的学习心得、知识点总结..."
                class="form-textarea"
                rows="8"
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">
                标签
                <span class="label-hint">按 Enter 添加</span>
              </label>
              <div class="tags-input-wrapper">
                <div class="tags-list">
                  <span v-for="(tag, idx) in form.tags" :key="idx" class="tag-item">
                    {{ tag }}
                    <button class="tag-remove" @click="removeTag(idx)">
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
                    </button>
                  </span>
                </div>
                <input
                  v-model="tagInput"
                  type="text"
                  placeholder="输入标签..."
                  class="tag-input"
                  @keydown.enter.prevent="addTag"
                  @keydown.backspace="handleBackspaceTag"
                />
              </div>
              <!-- 常用标签建议 -->
              <div v-if="suggestedTags.length > 0" class="tag-suggestions">
                <span
                  v-for="sug in suggestedTags"
                  :key="sug"
                  class="tag-suggestion"
                  @click="addSuggestionTag(sug)"
                >+ {{ sug }}</span>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="closeModal">取消</button>
            <button class="btn-save" :disabled="!form.title.trim()" @click="saveNote">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z" /><polyline points="17 21 17 13 7 13 7 21" /><polyline points="7 3 7 8 15 8" /></svg>
              {{ isEditing ? '保存修改' : '创建笔记' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ===== 删除确认对话框 ===== -->
    <DeleteConfirmDialog
      :visible="showDeleteConfirm"
      :title="deleteDialogConfig.title"
      :message="deleteDialogConfig.message"
      :type="deleteDialogConfig.type"
      :show-soft-delete="deleteDialogConfig.showSoftDelete"
      :details="deleteDialogConfig.details"
      @cancel="handleDeleteCancel"
      @hard-delete="handleDeleteHard"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getNotes, createNote, updateNote, deleteNote, exportNotes } from '@/api/notesApi'
import DeleteConfirmDialog from '@/components/common/DeleteConfirmDialog.vue'

const router = useRouter()

// ===== 笔记数据类型 =====

// ===== 响应式状态 =====
const notes = ref([])
const searchQuery = ref('')
const expandedId = ref(null)
const showModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const showDeleteConfirm = ref(false)
const deletingNote = ref(null)
const deleteDialogConfig = ref({
  title: '删除笔记',
  message: '',
  type: 'warning',
  showSoftDelete: false,
  details: []
})
const tagInput = ref('')
const titleInputRef = ref(null)

const form = ref({
  title: '',
  content: '',
  tags: []
})

// 常用标签建议
const suggestedTags = computed(() => {
  const allTags = ['Python', '数据分析', '机器学习', '深度学习', '前端', 'Vue', 'Spring AI', 'RAG', 'Agent', '架构', '算法', '数据库', '网络', '安全', 'DevOps', '工具']
  const currentTags = form.value.tags.map(t => t.toLowerCase())
  return allTags.filter(t => !currentTags.includes(t.toLowerCase()) && t.toLowerCase().includes(tagInput.value.toLowerCase())).slice(0, 5)
})

// ===== 过滤 & 排序 =====
const filteredNotes = computed(() => {
  let result = [...notes.value]
  const query = searchQuery.value.trim().toLowerCase()
  if (query) {
    result = result.filter(note =>
      note.title.toLowerCase().includes(query) ||
      note.content.toLowerCase().includes(query) ||
      (note.tags && note.tags.some(tag => tag.toLowerCase().includes(query)))
    )
  }
  // 按更新时间降序排列
  result.sort((a, b) => b.updatedAt - a.updatedAt)
  return result
})

// ===== 后端数据适配 =====
// 后端 Note：tags 为逗号分隔字符串、时间为 ISO 字符串 → 前端：数组 + 时间戳
const toLocalNote = (note) => {
  const content = note.content || ''
  const tags = note.tags ? String(note.tags).split(',').map(t => t.trim()).filter(Boolean) : []
  return {
    ...note,
    content,
    tags,
    preview: content.slice(0, 80),
    createdAt: note.createdAt ? new Date(note.createdAt).getTime() : Date.now(),
    updatedAt: note.updatedAt ? new Date(note.updatedAt).getTime() : Date.now()
  }
}

// ===== 数据加载（真实 API） =====
const loadNotes = async () => {
  try {
    const res = await getNotes()
    notes.value = (res?.data || []).map(toLocalNote)
  } catch (e) {
    console.error('加载笔记失败:', e)
    notes.value = []
  }
}

// ===== CRUD 操作 =====
const openCreateModal = () => {
  isEditing.value = false
  editingId.value = null
  form.value = { title: '', content: '', tags: [] }
  tagInput.value = ''
  showModal.value = true
  nextTick(() => {
    titleInputRef.value?.focus()
  })
}

const openEditModal = (note) => {
  isEditing.value = true
  editingId.value = note.id
  form.value = {
    title: note.title,
    content: note.content,
    tags: [...note.tags]
  }
  tagInput.value = ''
  showModal.value = true
  nextTick(() => {
    titleInputRef.value?.focus()
  })
}

const closeModal = () => {
  showModal.value = false
  isEditing.value = false
  editingId.value = null
}

const saveNote = async () => {
  if (!form.value.title.trim()) return
  const title = form.value.title.trim()
  const content = form.value.content.trim()
  const tags = form.value.tags.join(',')

  try {
    if (isEditing.value && editingId.value) {
      await updateNote(editingId.value, title, content, tags)
    } else {
      await createNote(title, content, tags)
    }
    closeModal()
    await loadNotes()
  } catch (e) {
    console.error('保存笔记失败:', e)
  }
}

const confirmDelete = (note) => {
  deletingNote.value = note
  deleteDialogConfig.value = {
    title: '删除笔记',
    message: `确定要删除笔记「${note.title}」吗？此操作不可撤销。`,
    type: 'warning',
    showSoftDelete: false,
    details: [
      { icon: '📝', text: `标题：${note.title}` },
      { icon: '📅', text: `创建时间：${note.createdAt || '未知'}` }
    ]
  }
  showDeleteConfirm.value = true
}

const executeDelete = async () => {
  if (deletingNote.value) {
    try {
      await deleteNote(deletingNote.value.id)
      notes.value = notes.value.filter(n => n.id !== deletingNote.value.id)
      if (expandedId.value === deletingNote.value.id) {
        expandedId.value = null
      }
    } catch (e) {
      console.error('删除笔记失败:', e)
    }
  }
  showDeleteConfirm.value = false
  deletingNote.value = null
}

const handleDeleteCancel = () => {
  showDeleteConfirm.value = false
  deletingNote.value = null
}

const handleDeleteHard = () => {
  executeDelete()
}

// ===== 导出笔记（markdown → 前端 Blob 下载，规避 URL 参数长度限制） =====
const exporting = ref(false)
const handleExport = async () => {
  if (notes.value.length === 0) return
  exporting.value = true
  try {
    const res = await exportNotes(null, 'markdown', true, true)
    const data = res?.data || {}
    const content = data.content || ''
    const filename = data.filename || 'notes_export.md'
    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('导出笔记失败:', e)
  } finally {
    exporting.value = false
  }
}

// ===== 展开/折叠 =====
const toggleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id
}

// ===== 标签操作 =====
const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !form.value.tags.includes(tag) && form.value.tags.length < 8) {
    form.value.tags.push(tag)
  }
  tagInput.value = ''
}

const removeTag = (index) => {
  form.value.tags.splice(index, 1)
}

const handleBackspaceTag = () => {
  if (tagInput.value === '' && form.value.tags.length > 0) {
    form.value.tags.pop()
  }
}

const addSuggestionTag = (tag) => {
  if (!form.value.tags.includes(tag) && form.value.tags.length < 8) {
    form.value.tags.push(tag)
  }
  tagInput.value = ''
}

// ===== 工具函数 =====
const formatDate = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / 86400000)

  if (days === 0) {
    const hours = Math.floor(diff / 3600000)
    if (hours === 0) {
      const mins = Math.floor(diff / 60000)
      return mins <= 1 ? '刚刚' : `${mins} 分钟前`
    }
    return `${hours} 小时前`
  }
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`

  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}



// ===== 生命周期 =====
onMounted(() => {
  loadNotes()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;
// ============================================
// 学习笔记页面 · 深色科技风
// 背景 $bg-primary · 强调色 $accent-primary
// ============================================

.study-notes {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  padding: 32px 48px 80px;
  animation: pageEnter 0.6s ease;
}

/* ===== 页面头部（统一规范） ===== */
.page-header { @include page-header-base; }
.page-title { @include page-title-base; }
.page-subtitle { @include page-subtitle-base; }
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; -webkit-text-fill-color: initial; }

.header-actions { display: flex; align-items: center; gap: 10px; }

.btn-back {
  @include page-header-btn;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.1);
  color: $text-secondary;

  &:hover {
    border-color: rgba($accent-primary, 0.2);
    color: $accent-primary;
    background: rgba($accent-primary, 0.04);
    transform: translateX(-2px);
  }

  svg { transition: transform 0.25s ease; }
  &:hover svg { transform: translateX(-3px); }
}

.title-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 7px;
  background: rgba($accent-primary, 0.1);
  border: 1px solid rgba($accent-primary, 0.15);
  border-radius: 12px;
  font-size: 0.72rem;
  font-weight: 700;
  color: $accent-primary;
  -webkit-text-fill-color: $accent-primary;
}

.btn-export {
  @include page-header-btn-ghost;
  color: #0d9488;
  border-color: rgba(13, 148, 136, 0.3);
  background: linear-gradient(135deg, rgba(13, 148, 136, 0.08), rgba(20, 184, 166, 0.08));

  &:hover:not(:disabled) {
    color: #14b8a6;
    border-color: rgba(20, 184, 166, 0.45);
    background: linear-gradient(135deg, rgba(13, 148, 136, 0.18), rgba(20, 184, 166, 0.18));
    box-shadow: 0 4px 12px rgba(20, 184, 166, 0.18);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.btn-create {
  @include page-header-btn-ghost;
  color: #14b8a6;
  border-color: rgba(20, 184, 166, 0.35);
  background: linear-gradient(135deg, rgba(13, 148, 136, 0.12), rgba(45, 212, 191, 0.12));

  &:hover:not(:disabled) {
    color: #2dd4bf;
    border-color: rgba(45, 212, 191, 0.55);
    background: linear-gradient(135deg, rgba(13, 148, 136, 0.25), rgba(45, 212, 191, 0.25));
    box-shadow: 0 4px 16px rgba(20, 184, 166, 0.25);
  }
}

/* ===== 搜索栏 ===== */
.search-bar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  animation: slideDown 0.5s ease 0.1s both;
}

.search-inner {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: rgba($bg-primary,0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 12px;
  transition: all 0.3s ease;

  &:focus-within {
    border-color: rgba($accent-primary,0.25);
    box-shadow: 0 0 0 3px rgba($accent-primary,0.04), 0 4px 20px rgba(0,0,0,0.2);
  }

  svg { color: $text-muted; flex-shrink: 0; }
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: $text-primary;
  font-size: 0.9rem;
  font-weight: 400;

  &::placeholder { color: $text-muted; }
}

.search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: rgba($accent-secondary,0.1);
  border: none;
  border-radius: 6px;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(255,60,90,0.15);
    color: #ff4060;
  }
}

.search-stats {
  flex-shrink: 0;
  font-size: 0.8rem;
  white-space: nowrap;
}

.result-count { color: $text-muted; font-weight: 500; }

.no-results {
  color: #ff6b6b;
  font-weight: 500;
}

/* ===== 笔记网格 ===== */
.notes-container {
  position: relative;
  z-index: 1;
  animation: fadeUp 0.6s ease 0.2s both;
}

.notes-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

/* ===== 笔记卡片 ===== */
.note-card {
  position: relative;
  padding: 24px;
  background: rgba($bg-primary,0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  animation: cardEnter 0.5s ease both;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba($accent-primary,0.15);
    box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba($accent-primary,0.03);
  }

  &.expanded {
    grid-column: 1 / -1;
    border-color: rgba($accent-primary,0.2);
    background: rgba($bg-primary,0.7);
    box-shadow: 0 8px 40px rgba(0,0,0,0.4), 0 0 60px rgba($accent-primary,0.05);
  }
}

.card-glow-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba($accent-primary,0.3), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;

  .note-card:hover & { opacity: 1; }
  .note-card.expanded & { opacity: 1; }
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.card-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.78rem;
  color: $text-muted;
  font-weight: 500;

  svg { flex-shrink: 0; color: $text-muted; }
}

.card-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.25s ease;

  .note-card:hover & { opacity: 1; }
  .note-card.expanded & { opacity: 1; }
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background: rgba($accent-secondary,0.03);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: $text-muted;

  &:hover {
    transform: translateY(-1px);
  }

  &.edit:hover {
    border-color: rgba($accent-primary,0.2);
    background: rgba($accent-primary,0.06);
    color: $accent-primary;
  }

  &.delete:hover {
    border-color: rgba(255,60,90,0.2);
    background: rgba(255,60,90,0.06);
    color: #ff4060;
  }
}

/* 卡片标题 */
.card-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 10px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;

  .note-card.expanded & {
    -webkit-line-clamp: unset;
    line-clamp: unset;
  }
}

/* 内容 */
.card-content {
  position: relative;
  margin-bottom: 14px;

  p {
    font-size: 0.85rem;
    color: $text-secondary;
    line-height: 1.7;
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &:not(.expanded) p {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.content-fade {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(transparent, rgba($bg-primary,0.5));
  pointer-events: none;
}

/* 展开/折叠按钮 */
.toggle-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: 1px solid rgba($accent-primary,0.1);
  border-radius: 6px;
  color: $accent-primary;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 14px;

  &:hover {
    background: rgba($accent-primary,0.06);
    border-color: rgba($accent-primary,0.2);
  }

  svg {
    transition: transform 0.3s ease;

    &.rotated { transform: rotate(180deg); }
  }
}

/* 标签 */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-chip {
  display: inline-block;
  padding: 4px 10px;
  background: rgba($accent-primary,0.06);
  border: 1px solid rgba($accent-primary,0.1);
  border-radius: 12px;
  font-size: 0.72rem;
  font-weight: 500;
  color: $accent-primary;
  transition: all 0.2s ease;

  &:hover {
    background: rgba($accent-primary,0.1);
    border-color: rgba($accent-primary,0.2);
  }
}

/* 卡片底部 */
.card-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba($accent-secondary,0.06);
}

.footer-updated {
  font-size: 0.72rem;
  color: $text-muted;
  font-weight: 400;
}

/* ===== 空状态 ===== */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-illustration {
  position: relative;
  width: 100px;
  height: 100px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba($accent-primary,0.08);
  animation: ringRotate 20s linear infinite;

  &.r1 { width: 100px; height: 100px; }
  &.r2 {
    width: 72px; height: 72px;
    animation-direction: reverse;
    animation-duration: 15s;
    border-color: rgba(123,97,255,0.08);
  }
}

@keyframes ringRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 2.4rem;
  position: relative;
  z-index: 1;
}

.empty-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 6px;
}

.empty-desc {
  font-size: 0.9rem;
  color: $text-muted;
  margin: 0 0 24px;
}

.btn-create-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba($accent-primary,0.12), rgba(0,85,255,0.08));
  border: 1px solid rgba($accent-primary,0.2);
  border-radius: 10px;
  color: $accent-primary;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba($accent-primary,0.12);
    border-color: rgba($accent-primary,0.3);
    background: linear-gradient(135deg, rgba($accent-primary,0.18), rgba(0,85,255,0.12));
  }
}

/* ===== 模态框 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px 20px 80px;
}

.modal-panel {
  width: 600px;
  max-width: 92vw;
  max-height: calc(100vh - 120px);
  background: rgba($bg-surface, 0.98);
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5), 0 0 40px rgba($accent-indigo, 0.05);
  display: flex;
  flex-direction: column;
  animation: modalEnter 0.3s ease;
}

@keyframes modalEnter {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  background: rgba($bg-elevated, 0.5);
  border-bottom: 1px solid rgba($accent-indigo, 0.1);
  flex-shrink: 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.15rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.modal-icon { font-size: 1.2rem; }

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: rgba($accent-indigo, 0.08);
  border: none;
  border-radius: 8px;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($accent-indigo, 0.15);
    color: $text-primary;
  }
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scrollbar-width: thin;
  scrollbar-color: rgba($accent-indigo, 0.12) transparent;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba($accent-indigo, 0.12); border-radius: 3px; }
}

.form-group {
  position: relative;
  margin-bottom: 20px;

  &:last-child { margin-bottom: 0; }
}

.form-label {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 8px;
}

.label-hint {
  font-weight: 400;
  font-size: 0.72rem;
  color: $text-muted;
  margin-left: 8px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  background: rgba($bg-surface, 0.6);
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 10px;
  color: $text-primary;
  font-size: 0.9rem;
  outline: none;
  transition: all 0.25s ease;
  box-sizing: border-box;
  font-family: inherit;

  &:focus {
    border-color: rgba($accent-indigo, 0.4);
    box-shadow: 0 0 0 3px rgba($accent-indigo, 0.08);
  }

  &::placeholder { color: $text-placeholder; }
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
  line-height: 1.7;
}

.form-counter {
  position: absolute;
  right: 14px;
  bottom: 10px;
  font-size: 0.7rem;
  color: $text-muted;
}

/* 标签输入 */
.tags-input-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 10px;
  background: rgba($bg-surface, 0.6);
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 10px;
  transition: border-color 0.25s ease;

  &:focus-within {
    border-color: rgba($accent-indigo, 0.4);
  }
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba($accent-primary,0.08);
  border: 1px solid rgba($accent-primary,0.12);
  border-radius: 8px;
  font-size: 0.78rem;
  font-weight: 500;
  color: $accent-primary;
  line-height: 1.4;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  background: transparent;
  border: none;
  color: rgba($accent-primary,0.5);
  cursor: pointer;
  padding: 0;
  border-radius: 3px;
  transition: all 0.15s;

  &:hover {
    color: #ff4060;
    background: rgba(255,60,90,0.1);
  }
}

.tag-input {
  flex: 1;
  min-width: 80px;
  background: transparent;
  border: none;
  outline: none;
  color: $text-primary;
  font-size: 0.85rem;

  &::placeholder { color: #606088; }
}

.tag-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.tag-suggestion {
  padding: 3px 10px;
  background: rgba($accent-indigo, 0.06);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: 8px;
  font-size: 0.72rem;
  color: $accent-indigo;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($accent-indigo, 0.12);
    border-color: rgba($accent-indigo, 0.2);
    color: $accent-indigo-light;
  }
}

/* 模态框底部 */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba($accent-indigo, 0.1);
  background: rgba($bg-elevated, 0.3);
  flex-shrink: 0;
}

.btn-cancel {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 10px;
  color: $text-secondary;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    color: $text-primary;
    background: rgba($accent-indigo, 0.05);
  }
}

.btn-save {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, $accent-indigo, $accent-violet);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba($accent-indigo, 0.3);
  }

  &:active:not(:disabled) { transform: scale(0.98) translateY(-1px); }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* ===== Transition 动画 ===== */
.note-list-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.note-list-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.note-list-enter-from {
  opacity: 0;
  transform: scale(0.92) translateY(16px);
}

.note-list-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(-10px);
}

.note-list-move {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 模态框动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-panel,
.modal-fade-enter-active .delete-dialog {
  animation: modalEnter 0.3s ease;
}

.modal-fade-leave-active .modal-panel,
.modal-fade-leave-active .delete-dialog {
  animation: modalEnter 0.25s ease reverse;
}

/* ===== 页面动画 ===== */
@keyframes pageEnter {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cardEnter {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .study-notes { padding: 24px 24px 60px; }
  .page-title { font-size: 1.5rem; }
  .notes-grid { gap: 16px; }
}

@media (max-width: 768px) {
  .study-notes { padding: 20px 16px 40px; }
  .page-header { flex-wrap: wrap; gap: 12px; }
  .header-left, .header-right { flex: 0 0 auto; }
  .header-center { order: -1; width: 100%; text-align: center; }
  .page-title { justify-content: center; font-size: 1.3rem; }
  .btn-create { padding: 8px 14px; font-size: 0.82rem; span { display: none; } }
  .notes-grid { grid-template-columns: 1fr; gap: 14px; }
  .search-bar { flex-direction: column; gap: 8px; align-items: stretch; }
  .search-stats { text-align: right; }
  .modal-panel { max-width: 96vw; }
  .modal-body { padding: 16px; }
}
</style>