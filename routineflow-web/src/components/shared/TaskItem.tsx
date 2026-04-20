import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { EnrichedTask } from '@/hooks/useToday'

interface TaskItemProps {
  task: EnrichedTask
  areaColor: string
  onToggle: (taskId: number, completed: boolean) => void
  isLast?: boolean
}

export function TaskItem({ task, areaColor, onToggle, isLast = false }: TaskItemProps) {
  function handleClick() {
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
        aria-label={task.completed ? 'Desmarcar tarefa' : 'Marcar como concluída'}
        className="mt-0.5 shrink-0 w-5 h-5 rounded-md flex items-center justify-center transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-1 focus-visible:ring-offset-[#141414]"
        style={{
          border: task.completed ? 'none' : `1.5px solid ${areaColor}`,
          backgroundColor: task.completed ? areaColor : 'transparent',
          // ring color matches area
          '--tw-ring-color': areaColor,
        } as React.CSSProperties}
      >
        {task.completed && <Check size={12} strokeWidth={2.5} className="text-white" />}
      </button>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span
            className={cn(
              'text-sm font-medium leading-snug transition-all duration-200',
              task.completed
                ? 'line-through text-[#86868b]'
                : 'text-[#f5f5f7]',
            )}
          >
            {task.title}
          </span>

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
      </div>
    </div>
  )
}
