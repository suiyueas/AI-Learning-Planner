import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useKnowledgeWorkflowStore = defineStore('knowledgeWorkflow', () => {
  const currentWorkflow = ref('retrieve')
  const isExecuting = ref(false)
  const workflowSteps = ref([])
  const workflowResult = ref('')
  const workflowHistory = ref([])

  const workflows = [
    {
      id: 'retrieve',
      icon: '📖',
      label: '智能检索',
      desc: '查找知识库中相关内容',
      placeholder: '输入你想查找的内容，如：什么是面向对象编程？'
    },
    {
      id: 'extract',
      icon: '✍️',
      label: '知识点提取',
      desc: '从文档中提取核心知识点',
      placeholder: '输入文档名称或粘贴文本内容...'
    },
    {
      id: 'quiz',
      icon: '📝',
      label: '生成测验题',
      desc: '基于知识库内容生成测验题',
      placeholder: '输入要出题的主题或章节...'
    },
    {
      id: 'summarize',
      icon: '📋',
      label: '文档总结',
      desc: '一键生成文档精华摘要',
      placeholder: '输入文档名称或粘贴文本内容...'
    }
  ]

  const templates = [
    { id: 't1', icon: '📊', label: '这个文档讲了什么？', workflow: 'summarize', query: '请总结这篇文档的主要内容' },
    { id: 't2', icon: '🔑', label: '提取关键知识点', workflow: 'extract', query: '请提取文档中的关键知识点' },
    { id: 't3', icon: '❓', label: '出5道题考考我', workflow: 'quiz', query: '请基于这篇文档出5道测验题' }
  ]

  const statusMap = {
    idle: '等待中',
    processing: '进行中',
    done: '已完成',
    error: '失败'
  }

  const selectWorkflow = (workflowId) => {
    currentWorkflow.value = workflowId
    workflowSteps.value = []
    workflowResult.value = ''
  }

  const addStep = (label, icon, status = 'processing', detail = '') => {
    workflowSteps.value.push({
      id: Date.now() + '_' + Math.random().toString(36).substr(2, 6),
      label,
      icon,
      status,
      detail,
      statusText: statusMap[status] || status
    })
  }

  const updateStep = (label, status, detail = '') => {
    const step = workflowSteps.value.find(s => s.label === label)
    if (step) {
      step.status = status
      step.statusText = statusMap[status] || status
      if (detail) {
        step.detail = detail
      }
    }
  }

  const setExecuting = (status) => {
    isExecuting.value = status
  }

  const setResult = (result) => {
    workflowResult.value = result
  }

  const clearSteps = () => {
    workflowSteps.value = []
  }

  const clearResult = () => {
    workflowResult.value = ''
  }

  const addToHistory = (record) => {
    workflowHistory.value.unshift({
      id: 'history_' + Date.now(),
      workflowId: record.workflowId,
      workflowLabel: record.workflowLabel,
      query: record.query,
      result: record.result,
      time: new Date().toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    })
    if (workflowHistory.value.length > 20) {
      workflowHistory.value = workflowHistory.value.slice(0, 20)
    }
  }

  const clearHistory = () => {
    workflowHistory.value = []
  }

  return {
    currentWorkflow,
    isExecuting,
    workflowSteps,
    workflowResult,
    workflowHistory,
    workflows,
    templates,
    selectWorkflow,
    addStep,
    updateStep,
    setExecuting,
    setResult,
    clearSteps,
    clearResult,
    addToHistory,
    clearHistory
  }
})