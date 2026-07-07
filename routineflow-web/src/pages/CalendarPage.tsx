import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import { useCalendar } from '@/hooks/useCalendar'
import { buildMonthGrid, monthLabel, shiftMonth } from '@/lib/calendar'
import { getLocalISODate } from '@/lib/utils'
import { cn } from '@/lib/utils'
import type { HeatmapDayResponse } from '@/types'

// ── Cell coloring — same ramp as the Analytics heatmap ───────────────────────

function cellBackground(day: HeatmapDayResponse | undefined, isFuture: boolean): string {
  if (isFuture) return '#0F0F11'
  if (!day || day.totalTasks === 0 || day.completionRate === 0) return '#1C1C1F'
  if (day.completionRate < 0.34) return 'rgba(47,139,255,0.25)'
  if (day.completionRate < 0.67) return 'rgba(47,139,255,0.55)'
  return '#2F8BFF'
}

function cellTextClass(day: HeatmapDayResponse | undefined, isFuture: boolean): string {
  if (isFuture) return 'text-[#5C5B59]'
  if (day && day.completionRate >= 0.67) return 'text-white'
  return 'text-[#B8B6B3]'
}

// ── Day cell ──────────────────────────────────────────────────────────────────

interface DayCellProps {
  date: string
  day: HeatmapDayResponse | undefined
  today: string
  onOpen: (date: string) => void
}

function DayCell({ date, day, today, onOpen }: DayCellProps) {
  const isToday = date === today
  const isFuture = date > today
  const dayNumber = parseInt(date.slice(8), 10)

  const tooltip = isFuture
    ? `${dayNumber}: futuro`
    : day && day.totalTasks > 0
      ? `${dayNumber}: ${day.completedTasks}/${day.totalTasks} (${Math.round(day.completionRate * 100)}%)${day.hasSkipDay ? ' · dia pulado' : ''}`
      : `${dayNumber}: sem tarefas`

  return (
    <button
      type="button"
      onClick={() => onOpen(date)}
      title={tooltip}
      aria-label={`Abrir dia ${date}`}
      className={cn(
        'aspect-square rounded-md flex items-center justify-center relative',
        'transition-all duration-150 hover:ring-1 hover:ring-[#4F9DFF] cursor-pointer',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-[#2F8BFF]',
        isToday && 'ring-2 ring-[#2F8BFF]',
      )}
      style={{ backgroundColor: cellBackground(day, isFuture) }}
    >
      <span className={cn('num text-[13px] leading-none', cellTextClass(day, isFuture))}>
        {dayNumber}
      </span>
      {/* Skip day marker */}
      {day?.hasSkipDay && !isFuture && (
        <span className="absolute bottom-1 h-1 w-1 rounded-full bg-[#FFB340]" aria-hidden />
      )}
    </button>
  )
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function CalendarSkeleton() {
  return (
    <div className="grid grid-cols-7 gap-1.5">
      {Array.from({ length: 35 }).map((_, i) => (
        <Skeleton key={i} className="aspect-square rounded-md bg-[#26262A]" />
      ))}
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

const WEEKDAYS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

export function CalendarPage() {
  const navigate = useNavigate()
  const today = getLocalISODate()
  const [view, setView] = useState(() => ({
    year: parseInt(today.slice(0, 4), 10),
    month: parseInt(today.slice(5, 7), 10),
  }))

  const { data: dayMap, isLoading, error } = useCalendar(view.year, view.month)

  const isNoRoutine =
    (error as { response?: { status?: number } } | null)?.response?.status === 404
  if (isNoRoutine) return <EmptyRoutineState />

  const grid = buildMonthGrid(view.year, view.month)
  const isCurrentMonth =
    view.year === parseInt(today.slice(0, 4), 10) &&
    view.month === parseInt(today.slice(5, 7), 10)

  // Month summary — only past/today days that had scheduled tasks
  const monthDays = dayMap
    ? [...dayMap.values()].filter((d) => d.date <= today && d.totalTasks > 0)
    : []
  const activeDays = monthDays.filter((d) => d.completedTasks > 0).length
  const avgRate =
    monthDays.length > 0
      ? Math.round((monthDays.reduce((s, d) => s + d.completionRate, 0) / monthDays.length) * 100)
      : 0

  function openDay(date: string) {
    // TodayPage reads ?date= — check-ins on past days, view-only on future days
    navigate(`/?date=${date}`)
  }

  return (
    /* Calendar is a compact 7-col grid — same narrow column as the list pages */
    <div className="max-w-2xl lg:max-w-3xl mx-auto">
      {/* Header */}
      <header className="mb-6">
        <h1 className="text-3xl font-light text-[#F4F2EF] tracking-tight">Calendário</h1>
        {monthDays.length > 0 ? (
          <p className="text-sm text-[#8C8A88] mt-1">
            <span className="num">{activeDays}</span> dia{activeDays !== 1 ? 's' : ''} ativo
            {activeDays !== 1 ? 's' : ''} &bull; média <span className="num">{avgRate}%</span>
          </p>
        ) : (
          <p className="text-sm text-[#8C8A88] mt-1">
            Toque em um dia para ver e editar os check-ins.
          </p>
        )}
      </header>

      {/* Month navigation */}
      <div className="flex items-center justify-between mb-4">
        <button
          type="button"
          onClick={() => setView((v) => shiftMonth(v.year, v.month, -1))}
          aria-label="Mês anterior"
          className="p-2 rounded-lg text-[#8C8A88] hover:text-[#F4F2EF] hover:bg-[#1C1C1F] transition-colors"
        >
          <ChevronLeft size={18} />
        </button>
        <span className="text-[15px] font-medium text-[#F4F2EF] tracking-[-0.006em]">
          {monthLabel(view.year, view.month)}
        </span>
        <button
          type="button"
          onClick={() => setView((v) => shiftMonth(v.year, v.month, 1))}
          disabled={isCurrentMonth}
          aria-label="Próximo mês"
          className="p-2 rounded-lg text-[#8C8A88] hover:text-[#F4F2EF] hover:bg-[#1C1C1F] transition-colors disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent"
        >
          <ChevronRight size={18} />
        </button>
      </div>

      {/* Weekday header */}
      <div className="grid grid-cols-7 gap-1.5 mb-1.5">
        {WEEKDAYS.map((d) => (
          <span
            key={d}
            className="text-center text-[10px] font-semibold text-[#5C5B59] uppercase tracking-widest"
          >
            {d}
          </span>
        ))}
      </div>

      {/* Month grid */}
      {isLoading ? (
        <CalendarSkeleton />
      ) : (
        <div className="grid grid-cols-7 gap-1.5">
          {grid.flat().map((date, i) =>
            date === null ? (
              <span key={`pad-${i}`} aria-hidden />
            ) : (
              <DayCell
                key={date}
                date={date}
                day={dayMap?.get(date)}
                today={today}
                onOpen={openDay}
              />
            ),
          )}
        </div>
      )}

      {/* Legend */}
      <div className="flex items-center gap-2 mt-4 justify-end">
        <span className="text-[10px] text-[#8C8A88]">Menos</span>
        {['#1C1C1F', 'rgba(47,139,255,0.25)', 'rgba(47,139,255,0.55)', '#2F8BFF'].map((c, i) => (
          <div key={i} className="rounded-[2px]" style={{ width: 10, height: 10, backgroundColor: c }} />
        ))}
        <span className="text-[10px] text-[#8C8A88]">Mais</span>
        <span className="mx-1 text-[#26262A]">·</span>
        <span className="h-1.5 w-1.5 rounded-full bg-[#FFB340]" aria-hidden />
        <span className="text-[10px] text-[#8C8A88]">Dia pulado</span>
      </div>
    </div>
  )
}
