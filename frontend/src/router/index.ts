import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { resolveAccess } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, _from, saved) {
    if (saved) return saved
    if (to.hash) return { el: to.hash, top: 72, behavior: 'smooth' }
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/LandingView.vue'),
      meta: { fullWidth: true },
    },
    { path: '/carros', name: 'catalog', component: () => import('@/views/CatalogView.vue') },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/cadastro',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/esqueci-senha',
      name: 'forgot-password',
      component: () => import('@/views/ForgotPasswordView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/redefinir-senha',
      name: 'reset-password',
      component: () => import('@/views/ResetPasswordView.vue'),
    },
    {
      path: '/perfil',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/minhas-locacoes',
      name: 'my-rentals',
      component: () => import('@/views/MyRentalsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      meta: { requiresAdmin: true },
      children: [
        { path: '', redirect: { name: 'admin-cars' } },
        {
          path: 'frota',
          name: 'admin-cars',
          component: () => import('@/views/admin/AdminCarsView.vue'),
        },
        {
          path: 'locacoes',
          name: 'admin-rentals',
          component: () => import('@/views/admin/AdminRentalsView.vue'),
        },
        {
          path: 'usuarios',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUsersView.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const result = resolveAccess(to, { isAuthenticated: auth.isAuthenticated, isAdmin: auth.isAdmin })

  if (result !== true && to.meta.requiresAdmin && auth.isAuthenticated) {
    useToastStore().warning('Área restrita a administradores.')
  }
  return result
})

export default router
