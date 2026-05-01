import { NavLink } from 'react-router-dom'
import { BarChart2, Calendar, CheckSquare, Home, Settings2, Upload } from 'lucide-react'
import { cn } from '@/lib/utils'

interface NavItem {
  to: string
  icon: React.ElementType
  label: string
}

// Desktop sidebar — all items including Importar
const SIDEBAR_ITEMS: NavItem[] = [
  { to: '/',          icon: Home,        label: 'Hoje'      },
  { to: '/tasks',     icon: CheckSquare, label: 'Tarefas'   },
  { to: '/semana',    icon: Calendar,    label: 'Semana'    },
  { to: '/analytics', icon: BarChart2,   label: 'Analytics' },
  { to: '/manage',    icon: Settings2,   label: 'Gerenciar' },
  { to: '/import',    icon: Upload,      label: 'Importar'  },
]

// Mobile bottom nav — 5 items, Importar excluded
const BOTTOM_NAV_ITEMS: NavItem[] = [
  { to: '/',          icon: Home,        label: 'Hoje'      },
  { to: '/tasks',     icon: CheckSquare, label: 'Tarefas'   },
  { to: '/semana',    icon: Calendar,    label: 'Semana'    },
  { to: '/analytics', icon: BarChart2,   label: 'Analytics' },
  { to: '/manage',    icon: Settings2,   label: 'Gerenciar' },
]

// ── Desktop Sidebar ───────────────────────────────────────────────────────────

export function SidebarNav() {
  return (
    <aside className="hidden md:flex flex-col w-[220px] shrink-0 border-r border-[#1f1f1f] h-full px-3 py-6 gap-1">
      {/* Logo */}
      <div className="px-3 mb-6">
        <span className="text-[15px] font-semibold tracking-tight text-[#f5f5f7]">
          RoutineFlow
        </span>
      </div>

      {SIDEBAR_ITEMS.map(({ to, icon: Icon, label }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors',
              isActive
                ? 'bg-[#1f1f1f] text-[#f5f5f7] font-medium'
                : 'text-[#86868b] hover:text-[#f5f5f7] hover:bg-[#141414]',
            )
          }
        >
          <Icon size={16} />
          {label}
        </NavLink>
      ))}
    </aside>
  )
}

// ── Mobile Bottom Nav ─────────────────────────────────────────────────────────

export function BottomNav() {
  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 flex border-t border-[#1f1f1f] bg-[#0a0a0a]/95 backdrop-blur-md h-16">
      {BOTTOM_NAV_ITEMS.map(({ to, icon: Icon, label }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          className={({ isActive }) =>
            cn(
              'flex-1 flex flex-col items-center justify-center gap-1 text-[10px] transition-colors',
              isActive
                ? 'text-[#0071e3]'
                : 'text-[#86868b] hover:text-[#f5f5f7]',
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
