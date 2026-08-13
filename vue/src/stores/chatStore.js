// 对话状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { fetchSSE } from '@/api/sseClient'
import { get } from '@/api/request'

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
    total: 30720
  })

  // 更新 Token 用量（增量方式，每次只累加本次消耗的 Token）
  const updateTokenUsage = (tokens) => {
    if (typeof tokens === 'number' && tokens > 0) {
      tokenUsage.value.used += tokens
    }
  }

  // 重置 Token 用量（切换会话时调用）
  const resetTokenUsage = () => {
    tokenUsage.value.used = 0
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
  const switchConversation = (conversationId) => {
    currentConversationId.value = conversationId
    const conversation = conversations.value.find(conv => conv.id === conversationId)
    if (conversation) {
      messages.value = conversation.messages
    }
    // 切换会话时重置 Token 计数
    resetTokenUsage()
  }
  
  // 删除会话
  const deleteConversation = (conversationId) => {
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
  const addMessage = (message) => {
    if (!message || !message.id) return
    
    // 查找是否已存在相同ID的消息
    const existingIndex = messages.value.findIndex(m => m.id === message.id)
    if (existingIndex > -1) {
      // 更新已有消息
      messages.value[existingIndex] = { ...messages.value[existingIndex], ...message }
    } else {
      // 添加新消息
      messages.value.push(message)
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
  
  // 会话 ID 持久化（单会话）
  const sessionId = ref(crypto.randomUUID ? crypto.randomUUID() : 'sess_' + Date.now())

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
      reactSteps: []
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
    
    // 调用真实 SSE 流式 API
    isStreaming.value = true
    aiStatus.value = 'generating'
    currentAbortController = new AbortController()
    let fullContent = ''

    return new Promise((resolve) => {
      fetchSSE('/api/chat/stream', {
        message: content,
        sessionId: sessionId.value,
        role: currentRole.value,
      }, {
        signal: currentAbortController.signal,
        onChunk(chunk) {
          fullContent += chunk
          const msgIdx = messages.value.findIndex(m => m.id === aiMessageId)
          if (msgIdx > -1) {
            messages.value[msgIdx].content = fullContent
          }
          streamingContent.value = fullContent
        },
        onDone() {
          isStreaming.value = false
          isTyping.value = false
          aiStatus.value = 'online'
          currentStreamingMessageId.value = null
          currentAbortController = null
          resolve(aiMessageId)
        },
        onError(error) {
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
          resolve(aiMessageId)
        },
      })
    })
  }
  
  // 停止流式输出
  const stopStreamingAction = () => {
    if (currentAbortController) {
      currentAbortController.abort()
      currentAbortController = null
    }
    stopStreaming()
  }

  // 从数据库加载历史对话列表
  const loadConversations = async () => {
    try {
      // 从 localStorage 获取用户 ID（登录时写入）
      const userId = localStorage.getItem('userId')
      if (!userId) return

      const token = localStorage.getItem('token')
      if (!token) return

      const response = await fetch(`/api/chat/history/user/${userId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      if (!response.ok) return

      const records = await response.json()
      if (!records || records.length === 0) return

      // 按 sessionId 分组
      const sessionMap = new Map()
      for (const record of records) {
        const sid = record.sessionId
        if (!sessionMap.has(sid)) {
          sessionMap.set(sid, [])
        }
        sessionMap.get(sid).push(record)
      }

      // 构建会话列表（每个 sessionId 一个会话）
      const loadedConversations = []
      for (const [sid, sessionRecords] of sessionMap) {
        // 按时间正序排列消息
        sessionRecords.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))

        // 找到第一条用户消息作为标题
        const firstUserMsg = sessionRecords.find(r => r.role === 'user')
        const title = firstUserMsg
          ? firstUserMsg.content.substring(0, 30) + (firstUserMsg.content.length > 30 ? '...' : '')
          : '对话记录'

        // 转换为前端消息格式
        const convMessages = sessionRecords.map(r => ({
          id: r.id || ('msg_' + Date.now() + '_' + Math.random()),
          role: r.role === 'user' ? 'user' : 'assistant',
          content: r.content,
          timestamp: new Date(r.createdAt),
          steps: [],
          status: 'completed'
        }))

        const lastRecord = sessionRecords[sessionRecords.length - 1]
        loadedConversations.push({
          id: sid,
          title: title,
          createdAt: new Date(sessionRecords[0].createdAt),
          updatedAt: new Date(lastRecord.createdAt),
          messages: convMessages
        })
      }

      // 按更新时间倒序排列（最新的在最上面）
      loadedConversations.sort((a, b) => b.updatedAt - a.updatedAt)

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

  /**
   * 用户切换时重置：清空全部对话/状态数据（不残留上一用户信息），
   * 已登录时重新拉取知识库与 MCP 状态
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
    tokenUsage.value = { used: 0, total: 30720 }
    isTyping.value = false
    isStreaming.value = false
    aiStatus.value = 'online'
    if (userId) {
      await Promise.all([fetchAllStatus(), loadConversations()])
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
    sessionId,
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
    resetTokenUsage
  }
})