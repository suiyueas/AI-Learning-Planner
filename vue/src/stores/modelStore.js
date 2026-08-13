// 模型管理 Store
// 支持前端动态切换模型

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentModel, getAvailableModels, switchModel } from '@/api/modelApi'

export const useModelStore = defineStore('model', () => {
  // 当前模型信息
  const currentModel = ref({
    provider: 'qwen',
    displayName: 'Qwen-Max（阿里云）',
    modelName: 'qwen-max',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode'
  })

  // 可用模型列表
  const availableModels = ref({
    qwen: { provider: 'qwen', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode', modelName: 'qwen-max' },
    deepseek: { provider: 'deepseek', baseUrl: 'https://api.deepseek.com', modelName: 'deepseek-chat' },
    xiaomi: { provider: 'xiaomi', baseUrl: 'https://api.xiaomimimo.com/v1', modelName: 'xiaomi/mimo-v2-pro' }
  })

  // 加载状态
  const loading = ref(false)

  // 计算属性
  const currentProvider = computed(() => currentModel.value.provider)
  const currentDisplayName = computed(() => currentModel.value.displayName)

  // 模型显示名称映射
  const modelDisplayNames = {
    qwen: 'Qwen-Max（阿里云）',
    deepseek: 'DeepSeek-V4-Flash',
    xiaomi: '小米 MiMo-V2.5-Pro'
  }

  /**
   * 加载当前模型信息
   */
  async function loadCurrentModel() {
    loading.value = true
    try {
      const data = await getCurrentModel()
      currentModel.value = data
    } catch (error) {
      console.error('加载当前模型失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载可用模型列表
   */
  async function loadAvailableModels() {
    try {
      const data = await getAvailableModels()
      if (data.models) {
        availableModels.value = data.models
      }
      if (data.current) {
        currentModel.value.provider = data.current
        currentModel.value.displayName = modelDisplayNames[data.current] || data.current
      }
    } catch (error) {
      console.error('加载可用模型失败:', error)
    }
  }

  /**
   * 切换模型
   * @param {string} provider - 模型提供者
   * @param {string} apiKey - API Key（可选）
   */
  async function switchToModel(provider, apiKey = '') {
    loading.value = true
    try {
      const result = await switchModel(provider, apiKey)
      // 确保 result 存在
      if (result && result.success) {
        currentModel.value = {
          provider: result.provider,
          displayName: result.displayName,
          modelName: result.modelName,
          baseUrl: availableModels.value[provider]?.baseUrl || ''
        }
        return { success: true, message: result.message }
      }
      return { success: false, message: result?.message || '切换失败' }
    } catch (error) {
      console.error('切换模型失败:', error)
      return { success: false, message: error.message || '切换失败' }
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取模型显示名称
   */
  function getModelDisplayName(provider) {
    return modelDisplayNames[provider] || provider
  }

  return {
    currentModel,
    availableModels,
    loading,
    currentProvider,
    currentDisplayName,
    loadCurrentModel,
    loadAvailableModels,
    switchToModel,
    getModelDisplayName
  }
})
