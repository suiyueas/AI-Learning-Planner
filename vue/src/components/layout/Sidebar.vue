<template>
  <aside class="sidebar" :class="{ collapsed: collapsed }">
    <!-- Logo 区域 -->
    <div class="sidebar-logo">
      <div class="logo-icon">
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <template v-if="!collapsed">
        <span class="logo-text">知途</span>
        <span class="logo-subtitle">Zhitu</span>
      </template>
    </div>

    <!-- 导航菜单 -->
    <nav class="sidebar-nav">
      <!-- 核心组 -->
      <div class="nav-group">
        <div v-if="!collapsed" class="nav-group-title">核心</div>
        <router-link
          v-for="item in coreItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :title="collapsed ? item.label : ''"
        >
          <component :is="item.icon" :size="18" />
          <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
        </router-link>
      </div>

      <!-- 个人组 -->
      <div class="nav-group">
        <div v-if="!collapsed" class="nav-group-title">个人</div>
        <router-link
          v-for="item in personalItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :title="collapsed ? item.label : ''"
        >
          <component :is="item.icon" :size="18" />
          <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
        </router-link>
      </div>
    </nav>

    <!-- 伸缩按钮 -->
    <button class="collapse-toggle" @click="$emit('toggle-collapse')">
      <ChevronLeft v-if="!collapsed" :size="16" />
      <ChevronRight v-else :size="16" />
    </button>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import {
  LayoutDashboard,
  Brain,
  Calendar,
  Trophy,
  Settings,
  Bot,
  BookOpen,
  Code2,
  ChevronLeft,
  ChevronRight
} from 'lucide-vue-next'

defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

defineEmits(['toggle-collapse'])

const route = useRoute()

const coreItems = [
  { path: '/workbench', label: '学习工作台', icon: LayoutDashboard },
  { path: '/knowledge', label: '知识库', icon: Brain },
  { path: '/calendar', label: '学习日历', icon: Calendar },
]

const personalItems = [
  { path: '/achievements', label: '成就徽章', icon: Trophy },
  { path: '/study-notes', label: '学习笔记', icon: BookOpen },
  { path: '/code-analyze', label: '代码分析', icon: Code2 },
  { path: '/agents', label: '智能体中心', icon: Bot },
  { path: '/profile', label: '个人中心', icon: Settings },
]

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.sidebar {
  width: $sidebar-width;
  height: 100vh;
  background: rgba($bg-surface, 0.85);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid rgba($border-default, 0.4);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 40;
  transition: width $transition-normal;
  overflow: hidden;

  &.collapsed {
    width: $sidebar-collapsed;
  }
}

.sidebar-logo {
  height: $topbar-height;
  padding: $space-4;
  display: flex;
  align-items: center;
  gap: $space-3;
  border-bottom: 1px solid rgba($border-subtle, 0.5);
  flex-shrink: 0;
}

.logo-icon {
  width: 28px;
  height: 28px;
  color: $accent-indigo;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  svg { width: 100%; height: 100%; }
}

.logo-text {
  font-size: $text-lg;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.logo-subtitle {
  font-size: $text-xs;
  color: $text-muted;
  font-weight: 500;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: $space-3 0;
}

.nav-group {
  margin-bottom: $space-2;
}

.nav-group-title {
  padding: $space-2 $space-4;
  font-size: 11px;
  font-weight: 600;
  color: $text-muted;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: 10px $space-4;
  margin: 2px $space-2;
  color: $text-secondary;
  text-decoration: none;
  border-radius: $radius-md;
  transition: all $transition-fast;
  border-left: 2px solid transparent;
  white-space: nowrap;

  &:hover {
    background: rgba($accent-indigo, 0.06);
    color: $text-primary;
  }

  &.active {
    background: rgba($accent-indigo, 0.1);
    color: $accent-indigo-light;
    border-left-color: $accent-indigo;
  }
}

.nav-label {
  font-size: $text-sm;
  font-weight: 500;
}

/* 伸缩按钮 */
.collapse-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  margin: $space-2;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.2);
  border-radius: $radius-md;
  color: $accent-indigo;
  cursor: pointer;
  transition: all $transition-fast;
  flex-shrink: 0;

  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.35);
    transform: scale(1.02);
  }
}

/* 移动端适配 */
@media (max-width: $breakpoint-md) {
  .sidebar {
    transform: translateX(-100%);
    z-index: 100;
    width: $sidebar-width;

    &.open {
      transform: translateX(0);
    }
  }
}
</style>