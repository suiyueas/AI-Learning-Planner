<template>
  <div class="home-page">
    <div class="bg-aurora">
      <div class="aurora-layer aurora-1"></div>
      <div class="aurora-layer aurora-2"></div>
    </div>

    <!-- STATUS BAR -->
    <header class="status-bar glass-panel">
      <div class="status-left">
        <div class="greeting">
          <span class="greeting-text">{{ greetingText }}，<strong>{{ displayName }}</strong></span>
        </div>
        <div class="agent-status">
          <span class="status-dot-alive"></span>
          <span class="status-text">{{ agentStatusText }}</span>
        </div>
      </div>
      <div class="status-right">
        <CheckinButton @refresh="handleCheckinRefresh" />
        <div class="stat-chip" v-if="continuousDays > 0">
          <Flame :size="14" />
          <span>{{ continuousDays }}天连续</span>
        </div>
        <div class="stat-chip" v-if="todayHours > 0">
          <Clock :size="14" />
          <span>今日 {{ todayHours }}h</span>
        </div>
        <div class="avatar-wrap" @click="router.push('/profile')">
          <img v-if="hasAvatar" :src="avatarUrl" class="avatar-img" />
          <span v-else class="avatar-fallback">{{ avatarLetter }}</span>
        </div>
      </div>
    </header>

    <!-- 快捷入口 -->
    <section class="quick-access-section">
      <div class="quick-access-grid">
        <div class="quick-access-item" @click="router.push('/learning-path')">
          <span class="qa-icon">📚</span>
          <span class="qa-label">学习路径</span>
        </div>
        <div class="quick-access-item" @click="router.push('/knowledge')">
          <span class="qa-icon">📖</span>
          <span class="qa-label">知识库</span>
        </div>
        <div class="quick-access-item" @click="router.push('/assessment')">
          <span class="qa-icon">📝</span>
          <span class="qa-label">能力测评</span>
        </div>
        <div class="quick-access-item" @click="router.push('/agents')">
          <span class="qa-icon">🤖</span>
          <span class="qa-label">智能体中心</span>
        </div>
        <div class="quick-access-item" @click="router.push('/achievements')">
          <span class="qa-icon">🏆</span>
          <span class="qa-label">成就打卡</span>
        </div>
      </div>
    </section>

    <!-- 学习报告摘要 -->
    <section class="report-summary-section" v-if="reportStats">
      <div class="report-summary-card glass-card">
        <div class="rs-header">
          <span class="rs-icon">📊</span>
          <h3 class="rs-title">学习报告摘要</h3>
        </div>
        <div class="rs-stats">
          <div class="rs-stat-item">
            <span class="rs-stat-value">{{ reportStats.totalHours || 0 }}h</span>
            <span class="rs-stat-label">学习时长</span>
          </div>
          <div class="rs-stat-item">
            <span class="rs-stat-value">{{ reportStats.completedTasks || 0 }}</span>
            <span class="rs-stat-label">完成任务</span>
          </div>
          <div class="rs-stat-item">
            <span class="rs-stat-value">{{ reportStats.checkinDays || 0 }}天</span>
            <span class="rs-stat-label">打卡天数</span>
          </div>
          <div class="rs-stat-item">
            <span class="rs-stat-value">{{ reportStats.avgAccuracy || '--' }}{{ reportStats.avgAccuracy ? '%' : '' }}</span>
            <span class="rs-stat-label">平均正确率</span>
          </div>
        </div>
        <button class="rs-link-btn" @click="router.push('/statistics')">查看完整报告 →</button>
      </div>
    </section>

    <!-- AI 智能洞察 -->
    <AIAssistantCard />

    <!-- AGENT FOCUS CARD -->
    <section class="agent-focus">
      <div v-if="activePath" class="focus-card glass-card sweep-card">
        <div class="focus-header">
          <div class="focus-agent">
            <div class="agent-avatar"><Bot :size="20" /><span class="agent-pulse"></span></div>
            <div class="agent-speech">
              <span class="speech-label">AI 学习助手</span>
              <p class="speech-text">{{ agentFocusText }}</p>
            </div>
          </div>
        </div>
        <div class="focus-content" @click="router.push(`/learning-path/${activePath.id}`)">
          <div class="focus-path-info">
            <h2 class="focus-title">{{ activePath.name }}</h2>
            <div class="focus-next" v-if="activePath.nextNode">
              <Zap :size="14" class="next-icon" />
              <span>下一步：<strong>{{ activePath.nextNode }}</strong></span>
            </div>
          </div>
          <div class="focus-progress">
            <div class="progress-ring-wrap">
              <svg class="progress-ring" viewBox="0 0 60 60">
                <circle class="ring-bg" cx="30" cy="30" r="26" />
                <circle class="ring-fill" cx="30" cy="30" r="26" :style="{ strokeDasharray: `${activePath.progress * 1.634} 163.4` }" />
              </svg>
              <span class="ring-text">{{ activePath.progress }}%</span>
            </div>
          </div>
        </div>
        <div class="focus-actions">
          <button class="btn-primary" @click.stop="router.push(`/learning-path/${activePath.id}`)"><Play :size="16" /> 继续学习</button>
          <button class="btn-ghost" @click.stop="router.push('/chat')"><MessageSquare :size="16" /> 问 AI</button>
        </div>
      </div>
      <div v-else class="focus-card glass-card">
        <div class="focus-header">
          <div class="focus-agent">
            <div class="agent-avatar"><Bot :size="20" /><span class="agent-pulse"></span></div>
            <div class="agent-speech">
              <span class="speech-label">AI 学习助手</span>
              <p class="speech-text">我还没有为你规划学习路径。告诉我你的目标，我来帮你制定专属计划。</p>
            </div>
          </div>
        </div>
        <div class="focus-empty">
          <div class="empty-icon">🎯</div>
          <h2 class="empty-title">开启你的学习旅程</h2>
          <p class="empty-desc">告诉我你想学什么，我会为你诊断水平、规划路径、生成练习</p>
        </div>
        <div class="focus-actions">
          <button class="btn-primary" @click="router.push('/goal-setting')"><Target :size="16" /> 设定目标</button>
          <button class="btn-ghost" @click="router.push('/chat')"><MessageSquare :size="16" /> 先聊聊</button>
        </div>
      </div>
    </section>

    <!-- COMPETENCY MAP -->
    <section class="competency-section" v-if="competencies.length > 0">
      <div class="section-header">
        <div class="section-title-wrap"><Radar :size="18" class="section-icon" /><h2 class="section-title">你的能力地图</h2></div>
        <button class="link-btn" @click="router.push('/statistics')">查看详情 <ArrowRight :size="14" /></button>
      </div>
      <div class="competency-grid">
        <div v-for="item in competencies" :key="item.name" class="competency-card glass-card">
          <div class="comp-header">
            <span class="comp-name">{{ item.name }}</span>
            <span class="comp-level" :class="item.level">{{ item.levelText }}</span>
          </div>
          <div class="comp-bar-wrap">
            <div class="comp-bar"><div class="comp-bar-fill" :style="{ width: item.mastery + '%' }" :class="item.level"></div></div>
            <span class="comp-percent">{{ item.mastery }}%</span>
          </div>
        </div>
      </div>
    </section>

    <!-- TODAY'S TASKS -->
    <section class="tasks-section" v-if="todayTasks.length > 0">
      <div class="section-header">
        <div class="section-title-wrap"><ClipboardList :size="18" class="section-icon" /><h2 class="section-title">Agent 今日为你安排</h2></div>
        <span class="task-count">{{ todayTasks.length }}项 · 约 {{ estimatedMinutes }} 分钟</span>
      </div>
      <div class="tasks-list glass-card">
        <div v-for="(task, idx) in todayTasks" :key="task.id" class="task-item" :class="{ completed: task.status === 'COMPLETED', current: idx === currentTaskIndex }">
          <div class="task-num" :class="{ done: task.status === 'COMPLETED' }">
            <CheckCircle2 v-if="task.status === 'COMPLETED'" :size="16" />
            <span v-else>{{ idx + 1 }}</span>
          </div>
          <div class="task-info">
            <span class="task-name">{{ task.title || task.name || '学习任务' }}</span>
            <span class="task-meta">
              <span v-if="task.estimatedMinutes" class="task-time"><Clock :size="11" /> {{ task.estimatedMinutes }}min</span>
              <span v-if="task.priority" class="task-priority" :class="task.priority.toLowerCase()">{{ task.priority === 'HIGH' ? '重点' : task.priority === 'MEDIUM' ? '巩固' : '回顾' }}</span>
            </span>
          </div>
          <div v-if="idx === currentTaskIndex && task.status !== 'COMPLETED'" class="task-current-badge"><Zap :size="12" /> 当前</div>
        </div>
        <button class="start-learning-btn" @click="startLearning"><Play :size="16" /> {{ currentTaskIndex >= 0 ? '继续当前任务' : '开始学习' }}</button>
      </div>
    </section>

    <!-- AGENT CENTER ENTRANCE -->
    <section class="agent-center-section">
      <div class="section-header">
        <div class="section-title-wrap"><Bot :size="18" class="section-icon" /><h2 class="section-title">🤖 智能体中心</h2></div>
        <button class="link-btn" @click="router.push('/agents')">进入中心 <ArrowRight :size="14" /></button>
      </div>
      <div class="agent-center-card glass-card" @click="router.push('/agents')">
        <div class="center-header">
          <div class="center-intro">
            <p class="center-desc">7 个专业助手为你服务</p>
            <p class="center-hint">点击进入智能体中心，或直接输入需求让 AI 自动分配</p>
          </div>
        </div>
        <div class="agent-icons-row">
          <div class="agent-icon-item" title="诊断Agent"><span class="agent-icon-badge">🔍</span><span class="agent-icon-label">诊断</span></div>
          <div class="agent-icon-item" title="规划Agent"><span class="agent-icon-badge">🗺️</span><span class="agent-icon-label">规划</span></div>
          <div class="agent-icon-item" title="答疑Agent"><span class="agent-icon-badge">💬</span><span class="agent-icon-label">答疑</span></div>
          <div class="agent-icon-item" title="报告Agent"><span class="agent-icon-badge">📊</span><span class="agent-icon-label">报告</span></div>
          <div class="agent-icon-item" title="干预Agent"><span class="agent-icon-badge">🛡️</span><span class="agent-icon-label">干预</span></div>
          <div class="agent-icon-item" title="激励Agent"><span class="agent-icon-badge">🏆</span><span class="agent-icon-label">激励</span></div>
          <div class="agent-icon-item" title="更多功能"><span class="agent-icon-badge">⚡</span><span class="agent-icon-label">更多</span></div>
        </div>
        <div class="center-footer">
          <div class="smart-input-hint">💡 不确定选哪个？直接输入你的需求，AI 帮你分配</div>
        </div>
      </div>
    </section>

    <!-- AGENT SCENARIOS -->
    <section class="agent-scenarios">
      <div class="section-header">
        <div class="section-title-wrap"><Wand2 :size="18" class="section-icon" /><h2 class="section-title">让 Agent 帮你</h2></div>
      </div>
      <div class="scenario-grid">
        <div class="scenario-card glass-card sweep-card" @click="router.push('/capability/diagnosis')">
          <div class="scenario-icon" style="background: rgba(212,168,83,0.12)"><Stethoscope :size="22" class="scenario-ico" /></div>
          <div class="scenario-info"><h3 class="scenario-title">诊断我的水平</h3><p class="scenario-desc">AI 分析你的知识薄弱点</p></div>
          <ArrowRight :size="16" class="scenario-arrow" />
        </div>
        <div class="scenario-card glass-card sweep-card" @click="router.push('/chat')">
          <div class="scenario-icon" style="background: rgba(91,154,191,0.12)"><MessageCircle :size="22" class="scenario-ico" /></div>
          <div class="scenario-info"><h3 class="scenario-title">问 AI 一个问题</h3><p class="scenario-desc">苏格拉底式引导，不直接给答案</p></div>
          <ArrowRight :size="16" class="scenario-arrow" />
        </div>
        <div class="scenario-card glass-card sweep-card" @click="router.push('/goal-setting')">
          <div class="scenario-icon" style="background: rgba(154,130,200,0.12)"><Route :size="22" class="scenario-ico" /></div>
          <div class="scenario-info"><h3 class="scenario-title">规划学习路径</h3><p class="scenario-desc">AI 根据目标生成专属路径</p></div>
          <ArrowRight :size="16" class="scenario-arrow" />
        </div>
        <div class="scenario-card glass-card sweep-card" @click="router.push('/exercise')">
          <div class="scenario-icon" style="background: rgba(90,171,138,0.12)"><Dumbbell :size="22" class="scenario-ico" /></div>
          <div class="scenario-info"><h3 class="scenario-title">生成练习题</h3><p class="scenario-desc">针对薄弱点出题，即时反馈</p></div>
          <ArrowRight :size="16" class="scenario-arrow" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Flame, Clock, Bot, Play, MessageSquare, Target, Zap,
  ClipboardList, CheckCircle2, ArrowRight, Radar, Wand2,
  Stethoscope, MessageCircle, Route, Dumbbell
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useStatsStore } from '@/stores/statsStore'
import { useDailyTaskStore } from '@/stores/dailyTaskStore'
import { getActivePath } from '@/api/learningPath'
import { getProgressCompetency } from '@/api/statsApi'
import AIAssistantCard from '@/components/AIAssistantCard.vue'
import CheckinButton from '@/components/CheckinButton.vue'

const router = useRouter()
const authStore = useAuthStore()
const statsStore = useStatsStore()
const dailyTaskStore = useDailyTaskStore()

const displayName = computed(() => authStore.displayName || '同学')
const hasAvatar = computed(() => authStore.hasAvatar)
const avatarUrl = computed(() => authStore.user?.avatarUrl || '')
const avatarLetter = computed(() => displayName.value.charAt(0))
const continuousDays = computed(() => authStore.user?.learningStats?.continuousDays || statsStore.dashboardStats?.continuousDays || 0)
const todayHours = computed(() => statsStore.dashboardStats?.todayHours || 0)

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const activePath = ref(null)

const reportStats = computed(() => {
  const stats = statsStore.dashboardStats
  if (!stats) return null
  return {
    totalHours: stats.totalHours || stats.todayHours || 0,
    completedTasks: stats.completedTasks || 0,
    checkinDays: stats.continuousDays || 0,
    avgAccuracy: stats.avgAccuracy || '--'
  }
})

const handleCheckinRefresh = () => {
  statsStore.fetchDashboardStats()
}

const agentStatusText = computed(() => {
  if (activePath.value) return `正在监控你的「${activePath.value.name}」学习进度`
  return '等待你的学习目标，随时准备出发'
})

const agentFocusText = computed(() => {
  if (!activePath.value) return ''
  const p = activePath.value.progress
  if (p === 0) return '你刚开启这条路，我帮你拆解了第一步，准备好了就出发。'
  if (p < 30) return '你正在打基础，保持节奏，我帮你巩固刚学的知识点。'
  if (p < 70) return '进展不错！接下来的内容有点难度，我会重点帮你练习。'
  if (p < 100) return '快到终点了！最后的冲刺阶段，我帮你做一次全面复习。'
  return '恭喜完成！我帮你整理了学习报告，要不要回顾一下？'
})

const competencies = ref([])
const todayTasks = computed(() => dailyTaskStore.todayPlan?.tasks || [])
const estimatedMinutes = computed(() => todayTasks.value.reduce((sum, t) => sum + (t.estimatedMinutes || 10), 0))
const currentTaskIndex = computed(() => todayTasks.value.findIndex(t => t.status !== 'COMPLETED'))

const startLearning = () => {
  if (activePath.value) router.push(`/learning-path/${activePath.value.id}`)
  else router.push('/goal-setting')
}

onMounted(async () => {
  await statsStore.fetchDashboardStats()
  try {
    const res = await getActivePath()
    const data = res?.data ?? res
    if (data?.hasPath && data?.path) {
      activePath.value = {
        id: data.path.id,
        name: (data.path.name || '学习路径').replace(/^【[^】]*】/, ''),
        progress: data.progress?.percentage || 0,
        nextNode: data.nextNode?.nodeName || null,
        status: data.status
      }
      dailyTaskStore.fetchTodayTasks(data.path.id).catch(() => {})
    }
  } catch (e) {
    console.warn('加载学习路径失败:', e.message)
  }
  try {
    const res = await getProgressCompetency()
    const data = res?.data ?? res
    if (Array.isArray(data) && data.length > 0) {
      competencies.value = data.slice(0, 5).map(item => ({
        name: item.name,
        mastery: item.mastery || 0,
        level: item.mastery >= 80 ? 'advanced' : item.mastery >= 50 ? 'intermediate' : 'beginner',
        levelText: item.mastery >= 80 ? '精通' : item.mastery >= 50 ? '进阶' : '入门'
      }))
    }
  } catch (e) { /* silent */ }
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.home-page {
  min-height: 100vh;
  position: relative;
  padding: 24px 32px;
  max-width: 1000px;
  margin: 0 auto;
}

// ---- Aurora background ----
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
  width: 700px;
  height: 700px;
  top: -250px;
  right: -150px;
  background: radial-gradient(circle, rgba($accent-primary, 0.07) 0%, transparent 70%);
}

.aurora-2 {
  width: 600px;
  height: 600px;
  bottom: -200px;
  left: -150px;
  background: radial-gradient(circle, rgba($accent-blue, 0.07) 0%, transparent 70%);
  animation-delay: -7s;
}

@keyframes aurora {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.1); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(20px, 10px) scale(1.05); }
}

// ---- Glass utilities ----
.glass-panel {
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  background: rgba($bg-elevated, 0.7);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
}

.glass-card {
  @include card-base;
}

// ---- Status bar ----
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
}

.status-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.greeting-text {
  font-size: 1rem;
  color: $text-primary;

  strong {
    font-weight: 700;
    color: #fff;
  }
}

.agent-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot-alive {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: $accent-emerald;
  animation: pulse-dot 2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 0 0 rgba($accent-emerald, 0.5); }
  50% { box-shadow: 0 0 0 6px rgba($accent-emerald, 0); }
}

.status-text {
  font-size: 0.8rem;
  color: $text-secondary;
}

.status-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: $radius-full;
  background: rgba($accent-primary, 0.06);
  font-size: 0.78rem;
  color: $text-secondary;
}

.avatar-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, $accent-primary, $accent-blue);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform $transition-normal, box-shadow $transition-normal;

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 0 14px rgba($accent-primary, 0.35);
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 0.9rem;
  font-weight: 600;
  color: #3d2e10;
}

// ---- Quick Access Section ----
.quick-access-section {
  margin-top: 20px;
}

.quick-access-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-access-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: rgba($bg-elevated, 0.6);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all $transition-fast;
  min-width: 90px;

  &:hover {
    background: rgba($accent-primary, 0.08);
    border-color: rgba($accent-primary, 0.3);
    transform: translateY(-2px);
  }

  .qa-icon {
    font-size: 1.5rem;
  }

  .qa-label {
    font-size: 0.8rem;
    color: $text-secondary;
    font-weight: 500;
  }
}

// ---- Report Summary Section ----
.report-summary-section {
  margin-top: 20px;
}

.report-summary-card {
  padding: 20px 24px;
}

.rs-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;

  .rs-icon {
    font-size: 1.3rem;
  }

  .rs-title {
    font-size: 1rem;
    font-weight: 600;
    color: $text-primary;
    margin: 0;
  }
}

.rs-stats {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.rs-stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .rs-stat-value {
    font-size: 1.4rem;
    font-weight: 700;
    color: $accent-primary;
  }

  .rs-stat-label {
    font-size: 0.75rem;
    color: $text-muted;
  }
}

.rs-link-btn {
  margin-top: 16px;
  background: none;
  border: none;
  color: $accent-primary;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: opacity $transition-fast;

  &:hover {
    opacity: 0.8;
  }
}

// ---- Agent Focus Card ----
.agent-focus {
  margin-top: 24px;
}

.focus-card {
  padding: 24px 28px;
}

.focus-header {
  display: flex;
  align-items: flex-start;
}

.focus-agent {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.agent-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, $accent-primary, $accent-blue);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  color: #fff;
}

.agent-pulse {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $accent-emerald;
  border: 2px solid $bg-elevated;
  animation: pulse-dot 2s ease-in-out infinite;
}

.agent-speech {
  flex: 1;
}

.speech-label {
  display: block;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: $accent-primary;
  margin-bottom: 4px;
}

.speech-text {
  font-size: 0.92rem;
  color: $text-secondary;
  line-height: 1.55;
  margin: 0;
}

.focus-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 22px;
  padding: 18px 22px;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid $border-subtle;
  cursor: pointer;
  transition: border-color $transition-normal, background $transition-normal;

  &:hover {
    border-color: $border-medium;
    background: rgba(255, 255, 255, 0.04);
  }
}

.focus-path-info {
  flex: 1;
}

.focus-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
}

.focus-next {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: $text-secondary;

  strong {
    color: $accent-primary;
  }
}

.next-icon {
  color: $accent-primary;
}

.focus-progress {
  flex-shrink: 0;
  margin-left: 24px;
}

.progress-ring-wrap {
  position: relative;
  width: 60px;
  height: 60px;
}

.progress-ring {
  width: 60px;
  height: 60px;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba($accent-primary, 0.1);
  stroke-width: 4;
}

.ring-fill {
  fill: none;
  stroke: $accent-primary;
  stroke-width: 4;
  stroke-linecap: round;
  transition: stroke-dasharray 1s ease;
}

.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: $accent-primary;
}

.focus-empty {
  text-align: center;
  padding: 32px 16px 24px;
}

.empty-icon {
  font-size: 2.8rem;
  margin-bottom: 14px;
}

.empty-title {
  font-size: 1.35rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 0.9rem;
  color: $text-secondary;
  margin: 0;
  max-width: 340px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.55;
}

.focus-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

// ---- Buttons ----
.btn-primary {
  @include button-primary;
}

.btn-ghost {
  @include button-ghost;
}

// ---- Competency Section ----
.competency-section {
  margin-top: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  color: $accent-primary;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.link-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: $accent-primary;
  font-family: $font-sans;
  font-size: 0.85rem;
  cursor: pointer;
  transition: color $transition-normal;

  &:hover {
    color: #e4c476;
  }
}

.competency-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.competency-card {
  padding: 16px 18px;
}

.comp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.comp-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: $text-primary;
}

.comp-level {
  font-size: 0.7rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: $radius-full;

  &.advanced {
    background: rgba($accent-emerald, 0.1);
    color: $accent-emerald;
  }

  &.intermediate {
    background: rgba($accent-amber, 0.1);
    color: $accent-amber;
  }

  &.beginner {
    background: rgba($accent-blue, 0.1);
    color: $accent-blue;
  }
}

.comp-bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comp-bar {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.comp-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 1s ease;

  &.advanced {
    background: $accent-emerald;
  }

  &.intermediate {
    background: $accent-amber;
  }

  &.beginner {
    background: $accent-blue;
  }
}

.comp-percent {
  font-size: 0.72rem;
  font-weight: 600;
  color: $text-muted;
  min-width: 30px;
  text-align: right;
}

// ---- Tasks Section ----
.tasks-section {
  margin-top: 32px;
}

.task-count {
  font-size: 0.85rem;
  color: $text-muted;
}

.tasks-list {
  padding: 0;
  overflow: hidden;
}

.task-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid $border-subtle;
  transition: background $transition-fast;

  &:last-of-type {
    border-bottom: none;
  }

  &:hover {
    background: rgba(#fff, 0.02);
  }

  &.completed {
    opacity: 0.5;
  }

  &.current {
    background: rgba($accent-primary, 0.03);
  }
}

.task-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 600;
  color: $text-muted;
  background: rgba(255, 255, 255, 0.04);
  margin-right: 14px;
  flex-shrink: 0;

  &.done {
    background: transparent;
    color: $accent-emerald;
  }
}

.task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.task-name {
  font-size: 0.9rem;
  color: $text-primary;
}

.task-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  font-size: 0.78rem;
  color: $text-muted;
}

.task-time {
  display: flex;
  align-items: center;
  gap: 3px;
}

.task-priority {
  padding: 2px 8px;
  border-radius: $radius-full;

  &.high {
    background: rgba($accent-red, 0.1);
    color: $accent-red;
  }

  &.medium {
    background: rgba($accent-amber, 0.1);
    color: $accent-amber;
  }

  &.low {
    background: rgba($accent-blue, 0.1);
    color: $accent-blue;
  }
}

.task-current-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: $radius-full;
  background: rgba($accent-primary, 0.1);
  color: $accent-primary;
  font-size: 0.72rem;
  font-weight: 600;
  flex-shrink: 0;
}

.start-learning-btn {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  border: none;
  background: rgba($accent-primary, 0.08);
  color: $accent-primary;
  font-family: $font-sans;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all $transition-normal;

  &:hover {
    background: rgba($accent-primary, 0.15);
  }
}

// ---- Agent Center ----
.agent-center-section {
  margin-top: 32px;
  margin-bottom: 8px;
}

.agent-center-card {
  padding: 24px 28px;
  cursor: pointer;
  transition: all $transition-normal;
  border: 1px solid rgba($accent-primary, 0.1);

  &:hover {
    border-color: rgba($accent-primary, 0.3);
    transform: translateY(-2px);
    box-shadow: 0 8px 32px rgba($accent-primary, 0.1);
  }
}

.center-header {
  margin-bottom: 20px;
}

.center-desc {
  font-size: 1.05rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
}

.center-hint {
  font-size: 0.82rem;
  color: $text-muted;
  margin: 0;
}

.agent-icons-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.agent-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  background: rgba($bg-card, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: all $transition-normal;

  &:hover {
    background: rgba($accent-primary, 0.1);
    border-color: rgba($accent-primary, 0.3);
    transform: translateY(-2px);
  }
}

.agent-icon-badge {
  font-size: 1.6rem;
  line-height: 1;
}

.agent-icon-label {
  font-size: 0.72rem;
  color: $text-secondary;
  font-weight: 500;
}

.center-footer {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.smart-input-hint {
  font-size: 0.82rem;
  color: $text-muted;
  display: flex;
  align-items: center;
  gap: 6px;
}

// ---- Agent Scenarios ----
.agent-scenarios {
  margin-top: 32px;
  margin-bottom: 32px;
}

.scenario-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.scenario-card {
  display: flex;
  align-items: center;
  padding: 18px 20px;
  cursor: pointer;
}

.scenario-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
  margin-right: 16px;
}

.scenario-ico {
  color: $accent-primary;
}

.scenario-info {
  flex: 1;
}

.scenario-title {
  font-size: 0.92rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 3px;
}

.scenario-desc {
  font-size: 0.78rem;
  color: $text-muted;
  margin: 0;
}

.scenario-arrow {
  color: $text-muted;
  flex-shrink: 0;
  margin-left: 8px;
  transition: color $transition-normal, transform $transition-normal;

  .scenario-card:hover & {
    color: $accent-primary;
    transform: translateX(3px);
  }
}

// ---- Sweep animation (reused from existing) ----
.sweep-card {
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: -75%;
    width: 50%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.04), transparent);
    transform: skewX(-20deg);
    transition: left 0.6s ease;
    pointer-events: none;
  }

  &:hover::after {
    left: 125%;
  }
}

// ---- Responsive ----
@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }

  .stat-chip {
    display: none;
  }

  .scenario-grid {
    grid-template-columns: 1fr;
  }

  .competency-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }

  .focus-title {
    font-size: 1.2rem;
  }

  .focus-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .focus-progress {
    margin-left: 0;
    align-self: center;
  }
}

@media (max-width: 480px) {
  .status-bar {
    flex-wrap: wrap;
    gap: 12px;
  }

  .focus-card {
    padding: 20px;
  }

  .focus-actions {
    flex-direction: column;

    .btn-primary,
    .btn-ghost {
      width: 100%;
      justify-content: center;
    }
  }

  .competency-grid {
    grid-template-columns: 1fr;
  }
}
</style>