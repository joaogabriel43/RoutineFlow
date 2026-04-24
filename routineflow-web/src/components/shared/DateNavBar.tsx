import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { cn } from '@/lib/utils'
import type { DailyProgressResponse } from '@/types'

// ── Constants ─────────────────────────────────────────────────────────────────

const DAY_ABBR = ['Do', 'Se', 'Te', 'Qu', 'Qu', 'Se', 'Sá']

// ── Helpers ───────────────────────────────────────────────────────────────────

function todayStr(): string {
  return new Date().toISOString().split('T')[0] as string
}

/** Build an array of YYYY-MM-DD strings: 7 days before today … today … 6 days after */
function buildDateRange(): string[] {
  const dates: string[] = []
  const today = new Date()
  for (let offset = -7; offset <= 6; offset++) {
    const d = new Date(today)
    d.setDate(today.getDate() + offset)
    dates.push(d.toISOString().split('T')[0] as string)
  }
  return dates
}

// ── DateNavBar ────────────────────────────────────────────────────────────────

interface DateNavBarProps {
  selectedDate: string
  onSelect: (date: string) => void
}

export function DateNavBar({ selectedDate, onSelect }: DateNavBarProps) {
  const today = todayStr()
  const dates = buildDateRange()
  const queryClient = useQueryClient()
  const todayRef = useRef<HTMLButtonElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  // Scroll today pill into view on mount
  useEffect(() => {
    if (todayRef.current && containerRef.current) {
      todayRef.current.scrollIntoView({
        behavior: 'instant',
        block: 'nearest',
        inline: 'center',
      })
    }
  }, [])

  function hasCompletions(dateStr: string): boolean {
    const cached = queryClient.getQueryData<DailyProgressResponse>([
      'day-progress',
      dateStr,
    ])
    return (cached?.overallCompletionRate ?? 0) > 0
  }

  return (
    <div
      ref={containerRef}
      className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none -mx-4 px-4 mb-5"
      style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' } as React.CSSProperties}
    >
      {dates.map((dateStr) => {
        const d = new Date(dateStr + 'T12:00:00')
        const dayIdx = d.getDay()
        const dayNum = d.getDate()
        const isToday = dateStr === today
        const isFuture = dateStr > today
        const isSelected = dateStr === selectedDate
        const hasDot = !isFuture && hasCompletions(dateStr)

        return (
          <button
            key={dateStr}
            ref={isToday ? todayRef : undefined}
            type="button"
            onClick={() => onSelect(dateStr)}
            className={cn(
              'flex flex-col items-center shrink-0 w-11 py-2 rounded-xl transition-all duration-150',
              'focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0071e3]',
              isSelected
                ? isToday
                  ? 'bg-[#0071e3] text-white'
                  : 'bg-[#1f1f1f] text-[#f5f5f7] ring-1 ring-[#3a3a3c]'
                : isFuture
                  ? 'text-[#3a3a3c] hover:bg-[#141414]'
                  : 'text-[#86868b] hover:bg-[#141414] hover:text-[#f5f5f7]',
            )}
          >
            {/* Abbreviated day name */}
            <span className="text-[10px] font-medium tracking-wide uppercase leading-none mb-1">
              {DAY_ABBR[dayIdx]}
            </span>

            {/* Day number */}
            <span className={cn('text-sm font-semibold leading-none', isToday && isSelected && 'text-white')}>
              {dayNum}
            </span>

            {/* Completion dot */}
            <span
              className={cn(
                'mt-1.5 h-1 w-1 rounded-full transition-opacity',
                hasDot ? 'opacity-100 bg-[#30d158]' : 'opacity-0 bg-transparent',
              )}
            />
          </button>
        )
      })}
    </div>
  )
}
