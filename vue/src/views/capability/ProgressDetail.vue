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
          <BarChartHorizontal :size="24" class="header-icon" />
          进度追踪
        </h1>
      </div>
      <button class="btn-action" @click="exportReport">
        <Download :size="16" />
        导出报告
      </button>
    </header>

    <div class="detail-content">
      <!-- 加载失败重试 -->
      <div v-if="loadError" class="load-error">
        <span>⚠️ 加载学习数据失败，请检查网络后重试</span>
        <button class="retry-btn" @click="loadAll">🔄 重试</button>
      </div>

      <!-- 学习概览 -->
      <section class="info-section">
        <div class="section-label">
          <BarChart3 :size="16" />
          学习概览
        </div>
        <div class="stats-grid">
          <div class="stats-card">
            <span class="stats-value">{{ statsData.streak }}</span>
            <span class="stats-label">连续学习（天）</span>
          </div>
          <div class="stats-card">
            <span class="stats-value">{{ statsData.totalHours }}</span>
            <span class="stats-label">总学时</span>
          </div>
          <div class="stats-card">
            <span class="stats-value">{{ statsData.completed }}</span>
            <span class="stats-label">已完成（节点）</span>
          </div>
          <div class="stats-card">
            <span class="stats-value">{{ statsData.avgScore }}</span>
            <span class="stats-label">平均分</span>
          </div>
        </div>
      </section>

      <!-- 学习曲线 -->
      <section class="info-section">
        <div class="section-label">
          <TrendingUp :size="16" />
          学习曲线
        </div>
        <div class="range-switch">
          <button
            v-for="opt in rangeOptions"
            :key="opt.value"
            class="range-btn"
            :class="{ active: timeRange === opt.value }"
            @click="switchRange(opt.value)"
          >
{{ opt.label }}
</button>
        </div>
        <div v-if="curveEmpty" class="chart-empty">
          <TrendingUp :size="40" class="empty-icon" />
          <p>完成第一个学习任务后，这里将显示你的成长轨迹</p>
        </div>
        <div v-else ref="lineChartRef" class="chart-container"></div>
      </section>

      <!-- AI 学习洞察 -->
      <section v-if="insights.length > 0" class="info-section insight-section">
        <div class="section-label">
          <Sparkles :size="16" />
          📊 学习洞察
        </div>
        <div class="insight-list">
          <div v-for="(insight, index) in insights" :key="index" class="insight-item">
            <span class="insight-icon">•</span>
            <span class="insight-text">{{ insight }}</span>
          </div>
        </div>
      </section>

      <!-- 能力成长矩阵 -->
      <section class="info-section">
        <div class="section-label">
          <Grid3X3 :size="16" />
          能力成长矩阵
        </div>
        <div v-if="growthData.length === 0" class="chart-empty">
          <Grid3X3 :size="40" class="empty-icon" />
          <p>完成测评或学习任务后，这里将展示你的多维度能力画像</p>
        </div>
        <div v-else class="growth-list">
          <div
            v-for="(skill, index) in growthData"
            :key="index"
            class="growth-item"
            :class="{ clickable: skill.pathId }"
            @click="viewDomainDetail(skill)"
          >
            <div class="growth-header">
              <span class="growth-name">{{ skill.name }}</span>
              <span class="growth-level" :class="levelClass(skill.level)">{{ skill.level }}</span>
              <span class="growth-value">{{ skill.mastery }}%</span>
            </div>
            <div class="growth-track">
              <div class="growth-fill" :style="{ width: skill.mastery + '%', background: masteryColor(skill.mastery) }"></div>
            </div>
            <div v-if="skill.recordCount" class="growth-meta">
              <span>已学 {{ skill.recordCount }} 项</span>
              <span v-if="skill.pathId" class="growth-link">查看路径 →</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 学习记录 -->
      <section class="info-section">
        <div class="section-label">
          <Clock :size="16" />
          学习记录
        </div>
        <div class="records-filter">
          <div class="filter-group">
            <select v-model="recordStatusFilter" class="filter-select" @change="loadRecords">
              <option value="all">全部状态</option>
              <option value="completed">✅ 已完成</option>
              <option value="in_progress">⏳ 进行中</option>
              <option value="pending">📌 未完成</option>
              <option value="skipped">⏭️ 已跳过</option>
            </select>
            <input
              v-model="recordKeyword"
              class="filter-search"
              placeholder="🔍 搜索任务名称..."
              @keyup.enter="loadRecords"
            />
            <button class="filter-btn" @click="loadRecords">搜索</button>
          </div>
        </div>
        <div v-if="recentRecords.length === 0" class="chart-empty">
          <Clock :size="40" class="empty-icon" />
          <p>完成第一个学习任务后，这里将显示你的学习记录</p>
        </div>
        <div v-else class="records-list">
          <div
            v-for="(record, index) in recentRecords"
            :key="index"
            class="record-item"
            @click="viewRecordDetail(record)"
          >
            <div class="record-date">
              <span class="record-day">{{ formatDate(record.date) }}</span>
            </div>
            <div class="record-info">
              <span class="record-title">{{ record.title }}</span>
              <span class="record-duration">{{ record.duration }}min</span>
            </div>
            <div class="record-status" :class="record.status">
              <span v-if="record.status === 'completed'">✅</span>
              <span v-else-if="record.status === 'skipped'">⏭️</span>
              <span v-else>⏳</span>
            </div>
          </div>
        </div>
        <button class="link-btn" @click="viewAllRecords">
          查看全部记录（{{ recordTotal }}）
          <ArrowRight :size="14" />
        </button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  ArrowLeft, BarChartHorizontal, Download, BarChart3,
  TrendingUp, Grid3X3, Clock, ArrowRight, Sparkles
} from 'lucide-vue-next'
import { getProgressOverview, getProgressCurve, getProgressCompetency, getProgressRecords } from '@/api/statsApi'

const router = useRouter()
const lineChartRef = ref(null)
let lineChart = null

const timeRange = ref('30')
const rangeOptions = [
  { label: '近7天', value: '7' },
  { label: '近30天', value: '30' },
  { label: '近90天', value: '90' },
  { label: '全部', value: 'all' }
]
const loadError = ref(false)

// ===== 学习概览（真实数据） =====
const statsData = ref({ streak: 0, totalHours: 0, completed: 0, avgScore: 0 })

// ===== 学习曲线（真实数据） =====
const curveData = ref({ labels: [], hours: [], mastery: [] })
const curveEmpty = computed(() => {
  const hours = curveData.value.hours || []
  return hours.every(h => h === 0)
})

// ===== 能力矩阵（真实数据） =====
const growthData = ref([])

// ===== 学习记录（真实数据） =====
const recentRecords = ref([])
const recordTotal = ref(0)
const recordStatusFilter = ref('all')
const recordKeyword = ref('')

// ===== AI 洞察（基于真实数据规则生成） =====
const insights = computed(() => buildInsights(curveData.value, statsData.value))

function buildInsights(curve, stats) {
  const list = []
  const labels = curve.labels || []
  const hours = curve.hours || []
  const mastery = curve.mastery || []
  if (labels.length === 0 || hours.every(h => h === 0)) return list

  // 峰值时段
  const avg = hours.reduce((a, b) => a + b, 0) / hours.length
  const peakIdx = hours.indexOf(Math.max(...hours))
  if (hours[peakIdx] > avg * 1.3 && hours[peakIdx] > 1) {
    list.push(`你在「${labels[peakIdx]}」的学习时长达到峰值（${hours[peakIdx]}h），建议保持这个节奏。`)
  }

  // 掌握度变化
  const validMastery = mastery.filter(m => m > 0)
  if (validMastery.length >= 2) {
    const maxM = Math.max(...validMastery)
    const minM = Math.min(...validMastery)
    if (maxM - minM >= 15) {
      list.push(`掌握度从 ${minM}% 提升到 ${maxM}%，持续学习效果显著，建议增加挑战性练习巩固。`)
    } else {
      list.push(`掌握度稳定在 ${Math.round(validMastery.reduce((a, b) => a + b, 0) / validMastery.length)}% 左右，建议尝试更高难度的学习内容。`)
    }
  }

  // 连续学习
  if (stats.streak >= 3) {
    list.push(`你已连续学习 ${stats.streak} 天，保持这个习惯，成长会加速！`)
  } else if (stats.totalHours > 0) {
    list.push(`累计学习 ${stats.totalHours}h、完成 ${stats.completed} 个知识点，继续加油！`)
  }
  return list.slice(0, 4)
}

const goBack = () => {
  router.push('/home')
}

const exportReport = () => {
  // 导出学习报告 - 通过打印/截图方式留存
  window.print()
}

const viewRecordDetail = (record) => {
  if (record.pathId) {
    router.push(`/learning-path/${record.pathId}`)
  }
}

const viewAllRecords = () => {
  router.push('/learning-records')
}

const viewDomainDetail = (skill) => {
  if (!skill.pathId) return
  router.push(`/learning-path/${skill.pathId}`)
}

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const levelClass = (level) => {
  const map = { '高级': 'high', '中级': 'medium', '初级': 'low' }
  return map[level] || ''
}

const masteryColor = (mastery) => {
  if (mastery >= 80) return 'linear-gradient(90deg, #10B981, #00E5FF)'
  if (mastery >= 60) return 'linear-gradient(90deg, #F59E0B, #FFBE0B)'
  return 'linear-gradient(90deg, #FF006E, #FF4060)'
}

// ===== 数据加载 =====
const loadAll = async () => {
  loadError.value = false
  try {
    await Promise.all([loadOverview(), loadRecords(), loadCompetency()])
  } catch (e) {
    loadError.value = true
  }
  loadCurve()
}

const loadOverview = async () => {
  const res = await getProgressOverview()
  const d = res?.data || {}
  statsData.value = {
    streak: d.streak || 0,
    totalHours: d.totalHours || 0,
    completed: d.completed || 0,
    avgScore: d.avgScore || 0
  }
}

const loadCurve = async () => {
  try {
    const res = await getProgressCurve(timeRange.value)
    curveData.value = res?.data || { labels: [], hours: [], mastery: [] }
    renderChart()
  } catch (e) {
    curveData.value = { labels: [], hours: [], mastery: [] }
    loadError.value = true
  }
}

const loadCompetency = async () => {
  const res = await getProgressCompetency()
  growthData.value = (res?.data || []).map(d => ({
    name: d.name || d.subject || '未知领域',
    mastery: d.mastery || 0,
    level: d.level || '初级',
    pathId: d.pathId || '',
    recordCount: d.recordCount || 0
  }))
}

const loadRecords = async () => {
  try {
    const res = await getProgressRecords({
      page: 1,
      size: 5,
      status: recordStatusFilter.value,
      keyword: recordKeyword.value.trim()
    })
    const d = res?.data || {}
    recentRecords.value = (d.records || []).map(r => ({
      id: r.id,
      date: r.date,
      title: r.title,
      duration: r.duration || 0,
      status: r.status || 'pending',
      pathId: r.pathId || '',
      source: r.source || ''
    }))
    recordTotal.value = d.total || 0
  } catch (e) {
    recentRecords.value = []
    recordTotal.value = 0
  }
}

const switchRange = (value) => {
  timeRange.value = value
  loadCurve()
}

// ===== 学习曲线图表 =====
const renderChart = () => {
  if (!lineChartRef.value) return
  if (!lineChart) {
    lineChart = echarts.init(lineChartRef.value)
  }
  const data = curveData.value
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(17, 17, 39, 0.9)',
      borderColor: 'rgba(0, 229, 255, 0.2)',
      textStyle: { color: '#F1F5F9' },
      formatter: (params) => {
        const label = params[0]?.name || ''
        let html = `${label}<br/>`
        params.forEach(p => {
          const color = p.seriesName === '学习时长' ? '#00E5FF' : '#10B981'
          html += `<span style="color:${color}">${p.seriesName}: ${p.value}${p.seriesName === '学习时长' ? 'h' : '%'}</span><br/>`
        })
        return html
      }
    },
    legend: {
      data: ['学习时长', '掌握度'],
      textStyle: { color: '#94A3B8' },
      top: 0
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.labels || [],
      axisLine: { lineStyle: { color: 'rgba(100, 100, 180, 0.2)' } },
      axisLabel: { color: '#94A3B8' }
    },
    yAxis: [
      {
        type: 'value',
        name: '时长(h)',
        nameTextStyle: { color: '#94A3B8' },
        axisLine: { show: false },
        axisLabel: { color: '#94A3B8' },
        splitLine: { lineStyle: { color: 'rgba(100, 100, 180, 0.06)' } }
      },
      {
        type: 'value',
        name: '掌握度(%)',
        nameTextStyle: { color: '#94A3B8' },
        axisLine: { show: false },
        axisLabel: { color: '#94A3B8' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '学习时长',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#00E5FF', width: 2 },
        itemStyle: { color: '#00E5FF' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0, 229, 255, 0.2)' },
              { offset: 1, color: 'rgba(0, 229, 255, 0.02)' }
            ]
          }
        },
        data: data.hours || []
      },
      {
        name: '掌握度',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbol: 'diamond',
        symbolSize: 8,
        lineStyle: { color: '#10B981', width: 2 },
        itemStyle: { color: '#10B981' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(16, 185, 129, 0.15)' },
              { offset: 1, color: 'rgba(16, 185, 129, 0.01)' }
            ]
          }
        },
        data: data.mastery || []
      }
    ]
  }
  lineChart.setOption(option, true)
}

const handleResize = () => {
  lineChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (lineChart) {
    lineChart.dispose()
    lineChart = null
  }
})
</script>

<style scoped>
@use '../styles/variables' as *;
.detail-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 80px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 48px;
  background: rgba($bg-primary, 0.8);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba($accent-secondary, 0.12);
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

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.4rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.header-icon {
  color: $accent-primary;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  color: $accent-primary;
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 24px rgba($accent-primary, 0.15);
  border-color: rgba($accent-primary, 0.4);
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
  background: rgba($bg-primary, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 16px;
  padding: 28px;
  transition: all 0.3s ease;
}

.info-section:hover {
  border-color: rgba($accent-primary, 0.15);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
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
  border-bottom: 1px solid rgba($accent-secondary, 0.08);
}

.section-label svg {
  color: $accent-primary;
}

/* 统计概览 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stats-card {
  text-align: center;
  padding: 24px 16px;
  background: rgba($accent-secondary, 0.03);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 12px;
}

.stats-value {
  display: block;
  font-size: 2.5rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  background: linear-gradient(135deg, $accent-primary 0%, #38BDF8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 15px rgba($accent-primary, 0.3));
  margin-bottom: 6px;
}

.stats-label {
  font-size: 0.85rem;
  color: $text-secondary;
}

/* 学习曲线 */
.chart-container {
  width: 100%;
  height: 350px;
}

/* 时间范围切换 */
.range-switch {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.range-btn {
  padding: 6px 16px;
  background: rgba($accent-secondary, 0.05);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.range-btn:hover {
  color: $accent-primary;
  border-color: rgba($accent-primary, 0.25);
}

.range-btn.active {
  color: $accent-primary;
  background: rgba($accent-primary, 0.1);
  border-color: rgba($accent-primary, 0.3);
}

/* 空态 */
.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px 20px;
  color: $text-muted;
  font-size: 0.9rem;
  text-align: center;
}

.empty-icon {
  opacity: 0.4;
}

/* 加载失败 */
.load-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  color: #FCA5A5;
  font-size: 0.9rem;
}

.retry-btn {
  padding: 6px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #FCA5A5;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.retry-btn:hover {
  background: rgba(239, 68, 68, 0.18);
}

/* AI 洞察 */
.insight-section {
  border-color: rgba(167, 139, 250, 0.15);
}

.insight-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(167, 139, 250, 0.04);
  border: 1px solid rgba(167, 139, 250, 0.08);
  border-radius: 10px;
  font-size: 0.85rem;
  line-height: 1.6;
  color: #C4B5FD;
}

.insight-icon {
  color: #A78BFA;
  font-weight: 700;
  flex-shrink: 0;
}

/* 能力成长 */
.growth-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.growth-item {
  padding: 4px 0;
}

.growth-item.clickable {
  cursor: pointer;
}

.growth-item.clickable:hover .growth-name {
  color: $accent-primary;
}

.growth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.growth-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: $text-primary;
  transition: color 0.2s;
  flex: 1;
}

.growth-level {
  font-size: 0.7rem;
  padding: 2px 10px;
  border-radius: 5px;
  font-weight: 600;
  flex-shrink: 0;
}

.growth-level.high {
  background: rgba(16, 185, 129, 0.12);
  color: #10B981;
}

.growth-level.medium {
  background: rgba(245, 158, 11, 0.12);
  color: #F59E0B;
}

.growth-level.low {
  background: rgba(255, 0, 110, 0.12);
  color: #FF006E;
}

.growth-value {
  font-size: 0.9rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: $accent-primary;
  flex-shrink: 0;
}

.growth-track {
  height: 8px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.growth-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease;
}

.growth-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 0.75rem;
  color: $text-muted;
}

.growth-link {
  color: $accent-primary;
  cursor: pointer;
}

/* 学习记录 */
.records-filter {
  margin-bottom: 14px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-select {
  padding: 7px 12px;
  background: rgba(30, 38, 56, 0.6);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 8px;
  color: #C0C0E0;
  font-size: 0.82rem;
  font-family: inherit;
  cursor: pointer;
  outline: none;
}

.filter-select:hover {
  border-color: rgba($accent-primary, 0.3);
}

.filter-search {
  flex: 1;
  min-width: 160px;
  padding: 7px 12px;
  background: rgba(30, 38, 56, 0.6);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 8px;
  color: #E2E8F0;
  font-size: 0.82rem;
  font-family: inherit;
  outline: none;
  transition: all 0.2s;
}

.filter-search::placeholder {
  color: $text-muted;
}

.filter-search:focus {
  border-color: rgba($accent-primary, 0.4);
}

.filter-btn {
  padding: 7px 16px;
  background: rgba($accent-primary, 0.08);
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 8px;
  color: $accent-primary;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.filter-btn:hover {
  background: rgba($accent-primary, 0.14);
  border-color: rgba($accent-primary, 0.35);
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: rgba($accent-secondary, 0.03);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.record-item:hover {
  background: rgba($accent-primary, 0.03);
  border-color: rgba($accent-primary, 0.15);
  transform: translateX(4px);
}

.record-date {
  min-width: 90px;
}

.record-day {
  font-size: 0.8rem;
  color: $text-secondary;
  font-family: 'JetBrains Mono', monospace;
}

.record-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.record-title {
  font-size: 0.9rem;
  font-weight: 500;
  color: $text-primary;
}

.record-duration {
  font-size: 0.8rem;
  color: $text-secondary;
}

.record-status {
  font-size: 1.1rem;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-primary, 0.06);
  border: 1px solid rgba($accent-primary, 0.12);
  border-radius: 8px;
  color: $accent-primary;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.link-btn:hover {
  background: rgba($accent-primary, 0.1);
  border-color: rgba($accent-primary, 0.25);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .detail-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 12px;
  }
  .header-left { width: 100%; }
  .btn-action { width: 100%; justify-content: center; }
  .detail-content { padding: 20px; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .record-item { flex-wrap: wrap; }
}
</style>
