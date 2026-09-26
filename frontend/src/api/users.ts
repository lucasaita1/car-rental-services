import { userApi } from './http'
import type { AdminUserPayload, ProfilePayload, User, UserRole } from './types'

export async function listUsers(): Promise<User[]> {
  const { data } = await userApi.get<User[]>('/users')
  return data
}

export async function createUser(payload: AdminUserPayload): Promise<User> {
  const { data } = await userApi.post<User>('/users', payload)
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

export async function uploadMyCnhDocument(file: File): Promise<User> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await userApi.put<User>('/users/me/cnh-document', form)
  return data
}

export async function openCnhDocument(userId?: number): Promise<void> {
  const url = userId === undefined ? '/users/me/cnh-document' : `/users/${userId}/cnh-document`
  const { data } = await userApi.get<Blob>(url, { responseType: 'blob' })
  const objectUrl = URL.createObjectURL(data)
  window.open(objectUrl, '_blank', 'noopener')
  setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
}
