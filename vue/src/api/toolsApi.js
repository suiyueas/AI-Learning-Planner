// 工具相关API
import { get, post, del } from './request'

/**
 * 获取工具列表
 * @returns {Promise} 工具列表
 */
export const getTools = () => {
  return get('/tools')
}

/**
 * 执行工具
 * @param {string} toolId - 工具ID
 * @param {object} params - 执行参数
 * @returns {Promise} 执行结果
 */
export const executeTool = (toolId, params = {}) => {
  return post(`/tools/${toolId}/execute`, params)
}

/**
 * 获取工具执行历史（分页）
 * @param {number} page - 页码
 * @param {number} size - 每页条数
 * @returns {Promise} 执行历史
 */
export const getToolExecutionHistory = (page = 0, size = 20) => {
  return get('/tools/executions/history', { page, size })
}

/**
 * 删除单条工具执行记录（物理删除，不可恢复）
 * @param {number|string} id - 执行记录ID
 * @returns {Promise}
 */
export const deleteToolExecution = (id) => {
  return del(`/tools/executions/${id}`)
}

/**
 * 清空当前用户的工具执行记录（物理删除）
 * @returns {Promise}
 */
export const clearToolExecutions = () => {
  return del('/tools/executions')
}

/**
 * 获取工具详情
 * @param {string} toolId - 工具ID
 * @returns {Promise} 工具详情
 */
export const getToolDetail = (toolId) => {
  return get(`/tools/${toolId}`)
}

/**
 * 获取工具分类
 * @returns {Promise} 工具分类列表
 */
export const getToolCategories = () => {
  return get('/tools/categories')
}

/**
 * 搜索工具
 * @param {string} keyword - 搜索关键词
 * @returns {Promise} 搜索结果
 */
export const searchTools = (keyword) => {
  return get('/tools/search', { keyword })
}

/**
 * 获取工具使用统计（全局统计）
 * @returns {Promise} 使用统计
 */
export const getToolStats = () => {
  return get('/tools/stats')
}

/**
 * 获取当前用户的工具使用统计（按用户隔离）
 * @returns {Promise} 用户工具使用统计
 */
export const getMyToolStats = () => {
  return get('/tools/my-stats')
}

/**
 * 获取单个工具详细统计
 * @param {string} toolId - 工具ID
 * @returns {Promise} 工具统计详情
 */
export const getToolStatsDetail = (toolId) => {
  return get(`/tools/${toolId}/stats`)
}

/**
 * 获取当前用户单个工具的调用次数（按用户隔离）
 * @param {string} toolId - 工具ID
 * @returns {Promise} 用户工具调用次数
 */
export const getMyToolUsageCount = (toolId) => {
  return get(`/tools/${toolId}/my-stats`)
}

/**
 * 记录一次工具调用（每次工具执行成功后调用）
 * @param {string} toolId - 工具ID
 * @returns {Promise} 记录结果
 */
export const recordToolCall = (toolId) => {
  return post(`/tools/${toolId}/record`)
}

/**
 * 聚合检索：同时搜索内部知识库、外部联网资源、知识图谱
 * @param {object} params - 检索参数
 * @param {string} params.query - 搜索关键词
 * @param {boolean} params.searchInternal - 是否搜索内部知识库，默认true
 * @param {boolean} params.searchWeb - 是否联网搜索，默认true
 * @param {boolean} params.searchGraph - 是否查询知识图谱，默认false
 * @param {number} params.limit - 每类结果上限，默认5
 * @returns {Promise} 聚合检索结果
 */
export const aggregatedSearch = (params) => {
  return post('/tools/aggregated-search', params)
}

// 创建工具API对象
export const toolsAPI = {
  getTools,
  executeTool,
  getToolExecutionHistory,
  getToolDetail,
  getToolCategories,
  searchTools,
  getToolStats,
  getMyToolStats,
  getToolStatsDetail,
  getMyToolUsageCount,
  recordToolCall,
  aggregatedSearch
}

export default toolsAPI