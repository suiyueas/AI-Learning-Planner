<template>
  <div ref="chatPageRef" class="chat-page" @mousemove="handleMouseMove" @mouseleave="handleMouseLeave" @mouseenter="handleMouseEnter">
    <!-- 背景层 -->
    <div class="bg-layer">
      <!-- 极光背景 -->
      <div class="aurora-bg">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <!-- 科技网格 -->
      <div class="grid-bg"></div>
      <!-- 鼠标光晕 -->
      <div class="mouse-glow" :class="{ visible: isMouseInside }" :style="glowStyle"></div>
      <!-- 动态浮动光晕 -->
      <div class="floating-glow fg-cyan"></div>
      <div class="floating-glow fg-purple"></div>
      <!-- 粒子效果 -->
      <div class="bg-particles">
        <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></div>
      </div>
    </div>

    <!-- 顶部标题栏 -->
    <header class="chat-header">
      <div class="hdr-left">
        <button class="icon-btn" title="对话历史" @click="showHistory = !showHistory">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
        </button>
        <span class="hdr-title">🤖 AI 智能学习助手</span>
      </div>
      <div class="hdr-right">
        <ModelSwitcher />
        <button class="icon-btn accent" title="新建对话" @click="createNewConversation">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
        </button>
      </div>
    </header>

    <!-- 对话历史抽屉 -->
    <transition name="history-slide">
      <div v-show="showHistory" class="history-overlay" @click="showHistory = false">
        <div class="history-panel" @click.stop>
          <div class="history-head">
            <span>对话历史</span>
            <button class="icon-btn-sm" @click="showHistory = false">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
          <button class="history-new" @click="createNewConversation(); showHistory = false">+ 新建对话</button>
          <div class="history-list">
            <div v-for="conv in chatStore.conversations" :key="conv.id" class="hist-item" :class="{ active: conv.id === currentConversationId }" @click="switchConversation(conv.id); showHistory = false">
              <span class="hist-title">{{ conv.title }}</span>
              <button class="hist-del" @click.stop="chatStore.deleteConversation(conv.id)">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 角色 + 快捷指令条 -->
    <div class="opts-bar">
      <div class="role-pills">
        <button v-for="role in roles" :key="role.id" class="role-pill" :class="{ active: currentRole === role.id }" @click="setRole(role.id)">
          {{ role.icon }} {{ role.shortName }}
        </button>
      </div>
      <div class="quick-pills">
        <button v-for="cmd in roleCommands" :key="cmd.id" class="qpill" :class="{ pri: cmd.pri }" :disabled="isTyping" @click="executeQuickCommand(cmd)">
          {{ cmd.icon }} {{ cmd.text }}
        </button>
      </div>
    </div>

    <!-- 主体区域：左面板 + 主对话区 -->
    <div class="chat-body" :class="{ 'panel-collapsed': !panelVisible }">
      <!-- 执行信息面板 -->
      <div v-show="panelVisible" class="execution-panel-wrapper" :style="{ width: panelWidth + 'px', minWidth: panelWidth + 'px' }">
        <ExecutionPanel
          :knowledge-status="chatStore.knowledgeStatus"
          :mcp-status="chatStore.mcpStatus"
          :tokens="contextTokens"
          :current-knowledge-ref="currentKnowledgeRef"
          :ai-status-text="aiStatusText"
          :web-search-enabled="settingsStore.webSearchEnabled"
          @toggle-web-search="settingsStore.toggleWebSearch()"
          @open-knowledge="showDocSelector = true"
          @open-tools="showToolSelector = true"
        />
      </div>

      <!-- 主对话区 -->
      <div class="main-area">
        <!-- 消息列表 -->
        <div ref="messagesRef" class="chat-main">
          <!-- 空白状态引导（仅当无消息时显示） -->
          <div v-if="messages.length === 0" class="empty-state">
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
              <button v-for="ex in roleExamples" :key="ex.id" class="example-card" @click="inputMessage = ex.text; nextTick(() => sendMessage())">
                <span class="ex-icon">{{ ex.icon }}</span>
                <span class="ex-text">{{ ex.text }}</span>
              </button>
            </div>
          </div>

          <!-- 消息 -->
          <div v-for="msg in messages" :key="msg.id" :class="[msg.role === 'user' ? 'user-message-wrapper' : 'ai-message-wrapper']">
            <div class="message" :class="[msg.role, { streaming: isStreaming && currentStreamingMessageId === msg.id }]">
              <template v-if="msg.role === 'user'">
                <div class="msg-content user-msg"><div class="msg-bubble">{{ msg.content }}</div></div>
                <div class="avatar user-av">👤</div>
              </template>
              <template v-else>
                <div class="avatar ai-av">{{ roleInfo.icon }}</div>
                <div class="msg-content ai-msg">
                  <div class="msg-meta"><span class="ai-label">{{ roleInfo.name }}</span><span class="msg-time">{{ formatTime(msg.timestamp) }}</span></div>
                  <div class="msg-bubble markdown-body" v-html="renderMd(msg.content)"></div>
                  <!-- 知识库引用来源 -->
                  <div v-if="msg.sources && msg.sources.length > 0" class="sources-section">
                    <div class="sources-title">📎 引用来源</div>
                    <div v-for="src in msg.sources" :key="src.id" class="source-item">
                      <div class="source-header">
                        <span class="source-doc">{{ src.docName }}</span>
                        <span class="source-relevance">相关度 {{ src.relevance }}%</span>
                      </div>
                      <div class="source-snippet">“{{ src.snippet }}”</div>
                    </div>
                  </div>
                  <!-- 工具调用卡片 -->
                  <div v-if="msg.toolCalls && msg.toolCalls.length > 0" class="tool-calls-section">
                    <div v-for="(tc, tcIdx) in msg.toolCalls" :key="'tc_' + tcIdx" class="tool-call-card">
                      <div class="tool-call-header">
                        <span class="tool-call-icon">🔧</span>
                        <span class="tool-call-name">正在调用工具：{{ tc.toolName || tc.name || '未知工具' }}</span>
                        <span v-if="tc.success === true" class="tool-call-status success">✅ 执行完成</span>
                        <span v-else-if="tc.success === false" class="tool-call-status error">❌ 执行失败</span>
                        <span v-else class="tool-call-status running">🔄 执行中...</span>
                      </div>
                      <div v-if="tc.params" class="tool-call-params">
                        <span class="params-label">参数：</span>
                        <code>{{ JSON.stringify(tc.params) }}</code>
                      </div>
                      <div v-if="tc.executionTime" class="tool-call-duration">耗时 {{ tc.executionTime }}ms</div>
                      <div v-if="tc.message" class="tool-call-result">{{ tc.message }}</div>
                    </div>
                  </div>
                <!-- 思考步骤：任务完成后自动折叠（collapsed=true），执行中保持展开 -->
                <ThinkingSteps v-if="msg.steps && msg.steps.length > 0" :steps="msg.steps" :collapsed="msg.status !== 'executing' && msg.status !== 'running'" />
                <MessageFooter v-if="msg.status" :message="msg" />
                <div v-if="msg.skills && msg.skills.length > 0" class="msg-skills">
                  <span class="sk-head" @click="toggleSkills(msg.id)">⚡ 活跃技能 {{ expandedSkills[msg.id] ? '收起' : '展开' }}</span>
                  <div v-show="expandedSkills[msg.id]" class="sk-list">
                    <span v-for="s in msg.skills" :key="s.id" class="sk-tag">{{ s.name }} {{ s.weight }}%</span>
                  </div>
                </div>
                <div class="msg-actions">
                  <button class="act-btn" title="复制" @click="handleCopyMessage(msg)">📋</button>
                  <button class="act-btn" title="重新生成" @click="handleRegenerateMessage(msg)">🔄</button>
                  <button class="act-btn" :class="{ liked: msg.liked === true }" title="点赞" @click="handleLikeMessage(msg)">👍</button>
                  <button class="act-btn" :class="{ liked: msg.liked === false }" title="踩" @click="handleDislikeMessage(msg)">👎</button>
                </div>
              </div>
              </template>
            </div>
          </div>
          <!-- 打字指示器（仅在有流式消息时不显示，避免重复头像） -->
          <div v-if="isTyping && !isStreaming && !currentStreamingMessageId" class="ai-message-wrapper">
            <div class="message assistant">
              <div class="avatar ai-av">{{ roleInfo.icon }}</div>
              <div class="msg-content ai-msg"><TypingIndicator :is-streaming="false" :show-dots="true" /></div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-row">
            <div class="plus-btn" @click="showPlusMenu = !showPlusMenu"><span>+</span></div>
            <div v-if="showPlusMenu" class="plus-menu">
              <div class="pm-item" @click="handleUpload">📎 上传资料</div>
              <div class="pm-item" @click="handleVoice">🎤 语音输入</div>
            </div>
            <textarea ref="textareaRef" v-model="inputMessage" placeholder="输入问题... (Enter 发送 / Shift+Enter 换行)" maxlength="10000" :disabled="isTyping" class="msg-input" rows="1" @keydown.enter.exact.prevent="sendMessage" @input="autoResize"></textarea>
            <button v-if="isStreaming" class="stop-btn" @click="stopGeneration">停止</button>
            <button v-else :disabled="isTyping || !inputMessage.trim()" class="send-btn" @click="sendMessage">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" /></svg>
            </button>
          </div>
          <div class="input-disclaimer">本服务生成内容由 AI 提供，仅供参考，不构成专业建议。</div>
        </div>
</div>
    </div>

    <!-- 底部详情抽屉 -->
    <DetailDrawer :visible="showDrawer" :knowledge-refs="currentKnowledgeRefs" :search-results="currentSearchResults" :skills="currentSkills" :context-tokens="contextTokens" :web-search="settingsStore.webSearchEnabled" @close="showDrawer = false" @toggle-search="handleToggleSearch" />

    <!-- 知识库文档选择面板 -->
    <DocSelectorPanel
      :visible="showDocSelector"
      @close="showDocSelector = false"
      @apply="onDocApply"
    />

    <!-- MCP 工具选择面板 -->
    <ToolSelectorPanel
      :visible="showToolSelector"
      @close="showToolSelector = false"
      @apply="onToolApply"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, onActivated, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import { useContextStore } from '@/stores/contextStore'
import { useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'
import { chatAPI } from '@/api/chatApi'
import { ElMessage } from 'element-plus'
import TypingIndicator from '@/components/TypingIndicator.vue'
import ThinkingSteps from '@/components/chat/ThinkingSteps.vue'
import MessageFooter from '@/components/chat/MessageFooter.vue'
import DetailDrawer from '@/components/chat/DetailDrawer.vue'
import ExecutionPanel from '@/components/chat/ExecutionPanel.vue'
import DocSelectorPanel from '@/components/chat/DocSelectorPanel.vue'
import ToolSelectorPanel from '@/components/chat/ToolSelectorPanel.vue'
import ModelSwitcher from '@/components/ModelSwitcher.vue'
import { useModelStore } from '@/stores/modelStore'
import { useToolsStore } from '@/stores/toolsStore'
import { renderMarkdown } from '@/utils/markdown'
import { securityFilter } from '@/utils/securityUtils'

const chatStore = useChatStore()
const contextStore = useContextStore()
const modelStore = useModelStore()
const toolsStore = useToolsStore()
const settingsStore = useSettingsStore()
const authStore = useAuthStore()
const chatPageRef = ref(null)
const inputMessage = ref('')
const messagesRef = ref()
const textareaRef = ref()
const showDrawer = ref(false)
const showPlusMenu = ref(false)
const showHistory = ref(false)
const expandedSkills = ref({})
const panelVisible = ref(true)
const panelWidth = ref(190)

// 选择面板可见性
const showDocSelector = ref(false)
const showToolSelector = ref(false)

// ===== 鼠标光晕 =====
const glowPosition = reactive({ x: -300, y: -300 })
const isMouseInside = ref(false)
let rafId = null

const handleMouseMove = (e) => {
  if (rafId) return
  rafId = requestAnimationFrame(() => {
    const rect = chatPageRef.value?.getBoundingClientRect()
    if (rect) {
      glowPosition.x = e.clientX - rect.left
      glowPosition.y = e.clientY - rect.top
    }
    rafId = null
  })
}

const handleMouseLeave = () => { isMouseInside.value = false }
const handleMouseEnter = () => { isMouseInside.value = true }

const glowStyle = computed(() => ({
  left: glowPosition.x + 'px',
  top: glowPosition.y + 'px'
}))

// ============= 粒子效果 =============
const particleStyle = () => {
  const size = Math.random() * 3 + 1
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: size + 'px',
    height: size + 'px',
    animationDuration: (Math.random() * 20 + 15) + 's',
    animationDelay: (Math.random() * 10) + 's',
    opacity: Math.random() * 0.3 + 0.1
  }
}

// 响应式逻辑
const checkResponsive = () => {
  const w = window.innerWidth
  if (w > 1200) {
    panelWidth.value = 190
    panelVisible.value = true
  } else if (w > 1024) {
    panelWidth.value = 160
    panelVisible.value = true
  } else {
    panelVisible.value = false
  }
}

const currentConversationId = computed(() => chatStore.currentConversationId)
const currentRole = computed(() => chatStore.currentRole)
const messages = computed(() => chatStore.messages)
const isTyping = computed(() => chatStore.isTyping)
const isStreaming = computed(() => chatStore.isStreaming)
const aiStatus = computed(() => chatStore.aiStatus)
const roleInfo = computed(() => chatStore.roleInfo)
const currentStreamingMessageId = computed(() => chatStore.currentStreamingMessageId)
const aiStatusText = computed(() => ({ idle: '就绪', online: '就绪', thinking: '思考中', generating: '生成中', searching: '搜索中', executing: '执行中' })[aiStatus.value] || '就绪')
const contextTokens = computed(() => ({
  current: Math.min(chatStore.tokenUsage.used, chatStore.tokenUsage.total),
  total: chatStore.tokenUsage.total
}))

const currentKnowledgeRef = computed(() => {
  if (!messages.value || messages.value.length === 0) return ''
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg && lastMsg.knowledgeRef && lastMsg.knowledgeRef.source) {
    return lastMsg.knowledgeRef.source
  }
  return ''
})

const roles = [
  { id: 'planner', icon: '🎯', name: '智能学习规划师', shortName: '规划师' },
  { id: 'expert', icon: '📚', name: '学习答疑专家', shortName: '专家' },
  { id: 'partner', icon: '🤝', name: '学习伙伴', shortName: '伙伴' }
]

// 角色差异化快捷指令
const commandsByRole = {
  planner: [
    { id: 'plan', icon: '📝', text: '制定计划', pri: true, prompt: '请为我制定一个关于「在此输入学习目标」的详细学习计划，包含阶段划分、时间节点和具体任务。' },
    { id: 'explain', icon: '💡', text: '解释概念', pri: true, prompt: '请用通俗易懂的方式解释「在此输入概念名称」，并给出实际应用举例。' },
    { id: 'quiz', icon: '✏️', text: '生成习题', pri: false, prompt: '请围绕「在此输入主题」生成一套由浅入深的练习题（5-10道），附带详细解析。' },
    { id: 'resource', icon: '📚', text: '推荐资源', pri: false, prompt: '请推荐关于「在此输入主题」的优质学习资源，包括书籍、视频和在线课程。' }
  ],
  expert: [
    { id: 'explain', icon: '💡', text: '解释概念', pri: true, prompt: '请深入讲解「在此输入概念名称」的原理、背景知识和实际应用，尽量详细。' },
    { id: 'compare', icon: '🔍', text: '对比分析', pri: true, prompt: '请对比分析「在此输入两个主题」的异同点，从原理、使用场景、优缺点等方面进行。' },
    { id: 'quiz', icon: '✏️', text: '生成习题', pri: false, prompt: '请围绕「在此输入主题」生成一套有挑战性的专业练习题（5-10道），附带详细解析。' },
    { id: 'deep', icon: '🔬', text: '深入探讨', pri: false, prompt: '请就「在此输入话题」展开深入探讨，包括前沿研究和发展趋势。' }
  ],
  partner: [
    { id: 'plan', icon: '📝', text: '制定计划', pri: true, prompt: '我想学习「在此输入学习目标」，帮我规划一下该怎么开始，我们一起加油！' },
    { id: 'explain', icon: '💡', text: '解释概念', pri: true, prompt: '我不太理解「在此输入概念名称」，能用简单的话给我讲讲吗？' },
    { id: 'quiz', icon: '✏️', text: '互相出题', pri: false, prompt: '我们来做个小测验吧，请围绕「在此输入主题」出几道题考考我。' },
    { id: 'review', icon: '📋', text: '今日总结', pri: false, prompt: '请帮我回顾今天的学习内容，总结重点，看看还有哪些需要巩固。' }
  ]
}

// 当前角色对应的指令列表
const roleCommands = computed(() => commandsByRole[currentRole.value] || commandsByRole.planner)

// 角色差异化问候语
const roleGreetings = {
  planner: '你好！我是你的智能学习规划师 🎯 我可以帮你制定个性化学习计划、规划学习路径。告诉我你的学习目标，让我们一起开始吧！',
  expert: '你好！我是学习答疑专家 📚 无论多复杂的概念，我都能帮你深入理解。有什么学习上的困惑，尽管问我！',
  partner: '你好！我是你的学习伙伴 🤝 学习的路上有我陪伴，我们可以互相出题、一起总结。今天想学点什么？'
}
const roleGreeting = computed(() => roleGreetings[currentRole.value] || roleGreetings.planner)

// 角色差异化示例问题
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
const currentKnowledgeRefs = ref([{ id: 'ref_1', source: 'Python数据分析指南.pdf - 第3章', content: 'Python提供了丰富的数据分析库', relevance: 0.95 }])
const currentSearchResults = ref([{ id: 'search_1', title: 'Python学习资源推荐', url: 'https://example.com/python-guide', snippet: '包含从入门到精通的Python学习路径' }])
const currentSkills = ref([{ id: 'skill_1', name: '知识检索', weight: 80 }, { id: 'skill_2', name: '路径规划', weight: 70 }, { id: 'skill_3', name: '自动摘要', weight: 60 }, { id: 'skill_4', name: '概念解释', weight: 75 }])

const formatTime = (d) => { if (!d) return ''; return new Date(d).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }
const scrollToBottom = () => { nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight }) }
const autoResize = () => { const el = textareaRef.value; if (!el) return; el.style.height = 'auto'; el.style.height = Math.min(el.scrollHeight, 100) + 'px' }
const toggleSkills = (id) => { expandedSkills.value[id] = !expandedSkills.value[id] }
const createNewConversation = () => { chatStore.createConversation() }
const switchConversation = (id) => { chatStore.switchConversation(id) }
const setRole = (r) => { chatStore.setRole(r) }
const executeQuickCommand = (cmd) => { if (isTyping.value) return; inputMessage.value = cmd.prompt; nextTick(() => { autoResize(); textareaRef.value?.focus(); textareaRef.value?.select(); }) }
const stopGeneration = () => { chatStore.stopStreaming(); chatAPI.stopStreaming(); ElMessage.info('已停止生成') }
const handleCopyMessage = async (m) => { try { await navigator.clipboard.writeText(m.content); ElMessage.success('已复制') } catch { ElMessage.error('复制失败') } }
const handleRegenerateMessage = (m) => { const i = messages.value.findIndex(x => x.id === m.id); if (i > 0 && messages.value[i - 1].role === 'user') { messages.value.splice(i, 1); inputMessage.value = messages.value[i - 1].content; sendMessage() } }
const handleLikeMessage = (m) => { const i = messages.value.findIndex(x => x.id === m.id); if (i > -1) messages.value[i].liked = true; ElMessage.success('已点赞') }
const handleDislikeMessage = (m) => { const i = messages.value.findIndex(x => x.id === m.id); if (i > -1) messages.value[i].liked = false; ElMessage.success('已踩') }
const handleToggleSearch = (e) => { ElMessage.success(`已${e ? '开启' : '关闭'}联网搜索`) }
const handleUpload = () => { showPlusMenu.value = false; ElMessage.info('上传功能开发中') }
const handleVoice = () => { showPlusMenu.value = false; ElMessage.info('语音输入功能开发中') }

// 选择面板确认回调
const onDocApply = ({ selectedDocIds }) => {
  ElMessage.success(`已应用 ${selectedDocIds.length} 个文档`)
}
const onToolApply = async ({ selectedToolIds }) => {
  // 记录每个选中工具的使用次数
  for (const toolId of selectedToolIds) {
    await toolsStore.recordToolCall(toolId)
  }
  // 刷新 MCP 状态
  await chatStore.fetchMcpStatus()
  await toolsStore.fetchToolStats()
  ElMessage.success(`已应用 ${selectedToolIds.length} 个工具`)
}

// Markdown 渲染方法（用于 AI 消息）
const renderMd = (content) => renderMarkdown(content || '')
let msgCounter = 0  // 消息ID计数器，确保唯一性
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isTyping.value) return

  // 输入长度限制（与后端 @Size(max=10000) 保持一致）
  const MAX_INPUT_LENGTH = 10000
  if (inputMessage.value.length > MAX_INPUT_LENGTH) {
    ElMessage.error(`输入内容不能超过 ${MAX_INPUT_LENGTH} 字符`)
    return
  }

  // 前端安全检查
  const safetyResult = securityFilter.sanitize(inputMessage.value)
  if (safetyResult.action === 'BLOCK') {
    ElMessage.error(safetyResult.message || '输入包含不允许的内容')
    return
  }
  if (safetyResult.riskLevel === 'MEDIUM') {
    console.warn('[Security] 检测到潜在风险输入:', safetyResult.detectedTypes)
  }

  // 如果没有当前会话，先创建一个新会话
  if (!currentConversationId.value) {
    createNewConversation()
  }

  const userMsgId = 'user_' + Date.now() + '_' + (++msgCounter)
  const userMsg = { id: userMsgId, role: 'user', content: inputMessage.value, timestamp: new Date() }
  chatStore.addMessage(userMsg); inputMessage.value = ''; if (textareaRef.value) textareaRef.value.style.height = 'auto'
  // 估算用户消息 Token 并增量更新
  const userTokenEstimate = Math.ceil(userMsg.content.length / 2)
  chatStore.updateTokenUsage(userTokenEstimate)
  scrollToBottom(); chatStore.setTyping(true); chatStore.clearReactSteps()
  const aiId = 'ai_' + Date.now() + '_' + (++msgCounter)
  chatStore.currentStreamingMessageId = aiId
  chatStore.addMessage({ id: aiId, role: 'assistant', content: '', timestamp: new Date(), steps: [], status: 'executing', knowledgeRef: { current: 1, total: 5 }, mcpRef: { current: 1, total: 5 }, tokens: { current: 64, total: 30720 } })
  try {
    await chatAPI.sendMessageStream({
      message: userMsg.content, role: currentRole.value, model: modelStore.currentProvider, conversationId: currentConversationId.value, webSearch: settingsStore.webSearchEnabled,
      useKnowledge: true, useTools: true,
      onMessage: (chunk) => { const i = messages.value.findIndex(m => m.id === aiId); if (i > -1) { messages.value[i].content += chunk.content; scrollToBottom() } },
      onKnowledgeRef: (ref) => {
        const i = messages.value.findIndex(m => m.id === aiId)
        if (i > -1) {
          if (!messages.value[i].sources) messages.value[i].sources = []
          messages.value[i].sources = ref.sources
        }
        // 更新 store 中的知识库状态
        chatStore.knowledgeStatus.connected = true
        chatStore.knowledgeStatus.documentCount = ref.sources.length
        scrollToBottom()
      },
      onToolCall: (tc) => {
        const i = messages.value.findIndex(m => m.id === aiId)
        if (i > -1) {
          if (!messages.value[i].toolCalls) messages.value[i].toolCalls = []
          messages.value[i].toolCalls.push(tc.toolCall)
          scrollToBottom()
        }
        // 更新 MCP 调用次数
        chatStore.mcpStatus.totalCalls++
      },
      onMcpStatus: (status) => {
        if (status.mcpStatus) {
          chatStore.knowledgeStatus = { ...chatStore.knowledgeStatus, ...status.mcpStatus }
        }
        if (status.toolStatus) {
          chatStore.mcpStatus = { ...chatStore.mcpStatus, ...status.toolStatus }
        }
      },
      onReactStep: (step) => { chatStore.addReactStep(step); const i = messages.value.findIndex(m => m.id === aiId); if (i > -1) messages.value[i].steps = [...chatStore.reactSteps]; scrollToBottom() },
      onComplete: () => {
        chatStore.setTyping(false); chatStore.setStreaming(false); chatStore.currentStreamingMessageId = null
        const i = messages.value.findIndex(m => m.id === aiId)
        if (i > -1) {
          messages.value[i].status = 'completed'
          // 增量更新 Token：按内容字符数估算 Token 用量
          const contentLen = messages.value[i].content?.length || 0
          if (contentLen > 0) {
            // 中文约 1.5 字符/Token，英文约 4 字符/Token，取保守估算
            const estimatedTokens = Math.ceil(contentLen / 2)
            chatStore.updateTokenUsage(estimatedTokens)
          }
        }
        scrollToBottom()
      },
      onError: (err) => {
        chatStore.setTyping(false); chatStore.setStreaming(false); chatStore.currentStreamingMessageId = null
        const i = messages.value.findIndex(m => m.id === aiId)
        let errorText = '抱歉，发送消息时出现了错误，请重试。'
        if (err && err.message) {
          const msg = err.message
          if (msg.includes('Failed to fetch') || msg.includes('NetworkError') || msg.includes('network')) {
            errorText = '⚠️ 无法连接到后端服务，请确认后端已启动（端口 8080）'
          } else if (msg.includes('401') || msg.includes('授权')) {
            errorText = '⚠️ 登录已过期，请重新登录后再试'
          } else if (msg.includes('500') || msg.includes('服务异常')) {
            errorText = '⚠️ 后端服务异常，请稍后重试'
          } else {
            errorText = '⚠️ ' + msg
          }
        }
        if (i > -1) { messages.value[i].content = errorText; messages.value[i].status = 'error' }
        ElMessage.error(err?.message || '发送失败')
      },
      onStatusChange: (s) => { chatStore.setAIStatus(s); if (s === 'generating') chatStore.setStreaming(true) }
    })
  } catch (err) {
    chatStore.setTyping(false); chatStore.setStreaming(false); chatStore.currentStreamingMessageId = null
    const i = messages.value.findIndex(m => m.id === aiId)
    if (i > -1) {
      let errorText = '⚠️ 发送失败'
      if (err?.name === 'TypeError' || err?.message?.includes('fetch')) {
        errorText = '⚠️ 无法连接到后端服务，请确认后端已启动（端口 8080）'
      } else if (err?.message) {
        errorText = '⚠️ ' + err.message
      }
      messages.value[i].content = errorText; messages.value[i].status = 'error'
    }
  }
}

onMounted(() => {
  checkResponsive()
  window.addEventListener('resize', checkResponsive)
  // 初始化设置（包括联网搜索状态）
  settingsStore.initSettings()
  // 从数据库加载历史对话
  chatStore.loadConversations()
  // 获取所有状态（知识库、MCP、Token用量、最近工具调用）
  chatStore.fetchAllStatus()
  // 加载文档列表和工具列表（用于选择面板）
  contextStore.fetchAll()
  // 定期刷新状态（每 30 秒）
  const statusTimer = setInterval(() => chatStore.fetchAllStatus(), 30000)
  onUnmounted(() => {
    window.removeEventListener('resize', checkResponsive)
    clearInterval(statusTimer)
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
  })
})

// ===== 用户切换同步（keep-alive 缓存页面不会重新执行 onMounted） =====
// keep-alive 恢复时刷新知识库与 MCP 状态，确保切换用户后展示当前用户数据
onActivated(() => {
  if (authStore.isAuthenticated) {
    chatStore.fetchAllStatus()
    contextStore.fetchAll()
  }
})

// 用户 ID 变化时兜底同步（authStore.login/logout 已触发 store 重置，此处防止遗漏）
watch(() => authStore.user.id, (newId) => {
  if (newId) {
    chatStore.fetchAllStatus()
    contextStore.fetchAll()
  }
})


</script>

<style lang="scss" scoped>
/* ===== 页面 ===== */
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 68px);
  background: #0a0a1a;
  position: relative;
  overflow: hidden;
}

/* ===== 背景层 ===== */
.bg-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* ===== 鼠标光晕 ===== */
.mouse-glow {
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  pointer-events: none;
  background: radial-gradient(circle, rgba(0, 245, 212, 0.12) 0%, rgba(123, 97, 255, 0.06) 40%, transparent 70%);
  filter: blur(24px);
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.4s ease;
  will-change: left, top;
}
.mouse-glow.visible {
  opacity: 1;
}

/* 触摸设备禁用鼠标光晕 */
@media (pointer: coarse) {
  .mouse-glow {
    display: none;
  }
}

/* ===== 动态浮动光晕 ===== */
.floating-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  animation: floatGlow 16s ease-in-out infinite alternate;
}

.fg-cyan {
  width: 500px;
  height: 500px;
  top: -120px;
  right: -120px;
  background: radial-gradient(circle, rgba(0, 245, 212, 0.06) 0%, transparent 70%);
  animation-duration: 18s;
}

.fg-purple {
  width: 420px;
  height: 420px;
  bottom: -100px;
  left: -100px;
  background: radial-gradient(circle, rgba(123, 97, 255, 0.06) 0%, transparent 70%);
  animation-duration: 22s;
  animation-delay: -6s;
}

@keyframes floatGlow {
  0% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(25px, -18px) scale(1.08);
  }
  50% {
    transform: translate(-18px, 22px) scale(0.92);
  }
  75% {
    transform: translate(15px, 12px) scale(1.04);
  }
  100% {
    transform: translate(-10px, -8px) scale(1.02);
  }
}

/* ===== 极光背景 ===== */
.aurora-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.aurora-layer {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  animation: aurora 20s ease-in-out infinite;
}
.a1 {
  width: 600px; height: 600px;
  top: -200px; right: -100px;
  background: radial-gradient(circle, rgba(0,245,212,0.07) 0%, transparent 70%);
}
.a2 {
  width: 500px; height: 500px;
  bottom: -150px; left: -100px;
  background: radial-gradient(circle, rgba(0,85,255,0.07) 0%, transparent 70%);
  animation-delay: -7s;
}
.a3 {
  width: 400px; height: 400px;
  top: 40%; left: 40%;
  background: radial-gradient(circle, rgba(123,97,255,0.05) 0%, transparent 70%);
  animation-delay: -14s;
}
@keyframes aurora {
  0%,100% { transform: translate(0,0) scale(1); }
  25% { transform: translate(30px,-30px) scale(1.1); }
  50% { transform: translate(-20px,20px) scale(0.95); }
  75% { transform: translate(20px,10px) scale(1.05); }
}
.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    /* 青色水平网格线 */
    linear-gradient(rgba(0,245,212,0.04) 1px, transparent 1px),
    /* 紫色垂直网格线 */
    linear-gradient(90deg, rgba(123,97,255,0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
  animation: gridPulse 8s ease-in-out infinite alternate;
  transform-origin: center center;
  will-change: opacity, transform;
}

@keyframes gridPulse {
  0% {
    opacity: 0.3;
    transform: scale(1);
  }
  100% {
    opacity: 0.6;
    transform: scale(1.02);
  }
}

/* ===== 粒子效果 ===== */
.bg-particles { position: absolute; inset: 0; }
.particle {
  position: absolute;
  border-radius: 50%;
  background: #00f5d4;
  animation: particleFloat linear infinite;
}
@keyframes particleFloat {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.4; }
  90% { opacity: 0.2; }
  100% { transform: translateY(-100vh) translateX(100px); opacity: 0; }
}

/* ===== 顶部标题栏 ===== */
.chat-header { display: flex; align-items: center; justify-content: space-between; padding: 0 16px; height: 40px; background: rgba(100,100,180,0.03); border-bottom: 1px solid rgba(100,100,180,0.06); flex-shrink: 0; position: relative; z-index: 2; }
.hdr-left { display: flex; align-items: center; gap: 8px; }
.hdr-title { font-size: 13px; font-weight: 600; color: #ffffff; }
.hdr-right { display: flex; align-items: center; gap: 6px; }
.icon-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: transparent; border: 1px solid rgba(100,100,180,0.08); border-radius: 6px; color: #9090b8; cursor: pointer; transition: all 0.2s; &:hover { background: rgba(100,100,180,0.08); color: #f0f0ff; } &.accent { color: #00f5d4; border-color: rgba(0,245,212,0.15); &:hover { background: rgba(0,245,212,0.08); box-shadow: 0 0 12px rgba(0,245,212,0.1); } } }
.icon-btn-sm { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; color: #9090b8; cursor: pointer; border-radius: 4px; &:hover { background: rgba(100,100,180,0.08); } }

/* ===== 对话历史抽屉 ===== */
.history-overlay { position: fixed; inset: 0; z-index: 1001; background: rgba(0,0,0,0.3); }
.history-panel { position: fixed; left: 0; top: 0; bottom: 0; width: 260px; background: rgba(10,14,26,0.98); backdrop-filter: blur(12px); border-right: 1px solid rgba(100,100,180,0.08); display: flex; flex-direction: column; z-index: 1002; }
.history-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; font-size: 14px; font-weight: 600; color: #f0f0ff; border-bottom: 1px solid rgba(100,100,180,0.06); }
.history-new { margin: 10px 12px; padding: 8px 14px; background: linear-gradient(135deg, rgba(0,245,212,0.1), rgba(0,85,255,0.06)); border: 1px solid rgba(0,245,212,0.15); border-radius: 6px; color: #00f5d4; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; &:hover { background: linear-gradient(135deg, rgba(0,245,212,0.18), rgba(0,85,255,0.1)); transform: translateY(-1px); } }
.history-list { flex: 1; overflow-y: auto; padding: 6px 10px; }
.hist-item { display: flex; align-items: center; gap: 8px; padding: 9px 10px; border-radius: 6px; cursor: pointer; transition: all 0.15s; &:hover { background: rgba(100,100,180,0.06); } &.active { background: rgba(0,245,212,0.07); border-left: 2px solid #00f5d4; } }
.hist-title { flex: 1; font-size: 13px; color: #c0c0e0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hist-del { width: 16px; height: 16px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; color: #9090b8; cursor: pointer; border-radius: 3px; opacity: 0; transition: opacity 0.15s; .hist-item:hover & { opacity: 1; } &:hover { color: #ff4060; } }
.history-slide-enter-active, .history-slide-leave-active { transition: opacity 0.2s; }
.history-slide-enter-active .history-panel, .history-slide-leave-active .history-panel { transition: transform 0.25s cubic-bezier(0.4,0,0.2,1); }
.history-slide-enter-from, .history-slide-leave-to { opacity: 0; }
.history-slide-enter-from .history-panel, .history-slide-leave-to .history-panel { transform: translateX(-100%); }

/* ===== 角色 + 快捷指令 ===== */
.opts-bar { display: flex; align-items: center; gap: 10px; padding: 6px 16px; border-bottom: 1px solid rgba(100,100,180,0.05); flex-shrink: 0; overflow-x: auto; scrollbar-width: none; position: relative; z-index: 2; &::-webkit-scrollbar { display: none; } }
.role-pills { display: flex; gap: 4px; flex-shrink: 0; }
.role-pill { display: flex; align-items: center; gap: 3px; padding: 5px 14px; border: 1px solid var(--border-ctrl-soft); border-radius: 12px; background: rgba(100,100,180,0.03); color: #c0c8e0; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s; white-space: nowrap; &:hover { border-color: var(--border-ctrl-hover); color: #f0f0ff; } &.active { border-color: rgba(0,245,212,0.3); background: rgba(0,245,212,0.08); color: #00f5d4; } }
.quick-pills { display: flex; gap: 4px; flex-shrink: 0; }
.qpill { display: flex; align-items: center; gap: 3px; padding: 5px 14px; border: 1px solid var(--border-ctrl-soft); border-radius: 12px; background: rgba(100,100,180,0.03); color: #c0c8e0; font-size: 13px; white-space: nowrap; cursor: pointer; transition: all 0.2s; &:hover { background: rgba(100,100,180,0.08); color: #f0f0ff; } &.pri { background: rgba(0,245,212,0.05); border-color: rgba(0,245,212,0.08); color: #7dd3fc; &:hover { background: rgba(0,245,212,0.1); } } &:disabled { opacity: 0.35; cursor: not-allowed; } }

/* ===== 主体区域：左面板 + 右对话区 ===== */
.chat-body {
  display: flex;
  flex: 1;
  min-height: 0;
  position: relative;
  z-index: 2;
}

.execution-panel-wrapper {
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  transition: width 0.3s ease, min-width 0.3s ease;
}

.main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ===== 主消息区 ===== */
.chat-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(0,245,212,0.12) transparent;
  position: relative;
  z-index: 2;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,245,212,0.12); border-radius: 2px; }
  &:hover::-webkit-scrollbar-thumb { background: rgba(0,245,212,0.25); }
}

/* ===== 欢迎横幅 ===== */
.welcome-banner {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(100, 100, 180, 0.04);
  border-radius: 12px;
  border: 1px solid rgba(100, 100, 180, 0.08);
  margin-bottom: 8px;
}
.welcome-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, rgba(0, 245, 212, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba(0, 245, 212, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}
.welcome-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.welcome-name {
  font-size: 13px;
  font-weight: 700;
  color: #00f5d4;
}
.welcome-text {
  font-size: 14px;
  color: #d0d0f0;
  line-height: 1.6;
}

/* 空白状态 */
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; opacity: 0.8; padding: 20px 20px 40px; }
.empty-title { font-size: 16px; font-weight: 600; color: #ffffff; text-shadow: 0 0 40px rgba(124,107,245,0.1); }
.empty-desc { font-size: 13px; color: var(--text-sub); margin-bottom: 8px; }
.empty-examples { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }
.example-card { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: rgba(100,100,180,0.04); border: 1px solid var(--border-ctrl-soft); border-radius: 10px; cursor: pointer; transition: all 0.2s; &:hover { background: rgba(0,245,212,0.06); border-color: rgba(0,245,212,0.2); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,245,212,0.06); } }
.ex-icon { font-size: 1rem; }
.ex-text { font-size: 13px; color: #d8dce8; }

/* ===== 消息 ===== */
.message { display: flex; gap: 10px; animation: msgIn 0.3s ease; margin-bottom: 2px;
  &.user {
    justify-content: flex-end;
    .msg-content { align-items: flex-end; }
    .msg-bubble {
      background: rgba(0,245,212,0.1);
      backdrop-filter: blur(8px);
      color: #fff;
      padding: 10px 14px;
      max-width: 78%;
      width: fit-content;
      border-radius: 16px 16px 4px 16px;
      font-size: 16px;
      font-weight: 500;
      border: 1px solid rgba(0,245,212,0.08);
      word-break: keep-all;
      overflow-wrap: break-word;
      white-space: normal;
      line-height: 1.6;
    }
  }
  &.assistant {
    justify-content: flex-start;
    .msg-content { align-items: flex-start; }
    .msg-bubble {
      background: rgba(17, 17, 39, 0.65);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      color: #e8e8ff;
      border: 1px solid rgba(100, 100, 180, 0.1);
      border-left: 2px solid rgba(0,245,212,0.4);
      padding: 12px 14px;
      max-width: 85%;
      border-radius: 16px 16px 16px 4px;
      font-size: 16px;
      line-height: 1.7;
      word-break: keep-all;
      overflow-wrap: break-word;
      box-shadow: 0 2px 12px rgba(0,0,0,0.15);
      transition: all 0.2s;
      &:hover { border-color: rgba(0,245,212,0.15); box-shadow: 0 4px 16px rgba(0,0,0,0.2); }
    }
  }
}
.user-message-wrapper { display: flex; justify-content: flex-end; width: 100%; }
.ai-message-wrapper { display: flex; justify-content: flex-start; width: 100%; }
.avatar { width: 42px; height: 42px; background: rgba(17, 17, 39, 0.6); border: 1px solid rgba(100,100,180,0.12); border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; backdrop-filter: blur(8px); }
.user-av { background: linear-gradient(135deg, rgba(0,245,212,0.15), rgba(123,97,255,0.12)); border-color: rgba(0,245,212,0.15); width: 38px; height: 38px; }
.msg-content { display: flex; flex-direction: column; max-width: 85%; min-width: 0; }
.msg-meta { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.ai-label { font-size: 12px; font-weight: 700; color: #00f5d4; }
.msg-time { font-size: 11px; color: var(--text-sub); opacity: 0.8; }
.msg-actions { display: flex; gap: 4px; margin-top: 8px; opacity: 0; transition: opacity 0.2s; .message:hover & { opacity: 1; } }
.act-btn { width: 26px; height: 26px; border: 1px solid rgba(100,100,180,0.1); border-radius: 6px; background: rgba(100,100,180,0.03); cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 0.75rem; transition: all 0.2s; &:hover { border-color: #00f5d4; background: rgba(0,245,212,0.08); transform: translateY(-1px); } &.liked { border-color: #00f5d4; background: rgba(0,245,212,0.08); } }
.msg-skills { margin-top: 10px; }
.sk-head { font-size: 11px; color: #9090b8; cursor: pointer; transition: color 0.15s; &:hover { color: #00f5d4; } }
.sk-list { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.sk-tag { padding: 3px 8px; background: rgba(0,245,212,0.04); border: 1px solid rgba(0,245,212,0.08); border-radius: 8px; font-size: 11px; color: var(--text-sub); }

/* ===== 输入区域 ===== */
.input-area { flex-shrink: 0; padding: 8px 16px 4px; border-top: 1px solid rgba(100,100,180,0.06); position: relative; z-index: 2; }
.input-row { display: flex; align-items: flex-end; gap: 8px; background: rgba(17, 17, 39, 0.5); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); border-radius: 12px; padding: 8px 10px; border: 1px solid rgba(100, 100, 180, 0.1); min-height: 44px; position: relative; transition: border-color 0.2s, box-shadow 0.2s; &:focus-within { border-color: rgba(0,245,212,0.25); box-shadow: 0 0 0 3px rgba(0,245,212,0.04), 0 0 20px rgba(0,245,212,0.04); } }
.plus-btn { width: 30px; height: 30px; border-radius: 50%; background: rgba(0,245,212,0.03); border: 1px dashed rgba(0,245,212,0.2); color: #00f5d4; display: flex; align-items: center; justify-content: center; cursor: pointer; flex-shrink: 0; font-size: 1.1rem; transition: all 0.2s; &:hover { background: rgba(0,245,212,0.1); transform: rotate(90deg); } }
.plus-menu { position: absolute; bottom: 100%; left: 0; margin-bottom: 6px; background: rgba(10,14,26,0.96); backdrop-filter: blur(12px); border: 1px solid rgba(100,100,180,0.08); border-radius: 8px; padding: 5px; box-shadow: 0 4px 16px rgba(0,0,0,0.4); z-index: 10; }
.pm-item { padding: 7px 12px; border-radius: 5px; cursor: pointer; font-size: 12px; color: #c0c0e0; transition: background 0.12s; white-space: nowrap; &:hover { background: rgba(0,245,212,0.06); color: #00f5d4; } }
.msg-input { flex: 1; background: transparent; border: none; color: var(--text-input); caret-color: var(--title-status); font-size: 15px; resize: none; outline: none; min-height: 46px; max-height: 120px; line-height: 1.5; padding: 4px 0; font-family: inherit; &::placeholder { color: var(--text-placeholder); } }
.stop-btn { padding: 6px 14px; background: rgba(255,64,96,0.2); color: #ff4060; border: 1px solid rgba(255,64,96,0.2); border-radius: 7px; font-size: 12px; cursor: pointer; flex-shrink: 0; transition: all 0.15s; &:hover { background: rgba(255,64,96,0.3); } }
.send-btn { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; background: var(--btn-gradient); color: #fff; border: none; border-radius: 8px; cursor: pointer; flex-shrink: 0; transition: all 0.2s; &:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(124,107,245,0.4); } &:disabled { opacity: 0.3; cursor: not-allowed; transform: none; box-shadow: none; } }

.input-disclaimer { text-align: center; font-size: 12px; color: var(--text-hint); padding: 6px 0 0; letter-spacing: 0.2px; }

/* ===== 底部状态栏已移除（移至左侧 ExecutionPanel） ===== */

/* ===== 引用来源展示 ===== */
.sources-section { margin-top: 10px; padding: 8px 10px; background: rgba(0,245,212,0.03); border: 1px solid rgba(0,245,212,0.08); border-radius: 8px; }
.sources-title { font-size: 11px; font-weight: 600; color: #00f5d4; margin-bottom: 6px; }
.source-item { padding: 5px 0; border-bottom: 1px solid rgba(100,100,180,0.05); &:last-child { border-bottom: none; } }
.source-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2px; }
.source-doc { font-size: 11px; font-weight: 600; color: #7dd3fc; font-family: 'JetBrains Mono', monospace; }
.source-relevance { font-size: 10px; color: var(--status-ready-text); font-weight: 500; }
.source-snippet { font-size: 11px; color: #a0a0c8; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; }

/* ===== 工具调用卡片 ===== */
.tool-calls-section { margin-top: 10px; display: flex; flex-direction: column; gap: 6px; }
.tool-call-card { padding: 8px 10px; background: rgba(123,97,255,0.04); border: 1px solid rgba(123,97,255,0.1); border-radius: 8px; font-size: 12px; }
.tool-call-header { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.tool-call-icon { font-size: 12px; }
.tool-call-name { flex: 1; color: #c0c0e0; font-weight: 500; font-size: 11px; }
.tool-call-status { font-size: 11px; font-weight: 600; }
.tool-call-status.success { color: var(--status-ready-text); }
.tool-call-status.error { color: #f87171; }
.tool-call-status.running { color: #F59E0B; }
.tool-call-params { padding: 4px 6px; background: rgba(0,0,0,0.15); border-radius: 4px; margin-top: 4px; font-size: 11px; color: #a0a0c8; word-break: break-all; }
.tool-call-params .params-label { color: #8080a8; }
.tool-call-params code { color: #7dd3fc; font-family: 'JetBrains Mono', monospace; font-size: 10px; }
.tool-call-duration { font-size: 10px; color: #8080a8; margin-top: 4px; }
.tool-call-result { font-size: 11px; color: #c0c0e0; margin-top: 4px; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
@keyframes msgIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

/* ===== Markdown 渲染样式（AI 消息） ===== */
.markdown-body {
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    color: #e8e8ff;
    font-weight: 700;
    margin: 12px 0 6px;
    line-height: 1.4;
  }
  :deep(h1) { font-size: 1.3rem; }
  :deep(h2) { font-size: 1.15rem; color: #00f5d4; }
  :deep(h3) { font-size: 1.05rem; color: #7dd3fc; }
  :deep(p) { margin: 6px 0; line-height: 1.75; color: #d0d0f0; }
  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin: 6px 0;
  }
  :deep(li) {
    margin: 3px 0;
    line-height: 1.7;
    color: #d0d0f0;
  }
  :deep(strong) { color: #00f5d4; font-weight: 700; }
  :deep(em) { color: #a78bfa; font-style: italic; }
  :deep(code) {
    background: rgba(0,245,212,0.08);
    border: 1px solid rgba(0,245,212,0.12);
    border-radius: 4px;
    padding: 1px 5px;
    font-size: 0.88em;
    color: #7dd3fc;
    font-family: 'Fira Code', 'Consolas', monospace;
  }
  :deep(pre) {
    background: rgba(0,0,0,0.3);
    border: 1px solid rgba(100,100,180,0.12);
    border-radius: 8px;
    padding: 12px 14px;
    margin: 8px 0;
    overflow-x: auto;
    code {
      background: transparent;
      border: none;
      padding: 0;
      color: #d0d0f0;
    }
  }
  :deep(blockquote) {
    border-left: 3px solid rgba(0,245,212,0.4);
    margin: 8px 0;
    padding: 8px 14px;
    background: rgba(0,245,212,0.04);
    border-radius: 0 8px 8px 0;
    color: #c0c0e0;
    p { margin: 2px 0; color: #c0c0e0; }
  }
  :deep(table) {
    border-collapse: collapse;
    margin: 8px 0;
    width: 100%;
    th, td {
      border: 1px solid rgba(100,100,180,0.15);
      padding: 6px 10px;
      font-size: 0.9em;
      color: #d0d0f0;
    }
    th { background: rgba(0,245,212,0.06); color: #00f5d4; font-weight: 600; }
  }
  :deep(hr) {
    border: none;
    border-top: 1px solid rgba(100,100,180,0.1);
    margin: 12px 0;
  }
  :deep(a) {
    color: #7dd3fc;
    text-decoration: underline;
    text-underline-offset: 2px;
    &:hover { color: #00f5d4; }
  }
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .execution-panel-wrapper { width: 180px !important; min-width: 180px !important; }
}
@media (max-width: 1024px) {
  .hdr-title { display: none; }
  .quick-pills { display: none; }
  .execution-panel-wrapper { display: none; }
}
@media (max-width: 1024px) {
  .chat-page { height: calc(100vh - 56px); }
}
@media (max-width: 640px) {
  .chat-page { height: calc(100vh - 110px); }
  .chat-header { padding: 0 10px; }
  .opts-bar { padding: 5px 10px; gap: 6px; }
  .chat-main { padding: 8px 10px; gap: 10px; }
  .input-area { padding: 6px 10px 2px; }

  /* 移动端头像适配 */
  .avatar { width: 36px; height: 36px; font-size: 18px; border-radius: 10px; }
  .user-av { width: 32px; height: 32px; }
  /* 移动端消息字体适配 */
  .message.user .msg-bubble { font-size: 15px; }
  .message.assistant .msg-bubble { font-size: 15px; }
  /* 移动端按钮字体适配 */
  .role-pill { font-size: 13px; padding: 4px 12px; }
  .qpill { font-size: 12px; padding: 4px 12px; }
  /* 移动端输入框适配 */
  .msg-input { font-size: 14px; min-height: 40px; }
}
</style>