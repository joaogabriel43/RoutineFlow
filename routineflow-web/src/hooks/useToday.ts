import { useEffect, useState } from 'react'
import { useQueries, useQueryClient } from '@tanstack/react-query'
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

function buildEnrichedAreas(
  scheduleAreas: AreaWithTasksResponse[],
  completedCounts: Map<number, number>,
): EnrichedArea[] {
  return scheduleAreas.map((area) => {
    const completed = completedCounts.get(area.id) ?? 0
    const sorted = [...area.tasks].sort((a, b) => a.orderIndex - b.orderIndex)
    return {
      ...area,
      tasks: sorted.map((task, idx) => ({
        ...task,
        completed: idx < completed,
        completedAt: idx < completed ? new Date().toISOString() : null,
      })),
    }
  })
}

function setTaskCompletion(
  areas: EnrichedArea[],
  taskId: number,
  completed: boolean,
): EnrichedArea[] {
  return areas.map((area) => ({
    ...area,
    tasks: area.tasks.map((task) =>
      task.id === taskId
        ? { ...task, completed, completedAt: completed ? new Date().toISOString() : null }
        : task,
    ),
  }))
}

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
  const queryClient = useQueryClient()
  const [enrichedAreas, setEnrichedAreas] = useState<EnrichedArea[]>([])

  const [scheduleQuery, progressQuery] = useQueries({
    queries: [
      {
        queryKey: ['today-schedule'],
        queryFn: routineApi.getToday,
        staleTime: 60_000,
      },
      {
        queryKey: ['today-progress'],
        queryFn: checkInApi.getTodayProgress,
        staleTime: 30_000,
      },
    ],
  })

  // Merge schedule + progress whenever either changes
  useEffect(() => {
    if (!scheduleQuery.data) return

    const completedCounts = new Map<number, number>()
    for (const area of progressQuery.data?.areas ?? []) {
      completedCounts.set(area.areaId, area.completedTasks)
    }

    setEnrichedAreas(buildEnrichedAreas(scheduleQuery.data.areas, completedCounts))
  }, [scheduleQuery.data, progressQuery.data])

  // Derive overallRate from local state for immediate feedback on toggle
  const overallRate = computeOverallRate(enrichedAreas)

  const isLoading = scheduleQuery.isLoading || progressQuery.isLoading
  const error = scheduleQuery.error ?? progressQuery.error

  // ── Mutations ───────────────────────────────────────────────────────────────

  async function completeTask(taskId: number) {
    // Optimistic: update local state immediately
    setEnrichedAreas((prev) => setTaskCompletion(prev, taskId, true))
    try {
      await checkInApi.complete(taskId)
      // Sync overallRate with server silently
      void queryClient.invalidateQueries({ queryKey: ['today-progress'] })
    } catch {
      // Revert on failure
      setEnrichedAreas((prev) => setTaskCompletion(prev, taskId, false))
      toast.error('Erro ao marcar tarefa. Tente novamente.')
    }
  }

  async function uncompleteTask(taskId: number) {
    setEnrichedAreas((prev) => setTaskCompletion(prev, taskId, false))
    try {
      await checkInApi.uncomplete(taskId)
      void queryClient.invalidateQueries({ queryKey: ['today-progress'] })
    } catch {
      setEnrichedAreas((prev) => setTaskCompletion(prev, taskId, true))
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
