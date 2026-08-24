<template>
  <el-dialog
    v-model="visible"
    title="调整学习计划"
    width="560px"
    :close-on-click-modal="false"
    class="adjust-plan-dialog"
    align-center
  >
    <div class="dialog-content">
      <div v-if="planData.id" class="plan-info-card">
        <div class="info-header">
          <span class="info-label">当前计划</span>
          <span v-if="planData.isActive" class="active-badge">进行中</span>
        </div>
        <div class="info-row">
          <span class="row-label">计划名称</span>
          <span class="row-value">{{ planData.name || '未命名' }}</span>
        </div>
        <div class="info-row">
          <span class="row-label">总进度</span>
          <div class="progress-wrapper">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: (planData.completionPercentage || 0) + '%' }"></div>
            </div>
            <span class="progress-text">{{ Math.round(planData.completionPercentage || 0) }}%</span>
          </div>
        </div>
        <div class="info-row">
          <span class="row-label">已用时间</span>
          <span class="row-value">{{ planData.usedDays || 0 }} 天 / 预估 {{ planData.totalDays || 0 }} 天</span>
        </div>
      </div>

      <div v-if="planData.id" class="divider"></div>

      <div class="adjust-options">
        <div
          v-for="option in adjustOptions"
          :key="option.value"
          class="option-card"
          :class="{ active: selectedOption === option.value }"
          @click="selectOption(option.value)"
        >
          <div class="option-icon">{{ option.icon }}</div>
          <div class="option-content">
            <div class="option-title">{{ option.title }}</div>
            <div class="option-desc">{{ option.desc }}</div>
          </div>
          <div v-if="selectedOption === option.value" class="option-check">
            <Check :size="16" />
          </div>
        </div>

        <div v-if="selectedOption === 'switch'" class="switch-plan-section">
          <div class="section-tip">选择一个已有计划切换为当前计划</div>
          <div v-if="pathsLoading" class="switch-loading">
            <span class="loading-text">加载中...</span>
          </div>
          <div v-else-if="pathList.length === 0" class="switch-empty">
            <span>暂无其他学习计划</span>
          </div>
          <div v-else class="path-list">
            <div
              v-for="p in pathList"
              :key="p.id"
              class="path-item"
              :class="{ selected: p.id === planData.id, current: p.isActive }"
              @click.stop="p.id !== planData.id && (selectedPathId = p.id)"
            >
              <div class="path-item-info">
                <div class="path-item-name">{{ p.name || '未命名计划' }}</div>
                <div class="path-item-meta">
                  <span class="meta-progress">{{ Math.round(p.completionPercentage || 0) }}%</span>
                  <span v-if="p.isActive" class="path-current-badge">当前</span>
                </div>
              </div>
              <div v-if="selectedPathId === p.id" class="path-check">
                <Check :size="14" />
              </div>
            </div>
          </div>
        </div>

        <div v-if="selectedOption === 'generate'" class="generate-section">
          <div class="generate-input-group">
            <label class="generate-label">学习目标 <span class="required">*</span></label>
            <el-input
              v-model="goalInput"
              type="textarea"
              :rows="3"
              placeholder="请输入你的学习目标，例如：学习 Python 数据分析，掌握 Pandas、NumPy 等库"
              class="generate-input"
            />
          </div>
          <div class="generate-input-group">
            <label class="generate-label">目标领域</label>
            <el-select
              v-model="targetField"
              placeholder="选择学习领域"
              class="generate-select"
              clearable
            >
              <el-option label="编程开发" value="programming" />
              <el-option label="数据分析" value="data_analysis" />
              <el-option label="人工智能" value="ai_ml" />
              <el-option label="Web 开发" value="web_dev" />
              <el-option label="移动开发" value="mobile_dev" />
              <el-option label="云计算" value="cloud" />
              <el-option label="网络安全" value="security" />
              <el-option label="其他" value="other" />
            </el-select>
          </div>
          <div class="generate-input-group">
            <label class="generate-label">计划周期</label>
            <el-select
              v-model="planDuration"
              placeholder="选择计划周期"
              class="generate-select"
            >
              <el-option label="1 个月" :value="1" />
              <el-option label="3 个月" :value="3" />
              <el-option label="6 个月" :value="6" />
            </el-select>
          </div>
        </div>

        <div v-if="selectedOption === 'manual'" class="manual-section">
          <div class="manual-tip">
            <span class="tip-icon">💡</span>
            <span>点击确认后将跳转到学习路径编辑页面，您可以拖拽调整节点顺序和内容</span>
          </div>
        </div>

        <div v-if="selectedOption === 'optimize'" class="optimize-section">
          <div class="optimize-tip">
            <span class="tip-icon">✨</span>
            <span>AI 将根据您的学习记录、掌握程度和薄弱知识点，自动调整学习计划的任务优先级和预计时间</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button class="btn-cancel" @click="visible = false">取消</el-button>
        <el-button class="btn-confirm" type="primary" :disabled="confirming" @click="confirmAdjust">
          <span v-if="confirming" class="loading-spinner-small"></span>
          <span v-else>确认调整</span>
        </el-button>
      </div>
    </template>

    <ResetConfirmDialog
      v-model="showResetConfirm"
      :plan-data="resetPlanData"
      @confirm="handleResetConfirm"
    />
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check } from 'lucide-vue-next'
import { getPathList, switchPath, resetPath, generatePath, optimizePath } from '@/api/learningPath'
import ResetConfirmDialog from './ResetConfirmDialog.vue'

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
      completionPercentage: 0,
      currentStage: '',
      usedDays: 0,
      totalDays: 0,
      isActive: false
    })
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const router = useRouter()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const selectedOption = ref('optimize')
const selectedPathId = ref(null)
const pathList = ref([])
const pathsLoading = ref(false)
const goalInput = ref('')
const targetField = ref('')
const planDuration = ref(3)
const confirming = ref(false)

const showResetConfirm = ref(false)
const resetPlanData = ref({})

const adjustOptions = [
  {
    value: 'optimize',
    icon: '✨',
    title: '智能优化',
    desc: 'AI 根据学习数据自动优化计划'
  },
  {
    value: 'switch',
    icon: '🔄',
    title: '选择已有计划',
    desc: '从已有学习路径中选择切换'
  },
  {
    value: 'generate',
    icon: '➕',
    title: '生成新计划',
    desc: '根据新目标生成学习路径'
  },
  {
    value: 'manual',
    icon: '📝',
    title: '手动调整',
    desc: '跳转到编辑页面调整节点'
  },
  {
    value: 'reset',
    icon: '🗑️',
    title: '重置计划',
    desc: '清空进度，重新开始学习'
  }
]

watch(() => props.modelValue, async (newVal) => {
  if (newVal) {
    selectedOption.value = 'optimize'
    goalInput.value = ''
    targetField.value = ''
    planDuration.value = 3
    selectedPathId.value = null
    confirming.value = false
  }
})

const selectOption = (value) => {
  selectedOption.value = value
  if (value === 'switch') {
    fetchPathList()
  }
}

const fetchPathList = async () => {
  pathsLoading.value = true
  try {
    const res = await getPathList()
    const list = res?.data ?? res
    pathList.value = Array.isArray(list) ? list : []
    if (props.planData.id) {
      selectedPathId.value = null
    }
  } catch (error) {
    console.error('获取路径列表失败:', error)
    pathList.value = []
  } finally {
    pathsLoading.value = false
  }
}

const confirmAdjust = async () => {
  confirming.value = true
  try {
    if (selectedOption.value === 'optimize') {
      if (!props.planData.id) {
        ElMessage.warning('当前没有可优化的计划')
        return
      }
      await optimizePath(props.planData.id)
      ElMessage.success('✨ 计划已智能优化')
      visible.value = false
      emit('confirm', 'optimize')
    } else if (selectedOption.value === 'switch') {
      if (!selectedPathId.value) {
        ElMessage.warning('请选择一个计划')
        return
      }
      if (selectedPathId.value === props.planData.id) {
        ElMessage.info('当前已是该计划')
        return
      }
      await switchPath(selectedPathId.value)
      ElMessage.success('🔄 计划切换成功')
      visible.value = false
      emit('confirm', 'switch')
    } else if (selectedOption.value === 'generate') {
      if (!goalInput.value.trim()) {
        ElMessage.warning('请输入学习目标')
        return
      }
      await generatePath({
        goal: goalInput.value.trim(),
        targetField: targetField.value,
        duration: planDuration.value
      })
      ElMessage.success('➕ 新计划已生成')
      visible.value = false
      emit('confirm', 'generate')
    } else if (selectedOption.value === 'manual') {
      visible.value = false
      router.push(`/path/edit/${props.planData.id}`)
      emit('confirm', 'manual')
      return
    } else if (selectedOption.value === 'reset') {
      if (!props.planData.id) {
        ElMessage.warning('当前没有可重置的计划')
        return
      }
      resetPlanData.value = {
        ...props.planData,
        completedModules: Math.round((props.planData.completionPercentage || 0) / 100 * (props.planData.totalModules || 10))
      }
      showResetConfirm.value = true
      return
    }
  } catch (error) {
    console.error('调整计划失败:', error)
    ElMessage.error('操作失败：' + (error?.response?.data?.message || error?.message || '请稍后重试'))
  } finally {
    confirming.value = false
  }
}

const handleResetConfirm = async (pathId) => {
  try {
    await resetPath(pathId)
    ElMessage.success('🗑️ 计划已重置，新路径已就绪')
    visible.value = false
    emit('confirm', 'reset')
  } catch (error) {
    console.error('重置计划失败:', error)
    ElMessage.error('重置失败：' + (error?.response?.data?.message || error?.message || '请稍后重试'))
  }
}
</script>

<style scoped>
@use '../styles/variables' as *;
.dialog-content {
  padding: 8px 0;
}

.plan-info-card {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 20px;
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-secondary, 0.1);
}

.info-label {
  font-size: 0.85rem;
  color: $accent-primary;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.active-badge {
  font-size: 0.7rem;
  padding: 2px 8px;
  background: rgba($accent-primary, 0.1);
  color: $accent-primary;
  border-radius: 4px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.info-row:not(:last-child) {
  border-bottom: 1px solid rgba($accent-secondary, 0.06);
}

.row-label {
  font-size: 0.85rem;
  color: $text-secondary;
  flex-shrink: 0;
}

.row-value {
  font-size: 0.9rem;
  color: $text-primary;
  font-weight: 500;
  text-align: right;
}

.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  justify-content: flex-end;
}

.progress-track {
  width: 100px;
  height: 6px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $accent-primary, #10B981);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.85rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: #10B981;
  min-width: 40px;
}

.divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba($accent-secondary, 0.15), transparent);
  margin: 24px 0;
}

.adjust-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
}

.option-card:hover {
  transform: translateY(-2px);
  border-color: rgba($accent-primary, 0.25);
  background: rgba($accent-primary, 0.04);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.option-card.active {
  border-color: rgba($accent-primary, 0.5);
  background: rgba($accent-primary, 0.08);
}

.option-icon {
  font-size: 1.3rem;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-secondary, 0.08);
  border-radius: 10px;
  flex-shrink: 0;
}

.option-card.active .option-icon {
  background: rgba($accent-primary, 0.12);
}

.option-content {
  flex: 1;
}

.option-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 3px;
}

.option-desc {
  font-size: 0.78rem;
  color: $text-secondary;
}

.option-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, $accent-primary, #0055FF);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 切换计划列表 */
.switch-plan-section {
  margin-top: 10px;
  padding: 14px 16px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 10px;
}

.section-tip {
  font-size: 0.8rem;
  color: $text-secondary;
  margin-bottom: 10px;
}

.switch-loading,
.switch-empty {
  text-align: center;
  padding: 16px;
  color: $text-secondary;
  font-size: 0.85rem;
}

.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.path-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.path-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.path-item:hover:not(.selected):not(.current) {
  border-color: rgba($accent-primary, 0.2);
  background: rgba($accent-primary, 0.04);
}

.path-item.selected {
  opacity: 0.5;
  cursor: not-allowed;
}

.path-item.current {
  border-color: rgba($accent-primary, 0.3);
  background: rgba($accent-primary, 0.06);
  cursor: not-allowed;
}

.path-item-info {
  flex: 1;
}

.path-item-name {
  font-size: 0.85rem;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 3px;
}

.path-item-meta {
  font-size: 0.75rem;
  color: $text-secondary;
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-progress {
  font-family: 'JetBrains Mono', monospace;
  color: #10B981;
}

.path-current-badge {
  font-size: 0.7rem;
  padding: 1px 6px;
  background: rgba($accent-primary, 0.1);
  color: $accent-primary;
  border-radius: 4px;
}

.path-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, $accent-primary, #0055FF);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

/* 生成计划输入 */
.generate-section {
  margin-top: 10px;
  padding: 14px 16px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.generate-input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.generate-label {
  font-size: 0.82rem;
  color: $text-secondary;
  font-weight: 500;
}

.required {
  color: #EF4444;
}

.generate-input {
  --el-input-bg-color: rgba(255, 255, 255, 0.04);
  --el-input-border-color: rgba($accent-secondary, 0.15);
  --el-input-hover-border-color: rgba($accent-primary, 0.3);
  --el-input-focus-border-color: $accent-primary;
  --el-input-text-color: $text-primary;
  --el-input-placeholder-color: $text-secondary;
}

.generate-input .el-textarea__inner {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba($accent-secondary, 0.15);
  color: $text-primary;
  border-radius: 8px;
  font-size: 0.85rem;
  resize: vertical;
}

.generate-input .el-textarea__inner:focus {
  border-color: $accent-primary;
  box-shadow: 0 0 8px rgba($accent-primary, 0.1);
}

.generate-select {
  --el-select-border-color-hover: rgba($accent-primary, 0.3);
  --el-select-input-focus-border-color: $accent-primary;
}

.generate-select .el-input__wrapper {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 8px;
  box-shadow: none;
}

.generate-select .el-input__wrapper:hover {
  border-color: rgba($accent-primary, 0.3);
}

.generate-select .el-input.is-focus .el-input__wrapper {
  border-color: $accent-primary;
  box-shadow: 0 0 8px rgba($accent-primary, 0.1);
}

.generate-select .el-input__inner {
  color: $text-primary;
}

/* 手动调整 */
.manual-section {
  margin-top: 10px;
  padding: 14px 16px;
  background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.15);
  border-radius: 10px;
}

.manual-tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 0.82rem;
  color: $text-secondary;
  line-height: 1.5;
}

.tip-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

/* 智能优化 */
.optimize-section {
  margin-top: 10px;
  padding: 14px 16px;
  background: rgba(124, 58, 237, 0.06);
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: 10px;
}

.optimize-tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 0.82rem;
  color: $text-secondary;
  line-height: 1.5;
}

.loading-spinner-small {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-cancel {
  padding: 10px 24px;
  background: rgba($accent-secondary, 0.08);
  border: 1px solid rgba($accent-secondary, 0.2);
  color: $text-secondary;
  border-radius: 8px;
  font-size: 0.9rem;
  transition: all 0.25s ease;
}

.btn-cancel:hover {
  color: $text-primary;
  border-color: rgba($accent-secondary, 0.3);
  background: rgba($accent-secondary, 0.12);
}

.btn-confirm {
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.2), rgba(0, 85, 255, 0.15));
  border: 1px solid rgba($accent-primary, 0.3);
  color: $accent-primary;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.25s ease;
}

.btn-confirm:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba($accent-primary, 0.2);
  border-color: rgba($accent-primary, 0.5);
}

.btn-confirm:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>

<style>
.adjust-plan-dialog.el-dialog {
  background: rgba($bg-primary, 0.95);
  backdrop-filter: blur(24px);
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 16px;
}

.adjust-plan-dialog .el-dialog__header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid rgba($accent-secondary, 0.1);
  margin-right: 0;
}

.adjust-plan-dialog .el-dialog__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: $text-primary;
}

.adjust-plan-dialog .el-dialog__headerbtn .el-dialog__close {
  color: $text-secondary;
  font-size: 1.2rem;
}

.adjust-plan-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: $accent-primary;
}

.adjust-plan-dialog .el-dialog__body {
  padding: 24px;
}

.adjust-plan-dialog .el-dialog__footer {
  padding: 16px 24px 20px;
}
</style>