<template>
  <div class="adaptive-page">
    <!-- 页面标题 -->
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">🧠</span>
            <span>自适应学习</span>
            <span class="title-sub">AI驱动的个性化学习路径</span>
          </h1>
        </div>
        <div class="header-right">
          <button class="btn-ghost" @click="openSettings">
            <Settings :size="16" />
            设置
          </button>
        </div>
      </div>
    </header>

    <!-- 加载失败重试 -->
    <div v-if="loadError" class="load-error">
      <span>⚠️ {{ loadError }}</span>
      <button class="retry-btn" @click="loadAll">🔄 重试</button>
    </div>

    <!-- 自适应引擎状态 -->
    <section class="info-section">
      <div class="section-label">
        <Activity :size="16" />
        自适应引擎状态
      </div>
      <div class="engine-grid">
        <div class="engine-item">
          <div class="engine-status-indicator">
            <span class="status-dot" :class="engineData.status === 'paused' ? 'offline' : 'online'"></span>
            <span class="engine-label">引擎状态</span>
          </div>
          <span class="engine-value" :class="engineData.status === 'paused' ? 'paused' : 'active'">
            {{ engineData.status === 'paused' ? '已暂停' : '运行中' }}
          </span>
        </div>
        <div class="engine-item">
          <span class="engine-label">当前策略</span>
          <span class="engine-value">{{ engineData.strategy }}</span>
        </div>
        <div class="engine-item">
          <span class="engine-label">历史调整</span>
          <span class="engine-value">{{ engineData.adjustments }} 次</span>
        </div>
        <div class="engine-item">
          <span class="engine-label">学习效率提升</span>
          <span class="engine-value highlight" :class="{ negative: engineData.efficiency < 0 }">
            {{ engineData.efficiency }}%
          </span>
        </div>
      </div>
    </section>

    <!-- 自适应调整历史 -->
    <section class="info-section">
      <div class="section-label">
        <History :size="16" />
        自适应调整历史
      </div>
      <div v-if="adjustHistory.length === 0" class="chart-empty">
        <History :size="40" class="empty-icon" />
        <p>暂无调整记录，系统检测到学习偏差后会在此展示真实的自适应调整轨迹</p>
      </div>
      <div v-else class="adjust-timeline">
        <div v-for="(item, index) in visibleAdjustments" :key="index" class="adjust-entry">
          <div class="adjust-entry-dot" :class="item.type"></div>
          <div class="adjust-entry-content">
            <div class="adjust-entry-header">
              <span class="adjust-entry-date">{{ formatDate(item.createdAt) }}</span>
              <span class="adjust-entry-tag" :class="item.type">{{ item.typeLabel }}</span>
            </div>
            <p class="adjust-entry-desc">{{ item.triggerReason }}</p>
            <p v-if="item.effect" class="adjust-entry-effect">📈 {{ item.effect }}</p>
          </div>
        </div>
      </div>
      <div v-if="adjustHistory.length > 3" class="timeline-actions">
        <button class="link-btn" @click="expanded = !expanded">
          {{ expanded ? '收起' : '展开全部' }}
          <ChevronDown :size="14" :class="{ rotated: expanded }" />
        </button>
      </div>
      <button v-if="adjustHistory.length > 0" class="link-btn" @click="viewAllAdjustments">
        查看全部调整
        <ArrowRight :size="14" />
      </button>
    </section>

    <!-- 自适应效果 -->
    <section class="info-section">
      <div class="section-label">
        <BarChart3 :size="16" />
        自适应效果
      </div>
      <div class="effect-grid">
        <div class="effect-card">
          <span class="effect-value">{{ effectData.efficiency }}%</span>
          <span class="effect-label">学习效率提升（近30天 vs 前30天）</span>
          <div class="effect-bar">
            <div class="effect-bar-fill cyan" :style="{ width: Math.min(Math.max(effectData.efficiency, 0), 100) + '%' }"></div>
          </div>
        </div>
        <div class="effect-card">
          <span class="effect-value">{{ effectData.mastery }}%</span>
          <span class="effect-label">知识掌握率（最近测评均值）</span>
          <div class="effect-bar">
            <div class="effect-bar-fill green" :style="{ width: Math.min(effectData.mastery, 100) + '%' }"></div>
          </div>
        </div>
        <div class="effect-card">
          <span class="effect-value">{{ effectData.streak }}</span>
          <span class="effect-label">连续学习天数</span>
          <div class="effect-bar">
            <div class="effect-bar-fill" :style="{ width: Math.min((effectData.streak / 50 * 100), 100) + '%' }"></div>
          </div>
        </div>
        <div class="effect-card">
          <span class="effect-value">{{ effectData.totalAdjustments }}</span>
          <span class="effect-label">累计自适应调整</span>
          <div class="effect-bar">
            <div class="effect-bar-fill purple" :style="{ width: Math.min(effectData.totalAdjustments / 20 * 100, 100) + '%' }"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- 推荐内容 -->
    <section class="info-section">
      <div class="section-label">
        <Sparkles :size="16" />
        推荐内容
      </div>
      <p class="recommend-intro">基于你的学习进度，个性化推荐以下内容：</p>
      <div v-if="recommendData.length === 0" class="chart-empty">
        <Sparkles :size="40" class="empty-icon" />
        <p>暂无推荐，完成测评或学习任务后将为你生成个性化推荐</p>
      </div>
      <div v-else class="recommend-list">
        <div v-for="(item, index) in recommendData" :key="item.id || index" class="recommend-item" @click="viewRecommend(item)">
          <div class="recommend-icon">
            <component :is="item.icon" :size="20" />
          </div>
          <div class="recommend-info">
            <span class="recommend-title">{{ item.title }}</span>
            <span class="recommend-desc">{{ item.matchReason || item.description }}</span>
          </div>
          <div class="recommend-right">
            <span class="recommend-match" :class="matchLevelClass(item.matchScore)">
              {{ matchLevelText(item.matchScore) }}
            </span>
            <span v-if="item.status === 'clicked'" class="recommend-status viewed">已查看</span>
            <span v-else-if="item.status === 'consumed'" class="recommend-status consumed">已学习</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 效率提升归因 -->
    <section v-if="attributionData.length > 0" class="info-section">
      <div class="section-label">
        <TrendingUp :size="16" />
        效率提升归因
      </div>
      <p class="recommend-intro">学习效率提升 {{ engineData.efficiency }}% 的来源分析：</p>
      <div class="attribution-list">
        <div v-for="(item, index) in attributionData" :key="index" class="attribution-item">
          <span class="attribution-label">{{ item.typeLabel }}</span>
          <div class="attribution-track">
            <div class="attribution-fill" :class="item.type" :style="{ width: attributionWidth(item) + '%' }"></div>
          </div>
          <span class="attribution-value">+{{ item.contribution }}%（{{ item.count }}次）</span>
        </div>
      </div>
    </section>

    <!-- 自适应规则说明 -->
    <section class="info-section rule-section">
      <div class="section-label">
        <Brain :size="16" />
        自适应引擎如何工作？
      </div>
      <div class="rule-grid">
        <div class="rule-item">
          <span class="rule-icon">🔄</span>
          <div class="rule-info">
            <span class="rule-title">复习插入</span>
            <span class="rule-desc">当测评正确率下降超过设定阈值时触发，插入复习节点</span>
          </div>
        </div>
        <div class="rule-item">
          <span class="rule-icon">🚀</span>
          <div class="rule-info">
            <span class="rule-title">进阶推荐</span>
            <span class="rule-desc">当学习进度超前计划时触发，推荐进阶内容</span>
          </div>
        </div>
        <div class="rule-item">
          <span class="rule-icon">📅</span>
          <div class="rule-info">
            <span class="rule-title">计划调整</span>
            <span class="rule-desc">当学习完成率低于目标时自动优化每日计划</span>
          </div>
        </div>
        <div class="rule-item">
          <span class="rule-icon">📚</span>
          <div class="rule-info">
            <span class="rule-title">资源推荐</span>
            <span class="rule-desc">基于薄弱知识点与兴趣方向智能匹配学习资源</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 设置对话框 -->
    <el-dialog
      v-model="prefDialogVisible"
      title="学习偏好设置"
      width="600px"
      :close-on-click-modal="false"
      class="preferences-dialog"
    >
      <div class="preferences-content">
        <!-- ... 对话框内容保持不变 ... -->
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resetDefaults">恢复默认</el-button>
          <div class="footer-right">
            <el-button @click="prefDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="savePreferences">保存偏好</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Settings, Activity, History,
  BarChart3, Sparkles, BookOpen, ArrowRight,
  TrendingUp, Brain, ChevronDown,
  BookMarked, FileText, Map
} from 'lucide-vue-next'
import { getPreferences, updatePreferences } from '@/api/user'
import { getProgressOverview } from '@/api/statsApi'
import {
  getAdaptiveStatus,
  getAdaptiveAdjustments,
  getAdaptiveRecommendations,
  clickRecommendation
} from '@/api/adaptiveApi'

const router = useRouter()

const prefDialogVisible = ref(false)
const preferences = ref({
  learningStyle: 'visual',
  difficulty: 'moderate',
  dailyHours: 2,
  timeSlots: ['morning', 'afternoon'],
  interventionEnabled: true,
  interventionProgressThreshold: 65,
  interventionScoreDeclineThreshold: 10,
  interventionInactiveDays: 3
})

const defaultPreferences = {
  learningStyle: 'visual',
  difficulty: 'moderate',
  dailyHours: 2,
  timeSlots: ['morning', 'afternoon'],
  interventionEnabled: true,
  interventionProgressThreshold: 65,
  interventionScoreDeclineThreshold: 10,
  interventionInactiveDays: 3
}

const customDuration = ref({
  active: false,
  hours: 2
})

const learningStyles = [
  { value: 'visual', label: '视觉型', icon: '📺', desc: '适合通过视频、图表、思维导图学习' },
  { value: 'auditory', label: '听觉型', icon: '🎧', desc: '适合通过播客、讲座、讨论学习' },
  { value: 'reading', label: '阅读/写作型', icon: '📖', desc: '适合通过阅读书籍、写笔记、做总结学习' },
  { value: 'kinesthetic', label: '动觉型', icon: '🖐️', desc: '适合通过动手实践、项目操作、实验学习' }
]

const difficultyLevels = [
  { value: 'gradual', label: '循序渐进', icon: '🌱', desc: '从最基础开始，每步都有详细讲解，适合零基础' },
  { value: 'moderate', label: '适中', icon: '📊', desc: '平衡基础与进阶，适合有一定基础的学习者' },
  { value: 'challenge', label: '挑战模式', icon: '🚀', desc: '直接进入高难度内容，适合快速进阶或复习' }
]

const durations = [
  { value: 0.5, label: '30分钟', icon: '⏰', desc: '适合日常碎片化学习' },
  { value: 1, label: '1小时', icon: '🕐', desc: '适合每天固定学习时段' },
  { value: 2, label: '2小时', icon: '⏳', desc: '适合深度专注学习' },
  { value: 3, label: '3+小时', icon: '🔥', desc: '适合全职学习或冲刺阶段' }
]

const timeSlots = [
  { value: 'morning', label: '早晨', icon: '🌅', time: '5:00 - 9:00', desc: '记忆力最佳时段' },
  { value: 'forenoon', label: '上午', icon: '☀️', time: '9:00 - 12:00', desc: '逻辑思维活跃期' },
  { value: 'afternoon', label: '下午', icon: '🌤️', time: '14:00 - 17:00', desc: '适合实践操作' },
  { value: 'evening', label: '晚上', icon: '🌙', time: '19:00 - 22:00', desc: '适合复习总结' }
]

const selectDuration = (duration) => {
  customDuration.value.active = false
  preferences.value.dailyHours = duration.value
}

const activateCustomDuration = () => {
  customDuration.value.active = true
  if (customDuration.value.hours && customDuration.value.hours > 0) {
    preferences.value.dailyHours = customDuration.value.hours
  }
}

const applyCustomDuration = () => {
  if (customDuration.value.hours && customDuration.value.hours > 0) {
    preferences.value.dailyHours = customDuration.value.hours
  }
}

const toggleTimeSlot = (value) => {
  if (!preferences.value.timeSlots) {
    preferences.value.timeSlots = []
  }
  const index = preferences.value.timeSlots.indexOf(value)
  if (index > -1) {
    preferences.value.timeSlots.splice(index, 1)
  } else {
    preferences.value.timeSlots.push(value)
  }
}

const loadPreferences = async () => {
  try {
    const res = await getPreferences()
    if (res && Object.keys(res).length > 0) {
      preferences.value = {
        ...defaultPreferences,
        ...res,
        timeSlots: Array.isArray(res.timeSlots) ? res.timeSlots : defaultPreferences.timeSlots
      }
    }
  } catch {
    // 使用默认值
  }
}

const savePreferences = async () => {
  try {
    await updatePreferences(preferences.value)
    ElMessage.success('偏好设置已保存')
    prefDialogVisible.value = false
  } catch (error) {
    ElMessage.error('保存失败：' + (error.message || '未知错误'))
  }
}

const resetDefaults = () => {
  preferences.value = { ...defaultPreferences }
  ElMessage.info('已恢复默认设置')
}

const openSettings = () => {
  loadPreferences()
  prefDialogVisible.value = true
}

// ===== 引擎状态（真实数据） =====
const engineData = ref({
  status: 'running',
  strategy: '个性化推荐 + 难度自适应',
  adjustments: 0,
  efficiency: 0
})

// ===== 调整历史（真实数据） =====
const adjustHistory = ref([])
const expanded = ref(false)
const visibleAdjustments = computed(() => {
  if (expanded.value) return adjustHistory.value
  return adjustHistory.value.slice(0, 3)
})

// ===== 自适应效果（真实数据） =====
const effectData = ref({
  efficiency: 0,
  mastery: 0,
  streak: 0,
  totalAdjustments: 0
})

// ===== 效率提升归因 =====
const attributionData = ref([])

// ===== 推荐内容（真实数据） =====
const recommendData = ref([])

const loadError = ref('')

// 推荐图标映射（按内容类型）
const recommendIcons = {
  resource: BookOpen,
  course: BookMarked,
  knowledge_block: FileText,
  learning_path: Map
}

// 匹配度文案与等级
const matchLevelText = (score) => {
  if (!score) return '匹配'
  if (score >= 0.9) return '高度匹配'
  if (score >= 0.75) return '推荐'
  return '兴趣推荐'
}

const matchLevelClass = (score) => {
  if (!score) return 'medium'
  if (score >= 0.9) return 'high'
  if (score >= 0.75) return 'medium'
  return 'low'
}

// 归因条宽度：相对最大贡献度归一化
const attributionWidth = (item) => {
  const max = Math.max(...attributionData.value.map(a => a.contribution), 1)
  return Math.round(item.contribution / max * 100)
}

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// ===== 数据加载 =====
const loadAll = async () => {
  loadError.value = ''
  try {
    const [statusRes, adjustRes, recommendRes] = await Promise.all([
      getAdaptiveStatus(),
      getAdaptiveAdjustments({ page: 0, size: 10 }),
      getAdaptiveRecommendations()
    ])
    applyStatus(statusRes?.data || statusRes || {})
    adjustHistory.value = adjustRes?.data?.content || adjustRes?.content || []
    applyRecommendations(recommendRes?.data?.content || recommendRes?.content || [])
  } catch (e) {
    const errorMsg = e.message || '加载失败，请检查网络后重试'
    loadError.value = errorMsg
    if (errorMsg.includes('未登录') || errorMsg.includes('未授权')) {
      loadError.value = '请先登录后再访问'
    }
  }
  loadOverview()
}

const applyStatus = (data) => {
  engineData.value = {
    status: data.status || 'running',
    strategy: data.currentStrategy || '个性化推荐 + 难度自适应',
    adjustments: data.totalAdjustments || 0,
    efficiency: data.efficiencyImprovement || 0
  }
  effectData.value = {
    efficiency: data.efficiencyImprovement || 0,
    mastery: data.knowledgeMastery || 0,
    streak: effectData.value.streak,
    totalAdjustments: data.totalAdjustments || 0
  }
  attributionData.value = data.attribution || []
}

const loadOverview = async () => {
  try {
    const res = await getProgressOverview()
    const d = res?.data || {}
    effectData.value.streak = d.streak || 0
  } catch {
    // 连续天数加载失败时保持 0
  }
}

const applyRecommendations = (list) => {
  recommendData.value = list.map(item => ({
    ...item,
    icon: recommendIcons[item.type] || BookOpen
  }))
}

const viewAllAdjustments = () => {
  router.push('/capability/adaptive/history')
}

// 推荐点击：记录点击行为 → 按类型跳转到对应内容页
const viewRecommend = async (item) => {
  if (item.id) {
    try {
      await clickRecommendation(item.id)
      item.status = 'clicked'
    } catch {
      // 点击记录失败不影响跳转
    }
  }
  const routeMap = {
    resource: '/knowledge',
    knowledge_block: '/knowledge',
    course: `/learning-path/${item.target?.contentId || ''}`,
    learning_path: `/learning-path/${item.target?.contentId || ''}`
  }
  const target = routeMap[item.type]
  if (target && !target.endsWith('/')) {
    router.push(target)
  } else {
    router.push('/knowledge')
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.adaptive-page {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  padding: 32px 48px 80px;
  animation: pageEnter 0.6s ease;
}

/* ===== 页面头部（统一规范） ===== */
.page-header { @include page-header-base; }
.page-title { @include page-title-base; }
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; -webkit-text-fill-color: initial; }

.btn-ghost {
  @include page-header-btn-ghost;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.2);
  color: $accent-indigo;
  
  &:hover {
    background: rgba($accent-indigo, 0.15);
    border-color: rgba($accent-indigo, 0.35);
    color: $accent-indigo-light;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
  
  &:hover svg { transform: rotate(180deg); }
}

/* ===== 内容区 ===== */
.info-section {
  position: relative;
  z-index: 1;
  background: rgba($bg-surface, 0.55);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: 14px;
  padding: 28px;
  margin-bottom: 20px;
  animation: cardEnter 0.5s ease both;
}

.info-section:hover {
  border-color: rgba($accent-indigo, 0.2);
  box-shadow: 0 0 24px rgba($accent-indigo, 0.06);
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
  border-bottom: 1px solid rgba($accent-indigo, 0.1);
}

.section-label svg {
  color: $accent-indigo;
}

/* ===== 页面动画 ===== */
@keyframes pageEnter {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cardEnter {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== 其他样式保持不变 ===== */
.engine-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.engine-item {
  padding: 20px;
  background: rgba($accent-indigo, 0.05);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 12px;
}

.engine-status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #10B981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.engine-label {
  font-size: 0.8rem;
  color: $text-secondary;
  display: block;
  margin-bottom: 6px;
}

.engine-value {
  display: block;
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
}

.engine-value.active {
  color: #10B981;
}

.engine-value.highlight {
  color: $accent-indigo;
  font-size: 1.4rem;
}

/* 调整历史 */
.adjust-timeline {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-left: 20px;
  margin-bottom: 16px;
}

.adjust-timeline::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: rgba($accent-indigo, 0.12);
}

.adjust-entry {
  display: flex;
  gap: 16px;
  position: relative;
}

.adjust-entry-dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid;
  background: rgba($bg-primary, 0.8);
  z-index: 1;
}

.adjust-entry-dot.review { border-color: #EF4444; }
.adjust-entry-dot.advance { border-color: $accent-indigo; }
.adjust-entry-dot.plan { border-color: #F59E0B; }
.adjust-entry-dot.resource { border-color: #A855F7; }

.adjust-entry-content {
  flex: 1;
}

.adjust-entry-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.adjust-entry-date {
  font-size: 0.8rem;
  color: $text-secondary;
  font-family: 'JetBrains Mono', monospace;
}

.adjust-entry-tag {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.adjust-entry-tag.review { background: rgba(239, 68, 68, 0.1); color: #EF4444; }
.adjust-entry-tag.advance { background: rgba($accent-indigo, 0.1); color: $accent-indigo; }
.adjust-entry-tag.plan { background: rgba(245, 158, 11, 0.1); color: #F59E0B; }
.adjust-entry-tag.resource { background: rgba(168, 85, 247, 0.1); color: #A855F7; }

.adjust-entry-desc {
  font-size: 0.9rem;
  color: $text-primary;
  margin: 0;
}

/* 效果 */
.effect-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.effect-card {
  padding: 20px;
  background: rgba($accent-indigo, 0.05);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 12px;
}

.effect-value {
  display: block;
  font-size: 2rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  color: $text-primary;
  margin-bottom: 4px;
}

.effect-label {
  display: block;
  font-size: 0.8rem;
  color: $text-secondary;
  margin-bottom: 12px;
}

.effect-bar {
  height: 4px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.effect-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-indigo, #38BDF8);
  border-radius: 2px;
  transition: width 1s ease;
}

.effect-bar-fill.green {
  background: linear-gradient(90deg, #10B981, #34D399);
}

.effect-bar-fill.purple {
  background: linear-gradient(90deg, #A855F7, #C084FC);
}

/* 推荐内容 */
.recommend-intro {
  font-size: 0.9rem;
  color: $text-secondary;
  margin-bottom: 16px;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: rgba($accent-indigo, 0.05);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.recommend-item:hover {
  background: rgba($accent-indigo, 0.08);
  border-color: rgba($accent-indigo, 0.22);
  transform: translateX(4px);
}

.recommend-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: 12px;
  flex-shrink: 0;
}

.recommend-icon svg {
  color: $accent-indigo;
}

.recommend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recommend-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: $text-primary;
}

.recommend-desc {
  font-size: 0.8rem;
  color: $text-secondary;
}

.recommend-match {
  font-size: 0.75rem;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 500;
  white-space: nowrap;
}

.recommend-match.high {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.recommend-match.medium {
  background: rgba($accent-indigo, 0.08);
  color: $accent-indigo;
  border: 1px solid rgba($accent-indigo, 0.15);
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 8px;
  color: $accent-indigo;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.link-btn:hover {
  background: rgba($accent-indigo, 0.12);
  border-color: rgba($accent-indigo, 0.28);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .engine-grid, .effect-grid { grid-template-columns: 1fr; }
}

/* 加载失败 / 空态 */
.load-error {
  position: relative;
  z-index: 1;
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
  margin-bottom: 20px;
}

.retry-btn {
  padding: 6px 14px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 8px;
  color: #EF4444;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.retry-btn:hover {
  background: rgba(239, 68, 68, 0.18);
}

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  color: rgba($accent-indigo, 0.4);
}

.chart-empty p {
  font-size: 0.9rem;
  color: $text-secondary;
  margin: 0;
  max-width: 420px;
  line-height: 1.6;
}

/* 引擎状态扩展 */
.status-dot.offline {
  background: $text-secondary;
  box-shadow: none;
  animation: none;
}

.engine-value.paused {
  color: $text-secondary;
}

.engine-value.negative {
  color: #EF4444;
}

/* 调整历史扩展 */
.adjust-entry-effect {
  font-size: 0.8rem;
  color: #10B981;
  margin: 4px 0 0;
}

.timeline-actions {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.link-btn svg.rotated {
  transform: rotate(180deg);
  transition: transform 0.25s ease;
}

/* 推荐状态徽标 */
.recommend-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.recommend-status {
  font-size: 0.7rem;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 500;
  white-space: nowrap;
}

.recommend-status.viewed {
  background: rgba($accent-indigo, 0.08);
  color: $accent-indigo;
  border: 1px solid rgba($accent-indigo, 0.2);
}

.recommend-status.consumed {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.recommend-match.low {
  background: rgba(148, 163, 184, 0.08);
  color: $text-secondary;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

/* 效率提升归因 */
.attribution-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.attribution-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.attribution-label {
  font-size: 0.85rem;
  color: $text-primary;
  width: 96px;
  flex-shrink: 0;
}

.attribution-track {
  flex: 1;
  height: 8px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.attribution-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.8s ease;
}

.attribution-fill.review_insert {
  background: linear-gradient(90deg, #EF4444, #F87171);
}

.attribution-fill.plan_adjust {
  background: linear-gradient(90deg, #F59E0B, #FBBF24);
}

.attribution-fill.resource_recommend {
  background: linear-gradient(90deg, #A855F7, #C084FC);
}

.attribution-fill.advance_recommend {
  background: linear-gradient(90deg, $accent-indigo, #38BDF8);
}

.attribution-fill.difficulty_adjust {
  background: linear-gradient(90deg, #10B981, #34D399);
}

.attribution-value {
  font-size: 0.8rem;
  color: $text-secondary;
  font-family: 'JetBrains Mono', monospace;
  width: 120px;
  text-align: right;
  flex-shrink: 0;
}

/* 自适应规则说明 */
.rule-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: rgba($accent-indigo, 0.05);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 12px;
}

.rule-icon {
  font-size: 1.3rem;
  flex-shrink: 0;
  line-height: 1.3;
}

.rule-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.rule-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: $text-primary;
}

.rule-desc {
  font-size: 0.78rem;
  color: $text-secondary;
  line-height: 1.5;
}

/* 自适应偏好设置 */
.adaptive-pref-section {
  padding-top: 20px;
  border-top: 1px solid rgba($accent-indigo, 0.08);
}

.adaptive-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: rgba($accent-indigo, 0.05);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 12px;
  margin-bottom: 14px;
}

.adaptive-switch-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.threshold-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.threshold-item {
  padding: 14px;
  background: rgba($accent-indigo, 0.05);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 12px;
}

.threshold-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 10px;
}

.threshold-input-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.threshold-unit {
  font-size: 0.8rem;
  color: $text-secondary;
}

.threshold-desc {
  display: block;
  font-size: 0.72rem;
  color: $text-secondary;
  margin-top: 8px;
  line-height: 1.4;
}

/* 响应式 */
@media (max-width: 1024px) {
  .adaptive-page { padding: 24px 24px 60px; }
  .page-title { font-size: 1.5rem; }
}

@media (max-width: 768px) {
  .adaptive-page { padding: 20px 16px 40px; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 12px; }
  .header-right { width: 100%; }
  .btn-ghost { width: 100%; justify-content: center; }
  .engine-grid, .effect-grid { grid-template-columns: 1fr; }
  .rule-grid, .threshold-grid { grid-template-columns: 1fr; }
}
</style>