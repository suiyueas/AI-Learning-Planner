<template>
  <div v-if="isLoading" class="global-loading">
    <div class="loading-overlay"></div>
    <div class="loading-content">
      <div class="loading-spinner">
        <div class="spinner"></div>
      </div>
      <div class="loading-text">{{ loadingText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAppStore } from '@/stores/appStore'

const appStore = useAppStore()

const isLoading = computed(() => appStore.isLoading)
const loadingText = computed(() => appStore.loadingText)
</script>

<style lang="scss" scoped>
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
}

.loading-content {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px;
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
}

.loading-spinner {
  width: 48px;
  height: 48px;
}

.spinner {
  width: 100%;
  height: 100%;
  border: 3px solid var(--border-subtle);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-primary);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>