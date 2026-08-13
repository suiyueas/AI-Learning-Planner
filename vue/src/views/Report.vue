<template>
  <div class="report-page">
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
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg>
        <span>返回</span>
      </button>
      <h1 class="page-title">
        <span class="title-icon">📊</span>
        <span class="title-text">学情报告</span>
      </h1>
      <div class="period-picker">
        <input v-model="startDate" type="date" class="date-input" :max="endDate" />
        <span class="period-sep">至</span>
        <input v-model="endDate" type="date" class="date-input" :min="startDate" :max="todayStr" />
      </div>
      <button class="report-btn" :disabled="generating" @click="generateReport">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" /></svg>
        <span>{{ generating ? '生成中...' : '生成报告' }}</span>
      </button>
    </header>

    <div class="report-content">
      <!-- 加载失败重试 -->
      <div v-if="loadError" class="load-error">
        <span>⚠️ 生成报告失败，请检查网络后重试</span>
        <button class="retry-btn" @click="generateReport">🔄 重试</button>
      </div>

      <template v-if="report">
        <!-- 报告期间 -->
        <div class="period-bar">
          报告期间：{{ report.periodStart }} ~ {{ report.periodEnd }}
          <span class="period-update">（数据更新于 {{ report.dataUpdateAt }}）</span>
        </div>

        <!-- 学习概览 -->
        <div class="summary-row">
          <div class="summary-card">
            <span class="summary-icon">⏱️</span>
            <span class="summary-label">学习时长</span>
            <span class="summary-value">{{ report.overview?.totalLearningHours ?? 0 }} 小时</span>
          </div>
          <div class="summary-card">
            <span class="summary-icon">📚</span>
            <span class="summary-label">完成任务</span>
            <span class="summary-value">{{ report.overview?.totalTasksCompleted ?? 0 }} 个</span>
          </div>
          <div class="summary-card">
            <span class="summary-icon">✅</span>
            <span class="summary-label">打卡天数</span>
            <span class="summary-value">{{ report.overview?.checkinDays ?? 0 }} 天</span>
          </div>
          <div class="summary-card">
            <span class="summary-icon">🎯</span>
            <span class="summary-label">平均正确率</span>
            <span class="summary-value">{{ report.overview?.overallAccuracy ?? '--' }}{{ report.overview?.overallAccuracy !== '--' ? '%' : '' }}</span>
          </div>
        </div>

        <div class="chart-area">
          <!-- 科目能力矩阵 -->
          <div class="chart-card">
            <h3 class="chart-title">🧠 科目能力矩阵</h3>
            <div v-if="!report.matrix?.hasAssessmentData" class="chart-empty with-action">
              <p class="empty-hint">{{ report.matrix?.dataSourceHint || '暂未参加测评' }}</p>
              <button v-if="report.matrix?.callToAction" class="action-btn" @click="handleMatrixAction">
                {{ report.matrix?.callToAction }}
              </button>
              <div v-if="report.matrix?.learningProgress" class="learning-progress">
                <div class="progress-item">
                  <span class="progress-label">已学习节点</span>
                  <span class="progress-value">{{ report.matrix.learningProgress.totalNodes }} 个</span>
                </div>
                <div class="progress-item">
                  <span class="progress-label">累计时长</span>
                  <span class="progress-value">{{ Math.round(report.matrix.learningProgress.totalTimeMinutes / 60 * 10) / 10 }} 小时</span>
                </div>
              </div>
            </div>
            <div v-else-if="subjectMatrix.length === 0" class="chart-empty">期间内暂无测评数据</div>
            <div v-else class="radar-items">
              <div v-for="dim in subjectMatrix" :key="dim.name" class="dimension-item">
                <div class="dim-header">
                  <span class="dim-name">{{ dim.name }}<span class="dim-meta">（{{ dim.attempts }}次 · {{ dim.level }}）</span></span>
                  <span class="dim-score">{{ dim.accuracy }}%</span>
                </div>
                <div class="dim-bar">
                  <div class="dim-fill" :style="{ width: dim.accuracy + '%', background: masteryColor(dim.accuracy) }"></div>
                </div>
              </div>
            </div>
          </div>
          <!-- 科目测评分布 -->
          <div class="chart-card">
            <h3 class="chart-title">📊 科目测评分布</h3>
            <div v-if="subjectDistribution.length === 0" class="chart-empty">期间内暂无测评记录</div>
            <div v-else class="chart-bars">
              <div v-for="(item, i) in subjectDistribution" :key="item.name" class="bar-group">
                <div class="bar-wrapper">
                  <div class="bar-fill" :style="{ height: Math.max((item.count / maxDistribution * 100), 4) + '%', background: barColor(i) }">
                    <span class="bar-value">{{ item.count }}次</span>
                  </div>
                </div>
                <span class="bar-label">{{ item.name }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 学习建议 -->
        <div v-if="suggestions.length > 0" class="suggestions">
          <h3 class="suggest-title">💡 学习建议</h3>
          <div v-for="(sug, i) in suggestions" :key="i" class="suggestion-item">
            <span class="suggestion-icon">{{ sug.icon }}</span>
            <p class="suggestion-text">{{ sug.text }}</p>
          </div>
        </div>
      </template>

      <div v-if="!report && !loadError" class="empty-state">
        <div class="empty-icon-wrap">📊</div>
        <h3 class="empty-title">还没有学情报告</h3>
        <p class="empty-desc">选择日期范围后点击「生成报告」，获取基于真实学习数据的分析</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { generateReport as apiGenerateReport } from '@/api/reportApi'

const router = useRouter()
const report = ref(null)
const generating = ref(false)
const loadError = ref(false)

// 默认日期范围：近 30 天
const todayStr = new Date().toISOString().slice(0, 10)
const defaultStart = new Date(Date.now() - 29 * 24 * 3600 * 1000).toISOString().slice(0, 10)
const startDate = ref(defaultStart)
const endDate = ref(todayStr)

// 科目能力矩阵（matrix.subjects 对象 → 数组）
const subjectMatrix = computed(() => {
  const subjects = report.value?.matrix?.subjects
  if (!subjects || typeof subjects !== 'object') return []
  return Object.entries(subjects).map(([name, data]) => ({ name, ...data }))
})

// 科目测评分布（overview.subjectDistribution 对象 → 数组，按次数降序）
const subjectDistribution = computed(() => {
  const dist = report.value?.overview?.subjectDistribution
  if (!dist || typeof dist !== 'object') return []
  return Object.entries(dist)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
})

const maxDistribution = computed(() => {
  const max = Math.max(...subjectDistribution.value.map(d => d.count), 1)
  return max
})

// 学习建议（推荐板块的弱项/强项/建议合并展示）
const suggestions = computed(() => {
  const rec = report.value?.recommendations
  if (!rec) return []
  const list = []

  const primaryIcon = {
    'start_learning': '🚨',
    'review_weak': '📚',
    'start_assessment': '📝',
    'challenge_higher': '🏆',
    'maintain_progress': '👍'
  }[rec.primaryAction] || '💡'

  if (Array.isArray(rec.suggestions)) {
    rec.suggestions.forEach((s, i) => list.push({ icon: i === 0 ? primaryIcon : '🎯', text: s }))
  }
  return list
})

const masteryColor = (accuracy) => {
  if (accuracy >= 80) return 'linear-gradient(90deg, #00f5d4, #10b981)'
  if (accuracy >= 60) return 'linear-gradient(90deg, #3a86ff, #7b61ff)'
  return 'linear-gradient(90deg, #f59e0b, #ef4444)'
}

const barColor = (index) => {
  const colors = ['rgba(0,245,212,0.65)', 'rgba(123,97,255,0.65)', 'rgba(58,134,255,0.65)', 'rgba(245,158,11,0.65)', 'rgba(16,185,129,0.65)', 'rgba(239,68,68,0.65)']
  return colors[index % colors.length]
}

function handleMatrixAction() {
  const actionType = report.value?.matrix?.actionType
  if (actionType === 'start_assessment') {
    router.push('/modules/assessment')
  }
}

async function generateReport() {
  if (!startDate.value || !endDate.value) {
    return
  }
  generating.value = true
  loadError.value = false
  try {
    const res = await apiGenerateReport(startDate.value, endDate.value, ['overview', 'matrix', 'recommendations'])
    report.value = res?.data || null
  } catch (e) {
    loadError.value = true
    report.value = null
  } finally {
    generating.value = false
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/modules')
}

onMounted(generateReport)
</script>

<style lang="scss" scoped>
.report-page {
  min-height: calc(100vh - 68px); background: #0a0a1a; position: relative;
}
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora { position: absolute; inset: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba(0,245,212,0.06) 0%, transparent 50%), radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%), radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift { 0%,100% { transform: scale(1) rotate(0deg); } 33% { transform: scale(1.08) rotate(0.8deg); } 66% { transform: scale(0.95) rotate(-0.6deg); } }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(0,245,212,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px); background-size: 40px 40px; animation: gridPulse 8s ease-in-out infinite alternate; }
@keyframes gridPulse { 0% { opacity: 0.3; } 100% { opacity: 0.6; } }

.page-header {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; gap: 16px;
  padding: 16px 32px;
  background: rgba(10,10,26,0.85); backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(100,100,180,0.08);
}
.back-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; background: rgba(100,100,180,0.06);
  border: 1px solid rgba(100,100,180,0.1); border-radius: 8px;
  color: #c0c0e0; font-size: 0.82rem; cursor: pointer;
  transition: all 0.25s ease;
  &:hover { border-color: rgba(0,245,212,0.2); color: #00f5d4; }
}
.page-title { flex: 1; display: flex; align-items: center; gap: 10px; }
.title-icon { font-size: 1.3rem; }
.title-text {
  font-size: 1.05rem; font-weight: 700;
  background: linear-gradient(135deg, #00f5d4, #3a86ff);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.report-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px;
  background: linear-gradient(135deg, rgba(123,97,255,0.15), rgba(0,85,255,0.1));
  border: 1px solid rgba(123,97,255,0.2); border-radius: 10px;
  color: #a78bfa; font-size: 0.85rem; font-weight: 600; cursor: pointer;
  transition: all 0.25s ease;
  &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 0 20px rgba(123,97,255,0.15); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.report-content {
  max-width: 1000px; margin: 0 auto;
  padding: 24px 32px 60px; position: relative; z-index: 1;
}

.summary-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px;
}
.summary-card {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 20px 16px;
  background: rgba(17,17,39,0.6); backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.1); border-radius: 14px;
  .summary-icon { font-size: 1.5rem; }
  .summary-label { font-size: 0.72rem; color: #9090b8; }
  .summary-value { font-size: 1.2rem; font-weight: 800; font-family: 'JetBrains Mono', monospace; color: #00f5d4; }
}

.chart-area { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
.chart-card {
  padding: 20px; background: rgba(17,17,39,0.5); backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.08); border-radius: 14px;
}
.chart-title { font-size: 0.9rem; font-weight: 700; color: #e8e8ff; margin: 0 0 16px; }

.chart-bars { display: flex; align-items: flex-end; gap: 8px; height: 180px; }
.bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; height: 100%; }
.bar-wrapper { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.bar-fill {
  width: 60%; border-radius: 6px 6px 0 0; position: relative; min-height: 4px;
  transition: height 0.6s cubic-bezier(0.34,1.56,0.64,1);
  .bar-value { position: absolute; top: -18px; left: 50%; transform: translateX(-50%); font-size: 0.65rem; color: #c0c0e0; font-family: 'JetBrains Mono', monospace; white-space: nowrap; }
}
.bar-label { font-size: 0.7rem; color: #9090b8; }

.radar-items { display: flex; flex-direction: column; gap: 14px; }
.dimension-item { display: flex; flex-direction: column; gap: 6px; }
.dim-header { display: flex; justify-content: space-between; }
.dim-name { font-size: 0.8rem; color: #c0c0e0; }
.dim-score { font-size: 0.8rem; font-weight: 700; font-family: 'JetBrains Mono', monospace; color: #e8e8ff; }
.dim-bar { height: 6px; background: rgba(100,100,180,0.1); border-radius: 3px; overflow: hidden; }
.dim-fill { height: 100%; border-radius: 3px; transition: width 0.6s cubic-bezier(0.34,1.56,0.64,1); }

.suggestions {
  margin-bottom: 24px; padding: 20px;
  background: rgba(17,17,39,0.5); backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.08); border-radius: 14px;
}
.suggest-title { font-size: 0.9rem; font-weight: 700; color: #e8e8ff; margin: 0 0 14px; }
.suggestion-item { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid rgba(100,100,180,0.04); &:last-child { border: none; } }
.suggestion-icon { font-size: 1rem; flex-shrink: 0; margin-top: 2px; }
.suggestion-text { font-size: 0.82rem; color: #c0c0e0; line-height: 1.6; margin: 0; }

.timeline-title { font-size: 0.9rem; font-weight: 700; color: #e8e8ff; margin: 0 0 14px; }
.timeline-item {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 16px; margin-bottom: 8px;
  background: rgba(17,17,39,0.3); border-radius: 12px;
  border: 1px solid rgba(100,100,180,0.04); cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: rgba(0,245,212,0.1); }
  &.active { border-color: rgba(0,245,212,0.2); background: rgba(0,245,212,0.03); }
}
.timeline-dot { width: 8px; height: 8px; border-radius: 50%; background: #00f5d4; flex-shrink: 0; }
.timeline-content { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.timeline-date { font-size: 0.7rem; color: #8080a8; }
.timeline-desc { font-size: 0.82rem; color: #c0c0e0; }
.timeline-badge { font-size: 0.65rem; padding: 2px 8px; border-radius: 8px; background: rgba(0,245,212,0.1); color: #00f5d4; }

.empty-state { text-align: center; padding: 80px 20px; }
.empty-icon-wrap { font-size: 3.5rem; margin-bottom: 12px; }
.empty-title { font-size: 1.2rem; font-weight: 700; color: #e8e8ff; margin: 0 0 6px; }
.empty-desc { font-size: 0.85rem; color: #9090b8; margin: 0; }

@media (max-width: 1024px) {
  .page-header { padding: 12px 20px; }
  .report-content { padding: 20px; }
  .chart-area { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .page-header { padding: 10px 12px; gap: 10px; flex-wrap: wrap; }
  .back-btn span, .report-btn span { display: none; }
  .summary-row { grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .summary-card { padding: 14px 10px; }
  .report-content { padding: 12px; }
  .chart-bars { height: 140px; }
}

/* ===== 日期范围选择 ===== */
.period-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(17, 17, 39, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 10px;
}

.date-input {
  padding: 5px 8px;
  background: rgba(10, 10, 26, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 6px;
  color: #c0c0e0;
  font-size: 0.78rem;
  outline: none;
  color-scheme: dark;
  transition: border-color 0.25s ease;
}

.date-input:focus {
  border-color: rgba(0, 245, 212, 0.3);
}

.period-sep {
  font-size: 0.78rem;
  color: #8080a8;
}

.period-bar {
  margin-bottom: 20px;
  padding: 10px 16px;
  font-size: 0.78rem;
  color: #9090b8;
  background: rgba(17, 17, 39, 0.4);
  border: 1px dashed rgba(100, 100, 180, 0.15);
  border-radius: 10px;
}

/* ===== 加载失败 / 空态 ===== */
.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  color: #EF4444;
  font-size: 0.9rem;
}

.retry-btn {
  padding: 6px 14px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 8px;
  color: #EF4444;
  font-size: 0.85rem;
  cursor: pointer;
  white-space: nowrap;
}

.chart-empty {
  padding: 40px 20px;
  text-align: center;
  font-size: 0.85rem;
  color: #9090b8;
}

.chart-empty.with-action {
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-hint {
  margin: 0;
  font-size: 0.8rem;
  color: #9090b8;
}

.action-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba(0,245,212,0.15), rgba(58,134,255,0.1));
  border: 1px solid rgba(0,245,212,0.25);
  border-radius: 8px;
  color: #00f5d4;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.25s;
  &:hover {
    background: linear-gradient(135deg, rgba(0,245,212,0.2), rgba(58,134,255,0.15));
    transform: translateY(-1px);
    box-shadow: 0 0 16px rgba(0,245,212,0.1);
  }
}

.learning-progress {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.progress-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.progress-label {
  font-size: 0.68rem;
  color: #8080a8;
}

.progress-value {
  font-size: 0.85rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  color: #00f5d4;
}

.period-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 0.78rem;
  color: #9090b8;
}

.period-update {
  color: #606080;
  font-size: 0.72rem;
}

.dim-meta {
  font-size: 0.68rem;
  color: #8080a8;
  font-weight: 400;
}
</style>