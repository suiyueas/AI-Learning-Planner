// 用户 API
import { get, put, post } from './request'

/**
 * 获取当前用户信息
 */
export function getProfile() {
  return get('/user/profile')
}

/**
 * 更新用户资料
 */
export function updateProfile(data) {
  return put('/user/profile', data)
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return put('/user/password', data)
}

/**
 * 获取学习偏好设置
 */
export function getPreferences() {
  return get('/user/preferences')
}

/**
 * 更新学习偏好设置
 */
export function updatePreferences(data) {
  return put('/user/preferences', data)
}

/**
 * 获取用户统计数据
 */
export function getUserStats() {
  return get('/user/stats')
}

/**
 * 获取用户学习统计（已完成节数、总任务数、平均得分等）
 */
export function getLearningStats() {
  return get('/user/learning-stats')
}

/**
 * 获取用户成就列表
 */
export function getAchievements() {
  return get('/user/achievements')
}

/**
 * 上传头像
 */
export function uploadAvatar(formData) {
  return post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
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
export function getPathDetail(pathId) {
  return get(`/learning-path/${pathId}`)
}

/**
 * 路径自动调整
 */
export function autoAdjustPath(pathId) {
  return post('/learning-path/auto-adjust', { pathId })
}

/**
 * 重置学习路径
 */
export function resetPath(pathId) {
  return post('/learning-path/reset', { pathId })
}