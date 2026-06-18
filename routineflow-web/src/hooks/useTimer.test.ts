import { renderHook, act } from '@testing-library/react'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { useTimer } from './useTimer'

describe('useTimer hook', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    localStorage.clear()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('should initialize countdown correctly', () => {
    const { result } = renderHook(() => useTimer({ taskId: 1, estimatedMinutes: 30, dateStr: '2026-06-18' }))
    expect(result.current.isRunning).toBe(false)
    expect(result.current.isFinished).toBe(false)
    expect(result.current.initialSeconds).toBe(1800)
    expect(result.current.remainingSeconds).toBe(1800)
    expect(result.current.elapsedSeconds).toBe(0)
    expect(result.current.isCountdown).toBe(true)
  })

  it('should start and pause countdown', () => {
    const { result } = renderHook(() => useTimer({ taskId: 1, estimatedMinutes: 30, dateStr: '2026-06-18' }))
    
    act(() => {
      result.current.start()
    })
    expect(result.current.isRunning).toBe(true)

    act(() => {
      vi.advanceTimersByTime(5000)
    })
    expect(result.current.elapsedSeconds).toBe(5)
    expect(result.current.remainingSeconds).toBe(1795)

    act(() => {
      result.current.pause()
    })
    expect(result.current.isRunning).toBe(false)

    act(() => {
      vi.advanceTimersByTime(5000)
    })
    // shouldn't advance when paused
    expect(result.current.elapsedSeconds).toBe(5)
  })

  it('should finish countdown and call onFinish', () => {
    const onFinish = vi.fn()
    const { result } = renderHook(() => useTimer({ taskId: 1, estimatedMinutes: 1, dateStr: '2026-06-18', onFinish }))
    
    act(() => {
      result.current.start()
    })

    act(() => {
      vi.advanceTimersByTime(60000)
    })

    expect(result.current.isRunning).toBe(false)
    expect(result.current.isFinished).toBe(true)
    expect(result.current.remainingSeconds).toBe(0)
    expect(onFinish).toHaveBeenCalledTimes(1)
  })

  it('should behave as stopwatch when estimatedMinutes is 0', () => {
    const { result } = renderHook(() => useTimer({ taskId: 2, estimatedMinutes: 0, dateStr: '2026-06-18' }))
    
    expect(result.current.isCountdown).toBe(false)
    
    act(() => {
      result.current.start()
    })

    act(() => {
      vi.advanceTimersByTime(120000) // 2 minutes
    })

    expect(result.current.elapsedSeconds).toBe(120)
    expect(result.current.isFinished).toBe(false) // stopwatch never finishes automatically
  })

  it('should reset correctly', () => {
    const { result } = renderHook(() => useTimer({ taskId: 1, estimatedMinutes: 30, dateStr: '2026-06-18' }))
    
    act(() => {
      result.current.start()
    })

    act(() => {
      vi.advanceTimersByTime(5000)
    })

    act(() => {
      result.current.reset()
    })

    expect(result.current.isRunning).toBe(false)
    expect(result.current.elapsedSeconds).toBe(0)
    expect(result.current.remainingSeconds).toBe(1800)
    expect(localStorage.getItem('timer_1_2026-06-18')).toBeNull()
  })

  it('should persist and load from localStorage', () => {
    // 1st render
    const { result, unmount } = renderHook(() => useTimer({ taskId: 3, estimatedMinutes: 10, dateStr: '2026-06-18' }))
    
    act(() => {
      result.current.start()
    })

    act(() => {
      vi.advanceTimersByTime(10000) // 10s
    })

    expect(result.current.elapsedSeconds).toBe(10)
    
    // Simulate user pausing before close
    act(() => {
      result.current.pause()
    })

    unmount()

    // 2nd render (simulate reload)
    const { result: result2 } = renderHook(() => useTimer({ taskId: 3, estimatedMinutes: 10, dateStr: '2026-06-18' }))
    
    expect(result2.current.elapsedSeconds).toBe(10)
    expect(result2.current.isRunning).toBe(false)
  })
})
