<template>
  <div class="u-tabs">
    <div class="u-tabs-list">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="u-tab"
        :class="{ 'u-tab--active': modelValue === tab.id }"
        @click="$emit('update:modelValue', tab.id)"
      >
        <span v-if="tab.icon" class="u-tab-icon">{{ tab.icon }}</span>
        <span class="u-tab-label">{{ tab.label }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  tabs: { type: Array, required: true },   // [{ id, label, icon? }]
  modelValue: { type: String, default: '' }
})

defineEmits(['update:modelValue'])
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-tabs {
  width: 100%;
}

.u-tabs-list {
  display: flex;
  gap: $space-1;
  padding: 3px;
  background: rgba($bg-elevated, 0.5);
  border-radius: $radius-lg;
  border: 1px solid $border-subtle;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.u-tab {
  display: flex;
  align-items: center;
  gap: $space-1;
  padding: 8px 14px;
  border: none;
  background: transparent;
  color: $text-muted;
  font-size: $text-sm;
  font-weight: 500;
  font-family: $font-sans;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    color: $text-secondary;
    background: rgba($accent-indigo, 0.05);
  }

  &--active {
    background: rgba($accent-indigo, 0.12);
    color: $accent-indigo;
    font-weight: 600;
    box-shadow: 0 1px 3px rgba($accent-indigo, 0.15);
  }
}

.u-tab-icon {
  font-size: 14px;
  line-height: 1;
}
</style>
