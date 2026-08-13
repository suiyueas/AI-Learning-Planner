<template>
  <div ref="switcherRef" class="model-switcher">
    <div class="current-model" @click="toggleDropdown">
      <span class="status-dot"></span>
      <span class="model-name">{{ currentDisplayName }}</span>
      <span class="dropdown-arrow" :class="{ open: showDropdown }">▼</span>
    </div>

    <transition name="dropdown-pop">
      <div v-if="showDropdown" class="model-dropdown">
        <div class="dropdown-header">选择模型</div>
        <div class="model-list">
          <div
            v-for="(model, key) in models"
            :key="key"
            class="model-option"
            :class="{ active: currentProvider === key }"
            @click="handleSwitch(key)"
          >
            <span class="option-icon">{{ getModelIcon(key) }}</span>
            <div class="option-info">
              <span class="option-name">{{ getModelDisplayName(key) }}</span>
              <span class="option-desc">{{ model.modelName }}</span>
            </div>
            <span v-if="currentProvider === key" class="check-icon">✅</span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useModelStore } from '@/stores/modelStore'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const modelStore = useModelStore()
const showDropdown = ref(false)
const switcherRef = ref(null)

const currentProvider = computed(() => modelStore.currentProvider)
const currentDisplayName = computed(() => modelStore.currentDisplayName)
const models = computed(() => modelStore.availableModels)

const getModelIcon = (provider) => {
  const icons = { qwen: '🔮', deepseek: '🧠', xiaomi: '📱' }
  return icons[provider] || '🤖'
}

const getModelDisplayName = (provider) => {
  return modelStore.getModelDisplayName(provider)
}

const toggleDropdown = () => {
  // 全局模型切换为管理操作，普通用户仅可查看当前模型
  if (!authStore.isAdmin) {
    ElMessage.warning('仅管理员可切换全局模型')
    return
  }
  showDropdown.value = !showDropdown.value
}

const handleSwitch = async (provider) => {
  if (provider === currentProvider.value) {
    showDropdown.value = false
    return
  }
  const result = await modelStore.switchToModel(provider)
  if (result.success) {
    ElMessage.success(result.message)
  } else {
    ElMessage.error(result.message)
  }
  showDropdown.value = false
}

const handleClickOutside = (event) => {
  if (switcherRef.value && !switcherRef.value.contains(event.target)) {
    showDropdown.value = false
  }
}

onMounted(() => { document.addEventListener('click', handleClickOutside); modelStore.loadCurrentModel() })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside) })
</script>

<style lang="scss" scoped>
.model-switcher {
  position: relative;
}

.current-model {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px 4px 10px;
  background: rgba(100,100,180,0.06);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(100,100,180,0.1);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(0, 245, 212, 0.3);
    background: rgba(100,100,180,0.1);
    color: #00f5d4;

    .model-name { color: #00f5d4; }
    .status-dot { background: #10b981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.5); }
  }
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
  flex-shrink: 0;
  animation: dot-pulse 2s ease-in-out infinite;
}

.model-name {
  font-size: 12px;
  font-weight: 500;
  color: #a0a0c8;
  white-space: nowrap;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.3s;
}

.dropdown-arrow {
  font-size: 8px;
  color: #606090;
  transition: transform 0.25s ease;
  &.open { transform: rotate(180deg); }
}

/* 下拉菜单 */
.model-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 240px;
  background: rgba(10, 14, 26, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(100,100,180,0.1);
  border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5), 0 0 30px rgba(0, 245, 212, 0.03);
  z-index: 1001;
  overflow: hidden;
}

.dropdown-pop-enter-active { animation: popIn 0.2s ease; }
.dropdown-pop-leave-active { animation: popIn 0.15s ease reverse; }
@keyframes popIn {
  from { opacity: 0; transform: translateY(-6px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.dropdown-header {
  padding: 10px 14px;
  border-bottom: 1px solid rgba(100,100,180,0.07);
  font-size: 11px;
  font-weight: 600;
  color: #8080a8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.model-list {
  padding: 6px;
}

.model-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    background: rgba(100,100,180,0.06);
  }

  &.active {
    background: rgba(0, 245, 212, 0.06);
    border: 1px solid rgba(0, 245, 212, 0.1);

    .option-name { color: #00f5d4; }
  }
}

.option-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.option-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.option-name {
  font-size: 12px;
  font-weight: 500;
  color: #e8e8ff;
  transition: color 0.15s;
}

.option-desc {
  font-size: 10px;
  color: #606090;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.check-icon {
  font-size: 12px;
  flex-shrink: 0;
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
