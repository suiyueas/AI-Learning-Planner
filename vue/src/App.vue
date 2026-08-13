<template>
  <div class="app-container" :class="{ 'auth-layout': isAuthPage }">
    <template v-if="!isAuthPage">
      <nav class="navbar">
      <div class="navbar-brand">
        <div class="brand-icon-wrap breathing-glow">
          <Cpu :size="22" class="brand-icon" />
        </div>
        <span class="brand-text">AI学习规划师</span>
      </div>
      <div class="navbar-menu">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item" :class="{ active: isActive(item.path) }">
          <component :is="item.icon" :size="16" />
          <span class="nav-text">{{ item.title }}</span>
        </router-link>
      </div>
      <div class="navbar-right">
        <NotificationSystem />
        <template v-if="!authStore.isAuthenticated">
          <router-link to="/login" class="login-btn wave-ripple">
            <LogIn :size="16" />
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
            <ChevronDown :size="14" class="user-arrow" :class="{ open: showDropdown }" />
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
                <CircleUser :size="16" class="dropdown-icon" />
                <span>个人中心</span>
              </router-link>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item logout-item" @click="handleLogout">
                <LogOut :size="16" class="dropdown-icon" />
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
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <keep-alive :include="cachedPages">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </transition>
      </router-view>
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
import { Home as LucideHome, MessageSquare as LucideMessage, Wrench as LucideWrench, BookOpen as LucideBook, Bot as LucideBot, Grid3X3 as LucideGrid, LogIn, ChevronDown, CircleUser, LogOut, Cpu } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const navItems = [
  { path: '/home', icon: LucideHome, title: '首页' },
  { path: '/chat', icon: LucideMessage, title: '对话' },
  { path: '/tools', icon: LucideWrench, title: '工具' },
  { path: '/knowledge', icon: LucideBook, title: '知识库' },
  { path: '/agents', icon: LucideBot, title: '智能体' },
  { path: '/modules', icon: LucideGrid, title: '全部功能' }
]
const showDropdown = ref(false)
const userModuleRef = ref(null)
const isAuthPage = computed(() => route.meta.layout === 'auth')
const cachedPages = computed(() => ['Chat', 'Tools', 'Knowledge', 'Agents'])
const isActive = (path) => route.path === path
const toggleDropdown = () => { showDropdown.value = !showDropdown.value }
const handleLogout = () => { showDropdown.value = false; authStore.logout(); router.push('/login') }
const handleClickOutside = (e) => { if (userModuleRef.value && !userModuleRef.value.contains(e.target)) { showDropdown.value = false } }
onMounted(() => { authStore.initAuth(); document.addEventListener('click', handleClickOutside) })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside) })
</script>

<style lang="scss" scoped>
.app-container { min-height: 100vh; display: flex; flex-direction: column; }
.navbar { position: fixed; top: 0; left: 0; right: 0; z-index: 1000; display: flex; align-items: center; justify-content: space-between; padding: 12px 24px; background: rgba(10, 10, 26, 0.9); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border-bottom: 1px solid rgba(0, 245, 212, 0.06); box-shadow: 0 4px 30px rgba(0, 0, 0, 0.3); }
.navbar-brand { display: flex; align-items: center; gap: 12px; }
.brand-icon-wrap { position: relative; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; }
.brand-icon { color: var(--accent-primary); filter: drop-shadow(0 0 8px rgba(0, 245, 212, 0.4)); }
.brand-text { font-size: 1.15rem; font-weight: 700; background: linear-gradient(135deg, #00f5d4 0%, #0055FF 50%, #7b61ff 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.navbar-menu { display: flex; gap: 2px; }
.nav-item { display: flex; align-items: center; gap: 6px; padding: 8px 14px; border-radius: 10px; text-decoration: none; color: var(--text-muted); transition: all 0.3s ease; font-size: 0.85rem; position: relative; overflow: hidden; &::after { content: ''; position: absolute; bottom: -1px; left: 50%; transform: translateX(-50%); width: 0; height: 2px; background: #00f5d4; border-radius: 1px; transition: width 0.3s ease; } &:hover { color: var(--text-primary); background: rgba(0, 245, 212, 0.04); } &.active { color: #00f5d4; background: rgba(0, 245, 212, 0.06); font-weight: 600; &::after { width: 20px; box-shadow: 0 0 8px rgba(0, 245, 212, 0.5); } } }
.nav-text { font-weight: 500; }
.navbar-right { display: flex; align-items: center; gap: 8px; position: relative; }
.user-module { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 6px 10px; border-radius: 10px; transition: all 0.3s ease; &:hover { background: rgba(0, 245, 212, 0.04); } }
.user-avatar { position: relative; width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; font-size: 0.8rem; flex-shrink: 0; box-shadow: 0 0 12px rgba(0, 245, 212, 0.2); }
.online-dot { position: absolute; bottom: 0; right: 0; width: 10px; height: 10px; background: var(--accent-emerald); border: 2px solid var(--bg-primary); border-radius: 50%; animation: onlinePulse 2s ease-in-out infinite; }
@keyframes onlinePulse { 0%, 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); } 50% { box-shadow: 0 0 0 4px rgba(16, 185, 129, 0); } }
.user-name { font-size: 0.85rem; font-weight: 500; color: var(--text-secondary); white-space: nowrap; }
.avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.avatar-text { color: #fff; font-weight: 600; font-size: 0.8rem; line-height: 1; }
.user-avatar.has-image { background: transparent; overflow: hidden; padding: 0; }
.user-arrow { color: var(--text-muted); transition: transform 0.3s ease; &.open { transform: rotate(180deg); } }
.login-btn { display: flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 10px; background: linear-gradient(135deg, rgba(0, 245, 212, 0.12), rgba(0, 85, 255, 0.08)); border: 1px solid rgba(0, 245, 212, 0.2); color: var(--accent-primary); font-size: 0.85rem; font-weight: 600; text-decoration: none; transition: all 0.3s ease; overflow: hidden; &:hover { background: linear-gradient(135deg, rgba(0, 245, 212, 0.2), rgba(0, 85, 255, 0.12)); border-color: rgba(0, 245, 212, 0.35); transform: translateY(-1px); box-shadow: 0 0 16px rgba(0, 245, 212, 0.12); color: var(--accent-primary); } }
.user-dropdown { position: absolute; top: calc(100% + 8px); right: 0; width: 240px; background: rgba(15, 20, 40, 0.95); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border: 1px solid rgba(0, 245, 212, 0.1); border-radius: 14px; box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5), 0 0 30px rgba(0, 245, 212, 0.05); z-index: 1001; overflow: hidden; animation: dropdownIn 0.2s ease; }
@keyframes dropdownIn { from { opacity: 0; transform: translateY(-8px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
.dropdown-header { display: flex; align-items: center; gap: 12px; padding: 16px; }
.dropdown-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden; box-shadow: 0 0 12px rgba(0, 245, 212, 0.2); .avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; } .avatar-text { color: #fff; font-weight: 700; font-size: 1rem; } }
.dropdown-user-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.dropdown-name { font-size: 0.9rem; font-weight: 600; color: var(--text-primary); }
.dropdown-email { font-size: 0.8rem; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dropdown-divider { height: 1px; background: rgba(0, 245, 212, 0.06); }
.dropdown-item { display: flex; align-items: center; gap: 10px; padding: 12px 16px; font-size: 0.85rem; color: var(--text-secondary); text-decoration: none; transition: all 0.15s ease; width: 100%; border: none; background: transparent; cursor: pointer; text-align: left; &:hover { background: rgba(0, 245, 212, 0.04); color: var(--text-primary); } &.logout-item { color: var(--accent-pink); &:hover { background: rgba(236, 72, 153, 0.06); } } }
.dropdown-icon { color: var(--text-muted); flex-shrink: 0; }
.dropdown-overlay { position: fixed; inset: 0; z-index: 999; }
.dropdown-enter-active, .dropdown-leave-active { transition: all 0.2s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-8px); }

/* ===== 路由切换过渡动画 ===== */
.page-fade-enter-active { transition: opacity 0.25s ease; }
.page-fade-leave-active { transition: opacity 0.18s ease; }
.page-fade-enter-from { opacity: 0; }
.page-fade-leave-to { opacity: 0; }

/* ===== 路由加载占位 ===== */
.page-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 68px);
  background: #0a0a1a;
  gap: 16px;
}
.loading-ring {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(100,100,180,0.08);
  border-top-color: #00f5d4;
  border-radius: 50%;
  animation: ring-spin 0.8s linear infinite;
}
.loading-text {
  font-size: 0.85rem;
  color: #606090;
}
@keyframes ring-spin {
  to { transform: rotate(360deg); }
}
.main-content { flex: 1; margin-top: 68px; }
.auth-main { margin-top: 0; }
@media (max-width: 1024px) { .navbar { padding: 10px 16px; } .nav-text { display: none; } .nav-item { padding: 10px 12px; } .user-name { display: none; } .main-content { margin-top: 56px; } }
@media (max-width: 640px) { .navbar { flex-direction: column; gap: 10px; padding: 10px 16px; } .navbar-menu { flex-wrap: wrap; justify-content: center; } .main-content { margin-top: 110px; } }
</style>
