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
