import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { SingleTaskResponse } from '@/types'

interface SingleTaskItemProps {
  task: SingleTaskResponse
  onComplete:   (id: number) => void
  onUncomplete: (id: number) => void
  onDelete:     (id: number) => void
  /** When true the item is in the archived list — show undo instead of complete */
  archived?: boolean
}

// ── Date helpers ──────────────────────────────────────────────────────────────

function formatDueDate(dateStr: string): string {
  const d = new Date(dateStr + 'T12:00:00')
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
}

function formatCompletedAt(isoStr: string): string {
  const d = new Date(isoStr)
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
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

  function handleComplete() {
    if (archived) return
    setFading(true)
    setTimeout(() => onComplete(task.id), 280)
  }

  function handleUncomplete() {
    setFading(true)
    setTimeout(() => onUncomplete(task.id), 280)
  }

  return (
    <div
      className={cn(
        'flex items-start gap-3 py-3 px-1 group transition-all duration-300 ease-in-out',
        fading && 'opacity-0 max-h-0 overflow-hidden py-0',
      )}
    >
      {/* Circular checkbox */}
      <button
        type="button"
        onClick={archived ? undefined : handleComplete}
        aria-label={archived ? 'Concluída' : 'Marcar como concluída'}
        className={cn(
          'mt-0.5 shrink-0 w-5 h-5 rounded-full flex items-center justify-center cursor-pointer',
          'transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0071e3]',
          archived
            ? 'bg-[#30d158] border-none cursor-default'
            : 'border-[1.5px] border-[#3a3a3c] hover:border-[#30d158]',
        )}
      >
        {archived && <Check size={11} strokeWidth={3} className="text-white" />}
      </button>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span
            className={cn(
              'text-sm font-medium leading-snug',
              archived ? 'line-through text-[#86868b]' : 'text-[#f5f5f7]',
            )}
          >
            {task.title}
          </span>

          {task.isOverdue && !archived && (
            <span className="shrink-0 text-[10px] font-medium text-red-400 uppercase tracking-wide">
              Atrasada
            </span>
          )}

          {task.dueDate && !task.isOverdue && !archived && (
            <span className="shrink-0 text-xs text-[#86868b]">
              até {formatDueDate(task.dueDate)}
            </span>
          )}
        </div>

        {task.description && (
          <p className="text-xs text-[#86868b] mt-0.5 leading-relaxed">{task.description}</p>
        )}

        {archived && task.completedAt && (
          <p className="text-xs text-[#86868b] mt-0.5">
            Concluída em {formatCompletedAt(task.completedAt)}
          </p>
        )}
      </div>

      {/* Actions — visible on hover */}
      <div className="shrink-0 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        {archived && (
          <button
            type="button"
            onClick={handleUncomplete}
            className="text-xs text-[#86868b] hover:text-[#f5f5f7] px-2 py-1 rounded hover:bg-[#1f1f1f] transition-colors cursor-pointer"
          >
            Desfazer
          </button>
        )}

        {!archived && (
          <button
            type="button"
            onClick={() => onDelete(task.id)}
            aria-label="Excluir tarefa"
            className="p-1 rounded text-[#3a3a3c] hover:text-red-400 hover:bg-[#1f1f1f] transition-colors cursor-pointer"
          >
            <X size={14} />
          </button>
        )}
      </div>
    </div>
  )
}
