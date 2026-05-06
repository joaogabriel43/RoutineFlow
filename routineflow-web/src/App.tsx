import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppLayout } from '@/components/shared/AppLayout'
import { LoginPage } from '@/pages/LoginPage'
import { TodayPage } from '@/pages/TodayPage'
import { WeekPage } from '@/pages/WeekPage'
import { AnalyticsPage } from '@/pages/AnalyticsPage'
import { AreaAnalyticsPage } from '@/pages/AreaAnalyticsPage'
import { ImportPage } from '@/pages/ImportPage'
import { ManagePage } from '@/pages/ManagePage'
import { HabitNowConverterPage } from '@/pages/HabitNowConverterPage'
import { SingleTasksPage } from '@/pages/SingleTasksPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
})

function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('rf_token')
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Rota pública */}
          <Route path="/login" element={<LoginPage />} />

          {/* Rotas protegidas — aninhadas dentro do AppLayout */}
          <Route
            element={
              <RequireAuth>
                <AppLayout />
              </RequireAuth>
            }
          >
            <Route index element={<TodayPage />} />
            <Route path="semana" element={<WeekPage />} />
            <Route path="analytics" element={<AnalyticsPage />} />
            <Route path="analytics/area/:areaId" element={<AreaAnalyticsPage />} />
            <Route path="import" element={<ImportPage />} />
            <Route path="import/habitnow" element={<HabitNowConverterPage />} />
            <Route path="tasks" element={<SingleTasksPage />} />
            <Route path="manage" element={<ManagePage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
