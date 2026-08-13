<template>
  <div ref="pageRef" class="home-page">
    <!-- 深空背景 -->
    <div class="bg-aurora">
      <div class="aurora-layer aurora-1"></div>
      <div class="aurora-layer aurora-2"></div>
      <div class="aurora-layer aurora-3"></div>
    </div>
    <div class="grid-bg"></div>
    <div class="cursor-glow" :style="{ left: mouseX + 'px', top: mouseY + 'px' }"></div>

    <!-- ===== Hero区域 ===== -->
    <section class="hero">
      <div class="hero-content">
        <div class="hero-decoration">
          <div class="decoration-ring ring-1"></div>
          <div class="decoration-ring ring-2"></div>
          <div class="decoration-ring ring-3"></div>
        </div>
        <div class="tech-tags">
          <span class="tech-tag">Spring AI</span>
          <span class="tech-divider">+</span>
          <span class="tech-tag">MCP</span>
          <span class="tech-divider">+</span>
          <span class="tech-tag">ReAct Agent</span>
        </div>
        <h1 class="hero-title">
          <span class="title-line">智能学习</span>
          <span class="title-line gradient-text streaming-text">新纪元</span>
        </h1>
        <div class="hero-subtitle-wrap">
          <div class="subtitle-bar"></div>
          <div class="subtitle-text">
            <p class="hero-subtitle">基于大模型的自主智能体，精准诊断、动态规划、引导式教学</p>
            <p class="hero-desc">为你打造千人千面的个性化学习路径</p>
          </div>
        </div>

        <!-- 数字滚动统计 -->
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-value" :class="{ 'animating': isAnimating }">{{ displayStats.learners }}</span>
            <div class="stat-trend"><TrendingUp :size="14" /><span>+12%</span></div>
            <span class="stat-label">活跃学习者</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value" :class="{ 'animating': isAnimating }">{{ displayStats.satisfaction }}%</span>
            <div class="stat-trend"><TrendingUp :size="14" /><span>+3%</span></div>
            <span class="stat-label">满意度</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">24/7</span>
            <div class="stat-trend"><Activity :size="14" /><span>持续</span></div>
            <span class="stat-label">在线服务</span>
          </div>
        </div>

        <div class="hero-actions">
          <button class="btn-primary breathing-glow" @click="router.push('/chat')">
            <MessageSquare :size="18" />
            <span class="btn-text">开始对话</span>
            <ArrowRight :size="16" class="btn-icon" />
          </button>
          <button class="btn-secondary sweep-card" @click="router.push('/knowledge')">
            <BookOpen :size="18" />
            <span class="btn-text">探索知识库</span>
          </button>
        </div>
      </div>
      <div class="hero-floating-cards">
        <div class="floating-card card-1">
          <Target :size="18" class="card-icon-svg" />
          <div class="card-text">智能规划</div>
        </div>
        <div class="floating-card card-2">
          <BookOpen :size="18" class="card-icon-svg" />
          <div class="card-text">知识检索</div>
        </div>
        <div class="floating-card card-3">
          <Sparkles :size="18" class="card-icon-svg" />
          <div class="card-text">精准答疑</div>
        </div>
      </div>
    </section>

    <!-- ===== 核心能力 ===== -->
    <section class="capabilities">
      <div class="section-header">
        <span class="section-badge"><Zap :size="14" /> 核心能力</span>
        <h2 class="section-title">AI 驱动的学习体验</h2>
      </div>
      <div class="capabilities-grid">
        <div v-for="(cap, index) in capabilities" :key="index" class="capability-card glass-card sweep-card" @click="navigateToCapability(cap.path)">
          <div class="cap-status">
            <span class="cap-status-dot"></span>
            <span class="cap-status-text">{{ cap.status }}</span>
          </div>
          <div class="cap-icon-wrapper">
            <component :is="cap.icon" :size="28" class="cap-icon-svg" />
          </div>
          <h3 class="cap-title">{{ cap.title }}</h3>
          <p class="cap-desc">{{ cap.desc }}</p>
          <div class="cap-metric">
            <span class="metric-value">{{ cap.metric }}</span>
            <span class="metric-label">{{ cap.metricLabel }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 快速开始 ===== -->
    <section class="quick-entry">
      <div class="section-header">
        <span class="section-badge"><Compass :size="14" /> 快速开始</span>
        <h2 class="section-title">探索 AI 能力</h2>
      </div>
      <div class="entry-grid">
        <div v-for="(entry, index) in quickEntries" :key="index" class="entry-card glass-card sweep-card" @click="router.push(entry.path)">
          <div class="entry-header">
            <div class="entry-icon-wrap">
              <component :is="entry.icon" :size="24" class="entry-icon-svg" />
            </div>
            <div class="entry-status" :class="entry.status">
              <span class="status-dot-pulse"></span>
              {{ entry.statusText }}
            </div>
          </div>
          <h3 class="entry-title">{{ entry.title }}</h3>
          <p class="entry-desc">{{ entry.desc }}</p>
          <div class="entry-link"><span>探索</span><ArrowRight :size="14" /></div>
        </div>
      </div>
    </section>

    <!-- ===== 热门推荐 ===== -->
    <section class="courses">
      <div class="section-header">
        <span class="section-badge"><Flame :size="14" /> 热门推荐</span>
        <h2 class="section-title">精品学习路径</h2>
      </div>
      <div class="courses-grid">
        <div v-for="course in displayCourses" :key="course.id" class="course-card glass-card" :class="{ 'guide-card': course.source === 'guide' }" @click="goToPathDetail(course)">
          <div class="course-gradient" :style="{ background: course.gradient }"></div>
          <div class="course-content">
            <div class="course-top">
              <span v-if="course.source === 'guide'" class="course-guide-badge">
                <Sparkles :size="12" /> 示例 · 去创建
              </span>
              <span v-else-if="course.recommended || course.isTodayRecommend" class="course-recommended">
                {{ course.isTodayRecommend ? '今日推荐' : '推荐' }}
              </span>
              <span class="course-difficulty" :class="course.level">{{ course.levelText }}</span>
            </div>
            <h3 class="course-title">{{ course.title }}</h3>
            <div class="course-meta">
              <span v-if="course.source !== 'guide'" class="course-learners"><Users :size="14" /> {{ course.studentsFormatted }} 学习者</span>
              <span class="course-rating"><Star :size="14" /> {{ course.rating }}</span>
            </div>
            <div v-if="course.source !== 'guide'" class="course-progress">
              <div class="course-progress-bar">
                <div class="course-progress-fill" :style="{ width: course.progress + '%' }"></div>
              </div>
              <span class="course-progress-text">已学 {{ course.progress }}%</span>
            </div>
            <div v-if="course.source !== 'guide' && course.nextNodeName" class="course-next-node">
              <span class="next-node-label">下一节点：</span>
              <span class="next-node-name">{{ course.nextNodeName }}</span>
            </div>
            <div v-if="course.source === 'guide'" class="course-action">
              <button class="course-start-btn" @click.stop="goToPathDetail(course)">
                <Plus :size="14" />
                去创建
              </button>
            </div>
            <div v-else-if="course.isTodayRecommend && course.progress === 0" class="course-action">
              <button class="course-start-btn" @click.stop="goToPathDetail(course)">
                <Compass :size="14" />
                开始学习
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Target, BookOpen, Sparkles, MessageSquare, ArrowRight,
  TrendingUp, Activity, Zap, Compass, Flame, Users, Star,
  Brain, Map, LineChart, RefreshCw, PenTool,
  BarChart3, Plus
} from 'lucide-vue-next'
import { useStatsStore } from '@/stores/statsStore'
import { getActivePath } from '@/api/learningPath'

const router = useRouter()

const navigateToCapability = (path) => {
  if (path) {
    router.push(path)
  }
}
const pageRef = ref(null)
const mouseX = ref(0)
const mouseY = ref(0)

const statsStore = useStatsStore()

const displayStats = ref({ learners: '0K+', satisfaction: '0' })
const isAnimating = ref(false)

const capabilities = [
  { icon: Brain, title: '智能诊断', desc: 'AI 精准分析你的知识水平，找出薄弱环节', metric: '98%', metricLabel: '诊断准确率', status: '运行中', path: '/capability/diagnosis' },
  { icon: Map, title: '动态规划', desc: '根据学习进度实时调整学习路径和计划', metric: '500+', metricLabel: '学习路径', status: '运行中', path: '/capability/planning' },
  { icon: LineChart, title: '进度追踪', desc: '可视化学习数据，掌握每一步进展', metric: '24/7', metricLabel: '实时监控', status: '运行中', path: '/capability/progress' },
  { icon: RefreshCw, title: '自适应学习', desc: 'AI 根据你的反馈自动优化教学策略', metric: '3.0', metricLabel: '智能版本', status: '运行中', path: '/capability/adaptive' }
]

const quickEntries = [
  { icon: MessageSquare, title: 'AI 对话', desc: '与 AI 学习助手实时对话，获取个性化指导', path: '/chat', status: 'active', statusText: '在线' },
  { icon: BookOpen, title: '知识库', desc: '浏览和搜索丰富的学习资源与知识文档', path: '/knowledge', status: 'active', statusText: '在线' },
  { icon: PenTool, title: '学习路径', desc: '查看和管理你的个性化学习计划', path: '/learning-path', status: 'ready', statusText: '就绪' },
  { icon: BarChart3, title: '学习统计', desc: '分析学习数据，了解你的成长轨迹', path: '/statistics', status: 'ready', statusText: '就绪' }
]

// 示例引导卡片：真实路径不足 4 条时补齐展示，点击跳转目标设定页生成专属路径
const guideCourses = [
  { id: 'guide-1', title: '机器学习实战', rating: '4.9', level: 'advanced', levelText: '高级', recommended: false, progress: 0, gradient: 'linear-gradient(135deg, rgba(168,85,247,0.08), rgba(0,229,255,0.06))', source: 'guide' },
  { id: 'guide-2', title: 'Web 全栈开发', rating: '4.7', level: 'intermediate', levelText: '中级', recommended: false, progress: 0, gradient: 'linear-gradient(135deg, rgba(0,85,255,0.08), rgba(168,85,247,0.06))', source: 'guide' },
  { id: 'guide-3', title: '云原生架构', rating: '4.8', level: 'advanced', levelText: '高级', recommended: false, progress: 0, gradient: 'linear-gradient(135deg, rgba(168,85,247,0.06), rgba(0,229,255,0.08))', source: 'guide' },
  { id: 'guide-4', title: '数据结构与算法', rating: '4.9', level: 'intermediate', levelText: '中级', recommended: false, progress: 0, gradient: 'linear-gradient(135deg, rgba(0,229,255,0.08), rgba(0,85,255,0.08))', source: 'guide' }
]

// 用户真实学习路径（P3：首页卡片与真实路径一致，点击直达详情）
const realCourses = ref([])

// 方案 A：真实路径优先展示，不足 4 条时用示例引导卡片补齐（点击跳转目标设定页）
const displayCourses = computed(() => {
  const real = realCourses.value.slice(0, 4)
  if (real.length >= 4) return real
  const guides = guideCourses.slice(0, 4 - real.length)
  return [...real, ...guides]
})

// 拉取用户真实学习路径（使用统一数据源，解决卡片与详情页数据不一致问题）
const fetchRealPaths = async () => {
  try {
    const res = await getActivePath()
    const data = res?.data ?? res

    // 无路径时显示引导卡片
    if (!data || !data.hasPath) {
      realCourses.value = []
      return
    }

    // 根据路径状态构建显示数据
    const path = data.path
    const progress = data.progress || { percentage: 0, completedNodes: 0, totalNodes: 0 }
    const nextNode = data.nextNode

    realCourses.value = [{
      id: path?.id,
      title: (path?.name || '未命名学习路径').replace(/^【[^】]*】/, ''),
      studentsFormatted: '—',
      rating: '4.5',
      level: 'intermediate',
      levelText: '中级',
      recommended: data.status === 'IN_PROGRESS',
      isTodayRecommend: true,
      progress: progress.percentage || 0,
      completedNodes: progress.completedNodes || 0,
      totalNodes: progress.totalNodes || 0,
      nextNodeName: nextNode?.nodeName || null,
      status: data.status,
      gradient: 'linear-gradient(135deg, rgba(0,229,255,0.08), rgba(0,85,255,0.08))',
      source: 'real'
    }]
  } catch (e) {
    console.warn('加载真实学习路径失败:', e.message)
  }
}

const goToPathDetail = (course) => {
  if (course?.source === 'real') {
    // 真实路径：直接进入对应路径详情页
    router.push(`/learning-path/${course.id}`)
  } else {
    // 示例引导卡片：跳转目标设定页，引导生成自己的学习路径
    router.push('/goal-setting')
  }
}

function fetchPopularPaths() {
}

const handleMouseMove = (e) => {
  if (pageRef.value) {
    const rect = pageRef.value.getBoundingClientRect()
    mouseX.value = e.clientX - rect.left
    mouseY.value = e.clientY - rect.top
  }
}

onMounted(async () => {
  document.addEventListener('mousemove', handleMouseMove)

  try {
    await Promise.all([
      statsStore.fetchDashboardStats(),
      fetchPopularPaths(),
      fetchRealPaths()
    ])

    const stats = statsStore.dashboardStats
    displayStats.value.learners = Math.round(stats.totalLearners / 1000) + 'K+'
    displayStats.value.satisfaction = stats.satisfaction
  } catch (error) {
    console.error('加载首页数据失败:', error)
  }

  setTimeout(() => {
    isAnimating.value = true
    const dur = 1500, t0 = performance.now()
    const anim = (t) => {
      const p = Math.min((t - t0) / dur, 1)
      const e = 1 - Math.pow(1 - p, 3)
      displayStats.value.learners = Math.round(18 * e) + 'K+'
      displayStats.value.satisfaction = Math.round(98 * e)
      if (p < 1) {
        requestAnimationFrame(anim)
      } else {
        isAnimating.value = false
      }
    }
    requestAnimationFrame(anim)
  }, 300)
})

onUnmounted(() => { document.removeEventListener('mousemove', handleMouseMove) })
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding-bottom: 80px;
  animation: fadeIn 0.6s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.bg-aurora {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -2;
}

.aurora-layer {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  animation: aurora 20s ease-in-out infinite;
}

.aurora-1 {
  width: 600px; height: 600px;
  top: -200px; right: -100px;
  background: radial-gradient(circle, rgba(0,229,255,0.08) 0%, transparent 70%);
}

.aurora-2 {
  width: 500px; height: 500px;
  bottom: -150px; left: -100px;
  background: radial-gradient(circle, rgba(0,85,255,0.08) 0%, transparent 70%);
  animation-delay: -7s;
}

.aurora-3 {
  width: 400px; height: 400px;
  top: 40%; left: 40%;
  background: radial-gradient(circle, rgba(168,85,247,0.05) 0%, transparent 70%);
  animation-delay: -14s;
}

@keyframes aurora {
  0%,100% { transform: translate(0,0) scale(1); }
  25% { transform: translate(30px,-30px) scale(1.1); }
  50% { transform: translate(-20px,20px) scale(0.95); }
  75% { transform: translate(20px,10px) scale(1.05); }
}

@keyframes data-breathe {
  0%, 100% {
    filter: var(--data-glow);
    text-shadow: var(--text-shadow-glow);
  }
  50% {
    filter: drop-shadow(0 0 25px rgba(0, 229, 255, 0.5));
    text-shadow: 0 0 35px rgba(0, 229, 255, 0.4);
  }
}

.grid-bg {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(0,229,255,0.015) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,229,255,0.015) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  z-index: -1;
}

.cursor-glow {
  position: absolute;
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(0,229,255,0.03) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  transform: translate(-50%,-50%);
  transition: left 0.3s ease, top 0.3s ease;
  z-index: 0;
}

.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 100px 48px 60px;
}

.hero-content {
  max-width: 800px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.hero-decoration {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%,-50%);
  pointer-events: none;
}

.decoration-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(0,229,255,0.06);
  animation: ring-rotate 30s linear infinite;
}

.ring-1 { width: 600px; height: 600px; top: -300px; left: -300px; }
.ring-2 { width: 500px; height: 500px; top: -250px; left: -250px; animation-direction: reverse; animation-duration: 25s; }
.ring-3 { width: 400px; height: 400px; top: -200px; left: -200px; animation-duration: 20s; }

@keyframes ring-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.tech-tags {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}

.tech-tag {
  padding: 8px 16px;
  background: rgba(0,229,255,0.06);
  border: 1px solid rgba(0,229,255,0.12);
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--accent-primary);
  letter-spacing: 0.02em;
}

.tech-divider { color: var(--text-muted); font-size: 0.85rem; }

.hero-title {
  font-size: 4.5rem;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 32px;
}

.title-line { display: block; }

.gradient-text {
  background: linear-gradient(135deg, #00E5FF 0%, #0055FF 50%, #A855F7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradient-shift 5s ease infinite;
  background-size: 200% 200%;
}

.streaming-text {
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(0, 229, 255, 0.3), transparent);
    animation: streaming-glow 3s ease-in-out infinite;
  }
}

@keyframes streaming-glow {
  0% { left: -100%; }
  50% { left: 100%; }
  100% { left: 100%; }
}

@keyframes gradient-shift {
  0%,100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.hero-subtitle-wrap {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 48px;
  text-align: left;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.subtitle-bar {
  width: 3px;
  min-height: 50px;
  background: linear-gradient(180deg, #00E5FF, #0055FF);
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 4px;
  box-shadow: 0 0 8px rgba(0,229,255,0.3);
}

.hero-subtitle {
  font-size: 1.2rem;
  color: var(--text-secondary);
  margin-bottom: 6px;
  font-weight: 500;
  line-height: 1.6;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.1);
}

.hero-desc {
  font-size: 1rem;
  color: var(--text-body);
  line-height: 1.6;
  opacity: 0.9;
}

.hero-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  margin-bottom: 48px;
}

.stat-item { text-align: center; }

.stat-value {
  display: block;
  font-size: 2.5rem;
  font-weight: 800;
  font-family: var(--font-mono);
  line-height: 1;
  margin-bottom: 4px;
  background: var(--data-highlight);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: var(--data-glow);
  text-shadow: var(--text-shadow-glow);
  animation: data-breathe 3s ease-in-out infinite;

  &.animating {
    color: #FFFFFF;
    -webkit-text-fill-color: #FFFFFF;
    text-shadow: 0 0 30px rgba(0, 229, 255, 0.6), 0 0 60px rgba(0, 229, 255, 0.3);
    filter: drop-shadow(0 0 20px rgba(0, 229, 255, 0.5));
  }
}

.stat-trend {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-bottom: 6px;
  svg { color: var(--accent-emerald); }
  span { font-size: 0.75rem; font-weight: 600; color: var(--accent-emerald); }
}

.stat-label { font-size: 0.85rem; color: var(--text-muted); }

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--border-subtle);
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 32px;
  background: linear-gradient(135deg, rgba(0,229,255,0.15), rgba(0,85,255,0.1));
  backdrop-filter: blur(12px);
  color: var(--accent-primary);
  border: 1px solid rgba(0,229,255,0.25);
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: -2px;
    background: linear-gradient(135deg, #00E5FF, #0055FF);
    border-radius: inherit;
    z-index: -1;
    opacity: 0;
    transition: opacity 0.3s ease;
    filter: blur(8px);
  }

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(0,229,255,0.4);
    box-shadow: 0 0 24px rgba(0,229,255,0.15);
    color: var(--accent-primary);

    &::before { opacity: 0.3; }
    .btn-icon { transform: translateX(4px); }
  }

  &:active { transform: scale(0.98) translateY(-1px); }
  .btn-icon { transition: transform 0.3s ease; }
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 32px;
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(12px);

  &:hover {
    border-color: rgba(0,229,255,0.25);
    color: var(--accent-primary);
    background: rgba(0,229,255,0.04);
    transform: translateY(-2px);
  }

  &:active { transform: scale(0.98); }
}

.hero-floating-cards {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.floating-card {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  background: rgba(15,20,40,0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0,229,255,0.08);
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.3);
  animation: float 6s ease-in-out infinite;
}

.card-1 { top: 20%; left: 5%; }
.card-2 { top: 60%; right: 5%; animation-delay: -2s; }
.card-3 { bottom: 15%; left: 10%; animation-delay: -4s; }

@keyframes float {
  0%,100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.card-icon-svg { color: var(--accent-primary); }
.card-text { font-size: 0.9rem; font-weight: 500; color: var(--text-primary); }

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(0,229,255,0.08);
  border: 1px solid rgba(0,229,255,0.15);
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 500;
  color: #00E5FF;
  margin-bottom: 16px;
}

.section-title {
  font-size: 2rem;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0;
}

.capabilities {
  padding: 64px 48px;
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeInUp 0.6s ease 0.2s both;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.capabilities-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.capability-card {
  position: relative;
  padding: 32px 24px;
  text-align: center;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, #00E5FF, transparent);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover::before { opacity: 1; }
}

.cap-status {
  position: absolute;
  top: 16px; left: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cap-status-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--accent-primary);
  animation: pulse 2s ease-in-out infinite;
}

.cap-status-text { font-size: 0.75rem; font-weight: 500; color: #94A3B8; }

.cap-icon-wrapper {
  position: relative;
  width: 64px; height: 64px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,229,255,0.12);
  border: 1px solid rgba(0,229,255,0.2);
  border-radius: 16px;
}

.cap-icon-svg {
  color: var(--accent-primary);
  filter: drop-shadow(0 0 8px rgba(0,229,255,0.5));
}

.cap-title { font-size: 1.1rem; font-weight: 600; color: #FFFFFF; margin-bottom: 10px; text-shadow: 0 0 20px rgba(0, 229, 255, 0.15); }
.cap-desc { font-size: 0.9rem; color: #E2E8F0; line-height: 1.6; margin-bottom: 20px; opacity: 1; }

.cap-metric {
  padding-top: 16px;
  border-top: 1px solid rgba(0,229,255,0.06);
}

.metric-value {
  display: block;
  font-size: 2rem;
  font-weight: 800;
  font-family: var(--font-mono);
  margin-bottom: 4px;
  background: var(--data-highlight);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: var(--data-glow);
  text-shadow: var(--text-shadow-glow);
}

.metric-label { font-size: 0.85rem; color: #94A3B8; }

.quick-entry {
  padding: 64px 48px;
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeInUp 0.6s ease 0.4s both;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.entry-card {
  position: relative;
  padding: 28px;
  cursor: pointer;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 50% 0%, rgba(0,229,255,0.03) 0%, transparent 60%);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover::before { opacity: 1; }

  &:hover .entry-link {
    opacity: 1;
    transform: translateX(0);
    svg { transform: translateX(0); }
  }
}

.entry-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.entry-icon-wrap {
  width: 52px; height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,229,255,0.1);
  border: 1px solid rgba(0,229,255,0.2);
  border-radius: 14px;
}

.entry-icon-svg { color: var(--accent-primary); filter: drop-shadow(0 0 6px rgba(0,229,255,0.4)); }

.entry-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;

  &.active {
    background: rgba(16,185,129,0.1);
    color: var(--accent-emerald);
    .status-dot-pulse { background: var(--accent-emerald); box-shadow: 0 0 6px rgba(16,185,129,0.5); }
  }

  &.ready {
    background: rgba(0,229,255,0.08);
    color: #94A3B8;
    .status-dot-pulse { background: #94A3B8; }
  }
}

.status-dot-pulse {
  width: 6px; height: 6px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.entry-title { font-size: 1.1rem; font-weight: 600; color: #FFFFFF; margin-bottom: 8px; text-shadow: 0 0 15px rgba(0, 229, 255, 0.1); }
.entry-desc { font-size: 0.9rem; color: #CBD5E1; line-height: 1.5; margin-bottom: 16px; opacity: 1; }

.entry-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--accent-primary);
  opacity: 0;
  transform: translateX(-8px);
  transition: all 0.3s ease;
  svg { transition: transform 0.3s ease; }
}

.courses {
  padding: 64px 48px;
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeInUp 0.6s ease 0.6s both;
  position: relative;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 20px 0;
}

.course-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
  }

  &:active {
    transform: scale(0.98);
  }
}

.course-gradient { position: absolute; inset: 0; }

.course-content {
  position: relative;
  padding: 20px;
  background: rgba(15,20,40,0.4);
  backdrop-filter: blur(8px);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.course-top { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; }

.course-difficulty {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
  border: 1px solid;

  &.beginner { color: var(--accent-emerald); border-color: rgba(16,185,129,0.2); background: rgba(16,185,129,0.08); }
  &.intermediate { color: var(--accent-blue); border-color: rgba(59,130,246,0.2); background: rgba(59,130,246,0.08); }
  &.advanced { color: var(--accent-purple); border-color: rgba(168,85,247,0.2); background: rgba(168,85,247,0.08); }
}

.course-recommended {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--accent-primary);
  border: 1px solid rgba(0,229,255,0.2);
  background: rgba(0,229,255,0.08);
}

.course-guide-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--accent-purple);
  border: 1px dashed rgba(168,85,247,0.35);
  background: rgba(168,85,247,0.08);
  svg { color: var(--accent-purple); }
}

.course-card.guide-card {
  border: 1px dashed rgba(168,85,247,0.25);

  &:hover {
    border-color: rgba(168,85,247,0.5);
    box-shadow: 0 12px 40px rgba(168,85,247,0.12);
  }

  .course-action { margin-top: auto; }
}

.course-title { font-size: 1rem; font-weight: 600; color: #FFFFFF; margin-bottom: 10px; line-height: 1.3; text-shadow: 0 0 20px rgba(0, 229, 255, 0.1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.course-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.course-learners, .course-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
  color: #E2E8F0;
  svg { color: #94A3B8; }
}

.course-rating svg { color: var(--accent-amber); }

.course-progress { margin-top: auto; display: flex; align-items: center; gap: 12px; }
.course-progress-bar { flex: 1; height: 4px; background: rgba(0,229,255,0.08); border-radius: 2px; overflow: hidden; }
.course-progress-fill { height: 100%; background: linear-gradient(90deg, #00E5FF, #10B981); border-radius: 2px; transition: width 1s ease; }
.course-progress-text { font-size: 0.75rem; font-weight: 500; color: var(--accent-emerald); white-space: nowrap; }
.course-next-node { display: flex; align-items: center; gap: 4px; margin-top: 6px; font-size: 0.75rem; }
.next-node-label { color: rgba(255,255,255,0.5); }
.next-node-name { color: var(--accent-cyan); font-weight: 500; }

.course-action {
  margin-top: 12px;
}

.course-start-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba(0, 229, 255, 0.25);
  border-radius: 8px;
  color: #00E5FF;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(0, 229, 255, 0.25), rgba(0, 85, 255, 0.2));
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 229, 255, 0.15);
  }
}

@media (max-width: 1200px) {
  .courses-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
}

@media (max-width: 1024px) {
  .hero { padding: 100px 24px 60px; }
  .hero-title { font-size: 3.5rem; }
  .capabilities-grid, .entry-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-floating-cards { display: none; }
  .courses-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (max-width: 640px) {
  .hero { padding: 80px 16px 40px; }
  .hero-title { font-size: 2.5rem; }
  .hero-stats { gap: 24px; flex-wrap: wrap; }
  .stat-value { font-size: 1.8rem; }
  .hero-actions { flex-direction: column; width: 100%; }
  .btn-primary, .btn-secondary { width: 100%; justify-content: center; }
  .capabilities-grid, .entry-grid { grid-template-columns: 1fr; }
  .section-title { font-size: 1.6rem; }
  .tech-tags { flex-wrap: wrap; gap: 8px; }
  .capabilities, .quick-entry, .courses { padding-left: 24px; padding-right: 24px; }
  .courses-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>