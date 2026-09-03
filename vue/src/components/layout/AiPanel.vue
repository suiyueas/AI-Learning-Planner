<template>
  <div class="ai-panel" :class="{ open: visible }">
    <div class="ai-panel-inner">
      <!-- 头部 -->
      <div class="panel-header">
        <div class="header-left">
          <div class="ai-dot"></div>
          <span>AI 助手</span>
        </div>
        <button class="close-btn" @click="$emit('update:visible', false)">
          <X :size="18" />
        </button>
      </div>

      <!-- 聊天区域 -->
      <div class="chat-area">
        <div class="empty-state">
          <div class="empty-icon">
            <Bot :size="40" />
          </div>
          <h3>开始对话</h3>
          <p>我是知途AI助手，可以帮助你解答学习问题、规划学习路径、分析薄弱点。</p>
          <div class="quick-actions">
            <button
              v-for="q in quickQuestions"
              :key="q"
              class="quick-btn"
              @click="sendQuickMessage(q)"
            >{{ q }}</button>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-box">
          <input
            v-model="inputMessage"
            type="text"
            placeholder="输入你的问题…"
            @keyup.enter="sendMessage"
          />
          <button
            class="send-btn"
            @click="sendMessage"
            :disabled="!inputMessage.trim()"
          >
            <Send :size="16" />
          </button>
        </div>
        <p class="input-hint">Enter 发送 · Shift+Enter 换行</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Bot, X, Send } from 'lucide-vue-next'

defineProps({
  visible: { type: Boolean, default: false }
})

defineEmits(['update:visible'])

const inputMessage = ref('')

const quickQuestions = [
  '帮我制定学习计划',
  '分析我的薄弱点',
  '解释这个知识点'
]

const sendMessage = () => {
  if (!inputMessage.value.trim()) return
  // TODO: 对接 AI 对话
  console.log('发送消息:', inputMessage.value)
  inputMessage.value = ''
}

const sendQuickMessage = (msg) => {
  inputMessage.value = msg
  sendMessage()
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.ai-panel {
  width: 0;
  height: 100vh;
  background: rgba($bg-surface, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-left: 1px solid rgba($border-default, 0.5);
  overflow: hidden;
  transition: width $transition-normal;
  flex-shrink: 0;
  position: relative;
  z-index: 2;

  &.open { width: $aipanel-width; }
}

.ai-panel-inner {
  width: $aipanel-width;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  height: $topbar-height;
  padding: 0 $space-4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid $border-subtle;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: $text-base;
  font-weight: 600;
  color: $text-primary;
}

.ai-dot {
  width: 8px;
  height: 8px;
  border-radius: $radius-full;
  background: $accent-cyan;
}

.close-btn {
  background: none;
  border: none;
  color: $text-muted;
  cursor: pointer;
  padding: 4px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: $space-6 $space-4;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  text-align: center;
}

.empty-icon {
  width: 72px;
  height: 72px;
  background: rgba($accent-cyan, 0.08);
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $space-4;

  svg { color: $accent-cyan; }
}

.empty-state h3 {
  font-size: $text-md;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 $space-2 0;
}

.empty-state p {
  font-size: $text-sm;
  color: $text-muted;
  margin: 0 0 $space-5 0;
  line-height: $leading-relaxed;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: $space-2;
  width: 100%;
}

.quick-btn {
  background: $bg-elevated;
  border: 1px solid $border-default;
  color: $text-secondary;
  padding: $space-3 $space-4;
  border-radius: $radius-md;
  font-size: $text-sm;
  cursor: pointer;
  transition: all $transition-fast;
  text-align: left;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
    border-color: $border-medium;
  }
}

.input-area {
  padding: $space-4;
  border-top: 1px solid $border-subtle;
  flex-shrink: 0;
}

.input-box {
  display: flex;
  align-items: center;
  gap: $space-2;
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: $space-2;
  transition: border-color $transition-fast;

  &:focus-within { border-color: $accent-indigo; }

  input {
    flex: 1;
    background: none;
    border: none;
    color: $text-primary;
    font-size: $text-sm;
    padding: $space-1 $space-2;
    outline: none;

    &::placeholder { color: $text-placeholder; }
  }
}

.send-btn {
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.25);
  color: $accent-indigo;
  width: 32px;
  height: 32px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover:not(:disabled) { 
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
  }
  &:disabled {
    background: $bg-muted;
    color: $text-muted;
    border-color: transparent;
    cursor: not-allowed;
  }
}

.input-hint {
  text-align: center;
  margin: $space-2 0 0 0;
  font-size: 11px;
  color: $text-muted;
}

@media (max-width: $breakpoint-md) {
  .ai-panel {
    position: fixed;
    top: $topbar-height;
    right: 0;
    bottom: 0;
    z-index: 50;

    &.open { width: 100%; }
  }
}
</style>