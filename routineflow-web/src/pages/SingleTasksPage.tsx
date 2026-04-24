import { useState } from 'react'
import { Plus } from 'lucide-react'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Skeleton } from '@/components/ui/skeleton'
import { SingleTaskItem } from '@/components/shared/SingleTaskItem'
import { CreateSingleTaskModal } from '@/components/shared/CreateSingleTaskModal'
import {
  useSingleTasksPending,
  useSingleTasksArchived,
  useCompleteSingleTask,
  useUncompleteSingleTask,
  useDeleteSingleTask,
} from '@/hooks/useSingleTasks'
import type { SingleTaskResponse } from '@/types'

// ── Helpers ───────────────────────────────────────────────────────────────────

function todayStr(): string {
  return new Date().toISOString().split('T')[0] as string
}

function groupPending(tasks: SingleTaskResponse[]): {
  overdue: SingleTaskResponse[]
  today: SingleTaskResponse[]
  noDate: SingleTaskResponse[]
  future: SingleTaskResponse[]
} {
  const today = todayStr()
  const overdue: SingleTaskResponse[] = []
  const todayTasks: SingleTaskResponse[] = []
  const noDate: SingleTaskResponse[] = []
  const future: SingleTaskResponse[] = []

  for (const t of tasks) {
    if (!t.dueDate) {
      noDate.push(t)
    } else if (t.dueDate < today) {
      overdue.push(t)
    } else if (t.dueDate === today) {
      todayTasks.push(t)
    } else {
      future.push(t)
    }
  }
  return { overdue, today: todayTasks, noDate, future }
}

// ── Group Section ─────────────────────────────────────────────────────────────

function TaskGroup({
  label,
  labelColor,
  tasks,
  onComplete,
  onDelete,
}: {
  label: string
  labelColor: string
  tasks: SingleTaskResponse[]
  onComplete: (id: number) => void
  onDelete: (id: number) => void
}) {
  if (tasks.length === 0) return null
  return (
    <div className="mb-4">
      <p className={`text-xs font-medium uppercase tracking-wide mb-2 ${labelColor}`}>{label}</p>
      <div className="rounded-xl bg-[#141414] px-4">
        {tasks.map((task, idx) => (
          <SingleTaskItem
            key={task.id}
            task={task}
            onComplete={onComplete}
            onDelete={onDelete}
            isLast={idx === tasks.length - 1}
          />
        ))}
      </div>
    </div>
  )
}

// ── Pending Tab ───────────────────────────────────────────────────────────────

function PendingTab() {
  const [modalOpen, setModalOpen] = useState(false)
  const { data: tasks = [], isLoading } = useSingleTasksPending()
  const completeMutation = useCompleteSingleTask()
  const deleteMutation = useDeleteSingleTask()

  const grouped = groupPending(tasks)
  const isEmpty = tasks.length === 0

  if (isLoading) {
    return (
      <div className="space-y-3 py-4">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-12 w-full rounded-xl bg-[#141414]" />
        ))}
      </div>
    )
  }

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button
          type="button"
          onClick={() => setModalOpen(true)}
          className="flex items-center gap-1.5 text-sm font-medium text-[#0071e3] hover:text-[#0077ed] transition-colors"
        >
          <Plus size={16} />
          Nova tarefa
        </button>
      </div>

      {isEmpty ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <p className="text-3xl mb-3">✅</p>
          <p className="text-[#f5f5f7] font-medium">Nenhuma tarefa pendente</p>
          <p className="text-[#86868b] text-sm mt-1">Que tal adicionar algo para fazer?</p>
        </div>
      ) : (
        <>
          <TaskGroup
            label="Atrasadas"
            labelColor="text-red-400"
            tasks={grouped.overdue}
            onComplete={(id) => completeMutation.mutate(id)}
            onDelete={(id) => deleteMutation.mutate(id)}
          />
          <TaskGroup
            label="Hoje"
            labelColor="text-[#0071e3]"
            tasks={grouped.today}
            onComplete={(id) => completeMutation.mutate(id)}
            onDelete={(id) => deleteMutation.mutate(id)}
          />
          <TaskGroup
            label="Sem prazo"
            labelColor="text-[#86868b]"
            tasks={grouped.noDate}
            onComplete={(id) => completeMutation.mutate(id)}
            onDelete={(id) => deleteMutation.mutate(id)}
          />
          <TaskGroup
            label="Futuras"
            labelColor="text-[#86868b]"
            tasks={grouped.future}
            onComplete={(id) => completeMutation.mutate(id)}
            onDelete={(id) => deleteMutation.mutate(id)}
          />
        </>
      )}

      <CreateSingleTaskModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </div>
  )
}

// ── Archived Tab ──────────────────────────────────────────────────────────────

function ArchivedTab() {
  const { data: tasks = [], isLoading } = useSingleTasksArchived()
  const uncompleteMutation = useUncompleteSingleTask()

  if (isLoading) {
    return (
      <div className="space-y-3 py-4">
        {[1, 2].map((i) => (
          <Skeleton key={i} className="h-12 w-full rounded-xl bg-[#141414]" />
        ))}
      </div>
    )
  }

  if (tasks.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <p className="text-3xl mb-3">📦</p>
        <p className="text-[#f5f5f7] font-medium">Nenhuma tarefa arquivada ainda</p>
        <p className="text-[#86868b] text-sm mt-1">Tarefas concluídas aparecem aqui.</p>
      </div>
    )
  }

  return (
    <div className="rounded-xl bg-[#141414] px-4">
      {tasks.map((task, idx) => (
        <SingleTaskItem
          key={task.id}
          task={task}
          onComplete={() => {}}
          onUncomplete={(id) => uncompleteMutation.mutate(id)}
          showUndoInstead
          isLast={idx === tasks.length - 1}
        />
      ))}
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function SingleTasksPage() {
  return (
    <div>
      <header className="mb-6">
        <h1 className="text-3xl font-light text-[#f5f5f7] tracking-tight">Tarefas</h1>
        <p className="text-sm text-[#86868b] mt-1">Tarefas pontuais, sem rotina.</p>
      </header>

      <Tabs defaultValue="pending">
        <TabsList className="bg-[#141414] border border-[#1f1f1f] mb-6 w-full">
          <TabsTrigger value="pending" className="flex-1 data-[state=active]:bg-[#1f1f1f] data-[state=active]:text-[#f5f5f7] text-[#86868b]">
            Pendentes
          </TabsTrigger>
          <TabsTrigger value="archived" className="flex-1 data-[state=active]:bg-[#1f1f1f] data-[state=active]:text-[#f5f5f7] text-[#86868b]">
            Arquivadas
          </TabsTrigger>
        </TabsList>

        <TabsContent value="pending">
          <PendingTab />
        </TabsContent>

        <TabsContent value="archived">
          <ArchivedTab />
        </TabsContent>
      </Tabs>
    </div>
  )
}
