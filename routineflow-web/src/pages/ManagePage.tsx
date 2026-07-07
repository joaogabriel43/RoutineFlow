import { lazy, Suspense, useEffect, useState } from 'react'
import { ArrowLeft, Loader2, Plus, SearchX } from 'lucide-react'
import { useManage } from '@/hooks/useManage'
import { AreaManageCard } from '@/components/shared/AreaManageCard'
import { TaskManageRow } from '@/components/shared/TaskManageRow'
import { EmptyRoutineState } from '@/components/shared/EmptyRoutineState'
import { FilterBar } from '@/components/shared/FilterBar'
import { FilterPills } from '@/components/shared/FilterPills'
import { ColorPicker } from '@/components/shared/ColorPicker'
import { DynamicIcon } from '@/components/shared/DynamicIcon'

// Lazy — keeps the full lucide icon catalog out of the main bundle.
const IconPicker = lazy(() => import('@/components/shared/IconPicker'))

const ICON_DEFAULT = 'folder'
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
import type { AreaResponse, GoalType, ResetFrequency, ScheduleType, TaskResponse } from '@/types'

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

const RESET_FREQUENCIES: { value: ResetFrequency; label: string }[] = [
  { value: 'DAILY',   label: 'Diária (todo dia)'    },
  { value: 'WEEKLY',  label: 'Semanal (toda segunda)' },
  { value: 'MONTHLY', label: 'Mensal (todo dia 1)'    },
]

const FREQ_FILTER_OPTIONS: { value: ResetFrequency; label: string }[] = [
  { value: 'DAILY',   label: 'Diário'   },
  { value: 'WEEKLY',  label: 'Semanal'  },
  { value: 'MONTHLY', label: 'Mensal'   },
]

const SCHEDULE_FILTER_OPTIONS: { value: ScheduleType; label: string }[] = [
  { value: 'DAY_OF_WEEK',  label: 'Semanal' },
  { value: 'DAY_OF_MONTH', label: 'Mensal'  },
]

const DAY_FILTER_OPTIONS = [
  { value: 'MONDAY',    label: 'Seg' },
  { value: 'TUESDAY',   label: 'Ter' },
  { value: 'WEDNESDAY', label: 'Qua' },
  { value: 'THURSDAY',  label: 'Qui' },
  { value: 'FRIDAY',    label: 'Sex' },
  { value: 'SATURDAY',  label: 'Sáb' },
  { value: 'SUNDAY',    label: 'Dom' },
]

// ── Area Modal ─────────────────────────────────────────────────────────────────

interface AreaModalProps {
  open: boolean
  initial?: AreaResponse
  onClose: () => void
  onSave: (name: string, color: string, icon: string, resetFrequency: ResetFrequency) => void
  isPending: boolean
}

function AreaModal({ open, initial, onClose, onSave, isPending }: AreaModalProps) {
  const [name, setName] = useState(initial?.name ?? '')
  const [color, setColor] = useState(initial?.color ?? '#2F8BFF')
  const [icon, setIcon] = useState(initial?.icon ?? ICON_DEFAULT)
  const [showIconPicker, setShowIconPicker] = useState(false)
  const [resetFrequency, setResetFrequency] = useState<ResetFrequency>(
    initial?.resetFrequency ?? 'DAILY',
  )

  const handleOpenChange = (o: boolean) => {
    if (o) {
      setName(initial?.name ?? '')
      setColor(initial?.color ?? '#2F8BFF')
      setIcon(initial?.icon ?? ICON_DEFAULT)
      setShowIconPicker(false)
      setResetFrequency(initial?.resetFrequency ?? 'DAILY')
    } else {
      onClose()
    }
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!name.trim() || !icon.trim()) return
    onSave(name.trim(), color, icon.trim(), resetFrequency)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="bg-[#141416] border-[#26262A] text-[#F4F2EF] max-w-sm">
        <DialogHeader>
          <DialogTitle className="text-[#F4F2EF]">
            {initial ? 'Editar Área' : 'Nova Área'}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 pt-1">
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">Nome</label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="ex: Inglês/PTE"
              className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#34343A] focus-visible:ring-[#2F8BFF]"
              autoFocus
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">Cor</label>
            <ColorPicker value={color} onChange={setColor} />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">Ícone</label>
            <button
              type="button"
              onClick={() => setShowIconPicker((v) => !v)}
              className="flex items-center gap-3 w-full rounded-md border border-[#26262A] bg-[#26262A] px-3 py-2 text-left hover:border-[#34343A] transition-colors"
            >
              <span
                className="w-8 h-8 rounded-md flex items-center justify-center shrink-0"
                style={{ backgroundColor: color + '22', color }}
              >
                <DynamicIcon name={icon} size={18} fallback="folder" />
              </span>
              <span className="num text-sm text-[#8C8A88] flex-1">{icon}</span>
              <span className="text-xs text-[#2F8BFF]">{showIconPicker ? 'Fechar' : 'Trocar'}</span>
            </button>
            {showIconPicker && (
              <div className="rounded-md border border-[#26262A] bg-[#0F0F11] p-2 mt-1">
                <Suspense
                  fallback={
                    <div className="flex items-center justify-center py-8 text-[#8C8A88]">
                      <Loader2 size={18} className="animate-spin" />
                    </div>
                  }
                >
                  <IconPicker
                    value={icon}
                    onChange={(n) => {
                      setIcon(n)
                      setShowIconPicker(false)
                    }}
                  />
                </Suspense>
              </div>
            )}
          </div>
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">Frequência do streak</label>
            <select
              value={resetFrequency}
              onChange={(e) => setResetFrequency(e.target.value as ResetFrequency)}
              className="w-full h-9 rounded-md border border-[#26262A] bg-[#26262A] text-[#F4F2EF] text-sm px-3 focus:outline-none focus:ring-2 focus:ring-[#2F8BFF]"
            >
              {RESET_FREQUENCIES.map((f) => (
                <option key={f.value} value={f.value} className="bg-[#26262A]">
                  {f.label}
                </option>
              ))}
            </select>
          </div>
          <DialogFooter className="gap-2 pt-2">
            <Button
              type="button"
              variant="ghost"
              onClick={onClose}
              className="text-[#8C8A88] hover:text-[#F4F2EF]"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isPending || !name.trim() || !icon.trim()}
              className="bg-[#2F8BFF] hover:bg-[#4F9DFF] text-white"
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
  onSave: (
    title: string,
    description: string | null,
    estimatedMinutes: number | null,
    scheduleType: ScheduleType,
    dayOfWeek: string | null,
    dayOfMonth: number | null,
    reminderTime: string | null,
    goalType: GoalType,
    goalTarget: number | null,
    goalUnit: string | null,
    icon: string | null,
    color: string | null,
  ) => void
  isPending: boolean
}

function TaskModal({ open, initial, onClose, onSave, isPending }: TaskModalProps) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [estimatedMinutes, setEstimatedMinutes] = useState(
    initial?.estimatedMinutes?.toString() ?? '',
  )
  const [scheduleType, setScheduleType] = useState<ScheduleType>(
    initial?.scheduleType ?? 'DAY_OF_WEEK',
  )
  const [dayOfWeek, setDayOfWeek] = useState<string>(initial?.dayOfWeek ?? 'MONDAY')
  const [dayOfMonth, setDayOfMonth] = useState<string>(
    initial?.dayOfMonth?.toString() ?? '',
  )
  const [reminderTime, setReminderTime] = useState<string>(initial?.reminderTime ?? '')
  const [goalType, setGoalType] = useState<GoalType>(initial?.goalType ?? 'BOOLEAN')
  const [goalTarget, setGoalTarget] = useState<string>(initial?.goalTarget?.toString() ?? '')
  const [goalUnit, setGoalUnit] = useState<string>(initial?.goalUnit ?? '')
  const [icon, setIcon] = useState<string | null>(initial?.icon ?? null)
  const [color, setColor] = useState<string | null>(initial?.color ?? null)
  const [showIconPicker, setShowIconPicker] = useState(false)

  const handleOpenChange = (o: boolean) => {
    if (o) {
      setTitle(initial?.title ?? '')
      setDescription(initial?.description ?? '')
      setEstimatedMinutes(initial?.estimatedMinutes?.toString() ?? '')
      setScheduleType(initial?.scheduleType ?? 'DAY_OF_WEEK')
      setDayOfWeek(initial?.dayOfWeek ?? 'MONDAY')
      setDayOfMonth(initial?.dayOfMonth?.toString() ?? '')
      setReminderTime(initial?.reminderTime ?? '')
      setGoalType(initial?.goalType ?? 'BOOLEAN')
      setGoalTarget(initial?.goalTarget?.toString() ?? '')
      setGoalUnit(initial?.goalUnit ?? '')
      setIcon(initial?.icon ?? null)
      setColor(initial?.color ?? null)
      setShowIconPicker(false)
    } else {
      onClose()
    }
  }

  const isValid =
    title.trim().length > 0 &&
    (scheduleType === 'DAY_OF_WEEK'
      ? !!dayOfWeek
      : !!dayOfMonth && parseInt(dayOfMonth, 10) >= 1 && parseInt(dayOfMonth, 10) <= 31) &&
    (goalType === 'BOOLEAN' || (!!goalTarget && parseFloat(goalTarget) > 0))

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!isValid) return
    const mins = estimatedMinutes ? parseInt(estimatedMinutes, 10) : null
    onSave(
      title.trim(),
      description.trim() || null,
      mins,
      scheduleType,
      scheduleType === 'DAY_OF_WEEK' ? dayOfWeek : null,
      scheduleType === 'DAY_OF_MONTH' ? parseInt(dayOfMonth, 10) : null,
      reminderTime.trim() || null,
      goalType,
      goalType === 'NUMERIC' && goalTarget ? parseFloat(goalTarget) : null,
      goalType === 'NUMERIC' && goalUnit.trim() ? goalUnit.trim() : null,
      icon,
      color,
    )
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="bg-[#141416] border-[#26262A] text-[#F4F2EF] max-w-sm">
        <DialogHeader>
          <DialogTitle className="text-[#F4F2EF]">
            {initial ? 'Editar Tarefa' : 'Nova Tarefa'}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 pt-1">
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">Título</label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="ex: Re-tell Lecture"
              className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#34343A] focus-visible:ring-[#2F8BFF]"
              autoFocus
            />
          </div>

          {/* Goal Type toggle */}
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">Tipo de Meta</label>
            <div className="flex rounded-lg border border-[#26262A] overflow-hidden">
              <button
                type="button"
                onClick={() => setGoalType('BOOLEAN')}
                className={`flex-1 py-2 text-xs font-medium transition-colors ${
                  goalType === 'BOOLEAN'
                    ? 'bg-[#2F8BFF] text-white'
                    : 'bg-[#26262A] text-[#8C8A88] hover:text-[#F4F2EF]'
                }`}
              >
                Concluída / Não Concluída
              </button>
              <button
                type="button"
                onClick={() => setGoalType('NUMERIC')}
                className={`flex-1 py-2 text-xs font-medium transition-colors ${
                  goalType === 'NUMERIC'
                    ? 'bg-[#2F8BFF] text-white'
                    : 'bg-[#26262A] text-[#8C8A88] hover:text-[#F4F2EF]'
                }`}
              >
                Alvo Numérico
              </button>
            </div>
          </div>

          {goalType === 'NUMERIC' && (
            <div className="flex gap-3">
              <div className="space-y-1.5 flex-1">
                <label className="text-xs text-[#8C8A88] font-medium">Alvo (ex: 3, 5.5)</label>
                <Input
                  type="number"
                  step="0.1"
                  min="0.1"
                  value={goalTarget}
                  onChange={(e) => setGoalTarget(e.target.value)}
                  placeholder="Ex: 3"
                  className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#34343A] focus-visible:ring-[#2F8BFF]"
                />
              </div>
              <div className="space-y-1.5 flex-1">
                <label className="text-xs text-[#8C8A88] font-medium">Unidade <span className="text-[#34343A]">— opcional</span></label>
                <Input
                  type="text"
                  value={goalUnit}
                  onChange={(e) => setGoalUnit(e.target.value)}
                  placeholder="Ex: copos, km, pag"
                  className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#34343A] focus-visible:ring-[#2F8BFF]"
                />
              </div>
            </div>
          )}

          {/* Schedule type toggle */}
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">Tipo de recorrência</label>
            <div className="flex rounded-lg border border-[#26262A] overflow-hidden">
              <button
                type="button"
                onClick={() => setScheduleType('DAY_OF_WEEK')}
                className={`flex-1 py-2 text-xs font-medium transition-colors ${
                  scheduleType === 'DAY_OF_WEEK'
                    ? 'bg-[#2F8BFF] text-white'
                    : 'bg-[#26262A] text-[#8C8A88] hover:text-[#F4F2EF]'
                }`}
              >
                Dia da semana
              </button>
              <button
                type="button"
                onClick={() => setScheduleType('DAY_OF_MONTH')}
                className={`flex-1 py-2 text-xs font-medium transition-colors ${
                  scheduleType === 'DAY_OF_MONTH'
                    ? 'bg-[#2F8BFF] text-white'
                    : 'bg-[#26262A] text-[#8C8A88] hover:text-[#F4F2EF]'
                }`}
              >
                Dia do mês
              </button>
            </div>
          </div>

          {scheduleType === 'DAY_OF_WEEK' && (
            <div className="space-y-1.5">
              <label className="text-xs text-[#8C8A88] font-medium">Dia da semana</label>
              <select
                value={dayOfWeek}
                onChange={(e) => setDayOfWeek(e.target.value)}
                className="w-full h-9 rounded-md border border-[#26262A] bg-[#26262A] text-[#F4F2EF] text-sm px-3 focus:outline-none focus:ring-2 focus:ring-[#2F8BFF]"
              >
                {DAYS_OF_WEEK.map((d) => (
                  <option key={d.value} value={d.value} className="bg-[#26262A]">
                    {d.label}
                  </option>
                ))}
              </select>
            </div>
          )}

          {scheduleType === 'DAY_OF_MONTH' && (
            <div className="space-y-1.5">
              <label className="text-xs text-[#8C8A88] font-medium">
                Dia do mês <span className="text-[#34343A]">(1–31)</span>
              </label>
              <Input
                type="number"
                min={1}
                max={31}
                value={dayOfMonth}
                onChange={(e) => setDayOfMonth(e.target.value)}
                placeholder="ex: 25"
                className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#34343A] focus-visible:ring-[#2F8BFF]"
              />
              <p className="text-[10px] text-[#8C8A88]">
                A tarefa aparecerá todo dia {dayOfMonth || 'N'} do mês.
                Em meses com menos dias, não aparecerá.
              </p>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">
              Duração estimada (min) <span className="text-[#34343A]">— opcional</span>
            </label>
            <Input
              type="number"
              min={1}
              value={estimatedMinutes}
              onChange={(e) => setEstimatedMinutes(e.target.value)}
              placeholder="30"
              className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#34343A] focus-visible:ring-[#2F8BFF]"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">
              Horário do lembrete <span className="text-[#34343A]">— opcional</span>
            </label>
            <Input
              type="time"
              value={reminderTime}
              onChange={(e) => setReminderTime(e.target.value)}
              className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] focus-visible:ring-[#2F8BFF]"
            />
          </div>

          {/* Icon + color (optional) */}
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">
              Ícone e cor <span className="text-[#34343A]">— opcional</span>
            </label>
            <button
              type="button"
              onClick={() => setShowIconPicker((v) => !v)}
              className="flex items-center gap-3 w-full rounded-md border border-[#26262A] bg-[#26262A] px-3 py-2 text-left hover:border-[#34343A] transition-colors"
            >
              <span
                className="w-8 h-8 rounded-md flex items-center justify-center shrink-0"
                style={{ backgroundColor: (color ?? '#8E8E93') + '22', color: color ?? '#8E8E93' }}
              >
                <DynamicIcon name={icon} size={18} fallback="circle" />
              </span>
              <span className="num text-sm text-[#8C8A88] flex-1">{icon ?? 'sem ícone'}</span>
              <span className="text-xs text-[#2F8BFF]">{showIconPicker ? 'Fechar' : 'Escolher'}</span>
            </button>
            {showIconPicker && (
              <div className="rounded-md border border-[#26262A] bg-[#0F0F11] p-2 mt-1 space-y-2">
                <ColorPicker value={color} onChange={setColor} />
                <Suspense
                  fallback={
                    <div className="flex items-center justify-center py-8 text-[#8C8A88]">
                      <Loader2 size={18} className="animate-spin" />
                    </div>
                  }
                >
                  <IconPicker
                    value={icon}
                    onChange={(n) => {
                      setIcon(n)
                      setShowIconPicker(false)
                    }}
                  />
                </Suspense>
              </div>
            )}
          </div>

          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] font-medium">
              Descrição <span className="text-[#34343A]">— opcional</span>
            </label>
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detalhes da tarefa…"
              rows={2}
              className="bg-[#26262A] border-[#26262A] text-[#F4F2EF] placeholder:text-[#34343A] focus-visible:ring-[#2F8BFF] resize-none"
            />
          </div>
          <DialogFooter className="gap-2 pt-2">
            <Button
              type="button"
              variant="ghost"
              onClick={onClose}
              className="text-[#8C8A88] hover:text-[#F4F2EF]"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isPending || !isValid}
              className="bg-[#2F8BFF] hover:bg-[#4F9DFF] text-white"
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
      <AlertDialogContent className="bg-[#141416] border-[#26262A] text-[#F4F2EF]">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-[#F4F2EF]">Confirmar exclusão</AlertDialogTitle>
          <AlertDialogDescription className="text-[#8C8A88]">
            Tem certeza que deseja excluir{' '}
            <span className="text-[#F4F2EF] font-medium">"{label}"</span>? Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel
            onClick={onClose}
            className="bg-transparent border-[#26262A] text-[#8C8A88] hover:text-[#F4F2EF] hover:bg-[#26262A]"
          >
            Cancelar
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            disabled={isPending}
            className="bg-danger hover:bg-danger-hover text-white border-0"
          >
            {isPending ? 'Excluindo…' : 'Excluir'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

// ── Types ──────────────────────────────────────────────────────────────────────

type AreaModalState = { open: boolean; area?: AreaResponse }
type TaskModalState = { open: boolean; task?: TaskResponse }
type DeleteTarget =
  | { open: false }
  | { open: true; type: 'area'; id: number; label: string }
  | { open: true; type: 'task'; areaId: number; id: number; label: string }

const DAY_ORDER = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

// ── Main page ─────────────────────────────────────────────────────────────────

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
  const [areaModal, setAreaModal] = useState<AreaModalState>({ open: false })
  const [taskModal, setTaskModal] = useState<TaskModalState>({ open: false })
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget>({ open: false })

  // ── Area filter state ──────────────────────────────────────────────────────
  const [areaSearch, setAreaSearch] = useState('')
  const [areaSearchDeferred, setAreaSearchDeferred] = useState('')
  const [areaFreqFilter, setAreaFreqFilter] = useState<ResetFrequency | null>(null)

  useEffect(() => {
    const timer = setTimeout(() => setAreaSearchDeferred(areaSearch), 200)
    return () => clearTimeout(timer)
  }, [areaSearch])

  // ── Task filter state ──────────────────────────────────────────────────────
  const [taskSearch, setTaskSearch] = useState('')
  const [taskSearchDeferred, setTaskSearchDeferred] = useState('')
  const [taskScheduleFilter, setTaskScheduleFilter] = useState<ScheduleType | null>(null)
  const [taskDayFilter, setTaskDayFilter] = useState<string | null>(null)

  useEffect(() => {
    const timer = setTimeout(() => setTaskSearchDeferred(taskSearch), 200)
    return () => clearTimeout(timer)
  }, [taskSearch])

  // Reset task filters when switching areas
  useEffect(() => {
    setTaskSearch('')
    setTaskSearchDeferred('')
    setTaskScheduleFilter(null)
    setTaskDayFilter(null)
  }, [selectedAreaId])

  // ── Derived data ───────────────────────────────────────────────────────────

  const selectedArea = areas.find((a) => a.id === selectedAreaId) ?? null

  const filteredAreas = areas.filter((area) => {
    const matchSearch = area.name
      .toLowerCase()
      .includes(areaSearchDeferred.toLowerCase().trim())
    const matchFreq = areaFreqFilter === null || area.resetFrequency === areaFreqFilter
    return matchSearch && matchFreq
  })

  const hasAreaFilter = areaSearch !== '' || areaFreqFilter !== null

  // All tasks for the selected area, filtered
  const filteredDayOfWeekTasks = selectedArea
    ? [...selectedArea.tasks]
        .filter((t) => {
          if (t.scheduleType !== 'DAY_OF_WEEK') return false
          const matchSearch = t.title
            .toLowerCase()
            .includes(taskSearchDeferred.toLowerCase().trim())
          const matchSchedule =
            taskScheduleFilter === null || taskScheduleFilter === 'DAY_OF_WEEK'
          const matchDay = taskDayFilter === null || t.dayOfWeek === taskDayFilter
          return matchSearch && matchSchedule && matchDay
        })
        .sort((a, b) => {
          const dayDiff =
            DAY_ORDER.indexOf(a.dayOfWeek ?? '') - DAY_ORDER.indexOf(b.dayOfWeek ?? '')
          return dayDiff !== 0 ? dayDiff : a.orderIndex - b.orderIndex
        })
    : []

  const filteredDayOfMonthTasks = selectedArea
    ? [...selectedArea.tasks]
        .filter((t) => {
          if (t.scheduleType !== 'DAY_OF_MONTH') return false
          const matchSearch = t.title
            .toLowerCase()
            .includes(taskSearchDeferred.toLowerCase().trim())
          const matchSchedule =
            taskScheduleFilter === null || taskScheduleFilter === 'DAY_OF_MONTH'
          return matchSearch && matchSchedule
        })
        .sort((a, b) =>
          (a.dayOfMonth ?? 0) - (b.dayOfMonth ?? 0) || a.orderIndex - b.orderIndex,
        )
    : []

  const hasTaskFilter = taskSearch !== '' || taskScheduleFilter !== null || taskDayFilter !== null
  const hasFilteredTasks = filteredDayOfWeekTasks.length > 0 || filteredDayOfMonthTasks.length > 0
  const hasAnyTasks =
    (selectedArea?.tasks.length ?? 0) > 0

  // ── 404 = no active routine ───────────────────────────────────────────────

  const isNoRoutine =
    (error as { response?: { status?: number } } | null)?.response?.status === 404

  if (isLoading) {
    return (
      <div className="space-y-3 pt-2">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-12 rounded-xl bg-[#141416] animate-pulse" />
        ))}
      </div>
    )
  }

  if (isNoRoutine) return <EmptyRoutineState />

  // ── Handlers — Areas ─────────────────────────────────────────────────────

  const handleSaveArea = (
    name: string,
    color: string,
    icon: string,
    resetFrequency: ResetFrequency,
  ) => {
    if (areaModal.area) {
      updateArea.mutate(
        { id: areaModal.area.id, data: { name, color, icon, resetFrequency } },
        { onSuccess: () => setAreaModal({ open: false }) },
      )
    } else {
      createArea.mutate(
        { name, color, icon, resetFrequency },
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
    scheduleType: ScheduleType,
    dayOfWeek: string | null,
    dayOfMonth: number | null,
    reminderTime: string | null,
    goalType: GoalType,
    goalTarget: number | null,
    goalUnit: string | null,
    icon: string | null,
    color: string | null,
  ) => {
    if (!selectedArea) return
    const data = { title, description, estimatedMinutes, scheduleType, dayOfWeek, dayOfMonth, reminderTime, goalType, goalTarget, goalUnit, icon, color }
    if (taskModal.task) {
      updateTask.mutate(
        { areaId: selectedArea.id, taskId: taskModal.task.id, data },
        { onSuccess: () => setTaskModal({ open: false }) },
      )
    } else {
      createTask.mutate(
        { areaId: selectedArea.id, data },
        { onSuccess: () => setTaskModal({ open: false }) },
      )
    }
  }

  const handleDeleteTask = (task: TaskResponse) => {
    if (!selectedArea) return
    setDeleteTarget({
      open: true,
      type: 'task',
      areaId: selectedArea.id,
      id: task.id,
      label: task.title,
    })
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

  return (
    <>
      <div className="mb-6">
        {/* H1 matches the page-title pattern used across the app (text-3xl font-light) */}
        <h1 className="text-3xl font-light text-[#F4F2EF] tracking-tight">Gerenciar Rotina</h1>
        <p className="text-sm text-[#8C8A88] mt-1">
          Adicione, edite ou remova áreas e tarefas da sua rotina.
        </p>
      </div>

      <div className="flex flex-col md:grid md:grid-cols-[280px_1fr] gap-4 md:gap-6">

        {/* ── Left panel: Areas list ── */}
        <div className={selectedArea ? 'hidden md:block' : 'block'}>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-[#8C8A88] uppercase tracking-widest">
              Áreas
            </span>
            <button
              onClick={() => setAreaModal({ open: true })}
              className="flex items-center gap-1 text-xs text-[#2F8BFF] hover:text-[#4F9DFF] font-medium transition-colors"
            >
              <Plus size={13} />
              Nova Área
            </button>
          </div>

          {/* Area filter bar */}
          {areas.length > 0 && (
            <div className="mb-3">
              <FilterBar
                search={areaSearch}
                onSearchChange={setAreaSearch}
                placeholder="Buscar área…"
              >
                <FilterPills
                  options={FREQ_FILTER_OPTIONS}
                  selected={areaFreqFilter}
                  onSelect={setAreaFreqFilter}
                />
              </FilterBar>
            </div>
          )}

          {areas.length === 0 ? (
            <div className="py-8 text-center text-sm text-[#8C8A88]">
              Nenhuma área encontrada.{' '}
              <button
                onClick={() => setAreaModal({ open: true })}
                className="text-[#2F8BFF] hover:underline"
              >
                Criar primeira área
              </button>
            </div>
          ) : filteredAreas.length === 0 ? (
            /* Empty state after filter */
            <div className="py-8 flex flex-col items-center gap-3 text-center">
              <SearchX size={32} className="text-[#34343A]" />
              <p className="text-sm text-[#8C8A88]">Nenhuma área encontrada</p>
              {hasAreaFilter && (
                <button
                  onClick={() => {
                    setAreaSearch('')
                    setAreaFreqFilter(null)
                  }}
                  className="text-xs text-[#2F8BFF] hover:underline"
                >
                  Limpar filtros
                </button>
              )}
            </div>
          ) : (
            <div className="space-y-1">
              {filteredAreas.map((area) => (
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
            <p className="text-sm text-[#34343A] text-center">
              Selecione uma área para gerenciar suas tarefas.
            </p>
          ) : (
            <div>
              {/* Mobile back button */}
              <button
                onClick={() => setSelectedAreaId(null)}
                className="md:hidden flex items-center gap-1.5 text-sm text-[#8C8A88] hover:text-[#F4F2EF] mb-4 transition-colors"
              >
                <ArrowLeft size={15} />
                Voltar para áreas
              </button>

              {/* Area header */}
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div
                    className="w-7 h-7 rounded-full flex items-center justify-center shrink-0"
                    style={{
                      backgroundColor: selectedArea.color + '22',
                      border: `2px solid ${selectedArea.color}`,
                      color: selectedArea.color,
                    }}
                  >
                    <DynamicIcon name={selectedArea.icon} size={15} fallback="folder" />
                  </div>
                  <span className="text-sm font-semibold text-[#F4F2EF]">
                    {selectedArea.name}
                  </span>
                </div>
                <button
                  onClick={() => setTaskModal({ open: true })}
                  className="flex items-center gap-1 text-xs text-[#2F8BFF] hover:text-[#4F9DFF] font-medium transition-colors"
                >
                  <Plus size={13} />
                  Nova Tarefa
                </button>
              </div>

              {/* Task filter bar (only when there are tasks) */}
              {hasAnyTasks && (
                <div className="mb-3">
                  <FilterBar
                    search={taskSearch}
                    onSearchChange={setTaskSearch}
                    placeholder="Buscar tarefa…"
                  >
                    <FilterPills
                      options={SCHEDULE_FILTER_OPTIONS}
                      selected={taskScheduleFilter}
                      onSelect={(v) => {
                        setTaskScheduleFilter(v)
                        setTaskDayFilter(null)
                      }}
                    />
                    {/* Day-of-week pills — only visible when DAY_OF_WEEK filter active */}
                    {taskScheduleFilter === 'DAY_OF_WEEK' && (
                      <FilterPills
                        options={DAY_FILTER_OPTIONS}
                        selected={taskDayFilter}
                        onSelect={setTaskDayFilter}
                      />
                    )}
                  </FilterBar>
                </div>
              )}

              {/* Tasks list */}
              {!hasAnyTasks ? (
                <div className="py-8 text-center text-sm text-[#8C8A88]">
                  Nenhuma tarefa nesta área.{' '}
                  <button
                    onClick={() => setTaskModal({ open: true })}
                    className="text-[#2F8BFF] hover:underline"
                  >
                    Adicionar tarefa
                  </button>
                </div>
              ) : !hasFilteredTasks ? (
                /* Empty state after filter */
                <div className="py-8 flex flex-col items-center gap-3 text-center">
                  <SearchX size={32} className="text-[#34343A]" />
                  <p className="text-sm text-[#8C8A88]">Nenhuma tarefa encontrada</p>
                  {hasTaskFilter && (
                    <button
                      onClick={() => {
                        setTaskSearch('')
                        setTaskScheduleFilter(null)
                        setTaskDayFilter(null)
                      }}
                      className="text-xs text-[#2F8BFF] hover:underline"
                    >
                      Limpar filtros
                    </button>
                  )}
                </div>
              ) : (
                <div className="space-y-0.5">
                  {/* DAY_OF_WEEK tasks grouped by weekday */}
                  {DAYS_OF_WEEK.filter((d) =>
                    filteredDayOfWeekTasks.some((t) => t.dayOfWeek === d.value),
                  ).map((day) => {
                    const dayTasks = filteredDayOfWeekTasks.filter(
                      (t) => t.dayOfWeek === day.value,
                    )
                    return (
                      <div key={day.value} className="mb-3">
                        <div className="px-3 mb-1">
                          <span className="text-[10px] font-semibold text-[#8C8A88] uppercase tracking-widest">
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
                  })}

                  {/* DAY_OF_MONTH tasks */}
                  {filteredDayOfMonthTasks.length > 0 && (
                    <div className="mb-3">
                      <div className="px-3 mb-1">
                        <span className="text-[10px] font-semibold text-[#8C8A88] uppercase tracking-widest">
                          Mensal
                        </span>
                      </div>
                      {filteredDayOfMonthTasks.map((task) => (
                        <TaskManageRow
                          key={task.id}
                          task={task}
                          onEdit={() => setTaskModal({ open: true, task })}
                          onDelete={() => handleDeleteTask(task)}
                        />
                      ))}
                    </div>
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
