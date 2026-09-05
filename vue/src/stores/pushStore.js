// ============================================
// 推送通知 Store
// 接收后端 WebSocket 推送的学习提醒/鼓励/调整建议
// ============================================

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

export const usePushStore = defineStore('push', () => {
  const router = useRouter()
  const notifications = ref([])
  const ws = ref(null)

  /**
   * 连接 WebSocket 推送通道
   * @param {string} userId
   */
  function connect(userId) {
    if (ws.value) return

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.host
      ws.value = new WebSocket(`${protocol}//${host}/ws/push/${userId}`)

      ws.value.onmessage = (event) => {
        try {
          const push = JSON.parse(event.data)
          notifications.value.unshift(push)
          handlePush(push)
        } catch (e) {
          console.error('解析推送消息失败:', e)
        }
      }

      ws.value.onclose = () => {
        // 自动重连
        setTimeout(() => {
          ws.value = null
          connect(userId)
        }, 5000)
      }

      ws.value.onerror = (err) => {
        console.warn('WebSocket 连接错误:', err)
      }
    } catch (e) {
      console.warn('WebSocket 初始化失败:', e.message)
    }
  }

  /**
   * 根据推送类型自动触发 UI 行为
   */
  function handlePush(push) {
    switch (push.type) {
      case 'ENCOURAGE':
        // 鼓励弹窗 - 可以用 ElMessage 或自定义弹窗
        console.log('[Push] 鼓励:', push.message)
        break

      case 'REMIND':
        // 学习提醒
        console.log('[Push] 提醒:', push.message)
        break

      case 'ADJUST':
        // 路径调整建议
        console.log('[Push] 调整建议:', push.message)
        break

      case 'CHALLENGE':
        // 挑战 - 打开习题面板
        console.log('[Push] 挑战:', push.message)
        if (push.action?.route) {
          router.push(push.action.route)
        }
        break

      default:
        console.log('[Push] 通知:', push.message)
    }
  }

  /**
   * 断开 WebSocket 连接
   */
  function disconnect() {
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
  }

  /**
   * 清除所有通知
   */
  function clearAll() {
    notifications.value = []
  }

  return {
    notifications,
    connect,
    disconnect,
    clearAll
  }
})
