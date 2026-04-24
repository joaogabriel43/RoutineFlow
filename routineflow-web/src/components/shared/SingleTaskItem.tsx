import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { SingleTaskResponse } from '@/types'

interface SingleTaskItemProps {
  task: SingleTaskResponse
  onComplete: (id: number) => void
  onDelete?: (id: number) => void
  onUncomplete?: (id: number) => void
  showUndoInstead?: boolean   // true in archived view
  isLast?: boolean
}

export function SingleTaskItem({
  task,
  onComplete,
  onDelete,
  onUncomplete,
  showUndoInstead = false,
  isLast = false,
}: SingleTaskItemProps) {
  const [fading, setFading] = useState(false)

  function handleComplete() {
    if (showUndoInstead || task.completed) return
    setFading(true)
    setTimeout(() => onComplete(task.id), 280)
  }

  function handleUncomplete() {
    if (onUncomplete) {
      setFading(true)
      setTimeout(() => onUncomplete(task.id), 280)
    }
  }

  function formatDueDate(dateStr: string): string {
    const d = new Date(dateStr + 'T12:00:00')
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
  }

  function formatCompletedAt(isoStr: string): string {
    const d = new Date(isoStr)
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
  }

  return (
    <div
      className={cn(
        'flex items-start gap-3 py-3 group transition-all duration-300 ease-in-out',
        fading && 'opacity-0 max-h-0 overflow-hidden py-0',
        !isLast && 'border-b border-[#1f1f1f]',
      )}
    >
      {/* Circular checkbox */}
      {!showUndoInstead ? (
        <button
          type="button"
          onClick={handleComplete}
          aria-label={task.completed ? 'Concluída' : 'Marcar como concluída'}
          className={cn(
            'mt-0.5 shrink-0 w-5 h-5 rounded-full flex items-center justify-center',
            'transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0071e3]',
            task.completed
              ? 'bg-[#30d158] border-none'
              : 'border-[1.5px] border-[#3a3a3c] hover:border-[#30d158]',
          )}
        >
          {task.completed && <Check size={11} strokeWidth={3} className="text-white" />}
        </button>
      ) : (
        <div className="mt-0.5 shrink-0 w-5 h-5 rounded-full bg-[#30d158] flex items-center justify-center">
          <Check size={11} strokeWidth={3} className="text-white" />
        </div>
      )}

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span
            className={cn(
              'text-sm font-medium leading-snug',
              task.completed || showUndoInstead
                ? 'line-through text-[#86868b]'
                : 'text-[#f5f5f7]',
            )}
          >
            {task.title}
          </span>

          {task.isOverdue && !task.completed && (
            <span className="shrink-0 text-[10px] font-medium text-red-400 uppercase tracking-wide">
              Atrasada
            </span>
          )}

          {task.dueDate && !task.isOverdue && !task.completed && (
            <span className="shrink-0 text-xs text-[#86868b]">
              até {formatDueDate(task.dueDate)}
            </span>
          )}
        </div>

        {task.description && (
          <p className="text-xs text-[#86868b] mt-0.5 leading-relaxed">{task.description}</p>
        )}

        {showUndoInstead && task.completedAt && (
          <p className="text-xs text-[#86868b] mt-0.5">
            Concluída em {formatCompletedAt(task.completedAt)}
          </p>
        )}
      </div>

      {/* Actions */}
      <div className="shrink-0 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        {showUndoInstead && onUncomplete && (
          <button
            type="button"
            onClick={handleUncomplete}
            className="text-xs text-[#86868b] hover:text-[#f5f5f7] px-2 py-1 rounded hover:bg-[#1f1f1f] transition-colors"
          >
            Desfazer
          </button>
        )}

        {!showUndoInstead && onDelete && (
          <button
            type="button"
            onClick={() => onDelete(task.id)}
            aria-label="Excluir tarefa"
            className="p-1 rounded text-[#3a3a3c] hover:text-red-400 hover:bg-[#1f1f1f] transition-colors"
          >
            <X size={14} />
          </button>
        )}
      </div>
    </div>
  )
}
