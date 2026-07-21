import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts'
import { ChevronRight, Download, Loader2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { toast } from 'sonner'
import { Skeleton } from '@/components/ui/skeleton'
import { DynamicIcon } from '@/components/shared/DynamicIcon'
import { useAnalytics } from '@/hooks/useAnalytics'
import { usePreferences } from '@/hooks/usePreferences'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import { exportApi } from '@/services/api'
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
      className="rounded-lg bg-[#141416] border border-line border-l-[3px] px-4 py-4 flex items-center gap-4
                 cursor-pointer transition-all duration-200 hover:bg-surface-3 focus-visible:outline-none
                 focus-visible:ring-2 focus-visible:ring-[#2F8BFF]"
      style={{ borderLeftColor: streak.color }}
      aria-label={`Ver detalhes da área ${streak.areaName}`}
    >
      <span className="shrink-0"><DynamicIcon name={streak.icon} color={streak.color} size={22} fallback="folder" /></span>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-[#8C8A88] truncate">{streak.areaName}</p>
        <p className="text-[10px] text-[#34343A] mt-0.5">
          {lastActive ? `Ativo em ${lastActive}` : 'Sem atividade'}
        </p>
      </div>
      <div className="text-right shrink-0">
        <p className="num text-2xl font-semibold leading-none" style={{ color: streak.color }}>
          {streak.currentStreak}
        </p>
        <p className="text-[10px] text-[#8C8A88] mt-0.5">dias</p>
      </div>
      <ChevronRight size={14} className="text-[#34343A] shrink-0" />
    </div>
  )
}

// ── Heatmap ───────────────────────────────────────────────────────────────────

function heatmapColor(day: FilledHeatmapDay): string {
  if (day.isFuture) return '#0F0F11'
  if (day.hasSkipDay) return '#f59e0b' // amber-500
  if (day.totalTasks === 0) return '#1C1C1F'
  if (day.completionRate === 0) return '#1C1C1F'
  if (day.completionRate < 0.34) return 'rgba(47,139,255,0.25)'
  if (day.completionRate < 0.67) return 'rgba(47,139,255,0.55)'
  return '#2F8BFF'
}

// GitHub-style layout: weeks run left→right (columns), days run top→bottom (rows).
// Cells use 1fr columns so the grid fills the full card width.
// Data from backend is week-major (Mon–Sun per week). CSS row-flow needs day-major
// (all Mondays, then all Tuesdays…), so we transpose before rendering.
const CELL_PX = 13
const GAP_PX = 3

function HeatmapGrid({ days, firstDayOfWeek }: { days: FilledHeatmapDay[], firstDayOfWeek: string }) {
  const isSundayFirst = firstDayOfWeek === 'SUNDAY'
  
  const DAY_LABEL_CONFIG = isSundayFirst ? [
    { label: 'Dom', row: 0 },
    { label: 'Ter', row: 2 },
    { label: 'Qui', row: 4 },
    { label: 'Sáb', row: 6 },
  ] : [
    { label: 'Seg', row: 0 },
    { label: 'Qua', row: 2 },
    { label: 'Sex', row: 4 },
    { label: 'Dom', row: 6 },
  ]
  const formatTip = (day: FilledHeatmapDay) => {
    const date = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' }).format(
      new Date(day.date + 'T00:00:00'),
    )
    if (day.isFuture) return `${date}: futuro`
    if (day.hasSkipDay) return `${date}: Dia Pulado`
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
    <div 
      className="flex items-start gap-2 w-full"
      role="img" 
      aria-label="Mapa de calor de atividades"
    >
      <span className="sr-only">
        Mapa de calor visual representando o nível de conclusão das tarefas diárias nas últimas semanas. 
        Dias mais escuros indicam menor atividade, enquanto dias azuis representam maior taxa de conclusão.
      </span>
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
                <span className="text-[9px] text-[#8C8A88] leading-none pr-1">
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
            aria-label={formatTip(day)}
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
    <div className="rounded-lg bg-[#1C1C1F] border border-[#26262A] px-3 py-2 text-xs">
      <p className="text-[#8C8A88]">{label}</p>
      <p className="num text-[#F4F2EF] font-medium mt-0.5">{payload[0].value}%</p>
    </div>
  )
}

function WeeklyLineChart({ data }: { data: WeekHistoryPoint[] }) {
  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center h-40 text-sm text-[#8C8A88]">
        Dados insuficientes para exibir o gráfico.
      </div>
    )
  }

  return (
    <div role="img" aria-label="Gráfico de evolução semanal">
      <span className="sr-only">Gráfico de linha exibindo o percentual de tarefas concluídas ao longo das semanas.</span>
      <ResponsiveContainer width="100%" height={180}>
        <LineChart data={data} margin={{ top: 4, right: 4, left: -24, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#26262A" vertical={false} />
        <XAxis
          dataKey="weekLabel"
          tick={{ fill: '#8C8A88', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
        />
        <YAxis
          domain={[0, 100]}
          tickFormatter={(v) => `${v}%`}
          tick={{ fill: '#8C8A88', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          ticks={[0, 25, 50, 75, 100]}
        />
        <Tooltip content={<CustomTooltip />} cursor={{ stroke: '#26262A', strokeWidth: 1 }} />
        <Line
          type="monotone"
          dataKey="rate"
          stroke="#2F8BFF"
          strokeWidth={2}
          dot={{ fill: '#2F8BFF', r: 3, strokeWidth: 0 }}
          activeDot={{ fill: '#2F8BFF', r: 5, strokeWidth: 0 }}
        />
      </LineChart>
    </ResponsiveContainer>
    </div>
  )
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function AnalyticsSkeleton() {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="space-y-2">
        <Skeleton className="h-8 w-40 bg-[#26262A]" />
      </div>

      {/* Streak cards */}
      <section>
        <Skeleton className="h-4 w-24 bg-[#26262A] mb-3" />
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="rounded-xl bg-[#141416] border-l-4 border-[#26262A] px-4 py-4 flex items-center gap-4">
              <Skeleton className="h-8 w-8 rounded bg-[#26262A]" />
              <div className="flex-1 space-y-1">
                <Skeleton className="h-3 w-24 bg-[#26262A]" />
                <Skeleton className="h-2 w-16 bg-[#26262A]" />
              </div>
              <Skeleton className="h-8 w-8 bg-[#26262A]" />
            </div>
          ))}
        </div>
      </section>

      {/* Heatmap */}
      <section>
        <Skeleton className="h-4 w-36 bg-[#26262A] mb-3" />
        <div className="rounded-xl bg-[#141416] p-4">
          <Skeleton className="h-28 w-full bg-[#26262A] rounded-lg" />
        </div>
      </section>

      {/* Chart */}
      <section>
        <Skeleton className="h-4 w-32 bg-[#26262A] mb-3" />
        <div className="rounded-xl bg-[#141416] p-4">
          <Skeleton className="h-44 w-full bg-[#26262A] rounded-lg" />
        </div>
      </section>
    </div>
  )
}

// ── Section wrapper ───────────────────────────────────────────────────────────

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h2 className="text-xs font-semibold text-[#8C8A88] uppercase tracking-widest mb-3">
        {title}
      </h2>
      {children}
    </section>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function AnalyticsPage() {
  const { preferences } = usePreferences()
  const firstDayOfWeek = preferences?.firstDayOfWeek ?? 'MONDAY'
  const { streaks, heatmapDays, weekHistoryData, isLoading, error } = useAnalytics(firstDayOfWeek)
  const [isExporting, setIsExporting] = useState(false)

  async function handleExport() {
    if (isExporting) return
    setIsExporting(true)
    try {
      await exportApi.exportCheckIns()
      toast.success('Exportação concluída!')
    } catch {
      toast.error('Erro ao exportar. Tente novamente.')
    } finally {
      setIsExporting(false)
    }
  }

  if (isLoading) return <AnalyticsSkeleton />

  const isNoRoutine = (error as { response?: { status?: number } } | null)?.response?.status === 404
  if (isNoRoutine) return <EmptyRoutineState />

  return (
    <div className="space-y-8">
      <header className="flex items-center justify-between gap-4">
        <h1 className="text-3xl font-light text-[#F4F2EF] tracking-tight">Analytics</h1>
        <button
          onClick={handleExport}
          disabled={isExporting}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#26262A] hover:bg-[#26262A]
                     text-[#8C8A88] hover:text-[#F4F2EF] text-sm transition-colors disabled:opacity-50"
        >
          {isExporting ? (
            <Loader2 size={14} className="animate-spin" />
          ) : (
            <Download size={14} />
          )}
          Exportar CSV
        </button>
      </header>

      {/* ── Streaks ─────────────────────────────────────────────────────── */}
      <Section title="Sequências">
        {streaks.length === 0 ? (
          <p className="text-sm text-[#8C8A88]">Nenhum streak registrado ainda.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
            {streaks.map((s) => (
              <StreakCard key={s.areaId} streak={s} />
            ))}
          </div>
        )}
      </Section>

      {/* ── Heatmap ─────────────────────────────────────────────────────── */}
      <Section title="Histórico de atividade">
        <div className="rounded-xl bg-[#141416] p-4 overflow-x-auto">
          {heatmapDays.length === 0 ? (
            <p className="text-sm text-[#8C8A88]">Sem dados de atividade.</p>
          ) : (
            <HeatmapGrid days={heatmapDays} firstDayOfWeek={firstDayOfWeek} />
          )}
        </div>

        {/* Legend */}
        <div className="flex items-center gap-2 mt-2 justify-end flex-wrap">
          <div className="flex items-center gap-1 mr-4">
            <div className="rounded-[2px]" style={{ width: 10, height: 10, backgroundColor: '#f59e0b' }} />
            <span className="text-[10px] text-[#8C8A88]">Skip Day</span>
          </div>
          <span className="text-[10px] text-[#8C8A88]">Menos</span>
          {['#1C1C1F', 'rgba(47,139,255,0.25)', 'rgba(47,139,255,0.55)', '#2F8BFF'].map((c, i) => (
            <div
              key={i}
              className="rounded-[2px]"
              style={{ width: 10, height: 10, backgroundColor: c }}
            />
          ))}
          <span className="text-[10px] text-[#8C8A88]">Mais</span>
        </div>
      </Section>

      {/* ── Weekly history chart ─────────────────────────────────────────── */}
      <Section title="Progresso semanal">
        <div className="rounded-xl bg-[#141416] px-4 pt-4 pb-2">
          <WeeklyLineChart data={weekHistoryData} />
        </div>
      </Section>
    </div>
  )
}
