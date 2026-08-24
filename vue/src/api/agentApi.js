// 智能体API接口
import { get, post, del } from './request'

/**
 * 获取Agent列表
 * @returns {Promise} Agent列表
 */
export const getAgents = () => {
  return get('/agent/list')
}

/**
 * 获取指定Agent状态
 * @param {string} agentId Agent ID
 * @returns {Promise} Agent状态
 */
export const getAgentDetail = (agentId) => {
  return get(`/agent/status/${agentId}`)
}

/**
 * 同步执行Agent任务
 * @param {object} params 请求参数
 * @param {string} params.agentId Agent ID
 * @param {string} params.message 任务描述
 * @returns {Promise} 执行结果
 */
export const executeAgentTask = (params) => {
  return post('/agent/execute', params)
}

/**
 * 停止Agent执行
 * @param {string} agentId Agent ID
 * @returns {Promise}
 */
export const stopAgentExecution = (agentId) => {
  return post(`/agent/stop/${agentId}`)
}

/**
 * 流式调用Agent（SSE - GET方式）
 * 通过EventSource建立连接，支持命名事件（think/act/observe/tool_call等）
 * @param {string} agentId Agent ID
 * @param {string} message 任务描述
 * @param {object} callbacks 回调函数
 * @param {function} callbacks.onThink 思考步骤回调
 * @param {function} callbacks.onAct 行动步骤回调
 * @param {function} callbacks.onObserve 观察步骤回调
 * @param {function} callbacks.onToolCall 工具调用回调
 * @param {function} callbacks.onToolResult 工具结果回调
 * @param {function} callbacks.onComplete 完成回调
 * @param {function} callbacks.onError 错误回调
 * @returns {EventSource} EventSource对象
 */
export const streamAgentExecution = (agentId, message, callbacks = {}) => {
  const params = new URLSearchParams({ agentId, message })
  const url = `/api/agent/stream?${params.toString()}`
  
  const eventSource = new EventSource(url)
  
  // 通用消息处理
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handleEventData(data, callbacks)
    } catch (e) {
      // 非 JSON 格式：仅当是普通文本（非 JSON 字符串）时才传递给 onChunk，防止原始 JSON 显示
      const text = event.data
      if (text && callbacks.onChunk && !text.trim().startsWith('{') && !text.trim().startsWith('[')) {
        callbacks.onChunk(text)
      }
    }
  }
  
  // 命名事件处理
  const eventTypes = ['think', 'act', 'observe', 'tool_call', 'tool_result', 
                      'llm_call', 'step_result', 'status', 'complete', 'error']
  
  eventTypes.forEach(eventType => {
    eventSource.addEventListener(eventType, (event) => {
      try {
        const data = JSON.parse(event.data)
        handleNamedEvent(eventType, data, callbacks)
      } catch (e) {
        console.warn(`SSE事件解析失败: ${eventType}`, event.data)
      }
    })
  })
  
  eventSource.onerror = (error) => {
    console.error('SSE连接错误:', error)
    if (callbacks.onError) callbacks.onError(error)
    eventSource.close()
  }
  
  return eventSource
}

/**
 * 处理通用事件数据
 */
function handleEventData(data, callbacks) {
  if (data.state === 'RUNNING' && callbacks.onStatus) {
    callbacks.onStatus(data)
  } else if (typeof data.content === 'string' && data.content && callbacks.onChunk) {
    // 仅当 content 是非空字符串时才传递，防止结构化 JSON 被误显示
    callbacks.onChunk(data.content)
  }
}

// ==================== 数据持久化接口 ====================

/**
 * 执行规划Agent（同步执行+数据持久化）
 * @param {object} params 请求参数
 * @param {string} params.agentId Agent ID
 * @param {string} params.message 任务描述
 * @returns {Promise} 执行结果（含持久化数据）
 */
export const executePlanTask = (params) => {
  return post('/agent/plan/execute', params)
}

/**
 * 获取Agent执行日志
 * @param {string} agentId Agent ID
 * @returns {Promise} 日志列表
 */
export const getAgentLogs = (agentId) => {
  return get(`/agent/logs/${agentId}`)
}

/**
 * 获取Agent执行结果列表
 * @param {string} agentId Agent ID
 * @returns {Promise} 结果列表
 */
export const getAgentResults = (agentId) => {
  return get(`/agent/results/${agentId}`)
}

/**
 * 获取所有执行日志
 * @returns {Promise} 全部日志
 */
export const getAllLogs = () => {
  return get('/agent/logs')
}

/**
 * 获取所有执行结果
 * @returns {Promise} 全部结果
 */
export const getAllResults = () => {
  return get('/agent/results')
}

/**
 * 批量保存Agent执行日志和结果（持久化到数据库）
 * @param {object} data 执行数据
 * @param {string} data.agentId Agent ID
 * @param {string} data.taskDescription 任务描述
 * @param {object} data.result 执行结果
 * @param {Array} data.logs 执行日志列表
 * @returns {Promise}
 */
export const saveAgentExecution = (data) => {
  return post('/agent/execution/save', data)
}

/**
 * 按ID获取单个执行结果详情
 * @param {string} id 结果ID
 * @returns {Promise} 结果详情
 */
export const getResultById = (id) => {
  return get(`/agent/result/${id}`)
}

/**
 * 获取Agent最新执行结果
 * @param {string} agentId Agent ID
 * @returns {Promise} 最新结果
 */
export const getLatestResult = (agentId) => {
  return get(`/agent/results/latest/${agentId}`)
}

/**
 * 清空所有执行日志
 * @returns {Promise}
 */
export const clearAllLogs = () => {
  return del('/agent/logs')
}

/**
 * 清空所有执行结果
 * @param {string} mode 删除方式：soft（默认，可回收站恢复）/ hard（物理删除）
 * @returns {Promise}
 */
export const clearAllResults = (mode = 'soft') => {
  return del('/agent/results', { mode })
}

/**
 * 按ID删除单条执行结果
 * @param {string} id 结果ID
 * @param {string} mode 删除方式：soft（默认，可回收站恢复）/ hard（物理删除）
 * @returns {Promise}
 */
export const deleteResultById = (id, mode = 'soft') => {
  return del(`/agent/results/${id}`, { mode })
}

/**
 * 批量删除执行结果
 * @param {Array<string>} ids 结果ID列表
 * @param {string} mode 删除方式：soft（默认，可回收站恢复）/ hard（物理删除）
 * @returns {Promise}
 */
export const deleteResultsBatch = (ids, mode = 'soft') => {
  // 手动构造 query 参数，避免 axios 将数组序列化为 ids[]=x 导致后端无法解析
  const query = (ids || []).map(id => `ids=${encodeURIComponent(id)}`).join('&')
  return del(`/agent/results/batch?${query}&mode=${mode}`)
}

/**
 * 获取当前用户活动执行结果计数（与列表同数据源）
 * @returns {Promise}
 */
export const getResultsCount = () => {
  return get('/agent/results/count')
}

/**
 * 获取回收站列表（软删除的执行结果）
 * @returns {Promise}
 */
export const getTrashResults = () => {
  return get('/agent/results/trash')
}

/**
 * 恢复软删除的执行结果
 * @param {string} id 结果ID
 * @returns {Promise}
 */
export const restoreResult = (id) => {
  return post(`/agent/results/${id}/restore`)
}

/**
 * 按 sessionId 获取执行日志
 * @param {string} sessionId 会话ID
 * @returns {Promise} 日志列表
 */
export const getLogsBySessionId = (sessionId) => {
  return get(`/agent/logs/session/${sessionId}`)
}

/**
 * 按 executionId 获取执行日志
 * @param {string} executionId 执行ID
 * @returns {Promise} 日志列表
 */
export const getLogsByExecutionId = (executionId) => {
  return get(`/agent/logs/execution/${executionId}`)
}

/**
 * POST SSE 流式执行（真实 ReAct 可视化）
 * 使用 fetch + ReadableStream 接收后端 SSE 流，支持命名事件
 * @param {string} agentId Agent ID
 * @param {string} message 任务描述
 * @param {object} callbacks 回调函数集
 * @param {function} callbacks.onStart 开始事件
 * @param {function} callbacks.onThink 思考步骤
 * @param {function} callbacks.onAct 行动步骤
 * @param {function} callbacks.onObserve 观察步骤
 * @param {function} callbacks.onReflect 反思步骤
 * @param {function} callbacks.onReplan 重规划步骤
 * @param {function} callbacks.onComplete 完成事件
 * @param {function} callbacks.onError 错误事件
 * @param {AbortSignal} signal 中断信号
 * @returns {Promise} 完成Promise
 */
export const postStreamExecution = async (agentId, message, callbacks = {}, signal = null) => {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream'
  }
  const token = localStorage.getItem('token')
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch('/api/agent/execute/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify({ agentId, message }),
    signal
  })

  if (!response.ok) {
    let errorMsg = `服务器响应错误 (${response.status})`
    if (response.status === 401) errorMsg = '未授权，请重新登录'
    else if (response.status === 404) errorMsg = '后端接口不存在，请检查后端服务'
    else if (response.status >= 500) errorMsg = '后端服务异常，请稍后重试'
    throw new Error(errorMsg)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  // eslint-disable-next-line no-constant-condition
  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // 按行解析 SSE 格式
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    let currentEvent = null
    let currentData = ''

    for (const line of lines) {
      const trimmed = line.trim()

      // 空行表示事件分隔符
      if (!trimmed) {
        if (currentEvent && currentData) {
          handleStreamEvent(currentEvent, currentData, callbacks)
        }
        currentEvent = null
        currentData = ''
        continue
      }

      if (trimmed.startsWith('event:')) {
        currentEvent = trimmed.slice(6).trim()
      } else if (trimmed.startsWith('data:')) {
        currentData = trimmed.slice(5).trim()
      }
    }

    // 处理最后一个未完成的事件
    if (currentEvent && currentData) {
      handleStreamEvent(currentEvent, currentData, callbacks)
    }
  }

  // 处理 buffer 中剩余数据
  if (buffer.trim()) {
    const trimmed = buffer.trim()
    let evt = null
    let data = ''
    for (const line of trimmed.split('\n')) {
      const t = line.trim()
      if (t.startsWith('event:')) evt = t.slice(6).trim()
      else if (t.startsWith('data:')) data = t.slice(5).trim()
    }
    if (evt && data) handleStreamEvent(evt, data, callbacks)
  }
}

/**
 * 处理 SSE 流式事件
 */
function handleStreamEvent(eventName, dataStr, callbacks) {
  let data
  try {
    data = JSON.parse(dataStr)
  } catch {
    data = { content: dataStr }
  }

  switch (eventName) {
    case 'start':
      if (callbacks.onStart) callbacks.onStart(data)
      break
    case 'think':
      if (callbacks.onThink) callbacks.onThink(data)
      break
    case 'act':
      if (callbacks.onAct) callbacks.onAct(data)
      break
    case 'observe':
      if (callbacks.onObserve) callbacks.onObserve(data)
      break
    case 'tool_call':
      if (callbacks.onAct) callbacks.onAct(data)
      break
    case 'tool_result':
      if (callbacks.onObserve) callbacks.onObserve(data)
      break
    case 'reflection':
    case 'reflect':
      if (callbacks.onReflect) callbacks.onReflect(data)
      break
    case 'replan':
      if (callbacks.onReplan) callbacks.onReplan(data)
      break
    case 'complete':
    case 'finish':
      if (callbacks.onComplete) callbacks.onComplete(data)
      break
    case 'error':
      if (callbacks.onError) callbacks.onError(data)
      break
    case 'status':
      if (callbacks.onStart) callbacks.onStart(data)
      break
    case 'step_result':
      if (callbacks.onStepResult) callbacks.onStepResult(data)
      break
    default:
      break
  }
}

/**
 * 处理命名事件
 */
function handleNamedEvent(eventType, data, callbacks) {
  switch (eventType) {
    case 'think':
      if (callbacks.onThink) callbacks.onThink(data)
      break
    case 'act':
      if (callbacks.onAct) callbacks.onAct(data)
      break
    case 'observe':
      if (callbacks.onObserve) callbacks.onObserve(data)
      break
    case 'tool_call':
      if (callbacks.onToolCall) callbacks.onToolCall(data)
      break
    case 'tool_result':
      if (callbacks.onToolResult) callbacks.onToolResult(data)
      break
    case 'complete':
      if (callbacks.onComplete) callbacks.onComplete(data)
      break
    case 'error':
      if (callbacks.onError) callbacks.onError(data)
      break
    case 'status':
      if (callbacks.onStatus) callbacks.onStatus(data)
      break
    case 'step_result':
      if (callbacks.onStepResult) callbacks.onStepResult(data)
      break
    default:
      break
  }
}

export default {
  getAgents,
  getAgentDetail,
  executeAgentTask,
  stopAgentExecution,
  streamAgentExecution,
  // 数据持久化接口
  executePlanTask,
  getAgentLogs,
  getAgentResults,
  saveAgentExecution,
  getResultById,
  clearAllLogs,
  clearAllResults,
  deleteResultById,
  deleteResultsBatch
}