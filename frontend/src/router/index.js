import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '@/views/LoginPage.vue'
import AppLayout from '@/components/AppLayout.vue'
import DashboardPage from '@/views/DashboardPage.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: DashboardPage
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
        component: () => import('@/views/AreaConfigPage.vue')
      },
      {
        path: 'threshold-config',
        name: 'ThresholdConfig',
        component: () => import('@/views/ThresholdConfigPage.vue')
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/StatisticsPage.vue')
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: () => import('@/views/SystemSettingsPage.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  next()
})

export default router
