import { Play, Pause, Check } from 'lucide-react'
import { useTimer } from '@/hooks/useTimer'

interface TaskTimerProps {
  taskId: number
  taskTitle: string
  estimatedMinutes: number
  onComplete: (note?: string) => void
}

function todayStr(): string {
  return new Date().toISOString().split('T')[0] as string
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

export function TaskTimer({ taskId, taskTitle, estimatedMinutes, onComplete }: TaskTimerProps) {
  const {
    isRunning,
    isFinished,
    remainingSeconds,
    elapsedSeconds,
    initialSeconds,
    isCountdown,
    start,
    pause,
  } = useTimer({
    taskId,
    estimatedMinutes,
    dateStr: todayStr(),
    onFinish: () => {
      // Play beep
      try {
        const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
        const oscillator = audioCtx.createOscillator()
        const gainNode = audioCtx.createGain()
        
        oscillator.type = 'sine'
        oscillator.frequency.setValueAtTime(880, audioCtx.currentTime) // A5
        
        gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime)
        gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.5)
        
        oscillator.connect(gainNode)
        gainNode.connect(audioCtx.destination)
        
        oscillator.start()
        oscillator.stop(audioCtx.currentTime + 0.5)
      } catch (e) {
        console.error('AudioContext error', e)
      }
      
      // Vibrate
      if (typeof navigator !== 'undefined' && navigator.vibrate) {
        navigator.vibrate([200, 100, 200])
      }
    }
  })

  // Calculations for SVG Ring
  const radius = 54
  const circumference = 2 * Math.PI * radius
  
  // Progress goes from 0 (empty) to 1 (full)
  const progressPercent = isCountdown && initialSeconds > 0 
    ? Math.min(1, elapsedSeconds / initialSeconds)
    : 0

  const strokeDashoffset = circumference - (progressPercent * circumference)
  const isDanger = isCountdown && remainingSeconds < initialSeconds * 0.2

  return (
    <div className="flex flex-col items-center justify-center p-6 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] mt-2 mb-4 shadow-sm animate-in fade-in slide-in-from-top-2 duration-200">
      <h3 className="text-[#f5f5f7] font-medium mb-4 text-sm tracking-wide text-center">
        {taskTitle}
      </h3>
      
      {/* Timer Circle */}
      <div className="relative w-[120px] h-[120px] mb-5">
        {isCountdown && (
          <svg className="w-full h-full transform -rotate-90">
            {/* Background track */}
            <circle
              cx="60"
              cy="60"
              r={radius}
              stroke="currentColor"
              strokeWidth="4"
              fill="transparent"
              className="text-[#2a2a2a]"
            />
            {/* Progress ring */}
            <circle
              cx="60"
              cy="60"
              r={radius}
              stroke="currentColor"
              strokeWidth="4"
              fill="transparent"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              className={`transition-all duration-1000 ease-linear ${isDanger ? 'text-[#ff9f0a]' : 'text-[#0071e3]'}`}
            />
          </svg>
        )}
        
        {/* Time display */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-light text-[#f5f5f7] tabular-nums tracking-wider">
            {isCountdown ? formatTime(remainingSeconds) : `⏱\u00A0${formatTime(elapsedSeconds)}`}
          </span>
        </div>
      </div>

      {/* Controls */}
      <div className="flex items-center gap-3">
        {isFinished ? (
          <button
            type="button"
            onClick={() => onComplete(`Concluído em ${Math.ceil(elapsedSeconds / 60)}min (estimado: ${estimatedMinutes}min)`)}
            className="flex items-center gap-2 bg-[#30d158] hover:bg-[#32d74b] text-white px-4 py-2 rounded-full text-sm font-medium transition-colors"
          >
            <Check size={16} />
            Marcar como feita
          </button>
        ) : (
          <>
            {!isRunning ? (
              <button
                type="button"
                onClick={start}
                className="flex items-center gap-2 bg-[#0071e3] hover:bg-[#0077ed] text-white px-5 py-2 rounded-full text-sm font-medium transition-colors"
              >
                <Play size={16} fill="currentColor" />
                {elapsedSeconds > 0 ? 'Retomar' : 'Iniciar'}
              </button>
            ) : (
              <button
                type="button"
                onClick={pause}
                className="flex items-center gap-2 bg-[#3a3a3c] hover:bg-[#4a4a4c] text-white px-5 py-2 rounded-full text-sm font-medium transition-colors"
              >
                <Pause size={16} fill="currentColor" />
                Pausar
              </button>
            )}
            
            <button
              type="button"
              onClick={() => onComplete(elapsedSeconds > 0 ? `Concluído em ${Math.ceil(elapsedSeconds / 60)}min (estimado: ${estimatedMinutes > 0 ? estimatedMinutes + 'min' : 'N/A'})` : undefined)}
              className="flex items-center justify-center w-9 h-9 rounded-full border border-[#3a3a3c] text-[#86868b] hover:text-[#30d158] hover:border-[#30d158] hover:bg-[#30d158]/10 transition-colors"
              title="Concluir tarefa"
            >
              <Check size={16} />
            </button>
          </>
        )}
      </div>
    </div>
  )
}
