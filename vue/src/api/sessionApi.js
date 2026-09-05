// ============================================
// 学习会话 API
// ============================================

import request from './request'
import { SSEClient } from './sseClient'

/**
 * 创建学习会话
 * @param {object} data - { goal: string }
 */
export function createSession(data) {
  return request.post('/session/create', data)
}

/**
 * 获取会话详情
 * @param {number|string} id
 */
export function getSession(id) {
  return request.get(`/session/${id}`)
}

/**
 * 获取用户会话列表
 */
export function listSessions() {
  return request.get('/session/list')
}

/**
 * 提交阶段数据（如答题结果）
 * @param {number|string} sessionId
 * @param {object} data
 */
export function submitPhaseData(sessionId, data) {
  return request.post(`/session/${sessionId}/submit`, data)
}

/**
 * 提交答案
 * @param {number|string} sessionId
 * @param {number|string} questionId
 * @param {*} answer
 */
export function submitSessionAnswer(sessionId, questionId, answer) {
  return submitPhaseData(sessionId, { type: 'answer', questionId, answer })
}

/**
 * 确认规划
 * @param {number|string} sessionId
 * @param {object} plan
 */
export function confirmSessionPlan(sessionId, plan) {
  return submitPhaseData(sessionId, { type: 'confirm_plan', plan })
}

/**
 * 连接学习会话 SSE 流
 * @param {number|string} sessionId
 * @param {string} phase
 * @param {object} callbacks - 各事件回调
 * @returns {SSEClient}
 */
export function connectSessionStream(sessionId, phase, callbacks = {}) {
  const client = new SSEClient({
    baseUrl: ''
  })

  client.connect(`/session/${sessionId}/stream`, { phase })

  // 注册事件监听
  if (callbacks.onQuestions) {
    client.on('question', (data) => {
      // data.questions 可能是 JSON 字符串（后端序列化后的字符串），需要解析为数组
      let questions = data.questions
      if (typeof questions === 'string') {
        try {
          questions = JSON.parse(questions)
        } catch (e) {
          console.warn('解析 questions JSON 失败，使用原始字符串:', e.message)
        }
      }
      callbacks.onQuestions(Array.isArray(questions) ? questions : [])
    })
  }

  if (callbacks.onPhaseData) {
    client.on('phase_data', (data) => callbacks.onPhaseData(data))
  }

  if (callbacks.onAnswerFeedback) {
    client.on('answer_feedback', (data) => callbacks.onAnswerFeedback(data))
  }

  if (callbacks.onPhaseResult) {
    client.on('phase_result', (data) => callbacks.onPhaseResult(data.phase, data))
  }

  if (callbacks.onPhaseTransition) {
    client.on('phase_transition', (data) => callbacks.onPhaseTransition(data.from, data.to, data.message))
  }

  if (callbacks.onSessionComplete) {
    client.on('session_completed', (data) => callbacks.onSessionComplete(data))
  }

  if (callbacks.onError) {
    client.on('error', (data) => callbacks.onError(data))
  }

  // 认证过期事件（Token 过期或丢失时触发）
  if (callbacks.onAuthExpired) {
    client.on('auth_expired', (data) => callbacks.onAuthExpired(data))
  }

  return client
}

/**
 * 删除学习会话
 * @param {number|string} sessionId
 */
export function deleteSession(sessionId) {
  return request.delete(`/session/${sessionId}`)
}