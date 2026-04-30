import { Pencil, Trash2 } from 'lucide-react'
import type { TaskResponse } from '@/types'

const DAY_SHORT: Record<string, string> = {
  MONDAY: 'Seg',
  TUESDAY: 'Ter',
  WEDNESDAY: 'Qua',
  THURSDAY: 'Qui',
  FRIDAY: 'Sex',
  SATURDAY: 'Sáb',
  SUNDAY: 'Dom',
}

/** Returns a short schedule label for the badge column. */
function scheduleLabel(task: TaskResponse): string {
  if (task.scheduleType === 'DAY_OF_MONTH') {
    return `D${task.dayOfMonth}`
  }
  if (task.dayOfWeek) {
    return DAY_SHORT[task.dayOfWeek] ?? task.dayOfWeek.slice(0, 3)
  }
  return '—'
}

interface Props {
  task: TaskResponse
  onEdit: () => void
  onDelete: () => void
}

export function TaskManageRow({ task, onEdit, onDelete }: Props) {
  return (
    <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-[#141414] group transition-colors">
      {/* Schedule badge */}
      <span className="text-[10px] font-semibold text-[#86868b] w-7 shrink-0 text-center uppercase tracking-wide">
        {scheduleLabel(task)}
      </span>

      {/* Title */}
      <span className="flex-1 text-sm text-[#f5f5f7] truncate">{task.title}</span>

      {/* Duration */}
      {task.estimatedMinutes != null && (
        <span className="text-[11px] text-[#86868b] shrink-0">
          {task.estimatedMinutes}min
        </span>
      )}

      {/* Actions */}
      <div className="flex items-center gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={onEdit}
          className="p-1.5 rounded-lg text-[#86868b] hover:text-[#f5f5f7] hover:bg-[#2a2a2a] transition-colors"
          title="Editar tarefa"
        >
          <Pencil size={13} />
        </button>
        <button
          onClick={onDelete}
          className="p-1.5 rounded-lg text-[#86868b] hover:text-red-400 hover:bg-red-500/10 transition-colors"
          title="Excluir tarefa"
        >
          <Trash2 size={13} />
        </button>
      </div>
    </div>
  )
}
