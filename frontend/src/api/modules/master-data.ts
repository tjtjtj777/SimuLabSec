import { http } from '@/api/http'
import type { LayerPage, LotPage, WaferPage } from '@/types/domain'

interface MasterQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
  lotId?: number
}

export const masterDataApi = {
  getLots(params: MasterQuery = {}) {
    return http.get<LotPage, LotPage>('/api/lots', { params })
  },
  getWafers(params: MasterQuery = {}) {
    return http.get<WaferPage, WaferPage>('/api/wafers', { params })
  },
  getLayers(params: MasterQuery = {}) {
    return http.get<LayerPage, LayerPage>('/api/layers', { params })
  },
}
