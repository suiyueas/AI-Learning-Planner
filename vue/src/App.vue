<template>
  <div class="app-container" :class="{ 'auth-layout': isAuthPage }">
    <template v-if="!isAuthPage">
      <nav class="navbar">
        <div class="navbar-brand">
          <div class="brand-icon-wrap">
            <BookOpen :size="20" class="brand-icon" />
          </div>
          <span class="brand-text">AI学习规划师</span>
        </div>
        <div class="navbar-menu">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <component :is="item.icon" :size="15" />
            <span class="nav-text">{{ item.title }}</span>
          </router-link>
        </div>
        <div class="navbar-right">
          <NotificationSystem />
          <template v-if="!authStore.isAuthenticated">
            <router-link to="/login" class="login-btn">
              <LogIn :size="15" />
              <span>登录</span>
            </router-link>
          </template>
          <template v-else>
            <div ref="userModuleRef" class="user-module" @click="toggleDropdown">
              <div class="user-avatar" :class="{ 'has-image': authStore.hasAvatar }">
                <img v-if="authStore.hasAvatar" :src="authStore.user.avatarUrl" :alt="authStore.displayName" class="avatar-img" />
                <span v-else class="avatar-text">{{ authStore.displayAvatar }}</span>
                <span class="online-dot"></span>
              </div>
              <span class="user-name">{{ authStore.displayName }}</span>
              <ChevronDown :size="13" class="user-arrow" :class="{ open: showDropdown }" />
            </div>
            <transition name="dropdown">
              <div v-if="showDropdown" class="user-dropdown" @click.stop>
                <div class="dropdown-header">
                  <div class="dropdown-avatar">
                    <img v-if="authStore.hasAvatar" :src="authStore.user.avatarUrl" class="avatar-img" />
                    <span v-else class="avatar-text">{{ authStore.displayAvatar }}</span>
                  </div>
                  <div class="dropdown-user-info">
                    <span class="dropdown-name">{{ authStore.displayName }}</span>
                    <span class="dropdown-email">{{ authStore.user.email }}</span>
                  </div>
                </div>
                <div class="dropdown-divider"></div>
                <router-link to="/profile" class="dropdown-item" @click="showDropdown = false">
                  <CircleUser :size="15" class="dropdown-icon" />
                  <span>个人中心</span>
                </router-link>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item logout-item" @click="handleLogout">
                  <LogOut :size="15" class="dropdown-icon" />
                  <span>退出登录</span>
                </button>
              </div>
            </transition>
          </template>
        </div>
      </nav>
      <div v-if="showDropdown" class="dropdown-overlay" @click="showDropdown = false"></div>
    </template>
    <main class="main-content" :class="{ 'auth-main': isAuthPage }">
      <router-view />
    </main>
    <GlobalLoading />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import GlobalLoading from '@/components/GlobalLoading.vue'
import NotificationSystem from '@/components/NotificationSystem.vue'
import { LayoutDashboard, Map, BookOpen, ClipboardCheck, Bot } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navItems = [
  { path: '/home', icon: LayoutDashboard, title: '仪表盘' },
  { path: '/learning-path', icon: Map, title: '学习路径' },
  { path: '/knowledge', icon: BookOpen, title: '知识库' },
  { path: '/assessment', icon: ClipboardCheck, title: '测评' },
  { path: '/agents', icon: Bot, title: '智能体中心' }
]

const showDropdown = ref(false)
const userModuleRef = ref(null)
const isAuthPage = computed(() => route.meta.layout === 'auth')
const isActive = (path) => route.path === path

const toggleDropdown = () => { showDropdown.value = !showDropdown.value }
const handleLogout = () => { showDropdown.value = false; authStore.logout(); router.push('/login') }
const handleClickOutside = (e) => {
  if (userModuleRef.value && !userModuleRef.value.contains(e.target)) {
    showDropdown.value = false
  }
}

onMounted(() => {
  authStore.initAuth()
  document.addEventListener('click', handleClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style lang="scss" scoped>
@use './styles/variables' as *;

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== 导航栏 ===== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: rgba($bg-primary, 0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid $border-subtle;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-icon-wrap {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-primary, 0.1);
  border-radius: $radius-sm;
}

.brand-icon {
  color: $accent-primary;
}

.brand-text {
  font-family: $font-display;
  font-size: 1.05rem;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: 0.02em;
}

.navbar-menu {
  display: flex;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: $radius-sm;
  text-decoration: none;
  color: $text-muted;
  font-size: 0.85rem;
  font-weight: 500;
  transition: all $transition-fast;
  position: relative;

  &:hover {
    color: $text-secondary;
    background: rgba($accent-primary, 0.04);
  }

  &.active {
    color: $accent-primary;
    background: rgba($accent-primary, 0.06);

    &::after {
      content: '';
      position: absolute;
      bottom: -1px;
      left: 50%;
      transform: translateX(-50%);
      width: 16px;
      height: 2px;
      background: $accent-primary;
      border-radius: 1px;
    }
  }
}

.nav-text {
  font-weight: 500;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

/* ===== 用户模块 ===== */
.user-module {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba($accent-primary, 0.04);
  }
}

.user-avatar {
  position: relative;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, $accent-primary, $accent-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 9px;
  height: 9px;
  background: $accent-emerald;
  border: 2px solid $bg-primary;
  border-radius: 50%;
}

.user-name {
  font-size: 0.85rem;
  font-weight: 500;
  color: $text-secondary;
  white-space: nowrap;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-text {
  color: #fff;
  font-weight: 600;
  font-size: 0.75rem;
  line-height: 1;
}

.user-avatar.has-image {
  background: transparent;
  overflow: hidden;
  padding: 0;
}

.user-arrow {
  color: $text-muted;
  transition: transform $transition-fast;

  &.open {
    transform: rotate(180deg);
  }
}

/* ===== 登录按钮 ===== */
.login-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: $radius-sm;
  background: rgba($accent-primary, 0.08);
  border: 1px solid rgba($accent-primary, 0.15);
  color: $accent-primary;
  font-size: 0.85rem;
  font-weight: 500;
  text-decoration: none;
  transition: all $transition-fast;

  &:hover {
    background: rgba($accent-primary, 0.12);
    border-color: rgba($accent-primary, 0.25);
    color: $accent-primary;
  }
}

/* ===== 下拉菜单 ===== */
.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 220px;
  background: $bg-elevated;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  box-shadow: $shadow-lg;
  z-index: 1001;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.dropdown-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, $accent-primary, $accent-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;

  .avatar-img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }

  .avatar-text {
    color: #fff;
    font-weight: 600;
    font-size: 0.9rem;
  }
}

.dropdown-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.dropdown-name {
  font-size: 0.88rem;
  font-weight: 600;
  color: $text-primary;
}

.dropdown-email {
  font-size: 0.78rem;
  color: $text-muted;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-divider {
  height: 1px;
  background: $border-subtle;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 16px;
  font-size: 0.85rem;
  color: $text-secondary;
  text-decoration: none;
  transition: all $transition-fast;
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;

  &:hover {
    background: rgba($accent-primary, 0.04);
    color: $text-primary;
  }

  &.logout-item {
    color: $accent-pink;

    &:hover {
      background: rgba($accent-pink, 0.06);
    }
  }
}

.dropdown-icon {
  color: $text-muted;
  flex-shrink: 0;
}

.dropdown-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ===== 主内容区 ===== */
.main-content {
  flex: 1;
  margin-top: 56px;
}

.auth-main {
  margin-top: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .navbar {
    padding: 0 16px;
  }

  .nav-text {
    display: none;
  }

  .nav-item {
    padding: 8px 10px;
  }

  .user-name {
    display: none;
  }

  .main-content {
    margin-top: 48px;
  }
}

@media (max-width: 640px) {
  .navbar {
    height: auto;
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 16px;
  }

  .navbar-menu {
    flex-wrap: wrap;
    justify-content: center;
  }

  .main-content {
    margin-top: 100px;
  }
}
</style>