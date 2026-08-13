<template>
  <div v-if="steps.length > 0" class="react-visualization">
    <div class="react-header">
      <span class="react-icon">🧠</span>
      <span class="react-title">AI思考过程</span>
      <el-button
        class="toggle-btn"
        size="small"
        @click="isExpanded = !isExpanded"
      >
        {{ isExpanded ? '收起' : '展开' }}
      </el-button>
    </div>

    <div v-show="isExpanded" class="react-steps">
      <div
        v-for="(step, index) in steps"
        :key="step.id"
        class="react-step"
        :class="[step.type, step.status]"
      >
        <div class="step-indicator">
          <div class="step-number">{{ index + 1 }}</div>
          <div v-if="index < steps.length - 1" class="step-line"></div>
        </div>

        <div class="step-icon">
          {{ getStepIcon(step.type) }}
        </div>

        <div class="step-content">
          <div class="step-type">{{ getStepTypeName(step.type) }}</div>
          <div class="step-text">{{ step.content }}</div>
          <div class="step-meta">
            <span class="step-time">{{ formatTime(step.timestamp) }}</span>
            <span class="step-status" :class="step.status">
              <span class="status-dot"></span>
              {{ getStatusName(step.status) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div v-show="!isExpanded" class="react-summary">
      <span class="summary-text">已完成 {{ completedSteps }} 个步骤</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  steps: {
    type: Array,
    default: () => []
  }
})

const isExpanded = ref(true)

// 计算已完成步骤数量
const completedSteps = computed(() => {
  return props.steps.filter(step => step.status === 'completed').length
})

// 获取步骤图标
const getStepIcon = (type) => {
  const icons = {
    think: '🧠',
    act: '⚡',
    observe: '👀'
  }
  return icons[type] || '❓'
}

// 获取步骤类型名称
const getStepTypeName = (type) => {
  const names = {
    think: '思考 (Think)',
    act: '行动 (Act)',
    observe: '观察 (Observe)'
  }
  return names[type] || '未知'
}

// 获取状态名称
const getStatusName = (status) => {
  const names = {
    in_progress: '进行中',
    completed: '已完成',
    failed: '失败'
  }
  return names[status] || '未知'
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}
</script>

<style lang="scss" scoped>
.react-visualization {
  margin-top: 16px;
  padding: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
}

.react-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  .react-icon {
    font-size: 1.2rem;
  }

  .react-title {
    font-size: 1rem;
    font-weight: 600;
    color: var(--text-primary);
    flex: 1;
  }

  .toggle-btn {
    font-size: 0.8rem;
  }
}

.react-steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.react-step {
  display: flex;
  gap: 12px;
  transition: all 0.3s ease;

  &.in_progress {
    opacity: 0.8;
  }

  &.completed {
    opacity: 1;
  }

  &.failed {
    opacity: 0.6;
  }
}

.step-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;

  .step-number {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--accent-primary);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.75rem;
    font-weight: 600;
    flex-shrink: 0;
  }

  .step-line {
    width: 2px;
    flex: 1;
    background: var(--border-subtle);
    margin-top: 4px;
    min-height: 20px;
  }
}

.step-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-content {
  flex: 1;
  min-width: 0;

  .step-type {
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .step-text {
    font-size: 0.9rem;
    color: #CBD5E1;
    line-height: 1.5;
    margin-bottom: 8px;
  }

  .step-meta {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 0.85rem;
    color: #94A3B8;

    .step-time {
      font-family: var(--font-mono);
    }

    .step-status {
      display: flex;
      align-items: center;
      gap: 6px;

      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--text-muted);

        .in_progress & {
          background: var(--accent-amber);
          animation: pulse 1.5s infinite;
        }

        .completed & {
          background: var(--accent-emerald);
        }

        .failed & {
          background: var(--accent-red);
        }
      }
    }
  }
}

// 不同步骤类型的颜色
.react-step {
  &.think {
    .step-number {
      background: var(--accent-primary);
    }
  }

  &.act {
    .step-number {
      background: var(--accent-amber);
    }
  }

  &.observe {
    .step-number {
      background: var(--accent-emerald);
    }
  }
}

.react-summary {
  padding: 8px 0;
  text-align: center;

  .summary-text {
    font-size: 0.9rem;
    color: var(--text-muted);
  }
}

// 动画
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>