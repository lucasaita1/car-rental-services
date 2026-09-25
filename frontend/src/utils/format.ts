import type { CarStatus } from '@/api/types'

export function formatDate(value: string | null | undefined): string {
  if (!value) return '—'
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year!, month! - 1, day).toLocaleDateString('pt-BR')
}

export function todayIso(): string {
  return new Date().toLocaleDateString('sv-SE')
}

export const carStatusLabel: Record<CarStatus, string> = {
  AVAILABLE: 'Disponível',
  RENTED: 'Alugado',
  MAINTENANCE: 'Manutenção',
}

export const carStatusBadge: Record<CarStatus, string> = {
  AVAILABLE: 'badge-success',
  RENTED: 'badge-warning',
  MAINTENANCE: 'badge-error',
}

export function formatCountdown(ms: number): string {
  const total = Math.max(0, Math.ceil(ms / 1000))
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}
