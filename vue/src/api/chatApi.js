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
   * @param {function} params.onMessage - 内容块回调 ({ type, content })
   * @param {function} params.onKnowledgeRef - 知识库引用回调 ({ sources })
   * @param {function} params.onToolCall - 工具调用回调 ({ toolCall })
   * @param {function} params.onMcpStatus - MCP状态回调 ({ mcpStatus, toolStatus })
   * @param {function} params.onReactStep
   * @param {function} params.onComplete
   * @param {function} params.onError
   * @param {function} params.onStatusChange
   */
  async sendMessageStream(params) {
    const {
      message, role, conversationId, model, webSearch,
      useKnowledge = true, useTools = true,
      onMessage, onKnowledgeRef, onToolCall, onMcpStatus,
      onComplete, onError, onStatusChange
    } = params

    this.currentAbortController = new AbortController()
    const signal = this.currentAbortController.signal

    try {
      if (onStatusChange) onStatusChange('thinking')

      await fetchSSE('/api/chat/stream', {
        message: message,
        sessionId: conversationId || undefined,
        role: role || 'planner',
        model: model || undefined,
        webSearch: webSearch || false,
        useKnowledge: useKnowledge,
        useTools: useTools
      }, {
        signal,
        onChunk(chunk) {
          // 尝试解析结构化 JSON 事件
          try {
            const parsed = JSON.parse(chunk)
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
              case 'done':
                // 流结束标记
                break
              case 'content':
              default:
                // 仅当 content 是非空字符串时才追加到消息，防止结构化 JSON 被误显示
                if (onMessage && typeof parsed.content === 'string' && parsed.content.length > 0) {
                  onMessage({
                    type: 'chunk',
                    content: parsed.content,
                    timestamp: new Date()
                  })
                }
                break
            }
          } catch (e) {
            // 非 JSON 格式：严格过滤，不显示原始 JSON 或结构化数据片段
            // 仅当 chunk 是普通文本（不包含 JSON 特殊字符）时才作为纯文本处理
            if (chunk && onMessage && !chunk.trim().startsWith('{') && !chunk.trim().startsWith('[')) {
              onMessage({
                type: 'chunk',
                content: chunk,
                timestamp: new Date()
              })
            }
            // 否则静默丢弃（可能是 JSON 片段、错误数据等）
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
      const response = await fetch(`/api/chat/history/${conversationId}`)
      if (!response.ok) throw new Error('HTTP ' + response.status)
      const data = await response.json()
      return { success: true, data: { conversationId, messages: data } }
    } catch {
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