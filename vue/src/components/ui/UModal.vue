<template>
  <Teleport to="body">
    <Transition name="u-modal">
      <div v-if="visible" class="u-modal-mask" @click.self="handleMaskClick">
        <div class="u-modal" :class="[`u-modal--${size}`]" @click.stop>
          <div v-if="title || $slots.header" class="u-modal-header">
            <slot name="header">
              <h3 class="u-modal-title">{{ title }}</h3>
            </slot>
            <button v-if="closable" class="u-modal-close" @click="handleClose">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="u-modal-body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="u-modal-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  size: { type: String, default: 'md' },        // sm / md / lg
  closable: { type: Boolean, default: true },
  maskClosable: { type: Boolean, default: true }
})

const emit = defineEmits(['update:visible', 'close'])

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

const handleMaskClick = () => {
  if (props.maskClosable) handleClose()
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.u-modal-mask {
  position: fixed;
  inset: 0;
  background: $bg-overlay;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-6;
}

.u-modal {
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: $radius-xl;
  box-shadow: $shadow-xl;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;

  &--sm { max-width: 400px; }
  &--md { max-width: 560px; }
  &--lg { max-width: 720px; }
}

.u-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-4 $space-5;
  border-bottom: 1px solid $border-subtle;
}

.u-modal-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.u-modal-close {
  background: none;
  border: none;
  color: $text-muted;
  cursor: pointer;
  padding: 4px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-fast;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }
}

.u-modal-body {
  padding: $space-5;
}

.u-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: $space-2;
  padding: $space-3 $space-5;
  border-top: 1px solid $border-subtle;
}

// transition
.u-modal-enter-active,
.u-modal-leave-active {
  transition: opacity 0.2s ease;

  .u-modal {
    transition: transform 0.2s ease;
  }
}

.u-modal-enter-from,
.u-modal-leave-to {
  opacity: 0;

  .u-modal {
    transform: scale(0.95);
  }
}
</style>
