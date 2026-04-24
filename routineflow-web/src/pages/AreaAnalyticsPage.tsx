import { useNavigate, useParams } from 'react-router-dom'
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  Cell,
  LabelList,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts'
import {
  ArrowLeft,
  BarChart2,
  CheckCircle2,
  TrendingUp,
  Trophy,
} from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { useAreaAnalytics } from '@/hooks/useAreaAnalytics'
import type { AreaAnalyticsResponse, DayOfWeekStat, WeeklyTrendPoint } from '@/types'

// ── Summary cards ─────────────────────────────────────────────────────────────

interface SummaryCardProps {
  label: string
  value: string | number
  icon: React.ReactNode
  color: string
}

function SummaryCard({ label, value, icon, color }: SummaryCardProps) {
  return (
    <div className="rounded-2xl bg-[#141414] p-5 flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <span className="text-xs text-[#86868b] uppercase tracking-wide font-medium">{label}</span>
        <span className="text-[#86868b]">{icon}</span>
      </div>
      <p className="text-3xl font-bold leading-none" style={{ color }}>
        {value}
      </p>
    </div>
  )
}

// ── Tooltip compartilhado ─────────────────────────────────────────────────────

interface TooltipProps {
  active?: boolean
  payload?: { value: number }[]
  label?: string
}

function DarkTooltip({ active, payload, label }: TooltipProps) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg bg-[#1c1c1e] border border-[#2a2a2a] px-3 py-2 text-xs">
      <p className="text-[#86868b]">{label}</p>
      <p className="text-[#f5f5f7] font-medium mt-0.5">{payload[0].value.toFixed(1)}%</p>
    </div>
  )
}

// ── Weekly trend chart ────────────────────────────────────────────────────────

function WeeklyTrendChart({ trend, color }: { trend: WeeklyTrendPoint[]; color: string }) {
  const allZero = trend.every((p) => p.completionRate === 0)

  if (allZero) {
    return (
      <div className="flex items-center justify-center h-40 text-sm text-[#86868b]">
        Nenhum dado ainda — comece marcando suas tarefas.
      </div>
    )
  }

  const data = trend.map((p) => ({ ...p, rate: p.completionRate }))

  return (
    <ResponsiveContainer width="100%" height={180}>
      <LineChart data={data} margin={{ top: 4, right: 4, left: -24, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#1f1f1f" vertical={false} />
        <XAxis
          dataKey="weekLabel"
          tick={{ fill: '#86868b', fontSize: 10 }}
          axisLine={false}
          tickLine={false}
        />
        <YAxis
          domain={[0, 100]}
          tickFormatter={(v) => `${v}%`}
          tick={{ fill: '#86868b', fontSize: 10 }}
          axisLine={false}
          tickLine={false}
          ticks={[0, 25, 50, 75, 100]}
        />
        <Tooltip content={<DarkTooltip />} cursor={{ stroke: '#2a2a2a', strokeWidth: 1 }} />
        <Line
          type="monotone"
          dataKey="rate"
          stroke={color}
          strokeWidth={2}
          dot={false}
          activeDot={{ fill: color, r: 4, strokeWidth: 0 }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

// ── Day of week bar chart ─────────────────────────────────────────────────────

interface BarTooltipProps {
  active?: boolean
  payload?: { value: number; payload: DayOfWeekStat }[]
  label?: string
}

function BarTooltipContent({ active, payload }: BarTooltipProps) {
  if (!active || !payload?.length) return null
  const stat = payload[0].payload
  return (
    <div className="rounded-lg bg-[#1c1c1e] border border-[#2a2a2a] px-3 py-2 text-xs">
      <p className="text-[#86868b]">{stat.dayLabel}</p>
      <p className="text-[#f5f5f7] font-medium mt-0.5">
        {stat.completedCount} feitos · {stat.completionRate.toFixed(1)}%
      </p>
    </div>
  )
}

function DayOfWeekBarChart({
  stats,
  color,
  bestDay,
}: {
  stats: DayOfWeekStat[]
  color: string
  bestDay: string | null
}) {
  if (stats.length === 0) {
    return (
      <div className="flex items-center justify-center h-32 text-sm text-[#86868b]">
        Sem dados de conclusão por dia.
      </div>
    )
  }

  const data = stats.map((s) => ({ ...s, rate: s.completionRate }))

  return (
    <ResponsiveContainer width="100%" height={Math.max(stats.length * 36, 120)}>
      <BarChart
        layout="vertical"
        data={data}
        margin={{ top: 0, right: 48, left: 0, bottom: 0 }}
      >
        <XAxis
          type="number"
          domain={[0, 100]}
          tickFormatter={(v) => `${v}%`}
          tick={{ fill: '#86868b', fontSize: 10 }}
          axisLine={false}
          tickLine={false}
        />
        <YAxis
          type="category"
          dataKey="dayLabel"
          tick={{ fill: '#86868b', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          width={56}
        />
        <Tooltip content={<BarTooltipContent />} cursor={{ fill: 'rgba(255,255,255,0.03)' }} />
        <Bar dataKey="rate" radius={[0, 4, 4, 0]}>
          <LabelList
            dataKey="rate"
            position="right"
            style={{ fill: '#86868b', fontSize: 10 }}
            formatter={(v: unknown) => `${Number(v).toFixed(0)}%`}
          />
          {data.map((entry) => (
            <Cell
              key={entry.dayOfWeek}
              fill={color}
              fillOpacity={entry.dayOfWeek === bestDay ? 1 : 0.35}
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}

// ── Page skeleton ─────────────────────────────────────────────────────────────

function AreaAnalyticsSkeleton() {
  return (
    <div className="space-y-8">
      <div className="flex items-center gap-3">
        <Skeleton className="h-8 w-8 rounded-full bg-[#1f1f1f]" />
        <Skeleton className="h-6 w-48 bg-[#1f1f1f]" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="rounded-2xl bg-[#141414] p-5 space-y-3">
            <Skeleton className="h-3 w-20 bg-[#1f1f1f]" />
            <Skeleton className="h-8 w-16 bg-[#1f1f1f]" />
          </div>
        ))}
      </div>
      <div className="rounded-xl bg-[#141414] p-4">
        <Skeleton className="h-4 w-36 bg-[#1f1f1f] mb-4" />
        <Skeleton className="h-44 w-full bg-[#1f1f1f] rounded-lg" />
      </div>
      <div className="rounded-xl bg-[#141414] p-4">
        <Skeleton className="h-4 w-40 bg-[#1f1f1f] mb-4" />
        <Skeleton className="h-32 w-full bg-[#1f1f1f] rounded-lg" />
      </div>
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
      <div className="rounded-xl bg-[#141414] p-4">{children}</div>
    </section>
  )
}

// ── Main content ──────────────────────────────────────────────────────────────

function AreaAnalyticsContent({ data }: { data: AreaAnalyticsResponse }) {
  const overallPct = `${data.overallCompletionRate.toFixed(1)}%`

  return (
    <div className="space-y-8">
      {/* Summary cards — 2×2 grid */}
      <div className="grid grid-cols-2 gap-3">
        <SummaryCard
          label="Total feito"
          value={data.totalCheckIns}
          icon={<CheckCircle2 size={16} />}
          color={data.color}
        />
        <SummaryCard
          label="Taxa geral"
          value={overallPct}
          icon={<BarChart2 size={16} />}
          color={data.color}
        />
        <SummaryCard
          label="Streak atual"
          value={`${data.currentStreak} dias`}
          icon={<TrendingUp size={16} />}
          color={data.color}
        />
        <SummaryCard
          label="Melhor streak"
          value={`${data.bestStreak} dias`}
          icon={<Trophy size={16} />}
          color={data.color}
        />
      </div>

      {/* Weekly trend */}
      <Section title="Progresso semanal — últimas 12 semanas">
        <WeeklyTrendChart trend={data.weeklyTrend} color={data.color} />
      </Section>

      {/* Day of week breakdown */}
      <Section title="Melhor dia da semana">
        {data.bestDayLabel && (
          <p className="text-xs text-[#86868b] mb-3">
            Melhor dia:{' '}
            <span className="font-medium" style={{ color: data.color }}>
              {data.bestDayLabel}
            </span>
          </p>
        )}
        <DayOfWeekBarChart
          stats={data.dayOfWeekStats}
          color={data.color}
          bestDay={data.bestDayOfWeek}
        />
      </Section>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function AreaAnalyticsPage() {
  const { areaId } = useParams<{ areaId: string }>()
  const navigate = useNavigate()

  const id = areaId ? parseInt(areaId, 10) : null
  const { data, isLoading, isError } = useAreaAnalytics(isNaN(id ?? NaN) ? null : id)

  // Invalid or missing areaId — redirect to analytics
  if (!id || isNaN(id)) {
    navigate('/analytics', { replace: true })
    return null
  }

  // 404 or network error — send back
  if (isError) {
    navigate('/analytics', { replace: true })
    return null
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <header>
        <button
          onClick={() => navigate('/analytics')}
          className="flex items-center gap-1.5 text-sm text-[#86868b] hover:text-[#f5f5f7] mb-4 transition-colors"
        >
          <ArrowLeft size={15} />
          Voltar para Analytics
        </button>

        {isLoading ? (
          <div className="flex items-center gap-3">
            <Skeleton className="h-9 w-9 rounded-full bg-[#1f1f1f]" />
            <Skeleton className="h-7 w-40 bg-[#1f1f1f]" />
          </div>
        ) : data ? (
          <div className="flex items-center gap-3">
            <div
              className="w-9 h-9 rounded-full flex items-center justify-center text-lg shrink-0"
              style={{ backgroundColor: data.color + '22', border: `2px solid ${data.color}` }}
            >
              {data.icon}
            </div>
            <h1 className="text-2xl font-semibold text-[#f5f5f7]">{data.areaName}</h1>
          </div>
        ) : null}
      </header>

      {/* Content */}
      {isLoading ? <AreaAnalyticsSkeleton /> : data ? <AreaAnalyticsContent data={data} /> : null}
    </div>
  )
}
