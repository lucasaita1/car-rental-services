import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { TOKEN_KEY } from '@/api/http'

function fakeToken(payload: Record<string, unknown>): string {
  const encode = (obj: object) => btoa(JSON.stringify(obj)).replace(/=+$/, '')
  return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode(payload)}.assinatura`
}

const inOneHour = () => Math.floor(Date.now() / 1000) + 3600

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('começa deslogado sem token salvo', () => {
    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.isAdmin).toBe(false)
  })

  it('lê id, nome e papel ADMIN do token', () => {
    const auth = useAuthStore()
    auth.setToken(fakeToken({ id: 7, name: 'Lucas', role: 'ADMIN', exp: inOneHour() }))

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.isAdmin).toBe(true)
    expect(auth.userId).toBe(7)
    expect(auth.userName).toBe('Lucas')
  })

  it('usuário comum não é admin', () => {
    const auth = useAuthStore()
    auth.setToken(fakeToken({ id: 3, role: 'USER', exp: inOneHour() }))

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.isAdmin).toBe(false)
  })

  it('token vencido não conta como sessão, nem de admin', () => {
    const auth = useAuthStore()
    auth.setToken(fakeToken({ id: 7, role: 'ADMIN', exp: Math.floor(Date.now() / 1000) - 10 }))

    expect(auth.isAuthenticated).toBe(false)
    expect(auth.isAdmin).toBe(false)
  })

  it('token malformado é ignorado', () => {
    const auth = useAuthStore()
    auth.setToken('nao-e-um-jwt')

    expect(auth.isAuthenticated).toBe(false)
  })

  it('persiste o token e o remove no logout', () => {
    const auth = useAuthStore()
    auth.setToken(fakeToken({ id: 1, role: 'USER', exp: inOneHour() }))
    expect(localStorage.getItem(TOKEN_KEY)).not.toBeNull()

    auth.logout()
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('recupera a sessão salva ao recarregar a página', () => {
    localStorage.setItem(TOKEN_KEY, fakeToken({ id: 9, role: 'USER', exp: inOneHour() }))
    setActivePinia(createPinia())

    expect(useAuthStore().userId).toBe(9)
  })
})
