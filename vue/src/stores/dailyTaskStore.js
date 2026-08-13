// 每日任务状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getTodayTasks, getDailyTasks, getWeekPreview, updateDailyTaskStatus, regenerateDailyTasks } from '@/api/learningPath'

export const useDailyTaskStore = defineStore('dailyTask', () => {
  // 今日任务
  const todayPlan = ref(null)
  // 本周预览
  const weekPreview = ref(null)
  // 加载状态
  const isLoading = ref(false)
  // 加载错误
  const loadError = ref(null)
  // 当前正在操作的任务 ID
  const updatingTaskId = ref(null)

  // 今日完成度
  const todayCompletion = computed(() => {
    if (!todayPlan.value || !todayPlan.value.tasks || todayPlan.value.totalTasks === 0) return 0
    return Math.round((todayPlan.value.totalCompleted / todayPlan.value.totalTasks) * 100)
  })

  // 是否全部完成
  const allCompleted = computed(() => {
    return todayPlan.value?.allCompleted || false
  })

  /**
   * 获取今日任务
   */
  const fetchTodayTasks = async (pathId) => {
    if (!pathId) return
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getTodayTasks(pathId)
      todayPlan.value = res?.data ?? res
    } catch (error) {
      console.error('获取今日任务失败:', error)
      loadError.value = error.message || '获取今日任务失败'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 获取指定日期的任务
   */
  const fetchDailyTasks = async (pathId, date) => {
    if (!pathId) return
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getDailyTasks(pathId, date)
      return res?.data ?? res
    } catch (error) {
      console.error('获取每日任务失败:', error)
      loadError.value = error.message || '获取每日任务失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 获取本周预览
   */
  const fetchWeekPreview = async (pathId, date) => {
    if (!pathId) return
    try {
      const res = await getWeekPreview(pathId, date)
      weekPreview.value = res?.data ?? res
    } catch (error) {
      console.error('获取本周预览失败:', error)
    }
  }

  /**
   * 更新任务状态
   */
  const updateStatus = async (pathId, taskId, status) => {
    updatingTaskId.value = taskId
    try {
      await updateDailyTaskStatus(pathId, taskId, status)
      // 刷新今日任务
      await fetchTodayTasks(pathId)
      await fetchWeekPreview(pathId)
    } catch (error) {
      console.error('更新任务状态失败:', error)
      throw error
    } finally {
      updatingTaskId.value = null
    }
  }

  /**
   * 重新生成每日任务
   */
  const regenerate = async (pathId) => {
    isLoading.value = true
    try {
      await regenerateDailyTasks(pathId)
      await fetchTodayTasks(pathId)
      await fetchWeekPreview(pathId)
    } catch (error) {
      console.error('重新生成任务失败:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  return {
    todayPlan,
    weekPreview,
    isLoading,
    loadError,
    updatingTaskId,
    todayCompletion,
    allCompleted,
    fetchTodayTasks,
    fetchDailyTasks,
    fetchWeekPreview,
    updateStatus,
    regenerate
  }
})
