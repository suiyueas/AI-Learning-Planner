<template>
  <!-- 最小化状态：右下角浮动按钮 -->
  <button
    v-if="isMinimized"
    class="gcb-fab"
    :class="{ 'gcb-fab--active': unreadCount > 0 }"
    @click="openPanel"
    :title="`打开 AI 助手${unreadCount > 0 ? ` (${unreadCount} 条未读)` : ''}`"
  >
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
      <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
    </svg>
    <span v-if="unreadCount > 0" class="gcb-fab-badge">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
    <span class="gcb-fab-pulse"></span>
  </button>

  <!-- 展开状态：右侧滑入面板 -->
  <Teleport to="body">
    <Transition name="gcb-drawer">
      <div v-if="!isMinimized" class="gcb-drawer">
        <!-- 面板头部 -->
        <div class="gcb-drawer-header">
          <div class="gcb-drawer-title">
            <span class="gcb-drawer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                <path d="M12 2a7 7 0 017 7c0 2.38-1.19 4.47-3 5.74V17a2 2 0 01-2 2h-4a2 2 0 01-2-2v-2.26C6.19 13.47 5 11.38 5 9a7 7 0 017-7z"/>
                <path d="M9 22h6"/>
              </svg>
            </span>
            <span>AI 助手</span>
            <span class="gcb-drawer-context">{{ currentContext }}</span>
          </div>
          <div class="gcb-drawer-actions">
            <button class="gcb-drawer-btn" @click="clearMessages" title="清空对话">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="13" height="13">
                <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
              </svg>
            </button>
            <button class="gcb-drawer-btn" @click="isMinimized = true" title="关闭">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="13" height="13">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="gcb-drawer-messages" ref="messagesRef">
          <!-- 空状态：快捷建议 -->
          <div v-if="messages.length === 0" class="gcb-drawer-empty">
            <div class="gcb-drawer-empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="36" height="36">
                <path d="M12 2a7 7 0 017 7c0 2.38-1.19 4.47-3 5.74V17a2 2 0 01-2 2h-4a2 2 0 01-2-2v-2.26C6.19 13.47 5 11.38 5 9a7 7 0 017-7z"/>
                <path d="M9 22h6"/>
              </svg>
            </div>
            <p class="gcb-drawer-empty-title">有什么可以帮你的？</p>
            <div class="gcb-drawer-suggestions">
              <button
                v-for="(suggestion, idx) in currentSuggestions"
                :key="idx"
                class="gcb-suggestion-chip"
                @click="sendQuickQuestion(suggestion.text)"
              >
                <span class="gcb-suggestion-icon">{{ suggestion.icon }}</span>
                <span class="gcb-suggestion-text">{{ suggestion.text }}</span>
              </button>
            </div>
          </div>

          <!-- 消息列表 -->
          <template v-for="msg in messages" :key="msg.id">
            <div
              :class="[
                'gcb-msg',
                `gcb-msg--${msg.role}`,
                { 'gcb-msg--streaming': msg.id === streamingMessageId && isStreaming }
              ]"
            >
              <div v-if="msg.role === 'assistant'" class="gcb-msg-avatar">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                  <path d="M12 2a7 7 0 017 7c0 2.38-1.19 4.47-3 5.74V17a2 2 0 01-2 2h-4a2 2 0 01-2-2v-2.26C6.19 13.47 5 11.38 5 9a7 7 0 017-7z"/>
                  <path d="M9 22h6"/>
                </svg>
              </div>
              <div class="gcb-msg-content">
                <div class="gcb-msg-bubble" :class="{ 'gcb-msg-bubble--typing': msg.content === '' && isStreaming }">
                  <template v-if="msg.content === '' && isStreaming">
                    <span class="gcb-typing-dot"></span>
                    <span class="gcb-typing-dot"></span>
                    <span class="gcb-typing-dot"></span>
                  </template>
                  <template v-else>
                    {{ msg.content }}
                  </template>
                </div>
                <span v-if="msg.time" class="gcb-msg-time">{{ msg.time }}</span>
              </div>
            </div>
          </template>
        </div>

        <!-- 输入区域 -->
        <div class="gcb-drawer-input">
          <div class="gcb-input-row">
            <textarea
              v-model="input"
              class="gcb-textarea"
              :placeholder="isStreaming ? 'AI 正在回复...' : '输入问题...'"
              :disabled="isStreaming"
              rows="1"
              @input="autoResizeInput"
              @keydown.enter.ctrl="sendMessage"
              @keydown.enter.meta="sendMessage"
              @keydown.enter.exact="!isStreaming && handleEnterSend"
            ></textarea>
            <div class="gcb-input-actions">
              <button
                v-if="isStreaming"
                class="gcb-btn gcb-btn--stop"
                @click="stopStreaming"
                title="停止生成"
              >
                <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                  <rect x="6" y="6" width="12" height="12" rx="2"/>
                </svg>
              </button>
              <button
                v-else
                class="gcb-btn gcb-btn--send"
                :disabled="!input.trim()"
                @click="sendMessage"
                title="发送"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                  <path d="M22 2L11 13M22 2l-7 20-4-9-9-4z" />
                </svg>
              </button>
            </div>
          </div>
          <div class="gcb-input-hint">
            <span>Ctrl+Enter 发送</span>
            <span v-if="isStreaming" class="gcb-hint-streaming">正在生成...</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { chatAPI } from '@/api/chatApi'

const props = defineProps({
  sidebarCollapsed: { type: Boolean, default: false }
})

const route = useRoute()

// ===== 面板状态 =====
const isMinimized = ref(true)
const input = ref('')
const messages = ref([])
const messagesRef = ref(null)
const isStreaming = ref(false)
const streamingMessageId = ref(null)
const unreadCount = ref(0)
let messageCounter = 0

// ===== 上下文感知 =====
const currentContext = computed(() => {
  const path = route.path
  if (path.startsWith('/study-notes')) return '学习笔记'
  if (path.startsWith('/code-analyze')) return '代码分析'
  if (path.startsWith('/calendar')) return '学习日历'
  if (path.startsWith('/achievements')) return '成就'
  if (path.startsWith('/profile')) return '个人中心'
  if (path.startsWith('/knowledge')) return '知识库'
  if (path.startsWith('/agents')) return '智能体'
  return '通用'
})

// ===== 上下文感知的快捷建议 =====
const currentSuggestions = computed(() => {
  const path = route.path
  if (path.startsWith('/study-notes')) {
    return [
      { icon: '📝', text: '帮我总结这篇笔记' },
      { icon: '💡', text: '如何高效做笔记？' },
      { icon: '🔗', text: '推荐相关学习资源' },
      { icon: '📋', text: '生成复习提纲' }
    ]
  }
  if (path.startsWith('/code-analyze')) {
    return [
      { icon: '🔍', text: '帮我优化这段代码' },
      { icon: '🐛', text: '代码有什么bug？' },
      { icon: '📖', text: '解释这段代码的原理' },
      { icon: '⚡', text: '如何提高代码性能？' }
    ]
  }
  if (path.startsWith('/calendar')) {
    return [
      { icon: '📅', text: '帮我规划本周学习' },
      { icon: '📊', text: '分析我的学习规律' },
      { icon: '🎯', text: '如何坚持学习计划？' },
      { icon: '💪', text: '给我一些学习动力' }
    ]
  }
  if (path.startsWith('/achievements')) {
    return [
      { icon: '🏆', text: '如何获得更多成就？' },
      { icon: '📈', text: '分析我的学习数据' },
      { icon: '🎯', text: '下一步学习建议' },
      { icon: '💡', text: '学习技巧分享' }
    ]
  }
  if (path.startsWith('/profile')) {
    return [
      { icon: '📚', text: '推荐适合我的课程' },
      { icon: '📊', text: '分析我的学习画像' },
      { icon: '🎯', text: '制定学习目标' },
      { icon: '💡', text: '如何提升学习效率？' }
    ]
  }
  if (path.startsWith('/knowledge')) {
    return [
      { icon: '🔍', text: '帮我搜索知识库' },
      { icon: '📄', text: '如何更好地管理文档？' },
      { icon: '💡', text: '推荐相关知识块' },
      { icon: '📚', text: '知识库使用技巧' }
    ]
  }
  if (path.startsWith('/agents')) {
    return [
      { icon: '🤖', text: '推荐合适的智能体' },
      { icon: '💡', text: '智能体使用技巧' },
      { icon: '🔗', text: '如何组合多个智能体？' },
      { icon: '🚀', text: '智能体最佳实践' }
    ]
  }
  // 通用建议
  return [
    { icon: '💡', text: '给我一些学习建议' },
    { icon: '📚', text: '推荐学习资源' },
    { icon: '🎯', text: '如何制定学习计划？' },
    { icon: '❓', text: '解答一个学习问题' }
  ]
})

// ===== 打开面板 =====
const openPanel = () => {
  isMinimized.value = false
  unreadCount.value = 0
}

// ===== 添加上下文指示 =====
watch(currentContext, (ctx) => {
  if (!isMinimized.value && messages.value.length > 0) {
    addSystemMessage(`已切换到「${ctx}」上下文，可以向我提问`)
  }
})

// ===== 消息管理 =====
const addSystemMessage = (content) => {
  const now = new Date()
  const time = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
  messages.value.push({
    id: ++messageCounter,
    role: 'system',
    content,
    time
  })
  scrollToBottom()
}

const addMessage = (role, content) => {
  const now = new Date()
  const time = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
  messages.value.push({
    id: ++messageCounter,
    role,
    content,
    time
  })
  scrollToBottom()
}

// ===== 发送消息 =====
const sendMessage = async () => {
  if (!input.value.trim() || isStreaming.value) return
  const text = input.value.trim()
  input.value = ''

  // 添加用户消息
  addMessage('user', text)

  // 添加空的 AI 消息占位
  const aiMsgId = ++messageCounter
  const now = new Date()
  messages.value.push({
    id: aiMsgId,
    role: 'assistant',
    content: '',
    time: `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
  })
  isStreaming.value = true
  streamingMessageId.value = aiMsgId
  scrollToBottom()

  try {
    // 接入真实 chatAPI 流式接口
    await chatAPI.sendMessageStream({
      message: text,
      role: 'planner',
      onMessage: (chunk) => {
        const msg = messages.value.find(m => m.id === aiMsgId)
        if (msg) {
          msg.content += chunk.content || chunk
          scrollToBottom()
        }
      },
      onComplete: () => {
        isStreaming.value = false
        streamingMessageId.value = null
        // 如果没有内容，设置默认回复
        const msg = messages.value.find(m => m.id === aiMsgId)
        if (msg && !msg.content.trim()) {
          msg.content = '收到你的消息，我已经记录下来了。'
        }
        scrollToBottom()
      },
      onError: (error) => {
        console.error('AI 回复失败:', error)
        const msg = messages.value.find(m => m.id === aiMsgId)
        if (msg) {
          if (!msg.content.trim()) {
            msg.content = '抱歉，我暂时无法回复，请稍后重试。'
          } else {
            msg.content += '\n\n> ⚠️ 回复被中断，请重试。'
          }
        }
        isStreaming.value = false
        streamingMessageId.value = null
        scrollToBottom()
      }
    })
  } catch (e) {
    console.error('发送消息失败:', e)
    const msg = messages.value.find(m => m.id === aiMsgId)
    if (msg && !msg.content.trim()) {
      msg.content = '抱歉，发生了错误，请稍后重试。'
    }
    isStreaming.value = false
    streamingMessageId.value = null
    scrollToBottom()
  }
}

// ===== 快捷提问 =====
const sendQuickQuestion = (text) => {
  input.value = text
  sendMessage()
}

// ===== 停止流式 =====
const stopStreaming = () => {
  chatAPI.stopStreaming()
  isStreaming.value = false
  streamingMessageId.value = null
}

// ===== 回车发送 =====
const handleEnterSend = (e) => {
  e.preventDefault()
  sendMessage()
}

// ===== 自动调整输入框高度 =====
const autoResizeInput = (e) => {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

// ===== 滚动到底部 =====
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// ===== 清空消息 =====
const clearMessages = () => {
  messages.value = []
  unreadCount.value = 0
}

// ===== 快捷键 =====
const handleKeydown = (e) => {
  // Cmd+K / Ctrl+K 切换面板
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    if (isMinimized.value) {
      openPanel()
    } else {
      isMinimized.value = true
    }
  }
  // Esc 关闭面板
  if (e.key === 'Escape' && !isMinimized.value) {
    isMinimized.value = true
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

/* ===== 右下角浮动按钮 ===== */
.gcb-fab {
  position: fixed;
  bottom: $space-8;
  right: $space-8;
  z-index: 1000;
  width: 48px;
  height: 48px;
  background: rgba($accent-indigo, 0.35);
  border: 1px solid rgba($accent-indigo, 0.25);
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-normal;
  box-shadow: 0 4px 16px rgba($accent-indigo, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  overflow: visible;

  &:hover {
    transform: scale(1.08) translateY(-2px);
    background: rgba($accent-indigo, 0.55);
    box-shadow: 0 6px 24px rgba($accent-indigo, 0.25);
  }

  &:active {
    transform: scale(0.95);
    background: rgba($accent-indigo, 0.7);
  }

  .gcb-fab-pulse {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: rgba($accent-indigo, 0.12);
    animation: fab-pulse 2.5s ease-out infinite;
  }
}

.gcb-fab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: $color-danger;
  color: white;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  z-index: 1;
}

@keyframes fab-pulse {
  0% { transform: scale(1); opacity: 0.4; }
  100% { transform: scale(1.6); opacity: 0; }
}

/* ===== 右侧滑入面板 ===== */
.gcb-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  width: 380px;
  max-width: calc(100vw - 16px);
  background: rgba($bg-surface, 0.97);
  border-left: 1px solid rgba($border-default, 0.5);
  box-shadow: -8px 0 40px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* ===== 面板头部 ===== */
.gcb-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  border-bottom: 1px solid rgba($border-subtle, 0.5);
  flex-shrink: 0;
  min-height: 48px;
}

.gcb-drawer-title {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
  min-width: 0;
}

.gcb-drawer-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, rgba($accent-indigo, 0.2), rgba($accent-indigo, 0.1));
  border-radius: $radius-md;
  color: $accent-indigo;
  flex-shrink: 0;
}

.gcb-drawer-context {
  font-size: $text-xs;
  color: $text-muted;
  margin-left: auto;
  padding: 2px 8px;
  background: rgba($accent-indigo, 0.06);
  border-radius: $radius-full;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.gcb-drawer-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

.gcb-drawer-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: $radius-md;
  color: $text-muted;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }
}

/* ===== 消息区域 ===== */
.gcb-drawer-messages {
  flex: 1;
  overflow-y: auto;
  padding: $space-3 $space-4;
  display: flex;
  flex-direction: column;
  gap: $space-2;
  @include custom-scrollbar;
}

/* ===== 空状态 ===== */
.gcb-drawer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: $space-4;
  gap: $space-4;
}

.gcb-drawer-empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, rgba($accent-indigo, 0.08), rgba($accent-indigo, 0.03));
  border-radius: 50%;
  color: $accent-indigo;
}

.gcb-drawer-empty-title {
  font-size: $text-base;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

/* ===== 快捷建议 ===== */
.gcb-drawer-suggestions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $space-2;
  width: 100%;
  max-width: 320px;
}

.gcb-suggestion-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-1;
  padding: $space-3 $space-2;
  background: rgba($bg-elevated, 0.5);
  border: 1px solid rgba($border-default, 0.3);
  border-radius: $radius-lg;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;
  text-align: center;

  &:hover {
    background: rgba($accent-indigo, 0.08);
    border-color: rgba($accent-indigo, 0.2);
    color: $text-primary;
    transform: translateY(-1px);
  }
}

.gcb-suggestion-icon {
  font-size: 1.2rem;
  line-height: 1;
}

.gcb-suggestion-text {
  font-size: $text-xs;
  line-height: $leading-tight;
  word-break: break-all;
}

/* ===== 消息气泡 ===== */
.gcb-msg {
  display: flex;
  gap: $space-2;
  max-width: 90%;
  animation: msg-slide-in 0.2s ease both;

  &--user {
    margin-left: auto;
    flex-direction: row-reverse;
  }

  &--assistant { margin-right: auto; }

  &--system {
    max-width: 100%;
    margin: $space-1 auto;
    justify-content: center;
    opacity: 0.7;
  }
}

.gcb-msg-avatar {
  width: 24px;
  height: 24px;
  border-radius: $radius-sm;
  background: linear-gradient(135deg, $accent-indigo, $accent-cyan);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
}

.gcb-msg-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.gcb-msg-bubble {
  padding: $space-2 $space-3;
  border-radius: $radius-md;
  font-size: $text-sm;
  line-height: $leading-relaxed;
  word-break: break-word;
  white-space: pre-wrap;

  .gcb-msg--user & {
    background: rgba($accent-indigo, 0.12);
    color: $text-primary;
    border: 1px solid rgba($accent-indigo, 0.15);
    border-bottom-right-radius: $radius-xs;
  }

  .gcb-msg--assistant & {
    background: rgba($bg-elevated, 0.6);
    color: $text-primary;
    border: 1px solid rgba($border-default, 0.3);
    border-bottom-left-radius: $radius-xs;
  }

  .gcb-msg--system & {
    background: transparent;
    color: $text-muted;
    font-size: $text-xs;
    text-align: center;
    padding: $space-1 $space-2;
  }
}

.gcb-msg-bubble--typing {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: $space-3 $space-4;
  min-width: 48px;
  justify-content: center;
}

.gcb-typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $accent-indigo;
  animation: typing-dot 1.4s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes typing-dot {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

.gcb-msg-time {
  font-size: 10px;
  color: $text-muted;
  padding: 0 $space-1;
}

.gcb-msg--streaming .gcb-msg-bubble::after {
  content: '▍';
  animation: cursor-blink 0.8s step-end infinite;
  color: $accent-indigo;
}

@keyframes cursor-blink {
  50% { opacity: 0; }
}

@keyframes msg-slide-in {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 输入区域 ===== */
.gcb-drawer-input {
  padding: $space-3 $space-4;
  border-top: 1px solid rgba($border-subtle, 0.5);
  flex-shrink: 0;
  background: rgba($bg-elevated, 0.2);
}

.gcb-input-row {
  display: flex;
  align-items: flex-end;
  gap: $space-2;
  background: rgba($bg-elevated, 0.6);
  border: 1px solid rgba($border-default, 0.4);
  border-radius: $radius-lg;
  padding: $space-2;
  transition: border-color $transition-fast;

  &:focus-within {
    border-color: rgba($accent-indigo, 0.4);
  }
}

.gcb-textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: $text-primary;
  font-size: $text-sm;
  font-family: $font-sans;
  line-height: $leading-normal;
  max-height: 120px;
  min-height: 20px;
  padding: 2px 0;

  &::placeholder {
    color: $text-muted;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.gcb-input-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.gcb-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  flex-shrink: 0;

  &--send {
    background: $accent-indigo;
    color: white;

    &:hover:not(:disabled) {
      background: $accent-indigo-light;
      transform: scale(1.05);
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  }

  &--stop {
    background: rgba($color-danger, 0.15);
    color: $color-danger;

    &:hover {
      background: rgba($color-danger, 0.25);
    }
  }
}

.gcb-input-hint {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: $space-1;
  padding: 0 $space-1;
  font-size: 10px;
  color: $text-muted;
}

.gcb-hint-streaming {
  color: $accent-indigo;
  animation: hint-pulse 1.5s ease-in-out infinite;
}

@keyframes hint-pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* ===== 动画 ===== */
.gcb-drawer-enter-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease;
}

.gcb-drawer-leave-active {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.15s ease;
}

.gcb-drawer-enter-from,
.gcb-drawer-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* ===== 响应式 ===== */
@media (max-width: $breakpoint-md) {
  .gcb-fab {
    bottom: $space-6;
    right: $space-6;
    width: 44px;
    height: 44px;
  }

  .gcb-drawer {
    width: 100vw;
    max-width: 100vw;
  }

  .gcb-drawer-suggestions {
    grid-template-columns: 1fr 1fr;
  }
}
</style>