import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { toast } from 'sonner'

const DISMISSED_KEY = 'rf_install_dismissed'
const MOBILE_BREAKPOINT = 768

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  readonly userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

function useInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null)

  useEffect(() => {
    const handler = (e: Event) => {
      e.preventDefault()
      setDeferredPrompt(e as BeforeInstallPromptEvent)
    }

    window.addEventListener('beforeinstallprompt', handler)
    return () => window.removeEventListener('beforeinstallprompt', handler)
  }, [])

  async function handleInstall() {
    if (!deferredPrompt) return

    await deferredPrompt.prompt()
    const { outcome } = await deferredPrompt.userChoice

    if (outcome === 'accepted') {
      toast.success('App instalado com sucesso!')
    }

    setDeferredPrompt(null)
  }

  return { deferredPrompt, handleInstall }
}

function isMobile(): boolean {
  return window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT}px)`).matches
}

export function InstallPrompt() {
  const { deferredPrompt, handleInstall } = useInstallPrompt()
  const [dismissed, setDismissed] = useState<boolean>(() =>
    localStorage.getItem(DISMISSED_KEY) === 'true',
  )

  function handleDismiss() {
    localStorage.setItem(DISMISSED_KEY, 'true')
    setDismissed(true)
  }

  // Only render on mobile and when the browser supports installation
  if (dismissed || !deferredPrompt || !isMobile()) return null

  return (
    <div
      className="fixed left-0 right-0 z-50 px-4 py-3"
      style={{ bottom: '64px' }} // sits above the BottomNav (64px tall)
    >
      <div className="flex items-center gap-3 rounded-xl bg-[#141414] border border-[#1f1f1f] px-4 py-3 shadow-lg">
        {/* App icon placeholder */}
        <div className="w-10 h-10 rounded-xl bg-[#0071e3]/15 border border-[#0071e3]/30 flex items-center justify-center shrink-0">
          <span className="text-lg">✅</span>
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-[#f5f5f7] leading-tight">
            Instale o RoutineFlow
          </p>
          <p className="text-[11px] text-[#86868b] mt-0.5">
            Adicione à tela inicial para acesso rápido
          </p>
        </div>

        <button
          onClick={handleInstall}
          className="shrink-0 px-3 py-1.5 rounded-lg bg-[#0071e3] text-white text-xs font-medium
                     hover:bg-[#0077ed] transition-colors"
        >
          Instalar
        </button>

        <button
          onClick={handleDismiss}
          aria-label="Fechar"
          className="shrink-0 p-1 rounded-full text-[#86868b] hover:text-[#f5f5f7]
                     hover:bg-[#1f1f1f] transition-colors"
        >
          <X size={14} />
        </button>
      </div>
    </div>
  )
}
