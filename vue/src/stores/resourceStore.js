// 资源状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getResources, getHotResources } from '@/api/resourceApi'

export const useResourceStore = defineStore('resource', () => {
  // 资源列表
  const resources = ref([])

  // 热门资源列表
  const hotResources = ref([])

  // 是否正在加载
  const isLoading = ref(false)

  // 加载错误
  const loadError = ref(null)

  // 获取资源列表
  const fetchResources = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getResources()
      if (res.success && res.data) {
        resources.value = res.data
      }
    } catch (error) {
      console.error('获取资源列表失败:', error)
      loadError.value = error.message || '获取资源列表失败'
    } finally {
      isLoading.value = false
    }
  }

  // 获取热门资源
  const fetchHotResources = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getHotResources()
      if (res.success && res.data) {
        hotResources.value = res.data
      }
    } catch (error) {
      console.error('获取热门资源失败:', error)
      loadError.value = error.message || '获取热门资源失败'
    } finally {
      isLoading.value = false
    }
  }

  return {
    resources,
    hotResources,
    isLoading,
    loadError,
    fetchResources,
    fetchHotResources
  }
})
