<template>
  <div class="statistics-page">
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
    </div>

    <header class="page-header">
      <button class="back-btn" @click="goBack">
        <ArrowLeft :size="20" />
        <span>返回</span>
      </button>
      <h1 class="page-title">
        <BarChart3 :size="24" class="title-icon" />
        <span class="title-text">学习统计</span>
      </h1>
      <div class="header-actions">
        <el-select v-model="timeRange" size="default" class="time-select">
          <el-option label="近7天" value="7" />
          <el-option label="近30天" value="30" />
        </el-select>
      </div>
    </header>

    <div class="page-content">
      <section class="overview-section">
        <div class="section-label">
          <LayoutDashboard :size="16" />
          概览
        </div>
        <div class="overview-grid">
          <div class="overview-card glass-card">
            <div class="overview-icon streak-icon">
              <Flame :size="24" />
            </div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.streak }}</span>
              <span class="overview-unit">天</span>
            </div>
            <span class="overview-label">连续学习</span>
          </div>
          <div class="overview-card glass-card">
            <div class="overview-icon hours-icon">
              <Clock :size="24" />
            </div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.totalHours }}</span>
              <span class="overview-unit">h</span>
            </div>
            <span class="overview-label">总学时</span>
          </div>
          <div class="overview-card glass-card">
            <div class="overview-icon completed-icon">
              <CheckCircle :size="24" />
            </div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.completed }}</span>
              <span class="overview-unit">个</span>
            </div>
            <span class="overview-label">已完成节点</span>
          </div>
          <div class="overview-card glass-card">
            <div class="overview-icon score-icon">
              <Star :size="24" />
            </div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.avgScore }}</span>
              <span class="overview-unit">分</span>
            </div>
            <span class="overview-label">平均分</span>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <div class="section-label">
          <TrendingUp :size="16" />
          学习趋势
        </div>
        <div class="chart-card glass-card">
          <div ref="lineChartRef" class="chart-container"></div>
        </div>
      </section>

      <section class="chart-section">
        <div class="section-label">
          <Radar :size="16" />
          能力雷达图
        </div>
        <div class="chart-card glass-card">
          <div ref="radarChartRef" class="chart-container radar-container"></div>
        </div>
      </section>

      <section class="records-section">
        <div class="section-label">
          <List :size="16" />
          学习记录
        </div>
        <div class="records-card glass-card">
          <div v-if="recentRecords.length === 0" class="empty-state">
            <BookOpen :size="48" class="empty-icon" />
            <p>暂无学习记录</p>
          </div>
          <div v-else class="records-list">
            <div v-for="(record, index) in recentRecords" :key="index" class="record-item">
              <div class="record-date">
                <span class="record-day">{{ formatDate(record.date) }}</span>
              </div>
              <div class="record-info">
                <span class="record-title">{{ record.title }}</span>
                <span class="record-duration">{{ record.duration }}min</span>
              </div>
              <div class="record-status" :class="record.status">
                <CheckCircle v-if="record.status === 'completed'" :size="16" class="status-icon completed" />
                <XCircle v-else :size="16" class="status-icon failed" />
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  ArrowLeft, BarChart3, LayoutDashboard, TrendingUp, Radar,
  Clock, Flame, CheckCircle, Star, XCircle, List, BookOpen
} from 'lucide-vue-next'

const router = useRouter()
const lineChartRef = ref(null)
const radarChartRef = ref(null)
let lineChart = null
let radarChart = null
const timeRange = ref('7')

// 模拟数据 - 待对接真实 API
const overviewData = ref({
  streak: 45,
  totalHours: 156,
  completed: 28,
  avgScore: 85
})

const trendData = ref({
  dates: ['7月12日', '7月13日', '7月14日', '7月15日', '7月16日', '7月17日', '7月18日'],
  hours: [2.5, 3.0, 1.5, 2.0, 3.5, 2.0, 1.0]
})

const radarData = ref({
  indicators: [
    { name: 'Python 基础', max: 100 },
    { name: '数据分析', max: 100 },
    { name: '机器学习', max: 100 },
    { name: 'Web 开发', max: 100 },
    { name: '算法思维', max: 100 },
    { name: '工程实践', max: 100 }
  ],
  values: [85, 72, 55, 60, 68, 45]
})

const recentRecords = ref([
  { date: '2026-07-18', title: '完成 Pandas 分组聚合', duration: 45, status: 'completed' },
  { date: '2026-07-17', title: '完成 NumPy 数组操作', duration: 60, status: 'completed' },
  { date: '2026-07-16', title: '完成 Matplotlib 数据可视化', duration: 90, status: 'completed' },
  { date: '2026-07-15', title: '未完成 数据可视化报告', duration: 0, status: 'failed' }
])

const goBack = () => {
  router.push('/home')
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const initLineChart = () => {
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(17, 17, 39, 0.9)',
      borderColor: 'rgba(0, 229, 255, 0.2)',
      textStyle: { color: '#F1F5F9' },
      formatter: (params) => {
        const data = params[0]
        return `${data.name}<br/><span style="color:#00E5FF">学习时长: ${data.value}h</span>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '20px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.dates,
      axisLine: { lineStyle: { color: 'rgba(100, 100, 180, 0.2)' } },
      axisLabel: { color: '#94A3B8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '学习时长 (h)',
      nameTextStyle: { color: '#94A3B8', fontSize: 11 },
      axisLine: { show: false },
      axisLabel: { color: '#94A3B8' },
      splitLine: { lineStyle: { color: 'rgba(100, 100, 180, 0.06)' } }
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        color: '#00E5FF',
        width: 3,
        shadowColor: 'rgba(0, 229, 255, 0.5)',
        shadowBlur: 10
      },
      itemStyle: {
        color: '#00E5FF',
        borderColor: '#fff',
        borderWidth: 2
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0, 229, 255, 0.3)' },
            { offset: 1, color: 'rgba(0, 229, 255, 0.02)' }
          ]
        }
      },
      data: trendData.value.hours
    }]
  }
  lineChart.setOption(option)
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  radarChart = echarts.init(radarChartRef.value)
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(17, 17, 39, 0.9)',
      borderColor: 'rgba(0, 229, 255, 0.2)',
      textStyle: { color: '#F1F5F9' }
    },
    radar: {
      indicator: radarData.value.indicators,
      shape: 'polygon',
      splitNumber: 4,
      axisName: {
        color: '#94A3B8',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(0, 229, 255, 0.1)',
          width: 1
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(0, 229, 255, 0.02)', 'rgba(0, 229, 255, 0.05)', 'rgba(0, 229, 255, 0.08)', 'rgba(0, 229, 255, 0.12)']
        }
      },
      axisLine: {
        lineStyle: { color: 'rgba(0, 229, 255, 0.2)' }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarData.value.values,
        name: '能力评估',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#00E5FF',
          width: 2
        },
        areaStyle: {
          color: 'rgba(0, 229, 255, 0.2)'
        },
        itemStyle: {
          color: '#00E5FF',
          borderColor: '#fff',
          borderWidth: 2
        }
      }]
    }]
  }
  radarChart.setOption(option)
}

const handleResize = () => {
  lineChart?.resize()
  radarChart?.resize()
}

watch(timeRange, (newVal) => {
  if (newVal === '30') {
    trendData.value = {
      dates: ['6月19日', '6月26日', '7月3日', '7月10日', '7月17日', '7月24日', '7月31日'],
      hours: [12, 18, 15, 22, 25, 20, 18]
    }
  } else {
    trendData.value = {
      dates: ['7月12日', '7月13日', '7月14日', '7月15日', '7月16日', '7月17日', '7月18日'],
      hours: [2.5, 3.0, 1.5, 2.0, 3.5, 2.0, 1.0]
    }
  }
  initLineChart()
})

onMounted(() => {
  initLineChart()
  initRadarChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  radarChart?.dispose()
})
</script>

<style lang="scss" scoped>
.statistics-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding-bottom: 40px;
  animation: fadeIn 0.5s ease;
}

.bg-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -1;
}

.bg-aurora {
  position: absolute;
  inset: 0;
}

.aurora-layer {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}

.a1 {
  width: 500px;
  height: 500px;
  top: -150px;
  right: -100px;
  background: radial-gradient(circle, rgba(0, 229, 255, 0.1) 0%, transparent 70%);
  animation: aurora 18s ease-in-out infinite;
}

.a2 {
  width: 400px;
  height: 400px;
  bottom: -100px;
  left: -100px;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.08) 0%, transparent 70%);
  animation: aurora 18s ease-in-out infinite;
  animation-delay: -6s;
}

.a3 {
  width: 300px;
  height: 300px;
  top: 40%;
  left: 30%;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.06) 0%, transparent 70%);
  animation: aurora 18s ease-in-out infinite;
  animation-delay: -12s;
}

@keyframes aurora {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -20px) scale(1.1); }
  66% { transform: translate(-15px, 15px) scale(0.95); }
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 229, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 229, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 48px;
  position: relative;
  z-index: 10;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: #E2E8F0;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(0, 229, 255, 0.1);
    border-color: rgba(0, 229, 255, 0.3);
    color: #00E5FF;
  }
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 600;
  color: #F1F5F9;
  margin: 0;

  .title-icon {
    color: #00E5FF;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-select {
  width: 120px;

  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: none;

    &:hover, &:focus {
      border-color: rgba(0, 229, 255, 0.3);
    }
  }

  :deep(.el-input__inner) {
    color: #E2E8F0;
  }
}

.page-content {
  padding: 0 48px 40px;
  position: relative;
  z-index: 1;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #00E5FF;
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(0, 229, 255, 0.2);
  }
}

.overview-section {
  margin-bottom: 32px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.overview-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;

  &.streak-icon {
    background: linear-gradient(135deg, rgba(255, 107, 107, 0.2), rgba(255, 159, 67, 0.2));
    color: #FF6B6B;
  }

  &.hours-icon {
    background: linear-gradient(135deg, rgba(0, 229, 255, 0.2), rgba(0, 133, 255, 0.2));
    color: #00E5FF;
  }

  &.completed-icon {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(0, 229, 255, 0.2));
    color: #10B981;
  }

  &.score-icon {
    background: linear-gradient(135deg, rgba(168, 85, 247, 0.2), rgba(255, 159, 67, 0.2));
    color: #A855F7;
  }
}

.overview-data {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.overview-value {
  font-size: 36px;
  font-weight: 700;
  color: #F1F5F9;
}

.overview-unit {
  font-size: 14px;
  color: #94A3B8;
}

.overview-label {
  font-size: 13px;
  color: #94A3B8;
}

.chart-section {
  margin-bottom: 32px;
}

.chart-card {
  padding: 24px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.radar-container {
  height: 350px;
}

.records-section {
  margin-bottom: 32px;
}

.records-card {
  padding: 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #64748B;

  .empty-icon {
    margin-bottom: 16px;
    opacity: 0.5;
  }
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(0, 229, 255, 0.03);
    border-color: rgba(0, 229, 255, 0.15);
  }
}

.record-date {
  width: 80px;
  flex-shrink: 0;
}

.record-day {
  font-size: 13px;
  color: #94A3B8;
}

.record-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.record-title {
  font-size: 14px;
  color: #E2E8F0;
}

.record-duration {
  font-size: 13px;
  color: #64748B;
}

.record-status {
  width: 32px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;

  .status-icon {
    &.completed {
      color: #10B981;
    }

    &.failed {
      color: #EF4444;
    }
  }
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 20px 24px;
    flex-wrap: wrap;
    gap: 16px;
  }

  .page-content {
    padding: 0 24px 24px;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .overview-card {
    flex-direction: row;
    text-align: left;
    gap: 20px;
  }

  .overview-icon {
    width: 48px;
    height: 48px;
  }

  .overview-data {
    flex-direction: row;
    align-items: baseline;
    gap: 6px;
  }

  .overview-value {
    font-size: 28px;
  }

  .chart-container {
    height: 250px;
  }

  .radar-container {
    height: 300px;
  }
}
</style>