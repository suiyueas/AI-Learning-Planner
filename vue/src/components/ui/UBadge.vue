<template>
  <span class="u-badge" :class="[`u-badge--${variant}`, `u-badge--${size}`]">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'default' },  // default / success / warning / danger / info
  size: { type: String, default: 'md' }            // sm / md
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: $radius-full;
  font-size: $text-xs;
  font-weight: 600;
  white-space: nowrap;
  border: 1px solid transparent;

  &--sm { padding: 2px 8px; font-size: 11px; }

  &--default {
    background: rgba($text-muted, 0.1);
    color: $text-secondary;
    border-color: rgba($text-muted, 0.15);
  }

  &--success {
    background: rgba($color-success, 0.1);
    color: $color-success;
    border-color: rgba($color-success, 0.2);
  }

  &--warning {
    background: rgba($color-warning, 0.1);
    color: $color-warning;
    border-color: rgba($color-warning, 0.2);
  }

  &--danger {
    background: rgba($color-danger, 0.1);
    color: $color-danger;
    border-color: rgba($color-danger, 0.2);
  }

  &--info {
    background: rgba($color-info, 0.1);
    color: $color-info;
    border-color: rgba($color-info, 0.2);
  }
}
</style>
