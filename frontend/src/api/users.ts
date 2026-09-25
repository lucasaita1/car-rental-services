import { userApi } from './http'
import type { User, UserRole } from './types'

export async function listUsers(): Promise<User[]> {
  const { data } = await userApi.get<User[]>('/users')
  return data
}

export async function changeRole(id: number, role: UserRole): Promise<User> {
  const { data } = await userApi.patch<User>(`/users/${id}/role`, { role })
  return data
}
