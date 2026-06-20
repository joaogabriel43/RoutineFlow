import { useState } from 'react'
import { Plus, Bell, X } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { AreaCard } from '@/components/shared/AreaCard'
import { DateNavBar } from '@/components/shared/DateNavBar'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import { SingleTaskItem } from '@/components/shared/SingleTaskItem'
import { CreateSingleTaskModal } from '@/components/shared/CreateSingleTaskModal'
import { useDay } from '@/hooks/useDay'
import { useSingleTasksToday, useCompleteSingleTask, useDeleteSingleTask } from '@/hooks/useSingleTasks'
import { usePushNotifications } from '@/hooks/usePushNotifications'
import { formatPercent, getLocalISODate } from '@/lib/utils'

// ── Helpers ───────────────────────────────────────────────────────────────────

function todayStr(): string {
  return getLocalISODate()
}

function capitalize(str: string): string {
  return str.charAt(0).toUpperCase() + str.slice(1)
}

/** Human-readable label for the selected date. */
function dateLabel(dateStr: string): string {
  const today = todayStr()
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  const yesterdayStr = getLocalISODate(yesterday)

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
      <h1 className="text-3xl font-light text-[#F4F2EF] tracking-tight">{label}</h1>

      {isFuture ? (
        <p className="text-sm text-[#8C8A88] mt-1">Visualização apenas — sem check-ins para dias futuros</p>
      ) : (
        <>
          <p className="text-sm text-[#8C8A88] mt-1">
            <span className="num">{doneTasks}</span> de <span className="num">{totalTasks}</span> tarefas &bull; <span className="num">{formatPercent(overallRate)}</span>
          </p>
          <div className="mt-3 h-[3px] rounded-full bg-[#26262A] overflow-hidden">
            <div
              className="h-full rounded-full bg-[#2F8BFF] transition-all duration-700 ease-out"
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
    <div className="space-y-3 max-w-2xl lg:max-w-3xl mx-auto">
      {/* Date nav skeleton */}
      <div className="flex gap-1.5 mb-5">
        {Array.from({ length: 7 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-11 rounded-xl bg-[#26262A] shrink-0" />
        ))}
      </div>

      {/* Header skeleton */}
      <div className="mb-6 space-y-2">
        <Skeleton className="h-9 w-64 bg-[#26262A]" />
        <Skeleton className="h-4 w-40 bg-[#26262A]" />
        <Skeleton className="h-[3px] w-full bg-[#26262A] mt-3" />
      </div>

      {/* 3 skeleton cards */}
      {[104, 148, 120].map((height, i) => (
        <div
          key={i}
          className="rounded-xl bg-[#141416] border-l-4 border-[#26262A] overflow-hidden"
        >
          <div className="flex items-center gap-3 px-4 py-3.5">
            <Skeleton className="h-6 w-6 rounded bg-[#26262A]" />
            <Skeleton className="h-4 flex-1 bg-[#26262A]" />
            <Skeleton className="h-3 w-8 bg-[#26262A]" />
            <Skeleton className="h-1.5 w-16 rounded-full bg-[#26262A]" />
          </div>
          <div className="px-4 pb-3 space-y-3" style={{ minHeight: height }}>
            {Array.from({ length: i + 2 }).map((_, j) => (
              <div key={j} className="flex items-center gap-3 py-2">
                <Skeleton className="h-5 w-5 rounded-md bg-[#26262A]" />
                <div className="flex-1 space-y-1">
                  <Skeleton className="h-4 w-3/4 bg-[#26262A]" />
                  <Skeleton className="h-3 w-1/2 bg-[#26262A]" />
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
      <p className="text-[#F4F2EF] font-medium">Nada agendado para {dayName}!</p>
      <p className="text-[#8C8A88] text-sm mt-1">
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
          <h2 className="text-sm font-medium text-[#F4F2EF]">Para fazer</h2>
          {tasks.length > 0 && (
            <span className="text-xs bg-[#26262A] text-[#8C8A88] px-2 py-0.5 rounded-full">
              {tasks.length}
            </span>
          )}
        </div>
        <button
          type="button"
          onClick={() => setModalOpen(true)}
          className="flex items-center gap-1 text-xs text-[#2F8BFF] hover:text-[#4F9DFF] transition-colors"
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
        <div className="rounded-xl bg-[#141416] px-4">
          {tasks.map((task) => (
            <SingleTaskItem
              key={task.id}
              task={task}
              onComplete={(id) => completeMutation.mutate(id)}
              onUncomplete={() => {}}
              onDelete={(id) => deleteMutation.mutate(id)}
            />
          ))}
        </div>
      )}

      <CreateSingleTaskModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </section>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

// ── Push notification banner ────────────────────────────────────────────────────

function PushBanner() {
  const { isSupported, permission, subscribe, loading } = usePushNotifications()
  const [dismissed, setDismissed] = useState(
    () => localStorage.getItem('rf_push_dismissed') === 'true',
  )

  // Don't show if not supported, already granted, already denied, or dismissed
  if (!isSupported || permission !== 'default' || dismissed) return null

  const handleActivate = async () => {
    await subscribe()
  }

  const handleDismiss = () => {
    localStorage.setItem('rf_push_dismissed', 'true')
    setDismissed(true)
  }

  return (
    <div className="mb-4 rounded-xl bg-[#1C1C1F] border border-[#1C1C1F] px-4 py-3 flex items-center gap-3">
      <Bell size={18} className="text-[#2F8BFF] shrink-0" />
      <p className="text-sm text-[#B8B6B3] flex-1">
        Ativar lembretes para receber notificações nos horários configurados
      </p>
      <button
        type="button"
        onClick={handleActivate}
        disabled={loading}
        className="text-xs font-medium text-[#2F8BFF] hover:text-[#4F9DFF] transition-colors disabled:opacity-50 shrink-0"
      >
        {loading ? 'Ativando...' : 'Ativar'}
      </button>
      <button
        type="button"
        onClick={handleDismiss}
        className="text-[#3a3a3c] hover:text-[#8C8A88] transition-colors shrink-0"
        aria-label="Dispensar"
      >
        <X size={14} />
      </button>
    </div>
  )
}

export function TodayPage() {
  const [selectedDate, setSelectedDate] = useState<string>(todayStr)
  const [activeTimerTaskId, setActiveTimerTaskId] = useState<number | null>(null)

  const handleToggleTimer = (taskId: number) => {
    setActiveTimerTaskId((prev) => (prev === taskId ? null : taskId))
  }

  const {
    enrichedAreas,
    overallRate,
    isLoading,
    isFuture,
    handleTaskToggle,
    updateNotes,
    incrementTaskProgress,
    resetTaskProgress,
    refetchProgress,
    error,
  } = useDay(selectedDate)

  if (isLoading) return <TodaySkeleton />

  // 404 = no active routine at all
  const isNoRoutine = (error as { response?: { status?: number } } | null)?.response?.status === 404
  if (isNoRoutine) return <EmptyRoutineState />

  const allTasks = enrichedAreas.flatMap((a) => a.tasks)
  const doneTasks = allTasks.filter((t) => t.completed).length
  const totalTasks = allTasks.length

  return (
    /* TodayPage is a vertical list of cards, not a dense grid — it does not need
       the wide global max-w (xl:max-w-6xl) applied in AppLayout for Manage/Analytics/
       Semana. A narrower column keeps area cards compact instead of stretching
       sparse content (e.g. a single-task area) across ~1100px. */
    <div className="max-w-2xl lg:max-w-3xl mx-auto">
      <DateNavBar selectedDate={selectedDate} onSelect={setSelectedDate} />

      {/* Push notification permission banner — only on today */}
      {selectedDate === todayStr() && <PushBanner />}

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
              activeTimerTaskId={activeTimerTaskId}
              onToggleTimer={handleToggleTimer}
              onUpdateNotes={updateNotes}
              onIncrementProgress={incrementTaskProgress}
              onResetProgress={resetTaskProgress}
              selectedDate={selectedDate}
              onRefetchProgress={refetchProgress}
            />
          ))}
        </div>
      )}

      {/* Single tasks — only visible on today's date */}
      {selectedDate === todayStr() && <SingleTasksSection />}
    </div>
  )
}
