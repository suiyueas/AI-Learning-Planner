<template>
  <Teleport to="body">
    <Transition name="logout-modal">
      <div v-if="visible" class="logout-overlay" @click.self="handleCancel">
        <div class="logout-modal">
          <!-- 头部图标 -->
          <div class="modal-icon-wrap">
            <div class="modal-icon warning">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
            </div>
          </div>

          <!-- 标题 -->
          <h3 class="modal-title">确认退出登录</h3>

          <!-- 描述信息 -->
          <div class="modal-desc">
            <p class="desc-main">确定要退出登录吗？退出后需要重新登录才能使用所有功能。</p>
          </div>

          <!-- 操作按钮 -->
          <div class="modal-actions">
            <button class="action-btn cancel" @click="handleCancel">
              取消
            </button>
            <button class="action-btn danger" @click="handleConfirm">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              确认退出
            </button>
          </div>

          <!-- 快捷键提示 -->
          <div class="modal-hint">
            按 <kbd>Enter</kbd> 确认 · 按 <kbd>Esc</kbd> 取消
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['cancel', 'confirm'])

const handleCancel = () => {
  emit('cancel')
}

const handleConfirm = () => {
  emit('confirm')
}

const handleKeydown = (e) => {
  if (!props.visible) return
  if (e.key === 'Escape') {
    handleCancel()
  } else if (e.key === 'Enter') {
    handleConfirm()
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

.logout-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.logout-modal {
  background: linear-gradient(145deg, rgba(25, 28, 50, 0.98), rgba(18, 20, 40, 0.98));
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 20px;
  padding: 32px;
  width: 420px;
  max-width: 90vw;
  box-shadow:
    0 25px 60px rgba(0, 0, 0, 0.5),
    0 0 40px rgba(124, 97, 245, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.modal-icon-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.modal-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  &.warning {
    background: linear-gradient(135deg, rgba(251, 191, 36, 0.15), rgba(245, 158, 11, 0.1));
    color: #fbbf24;
    border: 2px solid rgba(251, 191, 36, 0.2);
  }
}

.modal-title {
  text-align: center;
  font-size: 1.25rem;
  font-weight: 600;
  color: #f0f0ff;
  margin: 0 0 12px;
}

.modal-desc {
  text-align: center;
  margin-bottom: 28px;
}

.desc-main {
  font-size: 0.95rem;
  color: #a0a0c0;
  line-height: 1.6;
  margin: 0;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;

  &.cancel {
    background: rgba(100, 100, 180, 0.1);
    color: #a0a0c0;
    border: 1px solid rgba(100, 100, 180, 0.15);

    &:hover {
      background: rgba(100, 100, 180, 0.18);
      color: #d0d0e0;
    }
  }

  &.danger {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(220, 38, 38, 0.15));
    color: #f87171;
    border: 1px solid rgba(239, 68, 68, 0.25);

    &:hover {
      background: linear-gradient(135deg, rgba(239, 68, 68, 0.3), rgba(220, 38, 38, 0.25));
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(239, 68, 68, 0.3);
    }
  }
}

.modal-hint {
  text-align: center;
  margin-top: 20px;
  font-size: 0.75rem;
  color: #6b6b8b;

  kbd {
    display: inline-block;
    padding: 2px 6px;
    font-size: 0.7rem;
    font-family: monospace;
    background: rgba(100, 100, 180, 0.1);
    border: 1px solid rgba(100, 100, 180, 0.15);
    border-radius: 4px;
    color: #8b8ba8;
  }
}

/* 动画 */
.logout-modal-enter-active,
.logout-modal-leave-active {
  transition: opacity 0.2s ease;

  .logout-modal {
    transition: transform 0.2s ease, opacity 0.2s ease;
  }
}

.logout-modal-enter-from,
.logout-modal-leave-to {
  opacity: 0;

  .logout-modal {
    transform: scale(0.95) translateY(10px);
    opacity: 0;
  }
}
</style>
