import { useState } from 'react'
import { ArrowLeft, Plus } from 'lucide-react'
import { useManage } from '@/hooks/useManage'
import { AreaManageCard } from '@/components/shared/AreaManageCard'
import { TaskManageRow } from '@/components/shared/TaskManageRow'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import type { AreaResponse, TaskResponse } from '@/types'

// ── Constants ─────────────────────────────────────────────────────────────────

const DAYS_OF_WEEK = [
  { value: 'MONDAY',    label: 'Segunda-feira' },
  { value: 'TUESDAY',   label: 'Terça-feira'   },
  { value: 'WEDNESDAY', label: 'Quarta-feira'  },
  { value: 'THURSDAY',  label: 'Quinta-feira'  },
  { value: 'FRIDAY',    label: 'Sexta-feira'   },
  { value: 'SATURDAY',  label: 'Sábado'        },
  { value: 'SUNDAY',    label: 'Domingo'       },
]

const DAY_FULL: Record<string, string> = Object.fromEntries(
  DAYS_OF_WEEK.map((d) => [d.value, d.label]),
)

// ── Area Modal ─────────────────────────────────────────────────────────────────

interface AreaModalProps {
  open: boolean
  initial?: AreaResponse
  onClose: () => void
  onSave: (name: string, color: string, icon: string) => void
  isPending: boolean
}

function AreaModal({ open, initial, onClose, onSave, isPending }: AreaModalProps) {
  const [name, setName] = useState(initial?.name ?? '')
  const [color, setColor] = useState(initial?.color ?? '#3B82F6')
  const [icon, setIcon] = useState(initial?.icon ?? '📚')

  // Reset state when modal opens with new initial values
  const handleOpenChange = (open: boolean) => {
    if (open) {
      setName(initial?.name ?? '')
      setColor(initial?.color ?? '#3B82F6')
      setIcon(initial?.icon ?? '📚')
    } else {
      onClose()
    }
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!name.trim() || !icon.trim()) return
    onSave(name.trim(), color, icon.trim())
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="bg-[#141414] border-[#1f1f1f] text-[#f5f5f7] max-w-sm">
        <DialogHeader>
          <DialogTitle className="text-[#f5f5f7]">
            {initial ? 'Editar Área' : 'Nova Área'}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 pt-1">
          <div className="space-y-1.5">
            <label className="text-xs text-[#86868b] font-medium">Nome</label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="ex: Inglês/PTE"
              className="bg-[#1f1f1f] border-[#2a2a2a] text-[#f5f5f7] placeholder:text-[#3a3a3a] focus-visible:ring-[#0071e3]"
              autoFocus
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#86868b] font-medium">Ícone (emoji)</label>
            <Input
              value={icon}
              onChange={(e) => setIcon(e.target.value)}
              placeholder="📚"
              className="bg-[#1f1f1f] border-[#2a2a2a] text-[#f5f5f7] placeholder:text-[#3a3a3a] focus-visible:ring-[#0071e3]"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#86868b] font-medium">Cor</label>
            <div className="flex items-center gap-3">
              <input
                type="color"
                value={color}
                onChange={(e) => setColor(e.target.value)}
                className="w-10 h-10 rounded-lg border border-[#2a2a2a] bg-[#1f1f1f] cursor-pointer p-1"
              />
              <span className="text-sm text-[#86868b] font-mono">{color.toUpperCase()}</span>
            </div>
          </div>
          <DialogFooter className="gap-2 pt-2">
            <Button
              type="button"
              variant="ghost"
              onClick={onClose}
              className="text-[#86868b] hover:text-[#f5f5f7]"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isPending || !name.trim() || !icon.trim()}
              className="bg-[#0071e3] hover:bg-[#0077ed] text-white"
            >
              {isPending ? 'Salvando…' : 'Salvar'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// ── Task Modal ─────────────────────────────────────────────────────────────────

interface TaskModalProps {
  open: boolean
  initial?: TaskResponse
  onClose: () => void
  onSave: (title: string, description: string | null, estimatedMinutes: number | null, dayOfWeek: string) => void
  isPending: boolean
}

function TaskModal({ open, initial, onClose, onSave, isPending }: TaskModalProps) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [estimatedMinutes, setEstimatedMinutes] = useState(
    initial?.estimatedMinutes?.toString() ?? '',
  )
  const [dayOfWeek, setDayOfWeek] = useState(initial?.dayOfWeek ?? 'MONDAY')

  const handleOpenChange = (o: boolean) => {
    if (o) {
      setTitle(initial?.title ?? '')
      setDescription(initial?.description ?? '')
      setEstimatedMinutes(initial?.estimatedMinutes?.toString() ?? '')
      setDayOfWeek(initial?.dayOfWeek ?? 'MONDAY')
    } else {
      onClose()
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) return
    const mins = estimatedMinutes ? parseInt(estimatedMinutes, 10) : null
    onSave(title.trim(), description.trim() || null, mins, dayOfWeek)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="bg-[#141414] border-[#1f1f1f] text-[#f5f5f7] max-w-sm">
        <DialogHeader>
          <DialogTitle className="text-[#f5f5f7]">
            {initial ? 'Editar Tarefa' : 'Nova Tarefa'}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 pt-1">
          <div className="space-y-1.5">
            <label className="text-xs text-[#86868b] font-medium">Título</label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="ex: Re-tell Lecture"
              className="bg-[#1f1f1f] border-[#2a2a2a] text-[#f5f5f7] placeholder:text-[#3a3a3a] focus-visible:ring-[#0071e3]"
              autoFocus
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#86868b] font-medium">Dia da semana</label>
            <select
              value={dayOfWeek}
              onChange={(e) => setDayOfWeek(e.target.value)}
              className="w-full h-9 rounded-md border border-[#2a2a2a] bg-[#1f1f1f] text-[#f5f5f7] text-sm px-3 focus:outline-none focus:ring-2 focus:ring-[#0071e3]"
            >
              {DAYS_OF_WEEK.map((d) => (
                <option key={d.value} value={d.value} className="bg-[#1f1f1f]">
                  {d.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#86868b] font-medium">
              Duração estimada (min) <span className="text-[#3a3a3a]">— opcional</span>
            </label>
            <Input
              type="number"
              min={1}
              value={estimatedMinutes}
              onChange={(e) => setEstimatedMinutes(e.target.value)}
              placeholder="30"
              className="bg-[#1f1f1f] border-[#2a2a2a] text-[#f5f5f7] placeholder:text-[#3a3a3a] focus-visible:ring-[#0071e3]"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#86868b] font-medium">
              Descrição <span className="text-[#3a3a3a]">— opcional</span>
            </label>
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detalhes da tarefa…"
              rows={2}
              className="bg-[#1f1f1f] border-[#2a2a2a] text-[#f5f5f7] placeholder:text-[#3a3a3a] focus-visible:ring-[#0071e3] resize-none"
            />
          </div>
          <DialogFooter className="gap-2 pt-2">
            <Button
              type="button"
              variant="ghost"
              onClick={onClose}
              className="text-[#86868b] hover:text-[#f5f5f7]"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isPending || !title.trim()}
              className="bg-[#0071e3] hover:bg-[#0077ed] text-white"
            >
              {isPending ? 'Salvando…' : 'Salvar'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// ── Delete Confirm ─────────────────────────────────────────────────────────────

interface DeleteConfirmProps {
  open: boolean
  label: string
  onClose: () => void
  onConfirm: () => void
  isPending: boolean
}

function DeleteConfirm({ open, label, onClose, onConfirm, isPending }: DeleteConfirmProps) {
  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogContent className="bg-[#141414] border-[#1f1f1f] text-[#f5f5f7]">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-[#f5f5f7]">Confirmar exclusão</AlertDialogTitle>
          <AlertDialogDescription className="text-[#86868b]">
            Tem certeza que deseja excluir <span className="text-[#f5f5f7] font-medium">"{label}"</span>?
            Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel
            onClick={onClose}
            className="bg-transparent border-[#2a2a2a] text-[#86868b] hover:text-[#f5f5f7] hover:bg-[#1f1f1f]"
          >
            Cancelar
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            disabled={isPending}
            className="bg-red-600 hover:bg-red-700 text-white border-0"
          >
            {isPending ? 'Excluindo…' : 'Excluir'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

type AreaModal = { open: boolean; area?: AreaResponse }
type TaskModal = { open: boolean; task?: TaskResponse }
type DeleteTarget =
  | { open: false }
  | { open: true; type: 'area'; id: number; label: string }
  | { open: true; type: 'task'; areaId: number; id: number; label: string }

export function ManagePage() {
  const {
    areas,
    isLoading,
    error,
    createArea,
    updateArea,
    deleteArea,
    createTask,
    updateTask,
    deleteTask,
  } = useManage()

  const [selectedAreaId, setSelectedAreaId] = useState<number | null>(null)
  const [areaModal, setAreaModal] = useState<AreaModal>({ open: false })
  const [taskModal, setTaskModal] = useState<TaskModal>({ open: false })
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget>({ open: false })

  const selectedArea = areas.find((a) => a.id === selectedAreaId) ?? null

  // ── 404 = no active routine ───────────────────────────────────────────────

  const isNoRoutine =
    (error as { response?: { status?: number } } | null)?.response?.status === 404

  if (isLoading) {
    return (
      <div className="space-y-3 pt-2">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-12 rounded-xl bg-[#141414] animate-pulse" />
        ))}
      </div>
    )
  }

  if (isNoRoutine) return <EmptyRoutineState />

  // ── Handlers — Areas ─────────────────────────────────────────────────────

  const handleSaveArea = (name: string, color: string, icon: string) => {
    if (areaModal.area) {
      updateArea.mutate(
        { id: areaModal.area.id, data: { name, color, icon } },
        { onSuccess: () => setAreaModal({ open: false }) },
      )
    } else {
      createArea.mutate(
        { name, color, icon },
        { onSuccess: () => setAreaModal({ open: false }) },
      )
    }
  }

  const handleDeleteArea = (area: AreaResponse) => {
    setDeleteTarget({ open: true, type: 'area', id: area.id, label: area.name })
  }

  // ── Handlers — Tasks ─────────────────────────────────────────────────────

  const handleSaveTask = (
    title: string,
    description: string | null,
    estimatedMinutes: number | null,
    dayOfWeek: string,
  ) => {
    if (!selectedArea) return
    if (taskModal.task) {
      updateTask.mutate(
        { areaId: selectedArea.id, taskId: taskModal.task.id, data: { title, description, estimatedMinutes, dayOfWeek } },
        { onSuccess: () => setTaskModal({ open: false }) },
      )
    } else {
      createTask.mutate(
        { areaId: selectedArea.id, data: { title, description, estimatedMinutes, dayOfWeek } },
        { onSuccess: () => setTaskModal({ open: false }) },
      )
    }
  }

  const handleDeleteTask = (task: TaskResponse) => {
    if (!selectedArea) return
    setDeleteTarget({ open: true, type: 'task', areaId: selectedArea.id, id: task.id, label: task.title })
  }

  // ── Confirm delete ────────────────────────────────────────────────────────

  const handleConfirmDelete = () => {
    if (!deleteTarget.open) return
    if (deleteTarget.type === 'area') {
      deleteArea.mutate(deleteTarget.id, {
        onSuccess: () => {
          setDeleteTarget({ open: false })
          if (selectedAreaId === deleteTarget.id) setSelectedAreaId(null)
        },
      })
    } else {
      deleteTask.mutate(
        { areaId: deleteTarget.areaId, taskId: deleteTarget.id },
        { onSuccess: () => setDeleteTarget({ open: false }) },
      )
    }
  }

  const isDeletePending = deleteArea.isPending || deleteTask.isPending

  // ── Render ────────────────────────────────────────────────────────────────

  const tasksByDay = selectedArea
    ? [...selectedArea.tasks].sort((a, b) => {
        const dayOrder = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']
        const dayDiff = dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek)
        return dayDiff !== 0 ? dayDiff : a.orderIndex - b.orderIndex
      })
    : []

  return (
    <>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-[#f5f5f7]">Gerenciar Rotina</h1>
        <p className="text-sm text-[#86868b] mt-0.5">
          Adicione, edite ou remova áreas e tarefas da sua rotina.
        </p>
      </div>

      {/* ── Two-column layout (desktop) / single-panel (mobile) ── */}
      <div className="flex flex-col md:grid md:grid-cols-[280px_1fr] gap-4 md:gap-6">

        {/* ── Left panel: Areas list ── */}
        <div className={selectedArea ? 'hidden md:block' : 'block'}>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-[#86868b] uppercase tracking-widest">
              Áreas
            </span>
            <button
              onClick={() => setAreaModal({ open: true })}
              className="flex items-center gap-1 text-xs text-[#0071e3] hover:text-[#0077ed] font-medium transition-colors"
            >
              <Plus size={13} />
              Nova Área
            </button>
          </div>

          {areas.length === 0 ? (
            <div className="py-8 text-center text-sm text-[#86868b]">
              Nenhuma área encontrada.{' '}
              <button
                onClick={() => setAreaModal({ open: true })}
                className="text-[#0071e3] hover:underline"
              >
                Criar primeira área
              </button>
            </div>
          ) : (
            <div className="space-y-1">
              {areas.map((area) => (
                <AreaManageCard
                  key={area.id}
                  area={area}
                  isSelected={area.id === selectedAreaId}
                  onSelect={() => setSelectedAreaId(area.id)}
                  onEdit={() => setAreaModal({ open: true, area })}
                  onDelete={() => handleDeleteArea(area)}
                />
              ))}
            </div>
          )}
        </div>

        {/* ── Right panel: Tasks ── */}
        <div className={!selectedArea ? 'hidden md:flex md:items-center md:justify-center' : 'block'}>
          {!selectedArea ? (
            <p className="text-sm text-[#3a3a3a] text-center">
              Selecione uma área para gerenciar suas tarefas.
            </p>
          ) : (
            <div>
              {/* Mobile back button */}
              <button
                onClick={() => setSelectedAreaId(null)}
                className="md:hidden flex items-center gap-1.5 text-sm text-[#86868b] hover:text-[#f5f5f7] mb-4 transition-colors"
              >
                <ArrowLeft size={15} />
                Voltar para áreas
              </button>

              {/* Area header */}
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div
                    className="w-7 h-7 rounded-full flex items-center justify-center text-sm shrink-0"
                    style={{ backgroundColor: selectedArea.color + '22', border: `2px solid ${selectedArea.color}` }}
                  >
                    {selectedArea.icon}
                  </div>
                  <span className="text-sm font-semibold text-[#f5f5f7]">{selectedArea.name}</span>
                </div>
                <button
                  onClick={() => setTaskModal({ open: true })}
                  className="flex items-center gap-1 text-xs text-[#0071e3] hover:text-[#0077ed] font-medium transition-colors"
                >
                  <Plus size={13} />
                  Nova Tarefa
                </button>
              </div>

              {/* Tasks list */}
              {tasksByDay.length === 0 ? (
                <div className="py-8 text-center text-sm text-[#86868b]">
                  Nenhuma tarefa nesta área.{' '}
                  <button
                    onClick={() => setTaskModal({ open: true })}
                    className="text-[#0071e3] hover:underline"
                  >
                    Adicionar tarefa
                  </button>
                </div>
              ) : (
                <div className="space-y-0.5">
                  {/* Group tasks by day */}
                  {DAYS_OF_WEEK.filter((d) => tasksByDay.some((t) => t.dayOfWeek === d.value)).map(
                    (day) => {
                      const dayTasks = tasksByDay.filter((t) => t.dayOfWeek === day.value)
                      return (
                        <div key={day.value} className="mb-3">
                          <div className="px-3 mb-1">
                            <span className="text-[10px] font-semibold text-[#86868b] uppercase tracking-widest">
                              {DAY_FULL[day.value]}
                            </span>
                          </div>
                          {dayTasks.map((task) => (
                            <TaskManageRow
                              key={task.id}
                              task={task}
                              onEdit={() => setTaskModal({ open: true, task })}
                              onDelete={() => handleDeleteTask(task)}
                            />
                          ))}
                        </div>
                      )
                    },
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ── Modals ── */}
      <AreaModal
        open={areaModal.open}
        initial={areaModal.area}
        onClose={() => setAreaModal({ open: false })}
        onSave={handleSaveArea}
        isPending={createArea.isPending || updateArea.isPending}
      />

      <TaskModal
        open={taskModal.open}
        initial={taskModal.task}
        onClose={() => setTaskModal({ open: false })}
        onSave={handleSaveTask}
        isPending={createTask.isPending || updateTask.isPending}
      />

      <DeleteConfirm
        open={deleteTarget.open}
        label={deleteTarget.open ? deleteTarget.label : ''}
        onClose={() => setDeleteTarget({ open: false })}
        onConfirm={handleConfirmDelete}
        isPending={isDeletePending}
      />
    </>
  )
}
