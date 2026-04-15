import { http } from '@/api/http'
import type { PageResult } from '@/types/api'
import type { WaferConfigItem, WaferGenerateResult } from '@/types/domain'

export const waferConfigApi = {
  getDefault() {
    return http.get<WaferConfigItem, WaferConfigItem>('/api/wafer-configs/default')
  },
  validate(config: WaferConfigItem) {
    return http.post<string[], string[]>('/api/wafer-configs/validate', config)
  },
  getPage(params: { keyword?: string; dataScope?: string; layerId?: number; stage?: string; pageNo?: number; pageSize?: number } = {}) {
    return http.get<PageResult<WaferConfigItem>, PageResult<WaferConfigItem>>('/api/wafer-configs', { params })
  },
  getDetail(configId: string | number) {
    return http.get<WaferConfigItem, WaferConfigItem>(`/api/wafer-configs/${configId}`)
  },
  create(config: WaferConfigItem) {
    return http.post<WaferConfigItem, WaferConfigItem>('/api/wafer-configs', config)
  },
  update(configId: string | number, config: WaferConfigItem) {
    return http.put<WaferConfigItem, WaferConfigItem>(`/api/wafer-configs/${configId}`, config)
  },
  remove(configId: string | number) {
    return http.delete<void, void>(`/api/wafer-configs/${configId}`)
  },
  generate(payload: { configId?: string | number; saveAsConfig?: boolean; locale?: string; config?: WaferConfigItem }) {
    return http.post<WaferGenerateResult, WaferGenerateResult>('/api/wafer-configs/generate', payload, { timeout: 300000 })
  },
}
