<template>
  <button
    class="u-btn"
    :class="[`u-btn--${variant}`, `u-btn--${size}`, { 'u-btn--loading': loading, 'u-btn--block': block }]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="u-btn-spinner"></span>
    <span v-if="icon && !loading" class="u-btn-icon">
      <component :is="icon" :size="iconSize" />
    </span>
    <span v-if="$slots.default" class="u-btn-text">
      <slot />
    </span>
  </button>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'primary' },    // primary / secondary / ghost / danger
  size: { type: String, default: 'md' },             // sm / md / lg
  icon: { type: [Object, Function], default: null },
  iconSize: { type: Number, default: 16 },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false }
})

defineEmits(['click'])
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  border: 1px solid transparent;
  border-radius: $radius-md;
  font-family: $font-sans;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;
  user-select: none;
  position: relative;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &--sm { padding: 6px 12px; font-size: $text-xs; }
  &--md { padding: 8px 16px; font-size: $text-sm; }
  &--lg { padding: 10px 24px; font-size: $text-base; }

  &--block { width: 100%; }

  // primary
  &--primary {
    background: rgba($accent-indigo, 0.15);
    color: $accent-indigo;
    border-color: rgba($accent-indigo, 0.3);

    &:hover:not(:disabled) {
      background: rgba($accent-indigo, 0.25);
      border-color: rgba($accent-indigo, 0.5);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba($accent-indigo, 0.2);
    }
  }

  // secondary
  &--secondary {
    background: $bg-elevated;
    color: $text-secondary;
    border-color: $border-default;

    &:hover:not(:disabled) {
      background: $bg-muted;
      color: $text-primary;
      border-color: $border-medium;
    }
  }

  // ghost
  &--ghost {
    background: transparent;
    color: $text-secondary;
    border-color: transparent;

    &:hover:not(:disabled) {
      background: rgba($accent-indigo, 0.08);
      color: $accent-indigo;
    }
  }

  // danger
  &--danger {
    background: rgba($color-danger, 0.1);
    color: $color-danger;
    border-color: rgba($color-danger, 0.3);

    &:hover:not(:disabled) {
      background: rgba($color-danger, 0.2);
      border-color: rgba($color-danger, 0.5);
    }
  }
}

.u-btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: u-btn-spin 0.6s linear infinite;
}

@keyframes u-btn-spin {
  to { transform: rotate(360deg); }
}

.u-btn-icon {
  display: flex;
  align-items: center;
}
</style>
