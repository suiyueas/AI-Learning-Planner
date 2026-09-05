<template>
  <header class="topbar">
    <!-- 左侧区域 -->
    <div class="topbar-left">
      <button class="menu-toggle" @click="$emit('toggle-sidebar')">
        <Menu :size="20" />
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
    </div>

    <!-- 右侧区域 -->
    <div class="topbar-right">
      <div class="notification-wrapper">
        <button class="topbar-btn" @click="toggleNotifications">
          <Bell :size="20" />
          <span v-if="unreadCount > 0" class="badge" :class="{ emergency: hasEmergency }">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
        <NotificationPanel
          :visible="showNotificationPanel"
          @close="showNotificationPanel = false"
          @refresh-stats="onUnreadStatsUpdate"
          @view-detail="handleNotificationAction"
        />
      </div>



      <!-- 用户头像下拉 -->
      <div class="user-menu" @click.stop="showUserMenu = !showUserMenu">
        <div class="user-avatar" v-if="authStore.hasAvatar" @click.stop="goToProfile">
          <img :src="authStore.displayAvatar" alt="头像" />
        </div>
        <div class="user-avatar placeholder" v-else @click.stop="goToProfile">
          {{ authStore.displayName.charAt(0).toUpperCase() }}
        </div>

        <transition name="dropdown">
          <div v-if="showUserMenu" class="user-dropdown" @click.stop>
            <div class="dropdown-header">
              <div class="dropdown-name">{{ authStore.displayName }}</div>
              <div class="dropdown-email">{{ authStore.user.email || '未设置邮箱' }}</div>
            </div>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item danger" @click="handleLogout">
              <LogOut :size="16" />
              <span>退出登录</span>
            </button>
          </div>
        </transition>
      </div>
    </div>

    <!-- 调整计划对话框 -->
    <AdjustPlanDialog
      v-model="showAdjustPlanDialog"
      :plan-data="adjustPlanData"
    />

    <!-- 退出登录确认对话框 -->
    <LogoutConfirmDialog
      v-model="showLogoutConfirm"
      @cancel="showLogoutConfirm = false"
      @confirm="confirmLogout"
    />
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Menu, Bell, LogOut } from 'lucide-vue-next'
import NotificationPanel from './NotificationPanel.vue'
import AdjustPlanDialog from '@/components/AdjustPlanDialog.vue'
import LogoutConfirmDialog from '@/components/common/LogoutConfirmDialog.vue'
import { getUnreadStats } from '@/api/notificationApi'

defineProps({})

defineEmits(['toggle-sidebar'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showUserMenu = ref(false)
const showNotificationPanel = ref(false)
const showAdjustPlanDialog = ref(false)
const adjustPlanData = ref({})
const unreadCount = ref(0)
const hasEmergency = ref(false)
const showLogoutConfirm = ref(false)

const pageTitle = computed(() => route.meta.title || '知途')

const goToProfile = () => {
  showUserMenu.value = false
  router.push('/profile')
}

const handleLogout = () => {
  showLogoutConfirm.value = true
  showUserMenu.value = false
}

const confirmLogout = () => {
  authStore.logout()
  showLogoutConfirm.value = false
  router.push('/login')
}

const toggleNotifications = () => {
  showNotificationPanel.value = !showNotificationPanel.value
}

const onUnreadStatsUpdate = (count) => {
  unreadCount.value = count
}

const handleNotificationAction = (action) => {
  if (action.type === 'adjust-plan') {
    // 如果有 pathId，打开调整计划对话框
    adjustPlanData.value = {
      id: action.pathId,
      name: action.pathName || '',
      isActive: true
    }
    showAdjustPlanDialog.value = true
  }
}

const loadUnreadStats = async () => {
  try {
    const res = await getUnreadStats()
    if (res) {
      unreadCount.value = res.total || 0
      hasEmergency.value = (res.emergency || 0) > 0
    }
  } catch (e) {
    console.error('加载未读统计失败', e)
  }
}

const handleClickOutside = (e) => {
  if (!e.target.closest('.user-menu')) {
    showUserMenu.value = false
  }
  if (!e.target.closest('.notification-wrapper') && !e.target.closest('.notification-panel')) {
    showNotificationPanel.value = false
  }
}

let unreadInterval = null

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  loadUnreadStats()
  // 每30秒轮询未读统计
  unreadInterval = setInterval(loadUnreadStats, 30000)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (unreadInterval) {
    clearInterval(unreadInterval)
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.topbar {
  height: $topbar-height;
  background: rgba($bg-surface, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba($border-default, 0.5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $space-4;
  position: sticky;
  top: 0;
  z-index: 30;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.menu-toggle {
  display: none;
  background: none;
  border: none;
  color: $text-secondary;
  cursor: pointer;
  padding: $space-2;
  border-radius: $radius-md;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }

  @media (max-width: $breakpoint-md) {
    display: flex;
  }
}

.page-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.topbar-btn {
  position: relative;
  background: none;
  border: none;
  color: $text-secondary;
  cursor: pointer;
  padding: $space-2;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }

  &.active {
    color: $accent-indigo;
    background: rgba($accent-indigo, 0.1);
  }
}

.notification-wrapper {
  position: relative;
}

.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: $color-danger;
  color: white;
  font-size: 10px;
  font-weight: 600;
  min-width: 16px;
  height: 16px;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;

  &.emergency {
    background: #EF4444;
    box-shadow: 0 0 8px rgba(239, 68, 68, 0.6);
  }
}

.user-menu {
  position: relative;
  cursor: pointer;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: $radius-full;
  overflow: hidden;
  border: 2px solid $border-default;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &.placeholder {
    background: $gradient-brand;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: $text-sm;
    font-weight: 600;
  }
}

.user-dropdown {
  position: absolute;
  top: calc(100% + $space-2);
  right: 0;
  width: 200px;
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  z-index: 50;
  overflow: hidden;
}

.dropdown-header {
  padding: $space-3 $space-4;
}

.dropdown-name {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
}

.dropdown-email {
  font-size: $text-xs;
  color: $text-muted;
  margin-top: 2px;
}

.dropdown-divider {
  height: 1px;
  background: $border-subtle;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-4;
  color: $text-secondary;
  text-decoration: none;
  transition: all $transition-fast;
  width: 100%;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  font-size: $text-sm;

  &:hover {
    background: $bg-muted;
    color: $text-primary;
  }

  &.danger {
    color: $color-danger;
    &:hover { background: rgba($color-danger, 0.1); }
  }
}

/* dropdown 动画 */
.dropdown-enter-active, .dropdown-leave-active {
  transition: all 0.15s ease;
}
.dropdown-enter-from, .dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: $breakpoint-md) {
  .topbar { padding: 0 $space-3; }
  .page-title { font-size: $text-base; }
}
</style>