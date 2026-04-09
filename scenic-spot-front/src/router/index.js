import { createRouter, createWebHistory } from 'vue-router'
import { hasToken } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/',
      component: () => import('../views/admin/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: () => import('../views/admin/DashboardView.vue') },
        { path: 'tickets', component: () => import('../views/admin/TicketView.vue') },
        { path: 'orders', component: () => import('../views/admin/OrderView.vue') },
        { path: 'verify', component: () => import('../views/admin/VerifyView.vue') },
        { path: 'aftersale', component: () => import('../views/admin/AftersaleView.vue') },
        { path: 'video-jobs', component: () => import('../views/admin/VideoJobView.vue') },
        { path: 'reports', component: () => import('../views/admin/ReportView.vue') },
        { path: 'scenic', component: () => import('../views/admin/ScenicManageView.vue') },
        { path: 'users', component: () => import('../views/admin/UserManageView.vue') },
        { path: 'config', component: () => import('../views/admin/SystemConfigView.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    return true
  }
  if (!hasToken()) {
    return '/login'
  }
  return true
})

export default router
