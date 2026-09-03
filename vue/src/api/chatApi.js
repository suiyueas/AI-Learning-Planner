// 对话API接口 - 调用后端真实 API，支持 SSE 流式返回（结构化 JSON 事件）
import { fetchSSE } from './sseClient'
import { post, del } from './request'

/**
 * 对话API类
 */
export class ChatAPI {
  constructor(options = {}) {
    this.baseUrl = options.baseUrl || '/api'
    this.currentAbortController = null
  }

  /**
   * 发送消息并获取流式响应（支持结构化 SSE 事件）
   * @param {object} params
   * @param {string} params.message
   * @param {string} params.role
   * @param {string} params.conversationId
   * @param {string} params.model - 模型key
   * @param {boolean} params.webSearch - 是否启用联网搜索
   * @param {boolean} params.useKnowledge - 是否启用知识库检索
   * @param {boolean} params.useTools - 是否启用工具调用
   * @param {string} params.reasoningLevel - 思考深度模式 (fast/standard/deep)
   * @param {function} params.onMessage - 内容块回调 ({ type, content })
   * @param {function} params.onKnowledgeRef - 知识库引用回调 ({ sources })
   * @param {function} params.onToolCall - 工具调用回调 ({ toolCall })
   * @param {function} params.onMcpStatus - MCP状态回调 ({ mcpStatus, toolStatus })
   * @param {function} params.onThinkingProcess - 思考过程回调 ({ type, content, step, label, metadata })
   * @param {function} params.onComplete
   * @param {function} params.onError
   * @param {function} params.onStatusChange
   */
  async sendMessageStream(params) {
    const {
      message, role, conversationId, model, webSearch,
      useKnowledge = true, useTools = true, reasoningLevel = 'standard',
      selectedDocIds, selectedToolIds,
      onMessage, onKnowledgeRef, onToolCall, onMcpStatus,
      onThinkingProcess, onComplete, onError, onStatusChange
    } = params

    this.currentAbortController = new AbortController()
    const signal = this.currentAbortController.signal

    try {
      if (onStatusChange) onStatusChange('thinking')

      // 从任意对象中提取文本内容的辅助函数
      const extractContent = (obj) => {
        if (typeof obj === 'string') return obj
        if (!obj || typeof obj !== 'object') return null
        for (const key of ['content', 'answer', 'text', 'message', 'result', 'output', 'response', 'data']) {
          if (typeof obj[key] === 'string' && obj[key].length > 0) return obj[key]
        }
        return null
      }

      await fetchSSE('/api/chat/stream', {
        message: message,
        sessionId: conversationId || undefined,
        role: role || 'planner',
        model: model || undefined,
        webSearch: webSearch || false,
        useKnowledge: useKnowledge,
        useTools: useTools,
        reasoningLevel: reasoningLevel,
        selectedDocIds: selectedDocIds || undefined,
        selectedToolIds: selectedToolIds || undefined
      }, {
        signal,
        onChunk(chunk) {
          // 尝试解析结构化 JSON 事件
          try {
            const trimmed = chunk.trim()
            if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
              const parsed = JSON.parse(trimmed)
              const type = parsed.type || 'content'

              switch (type) {
                case 'knowledgeRef':
                  if (onKnowledgeRef && parsed.sources) {
                    onKnowledgeRef({ sources: parsed.sources })
                  }
                  break
                case 'toolCall':
                  if (onToolCall && parsed.toolCall) {
                    onToolCall({ toolCall: parsed.toolCall })
                  }
                  break
                case 'mcpStatus':
                  if (onMcpStatus) {
                    onMcpStatus({
                      mcpStatus: parsed.mcpStatus,
                      toolStatus: parsed.toolStatus
                    })
                  }
                  break
                case 'reasoning_mode':
                  if (onThinkingProcess) {
                    onThinkingProcess({
                      type: 'reasoning_mode',
                      reasoningLevel: parsed.reasoningLevel,
                      timestamp: new Date()
                    })
                  }
                  break
                case 'understanding':
                case 'planning':
                case 'thinking':
                case 'action':
                case 'observation':
                case 'reflection':
                case 'alternative':
                case 'result':
                  if (onThinkingProcess) {
                    onThinkingProcess({
                      type: parsed.type,
                      content: parsed.content,
                      step: parsed.step,
                      label: parsed.label,
                      metadata: parsed.metadata,
                      timestamp: new Date(parsed.timestamp)
                    })
                  }
                  break
                case 'done':
                  if (onComplete) {
                    onComplete({ type: 'complete', usage: parsed.usage, timestamp: new Date() })
                  }
                  break
                case 'content':
                  if (onMessage && typeof parsed.content === 'string' && parsed.content.length > 0) {
                    onMessage({ type: 'chunk', content: parsed.content, timestamp: new Date() })
                  }
                  break
                default:
                  // 对于未知类型，尝试从常见字段中提取文本内容
                  const contentStr = extractContent(parsed)
                  if (contentStr && onMessage) {
                    onMessage({ type: 'chunk', content: contentStr, timestamp: new Date() })
                  }
                  break
              }
              return
            }
          } catch (e) {
            // JSON 解析失败，作为纯文本处理
          }

          if (chunk && onMessage) {
            const text = chunk.trim()
            if (text && !text.startsWith('event:') && !text.startsWith('id:') && !text.startsWith('retry:') && text !== '[DONE]') {
              onMessage({ type: 'chunk', content: text, timestamp: new Date() })
            }
          }
        },
        onDone() {
          if (onComplete) {
            onComplete({ type: 'complete', timestamp: new Date() })
          }
          if (onStatusChange) onStatusChange('online')
        },
        onError(error) {
          if (onError) onError(error)
          if (onStatusChange) onStatusChange('online')
        }
      })
    } catch (error) {
      if (error.name === 'AbortError') {
        if (onComplete) onComplete({ type: 'stopped' })
      } else if (onError) {
        onError(error)
      }
      if (onStatusChange) onStatusChange('online')
    } finally {
      this.currentAbortController = null
    }
  }

  stopStreaming() {
    if (this.currentAbortController) {
      this.currentAbortController.abort()
      this.currentAbortController = null
    }
  }

  async getConversationHistory(conversationId) {
    try {
      const response = await fetch(`/api/chat/history/${conversationId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
        },
        credentials: 'same-origin'
      })
      if (!response.ok) throw new Error('HTTP ' + response.status)
      const data = await response.json()
      return { success: true, data: { conversationId, messages: data.data || data } }
    } catch (e) {
      console.warn('获取会话历史失败:', e)
      return { success: true, data: { conversationId, messages: [] } }
    }
  }

  async deleteConversation(conversationId) {
    try {
      await del(`/chat/history/${conversationId}`)
      return { success: true, message: '会话删除成功' }
    } catch (error) {
      console.error('删除会话失败:', error)
      return { success: false, message: error.message || '删除会话失败' }
    }
  }

  /**
   * 获取用户的会话摘要列表（轻量级，不含消息内容）
   */
  async getConversationSummaries() {
    try {
      const response = await fetch('/api/chat/conversations', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
        },
        credentials: 'same-origin'
      })
      if (!response.ok) {
        console.warn('获取会话摘要失败:', response.status, response.statusText)
        return { success: false, data: [] }
      }
      const data = await response.json()
      return { success: true, data: data.data || data }
    } catch (error) {
      console.error('获取会话摘要失败:', error)
      return { success: false, data: [] }
    }
  }
}

/**
 * 代码解析（非流式，普通 POST 请求）
 * @param {string} code 待分析代码
 * @param {string} language 语言（可选，后端自动检测）
 * @returns {Promise} { issues, suggestions, complexity, summary, optimizedCode }
 */
export async function analyzeCode(code, language = null) {
  const params = { code }
  if (language) params.language = language
  const res = await post('/chat/code-analyze', params)
  return res
}

export const createChatAPI = (options = {}) => new ChatAPI(options)
export const chatAPI = createChatAPI()