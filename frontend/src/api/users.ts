import { userApi } from './http'
import type { ProfilePayload, User, UserRole } from './types'

export async function listUsers(): Promise<User[]> {
  const { data } = await userApi.get<User[]>('/users')
  return data
}

export async function changeRole(id: number, role: UserRole): Promise<User> {
  const { data } = await userApi.patch<User>(`/users/${id}/role`, { role })
  return data
}

export async function getMe(): Promise<User> {
  const { data } = await userApi.get<User>('/users/me')
  return data
}

export async function updateMe(payload: ProfilePayload): Promise<User> {
  const { data } = await userApi.put<User>('/users/me', payload)
  return data
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await userApi.put('/users/me/password', { currentPassword, newPassword })
}

export async function uploadMyPhoto(file: File): Promise<User> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await userApi.post<User>('/users/me/photo', form)
  return data
}

export async function removeMyPhoto(): Promise<User> {
  const { data } = await userApi.delete<User>('/users/me/photo')
  return data
}
