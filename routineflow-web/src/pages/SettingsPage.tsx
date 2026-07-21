import { useState, useEffect } from 'react'
import { Loader2, LogOut, User, Lock, Info, Settings2, Volume2, Calendar as CalendarIcon, Moon, Download, Database } from 'lucide-react'
import { toast } from 'sonner'
import { authApi } from '@/services/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { usePreferences } from '@/hooks/usePreferences'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

// ── Section wrapper ───────────────────────────────────────────────────────────

function Section({
  icon: Icon,
  title,
  children,
}: {
  icon: React.ElementType
  title: string
  children: React.ReactNode
}) {
  return (
    <section className="rounded-xl bg-[#141416] border border-line overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-3 border-b border-line">
        <Icon size={14} className="text-fg-dim" />
        <h2 className="text-xs font-semibold text-fg-dim uppercase tracking-widest">{title}</h2>
      </div>
      <div className="px-5 py-4 space-y-4">{children}</div>
    </section>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function SettingsPage() {
  // ── Profile state ─────────────────────────────────────────────────────────
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [nameLoading, setNameLoading] = useState(false)
  const [profileLoading, setProfileLoading] = useState(true)

  // ── Preferences state ─────────────────────────────────────────────────────
  const { preferences, isLoading: prefsLoading, updatePreferences, isUpdating } = usePreferences()

  // ── Password state ────────────────────────────────────────────────────────
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [pwdLoading, setPwdLoading] = useState(false)

  // ── Logout dialog ─────────────────────────────────────────────────────────
  const [logoutOpen, setLogoutOpen] = useState(false)

  // ── Load profile on mount ─────────────────────────────────────────────────
  useEffect(() => {
    authApi
      .getProfile()
      .then((p) => {
        setName(p.name)
        setEmail(p.email)
      })
      .catch(() => {
        // Fallback to localStorage
        const cached = localStorage.getItem('rf_user')
        if (cached) {
          const u = JSON.parse(cached) as { name: string; email: string }
          setName(u.name)
          setEmail(u.email)
        }
      })
      .finally(() => setProfileLoading(false))
  }, [])

  // ── Handlers ──────────────────────────────────────────────────────────────

  async function handleUpdateName() {
    if (!name.trim()) return toast.error('Nome não pode ficar vazio')
    setNameLoading(true)
    try {
      const updated = await authApi.updateProfile({ name: name.trim() })
      setName(updated.name)
      localStorage.setItem('rf_user', JSON.stringify({ name: updated.name, email: updated.email }))
      toast.success('Nome atualizado!')
    } catch {
      toast.error('Erro ao atualizar nome')
    } finally {
      setNameLoading(false)
    }
  }

  async function handleChangePassword() {
    if (newPassword.length < 6) return toast.error('Nova senha: mínimo 6 caracteres')
    if (newPassword !== confirmPassword) return toast.error('As senhas não coincidem')
    setPwdLoading(true)
    try {
      await authApi.changePassword({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      toast.success('Senha alterada com sucesso!')
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } }).response?.status
      if (status === 400) toast.error('Senha atual incorreta')
      else toast.error('Erro ao alterar senha')
    } finally {
      setPwdLoading(false)
    }
  }

  function handleLogout() {
    authApi.logout()
  }

  const [backupLoading, setBackupLoading] = useState(false)
  async function handleExportBackup() {
    if (backupLoading) return
    setBackupLoading(true)
    try {
      // Assuming exportApi is imported
      const { exportApi } = await import('@/services/api')
      await exportApi.exportBackup()
      toast.success('Backup exportado com sucesso!')
    } catch {
      toast.error('Erro ao exportar backup.')
    } finally {
      setBackupLoading(false)
    }
  }

  const [revokeLoading, setRevokeLoading] = useState(false)
  async function handleRevokeSessions() {
    if (revokeLoading) return
    setRevokeLoading(true)
    try {
      await authApi.revokeSessions()
      toast.success('Todas as outras sessões foram encerradas.')
    } catch {
      toast.error('Erro ao encerrar sessões.')
    } finally {
      setRevokeLoading(false)
    }
  }

  // ── Render ────────────────────────────────────────────────────────────────

  if (profileLoading || prefsLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="animate-spin text-fg-dim" size={24} />
      </div>
    )
  }

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <header>
        <h1 className="text-3xl font-light text-[#F4F2EF] tracking-tight">Configurações</h1>
        <p className="text-sm text-[#8C8A88] mt-1">Gerencie seu perfil e conta</p>
      </header>

      {/* ── Profile ────────────────────────────────────────────────────── */}
      <Section icon={User} title="Perfil">
        <div>
          <label className="block text-xs font-medium text-fg-dim mb-1.5">Nome</label>
          <div className="flex gap-2">
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="bg-surface-1 border-line text-fg"
              maxLength={255}
            />
            <Button
              onClick={handleUpdateName}
              disabled={nameLoading}
              className="bg-brand hover:bg-brand/80 text-white shrink-0"
            >
              {nameLoading ? <Loader2 size={14} className="animate-spin" /> : 'Salvar'}
            </Button>
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-fg-dim mb-1.5">E-mail</label>
          <Input
            value={email}
            disabled
            className="bg-surface-1 border-line text-fg-lo cursor-not-allowed"
          />
          <p className="text-[10px] text-fg-dim mt-1">O e-mail não pode ser alterado</p>
        </div>
      </Section>

      {/* ── Password ───────────────────────────────────────────────────── */}
      <Section icon={Lock} title="Alterar senha">
        <div>
          <label className="block text-xs font-medium text-fg-dim mb-1.5">Senha atual</label>
          <Input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder="••••••••"
            className="bg-surface-1 border-line text-fg placeholder:text-[#34343A]"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-fg-dim mb-1.5">Nova senha</label>
          <Input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="Mínimo 6 caracteres"
            className="bg-surface-1 border-line text-fg placeholder:text-[#34343A]"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-fg-dim mb-1.5">Confirmar nova senha</label>
          <Input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Repita a nova senha"
            className="bg-surface-1 border-line text-fg placeholder:text-[#34343A]"
          />
        </div>
        <Button
          onClick={handleChangePassword}
          disabled={pwdLoading || !currentPassword || !newPassword || !confirmPassword}
          className="w-full bg-surface-3 hover:bg-surface-3/80 text-fg"
        >
          {pwdLoading ? <Loader2 size={14} className="animate-spin mr-2" /> : null}
          Alterar senha
        </Button>
      </Section>

      {/* ── Preferences ─────────────────────────────────────────────────── */}
      <Section icon={Settings2} title="Preferências">
        {/* Theme */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-surface-1 flex items-center justify-center shrink-0">
              <Moon size={14} className="text-fg-dim" />
            </div>
            <div>
              <p className="text-sm font-medium text-fg">Tema</p>
              <p className="text-xs text-fg-dim">Interface clara ou escura</p>
            </div>
          </div>
          <select
            value={preferences?.theme ?? 'SYSTEM'}
            onChange={(e) => updatePreferences({ ...preferences!, theme: e.target.value })}
            disabled={isUpdating}
            className="bg-surface-1 border border-line text-sm text-fg rounded-md px-2 py-1.5 outline-none focus:border-brand"
          >
            <option value="SYSTEM">Automático</option>
            <option value="DARK">Escuro</option>
            <option value="LIGHT">Claro</option>
          </select>
        </div>

        {/* Sound */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-surface-1 flex items-center justify-center shrink-0">
              <Volume2 size={14} className="text-fg-dim" />
            </div>
            <div>
              <p className="text-sm font-medium text-fg">Sons e Efeitos</p>
              <p className="text-xs text-fg-dim">Áudio ao completar tarefas</p>
            </div>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={preferences?.soundEnabled ?? false}
            disabled={isUpdating}
            onClick={() => updatePreferences({ ...preferences!, soundEnabled: !preferences?.soundEnabled })}
            className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-[#08080A] disabled:cursor-not-allowed disabled:opacity-50 ${preferences?.soundEnabled ? 'bg-brand' : 'bg-surface-2'}`}
          >
            <span className={`pointer-events-none block h-4 w-4 rounded-full bg-white shadow-lg ring-0 transition-transform ${preferences?.soundEnabled ? 'translate-x-4' : 'translate-x-0'}`} />
          </button>
        </div>

        {/* First day of week */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-surface-1 flex items-center justify-center shrink-0">
              <CalendarIcon size={14} className="text-fg-dim" />
            </div>
            <div>
              <p className="text-sm font-medium text-fg">Início da Semana</p>
              <p className="text-xs text-fg-dim">Dia em que o calendário semanal começa</p>
            </div>
          </div>
          <select
            value={preferences?.firstDayOfWeek ?? 'MONDAY'}
            onChange={(e) => updatePreferences({ ...preferences!, firstDayOfWeek: e.target.value })}
            disabled={isUpdating}
            className="bg-surface-1 border border-line text-sm text-fg rounded-md px-2 py-1.5 outline-none focus:border-brand"
          >
            <option value="MONDAY">Segunda-feira</option>
            <option value="SUNDAY">Domingo</option>
          </select>
        </div>
      </Section>

      {/* ── Data ──────────────────────────────────────────────────────── */}
      <Section icon={Database} title="Dados & Backup">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-fg">Backup Local Completo</p>
            <p className="text-xs text-fg-dim">Baixe todo seu histórico, tarefas e rotinas em JSON.</p>
          </div>
          <Button
            onClick={handleExportBackup}
            disabled={backupLoading}
            variant="outline"
            className="shrink-0 bg-surface-1 border-line text-fg hover:bg-surface-2"
          >
            {backupLoading ? <Loader2 size={14} className="animate-spin mr-2" /> : <Download size={14} className="mr-2" />}
            Exportar JSON
          </Button>
        </div>
      </Section>

      {/* ── Session ────────────────────────────────────────────────────── */}
      <Section icon={LogOut} title="Sessão">
        <div className="space-y-3">
          <Button
            onClick={handleRevokeSessions}
            disabled={revokeLoading}
            variant="outline"
            className="w-full bg-surface-1 border-line text-fg hover:bg-surface-2"
          >
            {revokeLoading ? <Loader2 size={14} className="animate-spin mr-2" /> : <Lock size={14} className="mr-2" />}
            Encerrar Outras Sessões Abertas
          </Button>

          <Button
            variant="destructive"
            onClick={() => setLogoutOpen(true)}
            className="w-full bg-danger/10 hover:bg-danger/20 text-danger border border-danger/20"
          >
            <LogOut size={14} className="mr-2" />
            Sair da conta
          </Button>
        </div>
      </Section>

      {/* ── About ──────────────────────────────────────────────────────── */}
      <Section icon={Info} title="Sobre">
        <div className="flex items-center justify-between text-sm">
          <span className="text-fg-dim">Versão</span>
          <span className="num text-fg-lo">3.2.0</span>
        </div>
        <div className="flex items-center justify-between text-sm">
          <span className="text-fg-dim">GitHub</span>
          <a
            href="https://github.com/joaogabriel43/RoutineFlow"
            target="_blank"
            rel="noopener noreferrer"
            className="text-brand hover:underline text-xs"
          >
            joaogabriel43/RoutineFlow
          </a>
        </div>
      </Section>

      {/* ── Logout confirmation ────────────────────────────────────────── */}
      <AlertDialog open={logoutOpen} onOpenChange={setLogoutOpen}>
        <AlertDialogContent className="bg-[#141416] border-[#26262A] text-[#F4F2EF]">
          <AlertDialogHeader>
            <AlertDialogTitle>Sair da conta?</AlertDialogTitle>
            <AlertDialogDescription className="text-[#8C8A88]">
              Você será redirecionado para a tela de login.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="bg-[#26262A] text-[#F4F2EF] border-[#26262A] hover:bg-[#26262A]/80">
              Cancelar
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleLogout}
              className="bg-danger hover:bg-danger/80 text-white"
            >
              Sair
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
