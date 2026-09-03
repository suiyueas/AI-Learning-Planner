<template>
  <div class="statistics-page">
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">📊</span>
            <span>学习统计</span>
            <span class="title-sub">数据驱动的学习洞察</span>
          </h1>
        </div>
        <div class="header-right">
          <div class="time-select" @click="dropdownOpen = !dropdownOpen">
            <span class="time-select-label">{{ timeLabel }}</span>
            <svg class="time-select-arrow" :class="{ open: dropdownOpen }" viewBox="0 0 12 12" fill="none">
              <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <transition name="dropdown">
              <div v-if="dropdownOpen" class="time-dropdown" @click.stop>
                <div
                  v-for="opt in timeOptions"
                  :key="opt.value"
                  class="time-dropdown-item"
                  :class="{ active: timeRange === opt.value }"
                  @click="selectTime(opt.value)"
                >
                  {{ opt.label }}
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </header>

    <div class="page-content">
      <section class="overview-section">
        <h3 class="section-title">
          <span class="section-icon">📈</span> 概览
        </h3>
        <div class="overview-grid">
          <div class="overview-card">
            <div class="overview-icon streak-icon">🔥</div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.streak }}</span>
              <span class="overview-unit">天</span>
            </div>
            <span class="overview-label">连续学习</span>
          </div>
          <div class="overview-card">
            <div class="overview-icon hours-icon">⏱️</div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.totalHours }}</span>
              <span class="overview-unit">h</span>
            </div>
            <span class="overview-label">总学时</span>
          </div>
          <div class="overview-card">
            <div class="overview-icon completed-icon">✅</div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.completed }}</span>
              <span class="overview-unit">个</span>
            </div>
            <span class="overview-label">已完成节点</span>
          </div>
          <div class="overview-card">
            <div class="overview-icon score-icon">⭐</div>
            <div class="overview-data">
              <span class="overview-value">{{ overviewData.avgScore }}</span>
              <span class="overview-unit">分</span>
            </div>
            <span class="overview-label">平均分</span>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <h3 class="section-title">
          <span class="section-icon">📉</span> 学习趋势
        </h3>
        <div class="chart-card">
          <div ref="lineChartRef" class="chart-container"></div>
        </div>
      </section>

      <section class="chart-section">
        <h3 class="section-title">
          <span class="section-icon">🎯</span> 能力雷达图
        </h3>
        <div class="chart-card">
          <div ref="radarChartRef" class="chart-container radar-container"></div>
        </div>
      </section>

      <section class="records-section">
        <h3 class="section-title">
          <span class="section-icon">📋</span> 学习记录
        </h3>
        <div class="records-card">
          <div v-if="recentRecords.length === 0" class="empty-state">
            <span class="empty-icon">📝</span>
            <h3 class="empty-title">暂无学习记录</h3>
            <p class="empty-desc">开始学习后，你的学习记录将在这里展示</p>
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
                <span class="status-dot" :class="record.status"></span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="aiSuggestion" class="suggestion-section">
        <h3 class="section-title">
          <span class="section-icon">💡</span> AI 学习建议
        </h3>
        <div class="suggestion-card">
          <div class="suggestion-content">
            <span class="suggestion-icon">✨</span>
            <div class="suggestion-text">
              <p class="suggestion-message">{{ aiSuggestion.message }}</p>
              <div v-if="aiSuggestion.actions && aiSuggestion.actions.length > 0" class="suggestion-actions">
                <span
                  v-for="(action, idx) in aiSuggestion.actions"
                  :key="idx"
                  class="suggestion-action-tag"
                >
                  {{ action.label }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>
      <section v-else-if="loadingSuggestion" class="suggestion-section">
        <h3 class="section-title">
          <span class="section-icon">💡</span> AI 学习建议
        </h3>
        <div class="suggestion-card">
          <div class="suggestion-content">
            <span class="suggestion-icon">✨</span>
            <div class="suggestion-loading">
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { getProgressOverview, getProgressCurve, getProgressCompetency, getProgressRecords, getAISuggestion, seedSampleData } from '@/api/statsApi'
import { ElMessage } from 'element-plus'

const lineChartRef = ref(null)
const radarChartRef = ref(null)
let lineChart = null
let radarChart = null
const timeRange = ref('7')
const dropdownOpen = ref(false)

const timeOptions = [
  { label: '近7天', value: '7' },
  { label: '近30天', value: '30' }
]

const timeLabel = computed(() => {
  return timeOptions.find(o => o.value === timeRange.value)?.label || '近7天'
})

const overviewData = ref({
  streak: 0,
  totalHours: 0,
  completed: 0,
  avgScore: 0
})

const trendData = ref({ labels: [], hours: [] })
const radarData = ref({ indicators: [], values: [] })
const recentRecords = ref([])
const aiSuggestion = ref(null)
const loadingSuggestion = ref(false)

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const selectTime = (val) => {
  timeRange.value = val
  dropdownOpen.value = false
}

const closeDropdown = (e) => {
  if (!e.target.closest('.time-select')) {
    dropdownOpen.value = false
  }
}

const loadOverviewData = async () => {
  try {
    const res = await getProgressOverview()
    if (res.success && res.data) {
      const data = res.data
      overviewData.value = {
        streak: data.streak || 0,
        totalHours: data.totalHours || 0,
        completed: data.completed || 0,
        avgScore: data.avgScore || 0
      }
    }
  } catch (e) {
    console.error('加载概览数据失败', e)
  }
}

const loadCurveData = async () => {
  try {
    const res = await getProgressCurve(timeRange.value)
    if (res.success && res.data) {
      const data = res.data
      trendData.value = {
        labels: data.labels || [],
        hours: data.hours || []
      }
      updateLineChart()
    }
  } catch (e) {
    console.error('加载学习曲线失败', e)
  }
}

const loadCompetencyData = async () => {
  try {
    const res = await getProgressCompetency()
    if (res.success && res.data && Array.isArray(res.data)) {
      if (res.data.length > 0) {
        radarData.value = {
          indicators: res.data.map(item => ({ name: item.name, max: 100 })),
          values: res.data.map(item => Math.round(item.mastery || 0))
        }
      } else {
        radarData.value = { indicators: [], values: [] }
      }
      updateRadarChart()
    }
  } catch (e) {
    console.error('加载能力矩阵失败', e)
  }
}

const loadRecordsData = async () => {
  try {
    const res = await getProgressRecords({ page: 1, size: 10 })
    if (res.success && res.data && Array.isArray(res.data.records)) {
      recentRecords.value = res.data.records.map(r => ({
        date: r.date,
        title: r.title,
        duration: r.duration,
        status: r.status
      }))
    } else {
      recentRecords.value = []
    }
  } catch (e) {
    console.error('加载学习记录失败', e)
    recentRecords.value = []
  }
}

const loadAISuggestion = async () => {
  loadingSuggestion.value = true
  try {
    const res = await getAISuggestion()
    if (res.success && res.data) {
      aiSuggestion.value = res.data
    }
  } catch (e) {
    console.error('加载AI建议失败', e)
  } finally {
    loadingSuggestion.value = false
  }
}

const initLineChart = () => {
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  updateLineChart()
}

const updateLineChart = () => {
  if (!lineChart || !lineChartRef.value) return
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(9, 11, 18, 0.92)',
      borderColor: 'rgba(16, 185, 129, 0.3)',
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (params) => {
        const data = params[0]
        return `${data.name}<br/><span style="color:#10b981">学习时长: ${data.value}h</span>`
      }
    },
    grid: {
      left: 12, right: 16, bottom: 8, top: 40,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.labels,
      axisLine: { lineStyle: { color: 'rgba(16, 185, 129, 0.15)' } },
      axisLabel: { color: '#71717a', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '学习时长 (h)',
      nameTextStyle: { color: '#71717a', fontSize: 11, padding: [0, 0, 0, 0] },
      axisLine: { show: false },
      axisLabel: { color: '#71717a' },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: 'rgba(16, 185, 129, 0.08)' } }
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        color: '#10b981',
        width: 3,
        shadowColor: 'rgba(16, 185, 129, 0.5)',
        shadowBlur: 10
      },
      itemStyle: {
        color: '#10b981',
        borderColor: 'rgba(9, 11, 18, 0.8)',
        borderWidth: 2
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(16, 185, 129, 0.22)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.01)' }
          ]
        }
      },
      data: trendData.value.hours.length > 0 ? trendData.value.hours : [0]
    }]
  }
  lineChart.setOption(option, true)
}

const updateRadarChart = () => {
  if (!radarChart || !radarChartRef.value) return
  const hasData = radarData.value.indicators.length > 0 && radarData.value.values.length > 0
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(9, 11, 18, 0.92)',
      borderColor: 'rgba(16, 185, 129, 0.3)',
      textStyle: { color: '#e2e8f0' }
    },
    radar: {
      indicator: hasData ? radarData.value.indicators : [{ name: '暂无数据', max: 100 }],
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: '#71717a', fontSize: 12 },
      splitLine: {
        lineStyle: { color: 'rgba(16, 185, 129, 0.1)', width: 1 }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(16, 185, 129, 0.02)', 'rgba(16, 185, 129, 0.04)', 'rgba(16, 185, 129, 0.06)', 'rgba(16, 185, 129, 0.08)']
        }
      },
      axisLine: { lineStyle: { color: 'rgba(16, 185, 129, 0.15)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: hasData ? radarData.value.values : [0],
        name: '能力评估',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#10b981', width: 2 },
        areaStyle: { color: 'rgba(16, 185, 129, 0.15)' },
        itemStyle: { color: '#10b981', borderColor: 'rgba(9, 11, 18, 0.8)', borderWidth: 2 }
      }]
    }]
  }
  radarChart.setOption(option, true)
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  radarChart = echarts.init(radarChartRef.value)
  updateRadarChart()
}

const handleResize = () => {
  lineChart?.resize()
  radarChart?.resize()
}

const isEmptyData = () => {
  const overviewIsEmpty = overviewData.value.streak === 0 &&
    overviewData.value.totalHours === 0 &&
    overviewData.value.completed === 0 &&
    overviewData.value.avgScore === 0
  const recordsIsEmpty = recentRecords.value.length === 0
  const competencyIsEmpty = radarData.value.indicators.length === 0
  return overviewIsEmpty && recordsIsEmpty && competencyIsEmpty
}

const trySeedSampleData = async () => {
  try {
    const res = await seedSampleData()
    if (res.success) {
      if (!res.data.alreadyExists) {
        ElMessage.success('已为你生成示例学习数据，方便体验统计功能')
      }
      await Promise.allSettled([
        loadOverviewData(),
        loadCurveData(),
        loadCompetencyData(),
        loadRecordsData(),
        loadAISuggestion()
      ])
    }
  } catch (e) {
    console.error('生成示例数据失败', e)
  }
}

const loadAllData = async () => {
  await Promise.allSettled([
    loadOverviewData(),
    loadCurveData(),
    loadCompetencyData(),
    loadRecordsData(),
    loadAISuggestion()
  ])
  if (isEmptyData()) {
    await trySeedSampleData()
  }
}

watch(timeRange, () => {
  loadCurveData()
})

onMounted(() => {
  initLineChart()
  initRadarChart()
  loadAllData()
  window.addEventListener('resize', handleResize)
  document.addEventListener('click', closeDropdown)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', closeDropdown)
  lineChart?.dispose()
  radarChart?.dispose()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.statistics-page {
  min-height: calc(100vh - 68px);
}

/* ===== 页面头部 ===== */
.page-header {
  @include page-header-base;
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba($bg-base, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba($accent-indigo, 0.06);
}

.page-title {
  @include page-title-base;
}
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; -webkit-text-fill-color: initial; }

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ===== 时间选择器（纯自定义，无白色） ===== */
.time-select {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: rgba($accent-cyan, 0.1);
  border: 1px solid rgba($accent-cyan, 0.2);
  border-radius: 8px;
  color: $accent-cyan;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;

  &:hover {
    background: rgba($accent-cyan, 0.16);
    border-color: rgba($accent-cyan, 0.35);
    box-shadow: 0 0 16px rgba($accent-cyan, 0.1);
  }
}

.time-select-label {
  white-space: nowrap;
}

.time-select-arrow {
  width: 12px;
  height: 12px;
  color: rgba($accent-cyan, 0.7);
  transition: transform 0.2s ease;
  flex-shrink: 0;

  &.open {
    transform: rotate(180deg);
  }
}

.time-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 110px;
  background: rgba($bg-surface, 0.7);
  border: 1px solid rgba($accent-cyan, 0.25);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 4px;
  z-index: 100;
  animation: dropdown-fade-in 0.2s ease;
}

@keyframes dropdown-fade-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.time-dropdown-item {
  padding: 7px 14px;
  border-radius: 6px;
  color: $text-secondary;
  font-size: 0.82rem;
  transition: all 0.2s ease;
  white-space: nowrap;
  background: transparent;

  &:hover {
    background: rgba($accent-cyan, 0.12);
    color: $accent-cyan;
    transform: translateX(2px);
  }

  &.active {
    background: rgba($accent-cyan, 0.18);
    color: $accent-cyan;
    font-weight: 600;
    border-left: 2px solid $accent-cyan;
    text-shadow: 0 0 8px rgba($accent-cyan, 0.4);
  }
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ===== 页面内容 ===== */
.page-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 32px 60px;
}

/* ===== 区块标题 ===== */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 16px;
}

.section-icon {
  font-size: 1.1rem;
}

/* ===== 毛玻璃卡片 ===== */
.overview-card,
.chart-card,
.records-card,
.suggestion-card {
  background: rgba($bg-surface, 0.55);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-cyan, 0.08);
  border-radius: 16px;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-cyan, 0.15);
    box-shadow: 0 0 24px rgba($accent-cyan, 0.05);
  }
}

/* ===== 概览卡片 ===== */
.overview-section {
  margin-bottom: 28px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.overview-card {
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
}

.overview-icon {
  font-size: 1.8rem;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;

  &.streak-icon {
    background: linear-gradient(135deg, rgba($color-danger, 0.1), rgba($color-warning, 0.1));
  }

  &.hours-icon {
    background: rgba($accent-cyan, 0.1);
  }

  &.completed-icon {
    background: rgba($accent-indigo, 0.1);
  }

  &.score-icon {
    background: rgba($accent-violet, 0.1);
  }
}

.overview-data {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.overview-value {
  font-size: 2rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  color: $accent-cyan;
  text-shadow: 0 0 20px rgba($accent-cyan, 0.25);
}

.overview-unit {
  font-size: 0.85rem;
  color: $text-secondary;
}

.overview-label {
  font-size: 0.8rem;
  color: $text-muted;
}

/* ===== 图表区域 ===== */
.chart-section {
  margin-bottom: 28px;
}

.chart-card {
  padding: 20px 16px;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.radar-container {
  height: 380px;
}

/* ===== 学习记录 ===== */
.records-section {
  margin-bottom: 28px;
}

.records-card {
  padding: 20px;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: rgba($bg-base, 0.3);
  border: 1px solid rgba($accent-cyan, 0.06);
  border-radius: 10px;
  transition: all 0.2s ease;

  &:hover {
    background: rgba($accent-cyan, 0.04);
    border-color: rgba($accent-cyan, 0.12);
  }
}

.record-date {
  width: 80px;
  flex-shrink: 0;
}

.record-day {
  font-size: 0.82rem;
  color: $text-secondary;
}

.record-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.record-title {
  font-size: 0.85rem;
  color: $text-primary;
}

.record-duration {
  font-size: 0.78rem;
  color: $text-muted;
  font-family: 'JetBrains Mono', monospace;
}

.record-status {
  width: 32px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.completed {
    background: $color-success;
    box-shadow: 0 0 8px rgba($color-success, 0.5);
  }

  &.failed {
    background: $color-danger;
    box-shadow: 0 0 8px rgba($color-danger, 0.5);
  }
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 2.4rem;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 6px;
}

.empty-desc {
  font-size: 0.85rem;
  color: $text-muted;
  margin: 0;
}

/* ===== AI建议 ===== */
.suggestion-section {
  margin-bottom: 28px;
}

.suggestion-card {
  padding: 24px;
}

.suggestion-content {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.suggestion-icon {
  font-size: 1.5rem;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-cyan, 0.08);
  border: 1px solid rgba($accent-cyan, 0.12);
  border-radius: 12px;
  flex-shrink: 0;
}

.suggestion-text {
  flex: 1;
}

.suggestion-message {
  font-size: 0.9rem;
  color: $text-primary;
  line-height: 1.7;
  margin: 0 0 12px;
}

.suggestion-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-action-tag {
  padding: 4px 12px;
  background: rgba($accent-cyan, 0.08);
  border: 1px solid rgba($accent-cyan, 0.15);
  border-radius: 16px;
  font-size: 0.75rem;
  color: $accent-cyan;
  font-weight: 500;
}

/* ===== 加载动画 ===== */
.suggestion-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 20px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  background: $accent-cyan;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;

  &:nth-child(1) { animation-delay: -0.32s; }
  &:nth-child(2) { animation-delay: -0.16s; }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .overview-grid { grid-template-columns: repeat(2, 1fr); }
  .page-header { padding: 12px 20px; }
  .page-content { padding: 20px; }
}

@media (max-width: 640px) {
  .page-header { padding: 10px 12px; gap: 10px; }
  .page-content { padding: 12px; }
  .overview-grid { grid-template-columns: 1fr; }
  .overview-card { flex-direction: row; text-align: left; gap: 16px; }
  .overview-icon { width: 44px; height: 44px; font-size: 1.4rem; }
  .overview-value { font-size: 1.5rem; }
  .chart-container { height: 250px; }
  .radar-container { height: 300px; }
}
</style>
