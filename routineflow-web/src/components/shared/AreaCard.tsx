import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'
import { TaskItem } from './TaskItem'
import type { EnrichedArea } from '@/hooks/useDay'

interface AreaCardProps {
  area: EnrichedArea
  onTaskToggle: (taskId: number, completed: boolean) => void
  disabled?: boolean
}

export function AreaCard({ area, onTaskToggle, disabled = false }: AreaCardProps) {
  const completedTasks = area.tasks.filter((t) => t.completed).length
  const totalTasks = area.tasks.length
  const completionRate = totalTasks > 0 ? completedTasks / totalTasks : 0

  // Start expanded unless 100% done
  const [expanded, setExpanded] = useState(completionRate < 1.0)

  return (
    <div
      className="rounded-xl bg-[#141414] border-l-4 overflow-hidden"
      style={{ borderLeftColor: area.color }}
    >
      {/* Card header — always visible */}
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="w-full flex items-center gap-3 px-4 py-3.5 text-left hover:bg-[#1a1a1a] transition-colors"
        aria-expanded={expanded}
      >
        {/* Icon + name */}
        <span className="text-lg shrink-0" aria-hidden>
          {area.icon}
        </span>
        <span className="flex-1 text-sm font-medium text-[#f5f5f7]">{area.name}</span>

        {/* Count badge */}
        <span className="text-xs text-[#86868b] shrink-0">
          {completedTasks}/{totalTasks}
        </span>

        {/* Mini progress bar */}
        <div className="w-16 h-1.5 rounded-full bg-[#1f1f1f] shrink-0 overflow-hidden">
          <div
            className="h-full rounded-full transition-all duration-500"
            style={{
              width: `${completionRate * 100}%`,
              backgroundColor: area.color,
            }}
          />
        </div>

        {/* Chevron */}
        <ChevronDown
          size={15}
          className={cn(
            'text-[#86868b] shrink-0 transition-transform duration-200',
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
          {area.tasks.map((task, idx) => (
            <TaskItem
              key={task.id}
              task={task}
              areaColor={area.color}
              onToggle={onTaskToggle}
              isLast={idx === area.tasks.length - 1}
              disabled={disabled}
            />
          ))}
        </div>
      </div>

      {/* Completion message when 100% */}
      {completionRate === 1.0 && (
        <div
          className="overflow-hidden transition-all duration-300 ease-in-out"
          style={{ maxHeight: expanded ? '0px' : '60px' }}
        >
          <p className="px-4 pb-3 text-xs text-[#86868b]">
            ✓ Área concluída
          </p>
        </div>
      )}

    </div>
  )
}
