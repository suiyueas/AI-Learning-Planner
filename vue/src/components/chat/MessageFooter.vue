<template>
  <div class="message-footer">
    <div class="footer-left">
      <span v-if="message.knowledgeRef" class="status-badge knowledge">
        📚 知识库 {{ message.knowledgeRef.current }}/{{ message.knowledgeRef.total }}
        <span v-if="message.knowledgeRef.total === 0" class="status-suffix">（未接入）</span>
      </span>
      <span v-if="message.mcpRef" class="status-badge mcp">
        🔧 MCP 工具 {{ message.mcpRef.current }}/{{ message.mcpRef.total }}
        <span v-if="message.mcpRef.total === 0" class="status-suffix">（待配置）</span>
      </span>
    </div>
    <div class="footer-right">
      <span class="status-badge" :class="message.status">
        <span class="status-icon">{{ getStatusIcon(message.status) }}</span>
        {{ getStatusText(message.status) }}
      </span>
      <span v-if="message.tokens" class="token-count">
        🔑 {{ message.tokens.current }}/{{ message.tokens.total }}
      </span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: Object,
    required: true
  }
})

// 获取状态图标
const getStatusIcon = (status) => {
  const icons = {
    pending: '⏳',
    executing: '⚡',
    completed: '✅',
    error: '❌'
  }
  return icons[status] || '⏳'
}

// 获取状态文本
const getStatusText = (status) => {
  const texts = {
    pending: '等待中',
    executing: '执行中',
    completed: '就绪',
    error: '执行失败'
  }
  return texts[status] || '未知'
}
</script>

<style lang="scss" scoped>
.message-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle, #e2e8f0);
  gap: 12px;
  flex-wrap: wrap;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--bg-elevated, #f8fafc);
  border: 1px solid var(--border-subtle, #e2e8f0);
  color: #CBD5E1;

  &.knowledge {
    border-color: #64748B;
    color: #64748B;
  }

  &.mcp {
    border-color: #64748B;
    color: #64748B;
  }

  &.pending {
    color: #94A3B8;
  }

  &.executing {
    border-color: var(--accent-primary, #4f46e5);
    color: var(--accent-primary, #4f46e5);

    .status-icon {
      animation: spin 1s linear infinite;
    }
  }

  &.completed {
    border-color: #10B981;
    color: #10B981;
    background: rgba(16, 185, 129, 0.1);
  }

  &.error {
    border-color: var(--accent-red, #ef4444);
    color: var(--accent-red, #ef4444);
  }
}

.status-icon {
  font-size: 0.85rem;
}

.token-count {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  color: var(--accent-amber, #f59e0b);
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.status-suffix {
  font-size: 0.65rem;
  color: #64748B;
  margin-left: 4px;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>