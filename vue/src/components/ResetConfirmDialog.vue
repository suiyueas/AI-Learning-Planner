<template>
  <Teleport to="body">
    <Transition name="reset-modal">
      <div v-if="visible" class="reset-confirm-overlay" @click.self="handleCancel">
        <div class="reset-confirm-modal">
          <!-- 头部图标 -->
          <div class="modal-icon-wrap">
            <div class="modal-icon danger">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
          </div>

          <!-- 标题 -->
          <h3 class="modal-title">确认重置计划</h3>

          <!-- 描述信息 -->
          <div class="modal-desc">
            <p class="desc-main">确定要重置学习计划「{{ planData.name || '未命名计划' }}」吗？</p>
            <div class="desc-details">
              <div class="detail-item">
                <span class="detail-icon">📊</span>
                <span class="detail-text">当前进度：{{ Math.round(planData.completionPercentage || 0) }}%</span>
              </div>
              <div class="detail-item">
                <span class="detail-icon">📝</span>
                <span class="detail-text">已完模块：{{ planData.completedModules || 0 }} 个</span>
              </div>
              <div class="detail-item">
                <span class="detail-icon">⏱️</span>
                <span class="detail-text">已用时间：{{ planData.usedDays || 0 }} 天</span>
              </div>
            </div>
            <p class="desc-warning">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              此操作将清空所有学习进度，AI 将根据你的目标重新生成学习路径，不可撤销！
            </p>
          </div>

          <!-- 操作按钮 -->
          <div class="modal-actions">
            <button class="action-btn cancel" @click="handleCancel">
              取消
            </button>
            <button class="action-btn danger" @click="handleConfirm">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
              确认重置
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  planData: {
    type: Object,
    default: () => ({
      id: '',
      name: '',
      completionPercentage: 0,
      completedModules: 0,
      usedDays: 0
    })
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleCancel = () => {
  visible.value = false
}

const handleConfirm = () => {
  if (props.planData.id) {
    emit('confirm', props.planData.id)
  }
  visible.value = false
}

const handleKeydown = (e) => {
  if (!props.modelValue) return
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
.reset-confirm-overlay {
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

.reset-confirm-modal {
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

  &.danger {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(220, 38, 38, 0.1));
    color: #ef4444;
    border: 2px solid rgba(239, 68, 68, 0.2);
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
  margin-bottom: 16px;
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

.desc-warning {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 0.8rem;
  color: #fbbf24;
  line-height: 1.5;
  margin: 0;
  padding: 12px;
  background: rgba(251, 191, 36, 0.08);
  border-radius: 10px;
  border: 1px solid rgba(251, 191, 36, 0.15);
  text-align: left;

  svg {
    flex-shrink: 0;
    margin-top: 2px;
  }
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
  padding: 12px 24px;
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

/* 动画 */
.reset-modal-enter-active,
.reset-modal-leave-active {
  transition: opacity 0.2s ease;

  .reset-confirm-modal {
    transition: transform 0.2s ease, opacity 0.2s ease;
  }
}

.reset-modal-enter-from,
.reset-modal-leave-to {
  opacity: 0;

  .reset-confirm-modal {
    transform: scale(0.95) translateY(10px);
    opacity: 0;
  }
}
</style>