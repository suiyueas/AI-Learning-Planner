// 全局状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 全局加载状态
  const isLoading = ref(false)
  const loadingText = ref('加载中...')
  
  // 错误信息
  const error = ref(null)
  
  // 通知列表
  const notifications = ref([])
  
  // 用户信息
  const user = ref({
    id: 'user_1',
    name: '用户',
    avatar: '明',
    email: 'user@example.com'
  })
  
  // 应用配置
  const config = ref({
    apiBaseUrl: '/api',
    sseEnabled: true,
    theme: 'light'
  })
  
  // 计算属性：未读通知数
  const unreadNotifications = computed(() => {
    return notifications.value.filter(n => !n.read).length
  })
  
  // 设置加载状态
  const setLoading = (status, text = '加载中...') => {
    isLoading.value = status
    loadingText.value = text
  }
  
  // 设置错误
  const setError = (errorInfo) => {
    error.value = errorInfo
    
    // 自动清除错误（5秒后）
    if (errorInfo) {
      setTimeout(() => {
        error.value = null
      }, 5000)
    }
  }
  
  // 清除错误
  const clearError = () => {
    error.value = null
  }
  
  // 添加通知
  const addNotification = (notification) => {
    const newNotification = {
      id: 'notif_' + Date.now(),
      read: false,
      timestamp: new Date(),
      ...notification
    }
    
    notifications.value.unshift(newNotification)
    
    // 限制通知数量（最多50条）
    if (notifications.value.length > 50) {
      notifications.value = notifications.value.slice(0, 50)
    }
    
    return newNotification
  }
  
  // 标记通知为已读
  const markAsRead = (notificationId) => {
    const notification = notifications.value.find(n => n.id === notificationId)
    if (notification) {
      notification.read = true
    }
  }
  
  // 标记所有通知为已读
  const markAllAsRead = () => {
    notifications.value.forEach(n => {
      n.read = true
    })
  }
  
  // 删除通知
  const deleteNotification = (notificationId) => {
    const index = notifications.value.findIndex(n => n.id === notificationId)
    if (index > -1) {
      notifications.value.splice(index, 1)
    }
  }
  
  // 清空所有通知
  const clearNotifications = () => {
    notifications.value = []
  }
  
  // 更新用户信息
  const updateUser = (userData) => {
    user.value = { ...user.value, ...userData }
  }
  
  // 更新配置
  const updateConfig = (configData) => {
    config.value = { ...config.value, ...configData }
  }
  
  return {
    isLoading,
    loadingText,
    error,
    notifications,
    user,
    config,
    unreadNotifications,
    setLoading,
    setError,
    clearError,
    addNotification,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    clearNotifications,
    updateUser,
    updateConfig
  }
})