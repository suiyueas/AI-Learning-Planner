// 认证状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import { getProfile as getProfileApi } from '@/api/user'
import { useContextStore } from '@/stores/contextStore'
import { useKnowledgeStore } from '@/stores/knowledgeStore'
import { useToolsStore } from '@/stores/toolsStore'
import { useChatStore } from '@/stores/chatStore'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const user = ref({
    id: null,
    username: '',
    email: '',
    nickname: '',
    role: '',
    avatarUrl: '',
    learningGoal: '',
    bio: '',
    createdAt: null,
    learningStats: {
      totalLearningHours: 0,
      completedNodes: 0,
      achievementCount: 0,
      continuousDays: 0
    }
  })

  // 计算属性
  const isAuthenticated = computed(() => !!token.value)

  // 管理员判定（登录响应与个人资料均会写入 role 字段）
  const isAdmin = computed(() => user.value.role === 'ADMIN')

  const displayName = computed(() => {
    return user.value.nickname || user.value.username || '未登录'
  })

  const displayAvatar = computed(() => {
    if (user.value.avatarUrl) {
      return user.value.avatarUrl
    }
    return displayName.value.charAt(0).toUpperCase()
  })

  const hasAvatar = computed(() => !!user.value.avatarUrl)

  /**
   * 用户切换时同步所有用户相关 store（在 action 内实例化，避免 Pinia 未激活）
   * userId 为 null 时清空全部用户数据（登出场景）
   */
  function notifyUserContextChanged(userId) {
    useContextStore().resetForUser(userId)
    useKnowledgeStore().resetForUser(userId)
    useToolsStore().resetForUser(userId)
    useChatStore().resetForUser(userId)
  }

  /**
   * 初始化认证状态（从 localStorage 恢复）
   */
  function initAuth() {
    const savedToken = localStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
      fetchProfile()
    }
  }

  /**
   * 用户登录
   */
  async function login(credentials) {
    const res = await loginApi(credentials)
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userId', String(res.data.userId))
      user.value.id = res.data.userId
      user.value.username = res.data.username
      user.value.nickname = res.data.nickname || res.data.username
      user.value.role = res.data.role || ''
      user.value.avatarUrl = res.data.avatarUrl || ''
      // 获取完整用户信息
      await fetchProfile()
      // 触发所有用户相关 store 重置并加载当前用户数据
      notifyUserContextChanged(res.data.userId)
      return true
    }
    throw new Error(res.message || '登录失败')
  }

  /**
   * 用户注册
   */
  async function register(userData) {
    // 兜底：后端要求 confirmPassword 必填，调用方未传时使用 password
    const payload = {
      ...userData,
      confirmPassword: userData.confirmPassword ?? userData.password
    }
    const res = await registerApi(payload)
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userId', String(res.data.userId))
      user.value.id = res.data.userId
      user.value.username = res.data.username
      user.value.nickname = res.data.nickname || res.data.username
      user.value.role = res.data.role || ''
      // 获取完整用户信息
      await fetchProfile()
      // 触发所有用户相关 store 重置并加载当前用户数据
      notifyUserContextChanged(res.data.userId)
      return true
    }
    throw new Error(res.message || '注册失败')
  }

  /**
   * 获取用户信息
   */
  async function fetchProfile() {
    try {
      const res = await getProfileApi()
      if (res.code === 200 && res.data) {
        user.value = { ...user.value, ...res.data }
      }
    } catch (e) {
      // 401/403 → Token 无效，清除登录状态
      if (e.response?.status === 401 || e.response?.status === 403) {
        console.warn('Token 无效或已过期，清除登录状态')
        token.value = ''
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
      } else {
        console.warn('获取用户信息失败:', e.message)
      }
    }
  }

  /**
   * 更新用户本地信息
   */
  function updateUser(data) {
    user.value = { ...user.value, ...data }
  }

  /**
   * 退出登录
   */
  function logout() {
    token.value = ''
    user.value = {
      id: null,
      username: '',
      email: '',
      nickname: '',
      role: '',
      avatarUrl: '',
      learningGoal: '',
      bio: '',
      createdAt: null,
      learningStats: {
        totalLearningHours: 0,
        completedNodes: 0,
        achievementCount: 0,
        continuousDays: 0
      }
    }
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    // 清空所有用户相关 store，避免残留上一用户数据
    notifyUserContextChanged(null)
  }

  return {
    // 状态
    token,
    user,
    // 计算属性
    isAuthenticated,
    isAdmin,
    displayName,
    displayAvatar,
    hasAvatar,
    // 方法
    initAuth,
    login,
    register,
    fetchProfile,
    updateUser,
    logout
  }
})
