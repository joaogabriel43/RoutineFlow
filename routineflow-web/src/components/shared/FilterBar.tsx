import { Search, X } from 'lucide-react'
import { cn } from '@/lib/utils'

interface FilterBarProps {
  search: string
  onSearchChange: (value: string) => void
  placeholder?: string
  /** Pill filters or any extra controls rendered to the right of the input. */
  children?: React.ReactNode
}

export function FilterBar({
  search,
  onSearchChange,
  placeholder = 'Buscar…',
  children,
}: FilterBarProps) {
  return (
    <div className="flex flex-wrap gap-2 items-center">
      {/* Search input */}
      <div className="relative w-full sm:w-64">
        <Search
          size={14}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8C8A88] pointer-events-none"
        />
        <input
          type="text"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Escape') onSearchChange('')
          }}
          placeholder={placeholder}
          className={cn(
            'w-full pl-8 pr-7 py-2 text-sm rounded-lg',
            'bg-[#141416] border border-[#26262A]',
            'text-[#F4F2EF] placeholder:text-[#8C8A88]',
            'focus:outline-none focus:border-[#2F8BFF]',
            'transition-colors',
          )}
        />
        {search !== '' && (
          <button
            type="button"
            onClick={() => onSearchChange('')}
            aria-label="Limpar busca"
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[#8C8A88] hover:text-[#F4F2EF] transition-colors"
          >
            <X size={13} />
          </button>
        )}
      </div>

      {/* Pill filters (passed as children) */}
      {children}
    </div>
  )
}
