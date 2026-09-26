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

export function daysUntil(isoDate: string, today: string = todayIso()): number {
  const [y1, m1, d1] = isoDate.split('-').map(Number)
  const [y2, m2, d2] = today.split('-').map(Number)
  return Math.round((Date.UTC(y1!, m1! - 1, d1) - Date.UTC(y2!, m2! - 1, d2)) / 86_400_000)
}

export function greeting(date: Date = new Date()): string {
  const hour = date.getHours()
  if (hour >= 5 && hour < 12) return 'Bom dia'
  if (hour >= 12 && hour < 18) return 'Boa tarde'
  return 'Boa noite'
}

export function firstName(fullName: string): string {
  return fullName.trim().split(/\s+/)[0] ?? ''
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function formatCurrency(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : currency.format(value)
}

export function rentalDays(start: string, end: string): number {
  return Math.max(1, daysUntil(end, start))
}

export function estimateTotal(dailyRate: number | null, start: string, end: string): number | null {
  if (dailyRate === null || !end) return null
  return Math.round(dailyRate * rentalDays(start, end) * 100) / 100
}
