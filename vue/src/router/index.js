// ============================================
// 路由配置 - 精简版
// 6个核心页面路由定义
// ============================================

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 路由定义 - 使用静态导入
export const routes = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: {
      title: '首页',
      requiresAuth: true,
    },
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue'),
    meta: {
      title: '智能对话',
      requiresAuth: true,
    },
  },
  {
    path: '/tools',
    name: 'Tools',
    component: () => import('@/views/Tools.vue'),
    meta: {
      title: '工具中心',
      requiresAuth: true,
    },
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: {
      title: '知识库',
      requiresAuth: true,
    },
  },
  {
    path: '/agents',
    name: 'Agents',
    component: () => import('@/views/Agents.vue'),
    meta: {
      title: '智能体',
      requiresAuth: true,
    },
  },
  {
    path: '/modules',
    name: 'AllModules',
    component: () => import('@/views/AllModules.vue'),
    meta: {
      title: '全部功能',
      requiresAuth: true,
    },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '登录',
      layout: 'auth',
    },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: {
      title: '注册',
      layout: 'auth',
    },
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue'),
    meta: { title: '学习统计', requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: {
      title: '个人中心',
      requiresAuth: true,
    },
  },
  {
    path: '/learning-path',
    name: 'LearningPathList',
    component: () => import('@/views/LearningPathList.vue'),
    meta: { title: '学习路径', requiresAuth: true },
  },
  {
    path: '/learning-path/:id',
    name: 'LearningPathDetail',
    component: () => import('@/views/LearningPathDetail.vue'),
    meta: { title: '学习路径详情', requiresAuth: true },
  },
  {
    path: '/calendar',
    name: 'LearningCalendar',
    component: () => import('@/views/LearningCalendar.vue'),
    meta: { title: '学习日历', requiresAuth: true },
  },
  {
    path: '/learning-paths',
    name: 'LearningPathBrowser',
    component: () => import('@/views/LearningPathBrowser.vue'),
    meta: { title: '学习路径浏览', requiresAuth: true },
  },
  {
    path: '/study-notes',
    name: 'StudyNotes',
    component: () => import('@/views/StudyNotes.vue'),
    meta: { title: '学习笔记', requiresAuth: true },
  },
  {
    path: '/learning-records',
    name: 'LearningRecords',
    component: () => import('@/views/LearningRecords.vue'),
    meta: { title: '学习记录', requiresAuth: true },
  },
  // ===== 核心能力详情页 =====
  {
    path: '/capability/diagnosis',
    name: 'DiagnosisDetail',
    component: () => import('@/views/capability/DiagnosisDetail.vue'),
    meta: { title: '智能诊断', requiresAuth: true },
  },
  {
    path: '/capability/weakness',
    name: 'WeaknessDetail',
    component: () => import('@/views/capability/WeaknessDetail.vue'),
    meta: { title: '薄弱知识点专项分析', requiresAuth: true },
  },
  {
    path: '/capability/planning',
    name: 'PlanningDetail',
    component: () => import('@/views/capability/PlanningDetail.vue'),
    meta: { title: '动态规划', requiresAuth: true },
  },
  {
    path: '/capability/progress',
    name: 'ProgressDetail',
    component: () => import('@/views/capability/ProgressDetail.vue'),
    meta: { title: '进度追踪', requiresAuth: true },
  },
  {
    path: '/capability/adaptive',
    name: 'AdaptiveDetail',
    component: () => import('@/views/capability/AdaptiveDetail.vue'),
    meta: { title: '自适应学习', requiresAuth: true },
  },
  {
    path: '/capability/adaptive/history',
    name: 'AdaptiveHistory',
    component: () => import('@/views/capability/AdaptiveHistory.vue'),
    meta: { title: '自适应调整历史', requiresAuth: true },
  },
  // ===== 功能页面路由 =====
  {
    path: '/assessment',
    name: 'Assessment',
    component: () => import('@/views/Assessment.vue'),
    meta: { title: '能力测评', requiresAuth: true },
  },
  {
    path: '/goal-setting',
    name: 'GoalSetting',
    component: () => import('@/views/GoalSetting.vue'),
    meta: { title: '目标设定', requiresAuth: true },
  },
  {
    path: '/exercise',
    name: 'Exercise',
    component: () => import('@/views/Exercise.vue'),
    meta: { title: '习题生成', requiresAuth: true },
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('@/views/Report.vue'),
    meta: { title: '学情报告', requiresAuth: true },
  },
  {
    path: '/achievements',
    name: 'Achievements',
    component: () => import('@/views/Achievements.vue'),
    meta: { title: '成就打卡', requiresAuth: true },
  },
  {
    path: '/web-fetch',
    name: 'WebFetch',
    component: () => import('@/views/FeaturePlaceholder.vue'),
    meta: { title: '网页抓取', featureName: '网页抓取', requiresAuth: true },
  },
  {
    path: '/code-analyze',
    name: 'CodeAnalyzer',
    component: () => import('@/views/CodeAnalyzer.vue'),
    meta: { title: '代码解析', requiresAuth: true },
  },
  // 404 路由
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/home',
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  const title = to.meta.title
  if (title) {
    document.title = `${title} - AI学习规划师`
  }

  const authStore = useAuthStore()

  // 已登录用户访问登录/注册页 → 跳转首页
  if ((to.path === '/login' || to.path === '/register') && authStore.isAuthenticated) {
    next('/home')
    return
  }

  // 未登录用户访问需认证页面 → 跳转登录页
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  next()
})

export default router