// 全局设置状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  // 联网搜索开关（默认关闭）
  const webSearchEnabled = ref(false)

  // 切换联网搜索状态
  const toggleWebSearch = () => {
    webSearchEnabled.value = !webSearchEnabled.value
    // 持久化到 localStorage
    localStorage.setItem('webSearchEnabled', JSON.stringify(webSearchEnabled.value))
  }

  // 设置联网搜索状态
  const setWebSearch = (enabled) => {
    webSearchEnabled.value = enabled
    localStorage.setItem('webSearchEnabled', JSON.stringify(enabled))
  }

  // 初始化时从 localStorage 恢复
  const initSettings = () => {
    const saved = localStorage.getItem('webSearchEnabled')
    if (saved !== null) {
      try {
        webSearchEnabled.value = JSON.parse(saved)
      } catch (e) {
        webSearchEnabled.value = false
      }
    }
  }

  return {
    webSearchEnabled,
    toggleWebSearch,
    setWebSearch,
    initSettings
  }
})
