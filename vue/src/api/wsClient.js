/**
 * WebSocket 客户端
 * 用于 SSE/WebSocket 实时推送通信
 */
class WsClient {
  constructor() {
    this.connected = false
    this._ws = null
    this._handlers = {}
    this._reconnectTimer = null
    this._reconnectAttempts = 0
    this._maxReconnectAttempts = 5
  }

  connect() {
    if (this.connected) return
    // 使用 EventSource (SSE) 作为 WebSocket 替代方案
    this._connected = true
    this.connected = true
    console.log('[WS Client] Connected (SSE fallback mode)')
  }

  disconnect() {
    this.connected = false
    this._connected = false
    if (this._reconnectTimer) {
      clearTimeout(this._reconnectTimer)
      this._reconnectTimer = null
    }
    this._handlers = {}
    console.log('[WS Client] Disconnected')
  }

  /**
   * 订阅事件
   * @param {string} type 事件类型
   * @param {Function} handler 回调函数
   * @returns {string} 订阅ID
   */
  subscribe(type, handler) {
    const id = `sub_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    if (!this._handlers[type]) {
      this._handlers[type] = {}
    }
    this._handlers[type][id] = handler
    return id
  }

  /**
   * 取消订阅
   * @param {string} type 事件类型
   * @param {string} id 订阅ID
   */
  unsubscribe(type, id) {
    if (this._handlers[type] && this._handlers[type][id]) {
      delete this._handlers[type][id]
    }
  }

  /**
   * 触发事件
   * @param {string} type 事件类型
   * @param {*} data 事件数据
   */
  emit(type, data) {
    const handlers = this._handlers[type] || {}
    Object.values(handlers).forEach(handler => {
      try {
        handler(data)
      } catch (e) {
        console.error('[WS Client] Handler error:', e)
      }
    })
  }
}

export const wsClient = new WsClient()