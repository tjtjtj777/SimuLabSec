import { http } from '@/api/http'
import type { DemoDatasetItem } from '@/types/domain'

export const demoDatasetApi = {
  getList(params: { status?: string } = {}) {
    return http.get<DemoDatasetItem[], DemoDatasetItem[]>('/api/demo-datasets', { params })
  },
}
