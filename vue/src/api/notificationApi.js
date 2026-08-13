// 智能通知中心相关API
import { get, post, put, del } from './request'

/**
 * 获取当前用户通知列表（P0 > P1 > P2 排序）
 * @returns {Promise} 通知列表
 */
export const getNotifications = () => {
  return get('/notifications')
}

/**
 * 获取未读统计（total 总数 + emergency 未读紧急数）
 * @returns {Promise} { total, emergency }
 */
export const getUnreadStats = () => {
  return get('/notifications/unread-stats')
}

/**
 * 标记单条通知已读
 * @param {number} id - 通知ID
 * @returns {Promise} 是否成功
 */
export const markNotificationAsRead = (id) => {
  return put(`/notifications/${id}/read`)
}

/**
 * 全部标记已读
 * @returns {Promise}
 */
export const markAllNotificationsAsRead = () => {
  return put('/notifications/read-all')
}

/**
 * 标记干预类通知为已处理
 * @param {number} id - 通知ID
 * @returns {Promise} 是否成功
 */
export const markNotificationAsHandled = (id) => {
  return put(`/notifications/${id}/handled`)
}

/**
 * 删除单条通知
 * @param {number} id - 通知ID
 * @returns {Promise} 是否成功
 */
export const deleteNotification = (id) => {
  return del(`/notifications/${id}`)
}

/**
 * 清空当前用户所有通知
 * @returns {Promise}
 */
export const clearAllNotifications = () => {
  return del('/notifications')
}

/**
 * 手动触发干预扫描
 * @returns {Promise} { progress, knowledge, inactive }
 */
export const scanInterventions = () => {
  return post('/notifications/scan')
}

// 创建通知API对象
export const notificationAPI = {
  getNotifications,
  getUnreadStats,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  markNotificationAsHandled,
  deleteNotification,
  clearAllNotifications,
  scanInterventions
}

export default notificationAPI
