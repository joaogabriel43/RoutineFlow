import { cn } from '@/lib/utils'

/**
 * Curated 12-color palette aligned with the Graphite design system.
 * Plus an optional native hex input for custom colors.
 */

interface ColorPickerProps {
  value: string | null
  onChange: (color: string) => void
}

const PALETTE = [
  '#2F8BFF', // azul (acento padrão)
  '#34C759', // verde
  '#FF9500', // laranja
  '#FF3B30', // vermelho
  '#AF52DE', // roxo
  '#FF2D55', // rosa
  '#5AC8FA', // ciano
  '#FFCC00', // amarelo
  '#FF6482', // coral
  '#30D158', // verde claro
  '#64D2FF', // azul claro
  '#8E8E93', // cinza
]

export function ColorPicker({ value, onChange }: ColorPickerProps) {
  const current = value ?? '#2F8BFF'

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap gap-2">
        {PALETTE.map((color) => {
          const selected = current.toLowerCase() === color.toLowerCase()
          return (
            <button
              key={color}
              type="button"
              onClick={() => onChange(color)}
              aria-label={`Cor ${color}`}
              aria-pressed={selected}
              className={cn(
                'w-7 h-7 rounded-full transition-transform',
                selected ? 'ring-2 ring-white ring-offset-2 ring-offset-surface-2 scale-110' : 'hover:scale-110',
              )}
              style={{ backgroundColor: color }}
            />
          )
        })}
      </div>

      {/* Custom hex */}
      <div className="flex items-center gap-2 pt-1">
        <input
          type="color"
          value={current}
          onChange={(e) => onChange(e.target.value)}
          aria-label="Cor personalizada"
          className="w-7 h-7 rounded border border-line bg-surface-1 cursor-pointer p-0.5"
        />
        <span className="num text-xs text-fg-lo">{current.toUpperCase()}</span>
      </div>
    </div>
  )
}
