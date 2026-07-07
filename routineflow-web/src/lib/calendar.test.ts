import { describe, expect, it } from 'vitest'
import { buildMonthGrid, monthBounds, monthLabel, shiftMonth } from './calendar'

describe('buildMonthGrid', () => {
  it('starts weeks on Monday — June 2026 (1st is a Monday) has no leading padding', () => {
    const grid = buildMonthGrid(2026, 6)
    expect(grid[0]![0]).toBe('2026-06-01')
    // 30 days from a Monday → 5 weeks, last one padded
    expect(grid).toHaveLength(5)
    expect(grid[4]![1]).toBe('2026-06-30') // Tue of last week
    expect(grid[4]![2]).toBeNull()
  })

  it('pads leading days — February 2024 (leap, 1st is a Thursday)', () => {
    const grid = buildMonthGrid(2024, 2)
    expect(grid[0]!.slice(0, 3)).toEqual([null, null, null]) // Mon–Wed padding
    expect(grid[0]![3]).toBe('2024-02-01')
    expect(grid.flat()).toContain('2024-02-29') // leap day present
  })

  it('every week has exactly 7 cells and all month days are present', () => {
    const grid = buildMonthGrid(2026, 7)
    for (const week of grid) expect(week).toHaveLength(7)
    const days = grid.flat().filter((d): d is string => d !== null)
    expect(days).toHaveLength(31)
    expect(days[0]).toBe('2026-07-01')
    expect(days[30]).toBe('2026-07-31')
  })
})

describe('monthBounds', () => {
  it('returns first and last ISO dates of the month', () => {
    expect(monthBounds(2026, 7)).toEqual({ from: '2026-07-01', to: '2026-07-31' })
    expect(monthBounds(2024, 2)).toEqual({ from: '2024-02-01', to: '2024-02-29' })
  })
})

describe('shiftMonth', () => {
  it('rolls over year boundaries in both directions', () => {
    expect(shiftMonth(2026, 1, -1)).toEqual({ year: 2025, month: 12 })
    expect(shiftMonth(2026, 12, 1)).toEqual({ year: 2027, month: 1 })
    expect(shiftMonth(2026, 7, -1)).toEqual({ year: 2026, month: 6 })
  })
})

describe('monthLabel', () => {
  it('returns the capitalized pt-BR label', () => {
    expect(monthLabel(2026, 7)).toBe('Julho de 2026')
  })
})
