<template>
  <div class="detail-page">
    <!-- 顶部导航栏 -->
    <header class="detail-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <ArrowLeft :size="20" />
          <span>返回</span>
        </button>
        <h1 class="page-title">
          <span class="title-glyph">🔍</span>
          <span>智能诊断</span>
        </h1>
      </div>
      <button class="btn-action" @click="retakeAssessment">
        <RefreshCw :size="16" />
        重新测评
      </button>
    </header>

    <div class="detail-content">
      <!-- 能力概览 -->
      <section class="info-section">
        <div class="section-label">
          <BarChart3 :size="16" />
          能力概览
        </div>
        <div class="overview-grid">
          <div class="overview-item">
            <span class="overview-value">{{ overviewData.mastery }}%</span>
            <span class="overview-label">整体掌握度</span>
          </div>
          <div class="overview-item">
            <span class="overview-value">{{ overviewData.nodes }}</span>
            <span class="overview-label">已测评知识节点</span>
          </div>
          <div class="overview-item">
            <span class="overview-value weakness">{{ overviewData.weakness }}</span>
            <span class="overview-label">薄弱知识点</span>
          </div>
          <div class="overview-item">
            <span class="overview-value date">{{ overviewData.lastAssess }}</span>
            <span class="overview-label">上次测评</span>
          </div>
        </div>
      </section>

      <!-- 能力雷达图 -->
      <section class="info-section">
        <div class="section-label">
          <PieChart :size="16" />
          能力雷达图
        </div>
        <div ref="radarChartRef" class="chart-container"></div>
      </section>

      <!-- 薄弱知识点分析 -->
      <section class="info-section">
        <div class="section-label">
          <AlertTriangle :size="16" />
          薄弱知识点分析
        </div>
        <div class="weakness-list">
          <div v-for="(item, index) in weaknessData" :key="index" class="weakness-item">
            <div class="weakness-header">
              <span class="weakness-name">{{ item.name }}</span>
              <span class="weakness-badge" :class="item.level">{{ item.level === 'danger' ? '需加强' : '待提升' }}</span>
            </div>
            <div class="weakness-progress">
              <div class="progress-track">
                <div class="progress-fill" :class="item.level" :style="{ width: item.percentage + '%' }"></div>
              </div>
              <span class="progress-text">{{ item.percentage }}%</span>
            </div>
          </div>
        </div>
        <button class="link-btn" @click="viewAllWeakness">
          查看全部薄弱点
          <ArrowRight :size="14" />
        </button>
      </section>

      <!-- 学习建议 -->
      <section class="info-section">
        <div class="section-label">
          <Lightbulb :size="16" />
          学习建议
        </div>
        <div class="advice-content">
          <p class="advice-intro">基于您的薄弱知识点，建议优先学习：</p>
          <div v-for="(advice, index) in adviceData" :key="index" class="advice-item">
            <span class="advice-num">{{ index + 1 }}</span>
            <div class="advice-info">
              <span class="advice-name">{{ advice.name }}</span>
              <span class="advice-course">推荐课程：{{ advice.course }}</span>
            </div>
          </div>
        </div>
        <button class="link-btn" @click="viewRecommendedPath">
          查看推荐路径
          <ArrowRight :size="14" />
        </button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  ArrowLeft, Search, RefreshCw, BarChart3, PieChart,
  AlertTriangle, Lightbulb, ArrowRight
} from 'lucide-vue-next'

const router = useRouter()
const radarChartRef = ref(null)
let radarChart = null

// 模拟数据 - 待替换为真实 API 调用
const overviewData = {
  mastery: 85,
  nodes: 38,
  weakness: 5,
  lastAssess: '2026-07-15'
}

const weaknessData = [
  { name: 'Python 面向对象编程', percentage: 45, level: 'danger' },
  { name: '机器学习调参技巧', percentage: 52, level: 'warning' },
  { name: '数据库索引优化', percentage: 48, level: 'danger' },
  { name: 'RESTful API 设计', percentage: 55, level: 'warning' },
  { name: '数据结构与算法', percentage: 60, level: 'warning' }
]

const adviceData = [
  { name: 'Python 面向对象编程', course: 'Python 进阶' },
  { name: '机器学习调参技巧', course: '机器学习实战' }
]

const goBack = () => {
  router.push('/home')
}

const retakeAssessment = () => {
  router.push('/assessment')
}

const viewAllWeakness = () => {
  // P0：跳转薄弱知识点专项页面（不再跳转通用路径列表）
  router.push('/capability/weakness')
}

const viewRecommendedPath = () => {
  // P0：携带薄弱点上下文跳转路径列表，列表页展示针对性推荐
  const subjects = adviceData.map(a => a.name).join(',')
  router.push({
    path: '/learning-path',
    query: { from: 'weakness', subjects }
  })
}

onMounted(() => {
  if (radarChartRef.value) {
    radarChart = echarts.init(radarChartRef.value)
    const option = {
      backgroundColor: 'transparent',
      radar: {
        indicator: [
          { name: 'Python基础', max: 100 },
          { name: '数据分析', max: 100 },
          { name: '机器学习', max: 100 },
          { name: 'Web开发', max: 100 },
          { name: '数据库', max: 100 },
          { name: '算法', max: 100 }
        ],
        shape: 'circle',
        splitNumber: 4,
        axisName: {
          color: '#94A3B8',
          fontSize: 12
        },
        splitLine: {
          lineStyle: {
            color: 'rgba(0, 229, 255, 0.1)'
          }
        },
        splitArea: {
          areaStyle: {
            color: ['rgba(0, 229, 255, 0.02)', 'rgba(0, 229, 255, 0.01)']
          }
        },
        axisLine: {
          lineStyle: {
            color: 'rgba(0, 229, 255, 0.15)'
          }
        }
      },
      series: [{
        type: 'radar',
        data: [{
          value: [85, 72, 55, 60, 48, 62],
          name: '当前能力',
          areaStyle: {
            color: 'rgba(0, 229, 255, 0.15)'
          },
          lineStyle: {
            color: '#00E5FF',
            width: 2
          },
          itemStyle: {
            color: '#00E5FF'
          }
        }]
      }]
    }
    radarChart.setOption(option)
  }
})

onUnmounted(() => {
  if (radarChart) {
    radarChart.dispose()
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

/* ===== 动态背景 ===== */
.bg-dynamic {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.bg-particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.particle {
  position: absolute;
  width: 3px;
  height: 3px;
  background: $accent-cyan;
  border-radius: 50%;
  box-shadow: 0 0 10px rgba($accent-cyan, 0.5);
  animation: particle-float 20s linear infinite;
}
.particle:nth-child(odd) {
  background: $accent-indigo;
  box-shadow: 0 0 10px rgba($accent-indigo, 0.5);
}
.particle:nth-child(1) { left: 5%; animation-delay: 0s; animation-duration: 18s; }
.particle:nth-child(2) { left: 15%; animation-delay: -3s; animation-duration: 22s; }
.particle:nth-child(3) { left: 25%; animation-delay: -7s; animation-duration: 25s; }
.particle:nth-child(4) { left: 35%; animation-delay: -1s; animation-duration: 20s; }
.particle:nth-child(5) { left: 45%; animation-delay: -10s; animation-duration: 23s; }
.particle:nth-child(6) { left: 55%; animation-delay: -5s; animation-duration: 19s; }
.particle:nth-child(7) { left: 65%; animation-delay: -12s; animation-duration: 26s; }
.particle:nth-child(8) { left: 75%; animation-delay: -8s; animation-duration: 21s; }
.particle:nth-child(9) { left: 85%; animation-delay: -2s; animation-duration: 24s; }
.particle:nth-child(10) { left: 92%; animation-delay: -15s; animation-duration: 17s; }
@keyframes particle-float {
  0% { transform: translateY(100vh); opacity: 0; }
  10% { opacity: 0.6; }
  90% { opacity: 0.6; }
  100% { transform: translateY(-100px); opacity: 0; }
}
.aurora-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  animation: aurora-float 25s ease-in-out infinite;
}
.orb-1 {
  width: 600px; height: 600px;
  top: -200px; right: -150px;
  background: radial-gradient(circle, rgba($accent-indigo, 0.12) 0%, transparent 70%);
}
.orb-2 {
  width: 500px; height: 500px;
  bottom: -200px; left: -150px;
  background: radial-gradient(circle, rgba($accent-cyan, 0.08) 0%, transparent 70%);
  animation-delay: -8s;
}
.orb-3 {
  width: 400px; height: 400px;
  top: 40%; left: 50%;
  background: radial-gradient(circle, rgba($accent-violet, 0.06) 0%, transparent 70%);
  animation-delay: -16s;
}
@keyframes aurora-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.1); }
  50% { transform: translate(-20px, 20px) scale(0.9); }
  75% { transform: translate(20px, 10px) scale(1.05); }
}

.detail-page {
  min-height: 100vh;
  background: $bg-base;
  padding-bottom: 80px;
  position: relative;
  margin: -#{$space-6};
  padding: #{$space-6} #{$space-6} 80px #{$space-6};
}
.detail-page > *:not(.bg-dynamic) {
  position: relative;
  z-index: 1;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 48px;
  background: rgba($bg-base, 0.7);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba($accent-indigo, 0.12);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-back:hover {
  color: $accent-primary;
  border-color: rgba($accent-primary, 0.2);
  background: rgba($accent-primary, 0.04);
}

.page-title { @include page-title-base; }

.header-icon {
  color: $accent-primary;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: rgba($accent-indigo, 0.1);
  color: $accent-indigo;
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: inherit;

  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
}

.detail-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-section {
  background: rgba($bg-surface, 0.65);
  backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: 16px;
  padding: 28px;
  box-shadow: $shadow-sm;
  transition: all 0.3s ease;
}

.info-section:hover {
  border-color: rgba($accent-indigo, 0.25);
  box-shadow: $shadow-md, 0 0 20px rgba($accent-indigo, 0.06);
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-indigo, 0.08);
}

.section-label svg {
  color: $accent-primary;
}

/* 概览网格 */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.overview-item {
  text-align: center;
  padding: 20px 16px;
  background: rgba($accent-indigo, 0.04);
  border: 1px solid rgba($accent-indigo, 0.08);
  border-radius: 12px;
}

.overview-value {
  display: block;
  font-size: 2.2rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  background: linear-gradient(135deg, $accent-primary 0%, #38BDF8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 15px rgba($accent-primary, 0.3));
  margin-bottom: 6px;
}

.overview-value.weakness {
  background: linear-gradient(135deg, #EF4444 0%, #F59E0B 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 15px rgba(239, 68, 68, 0.3));
}

.overview-value.date {
  font-size: 1.1rem;
  background: linear-gradient(135deg, $text-secondary 0%, #CBD5E1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: none;
}

.overview-label {
  font-size: 0.85rem;
  color: $text-secondary;
}

/* 雷达图 */
.chart-container {
  width: 100%;
  height: 380px;
}

/* 薄弱知识点 */
.weakness-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.weakness-item {
  padding: 4px 0;
}

.weakness-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.weakness-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: $text-primary;
}

.weakness-badge {
  font-size: 0.75rem;
  padding: 2px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.weakness-badge.danger {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.weakness-badge.warning {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.weakness-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 1s ease;
}

.progress-fill.danger {
  background: linear-gradient(90deg, #EF4444, #DC2626);
}

.progress-fill.warning {
  background: linear-gradient(90deg, #F59E0B, #D97706);
}

.progress-text {
  font-size: 0.85rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: $text-secondary;
  min-width: 40px;
  text-align: right;
}

/* 学习建议 */
.advice-content {
  margin-bottom: 16px;
}

.advice-intro {
  font-size: 0.9rem;
  color: $text-secondary;
  margin-bottom: 16px;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: rgba($accent-indigo, 0.04);
  border: 1px solid rgba($accent-indigo, 0.08);
  border-radius: 10px;
  margin-bottom: 10px;
}

.advice-num {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-primary, 0.1);
  color: $accent-primary;
  border-radius: 50%;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}

.advice-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.advice-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: $text-primary;
}

.advice-course {
  font-size: 0.8rem;
  color: $accent-primary;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-indigo, 0.06);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: 8px;
  color: $accent-primary;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.link-btn:hover {
  background: rgba($accent-indigo, 0.1);
  border-color: rgba($accent-indigo, 0.25);
  transform: translateY(-1px);
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 12px;
  }
  .header-left {
    width: 100%;
  }
  .btn-action {
    width: 100%;
    justify-content: center;
  }
  .detail-content {
    padding: 20px;
  }
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>