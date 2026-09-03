// 全局状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 全局加载状态
  const isLoading = ref(false)
  const loadingText = ref('加载中...')
  
  // 错误信息
  const error = ref(null)
  
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
  
  return {
    isLoading,
    loadingText,
    error,
    setLoading,
    setError,
    clearError
  }
})