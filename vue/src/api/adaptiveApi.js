// 自适应引擎API
import { get, post } from './request'

/**
 * 获取自适应引擎状态（策略/调整次数/效率提升/掌握率/归因）
 * @returns {Promise} { status, currentStrategy, totalAdjustments, efficiencyImprovement, knowledgeMastery, lastAdjustmentAt, attribution }
 */
export const getAdaptiveStatus = () => {
  return get('/adaptive/status')
}

/**
 * 获取自适应调整历史（分页 + 类型筛选）
 * @param {object} params { page, size, type }
 * @returns {Promise} { content, totalElements, page, size, totalPages }
 */
export const getAdaptiveAdjustments = (params = {}) => {
  return get('/adaptive/adjustments', params)
}

/**
 * 获取个性化推荐列表
 * @returns {Promise} { content, totalElements }
 */
export const getAdaptiveRecommendations = () => {
  return get('/adaptive/recommendations')
}

/**
 * 标记推荐为已点击
 * @param {string} id 推荐ID
 */
export const clickRecommendation = (id) => {
  return post(`/adaptive/recommendations/${id}/click`)
}

/**
 * 标记推荐为已消费（已学习）
 * @param {string} id 推荐ID
 */
export const consumeRecommendation = (id) => {
  return post(`/adaptive/recommendations/${id}/consume`)
}

export default {
  getAdaptiveStatus,
  getAdaptiveAdjustments,
  getAdaptiveRecommendations,
  clickRecommendation,
  consumeRecommendation
}
