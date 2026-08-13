<template>
  <div class="tool-call-card" :class="toolCall.status">
    <div class="tool-header">
      <span class="tool-icon">🔧</span>
      <span class="tool-name">{{ toolCall.name }}</span>
      <span class="tool-status" :class="toolCall.status">
        <span class="status-icon">{{ getStatusIcon(toolCall.status) }}</span>
        <span class="status-text">{{ getStatusText(toolCall.status) }}</span>
      </span>
    </div>
    <div v-if="toolCall.params" class="tool-params">
      <code class="params-code">{{ formatParams(toolCall.params) }}</code>
    </div>
    <div v-if="toolCall.result" class="tool-result">
      <span class="result-value">{{ toolCall.result }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  toolCall: {
    type: Object,
    required: true
  }
})

const getStatusIcon = (status) => {
  const icons = {
    pending: '⏳',
    executing: '⚡',
    completed: '✅',
    error: '❌'
  }
  return icons[status] || '⏳'
}

const getStatusText = (status) => {
  const texts = {
    pending: '等待',
    executing: '执行中',
    completed: '已完成',
    error: '失败'
  }
  return texts[status] || '未知'
}

const formatParams = (params) => {
  if (!params) return '{}'
  return JSON.stringify(params, null, 2)
}
</script>

<style lang="scss" scoped>
.tool-call-card {
  margin-top: 8px;
  background: rgba(123, 97, 255, 0.08);
  border: 1px solid rgba(123, 97, 255, 0.15);
  border-radius: 6px;
  padding: 8px 10px;
  font-family: 'JetBrains Mono', monospace;

  &.executing {
    border-color: #F59E0B;
    box-shadow: 0 0 12px rgba(245, 158, 11, 0.15);
    animation: pulse-border 1.5s ease-in-out infinite;
  }

  &.completed {
    border-color: rgba(16, 185, 129, 0.3);
  }

  &.error {
    border-color: rgba(239, 68, 68, 0.3);
  }
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tool-icon {
  font-size: 12px;
}

.tool-name {
  font-weight: 600;
  color: #7b61ff;
  font-size: 12px;
  flex: 1;
}

.tool-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;

  &.pending { color: #606090; }
  &.executing {
    color: #F59E0B;
    .status-icon { animation: spin 1s linear infinite; }
  }
  &.completed { color: #10B981; }
  &.error { color: #EF4444; }
}

.status-icon { font-size: 11px; }

.tool-params {
  margin-top: 6px;
}

.params-code {
  font-size: 11px;
  color: #a0a0c8;
  background: rgba(0, 0, 0, 0.2);
  padding: 3px 6px;
  border-radius: 3px;
  white-space: pre-wrap;
  word-break: break-all;
  display: block;
  line-height: 1.4;
}

.tool-result {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid rgba(100,100,180,0.06);
}

.result-value {
  font-size: 11px;
  color: #8080a8;
  line-height: 1.4;
}

@keyframes pulse-border {
  0%, 100% { border-color: rgba(245, 158, 11, 0.3); box-shadow: 0 0 8px rgba(245, 158, 11, 0.08); }
  50% { border-color: rgba(245, 158, 11, 0.7); box-shadow: 0 0 20px rgba(245, 158, 11, 0.2); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
