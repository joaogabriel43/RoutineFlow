// ── Import ───────────────────────────────────────────────────────────────────

export type ImportMode = 'REPLACE' | 'MERGE'

export interface ImportRoutineResponse {
  routineId: number
  name: string
  totalAreas: number
  totalTasks: number
  importedAt: string
  mode: ImportMode
  areasCreated: number
  areasMerged: number
  tasksCreated: number
  tasksSkipped: number
}

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

export type ScheduleType = 'DAY_OF_WEEK' | 'DAY_OF_MONTH'

export interface TaskResponse {
  id: number
  title: string
  description: string | null
  estimatedMinutes: number | null
  orderIndex: number
  scheduleType: ScheduleType
  dayOfWeek: string | null
  dayOfMonth: number | null
}

// ── Manage ───────────────────────────────────────────────────────────────────

export type ResetFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY'

export interface AreaResponse {
  id: number
  name: string
  color: string
  icon: string
  orderIndex: number
  resetFrequency: ResetFrequency
  tasks: TaskResponse[]
}

export interface CreateAreaRequest {
  name: string
  color: string
  icon: string
  resetFrequency?: ResetFrequency
}

export interface UpdateAreaRequest {
  name: string
  color: string
  icon: string
  resetFrequency?: ResetFrequency
}

export interface CreateTaskRequest {
  title: string
  description: string | null
  estimatedMinutes: number | null
  scheduleType: ScheduleType
  dayOfWeek: string | null
  dayOfMonth: number | null
}

export interface UpdateTaskRequest {
  title: string
  description: string | null
  estimatedMinutes: number | null
  scheduleType: ScheduleType
  dayOfWeek: string | null
  dayOfMonth: number | null
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
  completedTaskIds: number[]
}

export interface DailyProgressResponse {
  logDate: string
  areas: AreaProgressResponse[]
  overallCompletionRate: number
}

// ── Analytics — Area individual ──────────────────────────────────────────────

export interface DayOfWeekStat {
  dayOfWeek: string
  dayLabel: string
  completedCount: number
  completionRate: number
}

export interface WeeklyTrendPoint {
  weekStart: string
  weekLabel: string
  completedTasks: number
  totalTasks: number
  completionRate: number
}

export interface AreaAnalyticsResponse {
  areaId: number
  areaName: string
  color: string
  icon: string
  resetFrequency: ResetFrequency
  totalCheckIns: number
  totalExpected: number
  overallCompletionRate: number
  currentStreak: number
  bestStreak: number
  weeklyTrend: WeeklyTrendPoint[]
  dayOfWeekStats: DayOfWeekStat[]
  bestDayOfWeek: string | null
  bestDayLabel: string | null
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

// ── Single Tasks ──────────────────────────────────────────────────────────────

export interface SingleTaskResponse {
  id: number
  title: string
  description: string | null
  dueDate: string | null       // yyyy-MM-dd
  completed: boolean
  completedAt: string | null
  archivedAt: string | null
  createdAt: string
  isOverdue: boolean
}

export interface CreateSingleTaskRequest {
  title: string
  description: string | null
  dueDate: string | null
}
