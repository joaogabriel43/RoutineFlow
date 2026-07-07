import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '@/services/api'
import { monthBounds } from '@/lib/calendar'
import type { HeatmapDayResponse } from '@/types'

/**
 * Per-day completion data for one calendar month.
 * Reuses GET /analytics/heatmap?from&to (already supports arbitrary ranges)
 * and indexes the days by ISO date for O(1) cell lookup.
 */
export function useCalendar(year: number, month: number) {
  const { from, to } = monthBounds(year, month)

  return useQuery({
    queryKey: ['calendar', year, month],
    queryFn: () => analyticsApi.getHeatmap({ from, to }),
    staleTime: 60_000,
    select: (res): Map<string, HeatmapDayResponse> =>
      new Map(res.days.map((d) => [d.date, d])),
  })
}
