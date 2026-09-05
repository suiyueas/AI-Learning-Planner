<template>
  <div class="welcome-guide">
    <!-- 核心输入区 -->
    <div class="wg-hero">
      <h1 class="wg-title">今天想学什么？</h1>
      <p class="wg-subtitle">告诉我你的目标，AI 为你规划完整的学习路径</p>

      <div class="wg-input-area">
        <input
          v-model="goalInput"
          class="wg-input"
          placeholder="例如：三个月学会 Python 数据分析"
          @keydown.enter="startSession"
          autofocus
        />
        <button
          class="wg-start-btn"
          :disabled="!goalInput.trim() || creating"
          @click="startSession"
        >
          <Sparkles :size="20" />
          {{ creating ? '创建中...' : '开始学习' }}
        </button>
      </div>

      <!-- 快捷示例 -->
      <div class="wg-quick-picks">
        <span class="wg-qp-label">试试：</span>
        <button
          v-for="sample in samples"
          :key="sample"
          class="wg-qp-chip"
          @click="goalInput = sample"
        >
          {{ sample }}
        </button>
      </div>
    </div>

    <!-- 🌟 新手引导区：学习工作台功能介绍 -->
    <div class="wg-onboarding">
      <div class="wgo-header">
        <div class="wgo-badge">核心流程</div>
        <h2 class="wgo-title">学习工作台能做这些</h2>
        <p class="wgo-subtitle">AI 驱动的智能学习体验，从诊断到报告全流程覆盖</p>
      </div>
      <div class="wgo-flow">
        <div class="wgo-step" v-for="(step, index) in workflowSteps" :key="index">
          <div class="wgo-step-number">{{ index + 1 }}</div>
          <div class="wgo-step-icon">{{ step.icon }}</div>
          <div class="wgo-step-content">
            <h4 class="wgo-step-title">{{ step.title }}</h4>
            <p class="wgo-step-desc">{{ step.desc }}</p>
          </div>
          <svg v-if="index < workflowSteps.length - 1" class="wgo-step-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- 🌟 新手引导区：其他功能入口 -->
    <div class="wg-features">
      <div class="wgf-header">
        <div class="wgf-badge">扩展能力</div>
        <h2 class="wgf-title">更多工具</h2>
        <p class="wgf-subtitle">丰富的辅助工具，助力高效学习</p>
      </div>
      <div class="wgf-grid">
        <router-link to="/knowledge" class="wgf-card">
          <div class="wgf-icon-wrap">
            <span class="wgf-icon">📖</span>
          </div>
          <div class="wgf-card-content">
            <span class="wgf-name">知识库</span>
            <span class="wgf-desc">管理学习文档</span>
          </div>
        </router-link>
        <router-link to="/study-notes" class="wgf-card">
          <div class="wgf-icon-wrap">
            <span class="wgf-icon">📝</span>
          </div>
          <div class="wgf-card-content">
            <span class="wgf-name">学习笔记</span>
            <span class="wgf-desc">记录学习心得</span>
          </div>
        </router-link>
        <router-link to="/code-analyze" class="wgf-card">
          <div class="wgf-icon-wrap">
            <span class="wgf-icon">💻</span>
          </div>
          <div class="wgf-card-content">
            <span class="wgf-name">代码分析</span>
            <span class="wgf-desc">AI 分析代码</span>
          </div>
        </router-link>
        <router-link to="/calendar" class="wgf-card">
          <div class="wgf-icon-wrap">
            <span class="wgf-icon">📅</span>
          </div>
          <div class="wgf-card-content">
            <span class="wgf-name">学习日历</span>
            <span class="wgf-desc">查看日程安排</span>
          </div>
        </router-link>
        <router-link to="/achievements" class="wgf-card">
          <div class="wgf-icon-wrap">
            <span class="wgf-icon">🏆</span>
          </div>
          <div class="wgf-card-content">
            <span class="wgf-name">成就徽章</span>
            <span class="wgf-desc">查看学习成果</span>
          </div>
        </router-link>
        <router-link to="/agents" class="wgf-card">
          <div class="wgf-icon-wrap">
            <span class="wgf-icon">🤖</span>
          </div>
          <div class="wgf-card-content">
            <span class="wgf-name">智能体</span>
            <span class="wgf-desc">高级 AI 工具</span>
          </div>
        </router-link>
      </div>
    </div>

    <!-- 历史会话 -->
    <div v-if="recentSessions.length > 0" class="wg-history">
      <h3 class="wg-history-title">继续学习</h3>
      <div class="wg-history-list">
        <div
          v-for="session in recentSessions"
          :key="session.id"
          class="wg-history-item"
          @click="resumeSession(session.id)"
        >
          <span class="wghi-icon">{{ getSessionIcon(session.phase) }}</span>
          <div class="wghi-info">
            <span class="wghi-goal">{{ session.goal }}</span>
            <span class="wghi-progress">{{ session.progress }}% 完成</span>
          </div>
          <UBadge :variant="session.status === 'active' ? 'success' : 'default'" size="sm">
            {{ getPhaseLabel(session.phase) }}
          </UBadge>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, inject } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles } from 'lucide-vue-next'
import { UBadge } from '@/components/ui'
import { createSession } from '@/api/sessionApi'

const emit = defineEmits(['start'])
const router = useRouter()
const sessions = inject('sessions', ref([]))
const setActiveSessionId = inject('setActiveSessionId', () => {})

const goalInput = ref('')
const creating = ref(false)

const recentSessions = computed(() => {
  return sessions.value
    .filter(s => s.status === 'active' || s.status === 'paused')
    .slice(0, 5)
})

const samples = [
  '三个月学会 Python 数据分析',
  '掌握前端 Vue 开发',
  '学习机器学习基础',
  '提升英语口语能力'
]

const workflowSteps = [
  { icon: '🔍', title: '能力诊断', desc: '评估当前水平' },
  { icon: '🗺️', title: '学习规划', desc: '定制学习路径' },
  { icon: '📖', title: '互动学习', desc: 'AI 实时答疑' },
  { icon: '✏️', title: '习题巩固', desc: '自动出题练习' },
  { icon: '📊', title: '学情报告', desc: '追踪学习进度' }
]

const phaseIcons = { diagnosis: '🔍', planning: '🗺️', learning: '📖', exercise: '✏️', report: '📊' }
const phaseLabels = { diagnosis: '诊断中', planning: '规划中', learning: '学习中', exercise: '练习中', report: '报告中' }
const getSessionIcon = (phase) => phaseIcons[phase] || '📋'
const getPhaseLabel = (phase) => phaseLabels[phase] || '进行中'

const startSession = async () => {
  if (!goalInput.value.trim() || creating.value) return
  creating.value = true
  try {
    const res = await createSession({ goal: goalInput.value.trim() })
    const data = res?.data ?? res
    let session
    if (data?.data?.id) {
      session = data.data
      sessions.value.unshift(session)
    } else if (data?.id) {
      session = data
      sessions.value.unshift(session)
    }
    if (session) {
      // 设置活跃会话 ID，使 Workbench 显示 router-view 并隐藏 WelcomeGuide
      setActiveSessionId(session.id)
      // 导航到会话阶段页面，自动挂载 SessionPanel 并建立 SSE 流连接，启动诊断流程推送
      router.push(`/workbench/${session.phase || 'diagnosis'}`)
    }
  } catch (e) {
    console.error('创建会话失败:', e.message)
  } finally {
    creating.value = false
  }
}

const resumeSession = (id) => {
  const session = sessions.value.find(s => s.id === id)
  if (session) {
    setActiveSessionId(session.id)
    router.push(`/workbench/${session.phase || 'active'}`)
  }
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.welcome-guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  min-height: 100%;
  padding: $space-8 $space-6 100px;
  max-width: 700px;
  margin: 0 auto;
  overflow-y: auto;
}

.wg-hero {
  text-align: center;
  margin-bottom: $space-10;
}

.wg-title {
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, $text-primary 0%, $accent-indigo-light 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 $space-2;
}

.wg-subtitle {
  font-size: $text-md;
  color: $text-muted;
  margin: 0 0 $space-8;
}

.wg-input-area {
  display: flex;
  gap: $space-2;
  max-width: 560px;
  margin: 0 auto $space-4;
}

.wg-input {
  flex: 1;
  padding: 12px 16px;
  background: rgba($bg-surface, 0.6);
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  color: $text-primary;
  font-size: $text-base;
  font-family: $font-sans;
  outline: none;
  transition: border-color $transition-fast;

  &::placeholder { color: $text-placeholder; }
  &:focus { border-color: $accent-indigo; }
}

.wg-start-btn {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: 12px 24px;
  background: rgba($accent-indigo, 0.15);
  border: 1px solid rgba($accent-indigo, 0.3);
  border-radius: $radius-lg;
  color: $accent-indigo;
  font-size: $text-base;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;
  white-space: nowrap;

  &:hover:not(:disabled) {
    background: rgba($accent-indigo, 0.25);
    border-color: rgba($accent-indigo, 0.5);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.2);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.wg-quick-picks {
  display: flex;
  align-items: center;
  gap: $space-2;
  flex-wrap: wrap;
  justify-content: center;
}

.wg-qp-label {
  font-size: $text-sm;
  color: $text-muted;
}

.wg-qp-chip {
  padding: 4px 12px;
  background: rgba($bg-elevated, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-full;
  color: $text-secondary;
  font-size: $text-xs;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &:hover {
    background: rgba($accent-indigo, 0.08);
    border-color: rgba($accent-indigo, 0.2);
    color: $accent-indigo;
  }
}

// ===== 新手引导区：5步学习流程 =====
.wg-onboarding {
  width: 100%;
  max-width: 640px;
  margin: 0 auto $space-8;
  padding: $space-6;
  background: linear-gradient(145deg, rgba($bg-surface, 0.6), rgba($bg-surface, 0.3));
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-xl;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.wgo-header {
  margin-bottom: $space-6;
  text-align: center;
}

.wgo-badge {
  display: inline-block;
  padding: 4px 12px;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.2);
  border-radius: $radius-full;
  font-size: 11px;
  font-weight: 600;
  color: $accent-indigo;
  margin-bottom: $space-3;
  letter-spacing: 0.05em;
}

.wgo-title {
  font-size: $text-lg;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-2;
}

.wgo-subtitle {
  font-size: $text-sm;
  color: $text-muted;
  margin: 0;
  line-height: 1.5;
}

.wgo-flow {
  display: flex;
  align-items: stretch;
  gap: $space-2;
  justify-content: center;
  flex-wrap: wrap;
}

.wgo-step {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-4 $space-3;
  background: rgba($bg-elevated, 0.4);
  border: 1px solid rgba($border-default, 0.3);
  border-radius: $radius-lg;
  min-width: 90px;
  transition: all $transition-fast;
  flex: 1;
  max-width: 110px;

  &:hover {
    background: rgba($accent-indigo, 0.06);
    border-color: rgba($accent-indigo, 0.2);
    transform: translateY(-2px);
  }
}

.wgo-step-number {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 18px;
  height: 18px;
  background: $accent-indigo;
  border-radius: 50%;
  font-size: 10px;
  font-weight: 700;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wgo-step-icon {
  font-size: 1.6rem;
}

.wgo-step-content {
  text-align: center;
}

.wgo-step-title {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 2px;
}

.wgo-step-desc {
  font-size: 10px;
  color: $text-muted;
  margin: 0;
  line-height: 1.4;
}

.wgo-step-arrow {
  width: 16px;
  height: 16px;
  color: $text-muted;
  opacity: 0.5;
  flex-shrink: 0;
  align-self: center;
}

// ===== 新手引导区：更多工具 =====
.wg-features {
  width: 100%;
  max-width: 640px;
  margin: 0 auto $space-8;
  padding: $space-6;
  background: linear-gradient(145deg, rgba($bg-surface, 0.5), rgba($bg-surface, 0.25));
  border: 1px solid rgba($accent-indigo, 0.08);
  border-radius: $radius-xl;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
}

.wgf-header {
  margin-bottom: $space-5;
  text-align: center;
}

.wgf-badge {
  display: inline-block;
  padding: 4px 12px;
  background: rgba($accent-violet, 0.1);
  border: 1px solid rgba($accent-violet, 0.2);
  border-radius: $radius-full;
  font-size: 11px;
  font-weight: 600;
  color: $accent-violet;
  margin-bottom: $space-3;
  letter-spacing: 0.05em;
}

.wgf-title {
  font-size: $text-lg;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-2;
}

.wgf-subtitle {
  font-size: $text-sm;
  color: $text-muted;
  margin: 0;
}

.wgf-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-3;
}

.wgf-card {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: rgba($bg-elevated, 0.3);
  border: 1px solid rgba($border-default, 0.2);
  border-radius: $radius-lg;
  text-decoration: none;
  transition: all $transition-fast;

  &:hover {
    background: rgba($accent-indigo, 0.06);
    border-color: rgba($accent-indigo, 0.2);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.wgf-icon-wrap {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-indigo, 0.08);
  border-radius: $radius-md;
  flex-shrink: 0;
}

.wgf-icon {
  font-size: 1.2rem;
}

.wgf-card-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.wgf-name {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
}

.wgf-desc {
  font-size: 11px;
  color: $text-muted;
}

// ===== 历史会话 =====
.wg-history {
  width: 100%;
  max-width: 560px;
}

.wg-history-title {
  font-size: $text-base;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 $space-3;
}

.wg-history-list {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.wg-history-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: rgba($bg-surface, 0.55);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    background: rgba($accent-indigo, 0.06);
  }
}

.wghi-icon { font-size: 1.3rem; }

.wghi-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.wghi-goal {
  font-size: $text-sm;
  font-weight: 500;
  color: $text-primary;
}

.wghi-progress {
  font-size: $text-xs;
  color: $text-muted;
}
</style>