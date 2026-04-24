import axios from 'axios'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  DayScheduleResponse,
  ActiveRoutineResponse,
  DailyLogResponse,
  DailyProgressResponse,
  StreakListResponse,
  HeatmapResponse,
  WeeklyCompletionResponse,
  WeekComparisonResponse,
  WeeklyHistoryResponse,
  AreaResponse,
  AreaAnalyticsResponse,
  CreateAreaRequest,
  UpdateAreaRequest,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskResponse,
} from '@/types'

// ── Axios instance ────────────────────────────────────────────────────────────

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

// ── Request interceptor: attach JWT ──────────────────────────────────────────

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('rf_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor: handle 401 ─────────────────────────────────────────

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('rf_token')
      localStorage.removeItem('rf_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

// ── Auth ──────────────────────────────────────────────────────────────────────

export const authApi = {
  register: (data: RegisterRequest) =>
    api.post<AuthResponse>('/auth/register', data).then((r) => r.data),

  login: (data: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', data).then((r) => r.data),
}

// ── Routine ───────────────────────────────────────────────────────────────────

export const routineApi = {
  importRoutine: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.post<void>('/routines/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  getActive: () =>
    api.get<ActiveRoutineResponse>('/routines/active').then((r) => r.data),

  getToday: () =>
    api.get<DayScheduleResponse>('/routines/active/today').then((r) => r.data),

  getDay: (dayOfWeek: string) =>
    api.get<DayScheduleResponse>(`/routines/active/day/${dayOfWeek}`).then((r) => r.data),
}

// ── Check-in ──────────────────────────────────────────────────────────────────

export const checkInApi = {
  complete: (taskId: number) =>
    api.post<DailyLogResponse>(`/checkins/${taskId}/complete`).then((r) => r.data),

  uncomplete: (taskId: number) =>
    api.post<DailyLogResponse>(`/checkins/${taskId}/uncomplete`).then((r) => r.data),

  getTodayProgress: () =>
    api.get<DailyProgressResponse>('/checkins/today/progress').then((r) => r.data),
}

// ── Analytics ─────────────────────────────────────────────────────────────────

export const analyticsApi = {
  getStreaks: () =>
    api.get<StreakListResponse>('/analytics/streaks').then((r) => r.data),

  getHeatmap: (params?: { from?: string; to?: string }) =>
    api.get<HeatmapResponse>('/analytics/heatmap', { params }).then((r) => r.data),

  getCurrentWeek: () =>
    api.get<WeeklyCompletionResponse>('/analytics/weekly/current').then((r) => r.data),

  getWeekComparison: () =>
    api.get<WeekComparisonResponse>('/analytics/weekly/comparison').then((r) => r.data),

  getWeeklyHistory: (weeks = 8) =>
    api.get<WeeklyHistoryResponse>('/analytics/weekly/history', { params: { weeks } }).then((r) => r.data),

  getAreaAnalytics: (areaId: number) =>
    api.get<AreaAnalyticsResponse>(`/areas/${areaId}/analytics`).then((r) => r.data),
}

// ── Export ────────────────────────────────────────────────────────────────────

export const exportApi = {
  exportCheckIns: async (from?: string, to?: string): Promise<void> => {
    const params = new URLSearchParams()
    if (from) params.append('from', from)
    if (to) params.append('to', to)

    const response = await api.get(`/export/checkins?${params.toString()}`, {
      responseType: 'blob',
    })

    const url = window.URL.createObjectURL(new Blob([response.data as BlobPart]))
    const link = document.createElement('a')
    link.href = url
    const date = new Date().toISOString().split('T')[0]
    link.setAttribute('download', `routineflow-export-${date}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  },
}

// ── Manage (Areas + Tasks CRUD) ───────────────────────────────────────────────

export const manageApi = {
  // Areas
  getAreas: () =>
    api.get<AreaResponse[]>('/areas').then((r) => r.data),

  createArea: (data: CreateAreaRequest) =>
    api.post<AreaResponse>('/areas', data).then((r) => r.data),

  updateArea: (id: number, data: UpdateAreaRequest) =>
    api.put<AreaResponse>(`/areas/${id}`, data).then((r) => r.data),

  deleteArea: (id: number) =>
    api.delete<void>(`/areas/${id}`),

  reorderAreas: (areaIds: number[]) =>
    api.patch<AreaResponse[]>('/areas/reorder', { areaIds }).then((r) => r.data),

  // Tasks (nested under area)
  createTask: (areaId: number, data: CreateTaskRequest) =>
    api.post<TaskResponse>(`/areas/${areaId}/tasks`, data).then((r) => r.data),

  updateTask: (areaId: number, taskId: number, data: UpdateTaskRequest) =>
    api.put<TaskResponse>(`/areas/${areaId}/tasks/${taskId}`, data).then((r) => r.data),

  deleteTask: (areaId: number, taskId: number) =>
    api.delete<void>(`/areas/${areaId}/tasks/${taskId}`),

  reorderTasks: (areaId: number, taskIds: number[]) =>
    api.patch<TaskResponse[]>(`/areas/${areaId}/tasks/reorder`, { taskIds }).then((r) => r.data),
}

export default api
