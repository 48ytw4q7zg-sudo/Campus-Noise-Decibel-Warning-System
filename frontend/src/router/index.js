import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/DashboardPage.vue')
      },
      {
        path: 'noise-monitor',
        name: 'NoiseMonitor',
        component: () => import('@/views/NoiseMonitorPage.vue')
      },
      {
        path: 'alert-history',
        name: 'AlertHistory',
        component: () => import('@/views/AlertHistoryPage.vue')
      },
      {
        path: 'area-config',
        name: 'AreaConfig',
        component: () => import('@/views/AreaConfigPage.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'threshold-config',
        name: 'ThresholdConfig',
        component: () => import('@/views/ThresholdConfigPage.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/StatisticsPage.vue')
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: () => import('@/views/SystemSettingsPage.vue'),
        meta: { adminOnly: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 已登录用户访问 /login → 跳首页
  if (to.path === '/login' && token) {
    return next({ path: '/' })
  }

  // 需登录的页面但无 token → 跳登录
  if (to.meta.requiresAuth && !token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 管理员专属路由守卫
  if (to.meta.adminOnly) {
    const role = localStorage.getItem('role')
    if (role !== '管理员') {
      return next({ path: '/' })
    }
  }
  next()
})

export default router
