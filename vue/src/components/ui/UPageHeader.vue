<template>
  <header class="u-page-header">
    <div class="u-ph-row">
      <div class="u-ph-left">
        <h1 class="u-ph-title">
          <span v-if="icon" class="u-ph-glyph">{{ icon }}</span>
          <span>{{ title }}</span>
          <span v-if="subtitle" class="u-ph-subtitle">{{ subtitle }}</span>
        </h1>
      </div>
      <div v-if="$slots.actions" class="u-ph-right">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-page-header {
  position: relative;
  z-index: 1;
  margin-bottom: $space-6;
  animation: slideUp 0.5s ease both;
}

.u-ph-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  gap: $space-4;
}

.u-ph-left {
  flex: 1;
  min-width: 0;
}

.u-ph-title {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
  color: $text-primary;
}

.u-ph-glyph {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.u-ph-subtitle {
  font-size: $text-sm;
  font-weight: 400;
  color: $text-muted;
  margin-left: $space-2;
}

.u-ph-right {
  display: flex;
  align-items: center;
  gap: $space-2;
  flex-shrink: 0;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
