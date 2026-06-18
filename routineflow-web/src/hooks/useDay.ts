import { useEffect, useMemo, useState } from 'react'
import { useQueries } from '@tanstack/react-query'
import { toast } from 'sonner'
import { routineApi, checkInApi } from '@/services/api'
import type { AreaWithTasksResponse, TaskResponse } from '@/types'

// ── Enriched types ────────────────────────────────────────────────────────────

export interface EnrichedTask extends TaskResponse {
  completed: boolean
  completedAt: string | null
  notes: string | null
}

export interface EnrichedArea extends Omit<AreaWithTasksResponse, 'tasks'> {
  tasks: EnrichedTask[]
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const DOW_NAMES = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

function todayStr(): string {
  return new Date().toISOString().split('T')[0] as string
}

/** Convert YYYY-MM-DD to Java DayOfWeek enum name. Uses T12:00:00 to avoid TZ shift. */
function toDayOfWeek(dateStr: string): string {
  const d = new Date(dateStr + 'T12:00:00')
  return DOW_NAMES[d.getDay()] as string
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

export function useDay(selectedDate: string) {
  const today = todayStr()
  const isFuture = selectedDate > today
  const dayOfWeek = toDayOfWeek(selectedDate)

  // Local checkbox state — reinitialize whenever the selected date changes.
  const [localChecked, setLocalChecked] = useState<Map<number, boolean>>(new Map())
  const [localNotes, setLocalNotes] = useState<Map<number, string>>(new Map())
  const [initialized, setInitialized] = useState(false)

  // Reset local state on date change so we re-initialize from fresh server data.
  useEffect(() => {
    setLocalChecked(new Map())
    setLocalNotes(new Map())
    setInitialized(false)
  }, [selectedDate])

  const [scheduleQuery, progressQuery] = useQueries({
    queries: [
      {
        queryKey: ['day-schedule', dayOfWeek],
        queryFn: () => routineApi.getDay(dayOfWeek),
        staleTime: 5 * 60 * 1000,
        refetchOnWindowFocus: false,
      },
      {
        queryKey: ['day-progress', selectedDate],
        queryFn: () => checkInApi.getDayProgress(selectedDate),
        staleTime: 5 * 60 * 1000,
        refetchOnWindowFocus: false,
      },
    ],
  })

  // Initialize localChecked exactly once per date — after both queries resolve.
  // Uses completedTaskIds from the progress response to accurately initialize
  // each task's completed state. After this point, localChecked is the
  // sole source of truth and is never overwritten by server data.
  useEffect(() => {
    if (initialized) return
    if (!scheduleQuery.data) return
    if (progressQuery.isLoading) return

    const completedIdsByArea = new Map<number, Set<number>>()
    const initialNotes = new Map<number, string>()
    for (const area of progressQuery.data?.areas ?? []) {
      completedIdsByArea.set(area.areaId, new Set(area.completedTaskIds))
      if (area.taskNotes) {
        Object.entries(area.taskNotes).forEach(([idStr, note]) => {
          initialNotes.set(Number(idStr), note)
        })
      }
    }

    const initial = new Map<number, boolean>()
    for (const area of scheduleQuery.data.areas) {
      const completedIds = completedIdsByArea.get(area.id) ?? new Set<number>()
      for (const task of area.tasks) {
        initial.set(task.id, completedIds.has(task.id))
      }
    }

    setLocalChecked(initial)
    setLocalNotes(initialNotes)
    setInitialized(true)
  }, [scheduleQuery.data, progressQuery.data, progressQuery.isLoading, initialized])

  const enrichedAreas = useMemo((): EnrichedArea[] => {
    if (!scheduleQuery.data) return []
    return scheduleQuery.data.areas.map((area) => {
      const sorted = [...area.tasks].sort((a, b) => a.orderIndex - b.orderIndex)
      return {
        ...area,
        tasks: sorted.map((task) => {
          const done = localChecked.get(task.id) ?? false
          const note = localNotes.get(task.id) ?? null
          return {
            ...task,
            completed: done,
            completedAt: done ? new Date().toISOString() : null,
            notes: note,
          }
        }),
      }
    })
  }, [scheduleQuery.data, localChecked, localNotes])

  const overallRate = computeOverallRate(enrichedAreas)
  const isLoading = scheduleQuery.isLoading || progressQuery.isLoading
  const error = scheduleQuery.error ?? progressQuery.error

  // ── Mutations ─────────────────────────────────────────────────────────────────

  async function completeTask(taskId: number, notes?: string) {
    if (isFuture) return // future dates: read-only

    setLocalChecked((prev) => new Map(prev).set(taskId, true))
    if (notes) {
      setLocalNotes((prev) => new Map(prev).set(taskId, notes))
    }
    try {
      await checkInApi.complete(taskId, selectedDate, notes)
    } catch {
      setLocalChecked((prev) => new Map(prev).set(taskId, false))
      if (notes) {
        setLocalNotes((prev) => {
          const m = new Map(prev)
          m.delete(taskId)
          return m
        })
      }
      toast.error('Erro ao marcar tarefa. Tente novamente.')
    }
  }

  async function uncompleteTask(taskId: number) {
    if (isFuture) return

    setLocalChecked((prev) => new Map(prev).set(taskId, false))
    try {
      await checkInApi.uncomplete(taskId, selectedDate)
    } catch {
      setLocalChecked((prev) => new Map(prev).set(taskId, true))
      toast.error('Erro ao desmarcar tarefa. Tente novamente.')
    }
  }

  function handleTaskToggle(taskId: number, completed: boolean, notes?: string) {
    if (isFuture) return
    if (completed) {
      void completeTask(taskId, notes)
    } else {
      void uncompleteTask(taskId)
    }
  }

  async function updateNotes(taskId: number, notes: string) {
    if (isFuture) return
    setLocalNotes((prev) => new Map(prev).set(taskId, notes))
    try {
      await checkInApi.updateNotes(taskId, selectedDate, notes)
    } catch {
      // Revert if needed
      toast.error('Erro ao salvar nota.')
    }
  }

  return {
    enrichedAreas,
    overallRate,
    isLoading,
    error,
    isFuture,
    handleTaskToggle,
    updateNotes,
  }
}
