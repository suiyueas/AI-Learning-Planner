<template>
  <div class="ai-chat-panel">
    <div class="chat-main">
      <QuickScenarios :scenarios="scenarios" @select="handleScenarioSelect" />

      <div ref="messagesRef" class="chat-messages">
        <div v-if="messages.length === 0" class="chat-empty-state">
          <div class="welcome-banner">
            <div class="welcome-avatar">{{ roleInfo.icon }}</div>
            <div class="welcome-content">
              <div class="welcome-name">{{ roleInfo.name }}</div>
              <div class="welcome-text">{{ roleGreeting }}</div>
            </div>
          </div>
          <div class="empty-title">开始你的第一次对话</div>
          <div class="empty-desc">向 AI 提问任何学习相关的问题</div>
          <div class="empty-examples">
            <button
              v-for="ex in roleExamples"
              :key="ex.id"
              class="example-card"
              @click="inputMessage = ex.text; handleSend()"
            >
              <span class="ex-icon">{{ ex.icon }}</span>
              <span class="ex-text">{{ ex.text }}</span>
            </button>
          </div>
        </div>

        <MessageItem
          v-for="msg in messages"
          :key="msg.id"
          :message="msg"
          :role-icon="roleInfo.icon"
          :role-name="roleInfo.name"
          :is-streaming="isStreaming && currentStreamingId === msg.id"
          @like="handleLike"
          @regenerate="handleRegenerate"
        />

        <div v-if="isTyping && !isStreaming && !currentStreamingId" class="ai-message-wrapper">
          <div class="message assistant">
            <div class="avatar ai-av">{{ roleInfo.icon }}</div>
            <div class="msg-content ai-msg">
              <div class="typing-dots">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <ChatInput
        v-model="inputMessage"
        :disabled="isTyping"
        :is-streaming="isStreaming"
        :placeholder="inputPlaceholder"
        @send="handleSend"
        @stop="handleStop"
      />
    </div>

    <ChatContextSidebar
      :ai-status="aiStatus"
      :knowledge-status="knowledgeStatus"
      :mcp-status="mcpStatus"
      :token-usage="tokenUsage"
      @new-chat="handleNewChat"
      @clear-chat="handleClearChat"
    />
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import { renderMarkdown } from '@/utils/markdown'
import { securityFilter } from '@/utils/securityUtils'
import { ElMessage } from 'element-plus'
import QuickScenarios from './QuickScenarios.vue'
import ChatInput from './ChatInput.vue'
import MessageItem from './MessageItem.vue'
import ChatContextSidebar from './ChatContextSidebar.vue'

const chatStore = useChatStore()
const messagesRef = ref(null)
const inputMessage = ref('')

const messages = computed(() => chatStore.messages)
const isTyping = computed(() => chatStore.isTyping)
const isStreaming = computed(() => chatStore.isStreaming)
const aiStatus = computed(() => chatStore.aiStatus)
const currentStreamingId = computed(() => chatStore.currentStreamingMessageId)
const currentRole = computed(() => chatStore.currentRole)
const knowledgeStatus = computed(() => chatStore.knowledgeStatus)
const mcpStatus = computed(() => chatStore.mcpStatus)
const tokenUsage = computed(() => chatStore.tokenUsage)

const inputPlaceholder = computed(() => {
  return `向 ${roleInfo.value.name} 提问... (Enter 发送 / Shift+Enter 换行)`
})

const scenarios = [
  { id: 1, icon: '📝', label: '制定计划', prompt: '请为我制定一个关于「在此输入学习目标」的详细学习计划，包含阶段划分、时间节点和具体任务。' },
  { id: 2, icon: '💡', label: '解释概念', prompt: '请用通俗易懂的方式解释「在此输入概念名称」，并给出实际应用举例。' },
  { id: 3, icon: '✍️', label: '生成习题', prompt: '请围绕「在此输入主题」生成一套由浅入深的练习题（5-10道），附带详细解析。' },
  { id: 4, icon: '🔍', label: '推荐资源', prompt: '请推荐关于「在此输入主题」的优质学习资源，包括书籍、视频和在线课程。' }
]

const roles = [
  { id: 'planner', icon: '🎯', name: '智能学习规划师', shortName: '规划师' },
  { id: 'expert', icon: '📚', name: '学习答疑专家', shortName: '专家' },
  { id: 'partner', icon: '🤝', name: '学习伙伴', shortName: '伙伴' }
]

const roleInfo = computed(() => roles.find(r => r.id === currentRole.value) || roles[0])

const roleGreetings = {
  planner: '你好！我是你的智能学习规划师 🎯 我可以帮你制定个性化学习计划、规划学习路径。告诉我你的学习目标，让我们一起开始吧！',
  expert: '你好！我是学习答疑专家 📚 无论多复杂的概念，我都能帮你深入理解。有什么学习上的困惑，尽管问我！',
  partner: '你好！我是你的学习伙伴 🤝 学习的路上有我陪伴，我们可以互相出题、一起总结。今天想学点什么？'
}

const roleGreeting = computed(() => roleGreetings[currentRole.value] || roleGreetings.planner)

const roleExamplesMap = {
  planner: [
    { id: 1, icon: '📝', text: '帮我制定一个Python学习计划' },
    { id: 2, icon: '🗺️', text: '规划前端开发的学习路径' },
    { id: 3, icon: '📚', text: '推荐机器学习入门资源' }
  ],
  expert: [
    { id: 1, icon: '💡', text: '解释一下什么是机器学习' },
    { id: 2, icon: '🔍', text: '对比React和Vue的异同' },
    { id: 3, icon: '🔬', text: '深入讲解Python装饰器原理' }
  ],
  partner: [
    { id: 1, icon: '📝', text: '我想学Python，该怎么开始' },
    { id: 2, icon: '✏️', text: '互相出题：JavaScript基础' },
    { id: 3, icon: '📋', text: '帮我总结今天学的SQL知识' }
  ]
}

const roleExamples = computed(() => roleExamplesMap[currentRole.value] || roleExamplesMap.planner)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const handleScenarioSelect = (scenario) => {
  inputMessage.value = scenario.prompt
  handleSend()
}

const handleSend = async () => {
  const content = inputMessage.value.trim()
  if (!content || isTyping.value) return

  const safetyResult = securityFilter.sanitize(content)
  if (safetyResult.action === 'BLOCK') {
    ElMessage.error(safetyResult.message || '输入包含不允许的内容')
    return
  }

  if (!chatStore.currentConversationId) {
    chatStore.createConversation()
  }

  const userMsgId = 'user_' + Date.now()
  const userMsg = { id: userMsgId, role: 'user', content, timestamp: new Date() }
  chatStore.addMessage(userMsg)
  inputMessage.value = ''

  chatStore.updateTokenUsage(Math.ceil(content.length / 2))
  scrollToBottom()
  chatStore.setTyping(true)

  const aiId = 'ai_' + Date.now()
  chatStore.currentStreamingMessageId = aiId
  chatStore.addMessage({ id: aiId, role: 'assistant', content: '', timestamp: new Date(), steps: [], status: 'executing' })

  try {
    const { chatAPI } = await import('@/api/chatApi')
    await chatAPI.sendMessageStream({
      message: content,
      role: currentRole.value,
      conversationId: chatStore.currentConversationId,
      onMessage: (chunk) => {
        const i = messages.value.findIndex(m => m.id === aiId)
        if (i > -1) {
          messages.value[i].content += chunk.content
          scrollToBottom()
        }
      },
      onComplete: () => {
        chatStore.setTyping(false)
        chatStore.setStreaming(false)
        chatStore.currentStreamingMessageId = null
        const i = messages.value.findIndex(m => m.id === aiId)
        if (i > -1) {
          messages.value[i].status = 'completed'
          const contentLen = messages.value[i].content?.length || 0
          if (contentLen > 0) {
            chatStore.updateTokenUsage(Math.ceil(contentLen / 2))
          }
        }
        scrollToBottom()
      },
      onError: (err) => {
        chatStore.setTyping(false)
        chatStore.setStreaming(false)
        chatStore.currentStreamingMessageId = null
        const i = messages.value.findIndex(m => m.id === aiId)
        let errorText = '抱歉，发送消息时出现了错误，请重试。'
        if (err?.message) {
          if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError')) {
            errorText = '⚠️ 无法连接到后端服务，请确认后端已启动（端口 8080）'
          } else if (err.message.includes('401') || err.message.includes('授权')) {
            errorText = '⚠️ 登录已过期，请重新登录后再试'
          } else {
            errorText = '⚠️ ' + err.message
          }
        }
        if (i > -1) { messages.value[i].content = errorText; messages.value[i].status = 'error' }
        ElMessage.error(err?.message || '发送失败')
      },
      onStatusChange: (s) => {
        chatStore.setAIStatus(s)
        if (s === 'generating') chatStore.setStreaming(true)
      }
    })
  } catch (err) {
    chatStore.setTyping(false)
    chatStore.setStreaming(false)
    chatStore.currentStreamingMessageId = null
    const i = messages.value.findIndex(m => m.id === aiId)
    if (i > -1) {
      let errorText = '⚠️ 发送失败'
      if (err?.name === 'TypeError' || err?.message?.includes('fetch')) {
        errorText = '⚠️ 无法连接到后端服务，请确认后端已启动（端口 8080）'
      } else if (err?.message) {
        errorText = '⚠️ ' + err.message
      }
      messages.value[i].content = errorText
      messages.value[i].status = 'error'
    }
  }
}

const handleStop = () => {
  chatStore.stopStreaming()
  ElMessage.info('已停止生成')
}

const handleLike = (message, liked) => {
  message.liked = liked
}

const handleRegenerate = (message) => {
  ElMessage.info('重新生成功能开发中')
}

const handleNewChat = () => {
  chatStore.createConversation()
  inputMessage.value = ''
  ElMessage.success('已创建新对话')
}

const handleClearChat = () => {
  chatStore.messages = []
  chatStore.resetTokenUsage()
  ElMessage.success('对话已清空')
}
</script>

<style lang="scss" scoped>
.ai-chat-panel {
  display: flex;
  height: 100%;
  background: rgba(10, 10, 26, 0.6);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(100, 100, 180, 0.1);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
  scroll-behavior: smooth;
}

.chat-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.welcome-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: rgba(124, 97, 255, 0.08);
  border: 1px solid rgba(124, 97, 255, 0.15);
  border-radius: 16px;
  margin-bottom: 24px;
  max-width: 600px;
}

.welcome-avatar {
  font-size: 2.5rem;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(124, 97, 255, 0.15);
  border-radius: 50%;
  flex-shrink: 0;
}

.welcome-content {
  text-align: left;
}

.welcome-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #f0f0ff;
  margin-bottom: 6px;
}

.welcome-text {
  font-size: 0.85rem;
  color: var(--text-muted, #8b8ba8);
  line-height: 1.5;
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary, #f0f0ff);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 0.85rem;
  color: var(--text-muted, #8b8ba8);
  margin-bottom: 20px;
}

.empty-examples {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  max-width: 600px;
}

.example-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(100, 100, 180, 0.08);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 12px;
  color: var(--text-secondary, #a0a0c0);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(124, 97, 255, 0.12);
    border-color: rgba(124, 97, 255, 0.25);
    color: #f0f0ff;
    transform: translateY(-2px);
  }

  .ex-icon {
    font-size: 1rem;
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

  .msg-content {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
}

.typing-dots {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: rgba(124, 97, 255, 0.1);
  border: 1px solid rgba(124, 97, 255, 0.15);
  border-radius: 16px 16px 16px 4px;

  .dot {
    width: 8px;
    height: 8px;
    background: rgba(124, 97, 255, 0.6);
    border-radius: 50%;
    animation: typingBounce 1.4s ease-in-out infinite;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8px); opacity: 1; }
}
</style>