<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- 顶部：标题（统一规范） -->
      <header class="page-header">
        <div class="header-row">
          <div class="header-left">
            <h1 class="page-title">
              <span class="title-glyph">👤</span>
              <span>个人信息</span>
              <span class="title-sub">管理个人资料与偏好设置</span>
            </h1>
          </div>
          <div class="header-right">
            <button class="header-action-btn" title="编辑资料" @click="scrollToForm">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
              <span>编辑资料</span>
            </button>
          </div>
        </div>
      </header>

      <!-- 头像区域（居中） - 增强版 -->
      <div class="avatar-section glass-card">
        <!-- 呼吸光球背景 -->
        <div class="avatar-breathing-glow"></div>
        <div class="avatar-pulse-ring"></div>
        <!-- 骨架屏 -->
        <div v-if="isLoadingProfile" class="avatar-main skeleton-avatar"></div>
        <div v-else class="avatar-main">
          <AvatarUpload :size="90" @avatar-updated="onAvatarUpdated" />
        </div>

        <div class="avatar-info">
          <template v-if="isLoadingProfile">
            <div class="skeleton-text skeleton-name"></div>
            <div class="skeleton-text skeleton-username"></div>
            <div class="skeleton-text skeleton-goal"></div>
            <div class="skeleton-text skeleton-date"></div>
          </template>
          <template v-else>
            <h2 class="profile-name">{{ authStore.displayName }}</h2>
            <p class="profile-username">@{{ authStore.user.username }}</p>
            <p v-if="authStore.user.learningGoal" class="profile-goal">
              🎯 {{ authStore.user.learningGoal }}
            </p>
            <p class="profile-join-date">
              注册时间：{{ formatDate(authStore.user.createdAt) }}
            </p>
          </template>
        </div>

        <!-- 四维统计卡片 -->
        <div class="stat-cards">
          <template v-if="isLoadingStats">
            <div v-for="i in 4" :key="i" class="stat-card-item skeleton-stat">
              <div class="skeleton-text skeleton-stat-value"></div>
              <div class="skeleton-text skeleton-stat-label"></div>
            </div>
          </template>
          <template v-else>
            <div class="stat-card-item">
              <span class="stat-card-value">{{ userStore.userStats.streak }}</span>
              <span class="stat-card-label">连续学习</span>
              <span class="stat-card-unit">天</span>
            </div>
            <div class="stat-card-item">
              <span class="stat-card-value">{{ userStore.totalHours }}</span>
              <span class="stat-card-label">总学时</span>
              <span class="stat-card-unit">小时</span>
            </div>
            <div class="stat-card-item">
              <span class="stat-card-value">{{ userStore.userStats.achievementCount }}</span>
              <span class="stat-card-label">成就</span>
              <span class="stat-card-unit">个</span>
            </div>
            <div class="stat-card-item">
              <span class="stat-card-value">{{ userStore.userStats.completionRate }}%</span>
              <span class="stat-card-label">完成率</span>
              <span class="stat-card-unit"></span>
            </div>
          </template>
        </div>
      </div>

      <!-- 两栏内容 -->
      <div class="content-grid">
        <!-- 左栏：个人信息 -->
        <div class="content-card glass-card">
          <h3 class="card-title">📋 个人信息</h3>
          <form class="edit-form" @submit.prevent="handleUpdateProfile">
            <div class="form-group">
              <label class="form-label">昵称</label>
              <input
                v-model="profileForm.nickname"
                type="text"
                placeholder="设置昵称"
                class="form-input"
                :disabled="isLoadingProfile"
              />
            </div>
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <input
                :value="authStore.user.email"
                type="text"
                class="form-input form-input-disabled"
                disabled
              />
            </div>
            <div class="form-group">
              <label class="form-label">学习目标</label>
              <input
                v-model="profileForm.learningGoal"
                type="text"
                placeholder="例如：Python 数据分析"
                class="form-input"
                :disabled="isLoadingProfile"
              />
            </div>
            <div class="form-group">
              <label class="form-label">个人简介</label>
              <textarea
                v-model="profileForm.bio"
                placeholder="简单介绍一下自己..."
                class="form-textarea"
                rows="3"
                :disabled="isLoadingProfile"
              ></textarea>
            </div>
            <button type="submit" class="save-btn" :disabled="savingProfile">
              <span v-if="savingProfile" class="loading-spinner"></span>
              <span v-else>保存修改</span>
            </button>
          </form>
        </div>

        <!-- 右栏：学习统计 -->
        <div class="content-card glass-card">
          <h3 class="card-title">📊 学习统计</h3>

          <template v-if="isLoadingLearning">
            <div v-for="i in 4" :key="i" class="skeleton-stats-row">
              <div class="skeleton-text skeleton-stats-label"></div>
              <div class="skeleton-text skeleton-stats-value"></div>
            </div>
            <div class="skeleton-progress"></div>
          </template>
          <template v-else>
            <div class="stats-list">
              <div class="stats-row-item">
                <div class="stats-row-left">
                  <span class="stats-row-icon">📝</span>
                  <span class="stats-row-label">已完成节数</span>
                </div>
                <span class="stats-row-value">{{ userStore.learningStats.completedNodes }}</span>
              </div>
              <div class="stats-row-item">
                <div class="stats-row-left">
                  <span class="stats-row-icon">📋</span>
                  <span class="stats-row-label">总任务数</span>
                </div>
                <span class="stats-row-value">{{ userStore.learningStats.totalTasks }}</span>
              </div>
              <div class="stats-row-item">
                <div class="stats-row-left">
                  <span class="stats-row-icon">⭐</span>
                  <span class="stats-row-label">平均得分</span>
                </div>
                <span class="stats-row-value">{{ userStore.learningStats.averageScore }}</span>
              </div>
              <div class="stats-row-item">
                <div class="stats-row-left">
                  <span class="stats-row-icon">📈</span>
                  <span class="stats-row-label">本周进度</span>
                </div>
                <span class="stats-row-value">{{ userStore.weeklyCompletionRate }}%</span>
              </div>
            </div>
            <!-- 进度条 -->
            <div class="progress-section">
              <div class="progress-header">
                <span class="progress-title">学习进度</span>
                <span class="progress-percent">{{ userStore.learningStats.progress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: userStore.learningStats.progress + '%' }"></div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- 安全设置 -->
      <div class="glass-card security-card">
        <h3 class="card-title">🔒 安全设置</h3>
        <form class="edit-form" @submit.prevent="handleChangePassword">
          <div class="security-form-row">
            <div class="form-group">
              <label class="form-label">旧密码</label>
              <input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入旧密码"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">新密码</label>
              <input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="至少8位，包含字母和数字"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">确认新密码</label>
              <input
                v-model="passwordForm.confirmNewPassword"
                type="password"
                placeholder="请再次输入新密码"
                class="form-input"
              />
            </div>
          </div>
          <button type="submit" class="save-btn" :disabled="savingPassword">
            <span v-if="savingPassword" class="loading-spinner"></span>
            <span v-else>修改密码</span>
          </button>
        </form>
      </div>

      <!-- 智能干预设置 -->
      <div class="glass-card security-card">
        <h3 class="card-title">🔔 智能干预设置</h3>
        <p class="intervention-desc">自定义学习预警阈值，通知中心将按你的偏好生成提醒</p>

        <div class="intervention-setting-row">
          <div class="setting-label-wrap">
            <span class="setting-label">干预提醒总开关</span>
            <span class="setting-hint">关闭后不再生成进度滞后、知识点下降、连续未登录等提醒</span>
          </div>
          <label class="toggle-switch">
            <input v-model="interventionForm.enabled" type="checkbox" />
            <span class="toggle-slider"></span>
          </label>
        </div>

        <div class="intervention-grid">
          <div class="form-group">
            <label class="form-label">进度提醒阈值</label>
            <div class="threshold-input">
              <input v-model.number="interventionForm.progressThreshold" type="number" min="20" max="95" class="form-input" />
              <span class="threshold-unit">%</span>
            </div>
            <span class="setting-hint">完成率低于该值提醒，低于该值减 15% 视为紧急</span>
          </div>
          <div class="form-group">
            <label class="form-label">测评降幅阈值</label>
            <div class="threshold-input">
              <input v-model.number="interventionForm.scoreDeclineThreshold" type="number" min="1" max="50" class="form-input" />
              <span class="threshold-unit">%</span>
            </div>
            <span class="setting-hint">测评分数降幅超过该值提醒（3 倍视为紧急）</span>
          </div>
          <div class="form-group">
            <label class="form-label">连续未登录提醒</label>
            <div class="threshold-input">
              <input v-model.number="interventionForm.inactiveDays" type="number" min="1" max="14" class="form-input" />
              <span class="threshold-unit">天</span>
            </div>
            <span class="setting-hint">连续 N 天未登录后生成提醒</span>
          </div>
        </div>

        <button class="save-btn" :disabled="savingIntervention" @click="handleSaveIntervention">
          <span v-if="savingIntervention" class="loading-spinner"></span>
          <span v-else>保存设置</span>
        </button>
      </div>

      <!-- 成就系统 -->
      <div class="glass-card achievements-card">
        <h3 class="card-title">
          🏆 成就系统
          <span v-if="!isLoadingAchievements" class="achievement-progress-badge">
            {{ unlockedCount }}/{{ totalCount }} 已解锁
          </span>
          <span v-else class="skeleton-badge"></span>
        </h3>

        <template v-if="isLoadingAchievements">
          <div class="achievements-grid">
            <div v-for="i in 8" :key="i" class="achievement-item skeleton-achievement">
              <div class="skeleton-icon"></div>
              <div class="skeleton-text skeleton-ach-name"></div>
              <div class="skeleton-text skeleton-ach-desc"></div>
            </div>
          </div>
        </template>
        <template v-else-if="achievementsError">
          <div class="error-state">
            <p>加载成就失败</p>
            <button class="retry-btn" @click="retryAchievements">重试</button>
          </div>
        </template>
        <template v-else>
          <div class="achievements-grid">
            <div
              v-for="ach in achievements"
              :key="ach.id"
              class="achievement-item"
              :class="{ unlocked: ach.unlocked }"
            >
              <div class="achievement-icon-wrap" :class="{ locked: !ach.unlocked }">
                <span class="achievement-icon">{{ ach.unlocked ? ach.icon : '🔒' }}</span>
              </div>
              <div class="achievement-info">
                <span class="achievement-name">{{ ach.name }}</span>
                <span class="achievement-desc">{{ ach.description }}</span>
              </div>
              <span v-if="ach.unlocked" class="achievement-check">✅</span>
            </div>
          </div>
        </template>
      </div>

      <!-- 退出登录 -->
      <button class="logout-btn" @click="handleLogout">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
          <polyline points="16 17 21 12 16 7" />
          <line x1="21" y1="12" x2="9" y2="12" />
        </svg>
        <span>退出登录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { useAchievementStore } from '@/stores/achievement'
import { updateProfile as updateProfileApi, changePassword as changePasswordApi, getPreferences as getPreferencesApi, updatePreferences as updatePreferencesApi } from '@/api/user'
import AvatarUpload from '@/components/AvatarUpload.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()
const achievementStore = useAchievementStore()

// 加载状态
const isLoadingProfile = computed(() => !authStore.user.id)
const isLoadingStats = computed(() => userStore.isLoadingStats)
const isLoadingLearning = computed(() => userStore.isLoading)
const isLoadingAchievements = computed(() => achievementStore.loading)
const achievementsError = computed(() => achievementStore.error && achievementStore.error.includes('成就'))

// 成就数据
const achievements = computed(() => achievementStore.achievements)
const unlockedCount = computed(() => achievementStore.unlockedCount)
const totalCount = computed(() => achievementStore.totalCount)

// 表单状态
const profileForm = reactive({
  nickname: '',
  learningGoal: '',
  bio: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: ''
})

const savingProfile = ref(false)
const savingPassword = ref(false)

// 智能干预设置表单
const interventionForm = reactive({
  enabled: true,
  progressThreshold: 65,
  scoreDeclineThreshold: 10,
  inactiveDays: 3
})
const savingIntervention = ref(false)

/**
 * 加载干预阈值偏好
 */
async function loadInterventionPreferences() {
  try {
    const res = await getPreferencesApi()
    const data = res?.data || res || {}
    if (data.interventionEnabled !== undefined) {
      interventionForm.enabled = !!data.interventionEnabled
    }
    if (data.interventionProgressThreshold != null) {
      interventionForm.progressThreshold = Number(data.interventionProgressThreshold)
    }
    if (data.interventionScoreDeclineThreshold != null) {
      interventionForm.scoreDeclineThreshold = Number(data.interventionScoreDeclineThreshold)
    }
    if (data.interventionInactiveDays != null) {
      interventionForm.inactiveDays = Number(data.interventionInactiveDays)
    }
  } catch (e) {
    console.warn('加载干预偏好失败:', e.message)
  }
}

/**
 * 保存干预阈值偏好
 */
async function handleSaveIntervention() {
  if (interventionForm.progressThreshold < 20 || interventionForm.progressThreshold > 95) {
    ElMessage.warning('进度提醒阈值需在 20%-95% 之间')
    return
  }
  if (interventionForm.scoreDeclineThreshold < 1 || interventionForm.scoreDeclineThreshold > 50) {
    ElMessage.warning('测评降幅阈值需在 1%-50% 之间')
    return
  }
  if (interventionForm.inactiveDays < 1 || interventionForm.inactiveDays > 14) {
    ElMessage.warning('连续未登录天数需在 1-14 天之间')
    return
  }

  savingIntervention.value = true
  try {
    await updatePreferencesApi({
      interventionEnabled: interventionForm.enabled,
      interventionProgressThreshold: Number(interventionForm.progressThreshold),
      interventionScoreDeclineThreshold: Number(interventionForm.scoreDeclineThreshold),
      interventionInactiveDays: Number(interventionForm.inactiveDays)
    })
    ElMessage.success('智能干预设置已保存')
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '请稍后重试'))
  } finally {
    savingIntervention.value = false
  }
}

// 重试获取成就
const retryAchievements = () => {
  achievementStore.error = null
  achievementStore.fetchAchievements()
}

// 粒子样式生成（增强版）
const particleStyle = (i) => ({
  left: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 20}s`,
  animationDuration: `${15 + Math.random() * 25}s`,
  opacity: 0.2 + Math.random() * 0.4
})

onMounted(async () => {
  // 并行加载所有数据
  await Promise.all([
    authStore.fetchProfile(),
    userStore.fetchAllUserData(),
    achievementStore.fetchAchievements()
  ])

  // 初始化表单
  profileForm.nickname = authStore.user.nickname || ''
  profileForm.learningGoal = authStore.user.learningGoal || ''
  profileForm.bio = authStore.user.bio || ''

  // 加载干预阈值偏好
  loadInterventionPreferences()
})

/**
 * 滚动到编辑表单区域
 */
function scrollToForm() {
  const el = document.querySelector('.content-grid')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

/**
 * 格式化日期
 */
function formatDate(dateStr) {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

/**
 * 头像更新回调
 */
function onAvatarUpdated(url) {
  authStore.user.avatarUrl = url
  ElMessage.success('头像更新成功')
}

/**
 * 更新个人信息
 */
async function handleUpdateProfile() {
  savingProfile.value = true
  try {
    const res = await updateProfileApi({
      nickname: profileForm.nickname,
      learningGoal: profileForm.learningGoal,
      bio: profileForm.bio
    })
    if (res.code === 200) {
      authStore.updateUser(res.data)
      ElMessage.success('个人信息更新成功')
    } else {
      throw new Error(res.message || '更新失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    savingProfile.value = false
  }
}

/**
 * 修改密码
 */
async function handleChangePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword.length < 8) {
    ElMessage.warning('新密码至少8位')
    return
  }
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(passwordForm.newPassword)) {
    ElMessage.warning('新密码必须包含字母和数字')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
    ElMessage.warning('两次新密码不一致')
    return
  }

  savingPassword.value = true
  try {
    const res = await changePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmNewPassword: passwordForm.confirmNewPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmNewPassword = ''
    } else {
      throw new Error(res.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '密码修改失败')
  } finally {
    savingPassword.value = false
  }
}

/**
 * 退出登录
 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '确认退出', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.profile-page {
  min-height: calc(100vh - 80px);
  position: relative;
  overflow: hidden;
  padding: 24px;
}

/* ===== 粒子背景（增强版 - 双色发光） ===== */
.bg-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.particle {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  animation: particle-float 20s linear infinite;
  
  &.cyan {
    background: rgba(0, 245, 212, 0.6);
    box-shadow: 0 0 10px rgba(0, 245, 212, 0.5), 0 0 20px rgba(0, 245, 212, 0.3);
  }
  
  &.purple {
    background: rgba(139, 92, 246, 0.6);
    box-shadow: 0 0 10px rgba(139, 92, 246, 0.5), 0 0 20px rgba(139, 92, 246, 0.3);
  }
}

@keyframes particle-float {
  0% {
    transform: translateY(100vh) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100px) rotate(720deg);
    opacity: 0;
  }
}

/* ===== 极光背景（增强版） ===== */
.bg-aurora {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.aurora-layer {
  position: absolute;
  border-radius: 50%;
  filter: blur(150px);
  animation: aurora 25s ease-in-out infinite;
}

.aurora-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -100px;
  background: radial-gradient(circle, rgba($accent-primary, 0.12) 0%, transparent 70%);
}

.aurora-2 {
  width: 500px;
  height: 500px;
  bottom: -150px;
  left: -100px;
  background: radial-gradient(circle, rgba($accent-cyan, 0.1) 0%, transparent 70%);
  animation-delay: -8s;
}

.aurora-3 {
  width: 400px;
  height: 400px;
  top: 40%;
  left: 40%;
  background: radial-gradient(circle, rgba($accent-blue, 0.08) 0%, transparent 70%);
  animation-delay: -15s;
}

@keyframes aurora {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(40px, -40px) scale(1.15); }
  50% { transform: translate(-30px, 30px) scale(0.9); }
  75% { transform: translate(25px, 15px) scale(1.1); }
}

/* ===== 网格纹理 ===== */
.bg-grid-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background-image: 
    linear-gradient(rgba($accent-primary, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba($accent-primary, 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
}

.profile-container {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== 页面头部（统一规范） ===== */
.page-header { @include page-header-base; }
.page-title { @include page-title-base; }
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; -webkit-text-fill-color: initial; }

.back-btn {
  @include page-header-btn-ghost;
  width: 36px;
  height: 36px;
  padding: 0;
  justify-content: center;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.2);
  color: $accent-indigo;

  &:hover {
    border-color: rgba($accent-indigo, 0.35);
    color: $accent-indigo-light;
    background: rgba($accent-indigo, 0.15);
    transform: translateX(-2px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
}

.header-action-btn {
  @include page-header-btn-ghost;
  width: 36px;
  height: 36px;
  padding: 0;
  justify-content: center;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.2);
  color: $accent-indigo;

  &:hover {
    border-color: rgba($accent-indigo, 0.35);
    color: $accent-indigo-light;
    background: rgba($accent-indigo, 0.15);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
}

.glass-card {
  position: relative;
  background: rgba($bg-primary, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  overflow: hidden;

  &:hover {
    border-color: rgba($accent-secondary, 0.18);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.35);
  }
}



.avatar-section {
  position: relative;
  padding: 32px 24px 24px;
  text-align: center;
  overflow: hidden;
}

/* 呼吸光球背景（简化） */
.avatar-breathing-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba($accent-primary, 0.1) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.avatar-pulse-ring {
  display: none;
}

@keyframes avatarBreathe {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.7; }
}

.avatar-main {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.skeleton-avatar {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: linear-gradient(90deg, rgba($accent-secondary,0.1) 25%, rgba($accent-secondary,0.2) 50%, rgba($accent-secondary,0.1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.avatar-info {
  text-align: center;
  margin-bottom: 24px;
}

.profile-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 4px;
}

.profile-username {
  font-size: 0.85rem;
  color: $text-muted;
  margin: 0 0 10px;
}

.profile-goal {
  display: inline-block;
  font-size: 0.9rem;
  color: $accent-primary;
  font-weight: 500;
  padding: 6px 16px;
  background: rgba($accent-primary, 0.06);
  border: 1px solid rgba($accent-primary, 0.1);
  border-radius: 20px;
  margin-bottom: 8px;
}

.profile-join-date {
  font-size: 0.8rem;
  color: $text-muted;
  margin: 0;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 0 8px;
  position: relative;
  z-index: 1;
}

.stat-card-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 8px;
  background: rgba($bg-primary, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 12px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba($bg-primary, 0.8);
    border-color: rgba($accent-secondary, 0.18);
    transform: translateY(-2px);
  }
}

.stat-card-value {
  font-size: 1.5rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  color: $accent-primary;
  text-shadow: 0 0 12px rgba($accent-primary, 0.2);
  line-height: 1.2;
}

.stat-card-label {
  font-size: 0.75rem;
  color: $text-secondary;
  font-weight: 500;
}

.stat-card-unit {
  font-size: 0.7rem;
  color: $text-muted;
}

.skeleton-stat {
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.content-card {
  padding: 24px;
}

.card-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-secondary, 0.06);
  display: flex;
  align-items: center;
  gap: 8px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: $text-secondary;
}

.form-input,
.form-textarea {
  padding: 10px 14px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
  font-size: 0.9rem;
  color: $text-primary;
  outline: none;
  transition: all 0.25s ease;
  font-family: inherit;

  &:focus {
    border-color: rgba($accent-primary, 0.25);
    box-shadow: 0 0 0 3px rgba($accent-primary, 0.06);
  }

  &::placeholder {
    color: $text-muted;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.form-input-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-textarea {
  resize: vertical;
  min-height: 72px;
  line-height: 1.5;
}

.save-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.2);
  color: $accent-primary;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  align-self: flex-start;

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, rgba($accent-primary, 0.25), rgba(0, 85, 255, 0.15));
    border-color: rgba($accent-primary, 0.35);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
}

.stats-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stats-row-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba($accent-secondary, 0.04);

  &:last-child {
    border-bottom: none;
  }
}

.stats-row-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-row-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
}

.stats-row-label {
  font-size: 0.85rem;
  color: $text-secondary;
}

.stats-row-value {
  font-size: 1rem;
  font-weight: 700;
  color: $text-primary;
  font-family: 'JetBrains Mono', monospace;
}

.progress-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba($accent-secondary, 0.04);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-title {
  font-size: 0.85rem;
  color: $text-secondary;
}

.progress-percent {
  font-size: 0.85rem;
  font-weight: 600;
  color: $accent-primary;
}

.progress-bar {
  height: 6px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: $accent-primary;
  border-radius: 4px;
  transition: width 1s ease;
}

.security-card {
  padding: 24px;
}

.security-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 8px;
}

.achievements-card {
  padding: 24px;
}

.achievement-progress-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 10px;
  background: rgba($accent-primary, 0.1);
  border: 1px solid rgba($accent-primary, 0.15);
  border-radius: 20px;
  color: $accent-primary;
  margin-left: auto;
}

.skeleton-badge {
  display: inline-block;
  width: 100px;
  height: 20px;
  border-radius: 20px;
  background: linear-gradient(90deg, rgba($accent-secondary,0.1) 25%, rgba($accent-secondary,0.2) 50%, rgba($accent-secondary,0.1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-left: auto;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.achievement-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: rgba($bg-primary, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 12px;
  text-align: center;
  transition: all 0.25s ease;
  position: relative;

  &.unlocked {
    border-color: rgba($accent-primary, 0.15);

    &:hover {
      border-color: rgba($accent-primary, 0.3);
      transform: translateY(-2px);
    }
  }

  &:not(.unlocked) {
    opacity: 0.5;
  }
}

.skeleton-achievement {
  height: 120px;
  padding: 12px;
}

.skeleton-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(90deg, rgba($accent-secondary,0.1) 25%, rgba($accent-secondary,0.2) 50%, rgba($accent-secondary,0.1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.achievement-icon-wrap {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  background: rgba($accent-secondary, 0.06);
  border-radius: 12px;
  flex-shrink: 0;
  transition: all 0.3s ease;

  &.locked {
    opacity: 0.5;
  }
}

.achievement-icon {
  line-height: 1;
}

.achievement-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.achievement-name {
  font-size: 0.8rem;
  font-weight: 600;
  color: $text-primary;
}

.achievement-desc {
  font-size: 0.7rem;
  color: $text-muted;
  line-height: 1.3;
}

.achievement-check {
  position: absolute;
  top: 6px;
  right: 6px;
  font-size: 0.75rem;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(255, 64, 96, 0.2);
  background: rgba($bg-primary, 0.5);
  backdrop-filter: blur(12px);
  color: #ff4060;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 64, 96, 0.06);
    border-color: rgba(255, 64, 96, 0.35);
    transform: translateY(-1px);
  }
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.skeleton-text {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba($accent-secondary,0.1) 25%, rgba($accent-secondary,0.2) 50%, rgba($accent-secondary,0.1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-name {
  width: 120px;
  height: 24px;
  margin: 0 auto 8px;
}

.skeleton-username {
  width: 80px;
  height: 14px;
  margin: 0 auto 12px;
}

.skeleton-goal {
  width: 160px;
  height: 28px;
  border-radius: 20px;
  margin: 0 auto 8px;
}

.skeleton-date {
  width: 100px;
  height: 12px;
  margin: 0 auto;
}

.skeleton-stat-value {
  width: 40px;
  height: 24px;
}

.skeleton-stat-label {
  width: 50px;
  height: 12px;
}

.skeleton-stats-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid rgba($accent-secondary, 0.06);
}

.skeleton-stats-label {
  width: 80px;
  height: 14px;
}

.skeleton-stats-value {
  width: 40px;
  height: 14px;
}

.skeleton-progress {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba($accent-secondary, 0.06);
  height: 30px;
}

.skeleton-ach-name {
  width: 60px;
  height: 12px;
  margin-top: 4px;
}

.skeleton-ach-desc {
  width: 80px;
  height: 10px;
  margin-top: 2px;
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
  color: #ff4060;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 16px;
  background: rgba(255, 64, 96, 0.1);
  border: 1px solid rgba(255, 64, 96, 0.3);
  border-radius: 8px;
  color: #ff4060;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(255, 64, 96, 0.2);
  }
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .security-form-row {
    grid-template-columns: 1fr;
  }

  .achievements-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .achievements-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .profile-page {
    padding: 16px;
  }

  .avatar-section {
    padding: 24px 16px 20px;
  }

  .content-card,
  .security-card,
  .achievements-card {
    padding: 16px;
  }
}

/* ===== 智能干预设置 ===== */
.intervention-desc {
  font-size: 0.8rem;
  color: $text-muted;
  margin: 0 0 16px;
}

.intervention-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  background: rgba($bg-primary, 0.4);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
  margin-bottom: 16px;
}

.setting-label-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.setting-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: $text-primary;
}

.setting-hint {
  font-size: 0.7rem;
  color: $text-muted;
  line-height: 1.4;
}

/* 开关 */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  flex-shrink: 0;
  cursor: pointer;

  input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  .toggle-slider {
    position: absolute;
    inset: 0;
    background: rgba($accent-secondary, 0.2);
    border-radius: 12px;
    transition: all 0.3s ease;

    &::before {
      content: '';
      position: absolute;
      width: 18px;
      height: 18px;
      left: 3px;
      top: 3px;
      background: #fff;
      border-radius: 50%;
      transition: all 0.3s ease;
    }
  }

  input:checked + .toggle-slider {
    background: linear-gradient(135deg, $accent-primary, #0ea5e9);

    &::before {
      transform: translateX(20px);
    }
  }
}

.intervention-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.threshold-input {
  position: relative;

  .form-input {
    padding-right: 40px;
  }

  .threshold-unit {
    position: absolute;
    right: 14px;
    top: 50%;
    transform: translateY(-50%);
    color: $text-muted;
    font-size: 0.8rem;
    pointer-events: none;
  }
}

@media (max-width: 768px) {
  .intervention-grid {
    grid-template-columns: 1fr;
  }

  .intervention-setting-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>