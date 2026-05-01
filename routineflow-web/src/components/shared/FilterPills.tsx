import { cn } from '@/lib/utils'

interface FilterPillsProps<T extends string> {
  options: { value: T; label: string }[]
  selected: T | null
  onSelect: (value: T | null) => void
}

export function FilterPills<T extends string>({
  options,
  selected,
  onSelect,
}: FilterPillsProps<T>) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {/* "Todos" pill — always first, clears filter */}
      <button
        type="button"
        onClick={() => onSelect(null)}
        aria-pressed={selected === null}
        className={cn(
          'rounded-full px-3 py-1 text-xs transition-colors cursor-pointer',
          selected === null
            ? 'bg-[#0071e3] text-white'
            : 'bg-[#1f1f1f] text-[#86868b] hover:bg-[#2a2a2a]',
        )}
      >
        Todos
      </button>

      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onSelect(selected === opt.value ? null : opt.value)}
          aria-pressed={selected === opt.value}
          className={cn(
            'rounded-full px-3 py-1 text-xs transition-colors cursor-pointer',
            selected === opt.value
              ? 'bg-[#0071e3] text-white'
              : 'bg-[#1f1f1f] text-[#86868b] hover:bg-[#2a2a2a]',
          )}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}
