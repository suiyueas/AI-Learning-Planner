<template>
  <div ref="pageRef" class="modules-page" @mousemove="handleMouseMove" @mouseleave="handleMouseLeave" @mouseenter="handleMouseEnter">
    <!-- 背景层 -->
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
      <div class="mouse-glow" :class="{ visible: isMouseInside }" :style="glowStyle"></div>
      <div class="floating-glow fg-cyan"></div>
      <div class="floating-glow fg-purple"></div>
      <div class="bg-particles">
        <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></div>
      </div>
    </div>

    <!-- 页面头部 -->
    <header class="page-header">
      <h1 class="page-title">📦 全部功能</h1>
      <p class="page-subtitle">覆盖学习全生命周期的完整功能矩阵</p>
    </header>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-for="(stat, i) in statCards" :key="stat.key" class="stat-card" :style="{ animationDelay: i * 0.1 + 's' }">
        <span class="stat-icon" :class="stat.color" v-html="stat.icon"></span>
        <div class="stat-info">
          <span class="stat-num">{{ displayStats[stat.key] }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="search-filter-area">
      <div class="search-wrapper">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
        <input v-model="searchQuery" type="text" class="search-input" placeholder="搜索功能名称或描述..." />
        <span v-if="searchQuery" class="search-clear" @click="searchQuery = ''">✕</span>
      </div>
      <div class="filter-group">
        <button v-for="cat in categories" :key="cat.id" class="filter-btn" :class="{ active: activeCategory === cat.id }" @click="activeCategory = cat.id">
          <span class="filter-color" :style="{ background: cat.color }"></span>
          <span>{{ cat.name }}</span>
          <span class="filter-count">{{ cat.count }}</span>
        </button>
      </div>
    </div>

    <!-- 功能分组展示 -->
    <div class="modules-content">
      <div v-for="(group, gIdx) in filteredGroups" :key="group.id" class="module-group" :style="{ animationDelay: gIdx * 0.1 + 's' }">
        <div class="group-header">
          <span class="group-icon" :style="{ color: group.color }" v-html="group.iconSvg"></span>
          <span class="group-title">{{ group.name }}</span>
          <span class="group-badge">{{ group.modules.length }} 个功能</span>
        </div>
        <div class="module-grid">
          <div v-for="(mod, mIdx) in group.modules" :key="mod.id" class="module-card" :style="{ animationDelay: (gIdx * 0.1 + mIdx * 0.05) + 's' }" @click="handleModuleClick(mod)">
            <span v-if="mod.badge" class="mod-badge" :class="mod.badge">{{ getBadgeText(mod.badge) }}</span>
            <div class="mod-icon-wrap" :style="{ background: group.color + '18', borderColor: group.color + '35' }" v-html="mod.icon"></div>
            <div class="mod-info">
              <span class="mod-name">{{ mod.name }}</span>
              <span class="mod-desc">{{ mod.desc }}</span>
            </div>
            <div class="mod-bottom">
              <div class="mod-tag" :style="{ color: group.color }">{{ group.singleName }}</div>
              <div class="mod-arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 19" /></svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredGroups.length === 0" class="empty-state">
        <div class="empty-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" width="48" height="48"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
        </div>
        <p class="empty-title">未找到匹配的功能</p>
        <p class="empty-desc">请修改搜索关键词或切换分类</p>
      </div>
    </div>

    <!-- 底部统计 -->
    <div class="footer-stats">
      <span class="fs-item">📊 共 <strong>{{ totalModules }}</strong> 个功能</span>
      <span class="fs-divider"></span>
      <span class="fs-item">覆盖 <strong>{{ moduleGroups.length }}</strong> 个学习阶段</span>
    </div>

    <!-- 功能详情弹窗（差异化内容） -->
    <div v-if="showDetail" class="detail-overlay" @click="showDetail = false">
      <div class="detail-dialog" @click.stop>
        <div class="detail-header">
          <div class="detail-icon-wrap" v-html="selectedModule?.icon"></div>
          <div class="detail-info">
            <h3>{{ selectedModule?.name }}</h3>
            <p>{{ selectedModule?.desc }}</p>
          </div>
          <span v-if="selectedModule?.version" class="detail-version">{{ selectedModule.version }}</span>
          <button class="detail-close" @click="showDetail = false">✕</button>
        </div>

        <div class="detail-body">
          <!-- Meta 信息行 -->
          <div class="detail-meta">
            <span class="detail-tag" :style="{ color: getGroupColor(selectedModule?.groupId) }">{{ getGroupName(selectedModule?.groupId) }}</span>
            <span class="detail-tag status-tag" :class="'status-' + (selectedModule?.status || 'active')">
              {{ selectedModule?.statusText || '正常运行' }}
            </span>
            <span class="detail-id">#{{ selectedModule?.id }}</span>
          </div>

          <!-- 详细描述 -->
          <p class="detail-desc-text">{{ selectedModule?.detailDescription }}</p>

          <!-- 模块专属信息区域 -->
          <div class="detail-specific">
            <div v-for="(field, i) in currentModuleContent.fields" :key="i" class="specific-row">
              <span class="specific-label">{{ field.label }}</span>
              <span class="specific-value" v-html="field.value"></span>
            </div>
          </div>

          <!-- 功能清单 -->
          <div v-if="getModuleCapabilities(selectedModule?.name)?.length" class="detail-features">
            <div class="features-title">📋 功能清单</div>
            <div class="features-grid">
              <span v-for="f in getModuleCapabilities(selectedModule?.name)" :key="f.name" class="feature-item" :class="'feature-' + f.status">
                <svg v-if="f.status === 'implemented'" class="feature-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span v-else class="feature-status-icon">{{ f.icon }}</span>
                {{ f.name }}
              </span>
            </div>
          </div>
        </div>

        <div class="detail-footer">
          <button class="btn-secondary" @click="showDetail = false">关闭</button>
          <button class="btn-primary" @click="handleEnterModule">进入功能</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watchEffect, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useModuleStore } from '@/stores/moduleStore'
import { getActivePath } from '@/api/learningPath'

const router = useRouter()
const moduleStore = useModuleStore()
const pageRef = ref(null)

// ============= 搜索与筛选 =============
const searchQuery = ref('')
const activeCategory = ref('all')
const showDetail = ref(false)
const selectedModule = ref(null)

// ============= SVG 图标 =============
const icons = {
  user: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>',
  chart: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="10" width="4" height="10" rx="1"/><rect x="10" y="6" width="4" height="14" rx="1"/><rect x="17" y="2" width="4" height="18" rx="1"/></svg>',
  target: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1" fill="currentColor"/></svg>',
  path: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>',
  book: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>',
  chat: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>',
  refresh: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg>',
  doc: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>',
  report: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
  network: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="4" r="2.5"/><circle cx="5" cy="18" r="2.5"/><circle cx="19" cy="18" r="2.5"/><line x1="12" y1="6.5" x2="7" y2="15.5"/><line x1="12" y1="6.5" x2="17" y2="15.5"/><line x1="7" y1="15.5" x2="17" y2="15.5"/></svg>',
  bell: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>',
  badge: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="6"/><path d="M15.5 12.5L18 20l-6-3-6 3 2.5-7.5"/></svg>',
  layers: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>',
  diamond: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>',
  compass: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/><polyline points="12 2 15 9 22 12 15 15 12 22 9 15 2 12 9 9"/></svg>',
  note: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>',
  calendar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
  code: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>'
}

// ============= 统计卡片 =============
const statCards = [
  { key: 'total', icon: icons.layers, label: '总功能', color: 'cyan' },
  { key: 'basic', icon: icons.diamond, label: '学习基础', color: 'cyan' },
  { key: 'assist', icon: icons.chat, label: '学习辅助', color: 'blue' },
  { key: 'advanced', icon: icons.target, label: '进阶 & 激励', color: 'purple' }
]

const displayStats = reactive({ total: 0, basic: 0, assist: 0, advanced: 0 })

const animateCountUp = (key, target) => {
  const dur = 800
  const t0 = performance.now()
  const start = displayStats[key]
  const step = (t) => {
    const p = Math.min((t - t0) / dur, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    displayStats[key] = Math.round(start + (target - start) * ease)
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

// ============= 功能-能力映射表 =============
// status: 'implemented' | 'in-progress' | 'planned'
const functionCapabilities = {
  '能力测评': {
    implemented: ['水平测试', '薄弱点分析', '能力雷达图', '学习建议'],
    inProgress: [],
    planned: []
  },
  '目标设定': {
    implemented: ['目标创建', '进度追踪', '目标提醒', '目标调整'],
    inProgress: [],
    planned: []
  },
  '学习路径': {
    implemented: ['路径生成', '节点拆分', '进度追踪', '路径调整'],
    inProgress: [],
    planned: []
  },
  '学习日历': {
    implemented: ['日历视图', '任务管理', '进度追踪', '月度统计'],
    inProgress: [],
    planned: []
  },
  '进度追踪': {
    implemented: ['学习统计', '时长记录', '完成率', '趋势分析'],
    inProgress: [],
    planned: []
  },
  '智能答疑': {
    implemented: ['实时问答', '概念讲解', '历史记录', '代码解析'],
    inProgress: [],
    planned: []
  },
  '学习笔记': {
    implemented: ['笔记创建', '标签分类', '全文搜索', '笔记导出'],
    inProgress: [],
    planned: []
  },
  '习题生成': {
    implemented: ['智能出题', '自动批改', '错题本', '难度自适应'],
    inProgress: [],
    planned: []
  },
  '学情报告': {
    implemented: ['报告生成', '数据分析', '对比分析'],
    inProgress: ['PDF 导出'],
    planned: []
  },
  '成就打卡': {
    implemented: ['成就系统', '每日打卡', '排行榜', '成就分享'],
    inProgress: [],
    planned: []
  },
  '代码解析': {
    implemented: ['问题检测', '安全审计', '复杂度分析', '优化建议'],
    inProgress: [],
    planned: []
  }
}

const getModuleCapabilities = (moduleName) => {
  if (!moduleName) return []
  const caps = functionCapabilities[moduleName]
  if (!caps) return []

  const result = []
  caps.implemented.forEach(name => {
    result.push({ name, status: 'implemented', icon: null })
  })
  caps.inProgress.forEach(name => {
    result.push({ name, status: 'in-progress', icon: '🚧' })
  })
  caps.planned.forEach(name => {
    result.push({ name, status: 'planned', icon: '📋' })
  })
  return result
}

// ============= 差异化功能数据（4类10功能：移除冗余个人中心） =============
const defaultModuleGroups = [
  // ===== 学习基础（4个） =====
  {
    id: 'basic', name: '学习基础', singleName: '基础', color: '#00f5d4',
    iconSvg: icons.diamond,
    modules: [
      {
        id: 'M2', icon: icons.chart, name: '能力测评', desc: '多维度评估学习水平', groupId: 'basic', badge: 'hot',
        detailDescription: '通过多维度的能力测评，评估你在各学科的知识掌握程度，精准定位薄弱知识点，为后续学习提供数据支撑。',
        status: 'active', statusText: '正常运行', version: 'v1.3',
        features: ['水平测试', '薄弱点分析', '能力雷达图', '学习建议'],
        route: '/assessment',
        stats: { lastTest: '2026-07-10', level: '中级' },
        quickActions: [
          { label: '开始新测评', action: 'navigate', route: '/assessment', primary: true }
        ]
      },
      {
        id: 'M3', icon: icons.target, name: '目标设定', desc: '设定清晰的学习目标', groupId: 'basic', badge: 'core',
        detailDescription: '设定你的学习目标，系统会根据目标智能推荐学习路径和资源。合理的目标准则有助于保持学习动力。',
        status: 'active', statusText: '正常运行', version: 'v1.0',
        features: ['目标创建', '进度追踪', '目标提醒', '目标调整'],
        route: '/goal-setting',
        stats: { currentGoal: '', progress: 0 },
        quickActions: [
          { label: '设定新目标', action: 'navigate', route: '/goal-setting', primary: true }
        ]
      },
      {
        id: 'M4', icon: icons.path, name: '学习路径', desc: '智能规划个性化路径', groupId: 'basic', badge: 'core',
        detailDescription: '根据你的目标和当前水平，智能生成个性化的学习路径。每一步都有明确的学习内容和预期成果。',
        status: 'active', statusText: '正常运行', version: 'v2.0',
        features: ['路径生成', '节点拆分', '进度追踪', '路径调整'],
        route: '/learning-path',
        stats: { currentPath: '', progress: 0, nextNode: '' },
        quickActions: [
          { label: '查看路径', action: 'navigate', route: '/learning-path', primary: true }
        ]
      },
      {
        id: 'M9', icon: icons.calendar, name: '学习日历', desc: '日历视图规划学习任务', groupId: 'basic', badge: 'new',
        detailDescription: '以日历视图规划和管理每日学习任务，直观查看学习安排，帮助你更好地管理时间和执行计划。',
        status: 'active', statusText: '正常运行', version: 'v1.0',
        features: ['日历视图', '任务管理', '进度追踪', '月度统计'],
        route: '/calendar',
        stats: { todayTasks: 3, monthlyCompletion: 75 },
        quickActions: [
          { label: '查看日历', action: 'navigate', route: '/calendar', primary: true }
        ]
      }
    ]
  },
  // ===== 学习辅助（3个） =====
  {
    id: 'assist', name: '学习辅助', singleName: '辅助', color: '#3a86ff',
    iconSvg: icons.layers,
    modules: [
      {
        id: 'M7', icon: icons.chart, name: '进度追踪', desc: '实时追踪学习进度', groupId: 'assist', badge: 'popular',
        detailDescription: '全面追踪你的学习进度，包括学习时长、完成率、掌握程度等关键指标，助你时刻把握学习节奏。',
        status: 'active', statusText: '正常运行', version: 'v1.2',
        features: ['学习统计', '时长记录', '完成率', '趋势分析'],
        route: '/capability/progress',
        stats: { progress: 72, weeklyHours: 12.5, streak: 5 },
        quickActions: [
          { label: '查看看板', action: 'navigate', route: '/capability/progress', primary: true }
        ]
      },
      {
        id: 'M6', icon: icons.chat, name: '智能答疑', desc: 'AI 智能问答', groupId: 'assist', badge: 'hot',
        detailDescription: '随时向 AI 助手提问，获取即时、准确的解答。支持编程问题、概念理解、作业辅导等多种场景。',
        status: 'active', statusText: '正常运行', version: 'v3.0',
        features: ['实时问答', '代码解析', '概念讲解', '历史记录'],
        route: '/chat',
        stats: { todayQuestions: 3, recentQuestion: '什么是闭包？' },
        quickActions: [
          { label: '开始提问', action: 'navigate', route: '/chat', primary: true }
        ]
      },
      {
        id: 'M12', icon: icons.note, name: '学习笔记', desc: '记录与整理学习笔记', groupId: 'assist', badge: 'new',
        detailDescription: '记录学习过程中的笔记和心得，支持富文本编辑、标签分类和全文检索，构建个人知识库。',
        status: 'active', statusText: '正常运行', version: 'v1.0',
        features: ['笔记创建', '标签分类', '全文搜索', '笔记导出'],
        route: '/study-notes',
        stats: { totalNotes: 6, recentNote: '数据分析学习笔记' },
        quickActions: [
          { label: '新建笔记', action: 'navigate', route: '/study-notes', primary: true }
        ]
      }
    ]
  },
  // ===== 学习进阶（3个） =====
  {
    id: 'advanced', name: '学习进阶', singleName: '进阶', color: '#7b61ff',
    iconSvg: icons.target,
    modules: [
      {
        id: 'M10', icon: icons.doc, name: '习题生成', desc: '自动生成练习题', groupId: 'advanced', badge: 'popular',
        detailDescription: '根据学习内容和掌握程度，自动生成针对性的练习题，巩固知识，检验学习成果。',
        status: 'active', statusText: '正常运行', version: 'v1.1',
        features: ['智能出题', '自动批改', '错题本', '难度自适应'],
        route: '/exercise',
        stats: { totalExercises: 128, accuracy: 78, pending: 12 },
        quickActions: [
          { label: '生成习题', action: 'navigate', route: '/exercise', primary: true }
        ]
      },
      {
        id: 'M11', icon: icons.report, name: '学情报告', desc: '深度分析学习数据', groupId: 'advanced', badge: 'popular',
        detailDescription: '自动生成深度学情分析报告，涵盖学习行为、知识掌握、能力变化等多维度分析，为下一步学习提供参考。',
        status: 'active', statusText: '正常运行', version: 'v2.0',
        features: ['报告生成', '数据分析', '对比分析', 'PDF 导出'],
        route: '/report',
        stats: { lastReport: '2026-07-12', status: '已生成', nextDue: '2 天后' },
        quickActions: [
          { label: '生成报告', action: 'navigate', route: '/report', primary: true }
        ]
      },
      {
        id: 'M14', icon: icons.badge, name: '成就打卡', desc: '记录成就与打卡签到', groupId: 'advanced', badge: 'popular',
        detailDescription: '记录你的学习成就和打卡记录，通过成就系统和连续打卡激励你保持学习动力。',
        status: 'active', statusText: '正常运行', version: 'v1.0',
        features: ['成就系统', '每日打卡', '排行榜', '成就分享'],
        route: '/achievements',
        stats: { achievements: 24, streak: 15, weeklyCheckins: '7/7' },
        quickActions: [
          { label: '查看成就', action: 'navigate', route: '/achievements', primary: true }
        ]
      },
      {
        id: 'M15', icon: icons.code, name: '代码解析', desc: 'AI 分析代码质量', groupId: 'advanced', badge: 'new',
        detailDescription: '粘贴代码即可获得 AI 代码审计：检测性能、安全、可维护性问题，给出优化建议与优化后的代码。',
        status: 'active', statusText: '正常运行', version: 'v1.0',
        features: ['问题检测', '安全审计', '复杂度分析', '优化建议'],
        route: '/code-analyze',
        stats: { languages: 6, lastAnalysis: 'Python / Java / JS...' },
        quickActions: [
          { label: '开始分析', action: 'navigate', route: '/code-analyze', primary: true }
        ]
      }
    ]
  }
]

// ============= 模块专属展示内容 =============
// M3（目标设定）/ M4（学习路径）已在 currentModuleContent 中对接真实数据，此处仅保留其余静态模块
const moduleSpecificContent = {
  M2: {
    fields: [
      { label: '上次测评', value: '2026-07-10' },
      { label: '测评等级', value: '🎯 中级' },
      { label: '薄弱知识点', value: '函数、面向对象' }
    ]
  },
  M6: {
    fields: [
      { label: '今日提问', value: '3 个问题' },
      { label: '最近问题', value: '什么是闭包？' },
      { label: '推荐问题', value: 'Python 装饰器原理' }
    ]
  },
  M7: {
    fields: [
      { label: '学习进度', value: '72%' },
      { label: '本周学习时长', value: '12.5 小时' },
      { label: '连续学习', value: '5 天' }
    ]
  },
  M9: {
    fields: [
      { label: '今日任务', value: '3 项' },
      { label: '月度完成率', value: '75%' },
      { label: '本月学习天数', value: '15 天' }
    ]
  },
  M10: {
    fields: [
      { label: '最近报告', value: '2026-07-12' },
      { label: '报告状态', value: '已生成' },
      { label: '待生成报告', value: '周报（2 天后）' }
    ]
  },
  M11: {
    fields: [
      { label: '最近报告', value: '2026-07-12' },
      { label: '报告状态', value: '已生成' },
      { label: '待生成报告', value: '周报（2 天后）' }
    ]
  },
  M12: {
    fields: [
      { label: '笔记数量', value: '6 篇' },
      { label: '最近笔记', value: '数据分析学习笔记' },
      { label: '标签数量', value: '5 个' }
    ]
  },
  M14: {
    fields: [
      { label: '已解锁成就', value: '24 个' },
      { label: '连续打卡', value: '15 天' },
      { label: '本周打卡', value: '7/7 天' }
    ]
  }
}

// ============= 计算属性 =============
const moduleGroups = computed(() => {
  if (moduleStore.moduleGroups.length > 0) {
    // 合并 API 数据与本地增强数据，确保差异化字段不被 API 原始数据覆盖
    return moduleStore.moduleGroups.map(g => ({
      ...g,
      modules: (g.modules || []).map(m => {
        const enriched = allModulesMap[m.id] || {}
        return { ...m, ...enriched, groupId: g.id }
      })
    }))
  }
  return defaultModuleGroups
})

// 所有模块的 id 索引映射，用于 API 数据回流时补充增强字段
const allModulesMap = {}
defaultModuleGroups.forEach(g => {
  g.modules.forEach(m => { allModulesMap[m.id] = m })
})

const totalModules = computed(() => moduleGroups.value.reduce((s, g) => s + g.modules.length, 0))

const categories = computed(() => {
  const cats = [
    { id: 'all', name: '全部', count: totalModules.value, color: '#00f5d4' },
    ...moduleGroups.value.map(g => ({
      id: g.id, name: g.name, count: g.modules.length, color: g.color
    }))
  ]
  return cats
})

const filteredGroups = computed(() => {
  let groups = moduleGroups.value
  if (activeCategory.value !== 'all') {
    groups = groups.filter(g => g.id === activeCategory.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    groups = groups.map(g => ({
      ...g,
      modules: g.modules.filter(m => m.name.toLowerCase().includes(q) || m.desc.toLowerCase().includes(q))
    })).filter(g => g.modules.length > 0)
  }
  return groups
})

// ============= 真实数据：活跃学习路径（M3 目标设定 / M4 学习路径 卡片共用数据源） =============
// 与 Home.vue 首页卡片同一接口口径，保证全局数据一致，避免静态示例数据造成认知偏差
const activePathData = ref(null)

const fetchActivePath = async () => {
  try {
    const res = await getActivePath()
    const data = res?.data ?? res
    activePathData.value = data
    // 同步更新卡片 stats（数据一致性，避免遗留静态占位值）
    if (data?.hasPath && data.path) {
      const progress = data.progress?.percentage || 0
      if (allModulesMap.M3) allModulesMap.M3.stats = { currentGoal: stripFieldPrefix(data.path.name), progress }
      if (allModulesMap.M4) allModulesMap.M4.stats = { currentPath: stripFieldPrefix(data.path.name), progress, nextNode: data.nextNode?.nodeName || '' }
    }
  } catch (e) {
    console.warn('加载活跃学习路径失败:', e.message)
  }
}

// 去掉路径名中的【领域】前缀（如 【数据分析】学习python → 学习python）
const stripFieldPrefix = (name) => (name || '').replace(/^【[^】]*】/, '')

// M3 目标设定：真实目标 = 活跃学习路径（目标名 + 完成度 + 下一节点建议）
const buildGoalFields = () => {
  const d = activePathData.value
  if (!d || !d.hasPath || !d.path) {
    return {
      fields: [
        { label: '当前目标', value: '未设置' },
        { label: '目标完成度', value: '0%' },
        { label: '建议目标', value: '点击「新建目标」开始设定' }
      ]
    }
  }
  const progress = d.progress || { percentage: 0 }
  return {
    fields: [
      { label: '当前目标', value: stripFieldPrefix(d.path.name) || '未命名目标' },
      { label: '目标完成度', value: progress.percentage + '%' },
      { label: '建议目标', value: d.nextNode?.nodeName || '已完成所有节点 🎉' }
    ]
  }
}

// M4 学习路径：真实路径 + 进度 + 下一节点
const buildPathFields = () => {
  const d = activePathData.value
  if (!d || !d.hasPath || !d.path) {
    return {
      fields: [
        { label: '当前路径', value: '暂无学习路径' },
        { label: '路径完成度', value: '0%' },
        { label: '下一节点', value: '去生成学习路径' }
      ]
    }
  }
  const progress = d.progress || { percentage: 0 }
  return {
    fields: [
      { label: '当前路径', value: stripFieldPrefix(d.path.name) || '未命名路径' },
      { label: '路径完成度', value: progress.percentage + '%' },
      { label: '下一节点', value: d.nextNode?.nodeName || '已完成所有节点 🎉' }
    ]
  }
}

// ============= 方法 =============
const getBadgeText = (b) => ({ hot: '热门', core: '核心', new: '新增', popular: '推荐' }[b] || b)
const getGroupName = (id) => moduleGroups.value.find(g => g.id === id)?.name || '未知'
const getGroupColor = (id) => moduleGroups.value.find(g => g.id === id)?.color || '#9090b8'

const currentModuleContent = computed(() => {
  const id = selectedModule.value?.id
  if (id === 'M3') return buildGoalFields()
  if (id === 'M4') return buildPathFields()
  return moduleSpecificContent[id] || { fields: [] }
})

const handleModuleClick = (mod) => {
  selectedModule.value = mod
  moduleStore.updateModuleStatus(mod.id, { lastUsed: new Date().toISOString().split('T')[0] })
  showDetail.value = true
}

const handleEnterModule = () => {
  const mod = selectedModule.value
  if (!mod) return

  // 记录最后使用时间
  moduleStore.updateModuleStatus(mod.id, {
    lastUsed: new Date().toISOString().split('T')[0]
  })

  if (mod.route) {
    const exists = router.getRoutes().some(r => r.path === mod.route)
    if (exists) {
      router.push(mod.route)
      showDetail.value = false
      return
    }
  }

  // 无独立页面时弹窗提示
  showDetail.value = false
  ElMessage.info(`「${mod.name}」功能页面开发中，敬请期待！`)
}

// ============= 鼠标光晕 =============
const glowPosition = reactive({ x: -300, y: -300 })
const isMouseInside = ref(false)
let rafId = null
const handleMouseMove = (e) => {
  if (rafId) return
  rafId = requestAnimationFrame(() => {
    const rect = pageRef.value?.getBoundingClientRect()
    if (rect) { glowPosition.x = e.clientX - rect.left; glowPosition.y = e.clientY - rect.top }
    rafId = null
  })
}
const handleMouseLeave = () => { isMouseInside.value = false }
const handleMouseEnter = () => { isMouseInside.value = true }
const glowStyle = computed(() => ({ left: glowPosition.x + 'px', top: glowPosition.y + 'px' }))

// ============= 粒子效果 =============
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

// ============= 生命周期 =============
onMounted(async () => {
  try {
    await Promise.all([
      moduleStore.fetchModuleGroups(),
      fetchActivePath()
    ])
  } catch (e) { console.error('获取模块数据失败:', e) }
})

watchEffect(() => {
  const gs = moduleGroups.value
  const basic = gs.find(g => g.id === 'basic')
  const assist = gs.find(g => g.id === 'assist')
  const adv = gs.filter(g => g.id === 'advanced')
  nextTick(() => {
    animateCountUp('total', totalModules.value)
    animateCountUp('basic', basic?.modules.length || 0)
    animateCountUp('assist', assist?.modules.length || 0)
    animateCountUp('advanced', adv.reduce((s, g) => s + g.modules.length, 0))
  })
})

onUnmounted(() => { if (rafId) { cancelAnimationFrame(rafId); rafId = null } })
</script>

<style lang="scss" scoped>
.modules-page {
  min-height: 100vh;
  background: #0a0a1a;
  position: relative;
  overflow: hidden;
  padding: 32px 24px 60px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== 背景层 ===== */
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
  animation: gridPulse 8s ease-in-out infinite alternate;
  transform-origin: center center;
  will-change: opacity, transform;
}
@keyframes gridPulse {
  0% { opacity: 0.3; transform: scale(1); }
  100% { opacity: 0.6; transform: scale(1.02); }
}
.mouse-glow {
  position: absolute; width: 320px; height: 320px; border-radius: 50%;
  pointer-events: none;
  background: radial-gradient(circle, rgba(0,245,212,0.12) 0%, rgba(123,97,255,0.06) 40%, transparent 70%);
  filter: blur(24px); transform: translate(-50%, -50%); opacity: 0;
  transition: opacity 0.4s ease; will-change: left, top;
}
.mouse-glow.visible { opacity: 1; }
@media (pointer: coarse) { .mouse-glow { display: none; } }
.floating-glow {
  position: absolute; border-radius: 50%; filter: blur(100px); pointer-events: none;
  animation: floatGlow 16s ease-in-out infinite alternate;
}
.fg-cyan { width: 500px; height: 500px; top: -120px; right: -120px; background: radial-gradient(circle, rgba(0,245,212,0.06) 0%, transparent 70%); animation-duration: 18s; }
.fg-purple { width: 420px; height: 420px; bottom: -100px; left: -100px; background: radial-gradient(circle, rgba(123,97,255,0.06) 0%, transparent 70%); animation-duration: 22s; animation-delay: -6s; }
@keyframes floatGlow {
  0% { transform: translate(0,0) scale(1); }
  25% { transform: translate(25px,-18px) scale(1.08); }
  50% { transform: translate(-18px,22px) scale(0.92); }
  75% { transform: translate(15px,12px) scale(1.04); }
  100% { transform: translate(-10px,-8px) scale(1.02); }
}
.bg-particles { position: absolute; inset: 0; }
.particle {
  position: absolute; border-radius: 50%; background: #00f5d4;
  animation: particleFloat linear infinite;
}
@keyframes particleFloat {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.4; }
  90% { opacity: 0.2; }
  100% { transform: translateY(-100vh) translateX(100px); opacity: 0; }
}

/* ===== 页面头部 ===== */
.page-header {
  position: relative; z-index: 1; margin-bottom: 20px; animation: fadeSlideIn 0.5s ease;
  display: flex; flex-direction: column; gap: 8px;
}
.page-title {
  font-size: 2rem; font-weight: 800;
  background: linear-gradient(135deg, #00f5d4 0%, #3a86ff 50%, #7b61ff 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.page-subtitle { font-size: 0.9rem; color: #c0c0e0; }

/* ===== 统计卡片 ===== */
.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px;
  position: relative; z-index: 1;
}
.stat-card {
  display: flex; align-items: center; gap: 12px; padding: 14px 18px;
  background: rgba(17,17,39,0.6); backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.1); border-radius: 12px;
  animation: statEnter 0.5s ease both; transition: all 0.3s ease;
  &:hover { transform: translateY(-2px); border-color: rgba(0,245,212,0.25); box-shadow: 0 4px 20px rgba(0,245,212,0.08); }
}
.stat-icon { width: 28px; height: 28px; flex-shrink: 0; :deep(svg) { width: 100%; height: 100%; } }
.stat-icon.cyan { color: #00f5d4; }
.stat-icon.blue { color: #3a86ff; }
.stat-icon.purple { color: #7b61ff; }
.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-num {
  font-size: 1.4rem; font-weight: 800; font-family: 'JetBrains Mono', monospace;
  background: linear-gradient(135deg, #00f5d4, #3a86ff);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.stat-label { font-size: 0.7rem; color: #9090b8; font-weight: 500; }

/* ===== 搜索与筛选 ===== */
.search-filter-area {
  position: relative; z-index: 1; margin-bottom: 24px; animation: fadeSlideIn 0.5s ease 0.15s both;
}
.search-wrapper { position: relative; margin-bottom: 12px; }
.search-icon {
  position: absolute; left: 16px; top: 50%; transform: translateY(-50%);
  width: 18px; height: 18px; color: #9090b8;
}
.search-input {
  width: 100%; padding: 12px 36px 12px 44px;
  background: rgba(17,17,39,0.6); border: 1px solid rgba(100,100,180,0.1);
  border-radius: 12px; color: #e8e8ff; font-size: 14px; outline: none;
  backdrop-filter: blur(8px); transition: all 0.2s;
  &::placeholder { color: #8080a8; }
  &:focus { border-color: rgba(0,245,212,0.25); box-shadow: 0 0 0 3px rgba(0,245,212,0.04); }
}
.search-clear {
  position: absolute; right: 14px; top: 50%; transform: translateY(-50%);
  cursor: pointer; color: #9090b8; font-size: 16px;
  &:hover { color: #e8e8ff; }
}
.filter-group {
  display: flex; gap: 8px; flex-wrap: wrap;
}
.filter-btn {
  display: flex; align-items: center; gap: 6px; padding: 6px 14px;
  background: rgba(17,17,39,0.6); backdrop-filter: blur(8px);
  border: 1px solid rgba(100,100,180,0.1); border-radius: 20px;
  font-size: 13px; color: #c0c0e0; cursor: pointer; transition: all 0.2s;
  &:hover { border-color: rgba(0,245,212,0.2); color: #e8e8ff; }
  &.active { border-color: #00f5d4; color: #00f5d4; background: rgba(0,245,212,0.08); }
}
.filter-color { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.filter-count {
  font-size: 11px; padding: 0 5px; background: rgba(100,100,180,0.1); border-radius: 8px;
}

/* ===== 功能分组 ===== */
.modules-content { position: relative; z-index: 1; }
.module-group { margin-bottom: 24px; animation: fadeSlideIn 0.5s ease both; }
.group-header {
  display: flex; align-items: center; gap: 10px; margin-bottom: 14px; padding: 0 2px;
}
.group-icon { width: 24px; height: 24px; flex-shrink: 0; :deep(svg) { width: 100%; height: 100%; } }
.group-title { font-size: 15px; font-weight: 700; color: #e8e8ff; }
.group-badge {
  margin-left: auto; font-size: 11px; padding: 2px 10px;
  background: rgba(100,100,180,0.1); border-radius: 10px; color: #9090b8;
}

/* ===== 功能卡片 ===== */
.module-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px;
}
.module-card {
  position: relative; display: flex; align-items: center; gap: 14px;
  padding: 16px 18px;
  background: rgba(17,17,39,0.6); backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.1); border-radius: 14px;
  cursor: pointer; animation: cardEnter 0.4s ease both;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  overflow: hidden;
  &:hover {
    transform: translateY(-4px);
    border-color: rgba(0,245,212,0.3);
    box-shadow: 0 8px 32px rgba(0,245,212,0.08);
    .mod-arrow { opacity: 1; transform: translateX(0); }
  }
}
.mod-badge {
  position: absolute; top: 8px; right: 8px;
  font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 8px;
  &.hot { background: rgba(245,158,11,0.15); color: #f59e0b; border: 1px solid rgba(245,158,11,0.2); }
  &.core { background: rgba(0,245,212,0.15); color: #00f5d4; border: 1px solid rgba(0,245,212,0.2); }
  &.new { background: rgba(255,0,110,0.15); color: #ff006e; border: 1px solid rgba(255,0,110,0.2); }
  &.popular { background: rgba(123,97,255,0.15); color: #7b61ff; border: 1px solid rgba(123,97,255,0.2); }
}
.mod-icon-wrap {
  width: 42px; height: 42px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  :deep(svg) { width: 22px; height: 22px; }
}
.mod-info { flex: 1; min-width: 0; }
.mod-name { font-size: 14px; font-weight: 600; color: #e8e8ff; display: block; }
.mod-desc { font-size: 12px; color: #9090b8; margin-top: 2px; display: block; }
.mod-bottom { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.mod-tag { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 6px; background: rgba(100,100,180,0.08); }
.mod-arrow { color: #9090b8; opacity: 0; transform: translateX(-6px); transition: all 0.2s; display: flex; }

/* ===== 空状态 ===== */
.empty-state {
  display: flex; flex-direction: column; align-items: center; padding: 60px 20px; text-align: center;
}
.empty-icon-wrap { width: 48px; height: 48px; color: #9090b8; margin-bottom: 16px; :deep(svg) { width: 100%; height: 100%; } }
.empty-title { font-size: 15px; font-weight: 600; color: #c0c0e0; margin-bottom: 4px; }
.empty-desc { font-size: 13px; color: #9090b8; }

/* ===== 底部统计 ===== */
.footer-stats {
  position: relative; z-index: 1;
  display: flex; align-items: center; justify-content: center; gap: 16px;
  padding: 16px 0; margin-top: 32px;
  border-top: 1px solid rgba(100,100,180,0.1);
}
.fs-item { font-size: 13px; color: #9090b8; strong { color: #00f5d4; font-weight: 600; } }
.fs-divider { width: 1px; height: 16px; background: rgba(100,100,180,0.1); }

/* ===== 详情弹窗 ===== */
.detail-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 1000; animation: fadeIn 0.2s ease;
}
.detail-dialog {
  width: 420px; max-width: 90vw; max-height: 80vh;
  background: rgba(17,17,39,0.95); border: 1px solid rgba(100,100,180,0.12);
  border-radius: 16px; box-shadow: 0 16px 60px rgba(0,0,0,0.5);
  animation: modalEnter 0.3s ease;
  display: flex; flex-direction: column;
}
.detail-header {
  display: flex; align-items: center; gap: 14px; padding: 20px;
  border-bottom: 1px solid rgba(100,100,180,0.08);
  flex-shrink: 0;
}
.detail-icon-wrap {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  background: rgba(0,245,212,0.1); display: flex; align-items: center; justify-content: center;
  :deep(svg) { width: 24px; height: 24px; color: #00f5d4; }
}
.detail-info { flex: 1; h3 { font-size: 16px; font-weight: 700; color: #e8e8ff; margin-bottom: 4px; } p { font-size: 13px; color: #c0c0e0; } }
.detail-close {
  width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
  background: rgba(100,100,180,0.08); border: none; border-radius: 6px;
  font-size: 15px; cursor: pointer; color: #9090b8; transition: all 0.15s;
  &:hover { background: rgba(100,100,180,0.15); color: #e8e8ff; }
}
.detail-version {
  font-size: 10px; font-weight: 600; padding: 2px 8px;
  border-radius: 6px; background: rgba(0,245,212,0.1);
  color: #00f5d4; border: 1px solid rgba(0,245,212,0.15);
  font-family: 'JetBrains Mono', monospace; flex-shrink: 0;
}
.detail-body { padding: 20px; overflow-y: auto; flex: 1; }

/* Meta 信息行 */
.detail-meta {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 14px; flex-wrap: wrap;
}
.detail-tag {
  font-size: 11px; font-weight: 600; padding: 3px 10px;
  border-radius: 8px; background: rgba(100,100,180,0.08);
  border: 1px solid rgba(100,100,180,0.12);
}
.status-tag {
  &.status-active { background: rgba(16,185,129,0.12); color: #10b981; border-color: rgba(16,185,129,0.2); }
  &.status-beta { background: rgba(245,158,11,0.12); color: #f59e0b; border-color: rgba(245,158,11,0.2); }
  &.status-coming_soon { background: rgba(100,100,180,0.1); color: #9090b8; border-color: rgba(100,100,180,0.15); }
}
.detail-id {
  margin-left: auto; font-size: 11px; color: #8080a8;
  font-family: 'JetBrains Mono', monospace;
}

/* 详细描述 */
.detail-desc-text {
  font-size: 13px; line-height: 1.7;
  color: #c0c0e0; margin-bottom: 16px;
  padding: 12px; border-radius: 8px;
  background: rgba(100,100,180,0.04);
  border: 1px solid rgba(100,100,180,0.06);
}

/* 模块专属信息 */
.detail-specific {
  margin-bottom: 16px;
  border-radius: 8px;
  background: rgba(100,100,180,0.03);
  border: 1px solid rgba(100,100,180,0.06);
  overflow: hidden;
}
.specific-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(100,100,180,0.06);
  &:last-child { border-bottom: none; }
}
.specific-label {
  font-size: 12px; color: #9090b8;
  min-width: 70px; flex-shrink: 0;
}
.specific-value {
  font-size: 13px; color: #e8e8ff; font-weight: 500;
}

/* 功能清单 */
.detail-features { margin-bottom: 4px; }
.features-title {
  font-size: 12px; font-weight: 600; color: #c0c0e0;
  margin-bottom: 10px;
}
.features-grid {
  display: flex; flex-wrap: wrap; gap: 6px;
}
.feature-item {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: #c0c0e0;
  padding: 4px 10px;
  background: rgba(100,100,180,0.06);
  border: 1px solid rgba(100,100,180,0.08);
  border-radius: 6px;
  transition: all 0.15s;
  &:hover {
    background: rgba(0,245,212,0.06);
    border-color: rgba(0,245,212,0.12);
    color: #e8e8ff;
  }
  &.feature-in-progress {
    background: rgba(245,158,11,0.08);
    border-color: rgba(245,158,11,0.15);
    color: #f59e0b;
    &:hover { background: rgba(245,158,11,0.12); border-color: rgba(245,158,11,0.25); }
  }
  &.feature-planned {
    background: rgba(100,100,180,0.04);
    border-color: rgba(100,100,180,0.08);
    color: #9090b8;
    &:hover { background: rgba(100,100,180,0.08); border-color: rgba(100,100,180,0.12); }
  }
}
.feature-check {
  width: 12px; height: 12px; color: #00f5d4; flex-shrink: 0;
}
.feature-status-icon {
  font-size: 10px; flex-shrink: 0;
}
.detail-footer {
  display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px;
  border-top: 1px solid rgba(100,100,180,0.08);
  flex-shrink: 0;
  button { padding: 8px 20px; border-radius: 8px; font-weight: 600; font-size: 13px; cursor: pointer; transition: all 0.2s; border: none; }
  .btn-secondary { background: rgba(100,100,180,0.08); color: #c0c0e0; &:hover { background: rgba(100,100,180,0.15); } }
  .btn-primary {
    background: linear-gradient(135deg, #00f5d4, #3a86ff); color: #fff;
    &:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(0,245,212,0.25); }
  }
}

/* ===== 动画 ===== */
@keyframes fadeSlideIn { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@keyframes statEnter { from { opacity: 0; transform: translateY(12px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes cardEnter { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes modalEnter { from { opacity: 0; transform: scale(0.95) translateY(8px); } to { opacity: 1; transform: scale(1) translateY(0); } }

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .module-grid { grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); }
}
@media (max-width: 1024px) {
  .modules-page { padding: 24px 16px 40px; }
  .page-title { font-size: 1.6rem; }
}
@media (max-width: 640px) {
  .modules-page { padding: 20px 12px 32px; }
  .page-title { font-size: 1.3rem; }
  .stat-card { padding: 10px 14px; }
  .stat-num { font-size: 1.1rem; }
  .module-grid { grid-template-columns: 1fr; gap: 10px; }
  .module-card { padding: 14px; }
  .mod-icon-wrap { width: 36px; height: 36px; }
  .filter-btn { padding: 5px 10px; font-size: 12px; }
}
</style>