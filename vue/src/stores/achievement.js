import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAchievements, checkAchievements, checkIn } from '@/api/achievement'

export const useAchievementStore = defineStore('achievement', () => {
  const achievements = ref([])
  const unlockedCount = ref(0)
  const totalCount = ref(12)
  const loading = ref(false)
  const error = ref(null)
  const lastCheckTime = ref(null)

  const unlockedAchievements = computed(() => achievements.value.filter(a => a.unlocked))
  const lockedAchievements = computed(() => achievements.value.filter(a => !a.unlocked))
  const progress = computed(() => {
    if (totalCount.value === 0) return 0
    return Math.round((unlockedCount.value / totalCount.value) * 100)
  })

  async function fetchAchievements() {
    loading.value = true
    error.value = null
    try {
      const res = await getAchievements()
      if (res.code === 200 && res.data) {
        achievements.value = res.data.list || []
        unlockedCount.value = res.data.unlocked || 0
        totalCount.value = res.data.total || achievements.value.length
      }
    } catch (err) {
      error.value = err.message || '获取成就列表失败'
      console.error('获取成就列表失败:', err)
    } finally {
      loading.value = false
    }
  }

  async function checkAndUpdate() {
    loading.value = true
    error.value = null
    try {
      const res = await checkAchievements()
      if (res.code === 200 && res.data) {
        if (res.data.updated) {
          await fetchAchievements()
          return res.data.newlyUnlocked || []
        }
      }
      return []
    } catch (err) {
      error.value = err.message || '检查成就失败'
      console.error('检查成就失败:', err)
      return []
    } finally {
      loading.value = false
    }
  }

  async function doCheckIn() {
    loading.value = true
    error.value = null
    try {
      const res = await checkIn()
      if (res.code === 200) {
        lastCheckTime.value = new Date()
        const newlyUnlocked = await checkAndUpdate()
        return { success: true, newlyUnlocked }
      }
      return { success: false, newlyUnlocked: [] }
    } catch (err) {
      error.value = err.message || '打卡失败'
      console.error('打卡失败:', err)
      return { success: false, error: err.message }
    } finally {
      loading.value = false
    }
  }

  async function refresh() {
    await fetchAchievements()
  }

  return {
    achievements,
    unlockedCount,
    totalCount,
    loading,
    error,
    lastCheckTime,
    unlockedAchievements,
    lockedAchievements,
    progress,
    fetchAchievements,
    checkAndUpdate,
    doCheckIn,
    refresh
  }
})