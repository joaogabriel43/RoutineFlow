import { useState, useEffect, useRef } from 'react'

interface NoteInputProps {
  initialNote: string | null
  onSave: (note: string) => void
  disabled?: boolean
}

export function NoteInput({ initialNote, onSave, disabled = false }: NoteInputProps) {
  const [expanded, setExpanded] = useState(false)
  const [note, setNote] = useState(initialNote ?? '')
  const [isSaving, setIsSaving] = useState(false)
  const debounceTimer = useRef<NodeJS.Timeout | null>(null)

  useEffect(() => {
    setNote(initialNote ?? '')
  }, [initialNote])

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value
    setNote(val)
    
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current)
    }

    setIsSaving(true)
    debounceTimer.current = setTimeout(() => {
      onSave(val)
      setIsSaving(false)
    }, 800)
  }

  const handleBlur = () => {
    if (!note.trim()) {
      setExpanded(false)
    }
  }

  if (!expanded && !note.trim()) {
    return (
      <button
        onClick={() => setExpanded(true)}
        disabled={disabled}
        className="flex items-center gap-1 mt-2 text-xs text-[#86868b] hover:text-[#f5f5f7] transition-colors"
      >
        <span>📝</span> Adicionar nota... (opcional)
      </button>
    )
  }

  return (
    <div className="mt-2 flex flex-col gap-1 w-full animate-in fade-in slide-in-from-top-2 duration-300">
      <textarea
        value={note}
        onChange={handleChange}
        onBlur={handleBlur}
        disabled={disabled}
        autoFocus={expanded && !note.trim()}
        placeholder="Adicionar nota... (opcional)"
        className="w-full min-h-[48px] max-h-[120px] bg-[#1a1a1a] border border-[#2a2a2a] rounded-lg p-2 text-xs text-[#f5f5f7] placeholder:text-[#86868b] resize-y focus:outline-none focus:border-[#0071e3] transition-colors"
        rows={expanded || note.trim().length > 50 ? 3 : 1}
        onClick={() => setExpanded(true)}
      />
      <div className="flex justify-between items-center text-[10px] text-[#86868b] h-3">
        <span>{isSaving ? 'Salvando...' : ' '}</span>
        {note.length > 200 && (
          <span className={note.length > 1000 ? 'text-[#ff453a]' : ''}>
            {note.length} caracteres
          </span>
        )}
      </div>
    </div>
  )
}
