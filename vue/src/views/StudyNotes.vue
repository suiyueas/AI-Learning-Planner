<template>
  <div ref="pageRef" class="study-notes" @mousemove="handleMouseMove" @mouseleave="isMouseInside = false" @mouseenter="isMouseInside = true">
    <!-- ===== 深空背景层 ===== -->
    <div class="bg-layer">
      <div class="aurora-bg">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="grid-bg"></div>
      <div class="mouse-glow" :class="{ visible: isMouseInside }" :style="glowStyle"></div>
      <div class="floating-glow fg-cyan"></div>
      <div class="floating-glow fg-purple"></div>
      <div class="bg-particles">
        <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></div>
      </div>
    </div>

    <!-- ===== 顶部导航 ===== -->
    <header class="page-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
          <span>返回</span>
        </button>
      </div>
      <div class="header-center">
        <h1 class="page-title">
          <span class="title-icon">✏️</span>
          <span class="title-text">学习笔记</span>
          <span class="title-badge">{{ notes.length }}</span>
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
    <transition name="modal-fade">
      <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
        <div class="delete-dialog" @click.stop>
          <div class="delete-icon">🗑️</div>
          <h3 class="delete-title">确认删除</h3>
          <p class="delete-desc">确定要删除笔记「{{ deletingNote?.title }}」吗？此操作不可撤销。</p>
          <div class="delete-actions">
            <button class="btn-cancel" @click="showDeleteConfirm = false">取消</button>
            <button class="btn-delete" @click="executeDelete">删除</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getNotes, createNote, updateNote, deleteNote, exportNotes } from '@/api/notesApi'

const router = useRouter()

// ===== 鼠标跟踪 =====
const pageRef = ref(null)
const isMouseInside = ref(false)
const mouseX = ref(0)
const mouseY = ref(0)

const glowStyle = computed(() => ({
  left: mouseX.value + 'px',
  top: mouseY.value + 'px'
}))

const handleMouseMove = (e) => {
  if (pageRef.value) {
    const rect = pageRef.value.getBoundingClientRect()
    mouseX.value = e.clientX - rect.left
    mouseY.value = e.clientY - rect.top
  }
}

// ===== 粒子背景 =====
const particleStyle = (i) => {
  const size = Math.random() * 3 + 1.5
  return {
    width: size + 'px',
    height: size + 'px',
    left: Math.random() * 100 + '%',
    bottom: '-10px',
    opacity: Math.random() * 0.3 + 0.1,
    animationDuration: (Math.random() * 15 + 15) + 's',
    animationDelay: (Math.random() * 10) + 's',
    background: i % 3 === 0 ? '#00f5d4' : (i % 3 === 1 ? '#7b61ff' : 'rgba(0,229,255,0.4)')
  }
}

// ===== 导航 =====
const goBack = () => {
  router.back()
}

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

/* ===== 背景层 ===== */
.bg-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -2;
}

.aurora-bg {
  position: absolute;
  inset: 0;
}

.aurora-layer {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  animation: aurora 20s ease-in-out infinite;
}

.a1 {
  width: 600px; height: 600px;
  top: -200px; right: -100px;
  background: radial-gradient(circle, rgba($accent-primary,0.08) 0%, transparent 70%);
}

.a2 {
  width: 500px; height: 500px;
  bottom: -150px; left: -100px;
  background: radial-gradient(circle, rgba(0,85,255,0.07) 0%, transparent 70%);
  animation-delay: -7s;
}

.a3 {
  width: 400px; height: 400px;
  top: 40%; left: 40%;
  background: radial-gradient(circle, rgba(123,97,255,0.05) 0%, transparent 70%);
  animation-delay: -14s;
}

@keyframes aurora {
  0%,100% { transform: translate(0,0) scale(1); }
  25% { transform: translate(30px,-30px) scale(1.1); }
  50% { transform: translate(-20px,20px) scale(0.95); }
  75% { transform: translate(20px,10px) scale(1.05); }
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba($accent-primary,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(123,97,255,0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
  animation: gridPulse 8s ease-in-out infinite alternate;
}

@keyframes gridPulse {
  0% { opacity: 0.3; transform: scale(1); }
  100% { opacity: 0.6; transform: scale(1.02); }
}

.mouse-glow {
  position: absolute;
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba($accent-primary,0.04) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.4s ease, left 0.15s ease-out, top 0.15s ease-out;
  will-change: left, top;

  &.visible { opacity: 1; }
}

.floating-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: floatGlow 15s ease-in-out infinite;
  opacity: 0.3;

  &.fg-cyan {
    width: 400px; height: 400px;
    top: 30%; right: 10%;
    background: radial-gradient(circle, rgba($accent-primary,0.06) 0%, transparent 70%);
  }

  &.fg-purple {
    width: 350px; height: 350px;
    bottom: 20%; left: 5%;
    background: radial-gradient(circle, rgba(123,97,255,0.05) 0%, transparent 70%);
    animation-delay: -7s;
  }
}

@keyframes floatGlow {
  0%,100% { transform: translate(0,0) scale(1); }
  33% { transform: translate(20px,-30px) scale(1.05); }
  66% { transform: translate(-15px,20px) scale(0.95); }
}

.bg-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  border-radius: 50%;
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.5; }
  90% { opacity: 0.2; }
  100% { transform: translateY(-100vh) translateX(100px); opacity: 0; }
}

/* ===== 页面头部 ===== */
.page-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 20px;
  animation: slideDown 0.5s ease;
}

.header-left,
.header-right {
  flex: 1;
}

.header-left {
  display: flex;
  justify-content: flex-start;
}

.header-right {
  display: flex;
  justify-content: flex-end;
}

.header-center {
  flex: 0 0 auto;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-secondary,0.04);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 10px;
  color: $text-secondary;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    border-color: rgba($accent-primary,0.2);
    color: $accent-primary;
    background: rgba($accent-primary,0.04);
    transform: translateX(-2px);
  }

  svg { transition: transform 0.25s ease; }
  &:hover svg { transform: translateX(-3px); }
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 1.8rem;
  font-weight: 800;

  .title-icon { font-size: 1.6rem; }

  .title-text {
    background: linear-gradient(135deg, $accent-primary 0%, #0055FF 50%, $accent-purple 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .title-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 28px;
    height: 28px;
    padding: 0 8px;
    background: rgba($accent-primary,0.1);
    border: 1px solid rgba($accent-primary,0.15);
    border-radius: 14px;
    font-size: 0.8rem;
    font-weight: 700;
    color: $accent-primary;
    -webkit-text-fill-color: $accent-primary;
  }
}

.btn-export {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(123, 97, 255, 0.12);
  border: 1px solid rgba(123, 97, 255, 0.25);
  border-radius: 10px;
  color: #a78bfa;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 0 20px rgba(123, 97, 255, 0.15);
    border-color: rgba(123, 97, 255, 0.4);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.btn-create {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba($accent-primary,0.15), rgba(0,85,255,0.1));
  border: 1px solid rgba($accent-primary,0.2);
  border-radius: 10px;
  color: $accent-primary;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: -2px;
    background: linear-gradient(135deg, $accent-primary, #0055FF);
    border-radius: inherit;
    z-index: -1;
    opacity: 0;
    transition: opacity 0.3s ease;
    filter: blur(8px);
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba($accent-primary,0.15);
    border-color: rgba($accent-primary,0.35);

    &::before { opacity: 0.25; }
  }

  &:active { transform: scale(0.98) translateY(-1px); }
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
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-panel {
  width: 600px;
  max-width: 92vw;
  max-height: 85vh;
  background: rgba(12,14,30,0.97);
  border: 1px solid rgba($accent-secondary,0.12);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.6), 0 0 80px rgba($accent-primary,0.03);
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
  background: rgba($bg-primary,0.8);
  border-bottom: 1px solid rgba($accent-secondary,0.08);
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
  background: rgba($accent-secondary,0.08);
  border: none;
  border-radius: 8px;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($accent-secondary,0.15);
    color: $text-primary;
  }
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scrollbar-width: thin;
  scrollbar-color: rgba($accent-primary,0.12) transparent;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba($accent-primary,0.12); border-radius: 3px; }
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
  background: rgba(0,0,0,0.2);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 10px;
  color: $text-primary;
  font-size: 0.9rem;
  outline: none;
  transition: all 0.25s ease;
  box-sizing: border-box;
  font-family: inherit;

  &:focus {
    border-color: rgba($accent-primary,0.25);
    box-shadow: 0 0 0 3px rgba($accent-primary,0.04);
  }

  &::placeholder { color: #606088; }
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
  background: rgba(0,0,0,0.2);
  border: 1px solid rgba($accent-secondary,0.1);
  border-radius: 10px;
  transition: border-color 0.25s ease;

  &:focus-within {
    border-color: rgba($accent-primary,0.25);
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
  background: rgba(123,97,255,0.08);
  border: 1px solid rgba(123,97,255,0.12);
  border-radius: 8px;
  font-size: 0.72rem;
  color: #a78bfa;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(123,97,255,0.15);
    border-color: rgba(123,97,255,0.2);
    color: #c4b5fd;
  }
}

/* 模态框底部 */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba($accent-secondary,0.08);
  background: rgba($bg-primary,0.5);
}

.btn-cancel {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid rgba($accent-secondary,0.12);
  border-radius: 10px;
  color: $text-muted;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba($accent-secondary,0.2);
    color: $text-primary;
  }
}

.btn-save {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, $accent-primary, #0055FF);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba($accent-primary,0.25);
  }

  &:active:not(:disabled) { transform: scale(0.98) translateY(-1px); }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* ===== 删除确认对话框 ===== */
.delete-dialog {
  width: 380px;
  max-width: 88vw;
  padding: 32px 28px 24px;
  background: rgba(12,14,30,0.97);
  border: 1px solid rgba($accent-secondary,0.12);
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.6);
  text-align: center;
  animation: modalEnter 0.3s ease;
}

.delete-icon {
  font-size: 2.5rem;
  margin-bottom: 12px;
}

.delete-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 8px;
}

.delete-desc {
  font-size: 0.85rem;
  color: $text-muted;
  line-height: 1.6;
  margin: 0 0 24px;
}

.delete-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-delete {
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba(255,60,90,0.15), rgba(200,30,60,0.1));
  border: 1px solid rgba(255,60,90,0.2);
  border-radius: 10px;
  color: #ff6b6b;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(255,60,90,0.25), rgba(200,30,60,0.15));
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(255,60,90,0.15);
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
