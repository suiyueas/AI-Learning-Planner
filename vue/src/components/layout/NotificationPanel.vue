<template>
  <transition name="dropdown">
    <div v-if="visible" class="notification-panel" @click.stop>
      <div class="panel-header">
        <span class="panel-title">
          <Bell :size="16" />
          通知中心
          <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }} 未读</span>
        </span>
        <button
          v-if="unreadCount > 0"
          class="mark-all-btn"
          @click="handleMarkAllRead"
        >
          全部已读
        </button>
      </div>

      <div v-if="loading" class="panel-empty">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="notifications.length === 0" class="panel-empty">
        <Inbox :size="48" class="empty-icon" />
        <p>暂无通知</p>
      </div>

      <div v-else class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="[
            item.isRead ? 'read' : 'unread',
            item.priority?.toLowerCase()
          ]"
          @click="handleItemClick(item)"
        >
          <div class="item-badge" v-if="!item.isRead">
            <span class="badge-dot"></span>
          </div>
          <div class="item-content">
            <div class="item-header">
              <span class="item-title">{{ item.title }}</span>
              <span class="item-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <p v-if="item.content" class="item-content-text">{{ item.content }}</p>
            <div v-if="item.actionType" class="item-action">
              <span class="action-tag">{{ getActionLabel(item.actionType) }}</span>
            </div>
          </div>
          <button
            class="item-delete"
            @click.stop="handleDelete(item.id)"
          >
            <Trash2 :size="14" />
          </button>
        </div>
      </div>

      <div class="panel-footer" v-if="notifications.length > 0">
        <button class="clear-btn" @click="handleClearAll" @click.stop>
          清空全部
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Inbox, Trash2 } from 'lucide-vue-next'
import {
  getUserNotifications,
  getUnreadStats,
  markNotificationAsRead,
  markNotificationAsHandled,
  deleteNotification,
  clearNotifications,
  markAllAsRead
} from '@/api/notificationApi'
import { post } from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'refresh-stats', 'view-detail'])

const router = useRouter()
const notifications = ref([])
const loading = ref(false)
const unreadCount = ref(0)

const priorityLabels = {
  emergency: '紧急',
  warning: '预警',
  info: '普通'
}

const actionLabels = {
  ADJUST_PLAN: '立即调整计划',
  START_REVIEW: '开始复习',
  VIEW_WEAKNESS: '查看薄弱点',
  VIEW_DETAIL: '查看详情'
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await getUserNotifications()
    // 处理 ApiResponse 包装格式 {code, message, data} 或直接返回数组
    const data = Array.isArray(res) ? res : (res?.data || [])
    notifications.value = data
    
    // 如果没有通知，自动生成示例通知方便用户体验
    if (notifications.value.length === 0) {
      const seedRes = await post('/notifications/seed')
      const seedData = Array.isArray(seedRes) ? seedRes : (seedRes?.data || [])
      if (seedData.length > 0) {
        notifications.value = seedData
      }
    }
    await refreshUnreadStats()
  } catch (e) {
    console.error('加载通知失败', e)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const refreshUnreadStats = async () => {
  try {
    const res = await getUnreadStats()
    if (res) {
      unreadCount.value = res.total || 0
      emit('refresh-stats', unreadCount.value)
    }
  } catch (e) {
    console.error('刷新未读统计失败', e)
  }
}

const handleItemClick = async (item) => {
  if (!item.isRead) {
    await markNotificationAsRead(item.id)
    item.isRead = true
    await refreshUnreadStats()
  }
  
  const actionType = item.actionType
  const actionData = item.actionData ? JSON.parse(item.actionData) : null
  
  if (item.priority === 'EMERGENCY' && !item.isHandled) {
    try {
      await markNotificationAsHandled(item.id)
      item.isHandled = true
      ElMessage.success('已标记为处理完成')
    } catch (e) {
      console.error('标记处理失败', e)
    }
  }
  
  // 根据操作类型执行相应跳转
  if (actionType) {
    navigateByAction(actionType, actionData)
  } else if (actionData && actionData.route) {
    // 如果 actionData 中直接包含路由信息
    router.push(actionData.route)
  }
  
  emit('close')
}

const navigateByAction = (actionType, actionData) => {
  switch (actionType) {
    case 'ADJUST_PLAN':
      // 调整计划：如果有 pathId，打开调整对话框
      if (actionData?.pathId) {
        emit('view-detail', {
          type: 'adjust-plan',
          pathId: actionData.pathId,
          pathName: actionData.pathName
        })
      } else {
        // 如果没有 pathId，跳转到学习路径列表
        router.push('/learning-path')
      }
      break
      
    case 'START_REVIEW':
      // 开始复习：跳转到测评页面
      router.push('/assessment')
      break
      
    case 'VIEW_WEAKNESS':
      // 查看薄弱点：跳转到薄弱点分析页面
      router.push('/capability/weakness')
      break
      
    case 'VIEW_DETAIL':
      // 查看详情：根据类别跳转
      if (actionData?.category === 'PROGRESS') {
        router.push('/capability/progress')
      } else if (actionData?.category === 'KNOWLEDGE') {
        router.push('/knowledge')
      } else if (actionData?.pathId) {
        router.push(`/learning-path/${actionData.pathId}`)
      } else {
        // 默认跳转到首页仪表盘
        router.push('/home')
      }
      break
      
    default:
      // 未知类型，默认关闭即可
      break
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    await refreshUnreadStats()
    ElMessage.success('全部标记为已读')
  } catch (e) {
    console.error('标记全部已读失败', e)
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await deleteNotification(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
    await refreshUnreadStats()
    ElMessage.success('删除成功')
  } catch (e) {
    console.error('删除通知失败', e)
    ElMessage.error('删除失败')
  }
}

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有通知吗？此操作不可恢复', '确认清空', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await clearNotifications()
    notifications.value = []
    await refreshUnreadStats()
    ElMessage.success('已清空所有通知')
    emit('close')
  } catch {
  }
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const getActionLabel = (actionType) => {
  return actionLabels[actionType] || actionType
}

watch(() => props.visible, (val) => {
  if (val) {
    loadNotifications()
  }
})

const handleClickOutside = (e) => {
  if (props.visible && !e.target.closest('.notification-panel') && !e.target.closest('.topbar-btn')) {
    emit('close')
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.notification-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 360px;
  max-height: 500px;
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  z-index: 50;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;

  svg {
    color: $accent-primary;
  }
}

.unread-badge {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.mark-all-btn {
  background: none;
  border: none;
  color: $accent-primary;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover {
    background: rgba($accent-primary, 0.1);
  }
}

.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: $text-muted;

  .empty-icon {
    opacity: 0.4;
    margin-bottom: 12px;
  }

  p {
    margin: 0;
    font-size: 14px;
  }

  .loading-spinner {
    width: 24px;
    height: 24px;
    border: 2px solid rgba(0, 229, 255, 0.2);
    border-top-color: #00E5FF;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 12px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  max-height: 380px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  cursor: pointer;
  transition: all 0.2s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.02);
  }

  &.unread {
    background: rgba($accent-primary, 0.03);
  }

  &.emergency {
    background: rgba(239, 68, 68, 0.05);
    border-left: 3px solid #EF4444;
  }

  &.warning {
    border-left: 3px solid #F59E0B;
  }

  &.read {
    opacity: 0.7;
  }
}

.item-badge {
  padding-top: 8px;
  flex-shrink: 0;

  .badge-dot {
    display: block;
    width: 8px;
    height: 8px;
    background: #EF4444;
    border-radius: 50%;
  }
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.item-title {
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
  flex: 1;
}

.item-time {
  font-size: 11px;
  color: $text-muted;
  flex-shrink: 0;
}

.item-content-text {
  margin: 0 0 6px 0;
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.5;
}

.item-action {
  .action-tag {
    display: inline-block;
    font-size: 11px;
    padding: 2px 8px;
    background: rgba($accent-primary, 0.1);
    color: $accent-primary;
    border-radius: 4px;
  }
}

.item-delete {
  background: none;
  border: none;
  padding: 4px;
  color: $text-muted;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-top: -2px;

  &:hover {
    color: #EF4444;
    background: rgba(239, 68, 68, 0.1);
  }
}

.panel-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  text-align: right;
}

.clear-btn {
  background: none;
  border: none;
  color: #EF4444;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover {
    background: rgba(239, 68, 68, 0.1);
  }
}

@media (max-width: 768px) {
  .notification-panel {
    width: calc(100vw - 32px);
    right: -8px;
  }
}
</style>