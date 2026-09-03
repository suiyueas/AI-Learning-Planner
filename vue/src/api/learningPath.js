// 学习路径 API
import { get, post, put, del } from './request'

// ==================== 路径管理 ====================

/**
 * 获取当前活跃路径的统一数据（包含进度、下一节点、路径状态）
 */
export function getActivePath() {
  return get('/learning-path/active')
}

/**
 * 获取当前用户激活的学习路径
 */
export function getCurrentPath() {
  return get('/learning-path/current')
}

/**
 * 获取指定路径详情
 */
export function getPathDetail(id) {
  return get(`/learning-path/${id}`)
}

/**
 * 获取用户所有学习路径列表
 */
export function getPathList() {
  return get('/learning-path/list')
}

/**
 * 切换当前激活的学习路径
 */
export function switchPath(id) {
  return post(`/learning-path/switch/${id}`)
}

/**
 * AI 优化学习路径
 */
export function optimizePath(id) {
  return post(`/learning-path/optimize/${id}`)
}

/**
 * 重置学习路径
 */
export function resetPath(id) {
  return post(`/learning-path/reset/${id}`)
}

/**
 * 生成新的学习路径
 */
export function generatePath(data) {
  return post('/learning-path/generate', {
    goal: data.goal,
    targetField: data.targetField || '',
    duration: data.duration || undefined,
    durationWeeks: data.durationWeeks || undefined,
    source: data.source || 'manual'
  })
}

// ==================== 进度与任务（向后兼容） ====================

/**
 * 获取学习路径进度（含阶段/周/任务详情）
 */
export function getPathProgress(pathId) {
  return get(`/learning-path/${pathId}/progress`)
}

/**
 * 标记任务完成
 */
export function completeTask(pathId, taskId) {
  return post(`/learning-path/${pathId}/task/${taskId}/complete`, {})
}

/**
 * 更新任务学习进度
 */
export function updateTaskProgress(pathId, taskId, data) {
  return post(`/learning-path/${pathId}/task/${taskId}/progress`, data)
}

/**
 * 为存量空路径补生成学习大纲（章节-周-任务）
 */
export function generatePathOutline(pathId) {
  return post(`/learning-path/${pathId}/outline/generate`)
}

/**
 * 获取学习路径（别名，保持兼容）
 */
export function getLearningPath(pathId) {
  return get(`/learning-path/${pathId}`)
}

/**
 * 获取用户所有路径（按userId）
 */
export function getUserPaths(userId) {
  return get(`/learning-path/user/${userId}`)
}

/**
 * 创建学习路径
 */
export function createLearningPath(data) {
  return post('/learning-path', data)
}

/**
 * 更新学习路径
 */
export function updateLearningPath(pathId, data) {
  return post(`/learning-path/${pathId}`, data)
}

/**
 * 删除学习路径
 */
export function deletePath(id) {
  return del(`/learning-path/${id}`)
}

// ==================== 每日任务 ====================

/**
 * 获取今日任务
 */
export function getTodayTasks(pathId) {
  return get(`/learning-path/${pathId}/daily-tasks/today`)
}

/**
 * 获取指定日期的任务
 */
export function getDailyTasks(pathId, date) {
  const params = date ? { date } : {}
  return get(`/learning-path/${pathId}/daily-tasks`, params)
}

/**
 * 获取本周预览
 */
export function getWeekPreview(pathId, date) {
  const params = date ? { date } : {}
  return get(`/learning-path/${pathId}/daily-tasks/week`, params)
}

/**
 * 更新任务状态
 */
export function updateDailyTaskStatus(pathId, taskId, status) {
  return put(`/learning-path/${pathId}/daily-tasks/${taskId}`, { status })
}

/**
 * 重新生成每日任务
 */
export function regenerateDailyTasks(pathId) {
  return post(`/learning-path/${pathId}/daily-tasks/regenerate`, {})
}