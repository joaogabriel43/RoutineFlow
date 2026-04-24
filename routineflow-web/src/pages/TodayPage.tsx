import { useState } from 'react'
import { Plus } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { AreaCard } from '@/components/shared/AreaCard'
import { DateNavBar } from '@/components/shared/DateNavBar'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import { SingleTaskItem } from '@/components/shared/SingleTaskItem'
import { CreateSingleTaskModal } from '@/components/shared/CreateSingleTaskModal'
import { useDay } from '@/hooks/useDay'
import { useSingleTasksToday, useCompleteSingleTask, useDeleteSingleTask } from '@/hooks/useSingleTasks'
import { formatPercent } from '@/lib/utils'

// ── Helpers ───────────────────────────────────────────────────────────────────

function todayStr(): string {
  return new Date().toISOString().split('T')[0] as string
}

function capitalize(str: string): string {
  return str.charAt(0).toUpperCase() + str.slice(1)
}

/** Human-readable label for the selected date. */
function dateLabel(dateStr: string): string {
  const today = todayStr()
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  const yesterdayStr = yesterday.toISOString().split('T')[0]

  if (dateStr === today) {
    return capitalize(
      new Intl.DateTimeFormat('pt-BR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
      }).format(new Date()),
    )
  }
  if (dateStr === yesterdayStr) return 'Ontem'

  const d = new Date(dateStr + 'T12:00:00')
  return capitalize(
    new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }).format(d),
  )
}

// ── Today header ──────────────────────────────────────────────────────────────

function DayHeader({
  dateStr,
  overallRate,
  totalTasks,
  doneTasks,
  isFuture,
}: {
  dateStr: string
  overallRate: number
  totalTasks: number
  doneTasks: number
  isFuture: boolean
}) {
  const label = dateLabel(dateStr)

  return (
    <header className="mb-4">
      <h1 className="text-3xl font-light text-[#f5f5f7] tracking-tight">{label}</h1>

      {isFuture ? (
        <p className="text-sm text-[#86868b] mt-1">Visualização apenas — sem check-ins para dias futuros</p>
      ) : (
        <>
          <p className="text-sm text-[#86868b] mt-1">
            {doneTasks} de {totalTasks} tarefas &bull; {formatPercent(overallRate)}
          </p>
          <div className="mt-3 h-[3px] rounded-full bg-[#1f1f1f] overflow-hidden">
            <div
              className="h-full rounded-full bg-[#0071e3] transition-all duration-700 ease-out"
              style={{ width: `${Math.round(overallRate * 100)}%` }}
            />
          </div>
        </>
      )}
    </header>
  )
}

// ── Skeleton loading ──────────────────────────────────────────────────────────

function TodaySkeleton() {
  return (
    <div className="space-y-3">
      {/* Date nav skeleton */}
      <div className="flex gap-1.5 mb-5">
        {Array.from({ length: 7 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-11 rounded-xl bg-[#1f1f1f] shrink-0" />
        ))}
      </div>

      {/* Header skeleton */}
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

function EmptyDayState({ dateStr }: { dateStr: string }) {
  const today = todayStr()
  const isToday = dateStr === today

  const dayName = isToday
    ? capitalize(new Intl.DateTimeFormat('pt-BR', { weekday: 'long' }).format(new Date()))
    : capitalize(
        new Intl.DateTimeFormat('pt-BR', { weekday: 'long' }).format(
          new Date(dateStr + 'T12:00:00'),
        ),
      )

  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <p className="text-4xl mb-4">🎉</p>
      <p className="text-[#f5f5f7] font-medium">Nada agendado para {dayName}!</p>
      <p className="text-[#86868b] text-sm mt-1">
        {isToday ? 'Aproveite o seu dia livre.' : 'Sem tarefas agendadas neste dia.'}
      </p>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

// ── Single Tasks Section ──────────────────────────────────────────────────────

function SingleTasksSection() {
  const [modalOpen, setModalOpen] = useState(false)
  const { data: tasks = [], isLoading } = useSingleTasksToday()
  const completeMutation = useCompleteSingleTask()
  const deleteMutation = useDeleteSingleTask()

  if (isLoading) return null

  return (
    <section className="mt-6">
      {/* Section header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <h2 className="text-sm font-medium text-[#f5f5f7]">Para fazer</h2>
          {tasks.length > 0 && (
            <span className="text-xs bg-[#1f1f1f] text-[#86868b] px-2 py-0.5 rounded-full">
              {tasks.length}
            </span>
          )}
        </div>
        <button
          type="button"
          onClick={() => setModalOpen(true)}
          className="flex items-center gap-1 text-xs text-[#0071e3] hover:text-[#0077ed] transition-colors"
        >
          <Plus size={14} />
          Adicionar
        </button>
      </div>

      {/* Task list */}
      {tasks.length === 0 ? (
        <p className="text-xs text-[#3a3a3c] py-3">
          Nenhuma tarefa pendente — que tal adicionar algo?
        </p>
      ) : (
        <div className="rounded-xl bg-[#141414] px-4">
          {tasks.map((task, idx) => (
            <SingleTaskItem
              key={task.id}
              task={task}
              onComplete={(id) => completeMutation.mutate(id)}
              onDelete={(id) => deleteMutation.mutate(id)}
              isLast={idx === tasks.length - 1}
            />
          ))}
        </div>
      )}

      <CreateSingleTaskModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </section>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function TodayPage() {
  const [selectedDate, setSelectedDate] = useState<string>(todayStr)

  const { enrichedAreas, overallRate, isLoading, isFuture, handleTaskToggle, error } =
    useDay(selectedDate)

  if (isLoading) return <TodaySkeleton />

  // 404 = no active routine at all
  const isNoRoutine = (error as { response?: { status?: number } } | null)?.response?.status === 404
  if (isNoRoutine) return <EmptyRoutineState />

  const allTasks = enrichedAreas.flatMap((a) => a.tasks)
  const doneTasks = allTasks.filter((t) => t.completed).length
  const totalTasks = allTasks.length

  return (
    <div>
      <DateNavBar selectedDate={selectedDate} onSelect={setSelectedDate} />

      <DayHeader
        dateStr={selectedDate}
        overallRate={overallRate}
        totalTasks={totalTasks}
        doneTasks={doneTasks}
        isFuture={isFuture}
      />

      {enrichedAreas.length === 0 ? (
        <EmptyDayState dateStr={selectedDate} />
      ) : (
        <div className="space-y-3">
          {enrichedAreas.map((area) => (
            <AreaCard
              key={area.id}
              area={area}
              onTaskToggle={handleTaskToggle}
              disabled={isFuture}
            />
          ))}
        </div>
      )}

      {/* Single tasks — only visible on today's date */}
      {selectedDate === todayStr() && <SingleTasksSection />}
    </div>
  )
}
