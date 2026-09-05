<template>
  <div class="u-card" :class="[variant, { 'u-card--hover': hover }]">
    <div v-if="$slots.header" class="u-card-header">
      <slot name="header" />
    </div>
    <div class="u-card-body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="u-card-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'glass' },   // glass / solid / outline
  hover: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-card {
  border-radius: $radius-lg;
  transition: all $transition-normal;
  overflow: hidden;

  &--glass {
    background: rgba($bg-surface, 0.65);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba($border-default, 0.5);
    box-shadow: $shadow-sm;
  }

  &--solid {
    background: $bg-surface;
    border: 1px solid $border-subtle;
    box-shadow: $shadow-sm;
  }

  &--outline {
    background: transparent;
    border: 1px solid $border-default;
  }

  &--hover:hover {
    border-color: rgba($accent-indigo, 0.25);
    box-shadow: $shadow-md, 0 0 20px rgba($accent-indigo, 0.06);
  }
}

.u-card-header {
  padding: $space-4 $space-5;
  border-bottom: 1px solid $border-subtle;
}

.u-card-body {
  padding: $space-5;
}

.u-card-footer {
  padding: $space-3 $space-5;
  border-top: 1px solid $border-subtle;
}
</style>
