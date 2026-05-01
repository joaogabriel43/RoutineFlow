import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { singleTaskApi } from '@/services/api'
import type { CreateSingleTaskRequest, SingleTaskResponse } from '@/types'

// ── Query keys ────────────────────────────────────────────────────────────────

export const SINGLE_TASKS_KEYS = {
  today:    ['single-tasks', 'today']    as const,
  pending:  ['single-tasks', 'pending']  as const,
  archived: ['single-tasks', 'archived'] as const,
}

// ── Queries ───────────────────────────────────────────────────────────────────

export function useSingleTasksToday() {
  return useQuery({
    queryKey: SINGLE_TASKS_KEYS.today,
    queryFn: singleTaskApi.listToday,
    staleTime: 60_000,
  })
}

export function usePendingSingleTasks() {
  return useQuery({
    queryKey: SINGLE_TASKS_KEYS.pending,
    queryFn: singleTaskApi.listPending,
    staleTime: 60_000,
  })
}

export function useArchivedSingleTasks() {
  return useQuery({
    queryKey: SINGLE_TASKS_KEYS.archived,
    queryFn: singleTaskApi.listArchived,
    staleTime: 60_000,
  })
}

// ── Mutations ─────────────────────────────────────────────────────────────────

export function useCreateSingleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateSingleTaskRequest) => singleTaskApi.create(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.today })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      toast.success('Tarefa adicionada!')
    },
    onError: () => {
      toast.error('Erro ao criar tarefa. Tente novamente.')
    },
  })
}

export function useCompleteSingleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.complete(id),
    // Optimistic update: remove from today and pending immediately
    onMutate: async (id) => {
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.today })
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      const prevToday   = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.today)
      const prevPending = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.pending)
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.today,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.pending,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      return { prevToday, prevPending }
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.prevToday)   qc.setQueryData(SINGLE_TASKS_KEYS.today,   ctx.prevToday)
      if (ctx?.prevPending) qc.setQueryData(SINGLE_TASKS_KEYS.pending, ctx.prevPending)
      toast.error('Erro ao concluir tarefa. Tente novamente.')
    },
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.today })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.archived })
    },
  })
}

export function useUncompleteSingleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.uncomplete(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.today })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.archived })
    },
    onError: () => {
      toast.error('Erro ao desfazer conclusão. Tente novamente.')
    },
  })
}

export function useDeleteSingleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.remove(id),
    onMutate: async (id) => {
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.today })
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.archived })
      const prevToday   = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.today)
      const prevPending = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.pending)
      const prevArchived = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.archived)
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.today,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.pending,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.archived,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      return { prevToday, prevPending, prevArchived }
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.prevToday)    qc.setQueryData(SINGLE_TASKS_KEYS.today,    ctx.prevToday)
      if (ctx?.prevPending)  qc.setQueryData(SINGLE_TASKS_KEYS.pending,  ctx.prevPending)
      if (ctx?.prevArchived) qc.setQueryData(SINGLE_TASKS_KEYS.archived, ctx.prevArchived)
      toast.error('Erro ao excluir tarefa. Tente novamente.')
    },
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.today })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.archived })
    },
  })
}
