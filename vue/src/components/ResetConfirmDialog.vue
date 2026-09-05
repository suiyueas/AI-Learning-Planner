<template>
  <el-dialog
    v-model="visible"
    title="确认重置计划"
    width="480px"
    :close-on-click-modal="false"
    class="reset-confirm-dialog"
    align-center
  >
    <div class="dialog-content">
      <div class="warning-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="48" height="48">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 8v4M12 16h.01" />
        </svg>
      </div>
      <div class="warning-text">
        <p class="title">确定要重置学习计划吗？</p>
        <p class="desc">此操作将清空 "{{ planData.name }}" 的所有学习进度，包括：</p>
      </div>
      <div class="reset-details">
        <div class="detail-item">
          <span class="detail-label">已完成模块</span>
          <span class="detail-value">{{ planData.completedModules || 0 }} 个</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">已用时间</span>
          <span class="detail-value">{{ planData.usedDays || 0 }} 天</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">当前进度</span>
          <span class="detail-value">{{ Math.round(planData.completionPercentage || 0) }}%</span>
        </div>
      </div>
      <div class="notice">
        ⚠️ 重置后系统将自动生成新的学习路径，原进度数据不可恢复
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button class="btn-cancel" @click="visible = false">取消</el-button>
        <el-button class="btn-danger" type="danger" @click="handleConfirm">
          确认重置
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  planData: {
    type: Object,
    default: () => ({
      id: '',
      name: '',
      completionPercentage: 0,
      usedDays: 0,
      completedModules: 0
    })
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleConfirm = () => {
  emit('confirm', props.planData.id)
  visible.value = false
}
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.dialog-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  gap: 16px;
}

.warning-icon {
  color: var(--color-warning);
  opacity: 0.8;
}

.warning-text {
  text-align: center;
}

.warning-text .title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.warning-text .desc {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 0;
}

.reset-details {
  width: 100%;
  background: rgba(var(--color-warning), 0.05);
  border: 1px solid rgba(var(--color-warning), 0.15);
  border-radius: 8px;
  padding: 12px 16px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.detail-item:not(:last-child) {
  border-bottom: 1px solid rgba(var(--color-warning), 0.08);
}

.detail-label {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.detail-value {
  font-size: 0.9rem;
  color: var(--text-primary);
  font-weight: 500;
}

.notice {
  font-size: 0.8rem;
  color: var(--color-warning);
  text-align: center;
  line-height: 1.5;
  padding: 8px 12px;
  background: rgba(var(--color-warning), 0.05);
  border-radius: 6px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn-cancel {
  background: rgba(var(--border-default), 0.3);
  border: 1px solid rgba(var(--border-default), 0.5);
  color: var(--text-secondary);
}

.btn-danger {
  background: rgba(var(--color-danger), 0.2);
  border: 1px solid rgba(var(--color-danger), 0.3);
  color: var(--color-danger);
}

.btn-danger:hover {
  background: rgba(var(--color-danger), 0.3);
  border-color: var(--color-danger);
}
</style>