<template>
  <div class="workflow-steps">
    <div
      v-for="step in steps"
      :key="step.id"
      class="step-item"
      :class="step.status"
    >
      <div class="step-indicator">
        <span v-if="step.status === 'done'" class="step-check">✓</span>
        <span v-else-if="step.status === 'error'" class="step-error">✕</span>
        <span v-else-if="step.status === 'processing'" class="step-spinner"></span>
        <span v-else class="step-dot"></span>
      </div>
      <div class="step-content">
        <div class="step-header">
          <span class="step-icon">{{ step.icon }}</span>
          <span class="step-label">{{ step.label }}</span>
        </div>
        <div v-if="step.detail" class="step-detail" :class="step.status">
          {{ step.detail }}
        </div>
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
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 16px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 10px;
  transition: all 0.3s ease;

  &.done {
    background: rgba(16, 185, 129, 0.08);
    border-color: rgba(16, 185, 129, 0.2);

    .step-label {
      color: #10b981;
    }
  }

  &.error {
    background: rgba(239, 68, 68, 0.08);
    border-color: rgba(239, 68, 68, 0.2);

    .step-label {
      color: #ef4444;
    }
  }

  &.processing {
    background: rgba(245, 158, 11, 0.08);
    border-color: rgba(245, 158, 11, 0.2);

    .step-label {
      color: #f59e0b;
    }
  }
}

.step-indicator {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-check {
  font-size: 0.85rem;
  font-weight: bold;
  color: #10b981;
}

.step-error {
  font-size: 0.85rem;
  font-weight: bold;
  color: #ef4444;
}

.step-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(245, 158, 11, 0.3);
  border-top-color: #f59e0b;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.step-dot {
  width: 8px;
  height: 8px;
  background: rgba(100, 100, 180, 0.4);
  border-radius: 50%;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-icon {
  font-size: 0.9rem;
}

.step-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-primary, #f0f0ff);
}

.step-detail {
  margin-top: 4px;
  font-size: 0.78rem;
  color: var(--text-muted, #8b8ba8);
  line-height: 1.4;

  &.done {
    color: #10b981;
  }

  &.error {
    color: #ef4444;
  }

  &.processing {
    color: #f59e0b;
  }
}
</style>