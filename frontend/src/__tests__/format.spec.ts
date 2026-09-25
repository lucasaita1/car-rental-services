import { describe, expect, it } from 'vitest'
import { formatCountdown, formatDate, todayIso } from '@/utils/format'

describe('format', () => {
  it('formata LocalDate do backend no padrão brasileiro sem erro de fuso', () => {
    expect(formatDate('2026-03-01')).toBe('01/03/2026')
  })

  it('mostra travessão para data vazia', () => {
    expect(formatDate(null)).toBe('—')
  })

  it('todayIso devolve AAAA-MM-DD', () => {
    expect(todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('formata a contagem regressiva da reserva em mm:ss', () => {
    expect(formatCountdown(600_000)).toBe('10:00')
    expect(formatCountdown(61_500)).toBe('01:02')
    expect(formatCountdown(-5)).toBe('00:00')
  })
})
