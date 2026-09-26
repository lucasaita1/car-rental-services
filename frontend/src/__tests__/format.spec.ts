import { describe, expect, it } from 'vitest'
import {
  daysUntil,
  estimateTotal,
  formatCurrency,
  firstName,
  formatCountdown,
  formatDate,
  greeting,
  rentalDays,
  todayIso,
} from '@/utils/format'

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

  it('daysUntil conta dias corridos entre datas, negativo quando já passou', () => {
    expect(daysUntil('2026-10-03', '2026-09-26')).toBe(7)
    expect(daysUntil('2026-09-26', '2026-09-26')).toBe(0)
    expect(daysUntil('2026-09-20', '2026-09-26')).toBe(-6)
    expect(daysUntil('2026-11-02', '2026-10-30')).toBe(3)
  })

  it('greeting muda conforme o horário', () => {
    expect(greeting(new Date(2026, 8, 26, 8))).toBe('Bom dia')
    expect(greeting(new Date(2026, 8, 26, 14))).toBe('Boa tarde')
    expect(greeting(new Date(2026, 8, 26, 21))).toBe('Boa noite')
    expect(greeting(new Date(2026, 8, 26, 2))).toBe('Boa noite')
  })

  it('firstName pega só o primeiro nome', () => {
    expect(firstName('  Lucas Aita Prates ')).toBe('Lucas')
    expect(firstName('GURIS PUCPR')).toBe('GURIS')
  })

  it('formatCurrency usa real brasileiro', () => {
    expect(formatCurrency(1234.5).replace(/\s/g, ' ')).toBe('R$ 1.234,50')
    expect(formatCurrency(null)).toBe('—')
  })

  it('rentalDays cobra no mínimo uma diária', () => {
    expect(rentalDays('2026-09-26', '2026-09-26')).toBe(1)
    expect(rentalDays('2026-09-26', '2026-09-30')).toBe(4)
  })

  it('estimateTotal multiplica a diária pelos dias e arredonda centavos', () => {
    expect(estimateTotal(149.9, '2026-09-26', '2026-09-29')).toBe(449.7)
    expect(estimateTotal(null, '2026-09-26', '2026-09-29')).toBeNull()
    expect(estimateTotal(100, '2026-09-26', '')).toBeNull()
  })
})
