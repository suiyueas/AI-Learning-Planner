<template>
  <div class="u-stat-card" :class="[`u-stat-card--${variant}`, `u-stat-card--${size}`]">
    <span v-if="icon" class="u-stat-icon">{{ icon }}</span>
    <div class="u-stat-body">
      <span class="u-stat-value">{{ value }}</span>
      <span class="u-stat-label">{{ label }}</span>
    </div>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '' },
  value: { type: [String, Number], default: 0 },
  label: { type: String, default: '' },
  size: { type: String, default: 'md' },        // sm / md / lg
  variant: { type: String, default: 'default' }  // default / success / warning / danger
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-stat-card {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-4 $space-5;
  background: rgba($bg-surface, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-lg;
  transition: all $transition-normal;

  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba($accent-indigo, 0.1);
  }

  &--sm { padding: $space-3 $space-4; }
  &--lg { padding: $space-5 $space-6; }
}

.u-stat-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.u-stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.u-stat-value {
  font-family: $font-data;
  font-size: $text-xl;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.2;

  .u-stat-card--success & { color: $color-success; }
  .u-stat-card--warning & { color: $color-warning; }
  .u-stat-card--danger & { color: $color-danger; }
}

.u-stat-label {
  font-size: $text-xs;
  color: $text-muted;
  font-weight: 500;
}
</style>
