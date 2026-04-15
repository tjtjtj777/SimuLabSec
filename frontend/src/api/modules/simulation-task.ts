import { http } from '@/api/http'
import type { SimulationTaskDetail, SimulationTaskItem, SimulationTaskStatusSummary } from '@/types/domain'

export const simulationTaskApi = {
  getList(
    params: {
      keyword?: string
      status?: string
      scenarioType?: string
      lotId?: number
      layerId?: number
      recipeVersionId?: number
    } = {},
  ) {
    return http.get<SimulationTaskItem[], SimulationTaskItem[]>('/api/simulation-tasks', { params })
  },
  getStatusSummary(params: { status?: string; scenarioType?: string } = {}) {
    return http.get<SimulationTaskStatusSummary[], SimulationTaskStatusSummary[]>('/api/simulation-tasks/status-summary', {
      params,
    })
  },
  getDetail(taskId: string | number) {
    return http.get<SimulationTaskDetail, SimulationTaskDetail>(`/api/simulation-tasks/${taskId}`)
  },
}
