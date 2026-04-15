import {
  createRouter,
  createWebHistory,
  type NavigationGuardNext,
  type RouteLocationNormalized,
  type RouteRecordRaw,
} from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    titleKey?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/login/RegisterView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { titleKey: 'nav.dashboard' },
      },
      {
        path: 'tasks',
        name: 'tasks',
        component: () => import('@/views/tasks/TaskListView.vue'),
        meta: { titleKey: 'nav.tasks' },
      },
      {
        path: 'wafer-analysis',
        name: 'wafer-analysis',
        component: () => import('@/views/wafer/WaferAnalysisWorkbenchView.vue'),
        meta: { titleKey: 'nav.wafer' },
      },
      {
        path: 'wafer-config',
        name: 'wafer-config',
        component: () => import('@/views/wafer/WaferConfigView.vue'),
        meta: { titleKey: 'nav.waferConfig' },
      },
      {
        path: 'multi-wafer-heatmap',
        name: 'multi-wafer-heatmap',
        component: () => import('@/views/wafer/MultiWaferHeatmapView.vue'),
        meta: { titleKey: 'nav.multiWafer' },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { titleKey: 'nav.settings' },
      },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

function authGuard(to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth !== false && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    next({ name: 'dashboard' })
    return
  }
  next()
}

router.beforeEach(authGuard)
