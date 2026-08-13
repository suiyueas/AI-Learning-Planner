<template>
  <div class="detail-page">
    <!-- 顶部导航栏 -->
    <header class="detail-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <ArrowLeft :size="20" />
          <span>返回</span>
        </button>
        <h1 class="header-title">
          <Search :size="24" class="header-icon" />
          智能诊断
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

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #0a0a1a;
  padding-bottom: 80px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 48px;
  background: rgba(17, 17, 39, 0.8);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(100, 100, 180, 0.12);
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
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 8px;
  color: #94A3B8;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-back:hover {
  color: #00E5FF;
  border-color: rgba(0, 229, 255, 0.2);
  background: rgba(0, 229, 255, 0.04);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.4rem;
  font-weight: 700;
  color: #F1F5F9;
  margin: 0;
}

.header-icon {
  color: #00E5FF;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.15), rgba(0, 85, 255, 0.1));
  color: #00E5FF;
  border: 1px solid rgba(0, 229, 255, 0.25);
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 24px rgba(0, 229, 255, 0.15);
  border-color: rgba(0, 229, 255, 0.4);
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
  background: rgba(17, 17, 39, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 16px;
  padding: 28px;
  transition: all 0.3s ease;
}

.info-section:hover {
  border-color: rgba(0, 229, 255, 0.15);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #F1F5F9;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
}

.section-label svg {
  color: #00E5FF;
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
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 12px;
}

.overview-value {
  display: block;
  font-size: 2.2rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  background: linear-gradient(135deg, #00E5FF 0%, #38BDF8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 15px rgba(0, 229, 255, 0.3));
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
  background: linear-gradient(135deg, #94A3B8 0%, #CBD5E1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: none;
}

.overview-label {
  font-size: 0.85rem;
  color: #94A3B8;
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
  color: #F1F5F9;
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
  background: rgba(100, 100, 180, 0.06);
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
  color: #94A3B8;
  min-width: 40px;
  text-align: right;
}

/* 学习建议 */
.advice-content {
  margin-bottom: 16px;
}

.advice-intro {
  font-size: 0.9rem;
  color: #94A3B8;
  margin-bottom: 16px;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 10px;
  margin-bottom: 10px;
}

.advice-num {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 229, 255, 0.1);
  color: #00E5FF;
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
  color: #F1F5F9;
}

.advice-course {
  font-size: 0.8rem;
  color: #00E5FF;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(0, 229, 255, 0.06);
  border: 1px solid rgba(0, 229, 255, 0.12);
  border-radius: 8px;
  color: #00E5FF;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.link-btn:hover {
  background: rgba(0, 229, 255, 0.1);
  border-color: rgba(0, 229, 255, 0.25);
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