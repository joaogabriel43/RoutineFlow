import { Outlet, useLocation } from 'react-router-dom'
import { SidebarNav, BottomNav } from './NavBar'
import { Toaster } from '@/components/ui/sonner'

export function AppLayout() {
  const { pathname } = useLocation()

  return (
    <div className="flex h-full bg-[#0a0a0a]">
      {/* Sidebar — visível apenas no desktop */}
      <SidebarNav />

      {/* Conteúdo principal */}
      <main className="flex-1 overflow-y-auto pb-16 md:pb-0">
        {/* key força re-mount do children a cada mudança de rota → aciona a animação */}
        <div key={pathname} className="page-fade max-w-3xl mx-auto px-4 py-6 md:px-8 md:py-8">
          <Outlet />
        </div>
      </main>

      {/* Bottom nav — visível apenas no mobile */}
      <BottomNav />

      {/* Toasts globais */}
      <Toaster
        theme="dark"
        position="top-right"
        toastOptions={{
          style: {
            background: '#141414',
            border: '1px solid #1f1f1f',
            color: '#f5f5f7',
          },
        }}
      />
    </div>
  )
}
