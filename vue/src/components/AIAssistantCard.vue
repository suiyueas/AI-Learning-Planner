<template>
  <div class="ai-assistant-card glass-panel" :class="{ loading: isLoading }">
    <!-- 扫描线效果 -->
    <div class="scanline-effect"></div>
    
    <div class="card-header">
      <div class="header-left">
        <!-- AI 呼吸光球 -->
        <div class="ai-orb">
          <div class="orb-core">
            <Sparkles :size="16" />
          </div>
          <div class="orb-ring ring-1"></div>
          <div class="orb-ring ring-2"></div>
        </div>
        <span class="title">AI 学习洞察</span>
      </div>
      <span class="badge" v-if="!isLoading && suggestion">
        <span class="badge-dot"></span>
        实时
      </span>
      <span class="badge loading-badge" v-else-if="isLoading">
        <span class="loading-spinner"></span>
        分析中
      </span>
    </div>

    <div class="card-body">
      <div v-if="isLoading" class="loading-state">
        <div class="loading-wave">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
        </div>
        <span class="loading-text">正在分析你的学习数据...</span>
      </div>

      <div v-else-if="suggestion" class="content">
        <!-- 聊天气泡样式 -->
        <div class="insight-bubble">
          <div class="bubble-arrow"></div>
          <p class="insight">{{ suggestion.message }}</p>
        </div>
        <div class="actions" v-if="suggestion.actions && suggestion.actions.length > 0">
          <button
            v-for="action in suggestion.actions"
            :key="action.label"
            class="action-btn neon-btn"
            @click="handleAction(action)"
          >
            <span>{{ action.label }}</span>
            <ArrowRight :size="14" class="action-arrow" />
          </button>
        </div>
      </div>

      <div v-else class="empty-state">
        <div class="empty-orb">
          <Bot :size="24" />
        </div>
        <span>开始学习吧！AI 将为你提供智能建议</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles, Bot, ArrowRight } from 'lucide-vue-next'
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
  padding: 22px 26px;
  border-radius: $radius-lg;
  transition: all $transition-normal;
  position: relative;
  overflow: hidden;
  margin-top: 24px;

  // 扫描线效果
  .scanline-effect {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 100%;
    background: linear-gradient(180deg, 
      transparent 0%, 
      rgba($accent-cyan, 0.02) 50%, 
      transparent 100%);
    animation: scanline-move 6s linear infinite;
    pointer-events: none;
  }

  @keyframes scanline-move {
    0% { transform: translateY(-100%); }
    100% { transform: translateY(100%); }
  }

  &:hover {
    border-color: rgba($accent-cyan, 0.3);
    box-shadow: $glow-cyan;
  }

  &.loading {
    opacity: 0.9;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

// AI 呼吸光球
.ai-orb {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.orb-core {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, $accent-primary, $accent-cyan);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  z-index: 2;
  animation: orb-breathe 3s ease-in-out infinite;
}

@keyframes orb-breathe {
  0%, 100% {
    box-shadow: 
      0 0 15px rgba($accent-cyan, 0.4),
      0 0 30px rgba($accent-cyan, 0.2);
    transform: scale(1);
  }
  50% {
    box-shadow: 
      0 0 25px rgba($accent-cyan, 0.6),
      0 0 50px rgba($accent-cyan, 0.3);
    transform: scale(1.05);
  }
}

.orb-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba($accent-cyan, 0.2);
  animation: ring-expand 3s ease-in-out infinite;

  &.ring-1 {
    inset: -4px;
    animation-delay: 0s;
  }

  &.ring-2 {
    inset: -8px;
    animation-delay: 0.5s;
  }
}

@keyframes ring-expand {
  0%, 100% { 
    transform: scale(1); 
    opacity: 0.5; 
  }
  50% { 
    transform: scale(1.1); 
    opacity: 0.2; 
  }
}

.title {
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: 0.02em;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.72rem;
  padding: 4px 12px;
  border-radius: $radius-full;
  background: rgba($accent-cyan, 0.1);
  border: 1px solid rgba($accent-cyan, 0.2);
  color: $accent-cyan;
  font-weight: 500;
  position: relative;
  z-index: 1;

  .badge-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: $accent-cyan;
    animation: dot-pulse 1.5s ease-in-out infinite;
  }

  @keyframes dot-pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(0.8); }
  }

  &.loading-badge {
    background: rgba($accent-primary, 0.1);
    border-color: rgba($accent-primary, 0.2);
    color: $accent-primary;

    .badge-dot {
      display: none;
    }
  }
}

.loading-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba($accent-primary, 0.2);
  border-top-color: $accent-primary;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.card-body {
  min-height: 60px;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.loading-wave {
  display: flex;
  gap: 4px;
  align-items: center;
  height: 24px;

  span {
    width: 3px;
    height: 100%;
    background: linear-gradient(180deg, $accent-cyan, $accent-primary);
    border-radius: 2px;
    animation: wave 1.2s ease-in-out infinite;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.1s; }
    &:nth-child(3) { animation-delay: 0.2s; }
    &:nth-child(4) { animation-delay: 0.3s; }
    &:nth-child(5) { animation-delay: 0.4s; }
  }
}

@keyframes wave {
  0%, 100% { transform: scaleY(0.4); }
  50% { transform: scaleY(1); }
}

.loading-text {
  color: $text-secondary;
  font-size: 0.9rem;
}

.content {
  width: 100%;
}

// 聊天气泡样式
.insight-bubble {
  position: relative;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba($accent-cyan, 0.1);
  border-radius: 16px 16px 16px 4px;
  padding: 16px 20px;
  margin-bottom: 16px;

  .bubble-arrow {
    position: absolute;
    left: -8px;
    top: 16px;
    width: 0;
    height: 0;
    border: 6px solid transparent;
    border-right-color: rgba($accent-cyan, 0.1);
  }
}

.insight {
  font-size: 0.95rem;
  color: $text-primary;
  line-height: 1.65;
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-btn.neon-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, rgba($accent-cyan, 0.15), rgba($accent-primary, 0.1));
  color: $accent-cyan;
  font-size: 0.85rem;
  font-weight: 500;
  border: 1px solid rgba($accent-cyan, 0.3);
  cursor: pointer;
  transition: all $transition-normal;

  .action-arrow {
    transition: transform $transition-normal;
  }

  &:hover {
    background: linear-gradient(135deg, rgba($accent-cyan, 0.25), rgba($accent-primary, 0.2));
    border-color: rgba($accent-cyan, 0.5);
    box-shadow: $glow-cyan;
    transform: translateY(-2px);

    .action-arrow {
      transform: translateX(4px);
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px;
  width: 100%;
}

.empty-orb {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba($accent-cyan, 0.1);
  border: 1px solid rgba($accent-cyan, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $accent-cyan;
  animation: empty-pulse 2s ease-in-out infinite;
}

@keyframes empty-pulse {
  0%, 100% { box-shadow: 0 0 10px rgba($accent-cyan, 0.2); }
  50% { box-shadow: 0 0 25px rgba($accent-cyan, 0.4); }
}

.empty-state span {
  color: $text-secondary;
  font-size: 0.9rem;
  text-align: center;
}
</style>
