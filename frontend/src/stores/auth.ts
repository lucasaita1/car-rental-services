import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'
import * as authApi from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'
import type { RegisterPayload, TokenPayload } from '@/api/types'

function decode(token: string | null): TokenPayload | null {
  if (!token) return null
  try {
    return jwtDecode<TokenPayload>(token)
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))

  const payload = computed(() => decode(token.value))
  const isAuthenticated = computed(() => {
    const exp = payload.value?.exp
    return exp !== undefined && exp * 1000 > Date.now()
  })
  const isAdmin = computed(() => isAuthenticated.value && payload.value?.role === 'ADMIN')
  const userId = computed(() => payload.value?.id ?? null)
  const userName = computed(() => payload.value?.name ?? '')

  function setToken(value: string | null) {
    token.value = value
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  }

  async function login(email: string, password: string) {
    const response = await authApi.login(email, password)
    setToken(response.token)
  }

  async function register(data: RegisterPayload) {
    await authApi.register(data)
    await login(data.email, data.password)
  }

  function logout() {
    setToken(null)
  }

  return {
    token,
    payload,
    isAuthenticated,
    isAdmin,
    userId,
    userName,
    login,
    register,
    logout,
    setToken,
  }
})
