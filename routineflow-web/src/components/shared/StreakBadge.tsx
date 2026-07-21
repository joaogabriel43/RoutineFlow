import { Flame, Star, Trophy } from 'lucide-react'
import { cn } from '@/lib/utils'

interface StreakBadgeProps {
  streak: number
  className?: string
}

export function StreakBadge({ streak, className }: StreakBadgeProps) {
  if (streak < 3) return null // Only show badges for meaningful streaks

  let Icon = Flame
  let colors = 'bg-orange-500/10 text-orange-500 border-orange-500/20'

  if (streak >= 30) {
    Icon = Trophy
    colors = 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20'
  } else if (streak >= 7) {
    Icon = Star
    colors = 'bg-purple-500/10 text-purple-500 border-purple-500/20'
  }

  return (
    <div
      className={cn(
        'inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[10px] font-semibold tracking-wide',
        colors,
        className
      )}
      title={`${streak} dias consecutivos`}
    >
      <Icon size={10} className="shrink-0" />
      <span>{streak} d</span>
    </div>
  )
}
