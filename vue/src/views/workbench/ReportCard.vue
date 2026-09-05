<template>
  <div class="report-card">
    <div class="rc-header">
      <h2 class="rc-title">📊 学习报告</h2>
      <p class="rc-desc">你的学习旅程总结 — 了解进步轨迹，持续优化学习策略</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="waitingForReport" class="rc-loading">
      <div class="rc-spinner"></div>
      <p>正在生成报告...</p>
    </div>

    <!-- 超时 / 无数据 -->
    <div v-else-if="loadingTimeout || !report" class="rc-loading rc-loading--empty">
      <div class="rc-empty-icon">📭</div>
      <p>暂无报告数据</p>
      <p class="rc-empty-hint">完成学习任务后，报告将自动生成</p>
    </div>

    <template v-else>
      <!-- 核心数据概览 -->
      <div class="rc-summary">
        <div class="rc-stat">
          <span class="rc-stat-value">{{ report.totalHours || 0 }}<span class="rc-stat-unit">h</span></span>
          <span class="rc-stat-label">学习时长</span>
        </div>
        <div class="rc-stat">
          <span class="rc-stat-value">{{ report.completedTasks || 0 }}</span>
          <span class="rc-stat-label">完成任务</span>
        </div>
        <div class="rc-stat">
          <span class="rc-stat-value">{{ report.accuracy || 0 }}<span class="rc-stat-unit">%</span></span>
          <span class="rc-stat-label">正确率</span>
        </div>
        <div class="rc-stat">
          <span class="rc-stat-value">{{ report.streak || 0 }}<span class="rc-stat-unit">天</span></span>
          <span class="rc-stat-label">连续学习</span>
        </div>
      </div>

      <!-- 学习趋势图（折线图） -->
      <div class="rc-chart-section">
        <div class="rc-chart-header">
          <span class="rc-chart-title">学习趋势</span>
          <span class="rc-chart-subtitle">近 7 天学习时长分布</span>
        </div>
        <div class="rc-chart-container">
          <VChart v-if="trendChartOption" :option="trendChartOption" autoresize />
        </div>
      </div>

      <!-- 知识点掌握度雷达图 -->
      <div class="rc-chart-section">
        <div class="rc-chart-header">
          <span class="rc-chart-title">知识点掌握度</span>
          <span class="rc-chart-subtitle">各维度能力评估</span>
        </div>
        <div class="rc-chart-container rc-chart-container--radar">
          <VChart v-if="radarChartOption" :option="radarChartOption" autoresize />
        </div>
      </div>

      <!-- AI 总结 -->
      <div v-if="report.summary" class="rc-section">
        <div class="rc-section-header">
          <span class="rc-section-icon">🤖</span>
          <span class="rc-section-title">AI 总结</span>
        </div>
        <p class="rc-section-body">{{ report.summary }}</p>
      </div>

      <!-- 后续建议 -->
      <div v-if="report.recommendations?.length" class="rc-section">
        <div class="rc-section-header">
          <span class="rc-section-icon">💡</span>
          <span class="rc-section-title">后续建议</span>
        </div>
        <ul class="rc-recommendations">
          <li v-for="(rec, i) in report.recommendations" :key="i">
            <span class="rc-rec-bullet">{{ i + 1 }}</span>
            <span>{{ rec }}</span>
          </li>
        </ul>
      </div>

      <!-- 操作按钮 -->
      <div class="rc-actions">
        <button class="rc-action-btn rc-action-btn--primary" @click="exportReport">
          <span class="rc-action-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/>
            </svg>
          </span>
          <span>导出报告</span>
        </button>
        <button class="rc-action-btn rc-action-btn--secondary" @click="shareReport">
          <span class="rc-action-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"/>
            </svg>
          </span>
          <span>分享报告</span>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, RadarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  RadarComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

// 注册 ECharts 组件（按需加载，减小包体积）
use([
  CanvasRenderer,
  LineChart,
  RadarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  RadarComponent
])

const props = defineProps({
  session: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['complete'])

// 报告数据：优先从 session.report 获取，也支持动态更新
const report = ref(null)
const loadingTimeout = ref(false)
const waitingForReport = ref(true)

// 初始化或更新报告数据
const updateReport = (data) => {
  if (data) {
    report.value = data
    waitingForReport.value = false
    loadingTimeout.value = false
  }
}

// 监听 session.report 动态变化（SSE 推送）
watch(() => props.session?.report, (newReport) => {
  if (newReport) {
    updateReport(newReport)
  }
}, { immediate: true, deep: true })

// 如果 8 秒后仍未收到报告数据，显示超时提示
onMounted(() => {
  setTimeout(() => {
    if (waitingForReport.value && !report.value) {
      loadingTimeout.value = true
      waitingForReport.value = false
    }
  }, 8000)
})

// ===== 学习趋势图配置 =====
const trendChartOption = computed(() => {
  const trendData = report.value?.trend || []
  // 如果没有数据，使用模拟数据展示图表结构
  const days = trendData.length > 0
    ? trendData.map(d => d.day || d.date)
    : ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const hours = trendData.length > 0
    ? trendData.map(d => d.hours || 0)
    : [0, 0, 0, 0, 0, 0, 0]

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(24, 24, 27, 0.9)',
      borderColor: '#27272a',
      borderWidth: 1,
      textStyle: { color: '#fafafa', fontSize: 12 },
      formatter: (params) => {
        const item = params[0]
        return `<div style="font-weight:600;margin-bottom:4px">${item.axisValue}</div>${item.seriesName}: ${item.value} 小时`
      }
    },
    grid: {
      left: 30,
      right: 16,
      top: 20,
      bottom: 24
    },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: { lineStyle: { color: '#27272a' } },
      axisLabel: { color: '#71717a', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#27272a', type: 'dashed' } },
      axisLabel: { color: '#71717a', fontSize: 11 },
      name: '小时',
      nameTextStyle: { color: '#71717a', fontSize: 10 }
    },
    series: [{
      name: '学习时长',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        color: '#10b981',
        width: 2
      },
      itemStyle: {
        color: '#10b981'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(16, 185, 129, 0.25)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.02)' }
          ]
        }
      },
      data: hours
    }]
  }
})

// ===== 雷达图配置 =====
const radarChartOption = computed(() => {
  const radarData = report.value?.radar || []
  const indicators = radarData.length > 0
    ? radarData.map(d => ({ name: d.name, max: d.max || 100 }))
    : [
        { name: '基础知识', max: 100 },
        { name: '理解能力', max: 100 },
        { name: '应用能力', max: 100 },
        { name: '分析能力', max: 100 },
        { name: '综合能力', max: 100 }
      ]
  const values = radarData.length > 0
    ? radarData.map(d => d.value || 0)
    : [0, 0, 0, 0, 0]

  return {
    tooltip: {
      backgroundColor: 'rgba(24, 24, 27, 0.9)',
      borderColor: '#27272a',
      borderWidth: 1,
      textStyle: { color: '#fafafa', fontSize: 12 }
    },
    radar: {
      indicator: indicators,
      radius: '65%',
      center: ['50%', '50%'],
      splitNumber: 4,
      shape: 'circle',
      axisName: {
        color: '#a1a1aa',
        fontSize: 11,
        fontWeight: 500
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(39, 39, 42, 0.6)'
        }
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(16, 185, 129, 0.02)', 'rgba(16, 185, 129, 0.05)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(39, 39, 42, 0.4)'
        }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '当前能力',
        areaStyle: {
          color: 'rgba(16, 185, 129, 0.15)'
        },
        lineStyle: {
          color: '#10b981',
          width: 2
        },
        itemStyle: {
          color: '#10b981'
        }
      }]
    }]
  }
})

// ===== 导出 / 分享（占位功能）=====
const exportReport = () => {
  console.log('导出报告')
  // TODO: 实现 PDF 导出
}

const shareReport = () => {
  console.log('分享报告')
  // TODO: 实现报告分享
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.report-card {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: $space-4;
}

// ===== 头部 =====
.rc-header {
  margin-bottom: $space-6;
}

.rc-title {
  font-size: $text-2xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-1;
}

.rc-desc {
  color: $text-muted;
  margin: 0;
  font-size: $text-sm;
}

// ===== 加载 =====
.rc-loading {
  text-align: center;
  padding: $space-12;
  color: $text-muted;
}

.rc-loading--empty {
  .rc-empty-icon {
    font-size: 3rem;
    margin-bottom: $space-3;
  }
  p { font-size: $text-base; font-weight: 500; }
}

.rc-empty-hint {
  font-size: $text-sm !important;
  color: $text-muted !important;
  font-weight: 400 !important;
  margin-top: $space-2;
}

.rc-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid $border-default;
  border-top-color: $accent-indigo;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto $space-3;
}

@keyframes spin { to { transform: rotate(360deg); } }

// ===== 核心数据 =====
.rc-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $space-3;
  margin-bottom: $space-6;
}

.rc-stat {
  text-align: center;
  padding: $space-4 $space-2;
  background: rgba($bg-surface, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  transition: all $transition-normal;

  &:hover {
    border-color: rgba($accent-indigo, 0.2);
    transform: translateY(-2px);
  }
}

.rc-stat-value {
  display: block;
  font-family: $font-data;
  font-size: $text-xl;
  font-weight: 700;
  color: $accent-indigo;
  margin-bottom: $space-1;
}

.rc-stat-unit {
  font-size: $text-sm;
  font-weight: 500;
  opacity: 0.7;
}

.rc-stat-label {
  font-size: 10px;
  color: $text-muted;
  font-weight: 500;
}

// ===== 图表 =====
.rc-chart-section {
  margin-bottom: $space-5;
  background: rgba($bg-surface, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  overflow: hidden;
}

.rc-chart-header {
  padding: $space-3 $space-5;
  border-bottom: 1px solid $border-subtle;
  display: flex;
  align-items: center;
  gap: $space-3;
}

.rc-chart-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
}

.rc-chart-subtitle {
  font-size: 11px;
  color: $text-muted;
}

.rc-chart-container {
  height: 240px;
  padding: $space-3;

  &--radar {
    height: 280px;
  }
}

// ===== 文本区域 =====
.rc-section {
  margin-bottom: $space-4;
  padding: $space-5;
  background: rgba($bg-surface, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
}

.rc-section-header {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.rc-section-icon {
  font-size: 1.1rem;
}

.rc-section-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
}

.rc-section-body {
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.7;
  margin: 0;
}

// ===== 建议列表 =====
.rc-recommendations {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.rc-rec-bullet {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba($accent-indigo, 0.12);
  color: $accent-indigo;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
}

.rc-recommendations li {
  display: flex;
  align-items: flex-start;
  gap: $space-2;
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.6;
}

// ===== 操作按钮 =====
.rc-actions {
  display: flex;
  gap: $space-3;
  justify-content: center;
  padding: $space-4 0 $space-2;
}

.rc-action-btn {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: 8px 20px;
  border-radius: $radius-md;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &--primary {
    background: rgba($accent-indigo, 0.12);
    border: 1px solid rgba($accent-indigo, 0.25);
    color: $accent-indigo;

    &:hover {
      background: rgba($accent-indigo, 0.2);
      transform: translateY(-1px);
    }
  }

  &--secondary {
    background: transparent;
    border: 1px solid $border-default;
    color: $text-secondary;

    &:hover {
      border-color: $border-medium;
      color: $text-primary;
    }
  }
}

.rc-action-icon {
  display: flex;
}
</style>