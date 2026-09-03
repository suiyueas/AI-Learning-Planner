// ============================================
// 路由配置 - 精简版
// 以学习流程为核心
// ============================================

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 路由定义
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
      title: '仪表盘',
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
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: {
      title: '知识库',
      requiresAuth: true,
    },
  },
  {
    path: '/assessment',
    name: 'Assessment',
    component: () => import('@/views/Assessment.vue'),
    meta: { title: '测评', requiresAuth: true },
  },
  {
    path: '/chat',
    redirect: '/agents',
  },
  {
    path: '/exercise',
    name: 'Exercise',
    component: () => import('@/views/Exercise.vue'),
    meta: { title: '习题练习', requiresAuth: true },
  },
  {
    path: '/goal-setting',
    name: 'GoalSetting',
    component: () => import('@/views/GoalSetting.vue'),
    meta: { title: '目标设定', requiresAuth: true },
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
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue'),
    meta: { title: '学习统计', requiresAuth: true },
  },
  {
    path: '/achievements',
    name: 'Achievements',
    component: () => import('@/views/Achievements.vue'),
    meta: { title: '成就打卡', requiresAuth: true },
  },
  {
    path: '/calendar',
    name: 'LearningCalendar',
    component: () => import('@/views/LearningCalendar.vue'),
    meta: { title: '学习日历', requiresAuth: true },
  },
  {
    path: '/study-notes',
    name: 'StudyNotes',
    component: () => import('@/views/StudyNotes.vue'),
    meta: { title: '学习笔记', requiresAuth: true },
  },
  {
    path: '/code-analyze',
    name: 'CodeAnalyzer',
    component: () => import('@/views/CodeAnalyzer.vue'),
    meta: { title: '代码解析', requiresAuth: true },
  },
  {
    path: '/agents',
    name: 'Agents',
    component: () => import('@/views/Agents.vue'),
    meta: { title: '智能体中心', requiresAuth: true },
  },
  // 旧路由重定向
  { path: '/report', redirect: '/home' },
  { path: '/tools', redirect: '/agents' },
  { path: '/all-modules', redirect: '/home' },
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
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '注册',
      layout: 'auth',
    },
  },
  // 能力诊断子页面
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
    meta: { title: '薄弱点分析', requiresAuth: true },
  },
  {
    path: '/capability/progress',
    name: 'ProgressDetail',
    component: () => import('@/views/capability/ProgressDetail.vue'),
    meta: { title: '学习进度', requiresAuth: true },
  },
  {
    path: '/capability/planning',
    name: 'PlanningDetail',
    component: () => import('@/views/capability/PlanningDetail.vue'),
    meta: { title: '学习规划', requiresAuth: true },
  },
  {
    path: '/capability/adaptive-history',
    name: 'AdaptiveHistory',
    component: () => import('@/views/capability/AdaptiveHistory.vue'),
    meta: { title: '自适应历史', requiresAuth: true },
  },
  {
    path: '/capability/adaptive',
    name: 'AdaptiveDetail',
    component: () => import('@/views/capability/AdaptiveDetail.vue'),
    meta: { title: '自适应学习', requiresAuth: true },
  },
  {
    path: '/capability/diagnosis-report',
    name: 'DiagnosisReport',
    component: () => import('@/views/capability/DiagnosisReport.vue'),
    meta: { title: 'AI 诊断报告', requiresAuth: true },
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
    document.title = `${title} - 知途 Zhitu`
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