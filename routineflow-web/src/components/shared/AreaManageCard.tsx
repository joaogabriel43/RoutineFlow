import { Pencil, Trash2, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { AreaResponse } from '@/types'

interface Props {
  area: AreaResponse
  isSelected: boolean
  onSelect: () => void
  onEdit: () => void
  onDelete: () => void
}

export function AreaManageCard({ area, isSelected, onSelect, onEdit, onDelete }: Props) {
  return (
    <div
      className={cn(
        'flex items-center gap-3 px-3 py-3 rounded-xl cursor-pointer transition-colors group',
        isSelected ? 'bg-[#1f1f1f]' : 'hover:bg-[#141414]',
      )}
      onClick={onSelect}
    >
      {/* Color dot + icon */}
      <div
        className="w-8 h-8 rounded-full shrink-0 flex items-center justify-center text-base"
        style={{ backgroundColor: area.color + '22', border: `2px solid ${area.color}` }}
      >
        <span>{area.icon}</span>
      </div>

      {/* Name */}
      <span className="flex-1 text-sm text-[#f5f5f7] font-medium truncate">{area.name}</span>

      {/* Frequency badge — only shown for non-DAILY areas */}
      {area.resetFrequency !== 'DAILY' && (
        <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full shrink-0 bg-[#0071e3]/15 text-[#0071e3]">
          {area.resetFrequency === 'WEEKLY' ? 'semanal' : 'mensal'}
        </span>
      )}

      {/* Task count badge */}
      <span className="text-[11px] text-[#86868b] shrink-0">
        {area.tasks.length} {area.tasks.length === 1 ? 'tarefa' : 'tarefas'}
      </span>

      {/* Actions — visible on hover or when selected */}
      <div
        className={cn(
          'flex items-center gap-1 shrink-0 transition-opacity',
          isSelected ? 'opacity-100' : 'opacity-0 group-hover:opacity-100',
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onEdit}
          className="p-1.5 rounded-lg text-[#86868b] hover:text-[#f5f5f7] hover:bg-[#2a2a2a] transition-colors"
          title="Editar área"
        >
          <Pencil size={13} />
        </button>
        <button
          onClick={onDelete}
          className="p-1.5 rounded-lg text-[#86868b] hover:text-red-400 hover:bg-red-500/10 transition-colors"
          title="Excluir área"
        >
          <Trash2 size={13} />
        </button>
      </div>

      <ChevronRight
        size={14}
        className={cn(
          'shrink-0 transition-colors',
          isSelected ? 'text-[#0071e3]' : 'text-[#3a3a3a] group-hover:text-[#86868b]',
        )}
      />
    </div>
  )
}
