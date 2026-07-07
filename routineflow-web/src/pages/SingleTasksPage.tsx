import { useState } from 'react'
import { Plus, CheckSquare } from 'lucide-react'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { FilterPills } from '@/components/shared/FilterPills'
import { SingleTaskItem } from '@/components/shared/SingleTaskItem'
import {
  usePendingSingleTasks,
  useArchivedSingleTasks,
  useCreateSingleTask,
  useCompleteSingleTask,
  useUncompleteSingleTask,
  useDeleteSingleTask,
} from '@/hooks/useSingleTasks'
import type { SingleTaskResponse } from '@/types'
import { getLocalISODate } from '@/lib/utils'

// ── Deadline filter type ──────────────────────────────────────────────────────

type DeadlineFilter = 'OVERDUE' | 'TODAY' | 'FUTURE' | 'NO_DATE'

const DEADLINE_FILTER_OPTIONS: { value: DeadlineFilter; label: string }[] = [
  { value: 'OVERDUE', label: 'Atrasadas' },
  { value: 'TODAY',   label: 'Hoje'      },
  { value: 'FUTURE',  label: 'Futuras'   },
  { value: 'NO_DATE', label: 'Sem prazo' },
]

// ── Date helpers ──────────────────────────────────────────────────────────────

function todayIso(): string {
  return getLocalISODate()
}

function applyDeadlineFilter(
  tasks: SingleTaskResponse[],
  filter: DeadlineFilter | null,
): SingleTaskResponse[] {
  if (filter === null) return tasks
  const today = todayIso()
  switch (filter) {
    case 'OVERDUE': return tasks.filter((t) => t.isOverdue)
    case 'TODAY':   return tasks.filter((t) => t.dueDate === today && !t.isOverdue)
    case 'FUTURE':  return tasks.filter((t) => t.dueDate !== null && t.dueDate > today)
    case 'NO_DATE': return tasks.filter((t) => t.dueDate === null)
  }
}

// ── Create modal ──────────────────────────────────────────────────────────────

interface CreateModalProps {
  open:      boolean
  onClose:   () => void
  onSave:    (title: string, description: string | null, dueDate: string | null) => void
  isPending: boolean
}

function CreateSingleTaskModal({ open, onClose, onSave, isPending }: CreateModalProps) {
  const [title,       setTitle]       = useState('')
  const [description, setDescription] = useState('')
  const [dueDate,     setDueDate]     = useState('')

  function handleSave() {
    const trimmed = title.trim()
    if (!trimmed) return
    onSave(trimmed, description.trim() || null, dueDate || null)
    setTitle('')
    setDescription('')
    setDueDate('')
    onClose()
  }

  function handleClose() {
    setTitle('')
    setDescription('')
    setDueDate('')
    onClose()
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="bg-[#141416] border-[#26262A] text-[#F4F2EF] max-w-sm">
        <DialogHeader>
          <DialogTitle className="text-[#F4F2EF]">Nova tarefa</DialogTitle>
        </DialogHeader>

        <div className="space-y-3 py-1">
          <Input
            placeholder="Título da tarefa"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSave()}
            className="bg-[#08080A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#8C8A88] focus-visible:ring-[#2F8BFF]"
            autoFocus
          />
          <Input
            placeholder="Descrição (opcional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="bg-[#08080A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#8C8A88] focus-visible:ring-[#2F8BFF]"
          />
          <div>
            <label className="text-xs text-[#8C8A88] block mb-1">Prazo (opcional)</label>
            <Input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="bg-[#08080A] border-[#26262A] text-[#F4F2EF] focus-visible:ring-[#2F8BFF] [color-scheme:dark]"
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="ghost"
            onClick={handleClose}
            className="text-[#8C8A88] hover:text-[#F4F2EF]"
          >
            Cancelar
          </Button>
          <Button
            onClick={handleSave}
            disabled={!title.trim() || isPending}
            className="bg-[#2F8BFF] hover:bg-[#4F9DFF] text-white"
          >
            Criar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Skeletons ─────────────────────────────────────────────────────────────────

function TaskListSkeleton() {
  return (
    <div className="rounded-xl bg-[#141416] border border-[#26262A] divide-y divide-[#26262A] overflow-hidden">
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="flex items-center gap-3 px-4 py-3">
          <Skeleton className="h-5 w-5 rounded-full bg-[#26262A] shrink-0" />
          <div className="flex-1 space-y-1.5">
            <Skeleton className="h-4 w-3/4 bg-[#26262A]" />
            <Skeleton className="h-3 w-1/4 bg-[#26262A]" />
          </div>
        </div>
      ))}
    </div>
  )
}

// ── Empty states ──────────────────────────────────────────────────────────────

function EmptyPending() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <CheckSquare size={32} className="text-[#34343A] mb-3" />
      <p className="text-[#F4F2EF] text-sm font-medium">Nenhuma tarefa pendente</p>
      <p className="text-[#8C8A88] text-xs mt-1">Crie uma tarefa para começar</p>
    </div>
  )
}

function EmptyArchived() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <CheckSquare size={32} className="text-[#34343A] mb-3" />
      <p className="text-[#F4F2EF] text-sm font-medium">Nenhuma tarefa arquivada</p>
      <p className="text-[#8C8A88] text-xs mt-1">Tarefas concluídas aparecem aqui</p>
    </div>
  )
}

function EmptyFiltered({ onClear }: { onClear: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <p className="text-[#8C8A88] text-sm">Nenhuma tarefa com este filtro</p>
      <button
        type="button"
        onClick={onClear}
        className="mt-2 text-xs text-[#2F8BFF] hover:underline cursor-pointer"
      >
        Limpar filtro
      </button>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function SingleTasksPage() {
  const [showModal,      setShowModal]      = useState(false)
  const [deadlineFilter, setDeadlineFilter] = useState<DeadlineFilter | null>(null)

  const { data: pending,  isLoading: loadingPending  } = usePendingSingleTasks()
  const { data: archived, isLoading: loadingArchived } = useArchivedSingleTasks()

  const { mutate: create,    isPending: creating    } = useCreateSingleTask()
  const { mutate: complete                           } = useCompleteSingleTask()
  const { mutate: uncomplete                         } = useUncompleteSingleTask()
  const { mutate: remove                             } = useDeleteSingleTask()

  const filteredPending = applyDeadlineFilter(pending ?? [], deadlineFilter)

  function handleSave(title: string, description: string | null, dueDate: string | null) {
    create({ title, description, dueDate })
  }

  return (
    /* Vertical list page — narrower column than the global max-w (see TodayPage). */
    <div className="max-w-2xl lg:max-w-3xl mx-auto">
      {/* Page header */}
      <header className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-light text-[#F4F2EF] tracking-tight">Tarefas</h1>
          <p className="text-sm text-[#8C8A88] mt-1">
            {pending?.length ?? 0} pendente{(pending?.length ?? 0) !== 1 ? 's' : ''}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowModal(true)}
          className="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-[#2F8BFF] hover:bg-[#4F9DFF] text-white text-sm font-medium transition-colors cursor-pointer"
        >
          <Plus size={15} />
          Nova tarefa
        </button>
      </header>

      <Tabs defaultValue="pending">
        <TabsList className="bg-[#141416] border border-[#26262A] h-9 mb-4">
          <TabsTrigger
            value="pending"
            className="text-xs data-[state=active]:bg-[#26262A] data-[state=active]:text-[#F4F2EF] text-[#8C8A88]"
          >
            {/* Count intentionally omitted — the header subtitle already shows "X pendentes" */}
            Pendentes
          </TabsTrigger>
          <TabsTrigger
            value="archived"
            className="text-xs data-[state=active]:bg-[#26262A] data-[state=active]:text-[#F4F2EF] text-[#8C8A88]"
          >
            Arquivadas
          </TabsTrigger>
        </TabsList>

        {/* ── Pending tab ─────────────────────────────────────────────────────── */}
        <TabsContent value="pending">
          <div className="mb-4">
            <FilterPills
              options={DEADLINE_FILTER_OPTIONS}
              selected={deadlineFilter}
              onSelect={setDeadlineFilter}
            />
          </div>

          {loadingPending ? (
            <TaskListSkeleton />
          ) : (pending?.length ?? 0) === 0 ? (
            <EmptyPending />
          ) : filteredPending.length === 0 ? (
            <EmptyFiltered onClear={() => setDeadlineFilter(null)} />
          ) : (
            <div className="rounded-xl bg-[#141416] border border-[#26262A] divide-y divide-[#26262A] overflow-hidden px-3">
              {filteredPending.map((task) => (
                <SingleTaskItem
                  key={task.id}
                  task={task}
                  onComplete={complete}
                  onUncomplete={uncomplete}
                  onDelete={remove}
                />
              ))}
            </div>
          )}
        </TabsContent>

        {/* ── Archived tab ────────────────────────────────────────────────────── */}
        <TabsContent value="archived">
          {loadingArchived ? (
            <TaskListSkeleton />
          ) : (archived?.length ?? 0) === 0 ? (
            <EmptyArchived />
          ) : (
            <div className="rounded-xl bg-[#141416] border border-[#26262A] divide-y divide-[#26262A] overflow-hidden px-3">
              {archived!.map((task) => (
                <SingleTaskItem
                  key={task.id}
                  task={task}
                  onComplete={complete}
                  onUncomplete={uncomplete}
                  onDelete={remove}
                  archived
                />
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>

      <CreateSingleTaskModal
        open={showModal}
        onClose={() => setShowModal(false)}
        onSave={handleSave}
        isPending={creating}
      />
    </div>
  )
}
