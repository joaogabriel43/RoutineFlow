import { Skeleton } from '@/components/ui/skeleton'
import { useWeek, DAYS_OF_WEEK, DAY_LABELS_PT, getTodayDayIndex } from '@/hooks/useWeek'
import type { WeekAreaRow, DayKey } from '@/hooks/useWeek'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import { formatPercent } from '@/lib/utils'

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatWeekRange(start: string | null, end: string | null): string {
  if (!start || !end) return ''
  const fmt = (d: string) =>
    new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' }).format(
      new Date(d + 'T00:00:00'),
    )
  return `${fmt(start)} – ${fmt(end)}`
}

// ── Cell ──────────────────────────────────────────────────────────────────────

function DayCell({ row, day }: { row: WeekAreaRow; day: DayKey }) {
  const cell = row.days[day]

  if (!cell.scheduled) {
    return (
      <div className="flex items-center justify-center h-9">
        <span className="text-[#3a3a3a] text-lg">·</span>
      </div>
    )
  }

  // Opacity reflects area's weekly completion rate
  const opacity = Math.max(0.2, row.weeklyRate)

  return (
    <div className="flex items-center justify-center h-9">
      <div
        className="w-5 h-5 rounded-full transition-all duration-300"
        style={{ backgroundColor: row.color, opacity }}
        title={`${cell.tasksCount} tarefa${cell.tasksCount !== 1 ? 's' : ''}`}
      />
    </div>
  )
}

// ── Area row ──────────────────────────────────────────────────────────────────

function AreaRow({
  row,
  todayIndex,
  isLast,
}: {
  row: WeekAreaRow
  todayIndex: number
  isLast: boolean
}) {
  return (
    <div className={`flex items-stretch ${!isLast ? 'border-b border-[#1f1f1f]' : ''}`}>
      {/* Area label — sticky left */}
      <div className="w-40 shrink-0 flex items-center gap-2 px-4 py-1 sticky left-0 bg-[#141414] z-10">
        <span className="text-base shrink-0">{row.icon}</span>
        <div className="min-w-0">
          <p className="text-xs font-medium text-[#f5f5f7] truncate">{row.areaName}</p>
          <p className="text-[10px] text-[#86868b]">
            {row.completedTasks}/{row.totalTasks} · {formatPercent(row.weeklyRate)}
          </p>
        </div>
      </div>

      {/* Day cells */}
      {DAYS_OF_WEEK.map((day, idx) => (
        <div
          key={day}
          className="w-12 shrink-0 flex items-center justify-center"
          style={idx === todayIndex ? { backgroundColor: 'rgba(0,113,227,0.06)' } : undefined}
        >
          <DayCell row={row} day={day} />
        </div>
      ))}
    </div>
  )
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function WeekSkeleton() {
  return (
    <div className="space-y-4">
      {/* Header skeleton */}
      <div className="space-y-2 mb-6">
        <Skeleton className="h-8 w-48 bg-[#1f1f1f]" />
        <Skeleton className="h-4 w-32 bg-[#1f1f1f]" />
        <Skeleton className="h-[3px] w-full bg-[#1f1f1f] mt-3" />
      </div>

      {/* Grid skeleton */}
      <div className="rounded-xl bg-[#141414] overflow-hidden">
        {/* Header row */}
        <div className="flex border-b border-[#1f1f1f]">
          <div className="w-40 shrink-0 px-4 py-3">
            <Skeleton className="h-3 w-16 bg-[#1f1f1f]" />
          </div>
          {Array.from({ length: 7 }).map((_, i) => (
            <div key={i} className="w-12 shrink-0 flex items-center justify-center py-3">
              <Skeleton className="h-3 w-6 bg-[#1f1f1f]" />
            </div>
          ))}
        </div>
        {/* Area rows */}
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex items-center border-b border-[#1f1f1f] last:border-b-0">
            <div className="w-40 shrink-0 flex items-center gap-2 px-4 py-3">
              <Skeleton className="h-5 w-5 rounded bg-[#1f1f1f]" />
              <Skeleton className="h-3 flex-1 bg-[#1f1f1f]" />
            </div>
            {Array.from({ length: 7 }).map((_, j) => (
              <div key={j} className="w-12 shrink-0 flex items-center justify-center py-3">
                {j !== 5 && j !== 6 && (
                  <Skeleton className="h-4 w-4 rounded-full bg-[#1f1f1f]" />
                )}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}

// ── Empty state ───────────────────────────────────────────────────────────────

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <p className="text-4xl mb-4">📅</p>
      <p className="text-[#f5f5f7] font-medium">Nenhuma rotina ativa</p>
      <p className="text-[#86868b] text-sm mt-1">Importe sua rotina para ver a semana.</p>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function WeekPage() {
  const { areaRows, weekStart, weekEnd, overallRate, isLoading, error } = useWeek()
  const todayIndex = getTodayDayIndex()

  if (isLoading) return <WeekSkeleton />

  const isNoRoutine = (error as { response?: { status?: number } } | null)?.response?.status === 404
  if (isNoRoutine) return <EmptyRoutineState />

  if (areaRows.length === 0) return <EmptyState />

  return (
    <div>
      {/* Header */}
      <header className="mb-6">
        <h1 className="text-3xl font-light text-[#f5f5f7] tracking-tight">Semana</h1>
        <p className="text-sm text-[#86868b] mt-1">
          {formatWeekRange(weekStart, weekEnd)} &bull; {formatPercent(overallRate)} geral
        </p>
        <div className="mt-3 h-[3px] rounded-full bg-[#1f1f1f] overflow-hidden">
          <div
            className="h-full rounded-full bg-[#0071e3] transition-all duration-700 ease-out"
            style={{ width: `${Math.round(overallRate * 100)}%` }}
          />
        </div>
      </header>

      {/* Grid — horizontally scrollable on mobile */}
      <div className="overflow-x-auto -mx-4 px-4">
        <div className="rounded-xl bg-[#141414] overflow-hidden min-w-[448px]">
          {/* Day header row */}
          <div className="flex border-b border-[#1f1f1f]">
            <div className="w-40 shrink-0 px-4 py-3">
              <span className="text-xs text-[#86868b] font-medium">Área</span>
            </div>
            {DAYS_OF_WEEK.map((day, idx) => (
              <div
                key={day}
                className="w-12 shrink-0 flex items-center justify-center py-3"
                style={idx === todayIndex ? { backgroundColor: 'rgba(0,113,227,0.06)' } : undefined}
              >
                <span
                  className="text-xs font-medium"
                  style={{ color: idx === todayIndex ? '#0071e3' : '#86868b' }}
                >
                  {DAY_LABELS_PT[day]}
                </span>
              </div>
            ))}
          </div>

          {/* Area rows */}
          {areaRows.map((row, idx) => (
            <AreaRow
              key={row.areaId}
              row={row}
              todayIndex={todayIndex}
              isLast={idx === areaRows.length - 1}
            />
          ))}
        </div>
      </div>

      {/* Legend */}
      <p className="text-[10px] text-[#86868b] mt-3 text-right">
        Opacidade dos círculos reflete a taxa de conclusão semanal da área
      </p>
    </div>
  )
}
