<template>
  <el-dialog
    v-model="visible"
    title=""
    width="480px"
    :close-on-click-modal="false"
    class="reset-confirm-dialog"
    align-center
  >
    <div class="dialog-header">
      <div class="warning-icon">⚠️</div>
      <h3 class="dialog-title">确认重置学习计划</h3>
    </div>

    <div class="dialog-content">
      <div class="plan-summary-card">
        <div class="summary-header">
          <span class="summary-label">即将重置的计划</span>
        </div>
        <div class="summary-row">
          <span class="row-label">计划名称</span>
          <span class="row-value">{{ planData.name || '未命名学习计划' }}</span>
        </div>
        <div class="summary-row">
          <span class="row-label">当前进度</span>
          <span class="row-value highlight">{{ planData.progress || 0 }}%</span>
        </div>
        <div class="summary-row">
          <span class="row-label">已用时间</span>
          <span class="row-value">{{ planData.usedDays || 0 }} 天</span>
        </div>
        <div class="summary-row">
          <span class="row-label">当前阶段</span>
          <span class="row-value stage">{{ planData.currentStage || '未知' }}</span>
        </div>
        <div class="summary-row">
          <span class="row-label">已完成模块</span>
          <span class="row-value">{{ planData.completedModules || 0 }} / {{ planData.totalModules || 0 }} 个</span>
        </div>
      </div>

      <div class="warning-box">
        <div class="warning-icon-small">🚨</div>
        <p class="warning-text">此操作将删除所有学习进度和记录，不可恢复！</p>
      </div>

      <div class="confirm-input-group">
        <label class="confirm-label">请输入计划名称确认重置</label>
        <el-input
          v-model="confirmName"
          :placeholder="planData.name || '请输入计划名称'"
          class="confirm-input"
          clearable
        />
        <p v-if="confirmName && confirmName !== planData.name" class="confirm-hint">
          输入名称不匹配，请重新输入
        </p>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button class="btn-cancel" @click="handleCancel">取消</el-button>
        <el-button
          class="btn-danger"
          type="primary"
          :disabled="!isConfirmValid"
          :class="{ 'btn-danger-active': isConfirmValid }"
          @click="handleConfirm"
        >
          确认重置
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

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
      progress: 0,
      currentStage: '',
      usedDays: 0,
      totalDays: 0,
      completedModules: 0,
      totalModules: 0
    })
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const confirmName = ref('')

const isConfirmValid = computed(() => {
  return confirmName.value.trim() === (props.planData.name || '').trim()
})

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    confirmName.value = ''
  }
})

const handleCancel = () => {
  visible.value = false
}

const handleConfirm = () => {
  if (!isConfirmValid.value) {
    ElMessage.warning('请输入正确的计划名称以确认重置')
    return
  }
  emit('confirm', props.planData.id)
  visible.value = false
}
</script>

<style scoped>
.dialog-header {
  text-align: center;
  padding: 8px 0 20px;
}

.dialog-header .warning-icon {
  font-size: 3rem;
  margin-bottom: 12px;
}

.dialog-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #F1F5F9;
  margin: 0;
}

.dialog-content {
  padding: 0 4px;
}

.plan-summary-card {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.summary-header {
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(100, 100, 180, 0.1);
}

.summary-label {
  font-size: 0.8rem;
  color: #00E5FF;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.summary-row:not(:last-child) {
  border-bottom: 1px solid rgba(100, 100, 180, 0.06);
}

.row-label {
  font-size: 0.85rem;
  color: #94A3B8;
  flex-shrink: 0;
}

.row-value {
  font-size: 0.9rem;
  color: #F1F5F9;
  font-weight: 500;
  text-align: right;
}

.row-value.highlight {
  color: #10B981;
  font-weight: 600;
}

.row-value.stage {
  color: #00E5FF;
  font-size: 0.85rem;
}

.warning-box {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 10px;
  margin-bottom: 16px;
}

.warning-icon-small {
  font-size: 1.2rem;
  flex-shrink: 0;
}

.warning-text {
  font-size: 0.85rem;
  color: #FCA5A5;
  margin: 0;
  line-height: 1.5;
}

.confirm-input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.confirm-label {
  font-size: 0.85rem;
  color: #94A3B8;
  font-weight: 500;
}

.confirm-input {
  --el-input-bg-color: rgba(255, 255, 255, 0.04);
  --el-input-border-color: rgba(100, 100, 180, 0.15);
  --el-input-hover-border-color: rgba(0, 229, 255, 0.3);
  --el-input-focus-border-color: #00E5FF;
  --el-input-text-color: #F1F5F9;
  --el-input-placeholder-color: #94A3B8;
}

.confirm-input .el-input__wrapper {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 8px;
  box-shadow: none;
}

.confirm-input .el-input__wrapper:hover {
  border-color: rgba(239, 68, 68, 0.4);
}

.confirm-input .el-input__wrapper.is-focus {
  border-color: #EF4444;
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.15);
}

.confirm-input .el-input__inner {
  color: #F1F5F9;
  font-size: 0.9rem;
}

.confirm-hint {
  font-size: 0.75rem;
  color: #EF4444;
  margin: 0;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-cancel {
  padding: 10px 24px;
  background: rgba(100, 100, 180, 0.08);
  border: 1px solid rgba(100, 100, 180, 0.2);
  color: #94A3B8;
  border-radius: 8px;
  font-size: 0.9rem;
  transition: all 0.25s ease;
}

.btn-cancel:hover {
  color: #F1F5F9;
  border-color: rgba(100, 100, 180, 0.3);
  background: rgba(100, 100, 180, 0.12);
}

.btn-danger {
  padding: 10px 24px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #FCA5A5;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.25s ease;
}

.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-danger-active {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.3), rgba(220, 38, 38, 0.2));
  border-color: rgba(239, 68, 68, 0.5);
  color: #FCA5A5;
}

.btn-danger-active:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(239, 68, 68, 0.25);
  border-color: rgba(239, 68, 68, 0.6);
}
</style>

<style>
.reset-confirm-dialog.el-dialog {
  background: rgba(17, 17, 39, 0.98);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 16px;
}

.reset-confirm-dialog .el-dialog__header {
  padding: 20px 24px 0;
  margin-right: 0;
}

.reset-confirm-dialog .el-dialog__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #F1F5F9;
}

.reset-confirm-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #94A3B8;
  font-size: 1.2rem;
}

.reset-confirm-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: #EF4444;
}

.reset-confirm-dialog .el-dialog__body {
  padding: 0 24px 8px;
}

.reset-confirm-dialog .el-dialog__footer {
  padding: 16px 24px 20px;
}
</style>