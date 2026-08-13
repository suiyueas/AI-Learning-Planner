// 知识库状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getDocuments,
  getStatus,
  uploadDocument as uploadDocApi,
  deleteDocument as deleteDocApi,
  searchKnowledge as searchKnowledgeApi,
  askKnowledge as askKnowledgeApi
} from '@/api/knowledgeApi'

export const useKnowledgeStore = defineStore('knowledge', () => {
  // ============ 文档状态 ============
  const documents = ref([])
  const isLoading = ref(false)
  const loadError = ref(null)

  // ============ 搜索 ============
  const searchQuery = ref('')
  const searchResults = ref([])
  const isSearching = ref(false)

  // ============ 上传 ============
  const uploadProgress = ref(0)
  const isUploading = ref(false)

  // ============ 知识块 ============
  const knowledgeChunks = ref([])
  const selectedDocument = ref(null)

  // ============ 问答 ============
  const qaMessages = ref([])
  const qaInput = ref('')
  const isQALoading = ref(false)
  const qaConversations = ref([])
  const currentQAConversationId = ref(null)

  // ============ 知识库统计（来自API /status） ============
  const knowledgeStatus = ref({
    connected: false,
    documentCount: 0,
    chunkCount: 0,
    readyCount: 0
  })

  // ============ 计算属性 ============

  /** 按搜索查询过滤文档 */
  const filteredDocuments = computed(() => {
    if (!searchQuery.value) return documents.value
    const q = searchQuery.value.toLowerCase()
    return documents.value.filter(doc =>
      doc.title?.toLowerCase().includes(q) ||
      doc.type?.toLowerCase().includes(q)
    )
  })

  /** 文档统计（从 documents 列表实时计算，用于动画计数） */
  const documentStats = computed(() => {
    const total = documents.value.length
    const ready = documents.value.filter(d => d.status === 'ready').length
    const processing = documents.value.filter(d => d.status === 'processing' || d.status === 'uploading').length
    const totalChunks = documents.value.reduce((s, d) => s + (d.chunks || 0), 0)
    return { total, ready, processing, totalChunks }
  })

  // ============ 文档操作 ============

  /** 文档状态轮询定时器 */
  let statusPollingTimer = null

  /** 获取文档列表 */
  const fetchDocuments = async () => {
    isLoading.value = true
    loadError.value = null
    try {
      const res = await getDocuments()
      if (res.success && res.data) {
        documents.value = res.data
        // 重新同步选中文档引用，确保 selectedDocument 指向最新数据
        if (selectedDocument.value) {
          const updated = documents.value.find(d => d.id === selectedDocument.value.id)
          if (updated) {
            selectedDocument.value = updated
          }
        }
      }
    } catch (error) {
      console.error('获取文档列表失败:', error)
      loadError.value = error.message || '获取文档列表失败'
    } finally {
      isLoading.value = false
    }
  }

  /** 获取知识库状态 */
  const fetchStatus = async () => {
    try {
      const res = await getStatus()
      if (res.success && res.data) {
        knowledgeStatus.value = res.data
      }
    } catch (error) {
      console.error('获取知识库状态失败:', error)
    }
  }

  /** 加载所有数据（文档列表 + 状态） */
  const loadAll = async () => {
    await Promise.all([fetchDocuments(), fetchStatus()])
  }

  /**
   * 用户切换时重置：清空全部用户相关状态（含停止轮询），
   * 已登录时重新拉取当前用户数据
   */
  const resetForUser = async (userId = null) => {
    stopStatusPolling()
    documents.value = []
    isLoading.value = false
    loadError.value = null
    knowledgeStatus.value = {
      connected: false,
      documentCount: 0,
      chunkCount: 0,
      readyCount: 0
    }
    selectedDocument.value = null
    searchQuery.value = ''
    searchResults.value = []
    qaMessages.value = []
    qaConversations.value = []
    currentQAConversationId.value = null
    if (userId) {
      await loadAll()
    }
  }

  /** 上传文档 */
  const uploadDocument = async (file) => {
    isUploading.value = true
    uploadProgress.value = 0
    try {
      const res = await uploadDocApi(file, (progress) => {
        uploadProgress.value = progress
      })
      if (res.success && res.data) {
        documents.value.unshift(res.data)
        await fetchStatus()
        // 上传后启动轮询，等待文档处理完成
        startStatusPolling()
        return res.data
      } else {
        throw new Error(res.message || '上传失败')
      }
    } catch (error) {
      console.error('文档上传失败:', error)
      throw error
    } finally {
      isUploading.value = false
      uploadProgress.value = 0
    }
  }

  /** 删除文档 */
  const deleteDocument = async (docId) => {
    try {
      const res = await deleteDocApi(docId)
      if (res.success) {
        const idx = documents.value.findIndex(d => d.id === docId)
        if (idx > -1) documents.value.splice(idx, 1)
        await fetchStatus()
        // 删除后同步 selectedDocument
        if (selectedDocument.value?.id === docId) {
          const nextReady = documents.value.find(d => d.status === 'ready')
          selectedDocument.value = nextReady || null
        }
        return true
      } else {
        throw new Error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('文档删除失败:', error)
      throw error
    }
  }

  /**
   * 启动状态轮询：每 3 秒检查一次文档处理状态，
   * 直到所有 processing 文档都变为 ready 后自动停止
   */
  const startStatusPolling = () => {
    // 如果已有轮询则不重复启动
    if (statusPollingTimer) return
    statusPollingTimer = setInterval(async () => {
      await fetchDocuments()
      await fetchStatus()
      // 如果所有文档都已就绪（或出错），停止轮询
      const hasUnfinished = documents.value.some(
        d => d.status === 'processing' || d.status === 'uploading'
      )
      if (!hasUnfinished) {
        stopStatusPolling()
      }
    }, 3000)
  }

  /** 停止状态轮询 */
  const stopStatusPolling = () => {
    if (statusPollingTimer) {
      clearInterval(statusPollingTimer)
      statusPollingTimer = null
    }
  }

  // ============ 知识块 ============

  /** 选中文档 */
  const selectDocument = (doc) => {
    selectedDocument.value = doc || null
  }

  /** 获取当前选中文档的知识块（模拟分块展示） */
  const getSelectedDocumentChunks = () => {
    const doc = selectedDocument.value
    if (!doc || doc.status !== 'ready') return []
    const count = Math.min(doc.chunks || 0, 20)
    if (count === 0) return []
    const chunks = []
    for (let i = 1; i <= count; i++) {
      chunks.push({
        id: `${doc.id}_chunk_${i}`,
        docId: doc.id,
        docTitle: doc.title,
        index: i,
        preview: `知识块 #${i}：${doc.title} 的学习内容片段，包含相关知识点和示例说明。`,
        fullContent: `知识块 #${i} - ${doc.title}\n\n这是从"${doc.title}"文档中提取的第 ${i} 个知识片段，包含了相关的学习资料和关键知识点。`
      })
    }
    return chunks
  }

  // ============ 知识库搜索 ============

  /** 搜索知识块 */
  const searchChunks = async (keyword) => {
    if (!keyword.trim()) {
      searchResults.value = []
      return []
    }
    isSearching.value = true
    try {
      const res = await searchKnowledgeApi(keyword)
      // 后端可能返回各种格式，做兼容处理
      const data = res.data || res
      const list = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : [])
      const results = list.map((item, idx) => ({
        id: item.id || `search_${idx}`,
        docTitle: item.documentTitle || item.title || item.source || '未知文档',
        content: item.content || item.text || '',
        relevance: item.score || item.relevance || 0,
        preview: (item.content || item.text || '').substring(0, 80)
      }))
      searchResults.value = results
      return results
    } catch (error) {
      console.error('知识库搜索失败:', error)
      searchResults.value = []
      return []
    } finally {
      isSearching.value = false
    }
  }

  // ============ 知识库问答 ============

  /** 发起问答 */
  const askQuestion = async (question) => {
    if (!question.trim()) return

    // 添加用户消息
    qaMessages.value.push({
      id: 'msg_' + Date.now(),
      role: 'user',
      content: question,
      timestamp: new Date()
    })

    isQALoading.value = true
    qaInput.value = ''

    const aiMsgId = 'msg_' + (Date.now() + 1)
    const aiMessage = {
      id: aiMsgId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      sources: [],
      liked: null
    }
    qaMessages.value.push(aiMessage)

    try {
      const res = await askKnowledgeApi(question)
      const data = res.data || res
      const answer = data.answer || data.content || data.response || JSON.stringify(data)
      const sources = data.sources || data.references || []

      const idx = qaMessages.value.findIndex(m => m.id === aiMsgId)
      if (idx > -1) {
        qaMessages.value[idx].content = answer
        qaMessages.value[idx].sources = sources.map((s, i) => ({
          documentTitle: s.documentTitle || s.title || s.source || '引用文档',
          page: s.page || null,
          relevance: s.relevance || s.score || (0.95 - i * 0.03)
        }))
      }
    } catch (error) {
      console.error('知识库问答失败:', error)
      const idx = qaMessages.value.findIndex(m => m.id === aiMsgId)
      if (idx > -1) {
        qaMessages.value[idx].content = '抱歉，问答请求失败，请检查知识库服务是否正常运行。'
      }
    } finally {
      isQALoading.value = false
    }
  }

  // ============ 对话管理 ============

  const createQAConversation = () => {
    const id = 'qa_conv_' + Date.now()
    qaConversations.value.unshift({
      id,
      title: '新对话',
      createdAt: new Date(),
      updatedAt: new Date(),
      messages: []
    })
    currentQAConversationId.value = id
    qaMessages.value = []
  }

  const switchQAConversation = (id) => {
    currentQAConversationId.value = id
    const conv = qaConversations.value.find(c => c.id === id)
    qaMessages.value = conv ? [...conv.messages] : []
  }

  const deleteQAConversation = (id) => {
    const idx = qaConversations.value.findIndex(c => c.id === id)
    if (idx > -1) {
      qaConversations.value.splice(idx, 1)
      if (currentQAConversationId.value === id) {
        if (qaConversations.value.length > 0) {
          switchQAConversation(qaConversations.value[0].id)
        } else {
          createQAConversation()
        }
      }
    }
  }

  const clearQAMessages = () => {
    qaMessages.value = []
  }

  return {
    // 状态
    documents,
    isLoading,
    loadError,
    searchQuery,
    searchResults,
    isSearching,
    uploadProgress,
    isUploading,
    knowledgeChunks,
    selectedDocument,
    knowledgeStatus,

    // 问答
    qaMessages,
    qaInput,
    isQALoading,
    qaConversations,
    currentQAConversationId,

    // 计算属性
    filteredDocuments,
    documentStats,

    // 文档方法
    fetchDocuments,
    fetchStatus,
    loadAll,
    resetForUser,
    uploadDocument,
    deleteDocument,
    startStatusPolling,
    stopStatusPolling,

    // 知识块方法
    selectDocument,
    getSelectedDocumentChunks,
    searchChunks,

    // 问答方法
    askQuestion,
    createQAConversation,
    switchQAConversation,
    deleteQAConversation,
    clearQAMessages
  }
})
