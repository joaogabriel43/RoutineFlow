import { useRef, useState, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { FileText, Loader2, RefreshCw, GitMerge, UploadCloud, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog'
import { useImportRoutine } from '@/hooks/useImportRoutine'
import { cn } from '@/lib/utils'
import type { ImportMode } from '@/types'

// ── File size formatter ───────────────────────────────────────────────────────

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// ── Valid extensions ──────────────────────────────────────────────────────────

const VALID_EXTS = ['.yaml', '.yml', '.txt']

function isValidExtension(filename: string): boolean {
  return VALID_EXTS.some((ext) => filename.toLowerCase().endsWith(ext))
}

// ── Example YAML ─────────────────────────────────────────────────────────────

const EXAMPLE_YAML = `routine:
  name: "Minha Rotina"
  areas:
    - name: "Estudo"
      color: "#3B82F6"
      icon: "📚"
      schedule:
        MONDAY:
          - title: "Leitura"
            description: "30 minutos"
            estimatedMinutes: 30`

// ── Mode card ─────────────────────────────────────────────────────────────────

interface ModeCardProps {
  selected: boolean
  onClick: () => void
  icon: React.ReactNode
  title: string
  description: string
  badge?: string
}

function ModeCard({ selected, onClick, icon, title, description, badge }: ModeCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'w-full text-left rounded-xl border-2 p-4 transition-all duration-150',
        selected
          ? 'border-[#0071e3] bg-[#0071e3]/10'
          : 'border-[#2a2a2a] bg-[#141414] hover:border-[#3a3a3a]',
      )}
    >
      <div className="flex items-start gap-3">
        <span className={cn('mt-0.5 shrink-0', selected ? 'text-[#0071e3]' : 'text-[#86868b]')}>
          {icon}
        </span>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-[#f5f5f7]">{title}</span>
            {badge && (
              <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full bg-[#0071e3]/20 text-[#0071e3]">
                {badge}
              </span>
            )}
          </div>
          <p className="text-xs text-[#86868b] mt-1 leading-relaxed">{description}</p>
        </div>
      </div>
    </button>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function ImportPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const inputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [showModeModal, setShowModeModal] = useState(false)
  const [selectedMode, setSelectedMode] = useState<ImportMode>('REPLACE')
  const { mutate, isPending } = useImportRoutine()

  // Welcome toast on first login redirect
  useEffect(() => {
    if ((location.state as { welcome?: boolean } | null)?.welcome) {
      toast.success('Bem-vindo ao RoutineFlow! Comece importando sua rotina.')
    }
  }, [location.state])

  // ── File handling ───────────────────────────────────────────────────────────

  function acceptFile(f: File) {
    if (!isValidExtension(f.name)) {
      toast.error('Apenas arquivos .yaml, .yml ou .txt são aceitos')
      return
    }
    setFile(f)
  }

  function onDragOver(e: React.DragEvent) {
    e.preventDefault()
    setIsDragging(true)
  }

  function onDragLeave(e: React.DragEvent) {
    e.preventDefault()
    setIsDragging(false)
  }

  function onDrop(e: React.DragEvent) {
    e.preventDefault()
    setIsDragging(false)
    const f = e.dataTransfer.files[0]
    if (f) acceptFile(f)
  }

  function onInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (f) acceptFile(f)
    // Reset so the same file can be re-selected after removal
    e.target.value = ''
  }

  function handleUploadClick() {
    if (!file || isPending) return
    setShowModeModal(true)
  }

  function handleConfirmImport() {
    if (!file) return
    setShowModeModal(false)
    mutate(file, selectedMode)
  }

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="max-w-lg mx-auto">
      {/* Header */}
      <header className="mb-8">
        <h1 className="text-2xl font-light text-[#f5f5f7] tracking-tight">Importar rotina</h1>
        <p className="text-sm text-[#86868b] mt-1">
          Envie um arquivo .yaml ou .txt com sua rotina
        </p>
      </header>

      {/* Drop zone */}
      <div
        role="button"
        tabIndex={0}
        aria-label="Zona de upload — clique ou arraste seu arquivo aqui"
        onClick={() => !file && inputRef.current?.click()}
        onKeyDown={(e) => e.key === 'Enter' && !file && inputRef.current?.click()}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        className={cn(
          'rounded-2xl border-2 border-dashed p-12 text-center cursor-pointer',
          'transition-all duration-200',
          isDragging
            ? 'border-[#0071e3] bg-[#0071e3]/5'
            : 'border-[#1f1f1f] hover:border-[#0071e3] hover:bg-[#0071e3]/5',
          file && 'cursor-default hover:border-[#1f1f1f] hover:bg-transparent',
        )}
      >
        {file ? (
          /* File preview */
          <div className="flex items-center gap-3 justify-center">
            <FileText size={24} className="text-[#0071e3] shrink-0" />
            <div className="text-left min-w-0">
              <p className="text-sm font-medium text-[#f5f5f7] truncate">{file.name}</p>
              <p className="text-xs text-[#86868b] mt-0.5">{formatBytes(file.size)}</p>
            </div>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation()
                setFile(null)
              }}
              aria-label="Remover arquivo"
              className="ml-2 p-1 rounded-full text-[#86868b] hover:text-[#f5f5f7] hover:bg-[#1f1f1f] transition-colors"
            >
              <X size={16} />
            </button>
          </div>
        ) : (
          /* Upload prompt */
          <>
            <UploadCloud size={48} className="mx-auto text-[#86868b] mb-4" strokeWidth={1.25} />
            <p className="text-sm font-medium text-[#f5f5f7]">Arraste seu arquivo aqui</p>
            <p className="text-xs text-[#86868b] mt-1">
              ou{' '}
              <span className="text-[#0071e3] font-medium">clique para selecionar</span>
            </p>
            <p className="text-[10px] text-[#3a3a3a] mt-3">.yaml · .yml · .txt</p>
          </>
        )}
      </div>

      {/* Hidden file input */}
      <input
        ref={inputRef}
        type="file"
        accept=".yaml,.yml,.txt"
        onChange={onInputChange}
        className="hidden"
        aria-hidden="true"
      />

      {/* Upload button — only visible when file is selected */}
      {file && (
        <Button
          onClick={handleUploadClick}
          disabled={isPending}
          className="w-full mt-4 bg-[#0071e3] hover:bg-[#0077ed] text-white font-medium h-11 rounded-xl transition-colors disabled:opacity-60"
        >
          {isPending ? (
            <span className="flex items-center gap-2">
              <Loader2 size={16} className="animate-spin" />
              Importando…
            </span>
          ) : (
            'Importar rotina'
          )}
        </Button>
      )}

      {/* Help section */}
      <section className="mt-10">
        <h2 className="text-xs font-semibold text-[#86868b] uppercase tracking-widest mb-3">
          Formato suportado
        </h2>
        <pre className="rounded-xl bg-[#141414] border border-[#1f1f1f] p-4 text-xs font-mono text-[#86868b] overflow-x-auto leading-relaxed whitespace-pre">
          {EXAMPLE_YAML}
        </pre>
        <p className="text-[11px] text-[#3a3a3a] mt-3">
          Dias suportados: MONDAY · TUESDAY · WEDNESDAY · THURSDAY · FRIDAY · SATURDAY · SUNDAY
        </p>
        <p className="text-xs text-[#86868b] mt-3">
          Vem do HabitNow?{' '}
          <button
            onClick={() => navigate('/import/habitnow')}
            className="text-[#0071e3] hover:underline"
          >
            Converter backup .hn →
          </button>
        </p>
      </section>

      {/* Mode selection dialog */}
      <Dialog open={showModeModal} onOpenChange={setShowModeModal}>
        <DialogContent className="bg-[#1c1c1e] border border-[#2a2a2a] rounded-2xl max-w-sm">
          <DialogHeader>
            <DialogTitle className="text-[#f5f5f7] text-base font-medium">
              Como importar?
            </DialogTitle>
            <DialogDescription className="text-[#86868b] text-sm">
              Escolha o que acontece com sua rotina atual.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-3 mt-1">
            <ModeCard
              selected={selectedMode === 'REPLACE'}
              onClick={() => setSelectedMode('REPLACE')}
              icon={<RefreshCw size={18} />}
              title="Substituir"
              badge="padrão"
              description="Desativa a rotina atual e cria uma nova do zero com o arquivo enviado."
            />
            <ModeCard
              selected={selectedMode === 'MERGE'}
              onClick={() => setSelectedMode('MERGE')}
              icon={<GitMerge size={18} />}
              title="Mesclar"
              description="Mantém a rotina ativa e adiciona apenas áreas e tarefas novas. Duplicatas são ignoradas silenciosamente."
            />
          </div>

          <div className="flex gap-2 mt-2">
            <Button
              variant="ghost"
              onClick={() => setShowModeModal(false)}
              className="flex-1 text-[#86868b] hover:text-[#f5f5f7] hover:bg-[#2a2a2a] h-10 rounded-xl"
            >
              Cancelar
            </Button>
            <Button
              onClick={handleConfirmImport}
              className="flex-1 bg-[#0071e3] hover:bg-[#0077ed] text-white h-10 rounded-xl"
            >
              Confirmar
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
