import { http } from '@/api/http'
import type { DashboardOverview } from '@/types/domain'

export const dashboardApi = {
  getOverview() {
    return http.get<DashboardOverview, DashboardOverview>('/api/dashboard/overview')
  },
}
