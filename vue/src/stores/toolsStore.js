// 工具状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getTools,
  executeTool as executeToolApi,
  getToolExecutionHistory,
  getToolStats,
  getMyToolStats,
  recordToolCall as recordToolCallApi,
  aggregatedSearch as aggregatedSearchApi
} from '@/api/toolsApi'

export const useToolsStore = defineStore('tools', () => {
  // 工具列表
  const tools = ref([])

  // 是否正在加载
  const isLoading = ref(false)

  // 加载错误
  const loadError = ref(null)

  // 正在执行的工具ID
  const executingToolId = ref(null)

  // 执行历史
  const executionHistory = ref([])

  // 执行历史总数
  const historyTotal = ref(0)

  // 工具统计数据（前端界面统计：总数、可用数等）
  const toolStatsData = ref({ total: 0, available: 0 })

  // ===== 工具调用统计（基于 tool_call_stats 表的真实统计） =====
  // 格式: { toolId: { totalCalls, sessionCalls, lastCalled } }
  const toolStats = ref({})

  // localStorage 缓存键
  const STATS_CACHE_KEY = 'mcp_tool_stats_cache'

  // 从 localStorage 恢复缓存
  function loadStatsFromCache() {
    try {
      const cached = localStorage.getItem(STATS_CACHE_KEY)
      if (cached) {
        toolStats.value = JSON.parse(cached)
      }
    } catch { /* ignore */ }
  }

  // 缓存到 localStorage
  function saveStatsToCache() {
    try {
      localStorage.setItem(STATS_CACHE_KEY, JSON.stringify(toolStats.value))
    } catch { /* ignore */ }
  }

  // 初始化时恢复缓存
  loadStatsFromCache()

  // 计算属性：工具汇总统计（总工具数、可用数等）
  const toolSummary = computed(() => {
    if (toolStatsData.value.total > 0) {
      return toolStatsData.value
    }
    const total = tools.value.length
    const available = tools.value.filter(t => t.status === 'available').length
    return { total, available, executing: 0, unavailable: 0, configuring: 0, upgrading: 0 }
  })

  // ===== 工具调用统计方法 =====

  // 获取当前用户的工具调用统计（按用户隔离）
  const fetchToolStats = async () => {
    try {
      const res = await getMyToolStats()
      if (res.success && res.data) {
        toolStats.value = res.data
        saveStatsToCache()
      }
    } catch (error) {
      console.error('获取工具调用统计失败:', error)
    }
  }

  // 记录一次工具调用
  const recordToolCall = async (toolId) => {
    try {
      const res = await recordToolCallApi(toolId)
      if (res.success) {
        // 更新本地状态
        if (!toolStats.value[toolId]) {
          toolStats.value[toolId] = { totalCalls: 0, sessionCalls: 0, lastCalled: null }
        }
        toolStats.value[toolId].totalCalls = res.totalCalls
        toolStats.value[toolId].sessionCalls = (toolStats.value[toolId].sessionCalls || 0) + 1
        toolStats.value[toolId].lastCalled = new Date().toISOString()
        // 保存到 localStorage
        saveStatsToCache()
      }
    } catch (error) {
      console.error('记录工具调用失败:', error)
      // 降级：本地记录
      if (!toolStats.value[toolId]) {
        toolStats.value[toolId] = { totalCalls: 0, sessionCalls: 0, lastCalled: null }
      }
      toolStats.value[toolId].totalCalls = (toolStats.value[toolId].totalCalls || 0) + 1
      toolStats.value[toolId].sessionCalls = (toolStats.value[toolId].sessionCalls || 0) + 1
      toolStats.value[toolId].lastCalled = new Date().toISOString()
      saveStatsToCache()
    }
  }

  // 获取工具显示文本
  const getToolDisplay = (toolId) => {
    const stats = toolStats.value[toolId] || { totalCalls: 0, sessionCalls: 0 }
    const totalCalls = stats.totalCalls || 0
    const sessionCalls = stats.sessionCalls || 0
    let displayText = `已用 ${totalCalls} 次`
    if (sessionCalls > 0) {
      displayText += ` (本次 +${sessionCalls})`
    }
    return { totalCalls, sessionCalls, displayText }
  }

  // 获取工具列表
  const fetchTools = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getTools()
      if (res.success && res.data) {
        tools.value = res.data
      }
    } catch (error) {
      console.error('获取工具列表失败:', error)
      loadError.value = error.message || '获取工具列表失败'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 用户切换时重置：清空全部用户相关状态，
   * 已登录时按当前用户重新拉取工具列表与调用统计
   */
  const resetForUser = async (userId = null) => {
    tools.value = []
    executingToolId.value = null
    executionHistory.value = []
    historyTotal.value = 0
    toolStats.value = {}
    toolStatsData.value = { total: 0, available: 0 }
    loadError.value = null
    if (userId) {
      await Promise.all([fetchTools(), fetchToolStats()])
    }
  }

  // 获取工具统计（界面汇总）
  const fetchToolSummary = async () => {
    try {
      const res = await getToolStats()
      if (res.success && res.data) {
        // 计算汇总
        const statsArray = Object.values(res.data)
        const totalCalls = statsArray.reduce((sum, s) => sum + (s.totalCalls || 0), 0)
        toolStatsData.value = {
          total: tools.value.length,
          available: tools.value.filter(t => t.status === 'available').length,
          totalCalls
        }
      }
    } catch (error) {
      console.error('获取工具汇总统计失败:', error)
    }
  }

  // 获取执行历史
  const fetchExecutionHistory = async (page = 0, size = 20) => {
    try {
      const res = await getToolExecutionHistory(page, size)
      if (res.success && res.data) {
        executionHistory.value = res.data.records || []
        historyTotal.value = res.data.total || 0
      }
    } catch (error) {
      console.error('获取执行历史失败:', error)
    }
  }

  // 执行工具
  const executeTool = async (toolId, params = {}) => {
    const tool = tools.value.find(t => t.id === toolId)
    if (tool && tool.status !== 'available') {
      throw new Error('工具不可用')
    }

    // 设置执行状态
    executingToolId.value = toolId
    if (tool) tool.status = 'executing'

    try {
      const res = await executeToolApi(toolId, params)
      if (res.success) {
        if (tool) tool.status = 'available'
        executingToolId.value = null

        // 工具执行成功后记录调用统计
        await recordToolCall(toolId)

        return res.data
      } else {
        throw new Error(res.message || '工具执行失败')
      }
    } catch (error) {
      if (tool) tool.status = 'available'
      executingToolId.value = null
      throw error
    }
  }

  // 更新工具状态
  const updateToolStatus = (toolId, status) => {
    const tool = tools.value.find(t => t.id === toolId)
    if (tool) {
      tool.status = status
    }
  }

  // 聚合检索（同时搜索内部知识库、联网资源、知识图谱）
  const aggregatedSearch = async (params = {}) => {
    try {
      const res = await aggregatedSearchApi(params)
      if (res.success && res.data) {
        return res.data
      } else {
        throw new Error(res.message || '聚合检索失败')
      }
    } catch (error) {
      console.error('聚合检索失败:', error)
      throw error
    }
  }

  return {
    // 状态
    tools,
    executingToolId,
    executionHistory,
    historyTotal,
    isLoading,
    loadError,

    // 工具调用统计
    toolStats,

    // 计算属性
    toolSummary,

    // 方法
    fetchTools,
    fetchToolStats,
    fetchToolSummary,
    fetchExecutionHistory,
    executeTool,
    updateToolStatus,
    recordToolCall,
    getToolDisplay,
    aggregatedSearch,
    resetForUser
  }
})