import { useMemo } from 'react'
import { getMotivationalMessage } from '@/lib/motivationalMessages'
import { StreakBadge } from './StreakBadge'
import { formatPercent } from '@/lib/utils'

interface DashboardCardProps {
  overallRate: number
  totalTasks: number
  doneTasks: number
  bestStreak?: number
}

export function DashboardCard({ overallRate, totalTasks, doneTasks, bestStreak = 0 }: DashboardCardProps) {
  const message = useMemo(() => getMotivationalMessage(overallRate), [overallRate])

  // Simple SVG Ring Math
  const size = 64
  const strokeWidth = 6
  const radius = (size - strokeWidth) / 2
  const circumference = radius * 2 * Math.PI
  const strokeDashoffset = circumference - overallRate * circumference

  return (
    <div className="bg-surface-2 border border-line rounded-xl p-5 mb-6 flex items-center justify-between gap-4">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-3 mb-1">
          <h2 className="text-lg font-medium text-[#F4F2EF] tracking-tight">
            Progresso Diário
          </h2>
          <StreakBadge streak={bestStreak} />
        </div>
        <p className="text-sm text-fg-dim truncate">
          {doneTasks} de {totalTasks} tarefas concluídas
        </p>
        <p className="text-xs text-brand mt-2 font-medium">
          {message}
        </p>
      </div>

      {/* Ring Chart */}
      <div className="relative shrink-0 flex items-center justify-center" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="transform -rotate-90">
          {/* Background Ring */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="transparent"
            stroke="#26262A"
            strokeWidth={strokeWidth}
          />
          {/* Progress Ring */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="transparent"
            stroke="#2F8BFF"
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            className="transition-all duration-700 ease-out"
          />
        </svg>
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-[13px] font-bold text-[#F4F2EF]">
            {formatPercent(overallRate)}
          </span>
        </div>
      </div>
    </div>
  )
}
