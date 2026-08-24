<template>
  <div class="ai-assistant-card glass-panel" :class="{ loading: isLoading }">
    <div class="card-header">
      <div class="header-left">
        <span class="icon">💡</span>
        <span class="title">AI 学习洞察</span>
      </div>
      <span class="badge" v-if="!isLoading && suggestion">实时</span>
      <span class="badge loading-badge" v-else-if="isLoading">分析中...</span>
    </div>

    <div class="card-body">
      <div v-if="isLoading" class="loading-state">
        <span class="pulse-dot"></span>
        <span>正在分析你的学习数据...</span>
      </div>

      <div v-else-if="suggestion" class="content">
        <p class="insight">{{ suggestion.message }}</p>
        <div class="actions" v-if="suggestion.actions && suggestion.actions.length > 0">
          <button
            v-for="action in suggestion.actions"
            :key="action.label"
            class="action-btn"
            @click="handleAction(action)"
          >
            {{ action.label }} →
          </button>
        </div>
      </div>

      <div v-else class="empty-state">
        <span>开始学习吧！AI 将为你提供智能建议 🚀</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAISuggestion } from '@/api/statsApi'

const router = useRouter()
const isLoading = ref(true)
const suggestion = ref(null)

const fetchSuggestion = async () => {
  isLoading.value = true
  try {
    const res = await getAISuggestion()
    const data = res?.data ?? res
    if (data && data.message) {
      suggestion.value = data
    } else {
      suggestion.value = getFallbackSuggestion()
    }
  } catch (error) {
    console.warn('AI 推荐加载失败，使用降级方案:', error.message)
    suggestion.value = getFallbackSuggestion()
  } finally {
    isLoading.value = false
  }
}

const getFallbackSuggestion = () => ({
  message: '检测到你还未开始学习，建议从「Python 基础」路径开始。',
  actions: [
    { label: '查看学习路径', target: '/learning-path' }
  ]
})

const handleAction = (action) => {
  if (action.target) {
    router.push(action.target)
  }
}

onMounted(fetchSuggestion)
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.ai-assistant-card {
  padding: 18px 22px;
  border-radius: $radius-lg;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-primary, 0.3);
    box-shadow: 0 4px 20px rgba($accent-primary, 0.1);
  }

  &.loading {
    opacity: 0.85;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon {
  font-size: 1.1rem;
}

.title {
  font-size: 0.95rem;
  font-weight: 600;
  color: $text-primary;
}

.badge {
  font-size: 0.72rem;
  padding: 3px 10px;
  border-radius: $radius-full;
  background: rgba($accent-emerald, 0.12);
  color: $accent-emerald;
  font-weight: 500;

  &.loading-badge {
    background: rgba($accent-primary, 0.1);
    color: $accent-primary;
  }
}

.card-body {
  min-height: 60px;
  display: flex;
  align-items: center;
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 10px;
  color: $text-secondary;
  font-size: 0.88rem;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $accent-primary;
  animation: pulse-animation 1.5s ease-in-out infinite;
}

@keyframes pulse-animation {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

.content {
  width: 100%;
}

.insight {
  font-size: 0.92rem;
  color: $text-primary;
  line-height: 1.6;
  margin: 0 0 14px 0;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 8px 16px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, $accent-primary, darken($accent-primary, 10%));
  color: #fff;
  font-size: 0.82rem;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-primary, 0.3);
  }

  &:active {
    transform: translateY(0);
  }
}

.empty-state {
  color: $text-secondary;
  font-size: 0.88rem;
}
</style>