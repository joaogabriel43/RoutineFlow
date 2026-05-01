import { useState } from 'react'
import { X } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { SingleTaskResponse } from '@/types'

interface SingleTaskItemProps {
  task: SingleTaskResponse
  onComplete:   (id: number) => void
  onUncomplete: (id: number) => void
  onDelete:     (id: number) => void
  /** When true the item is in the archived list — show uncomplete instead of complete */
  archived?: boolean
}

// ── Date helpers ──────────────────────────────────────────────────────────────

function formatDueDate(dueDate: string): string {
  const d = new Date(dueDate + 'T00:00:00')
  return new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' }).format(d)
}

// ── Component ─────────────────────────────────────────────────────────────────

export function SingleTaskItem({
  task,
  onComplete,
  onUncomplete,
  onDelete,
  archived = false,
}: SingleTaskItemProps) {
  const [fading, setFading] = useState(false)

  function handleCheck() {
    if (archived) {
      onUncomplete(task.id)
      return
    }
    // Fade-out animation before calling parent
    setFading(true)
    setTimeout(() => onComplete(task.id), 280)
  }

  return (
    <div
      className={cn(
        'flex items-center gap-3 py-2.5 px-3 rounded-lg group transition-opacity duration-[280ms]',
        fading ? 'opacity-0' : 'opacity-100',
      )}
    >
      {/* Circular checkbox */}
      <button
        type="button"
        onClick={handleCheck}
        aria-label={archived ? 'Desfazer conclusão' : 'Marcar como concluída'}
        className={cn(
          'shrink-0 h-5 w-5 rounded-full border-2 flex items-center justify-center transition-colors cursor-pointer',
          archived
            ? 'border-[#0071e3] bg-[#0071e3]'
            : 'border-[#3a3a3a] hover:border-[#0071e3]',
        )}
      >
        {archived && (
          <svg
            viewBox="0 0 10 8"
            fill="none"
            className="w-2.5 h-2"
            aria-hidden="true"
          >
            <path
              d="M1 4l2.5 2.5L9 1"
              stroke="white"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        )}
      </button>

      {/* Title + metadata */}
      <div className="flex-1 min-w-0">
        <p
          className={cn(
            'text-sm truncate',
            archived ? 'text-[#86868b] line-through' : 'text-[#f5f5f7]',
          )}
        >
          {task.title}
        </p>

        {/* Due date + overdue badge */}
        {task.dueDate && (
          <div className="flex items-center gap-1.5 mt-0.5">
            <span
              className={cn(
                'text-[11px]',
                task.isOverdue && !archived ? 'text-red-400' : 'text-[#86868b]',
              )}
            >
              {formatDueDate(task.dueDate)}
            </span>
            {task.isOverdue && !archived && (
              <span className="text-[10px] font-medium px-1.5 py-0 rounded-full bg-red-500/15 text-red-400">
                Atrasada
              </span>
            )}
          </div>
        )}
      </div>

      {/* Delete button — visible on hover */}
      <button
        type="button"
        onClick={() => onDelete(task.id)}
        aria-label="Excluir tarefa"
        className="shrink-0 opacity-0 group-hover:opacity-100 transition-opacity text-[#3a3a3a] hover:text-red-400 cursor-pointer"
      >
        <X size={15} />
      </button>
    </div>
  )
}
