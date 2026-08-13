<template>
  <div ref="pageRef" class="learning-path-browser">
    <!-- ===== 深空动态背景 ===== -->
    <div class="bg-deep">
      <div class="aurora-orb orb-cyan"></div>
      <div class="aurora-orb orb-purple"></div>
      <div class="aurora-orb orb-pink"></div>
    </div>
    <div class="grid-overlay"></div>

    <!-- 浮动粒子 -->
    <div class="particles-wrap">
      <div
        v-for="p in particles"
        :key="p.id"
        class="particle"
        :style="{
          left: p.x + '%',
          top: p.y + '%',
          width: p.size + 'px',
          height: p.size + 'px',
          animationDuration: p.duration + 's',
          animationDelay: p.delay + 's',
          opacity: p.opacity
        }"
      ></div>
    </div>

    <!-- ===== 顶部标题区 ===== -->
    <header class="page-header">
      <div class="header-top-row">
        <button class="back-btn" @click="goBack">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          <span>返回</span>
        </button>

        <div class="header-title-area">
          <h1 class="page-title">
            <span class="title-icon">📚</span>
            <span class="title-text">学习路径浏览</span>
            <span class="title-badge">{{ filteredPaths.length }} 条路径</span>
          </h1>
          <p class="page-subtitle">发现系统化的学习路线，从入门到精通，开启你的成长之旅</p>
        </div>

        <button class="create-btn" @click="handleCreatePath">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span>创建新路径</span>
        </button>
      </div>

      <!-- 搜索栏 -->
      <div class="search-section">
        <div class="search-box">
          <svg class="search-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            v-model="searchQuery"
            type="text"
            class="search-field"
            placeholder="搜索路径名称、描述、技能标签..."
          />
          <span v-if="searchQuery" class="search-clear" @click="searchQuery = ''">✕</span>
        </div>
      </div>

      <!-- 筛选标签 -->
      <nav class="filter-tabs">
        <button
          v-for="tab in filterTabs"
          :key="tab.key"
          class="filter-tab"
          :class="{ active: activeFilter === tab.key }"
          @click="activeFilter = tab.key"
        >
          <span class="tab-ico">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
        </button>
      </nav>
    </header>

    <!-- ===== 路径卡片网格 ===== -->
    <section class="paths-section">
      <TransitionGroup name="card-trans" tag="div" class="paths-grid">
        <div
          v-for="(path, idx) in filteredPaths"
          :key="path.id"
          class="path-card"
          :style="{ animationDelay: (idx * 0.07) + 's' }"
          @click="viewDetail(path)"
        >
          <!-- 旋转光效边框 -->
          <div class="card-glow-border"></div>

          <!-- 顶部色条 -->
          <div
class="card-stripe" :style="{
            background: `linear-gradient(90deg, ${path.color}, transparent 80%)`
          }"
></div>

          <!-- 卡片图标区 -->
          <div class="card-icon-wrap" :style="{ background: `radial-gradient(circle at center, ${path.color}22, transparent 70%)` }">
            <span class="card-icon">{{ path.icon }}</span>
          </div>

          <!-- 卡片内容 -->
          <div class="card-body">
            <div class="card-top-row">
              <span
                class="card-level"
                :class="path.levelClass"
              >{{ path.levelText }}</span>
              <span v-if="path.recommended" class="card-badge">⭐ 推荐</span>
            </div>

            <h3 class="card-title">{{ path.name }}</h3>
            <p class="card-desc">{{ path.description }}</p>

            <!-- 标签行 -->
            <div class="card-tags">
              <span
                v-for="tag in path.tags"
                :key="tag"
                class="card-tag"
              >{{ tag }}</span>
              <span class="card-tag duration-tag">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                {{ path.duration }}
              </span>
            </div>

            <!-- 统计 & 按钮 -->
            <div class="card-footer">
              <div class="card-stats">
                <div class="stat-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                  </svg>
                  <span>{{ path.students }}</span>
                </div>
                <div class="stat-item rating">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                  </svg>
                  <span>{{ path.rating }}</span>
                </div>
              </div>
              <button class="detail-btn" @click.stop="viewDetail(path)">
                <span>查看详情</span>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </TransitionGroup>

      <!-- 空状态 -->
      <div v-if="filteredPaths.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <p class="empty-title">没有找到匹配的路径</p>
        <p class="empty-desc">尝试更换筛选条件或搜索关键词</p>
        <button class="empty-btn" @click="resetFilters">重置筛选</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const pageRef = ref(null)

// ===== 浮动粒子 =====
const particles = ref([])
const generateParticles = () => {
  const arr = []
  for (let i = 0; i < 35; i++) {
    arr.push({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 3 + 1,
      duration: Math.random() * 25 + 15,
      delay: Math.random() * 12,
      opacity: Math.random() * 0.35 + 0.08
    })
  }
  particles.value = arr
}

// ===== 导航 =====
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

const viewDetail = () => {
  // 浏览页展示的是示例数据（id 非真实路径 UUID），跳详情会 404；引导至真实路径列表
  ElMessage.info('示例路径仅供浏览，请前往「学习路径」查看真实路径')
  router.push('/learning-path')
}

const handleCreatePath = () => {
  router.push('/path-adjust')
}

// ===== 筛选标签 =====
const activeFilter = ref('all')
const searchQuery = ref('')

const filterTabs = [
  { key: 'all', label: '全部', icon: '📋', count: 0 },
  { key: 'beginner', label: '入门', icon: '🌱', count: 0 },
  { key: 'intermediate', label: '中级', icon: '⚡', count: 0 },
  { key: 'advanced', label: '高级', icon: '🚀', count: 0 },
  { key: 'recommended', label: '推荐', icon: '⭐', count: 0 }
]

// ===== Mock 数据：8 条学习路径 =====
const paths = [
  {
    id: 1,
    name: 'Python 数据分析',
    description: '从零开始掌握 Python 数据分析全流程，覆盖 NumPy、Pandas、Matplotlib 等核心库，配合真实数据集实战演练。',
    icon: '🐍',
    color: '#00f5d4',
    levelClass: 'beginner',
    levelText: '入门',
    recommended: true,
    students: '12.4K',
    rating: '4.8',
    duration: '8 周',
    tags: ['Python', '数据分析', '可视化']
  },
  {
    id: 2,
    name: '机器学习实战',
    description: '深入理解监督学习、无监督学习、集成方法等核心算法，基于 Scikit-learn 与 XGBoost 完成工业级项目实践。',
    icon: '🤖',
    color: '#a855f7',
    levelClass: 'advanced',
    levelText: '高级',
    recommended: true,
    students: '8.5K',
    rating: '4.9',
    duration: '12 周',
    tags: ['机器学习', 'Scikit-learn', 'XGBoost']
  },
  {
    id: 3,
    name: 'Web 全栈开发',
    description: '从前端到后端全面覆盖，掌握 Vue / React 生态、Node.js 服务端开发、数据库设计与部署上线全流程。',
    icon: '🌐',
    color: '#3b82f6',
    levelClass: 'intermediate',
    levelText: '中级',
    recommended: true,
    students: '21K',
    rating: '4.7',
    duration: '16 周',
    tags: ['Vue', 'React', 'Node.js']
  },
  {
    id: 4,
    name: '云原生架构',
    description: '学习 Docker 容器化、Kubernetes 编排、微服务架构设计、Service Mesh 等云原生核心技术栈。',
    icon: '☁️',
    color: '#10b981',
    levelClass: 'advanced',
    levelText: '高级',
    recommended: false,
    students: '4.3K',
    rating: '4.8',
    duration: '14 周',
    tags: ['Docker', 'Kubernetes', '微服务']
  },
  {
    id: 5,
    name: '数据结构与算法',
    description: '系统学习数组、链表、树、图、动态规划等经典数据结构与算法，配合 LeetCode 高频题实战训练。',
    icon: '🧩',
    color: '#f59e0b',
    levelClass: 'intermediate',
    levelText: '中级',
    recommended: false,
    students: '15.7K',
    rating: '4.6',
    duration: '10 周',
    tags: ['算法', '数据结构', 'LeetCode']
  },
  {
    id: 6,
    name: '深度学习进阶',
    description: '深入掌握 CNN、RNN、Transformer、GAN 等前沿模型，使用 PyTorch 框架完成计算机视觉与 NLP 项目。',
    icon: '🧠',
    color: '#ef4444',
    levelClass: 'advanced',
    levelText: '高级',
    recommended: true,
    students: '6.8K',
    rating: '4.9',
    duration: '16 周',
    tags: ['PyTorch', 'CNN', 'Transformer']
  },
  {
    id: 7,
    name: '移动端开发',
    description: '掌握 Flutter / React Native 跨平台开发框架，从 UI 搭建到原生模块集成，构建生产级移动应用。',
    icon: '📱',
    color: '#ec4899',
    levelClass: 'intermediate',
    levelText: '中级',
    recommended: false,
    students: '9.2K',
    rating: '4.5',
    duration: '12 周',
    tags: ['Flutter', 'React Native', '移动端']
  },
  {
    id: 8,
    name: 'DevOps 工程实践',
    description: '学习 CI/CD 流水线、基础设施即代码、监控告警、日志收集等 DevOps 核心理念与工具链实践。',
    icon: '🔧',
    color: '#06b6d4',
    levelClass: 'beginner',
    levelText: '入门',
    recommended: false,
    students: '5.6K',
    rating: '4.7',
    duration: '10 周',
    tags: ['CI/CD', 'Docker', '监控']
  }
]

// ===== 计算筛选计数 =====
const updateFilterCounts = () => {
  filterTabs.forEach(tab => {
    if (tab.key === 'all') {
      tab.count = paths.length
    } else if (tab.key === 'recommended') {
      tab.count = paths.filter(p => p.recommended).length
    } else {
      tab.count = paths.filter(p => p.levelClass === tab.key).length
    }
  })
}
updateFilterCounts()

// ===== 过滤逻辑 =====
const filteredPaths = computed(() => {
  let result = [...paths]

  // 筛选标签
  if (activeFilter.value !== 'all') {
    if (activeFilter.value === 'recommended') {
      result = result.filter(p => p.recommended)
    } else {
      result = result.filter(p => p.levelClass === activeFilter.value)
    }
  }

  // 搜索关键词
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    result = result.filter(p =>
      p.name.toLowerCase().includes(q) ||
      p.description.toLowerCase().includes(q) ||
      p.tags.some(tag => tag.toLowerCase().includes(q))
    )
  }

  return result
})

const resetFilters = () => {
  activeFilter.value = 'all'
  searchQuery.value = ''
}

// ===== 生命周期 =====
onMounted(() => {
  generateParticles()
})
</script>

<style lang="scss" scoped>
// ============================================
// 学习路径浏览 · 深色科技风
// ============================================

$bg-deep: #0a0a1a;
$accent: #00f5d4;
$accent-purple: #a855f7;
$accent-blue: #3b82f6;
$text-primary: #e8e8ff;
$text-secondary: #c0c0e0;
$text-muted: #9090b8;

.learning-path-browser {
  min-height: 100vh;
  background: $bg-deep;
  position: relative;
  padding: 0 40px 60px;
  overflow-x: hidden;
}

/* ===== 深空动态背景 ===== */
.bg-deep {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(ellipse at 70% 20%, rgba(0, 245, 212, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 80%, rgba(123, 97, 255, 0.06) 0%, transparent 50%),
    $bg-deep;
}
.aurora-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(130px);
  animation: floatOrb 22s ease-in-out infinite;
}
.orb-cyan {
  width: 650px; height: 650px;
  top: -220px; right: -120px;
  background: radial-gradient(circle, rgba(0, 245, 212, 0.12) 0%, transparent 70%);
}
.orb-purple {
  width: 550px; height: 550px;
  bottom: -180px; left: -100px;
  background: radial-gradient(circle, rgba(123, 97, 255, 0.1) 0%, transparent 70%);
  animation-delay: -8s;
}
.orb-pink {
  width: 420px; height: 420px;
  top: 40%; left: 42%;
  background: radial-gradient(circle, rgba(255, 0, 110, 0.06) 0%, transparent 70%);
  animation-delay: -15s;
}
@keyframes floatOrb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(35px, -35px) scale(1.1); }
  50% { transform: translate(-25px, 25px) scale(0.92); }
  75% { transform: translate(25px, 12px) scale(1.06); }
}

.grid-overlay {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 245, 212, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 245, 212, 0.025) 1px, transparent 1px);
  background-size: 64px 64px;
  pointer-events: none;
  z-index: 0;
}

/* ===== 浮动粒子 ===== */
.particles-wrap {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(0, 245, 212, 0.35);
  animation: particleRise linear infinite;
  will-change: transform;
}
@keyframes particleRise {
  0% { transform: translateY(0) translateX(0); }
  25% { transform: translateY(-30vh) translateX(18px); }
  50% { transform: translateY(-55vh) translateX(-12px); }
  75% { transform: translateY(-80vh) translateX(8px); }
  100% { transform: translateY(-100vh) translateX(0); }
}

/* ===== 顶部标题区 ===== */
.page-header {
  position: relative;
  z-index: 1;
  padding: 24px 0 8px;
}

.header-top-row {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  flex-shrink: 0;
  margin-top: 6px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  &:hover {
    border-color: rgba($accent, 0.25);
    color: $accent;
    box-shadow: 0 0 18px rgba($accent, 0.08);
    background: rgba($accent, 0.04);
  }
  svg { flex-shrink: 0; }
}

.header-title-area {
  flex: 1;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1.9rem;
  font-weight: 800;
  color: $text-primary;
  margin-bottom: 6px;
  letter-spacing: -0.02em;

  .title-icon {
    font-size: 1.8rem;
    line-height: 1;
  }
  .title-text {
    background: linear-gradient(135deg, #e8e8ff 0%, $accent 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  .title-badge {
    font-size: 0.7rem;
    font-weight: 600;
    padding: 3px 12px;
    border-radius: 20px;
    background: rgba($accent, 0.08);
    border: 1px solid rgba($accent, 0.15);
    color: $accent;
    -webkit-text-fill-color: $accent;
    margin-left: 4px;
  }
}

.page-subtitle {
  font-size: 0.88rem;
  color: $text-muted;
  font-weight: 400;
  letter-spacing: 0.01em;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  flex-shrink: 0;
  margin-top: 6px;
  background: linear-gradient(135deg, rgba($accent, 0.12) 0%, rgba($accent, 0.06) 100%);
  border: 1px solid rgba($accent, 0.2);
  border-radius: 10px;
  color: $accent;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;

  &:hover {
    background: linear-gradient(135deg, rgba($accent, 0.2) 0%, rgba($accent, 0.1) 100%);
    border-color: rgba($accent, 0.4);
    box-shadow: 0 0 30px rgba($accent, 0.1), 0 4px 20px rgba($accent, 0.05);
    transform: translateY(-1px);
  }
  &:active {
    transform: translateY(0);
  }
  svg { flex-shrink: 0; }
}

/* ===== 搜索栏 ===== */
.search-section {
  margin-top: 24px;
}
.search-box {
  position: relative;
  max-width: 560px;
}
.search-ico {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: $text-muted;
  pointer-events: none;
}
.search-field {
  width: 100%;
  padding: 12px 44px 12px 44px;
  background: rgba(25, 30, 60, 0.5);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 12px;
  color: $text-primary;
  font-size: 0.88rem;
  outline: none;
  transition: all 0.3s ease;

  &::placeholder { color: $text-muted; font-weight: 400; }
  &:focus {
    border-color: rgba($accent, 0.25);
    background: rgba(25, 30, 60, 0.7);
    box-shadow: 0 0 20px rgba($accent, 0.04), 0 0 0 1px rgba($accent, 0.06);
  }
}
.search-clear {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 0.7rem;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: rgba(100, 100, 180, 0.15);
    color: $text-primary;
  }
}

/* ===== 筛选标签 ===== */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-top: 18px;
  flex-wrap: wrap;
}
.filter-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: rgba(25, 30, 60, 0.3);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 24px;
  color: $text-secondary;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;

  .tab-ico { font-size: 0.9rem; line-height: 1; }
  .tab-count {
    font-size: 0.7rem;
    padding: 1px 7px;
    border-radius: 10px;
    background: rgba(100, 100, 180, 0.1);
    color: $text-muted;
    transition: all 0.3s ease;
  }

  &:hover {
    border-color: rgba($accent, 0.15);
    background: rgba($accent, 0.03);
    color: $text-primary;
  }

  &.active {
    background: rgba($accent, 0.08);
    border-color: rgba($accent, 0.2);
    color: $accent;
    box-shadow: 0 0 16px rgba($accent, 0.04);

    .tab-count {
      background: rgba($accent, 0.12);
      color: $accent;
    }
  }
}

/* ===== 卡片网格 ===== */
.paths-section {
  position: relative;
  z-index: 1;
  padding-top: 28px;
}
.paths-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* ===== 卡片过渡动画 ===== */
.card-trans-enter-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.card-trans-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.card-trans-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.96);
}
.card-trans-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.96);
}
.card-trans-move {
  transition: transform 0.4s ease;
}

/* ===== 路径卡片 ===== */
.path-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  background: rgba(17, 20, 42, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(100, 100, 180, 0.08);
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.4s ease;
  animation: cardEnter 0.6s cubic-bezier(0.4, 0, 0.2, 1) both;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3), 0 0 60px rgba($accent, 0.03);
    border-color: rgba($accent, 0.12);

    .card-glow-border { opacity: 1; }
    .detail-btn {
      background: rgba($accent, 0.12);
      border-color: rgba($accent, 0.25);
    }
    .card-icon { transform: scale(1.15); }
  }

  &:active { transform: translateY(-2px) scale(0.98); }
}

@keyframes cardEnter {
  0% {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 旋转光效边框 */
.card-glow-border {
  position: absolute;
  inset: -1px;
  border-radius: 16px;
  opacity: 0;
  transition: opacity 0.5s ease;
  z-index: 0;
  pointer-events: none;
  background: conic-gradient(
    from 0deg,
    transparent,
    rgba($accent, 0.06),
    rgba($accent, 0.1),
    rgba($accent, 0.06),
    transparent 60%,
    transparent
  );
  animation: glowRotate 4s linear infinite;
}
@keyframes glowRotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 顶部色条 */
.card-stripe {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 1;
}

/* 图标区 */
.card-icon-wrap {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 20px 8px;
}
.card-icon {
  font-size: 2.4rem;
  line-height: 1;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 卡片内容 */
.card-body {
  position: relative;
  z-index: 1;
  padding: 0 18px 18px;
  display: flex;
  flex-direction: column;
}

.card-top-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.card-level {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  border: 1px solid;

  &.beginner {
    color: #10b981;
    border-color: rgba(16, 185, 129, 0.2);
    background: rgba(16, 185, 129, 0.08);
  }
  &.intermediate {
    color: $accent-blue;
    border-color: rgba(59, 130, 246, 0.2);
    background: rgba(59, 130, 246, 0.08);
  }
  &.advanced {
    color: $accent-purple;
    border-color: rgba(168, 85, 247, 0.2);
    background: rgba(168, 85, 247, 0.08);
  }
}

.card-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
  color: $accent;
  border: 1px solid rgba($accent, 0.2);
  background: rgba($accent, 0.08);
}

.card-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 6px;
  line-height: 1.3;
  letter-spacing: -0.01em;
}

.card-desc {
  font-size: 0.78rem;
  color: $text-secondary;
  line-height: 1.55;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 标签行 */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}
.card-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 0.7rem;
  font-weight: 500;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.08);
  color: $text-muted;

  &.duration-tag {
    color: rgba($accent, 0.7);
    border-color: rgba($accent, 0.08);
    background: rgba($accent, 0.04);
    svg { width: 11px; height: 11px; }
  }
}

/* 底部统计 & 按钮 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(100, 100, 180, 0.06);
}

.card-stats {
  display: flex;
  gap: 14px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.78rem;
  color: $text-muted;
  font-weight: 500;

  svg {
    width: 13px;
    height: 13px;
    opacity: 0.5;
  }

  &.rating {
    color: #f59e0b;
    svg { opacity: 0.7; }
  }
}

.detail-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  background: rgba($accent, 0.04);
  border: 1px solid rgba($accent, 0.1);
  border-radius: 8px;
  color: $accent;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;

  svg {
    width: 12px;
    height: 12px;
    transition: transform 0.3s ease;
  }

  &:hover {
    background: rgba($accent, 0.12);
    border-color: rgba($accent, 0.25);
    svg { transform: translateX(3px); }
  }
}

/* ===== 空状态 ===== */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    font-size: 3rem;
    margin-bottom: 16px;
    opacity: 0.6;
  }
  .empty-title {
    font-size: 1.2rem;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 8px;
  }
  .empty-desc {
    font-size: 0.85rem;
    color: $text-muted;
    margin-bottom: 20px;
  }
  .empty-btn {
    padding: 10px 28px;
    background: rgba($accent, 0.08);
    border: 1px solid rgba($accent, 0.15);
    border-radius: 10px;
    color: $accent;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    &:hover {
      background: rgba($accent, 0.14);
      border-color: rgba($accent, 0.25);
      box-shadow: 0 0 20px rgba($accent, 0.06);
    }
  }
}

/* ===== 响应式 ===== */

/* 平板：3 列 */
@media (max-width: 1200px) {
  .paths-grid { grid-template-columns: repeat(3, 1fr); }
}

/* 小平板：2 列 */
@media (max-width: 900px) {
  .learning-path-browser { padding: 0 24px 40px; }
  .paths-grid { grid-template-columns: repeat(2, 1fr); }
  .page-title { font-size: 1.6rem; }
  .create-btn { padding: 8px 16px; font-size: 0.8rem; }
}

/* 手机：1 列 */
@media (max-width: 640px) {
  .learning-path-browser { padding: 0 16px 32px; }
  .paths-grid { grid-template-columns: 1fr; gap: 16px; }

  .header-top-row {
    flex-wrap: wrap;
    gap: 12px;
  }
  .back-btn { order: 1; }
  .header-title-area { order: 2; flex-basis: 100%; }
  .create-btn {
    order: 3;
    width: 100%;
    justify-content: center;
    margin-top: 0;
  }

  .page-title {
    font-size: 1.4rem;
    flex-wrap: wrap;
    .title-badge { font-size: 0.65rem; }
  }

  .filter-tabs { gap: 6px; }
  .filter-tab {
    padding: 6px 14px;
    font-size: 0.78rem;
    .tab-count { display: none; }
  }

  .card-icon { font-size: 2rem; }
  .card-title { font-size: 0.95rem; }
}
</style>
