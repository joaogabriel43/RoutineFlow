import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Home, Calendar, CalendarDays, CheckSquare, BarChart2, Settings2, Upload } from 'lucide-react'
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import { useKeyboardShortcut } from '@/hooks/useKeyboardShortcut'

const PAGES = [
  { to: '/', label: 'Hoje', icon: Home },
  { to: '/tasks', label: 'Tarefas', icon: CheckSquare },
  { to: '/semana', label: 'Semana', icon: Calendar },
  { to: '/calendario', label: 'Calendário', icon: CalendarDays },
  { to: '/analytics', label: 'Analytics', icon: BarChart2 },
  { to: '/manage', label: 'Gerenciar', icon: Settings2 },
  { to: '/import', label: 'Importar', icon: Upload },
  { to: '/settings', label: 'Configurações', icon: Settings2 },
]

export function SpotlightSearch() {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  // Toggle with Ctrl+K or Cmd+K
  useKeyboardShortcut(['k', 'ctrl'], () => setOpen((open) => !open))

  // Close when navigating
  const runCommand = (command: () => void) => {
    setOpen(false)
    command()
  }

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="hidden md:flex items-center gap-2 px-3 py-1.5 text-sm text-[#8C8A88] bg-[#141416] border border-[#26262A] rounded-md hover:bg-[#26262A] transition-colors w-full mb-4"
      >
        <Search size={14} />
        <span>Busca rápida...</span>
        <kbd className="ml-auto pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border border-[#26262A] bg-[#08080A] px-1.5 font-mono text-[10px] font-medium text-[#8C8A88] opacity-100">
          <span className="text-xs">⌘</span>K
        </kbd>
      </button>

      <CommandDialog open={open} onOpenChange={setOpen}>
        <CommandInput placeholder="Digite para buscar páginas..." />
        <CommandList>
          <CommandEmpty>Nenhum resultado encontrado.</CommandEmpty>
          <CommandGroup heading="Navegação">
            {PAGES.map((page) => (
              <CommandItem
                key={page.to}
                value={page.label}
                onSelect={() => runCommand(() => navigate(page.to))}
                className="flex items-center gap-2 cursor-pointer"
              >
                <page.icon size={14} className="text-fg-dim" />
                <span>{page.label}</span>
              </CommandItem>
            ))}
          </CommandGroup>
        </CommandList>
      </CommandDialog>
    </>
  )
}
