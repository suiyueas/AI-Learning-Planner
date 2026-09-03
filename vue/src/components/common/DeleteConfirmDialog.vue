<template>
  <Teleport to="body">
    <Transition name="delete-modal">
      <div v-if="visible" class="delete-confirm-overlay" @click.self="handleCancel">
        <div class="delete-confirm-modal">
          <!-- 头部图标 -->
          <div class="modal-icon-wrap">
            <div class="modal-icon" :class="type">
              <!-- danger: 警告三角 -->
              <svg v-if="type === 'danger'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <!-- warning: 刷新/重试 -->
              <svg v-else-if="type === 'warning'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                <polyline points="22 2 22 8 16 8"/>
              </svg>
              <!-- info: 工具/修复 -->
              <svg v-else-if="type === 'info'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
              </svg>
              <!-- success: 对勾 -->
              <svg v-else-if="type === 'success'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <!-- 默认: 垃圾桶 -->
              <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                <line x1="10" y1="11" x2="10" y2="17"/>
                <line x1="14" y1="11" x2="14" y2="17"/>
              </svg>
            </div>
          </div>

          <!-- 标题 -->
          <h3 class="modal-title">{{ title }}</h3>

          <!-- 描述信息 -->
          <div class="modal-desc">
            <p class="desc-main">{{ message }}</p>
            <div v-if="details" class="desc-details">
              <div class="detail-item" v-for="(item, index) in details" :key="index">
                <span class="detail-icon">{{ item.icon }}</span>
                <span class="detail-text">{{ item.text }}</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="modal-actions">
            <button class="action-btn cancel" @click="handleCancel">
              {{ cancelButtonText }}
            </button>
            <button 
              v-if="showSoftDelete" 
              class="action-btn soft" 
              @click="handleSoftDelete"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
              移至回收站
            </button>
            <button :class="['action-btn', effectiveConfirmType]" @click="handleHardDelete">
              <svg v-if="effectiveConfirmType === 'danger' || effectiveConfirmType === 'warning'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                <line x1="10" y1="11" x2="10" y2="17"/>
                <line x1="14" y1="11" x2="14" y2="17"/>
              </svg>
              <svg v-else-if="effectiveConfirmType === 'success'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <svg v-else-if="effectiveConfirmType === 'primary'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              {{ displayConfirmText }}
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
import { onMounted, onUnmounted, computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '确认删除' },
  message: { type: String, default: '确定要删除这条记录吗？' },
  type: { type: String, default: 'warning' }, // warning, danger, info, success
  cancelButtonText: { type: String, default: '取消' },
  confirmButtonText: { type: String, default: '' },
  confirmButtonType: { type: String, default: '' }, // danger, warning, primary, success, info
  showSoftDelete: { type: Boolean, default: true },
  details: { type: Array, default: () => [] }
})

const emit = defineEmits(['cancel', 'soft-delete', 'hard-delete'])

const handleCancel = () => {
  emit('cancel')
}

const handleSoftDelete = () => {
  emit('soft-delete')
}

const handleHardDelete = () => {
  emit('hard-delete')
}

// 计算确认按钮类型（优先使用 confirmButtonType，否则根据 type 推断）
const effectiveConfirmType = computed(() => {
  if (props.confirmButtonType) {
    return props.confirmButtonType
  }
  // 根据对话框类型推断按钮类型
  return props.type === 'danger' ? 'danger' : 'warning'
})

// 计算确认按钮文本（优先使用 confirmButtonText，否则显示默认文本）
const displayConfirmText = computed(() => {
  if (props.confirmButtonText) {
    return props.confirmButtonText
  }
  // 根据按钮类型显示默认文本
  const typeMap = {
    danger: '永久删除',
    warning: '确认执行',
    primary: '确认',
    success: '确认执行',
    info: '确认'
  }
  return typeMap[effectiveConfirmType.value] || '确认'
})

const handleKeydown = (e) => {
  if (!props.visible) return
  if (e.key === 'Escape') {
    handleCancel()
  } else if (e.key === 'Enter') {
    if (props.showSoftDelete) {
      handleSoftDelete()
    } else {
      handleHardDelete()
    }
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
.delete-confirm-overlay {
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

.delete-confirm-modal {
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
  
  &.danger {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(220, 38, 38, 0.1));
    color: #ef4444;
    border: 2px solid rgba(239, 68, 68, 0.2);
  }
  
  &.info {
    background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(124, 58, 237, 0.1));
    color: #a78bfa;
    border: 2px solid rgba(139, 92, 246, 0.2);
  }
  
  &.success {
    background: linear-gradient(135deg, rgba(34, 197, 94, 0.15), rgba(22, 163, 74, 0.1));
    color: #4ade80;
    border: 2px solid rgba(34, 197, 94, 0.2);
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
  margin: 0 0 16px;
}

.desc-details {
  background: rgba(100, 100, 180, 0.06);
  border-radius: 12px;
  padding: 12px 16px;
  text-align: left;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  
  &:not(:last-child) {
    border-bottom: 1px solid rgba(100, 100, 180, 0.08);
  }
}

.detail-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.detail-text {
  font-size: 0.85rem;
  color: #c0c0e0;
  line-height: 1.5;
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
  
  &.soft {
    background: linear-gradient(135deg, rgba(124, 97, 245, 0.2), rgba(99, 102, 241, 0.15));
    color: #a78bfa;
    border: 1px solid rgba(124, 97, 245, 0.25);
    
    &:hover {
      background: linear-gradient(135deg, rgba(124, 97, 245, 0.3), rgba(99, 102, 241, 0.25));
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(124, 97, 245, 0.3);
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
  
  &.primary {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(37, 99, 235, 0.15));
    color: #60a5fa;
    border: 1px solid rgba(59, 130, 246, 0.25);
    
    &:hover {
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.3), rgba(37, 99, 235, 0.25));
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(59, 130, 246, 0.3);
    }
  }
  
  &.success {
    background: linear-gradient(135deg, rgba(34, 197, 94, 0.2), rgba(22, 163, 74, 0.15));
    color: #4ade80;
    border: 1px solid rgba(34, 197, 94, 0.25);
    
    &:hover {
      background: linear-gradient(135deg, rgba(34, 197, 94, 0.3), rgba(22, 163, 74, 0.25));
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(34, 197, 94, 0.3);
    }
  }
  
  &.info {
    background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(124, 58, 237, 0.15));
    color: #a78bfa;
    border: 1px solid rgba(139, 92, 246, 0.25);
    
    &:hover {
      background: linear-gradient(135deg, rgba(139, 92, 246, 0.3), rgba(124, 58, 237, 0.25));
      transform: translateY(-2px);
      box-shadow: 0 4px 20px rgba(139, 92, 246, 0.3);
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
.delete-modal-enter-active,
.delete-modal-leave-active {
  transition: opacity 0.2s ease;
  
  .delete-confirm-modal {
    transition: transform 0.2s ease, opacity 0.2s ease;
  }
}

.delete-modal-enter-from,
.delete-modal-leave-to {
  opacity: 0;
  
  .delete-confirm-modal {
    transform: scale(0.95) translateY(10px);
    opacity: 0;
  }
}
</style>
