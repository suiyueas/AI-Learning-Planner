// 统计数据状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDashboardStats } from '@/api/statsApi'

export const useStatsStore = defineStore('stats', () => {
  // 统计数据
  const dashboardStats = ref({
    progress: 0,
    todayHours: 0,
    unlockedAchievements: 0,
    totalAchievements: 12,
    continuousDays: 0,
    totalLearners: 18400,
    satisfaction: 98,
    onlineService: '24/7'
  })

  // 是否正在加载
  const isLoading = ref(false)

  // 加载错误
  const loadError = ref(null)

  // 获取仪表板统计数据
  const fetchDashboardStats = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getDashboardStats()
      if (res.success && res.data) {
        dashboardStats.value = res.data
      }
    } catch (error) {
      console.error('获取统计数据失败:', error)
      loadError.value = error.message || '获取统计数据失败'
    } finally {
      isLoading.value = false
    }
  }

  return {
    dashboardStats,
    isLoading,
    loadError,
    fetchDashboardStats
  }
})
