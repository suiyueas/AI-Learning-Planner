/**
 * WebSocket 客户端 - 原生 WebSocket（无外部依赖）
 * 用于实时接收知识库状态变更推送
 */

class WebSocketClient {
  constructor() {
    this.ws = null
    this.isConnected = false
    this.listeners = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 2000
    this.reconnectTimer = null
  }

  /**
   * 连接 WebSocket 服务器
   */
  connect() {
    if (this.ws && this.isConnected) {
      return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const token = localStorage.getItem('token')
    const url = `${protocol}//${host}/ws/knowledge${token ? '?token=' + token : ''}`

    console.log('[WS] Connecting to:', url)

    try {
      this.ws = new WebSocket(url)

      this.ws.onopen = () => {
        this.isConnected = true
        this.reconnectAttempts = 0
        console.log('[WS] Connected')
        this._resubscribeAll()
      }

      this.ws.onclose = (event) => {
        this.isConnected = false
        console.log('[WS] Disconnected, code:', event.code)
        this._scheduleReconnect()
      }

      this.ws.onerror = (error) => {
        console.error('[WS] Error:', error)
        this.isConnected = false
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this._dispatch(data)
        } catch (e) {
          console.error('[WS] Failed to parse message:', e)
        }
      }
    } catch (e) {
      console.error('[WS] Connection failed:', e)
      this._scheduleReconnect()
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
      this.isConnected = false
    }
  }

  /**
   * 订阅知识库事件
   * @param {string} eventType - 事件类型（'doc_uploaded', 'doc_ready', 'doc_deleted', 'all'）
   * @param {function} callback - 回调函数
   * @returns {string} 订阅ID
   */
  subscribe(eventType, callback) {
    const id = `sub_${eventType}_${Date.now()}`

    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, new Map())
    }
    this.listeners.get(eventType).set(id, callback)

    return id
  }

  /**
   * 取消订阅
   * @param {string} eventType - 事件类型
   * @param {string} id - 订阅ID
   */
  unsubscribe(eventType, id) {
    if (this.listeners.has(eventType)) {
      this.listeners.get(eventType).delete(id)
    }
  }

  /**
   * 分发消息到对应的监听器
   */
  _dispatch(data) {
    // 分发到具体事件类型的监听器
    if (data.type && this.listeners.has(data.type)) {
      this.listeners.get(data.type).forEach((callback) => {
        try {
          callback(data)
        } catch (e) {
          console.error('[WS] Callback error:', e)
        }
      })
    }

    // 分发到 'all' 类型的监听器（接收所有事件）
    if (this.listeners.has('all')) {
      this.listeners.get('all').forEach((callback) => {
        try {
          callback(data)
        } catch (e) {
          console.error('[WS] Callback error:', e)
        }
      })
    }
  }

  /**
   * 重新订阅所有已有监听器
   */
  _resubscribeAll() {
    // 无需重新订阅，因为 native WebSocket 不需要服务端订阅
  }

  /**
   * 尝试重连
   */
  _scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('[WS] Max reconnect attempts reached, giving up')
      return
    }

    this.reconnectAttempts++
    const delay = this.reconnectDelay * Math.min(this.reconnectAttempts, 3)
    console.log(`[WS] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`)

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  /**
   * 检查连接状态
   */
  get connected() {
    return this.isConnected
  }
}

// 导出单例
export const wsClient = new WebSocketClient()

export default wsClient
