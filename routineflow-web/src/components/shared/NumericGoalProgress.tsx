import { Check, Minus, Plus } from 'lucide-react'
import { cn } from '@/lib/utils'

interface NumericGoalProgressProps {
  taskId: number
  goalProgress: number
  goalTarget: number
  goalUnit: string
  completed: boolean
  disabled: boolean
  areaColor: string
  onIncrement: (increment: number) => void
  onReset: () => void
}

export function NumericGoalProgress({
  goalProgress,
  goalTarget,
  goalUnit,
  completed,
  disabled,
  areaColor,
  onIncrement,
  onReset,
}: NumericGoalProgressProps) {
  const percentage = Math.min(100, Math.max(0, (goalProgress / goalTarget) * 100))

  if (completed) {
    return (
      <button
        type="button"
        onClick={onReset}
        disabled={disabled}
        aria-label={disabled ? 'Check-in indisponível' : 'Desmarcar tarefa numérica'}
        className={cn(
          'mt-0.5 shrink-0 flex items-center gap-2 rounded-md px-2 py-1 transition-all duration-150',
          disabled ? 'opacity-50 cursor-not-allowed' : 'hover:opacity-80'
        )}
        style={{
          backgroundColor: disabled ? 'var(--bg-disabled)' : `${areaColor}15`,
        }}
      >
        <div
          className="w-5 h-5 rounded-xs flex items-center justify-center"
          style={{ backgroundColor: disabled ? 'var(--text-disabled)' : areaColor }}
        >
          <Check size={12} strokeWidth={2.5} className="text-white" />
        </div>
        <span
          className="text-sm font-medium tracking-tight"
          style={{ color: disabled ? 'var(--text-disabled)' : areaColor }}
        >
          <span className="num">{goalTarget}</span> / <span className="num">{goalTarget}</span>{' '}
          {goalUnit}
        </span>
      </button>
    )
  }

  return (
    <div className="mt-0.5 shrink-0 flex flex-col items-center gap-1.5 w-[110px]">
      <div className="flex items-center justify-between w-full bg-surface-2 rounded-md p-1 border border-line-subtle relative overflow-hidden">
        {/* Progress bar background */}
        <div
          className="absolute left-0 top-0 bottom-0 transition-all duration-300 opacity-20"
          style={{ backgroundColor: areaColor, width: `${percentage}%` }}
        />
        
        <button
          type="button"
          onClick={() => onIncrement(-1)}
          disabled={disabled || goalProgress <= 0}
          aria-label="Decrementar progresso"
          className="relative z-10 w-6 h-6 flex items-center justify-center rounded-sm hover:bg-surface-3 disabled:opacity-30 disabled:hover:bg-transparent"
        >
          <Minus size={14} className="text-fg-subtle" />
        </button>

        <span className="relative z-10 text-xs font-semibold text-fg tracking-tight flex-1 text-center select-none">
          <span className="num">{goalProgress}</span> / <span className="num">{goalTarget}</span>
        </span>

        <button
          type="button"
          onClick={() => onIncrement(1)}
          disabled={disabled}
          aria-label="Incrementar progresso"
          className="relative z-10 w-6 h-6 flex items-center justify-center rounded-sm hover:bg-surface-3 disabled:opacity-30 disabled:hover:bg-transparent"
        >
          <Plus size={14} className="text-fg-subtle" />
        </button>
      </div>
      
      {goalUnit && (
        <span className="text-[10px] uppercase tracking-wider font-semibold text-fg-lo">
          {goalUnit}
        </span>
      )}
    </div>
  )
}
