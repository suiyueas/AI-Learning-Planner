<template>
  <div class="notification-system">
    <!-- 通知触发器 -->
    <div class="notification-trigger" @click="togglePanel">
      <span class="notification-icon">🔔</span>
      <span
        v-if="notificationStore.unreadCount > 0"
        class="notification-badge"
        :class="notificationStore.badgeColor"
      >
        {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
      </span>
    </div>

    <!-- 智能通知中心面板 -->
    <div v-if="showPanel" class="notification-panel" @click.stop>
      <div class="panel-header">
        <h3 class="panel-title">🔔 智能通知中心</h3>
        <div class="panel-actions">
          <el-button size="small" @click="handleMarkAllAsRead">全部已读</el-button>
          <el-button size="small" @click="handleClear">清空</el-button>
        </div>
      </div>

      <div class="panel-filter">
        <button
          v-for="f in filters"
          :key="f.id"
          class="filter-btn"
          :class="{ active: activeFilter === f.id }"
          @click="activeFilter = f.id"
        >
          <span class="filter-dot" :class="f.color"></span>{{ f.label }}
          <span class="filter-count">{{ f.count }}</span>
        </button>
      </div>

      <div class="notification-list">
        <template v-for="group in groupedNotifications" :key="group.priority">
          <div class="group-title" :class="group.priority">
            {{ group.label }}
          </div>
          <div
            v-for="notification in group.items"
            :key="notification.id"
            class="notification-item"
            :class="[notification.priority, { unread: !notification.isRead, handled: notification.isHandled }]"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon-wrapper">
              <span class="notification-type-icon">{{ getPriorityIcon(notification.priority) }}</span>
            </div>

            <div class="notification-content">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-message">{{ notification.content }}</div>
              <div class="notification-meta">
                <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
                <span v-if="notification.isHandled" class="handled-tag">已处理</span>
                <span v-if="!notification.isRead" class="unread-dot"></span>
              </div>

              <!-- 快捷操作按钮 -->
              <div class="notification-actions" @click.stop>
                <template v-if="notification.priority === 'EMERGENCY'">
                  <button class="action-btn primary" @click="handleQuickAction(notification, 'ADJUST_PLAN')">
                    立即调整计划 →
                  </button>
                  <button class="action-btn" @click="handleQuickAction(notification, 'VIEW_DETAIL')">查看详情</button>
                </template>
                <template v-else-if="notification.priority === 'WARNING'">
                  <button class="action-btn primary" @click="handleQuickAction(notification, 'START_REVIEW')">
                    开始复习 →
                  </button>
                  <button class="action-btn" @click="handleQuickAction(notification, 'VIEW_WEAKNESS')">查看薄弱点</button>
                </template>
                <template v-else>
                  <button class="action-btn" @click="handleQuickAction(notification, 'VIEW_DETAIL')">查看详情</button>
                </template>
              </div>
            </div>

            <button class="delete-btn" @click.stop="handleDelete(notification.id)">✕</button>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-if="notifications.length === 0" class="empty-notifications">
          <span class="empty-icon">🔔</span>
          <p>暂无通知</p>
          <p class="empty-sub">学习状态良好时，这里会很安静</p>
        </div>
        <div v-else-if="filteredNotifications.length === 0" class="empty-notifications">
          <span class="empty-icon">🔍</span>
          <p>没有匹配的通知</p>
        </div>
      </div>
    </div>

    <!-- Toast通知 -->
    <div class="toast-container">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast"
        :class="toast.type"
      >
        <span class="toast-icon">{{ getTypeIcon(toast.type) }}</span>
        <span class="toast-message">{{ toast.message }}</span>
        <button class="toast-close" @click="removeToast(toast.id)">×</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notificationStore'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const notificationStore = useNotificationStore()
const authStore = useAuthStore()

// 响应式数据定义
const showPanel = ref(false)
const toasts = ref([])
const activeFilter = ref('all')

// 过滤标签
const filters = computed(() => [
  { id: 'all', label: '全部', count: notifications.value.length, color: 'cyan' },
  { id: 'EMERGENCY', label: '紧急', count: notifications.value.filter(n => n.priority === 'EMERGENCY').length, color: 'red' },
  { id: 'WARNING', label: '预警', count: notifications.value.filter(n => n.priority === 'WARNING').length, color: 'yellow' },
  { id: 'INFO', label: '普通', count: notifications.value.filter(n => n.priority === 'INFO').length, color: 'blue' },
])

const notifications = computed(() => notificationStore.notifications)
const filteredNotifications = computed(() => {
  if (activeFilter.value === 'all') return notifications.value
  return notifications.value.filter(n => n.priority === activeFilter.value)
})

// 按优先级分组展示（P0 紧急 → P1 预警 → P2 普通）
const groupedNotifications = computed(() => {
  const groups = [
    { priority: 'EMERGENCY', label: '🔴 紧急干预', items: [] },
    { priority: 'WARNING', label: '🟡 预警提醒', items: [] },
    { priority: 'INFO', label: '🔵 普通通知', items: [] },
  ]
  filteredNotifications.value.forEach(n => {
    const group = groups.find(g => g.priority === n.priority) || groups[2]
    group.items.push(n)
  })
  return groups.filter(g => g.items.length > 0)
})

// 获取优先级图标
const getPriorityIcon = (priority) => {
  const icons = { EMERGENCY: '🔴', WARNING: '🟡', INFO: '🔵' }
  return icons[priority] || '🔵'
}

// 获取类型对应的图标（Toast）
const getTypeIcon = (type) => {
  const icons = {
    success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️',
    task: '📋', chat: '💬', knowledge: '📚', agent: '🤖'
  }
  return icons[type] || 'ℹ️'
}

// 格式化时间显示
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 86400000 * 7) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString('zh-CN')
}

// 切换面板
const togglePanel = async () => {
  if (!authStore.isAuthenticated) {
    router.push('/login')
    return
  }
  showPanel.value = !showPanel.value
  if (showPanel.value) {
    await notificationStore.fetchNotifications()
  }
}

// 点击通知（普通通知点击即标记已读；干预通知保持未读直到执行操作）
const handleNotificationClick = (notification) => {
  if (notification.priority === 'INFO' || !notification.actionType) {
    notificationStore.markAsRead(notification.id)
  }
}

// 快捷操作
const handleQuickAction = async (notification, actionType) => {
  const action = notification.actionType || actionType
  // 解析 actionData（JSON 字符串）
  let actionData = {}
  try {
    actionData = notification.actionData ? JSON.parse(notification.actionData) : {}
  } catch (e) {
    actionData = {}
  }

  switch (action) {
    case 'ADJUST_PLAN':
      // 跳转动态规划页面，自动生成补救任务
      await notificationStore.markAsHandled(notification.id)
      showPanel.value = false
      router.push({
        path: '/capability/planning',
        query: actionData.pathId ? { pathId: actionData.pathId, fromNotification: notification.id } : { fromNotification: notification.id }
      })
      break
    case 'START_REVIEW':
      // 跳转知识库对应知识点
      await notificationStore.markAsHandled(notification.id)
      showPanel.value = false
      router.push({
        path: '/knowledge',
        query: actionData.subject ? { keyword: actionData.subject, fromNotification: notification.id } : { fromNotification: notification.id }
      })
      break
    case 'VIEW_WEAKNESS':
      // 跳转智能诊断查看薄弱点
      notificationStore.markAsRead(notification.id)
      showPanel.value = false
      router.push({
        path: '/capability/diagnosis',
        query: actionData.subject ? { subject: actionData.subject } : {}
      })
      break
    case 'VIEW_DETAIL':
    default:
      // 详情入口已统一由通知中心承载：标记已读即可，无需跳转
      notificationStore.markAsRead(notification.id)
      break
  }
}

// 全部已读
const handleMarkAllAsRead = () => {
  notificationStore.markAllAsRead()
}

// 清空通知
const handleClear = () => {
  notificationStore.clearAll()
}

// 删除单条通知
const handleDelete = (notificationId) => {
  notificationStore.remove(notificationId)
}

// 添加Toast通知
const addToast = (toast) => {
  const newToast = {
    id: 'toast_' + Date.now(),
    duration: 3000,
    ...toast
  }
  toasts.value.push(newToast)
  setTimeout(() => {
    removeToast(newToast.id)
  }, newToast.duration)
}

// 移除Toast
const removeToast = (toastId) => {
  const index = toasts.value.findIndex(t => t.id === toastId)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

// 点击外部关闭面板
const handleClickOutside = (e) => {
  if (!e.target.closest('.notification-system')) {
    showPanel.value = false
  }
}

// 登录状态变化：登录后拉取通知并启动轮询，退出后重置
watch(() => authStore.isAuthenticated, (isAuth) => {
  if (isAuth) {
    notificationStore.fetchNotifications()
    notificationStore.startPolling()
  } else {
    notificationStore.reset()
  }
})

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (authStore.isAuthenticated) {
    notificationStore.fetchNotifications()
    notificationStore.startPolling()
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  notificationStore.stopPolling()
})

// 暴露方法给父组件
defineExpose({
  addToast
})
</script>

<style lang="scss" scoped>
.notification-system {
  position: relative;
}

.notification-trigger {
  position: relative;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(0, 229, 255, 0.08);
  }

  .notification-icon {
    font-size: 1.2rem;
  }

  // 角标：存在 P0 未读时红色，仅 P1/P2 未读时蓝色
  .notification-badge {
    position: absolute;
    top: 2px;
    right: 2px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    color: white;
    font-size: 0.7rem;
    font-weight: 600;
    border-radius: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 0 8px rgba(0, 0, 0, 0.3);

    &.red {
      background: linear-gradient(135deg, #ef4444, #f97316);
      box-shadow: 0 0 10px rgba(239, 68, 68, 0.5);
      animation: badgePulse 2s ease-in-out infinite;
    }

    &.blue {
      background: linear-gradient(135deg, #3b82f6, #0ea5e9);
    }
  }
}

@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 6px rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 14px rgba(239, 68, 68, 0.8); }
}

// 通知面板
.notification-panel {
  position: absolute;
  top: 100%;
  right: 0;
  width: 400px;
  max-height: 520px;
  background: rgba(14, 17, 33, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-ctrl);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  overflow: hidden;
  margin-top: 8px;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-subtle);

  .panel-title {
    font-size: 1.05rem;
    font-weight: 600;
    color: var(--text-primary);
  }

  .panel-actions {
    display: flex;
    gap: 8px;
  }
}

// 分类筛选
.panel-filter {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-subtle);
  flex-wrap: wrap;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 14px;
  font-size: 0.75rem;
  color: #c0c0e0;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    border-color: #00f5d4;
    color: #00f5d4;
    background: rgba(0, 245, 212, 0.08);
  }
}

.filter-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  &.cyan { background: #00f5d4; }
  &.red { background: #ef4444; }
  &.yellow { background: #f59e0b; }
  &.blue { background: #3b82f6; }
}

.filter-count {
  font-size: 0.65rem;
  padding: 0 5px;
  background: rgba(100, 100, 180, 0.12);
  border-radius: 8px;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
  flex: 1;
}

// 分组标题
.group-title {
  padding: 10px 20px 6px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #9090b8;
  background: rgba(10, 10, 26, 0.4);
  position: sticky;
  top: 0;
  z-index: 1;
  backdrop-filter: blur(8px);

  &.EMERGENCY { color: #ef4444; }
  &.WARNING { color: #f59e0b; }
  &.INFO { color: #3b82f6; }
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: all 0.25s ease;
  border-left: 3px solid transparent;

  // 优先级高亮：P0 红色边框、P1 黄色边框、P2 蓝色
  &.EMERGENCY {
    border-left-color: #ef4444;
    background: linear-gradient(90deg, rgba(239, 68, 68, 0.06), transparent 70%);

    &:hover { background: linear-gradient(90deg, rgba(239, 68, 68, 0.1), transparent 70%); }
  }

  &.WARNING {
    border-left-color: #f59e0b;
    background: linear-gradient(90deg, rgba(245, 158, 11, 0.04), transparent 70%);

    &:hover { background: linear-gradient(90deg, rgba(245, 158, 11, 0.08), transparent 70%); }
  }

  &.INFO {
    border-left-color: #3b82f6;
  }

  &:hover {
    background: rgba(0, 229, 255, 0.04);
  }

  &:last-child {
    border-bottom: none;
  }

  &.unread {
    .notification-title {
      font-weight: 700;
      color: #ffffff;
    }
  }

  &.handled {
    opacity: 0.6;

    .notification-title {
      color: var(--text-sub);
    }
  }
}

.notification-icon-wrapper {
  flex-shrink: 0;
  padding-top: 2px;

  .notification-type-icon {
    font-size: 1.1rem;
  }
}

.notification-content {
  flex: 1;
  min-width: 0;

  .notification-title {
    font-size: 0.9rem;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .notification-message {
    font-size: 0.82rem;
    color: #c0c8e0;
    line-height: 1.5;
    margin-bottom: 6px;
    white-space: pre-line;
    word-break: break-word;
  }

  .notification-meta {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .notification-time {
    font-size: 0.72rem;
    color: var(--text-sub);
  }

  .unread-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #00f5d4;
    box-shadow: 0 0 6px rgba(0, 245, 212, 0.6);
  }

  .handled-tag {
    font-size: 0.65rem;
    padding: 1px 8px;
    border-radius: 8px;
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
    border: 1px solid rgba(16, 185, 129, 0.15);
  }
}

// 快捷操作按钮
.notification-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 5px 12px;
  background: rgba(100, 100, 180, 0.08);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 8px;
  color: #c0c0e0;
  font-size: 0.72rem;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba(0, 245, 212, 0.3);
    color: #00f5d4;
  }

  &.primary {
    background: linear-gradient(135deg, rgba(0, 245, 212, 0.12), rgba(0, 85, 255, 0.1));
    border-color: rgba(0, 245, 212, 0.25);
    color: #00f5d4;
    font-weight: 600;

    &:hover {
      background: linear-gradient(135deg, rgba(0, 245, 212, 0.2), rgba(0, 85, 255, 0.16));
      box-shadow: 0 0 12px rgba(0, 245, 212, 0.15);
    }
  }
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
  align-self: flex-start;
  background: none;
  border: none;
  color: var(--text-sub);
  font-size: 0.85rem;
  cursor: pointer;
  padding: 2px 4px;

  &:hover {
    color: #ef4444;
  }
}

.notification-item:hover .delete-btn {
  opacity: 1;
}

// 空状态
.empty-notifications {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  color: var(--text-sub);

  .empty-icon {
    font-size: 2rem;
    margin-bottom: 8px;
  }

  p {
    font-size: 0.9rem;
    margin: 0;
  }

  .empty-sub {
    font-size: 0.75rem;
    color: #606090;
    margin-top: 6px;
  }
}

// Toast通知
.toast-container {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(14, 17, 33, 0.97);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border-ctrl);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  min-width: 300px;
  max-width: 400px;
  animation: slideIn 0.3s ease;

  &.success { border-left: 4px solid var(--accent-emerald); }
  &.error { border-left: 4px solid var(--accent-red); }
  &.warning { border-left: 4px solid var(--accent-amber); }
  &.info { border-left: 4px solid var(--accent-blue); }
}

.toast-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
}

.toast-message {
  flex: 1;
  font-size: 0.9rem;
  color: var(--text-primary);
}

.toast-close {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: var(--text-sub);
  cursor: pointer;
  padding: 0;
  line-height: 1;

  &:hover {
    color: var(--text-primary);
  }
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@media (max-width: 640px) {
  .notification-panel {
    width: calc(100vw - 24px);
    right: -70px;
  }
}
</style>
