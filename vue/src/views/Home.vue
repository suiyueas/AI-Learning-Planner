<template>
  <div class="home-page">
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
      <div v-if="activePath" class="focus-card glass-card">
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
        <div class="scenario-card glass-card" @click="router.push('/capability/diagnosis')">
          <div class="scenario-icon" style="background: rgba(212,168,83,0.12)"><Stethoscope :size="22" class="scenario-ico" /></div>
          <div class="scenario-info"><h3 class="scenario-title">诊断我的水平</h3><p class="scenario-desc">AI 分析你的知识薄弱点</p></div>
          <ArrowRight :size="16" class="scenario-arrow" />
        </div>
        <div class="scenario-card glass-card" @click="router.push('/chat')">
          <div class="scenario-icon" style="background: rgba(91,154,191,0.12)"><MessageCircle :size="22" class="scenario-ico" /></div>
          <div class="scenario-info"><h3 class="scenario-title">问 AI 一个问题</h3><p class="scenario-desc">苏格拉底式引导，不直接给答案</p></div>
          <ArrowRight :size="16" class="scenario-arrow" />
        </div>
        <div class="scenario-card glass-card" @click="router.push('/goal-setting')">
          <div class="scenario-icon" style="background: rgba(154,130,200,0.12)"><Route :size="22" class="scenario-ico" /></div>
          <div class="scenario-info"><h3 class="scenario-title">规划学习路径</h3><p class="scenario-desc">AI 根据目标生成专属路径</p></div>
          <ArrowRight :size="16" class="scenario-arrow" />
        </div>
        <div class="scenario-card glass-card" @click="router.push('/exercise')">
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

// ============================================
// 知途 (Zhitu) · 仪表盘页面
// 动态粒子背景 · 半透明面板 · 绿色渐变主题
// ============================================

// ---- 动态背景 ----
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

// ---- Page ----
.home-page {
  min-height: 100vh;
  position: relative;
  padding: $space-6 $space-8;
  max-width: 1000px;
  margin: 0 auto;
  color: $text-primary;
  font-family: $font-sans;
}

.home-page > *:not(.bg-dynamic) {
  position: relative;
  z-index: 1;
}

// ---- Surface utilities (半透明毛玻璃面板) ----
.glass-panel,
.glass-card {
  background: rgba($bg-surface, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: $radius-lg;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: $shadow-sm;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    box-shadow: $shadow-md, 0 0 20px rgba($accent-indigo, 0.06);
  }
}

// ---- Status bar (半透明毛玻璃) ----
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-4 $space-6;
  background: rgba($bg-surface, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: $radius-lg;
  position: relative;
  z-index: 10;
}

.status-left {
  display: flex;
  flex-direction: column;
  gap: $space-1;
}

.greeting-text {
  font-size: $text-lg;
  font-weight: 500;
  color: $text-primary;

  strong {
    font-weight: 700;
    background: linear-gradient(135deg, $accent-indigo 0%, #38BDF8 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.agent-status {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.status-dot-alive {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $accent-indigo;
  flex-shrink: 0;
  box-shadow: 0 0 10px rgba($accent-indigo, 0.5);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: $text-sm;
  color: $text-secondary;
}

.status-right {
  display: flex;
  align-items: center;
  gap: $space-3;
}

// ---- Stat chips (半透明) ----
.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: $space-1;
  padding: $space-1 $space-3;
  border-radius: $radius-full;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.15);
  font-size: $text-xs;
  color: $accent-indigo-light;
  transition: all $transition-normal;

  &:hover {
    background: rgba($accent-indigo, 0.15);
    border-color: rgba($accent-indigo, 0.25);
  }
}

// ---- Avatar (绿色主题) ----
.avatar-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, $accent-indigo, $accent-cyan);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-normal;
  box-shadow: 0 0 15px rgba($accent-indigo, 0.3);

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 0 20px rgba($accent-indigo, 0.5);
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-inverse;
}

// ---- Quick Access Section ----
.quick-access-section {
  margin-top: $space-5;
}

.quick-access-grid {
  display: flex;
  gap: $space-3;
  flex-wrap: wrap;
}

.quick-access-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
  padding: $space-4 $space-5;
  background: rgba($bg-surface, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 90px;

  &:hover {
    border-color: rgba($accent-indigo, 0.3);
    background: rgba($accent-indigo, 0.1);
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba($accent-indigo, 0.15);
  }

  .qa-icon {
    font-size: 1.5rem;
  }

  .qa-label {
    font-size: $text-sm;
    color: $text-secondary;
    font-weight: 500;
  }
}

// ---- Report Summary Section (半透明毛玻璃) ----
.report-summary-section {
  margin-top: $space-5;
}

.report-summary-card {
  padding: $space-5 $space-6;
}

.rs-header {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-4;

  .rs-icon {
    font-size: 1.3rem;
  }

  .rs-title {
    font-size: $text-lg;
    font-weight: 600;
    color: $text-primary;
    margin: 0;
  }
}

.rs-stats {
  display: flex;
  gap: $space-6;
  flex-wrap: wrap;
}

.rs-stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .rs-stat-value {
    font-family: $font-data;
    font-size: 1.5rem;
    font-weight: 700;
    background: linear-gradient(135deg, $accent-indigo 0%, #38BDF8 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    filter: drop-shadow(0 0 10px rgba($accent-indigo, 0.3));
  }

  .rs-stat-label {
    font-size: $text-xs;
    color: $text-muted;
  }
}

.rs-link-btn {
  margin-top: $space-4;
  background: none;
  border: none;
  color: $accent-indigo;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: all 0.3s ease;

  &:hover {
    color: $accent-indigo-light;
    text-shadow: 0 0 10px rgba($accent-indigo, 0.5);
  }
}

// ---- Agent Focus Card (半透明毛玻璃) ----
.agent-focus {
  margin-top: $space-6;
}

.focus-card {
  padding: $space-6 $space-8;
  background: rgba($bg-surface, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: $radius-lg;
  transition: all 0.3s ease;
  box-shadow: $shadow-sm;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    box-shadow: $shadow-md, 0 0 20px rgba($accent-indigo, 0.06);
  }
}

.focus-header {
  display: flex;
  align-items: flex-start;
}

.focus-agent {
  display: flex;
  gap: $space-3;
  align-items: flex-start;
}

// AI avatar (绿色渐变主题)
.agent-avatar {
  width: 36px;
  height: 36px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, $accent-indigo, $accent-cyan);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  color: $text-inverse;
  box-shadow: 0 0 15px rgba($accent-indigo, 0.3);
}

.agent-pulse {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $accent-indigo;
  border: 2px solid $bg-surface;
  box-shadow: 0 0 8px rgba($accent-indigo, 0.6);
  animation: pulse-dot 2s ease-in-out infinite;
}

.agent-speech {
  flex: 1;
}

.speech-label {
  display: block;
  font-size: $text-xs;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: $accent-indigo;
  margin-bottom: 3px;
  text-shadow: 0 0 10px rgba($accent-indigo, 0.3);
}

.speech-text {
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.55;
  margin: 0;
}

.focus-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $space-5;
  padding: $space-4 $space-5;
  border-radius: $radius-md;
  background: rgba($bg-elevated, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba($accent-indigo, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    background: rgba($accent-indigo, 0.08);
  }
}

.focus-path-info {
  flex: 1;
}

.focus-title {
  font-size: $text-2xl;
  font-weight: 700;
  background: linear-gradient(135deg, $text-primary 0%, $accent-indigo-light 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 $space-2;
}

.focus-next {
  display: flex;
  align-items: center;
  gap: $space-1;
  font-size: $text-sm;
  color: $text-secondary;

  strong {
    color: $accent-indigo-light;
    text-shadow: 0 0 8px rgba($accent-indigo, 0.3);
  }
}

.next-icon {
  color: $accent-indigo;
  filter: drop-shadow(0 0 5px rgba($accent-indigo, 0.5));
}

.focus-progress {
  flex-shrink: 0;
  margin-left: $space-6;
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
  stroke: rgba($text-muted, 0.15);
  stroke-width: 4;
}

.ring-fill {
  fill: none;
  stroke: $accent-indigo;
  stroke-width: 4;
  stroke-linecap: round;
  filter: drop-shadow(0 0 6px rgba($accent-indigo, 0.5));
  transition: stroke-dasharray 1s ease;
}

.ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: $font-data;
  font-size: $text-xs;
  font-weight: 700;
  color: $accent-indigo-light;
  text-shadow: 0 0 8px rgba($accent-indigo, 0.5);
}

.focus-empty {
  text-align: center;
  padding: $space-8 $space-4 $space-6;
}

.empty-icon {
  font-size: 2.8rem;
  margin-bottom: $space-3;
}

.empty-title {
  font-size: $text-2xl;
  font-weight: 700;
  background: linear-gradient(135deg, $text-primary 0%, $accent-indigo-light 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 $space-2;
}

.empty-desc {
  font-size: $text-sm;
  color: $text-secondary;
  margin: 0;
  max-width: 340px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.55;
}

.focus-actions {
  display: flex;
  gap: $space-3;
  margin-top: $space-5;
}

// ---- Buttons (半透明青绿色主题) ----
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-2 $space-5;
  background: rgba($accent-indigo, 0.1);
  color: $accent-indigo;
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: $radius-md;
  font-family: $font-sans;
  font-weight: 500;
  font-size: $text-sm;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-2 $space-5;
  background: rgba($accent-indigo, 0.06);
  color: $text-secondary;
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: $radius-md;
  font-family: $font-sans;
  font-weight: 500;
  font-size: $text-sm;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    color: $accent-indigo-light;
    background: rgba($accent-indigo, 0.1);
    transform: translateY(-1px);
  }
}

// ---- Competency Section (半透明毛玻璃) ----
.competency-section {
  margin-top: $space-8;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-4;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.section-icon {
  color: $accent-indigo;
  filter: drop-shadow(0 0 5px rgba($accent-indigo, 0.5));
}

.section-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.link-btn {
  display: flex;
  align-items: center;
  gap: $space-1;
  background: none;
  border: none;
  color: $accent-indigo;
  font-family: $font-sans;
  font-size: $text-sm;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    color: $accent-indigo-light;
    text-shadow: 0 0 10px rgba($accent-indigo, 0.5);
  }
}

.competency-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: $space-3;
}

.competency-card {
  padding: $space-4 $space-5;
  background: rgba($bg-surface, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-lg;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    background: rgba($accent-indigo, 0.06);
    box-shadow: 0 8px 20px rgba($accent-indigo, 0.1);
  }
}

.comp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-3;
}

.comp-name {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
}

.comp-level {
  font-size: $text-xs;
  font-weight: 600;
  padding: 2px $space-2;
  border-radius: $radius-full;

  &.advanced {
    background: rgba($accent-indigo, 0.15);
    color: $accent-indigo-light;
    border: 1px solid rgba($accent-indigo, 0.25);
  }

  &.intermediate {
    background: rgba($color-warning, 0.1);
    color: $color-warning;
    border: 1px solid rgba($color-warning, 0.2);
  }

  &.beginner {
    background: rgba($accent-cyan, 0.1);
    color: $accent-cyan;
    border: 1px solid rgba($accent-cyan, 0.2);
  }
}

.comp-bar-wrap {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.comp-bar {
  flex: 1;
  height: 4px;
  background: rgba($bg-elevated, 0.6);
  border-radius: 2px;
  overflow: hidden;
}

.comp-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 1s ease;

  &.advanced {
    background: linear-gradient(90deg, $accent-indigo, $accent-cyan);
    box-shadow: 0 0 8px rgba($accent-indigo, 0.5);
  }

  &.intermediate {
    background: $color-warning;
  }

  &.beginner {
    background: $accent-cyan;
  }
}

.comp-percent {
  font-family: $font-data;
  font-size: $text-xs;
  font-weight: 600;
  color: $text-muted;
  min-width: 30px;
  text-align: right;
}

// ---- Tasks Section (半透明毛玻璃) ----
.tasks-section {
  margin-top: $space-8;
}

.task-count {
  font-family: $font-data;
  font-size: $text-sm;
  color: $text-muted;
}

.tasks-list {
  padding: 0;
  overflow: hidden;
  background: rgba($bg-surface, 0.6);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-lg;
}

.task-item {
  display: flex;
  align-items: center;
  padding: $space-3 $space-5;
  border-bottom: 1px solid rgba($accent-indigo, 0.06);
  transition: all 0.3s ease;

  &:last-of-type {
    border-bottom: none;
  }

  &:hover {
    background: rgba($accent-indigo, 0.05);
  }

  &.completed {
    opacity: 0.5;
  }

  &.current {
    background: rgba($accent-indigo, 0.08);
    border-left: 3px solid $accent-indigo;
  }
}

.task-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: $font-data;
  font-size: $text-xs;
  font-weight: 600;
  color: $text-muted;
  background: rgba($bg-elevated, 0.5);
  border: 1px solid rgba($accent-indigo, 0.1);
  margin-right: $space-3;
  flex-shrink: 0;

  &.done {
    background: rgba($accent-indigo, 0.15);
    border-color: rgba($accent-indigo, 0.3);
    color: $accent-indigo-light;
    box-shadow: 0 0 10px rgba($accent-indigo, 0.2);
  }
}

.task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.task-name {
  font-size: $text-sm;
  color: $text-primary;
}

.task-meta {
  display: flex;
  gap: $space-2;
  margin-top: 2px;
  font-size: $text-xs;
  color: $text-muted;
}

.task-time {
  display: flex;
  align-items: center;
  gap: 3px;
}

.task-priority {
  padding: 1px $space-2;
  border-radius: $radius-full;
  font-size: 0.7rem;

  &.high {
    background: rgba($color-danger, 0.1);
    color: $color-danger;
  }

  &.medium {
    background: rgba($color-warning, 0.1);
    color: $color-warning;
  }

  &.low {
    background: rgba($accent-indigo, 0.1);
    color: $accent-indigo-light;
  }
}

.task-current-badge {
  display: flex;
  align-items: center;
  gap: $space-1;
  padding: 2px $space-2;
  border-radius: $radius-full;
  background: rgba($accent-indigo, 0.15);
  border: 1px solid rgba($accent-indigo, 0.3);
  color: $accent-indigo-light;
  font-size: $text-xs;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 0 10px rgba($accent-indigo, 0.2);
}

.start-learning-btn {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-4;
  border: none;
  background: linear-gradient(135deg, rgba($accent-indigo, 0.15), rgba($accent-cyan, 0.1));
  color: $accent-indigo-light;
  font-family: $font-sans;
  font-weight: 600;
  font-size: $text-sm;
  cursor: pointer;
  transition: all 0.3s ease;
  border-top: 1px solid rgba($accent-indigo, 0.1);

  &:hover {
    background: linear-gradient(135deg, rgba($accent-indigo, 0.25), rgba($accent-cyan, 0.15));
    text-shadow: 0 0 10px rgba($accent-indigo, 0.5);
  }
}

// ---- Agent Center (半透明毛玻璃) ----
.agent-center-section {
  margin-top: $space-8;
  margin-bottom: $space-2;
}

.agent-center-card {
  padding: $space-6 $space-8;
  cursor: pointer;
  background: rgba($bg-surface, 0.6);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-lg;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    transform: translateY(-3px);
    box-shadow: 0 10px 25px rgba($accent-indigo, 0.12);
  }
}

.center-header {
  margin-bottom: $space-5;
}

.center-desc {
  font-size: $text-lg;
  font-weight: 600;
  background: linear-gradient(135deg, $text-primary 0%, $accent-indigo-light 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 $space-1;
}

.center-hint {
  font-size: $text-sm;
  color: $text-muted;
  margin: 0;
}

.agent-icons-row {
  display: flex;
  gap: $space-3;
  flex-wrap: wrap;
  margin-bottom: $space-5;
}

.agent-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-1;
  padding: $space-3 $space-4;
  background: rgba($bg-elevated, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: $radius-md;
  border: 1px solid rgba($accent-indigo, 0.08);
  transition: all 0.3s ease;

  &:hover {
    background: rgba($accent-indigo, 0.12);
    border-color: rgba($accent-indigo, 0.25);
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba($accent-indigo, 0.15);
  }
}

.agent-icon-badge {
  font-size: 1.6rem;
  line-height: 1;
}

.agent-icon-label {
  font-size: $text-xs;
  color: $text-secondary;
  font-weight: 500;
}

.center-footer {
  padding-top: $space-4;
  border-top: 1px solid rgba($accent-indigo, 0.08);
}

.smart-input-hint {
  font-size: $text-sm;
  color: $text-muted;
  display: flex;
  align-items: center;
  gap: $space-1;
}

// ---- Agent Scenarios (半透明毛玻璃) ----
.agent-scenarios {
  margin-top: $space-8;
  margin-bottom: $space-8;
}

.scenario-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-3;
}

.scenario-card {
  display: flex;
  align-items: center;
  padding: $space-4 $space-5;
  background: rgba($bg-surface, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    background: rgba($accent-indigo, 0.06);
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba($accent-indigo, 0.1);
  }
}

.scenario-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-md;
  flex-shrink: 0;
  margin-right: $space-4;
  transition: all 0.3s ease;
}

.scenario-ico {
  color: $accent-indigo;
  filter: drop-shadow(0 0 5px rgba($accent-indigo, 0.3));
}

.scenario-card:hover .scenario-ico {
  filter: drop-shadow(0 0 8px rgba($accent-indigo, 0.5));
}

.scenario-info {
  flex: 1;
}

.scenario-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 2px;
  transition: all 0.3s ease;
}

.scenario-card:hover .scenario-title {
  background: linear-gradient(135deg, $text-primary 0%, $accent-indigo-light 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.scenario-desc {
  font-size: $text-xs;
  color: $text-muted;
  margin: 0;
}

.scenario-arrow {
  color: $text-muted;
  flex-shrink: 0;
  margin-left: $space-2;
  transition: all 0.3s ease;

  .scenario-card:hover & {
    color: $accent-indigo;
    transform: translateX(3px);
    filter: drop-shadow(0 0 5px rgba($accent-indigo, 0.5));
  }
}

// ---- Responsive ----
@media (max-width: $breakpoint-md) {
  .home-page {
    padding: $space-4;
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
    font-size: $text-xl;
  }

  .focus-content {
    flex-direction: column;
    align-items: flex-start;
    gap: $space-4;
  }

  .focus-progress {
    margin-left: 0;
    align-self: center;
  }
}

@media (max-width: 480px) {
  .status-bar {
    flex-wrap: wrap;
    gap: $space-3;
  }

  .focus-card {
    padding: $space-5;
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