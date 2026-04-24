// ── HabitNow backup parser ────────────────────────────────────────────────────
//
// Format overview:
//   B[timestamp]{sections}
//   Section H = habits, separated by |, each habit has 13 semicolon-delimited fields:
//     [0]  id
//     [1]  id2
//     [2]  name
//     [3]  categoryId
//     [4]  color (e.g. "0a71")
//     [5]  dateRange — B[base36 days since 2012-01-01], one date = active, two = archived
//     [6]  description
//     [7]  "0"
//     [8]  frequency ("01"=scheduled, "00"=any day, "10"=countable)
//     [9]  "01" or "00"
//     [10] empty
//     [11] days ("2 3 4 5 6" = Mon–Fri; "" = every day)
//           1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
//     [12] target count
//
// Date decoding: days are base-36 encoded, epoch = 2012-01-01

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface HabitNowHabit {
  id: string
  name: string
  description: string
  days: DayOfWeek[]   // empty means every day
  isActive: boolean
  startDate: string   // ISO yyyy-MM-dd
}

// HabitNow epoch: 2012-01-01
const EPOCH = new Date('2012-01-01T00:00:00.000Z')

function decodeDate(base36Days: string): string {
  const days = parseInt(base36Days, 36)
  const ms = EPOCH.getTime() + days * 24 * 60 * 60 * 1000
  return new Date(ms).toISOString().split('T')[0]
}

// Maps HabitNow day codes (1-7) to DayOfWeek — 1=Sun, 2=Mon ... 7=Sat
const HN_DAY_MAP: Record<string, DayOfWeek> = {
  '2': 'MONDAY',
  '3': 'TUESDAY',
  '4': 'WEDNESDAY',
  '5': 'THURSDAY',
  '6': 'FRIDAY',
  '7': 'SATURDAY',
  '1': 'SUNDAY',
}

const ALL_DAYS: DayOfWeek[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
]

function parseDays(rawDays: string): DayOfWeek[] {
  const trimmed = rawDays.trim()
  if (!trimmed) return [] // empty = every day
  return trimmed
    .split(/\s+/)
    .map((d) => HN_DAY_MAP[d])
    .filter((d): d is DayOfWeek => d !== undefined)
}

function parseHabit(raw: string): HabitNowHabit | null {
  const fields = raw.split(';')
  if (fields.length < 12) return null

  const id          = fields[0] ?? ''
  const name        = fields[2]?.trim() ?? ''
  const description = fields[6]?.trim() ?? ''
  const dateRange   = fields[5] ?? ''
  const rawDays     = fields[11] ?? ''

  if (!name) return null

  // Extract date tokens — format: B[code] or B[code]B[code]
  const dateTokens = dateRange.match(/[^B]+/g) ?? []
  const isActive   = dateTokens.length === 1
  const startDate  = dateTokens.length > 0 ? decodeDate(dateTokens[0] ?? '') : ''

  return {
    id,
    name,
    description,
    days: parseDays(rawDays),
    isActive,
    startDate,
  }
}

export function parseHabitNowBackup(content: string): HabitNowHabit[] {
  // Extract the H section: everything between {H and the next { or end of string
  const hSection = content.match(/\{H([^{]*)/)?.[1]
  if (!hSection) return []

  return hSection
    .split('|')
    .map((raw) => parseHabit(raw.trim()))
    .filter((h): h is HabitNowHabit => h !== null)
}

// ── YAML generator ────────────────────────────────────────────────────────────

function indent(level: number): string {
  return '  '.repeat(level)
}

function escapeYamlString(value: string): string {
  // Wrap in double-quotes if contains special chars
  if (/[:#\[\]{}&*!|>'"@`%]/.test(value) || value.includes('\n') || value.trim() !== value) {
    return '"' + value.replace(/"/g, '\\"') + '"'
  }
  return value
}

export function generateRoutineYaml(habits: HabitNowHabit[], routineName: string): string {
  const lines: string[] = []

  lines.push('routine:')
  lines.push(`${indent(1)}name: ${escapeYamlString(routineName)}`)
  lines.push(`${indent(1)}areas:`)

  for (const habit of habits) {
    const effectiveDays = habit.days.length > 0 ? habit.days : ALL_DAYS

    lines.push(`${indent(2)}- name: ${escapeYamlString(habit.name)}`)
    lines.push(`${indent(3)}color: "#6B7280"`)
    lines.push(`${indent(3)}icon: "⭐"`)
    lines.push(`${indent(3)}schedule:`)

    for (const day of effectiveDays) {
      lines.push(`${indent(4)}${day}:`)
      lines.push(`${indent(5)}- title: ${escapeYamlString(habit.name)}`)
      if (habit.description) {
        lines.push(`${indent(6)}description: ${escapeYamlString(habit.description)}`)
      }
      lines.push(`${indent(6)}estimatedMinutes: 0`)
    }
  }

  return lines.join('\n') + '\n'
}
