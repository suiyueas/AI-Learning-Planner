<template>
  <aside class="sidebar">
    <!-- Logo 区域 -->
    <div class="sidebar-logo">
      <div class="logo-icon">
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <span class="logo-text">知途</span>
      <span class="logo-subtitle">Zhitu</span>
    </div>

    <!-- 导航菜单 -->
    <nav class="sidebar-nav">
      <!-- 总览组 -->
      <div class="nav-group">
        <div class="nav-group-title">总览</div>
        <router-link
          :to="overviewItem.path"
          class="nav-item"
          :class="{ active: isActive(overviewItem.path) }"
        >
          <component :is="overviewItem.icon" :size="18" />
          <span class="nav-label">{{ overviewItem.label }}</span>
        </router-link>
      </div>

      <!-- 学习组 -->
      <div class="nav-group">
        <div class="nav-group-title">学习</div>
        <router-link
          v-for="item in learningItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" :size="18" />
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </div>

      <!-- 知识组 -->
      <div class="nav-group">
        <div class="nav-group-title">知识</div>
        <router-link
          v-for="item in knowledgeItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" :size="18" />
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </div>

      <!-- 诊断组 -->
      <div class="nav-group">
        <div class="nav-group-title">诊断</div>
        <router-link
          v-for="item in diagnosisItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" :size="18" />
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </div>

      <!-- 智能体中心 -->
      <div class="nav-group">
        <div class="nav-group-title">智能体中心</div>
        <router-link
          v-for="item in agentItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" :size="18" />
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </div>
    </nav>

    <!-- 底部导航 -->
    <div class="sidebar-footer">
      <router-link
        v-for="item in footerItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <component :is="item.icon" :size="18" />
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </div>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import {
  LayoutDashboard,
  BookOpen,
  Calendar,
  FileText,
  Brain,
  Code,
  Stethoscope,
  AlertTriangle,
  Target,
  Bot,
  BarChart3,
  Trophy,
  Settings
} from 'lucide-vue-next'

const route = useRoute()

const overviewItem = { path: '/home', label: '仪表盘', icon: LayoutDashboard }

const learningItems = [
  { path: '/learning-path', label: '学习路径', icon: BookOpen },
  { path: '/calendar', label: '学习日历', icon: Calendar },
  { path: '/study-notes', label: '学习笔记', icon: FileText }
]

const knowledgeItems = [
  { path: '/knowledge', label: '知识库', icon: Brain },
  { path: '/code-analyze', label: '代码管理', icon: Code }
]

const diagnosisItems = [
  { path: '/assessment', label: '能力评估', icon: Stethoscope },
  { path: '/capability/weakness', label: '薄弱点分析', icon: AlertTriangle },
  { path: '/capability/adaptive', label: '自适应学习', icon: Target }
]

const agentItems = [
  { path: '/agents', label: '智能体中心', icon: Bot }
]

const footerItems = [
  { path: '/statistics', label: '学习统计', icon: BarChart3 },
  { path: '/achievements', label: '成就系统', icon: Trophy },
  { path: '/profile', label: '个人中心', icon: Settings }
]

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.sidebar {
  width: $sidebar-width;
  height: 100vh;
  background: rgba($bg-surface, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba($border-default, 0.5);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 40;
  transition: transform $transition-normal;
}

.sidebar-logo {
  padding: $space-5 $space-4;
  display: flex;
  align-items: center;
  gap: $space-3;
  border-bottom: 1px solid $border-subtle;
  flex-shrink: 0;
}

.logo-icon {
  width: 28px;
  height: 28px;
  color: $accent-indigo;
  display: flex;
  align-items: center;
  justify-content: center;

  svg { width: 100%; height: 100%; }
}

.logo-text {
  font-size: $text-lg;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: -0.01em;
}

.logo-subtitle {
  font-size: $text-xs;
  color: $text-muted;
  margin-left: $space-1;
  font-weight: 500;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: $space-3 0;
}

.sidebar-footer {
  border-top: 1px solid $border-subtle;
  padding: $space-3 0;
  flex-shrink: 0;
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
}

.nav-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: 8px 16px;
  margin: 1px 8px;
  color: $text-secondary;
  text-decoration: none;
  border-radius: $radius-md;
  transition: all $transition-fast;
  border-left: 2px solid transparent;

  &:hover {
    background: $bg-muted;
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

/* 移动端适配 */
@media (max-width: $breakpoint-md) {
  .sidebar {
    transform: translateX(-100%);
    z-index: 100;

    &.open {
      transform: translateX(0);
    }
  }
}
</style>