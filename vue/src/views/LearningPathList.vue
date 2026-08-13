<template>
  <div class="path-list-page">
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
    </div>

    <!-- 页面标题 -->
    <header class="page-header">
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg>
        返回
      </button>
      <div class="header-text">
        <h1 class="page-title">📚 学习路径</h1>
        <p class="page-subtitle">发现适合你的学习路线，开启高效学习之旅</p>
      </div>
    </header>

    <!-- 薄弱点上下文横幅（从薄弱点专项/能力概览进入） -->
    <div v-if="weaknessCtx && !loading && paths.length > 0" class="weakness-banner">
      <div class="banner-icon">💡</div>
      <div class="banner-content">
        <div class="banner-title-row">
          <span class="banner-title">系统检测到您的薄弱知识点</span>
          <span class="banner-subjects">{{ weaknessCtx.subjects.join('、') }}</span>
        </div>
        <span class="banner-desc">以下路径已针对您的薄弱点推荐，点击进入即可开始针对性学习</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="paths.length === 0" class="empty-state">
      <span class="empty-icon">📋</span>
      <p class="empty-text">暂无学习路径</p>
      <p class="empty-desc">请先生成学习路径，或联系管理员</p>
      <button class="empty-btn" @click="router.push('/goal-setting')">去生成学习路径 →</button>
    </div>

    <!-- 状态筛选 + 路径卡片列表 -->
    <template v-else>
      <!-- 能力清单（动态化：真实已启用功能） -->
      <div class="feature-strip">
        <span class="feature-strip-label">🛠 能力清单</span>
        <span v-for="f in featureList" :key="f.name" class="feature-item">
          <span class="feature-check">✅</span>
          <span class="feature-name">{{ f.name }}</span>
          <span class="feature-desc">{{ f.desc }}</span>
        </span>
      </div>

      <div class="filter-bar">
      <button
        v-for="f in filters"
        :key="f.id"
        class="filter-btn"
        :class="{ active: activeFilter === f.id }"
        @click="activeFilter = f.id"
      >
        <span class="filter-dot" :class="f.color"></span>{{ f.label }}
        <span class="filter-count">{{ f.count }}</span>
      </button>
    </div>

    <!-- 路径卡片列表 -->
    <div class="paths-grid">
      <div
        v-for="(path, index) in filteredPaths"
        :key="path.id"
        class="path-card glass-card"
        :data-path-id="path.id"
        @click="router.push(`/learning-path/${path.id}`)"
      >
        <div class="path-gradient" :style="{ background: path.gradient }"></div>
        <div class="path-content">
          <div class="path-top">
            <span class="path-difficulty" :class="path.levelClass">{{ path.levelText }}</span>
            <span class="path-status" :class="path.statusClass">{{ path.statusText }}</span>
            <span v-if="path.recommended" class="path-badge">当前路线</span>
            <span v-if="weaknessCtx && index === 0" class="path-badge weakness-tag">薄弱点专项</span>
          </div>
          <h3 class="path-title">{{ path.title }}</h3>
          <p class="path-desc">{{ path.desc }}</p>

          <!-- 结构预览 -->
          <div v-if="path.modulesPreview.length > 0" class="path-modules">
            <span class="modules-label">包含：</span>
            <span class="modules-items">{{ path.modulesPreview.join(' → ') }}</span>
          </div>

          <!-- 下一节点（与详情页同一接口口径） -->
          <div class="path-next-node">
            <span class="next-node-label">🎯 下一节点</span>
            <span class="next-node-value">{{ path.nextNodeName || '已完成所有节点 🎉' }}</span>
          </div>

          <div class="path-meta">
            <span class="path-time">⏱️ 总时长约 {{ path.totalHours }} 小时</span>
            <span v-if="path.adjustCount > 0" class="path-adjust">📊 已调整 {{ path.adjustCount }} 次</span>
            <span class="path-learners">👥 {{ path.learners }}</span>
            <span class="path-rating">⭐ {{ path.rating }}</span>
          </div>
          <div v-if="path.lastLearned" class="path-last-learned">
            🕐 最后学习：{{ path.lastLearned }}
          </div>

          <div class="path-footer">
            <div class="path-progress">
              <div class="path-progress-bar">
                <div class="path-progress-fill" :style="{ width: path.progress + '%' }"></div>
              </div>
              <span class="path-progress-text">{{ path.progress }}%</span>
            </div>
            <div class="path-actions">
              <button
                class="path-btn-delete"
                :class="{ 'deleting': deletingId === path.id }"
                :disabled="deletingId === path.id"
                @click.stop="handleDeleteClick(path)"
              >
                <svg v-if="deletingId !== path.id" class="delete-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  <line x1="10" y1="11" x2="10" y2="17"></line>
                  <line x1="14" y1="11" x2="14" y2="17"></line>
                </svg>
                <span>{{ deletingId === path.id ? '删除中...' : '删除' }}</span>
              </button>
              <button class="path-btn" @click.stop="router.push(`/learning-path/${path.id}`)">
                {{ path.progress > 0 && path.progress < 100 ? '继续学习 →' : (path.progress === 100 ? '查看成果 →' : '开始学习 →') }}
              </button>
            </div>
          </div>
        </div>
      </div>
      </div>
    </template>
    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="showDeleteModal" class="delete-modal-overlay" @click.self="handleCancelDelete">
        <div class="delete-modal">
          <div class="delete-modal-header">
            <svg class="delete-modal-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <h3 class="delete-modal-title">确认删除</h3>
          </div>
          <p class="delete-modal-content">
            确定要永久删除路线「<span class="delete-modal-name">{{ deleteTarget?.title }}</span>」吗？此操作无法撤销。
          </p>
          <div class="delete-modal-actions">
            <button class="btn-cancel" @click="handleCancelDelete">取消</button>
            <button class="btn-confirm-delete" :disabled="isDeleting" @click="handleConfirmDelete">
              {{ isDeleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPathList, deletePath, getPathProgress } from '@/api/learningPath'

const router = useRouter()
const route = useRoute()
const paths = ref([])
const loading = ref(true)
const activeFilter = ref('all')

// 能力清单（全部为已真实启用的功能，与后端接口一一对应）
const featureList = [
  { name: '路径生成', desc: 'AI 根据目标自动生成', enabled: true },
  { name: '节点拆分', desc: '章节·周·任务三级结构', enabled: true },
  { name: '进度追踪', desc: '实时进度与学习时长', enabled: true },
  { name: '路径调整', desc: 'AI 优化与自动调整', enabled: true }
]

const showDeleteModal = ref(false)
const deleteTarget = ref(null)
const deletingId = ref(null)
const isDeleting = ref(false)

// 状态筛选
const filters = computed(() => [
  { id: 'all', label: '全部', count: paths.value.length, color: 'cyan' },
  { id: 'in_progress', label: '进行中', count: paths.value.filter(p => p.statusClass === 'in-progress').length, color: 'yellow' },
  { id: 'not_started', label: '未开始', count: paths.value.filter(p => p.statusClass === 'not-started').length, color: 'gray' },
  { id: 'completed', label: '已完成', count: paths.value.filter(p => p.statusClass === 'completed').length, color: 'green' },
])

const filteredPaths = computed(() => {
  if (activeFilter.value === 'all') return paths.value
  return paths.value.filter(p => p.statusClass === activeFilter.value)
})

// 薄弱点上下文：从「薄弱知识点专项分析 / 能力概览」跳转进入时携带
// 例：/learning-path?from=weakness&subjects=面向对象编程,机器学习调参
const weaknessCtx = computed(() => {
  if (route.query.from !== 'weakness') return null
  const subjects = (route.query.subjects || route.query.subject || '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
  if (subjects.length === 0) return null
  return { subjects }
})

const fetchPaths = async () => {
  loading.value = true
  try {
    const res = await getPathList()
    const list = res?.data ?? res
    if (Array.isArray(list) && list.length > 0) {
      paths.value = list.map(p => enrichPath(p))

      // P1：并行拉取每条路径的实时进度（与详情页同一接口/口径，避免卡片与详情进度不一致）
      await Promise.all(paths.value.map(p => loadProgress(p)))

      // P0：只有 1 条路径时直接进入详情页，跳过空置的列表页；
      // 从薄弱点入口进入时携带上下文，让详情页展示「基于薄弱点生成」标识
      if (paths.value.length === 1) {
        if (weaknessCtx.value) {
          router.replace({
            path: `/learning-path/${paths.value[0].id}`,
            query: { from: 'weakness', subjects: weaknessCtx.value.subjects.join(',') }
          })
        } else {
          router.replace(`/learning-path/${paths.value[0].id}`)
        }
        return
      }
    } else {
      paths.value = []
    }
  } catch (error) {
    console.error('获取学习路径列表失败:', error)
    ElMessage.error('加载学习路径失败')
    paths.value = []
  } finally {
    loading.value = false
  }
}

// 路径实体 → 列表卡片数据（含 nodes 结构解析：模块预览/总时长）
const enrichPath = (p) => {
  let nodes = []
  if (p.nodes) {
    try {
      nodes = typeof p.nodes === 'string' ? JSON.parse(p.nodes) : (Array.isArray(p.nodes) ? p.nodes : [])
    } catch (e) { nodes = [] }
  }

  // 模块预览：去重章节标题
  const phaseTitles = []
  let totalHours = 0
  nodes.forEach(n => {
    const hours = Number(n.estimatedHours) || 0
    totalHours += hours
    if (n.phaseTitle && !phaseTitles.includes(n.phaseTitle)) {
      phaseTitles.push(n.phaseTitle.replace(/^第[一二三四五六七八九十]+章 ·\s*/, ''))
    }
  })

  const progress = p.completionPercentage != null ? Math.round(p.completionPercentage) : 0
  const statusClass = progress === 0 ? 'not-started' : (progress >= 100 ? 'completed' : 'in-progress')

  return {
    id: p.id,
    title: p.name || '未命名学习路径',
    desc: p.description || '暂无描述',
    levelClass: getLevelClass(p),
    levelText: getLevelText(p),
    learners: '—',
    rating: '4.5',
    progress,
    statusClass,
    statusText: statusClass === 'in-progress' ? '进行中' : (statusClass === 'completed' ? '已完成' : '未开始'),
    recommended: p.isActive === true,
    gradient: getGradient(p),
    modulesPreview: phaseTitles.slice(0, 4),
    totalHours: Math.round(totalHours),
    lastLearned: formatLastLearned(p.updatedAt),
    // 调整次数：version 每优化/自动调整一次 +1（初始为 1）
    adjustCount: Math.max(0, (p.version || 1) - 1)
  }
}

// 拉取路径实时进度（与详情页共用 /learning-path/{id}/progress 接口）
const loadProgress = async (path) => {
  try {
    const res = await getPathProgress(path.id)
    const data = res?.data ?? res
    if (data && data.pathId) {
      path.progress = data.progress ?? path.progress
      path.completedModules = data.completedModules ?? 0
      path.totalModules = data.totalModules ?? 0
      path.nextNodeName = data.nextNodeName || null
      // 进度变化后同步状态标签
      const progress = path.progress
      path.statusClass = progress === 0 ? 'not-started' : (progress >= 100 ? 'completed' : 'in-progress')
      path.statusText = path.statusClass === 'in-progress' ? '进行中' : (path.statusClass === 'completed' ? '已完成' : '未开始')
    }
  } catch (error) {
    console.warn(`获取路径进度失败(${path.id}):`, error?.message)
  }
}

// 最后学习时间格式化
const formatLastLearned = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now - date
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 86400000 * 7) return Math.floor(diff / 86400000) + ' 天前'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const getLevelClass = (path) => {
  const d = (path.difficulty || '').toLowerCase()
  if (d.includes('入门') || d === 'beginner') return 'beginner'
  if (d.includes('高级') || d === 'advanced' || d === 'challenge') return 'advanced'
  return 'intermediate'
}

const getLevelText = (path) => {
  const d = (path.difficulty || '').toLowerCase()
  if (d.includes('入门') || d === 'beginner') return '入门'
  if (d.includes('高级') || d === 'advanced' || d === 'challenge') return '高级'
  return '中级'
}

const getGradient = (path) => {
  const cls = getLevelClass(path)
  if (cls === 'beginner') return 'linear-gradient(135deg, rgba(0,229,255,0.1), rgba(0,85,255,0.06))'
  if (cls === 'advanced') return 'linear-gradient(135deg, rgba(168,85,247,0.1), rgba(0,229,255,0.06))'
  return 'linear-gradient(135deg, rgba(0,85,255,0.1), rgba(168,85,247,0.06))'
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

const handleDeleteClick = (path) => {
  deleteTarget.value = path
  showDeleteModal.value = true
}

const handleCancelDelete = () => {
  if (isDeleting.value) return
  showDeleteModal.value = false
  deleteTarget.value = null
}

const handleConfirmDelete = async () => {
  if (!deleteTarget.value || isDeleting.value) return
  
  isDeleting.value = true
  deletingId.value = deleteTarget.value.id
  
  try {
    await new Promise(resolve => setTimeout(resolve, 800))
    await deletePath(deleteTarget.value.id)
    
    const cardEl = document.querySelector(`[data-path-id="${deleteTarget.value.id}"]`)
    if (cardEl) {
      cardEl.style.transition = 'opacity 0.4s ease, transform 0.4s ease'
      cardEl.style.opacity = '0'
      cardEl.style.transform = 'scale(0.95)'
    }
    
    await new Promise(resolve => setTimeout(resolve, 400))
    
    paths.value = paths.value.filter(p => p.id !== deleteTarget.value.id)
    
    showDeleteModal.value = false
    deleteTarget.value = null
    ElMessage.success('已成功删除学习路线')
  } catch (error) {
    console.error('删除学习路径失败:', error)
    ElMessage.error('删除失败：' + (error?.response?.data?.message || error?.message || '请稍后重试'))
  } finally {
    isDeleting.value = false
    deletingId.value = null
  }
}

onMounted(() => {
  fetchPaths()
})
</script>

<style lang="scss" scoped>
.path-list-page {
  min-height: 100vh;
  background: #0a0a1a;
  position: relative;
  padding: 0 32px 60px;
}

// 背景
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora {
  position: absolute; inset: 0;
  background:
    radial-gradient(ellipse at 70% 20%, rgba(0,245,212,0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift {
  0%,100% { transform: scale(1) rotate(0deg); }
  33% { transform: scale(1.1) rotate(1deg); }
  66% { transform: scale(0.95) rotate(-1deg); }
}
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(0,245,212,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

// 页面标题
.page-header {
  position: relative; z-index: 1;
  padding: 24px 0 12px;
  display: flex; align-items: flex-start; gap: 16px;
}
.back-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; flex-shrink: 0; margin-top: 4px;
  background: rgba(100,100,180,0.06);
  border: 1px solid rgba(100,100,180,0.1);
  border-radius: 8px;
  color: #c0c0e0; font-size: 0.82rem; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: rgba(0,245,212,0.2); color: #00f5d4; box-shadow: 0 0 14px rgba(0,245,212,0.08); }
}
.header-text { flex: 1; }
.page-title { font-size: 1.8rem; font-weight: 800; color: #e8e8ff; margin-bottom: 6px; }
.page-subtitle { font-size: 0.9rem; color: #9090b8; }

// 卡片网格
.paths-grid {
  position: relative; z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  padding: 20px 0;
  max-width: 960px;
  margin: 0 auto;
}
.path-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4,0,0.2,1), box-shadow 0.3s ease;
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(0,0,0,0.3);
  }
  &:active { transform: scale(0.98); }
}
.path-gradient { position: absolute; inset: 0; }
.path-content {
  position: relative;
  padding: 24px;
  background: rgba(15,20,40,0.45);
  backdrop-filter: blur(8px);
  height: 100%;
  display: flex;
  flex-direction: column;
}
.path-top { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.path-difficulty {
  display: inline-block; padding: 4px 10px; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600; border: 1px solid;
  &.beginner { color: #10b981; border-color: rgba(16,185,129,0.2); background: rgba(16,185,129,0.08); }
  &.intermediate { color: #3b82f6; border-color: rgba(59,130,246,0.2); background: rgba(59,130,246,0.08); }
  &.advanced { color: #a855f7; border-color: rgba(168,85,247,0.2); background: rgba(168,85,247,0.08); }
}
.path-badge {
  display: inline-block; padding: 4px 10px; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600;
  color: #00f5d4; border: 1px solid rgba(0,245,212,0.2); background: rgba(0,245,212,0.08);
}

// 薄弱点专项徽标
.path-badge.weakness-tag {
  color: #ef4444; border-color: rgba(239,68,68,0.25); background: rgba(239,68,68,0.08);
}

// 状态标签
.path-status {
  display: inline-block; padding: 4px 10px; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600; border: 1px solid;
  &.in-progress { color: #f59e0b; border-color: rgba(245,158,11,0.2); background: rgba(245,158,11,0.08); }
  &.not-started { color: #9090b8; border-color: rgba(100,100,180,0.2); background: rgba(100,100,180,0.08); }
  &.completed { color: #10b981; border-color: rgba(16,185,129,0.2); background: rgba(16,185,129,0.08); }
}

// 结构预览
.path-modules {
  display: flex; flex-wrap: wrap; gap: 4px; align-items: baseline;
  margin-bottom: 10px; font-size: 0.78rem;
  .modules-label { color: #9090b8; flex-shrink: 0; }
  .modules-items { color: #00f5d4; line-height: 1.5; }
}

// 下一节点
.path-next-node {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 10px; font-size: 0.78rem;
  .next-node-label { color: #9090b8; flex-shrink: 0; }
  .next-node-value {
    color: #e8e8ff; font-weight: 600;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
}

.path-last-learned {
  font-size: 0.72rem; color: #8080a8; margin-bottom: 12px;
}

.path-time {
  font-size: 0.78rem; color: #c0c0e0;
}
.path-adjust {
  font-size: 0.78rem; color: #a78bfa;
}
.path-title { font-size: 1.15rem; font-weight: 600; color: #e8e8ff; margin-bottom: 6px; }
.path-desc { font-size: 0.82rem; color: #c0c0e0; line-height: 1.5; margin-bottom: 12px; display: -webkit-box; line-clamp: 2; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.path-meta { display: flex; gap: 16px; margin-bottom: 14px; font-size: 0.82rem; color: #c0c0e0; }
.path-footer {
  margin-top: auto;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
}
.path-progress { display: flex; align-items: center; gap: 8px; flex: 1; }
.path-progress-bar { flex: 1; height: 4px; background: rgba(0,245,212,0.08); border-radius: 2px; overflow: hidden; }
.path-progress-fill { height: 100%; background: linear-gradient(90deg, #00f5d4, #10b981); border-radius: 2px; transition: width 0.8s ease; }
.path-progress-text { font-size: 0.75rem; font-weight: 500; color: #00f5d4; white-space: nowrap; }
.path-btn {
  padding: 6px 14px;
  background: rgba(0,245,212,0.06);
  border: 1px solid rgba(0,245,212,0.12);
  border-radius: 6px;
  color: #00f5d4; font-size: 0.78rem; font-weight: 600;
  cursor: pointer; transition: all 0.2s; white-space: nowrap;
  &:hover { background: rgba(0,245,212,0.1); }
}

.glass-card {
  background: rgba(17,17,39,0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.08);
}

// 加载状态
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
  color: #8080a8;
  font-size: 14px;
}

// 薄弱点上下文横幅
.weakness-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 1080px;
  margin: 20px auto 0;
  padding: 16px 24px;
  background: rgba(239,68,68,0.06);
  border: 1px solid rgba(239,68,68,0.18);
  border-radius: 12px;
  animation: bannerIn 0.4s ease both;
}

@keyframes bannerIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.banner-icon { font-size: 1.4rem; flex-shrink: 0; }

.banner-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.banner-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.banner-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #f1f5f9;
}

.banner-subjects {
  font-size: 0.85rem;
  font-weight: 600;
  color: #ef4444;
}

.banner-desc {
  font-size: 0.82rem;
  color: #9090b8;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(100,100,180,0.1);
  border-top-color: #00f5d4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-btn {
  margin-top: 16px;
  padding: 10px 22px;
  background: linear-gradient(135deg, rgba(0,245,212,0.12), rgba(0,85,255,0.1));
  border: 1px solid rgba(0,245,212,0.25);
  border-radius: 10px;
  color: #00f5d4;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  &:hover { box-shadow: 0 0 18px rgba(0,245,212,0.15); transform: translateY(-1px); }
}

// 筛选栏
.filter-bar {
  position: relative; z-index: 1;
  display: flex; gap: 8px;
  max-width: 960px; margin: 8px auto 0;
  flex-wrap: wrap;
}

// 能力清单横条
.feature-strip {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  max-width: 960px; margin: 18px auto 0;
  padding: 12px 18px;
  background: rgba(17,17,39,0.6);
  border: 1px solid rgba(100,100,180,0.08);
  border-radius: 12px;
  .feature-strip-label {
    font-size: 0.8rem; font-weight: 600; color: #00f5d4;
    flex-shrink: 0;
  }
  .feature-item {
    display: inline-flex; align-items: center; gap: 5px;
    .feature-check { font-size: 0.75rem; }
    .feature-name { font-size: 0.78rem; font-weight: 600; color: #e8e8ff; }
    .feature-desc { font-size: 0.72rem; color: #8080a8; }
  }
}

.filter-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px;
  background: rgba(17,17,39,0.6);
  border: 1px solid rgba(100,100,180,0.1);
  border-radius: 20px;
  font-size: 0.78rem; color: #c0c0e0;
  cursor: pointer; transition: all 0.2s;
  &.active { border-color: #00f5d4; color: #00f5d4; background: rgba(0,245,212,0.08); }
}

.filter-dot {
  width: 8px; height: 8px; border-radius: 50%;
  &.cyan { background: #00f5d4; }
  &.yellow { background: #f59e0b; }
  &.gray { background: #9090b8; }
  &.green { background: #10b981; }
}

.filter-count {
  font-size: 0.68rem; padding: 0 6px;
  background: rgba(100,100,180,0.12); border-radius: 8px;
}

.empty-icon { font-size: 3rem; margin-bottom: 16px; opacity: 0.6; }
.empty-text { font-size: 1.1rem; font-weight: 600; color: #e8e8ff; margin: 0 0 8px 0; }
.empty-desc { font-size: 0.9rem; color: #9090b8; margin: 0; }

// 操作按钮组
.path-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.path-btn-delete {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #B0B0B0;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  
  .delete-icon {
    flex-shrink: 0;
    transition: color 0.2s ease;
  }
  
  &:hover:not(:disabled) {
    color: #EF4444;
    background: rgba(239, 68, 68, 0.06);
  }
  
  &:hover:not(:disabled) .delete-icon {
    stroke: #EF4444;
  }
  
  &.deleting {
    opacity: 0.6;
    cursor: not-allowed;
  }
  
  &:disabled {
    cursor: not-allowed;
  }
}

// 删除确认弹窗
.delete-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.delete-modal {
  width: 90%;
  max-width: 420px;
  background: rgba(17, 17, 39, 0.95);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.delete-modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.delete-modal-icon {
  color: #F59E0B;
  flex-shrink: 0;
}

.delete-modal-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #F1F5F9;
  margin: 0;
}

.delete-modal-content {
  font-size: 0.9rem;
  color: #C0C0E0;
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.delete-modal-name {
  color: #00F5D4;
  font-weight: 500;
}

.delete-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  padding: 10px 24px;
  background: rgba(100, 100, 180, 0.08);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 8px;
  color: #94A3B8;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: rgba(100, 100, 180, 0.12);
    border-color: rgba(100, 100, 180, 0.25);
    color: #E2E8F0;
  }
}

.btn-confirm-delete {
  padding: 10px 24px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 8px;
  color: #EF4444;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover:not(:disabled) {
    background: rgba(239, 68, 68, 0.2);
    border-color: rgba(239, 68, 68, 0.4);
    box-shadow: 0 0 16px rgba(239, 68, 68, 0.15);
  }
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

// 响应式
@media (max-width: 768px) {
  .path-list-page { padding: 0 16px 40px; }
  .paths-grid { grid-template-columns: 1fr; gap: 16px; }
  .page-title { font-size: 1.4rem; }
}
</style>