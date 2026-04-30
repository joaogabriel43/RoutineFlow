import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { routineApi } from '@/services/api'
import type { ImportMode, ImportRoutineResponse } from '@/types'

function buildSuccessMessage(data: ImportRoutineResponse): string {
  if (data.mode === 'REPLACE') {
    return `Rotina importada! ${data.areasCreated} ${data.areasCreated === 1 ? 'área' : 'áreas'} e ${data.tasksCreated} ${data.tasksCreated === 1 ? 'tarefa' : 'tarefas'} criadas.`
  }
  // MERGE
  const parts: string[] = []
  if (data.areasCreated > 0)
    parts.push(`${data.areasCreated} ${data.areasCreated === 1 ? 'área nova' : 'áreas novas'}`)
  if (data.areasMerged > 0)
    parts.push(`${data.areasMerged} ${data.areasMerged === 1 ? 'área mesclada' : 'áreas mescladas'}`)
  if (data.tasksCreated > 0)
    parts.push(`${data.tasksCreated} ${data.tasksCreated === 1 ? 'tarefa adicionada' : 'tarefas adicionadas'}`)
  if (data.tasksSkipped > 0)
    parts.push(`${data.tasksSkipped} ${data.tasksSkipped === 1 ? 'duplicata ignorada' : 'duplicatas ignoradas'}`)
  return parts.length > 0
    ? `Merge concluído! ${parts.join(', ')}.`
    : 'Merge concluído. Nenhuma novidade encontrada no arquivo.'
}

export function useImportRoutine() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { mutate: mutateBase, isPending, isSuccess } = useMutation({
    mutationFn: ({ file, mode }: { file: File; mode: ImportMode }) =>
      routineApi.importRoutine(file, mode).then((r) => r.data),
    onSuccess: (data) => {
      toast.success(buildSuccessMessage(data))
      // Invalidate all schedule and analytics caches
      void queryClient.invalidateQueries({ queryKey: ['today-schedule'] })
      void queryClient.invalidateQueries({ queryKey: ['today-progress'] })
      void queryClient.invalidateQueries({ queryKey: ['weekly-current'] })
      void queryClient.invalidateQueries({ queryKey: ['day-schedule'] })
      void queryClient.invalidateQueries({ queryKey: ['analytics-streaks'] })
      void queryClient.invalidateQueries({ queryKey: ['analytics-heatmap'] })
      void queryClient.invalidateQueries({ queryKey: ['analytics-history'] })
      // Delay redirect so the toast is visible
      setTimeout(() => navigate('/'), 800)
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Erro ao importar arquivo. Verifique o formato e tente novamente.')
    },
  })

  // Expose a clean mutate with (file, mode) signature
  function mutate(file: File, mode: ImportMode) {
    mutateBase({ file, mode })
  }

  return { mutate, isPending, isSuccess }
}
