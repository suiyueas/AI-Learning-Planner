// 智能通知中心状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getNotifications as fetchNotificationsApi,
  getUnreadStats as fetchUnreadStatsApi,
  markNotificationAsRead as markReadApi,
  markAllNotificationsAsRead as markAllReadApi,
  markNotificationAsHandled as markHandledApi,
  deleteNotification as deleteApi,
  clearAllNotifications as clearAllApi,
  scanInterventions as scanApi
} from '@/api/notificationApi'

const POLL_INTERVAL = 60000 // 未读数轮询间隔：60秒

export const useNotificationStore = defineStore('notification', () => {
  // 状态
  const notifications = ref([])
  const unreadTotal = ref(0)
  const unreadEmergency = ref(0)
  const loading = ref(false)
  const lastError = ref(null)
  let pollTimer = null

  // 计算属性
  // 角标逻辑：存在 P0 未读 → 红色数字角标（含数量）；仅 P1/P2 → 蓝色普通角标
  const hasEmergencyUnread = computed(() => unreadEmergency.value > 0)
  const unreadCount = computed(() => unreadTotal.value)
  const badgeColor = computed(() => hasEmergencyUnread.value ? 'red' : 'blue')

  // 获取通知列表
  const fetchNotifications = async () => {
    loading.value = true
    try {
      const res = await fetchNotificationsApi()
      notifications.value = Array.isArray(res) ? res : (res?.data || [])
      await fetchUnreadStats()
      return notifications.value
    } catch (e) {
      lastError.value = e
      throw e
    } finally {
      loading.value = false
    }
  }

  // 获取未读统计
  const fetchUnreadStats = async () => {
    try {
      const res = await fetchUnreadStatsApi()
      const data = res?.data || res || {}
      unreadTotal.value = Number(data.total) || 0
      unreadEmergency.value = Number(data.emergency) || 0
    } catch (e) {
      lastError.value = e
    }
  }

  // 标记单条已读（本地即时更新 + 服务端同步）
  const markAsRead = async (id) => {
    const item = notifications.value.find(n => n.id === id)
    if (item && !item.isRead) {
      item.isRead = true
      unreadTotal.value = Math.max(0, unreadTotal.value - 1)
      if (item.priority === 'EMERGENCY') {
        unreadEmergency.value = Math.max(0, unreadEmergency.value - 1)
      }
    }
    try {
      await markReadApi(id)
    } catch (e) {
      lastError.value = e
    }
  }

  // 全部标记已读
  const markAllAsRead = async () => {
    notifications.value.forEach(n => { n.isRead = true })
    unreadTotal.value = 0
    unreadEmergency.value = 0
    try {
      await markAllReadApi()
    } catch (e) {
      lastError.value = e
    }
  }

  // 标记干预类通知为已处理
  const markAsHandled = async (id) => {
    const item = notifications.value.find(n => n.id === id)
    if (item) {
      item.isHandled = true
      item.handledAt = new Date().toISOString()
    }
    try {
      await markHandledApi(id)
    } catch (e) {
      lastError.value = e
    }
  }

  // 删除单条通知
  const remove = async (id) => {
    notifications.value = notifications.value.filter(n => n.id !== id)
    try {
      await deleteApi(id)
      await fetchUnreadStats()
    } catch (e) {
      lastError.value = e
    }
  }

  // 清空所有通知
  const clearAll = async () => {
    notifications.value = []
    unreadTotal.value = 0
    unreadEmergency.value = 0
    try {
      await clearAllApi()
    } catch (e) {
      lastError.value = e
    }
  }

  // 手动触发干预扫描
  const scan = async () => {
    try {
      const res = await scanApi()
      await fetchNotifications()
      return res
    } catch (e) {
      lastError.value = e
      throw e
    }
  }

  // 启动未读数轮询（组件常驻时调用）
  const startPolling = () => {
    stopPolling()
    pollTimer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        fetchUnreadStats()
      }
    }, POLL_INTERVAL)
  }

  // 停止轮询
  const stopPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  // 重置（退出登录时调用）
  const reset = () => {
    stopPolling()
    notifications.value = []
    unreadTotal.value = 0
    unreadEmergency.value = 0
    lastError.value = null
  }

  return {
    // 状态
    notifications,
    unreadTotal,
    unreadEmergency,
    loading,
    lastError,
    // 计算属性
    hasEmergencyUnread,
    unreadCount,
    badgeColor,
    // 方法
    fetchNotifications,
    fetchUnreadStats,
    markAsRead,
    markAllAsRead,
    markAsHandled,
    remove,
    clearAll,
    scan,
    startPolling,
    stopPolling,
    reset
  }
})
