import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    guestOnly?: boolean
    fullWidth?: boolean
  }
}

export interface SessionState {
  isAuthenticated: boolean
  isAdmin: boolean
}

export function resolveAccess(
  to: RouteLocationNormalized,
  session: SessionState,
): true | RouteLocationRaw {
  if ((to.meta.requiresAuth || to.meta.requiresAdmin) && !session.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !session.isAdmin) {
    return { name: 'catalog' }
  }
  if (to.meta.guestOnly && session.isAuthenticated) {
    return { name: 'catalog' }
  }
  return true
}
