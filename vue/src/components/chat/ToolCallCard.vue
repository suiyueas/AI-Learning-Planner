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
@use '@/styles/variables' as *;

.tool-call-card {
  margin-top: 8px;
  background: rgba($accent-primary, 0.08);
  border: 1px solid rgba($accent-primary, 0.15);
  border-radius: 6px;
  padding: 8px 10px;
  font-family: $font-mono;

  &.executing {
    border-color: $accent-amber;
    box-shadow: 0 0 12px rgba($accent-amber, 0.15);
    animation: pulse-border 1.5s ease-in-out infinite;
  }

  &.completed {
    border-color: rgba($accent-emerald, 0.3);
  }

  &.error {
    border-color: rgba($accent-red, 0.3);
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
  color: $accent-primary;
  font-size: 12px;
  flex: 1;
}

.tool-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;

  &.pending { color: $text-muted; }
  &.executing {
    color: $accent-amber;
    .status-icon { animation: spin 1s linear infinite; }
  }
  &.completed { color: $accent-emerald; }
  &.error { color: $accent-red; }
}

.status-icon { font-size: 11px; }

.tool-params {
  margin-top: 6px;
}

.params-code {
  font-size: 11px;
  color: $text-secondary;
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
  border-top: 1px solid rgba($text-muted, 0.06);
}

.result-value {
  font-size: 11px;
  color: $text-muted;
  line-height: 1.4;
}

@keyframes pulse-border {
  0%, 100% { border-color: rgba($accent-amber, 0.3); box-shadow: 0 0 8px rgba($accent-amber, 0.08); }
  50% { border-color: rgba($accent-amber, 0.7); box-shadow: 0 0 20px rgba($accent-amber, 0.2); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>