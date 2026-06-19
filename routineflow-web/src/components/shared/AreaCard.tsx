import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'
import { DynamicIcon } from './DynamicIcon'
import { TaskItem } from './TaskItem'
import { SkipDayModal } from './SkipDayModal'
import { skipDaysApi } from '@/services/api'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { EnrichedArea } from '@/hooks/useDay'

interface AreaCardProps {
  area: EnrichedArea
  onTaskToggle: (taskId: number, completed: boolean) => void
  disabled?: boolean
  activeTimerTaskId?: number | null
  onToggleTimer?: (taskId: number) => void
  onUpdateNotes?: (taskId: number, notes: string) => void
  onIncrementProgress?: (taskId: number, increment: number, target: number) => void
  onResetProgress?: (taskId: number) => void
  selectedDate: string
  onRefetchProgress?: () => void
}

export function AreaCard({
  area,
  onTaskToggle,
  disabled = false,
  activeTimerTaskId = null,
  onToggleTimer,
  onUpdateNotes,
  onIncrementProgress,
  onResetProgress,
  selectedDate,
  onRefetchProgress,
}: AreaCardProps) {
  const completedTasks = area.tasks.filter((t) => t.completed).length
  const totalTasks = area.tasks.length
  const completionRate = totalTasks > 0 ? completedTasks / totalTasks : 0

  // Start expanded unless 100% done
  const [expanded, setExpanded] = useState(completionRate < 1.0)
  const [skipModalOpen, setSkipModalOpen] = useState(false)

  const queryClient = useQueryClient()

  const unskipMutation = useMutation({
    mutationFn: () => skipDaysApi.removeSkipDay(area.id, selectedDate),
    onSuccess: () => {
      toast.success(`Skip day removido de ${area.name}`)
      queryClient.invalidateQueries({ queryKey: ['day-progress'] })
      queryClient.invalidateQueries({ queryKey: ['heatmap'] })
      onRefetchProgress?.()
    },
    onError: () => toast.error('Erro ao remover skip day'),
  })

  return (
    <div
      className="rounded-lg bg-surface-2 border border-line border-l-[3px] overflow-hidden"
      style={{ borderLeftColor: area.color }}
    >
      {/* Card header — always visible */}
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="w-full flex items-center gap-3 px-4 py-3.5 text-left hover:bg-surface-3 transition-colors duration-200"
        aria-expanded={expanded}
      >
        {/* Icon + name */}
        <span className="shrink-0" aria-hidden>
          <DynamicIcon name={area.icon} color={area.color} size={20} fallback="folder" />
        </span>
        <span className="flex-1 text-[15px] font-medium text-fg tracking-[-0.006em]">{area.name}</span>

        {/* Count badge */}
        <span className="num text-xs text-fg-lo shrink-0">
          {completedTasks}/{totalTasks}
        </span>

        {/* Mini progress bar or Skip Badge */}
        {area.isSkippedToday ? (
          <span className="text-[10px] font-semibold tracking-wider uppercase bg-amber-500/20 text-amber-500 px-2 py-0.5 rounded-full shrink-0">
            Skipped
          </span>
        ) : (
          <div className="w-16 h-1.5 rounded-full bg-surface-1 shrink-0 overflow-hidden">
            <div
              className="h-full rounded-full transition-all duration-700 ease-out"
              style={{
                width: `${completionRate * 100}%`,
                backgroundColor: area.color,
              }}
            />
          </div>
        )}

        {/* Chevron */}
        <ChevronDown
          size={15}
          className={cn(
            'text-fg-lo shrink-0 transition-transform duration-200',
            expanded && 'rotate-180',
          )}
        />
      </button>

      {/* Task list — max-height animation */}
      <div
        className="overflow-hidden transition-all duration-300 ease-in-out"
        style={{ maxHeight: expanded ? '600px' : '0px' }}
        aria-hidden={!expanded}
      >
        <div className="px-4 pb-1">
          {area.isSkippedToday ? (
            <div className="py-6 text-center">
              <p className="text-amber-500 text-sm font-medium mb-1">Dia Pulado</p>
              <p className="text-xs text-fg-lo mb-4">Você está no modo férias para esta área hoje.</p>
              <button
                type="button"
                onClick={() => unskipMutation.mutate()}
                disabled={unskipMutation.isPending || disabled}
                className="text-xs font-medium text-fg bg-surface-1 hover:bg-surface-3 border border-line rounded-lg px-4 py-2 transition-colors disabled:opacity-50"
              >
                {unskipMutation.isPending ? 'Desfazendo...' : 'Desfazer Skip'}
              </button>
            </div>
          ) : (
            area.tasks.map((task, idx) => (
              <TaskItem
                key={task.id}
                task={task}
                areaColor={area.color}
                onToggle={onTaskToggle}
                isLast={idx === area.tasks.length - 1}
                disabled={disabled}
                isActiveTimer={activeTimerTaskId === task.id}
                onToggleTimer={onToggleTimer}
                onUpdateNotes={onUpdateNotes}
                onIncrementProgress={onIncrementProgress}
                onResetProgress={onResetProgress}
              />
            ))
          )}
        </div>
        
        {/* Skip button at the bottom of the list if not completed and not skipped */}
        {!area.isSkippedToday && completionRate < 1.0 && !disabled && (
          <div className="px-4 pb-3 flex justify-end">
            <button
              type="button"
              onClick={() => setSkipModalOpen(true)}
              className="text-[11px] font-medium text-fg-lo hover:text-amber-500 transition-colors"
            >
              Pular Dia
            </button>
          </div>
        )}
      </div>

      {/* Completion message when 100% */}
      {completionRate === 1.0 && !area.isSkippedToday && (
        <div
          className="overflow-hidden transition-all duration-300 ease-in-out"
          style={{ maxHeight: expanded ? '0px' : '60px' }}
        >
          <p className="px-4 pb-3 text-xs text-fg-lo">
            ✓ Área concluída
          </p>
        </div>
      )}

      <SkipDayModal
        open={skipModalOpen}
        onClose={() => setSkipModalOpen(false)}
        areaId={area.id}
        areaName={area.name}
        date={selectedDate}
        onSuccess={() => onRefetchProgress?.()}
      />
    </div>
  )
}
