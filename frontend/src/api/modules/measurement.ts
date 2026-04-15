import { http } from '@/api/http'
import type { MeasurementRunItem, WaferAnalysisImportConfig, WaferAnalysisImportResult } from '@/types/domain'

export const measurementApi = {
  getRuns(
    params: {
      lotId?: string | number
      waferId?: string | number
      layerId?: string | number
      measurementType?: string
      stage?: string
      status?: string
      sourceType?: string
    } = {},
  ) {
    return http.get<MeasurementRunItem[], MeasurementRunItem[]>('/api/measurement-runs', { params })
  },
  importWaferAnalysis(file: File, config: WaferAnalysisImportConfig) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('config', new Blob([JSON.stringify(config)], { type: 'application/json' }))
    return http.post<WaferAnalysisImportResult, WaferAnalysisImportResult>('/api/measurement-runs/import', formData)
  },
  downloadImportTemplate() {
    return http.get<Blob, Blob>('/api/measurement-runs/import-template', { responseType: 'blob' as const })
  },
}
