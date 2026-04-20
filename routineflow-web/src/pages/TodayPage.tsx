import { Skeleton } from '@/components/ui/skeleton'
import { AreaCard } from '@/components/shared/AreaCard'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import { useToday } from '@/hooks/useToday'
import { formatPercent } from '@/lib/utils'

// ── Today header ──────────────────────────────────────────────────────────────

function TodayHeader({
  overallRate,
  totalTasks,
  doneTasks,
}: {
  overallRate: number
  totalTasks: number
  doneTasks: number
}) {
  const today = new Date()
  const dateLabel = capitalize(
    new Intl.DateTimeFormat('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    }).format(today),
  )

  return (
    <header className="mb-6">
      <h1 className="text-3xl font-light text-[#f5f5f7] tracking-tight">{dateLabel}</h1>
      <p className="text-sm text-[#86868b] mt-1">
        {doneTasks} de {totalTasks} tarefas &bull; {formatPercent(overallRate)}
      </p>

      {/* Overall progress bar */}
      <div className="mt-3 h-[3px] rounded-full bg-[#1f1f1f] overflow-hidden">
        <div
          className="h-full rounded-full bg-[#0071e3] transition-all duration-700 ease-out"
          style={{ width: `${Math.round(overallRate * 100)}%` }}
        />
      </div>
    </header>
  )
}

// ── Skeleton loading ──────────────────────────────────────────────────────────

function TodaySkeleton() {
  return (
    <div className="space-y-3">
      {/* Skeleton header */}
      <div className="mb-6 space-y-2">
        <Skeleton className="h-9 w-64 bg-[#1f1f1f]" />
        <Skeleton className="h-4 w-40 bg-[#1f1f1f]" />
        <Skeleton className="h-[3px] w-full bg-[#1f1f1f] mt-3" />
      </div>

      {/* 3 skeleton cards */}
      {[104, 148, 120].map((height, i) => (
        <div
          key={i}
          className="rounded-xl bg-[#141414] border-l-4 border-[#1f1f1f] overflow-hidden"
        >
          <div className="flex items-center gap-3 px-4 py-3.5">
            <Skeleton className="h-6 w-6 rounded bg-[#1f1f1f]" />
            <Skeleton className="h-4 flex-1 bg-[#1f1f1f]" />
            <Skeleton className="h-3 w-8 bg-[#1f1f1f]" />
            <Skeleton className="h-1.5 w-16 rounded-full bg-[#1f1f1f]" />
          </div>
          <div className="px-4 pb-3 space-y-3" style={{ minHeight: height }}>
            {Array.from({ length: i + 2 }).map((_, j) => (
              <div key={j} className="flex items-center gap-3 py-2">
                <Skeleton className="h-5 w-5 rounded-md bg-[#1f1f1f]" />
                <div className="flex-1 space-y-1">
                  <Skeleton className="h-4 w-3/4 bg-[#1f1f1f]" />
                  <Skeleton className="h-3 w-1/2 bg-[#1f1f1f]" />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

// ── Empty state ───────────────────────────────────────────────────────────────

function EmptyState() {
  const dayName = capitalize(
    new Intl.DateTimeFormat('pt-BR', { weekday: 'long' }).format(new Date()),
  )
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <p className="text-4xl mb-4">🎉</p>
      <p className="text-[#f5f5f7] font-medium">Nada agendado para {dayName}!</p>
      <p className="text-[#86868b] text-sm mt-1">Aproveite o seu dia livre.</p>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function TodayPage() {
  const { enrichedAreas, overallRate, isLoading, handleTaskToggle, error } = useToday()

  if (isLoading) return <TodaySkeleton />

  // 404 = no active routine at all
  const isNoRoutine = (error as { response?: { status?: number } } | null)?.response?.status === 404
  if (isNoRoutine) return <EmptyRoutineState />

  // Routine exists but no tasks scheduled today
  if (enrichedAreas.length === 0) return <EmptyState />

  const allTasks = enrichedAreas.flatMap((a) => a.tasks)
  const doneTasks = allTasks.filter((t) => t.completed).length
  const totalTasks = allTasks.length

  return (
    <div>
      <TodayHeader overallRate={overallRate} totalTasks={totalTasks} doneTasks={doneTasks} />

      <div className="space-y-3">
        {enrichedAreas.map((area) => (
          <AreaCard key={area.id} area={area} onTaskToggle={handleTaskToggle} />
        ))}
      </div>
    </div>
  )
}

// ── Utils ─────────────────────────────────────────────────────────────────────

function capitalize(str: string): string {
  return str.charAt(0).toUpperCase() + str.slice(1)
}
