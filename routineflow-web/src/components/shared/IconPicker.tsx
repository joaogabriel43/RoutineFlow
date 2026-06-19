import { useMemo, useState } from 'react'
import * as Icons from 'lucide-react'
import { iconNames } from 'lucide-react/dynamic'
import { Search } from 'lucide-react'
import { kebabToPascal } from '@/lib/icons'
import { ICON_CATEGORIES } from '@/lib/iconCatalog'
import { cn } from '@/lib/utils'

/**
 * Searchable lucide icon picker with curated categories.
 *
 * Default export so it can be React.lazy()-loaded — the `import * as Icons`
 * (the full ~2900-icon catalog) is therefore isolated in this lazy chunk and
 * NEVER lands in the main bundle. Renders straight from the in-memory catalog
 * (no per-icon network fetch), capping visible results so the grid stays fast.
 *
 * Sprint 26: shows curated categories by default (no search needed).
 * Full search (~1500 icons) activates when the user types in the search field.
 */

interface IconPickerProps {
  /** Currently selected icon, kebab-case (e.g. "dumbbell"). */
  value: string | null
  onChange: (iconName: string) => void
}

const MAX_RESULTS = 120
const ICON_CATALOG = Icons as unknown as Record<string, React.ComponentType<{ size?: number }>>

function IconButton({
  name,
  selected,
  onClick,
}: {
  name: string
  selected: boolean
  onClick: () => void
}) {
  const Cmp = ICON_CATALOG[kebabToPascal(name)]
  if (!Cmp) return null
  return (
    <button
      type="button"
      title={name}
      onClick={onClick}
      className={cn(
        'aspect-square flex items-center justify-center rounded-md transition-colors',
        selected
          ? 'bg-brand/10 text-brand ring-1 ring-brand'
          : 'text-fg-lo hover:bg-surface-3 hover:text-fg',
      )}
    >
      <Cmp size={18} />
    </button>
  )
}

export default function IconPicker({ value, onChange }: IconPickerProps) {
  const [query, setQuery] = useState('')

  const isSearching = query.trim().length > 0

  const searchResults = useMemo(() => {
    if (!isSearching) return []
    const q = query.trim().toLowerCase()
    return iconNames.filter((n) => n.includes(q)).slice(0, MAX_RESULTS)
  }, [query, isSearching])

  return (
    <div className="flex flex-col gap-2">
      {/* Search */}
      <div className="relative">
        <Search
          size={14}
          className="absolute left-2.5 top-1/2 -translate-y-1/2 text-fg-dim pointer-events-none"
        />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar ícone…"
          autoFocus
          className="w-full h-9 pl-8 pr-3 rounded-md bg-surface-1 border border-line text-fg text-sm placeholder:text-fg-dim focus:outline-none focus:border-brand"
        />
      </div>

      {/* Content */}
      <div className="max-h-[300px] overflow-y-auto pr-1">
        {isSearching ? (
          /* ── Search results ──────────────────────────────────────── */
          searchResults.length === 0 ? (
            <p className="text-xs text-fg-dim text-center py-4">Nenhum ícone encontrado.</p>
          ) : (
            <div className="grid grid-cols-8 gap-1">
              {searchResults.map((name) => (
                <IconButton
                  key={name}
                  name={name}
                  selected={name === value}
                  onClick={() => onChange(name)}
                />
              ))}
            </div>
          )
        ) : (
          /* ── Curated categories ──────────────────────────────────── */
          <div className="space-y-3">
            {Object.entries(ICON_CATEGORIES).map(([category, icons]) => (
              <div key={category}>
                <p className="text-[10px] uppercase tracking-widest text-fg-dim font-medium mb-1.5 px-0.5">
                  {category}
                </p>
                <div className="grid grid-cols-8 gap-1">
                  {icons.map((name) => (
                    <IconButton
                      key={name}
                      name={name}
                      selected={name === value}
                      onClick={() => onChange(name)}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
