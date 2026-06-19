import { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { useCreateSingleTask } from '@/hooks/useSingleTasks'
import { getLocalISODate } from '@/lib/utils'

interface CreateSingleTaskModalProps {
  open: boolean
  onClose: () => void
}

function todayStr(): string {
  return getLocalISODate()
}

export function CreateSingleTaskModal({ open, onClose }: CreateSingleTaskModalProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [titleError, setTitleError] = useState('')

  const createMutation = useCreateSingleTask()

  function reset() {
    setTitle('')
    setDescription('')
    setDueDate('')
    setTitleError('')
  }

  function handleClose() {
    reset()
    onClose()
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    if (!title.trim()) {
      setTitleError('O título é obrigatório')
      return
    }
    setTitleError('')

    await createMutation.mutateAsync({
      title: title.trim(),
      description: description.trim() || null,
      dueDate: dueDate || null,
    })

    reset()
    onClose()
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="bg-[#141416] border-[#26262A] text-[#F4F2EF] max-w-md">
        <DialogHeader>
          <DialogTitle className="text-[#F4F2EF] font-medium">Nova tarefa</DialogTitle>
        </DialogHeader>

        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4 py-2">
          {/* Title */}
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] uppercase tracking-wide font-medium">
              Título *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ex: Comprar leite"
              autoFocus
              className="w-full bg-[#26262A] border border-[#26262A] rounded-lg px-3 py-2 text-sm text-[#F4F2EF] placeholder:text-[#3a3a3c] focus:outline-none focus:ring-1 focus:ring-[#2F8BFF] focus:border-[#2F8BFF] transition-colors"
            />
            {titleError && (
              <p className="text-xs text-danger">{titleError}</p>
            )}
          </div>

          {/* Description */}
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] uppercase tracking-wide font-medium">
              Descrição (opcional)
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detalhes adicionais..."
              rows={2}
              className="w-full bg-[#26262A] border border-[#26262A] rounded-lg px-3 py-2 text-sm text-[#F4F2EF] placeholder:text-[#3a3a3c] focus:outline-none focus:ring-1 focus:ring-[#2F8BFF] focus:border-[#2F8BFF] transition-colors resize-none"
            />
          </div>

          {/* Due date */}
          <div className="space-y-1.5">
            <label className="text-xs text-[#8C8A88] uppercase tracking-wide font-medium">
              Prazo (opcional)
            </label>
            <input
              type="date"
              value={dueDate}
              min={todayStr()}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full bg-[#26262A] border border-[#26262A] rounded-lg px-3 py-2 text-sm text-[#F4F2EF] focus:outline-none focus:ring-1 focus:ring-[#2F8BFF] focus:border-[#2F8BFF] transition-colors [color-scheme:dark]"
            />
          </div>

          <DialogFooter className="pt-2 gap-2">
            <button
              type="button"
              onClick={handleClose}
              className="px-4 py-2 text-sm text-[#8C8A88] hover:text-[#F4F2EF] transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-4 py-2 text-sm font-medium bg-[#2F8BFF] hover:bg-[#4F9DFF] text-white rounded-lg transition-colors disabled:opacity-50"
            >
              {createMutation.isPending ? 'Criando...' : 'Criar tarefa'}
            </button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
