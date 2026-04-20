import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { authApi } from '@/services/api'
import { Button } from '@/components/ui/button'

type Mode = 'login' | 'register'

export function LoginPage() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>('login')
  const [loading, setLoading] = useState(false)

  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [errors, setErrors] = useState<Partial<typeof form>>({})

  const set = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }))
    setErrors((prev) => ({ ...prev, [field]: '' }))
  }

  function validate(): boolean {
    const next: Partial<typeof form> = {}
    if (mode === 'register' && !form.name.trim()) next.name = 'Nome obrigatório'
    if (!form.email.includes('@')) next.email = 'E-mail inválido'
    if (form.password.length < 6) next.password = 'Mínimo 6 caracteres'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      const data =
        mode === 'login'
          ? await authApi.login({ email: form.email, password: form.password })
          : await authApi.register({ name: form.name, email: form.email, password: form.password })

      localStorage.setItem('rf_token', data.token)
      localStorage.setItem('rf_user', JSON.stringify({ name: data.name, email: data.email }))

      // First-ever login → send to ImportPage with welcome flag
      const isFirstLogin = !localStorage.getItem('rf_has_logged_in')
      if (isFirstLogin) {
        localStorage.setItem('rf_has_logged_in', 'true')
        navigate('/import', { state: { welcome: true } })
      } else {
        navigate('/')
      }
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } }).response?.status
      if (status === 401) toast.error('E-mail ou senha incorretos')
      else if (status === 409) toast.error('E-mail já cadastrado')
      else toast.error('Algo deu errado. Tente novamente.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#0a0a0a] px-4">
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-10">
          <h1 className="text-2xl font-semibold text-[#f5f5f7] tracking-tight">
            RoutineFlow
          </h1>
          <p className="text-sm text-[#86868b] mt-1">
            {mode === 'login' ? 'Bem-vindo de volta' : 'Crie sua conta'}
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {mode === 'register' && (
            <Field
              label="Nome"
              type="text"
              value={form.name}
              onChange={set('name')}
              placeholder="Seu nome"
              error={errors.name}
              autoFocus
            />
          )}

          <Field
            label="E-mail"
            type="email"
            value={form.email}
            onChange={set('email')}
            placeholder="voce@exemplo.com"
            error={errors.email}
            autoFocus={mode === 'login'}
          />

          <Field
            label="Senha"
            type="password"
            value={form.password}
            onChange={set('password')}
            placeholder="••••••••"
            error={errors.password}
          />

          <Button
            type="submit"
            disabled={loading}
            className="w-full mt-2 bg-[#0071e3] hover:bg-[#0077ed] text-white font-medium h-11 rounded-xl transition-colors"
          >
            {loading
              ? mode === 'login' ? 'Entrando…' : 'Criando conta…'
              : mode === 'login' ? 'Entrar' : 'Criar conta'}
          </Button>
        </form>

        {/* Toggle */}
        <p className="text-center text-sm text-[#86868b] mt-6">
          {mode === 'login' ? 'Não tem conta?' : 'Já tem conta?'}{' '}
          <button
            type="button"
            onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setErrors({}) }}
            className="text-[#0071e3] hover:underline font-medium"
          >
            {mode === 'login' ? 'Criar conta' : 'Fazer login'}
          </button>
        </p>
      </div>
    </div>
  )
}

// ── Field component ───────────────────────────────────────────────────────────

interface FieldProps {
  label: string
  type: string
  value: string
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  placeholder?: string
  error?: string
  autoFocus?: boolean
}

function Field({ label, type, value, onChange, placeholder, error, autoFocus }: FieldProps) {
  return (
    <div>
      <label className="block text-xs font-medium text-[#86868b] mb-1.5">{label}</label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoFocus={autoFocus}
        className={`
          w-full px-3.5 py-2.5 rounded-xl text-sm text-[#f5f5f7]
          bg-[#141414] border outline-none transition-colors
          placeholder:text-[#3a3a3a]
          focus:border-[#0071e3] focus:ring-1 focus:ring-[#0071e3]
          ${error ? 'border-red-500/60' : 'border-[#1f1f1f]'}
        `}
      />
      {error && <p className="text-xs text-red-400 mt-1">{error}</p>}
    </div>
  )
}
