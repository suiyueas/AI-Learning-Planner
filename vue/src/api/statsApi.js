// 统计数据API
import { get } from './request'

/**
 * 获取首页仪表板统计数据
 * @returns {Promise} 统计数据
 */
export const getDashboardStats = () => {
  return get('/stats/dashboard')
}

/**
 * 获取学习概览统计（连续天数/总学时/完成节点/平均分，真实数据聚合）
 * @returns {Promise}
 */
export const getProgressOverview = () => {
  return get('/stats/progress/overview')
}

/**
 * 获取学习曲线数据（按时间范围聚合学习时长与掌握度）
 * @param {string} range 7=近7天(按天) / 30=近30天(按天) / 90=近90天(按周) / all=全部(按周)
 * @returns {Promise} { labels, hours, mastery, range, byWeek }
 */
export const getProgressCurve = (range = '30') => {
  return get('/stats/progress/curve', { range })
}

/**
 * 获取能力矩阵（各知识域掌握度）
 * @returns {Promise} [{ name, mastery, level, pathId, recordCount }]
 */
export const getProgressCompetency = () => {
  return get('/stats/progress/competency')
}

/**
 * 获取 AI 学习洞察和建议
 * @returns {Promise} { message, actions: [{label, target}] }
 */
export const getAISuggestion = () => {
  return get('/stats/ai-suggestion')
}

/**
 * 获取学习记录列表（分页/状态筛选/关键词搜索/日期范围）
 * @param {object} params 查询参数
 * @returns {Promise} { records, total, page, size, totalPages }
 */
export const getProgressRecords = (params = {}) => {
  return get('/stats/progress/records', params)
}

export default {
  getDashboardStats,
  getProgressOverview,
  getProgressCurve,
  getProgressCompetency,
  getAISuggestion,
  getProgressRecords
}