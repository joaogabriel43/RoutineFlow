import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { skipDaysApi } from '@/services/api'
import { toast } from 'sonner'
import { X } from 'lucide-react'

interface SkipDayModalProps {
  open: boolean
  onClose: () => void
  areaId: number
  areaName: string
  date: string
  onSuccess: () => void
}

export function SkipDayModal({
  open,
  onClose,
  areaId,
  areaName,
  date,
  onSuccess,
}: SkipDayModalProps) {
  const [reason, setReason] = useState('')
  const queryClient = useQueryClient()

  const skipMutation = useMutation({
    mutationFn: () => skipDaysApi.skipDay(areaId, { date, reason: reason.trim() || undefined }),
    onSuccess: () => {
      toast.success(`Dia pulado para ${areaName}`)
      queryClient.invalidateQueries({ queryKey: ['day-progress'] })
      queryClient.invalidateQueries({ queryKey: ['heatmap'] })
      onSuccess()
      onClose()
    },
    onError: (error: any) => {
      const msg = error.response?.data?.detail || 'Erro ao pular dia'
      toast.error(msg)
    },
  })

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="w-full sm:w-[400px] bg-surface-2 rounded-t-2xl sm:rounded-2xl border border-line shadow-2xl p-5 animate-in slide-in-from-bottom-4 sm:slide-in-from-bottom-8 duration-300"
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-medium text-fg">Pular Dia: {areaName}</h2>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-fg-lo hover:text-fg hover:bg-surface-3 transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        <p className="text-sm text-fg-lo mb-4">
          Um skip day permite ignorar as tarefas desta área hoje sem quebrar seu streak. 
          Você tem direito a 2 skip days por mês por área.
        </p>

        <div className="mb-5">
          <label className="block text-sm text-fg mb-2">Motivo (opcional)</label>
          <input
            type="text"
            placeholder="Ex: Viagem, Doença, Feriado..."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full bg-surface-1 border border-line rounded-lg px-3 py-2 text-fg placeholder:text-fg-lo/50 focus:outline-none focus:border-brand transition-colors"
            maxLength={100}
          />
        </div>

        <div className="flex gap-3">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl font-medium text-fg bg-surface-3 hover:bg-surface-4 transition-colors"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={() => skipMutation.mutate()}
            disabled={skipMutation.isPending}
            className="flex-1 py-2.5 rounded-xl font-medium text-white bg-amber-600 hover:bg-amber-500 transition-colors disabled:opacity-50"
          >
            {skipMutation.isPending ? 'Pulando...' : 'Confirmar Skip'}
          </button>
        </div>
      </div>
    </div>
  )
}
