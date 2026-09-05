// ============================================
// 路由配置 - 改造版
// 以学习工作台为核心
// ============================================

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 路由定义
export const routes = [
  // ===== 认证 =====
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', layout: 'auth' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Login.vue'),
    meta: { title: '注册', layout: 'auth' }
  },

  // ===== 核心工作台 =====
  {
    path: '/workbench',
    name: 'Workbench',
    component: () => import('@/views/Workbench.vue'),
    meta: { title: '学习工作台', requiresAuth: true },
    children: [
      { path: '', redirect: '/workbench/active' },
      { path: 'active', name: 'ActiveSession', component: () => import('@/views/workbench/SessionPanel.vue') },
      { path: 'diagnosis', name: 'DiagnosisPanel', component: () => import('@/views/workbench/SessionPanel.vue') },
      { path: 'planning', name: 'PlanningPanel', component: () => import('@/views/workbench/SessionPanel.vue') },
      { path: 'learning', name: 'LearningPanel', component: () => import('@/views/workbench/SessionPanel.vue') },
      { path: 'exercise', name: 'ExercisePanel', component: () => import('@/views/workbench/SessionPanel.vue') },
      { path: 'report', name: 'ReportPanel', component: () => import('@/views/workbench/SessionPanel.vue') },
    ]
  },

  // ===== 辅助页面 =====
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: { title: '知识库', requiresAuth: true }
  },
  {
    path: '/calendar',
    name: 'LearningCalendar',
    component: () => import('@/views/LearningCalendar.vue'),
    meta: { title: '学习日历', requiresAuth: true }
  },
  {
    path: '/achievements',
    name: 'Achievements',
    component: () => import('@/views/Achievements.vue'),
    meta: { title: '成就', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },

  // ===== 旧路由保留（专家模式/兼容）=====
  {
    path: '/agents',
    name: 'Agents',
    component: () => import('@/views/Agents.vue'),
    meta: { title: '智能体中心', requiresAuth: true }
  },
  {
    path: '/study-notes',
    name: 'StudyNotes',
    component: () => import('@/views/StudyNotes.vue'),
    meta: { title: '学习笔记', requiresAuth: true }
  },
  {
    path: '/code-analyze',
    name: 'CodeAnalyzer',
    component: () => import('@/views/CodeAnalyzer.vue'),
    meta: { title: '代码分析', requiresAuth: true }
  },

  // ===== 旧路由重定向（保持向后兼容）=====
  { path: '/', redirect: '/workbench' },
  { path: '/home', redirect: '/workbench' },
  { path: '/assessment', redirect: '/workbench/diagnosis' },
  { path: '/goal-setting', redirect: '/workbench/planning' },
  { path: '/exercise', redirect: '/workbench/exercise' },
  { path: '/statistics', redirect: '/workbench/report' },
  { path: '/chat', redirect: '/workbench' },
  { path: '/all-modules', redirect: '/workbench' },
  { path: '/report', redirect: '/workbench/report' },
  { path: '/tools', redirect: '/agents' },
  { path: '/learning-path', redirect: '/workbench' },

  // 能力诊断子页面重定向
  { path: '/capability/diagnosis', redirect: '/workbench/diagnosis' },
  { path: '/capability/weakness', redirect: '/workbench/diagnosis' },
  { path: '/capability/progress', redirect: '/workbench/report' },
  { path: '/capability/planning', redirect: '/workbench/planning' },
  { path: '/capability/adaptive-history', redirect: '/workbench' },
  { path: '/capability/adaptive', redirect: '/workbench' },
  { path: '/capability/diagnosis-report', redirect: '/workbench/report' },
  { path: '/capability/report', redirect: '/workbench/report' },
  { path: '/capability/*', redirect: '/workbench' },

  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/workbench'
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  const title = to.meta.title
  if (title) {
    document.title = `${title} - 知途 Zhitu`
  }

  const authStore = useAuthStore()

  // 已登录用户访问登录/注册页 -> 跳转工作台
  if ((to.path === '/login' || to.path === '/register') && authStore.isAuthenticated) {
    next('/workbench')
    return
  }

  // 未登录用户访问需认证页面 -> 跳转登录页
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  next()
})

export default router
