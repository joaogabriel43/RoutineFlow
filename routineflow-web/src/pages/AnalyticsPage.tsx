import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts'
import { ChevronRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Skeleton } from '@/components/ui/skeleton'
import { useAnalytics } from '@/hooks/useAnalytics'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import type { StreakResponse } from '@/types'
import type { FilledHeatmapDay, WeekHistoryPoint } from '@/hooks/useAnalytics'

// ── Streak card ───────────────────────────────────────────────────────────────

function StreakCard({ streak }: { streak: StreakResponse }) {
  const navigate = useNavigate()

  const lastActive = streak.lastActiveDate
    ? new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' }).format(
        new Date(streak.lastActiveDate + 'T00:00:00'),
      )
    : null

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => navigate(`/analytics/area/${streak.areaId}`)}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/analytics/area/${streak.areaId}`)}
      className="rounded-xl bg-[#141414] border-l-4 px-4 py-4 flex items-center gap-4
                 cursor-pointer transition-opacity hover:opacity-80 focus-visible:outline-none
                 focus-visible:ring-2 focus-visible:ring-[#0071e3]"
      style={{ borderLeftColor: streak.color }}
    >
      <span className="text-2xl shrink-0">{streak.icon}</span>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-[#86868b] truncate">{streak.areaName}</p>
        <p className="text-[10px] text-[#3a3a3a] mt-0.5">
          {lastActive ? `Ativo em ${lastActive}` : 'Sem atividade'}
        </p>
      </div>
      <div className="text-right shrink-0">
        <p className="text-2xl font-semibold leading-none" style={{ color: streak.color }}>
          {streak.currentStreak}
        </p>
        <p className="text-[10px] text-[#86868b] mt-0.5">dias</p>
      </div>
      <ChevronRight size={14} className="text-[#3a3a3a] shrink-0" />
    </div>
  )
}

// ── Heatmap ───────────────────────────────────────────────────────────────────

function heatmapColor(day: FilledHeatmapDay): string {
  if (day.isFuture) return '#111111'
  if (day.totalTasks === 0) return '#1c1c1e'
  if (day.completionRate === 0) return '#1f2f1f'
  if (day.completionRate < 0.34) return 'rgba(0,113,227,0.25)'
  if (day.completionRate < 0.67) return 'rgba(0,113,227,0.55)'
  return '#0071e3'
}

// GitHub-style layout: weeks run left→right (columns), days run top→bottom (rows).
// Cells use 1fr columns so the grid fills the full card width.
// Data from backend is week-major (Mon–Sun per week). CSS row-flow needs day-major
// (all Mondays, then all Tuesdays…), so we transpose before rendering.
const CELL_PX = 13
const GAP_PX = 3

const DAY_LABEL_CONFIG: { label: string; row: number }[] = [
  { label: 'Seg', row: 0 },
  { label: 'Qua', row: 2 },
  { label: 'Sex', row: 4 },
  { label: 'Dom', row: 6 },
]

function HeatmapGrid({ days }: { days: FilledHeatmapDay[] }) {
  const formatTip = (day: FilledHeatmapDay) => {
    const date = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' }).format(
      new Date(day.date + 'T00:00:00'),
    )
    if (day.isFuture) return `${date}: futuro`
    if (day.totalTasks === 0) return `${date}: sem tarefas`
    return `${date}: ${day.completedTasks}/${day.totalTasks} (${Math.round(day.completionRate * 100)}%)`
  }

  // Transpose week-major → day-major so CSS row-flow fills correctly:
  // row 0 = all Mondays (wk0…wk12), row 1 = all Tuesdays, etc.
  // Math.ceil to include the last partial week (e.g. Mon–Thu when today is Thursday).
  const weeks = Math.ceil(days.length / 7) || 1
  const transposed: FilledHeatmapDay[] = []
  for (let d = 0; d < 7; d++) {
    for (let w = 0; w < weeks; w++) {
      const item = days[w * 7 + d]
      if (item) transposed.push(item)
    }
  }

  const rowTemplate = `repeat(7, ${CELL_PX}px)`
  const gap = `${GAP_PX}px`

  return (
    <div className="flex items-start gap-2 w-full">
      {/* Day labels — stacked vertically, aligned to grid rows */}
      <div
        className="grid shrink-0"
        style={{ gridTemplateRows: rowTemplate, gap }}
      >
        {Array.from({ length: 7 }, (_, row) => {
          const config = DAY_LABEL_CONFIG.find((c) => c.row === row)
          return (
            <div
              key={row}
              className="flex items-center justify-end"
              style={{ height: CELL_PX }}
            >
              {config && (
                <span className="text-[9px] text-[#86868b] leading-none pr-1">
                  {config.label}
                </span>
              )}
            </div>
          )
        })}
      </div>

      {/* Cell grid — 1fr columns fill available width, fixed row height */}
      <div
        className="grid flex-1 min-w-0"
        style={{
          gridTemplateColumns: `repeat(${weeks}, minmax(0, 1fr))`,
          gridTemplateRows: rowTemplate,
          gap,
        }}
      >
        {transposed.map((day, i) => (
          <div
            key={i}
            className="rounded-[2px] cursor-default transition-opacity hover:opacity-80"
            style={{
              height: CELL_PX,
              backgroundColor: heatmapColor(day),
            }}
            title={formatTip(day)}
          />
        ))}
      </div>
    </div>
  )
}

// ── Weekly chart ──────────────────────────────────────────────────────────────

interface CustomTooltipProps {
  active?: boolean
  payload?: { value: number }[]
  label?: string
}

function CustomTooltip({ active, payload, label }: CustomTooltipProps) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg bg-[#1c1c1e] border border-[#2a2a2a] px-3 py-2 text-xs">
      <p className="text-[#86868b]">{label}</p>
      <p className="text-[#f5f5f7] font-medium mt-0.5">{payload[0].value}%</p>
    </div>
  )
}

function WeeklyLineChart({ data }: { data: WeekHistoryPoint[] }) {
  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center h-40 text-sm text-[#86868b]">
        Dados insuficientes para exibir o gráfico.
      </div>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={180}>
      <LineChart data={data} margin={{ top: 4, right: 4, left: -24, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#1f1f1f" vertical={false} />
        <XAxis
          dataKey="weekLabel"
          tick={{ fill: '#86868b', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
        />
        <YAxis
          domain={[0, 100]}
          tickFormatter={(v) => `${v}%`}
          tick={{ fill: '#86868b', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          ticks={[0, 25, 50, 75, 100]}
        />
        <Tooltip content={<CustomTooltip />} cursor={{ stroke: '#2a2a2a', strokeWidth: 1 }} />
        <Line
          type="monotone"
          dataKey="rate"
          stroke="#0071e3"
          strokeWidth={2}
          dot={{ fill: '#0071e3', r: 3, strokeWidth: 0 }}
          activeDot={{ fill: '#0071e3', r: 5, strokeWidth: 0 }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function AnalyticsSkeleton() {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="space-y-2">
        <Skeleton className="h-8 w-40 bg-[#1f1f1f]" />
      </div>

      {/* Streak cards */}
      <section>
        <Skeleton className="h-4 w-24 bg-[#1f1f1f] mb-3" />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="rounded-xl bg-[#141414] border-l-4 border-[#1f1f1f] px-4 py-4 flex items-center gap-4">
              <Skeleton className="h-8 w-8 rounded bg-[#1f1f1f]" />
              <div className="flex-1 space-y-1">
                <Skeleton className="h-3 w-24 bg-[#1f1f1f]" />
                <Skeleton className="h-2 w-16 bg-[#1f1f1f]" />
              </div>
              <Skeleton className="h-8 w-8 bg-[#1f1f1f]" />
            </div>
          ))}
        </div>
      </section>

      {/* Heatmap */}
      <section>
        <Skeleton className="h-4 w-36 bg-[#1f1f1f] mb-3" />
        <div className="rounded-xl bg-[#141414] p-4">
          <Skeleton className="h-28 w-full bg-[#1f1f1f] rounded-lg" />
        </div>
      </section>

      {/* Chart */}
      <section>
        <Skeleton className="h-4 w-32 bg-[#1f1f1f] mb-3" />
        <div className="rounded-xl bg-[#141414] p-4">
          <Skeleton className="h-44 w-full bg-[#1f1f1f] rounded-lg" />
        </div>
      </section>
    </div>
  )
}

// ── Section wrapper ───────────────────────────────────────────────────────────

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h2 className="text-xs font-semibold text-[#86868b] uppercase tracking-widest mb-3">
        {title}
      </h2>
      {children}
    </section>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function AnalyticsPage() {
  const { streaks, heatmapDays, weekHistoryData, isLoading, error } = useAnalytics()

  if (isLoading) return <AnalyticsSkeleton />

  const isNoRoutine = (error as { response?: { status?: number } } | null)?.response?.status === 404
  if (isNoRoutine) return <EmptyRoutineState />

  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-3xl font-light text-[#f5f5f7] tracking-tight">Analytics</h1>
      </header>

      {/* ── Streaks ─────────────────────────────────────────────────────── */}
      <Section title="Sequências">
        {streaks.length === 0 ? (
          <p className="text-sm text-[#86868b]">Nenhum streak registrado ainda.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {streaks.map((s) => (
              <StreakCard key={s.areaId} streak={s} />
            ))}
          </div>
        )}
      </Section>

      {/* ── Heatmap ─────────────────────────────────────────────────────── */}
      <Section title="Histórico de atividade">
        <div className="rounded-xl bg-[#141414] p-4 overflow-x-auto">
          {heatmapDays.length === 0 ? (
            <p className="text-sm text-[#86868b]">Sem dados de atividade.</p>
          ) : (
            <HeatmapGrid days={heatmapDays} />
          )}
        </div>

        {/* Legend */}
        <div className="flex items-center gap-2 mt-2 justify-end">
          <span className="text-[10px] text-[#86868b]">Menos</span>
          {['#1c1c1e', 'rgba(0,113,227,0.25)', 'rgba(0,113,227,0.55)', '#0071e3'].map((c, i) => (
            <div
              key={i}
              className="rounded-[2px]"
              style={{ width: 10, height: 10, backgroundColor: c }}
            />
          ))}
          <span className="text-[10px] text-[#86868b]">Mais</span>
        </div>
      </Section>

      {/* ── Weekly history chart ─────────────────────────────────────────── */}
      <Section title="Progresso semanal">
        <div className="rounded-xl bg-[#141414] px-4 pt-4 pb-2">
          <WeeklyLineChart data={weekHistoryData} />
        </div>
      </Section>
    </div>
  )
}
