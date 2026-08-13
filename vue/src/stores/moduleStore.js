// 功能模块状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getModules, getModuleGroups } from '@/api/moduleApi'

export const useModuleStore = defineStore('module', () => {
  // 模块列表
  const modules = ref([])

  // 模块分组列表
  const moduleGroups = ref([])

  // 是否正在加载
  const isLoading = ref(false)

  // 加载错误
  const loadError = ref(null)

  // 获取模块列表
  const fetchModules = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getModules()
      if (res.success && res.data) {
        modules.value = res.data
      }
    } catch (error) {
      console.error('获取模块列表失败:', error)
      loadError.value = error.message || '获取模块列表失败'
    } finally {
      isLoading.value = false
    }
  }

  // ===== 模块状态持久化 =====
  // 从 localStorage 加载模块使用状态
  function loadModuleStatus() {
    try {
      const saved = localStorage.getItem('ai-module-status')
      return saved ? JSON.parse(saved) : {}
    } catch {
      return {}
    }
  }

  const moduleStatus = ref(loadModuleStatus())

  function saveModuleStatus() {
    localStorage.setItem('ai-module-status', JSON.stringify(moduleStatus.value))
  }

  function updateModuleStatus(id, data) {
    moduleStatus.value[id] = { ...(moduleStatus.value[id] || {}), ...data }
    saveModuleStatus()
  }

  function getModuleStatus(id) {
    return moduleStatus.value[id] || {}
  }

  // 获取模块分组列表
  const fetchModuleGroups = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getModuleGroups()
      if (res.success && res.data) {
        moduleGroups.value = res.data
      }
    } catch (error) {
      console.error('获取模块分组失败:', error)
      loadError.value = error.message || '获取模块分组失败'
    } finally {
      isLoading.value = false
    }
  }

  // 总模块数
  const totalModules = computed(() => {
    return modules.value.length
  })

  return {
    modules,
    moduleGroups,
    isLoading,
    loadError,
    totalModules,
    fetchModules,
    fetchModuleGroups,
    moduleStatus,
    updateModuleStatus,
    getModuleStatus
  }
})
