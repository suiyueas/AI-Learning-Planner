<template>
  <div class="u-chat-input">
    <div class="uci-container">
      <span v-if="context" class="uci-context">{{ context }}</span>
      <textarea
        ref="textareaRef"
        v-model="localValue"
        :placeholder="placeholder"
        :rows="rows"
        @input="handleInput"
        @keydown.enter.ctrl="handleSend"
        @keydown.enter.meta="handleSend"
        @keydown.enter="!useCtrlEnter && handleSend"
      ></textarea>
      <div class="uci-footer">
        <span class="uci-hint">{{ hint }}</span>
        <button
          class="uci-send"
          :disabled="!canSend"
          @click="handleSend"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4z" />
          </svg>
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '输入消息...' },
  context: { type: String, default: '' },
  rows: { type: Number, default: 2 },
  useCtrlEnter: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'send'])

const localValue = ref(props.modelValue)
const textareaRef = ref(null)

const canSend = () => localValue.value.trim().length > 0

const hint = props.useCtrlEnter
  ? 'Ctrl + Enter 发送'
  : 'Enter 发送'

watch(() => props.modelValue, (val) => {
  localValue.value = val
})

watch(localValue, (val) => {
  emit('update:modelValue', val)
})

function handleInput(e) {
  // 自动调整高度
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

function handleSend() {
  if (!canSend()) return
  emit('send', localValue.value.trim())
  localValue.value = ''
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

onMounted(() => {
  if (textareaRef.value) {
    handleInput({ target: textareaRef.value })
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-chat-input {
  width: 100%;
}

.uci-container {
  background: rgba($bg-elevated, 0.8);
  border: 1px solid rgba($border-default, 0.5);
  border-radius: $radius-xl;
  overflow: hidden;
}

.uci-context {
  display: inline-block;
  padding: $space-2 $space-3;
  font-size: $text-xs;
  color: $accent-indigo;
  background: rgba($accent-indigo, 0.1);
  margin: $space-2;
  border-radius: $radius-full;
}

textarea {
  width: 100%;
  min-height: 56px;
  padding: $space-4;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: $text-primary;
  font-size: $text-sm;
  font-family: inherit;
  line-height: $leading-normal;

  &::placeholder {
    color: $text-muted;
  }
}

.uci-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-2 $space-4;
  border-top: 1px solid rgba($border-default, 0.5);
  background: rgba($bg-elevated, 0.5);
}

.uci-hint {
  font-size: $text-xs;
  color: $text-muted;
}

.uci-send {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-4;
  background: $accent-indigo;
  color: white;
  border: none;
  border-radius: $radius-md;
  font-size: $text-xs;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover:not(:disabled) {
    background: $accent-indigo-dark;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  svg {
    flex-shrink: 0;
  }
}
</style>