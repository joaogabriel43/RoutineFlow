import { useQueries } from '@tanstack/react-query'
import { analyticsApi } from '@/services/api'
import type { HeatmapDayResponse } from '@/types'

// ── Types ─────────────────────────────────────────────────────────────────────

export interface WeekHistoryPoint {
  weekLabel: string
  rate: number
}

export interface FilledHeatmapDay extends HeatmapDayResponse {
  isFuture: boolean
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Builds the heatmap date range: last Monday 12 weeks ago → today */
function buildHeatmapRange(): { from: string; to: string } {
  const today = new Date()
  const todayIndex = (today.getDay() + 6) % 7 // Mon=0, Sun=6

  const thisMonday = new Date(today)
  thisMonday.setDate(today.getDate() - todayIndex)

  const from = new Date(thisMonday)
  from.setDate(thisMonday.getDate() - 12 * 7)

  return {
    from: from.toISOString().split('T')[0],
    to: today.toISOString().split('T')[0],
  }
}

/**
 * Fills missing dates in the heatmap response with zero-data entries,
 * ensuring one entry per day from `from` to `to`.
 */
function fillHeatmapDays(
  days: HeatmapDayResponse[],
  from: string,
  to: string,
): FilledHeatmapDay[] {
  const dayMap = new Map<string, HeatmapDayResponse>()
  for (const d of days) dayMap.set(d.date, d)

  const result: FilledHeatmapDay[] = []
  const cursor = new Date(from + 'T00:00:00')
  const end = new Date(to + 'T00:00:00')
  const todayStr = new Date().toISOString().split('T')[0]

  while (cursor <= end) {
    const dateStr = cursor.toISOString().split('T')[0]
    const existing = dayMap.get(dateStr)
    result.push(
      existing
        ? { ...existing, isFuture: false }
        : {
            date: dateStr,
            completedTasks: 0,
            totalTasks: 0,
            completionRate: 0,
            isFuture: dateStr > todayStr,
          },
    )
    cursor.setDate(cursor.getDate() + 1)
  }

  return result
}

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useAnalytics() {
  const { from, to } = buildHeatmapRange()

  const [streaksQuery, heatmapQuery, historyQuery] = useQueries({
    queries: [
      {
        queryKey: ['analytics-streaks'],
        queryFn: analyticsApi.getStreaks,
        staleTime: 60_000,
      },
      {
        queryKey: ['analytics-heatmap', from, to],
        queryFn: () => analyticsApi.getHeatmap({ from, to }),
        staleTime: 60_000,
      },
      {
        queryKey: ['analytics-history', 8],
        queryFn: () => analyticsApi.getWeeklyHistory(8),
        staleTime: 60_000,
      },
    ],
  })

  const isLoading = streaksQuery.isLoading || heatmapQuery.isLoading || historyQuery.isLoading
  const error = streaksQuery.error ?? heatmapQuery.error ?? historyQuery.error

  // Fill heatmap gaps and expose full day range
  const heatmapDays: FilledHeatmapDay[] = heatmapQuery.data
    ? fillHeatmapDays(heatmapQuery.data.days, from, to)
    : []

  // Transform weekly history → Recharts-ready data (oldest → newest)
  const weekHistoryData: WeekHistoryPoint[] = (historyQuery.data?.weeks ?? []).map((week, idx) => ({
    weekLabel: `S${idx + 1}`,
    rate: Math.round(week.overallRate * 100),
  }))

  return {
    streaks: streaksQuery.data?.streaks ?? [],
    heatmapDays,
    heatmapFrom: from,
    weekHistoryData,
    isLoading,
    error,
  }
}
