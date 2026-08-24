<template>
  <div class="workflow-executor">
    <QuickTemplates :templates="templates" @run="handleRunTemplate" />

    <div class="workflow-input-area">
      <div class="input-wrapper">
        <input
          v-model="inputValue"
          type="text"
          class="workflow-input"
          :placeholder="currentPlaceholder"
          :disabled="isExecuting"
          @keyup.enter="handleExecute"
        />
        <button
          class="execute-btn"
          :disabled="isExecuting || !inputValue.trim()"
          @click="handleExecute"
        >
          <span v-if="isExecuting" class="btn-spinner"></span>
          <span v-else>执行</span>
        </button>
      </div>
    </div>

    <div v-if="isExecuting || steps.length > 0" class="workflow-status">
      <WorkflowSteps :steps="steps" />

      <div v-if="workflowResult" class="workflow-result">
        <div class="result-header">
          <span class="result-title">📄 执行结果</span>
          <button class="copy-btn" @click="handleCopy">📋 复制</button>
        </div>
        <div class="result-body markdown-body" v-html="renderedResult"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'
import QuickTemplates from './QuickTemplates.vue'
import WorkflowSteps from './WorkflowSteps.vue'

const props = defineProps({
  currentWorkflow: {
    type: String,
    default: 'retrieve'
  },
  isExecuting: {
    type: Boolean,
    default: false
  },
  steps: {
    type: Array,
    default: () => []
  },
  workflowResult: {
    type: String,
    default: ''
  },
  templates: {
    type: Array,
    default: () => [
      { id: 't1', icon: '📊', label: '这个文档讲了什么？', workflow: 'summarize' },
      { id: 't2', icon: '🔑', label: '提取关键知识点', workflow: 'extract' },
      { id: 't3', icon: '❓', label: '出5道题考考我', workflow: 'quiz' }
    ]
  }
})

const emit = defineEmits(['execute', 'run-template'])

const inputValue = ref('')

const placeholders = {
  retrieve: '输入你想查找的内容，如：什么是面向对象编程？',
  extract: '输入文档名称或粘贴文本内容...',
  quiz: '输入要出题的主题或章节...',
  summarize: '输入文档名称或粘贴文本内容...'
}

const currentPlaceholder = computed(() => placeholders[props.currentWorkflow] || placeholders.retrieve)

const renderedResult = computed(() => renderMarkdown(props.workflowResult || ''))

const handleExecute = () => {
  if (!inputValue.value.trim() || props.isExecuting) return
  emit('execute', {
    workflow: props.currentWorkflow,
    query: inputValue.value.trim()
  })
}

const handleRunTemplate = (template) => {
  emit('run-template', template)
}

const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.workflowResult)
    ElMessage.success('已复制结果')
  } catch {
    ElMessage.error('复制失败')
  }
}

defineExpose({
  inputValue
})
</script>

<style lang="scss" scoped>
.workflow-executor {
  background: rgba(10, 10, 26, 0.6);
  border-radius: 12px;
  overflow: hidden;
}

.workflow-input-area {
  padding: 16px;
  background: rgba(17, 17, 39, 0.3);
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.workflow-input {
  flex: 1;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(124, 97, 255, 0.2);
  border-radius: 10px;
  color: #f0f0ff;
  font-size: 0.9rem;
  outline: none;
  transition: all 0.2s ease;

  &::placeholder {
    color: var(--text-placeholder, #6b6b8b);
  }

  &:focus {
    border-color: rgba(124, 97, 255, 0.5);
    box-shadow: 0 0 20px rgba(124, 97, 255, 0.1);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.execute-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(124, 97, 255, 0.8), rgba(91, 134, 255, 0.8));
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  min-width: 80px;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(124, 97, 255, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.workflow-status {
  max-height: 400px;
  overflow-y: auto;
}

.workflow-result {
  margin: 0 16px 16px;
  padding: 16px;
  background: rgba(100, 100, 180, 0.08);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 12px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.result-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary, #f0f0ff);
}

.copy-btn {
  padding: 6px 12px;
  background: rgba(100, 100, 180, 0.1);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 6px;
  color: var(--text-secondary, #a0a0c0);
  font-size: 0.78rem;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(100, 100, 180, 0.2);
    color: #f0f0ff;
  }
}

.result-body {
  font-size: 0.88rem;
  line-height: 1.6;
  color: var(--text-secondary, #a0a0c0);

  :deep(p) {
    margin: 0 0 12px;
    &:last-child { margin-bottom: 0; }
  }

  :deep(code) {
    padding: 2px 6px;
    background: rgba(0, 0, 0, 0.3);
    border-radius: 4px;
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.85em;
  }

  :deep(pre) {
    padding: 12px;
    background: rgba(0, 0, 0, 0.3);
    border-radius: 8px;
    overflow-x: auto;
    margin: 12px 0;

    code {
      padding: 0;
      background: none;
    }
  }

  :deep(strong) {
    color: #00f5d4;
  }
}
</style>