import { useRef, useState, useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { toast } from 'sonner'
import { FileText, Loader2, UploadCloud, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useImportRoutine } from '@/hooks/useImportRoutine'
import { cn } from '@/lib/utils'

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

// ── Page ──────────────────────────────────────────────────────────────────────

export function ImportPage() {
  const location = useLocation()
  const inputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [file, setFile] = useState<File | null>(null)
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

  function handleUpload() {
    if (!file || isPending) return
    mutate(file)
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
          onClick={handleUpload}
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
      </section>
    </div>
  )
}
