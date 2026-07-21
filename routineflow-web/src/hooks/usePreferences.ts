import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { preferencesApi } from '@/services/api'
import type { UpdatePreferencesRequest } from '@/types'
import { toast } from 'sonner'
import { useEffect } from 'react'

export function usePreferences() {
  const queryClient = useQueryClient()

  const { data: preferences, isLoading } = useQuery({
    queryKey: ['preferences'],
    queryFn: preferencesApi.getPreferences,
    staleTime: Infinity, // Preferências raramente mudam fora do app
  })

  const updateMutation = useMutation({
    mutationFn: (req: UpdatePreferencesRequest) => preferencesApi.updatePreferences(req),
    onSuccess: (newPrefs) => {
      queryClient.setQueryData(['preferences'], newPrefs)
      toast.success('Preferências salvas com sucesso!')
    },
    onError: () => {
      toast.error('Erro ao salvar preferências.')
    },
  })

  // Efeito para aplicar o tema no <html> tag
  useEffect(() => {
    if (!preferences?.theme) return

    const root = window.document.documentElement
    root.classList.remove('light', 'dark')

    if (preferences.theme === 'SYSTEM') {
      const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light'
      root.classList.add(systemTheme)
    } else {
      root.classList.add(preferences.theme.toLowerCase())
    }
  }, [preferences?.theme])

  return {
    preferences,
    isLoading,
    updatePreferences: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
  }
}
