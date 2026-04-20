import { CalendarX2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'

export function EmptyRoutineState() {
  const navigate = useNavigate()

  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <CalendarX2 size={64} className="text-[#86868b] mb-5" strokeWidth={1.25} />
      <p className="text-[#f5f5f7] font-medium text-lg">Nenhuma rotina configurada</p>
      <p className="text-[#86868b] text-sm mt-1 max-w-xs">
        Importe um arquivo YAML para começar a usar o RoutineFlow.
      </p>
      <Button
        onClick={() => navigate('/import')}
        className="mt-6 bg-[#0071e3] hover:bg-[#0077ed] text-white rounded-xl px-6 h-10 text-sm font-medium"
      >
        Importar rotina →
      </Button>
    </div>
  )
}
