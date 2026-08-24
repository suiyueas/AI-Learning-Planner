<template>
  <div class="chat-context-sidebar">
    <div class="context-section">
      <h4 class="context-title">🤖 AI 状态</h4>
      <div class="context-status">
        <span class="status-indicator" :class="aiStatusClass"></span>
        <span>{{ aiStatusText }}</span>
      </div>
    </div>

    <div class="context-section">
      <h4 class="context-title">📚 知识库</h4>
      <div class="context-stat">
        <span>文档数量</span>
        <span class="stat-value">{{ knowledgeStatus.documentCount || 0 }}</span>
      </div>
      <div class="context-stat">
        <span>就绪状态</span>
        <span class="stat-value">{{ knowledgeStatus.readyCount || 0 }}/{{ knowledgeStatus.chunkCount || 0 }}</span>
      </div>
      <div v-if="knowledgeStatus.connected" class="status-ok">
        <span class="status-dot green"></span>
        <span>已接入</span>
      </div>
      <div v-else class="status-offline">
        <span class="status-dot gray"></span>
        <span>未接入</span>
      </div>
    </div>

    <div class="context-section">
      <h4 class="context-title">🔧 MCP 服务</h4>
      <div class="context-stat">
        <span>可用工具</span>
        <span class="stat-value">{{ mcpStatus.availableCount || 0 }}</span>
      </div>
      <div class="context-stat">
        <span>调用次数</span>
        <span class="stat-value">{{ mcpStatus.totalCalls || 0 }}</span>
      </div>
    </div>

    <div class="context-section">
      <h4 class="context-title">📊 Token 用量</h4>
      <div class="token-bar">
        <div class="token-fill" :style="{ width: tokenPercent + '%' }"></div>
      </div>
      <div class="token-text">{{ tokenUsage.used || 0 }} / {{ tokenUsage.total || 30720 }}</div>
    </div>

    <div class="context-section">
      <h4 class="context-title">💡 快捷操作</h4>
      <div class="quick-actions">
        <button class="quick-action-btn" @click="$emit('new-chat')">
          <span class="qa-icon">➕</span>
          <span>新建对话</span>
        </button>
        <button class="quick-action-btn" @click="$emit('clear-chat')">
          <span class="qa-icon">🗑️</span>
          <span>清空对话</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  aiStatus: {
    type: String,
    default: 'idle'
  },
  knowledgeStatus: {
    type: Object,
    default: () => ({
      connected: false,
      documentCount: 0,
      chunkCount: 0,
      readyCount: 0
    })
  },
  mcpStatus: {
    type: Object,
    default: () => ({
      availableCount: 0,
      totalCalls: 0
    })
  },
  tokenUsage: {
    type: Object,
    default: () => ({
      used: 0,
      total: 30720
    })
  }
})

defineEmits(['new-chat', 'clear-chat'])

const aiStatusText = computed(() => {
  const statusMap = {
    idle: '就绪',
    online: '就绪',
    thinking: '思考中',
    generating: '生成中',
    searching: '搜索中',
    executing: '执行中'
  }
  return statusMap[props.aiStatus] || '就绪'
})

const aiStatusClass = computed(() => {
  const statusMap = {
    idle: 'ready',
    online: 'ready',
    thinking: 'thinking',
    generating: 'generating',
    searching: 'searching',
    executing: 'executing'
  }
  return statusMap[props.aiStatus] || 'ready'
})

const tokenPercent = computed(() => {
  if (!props.tokenUsage.total) return 0
  return Math.min((props.tokenUsage.used / props.tokenUsage.total) * 100, 100)
})
</script>

<style lang="scss" scoped>
.chat-context-sidebar {
  width: 260px;
  height: 100%;
  background: rgba(18, 18, 42, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-left: 1px solid rgba(100, 100, 180, 0.08);
  padding: 16px 12px;
  overflow-y: auto;
  flex-shrink: 0;
}

.context-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);

  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
  }
}

.context-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-primary, #f0f0ff);
  margin: 0 0 12px;
}

.context-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: var(--text-secondary, #a0a0c0);
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: statusPulse 2s ease-in-out infinite;

  &.ready {
    background: #10b981;
    box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
  }

  &.thinking, &.generating {
    background: #f59e0b;
    box-shadow: 0 0 8px rgba(245, 158, 11, 0.5);
  }

  &.searching, &.executing {
    background: #3b82f6;
    box-shadow: 0 0 8px rgba(59, 130, 246, 0.5);
  }
}

@keyframes statusPulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.9); }
}

.context-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
  color: var(--text-muted, #8b8ba8);
  margin-bottom: 8px;

  &:last-of-type {
    margin-bottom: 0;
  }
}

.stat-value {
  color: var(--text-secondary, #a0a0c0);
  font-weight: 500;
}

.status-ok, .status-offline {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 0.78rem;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.green {
    background: #10b981;
  }

  &.gray {
    background: #6b6b8b;
  }
}

.token-bar {
  height: 6px;
  background: rgba(100, 100, 180, 0.15);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.token-fill {
  height: 100%;
  background: linear-gradient(90deg, rgba(0, 245, 212, 0.6), rgba(124, 97, 255, 0.6));
  border-radius: 3px;
  transition: width 0.3s ease;
}

.token-text {
  font-size: 0.75rem;
  color: var(--text-sub, #6b6b8b);
  text-align: right;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  background: rgba(100, 100, 180, 0.08);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 8px;
  color: var(--text-secondary, #a0a0c0);
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(100, 100, 180, 0.15);
    border-color: rgba(100, 100, 180, 0.2);
    color: var(--text-primary, #f0f0ff);
  }

  .qa-icon {
    font-size: 0.9rem;
  }
}
</style>