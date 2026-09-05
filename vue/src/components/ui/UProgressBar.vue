<template>
  <div class="u-progress" :class="[`u-progress--${variant}`, `u-progress--${size}`]">
    <div v-if="showLabel" class="u-progress-header">
      <span class="u-progress-label">{{ label }}</span>
      <span class="u-progress-value">{{ percentage }}%</span>
    </div>
    <div class="u-progress-track">
      <div
        class="u-progress-fill"
        :class="{ 'u-progress-fill--animated': animated }"
        :style="{ width: `${percentage}%` }"
      ></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  percentage: { type: Number, default: 0 },
  variant: { type: String, default: 'default' },  // default / success / warning / danger
  size: { type: String, default: 'md' },           // sm / md / lg
  label: { type: String, default: '' },
  showLabel: { type: Boolean, default: false },
  animated: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-progress {
  width: 100%;
}

.u-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-1;
}

.u-progress-label {
  font-size: $text-xs;
  color: $text-secondary;
  font-weight: 500;
}

.u-progress-value {
  font-family: $font-data;
  font-size: $text-xs;
  color: $text-muted;
  font-weight: 600;
}

.u-progress-track {
  height: 6px;
  background: rgba($bg-elevated, 0.6);
  border-radius: 3px;
  overflow: hidden;

  .u-progress--sm & { height: 4px; }
  .u-progress--lg & { height: 8px; }
}

.u-progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
  background: linear-gradient(90deg, $accent-indigo, $accent-cyan);

  &--animated {
    background-size: 200% 100%;
    animation: progress-shimmer 1.5s ease-in-out infinite;
  }

  .u-progress--success & { background: $color-success; }
  .u-progress--warning & { background: $color-warning; }
  .u-progress--danger & { background: $color-danger; }
}

@keyframes progress-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
