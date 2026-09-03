<template>
  <div class="path-list-page">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">🧭</span>
            <span>学习路径</span>
            <span class="title-sub">AI 为你规划的专属学习路线</span>
          </h1>
        </div>
        <div class="header-right">
          <div class="header-actions">
            <div class="view-toggle">
              <button class="toggle-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'" title="列表视图">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
              </button>
              <button class="toggle-btn" :class="{ active: viewMode === 'map' }" @click="viewMode = 'map'" title="地图视图">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/><line x1="8" y1="2" x2="8" y2="18"/><line x1="16" y1="6" x2="16" y2="22"/></svg>
              </button>
            </div>
            <button class="btn-new-path" @click="showCreateModal = true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              新建学习路径
            </button>
          </div>
        </div>
      </div>

      <!-- 步骤进度条 -->
      <div class="step-bar">
        <div class="step-item completed">
          <div class="step-dot">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <span class="step-label">目标设定</span>
        </div>
        <div class="step-line"></div>
        <div class="step-item completed">
          <div class="step-dot">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <span class="step-label">AI 规划</span>
        </div>
        <div class="step-line active"></div>
        <div class="step-item active">
          <div class="step-dot pulse">
            <span class="dot-inner"></span>
          </div>
          <span class="step-label">路径执行</span>
        </div>
        <div class="step-line"></div>
        <div class="step-item">
          <div class="step-dot">
            <span class="dot-num">4</span>
          </div>
          <span class="step-label">学习完成</span>
        </div>
      </div>
    </header>

    <!-- 薄弱点横幅 -->
    <div v-if="weaknessCtx && !loading && paths.length > 0" class="weakness-banner">
      <div class="banner-bar"></div>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="banner-icon"><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><circle cx="12" cy="12" r="10"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
      <div class="banner-text">
        <span>薄弱点专项：{{ weaknessCtx.subjects.join('、') }}</span>
      </div>
    </div>

    <!-- 加载 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>加载中…</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="paths.length === 0" class="empty-state">
      <div class="empty-illustration">
        <svg width="180" height="160" viewBox="0 0 180 160">
          <!-- AI 机器人形象 -->
          <defs>
            <linearGradient id="robotGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#8b5cf6;stop-opacity:0.3" />
              <stop offset="100%" style="stop-color:#00f0ff;stop-opacity:0.3" />
            </linearGradient>
          </defs>
          <!-- 机器人头部 -->
          <circle cx="90" cy="60" r="35" fill="url(#robotGrad)" stroke="rgba(139,92,246,0.4)" stroke-width="2"/>
          <!-- 天线 -->
          <line x1="90" y1="25" x2="90" y2="15" stroke="#00f0ff" stroke-width="2"/>
          <circle cx="90" cy="12" r="4" fill="#00f0ff"/>
          <!-- 眼睛 -->
          <circle cx="78" cy="55" r="5" fill="#00f0ff"/>
          <circle cx="102" cy="55" r="5" fill="#00f0ff"/>
          <!-- 嘴巴 -->
          <path d="M78 72 Q90 82 102 72" stroke="#00f0ff" stroke-width="2" fill="none"/>
          <!-- 身体 -->
          <rect x="65" y="95" width="50" height="40" rx="10" fill="url(#robotGrad)" stroke="rgba(139,92,246,0.4)" stroke-width="2"/>
          <!-- 手臂 -->
          <circle cx="55" cy="110" r="8" fill="url(#robotGrad)" stroke="rgba(139,92,246,0.4)" stroke-width="1.5"/>
          <circle cx="125" cy="110" r="8" fill="url(#robotGrad)" stroke="rgba(139,92,246,0.4)" stroke-width="1.5"/>
          <!-- 地图 -->
          <rect x="130" y="70" width="35" height="28" rx="4" fill="rgba(0,240,255,0.1)" stroke="rgba(0,240,255,0.3)" stroke-width="1.5"/>
          <path d="M138 82 L148 76 L155 85" stroke="#00f0ff" stroke-width="1.5" fill="none"/>
          <circle cx="152" cy="88" r="2" fill="#00f0ff"/>
        </svg>
      </div>
      <h3 class="empty-title">还没有学习路径</h3>
      <p class="empty-desc">让 AI 帮你规划一个吧！</p>
      <button class="empty-cta" @click="showCreateModal = true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        立即生成我的第一条路径
      </button>
      <div class="empty-hot-paths">
        <span class="hot-hint">或选择热门路径快速开始：</span>
        <div class="hot-tags">
          <button v-for="path in hotPaths.slice(0, 4)" :key="path.goal" class="hot-tag" @click="quickCreateFromHot(path)">
            {{ path.icon }} {{ path.title }}
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选为空 -->
    <div v-else-if="filteredPaths.length === 0" class="empty-state">
      <p>当前筛选条件下没有匹配的路径</p>
    </div>

    <!-- 内容区 -->
    <template v-else>
      <!-- 筛选 Tab 栏 -->
      <div class="filter-bar">
        <button
          v-for="f in filters"
          :key="f.id"
          class="filter-tab"
          :class="{ active: activeFilter === f.id }"
          @click="activeFilter = f.id"
        >
          {{ f.label }}
          <span class="filter-badge" :class="f.color">{{ f.count }}</span>
        </button>
      </div>

      <!-- 列表视图 -->
      <div v-if="viewMode === 'list'" class="paths-grid">
        <div
          v-for="path in filteredPaths"
          :key="path.id"
          class="path-card"
          :class="{ 'is-active': path.recommended }"
          :data-path-id="path.id"
          @click="router.push(`/learning-path/${path.id}`)"
        >
          <!-- ═══ 头部：身份区 ═══ -->
          <div class="card-header">
            <div class="card-tags">
              <span class="tag tag-level" :class="path.levelClass">{{ path.levelText }}</span>
              <span class="tag tag-status" :class="path.statusClass">{{ path.statusText }}</span>
              <span v-if="path.source === 'ai_chat'" class="tag tag-source ai-chat">AI对话</span>
              <span v-if="path.source === 'ai_assessment'" class="tag tag-source ai-assessment">测评推荐</span>
              <span v-if="path.recommended" class="tag tag-active">当前路线</span>
              <span v-if="weaknessCtx" class="tag tag-weakness">薄弱点专项</span>
            </div>
            <h3 class="card-title">{{ path.title }}</h3>
            <div class="card-meta-row">
              <span>{{ path.desc }}</span>
            </div>
          </div>

          <!-- ═══ 中部：内容预览区 ═══ -->
          <div class="card-body">
            <!-- 章节节点图 -->
            <div v-if="path.modulesPreview.length > 0" class="chapter-nodes">
              <template v-for="(mod, i) in path.modulesPreview" :key="i">
                <span
                  class="node-chip"
                  :class="{
                    completed: i < path.completedModules,
                    current: i === path.completedModules
                  }"
                >
                  <span class="node-dot"></span>
                  {{ mod }}
                </span>
                <svg v-if="i < path.modulesPreview.length - 1" class="node-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              </template>
              <span v-if="path.totalModules > 3" class="node-more">+{{ path.totalModules - 3 }}</span>
            </div>

            <!-- 关键节点预览 -->
            <div v-if="path.keyNodes && path.keyNodes.length > 0" class="key-nodes-preview">
              <div class="key-nodes-header">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
                <span>关键学习节点</span>
              </div>
              <div class="key-nodes-list">
                <div v-for="(node, idx) in path.keyNodes.slice(0, 3)" :key="idx" class="key-node-item">
                  <span class="key-node-dot"></span>
                  <span class="key-node-title">{{ node.title }}</span>
                  <span class="key-node-hours">{{ node.hours }}h</span>
                </div>
              </div>
            </div>

            <!-- 下一节点提示 -->
            <div v-if="path.nextNode" class="next-node">
              <span class="next-icon">→</span>
              <span class="next-label">下一步：</span>
              <span class="next-value">{{ path.nextNode.title }}</span>
              <span class="next-reason" :class="path.nextNode.reasonType">{{ path.nextNode.reason }}</span>
            </div>
          </div>

          <!-- ═══ 底部：数据与操作 ═══ -->
          <div class="card-footer">
            <!-- 数据网格 -->
            <div class="data-grid">
              <div class="data-cell">
                <span class="data-value">{{ path.totalModules }}<small>章</small></span>
                <span class="data-label">总章节</span>
              </div>
              <div class="data-cell">
                <span class="data-value">{{ path.totalHours }}<small>h</small></span>
                <span class="data-label">总学时</span>
              </div>
              <div class="data-cell">
                <span class="data-value">{{ path.completedModules }}</span>
                <span class="data-label">已完成</span>
              </div>
              <div class="data-cell">
                <span class="data-value">{{ path.remainingDays }}<small>天</small></span>
                <span class="data-label">剩余</span>
              </div>
            </div>

            <!-- 进度条 -->
            <div class="progress-bar">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: path.progressPercent + '%' }"></div>
              </div>
              <span class="progress-value">{{ path.progressPercent }}%</span>
            </div>

            <!-- AI 预测 -->
            <div v-if="path.estimatedCompletion" class="ai-predict">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span>AI 预计 {{ path.estimatedCompletion }} 完成</span>
            </div>

            <!-- 操作按钮 -->
            <div class="card-actions">
              <div class="action-left">
                <button class="btn-icon" @click.stop="toggleFavorite(path)" :title="path.isFavorite ? '取消收藏' : '收藏'">
                  <svg width="14" height="14" viewBox="0 0 24 24" :fill="path.isFavorite ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                </button>
                <button class="btn-icon danger" @click.stop="confirmDelete(path)" title="删除">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                </button>
              </div>
              <button class="btn-primary" :class="{ breathing: path.statusClass === 'in-progress' }" @click.stop="router.push(`/learning-path/${path.id}`)">
                {{ path.statusClass === 'in-progress' ? '继续学习' : '开始学习' }}
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 地图视图 -->
      <div v-else-if="viewMode === 'map'" class="map-view">
        <div class="map-container">
          <div
            v-for="(path, idx) in filteredPaths"
            :key="path.id"
            class="map-node"
            :class="path.statusClass"
            @click="router.push(`/learning-path/${path.id}`)"
          >
            <div class="map-node-dot">
              <span class="map-dot-num">{{ idx + 1 }}</span>
            </div>
            <div class="map-node-info">
              <div class="map-node-title">{{ path.title }}</div>
              <div class="map-node-progress">{{ path.progressPercent }}%</div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 创建学习路径模态框 -->
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="create-modal">
        <div class="create-modal-header">
          <h3>新建学习路径</h3>
          <button class="modal-close-btn" @click="showCreateModal = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="create-modal-body">
          <div class="create-tabs">
            <button class="create-tab" :class="{ active: createMode === 'ai' }" @click="createMode = 'ai'">
              <span class="tab-icon">🤖</span>
              <span class="tab-text">AI 生成</span>
            </button>
            <button class="create-tab" :class="{ active: createMode === 'hot' }" @click="createMode = 'hot'">
              <span class="tab-icon">🔥</span>
              <span class="tab-text">热门路径</span>
            </button>
            <button class="create-tab" :class="{ active: createMode === 'import' }" @click="createMode = 'import'">
              <span class="tab-icon">💬</span>
              <span class="tab-text">从对话导入</span>
            </button>
          </div>

          <!-- AI 生成表单 -->
          <div v-if="createMode === 'ai'" class="create-form">
            <div class="form-field">
              <label>学习目标</label>
              <input v-model="createForm.goal" class="form-input" placeholder="例如：成为全栈工程师" />
            </div>
            <div class="form-row">
              <div class="form-field">
                <label>所属领域</label>
                <input v-model="createForm.field" class="form-input" placeholder="例如：编程" />
              </div>
              <div class="form-field">
                <label>预计时长（周）</label>
                <select v-model="createForm.duration" class="form-select">
                  <option value="4">4 周</option>
                  <option value="8">8 周</option>
                  <option value="12">12 周</option>
                  <option value="16">16 周</option>
                  <option value="24">24 周</option>
                </select>
              </div>
            </div>
          </div>

          <!-- 热门路径 -->
          <div v-else-if="createMode === 'hot'" class="hot-paths">
            <div
              v-for="path in hotPaths"
              :key="path.goal"
              class="hot-path-card"
              :class="{ selected: selectedHotPath?.goal === path.goal }"
              @click="selectedHotPath = path"
            >
              <span class="hot-icon">{{ path.icon }}</span>
              <div class="hot-info">
                <span class="hot-title">{{ path.title }}</span>
                <span class="hot-desc">{{ path.desc }}</span>
              </div>
              <span class="hot-duration">{{ path.duration }}</span>
            </div>
          </div>

          <!-- 从对话导入 -->
          <div v-else-if="createMode === 'import'" class="import-section">
            <div v-if="recentChats.length === 0" class="no-chats">
              <p>暂无可用对话</p>
              <span>请先通过 AI 对话生成学习计划</span>
            </div>
            <div v-else class="chat-list">
              <div
                v-for="chat in recentChats"
                :key="chat.id"
                class="chat-item"
                :class="{ selected: selectedChat?.id === chat.id }"
                @click="selectedChat = chat"
              >
                <div class="chat-preview">
                  <span class="chat-title">{{ chat.title }}</span>
                  <span class="chat-time">{{ chat.time }}</span>
                </div>
                <span class="chat-count">{{ chat.messageCount }} 条消息</span>
              </div>
            </div>
          </div>
        </div>
        <div class="create-modal-footer">
          <button class="btn-cancel" @click="showCreateModal = false">取消</button>
          <button
            class="btn-generate"
            :disabled="creating || !canGenerate"
            @click="handleGenerate"
          >
            <div v-if="creating" class="creating-spinner"></div>
            <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/></svg>
            {{ creating ? '生成中...' : '生成路径' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <DeleteConfirmDialog
      v-model:visible="showDeleteModal"
      :config="deleteDialogConfig"
      :loading="isDeleting"
      @confirm="handleDeleteConfirm"
      @cancel="showDeleteModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPathList, deletePath, getPathProgress, generatePath } from '@/api/learningPath'
import DeleteConfirmDialog from '@/components/common/DeleteConfirmDialog.vue'

const router = useRouter()
const route = useRoute()
const pageRef = ref(null)
const paths = ref([])
const loading = ref(true)
const activeFilter = ref('all')
const viewMode = ref('list')

// ===== 粒子效果 =====
const particleStyle = () => {
  const size = Math.random() * 3 + 1
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: size + 'px',
    height: size + 'px',
    animationDuration: (Math.random() * 20 + 15) + 's',
    animationDelay: (Math.random() * 10) + 's',
    opacity: Math.random() * 0.3 + 0.1
  }
}

const showDeleteModal = ref(false)
const deleteTarget = ref(null)
const deletingId = ref(null)
const isDeleting = ref(false)
const deleteDialogConfig = ref({
  title: '删除学习路径',
  message: '',
  type: 'warning',
  showSoftDelete: false,
  details: []
})

// ===== 新建学习路径相关 =====
const showCreateModal = ref(false)
const createMode = ref('ai') // ai | hot | import
const creating = ref(false)
const createForm = ref({
  goal: '',
  field: '',
  duration: '12'
})
const selectedHotPath = ref(null)
const selectedChat = ref(null)
const recentChats = ref([])

// 热门路径数据
const hotPaths = [
  { icon: '🐍', title: 'Python 全栈工程师', desc: '从零基础到全栈开发能力', goal: 'Python全栈工程师培养', duration: '16周', field: '编程' },
  { icon: '⚛️', title: 'React 前端专家', desc: '现代React生态深度掌握', goal: 'React前端专家培养', duration: '12周', field: '前端' },
  { icon: '☕', title: 'Java 后端架构师', desc: 'Spring生态 + 微服务架构', goal: 'Java后端架构师培养', duration: '20周', field: '编程' },
  { icon: '📊', title: '数据分析师', desc: 'Python数据分析 + 可视化', goal: '数据分析师技能培养', duration: '10周', field: '编程' },
  { icon: '🤖', title: 'AI 应用开发者', desc: 'LLM应用 + RAG + Agent', goal: 'AI应用开发者培养', duration: '14周', field: '编程' },
  { icon: '🏗️', title: '系统设计面试突击', desc: '大厂系统设计面试备战', goal: '系统设计面试突击', duration: '8周', field: '系统设计' },
]

const filters = computed(() => [
  { id: 'all', label: '全部', count: paths.value.length, color: '' },
  { id: 'in_progress', label: '进行中', count: paths.value.filter(p => p.statusClass === 'in-progress').length, color: 'amber' },
  { id: 'not_started', label: '未开始', count: paths.value.filter(p => p.statusClass === 'not-started').length, color: 'slate' },
  { id: 'completed', label: '已完成', count: paths.value.filter(p => p.statusClass === 'completed').length, color: 'green' },
])

const filteredPaths = computed(() => {
  if (activeFilter.value === 'all') return paths.value
  return paths.value.filter(p => p.statusClass === activeFilter.value)
})

const weaknessCtx = computed(() => {
  if (route.query.from !== 'weakness') return null
  const subjects = (route.query.subjects || route.query.subject || '')
    .split(',').map(s => s.trim()).filter(Boolean)
  return subjects.length > 0 ? { subjects } : null
})

const nextNodeTags = [
  { icon: '🔥', text: '高频考点', type: 'hot' },
  { icon: '🛠', text: '实战前置', type: 'practice' },
  { icon: '💡', text: '基础核心', type: 'core' },
]

const enrichPath = (p) => {
  let nodes = []
  if (p.nodes) {
    try { nodes = typeof p.nodes === 'string' ? JSON.parse(p.nodes) : (Array.isArray(p.nodes) ? p.nodes : []) } catch { nodes = [] }
  }
  const phaseTitles = []

  // 解析阶段标题
  if (p.phase_titles) {
    try {
      const pt = typeof p.phase_titles === 'string' ? JSON.parse(p.phase_titles) : p.phase_titles
      if (Array.isArray(pt)) phaseTitles.push(...pt)
    } catch {}
  }

  // 提取模块预览（最多 3 个）
  const modulesPreview = nodes.slice(0, 3).map(n => n.title || n.name || '未命名')
  const totalModules = nodes.length
  const completedModules = p.completed_nodes || 0

  // 计算总学时
  const totalHours = nodes.reduce((sum, n) => sum + (n.estimated_hours || n.hours || 0), 0)

  // 进度百分比
  const progressPercent = totalModules > 0 ? Math.round((completedModules / totalModules) * 100) : 0

  // 状态样式映射
  let statusClass = 'not-started'
  let statusText = '未开始'
  if (p.status === 'completed' || progressPercent === 100) {
    statusClass = 'completed'
    statusText = '已完成'
  } else if (progressPercent > 0) {
    statusClass = 'in-progress'
    statusText = '进行中'
  }

  // 等级样式映射
  let levelClass = 'beginner'
  let levelText = '入门'
  if (p.level === 'intermediate') { levelClass = 'intermediate'; levelText = '进阶' }
  else if (p.level === 'advanced') { levelClass = 'advanced'; levelText = '高级' }

  // 关键节点（取前 3 个未完成的）
  const keyNodes = nodes
    .filter((n, i) => i >= completedModules)
    .slice(0, 3)
    .map(n => ({
      title: n.title || n.name || '未命名',
      hours: n.estimated_hours || n.hours || 0
    }))

  // 下一节点
  let nextNode = null
  if (completedModules < totalModules) {
    const next = nodes[completedModules]
    if (next) {
      // 随机选择一个原因类型
      const reasonType = nextNodeTags[Math.floor(Math.random() * nextNodeTags.length)].type
      const reasonObj = nextNodeTags.find(t => t.type === reasonType)
      nextNode = {
        title: next.title || next.name || '未命名',
        reason: reasonObj.text,
        reasonType
      }
    }
  }

  // 剩余天数估算
  const remainingDays = totalModules > completedModules
    ? Math.ceil((totalModules - completedModules) * 2.5)
    : 0

  // AI 预估完成时间
  let estimatedCompletion = null
  if (remainingDays > 0) {
    const date = new Date()
    date.setDate(date.getDate() + remainingDays)
    estimatedCompletion = `${date.getMonth() + 1}月${date.getDate()}日`
  }

  return {
    ...p,
    modulesPreview,
    totalModules,
    completedModules,
    totalHours,
    progressPercent,
    statusClass,
    statusText,
    levelClass,
    levelText,
    keyNodes,
    nextNode,
    remainingDays,
    estimatedCompletion,
    isFavorite: p.is_favorite || false
  }
}

const canGenerate = computed(() => {
  if (createMode.value === 'ai') {
    return createForm.value.goal.trim().length > 0
  } else if (createMode.value === 'hot') {
    return selectedHotPath.value !== null
  } else {
    return selectedChat.value !== null
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPathList()
    paths.value = (res.data || []).map(enrichPath)
  } catch (err) {
    ElMessage.error('加载学习路径失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

const confirmDelete = (path) => {
  deleteTarget.value = path
  deleteDialogConfig.value = {
    title: '删除学习路径',
    message: `确定要删除学习路径「${path.title}」吗？此操作不可恢复。`,
    type: 'warning',
    showSoftDelete: false,
    details: [
      { label: '路径名称', value: path.title },
      { label: '当前进度', value: `${path.progressPercent}%` },
      { label: '总章节', value: `${path.totalModules} 章` }
    ]
  }
  showDeleteModal.value = true
}

const handleDeleteConfirm = async () => {
  if (!deleteTarget.value) return
  isDeleting.value = true
  try {
    await deletePath(deleteTarget.value.id)
    ElMessage.success('删除成功')
    showDeleteModal.value = false
    loadData()
  } catch (err) {
    ElMessage.error('删除失败')
  } finally {
    isDeleting.value = false
  }
}

const toggleFavorite = async (path) => {
  // TODO: 调用收藏/取消收藏 API
  path.isFavorite = !path.isFavorite
  ElMessage.success(path.isFavorite ? '已收藏' : '已取消收藏')
}

const handleGenerate = async () => {
  creating.value = true
  try {
    let payload = {}
    if (createMode.value === 'ai') {
      payload = {
        goal: createForm.value.goal,
        field: createForm.value.field,
        duration_weeks: parseInt(createForm.value.duration)
      }
    } else if (createMode.value === 'hot') {
      payload = {
        goal: selectedHotPath.value.goal,
        field: selectedHotPath.value.field,
        duration_weeks: parseInt(selectedHotPath.value.duration)
      }
    } else {
      payload = { chat_id: selectedChat.value.id }
    }
    await generatePath(payload)
    ElMessage.success('学习路径生成成功！')
    showCreateModal.value = false
    loadData()
  } catch (err) {
    ElMessage.error('生成失败，请重试')
  } finally {
    creating.value = false
  }
}

const quickCreateFromHot = (path) => {
  createMode.value = 'hot'
  selectedHotPath.value = path
  showCreateModal.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

// ============================================
// 知途 (Zhitu) · 清洁科技设计风格
// Vercel/Supabase 风格 · 极简 · 专业
// ============================================

.mono { font-family: $font-mono; }

.path-list-page {
  min-height: 100vh;
  padding: $space-6;
  font-family: $font-sans;
}

.page-header { @include page-header-base; max-width: 1100px; margin: 0 auto 24px; }
.page-title { @include page-title-base; }
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; }

.btn-new-path {
  @include page-header-btn-primary;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.25);
  color: $accent-indigo;
  &:hover { 
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
  svg { width: 16px; height: 16px; }
}

.view-toggle {
  display: flex;
  gap: 2px;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: 2px;
}
.toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 30px;
  border: none;
  border-radius: $radius-sm;
  background: transparent;
  color: $text-muted;
  cursor: pointer;
  transition: all $transition-fast;
  &.active { 
    background: rgba($accent-indigo, 0.1); 
    color: $accent-indigo; 
  }
  &:hover:not(.active) { color: $text-secondary; }
}

/* ═══ 步骤进度条 ═══ */
.step-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: $space-4 0 0;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
  position: relative;
}
.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-surface;
  border: 1.5px solid $border-default;
  color: $text-muted;
  font-size: $text-xs;
  font-weight: 600;
  transition: all $transition-fast;
}
.step-item.completed .step-dot {
  border-color: $color-success;
  background: rgba($color-success, 0.1);
  color: $color-success;
}
.step-item.active .step-dot {
  border-color: $accent-indigo;
  background: rgba($accent-indigo, 0.1);
  color: $accent-indigo;
}
.step-dot.pulse { position: relative; }
.dot-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $accent-indigo;
  animation: dotPulse 2s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}
.dot-num { font-size: $text-xs; }
.step-label {
  font-size: $text-xs;
  color: $text-muted;
  white-space: nowrap;
  .completed & { color: $text-secondary; }
  .active & { color: $accent-indigo; font-weight: 600; }
}
.step-line {
  flex: 1;
  max-width: 80px;
  height: 2px;
  background: $border-default;
  margin: 0 $space-1;
  margin-bottom: 20px;
  &.active {
    background: $gradient-brand;
  }
}

/* ═══ 薄弱点横幅 ═══ */
.weakness-banner {
  display: flex;
  align-items: center;
  gap: $space-3;
  max-width: 1100px;
  margin: 0 auto $space-6;
  padding: $space-3 $space-4;
  background: rgba($color-danger, 0.05);
  border: 1px solid rgba($color-danger, 0.15);
  border-radius: $radius-md;
  position: relative;
}
.banner-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: $color-danger;
  border-radius: 2px;
}
.banner-icon { 
  color: $color-danger; 
  flex-shrink: 0; 
}
.banner-text { 
  font-size: $text-sm; 
  color: $text-secondary; 
}
.banner-text strong { color: $color-danger; }

/* ═══ 加载 ═══ */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $space-16 $space-5;
  gap: $space-4;
  color: $text-muted;
}
.spinner {
  width: 28px;
  height: 28px;
  border: 2px solid $border-default;
  border-top-color: $accent-indigo;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ═══ 空状态 ═══ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $space-12 $space-5;
  text-align: center;
  max-width: 1100px;
  margin: 0 auto;
}
.empty-illustration {
  margin-bottom: $space-6;
  animation: float 4s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
.empty-title { 
  font-size: $text-xl; 
  font-weight: 700; 
  color: $text-primary; 
  margin: 0 0 $space-2; 
}
.empty-desc { 
  font-size: $text-base; 
  color: $text-muted; 
  margin: 0 0 $space-6; 
}
.empty-cta {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-5;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-sm;
  font-weight: 500;
  font-family: $font-sans;
  cursor: pointer;
  transition: all 0.3s ease;
  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
  svg { width: 16px; height: 16px; }
}
.empty-hot-paths {
  margin-top: $space-8;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-3;
}
.hot-hint {
  font-size: $text-sm;
  color: $text-muted;
}
.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  justify-content: center;
}
.hot-tag {
  padding: $space-2 $space-3;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-full;
  color: $text-secondary;
  font-size: $text-xs;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    border-color: $accent-indigo;
    color: $accent-indigo;
    background: rgba($accent-indigo, 0.05);
  }
}

/* ═══ 筛选 Tab 栏 ═══ */
.filter-bar {
  display: flex;
  gap: $space-2;
  max-width: 1100px;
  margin: 0 auto $space-6;
}
.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-4;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-full;
  color: $text-muted;
  font-size: $text-sm;
  font-weight: 500;
  font-family: $font-sans;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover { 
    border-color: $border-medium; 
    color: $text-secondary; 
  }
  &.active {
    background: rgba($accent-indigo, 0.1);
    border-color: $accent-indigo;
    color: $accent-indigo;
  }
}
.filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 $space-1;
  border-radius: $radius-full;
  font-size: $text-xs;
  font-weight: 600;
  background: $bg-muted;
  color: $text-muted;
  .active & { 
    background: rgba($accent-indigo, 0.15); 
    color: $accent-indigo; 
  }
  &.amber { 
    background: rgba($color-warning, 0.1); 
    color: $color-warning; 
  }
  &.slate { 
    background: rgba($bg-muted, 0.8); 
    color: $text-muted; 
  }
  &.green { 
    background: rgba($color-success, 0.1); 
    color: $color-success; 
  }
}

/* ═══ 路径卡片 ═══ */
.paths-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-6;
  max-width: 1100px;
  margin: 0 auto;
}

.path-card {
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  overflow: hidden;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    border-color: $border-medium;
    transform: translateY(-2px);
  }
  &.is-active {
    border-color: rgba($accent-indigo, 0.3);
  }
}

/* 头部 */
.card-header {
  padding: $space-5 $space-6 0;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  margin-bottom: $space-3;
}
.tag {
  display: inline-block;
  padding: 2px $space-2;
  border-radius: $radius-sm;
  font-size: $text-xs;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.tag-level {
  &.beginner { 
    color: $color-success; 
    background: rgba($color-success, 0.1); 
    border: 1px solid rgba($color-success, 0.2); 
  }
  &.intermediate { 
    color: $accent-indigo; 
    background: rgba($accent-indigo, 0.1); 
    border: 1px solid rgba($accent-indigo, 0.2); 
  }
  &.advanced { 
    color: $accent-violet; 
    background: rgba($accent-violet, 0.1); 
    border: 1px solid rgba($accent-violet, 0.2); 
  }
}
.tag-status {
  color: $text-muted;
  background: $bg-muted;
  border: 1px solid $border-default;
  &.in-progress { 
    color: $color-warning; 
    background: rgba($color-warning, 0.1); 
    border-color: rgba($color-warning, 0.2); 
  }
  &.completed { 
    color: $color-success; 
    background: rgba($color-success, 0.1); 
    border-color: rgba($color-success, 0.2); 
  }
  &.new-generated { 
    color: $accent-indigo; 
    background: rgba($accent-indigo, 0.1); 
    border-color: rgba($accent-indigo, 0.2); 
  }
}
.tag-source {
  font-size: $text-xs;
  &.ai-chat { 
    color: $accent-cyan; 
    background: rgba($accent-cyan, 0.1); 
    border: 1px solid rgba($accent-cyan, 0.2); 
  }
  &.ai-assessment { 
    color: $color-warning; 
    background: rgba($color-warning, 0.1); 
    border: 1px solid rgba($color-warning, 0.2); 
  }
}
.tag-active {
  color: $color-warning;
  background: rgba($color-warning, 0.1);
  border: 1px solid rgba($color-warning, 0.2);
}
.tag-weakness {
  color: $color-danger;
  background: rgba($color-danger, 0.1);
  border: 1px solid rgba($color-danger, 0.15);
}

.card-title {
  font-size: $text-xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-2;
  letter-spacing: -0.01em;
  line-height: $leading-tight;
}
.card-meta-row {
  font-size: $text-sm;
  color: $text-muted;
  line-height: $leading-normal;
  display: -webkit-box;
  line-clamp: 1;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 中部 */
.card-body {
  padding: $space-4 $space-6;
}
.chapter-nodes {
  display: flex;
  align-items: center;
  gap: $space-1;
  flex-wrap: wrap;
  margin-bottom: $space-3;
}
.node-chip {
  display: inline-flex;
  align-items: center;
  gap: $space-1;
  padding: $space-1 $space-2;
  background: $bg-muted;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  font-size: $text-xs;
  color: $text-secondary;
  transition: all $transition-fast;
  &:hover { background: rgba($bg-muted, 0.8); }
  &.completed {
    color: $color-success;
    border-color: rgba($color-success, 0.2);
    .node-dot { background: $color-success; }
  }
  &.current {
    color: $accent-indigo;
    border-color: rgba($accent-indigo, 0.3);
    background: rgba($accent-indigo, 0.05);
    .node-dot { background: $accent-indigo; }
  }
}
.node-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: $text-muted;
}
.node-arrow {
  color: $border-default;
  flex-shrink: 0;
}
.node-more {
  font-size: $text-xs;
  color: $text-muted;
  padding: 0 $space-1;
}

.key-nodes-preview {
  margin-top: $space-3;
  padding: $space-3;
  background: $bg-muted;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  opacity: 0;
  transform: translateY(-4px);
  transition: all $transition-fast;
  pointer-events: none;
}
.card:hover .key-nodes-preview {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}
.key-nodes-header {
  display: flex;
  align-items: center;
  gap: $space-1;
  font-size: $text-xs;
  color: $text-muted;
  margin-bottom: $space-2;
  svg { opacity: 0.5; }
}
.key-nodes-list {
  display: flex;
  flex-direction: column;
  gap: $space-1;
}
.key-node-item {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: $text-xs;
  color: $text-secondary;
}
.key-node-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: $accent-indigo;
  flex-shrink: 0;
}
.key-node-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.key-node-hours {
  color: $text-muted;
  font-size: $text-xs;
  font-family: $font-mono;
}

.next-node {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: $text-sm;
}
.next-icon { 
  color: $accent-indigo; 
  font-weight: 700; 
  font-size: $text-base; 
}
.next-label { color: $text-muted; }
.next-value { 
  color: $text-primary; 
  font-weight: 600; 
}
.next-reason {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px $space-2;
  border-radius: $radius-xs;
  font-size: $text-xs;
  font-weight: 600;
  margin-left: $space-1;
  &.hot { 
    color: $color-warning; 
    background: rgba($color-warning, 0.1); 
  }
  &.practice { 
    color: $accent-indigo; 
    background: rgba($accent-indigo, 0.1); 
  }
  &.core { 
    color: $color-info; 
    background: rgba($color-info, 0.1); 
  }
}

/* 底部 */
.card-footer {
  padding: 0 $space-6 $space-5;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  margin-bottom: $space-4;
  padding: $space-3 0;
  border-top: 1px solid $border-default;
  border-bottom: 1px solid $border-default;
}
.data-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.data-value {
  font-size: $text-base;
  font-weight: 600;
  color: $text-primary;
  small { 
    font-size: 0.7em; 
    color: $text-muted; 
    font-weight: 400; 
    margin-left: 1px; 
  }
}
.data-label {
  font-size: $text-xs;
  color: $text-muted;
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-2;
}
.progress-track {
  flex: 1;
  height: 5px;
  background: $bg-muted;
  border-radius: $radius-full;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: $radius-full;
  background: $gradient-brand;
  transition: width 0.6s ease;
}

.progress-value {
  font-size: $text-xs;
  font-weight: 600;
  color: $accent-indigo;
  min-width: 32px;
  text-align: right;
}

.ai-predict {
  display: inline-flex;
  align-items: center;
  gap: $space-1;
  padding: $space-1 $space-2;
  border-radius: $radius-sm;
  font-size: $text-xs;
  color: $accent-indigo;
  background: rgba($accent-indigo, 0.05);
  margin-bottom: $space-3;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.action-left { 
  display: flex;
  gap: $space-1;
  opacity: 0;
  transition: opacity $transition-fast;
}
.card:hover .action-left { opacity: 1; }

.btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: $radius-sm;
  color: $text-muted;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover { 
    color: $accent-indigo; 
    background: rgba($accent-indigo, 0.1); 
  }
  &.danger {
    &:hover { 
      color: $color-danger; 
      background: rgba($color-danger, 0.05); 
      border-color: rgba($color-danger, 0.1); 
    }
  }
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: $space-1;
  padding: $space-2 $space-4;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-sm;
  font-weight: 500;
  font-family: $font-sans;
  cursor: pointer;
  transition: all 0.3s ease;
  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
  &.breathing { 
    animation: btnBreathe 3s ease-in-out infinite; 
  }
}
@keyframes btnBreathe {
  0%, 100% { box-shadow: 0 0 0 0 rgba($accent-indigo, 0.1); }
  50% { box-shadow: 0 0 0 4px rgba($accent-indigo, 0.08); }
}

/* ═══ 地图视图 ═══ */
.map-view { 
  max-width: 1100px; 
  margin: 0 auto; 
}
.map-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: $space-4;
}
.map-node {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-4;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    border-color: $border-medium;
    transform: translateY(-2px);
  }
  &.active { 
    border-color: $accent-indigo; 
  }
  &.in-progress .map-node-dot { 
    border-color: $accent-indigo; 
    color: $accent-indigo; 
  }
  &.completed .map-node-dot { 
    border-color: $color-success; 
    color: $color-success; 
  }
}
.map-node-dot {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-muted;
  border: 1.5px solid $border-default;
  color: $text-muted;
  font-size: $text-xs;
  font-weight: 600;
  transition: all $transition-fast;
}
.map-dot-num { font-size: $text-xs; }
.map-node-info { 
  display: flex; 
  flex-direction: column; 
  gap: 2px; 
  min-width: 0; 
}
.map-node-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.map-node-progress { 
  font-size: $text-xs; 
  color: $accent-indigo; 
  font-family: $font-mono; 
}

/* ═══ 创建学习路径模态框 ═══ */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-overlay;
  padding: $space-5;
}
.create-modal {
  width: 520px;
  max-width: 92vw;
  max-height: 85vh;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-xl;
  animation: modalEnter 0.3s ease;
  display: flex;
  flex-direction: column;
}
.create-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-5 $space-6;
  border-bottom: 1px solid $border-default;
  h3 { 
    font-size: $text-lg; 
    font-weight: 700; 
    color: $text-primary; 
    margin: 0; 
  }
}
.modal-close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  color: $text-muted;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover { 
    background: $bg-muted; 
    color: $text-primary; 
  }
}
.create-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: $space-5 $space-6;
}
.create-tabs {
  display: flex;
  gap: $space-2;
  margin-bottom: $space-5;
}
.create-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-1;
  padding: $space-3 $space-2;
  background: $bg-muted;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-muted;
  cursor: pointer;
  transition: all $transition-fast;
  .tab-icon { font-size: $text-xl; }
  .tab-text { font-size: $text-xs; font-weight: 500; }
  &:hover { 
    border-color: $border-medium; 
    color: $text-secondary; 
  }
  &.active {
    background: rgba($accent-indigo, 0.1);
    border-color: $accent-indigo;
    color: $accent-indigo;
  }
}
.create-form {
  display: flex;
  flex-direction: column;
  gap: $space-4;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: $space-2;
  label { 
    font-size: $text-sm; 
    font-weight: 600; 
    color: $text-secondary; 
  }
}
.form-input, .form-select {
  padding: $space-3 $space-4;
  background: $bg-muted;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: $text-base;
  font-family: $font-sans;
  outline: none;
  transition: border-color $transition-fast;
  &:focus { border-color: $accent-indigo; }
  &::placeholder { color: $text-placeholder; }
}
.form-select {
  cursor: pointer;
  option { background: $bg-elevated; }
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $space-3;
}
.hot-paths {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}
.hot-path-card {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: $bg-muted;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    border-color: $accent-indigo;
    background: rgba($accent-indigo, 0.05);
  }
  &.selected {
    border-color: $accent-indigo;
    background: rgba($accent-indigo, 0.1);
  }
}
.hot-icon { font-size: $text-2xl; }
.hot-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.hot-title { 
  font-size: $text-base; 
  font-weight: 600; 
  color: $text-primary; 
}
.hot-desc { 
  font-size: $text-xs; 
  color: $text-muted; 
}
.hot-duration {
  font-size: $text-xs;
  color: $accent-indigo;
  font-family: $font-mono;
}
.import-section {
  min-height: 120px;
}
.no-chats {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $space-10 $space-5;
  color: $text-muted;
  p { 
    margin: 0 0 $space-2; 
    font-size: $text-base; 
  }
  span { font-size: $text-sm; }
}
.chat-list {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}
.chat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  background: $bg-muted;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover {
    border-color: $accent-indigo;
    background: rgba($accent-indigo, 0.05);
  }
}
.chat-preview {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.chat-title { 
  font-size: $text-sm; 
  font-weight: 500; 
  color: $text-primary; 
}
.chat-time { 
  font-size: $text-xs; 
  color: $text-muted; 
}
.chat-count { 
  font-size: $text-xs; 
  color: $text-muted; 
  white-space: nowrap; 
}
.create-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $space-3;
  padding: $space-4 $space-6;
  border-top: 1px solid $border-default;
}
.btn-cancel {
  padding: $space-2 $space-4;
  background: transparent;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: $text-sm;
  font-family: $font-sans;
  cursor: pointer;
  transition: all $transition-fast;
  &:hover { 
    border-color: $border-medium; 
    color: $text-primary; 
  }
}
.btn-generate {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-5;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-sm;
  font-weight: 500;
  font-family: $font-sans;
  cursor: pointer;
  transition: all 0.3s ease;
  &:hover:not(:disabled) {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
  &:disabled { 
    opacity: 0.5; 
    cursor: not-allowed; 
  }
}
.creating-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.fade-enter-active { transition: opacity 0.2s; }
.fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ═══ 响应式 ═══ */
@media (max-width: 900px) {
  .paths-grid { grid-template-columns: 1fr; }
  .step-bar { gap: 0; }
  .step-line { max-width: 40px; }
}
@media (max-width: 768px) {
  .path-list-page { padding: $space-4 $space-4 $space-12; }
  .page-title { font-size: $text-xl; }
  .card-header, .card-body, .card-footer { 
    padding-left: $space-4; 
    padding-right: $space-4; 
  }
  .card-title { font-size: $text-lg; }
  .data-grid { 
    grid-template-columns: repeat(2, 1fr); 
    gap: $space-3; 
  }
  .filter-bar { 
    padding-bottom: $space-6; 
    overflow-x: auto; 
    flex-wrap: nowrap; 
  }
}
</style>