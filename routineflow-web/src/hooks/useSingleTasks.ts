import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { singleTaskApi } from '@/services/api'
import type { CreateSingleTaskRequest, SingleTaskResponse } from '@/types'

const KEYS = {
  today:    ['single-tasks-today']    as const,
  pending:  ['single-tasks-pending']  as const,
  archived: ['single-tasks-archived'] as const,
}

// ── Queries ───────────────────────────────────────────────────────────────────

export function useSingleTasksToday() {
  return useQuery({
    queryKey: KEYS.today,
    queryFn: singleTaskApi.listToday,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  })
}

export function useSingleTasksPending() {
  return useQuery({
    queryKey: KEYS.pending,
    queryFn: singleTaskApi.listPending,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  })
}

export function useSingleTasksArchived() {
  return useQuery({
    queryKey: KEYS.archived,
    queryFn: singleTaskApi.listArchived,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  })
}

// ── Mutations ─────────────────────────────────────────────────────────────────

export function useCreateSingleTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateSingleTaskRequest) => singleTaskApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: KEYS.today })
      void queryClient.invalidateQueries({ queryKey: KEYS.pending })
      toast.success('Tarefa adicionada!')
    },
    onError: () => {
      toast.error('Erro ao criar tarefa. Tente novamente.')
    },
  })
}

export function useCompleteSingleTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.complete(id),
    // Optimistic update: remove from today and pending lists immediately
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: KEYS.today })
      await queryClient.cancelQueries({ queryKey: KEYS.pending })

      const prevToday = queryClient.getQueryData<SingleTaskResponse[]>(KEYS.today)
      const prevPending = queryClient.getQueryData<SingleTaskResponse[]>(KEYS.pending)

      queryClient.setQueryData<SingleTaskResponse[]>(
        KEYS.today,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      queryClient.setQueryData<SingleTaskResponse[]>(
        KEYS.pending,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )

      return { prevToday, prevPending }
    },
    onError: (_err, _id, context) => {
      queryClient.setQueryData(KEYS.today, context?.prevToday)
      queryClient.setQueryData(KEYS.pending, context?.prevPending)
      toast.error('Erro ao concluir tarefa. Tente novamente.')
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: KEYS.today })
      void queryClient.invalidateQueries({ queryKey: KEYS.pending })
      void queryClient.invalidateQueries({ queryKey: KEYS.archived })
    },
  })
}

export function useUncompleteSingleTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.uncomplete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: KEYS.today })
      void queryClient.invalidateQueries({ queryKey: KEYS.pending })
      void queryClient.invalidateQueries({ queryKey: KEYS.archived })
    },
    onError: () => {
      toast.error('Erro ao desfazer conclusão. Tente novamente.')
    },
  })
}

export function useDeleteSingleTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: KEYS.today })
      void queryClient.invalidateQueries({ queryKey: KEYS.pending })
    },
    onError: () => {
      toast.error('Erro ao excluir tarefa. Tente novamente.')
    },
  })
}
