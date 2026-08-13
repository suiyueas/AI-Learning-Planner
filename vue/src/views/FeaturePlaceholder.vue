<template>
  <div class="placeholder-page">
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
    </div>

    <div class="placeholder-content">
      <div class="placeholder-icon-wrap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" width="64" height="64">
          <rect x="3" y="3" width="18" height="18" rx="3" stroke-dasharray="4 3" />
          <line x1="12" y1="8" x2="12" y2="16" />
          <line x1="8" y1="12" x2="16" y2="12" />
        </svg>
      </div>
      <h1 class="placeholder-title">{{ featureName }}</h1>
      <div class="placeholder-badge">开发中</div>
      <p class="placeholder-desc">该功能页面正在紧锣密鼓地开发中，敬请期待！</p>
      <div class="placeholder-progress">
        <div class="progress-track">
          <div class="progress-fill"></div>
        </div>
        <span class="progress-text">开发进度 75%</span>
      </div>
      <div class="placeholder-actions">
        <button class="btn-back" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const featureName = computed(() => route.meta.featureName || '未知功能')

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/modules')
  }
}
</script>

<style lang="scss" scoped>
.placeholder-page {
  min-height: 100vh;
  background: #0a0a1a;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora {
  position: absolute; inset: 0;
  background:
    radial-gradient(ellipse at 70% 20%, rgba(0,245,212,0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift {
  0%,100% { transform: scale(1) rotate(0deg); }
  33% { transform: scale(1.1) rotate(1deg); }
  66% { transform: scale(0.95) rotate(-1deg); }
}
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(0,245,212,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  animation: gridPulse 8s ease-in-out infinite alternate;
  transform-origin: center center;
}
@keyframes gridPulse {
  0% { opacity: 0.3; transform: scale(1); }
  100% { opacity: 0.6; transform: scale(1.02); }
}

.placeholder-content {
  position: relative; z-index: 1;
  text-align: center;
  padding: 40px;
  max-width: 480px;
  animation: fadeSlideUp 0.6s ease;
}

.placeholder-icon-wrap {
  width: 100px; height: 100px;
  margin: 0 auto 24px;
  border-radius: 24px;
  background: rgba(123,97,255,0.08);
  border: 1px solid rgba(123,97,255,0.15);
  display: flex; align-items: center; justify-content: center;
  color: #7b61ff;
  animation: iconPulse 3s ease-in-out infinite;
}
@keyframes iconPulse {
  0%,100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.05); opacity: 1; }
}

.placeholder-title {
  font-size: 2rem; font-weight: 800;
  background: linear-gradient(135deg, #00f5d4 0%, #3a86ff 50%, #7b61ff 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  margin-bottom: 12px;
}

.placeholder-badge {
  display: inline-block;
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 13px; font-weight: 600;
  background: rgba(245,158,11,0.15);
  color: #f59e0b;
  border: 1px solid rgba(245,158,11,0.2);
  margin-bottom: 16px;
}

.placeholder-desc {
  font-size: 15px;
  color: #c0c0e0;
  line-height: 1.6;
  margin-bottom: 28px;
}

.placeholder-progress {
  display: flex; align-items: center; gap: 12px;
  justify-content: center;
  margin-bottom: 36px;
}

.progress-track {
  width: 200px; height: 6px;
  background: rgba(100,100,180,0.1);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  width: 75%; height: 100%;
  background: linear-gradient(90deg, #00f5d4, #7b61ff);
  border-radius: 3px;
  animation: progressShimmer 2s ease-in-out infinite;
  background-size: 200% 100%;
}
@keyframes progressShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.progress-text {
  font-size: 12px; color: #9090b8; font-family: 'JetBrains Mono', monospace;
}

.placeholder-actions {
  display: flex; justify-content: center;
}

.btn-back {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 28px;
  border-radius: 10px;
  font-size: 14px; font-weight: 600;
  border: 1px solid rgba(100,100,180,0.15);
  background: rgba(17,17,39,0.6);
  color: #c0c0e0;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(8px);
  &:hover {
    border-color: rgba(0,245,212,0.25);
    color: #e8e8ff;
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(0,245,212,0.08);
  }
}

@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
