import { Outlet, useLocation } from 'react-router-dom'
import { SidebarNav, BottomNav } from './NavBar'
import { Toaster } from '@/components/ui/sonner'
import { InstallPrompt } from './InstallPrompt'
import { usePreferences } from '@/hooks/usePreferences'

export function AppLayout() {
  const { pathname } = useLocation()
  usePreferences() // Fetch and apply preferences on app mount

  return (
    <div className="flex h-full bg-[#08080A]">
      {/* Sidebar — visível apenas no desktop */}
      <SidebarNav />

      {/* Conteúdo principal */}
      <main className="flex-1 overflow-y-auto pb-16 md:pb-0">
        {/* key força re-mount do children a cada mudança de rota → aciona a animação */}
        <div key={pathname} className="page-fade max-w-3xl lg:max-w-5xl xl:max-w-6xl mx-auto px-4 py-6 md:px-8 md:py-8">
          <Outlet />
        </div>
      </main>

      {/* Bottom nav — visível apenas no mobile */}
      <BottomNav />

      {/* PWA install prompt — mobile only */}
      <InstallPrompt />

      {/* Toasts globais */}
      <Toaster
        theme="dark"
        position="top-right"
        toastOptions={{
          style: {
            background: '#141416',
            border: '1px solid #26262A',
            color: '#F4F2EF',
          },
        }}
      />
    </div>
  )
}
