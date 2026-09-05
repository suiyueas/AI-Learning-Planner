// SSE客户端封装
// 参考yu-ai-agent的SSE实现
import { securityFilter } from '@/utils/securityUtils'

/**
 * SSE客户端类
 * 用于处理Server-Sent Events连接
 */
export class SSEClient {
  constructor(options = {}) {
    this.baseUrl = options.baseUrl || ''
    this.headers = options.headers || {}
    this.eventSource = null
    this.listeners = {}
    this.shouldReconnect = true // 是否允许自动重连
    this._isCompleted = false // 标记是否已收到完成信号
  }
  
  /**
   * 连接到SSE端点
   * @param {string} url - SSE端点URL（无需/api前缀，会自动添加）
   * @param {object} params - 查询参数
   */
  connect(url, params = {}) {
    // 重置完成标记和重连标志（新连接开始时允许重连）
    this._isCompleted = false
    this.shouldReconnect = true

    // 确保 URL 有 /api 前缀（后端 context-path 为 /api，SSE 不走 Axios 拦截器）
    if (!url.startsWith('/api') && !url.startsWith('http')) {
      url = '/api' + (url.startsWith('/') ? url : '/' + url)
    }
    
    // 自动注入 JWT Token（EventSource 不支持自定义请求头，只能通过 query param 传递）
    const token = localStorage.getItem('token')
    if (token) {
      params._token = token
    }
    
    // 构建完整URL
    const fullUrl = this.buildUrl(url, params)
    console.log('[SSEClient] 连接:', fullUrl)
    
    // 创建EventSource连接
    this.eventSource = new EventSource(fullUrl)
    
    // 设置事件监听器
    this.setupEventListeners()
    
    return this
  }
  
  /**
   * 构建URL
   * @param {string} url - 基础URL
   * @param {object} params - 查询参数
   * @returns {string} 完整URL
   */
  buildUrl(url, params) {
    // 当 baseUrl 为空时，使用当前页面 origin 作为 base
    const base = this.baseUrl || window.location.origin
    const urlObj = new URL(url, base)
    
    // 添加查询参数
    Object.entries(params).forEach(([key, value]) => {
      // 对 message 参数进行安全检查
      if (key === 'message' && typeof value === 'string') {
        const safetyResult = securityFilter.sanitize(value)
        if (safetyResult.action === 'BLOCK') {
          console.error('[SSEClient] 消息包含不允许的内容:', safetyResult.message)
          // 替换为安全消息
          value = '[已过滤的不安全输入]'
        } else if (safetyResult.cleaned !== value) {
          // 使用清洗后的消息
          value = safetyResult.cleaned
        }
        // 额外长度检查
        if (value.length > 50000) {
          console.warn('[SSEClient] 消息过长，已截断')
          value = value.substring(0, 50000)
        }
      }
      urlObj.searchParams.append(key, value)
    })
    
    return urlObj.toString()
  }
  
  /**
   * 设置事件监听器
   */
  setupEventListeners() {
    if (!this.eventSource) return
    
    // 连接打开事件
    this.eventSource.onopen = (event) => {
      console.log('[SSEClient] 连接已打开')
      this.emit('open', event)
    }
    
    // 消息事件
    this.eventSource.onmessage = (event) => {
      this.emit('message', this.parseMessage(event.data))
    }
    
    // 错误事件
    this.eventSource.onerror = (event) => {
      // ★ 如果已经收到完成信号，直接忽略所有错误事件，不再重连
      if (this._isCompleted || !this.shouldReconnect) {
        console.log('[SSEClient] 会话已完成，忽略错误事件，不再重连')
        this.close()
        return
      }

      console.error('[SSEClient] 连接错误:', event)

      // EventSource 不暴露 HTTP 状态码，但可以通过 readyState 判断
      // CONNECTING(0) = 正在重连, CLOSED(2) = 已关闭
      // 如果是 401/403 导致的错误，服务端会关闭连接，readyState 变为 CLOSED
      if (this.eventSource && this.eventSource.readyState === EventSource.CLOSED) {
        // 连接已彻底关闭（非自动重连），检查 Token 是否有效
        const token = localStorage.getItem('token')
        if (!token) {
          console.warn('[SSEClient] Token 已丢失，停止重连')
          this.emit('auth_expired', { message: '登录已过期，请重新登录' })
          this.close()
          return
        }
        // Token 存在但连接关闭，可能是 Token 过期
        // 通过一个轻量级请求验证 Token 有效性
        this._checkTokenValidity().then(valid => {
          if (!valid) {
            console.warn('[SSEClient] Token 已过期，通知认证过期')
            this.emit('auth_expired', { message: '登录已过期，请重新登录' })
            this.close()
          } else {
            console.warn('[SSEClient] Token 有效但连接关闭，可能是服务端异常')
          }
        })
      }

      this.emit('error', event)
      // 不在此处主动 close，让 EventSource 自动重连（readyState === CONNECTING 时）
      // 但自动重连受 shouldReconnect 控制（由外部在 onError 回调中判断）
    }
  }
  
  /**
   * 解析消息
   * @param {string} data - 原始消息数据
   * @returns {object} 解析后的消息
   */
  parseMessage(data) {
    try {
      return JSON.parse(data)
    } catch (e) {
      return { content: data }
    }
  }
  
  /**
   * 添加事件监听器
   * @param {string} event - 事件名称
   * @param {function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
      // 首次注册命名事件时，通过 addEventListener 注册到 EventSource
      // 注意：open/message/error 是 EventSource 内置事件，用对应回调处理
      if (this.eventSource && event !== 'open' && event !== 'message') {
        // 对于 error 事件，我们使用 addEventListener 捕获后端推送的命名 error 事件
        // 这与 EventSource 内置的 onerror（连接错误）不同，两者互不干扰
        this.eventSource.addEventListener(event, (e) => {
          const data = this.parseMessage(e.data)
          // ★ 关键：收到 session_completed 事件后，主动关闭连接并禁用重连
          if (event === 'session_completed') {
            this._isCompleted = true
            this.shouldReconnect = false
            // 主动关闭连接，阻止浏览器内置自动重连
            this.close()
          }
          this.listeners[event]?.forEach(cb => cb(data))
        })
      }
    }
    this.listeners[event].push(callback)
    return this
  }
  
  /**
   * 移除事件监听器
   * @param {string} event - 事件名称
   * @param {function} callback - 回调函数
   */
  off(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
    }
    return this
  }
  
  /**
   * 触发事件
   * @param {string} event - 事件名称
   * @param {*} data - 事件数据
   */
  emit(event, data) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(callback => callback(data))
    }
  }
  
  /**
   * 检查 Token 有效性（轻量级请求，不触发 UI 加载状态）
   * @returns {Promise<boolean>} Token 是否有效
   */
  async _checkTokenValidity() {
    try {
      const token = localStorage.getItem('token')
      if (!token) return false
      const response = await fetch('/api/notifications/unread-stats', {
        headers: { 'Authorization': `Bearer ${token}` },
        // 不触发 loading 状态
      })
      return response.ok
    } catch {
      return false
    }
  }

  /**
   * 关闭连接
   * @param {boolean} completed - 是否因会话完成而关闭
   */
  close(completed = false) {
    if (completed) {
      this._isCompleted = true
      this.shouldReconnect = false
    }
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }
  
  /**
   * 检查连接状态
   * @returns {boolean} 是否已连接
   */
  isConnected() {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN
  }
}

/**
 * 创建SSE客户端实例
 * @param {object} options - 配置选项
 * @returns {SSEClient} SSE客户端实例
 */
export const createSSEClient = (options = {}) => {
  return new SSEClient(options)
}

/**
 * 使用 fetch + ReadableStream 发送 POST SSE 请求
 * EventSource 只支持 GET，需要手动解析 POST SSE 响应
 * @param {string} url - 请求 URL
 * @param {object} body - 请求体对象
 * @param {object} options - 配置项
 * @param {function} options.onChunk - 每收到一段文本时回调
 * @param {function} options.onDone - 流结束时回调
 * @param {function} options.onError - 错误回调
 * @param {AbortSignal} options.signal - 中断信号
 * @returns {Promise} 完成Promise
 */
export const fetchSSE = async (url, body, options = {}) => {
  const { onChunk, onDone, onError, signal } = options

  try {
    // 自动注入 JWT Token（与 Axios 拦截器逻辑一致）
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
    }
    const token = localStorage.getItem('token')
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal,
    })

    if (!response.ok) {
      let errorMsg = `服务器响应错误 (${response.status})`
      if (response.status === 401) errorMsg = '未授权，请重新登录'
      else if (response.status === 403) errorMsg = '无权访问此功能'
      else if (response.status === 404) errorMsg = '后端接口不存在，请检查后端服务'
      else if (response.status >= 500) errorMsg = '后端服务异常，请稍后重试'
      throw new Error(errorMsg)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    // eslint-disable-next-line no-constant-condition -- SSE 读取循环依赖 break 退出
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // SSE 格式：每条消息以 \n\n 分隔
      const lines = buffer.split('\n')
      // 保留最后一行（可能不完整）
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue

        // SSE 数据行以 "data:" 开头，也可能是纯文本
        if (trimmed.startsWith('data:')) {
          const data = trimmed.slice(5).trim()
          if (data === '[DONE]') continue
          // 传递所有数据给 onChunk 回调，由调用方负责解析/过滤
          if (onChunk) {
            onChunk(data)
          }
        } else if (!trimmed.startsWith('event:') && !trimmed.startsWith('id:') && !trimmed.startsWith('retry:')) {
          // 纯文本数据（Spring AI Flux<String> 直接输出文本）
          if (onChunk) {
            onChunk(trimmed)
          }
        }
      }
    }

    // 处理 buffer 中剩余数据
    if (buffer.trim()) {
      const trimmed = buffer.trim()
      if (trimmed.startsWith('data:')) {
        const data = trimmed.slice(5).trim()
        if (data !== '[DONE]' && onChunk) onChunk(data)
      } else if (!trimmed.startsWith('event:') && !trimmed.startsWith('id:') && !trimmed.startsWith('retry:')) {
        if (onChunk) onChunk(trimmed)
      }
    }

    if (onDone) onDone()
  } catch (error) {
    if (error.name === 'AbortError') {
      if (onDone) onDone()
      return
    }
    if (onError) onError(error)
    else throw error
  }
}