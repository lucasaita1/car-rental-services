import { userApi } from './http'
import type { LoginResponse, RegisterPayload, User } from './types'

export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await userApi.post<LoginResponse>('/auth/users/login', { email, password })
  return data
}

export async function register(payload: RegisterPayload): Promise<User> {
  const { data } = await userApi.post<User>('/users/register', payload)
  return data
}

export async function logout(): Promise<void> {
  await userApi.post('/auth/logout')
}

export async function forgotPassword(email: string): Promise<string> {
  const { data } = await userApi.post<{ message: string }>('/auth/password/forgot', { email })
  return data.message
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await userApi.post('/auth/password/reset', { token, newPassword })
}
