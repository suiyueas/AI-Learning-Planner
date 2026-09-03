import { get, put, del } from './request'

export function getUserNotifications() {
  return get('/notifications')
}

export function getUnreadStats() {
  return get('/notifications/unread-stats')
}

export function markNotificationAsRead(id) {
  return put(`/notifications/${id}/read`)
}

export function markAllAsRead() {
  return put('/notifications/read-all')
}

export function markNotificationAsHandled(id) {
  return put(`/notifications/${id}/handled`)
}

export function deleteNotification(id) {
  return del(`/notifications/${id}`)
}

export function clearNotifications() {
  return del('/notifications')
}

export default {
  getUserNotifications,
  getUnreadStats,
  markNotificationAsRead,
  markAllAsRead,
  markNotificationAsHandled,
  deleteNotification,
  clearNotifications
}