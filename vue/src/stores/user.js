// 用户学习数据状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserStats, getLearningStats, getAchievements } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // 统计数据
  const userStats = ref({
    streak: 0,
    totalHours: 0,
    completedNodes: 0,
    achievementCount: 0,
    completionRate: 0
  })

  // 学习统计数据
  const learningStats = ref({
    completedNodes: 0,
    totalTasks: 0,
    averageScore: 0,
    weeklyProgress: 0,
    progress: 0,
    weeklyGoal: 20
  })

  // 成就数据
  const achievements = ref({
    unlocked: 0,
    total: 0,
    list: []
  })

  // 学习偏好
  const preferences = ref({
    language: 'zh-CN',
    theme: 'dark',
    notifications: true,
    dailyReminder: true,
    reminderTime: '09:00'
  })

  // 加载状态
  const isLoading = ref(false)
  const isLoadingStats = ref(false)
  const isLoadingAchievements = ref(false)
  const error = ref(null)

  // 计算属性：完成率
  const completionRate = computed(() => userStats.value.completionRate)

  // 计算属性：周目标完成率
  const weeklyCompletionRate = computed(() => {
    if (learningStats.value.weeklyGoal === 0) return 0
    return Math.round((learningStats.value.weeklyProgress / learningStats.value.weeklyGoal) * 100)
  })

  // 计算属性：连续学习天数
  const streakDays = computed(() => userStats.value.streak)

  // 计算属性：总学时
  const totalHours = computed(() => userStats.value.totalHours)

  // 获取统计数据
  const fetchUserStats = async () => {
    isLoadingStats.value = true
    error.value = null
    try {
      const res = await getUserStats()
      if (res.code === 200 && res.data) {
        userStats.value = res.data
      }
    } catch (err) {
      error.value = err.message || '获取统计数据失败'
      console.error('获取统计数据失败:', err)
    } finally {
      isLoadingStats.value = false
    }
  }

  // 获取学习统计
  const fetchLearningStats = async () => {
    isLoading.value = true
    error.value = null
    try {
      const res = await getLearningStats()
      if (res.code === 200 && res.data) {
        learningStats.value = res.data
      }
    } catch (err) {
      error.value = err.message || '获取学习统计失败'
      console.error('获取学习统计失败:', err)
    } finally {
      isLoading.value = false
    }
  }

  // 获取成就列表
  const fetchAchievements = async () => {
    isLoadingAchievements.value = true
    error.value = null
    try {
      const res = await getAchievements()
      if (res.code === 200 && res.data) {
        achievements.value = res.data
      }
    } catch (err) {
      error.value = err.message || '获取成就列表失败'
      console.error('获取成就列表失败:', err)
    } finally {
      isLoadingAchievements.value = false
    }
  }

  // 获取所有用户数据
  const fetchAllUserData = async () => {
    await Promise.all([
      fetchUserStats(),
      fetchLearningStats(),
      fetchAchievements()
    ])
  }

  // 更新学习进度
  const updateProgress = (value) => {
    if (value >= 0 && value <= 100) {
      learningStats.value.progress = value
    }
  }

  // 更新今日学习时长
  const updateTodayHours = (hours) => {
    if (hours >= 0) {
      userStats.value.totalHours += hours
    }
  }

  // 解锁成就
  const unlockAchievement = (achievementId) => {
    const achievement = achievements.value.list.find(a => a.id === achievementId)
    if (achievement && !achievement.unlocked) {
      achievement.unlocked = true
      achievements.value.unlocked++
      return true
    }
    return false
  }

  // 更新统计数据
  const updateStats = (newStats) => {
    userStats.value = { ...userStats.value, ...newStats }
  }

  // 更新学习偏好
  const updatePreferences = (newPreferences) => {
    preferences.value = { ...preferences.value, ...newPreferences }
  }

  // 增加连续学习天数
  const incrementStreak = () => {
    userStats.value.streak++
  }

  // 重置连续学习天数
  const resetStreak = () => {
    userStats.value.streak = 0
  }

  return {
    // 状态
    userStats,
    learningStats,
    achievements,
    preferences,
    isLoading,
    isLoadingStats,
    isLoadingAchievements,
    error,

    // 计算属性
    completionRate,
    weeklyCompletionRate,
    streakDays,
    totalHours,

    // 方法
    fetchUserStats,
    fetchLearningStats,
    fetchAchievements,
    fetchAllUserData,
    updateProgress,
    updateTodayHours,
    unlockAchievement,
    updateStats,
    updatePreferences,
    incrementStreak,
    resetStreak
  }
})