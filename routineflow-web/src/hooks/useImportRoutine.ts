import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { routineApi } from '@/services/api'

export function useImportRoutine() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { mutate, isPending, isSuccess } = useMutation({
    mutationFn: (file: File) => routineApi.importRoutine(file).then((r) => r.data),
    onSuccess: () => {
      toast.success('Rotina importada com sucesso!')
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

  return { mutate, isPending, isSuccess }
}
