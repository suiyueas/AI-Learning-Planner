// 对话状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get } from '@/api/request'
import { wsClient } from '@/api/wsClient'

export const useChatStore = defineStore('chat', () => {
  // 对话历史列表
  const conversations = ref([])
  
  // 当前会话ID
  const currentConversationId = ref(null)
  
  // 当前角色
  const currentRole = ref('planner') // planner, expert, partner
  
  // 消息列表
  const messages = ref([])
  
  // 是否正在输入
  const isTyping = ref(false)
  
  // AI状态 (online, thinking, acting, generating)
  const aiStatus = ref('online')
  
  // 是否正在流式输出
  const isStreaming = ref(false)
  
  // 流式输出定时器
  let streamingTimer = null
  
  // ReAct步骤
  const reactSteps = ref([])
  
  // 思考步骤
  const thinkingSteps = ref([])
  
  // 流式输出内容
  const streamingContent = ref('')
  
  // 当前流式消息ID
  const currentStreamingMessageId = ref(null)
  
  // 知识库文档列表
  const knowledgeDocuments = ref([])
  
  // MCP服务列表
  const mcpServices = ref([])
  
  // 知识库检索统计
  const knowledgeStats = ref({
    total: 0,
    loaded: 0,
    searchCount: 0
  })
  
  // MCP调用统计
  const mcpStats = ref({
    total: 0,
    success: 0,
    failed: 0
  })
  
  // ===== 知识库状态（对话界面专用） =====
  const knowledgeStatus = ref({
    connected: false,
    documentCount: 0,
    chunkCount: 0,
    readyCount: 0
  })

  // ===== Token 用量 =====
  const tokenUsage = ref({
    used: 0,
    total: 30720,
    promptTokens: 0,
    completionTokens: 0
  })

  // 更新 Token 用量（增量方式，每次只累加本次消耗的 Token；兼容旧调用方）
  const updateTokenUsage = (tokens) => {
    if (typeof tokens === 'number' && tokens > 0) {
      tokenUsage.value.used += tokens
    }
  }

  // 更新 Token 用量明细（输入/输出分别统计，used 由明细汇总得出）
  const updateTokenUsageDetail = ({ promptTokens, completionTokens }) => {
    const prompt = Number(promptTokens) > 0 ? Number(promptTokens) : 0
    const completion = Number(completionTokens) > 0 ? Number(completionTokens) : 0
    tokenUsage.value.promptTokens += prompt
    tokenUsage.value.completionTokens += completion
    tokenUsage.value.used = tokenUsage.value.promptTokens + tokenUsage.value.completionTokens
  }

  // 重置 Token 用量（切换会话时调用）
  const resetTokenUsage = () => {
    tokenUsage.value.used = 0
    tokenUsage.value.promptTokens = 0
    tokenUsage.value.completionTokens = 0
  }

  // ===== MCP 服务状态（对话界面专用） =====
  const mcpStatus = ref({
    availableCount: 0,
    totalCalls: 0,
    lastCall: null
  })
  
  // 计算属性：当前会话
  const currentConversation = computed(() => {
    return conversations.value.find(conv => conv.id === currentConversationId.value)
  })
  
  // 计算属性：角色信息
  const roleInfo = computed(() => {
    const roles = {
      planner: {
        name: '学习规划师',
        icon: '🎯',
        description: '制定个性化学习计划，规划学习路径'
      },
      expert: {
        name: '学科专家',
        icon: '📚',
        description: '解答专业知识，深入讲解概念'
      },
      partner: {
        name: '学习伙伴',
        icon: '🤝',
        description: '陪伴式学习，鼓励和督促'
      }
    }
    return roles[currentRole.value]
  })
  
  // 创建新会话
  const createConversation = () => {
    const newId = 'conv_' + Date.now()
    const newConversation = {
      id: newId,
      title: '新对话',
      createdAt: new Date(),
      updatedAt: new Date(),
      messages: []
    }
    conversations.value.unshift(newConversation)
    currentConversationId.value = newId
    messages.value = []
    return newConversation
  }
  
  // 切换会话
  const switchConversation = async (conversationId) => {
    if (!conversationId) return
    currentConversationId.value = conversationId
    const conversation = conversations.value.find(conv => conv.id === conversationId)
    if (conversation) {
      // 如果消息为空（摘要模式），从后端加载
      if (!conversation.messages || conversation.messages.length === 0) {
        try {
          const { chatAPI } = await import('@/api/chatApi')
          const result = await chatAPI.getConversationHistory(conversationId)
          if (result.success && result.data && result.data.messages) {
            const rawMessages = Array.isArray(result.data.messages) ? result.data.messages : []
            conversation.messages = rawMessages.map(r => ({
              id: r.id || r.messageId || ('msg_' + Date.now() + '_' + Math.random()),
              role: r.role === 'user' ? 'user' : 'assistant',
              content: r.content || r.message || '',
              timestamp: r.createdAt ? new Date(r.createdAt) : new Date(),
              steps: r.steps || [],
              status: 'completed',
              sources: r.sources || [],
              toolCalls: r.toolCalls || []
            }))
          }
        } catch (e) {
          console.warn('加载会话消息失败:', e)
          conversation.messages = []
        }
      }
      messages.value = conversation.messages || []
    } else {
      messages.value = []
    }
    // 切换会话时重置 Token 计数
    resetTokenUsage()
  }
  
  // 删除会话（同时调用后端删除）
  const deleteConversation = async (conversationId) => {
    // 先调用后端删除
    try {
      const { chatAPI } = await import('@/api/chatApi')
      await chatAPI.deleteConversation(conversationId)
    } catch (e) {
      console.warn('后端删除会话失败:', e)
    }

    // 再从前端列表中移除
    const index = conversations.value.findIndex(conv => conv.id === conversationId)
    if (index > -1) {
      conversations.value.splice(index, 1)
      if (currentConversationId.value === conversationId) {
        if (conversations.value.length > 0) {
          switchConversation(conversations.value[0].id)
        } else {
          createConversation()
        }
      }
    }
  }
  
  // 添加消息（支持去重：相同ID的消息会被更新而不是重复添加）
  const MAX_MESSAGES = 200
  const addMessage = (message) => {
    if (!message || !message.id) return
    
    // 查找是否已存在相同ID的消息
    const existingIndex = messages.value.findIndex(m => m.id === message.id)
    if (existingIndex > -1) {
      // 更新已有消息
      messages.value[existingIndex] = { ...messages.value[existingIndex], ...message }
    } else {
      // 添加新消息，超出上限时裁剪最早的消息
      messages.value.push(message)
      if (messages.value.length > MAX_MESSAGES) {
        messages.value = messages.value.slice(-MAX_MESSAGES)
      }
    }
    
    // 更新会话
    const conversation = conversations.value.find(conv => conv.id === currentConversationId.value)
    if (conversation) {
      conversation.messages = messages.value
      conversation.updatedAt = new Date()
      // 更新会话标题（使用第一条用户消息）
      if (message.role === 'user' && conversation.title === '新对话') {
        conversation.title = message.content.substring(0, 20) + (message.content.length > 20 ? '...' : '')
      }
    }
  }
  
  // 设置角色
  const setRole = (role) => {
    currentRole.value = role
  }
  
  // 设置输入状态
  const setTyping = (status) => {
    isTyping.value = status
    if (status) {
      aiStatus.value = 'thinking'
    } else {
      aiStatus.value = 'online'
    }
  }
  
  // 设置AI状态
  const setAIStatus = (status) => {
    aiStatus.value = status
  }
  
  // 设置流式输出状态
  const setStreaming = (status) => {
    isStreaming.value = status
    if (status) {
      aiStatus.value = 'generating'
    }
  }
  
  // 停止流式输出
  const stopStreaming = () => {
    if (streamingTimer) {
      clearInterval(streamingTimer)
      streamingTimer = null
    }
    isStreaming.value = false
    isTyping.value = false
    aiStatus.value = 'online'
    currentStreamingMessageId.value = null
  }
  
  // 添加ReAct步骤
  const addReactStep = (step) => {
    reactSteps.value.push(step)
  }
  
  // 更新ReAct步骤状态
  const updateReactStepStatus = (stepId, status) => {
    const step = reactSteps.value.find(s => s.id === stepId)
    if (step) {
      step.status = status
    }
  }
  
  // 清空ReAct步骤
  const clearReactSteps = () => {
    reactSteps.value = []
  }
  
  // 清空思考步骤
  const clearThinkingSteps = () => {
    thinkingSteps.value = []
  }
  
  // 添加思考步骤
  const addThinkingStep = (step) => {
    thinkingSteps.value.push(step)
  }
  
  // 设置流式内容
  const setStreamingContent = (content) => {
    streamingContent.value = content
  }
  
  // 追加流式内容
  const appendStreamingContent = (content) => {
    streamingContent.value += content
  }
  
  // 清空流式内容
  const clearStreamingContent = () => {
    streamingContent.value = ''
  }
  
  // 当前请求的 AbortController
  let currentAbortController = null

  // 发送消息（流式）
  const sendMessageStream = async (content) => {
    if (!content.trim() || isTyping.value) return
    
    // 添加用户消息
    const userMessage = {
      id: 'msg_' + Date.now(),
      role: 'user',
      content: content,
      timestamp: new Date()
    }
    addMessage(userMessage)
    
    // 设置输入状态
    isTyping.value = true
    aiStatus.value = 'thinking'
    
    // 清空之前的步骤
    clearReactSteps()
    clearThinkingSteps()
    clearStreamingContent()
    
    // 创建AI消息占位符
    const aiMessageId = 'msg_' + Date.now()
    currentStreamingMessageId.value = aiMessageId
    const aiMessage = {
      id: aiMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      reactSteps: [],
      sources: [],
      toolCalls: []
    }
    addMessage(aiMessage)
    
    // 显示思考步骤
    const thinkStep = {
      id: 'think_' + Date.now(),
      type: 'think',
      content: '正在分析你的问题...',
      timestamp: new Date(),
      status: 'completed'
    }
    addReactStep(thinkStep)
    
    const msgIndex = messages.value.findIndex(m => m.id === aiMessageId)
    if (msgIndex > -1) {
      messages.value[msgIndex].reactSteps = [...reactSteps.value]
    }
    
    // 调用 chatApi.js 的流式 API（支持结构化事件）
    isStreaming.value = true
    aiStatus.value = 'generating'
    currentAbortController = new AbortController()

    try {
      const { chatAPI } = await import('@/api/chatApi')
      await chatAPI.sendMessageStream({
        message: content,
        role: currentRole.value,
        conversationId: currentConversationId.value,
        onMessage: (chunk) => {
          const msgIdx = messages.value.findIndex(m => m.id === aiMessageId)
          if (msgIdx > -1 && chunk.content) {
            messages.value[msgIdx].content += chunk.content
            streamingContent.value = messages.value[msgIdx].content
          }
        },
        onComplete: (result) => {
          isStreaming.value = false
          isTyping.value = false
          aiStatus.value = 'online'
          currentStreamingMessageId.value = null
          currentAbortController = null
          if (result?.usage) {
            updateTokenUsageDetail(result.usage)
          }
        },
        onError: (error) => {
          console.error('SSE 请求失败:', error)
          const msgIdx = messages.value.findIndex(m => m.id === aiMessageId)
          if (msgIdx > -1 && !messages.value[msgIdx].content) {
            messages.value[msgIdx].content = '抱歉，请求失败，请检查后端服务是否已启动。'
          }
          isStreaming.value = false
          isTyping.value = false
          aiStatus.value = 'online'
          currentStreamingMessageId.value = null
          currentAbortController = null
        },
        onThinkingProcess: (step) => {
          const reactStep = {
            id: 'step_' + Date.now(),
            type: step.type,
            content: step.content,
            timestamp: step.timestamp || new Date(),
            status: 'completed'
          }
          addReactStep(reactStep)
          const msgIdx = messages.value.findIndex(m => m.id === aiMessageId)
          if (msgIdx > -1) {
            messages.value[msgIdx].reactSteps = [...reactSteps.value]
          }
        },
        onKnowledgeRef: (ref) => {
          const msgIdx = messages.value.findIndex(m => m.id === aiMessageId)
          if (msgIdx > -1 && ref.sources) {
            messages.value[msgIdx].sources = ref.sources
          }
        },
        onToolCall: (call) => {
          const msgIdx = messages.value.findIndex(m => m.id === aiMessageId)
          if (msgIdx > -1) {
            if (!messages.value[msgIdx].toolCalls) {
              messages.value[msgIdx].toolCalls = []
            }
            messages.value[msgIdx].toolCalls.push(call.toolCall)
          }
        },
        onMcpStatus: (status) => {
          if (status.mcpStatus) {
            mcpStatus.value = { ...mcpStatus.value, ...status.mcpStatus }
          }
        },
        onStatusChange: (status) => {
          aiStatus.value = status
        }
      })
    } catch (error) {
      console.error('发送消息失败:', error)
      const msgIdx = messages.value.findIndex(m => m.id === aiMessageId)
      if (msgIdx > -1 && !messages.value[msgIdx].content) {
        messages.value[msgIdx].content = '抱歉，请求失败，请稍后重试。'
      }
      isStreaming.value = false
      isTyping.value = false
      aiStatus.value = 'online'
      currentStreamingMessageId.value = null
      currentAbortController = null
    }
    
    return aiMessageId
  }
  
  // 停止流式输出
  const stopStreamingAction = () => {
    if (currentAbortController) {
      currentAbortController.abort()
      currentAbortController = null
    }
    stopStreaming()
  }

  // 从数据库加载历史对话列表（使用轻量级摘要 API）
  const loadConversations = async () => {
    try {
      const { chatAPI } = await import('@/api/chatApi')
      const result = await chatAPI.getConversationSummaries()
      if (!result.success || !result.data || result.data.length === 0) return

      // 构建会话列表（从摘要数据）
      const loadedConversations = result.data.map(conv => ({
        id: conv.sessionId,
        title: conv.title,
        messageCount: conv.messageCount,
        lastMessage: conv.lastMessage,
        createdAt: new Date(conv.createdAt),
        updatedAt: new Date(conv.updatedAt),
        messages: [] // 摘要模式下不预加载消息，切换时再加载
      }))

      // 合并到 conversations（避免重复）
      const existingIds = new Set(conversations.value.map(c => c.id))
      for (const conv of loadedConversations) {
        if (!existingIds.has(conv.id)) {
          conversations.value.push(conv)
        }
      }

      // 如果当前没有选中会话，或当前选中的会话不在加载列表中，则选中最新的
      const currentExists = currentConversationId.value && conversations.value.find(c => c.id === currentConversationId.value)
      if (!currentExists) {
        if (conversations.value.length > 0) {
          switchConversation(conversations.value[0].id)
        }
      }
    } catch (e) {
      console.warn('加载对话历史失败:', e)
    }
  }

  // ===== 获取知识库状态 =====
  const fetchKnowledgeStatus = async () => {
    try {
      const res = await get('/knowledge/status')
      if (res.success && res.data) {
        knowledgeStatus.value = { ...knowledgeStatus.value, ...res.data }
      }
    } catch (error) {
      console.warn('获取知识库状态失败:', error)
    }
  }

  // ===== 获取 MCP 服务状态 =====
  const fetchMcpStatus = async () => {
    try {
      const res = await get('/tools/status')
      if (res.success && res.data) {
        mcpStatus.value = { ...mcpStatus.value, ...res.data }
      }
    } catch (error) {
      console.warn('获取MCP状态失败:', error)
    }
  }

  // ===== 同时获取所有状态 =====
  const fetchAllStatus = async () => {
    await Promise.all([
      fetchKnowledgeStatus(),
      fetchMcpStatus()
    ])
  }

  // ===== WebSocket 实时推送 =====
  const wsSubscriptions = ref([])

  /**
   * 连接 WebSocket 并订阅知识库事件
   */
  const connectWebSocket = () => {
    if (wsClient.connected) return

    wsClient.connect()

    // 订阅知识库事件
    const subId = wsClient.subscribe('all', (event) => {
      console.log('[WS] Knowledge event received:', event.type)

      // 文档上传事件
      if (event.type === 'doc_uploaded') {
        knowledgeStatus.value.documentCount++
        knowledgeStatus.value.connected = true
      }

      // 文档就绪事件（知识块生成完成）
      if (event.type === 'doc_ready') {
        knowledgeStatus.value.readyCount++
        if (event.totalDocs !== undefined) knowledgeStatus.value.documentCount = event.totalDocs
        if (event.totalChunks !== undefined) knowledgeStatus.value.chunkCount = event.totalChunks
      }

      // 文档错误事件（知识块生成失败）
      if (event.type === 'doc_error') {
        if (event.totalDocs !== undefined) knowledgeStatus.value.documentCount = event.totalDocs
        if (event.totalChunks !== undefined) knowledgeStatus.value.chunkCount = event.totalChunks
        if (event.readyDocs !== undefined) knowledgeStatus.value.readyCount = event.readyDocs
      }

      // 文档删除事件
      if (event.type === 'doc_deleted') {
        if (event.totalDocs !== undefined) knowledgeStatus.value.documentCount = event.totalDocs
        if (event.totalChunks !== undefined) knowledgeStatus.value.chunkCount = event.totalChunks
        if (event.readyDocs !== undefined) knowledgeStatus.value.readyCount = event.readyDocs
      }

      // 全量生成完成事件
      if (event.type === 'chunks_generated') {
        if (event.totalDocs !== undefined) knowledgeStatus.value.documentCount = event.totalDocs
        if (event.totalChunks !== undefined) knowledgeStatus.value.chunkCount = event.totalChunks
        if (event.readyDocs !== undefined) knowledgeStatus.value.readyCount = event.readyDocs
      }
    })
    wsSubscriptions.value.push({ type: 'all', id: subId })
  }

  /**
   * 断开 WebSocket 连接
   */
  const disconnectWebSocket = () => {
    wsSubscriptions.value.forEach(({ type, id }) => {
      wsClient.unsubscribe(type, id)
    })
    wsSubscriptions.value = []
    wsClient.disconnect()
  }

  /**
   * 用户切换时重置：清空全部对话/状态数据（不残留上一用户信息），
   * 已登录时重新拉取知识库与 MCP 状态，并连接 WebSocket
   */
  const resetForUser = async (userId = null) => {
    conversations.value = []
    currentConversationId.value = null
    messages.value = []
    reactSteps.value = []
    thinkingSteps.value = []
    knowledgeDocuments.value = []
    mcpServices.value = []
    knowledgeStats.value = { total: 0, loaded: 0, searchCount: 0 }
    mcpStats.value = { total: 0, success: 0, failed: 0 }
    knowledgeStatus.value = { connected: false, documentCount: 0, chunkCount: 0, readyCount: 0 }
    mcpStatus.value = { availableCount: 0, totalCalls: 0, lastCall: null }
    tokenUsage.value = { used: 0, total: 30720, promptTokens: 0, completionTokens: 0 }
    isTyping.value = false
    isStreaming.value = false
    aiStatus.value = 'online'
    if (userId) {
      await Promise.all([fetchAllStatus(), loadConversations()])
      connectWebSocket()
    } else {
      disconnectWebSocket()
    }
  }

  return {
    conversations,
    currentConversationId,
    currentRole,
    messages,
    isTyping,
    aiStatus,
    isStreaming,
    reactSteps,
    thinkingSteps,
    streamingContent,
    currentStreamingMessageId,
    knowledgeDocuments,
    mcpServices,
    knowledgeStats,
    mcpStats,
    knowledgeStatus,
    mcpStatus,
    tokenUsage,
    currentConversation,
    roleInfo,
    createConversation,
    switchConversation,
    deleteConversation,
    addMessage,
    setRole,
    setTyping,
    setAIStatus,
    setStreaming,
    stopStreaming,
    stopStreamingAction,
    addReactStep,
    updateReactStepStatus,
    clearReactSteps,
    clearThinkingSteps,
    addThinkingStep,
    setStreamingContent,
    appendStreamingContent,
    clearStreamingContent,
    sendMessageStream,
    loadConversations,
    fetchKnowledgeStatus,
    fetchMcpStatus,
    fetchAllStatus,
    resetForUser,
    updateTokenUsage,
    updateTokenUsageDetail,
    resetTokenUsage,
    connectWebSocket,
    disconnectWebSocket
  }
})