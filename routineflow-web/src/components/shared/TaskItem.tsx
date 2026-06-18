import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { EnrichedTask } from '@/hooks/useDay'
import { TaskTimer } from '@/components/shared/TaskTimer'
import { NoteInput } from '@/components/shared/NoteInput'
import { NumericGoalProgress } from '@/components/shared/NumericGoalProgress'

interface TaskItemProps {
  task: EnrichedTask
  areaColor: string
  onToggle: (taskId: number, completed: boolean, notes?: string) => void
  isLast?: boolean
  disabled?: boolean
  isActiveTimer?: boolean
  onToggleTimer?: (taskId: number) => void
  onUpdateNotes?: (taskId: number, notes: string) => void
  onIncrementProgress?: (taskId: number, increment: number, target: number) => void
  onResetProgress?: (taskId: number) => void
}

export function TaskItem({
  task,
  areaColor,
  onToggle,
  isLast = false,
  disabled = false,
  isActiveTimer = false,
  onToggleTimer,
  onUpdateNotes,
  onIncrementProgress,
  onResetProgress,
}: TaskItemProps) {
  function handleClick() {
    if (disabled) return
    onToggle(task.id, !task.completed)
  }

  return (
    <div
      className={cn(
        'flex items-start gap-3 py-3',
        !isLast && 'border-b border-line-subtle',
      )}
    >
      {/* Checkbox or Numeric Progress */}
      {task.goalType === 'NUMERIC' ? (
        <NumericGoalProgress
          taskId={task.id}
          goalProgress={task.goalProgress ?? 0}
          goalTarget={task.goalTarget ?? 1}
          goalUnit={task.goalUnit ?? ''}
          completed={task.completed}
          disabled={disabled}
          areaColor={areaColor}
          onIncrement={(inc) => {
            if (onIncrementProgress) onIncrementProgress(task.id, inc, task.goalTarget ?? 1)
          }}
          onReset={() => {
            if (onResetProgress) onResetProgress(task.id)
          }}
        />
      ) : (
        <button
          type="button"
          onClick={handleClick}
          disabled={disabled}
          aria-label={
            disabled
              ? 'Check-in indisponível'
              : task.completed
                ? 'Desmarcar tarefa'
                : 'Marcar como concluída'
          }
          className="mt-0.5 shrink-0 w-5 h-5 rounded-xs flex items-center justify-center transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-1 focus-visible:ring-offset-surface-2 disabled:cursor-not-allowed disabled:opacity-40"
          style={{
            border: task.completed ? 'none' : `1.5px solid ${disabled ? 'var(--text-disabled)' : areaColor}`,
            backgroundColor: task.completed ? (disabled ? 'var(--text-disabled)' : areaColor) : 'transparent',
            '--tw-ring-color': areaColor,
          } as React.CSSProperties}
        >
          {task.completed && <Check size={12} strokeWidth={2.5} className="text-white" />}
        </button>
      )}

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <button
            type="button"
            disabled={disabled || task.completed}
            onClick={() => {
              if (onToggleTimer && !disabled && !task.completed) onToggleTimer(task.id)
            }}
            className={cn(
              'text-[15px] font-medium leading-snug transition-colors duration-200 text-left tracking-[-0.006em]',
              task.completed
                ? 'line-through text-fg-dim cursor-default'
                : disabled ? 'text-fg-lo cursor-not-allowed' : 'text-fg hover:text-brand cursor-pointer',
            )}
          >
            {task.title}
          </button>

          {/* Time pill */}
          {task.estimatedMinutes != null && task.estimatedMinutes > 0 && (
            <span className="shrink-0 text-xs bg-surface-3 text-fg-lo px-2 py-0.5 rounded-full">
              <span className="num">{task.estimatedMinutes}</span> min
            </span>
          )}
        </div>

        {task.description && (
          <p className="text-xs text-fg-lo mt-0.5 leading-relaxed">{task.description}</p>
        )}

        {/* Expanded Timer */}
        {isActiveTimer && !task.completed && (
          <div className="mt-3">
            <TaskTimer
              taskId={task.id}
              taskTitle={task.title}
              estimatedMinutes={task.estimatedMinutes ?? 0}
              onComplete={(noteString) => {
                onToggle(task.id, true, noteString)
                if (onToggleTimer) onToggleTimer(task.id)
              }}
            />
          </div>
        )}

        {/* Notes (Past or Completed) */}
        {task.completed && (
          disabled ? (
            task.notes && (
              <p className="text-xs text-fg-lo italic mt-2 leading-relaxed whitespace-pre-wrap">
                {task.notes}
              </p>
            )
          ) : (
            <NoteInput
              initialNote={task.notes}
              onSave={(notes) => onUpdateNotes?.(task.id, notes)}
              disabled={disabled}
            />
          )
        )}
      </div>
    </div>
  )
}
