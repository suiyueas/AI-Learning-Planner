<template>
  <div class="workflow-steps">
    <div
      v-for="(step, index) in steps"
      :key="index"
      class="step-item"
      :class="{ active: step.active, completed: step.completed, error: step.error }"
    >
      <div class="step-indicator">
        <span v-if="step.completed" class="step-icon">✓</span>
        <span v-else-if="step.error" class="step-icon">✕</span>
        <span v-else-if="step.active" class="step-icon spin">⟳</span>
        <span v-else class="step-index">{{ index + 1 }}</span>
      </div>
      <div class="step-content">
        <span class="step-label">{{ step.label }}</span>
        <span v-if="step.detail" class="step-detail">{{ step.detail }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  steps: {
    type: Array,
    default: () => []
  }
})
</script>

<style lang="scss" scoped>
.workflow-steps {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
  transition: all 0.2s ease;

  &.active {
    background: rgba(124, 97, 255, 0.08);
  }

  &.completed {
    background: rgba(16, 185, 129, 0.06);
  }

  &.error {
    background: rgba(239, 68, 68, 0.06);
  }
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 0.75rem;
  font-weight: 600;

  .step-index {
    color: #6b6b8b;
  }

  .step-icon {
    color: #10b981;
  }

  .step-icon.spin {
    animation: spin 1s linear infinite;
    color: #7c61ff;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.step-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-label {
  font-size: 0.85rem;
  color: #e0e0f0;
  font-weight: 500;
}

.step-detail {
  font-size: 0.78rem;
  color: #8b8ba8;
}
</style>