import { describe, expect, it } from 'vitest'
import type { RouteLocationNormalized, RouteMeta } from 'vue-router'
import { resolveAccess } from '@/router/guard'

function route(meta: RouteMeta, fullPath = '/destino'): RouteLocationNormalized {
  return { meta, fullPath } as RouteLocationNormalized
}

const anonymous = { isAuthenticated: false, isAdmin: false }
const user = { isAuthenticated: true, isAdmin: false }
const admin = { isAuthenticated: true, isAdmin: true }

describe('resolveAccess', () => {
  it('libera rotas públicas para qualquer um', () => {
    expect(resolveAccess(route({}), anonymous)).toBe(true)
  })

  it('manda anônimo para o login guardando o destino', () => {
    expect(resolveAccess(route({ requiresAuth: true }, '/minhas-locacoes'), anonymous)).toEqual({
      name: 'login',
      query: { redirect: '/minhas-locacoes' },
    })
  })

  it('manda anônimo para o login também em rota de admin', () => {
    expect(resolveAccess(route({ requiresAdmin: true }), anonymous)).toMatchObject({
      name: 'login',
    })
  })

  it('libera rota autenticada para usuário logado', () => {
    expect(resolveAccess(route({ requiresAuth: true }), user)).toBe(true)
  })

  it('bloqueia usuário comum em rota de admin', () => {
    expect(resolveAccess(route({ requiresAdmin: true }), user)).toEqual({ name: 'home' })
  })

  it('libera rota de admin para admin', () => {
    expect(resolveAccess(route({ requiresAdmin: true }), admin)).toBe(true)
  })

  it('tira quem já está logado das telas de login e cadastro', () => {
    expect(resolveAccess(route({ guestOnly: true }), user)).toEqual({ name: 'home' })
    expect(resolveAccess(route({ guestOnly: true }), anonymous)).toBe(true)
  })
})
