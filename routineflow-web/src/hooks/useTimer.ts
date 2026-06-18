import { useState, useEffect, useCallback, useRef } from 'react'

export interface UseTimerProps {
  taskId: number
  estimatedMinutes: number // 0 for stopwatch
  dateStr: string // used to separate days in localStorage
  onFinish?: () => void
}

interface TimerStorage {
  taskId: number
  isRunning: boolean
  startedAt: number | null // timestamp ms
  elapsedBeforeStart: number // seconds accumulated before last start
}

export function useTimer({ taskId, estimatedMinutes, dateStr, onFinish }: UseTimerProps) {
  const initialSeconds = estimatedMinutes * 60
  const isCountdown = estimatedMinutes > 0
  const storageKey = `timer_${taskId}_${dateStr}`

  const [isRunning, setIsRunning] = useState(false)
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [isFinished, setIsFinished] = useState(false)
  const onFinishRef = useRef(onFinish)

  useEffect(() => {
    onFinishRef.current = onFinish
  }, [onFinish])

  // Reference for precise time calculation
  const stateRef = useRef<{ isRunning: boolean; startedAt: number | null; elapsedBeforeStart: number }>({
    isRunning: false,
    startedAt: null,
    elapsedBeforeStart: 0,
  })

  const persist = useCallback(() => {
    localStorage.setItem(storageKey, JSON.stringify({
      taskId,
      isRunning: stateRef.current.isRunning,
      startedAt: stateRef.current.startedAt,
      elapsedBeforeStart: stateRef.current.elapsedBeforeStart,
    }))
  }, [storageKey, taskId])

  // Load state from localStorage on mount
  useEffect(() => {
    try {
      const stored = localStorage.getItem(storageKey)
      if (stored) {
        const parsed = JSON.parse(stored) as TimerStorage
        const now = Date.now()
        
        let currentElapsed = parsed.elapsedBeforeStart || 0
        if (parsed.isRunning && parsed.startedAt) {
          const diffSeconds = Math.floor((now - parsed.startedAt) / 1000)
          currentElapsed += diffSeconds
        }

        if (isCountdown && currentElapsed >= initialSeconds) {
          currentElapsed = initialSeconds
          setIsFinished(true)
          setIsRunning(false)
          stateRef.current = { isRunning: false, startedAt: null, elapsedBeforeStart: currentElapsed }
          setElapsedSeconds(currentElapsed)
          // Do not call onFinish on mount to prevent unexpected side effects on reload
        } else {
          setIsFinished(false)
          setIsRunning(parsed.isRunning)
          stateRef.current = {
            isRunning: parsed.isRunning,
            startedAt: parsed.isRunning ? parsed.startedAt : null,
            elapsedBeforeStart: parsed.isRunning ? parsed.elapsedBeforeStart : currentElapsed
          }
          setElapsedSeconds(currentElapsed)
        }
      }
    } catch (e) {
      console.error('Failed to parse timer state', e)
    }
  }, [storageKey, isCountdown, initialSeconds])

  // Timer interval calculation
  useEffect(() => {
    let interval: number | null = null

    if (isRunning && !isFinished) {
      interval = window.setInterval(() => {
        const { startedAt, elapsedBeforeStart } = stateRef.current
        if (!startedAt) return

        const now = Date.now()
        const diffSeconds = Math.floor((now - startedAt) / 1000)
        const totalElapsed = elapsedBeforeStart + diffSeconds

        if (isCountdown && totalElapsed >= initialSeconds) {
          setElapsedSeconds(initialSeconds)
          setIsFinished(true)
          setIsRunning(false)
          stateRef.current = { isRunning: false, startedAt: null, elapsedBeforeStart: initialSeconds }
          persist()
          if (onFinishRef.current) onFinishRef.current()
        } else {
          setElapsedSeconds(totalElapsed)
        }
      }, 1000)
    }

    return () => {
      if (interval !== null) clearInterval(interval)
    }
  }, [isRunning, isFinished, isCountdown, initialSeconds, persist])

  const start = useCallback(() => {
    if (isFinished || isRunning) return
    stateRef.current = {
      isRunning: true,
      startedAt: Date.now(),
      elapsedBeforeStart: elapsedSeconds,
    }
    setIsRunning(true)
    persist()
  }, [isFinished, isRunning, elapsedSeconds, persist])

  const pause = useCallback(() => {
    if (!isRunning) return
    
    // Calculate final elapsed before pausing precisely
    let finalElapsed = elapsedSeconds
    if (stateRef.current.startedAt) {
      const now = Date.now()
      const diffSeconds = Math.floor((now - stateRef.current.startedAt) / 1000)
      finalElapsed = stateRef.current.elapsedBeforeStart + diffSeconds
    }

    stateRef.current = {
      isRunning: false,
      startedAt: null,
      elapsedBeforeStart: finalElapsed,
    }
    setElapsedSeconds(finalElapsed)
    setIsRunning(false)
    persist()
  }, [isRunning, elapsedSeconds, persist])

  const reset = useCallback(() => {
    setIsRunning(false)
    setIsFinished(false)
    setElapsedSeconds(0)
    stateRef.current = { isRunning: false, startedAt: null, elapsedBeforeStart: 0 }
    localStorage.removeItem(storageKey)
  }, [storageKey])

  const remainingSeconds = isCountdown ? Math.max(0, initialSeconds - elapsedSeconds) : 0

  return {
    isRunning,
    isFinished,
    elapsedSeconds,
    remainingSeconds,
    initialSeconds,
    isCountdown,
    start,
    pause,
    reset,
  }
}
