<template>
  <div class="chat-input">
    <div class="input-row">
      <textarea
        ref="textareaRef"
        v-model="inputValue"
        :placeholder="placeholder"
        :maxlength="maxLength"
        :disabled="disabled"
        class="msg-input"
        rows="1"
        @keydown.enter.exact.prevent="handleSend"
        @input="autoResize"
      ></textarea>
      <button
        v-if="isStreaming"
        class="stop-btn"
        @click="$emit('stop')"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="6" width="12" height="12" rx="2" />
        </svg>
      </button>
      <button
        v-else
        :disabled="disabled || !inputValue.trim()"
        class="send-btn"
        @click="handleSend"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" />
          <polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>
    <div class="input-disclaimer">本服务生成内容由 AI 提供，仅供参考，不构成专业建议。</div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '输入问题... (Enter 发送 / Shift+Enter 换行)'
  },
  maxLength: {
    type: Number,
    default: 10000
  },
  disabled: {
    type: Boolean,
    default: false
  },
  isStreaming: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'send', 'stop'])

const textareaRef = ref(null)
const inputValue = ref(props.modelValue)

watch(() => props.modelValue, (newVal) => {
  inputValue.value = newVal
})

watch(inputValue, (newVal) => {
  emit('update:modelValue', newVal)
})

const handleSend = () => {
  if (!inputValue.value.trim() || props.disabled) return
  emit('send', inputValue.value)
}

const autoResize = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 100) + 'px'
}

const focus = () => {
  textareaRef.value?.focus()
}

defineExpose({
  focus,
  textareaRef
})
</script>

<style lang="scss" scoped>
.chat-input {
  padding: 12px 16px;
  background: rgba(17, 17, 39, 0.6);
  border-top: 1px solid rgba(100, 100, 180, 0.1);
}

.input-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.msg-input {
  flex: 1;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(124, 97, 255, 0.2);
  border-radius: 12px;
  color: #f0f0ff;
  font-size: 0.9rem;
  font-family: inherit;
  outline: none;
  resize: none;
  transition: all 0.2s ease;
  min-height: 44px;
  max-height: 100px;

  &::placeholder {
    color: var(--text-placeholder, #6b6b8b);
  }

  &:focus {
    border-color: rgba(124, 97, 255, 0.5);
    box-shadow: 0 0 20px rgba(124, 97, 255, 0.1);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.stop-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 12px;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;

  &:hover {
    background: rgba(239, 68, 68, 0.25);
    transform: scale(1.05);
  }
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, rgba(0, 245, 212, 0.8), rgba(91, 134, 255, 0.8));
  border: none;
  border-radius: 12px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;

  &:hover:not(:disabled) {
    transform: scale(1.05);
    box-shadow: 0 4px 20px rgba(0, 245, 212, 0.3);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.input-disclaimer {
  margin-top: 8px;
  font-size: 0.72rem;
  color: var(--text-sub, #6b6b8b);
  text-align: center;
}
</style>