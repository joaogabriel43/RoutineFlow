import { AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'

export function ErrorFallback() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] h-full text-center p-6">
      <div className="w-16 h-16 rounded-full bg-danger/10 flex items-center justify-center mb-6">
        <AlertTriangle className="text-danger w-8 h-8" />
      </div>
      <h2 className="text-xl font-medium text-[#F4F2EF] mb-2">Algo deu errado</h2>
      <p className="text-[#8C8A88] max-w-sm mb-6 text-sm">
        Ocorreu um erro inesperado ao carregar esta página. Nossa equipe foi notificada, mas você pode tentar recarregar.
      </p>
      <Button
        onClick={() => window.location.reload()}
        className="bg-brand hover:bg-brand/80 text-white"
      >
        Recarregar página
      </Button>
    </div>
  )
}
