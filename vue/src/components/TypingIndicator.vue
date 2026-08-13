<template>
  <div class="typing-indicator" :class="{ 'is-streaming': isStreaming }">
    <div class="typing-content">
      <span v-if="showText" class="typing-text">{{ displayText }}</span>
      <span v-if="isStreaming" class="typing-cursor">|</span>
    </div>
    <div v-if="showDots" class="typing-dots">
      <span class="dot"></span>
      <span class="dot"></span>
      <span class="dot"></span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  isStreaming: {
    type: Boolean,
    default: false
  },
  showText: {
    type: Boolean,
    default: true
  },
  showDots: {
    type: Boolean,
    default: false
  },
  text: {
    type: String,
    default: '正在思考...'
  }
})

// 显示文本
const displayText = computed(() => {
  if (props.isStreaming) {
    return props.text
  }
  return ''
})
</script>

<style lang="scss" scoped>
.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  transition: all 0.3s ease;

  &.is-streaming {
    border-color: var(--accent-primary);
    background: rgba(0, 229, 255, 0.05);
  }
}

.typing-content {
  display: flex;
  align-items: center;
  gap: 2px;
}

.typing-text {
  font-size: 0.9rem;
  color: #CBD5E1;
  font-weight: 500;
}

.typing-cursor {
  font-size: 1rem;
  color: var(--accent-primary);
  font-weight: 700;
  animation: blink 0.8s infinite;
  margin-left: 2px;
}

.typing-dots {
  display: flex;
  gap: 4px;

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--accent-primary);
    animation: typing-bounce 1.4s ease-in-out infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

@keyframes typing-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}
</style>