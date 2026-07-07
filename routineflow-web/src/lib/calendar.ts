/**
 * Pure month-grid math for the CalendarPage (Sprint 28).
 *
 * All functions work with plain year/month/day integers and ISO strings —
 * no Date→toISOString conversions of "now" (see CLAUDE.md timezone lesson).
 * Weeks start on Monday, matching the heatmap and WeekPage convention.
 */

/** ISO date for a given calendar day, e.g. iso(2026, 7, 3) → "2026-07-03". */
function iso(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

/**
 * Builds the month grid as rows of 7 cells (Mon…Sun).
 * Cells outside the month are null (leading/trailing padding).
 * `month` is 1-based (1 = January).
 */
export function buildMonthGrid(year: number, month: number): (string | null)[][] {
  // Date.UTC keeps this pure calendar math — immune to the local timezone.
  const firstDow = new Date(Date.UTC(year, month - 1, 1)).getUTCDay() // 0=Sun…6=Sat
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate()
  const leadPad = (firstDow + 6) % 7 // Monday-first: Mon=0 … Sun=6

  const cells: (string | null)[] = []
  for (let i = 0; i < leadPad; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push(iso(year, month, d))
  while (cells.length % 7 !== 0) cells.push(null)

  const weeks: (string | null)[][] = []
  for (let i = 0; i < cells.length; i += 7) weeks.push(cells.slice(i, i + 7))
  return weeks
}

/** First/last ISO dates of the month — the from/to range for the heatmap API. */
export function monthBounds(year: number, month: number): { from: string; to: string } {
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate()
  return { from: iso(year, month, 1), to: iso(year, month, daysInMonth) }
}

/** "julho de 2026" → "Julho de 2026" (pt-BR, capitalized). */
export function monthLabel(year: number, month: number): string {
  // timeZone: 'UTC' must match the Date.UTC construction — without it the local
  // offset (BRT = UTC−3) shifts July 1st 00:00 UTC back into June 30th and the
  // header shows the previous month. Caught by calendar.test.ts.
  const label = new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, 1)))
  return label.charAt(0).toUpperCase() + label.slice(1)
}

/** Previous / next month, rolling over the year. `month` is 1-based. */
export function shiftMonth(
  year: number,
  month: number,
  delta: -1 | 1,
): { year: number; month: number } {
  const m = month + delta
  if (m < 1) return { year: year - 1, month: 12 }
  if (m > 12) return { year: year + 1, month: 1 }
  return { year, month: m }
}
