import { useCallback } from 'react'
import confetti from 'canvas-confetti'
import { usePreferences } from '@/hooks/usePreferences'

export function useConfetti() {
  const { preferences } = usePreferences()
  const soundEnabled = preferences?.soundEnabled ?? false

  const triggerConfetti = useCallback(() => {
    // Play a nice success "ding" chord if enabled
    if (soundEnabled && typeof window !== 'undefined') {
      try {
        const AudioContext = window.AudioContext || (window as any).webkitAudioContext
        if (AudioContext) {
          const ctx = new AudioContext()
          
          const osc1 = ctx.createOscillator()
          const osc2 = ctx.createOscillator()
          const gainNode = ctx.createGain()
          
          osc1.type = 'sine'
          osc2.type = 'sine'
          
          // C6 / E6 chord
          osc1.frequency.setValueAtTime(1046.50, ctx.currentTime) 
          osc2.frequency.setValueAtTime(1318.51, ctx.currentTime) 
          
          gainNode.gain.setValueAtTime(0, ctx.currentTime)
          gainNode.gain.linearRampToValueAtTime(0.15, ctx.currentTime + 0.02)
          gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5)
          
          osc1.connect(gainNode)
          osc2.connect(gainNode)
          gainNode.connect(ctx.destination)
          
          osc1.start()
          osc2.start()
          osc1.stop(ctx.currentTime + 0.5)
          osc2.stop(ctx.currentTime + 0.5)
        }
      } catch (e) {
        // Silently ignore audio errors
      }
    }

    const duration = 2000
    const end = Date.now() + duration

    const frame = () => {
      confetti({
        particleCount: 3,
        angle: 60,
        spread: 55,
        origin: { x: 0 },
        colors: ['#2F8BFF', '#10B981', '#F59E0B']
      })
      confetti({
        particleCount: 3,
        angle: 120,
        spread: 55,
        origin: { x: 1 },
        colors: ['#2F8BFF', '#10B981', '#F59E0B']
      })

      if (Date.now() < end) {
        requestAnimationFrame(frame)
      }
    }

    frame()
  }, [soundEnabled])

  return { triggerConfetti }
}
