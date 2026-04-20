// ── Auth ────────────────────────────────────────────────────────────────────

export interface AuthResponse {
  token: string
  name: string
  email: string
  expiresAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

// ── Routine / Schedule ───────────────────────────────────────────────────────

export interface TaskResponse {
  id: number
  title: string
  description: string | null
  estimatedMinutes: number | null
  orderIndex: number
  dayOfWeek: string
}

// ── Manage ───────────────────────────────────────────────────────────────────

export interface AreaResponse {
  id: number
  name: string
  color: string
  icon: string
  orderIndex: number
  tasks: TaskResponse[]
}

export interface CreateAreaRequest {
  name: string
  color: string
  icon: string
}

export interface UpdateAreaRequest {
  name: string
  color: string
  icon: string
}

export interface CreateTaskRequest {
  title: string
  description: string | null
  estimatedMinutes: number | null
  dayOfWeek: string
}

export interface UpdateTaskRequest {
  title: string
  description: string | null
  estimatedMinutes: number | null
  dayOfWeek: string
}

export interface AreaWithTasksResponse {
  id: number
  name: string
  color: string
  icon: string
  tasks: TaskResponse[]
}

export interface DayScheduleResponse {
  dayOfWeek: string
  areas: AreaWithTasksResponse[]
}

export interface ActiveRoutineResponse {
  id: number
  name: string
  areas: AreaWithTasksResponse[]
}

// ── Check-in / Progress ──────────────────────────────────────────────────────

export interface DailyLogResponse {
  id: number
  taskId: number
  logDate: string
  completed: boolean
  completedAt: string | null
}

export interface AreaProgressResponse {
  areaId: number
  areaName: string
  color: string
  icon: string
  totalTasks: number
  completedTasks: number
  completionRate: number
}

export interface DailyProgressResponse {
  logDate: string
  areas: AreaProgressResponse[]
  overallCompletionRate: number
}

// ── Analytics — Streaks ──────────────────────────────────────────────────────

export interface StreakResponse {
  areaId: number
  areaName: string
  color: string
  icon: string
  currentStreak: number
  lastActiveDate: string | null
}

export interface StreakListResponse {
  streaks: StreakResponse[]
}

// ── Analytics — Heatmap ──────────────────────────────────────────────────────

export interface HeatmapDayResponse {
  date: string
  completedTasks: number
  totalTasks: number
  completionRate: number
}

export interface HeatmapResponse {
  from: string
  to: string
  days: HeatmapDayResponse[]
  peakDay: HeatmapDayResponse | null
}

// ── Analytics — Weekly ───────────────────────────────────────────────────────

export interface WeeklyAreaCompletion {
  areaId: number
  areaName: string
  color: string
  icon: string
  completedTasks: number
  totalTasks: number
  completionRate: number
}

export interface WeeklyCompletionResponse {
  weekStart: string
  weekEnd: string
  areas: WeeklyAreaCompletion[]
  overallRate: number
}

export interface WeekDelta {
  areaId: number
  areaName: string
  currentRate: number
  previousRate: number | null
  delta: number | null
}

export interface WeekComparisonResponse {
  currentWeek: WeeklyCompletionResponse
  previousWeek: WeeklyCompletionResponse
  deltas: WeekDelta[]
}

export interface WeeklyHistoryResponse {
  weeks: WeeklyCompletionResponse[]
}
