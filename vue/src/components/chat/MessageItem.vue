<template>
  <div :class="[message.role === 'user' ? 'user-message-wrapper' : 'ai-message-wrapper']">
    <div class="message" :class="[message.role, { streaming: isStreaming }]">
      <template v-if="message.role === 'user'">
        <div class="msg-content user-msg">
          <div class="msg-bubble">{{ message.content }}</div>
        </div>
        <div class="avatar user-av">👤</div>
      </template>
      <template v-else>
        <div class="avatar ai-av">{{ roleIcon }}</div>
        <div class="msg-content ai-msg">
          <div class="msg-meta">
            <span class="ai-label">{{ roleName }}</span>
            <span class="msg-time">{{ formattedTime }}</span>
          </div>
          <div class="msg-bubble markdown-body" v-html="renderedContent"></div>
          <div v-if="message.sources && message.sources.length > 0" class="sources-section">
            <div class="sources-title">📎 引用来源</div>
            <div v-for="src in message.sources" :key="src.id" class="source-item">
              <div class="source-header">
                <span class="source-doc">{{ src.docName }}</span>
                <span class="source-relevance">相关度 {{ src.relevance }}%</span>
              </div>
              <div class="source-snippet">"{{ src.snippet }}"</div>
            </div>
          </div>
          <div v-if="message.toolCalls && message.toolCalls.length > 0" class="tool-calls-section">
            <div v-for="(tc, tcIdx) in message.toolCalls" :key="'tc_' + tcIdx" class="tool-call-card">
              <div class="tool-call-header">
                <span class="tool-call-icon">🔧</span>
                <span class="tool-call-name">正在调用工具：{{ tc.toolName || tc.name || '未知工具' }}</span>
                <span v-if="tc.success === true" class="tool-call-status success">✅ 执行完成</span>
                <span v-else-if="tc.success === false" class="tool-call-status error">❌ 执行失败</span>
                <span v-else class="tool-call-status running">🔄 执行中...</span>
              </div>
            </div>
          </div>
          <div class="msg-actions">
            <button class="act-btn" title="复制" @click="handleCopy">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1" />
              </svg>
            </button>
            <button class="act-btn" title="重新生成" @click="$emit('regenerate', message)">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 2v6h-6" />
                <path d="M3 12a9 9 0 0115-6.7L21 8" />
                <path d="M3 22v-6h6" />
                <path d="M21 12a9 9 0 01-15 6.7L3 16" />
              </svg>
            </button>
            <button class="act-btn" :class="{ liked: message.liked === true }" title="点赞" @click="$emit('like', message, true)">
              👍
            </button>
            <button class="act-btn" :class="{ liked: message.liked === false }" title="踩" @click="$emit('like', message, false)">
              👎
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import { ElMessage } from 'element-plus'

const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  roleIcon: {
    type: String,
    default: '🤖'
  },
  roleName: {
    type: String,
    default: 'AI 助手'
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

defineEmits(['copy', 'regenerate', 'like'])

const formattedTime = computed(() => {
  if (!props.message.timestamp) return ''
  const date = new Date(props.message.timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})

const renderedContent = computed(() => {
  return renderMarkdown(props.message.content || '')
})

const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<style lang="scss" scoped>
.user-message-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 8px 16px;

  .message {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    max-width: 75%;
  }

  .msg-content {
    display: flex;
    flex-direction: column;
  }

  .msg-bubble {
    padding: 12px 16px;
    background: rgba(0, 245, 212, 0.15);
    border: 1px solid rgba(0, 245, 212, 0.2);
    border-radius: 16px 16px 4px 16px;
    color: #f0f0ff;
    font-size: 0.9rem;
    line-height: 1.5;
    word-break: break-word;
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.1rem;
    flex-shrink: 0;
  }

  .user-av {
    background: rgba(0, 245, 212, 0.1);
  }
}

.ai-message-wrapper {
  display: flex;
  justify-content: flex-start;
  padding: 8px 16px;

  .message {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    max-width: 80%;
  }

  .msg-content {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .msg-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-left: 4px;
  }

  .ai-label {
    font-size: 0.78rem;
    color: var(--text-secondary, #a0a0c0);
    font-weight: 500;
  }

  .msg-time {
    font-size: 0.7rem;
    color: var(--text-sub, #6b6b8b);
  }

  .msg-bubble {
    padding: 12px 16px;
    background: rgba(124, 97, 255, 0.1);
    border: 1px solid rgba(124, 97, 255, 0.15);
    border-radius: 16px 16px 16px 4px;
    color: #f0f0ff;
    font-size: 0.9rem;
    line-height: 1.6;
    word-break: break-word;
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.1rem;
    flex-shrink: 0;
  }

  .ai-av {
    background: rgba(124, 97, 255, 0.15);
  }
}

.streaming .ai-msg .msg-bubble {
  background: rgba(124, 97, 255, 0.15);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.sources-section {
  margin-top: 8px;
  padding: 10px;
  background: rgba(100, 100, 180, 0.08);
  border-radius: 8px;
  font-size: 0.8rem;

  .sources-title {
    color: var(--text-secondary, #a0a0c0);
    margin-bottom: 8px;
    font-weight: 500;
  }

  .source-item {
    margin-bottom: 8px;
    padding: 8px;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 6px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .source-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
  }

  .source-doc {
    color: #00f5d4;
    font-weight: 500;
  }

  .source-relevance {
    color: var(--text-sub, #6b6b8b);
    font-size: 0.75rem;
  }

  .source-snippet {
    color: var(--text-muted, #8b8ba8);
    font-size: 0.78rem;
    line-height: 1.4;
  }
}

.tool-calls-section {
  margin-top: 8px;

  .tool-call-card {
    padding: 10px;
    background: rgba(100, 100, 180, 0.08);
    border: 1px solid rgba(100, 100, 180, 0.1);
    border-radius: 8px;
    margin-bottom: 6px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .tool-call-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.82rem;
  }

  .tool-call-icon {
    font-size: 0.9rem;
  }

  .tool-call-name {
    color: var(--text-primary, #f0f0ff);
    flex: 1;
  }

  .tool-call-status {
    font-size: 0.75rem;
    padding: 2px 8px;
    border-radius: 10px;

    &.success {
      background: rgba(16, 185, 129, 0.15);
      color: #10b981;
    }

    &.error {
      background: rgba(239, 68, 68, 0.15);
      color: #ef4444;
    }

    &.running {
      background: rgba(245, 158, 11, 0.15);
      color: #f59e0b;
    }
  }
}

.msg-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  opacity: 0;
  transition: opacity 0.2s ease;

  .ai-message-wrapper:hover & {
    opacity: 1;
  }
}

.act-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: rgba(100, 100, 180, 0.1);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 6px;
  color: var(--text-sub, #6b6b8b);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.8rem;

  &:hover {
    background: rgba(100, 100, 180, 0.2);
    color: var(--text-primary, #f0f0ff);
  }

  &.liked {
    background: rgba(0, 245, 212, 0.15);
    border-color: rgba(0, 245, 212, 0.3);
    color: #00f5d4;
  }
}

.markdown-body {
  :deep(p) {
    margin: 0 0 8px;
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
    margin: 8px 0;

    code {
      padding: 0;
      background: none;
    }
  }

  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 20px;
  }

  :deep(strong) {
    color: #00f5d4;
  }
}
</style>