// 学情报告API
import { get } from './request'

/**
 * 生成学情报告（真实数据聚合）
 * @param {string} startDate 开始日期 YYYY-MM-DD
 * @param {string} endDate 结束日期 YYYY-MM-DD
 * @param {Array<string>} sections 报告板块 overview/matrix/recommendations
 * @param {string} style 风格 standard
 * @returns {Promise} { generatedAt, periodStart, periodEnd, overview, matrix, recommendations }
 */
export const generateReport = (startDate, endDate, sections = ['overview', 'matrix', 'recommendations'], style = 'standard') => {
  return get('/report/generate', { startDate, endDate, sections, style })
}

export default {
  generateReport
}
