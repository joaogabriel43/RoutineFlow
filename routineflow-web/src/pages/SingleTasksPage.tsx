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
  return new Date().toISOString().split('T')[0]
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
  open: boolean
  onClose: () => void
  onSave: (title: string, dueDate: string | null) => void
  isPending: boolean
}

function CreateSingleTaskModal({ open, onClose, onSave, isPending }: CreateModalProps) {
  const [title, setTitle]     = useState('')
  const [dueDate, setDueDate] = useState('')

  function handleSave() {
    const trimmed = title.trim()
    if (!trimmed) return
    onSave(trimmed, dueDate || null)
    setTitle('')
    setDueDate('')
    onClose()
  }

  function handleClose() {
    setTitle('')
    setDueDate('')
    onClose()
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="bg-[#141414] border-[#1f1f1f] text-[#f5f5f7] max-w-sm">
        <DialogHeader>
          <DialogTitle className="text-[#f5f5f7]">Nova tarefa</DialogTitle>
        </DialogHeader>

        <div className="space-y-3 py-1">
          <Input
            placeholder="Título da tarefa"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSave()}
            className="bg-[#0a0a0a] border-[#2a2a2a] text-[#f5f5f7] placeholder:text-[#86868b] focus-visible:ring-[#0071e3]"
            autoFocus
          />
          <div>
            <label className="text-xs text-[#86868b] block mb-1">Prazo (opcional)</label>
            <Input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="bg-[#0a0a0a] border-[#2a2a2a] text-[#f5f5f7] focus-visible:ring-[#0071e3] [color-scheme:dark]"
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="ghost"
            onClick={handleClose}
            className="text-[#86868b] hover:text-[#f5f5f7]"
          >
            Cancelar
          </Button>
          <Button
            onClick={handleSave}
            disabled={!title.trim() || isPending}
            className="bg-[#0071e3] hover:bg-[#0077ed] text-white"
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
    <div className="space-y-1">
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="flex items-center gap-3 px-3 py-2.5">
          <Skeleton className="h-5 w-5 rounded-full bg-[#1f1f1f] shrink-0" />
          <div className="flex-1 space-y-1.5">
            <Skeleton className="h-4 w-3/4 bg-[#1f1f1f]" />
            <Skeleton className="h-3 w-1/4 bg-[#1f1f1f]" />
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
      <CheckSquare size={32} className="text-[#3a3a3a] mb-3" />
      <p className="text-[#f5f5f7] text-sm font-medium">Nenhuma tarefa pendente</p>
      <p className="text-[#86868b] text-xs mt-1">Crie uma tarefa para começar</p>
    </div>
  )
}

function EmptyArchived() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <CheckSquare size={32} className="text-[#3a3a3a] mb-3" />
      <p className="text-[#f5f5f7] text-sm font-medium">Nenhuma tarefa arquivada</p>
      <p className="text-[#86868b] text-xs mt-1">Tarefas concluídas aparecem aqui</p>
    </div>
  )
}

function EmptyFiltered({ onClear }: { onClear: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <p className="text-[#86868b] text-sm">Nenhuma tarefa com este filtro</p>
      <button
        type="button"
        onClick={onClear}
        className="mt-2 text-xs text-[#0071e3] hover:underline cursor-pointer"
      >
        Limpar filtro
      </button>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function SingleTasksPage() {
  const [showModal,       setShowModal]       = useState(false)
  const [deadlineFilter,  setDeadlineFilter]  = useState<DeadlineFilter | null>(null)

  const { data: pending,  isLoading: loadingPending  } = usePendingSingleTasks()
  const { data: archived, isLoading: loadingArchived } = useArchivedSingleTasks()

  const { mutate: create,    isPending: creating    } = useCreateSingleTask()
  const { mutate: complete                           } = useCompleteSingleTask()
  const { mutate: uncomplete                         } = useUncompleteSingleTask()
  const { mutate: remove                             } = useDeleteSingleTask()

  const filteredPending = applyDeadlineFilter(pending ?? [], deadlineFilter)

  function handleSave(title: string, dueDate: string | null) {
    create({ title, dueDate })
  }

  return (
    <div>
      {/* Page header */}
      <header className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-light text-[#f5f5f7] tracking-tight">Tarefas</h1>
          <p className="text-sm text-[#86868b] mt-1">
            {pending?.length ?? 0} pendente{(pending?.length ?? 0) !== 1 ? 's' : ''}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowModal(true)}
          className="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-[#0071e3] hover:bg-[#0077ed] text-white text-sm font-medium transition-colors cursor-pointer"
        >
          <Plus size={15} />
          Nova tarefa
        </button>
      </header>

      <Tabs defaultValue="pending">
        <TabsList className="bg-[#141414] border border-[#1f1f1f] h-9 mb-4">
          <TabsTrigger
            value="pending"
            className="text-xs data-[state=active]:bg-[#1f1f1f] data-[state=active]:text-[#f5f5f7] text-[#86868b]"
          >
            Pendentes
            {(pending?.length ?? 0) > 0 && (
              <span className="ml-1.5 text-[10px] bg-[#0071e3] text-white rounded-full px-1.5 py-0">
                {pending?.length}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger
            value="archived"
            className="text-xs data-[state=active]:bg-[#1f1f1f] data-[state=active]:text-[#f5f5f7] text-[#86868b]"
          >
            Arquivadas
          </TabsTrigger>
        </TabsList>

        {/* ── Pending tab ─────────────────────────────────────────────────────── */}
        <TabsContent value="pending">
          {/* Deadline filter pills */}
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
            <div className="rounded-xl bg-[#141414] border border-[#1f1f1f] divide-y divide-[#1f1f1f] overflow-hidden">
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
            <div className="rounded-xl bg-[#141414] border border-[#1f1f1f] divide-y divide-[#1f1f1f] overflow-hidden">
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
