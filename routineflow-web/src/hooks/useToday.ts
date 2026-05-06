import { useEffect, useMemo, useState } from 'react'
import { useQueries } from '@tanstack/react-query'
import { toast } from 'sonner'
import { routineApi, checkInApi } from '@/services/api'
import type { AreaWithTasksResponse, TaskResponse } from '@/types'

// ── Enriched types ────────────────────────────────────────────────────────────

export interface EnrichedTask extends TaskResponse {
  completed: boolean
  completedAt: string | null
}

export interface EnrichedArea extends Omit<AreaWithTasksResponse, 'tasks'> {
  tasks: EnrichedTask[]
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function computeOverallRate(areas: EnrichedArea[]): number {
  let total = 0
  let done = 0
  for (const area of areas) {
    total += area.tasks.length
    done += area.tasks.filter((t) => t.completed).length
  }
  return total > 0 ? done / total : 0
}

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useToday() {
  // Source of truth for checkbox state.
  // Initialized once from server counts; never overwritten by subsequent refetches.
  const [localChecked, setLocalChecked] = useState<Map<number, boolean>>(new Map())
  const [initialized, setInitialized] = useState(false)

  const [scheduleQuery, progressQuery] = useQueries({
    queries: [
      {
        queryKey: ['today-schedule'],
        queryFn: routineApi.getToday,
        staleTime: 5 * 60 * 1000,   // 5 min — avoid background refetches
        refetchOnWindowFocus: false, // tab-switching never resets checkboxes
      },
      {
        queryKey: ['today-progress'],
        queryFn: checkInApi.getTodayProgress,
        staleTime: 5 * 60 * 1000,
        refetchOnWindowFocus: false,
      },
    ],
  })

  // Initialize localChecked exactly once — after both queries have resolved.
  // Uses completedTaskIds from the progress response to accurately initialize
  // each task's completed state. After this point, localChecked is the
  // sole source of truth and is never overwritten by server data.
  useEffect(() => {
    if (initialized) return
    if (!scheduleQuery.data) return
    if (progressQuery.isLoading) return // wait for progress before initializing

    const completedIdsByArea = new Map<number, Set<number>>()
    for (const area of progressQuery.data?.areas ?? []) {
      completedIdsByArea.set(area.areaId, new Set(area.completedTaskIds))
    }

    const initial = new Map<number, boolean>()
    for (const area of scheduleQuery.data.areas) {
      const completedIds = completedIdsByArea.get(area.id) ?? new Set<number>()
      for (const task of area.tasks) {
        initial.set(task.id, completedIds.has(task.id))
      }
    }

    setLocalChecked(initial)
    setInitialized(true)
  }, [scheduleQuery.data, progressQuery.data, progressQuery.isLoading, initialized])

  // Derive enriched areas from schedule data + localChecked.
  // Recomputes on any localChecked mutation — never touches server state.
  const enrichedAreas = useMemo((): EnrichedArea[] => {
    if (!scheduleQuery.data) return []
    return scheduleQuery.data.areas.map((area) => {
      const sorted = [...area.tasks].sort((a, b) => a.orderIndex - b.orderIndex)
      return {
        ...area,
        tasks: sorted.map((task) => {
          const done = localChecked.get(task.id) ?? false
          return {
            ...task,
            completed: done,
            completedAt: done ? new Date().toISOString() : null,
          }
        }),
      }
    })
  }, [scheduleQuery.data, localChecked])

  const overallRate = computeOverallRate(enrichedAreas)
  const isLoading = scheduleQuery.isLoading || progressQuery.isLoading
  const error = scheduleQuery.error ?? progressQuery.error

  // ── Mutations ────────────────────────────────────────────────────────────────
  // Update localChecked immediately (optimistic).
  // Revert on API failure — no invalidateQueries needed since localChecked
  // is the source of truth and server refetches no longer overwrite it.

  async function completeTask(taskId: number) {
    setLocalChecked((prev) => new Map(prev).set(taskId, true))
    try {
      await checkInApi.complete(taskId)
    } catch {
      setLocalChecked((prev) => new Map(prev).set(taskId, false))
      toast.error('Erro ao marcar tarefa. Tente novamente.')
    }
  }

  async function uncompleteTask(taskId: number) {
    setLocalChecked((prev) => new Map(prev).set(taskId, false))
    try {
      await checkInApi.uncomplete(taskId)
    } catch {
      setLocalChecked((prev) => new Map(prev).set(taskId, true))
      toast.error('Erro ao desmarcar tarefa. Tente novamente.')
    }
  }

  function handleTaskToggle(taskId: number, completed: boolean) {
    if (completed) {
      void completeTask(taskId)
    } else {
      void uncompleteTask(taskId)
    }
  }

  return {
    enrichedAreas,
    overallRate,
    isLoading,
    error,
    handleTaskToggle,
  }
}
