<template>
  <div class="diagnosis-report-page">
    <!-- 顶部导航 -->
    <header class="report-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="15 18 9 12 15 6"/></svg>
          <span>返回</span>
        </button>
      </div>
      <div class="header-center">
        <span class="page-title">
          <span class="title-glyph">📋</span>
          <span>AI 诊断报告</span>
        </span>
        <span class="subject-badge">{{ reportData.subject }}</span>
        <span v-if="reportData.isAdaptive" class="adaptive-badge">自适应测评</span>
      </div>
      <div class="header-right">
        <span class="report-date">{{ reportData.date }}</span>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="report-content">
      <!-- 左侧：巨型雷达图 -->
      <section class="radar-section">
        <div class="radar-container">
          <!-- SVG雷达图 -->
          <div class="radar-chart-wrap">
            <svg viewBox="0 0 300 300" class="radar-svg">
              <defs>
                <!-- 用户区域渐变 -->
                <linearGradient id="userGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#8b5cf6;stop-opacity:0.25"/>
                  <stop offset="100%" style="stop-color:#06b6d4;stop-opacity:0.15"/>
                </linearGradient>
                <!-- 水波扩散滤镜 -->
                <filter id="glow">
                  <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
                  <feMerge>
                    <feMergeNode in="coloredBlur"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
              </defs>

              <!-- 背景网格 - 水波扩散入场 -->
              <g class="radar-grid">
                <polygon v-for="level in 4" :key="level" 
                  :points="getHexagonPoints(level * 25)" 
                  fill="none" 
                  :stroke="level === 4 ? 'rgba(139,92,246,0.2)' : 'rgba(139,92,246,0.08)'" 
                  :stroke-width="level === 4 ? 1.5 : 1"
                  class="grid-line"
                  :style="{ animationDelay: level * 0.15 + 's' }"/>
              </g>
              
              <!-- 轴线 -->
              <g class="radar-axes">
                <line v-for="(dim, i) in radarDimensions" :key="'axis-'+i"
                  x1="150" y1="150" 
                  :x2="150 + Math.cos((i * 60 - 90) * Math.PI / 180) * 125"
                  :y2="150 + Math.sin((i * 60 - 90) * Math.PI / 180) * 125"
                  stroke="rgba(139,92,246,0.12)" stroke-width="1"/>
              </g>

              <!-- 标准模型（灰色虚线） -->
              <polygon :points="standardModelPoints" 
                fill="rgba(255,255,255,0.02)" 
                stroke="rgba(255,255,255,0.18)" 
                stroke-width="1" 
                stroke-dasharray="6,4"
                class="standard-model"
                :class="{ 'visible': isLoaded }"/>
              
              <!-- 用户得分（渐变区域） -->
              <polygon :points="userScorePoints" 
                fill="url(#userGradient)" 
                stroke="url(#userGradient)"
                stroke-width="2.5"
                filter="url(#glow)"
                class="user-score"
                :class="{ 'visible': isLoaded }"/>

              <!-- 维度标签 + 交互热区 -->
              <g v-for="(dim, i) in radarDimensions" :key="'label-'+i" 
                class="dim-group"
                @mouseenter="hoverDimension(i)"
                @mouseleave="leaveDimension">
                <!-- 连接线 -->
                <line :x1="150" :y1="150"
                  :x2="150 + Math.cos((i * 60 - 90) * Math.PI / 180) * (reportData.scores[i] / 100 * 125)"
                  :y2="150 + Math.sin((i * 60 - 90) * Math.PI / 180) * (reportData.scores[i] / 100 * 125)"
                  stroke="rgba(139,92,246,0.06)" stroke-width="1"/>
                <!-- 数据点 -->
                <circle :cx="150 + Math.cos((i * 60 - 90) * Math.PI / 180) * (reportData.scores[i] / 100 * 125)"
                  :cy="150 + Math.sin((i * 60 - 90) * Math.PI / 180) * (reportData.scores[i] / 100 * 125)"
                  :r="hoverIndex === i ? 7 : 4" 
                  :fill="hoverIndex === i ? '#a78bfa' : '#8b5cf6'" 
                  class="data-point"
                  :class="{ 'hovered': hoverIndex === i }"
                  :style="{ animationDelay: i * 0.1 + 's' }"/>
                <!-- 隐形热区 -->
                <circle :cx="150 + Math.cos((i * 60 - 90) * Math.PI / 180) * (reportData.scores[i] / 100 * 125)"
                  :cy="150 + Math.sin((i * 60 - 90) * Math.PI / 180) * (reportData.scores[i] / 100 * 125)"
                  r="20" fill="transparent" cursor="pointer"/>
                <!-- 标签 -->
                <text 
                  :x="150 + Math.cos((i * 60 - 90) * Math.PI / 180) * 145"
                  :y="150 + Math.sin((i * 60 - 90) * Math.PI / 180) * 145"
                  text-anchor="middle" dominant-baseline="middle"
                  :fill="hoverIndex === i ? '#a78bfa' : '#7c7c9a'" 
                  :font-size="hoverIndex === i ? 13 : 11" 
                  :font-weight="hoverIndex === i ? 700 : 500"
                  class="dim-label"
                  style="pointer-events: none;">
                  {{ dim }}
                </text>
              </g>

              <!-- Tooltip -->
              <g v-if="hoverIndex >= 0" class="tooltip-group" style="pointer-events: none;">
                <rect :x="tooltipX - 52" :y="tooltipY - 48" width="104" height="36" rx="8"
                  fill="rgba(15,15,25,0.92)" stroke="rgba(139,92,246,0.3)" stroke-width="1"/>
                <text :x="tooltipX" :y="tooltipY - 34" text-anchor="middle" fill="#e2e8f0" font-size="13" font-weight="700">
                  {{ reportData.scores[hoverIndex] }}分 - {{ getScoreLevel(reportData.scores[hoverIndex]) }}
                </text>
              </g>
            </svg>

            <!-- 中央分数 -->
            <div class="radar-center">
              <span class="score-value" :class="{ 'animate': isLoaded }">{{ animatedScore }}</span>
              <span class="score-label">综合得分</span>
            </div>

            <!-- 扫描线 -->
            <div class="radar-scan-line" :class="{ 'active': isScanning }"></div>
          </div>

          <!-- 统计卡片 -->
          <div class="radar-stats">
            <div class="stat-card">
              <span class="stat-value">{{ reportData.percentile }}%</span>
              <span class="stat-label">超过同级学习者</span>
            </div>
            <div class="stat-card highlight">
              <span class="stat-value">{{ reportData.level }}</span>
              <span class="stat-label">当前等级</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ weakCount }}</span>
              <span class="stat-label">薄弱知识点</span>
            </div>
            <div v-if="reportData.correctCount > 0" class="stat-card">
              <span class="stat-value correct">{{ reportData.correctCount }}✓</span>
              <span class="stat-value wrong">{{ reportData.wrongCount }}✗</span>
              <span class="stat-label">答题统计</span>
            </div>
          </div>

          <!-- 图例 -->
          <div class="radar-legend">
            <div class="legend-item">
              <span class="legend-dot mine"></span>
              <span>我的得分</span>
            </div>
            <div class="legend-item">
              <span class="legend-line standard"></span>
              <span>岗位标准模型</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧：分析区 -->
      <section class="analysis-section">
        <!-- 强弱项对比 -->
        <div class="compare-card glass-card">
          <div class="compare-grid">
            <!-- 最擅长 -->
            <div class="compare-col strengths">
              <div class="compare-header">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#00E676" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                <span class="compare-title">最擅长</span>
              </div>
              <div class="compare-list">
                <div v-for="item in topSkills" :key="item.name" class="compare-item strength">
                  <span class="item-name">{{ item.name }}</span>
                  <div class="item-score-bar">
                    <div class="bar-track">
                      <div class="bar-fill strength" :style="{ width: item.score + '%' }"></div>
                    </div>
                    <span class="item-score">{{ item.score }}%</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 分隔线 -->
            <div class="compare-divider"></div>

            <!-- 急需提升 -->
            <div class="compare-col weaknesses">
              <div class="compare-header">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#FF4D4F" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                <span class="compare-title">需提升</span>
              </div>
              <div class="compare-list">
                <div v-for="item in weakSkills" :key="item.name" class="compare-item weakness">
                  <span class="item-name">{{ item.name }}</span>
                  <div class="item-score-bar">
                    <div class="bar-track">
                      <div class="bar-fill weakness" :style="{ width: item.score + '%' }"></div>
                    </div>
                    <span class="item-score">{{ item.score }}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- AI 深度诊断 -->
        <div class="ai-card glass-card">
          <div class="ai-header">
            <div class="ai-avatar-wrap">
              <div class="avatar-glow"></div>
              <span class="avatar-emoji">🧠</span>
            </div>
            <div class="ai-title-wrap">
              <span class="ai-title">AI 深度诊断</span>
              <span class="ai-badge">智能分析</span>
            </div>
          </div>
          
          <div class="ai-body">
            <div class="ai-text" :class="{ 'typing': isTyping }">
              {{ displayText }}<span v-if="isTyping" class="cursor">|</span>
            </div>
          </div>

          <!-- 行动建议 -->
          <div class="action-list">
            <div v-for="(action, i) in actionItems" :key="i" 
              class="action-item"
              :style="{ animationDelay: i * 0.08 + 's' }">
              <span class="action-icon">{{ action.icon }}</span>
              <div class="action-content">
                <span class="action-title">{{ action.title }}</span>
                <span class="action-desc">{{ action.desc }}</span>
              </div>
              <svg class="action-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="9 18 15 12 9 6"/></svg>
            </div>
          </div>
        </div>

        <!-- 错题回顾 -->
        <div class="mistakes-card glass-card">
          <div class="mistakes-header" @click="toggleMistakes">
            <div class="mistakes-title-wrap">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
              <span class="mistakes-title">错题回顾</span>
              <span class="mistakes-count">{{ mistakes.length }} 道</span>
            </div>
            <svg class="expand-icon" :class="{ 'expanded': showMistakes }" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          
          <transition name="expand">
            <div v-if="showMistakes" class="mistakes-list">
              <div v-for="(mistake, i) in mistakes" :key="i" class="mistake-item">
                <div class="mistake-question">
                  <span class="question-num">Q{{ i + 1 }}</span>
                  <span class="question-text">{{ mistake.question }}</span>
                </div>
                <div class="mistake-answers">
                  <div class="answer-row wrong">
                    <span class="answer-label">你的答案</span>
                    <span class="answer-value">{{ mistake.userAnswer }}</span>
                  </div>
                  <div class="answer-row correct">
                    <span class="answer-label">正确答案</span>
                    <span class="answer-value">{{ mistake.correctAnswer }}</span>
                  </div>
                </div>
                <button class="btn-explanation" @click.stop="showExplanation(i)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                  <span>查看解析</span>
                </button>
              </div>
            </div>
          </transition>
        </div>

        <!-- 底部操作 -->
        <div class="bottom-actions">
          <button class="btn-neon" @click="addToLearningPath">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>
            加入学习计划
          </button>
          <button class="btn-ghost" @click="retakeAssessment">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg>
            重新挑战
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// ===== 状态 =====
const isLoaded = ref(false)
const isScanning = ref(true)
const isTyping = ref(false)
const showMistakes = ref(false)
const animatedScore = ref(0)
const displayText = ref('')
const hoverIndex = ref(-1)
let typingTimer = null
let scoreTimer = null

// ===== 雷达图维度 =====
const radarDimensions = ['基础语法', '集合框架', '并发编程', 'JVM原理', 'IO流', '设计模式']

// ===== 报告数据（从测评结果接收） =====
const reportData = ref({
  subject: route.query.subject || 'Java',
  date: new Date().toLocaleDateString('zh-CN'),
  scores: generateScoresFromAccuracy(route.query.accuracy),
  level: getLevelFromScore(parseInt(route.query.score) || 65),
  percentile: Math.min(99, Math.max(1, (parseInt(route.query.accuracy) || 65) - 10 + Math.floor(Math.random() * 20))),
  totalScore: parseInt(route.query.score) || 65,
  accuracy: parseInt(route.query.accuracy) || 65,
  correctCount: parseInt(route.query.correctCount) || 0,
  wrongCount: parseInt(route.query.wrongCount) || 0,
  isAdaptive: route.query.adaptive === '1',
  abilityEstimate: parseInt(route.query.abilityEstimate) || 0
})

function generateScoresFromAccuracy(accuracy) {
  const acc = parseInt(accuracy) || 65
  const base = acc / 100
  return [
    Math.min(100, Math.round((base + (Math.random() * 0.2 - 0.1)) * 100)),
    Math.min(100, Math.round((base - 0.15 + (Math.random() * 0.2)) * 100)),
    Math.min(100, Math.round((base - 0.05 + (Math.random() * 0.15)) * 100)),
    Math.min(100, Math.round((base + 0.05 + (Math.random() * 0.1)) * 100)),
    Math.min(100, Math.round((base - 0.1 + (Math.random() * 0.2)) * 100)),
    Math.min(100, Math.round((base + (Math.random() * 0.15 - 0.05)) * 100))
  ]
}

function getLevelFromScore(score) {
  if (score >= 90) return '精通'
  if (score >= 80) return '优秀'
  if (score >= 70) return '良好'
  if (score >= 60) return '中等'
  if (score >= 40) return '一般'
  return '需提升'
}

// ===== 计算属性 =====
const weakCount = computed(() => {
  return reportData.value.scores.filter(s => s < 60).length
})

const topSkills = computed(() => {
  return radarDimensions
    .map((name, i) => ({ name, score: reportData.value.scores[i] }))
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
})

const weakSkills = computed(() => {
  return radarDimensions
    .map((name, i) => ({ name, score: reportData.value.scores[i] }))
    .sort((a, b) => a.score - b.score)
    .slice(0, 3)
})

const standardModelPoints = computed(() => {
  return getHexagonPoints(100)
})

const userScorePoints = computed(() => {
  const points = []
  reportData.value.scores.forEach((score, i) => {
    const angle = (i * 60 - 90) * Math.PI / 180
    const radius = (score / 100) * 125
    points.push(`${150 + Math.cos(angle) * radius},${150 + Math.sin(angle) * radius}`)
  })
  return points.join(' ')
})

const tooltipX = computed(() => {
  if (hoverIndex.value < 0) return 150
  const angle = (hoverIndex.value * 60 - 90) * Math.PI / 180
  return 150 + Math.cos(angle) * (reportData.value.scores[hoverIndex.value] / 100 * 125)
})

const tooltipY = computed(() => {
  if (hoverIndex.value < 0) return 150
  const angle = (hoverIndex.value * 60 - 90) * Math.PI / 180
  return 150 + Math.sin(angle) * (reportData.value.scores[hoverIndex.value] / 100 * 125)
})

// ===== AI诊断文案 =====
const aiDiagnosisText = computed(() => {
  const subject = reportData.value.subject
  return `检测到你在 ${subject} 集合框架方面存在概念混淆。特别是 equals() 和 hashCode() 的契约关系理解有误。这会导致你在使用 HashMap 时遇到严重的性能陷阱。建议重点复习集合框架的底层实现原理，并通过专项练习巩固理解。`
})

// ===== 行动建议 =====
const actionItems = ref([
  { icon: '📚', title: '推荐阅读', desc: '《Java编程思想》第17章 / 知识库文档 Java集合源码分析.md' },
  { icon: '🎯', title: '专项练习', desc: '生成 5 道关于 HashMap 的针对性练习题' },
  { icon: '🛣️', title: '路径调整', desc: '建议在"学习路径"中插入"集合框架进阶"模块' }
])

// ===== 错题数据 =====
const mistakes = ref([
  {
    question: '关于 HashMap 的描述，以下哪项是正确的？',
    userAnswer: 'B. HashMap 的 key 可以重复',
    correctAnswer: 'A. HashMap 允许 null 键和 null 值'
  },
  {
    question: 'equals() 和 hashCode() 的关系是什么？',
    userAnswer: 'C. 两者没有必然联系',
    correctAnswer: 'B. 重写 equals 必须重写 hashCode'
  },
  {
    question: '以下哪个不是 ConcurrentHashMap 的特点？',
    userAnswer: 'D. 允许 null 作为 key',
    correctAnswer: 'C. 使用分段锁实现并发控制'
  }
])

// ===== 方法 =====
function getHexagonPoints(radius) {
  const points = []
  for (let i = 0; i < 6; i++) {
    const angle = (i * 60 - 90) * Math.PI / 180
    points.push(`${150 + Math.cos(angle) * radius},${150 + Math.sin(angle) * radius}`)
  }
  return points.join(' ')
}

function getParticleStyle(i) {
  const size = Math.random() * 3 + 1
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: size + 'px',
    height: size + 'px',
    animationDuration: (Math.random() * 20 + 15) + 's',
    animationDelay: (Math.random() * 10) + 's'
  }
}

function getScoreLevel(score) {
  if (score >= 80) return '优秀'
  if (score >= 60) return '良好'
  if (score >= 40) return '一般'
  return '薄弱'
}

function hoverDimension(i) {
  hoverIndex.value = i
}

function leaveDimension() {
  hoverIndex.value = -1
}

function toggleMistakes() {
  showMistakes.value = !showMistakes.value
}

function showExplanation(index) {
  console.log('显示解析', index)
}

function goBack() {
  router.push('/assessment')
}

function addToLearningPath() {
  router.push({
    path: '/learning-path',
    query: { from: 'diagnosis', subject: reportData.value.subject }
  })
}

function retakeAssessment() {
  router.push('/assessment')
}

// ===== 打字机效果 =====
function startTyping() {
  isTyping.value = true
  const text = aiDiagnosisText.value
  let index = 0
  displayText.value = ''
  
  typingTimer = setInterval(() => {
    if (index < text.length) {
      displayText.value += text[index]
      index++
    } else {
      clearInterval(typingTimer)
      isTyping.value = false
    }
  }, 30)
}

// ===== 分数动画 =====
function animateScore() {
  const target = reportData.value.totalScore
  const duration = 1500
  const start = performance.now()
  
  scoreTimer = requestAnimationFrame(function step(t) {
    const p = Math.min((t - start) / duration, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    animatedScore.value = Math.round(target * ease)
    
    if (p < 1) {
      scoreTimer = requestAnimationFrame(step)
    }
  })
}

// ===== 生命周期 =====
onMounted(() => {
  setTimeout(() => {
    isScanning.value = false
    isLoaded.value = true
    animateScore()
    startTyping()
  }, 800)
})

onUnmounted(() => {
  if (typingTimer) clearInterval(typingTimer)
  if (scoreTimer) cancelAnimationFrame(scoreTimer)
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.diagnosis-report-page {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

/* ===== 背景层 ===== */
.bg-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.bg-aurora {
  position: absolute;
  inset: 0;
}

.aurora-layer {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  
  &.a1 {
    width: 700px;
    height: 700px;
    top: -250px;
    right: -150px;
    background: radial-gradient(circle, rgba($accent-primary, 0.1) 0%, transparent 70%);
    animation: auroraFloat 20s ease-in-out infinite;
  }
  
  &.a2 {
    width: 600px;
    height: 600px;
    bottom: -200px;
    left: -150px;
    background: radial-gradient(circle, rgba($accent-secondary, 0.08) 0%, transparent 70%);
    animation: auroraFloat 25s ease-in-out infinite reverse;
  }
}

@keyframes auroraFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -20px) scale(1.05); }
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba($accent-primary, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba($accent-primary, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
}

.bg-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: $accent-primary;
  animation: particleFloat linear infinite;
}

@keyframes particleFloat {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.3; }
  90% { opacity: 0.15; }
  100% { transform: translateY(-100vh) translateX(80px); opacity: 0; }
}

/* ===== 顶部导航 ===== */
.report-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 32px;
  background: rgba($bg-primary, 0.88);
  backdrop-filter: blur(24px);
  border-bottom: 1px solid rgba($accent-secondary, 0.08);
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: rgba(255,255,255,0.5);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    color: $accent-primary;
    background: rgba($accent-primary, 0.06);
  }
}

.header-center {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title { @include page-title-base; }

.subject-badge {
  padding: 3px 10px;
  background: rgba($accent-primary, 0.1);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 12px;
  font-size: 0.72rem;
  font-weight: 600;
  color: $accent-primary;
}

.adaptive-badge {
  padding: 3px 10px;
  background: rgba($accent-secondary, 0.15);
  border: 1px solid rgba($accent-secondary, 0.3);
  border-radius: 12px;
  font-size: 0.72rem;
  font-weight: 600;
  color: $accent-secondary;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.8; }
  50% { opacity: 1; }
}

.header-right {
  text-align: right;
}

.report-date {
  font-size: 0.78rem;
  color: rgba(255,255,255,0.3);
}

/* ===== 主内容区 ===== */
.report-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  max-width: 1500px;
  margin: 0 auto;
  padding: 32px;
  position: relative;
  z-index: 1;
  min-height: calc(100vh - 60px);
}

.glass-card {
  background: rgba(15,15,25,0.5);
  backdrop-filter: blur(24px);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 16px;
  padding: 24px;
}

/* ===== 左侧：雷达图 ===== */
.radar-section {
  display: flex;
  align-items: center;
  justify-content: center;
}

.radar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.radar-chart-wrap {
  position: relative;
  width: 100%;
  max-width: 480px;
  aspect-ratio: 1;
}

.radar-svg {
  width: 100%;
  height: 100%;
}

.grid-line {
  animation: rippleIn 0.8s ease forwards;
  opacity: 0;
}

@keyframes rippleIn {
  0% { opacity: 0; transform-origin: center; transform: scale(0.5); }
  100% { opacity: 1; transform-origin: center; transform: scale(1); }
}

.standard-model {
  opacity: 0;
  transition: opacity 1s ease 0.5s;
  
  &.visible { opacity: 1; }
}

.user-score {
  opacity: 0;
  transition: opacity 1s ease 0.8s;
  
  &.visible { opacity: 1; }
}

.data-point {
  animation: pointPop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  opacity: 0;
  transition: r 0.2s ease, fill 0.2s ease;
  
  &.hovered {
    filter: drop-shadow(0 0 8px rgba($accent-primary, 0.6));
  }
}

@keyframes pointPop {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 1; }
}

.dim-label {
  transition: fill 0.2s ease, font-size 0.2s ease;
}

.dim-group {
  cursor: pointer;
}

.tooltip-group {
  animation: tooltipFadeIn 0.15s ease;
}

@keyframes tooltipFadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.radar-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.score-value {
  display: block;
  font-size: 3.5rem;
  font-weight: 900;
  font-family: 'JetBrains Mono', monospace;
  background: linear-gradient(135deg, #a78bfa 0%, #06b6d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  
  &.animate {
    filter: drop-shadow(0 0 20px rgba($accent-primary, 0.4));
  }
}

.score-label {
  font-size: 0.78rem;
  color: rgba(255,255,255,0.35);
  margin-top: 6px;
}

.radar-scan-line {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 45%;
  height: 2px;
  background: linear-gradient(90deg, rgba($accent-primary, 0.6), transparent);
  transform-origin: left center;
  opacity: 0;
  
  &.active {
    animation: radarScan 2.5s linear infinite;
  }
}

@keyframes radarScan {
  0% { transform: rotate(0deg); opacity: 0.6; }
  100% { transform: rotate(360deg); opacity: 0.6; }
}

/* 统计卡片 */
.radar-stats {
  display: flex;
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  background: rgba(15,15,25,0.4);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
  min-width: 100px;
  
  &.highlight {
    border-color: rgba($accent-primary, 0.2);
    background: rgba($accent-primary, 0.05);
  }
}

.stat-value {
  font-size: 1.2rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  color: $accent-primary;
  
  &.correct {
    color: $accent-emerald;
    margin-right: 8px;
  }
  
  &.wrong {
    color: $accent-red;
  }
}

.stat-label {
  font-size: 0.68rem;
  color: rgba(255,255,255,0.35);
  margin-top: 2px;
}

/* 图例 */
.radar-legend {
  display: flex;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
  color: rgba(255,255,255,0.4);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  
  &.mine {
    background: linear-gradient(135deg, #8b5cf6, #06b6d4);
  }
}

.legend-line {
  width: 16px;
  height: 0;
  border-top: 2px dashed rgba(255,255,255,0.2);
}

/* ===== 右侧：分析区 ===== */
.analysis-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 强弱项对比 */
.compare-card {
  padding: 20px;
}

.compare-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 20px;
  align-items: start;
}

.compare-divider {
  width: 1px;
  height: 100%;
  min-height: 120px;
  background: rgba($accent-secondary, 0.1);
}

.compare-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.compare-title {
  font-size: 0.88rem;
  font-weight: 600;
  color: $text-primary;
}

.compare-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compare-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-name {
  font-size: 0.82rem;
  color: $text-secondary;
}

.item-score-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-track {
  flex: 1;
  height: 5px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 1.2s cubic-bezier(0.4, 0, 0.2, 1);
  
  &.strength {
    background: linear-gradient(90deg, #00E676, #69F0AE);
    box-shadow: 0 0 8px rgba(0, 230, 118, 0.3);
  }
  
  &.weakness {
    background: linear-gradient(90deg, #FF4D4F, #FF7875);
    box-shadow: 0 0 8px rgba(255, 77, 79, 0.3);
  }
}

.item-score {
  font-size: 0.75rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  min-width: 35px;
  text-align: right;
  
  .strength & { color: #00E676; }
  .weakness & { color: #FF4D4F; }
}

/* AI 诊断 */
.ai-card {
  padding: 20px;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.ai-avatar-wrap {
  position: relative;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.avatar-glow {
  position: absolute;
  inset: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.25), rgba($accent-secondary, 0.15));
  animation: avatarGlow 2s ease-in-out infinite;
}

@keyframes avatarGlow {
  0%, 100% { box-shadow: 0 0 16px rgba($accent-primary, 0.2); }
  50% { box-shadow: 0 0 24px rgba($accent-primary, 0.4); }
}

.avatar-emoji {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 1.3rem;
}

.ai-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-title {
  font-size: 0.92rem;
  font-weight: 600;
  color: $text-primary;
}

.ai-badge {
  padding: 2px 8px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.12), rgba($accent-secondary, 0.08));
  border-radius: 10px;
  font-size: 0.65rem;
  font-weight: 600;
  color: $accent-primary;
}

.ai-body {
  margin-bottom: 16px;
}

.ai-text {
  font-size: 0.88rem;
  line-height: 1.75;
  color: rgba(255,255,255,0.5);
  padding: 14px 16px;
  background: rgba($accent-primary, 0.03);
  border: 1px solid rgba($accent-primary, 0.08);
  border-radius: 10px;
  
  &.typing { color: rgba(255,255,255,0.7); }
}

.cursor {
  animation: blink 0.8s infinite;
  color: $accent-primary;
  font-weight: 300;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(15,15,25,0.3);
  border: 1px solid rgba($accent-secondary, 0.06);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  animation: slideIn 0.4s ease forwards;
  opacity: 0;
  
  &:hover {
    border-color: rgba($accent-primary, 0.15);
    background: rgba($accent-primary, 0.04);
    transform: translateX(4px);
  }
}

.action-icon {
  font-size: 1.1rem;
}

.action-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.action-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: $text-primary;
}

.action-desc {
  font-size: 0.72rem;
  color: rgba(255,255,255,0.35);
}

.action-arrow {
  color: rgba(255,255,255,0.2);
  transition: all 0.2s;
  
  .action-item:hover & {
    transform: translateX(3px);
    color: $accent-primary;
  }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}

/* 错题回顾 */
.mistakes-card {
  padding: 0;
  overflow: hidden;
}

.mistakes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;
  
  &:hover { background: rgba($accent-primary, 0.03); }
}

.mistakes-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  color: $text-secondary;
}

.mistakes-title {
  font-size: 0.88rem;
  font-weight: 600;
}

.mistakes-count {
  font-size: 0.72rem;
  padding: 2px 8px;
  background: rgba(255, 77, 79, 0.08);
  border-radius: 8px;
  color: #FF4D4F;
}

.expand-icon {
  color: rgba(255,255,255,0.3);
  transition: transform 0.3s;
  
  &.expanded { transform: rotate(180deg); }
}

.expand-enter-active, .expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from, .expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to, .expand-leave-from {
  opacity: 1;
  max-height: 600px;
}

.mistakes-list {
  padding: 0 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mistake-item {
  padding: 14px;
  background: rgba(15,15,25,0.3);
  border: 1px solid rgba($accent-secondary, 0.06);
  border-radius: 10px;
}

.mistake-question {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.question-num {
  font-size: 0.7rem;
  font-weight: 700;
  color: $accent-primary;
  background: rgba($accent-primary, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

.question-text {
  font-size: 0.84rem;
  color: $text-primary;
  line-height: 1.5;
}

.mistake-answers {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.answer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  
  &.wrong {
    background: rgba(255, 77, 79, 0.05);
    border: 1px solid rgba(255, 77, 79, 0.1);
  }
  
  &.correct {
    background: rgba(0, 230, 118, 0.05);
    border: 1px solid rgba(0, 230, 118, 0.1);
  }
}

.answer-label {
  font-size: 0.68rem;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 3px;
  flex-shrink: 0;
  
  .wrong & {
    background: rgba(255, 77, 79, 0.12);
    color: #FF4D4F;
  }
  
  .correct & {
    background: rgba(0, 230, 118, 0.12);
    color: #00E676;
  }
}

.answer-value {
  font-size: 0.8rem;
  color: $text-secondary;
}

.btn-explanation {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid rgba($accent-primary, 0.12);
  border-radius: 6px;
  color: $accent-primary;
  font-size: 0.78rem;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background: rgba($accent-primary, 0.08);
    border-color: rgba($accent-primary, 0.2);
  }
}

/* 底部按钮 */
.bottom-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}

.btn-neon, .btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: inherit;
}

.btn-neon {
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.25);
  color: $accent-indigo;
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
  }
}

.btn-ghost {
  background: rgba($accent-indigo, 0.06);
  border: 1px solid rgba($accent-indigo, 0.12);
  color: $text-secondary;
  
  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    color: $accent-indigo-light;
    background: rgba($accent-indigo, 0.1);
  }
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .report-content {
    grid-template-columns: 1fr;
    padding: 24px 20px;
  }
  
  .radar-section {
    order: -1;
  }
  
  .radar-chart-wrap {
    max-width: 360px;
  }
  
  .compare-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .compare-divider {
    width: 100%;
    height: 1px;
    min-height: 0;
  }
}

@media (max-width: 640px) {
  .report-header {
    padding: 12px 16px;
  }
  
  .report-content {
    padding: 16px;
    gap: 16px;
  }
  
  .radar-chart-wrap {
    max-width: 280px;
  }
  
  .radar-stats {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .bottom-actions {
    flex-direction: column;
  }
  
  .btn-neon, .btn-ghost {
    width: 100%;
    justify-content: center;
  }
}
</style>
