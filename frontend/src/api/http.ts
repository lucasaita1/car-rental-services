import axios, { AxiosError, type AxiosInstance } from 'axios'
import type { Router } from 'vue-router'

export const TOKEN_KEY = 'car-rental.token'

export const userApi = axios.create({
  baseURL: import.meta.env.VITE_USER_API_URL ?? 'http://localhost:8081',
})

export const carApi = axios.create({
  baseURL: import.meta.env.VITE_CAR_API_URL ?? 'http://localhost:8082',
})

function attachToken(instance: AxiosInstance) {
  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })
}

attachToken(userApi)
attachToken(carApi)

export function installSessionExpiryHandler(router: Router, onExpired: () => void) {
  const handler = (error: AxiosError) => {
    const hadToken = Boolean(error.config?.headers?.Authorization)
    if (error.response?.status === 401 && hadToken) {
      onExpired()
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return Promise.reject(error)
  }
  userApi.interceptors.response.use((r) => r, handler)
  carApi.interceptors.response.use((r) => r, handler)
}

export function errorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | string | undefined
    if (typeof data === 'string' && data) return data
    if (data && typeof data === 'object' && data.message) return data.message
    if (error.response?.status === 403) return 'Você não tem permissão para esta ação.'
    if (!error.response) return 'Não foi possível conectar ao servidor.'
  }
  return fallback
}
