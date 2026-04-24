import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '@/services/api'

export function useAreaAnalytics(areaId: number | null) {
  return useQuery({
    queryKey: ['area-analytics', areaId],
    queryFn: () => analyticsApi.getAreaAnalytics(areaId!),
    enabled: areaId !== null,
    staleTime: 2 * 60 * 1000,
  })
}
