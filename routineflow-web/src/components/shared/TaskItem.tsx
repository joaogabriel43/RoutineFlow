import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { EnrichedTask } from '@/hooks/useDay'

interface TaskItemProps {
  task: EnrichedTask
  areaColor: string
  onToggle: (taskId: number, completed: boolean, notes?: string) => void
  isLast?: boolean
  disabled?: boolean
  isActiveTimer?: boolean
  onUpdateNotes?: (taskId: number, notes: string) => void
}

import { TaskTimer } from '@/components/shared/TaskTimer'
import { NoteInput } from '@/components/shared/NoteInput'

export function TaskItem({
  task,
  areaColor,
  onToggle,
  isLast = false,
  disabled = false,
  isActiveTimer = false,
  onToggleTimer,
  onUpdateNotes,
}: TaskItemProps) {
  function handleClick() {
    if (disabled) return
    onToggle(task.id, !task.completed)
  }

  return (
    <div
      className={cn(
        'flex items-start gap-3 py-3',
        !isLast && 'border-b border-[#1f1f1f]',
      )}
    >
      {/* Custom checkbox */}
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
        className="mt-0.5 shrink-0 w-5 h-5 rounded-md flex items-center justify-center transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-1 focus-visible:ring-offset-[#141414] disabled:cursor-not-allowed disabled:opacity-40"
        style={{
          border: task.completed ? 'none' : `1.5px solid ${disabled ? '#3a3a3c' : areaColor}`,
          backgroundColor: task.completed ? (disabled ? '#3a3a3c' : areaColor) : 'transparent',
          '--tw-ring-color': areaColor,
        } as React.CSSProperties}
      >
        {task.completed && <Check size={12} strokeWidth={2.5} className="text-white" />}
      </button>

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
              'text-sm font-medium leading-snug transition-all duration-200 text-left',
              task.completed
                ? 'line-through text-[#86868b] cursor-default'
                : disabled ? 'text-[#86868b] cursor-not-allowed' : 'text-[#f5f5f7] hover:text-[#0071e3] cursor-pointer',
            )}
          >
            {task.title}
          </button>

          {/* Time pill */}
          {task.estimatedMinutes != null && task.estimatedMinutes > 0 && (
            <span className="shrink-0 text-xs bg-[#1f1f1f] text-[#86868b] px-2 py-0.5 rounded-full">
              {task.estimatedMinutes} min
            </span>
          )}
        </div>

        {task.description && (
          <p className="text-xs text-[#86868b] mt-0.5 leading-relaxed">{task.description}</p>
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
              <p className="text-xs text-[#86868b] italic mt-2 leading-relaxed whitespace-pre-wrap">
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
