import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts'
import { Skeleton } from '@/components/ui/skeleton'
import { useAnalytics } from '@/hooks/useAnalytics'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import type { StreakResponse } from '@/types'
import type { FilledHeatmapDay, WeekHistoryPoint } from '@/hooks/useAnalytics'

// ── Streak card ───────────────────────────────────────────────────────────────

function StreakCard({ streak }: { streak: StreakResponse }) {
  const lastActive = streak.lastActiveDate
    ? new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' }).format(
        new Date(streak.lastActiveDate + 'T00:00:00'),
      )
    : null

  return (
    <div
      className="rounded-xl bg-[#141414] border-l-4 px-4 py-4 flex items-center gap-4"
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
    </div>
  )
}

// ── Heatmap ───────────────────────────────────────────────────────────────────

const DAY_LABELS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

function heatmapColor(day: FilledHeatmapDay): string {
  if (day.isFuture) return '#111111'
  if (day.totalTasks === 0) return '#1c1c1e'
  if (day.completionRate === 0) return '#1f2f1f'
  if (day.completionRate < 0.34) return 'rgba(0,113,227,0.25)'
  if (day.completionRate < 0.67) return 'rgba(0,113,227,0.55)'
  return '#0071e3'
}

const CELL_PX = 10
const GAP_PX = 2

function HeatmapGrid({ days }: { days: FilledHeatmapDay[] }) {
  const formatTip = (day: FilledHeatmapDay) => {
    const date = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' }).format(
      new Date(day.date + 'T00:00:00'),
    )
    if (day.isFuture) return `${date}: futuro`
    if (day.totalTasks === 0) return `${date}: sem tarefas`
    return `${date}: ${day.completedTasks}/${day.totalTasks} (${Math.round(day.completionRate * 100)}%)`
  }

  const colTemplate = `repeat(7, ${CELL_PX}px)`
  const gridGap = `${GAP_PX}px`

  return (
    <div style={{ display: 'inline-block' }}>
      {/* Day labels — aligned to fixed cell columns */}
      <div
        className="grid mb-1"
        style={{ gridTemplateColumns: colTemplate, gap: gridGap }}
      >
        {DAY_LABELS.map((d) => (
          <div
            key={d}
            className="text-[9px] text-[#86868b] text-center leading-none"
            style={{ width: CELL_PX }}
          >
            {d}
          </div>
        ))}
      </div>

      {/* Cells — fixed 10×10px per cell */}
      <div
        className="grid"
        style={{ gridTemplateColumns: colTemplate, gap: gridGap }}
      >
        {days.map((day, i) => (
          <div
            key={i}
            className="rounded-[2px] cursor-default transition-opacity hover:opacity-80"
            style={{
              width: CELL_PX,
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
        <div className="rounded-xl bg-[#141414] p-4 flex flex-col items-start">
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
