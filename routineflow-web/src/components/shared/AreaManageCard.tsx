import { Pencil, Trash2, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'
import { DynamicIcon } from './DynamicIcon'
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
        isSelected ? 'bg-[#26262A]' : 'hover:bg-[#141416]',
      )}
      onClick={onSelect}
    >
      {/* Color dot + icon */}
      <div
        className="w-8 h-8 rounded-full shrink-0 flex items-center justify-center"
        style={{ backgroundColor: area.color + '22', border: `2px solid ${area.color}`, color: area.color }}
      >
        <DynamicIcon name={area.icon} size={16} fallback="folder" />
      </div>

      {/* Name */}
      <span className="flex-1 text-sm text-[#F4F2EF] font-medium truncate">{area.name}</span>

      {/* Frequency badge — only shown for non-DAILY areas */}
      {area.resetFrequency !== 'DAILY' && (
        <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full shrink-0 bg-[#2F8BFF]/15 text-[#2F8BFF]">
          {area.resetFrequency === 'WEEKLY' ? 'semanal' : 'mensal'}
        </span>
      )}

      {/* Task count badge */}
      <span className="text-[11px] text-[#8C8A88] shrink-0">
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
          className="p-1.5 rounded-lg text-[#8C8A88] hover:text-[#F4F2EF] hover:bg-[#26262A] transition-colors"
          title="Editar área"
        >
          <Pencil size={13} />
        </button>
        <button
          onClick={onDelete}
          className="p-1.5 rounded-lg text-[#8C8A88] hover:text-danger hover:bg-danger/10 transition-colors"
          title="Excluir área"
        >
          <Trash2 size={13} />
        </button>
      </div>

      <ChevronRight
        size={14}
        className={cn(
          'shrink-0 transition-colors',
          isSelected ? 'text-[#2F8BFF]' : 'text-[#34343A] group-hover:text-[#8C8A88]',
        )}
      />
    </div>
  )
}
