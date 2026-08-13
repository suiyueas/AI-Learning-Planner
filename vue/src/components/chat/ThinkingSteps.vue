<template>
  <div class="thinking-steps" :class="{ 'is-collapsed': collapsed }">
    <div
      v-for="(step, index) in steps"
      :key="step.id || index"
      class="step"
      :class="[step.status, { expanded: !collapsed && expandedSteps[step.id || index] }]"
    >
      <div class="step-header" @click="toggleStep(step.id || index)">
        <span class="step-num">步骤{{ index + 1 }}</span>
        <span class="step-title">{{ step.title || step.content }}</span>
        <span class="step-status-icon">{{ getStatusIcon(step.status) }}</span>
        <span class="step-arrow">{{ !collapsed && expandedSteps[step.id || index] ? '▼' : '▶' }}</span>
      </div>
      <div v-if="!collapsed && expandedSteps[step.id || index]" class="step-body">
        <div v-if="step.content" class="step-detail">{{ step.content }}</div>
        <ToolCallCard v-if="step.toolCall" :tool-call="step.toolCall" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import ToolCallCard from './ToolCallCard.vue'

const props = defineProps({
  steps: {
    type: Array,
    default: () => []
  },
  /**
   * 是否折叠所有步骤（任务完成后传入 true 自动折叠中间步骤）
   * 执行中传入 false 或不传，步骤可展开
   */
  collapsed: {
    type: Boolean,
    default: false
  }
})

const expandedSteps = ref({})

onMounted(() => {
  // 默认展开第一个步骤（仅在非折叠状态下）
  if (!props.collapsed && props.steps.length > 0) {
    expandedSteps.value[props.steps[0].id || 0] = true
  }
})

// 监听 collapsed 变化，任务完成后自动折叠
watch(() => props.collapsed, (newVal) => {
  if (newVal) {
    // 折叠：清空展开状态
    expandedSteps.value = {}
  } else {
    // 展开：默认展开第一个步骤
    if (props.steps.length > 0) {
      expandedSteps.value[props.steps[0].id || 0] = true
    }
  }
})

const toggleStep = (id) => {
  if (props.collapsed) return
  expandedSteps.value[id] = !expandedSteps.value[id]
}

const getStatusIcon = (status) => {
  const icons = {
    pending: '⏳',
    executing: '🔄',
    completed: '✅',
    error: '❌',
    doing: '🔄',
    done: '✅'
  }
  return icons[status] || '⏳'
}
</script>

<style lang="scss" scoped>
.thinking-steps {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 12px;
}

.step {
  background: rgba(100,100,180,0.06);
  border: 1px solid rgba(100,100,180,0.08);
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.2s ease;

  &.executing, &.doing {
    border-color: rgba(245, 158, 11, 0.3);
  }

  &.completed, &.done {
    border-color: rgba(16, 185, 129, 0.2);
  }

  &.error {
    border-color: rgba(239, 68, 68, 0.3);
  }
}

.step-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;

  &:hover {
    background: rgba(100,100,180,0.06);
  }
}

.step-num {
  font-size: 11px;
  font-weight: 600;
  color: #7b61ff;
  background: rgba(123, 97, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.step-title {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #e8e8ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-status-icon {
  font-size: 12px;
  flex-shrink: 0;
}

.step-arrow {
  font-size: 9px;
  color: #606090;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.step-body {
  padding: 0 10px 8px;
  border-top: 1px solid rgba(100,100,180,0.06);
  padding-top: 6px;
  margin-top: 2px;
}

.step-detail {
  font-size: 12px;
  color: #a0a0c8;
  line-height: 1.5;
}
</style>