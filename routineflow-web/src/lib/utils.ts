import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatDatePtBR(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(date)
}

export function formatShortDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
  }).format(date)
}

export function formatPercent(rate: number): string {
  return `${Math.round(rate * 100)}%`
}

export const AREA_COLORS: Record<string, string> = {
  '#3B82F6': '#3B82F6',
  '#F59E0B': '#F59E0B',
  '#F43F5E': '#F43F5E',
  '#EC4899': '#EC4899',
  '#10B981': '#10B981',
  '#8B5CF6': '#8B5CF6',
  '#F97316': '#F97316',
  '#06B6D4': '#06B6D4',
}
