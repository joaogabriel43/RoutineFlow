import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { singleTaskApi } from '@/services/api'
import type { CreateSingleTaskRequest, SingleTaskResponse } from '@/types'

// ── Query keys ────────────────────────────────────────────────────────────────

export const SINGLE_TASKS_KEYS = {
  pending:  ['single-tasks', 'pending']  as const,
  archived: ['single-tasks', 'archived'] as const,
}

// ── Queries ───────────────────────────────────────────────────────────────────

export function usePendingSingleTasks() {
  return useQuery({
    queryKey: SINGLE_TASKS_KEYS.pending,
    queryFn: singleTaskApi.listPending,
  })
}

export function useArchivedSingleTasks() {
  return useQuery({
    queryKey: SINGLE_TASKS_KEYS.archived,
    queryFn: singleTaskApi.listArchived,
  })
}

// ── Mutations ─────────────────────────────────────────────────────────────────

export function useCreateSingleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateSingleTaskRequest) => singleTaskApi.create(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
    },
  })
}

export function useCompleteSingleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.complete(id),
    // Optimistic update: remove from pending immediately
    onMutate: async (id) => {
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      const previous = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.pending)
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.pending,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      return { previous }
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.previous) {
        qc.setQueryData(SINGLE_TASKS_KEYS.pending, ctx.previous)
      }
    },
    onSettled: () => {
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
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.archived })
    },
  })
}

export function useDeleteSingleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => singleTaskApi.remove(id),
    onMutate: async (id) => {
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      await qc.cancelQueries({ queryKey: SINGLE_TASKS_KEYS.archived })
      const prevPending  = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.pending)
      const prevArchived = qc.getQueryData<SingleTaskResponse[]>(SINGLE_TASKS_KEYS.archived)
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.pending,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      qc.setQueryData<SingleTaskResponse[]>(
        SINGLE_TASKS_KEYS.archived,
        (old) => old?.filter((t) => t.id !== id) ?? [],
      )
      return { prevPending, prevArchived }
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.prevPending)  qc.setQueryData(SINGLE_TASKS_KEYS.pending,  ctx.prevPending)
      if (ctx?.prevArchived) qc.setQueryData(SINGLE_TASKS_KEYS.archived, ctx.prevArchived)
    },
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.pending })
      void qc.invalidateQueries({ queryKey: SINGLE_TASKS_KEYS.archived })
    },
  })
}
