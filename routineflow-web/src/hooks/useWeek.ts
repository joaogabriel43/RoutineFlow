import { useQuery, useQueries } from '@tanstack/react-query'
import { routineApi, analyticsApi } from '@/services/api'

// ── Constants ─────────────────────────────────────────────────────────────────

export const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const

export const DAYS_OF_WEEK_SUNDAY = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
] as const

export type DayKey = (typeof DAYS_OF_WEEK)[number]

export const DAY_LABELS_PT: Record<DayKey, string> = {
  MONDAY: 'Seg',
  TUESDAY: 'Ter',
  WEDNESDAY: 'Qua',
  THURSDAY: 'Qui',
  FRIDAY: 'Sex',
  SATURDAY: 'Sáb',
  SUNDAY: 'Dom',
}

// ── Types ─────────────────────────────────────────────────────────────────────

export interface WeekCell {
  scheduled: boolean
  tasksCount: number
}

export interface WeekAreaRow {
  areaId: number
  areaName: string
  color: string
  icon: string
  weeklyRate: number
  completedTasks: number
  totalTasks: number
  days: Record<DayKey, WeekCell>
}

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useWeek(firstDayOfWeek: string = 'MONDAY') {
  const daysOfWeek = firstDayOfWeek === 'SUNDAY' ? DAYS_OF_WEEK_SUNDAY : DAYS_OF_WEEK
  // Separated from dayQueries so TypeScript infers the correct return type
  // (mixing heterogeneous queries in a single useQueries call produces a union type
  //  that prevents property access on either branch)
  const weeklyQuery = useQuery({
    queryKey: ['weekly-current'],
    queryFn: analyticsApi.getCurrentWeek,
    staleTime: 60_000,
  })

  const dayQueries = useQueries({
    queries: daysOfWeek.map((day) => ({
      queryKey: ['day-schedule', day] as const,
      queryFn: () => routineApi.getDay(day),
      staleTime: 300_000,
    })),
  })

  const isLoading = weeklyQuery.isLoading || dayQueries.some((q) => q.isLoading)
  const weeklyData = weeklyQuery.data   // WeeklyCompletionResponse | undefined
  const error = weeklyQuery.error

  const areaRows: WeekAreaRow[] = []

  if (weeklyData && dayQueries.every((q) => q.data !== undefined)) {
    const daySchedules = dayQueries.map((q) => q.data!)

    for (const area of weeklyData.areas) {
      const days = {} as Record<DayKey, WeekCell>

      for (let i = 0; i < daysOfWeek.length; i++) {
        const day = daysOfWeek[i]
        const areaOnDay = daySchedules[i].areas.find((a) => a.id === area.areaId)
        days[day] = {
          scheduled: !!areaOnDay,
          tasksCount: areaOnDay?.tasks.length ?? 0,
        }
      }

      areaRows.push({
        areaId: area.areaId,
        areaName: area.areaName,
        color: area.color,
        icon: area.icon,
        weeklyRate: area.completionRate,
        completedTasks: area.completedTasks,
        totalTasks: area.totalTasks,
        days,
      })
    }
  }

  return {
    areaRows,
    weekStart: weeklyData?.weekStart ?? null,
    weekEnd: weeklyData?.weekEnd ?? null,
    overallRate: weeklyData?.overallRate ?? 0,
    isLoading,
    error,
  }
}

// ── Utils ─────────────────────────────────────────────────────────────────────

/** Returns the DAYS_OF_WEEK index (0 = Mon, 6 = Sun) for today. */
export function getTodayDayIndex(firstDayOfWeek: string = 'MONDAY'): number {
  const today = new Date().getDay()
  return firstDayOfWeek === 'SUNDAY' ? today : (today + 6) % 7
}
