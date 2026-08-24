<template>
  <div class="achievements-page">
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
        <span class="title-icon">🏆</span>
        <span class="title-text">成就打卡</span>
      </h1>
      <button class="checkin-btn" :disabled="isChecking || checkedInToday" @click="doCheckin">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12" /></svg>
        <span>{{ isChecking ? '打卡中...' : checkedInToday ? '今日已打卡' : '每日打卡' }}</span>
      </button>
    </header>

    <div class="achievements-content">
      <div class="streak-section">
        <div class="streak-card">
          <div class="streak-main">
            <span class="streak-number">{{ streak }}</span>
            <span class="streak-unit">天</span>
          </div>
          <span class="streak-label">连续打卡</span>
          <div class="streak-dots">
            <span v-for="i in 7" :key="i" class="streak-dot" :class="{ active: i <= streak % 7 || (streak > 0 && i <= streak) }"></span>
          </div>
        </div>
        <div class="monthly-card">
          <div class="monthly-header">
            <span class="monthly-title">本月打卡</span>
            <span class="monthly-count">{{ monthlyCheckins }}/{{ daysInMonth }}</span>
          </div>
          <div class="monthly-grid">
            <span v-for="d in daysInMonth" :key="d" class="day-cell" :class="{ checked: isCheckedIn(d), today: d === todayDay, future: d > todayDay }">
              {{ d }}
            </span>
          </div>
        </div>
      </div>

      <div class="achievements-grid">
        <h3 class="section-title">🎖️ 成就徽章</h3>
        <div class="badge-grid">
          <div v-for="badge in badges" :key="badge.id" class="badge-card" :class="{ unlocked: badge.unlocked, locked: !badge.unlocked }">
            <div class="badge-icon-wrap">
              <span class="badge-icon">{{ badge.icon }}</span>
            </div>
            <div class="badge-info">
              <span class="badge-name">{{ badge.name }}</span>
              <span class="badge-desc">{{ badge.desc }}</span>
            </div>
            <div class="badge-right">
              <span class="badge-status">{{ badge.unlocked ? '✅ 已解锁' : '🔒 未解锁' }}</span>
              <button v-if="badge.unlocked" class="badge-share-btn" title="分享成就" @click="shareBadge(badge)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" /></svg>
                分享
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="history-section">
        <h3 class="section-title">📋 打卡记录</h3>
        <div class="history-list">
          <div v-for="record in checkinHistory" :key="record.date" class="history-item">
            <span class="history-date">{{ formatDate(record.date) }}</span>
            <span class="history-status checked-in">✅ 已打卡</span>
            <span class="history-time">{{ record.time }}</span>
          </div>
          <div v-if="checkinHistory.length === 0" class="history-empty">
            <p>还没有打卡记录，开始你的第一天打卡吧！</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 成就分享弹窗 ===== -->
    <transition name="modal-fade">
      <div v-if="shareVisible" class="share-overlay" @click.self="closeShare">
        <div class="share-dialog">
          <div class="share-header">
            <h3 class="share-title">🎉 分享成就</h3>
            <button class="share-close" @click="closeShare">✕</button>
          </div>
          <div v-if="shareData" class="share-body">
            <div class="share-badge-preview">
              <span class="share-badge-icon">{{ shareData.shareData?.icon }}</span>
              <div class="share-badge-text">
                <span class="share-badge-name">{{ shareData.shareData?.achievementName }}</span>
                <span class="share-badge-date">解锁于 {{ shareData.shareData?.unlockedAt }}</span>
              </div>
            </div>
            <div class="share-text-box">{{ shareData.shareText }}</div>
            <div class="share-url-box">
              <span class="share-url-label">成就链接</span>
              <span class="share-url">{{ shareData.shareUrl }}</span>
            </div>
          </div>
          <div class="share-footer">
            <button class="btn-copy" @click="copyShareText">📋 复制分享文案</button>
            <button class="btn-close" @click="closeShare">关闭</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAchievementStore } from '@/stores/achievement'
import { ElMessage } from 'element-plus'
import { checkin, getCheckinStats, shareAchievement } from '@/api/achievement'

const router = useRouter()
const achievementStore = useAchievementStore()

const now = new Date()
const todayDay = now.getDate()
const thisYear = now.getFullYear()
const thisMonth = now.getMonth() + 1
const daysInMonth = new Date(thisYear, thisMonth, 0).getDate()

const isChecking = ref(false)
const checkinStats = ref({
    todayChecked: false,
    continuousDays: 0,
    totalDays: 0,
    monthDays: []
})

const checkedInToday = computed(() => checkinStats.value.todayChecked)

const streak = computed(() => checkinStats.value.continuousDays)

const monthlyCheckins = computed(() => {
  if (!checkinStats.value.monthDays) return 0
  return checkinStats.value.monthDays.length
})

const checkinHistory = computed(() => {
  if (!checkinStats.value.monthDays) return []
  return checkinStats.value.monthDays.map(date => ({
    date: date,
    time: '00:00'
  })).reverse()
})

const badges = computed(() => {
  return achievementStore.achievements.map(a => ({
    id: a.id,
    name: a.name,
    icon: a.icon,
    desc: a.description,
    unlocked: a.unlocked
  }))
})

function isCheckedIn(day) {
  if (!checkinStats.value.monthDays) return false
  const dayStr = `${thisYear}-${String(thisMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  return checkinStats.value.monthDays.some(d => {
    if (typeof d === 'string') return d === dayStr
    if (d && d.toString) return d.toString() === dayStr
    return false
  })
}

async function doCheckin() {
  if (isChecking.value) return
  if (checkedInToday.value) {
    ElMessage.warning('今天已打卡，明天再来吧！')
    return
  }
  isChecking.value = true
  try {
    const res = await checkin()
    checkinStats.value = res.data
    ElMessage.success(`🎉 打卡成功！已连续打卡 ${res.data.continuousDays} 天`)
    await achievementStore.fetchAchievements()
    await loadCheckinStats()
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '打卡失败，请重试'
    ElMessage.error(msg)
  } finally {
    isChecking.value = false
  }
}

async function loadCheckinStats() {
  try {
    const res = await getCheckinStats()
    if (res.data) {
      checkinStats.value = res.data
    }
  } catch (error) {
    console.error('加载打卡统计失败', error)
  }
}

function formatDate(d) {
  if (!d) return ''
  const dateStr = typeof d === 'string' ? d : d.toString()
  const parts = dateStr.split('-')
  if (parts.length !== 3) return dateStr
  return `${parts[0]}年${parts[1]}月${parts[2]}日`
}

async function loadData() {
  await achievementStore.fetchAchievements()
  await loadCheckinStats()
}

// ===== 成就分享 =====
const shareVisible = ref(false)
const shareData = ref(null)
const sharingBadge = ref(false)

async function shareBadge(badge) {
  if (sharingBadge.value) return
  sharingBadge.value = true
  try {
    const res = await shareAchievement(badge.id, 'text')
    shareData.value = res?.data || null
    shareVisible.value = true
  } catch (error) {
    ElMessage.error('分享失败：' + (error.message || '未知错误'))
  } finally {
    sharingBadge.value = false
  }
}

function closeShare() {
  shareVisible.value = false
  shareData.value = null
}

async function copyShareText() {
  if (!shareData.value?.shareText) return
  try {
    await navigator.clipboard.writeText(shareData.value.shareText)
    ElMessage.success('分享文案已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/modules')
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;
.achievements-page {
  min-height: calc(100vh - 68px); background: $bg-primary; position: relative;
}
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora { position: absolute; inset: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba($accent-primary,0.06) 0%, transparent 50%), radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%), radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift { 0%,100% { transform: scale(1) rotate(0deg); } 33% { transform: scale(1.08) rotate(0.8deg); } 66% { transform: scale(0.95) rotate(-0.6deg); } }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba($accent-primary,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px); background-size: 40px 40px; animation: gridPulse 8s ease-in-out infinite alternate; }
@keyframes gridPulse { 0% { opacity: 0.3; } 100% { opacity: 0.6; } }

.page-header {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; gap: 16px;
  padding: 16px 32px;
  background: rgba($bg-primary,0.85); backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba($accent-secondary,0.08);
}
.back-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; background: rgba($accent-secondary,0.06);
  border: 1px solid rgba($accent-secondary,0.1); border-radius: 8px;
  color: $text-secondary; font-size: 0.82rem; cursor: pointer;
  transition: all 0.25s ease;
  &:hover { border-color: rgba($accent-primary,0.2); color: $accent-primary; }
}
.page-title { flex: 1; display: flex; align-items: center; gap: 10px; }
.title-icon { font-size: 1.3rem; }
.title-text {
  font-size: 1.05rem; font-weight: 700;
  background: linear-gradient(135deg, $accent-amber, $accent-red);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.checkin-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px;
  background: linear-gradient(135deg, rgba(245,158,11,0.15), rgba(239,68,68,0.1));
  border: 1px solid rgba(245,158,11,0.2); border-radius: 10px;
  color: $accent-amber; font-size: 0.85rem; font-weight: 600; cursor: pointer;
  transition: all 0.25s ease;
  &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 0 20px rgba(245,158,11,0.15); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.achievements-content {
  max-width: 1000px; margin: 0 auto;
  padding: 24px 32px 60px; position: relative; z-index: 1;
}

.streak-section { display: grid; grid-template-columns: 1fr 1.5fr; gap: 16px; margin-bottom: 28px; }
.streak-card {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 24px;
  background: rgba($bg-primary,0.6); backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.1); border-radius: 16px;
}
.streak-main { display: flex; align-items: baseline; gap: 4px; }
.streak-number { font-size: 3rem; font-weight: 900; font-family: 'JetBrains Mono', monospace; color: $accent-amber; text-shadow: 0 0 20px rgba(245,158,11,0.3); }
.streak-unit { font-size: 1rem; font-weight: 600; color: $text-secondary; }
.streak-label { font-size: 0.82rem; color: $text-muted; }
.streak-dots { display: flex; gap: 6px; }
.streak-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: rgba($accent-secondary,0.15);
  &.active { background: $accent-amber; box-shadow: 0 0 6px rgba(245,158,11,0.4); }
}

.monthly-card {
  padding: 20px;
  background: rgba($bg-primary,0.5); backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08); border-radius: 16px;
}
.monthly-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.monthly-title { font-size: 0.85rem; font-weight: 700; color: $text-primary; }
.monthly-count { font-size: 0.8rem; color: $accent-amber; font-family: 'JetBrains Mono', monospace; }
.monthly-grid {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px;
}
.day-cell {
  aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
  font-size: 0.72rem; color: $text-secondary; border-radius: 6px;
  background: rgba($accent-secondary,0.03);
  &.today { border: 1px solid rgba(245,158,11,0.3); color: $accent-amber; font-weight: 700; }
  &.checked { background: rgba(245,158,11,0.12); color: $accent-amber; font-weight: 600; }
  &.future { opacity: 0.2; }
}

.section-title { font-size: 1rem; font-weight: 700; color: $text-primary; margin: 0 0 16px; }

.badge-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; margin-bottom: 28px; }
.badge-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 18px; border-radius: 14px;
  background: rgba($bg-primary,0.5); backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08);
  transition: all 0.3s ease;
  &.unlocked { border-color: rgba(245,158,11,0.15); &:hover { border-color: rgba(245,158,11,0.3); transform: translateY(-2px); } }
  &.locked { opacity: 0.5; }
}
.badge-icon-wrap { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; border-radius: 12px; background: rgba($accent-secondary,0.06); flex-shrink: 0; }
.badge-icon { font-size: 1.2rem; }
.badge-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.badge-name { font-size: 0.85rem; font-weight: 600; color: $text-primary; }
.badge-desc { font-size: 0.72rem; color: $text-muted; }
.badge-status { font-size: 0.7rem; font-weight: 500; white-space: nowrap; }

.badge-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.badge-share-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 8px;
  color: $accent-amber;
  font-size: 0.7rem;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(245, 158, 11, 0.15);
    border-color: rgba(245, 158, 11, 0.35);
  }
}

/* ===== 分享弹窗 ===== */
.share-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($bg-primary, 0.7);
  backdrop-filter: blur(8px);
}

.share-dialog {
  width: 420px;
  max-width: calc(100vw - 32px);
  background: rgba($bg-primary, 0.97);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.share-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.share-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.share-close {
  background: none;
  border: none;
  color: $text-muted;
  font-size: 1rem;
  cursor: pointer;
}

.share-badge-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.15);
  border-radius: 12px;
  margin-bottom: 12px;
}

.share-badge-icon {
  font-size: 2rem;
}

.share-badge-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.share-badge-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: $accent-amber;
}

.share-badge-date {
  font-size: 0.72rem;
  color: $text-muted;
}

.share-text-box {
  padding: 14px;
  background: rgba($accent-secondary, 0.05);
  border: 1px dashed rgba($accent-secondary, 0.15);
  border-radius: 10px;
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.7;
  margin-bottom: 10px;
  word-break: break-all;
}

.share-url-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.share-url-label {
  font-size: 0.72rem;
  color: $text-muted;
  flex-shrink: 0;
}

.share-url {
  font-size: 0.75rem;
  color: #3a86ff;
  font-family: 'JetBrains Mono', monospace;
  word-break: break-all;
}

.share-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn-copy {
  padding: 9px 16px;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: 8px;
  color: $accent-amber;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(245, 158, 11, 0.2);
  }
}

.btn-close {
  padding: 9px 16px;
  background: rgba($accent-secondary, 0.08);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.82rem;
  cursor: pointer;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.history-section { margin-bottom: 28px; }
.history-list { display: flex; flex-direction: column; gap: 8px; }
.history-item {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 16px;
  background: rgba($bg-primary,0.3); border: 1px solid rgba($accent-secondary,0.04); border-radius: 10px;
}
.history-date { font-size: 0.82rem; color: $text-secondary; min-width: 120px; }
.history-status { font-size: 0.78rem; }
.history-time { margin-left: auto; font-size: 0.72rem; color: $text-muted; font-family: 'JetBrains Mono', monospace; }
.history-empty { text-align: center; padding: 30px; color: $text-muted; font-size: 0.85rem; }

@media (max-width: 1024px) {
  .page-header { padding: 12px 20px; }
  .achievements-content { padding: 20px; }
  .streak-section { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .page-header { padding: 10px 12px; gap: 10px; flex-wrap: wrap; }
  .back-btn span, .checkin-btn span { display: none; }
  .achievements-content { padding: 12px; }
  .streak-number { font-size: 2.2rem; }
  .badge-grid { grid-template-columns: 1fr; }
}
</style>