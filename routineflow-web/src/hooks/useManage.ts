import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { manageApi } from '@/services/api'
import type { CreateAreaRequest, UpdateAreaRequest, CreateTaskRequest, UpdateTaskRequest } from '@/types'

const AREAS_KEY = ['areas'] as const

export function useManage() {
  const queryClient = useQueryClient()

  const invalidate = () => queryClient.invalidateQueries({ queryKey: AREAS_KEY })

  // ── Query ─────────────────────────────────────────────────────────────────

  const areasQuery = useQuery({
    queryKey: AREAS_KEY,
    queryFn: manageApi.getAreas,
    retry: (count, error: unknown) => {
      // Don't retry on 404 (no active routine)
      const status = (error as { response?: { status?: number } })?.response?.status
      if (status === 404) return false
      return count < 1
    },
  })

  // ── Area mutations ────────────────────────────────────────────────────────

  const createArea = useMutation({
    mutationFn: (data: CreateAreaRequest) => manageApi.createArea(data),
    onSuccess: () => { toast.success('Área criada com sucesso'); void invalidate() },
    onError: () => toast.error('Erro ao criar área'),
  })

  const updateArea = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateAreaRequest }) =>
      manageApi.updateArea(id, data),
    onSuccess: () => { toast.success('Área atualizada'); void invalidate() },
    onError: () => toast.error('Erro ao atualizar área'),
  })

  const deleteArea = useMutation({
    mutationFn: (id: number) => manageApi.deleteArea(id),
    onSuccess: () => { toast.success('Área excluída'); void invalidate() },
    onError: () => toast.error('Erro ao excluir área'),
  })

  const reorderAreas = useMutation({
    mutationFn: (areaIds: number[]) => manageApi.reorderAreas(areaIds),
    onSuccess: () => void invalidate(),
    onError: () => toast.error('Erro ao reordenar áreas'),
  })

  // ── Task mutations ────────────────────────────────────────────────────────

  const createTask = useMutation({
    mutationFn: ({ areaId, data }: { areaId: number; data: CreateTaskRequest }) =>
      manageApi.createTask(areaId, data),
    onSuccess: () => { toast.success('Tarefa criada'); void invalidate() },
    onError: () => toast.error('Erro ao criar tarefa'),
  })

  const updateTask = useMutation({
    mutationFn: ({ areaId, taskId, data }: { areaId: number; taskId: number; data: UpdateTaskRequest }) =>
      manageApi.updateTask(areaId, taskId, data),
    onSuccess: () => { toast.success('Tarefa atualizada'); void invalidate() },
    onError: () => toast.error('Erro ao atualizar tarefa'),
  })

  const deleteTask = useMutation({
    mutationFn: ({ areaId, taskId }: { areaId: number; taskId: number }) =>
      manageApi.deleteTask(areaId, taskId),
    onSuccess: () => { toast.success('Tarefa excluída'); void invalidate() },
    onError: () => toast.error('Erro ao excluir tarefa'),
  })

  const reorderTasks = useMutation({
    mutationFn: ({ areaId, taskIds }: { areaId: number; taskIds: number[] }) =>
      manageApi.reorderTasks(areaId, taskIds),
    onSuccess: () => void invalidate(),
    onError: () => toast.error('Erro ao reordenar tarefas'),
  })

  return {
    areas: areasQuery.data ?? [],
    isLoading: areasQuery.isLoading,
    error: areasQuery.error,
    createArea,
    updateArea,
    deleteArea,
    reorderAreas,
    createTask,
    updateTask,
    deleteTask,
    reorderTasks,
  }
}
