<template>
  <div class="app-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <!-- 动态背景 -->
    <div class="bg-dynamic">
      <!-- 粒子背景 -->
      <div class="bg-particles">
        <div class="particle" v-for="i in 20" :key="i" :style="particleStyle(i)"></div>
      </div>
      <!-- 极光背景 -->
      <div class="aurora-orb orb-1"></div>
      <div class="aurora-orb orb-2"></div>
      <div class="aurora-orb orb-3"></div>
      <div class="grid-overlay"></div>
    </div>

    <!-- 侧边栏 -->
    <Sidebar
      :collapsed="sidebarCollapsed"
      :class="{ open: sidebarOpen }"
      @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
    />

    <!-- 移动端遮罩 -->
    <div
      class="sidebar-overlay"
      :class="{ visible: sidebarOpen }"
      @click="sidebarOpen = false"
    ></div>

    <!-- 主内容区域 -->
    <div class="main-area">
      <TopBar
        @toggle-sidebar="sidebarOpen = !sidebarOpen"
      />

      <main class="page-content">
        <slot />
      </main>
    </div>

    <!-- 全局聊天栏 -->
    <GlobalChatBar :sidebar-collapsed="sidebarCollapsed" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import Sidebar from '@/components/layout/Sidebar.vue'
import TopBar from '@/components/layout/TopBar.vue'
import GlobalChatBar from '@/components/chat/GlobalChatBar.vue'

const props = defineProps({})

const sidebarOpen = ref(false)
const sidebarCollapsed = ref(true)

const handleResize = () => {
  if (window.innerWidth > 768) sidebarOpen.value = false
}

// 粒子样式生成
const particleStyle = (i) => ({
  left: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 20}s`,
  animationDuration: `${15 + Math.random() * 25}s`,
  opacity: 0.2 + Math.random() * 0.4
})

onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => window.removeEventListener('resize', handleResize))
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.app-layout {
  display: flex;
  align-items: stretch;
  min-height: 100vh;
  background: $bg-base;
  position: relative;
  overflow: hidden;
}

/* ===== 动态背景 ===== */
.bg-dynamic {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

/* ===== 粒子背景 ===== */
.bg-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  width: 3px;
  height: 3px;
  background: $accent-cyan;
  border-radius: 50%;
  box-shadow: 0 0 10px rgba($accent-cyan, 0.5);
  animation: particle-float 20s linear infinite;

  &:nth-child(odd) {
    background: $accent-indigo;
    box-shadow: 0 0 10px rgba($accent-indigo, 0.5);
  }
}

@keyframes particle-float {
  0% { transform: translateY(100vh) rotate(0deg); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(-100px) rotate(720deg); opacity: 0; }
}

.aurora-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  animation: aurora-float 25s ease-in-out infinite;
}

.orb-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -150px;
  background: radial-gradient(circle, rgba($accent-indigo, 0.1) 0%, transparent 70%);
}

.orb-2 {
  width: 500px;
  height: 500px;
  bottom: -200px;
  left: -150px;
  background: radial-gradient(circle, rgba($accent-cyan, 0.08) 0%, transparent 70%);
  animation-delay: -8s;
}

.orb-3 {
  width: 400px;
  height: 400px;
  top: 40%;
  left: 50%;
  background: radial-gradient(circle, rgba($accent-violet, 0.06) 0%, transparent 70%);
  animation-delay: -16s;
}

@keyframes aurora-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.1); }
  50% { transform: translate(-20px, 20px) scale(0.9); }
  75% { transform: translate(20px, 10px) scale(1.05); }
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba($accent-indigo, 0.015) 1px, transparent 1px),
    linear-gradient(90deg, rgba($accent-indigo, 0.015) 1px, transparent 1px);
  background-size: 60px 60px;
}

.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: $bg-overlay;
  z-index: 35;
  opacity: 0;
  transition: opacity $transition-normal;

  &.visible { opacity: 1; }

  @media (max-width: $breakpoint-md) {
    display: block;
  }
}

.main-area {
  flex: 1;
  margin-left: $sidebar-width;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left $transition-normal;
  position: relative;
  z-index: 1;
}

.sidebar-collapsed .main-area {
  margin-left: $sidebar-collapsed;
}

.page-content {
  flex: 1;
  padding: $space-6;
  overflow-y: auto;
}

@media (max-width: $breakpoint-md) {
  .main-area {
    margin-left: 0;
    width: 100%;
  }
  .page-content { padding: $space-4; }
}
</style>