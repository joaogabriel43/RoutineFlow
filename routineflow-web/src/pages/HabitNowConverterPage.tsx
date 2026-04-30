import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, FileText, Loader2, UploadCloud, X } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import {
  parseHabitNowBackup,
  generateRoutineYaml,
} from '@/lib/habitnow-parser'
import type { HabitNowHabit } from '@/lib/habitnow-parser'

// ── Habit preview row ─────────────────────────────────────────────────────────

function dayLabel(days: HabitNowHabit['days']): string {
  if (days.length === 0) return 'Todo dia'
  const SHORT: Record<string, string> = {
    MONDAY: 'Seg', TUESDAY: 'Ter', WEDNESDAY: 'Qua',
    THURSDAY: 'Qui', FRIDAY: 'Sex', SATURDAY: 'Sáb', SUNDAY: 'Dom',
  }
  return days.map((d) => SHORT[d] ?? d).join(', ')
}

interface HabitRowProps {
  habit: HabitNowHabit
  checked: boolean
  onChange: (checked: boolean) => void
}

function HabitRow({ habit, checked, onChange }: HabitRowProps) {
  return (
    <label className="flex items-center gap-3 py-2.5 px-3 rounded-lg hover:bg-[#1f1f1f] cursor-pointer transition-colors">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="w-4 h-4 accent-[#0071e3] shrink-0"
      />
      <span className="flex-1 text-sm text-[#f5f5f7] truncate">{habit.name}</span>
      <span className="text-[11px] text-[#86868b] shrink-0 min-w-[56px] text-right">
        {dayLabel(habit.days)}
      </span>
      <span
        className={cn(
          'text-[10px] font-medium px-1.5 py-0.5 rounded-full shrink-0',
          habit.isActive
            ? 'bg-green-900/30 text-green-400'
            : 'bg-[#2a2a2a] text-[#86868b]',
        )}
      >
        {habit.isActive ? 'ativo' : 'inativo'}
      </span>
    </label>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function HabitNowConverterPage() {
  const navigate = useNavigate()
  const inputRef = useRef<HTMLInputElement>(null)

  const [isDragging, setIsDragging] = useState(false)
  const [file, setFile]             = useState<File | null>(null)
  const [habits, setHabits]         = useState<HabitNowHabit[]>([])
  const [selected, setSelected]     = useState<Set<string>>(new Set())
  const [routineName, setRoutineName] = useState('Minha Rotina HabitNow')
  const [isGenerating, setIsGenerating] = useState(false)

  // ── File handling ─────────────────────────────────────────────────────────

  function acceptFile(f: File) {
    if (!f.name.toLowerCase().endsWith('.hn')) {
      toast.error('Apenas arquivos .hn são aceitos')
      return
    }
    setFile(f)
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const content = e.target?.result as string
        const parsed  = parseHabitNowBackup(content)
        if (parsed.length === 0) {
          toast.error('Nenhum hábito encontrado no arquivo.')
          return
        }
        setHabits(parsed)
        // Pre-select only active habits
        setSelected(new Set(parsed.filter((h) => h.isActive).map((h) => h.id)))
        toast.success(`${parsed.length} hábitos encontrados.`)
      } catch {
        toast.error('Erro ao ler o arquivo. Verifique se é um backup HabitNow válido.')
      }
    }
    reader.readAsText(f)
  }

  function onDragOver(e: React.DragEvent) { e.preventDefault(); setIsDragging(true) }
  function onDragLeave(e: React.DragEvent) { e.preventDefault(); setIsDragging(false) }

  function onDrop(e: React.DragEvent) {
    e.preventDefault()
    setIsDragging(false)
    const f = e.dataTransfer.files[0]
    if (f) acceptFile(f)
  }

  function onInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (f) acceptFile(f)
    e.target.value = ''
  }

  function removeFile() {
    setFile(null)
    setHabits([])
    setSelected(new Set())
  }

  // ── Selection helpers ──────────────────────────────────────────────────────

  function toggleHabit(id: string, checked: boolean) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (checked) next.add(id); else next.delete(id)
      return next
    })
  }

  function selectAllActive() {
    setSelected(new Set(habits.filter((h) => h.isActive).map((h) => h.id)))
  }

  function deselectAll() {
    setSelected(new Set())
  }

  // ── YAML download ──────────────────────────────────────────────────────────

  function handleGenerate() {
    const toExport = habits.filter((h) => selected.has(h.id))
    if (toExport.length === 0) {
      toast.error('Selecione ao menos um hábito.')
      return
    }

    setIsGenerating(true)
    try {
      const yaml = generateRoutineYaml(toExport, routineName)
      const blob = new Blob([yaml], { type: 'text/yaml' })
      const url  = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', 'routineflow-habitnow.yaml')
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
      toast.success('Arquivo .yaml gerado! Importe-o na tela de Importar.')
    } catch {
      toast.error('Erro ao gerar o YAML.')
    } finally {
      setIsGenerating(false)
    }
  }

  // ── Counts ─────────────────────────────────────────────────────────────────

  const activeCount   = habits.filter((h) => h.isActive).length
  const inactiveCount = habits.length - activeCount

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="max-w-lg mx-auto">
      {/* Header */}
      <header className="mb-8">
        <h1 className="text-2xl font-light text-[#f5f5f7] tracking-tight">
          Converter backup HabitNow
        </h1>
        <p className="text-sm text-[#86868b] mt-1">
          Importe seu arquivo .hn e converta para o formato RoutineFlow
        </p>
      </header>

      {/* Drop zone */}
      <div
        role="button"
        tabIndex={0}
        aria-label="Zona de upload — clique ou arraste seu arquivo .hn aqui"
        onClick={() => !file && inputRef.current?.click()}
        onKeyDown={(e) => e.key === 'Enter' && !file && inputRef.current?.click()}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        className={cn(
          'rounded-2xl border-2 border-dashed p-10 text-center cursor-pointer transition-all duration-200',
          isDragging
            ? 'border-[#0071e3] bg-[#0071e3]/5'
            : 'border-[#1f1f1f] hover:border-[#0071e3] hover:bg-[#0071e3]/5',
          file && 'cursor-default hover:border-[#1f1f1f] hover:bg-transparent',
        )}
      >
        {file ? (
          <div className="flex items-center gap-3 justify-center">
            <FileText size={24} className="text-[#0071e3] shrink-0" />
            <div className="text-left min-w-0">
              <p className="text-sm font-medium text-[#f5f5f7] truncate">{file.name}</p>
              <p className="text-xs text-[#86868b] mt-0.5">{habits.length} hábitos</p>
            </div>
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); removeFile() }}
              aria-label="Remover arquivo"
              className="ml-2 p-1 rounded-full text-[#86868b] hover:text-[#f5f5f7] hover:bg-[#1f1f1f] transition-colors"
            >
              <X size={16} />
            </button>
          </div>
        ) : (
          <>
            <UploadCloud size={40} className="mx-auto text-[#86868b] mb-3" strokeWidth={1.25} />
            <p className="text-sm font-medium text-[#f5f5f7]">Arraste seu arquivo aqui</p>
            <p className="text-xs text-[#86868b] mt-1">
              ou <span className="text-[#0071e3] font-medium">clique para selecionar</span>
            </p>
            <p className="text-[10px] text-[#3a3a3a] mt-3">.hn</p>
          </>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept=".hn"
        onChange={onInputChange}
        className="hidden"
        aria-hidden="true"
      />

      {/* Preview */}
      {habits.length > 0 && (
        <div className="mt-6 space-y-4">
          {/* Summary + bulk actions */}
          <div className="flex items-center justify-between">
            <p className="text-xs text-[#86868b]">
              <span className="text-[#f5f5f7] font-medium">{habits.length}</span> hábitos encontrados
              {' '}({activeCount} ativos, {inactiveCount} inativos)
            </p>
            <div className="flex gap-2">
              <button
                onClick={selectAllActive}
                className="text-[10px] text-[#0071e3] hover:underline"
              >
                Selecionar ativos
              </button>
              <span className="text-[10px] text-[#3a3a3a]">·</span>
              <button
                onClick={deselectAll}
                className="text-[10px] text-[#86868b] hover:underline"
              >
                Desmarcar todos
              </button>
            </div>
          </div>

          {/* Habit list */}
          <div className="rounded-xl bg-[#141414] border border-[#1f1f1f] p-1 divide-y divide-[#1f1f1f]">
            {habits.map((habit) => (
              <HabitRow
                key={habit.id}
                habit={habit}
                checked={selected.has(habit.id)}
                onChange={(checked) => toggleHabit(habit.id, checked)}
              />
            ))}
          </div>

          {/* Routine name input */}
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-[#86868b] uppercase tracking-wider">
              Nome da rotina
            </label>
            <input
              type="text"
              value={routineName}
              onChange={(e) => setRoutineName(e.target.value)}
              placeholder="Minha Rotina HabitNow"
              className="w-full rounded-xl bg-[#141414] border border-[#1f1f1f] px-4 py-2.5
                         text-sm text-[#f5f5f7] placeholder-[#3a3a3a]
                         focus:outline-none focus:border-[#0071e3] transition-colors"
            />
          </div>

          {/* Action buttons */}
          <div className="flex gap-3">
            <Button
              onClick={handleGenerate}
              disabled={isGenerating || selected.size === 0}
              className="flex-1 bg-[#0071e3] hover:bg-[#0077ed] text-white font-medium h-11 rounded-xl
                         transition-colors disabled:opacity-60"
            >
              {isGenerating ? (
                <span className="flex items-center gap-2">
                  <Loader2 size={14} className="animate-spin" />
                  Gerando…
                </span>
              ) : (
                `Gerar YAML e baixar (${selected.size})`
              )}
            </Button>
            <Button
              onClick={() => navigate('/import')}
              variant="outline"
              className="h-11 rounded-xl border-[#1f1f1f] bg-transparent text-[#86868b]
                         hover:text-[#f5f5f7] hover:bg-[#1f1f1f] hover:border-[#1f1f1f]
                         transition-colors"
            >
              Ir para Importar
              <ArrowRight size={14} className="ml-1.5" />
            </Button>
          </div>

          {/* Info note */}
          <p className="text-[11px] text-[#3a3a3a] leading-relaxed">
            Após baixar o arquivo .yaml, importe-o na tela de Importar.
            O histórico de check-ins do HabitNow não é importado —
            apenas a estrutura de hábitos e agendamentos.
          </p>

          {/* Merge tip */}
          <div className="rounded-xl border border-[#1f1f1f] bg-[#141414] p-3">
            <p className="text-[11px] text-[#86868b] leading-relaxed">
              <span className="text-[#0071e3] font-medium">Dica:</span> Se você já tem uma rotina
              ativa e quer adicionar apenas os hábitos novos, use o modo{' '}
              <span className="text-[#f5f5f7]">Mesclar</span> ao importar. Duplicatas são
              ignoradas automaticamente.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
