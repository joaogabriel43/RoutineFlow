import { NavLink, useNavigate } from 'react-router-dom'
import { BarChart2, Calendar, CalendarDays, CheckSquare, Home, LogOut, Settings2, Upload } from 'lucide-react'
import { cn } from '@/lib/utils'
import { authApi } from '@/services/api'
import { SpotlightSearch } from '@/components/shared/SpotlightSearch'
import { useInstallPrompt } from './InstallPrompt'
import { Download } from 'lucide-react'

interface NavItem {
  to: string
  icon: React.ElementType
  label: string
}

// Desktop sidebar — all items including Importar + Settings
const SIDEBAR_ITEMS: NavItem[] = [
  { to: '/',           icon: Home,         label: 'Hoje'       },
  { to: '/tasks',      icon: CheckSquare,  label: 'Tarefas'    },
  { to: '/semana',     icon: Calendar,     label: 'Semana'     },
  { to: '/calendario', icon: CalendarDays, label: 'Calendário' },
  { to: '/analytics',  icon: BarChart2,    label: 'Analytics'  },
  { to: '/manage',     icon: Settings2,    label: 'Gerenciar'  },
  { to: '/import',     icon: Upload,       label: 'Importar'   },
]

// Mobile bottom nav — Importar excluded, Settings added
const BOTTOM_NAV_ITEMS: NavItem[] = [
  { to: '/',           icon: Home,         label: 'Hoje'       },
  { to: '/tasks',      icon: CheckSquare,  label: 'Tarefas'    },
  { to: '/semana',     icon: Calendar,     label: 'Semana'     },
  { to: '/calendario', icon: CalendarDays, label: 'Calendário' },
  { to: '/analytics',  icon: BarChart2,    label: 'Analytics'  },
  { to: '/manage',     icon: Settings2,    label: 'Gerenciar'  },
]

// ── Desktop Sidebar ───────────────────────────────────────────────────────────

function getUserInfo(): { name: string; email: string } | null {
  const raw = localStorage.getItem('rf_user')
  if (!raw) return null
  try {
    return JSON.parse(raw) as { name: string; email: string }
  } catch {
    return null
  }
}

export function SidebarNav() {
  const navigate = useNavigate()
  const user = getUserInfo()
  const { deferredPrompt, handleInstall } = useInstallPrompt()

  return (
    <aside className="hidden md:flex flex-col w-[220px] shrink-0 border-r border-[#26262A] h-full px-3 py-6">
      {/* Logo */}
      <div className="px-3 mb-6">
        <span className="text-[15px] font-semibold tracking-tight text-[#F4F2EF]">
          RoutineFlow
        </span>
      </div>

      <div className="px-3">
        <SpotlightSearch />
      </div>

      {/* Nav items */}
      <nav className="flex flex-col gap-1 flex-1">
        {SIDEBAR_ITEMS.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors',
                isActive
                  ? 'bg-[#26262A] text-[#F4F2EF] font-medium'
                  : 'text-[#8C8A88] hover:text-[#F4F2EF] hover:bg-[#141416]',
              )
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* User footer */}
      <div className="border-t border-[#26262A] pt-3 mt-3 space-y-1">
        {/* Settings link */}
        <NavLink
          to="/settings"
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors',
              isActive
                ? 'bg-[#26262A] text-[#F4F2EF] font-medium'
                : 'text-[#8C8A88] hover:text-[#F4F2EF] hover:bg-[#141416]',
            )
          }
        >
          <Settings2 size={16} />
          Configurações
        </NavLink>

        {deferredPrompt && (
          <button
            onClick={handleInstall}
            className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-[#2F8BFF] hover:bg-[#141416] transition-colors text-left"
          >
            <Download size={16} />
            Instalar App
          </button>
        )}

        {/* User info + logout */}
        {user && (
          <div className="flex items-center gap-2 px-3 py-2">
            <div className="w-7 h-7 rounded-full bg-brand/20 text-brand flex items-center justify-center text-xs font-semibold shrink-0">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-xs text-[#F4F2EF] font-medium truncate">{user.name}</p>
              <p className="text-[10px] text-[#8C8A88] truncate">{user.email}</p>
            </div>
            <button
              type="button"
              onClick={() => {
                authApi.logout()
                navigate('/login')
              }}
              title="Sair"
              className="shrink-0 p-1.5 rounded-md text-[#8C8A88] hover:text-danger hover:bg-danger/10 transition-colors"
            >
              <LogOut size={14} />
            </button>
          </div>
        )}
      </div>
    </aside>
  )
}

// ── Mobile Bottom Nav ─────────────────────────────────────────────────────────

export function BottomNav() {
  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 flex border-t border-[#26262A] bg-[#08080A]/95 backdrop-blur-md h-16">
      {BOTTOM_NAV_ITEMS.map(({ to, icon: Icon, label }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          className={({ isActive }) =>
            cn(
              'flex-1 flex flex-col items-center justify-center gap-1 text-[10px] transition-colors',
              isActive
                ? 'text-[#2F8BFF]'
                : 'text-[#8C8A88] hover:text-[#F4F2EF]',
            )
          }
        >
          <Icon size={20} />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}
