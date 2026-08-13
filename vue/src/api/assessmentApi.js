import { get, post, del } from './request'

/**
 * 获取测评题目
 * @param {string} subject 科目（支持自定义名称）
 * @param {number} count 题目数量
 * @param {string} difficulty 难度 easy/medium/hard
 */
export function getQuestions(subject = 'Python', count = 10, difficulty = 'medium') {
  return get('/assessment/questions', { subject, count, difficulty })
}

/**
 * 重新生成题目
 * @param {string} subject 科目
 * @param {number} count 题目数量
 * @param {string} difficulty 难度 easy/medium/hard
 */
export function regenerateQuestions(subject = 'Python', count = 10, difficulty = 'medium') {
  return post('/assessment/regenerate', null, { params: { subject, count, difficulty } })
}

/**
 * 提交答案
 * @param {string} subject 科目
 * @param {object} answers 题目ID -> 答案索引
 * @param {array} questions 完整题目列表
 * @param {string} difficulty 难度
 */
export function submitAnswers(subject, answers, questions, difficulty = 'medium') {
  const questionIds = questions.map(q => q.id)
  return post('/assessment/submit', { subject, answers, questionIds, difficulty })
}

/**
 * 获取支持科目列表
 */
export function getSubjects() {
  return get('/assessment/subjects')
}

/**
 * 获取历史测评记录（分页）
 * @param {number} page 页码
 * @param {number} size 每页数量
 * @param {string} subject 科目筛选（可选）
 */
export function getHistory(page = 1, size = 10, subject = null) {
  const params = { page, size }
  if (subject) params.subject = subject
  return get('/assessment/history', params)
}

/**
 * 获取历史测评详情
 * @param {number} id 记录ID
 */
export function getHistoryDetail(id) {
  return get(`/assessment/history/${id}`)
}

/**
 * 获取用户历史测评科目列表
 */
export function getHistorySubjects() {
  return get('/assessment/history/subjects')
}

/**
 * 删除历史测评记录
 * @param {number} id 记录ID
 */
export function deleteHistory(id) {
  return del(`/assessment/history/${id}`)
}

/**
 * 获取自适应难度配置（基于用户历史测评表现计算推荐难度/题数）
 * @param {string} subject 科目
 * @returns {Promise} { difficulty, recommendedCount, estimatedTime, historicalAccuracy, totalAttempts }
 */
export function getAdaptiveConfig(subject) {
  return get('/assessment/quiz/adaptive-config', { subject })
}

/**
 * 生成题目（支持自适应难度）
 * @param {string} subject 科目
 * @param {boolean} adaptive 是否启用自适应难度
 * @param {string} difficulty 指定难度（adaptive=false 时生效）
 * @param {number} count 题目数量
 */
export function generateAdaptiveQuiz(subject, adaptive = true, difficulty = null, count = 5) {
  const params = { subject, adaptive, count }
  if (difficulty) params.difficulty = difficulty
  return get('/assessment/quiz/generate', params)
}