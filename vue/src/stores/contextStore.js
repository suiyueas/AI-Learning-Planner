// 对话上下文状态管理（知识库文档 + MCP 工具选中状态）
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get } from '@/api/request'

export const useContextStore = defineStore('context', () => {
  // ===== 知识库文档列表（从 API 加载） =====
  const documents = ref([])
  // ===== 选中的文档 ID 列表 =====
  const selectedDocIds = ref([])
  // ===== MCP 工具列表（从 API 加载） =====
  const tools = ref([])
  // ===== 选中的工具 ID 列表 =====
  const selectedToolIds = ref([])

  // ===== 当前用户 ID（决定选中状态的 localStorage 隔离键） =====
  const currentUserId = ref(null)

  // ===== 从 localStorage 恢复选中状态（按用户隔离，避免切换用户后残留上一用户选择） =====
  const STORAGE_KEY_PREFIX = 'ai-chat-context-selection-'

  function storageKey() {
    return STORAGE_KEY_PREFIX + (currentUserId.value || 'guest')
  }

  function loadFromLocal() {
    try {
      const saved = localStorage.getItem(storageKey())
      if (saved) {
        const data = JSON.parse(saved)
        selectedDocIds.value = data.selectedDocIds || []
        selectedToolIds.value = data.selectedToolIds || []
      }
    } catch { /* ignore */ }
  }

  function saveToLocal() {
    try {
      localStorage.setItem(storageKey(), JSON.stringify({
        selectedDocIds: selectedDocIds.value,
        selectedToolIds: selectedToolIds.value
      }))
    } catch { /* ignore */ }
  }

  // ===== 计算属性 =====
  const selectedDocs = computed(() =>
    documents.value.filter(d => selectedDocIds.value.includes(d.id))
  )
  const selectedTools = computed(() =>
    tools.value.filter(t => selectedToolIds.value.includes(t.id))
  )
  const selectedDocCount = computed(() => selectedDocIds.value.length)
  const selectedToolCount = computed(() => selectedToolIds.value.length)

  // ===== 加载数据 =====
  const fetchDocuments = async () => {
    try {
      const res = await get('/knowledge/documents')
      if (res.success && res.data) {
        documents.value = res.data
      }
    } catch (error) {
      console.warn('[contextStore] 获取文档列表失败:', error)
    }
  }

  const fetchTools = async () => {
    try {
      const res = await get('/tools')
      if (res.success && res.data) {
        tools.value = res.data
      }
    } catch (error) {
      console.warn('[contextStore] 获取工具列表失败:', error)
    }
  }

  const fetchAll = async () => {
    await Promise.all([fetchDocuments(), fetchTools()])
  }

  /**
   * 用户切换时重置：清空全部状态后，按目标用户重新加载
   * userId 为空时仅清空（未登录不拉取数据）
   */
  const resetForUser = async (userId = null) => {
    currentUserId.value = userId || null
    documents.value = []
    tools.value = []
    selectedDocIds.value = []
    selectedToolIds.value = []
    // 按用户隔离恢复选中状态
    loadFromLocal()
    if (userId) {
      await fetchAll()
    }
  }

  // ===== 选中操作 =====
  function toggleDoc(docId) {
    const idx = selectedDocIds.value.indexOf(docId)
    if (idx > -1) {
      selectedDocIds.value.splice(idx, 1)
    } else {
      selectedDocIds.value.push(docId)
    }
    saveToLocal()
  }

  function toggleTool(toolId) {
    const idx = selectedToolIds.value.indexOf(toolId)
    if (idx > -1) {
      selectedToolIds.value.splice(idx, 1)
    } else {
      selectedToolIds.value.push(toolId)
    }
    saveToLocal()
  }

  function selectAllDocs() {
    selectedDocIds.value = documents.value.map(d => d.id)
    saveToLocal()
  }

  function deselectAllDocs() {
    selectedDocIds.value = []
    saveToLocal()
  }

  function selectAllTools() {
    selectedToolIds.value = tools.value.map(t => t.id)
    saveToLocal()
  }

  function deselectAllTools() {
    selectedToolIds.value = []
    saveToLocal()
  }

  function isDocSelected(docId) {
    return selectedDocIds.value.includes(docId)
  }

  function isToolSelected(toolId) {
    return selectedToolIds.value.includes(toolId)
  }

  return {
    documents,
    tools,
    selectedDocIds,
    selectedToolIds,
    selectedDocs,
    selectedTools,
    selectedDocCount,
    selectedToolCount,
    fetchDocuments,
    fetchTools,
    fetchAll,
    resetForUser,
    toggleDoc,
    toggleTool,
    selectAllDocs,
    deselectAllDocs,
    selectAllTools,
    deselectAllTools,
    isDocSelected,
    isToolSelected
  }
})
